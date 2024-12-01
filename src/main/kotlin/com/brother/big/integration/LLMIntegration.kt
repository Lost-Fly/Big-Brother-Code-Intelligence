package com.brother.big.integration

import com.brother.big.integration.llm_utils.LLMPromptConsts.MOTIVATION_BASICS
import com.brother.big.integration.llm_utils.LLMUtils.generateMergePrompt
import com.brother.big.integration.llm_utils.LLMUtils.generatePrompt
import com.brother.big.integration.llm_utils.LLMUtils.loadSchema
import com.brother.big.model.llm.Evaluation
import com.brother.big.model.llm.MatrixSchema
import com.brother.big.utils.BigLogger.logDebug
import com.brother.big.utils.BigLogger.logError
import com.brother.big.utils.BigLogger.logInfo
import com.brother.big.utils.Config
import com.brother.big.utils.JsonUtils.cleanRawString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.InetSocketAddress
import java.net.Proxy
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


class LLMIntegration {
    companion object LLMIntegration {
        val MODEL = Config["llm.model"] ?: "llama3.7"
        val MAX_TOKENS: Int = Config["llm.maxTokens"]?.toInt() ?: 8092
        val TEMPERATURE: Double = Config["llm.temperature"]?.toDouble() ?: 0.0
        val SYSTEM_ROLE = Config["llm.systemRole"] ?: "system"
        val USER_ROLE = Config["llm.userRole"] ?: "user"
        val API_KEY = Config["llm.apiKey"] ?: "apiKey"
        val LLM_URL = Config["llm.llmUrl"] ?: "llm://localhost:1212"
        val MERGE_LIMIT: Int = Config["llm.mergeLimit"]?.toInt() ?: 30
        val PROXY_URL = Config["llm.proxyUrl"] ?: "proxyHost"
        val PROXY_PORT: Int = Config["llm.proxyPort"]?.toInt() ?: 60443
        val USE_PROXY: Boolean = Config["llm.useProxy"]?.toBoolean() ?: false
        val RETRY_ATTEMPTS: Int = Config["llm.retryAttempts"]?.toInt() ?: 3
        val RETRY_DELAY_MS: Long = Config["llm.retryDelayMs"]?.toLong() ?: 2000
        val CB_FAILURE_THRESHOLD = Config["circuitBreaker.failureThreshold"]?.toInt() ?: 3
        val CB_TIMEOUT_DURATION: Long = Config["circuitBreaker.timeoutDurationMillis"]?.toLong() ?: 12000
    }

    private val proxyAddress = InetSocketAddress(PROXY_URL, PROXY_PORT)
    private val httpsProxy = Proxy(Proxy.Type.HTTP, proxyAddress)

    private var consecutiveFailures = AtomicInteger(0)
    private val circuitOpenUntil = AtomicLong(0)
    private val mutex = Mutex()

    private val mapper = jacksonObjectMapper()

    val llmClient = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
                proxy = httpsProxy
            }
        }
        install (HttpTimeout) {
            requestTimeoutMillis = Config["llm.requestTimeoutMillis"]?.toLong() ?: 10000
            connectTimeoutMillis = Config["llm.connectionTimeoutMillis"]?.toLong() ?: 10000
        }
        install(HttpRequestRetry) {
            maxRetries = RETRY_ATTEMPTS
            retryIf { _, response ->
                response.status.value >= 500
            }
            retryOnExceptionIf { _, cause ->
                cause is TimeoutCancellationException || cause is java.io.IOException
            }
            delayMillis { retry ->
                RETRY_DELAY_MS * retry
            }
            modifyRequest { http ->
                logInfo("Retrying request [Body: ${http.body}]")
            }
        }
    }

    private fun isCircuitOpen(): Boolean {
        val now = System.currentTimeMillis()
        return now < circuitOpenUntil.get()
    }

    private fun openCircuit() {
        val now = System.currentTimeMillis()
        circuitOpenUntil.set(now + CB_TIMEOUT_DURATION)
        logError("CB is open until ${circuitOpenUntil.get()}")
    }

    private suspend fun resetCircuit() {
        mutex.withLock {
            consecutiveFailures.set(0)
            circuitOpenUntil.set(0)
        }
    }

    private fun getRequestBody(prompt: String): String {
        return mapper.writeValueAsString(
            mapOf(
                "model" to MODEL,
                "messages" to listOf(
                    mapOf("role" to SYSTEM_ROLE, "content" to MOTIVATION_BASICS),
                    mapOf("role" to USER_ROLE, "content" to prompt)
                ),
                "max_tokens" to MAX_TOKENS,
                "temperature" to TEMPERATURE
            )
        )
    }

    private suspend fun getResponse(requestBody: String): HttpResponse? {
        if (isCircuitOpen()) {
            logError("Circuit breaker is open, refusing request.")
            return null
        }
        return try {
            val response = llmClient.post(LLM_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $API_KEY")
                setBody(requestBody)
            }

            resetCircuit()
            response
        } catch (e: Exception) {
            logError("Error while http request to LLM: ${e.message}")

            val currentFailures = consecutiveFailures.incrementAndGet()
            if (currentFailures >= CB_FAILURE_THRESHOLD) {
                openCircuit()
            }
            return null
        }
    }

    private fun deserializeMatrixSchemaJac(jsonString: String): MatrixSchema {
        val cleanedJson = cleanRawString(jsonString)
        return try {
            mapper.readValue<MatrixSchema>(cleanedJson)
        } catch (e: Exception) {
            logError("DESERIALIZATION ERROR: ${e.message}")
            return emptyMatrix
        }
    }

    suspend fun analyzeCode(code: String, language: String): MatrixSchema {
        val prompt = generatePrompt(code, language)
        val requestBody = getRequestBody(prompt)

        val response: HttpResponse = getResponse(requestBody) ?: return emptyMatrix
        val rawResponseBody = response.bodyAsText()

        val content = extractGptResponse(rawResponseBody)

        return deserializeMatrixSchemaJac(content)
    }

    suspend fun mergeResults(results: MutableMap<String, MutableList<MatrixSchema>>): MutableMap<String, MatrixSchema> {
        val mergedResults: MutableMap<String, MatrixSchema> = mutableMapOf()

        val limitedResults: Map<String, List<MatrixSchema>> = results.mapValues { entry ->
            entry.value.take(MERGE_LIMIT)
        }

        for ((language, matrixSchemasList) in limitedResults) {
            val schema = loadSchema(language)
            val prompt = generateMergePrompt(matrixSchemasList, schema)

            val requestBody = getRequestBody(prompt)

            val response = getResponse(requestBody) ?: return mutableMapOf("default" to emptyMatrix)
            val rawResponseBody = response.bodyAsText()

            val content = extractGptResponse(rawResponseBody)

            val mergedMatrixSchema = deserializeMatrixSchemaJac(content)
            mergedResults[language] = mergedMatrixSchema
        }

        return mergedResults
    }

    private fun extractGptResponse(rawResp: String): String {
        val jsonNode: JsonNode = mapper.readTree(rawResp)
        try {
            val content = jsonNode["choices"]?.get(0)?.get("message")?.get("content")?.asText()
            requireNotNull(content)
            return content
        } catch (e: Error) {
            logError("LLM RESPONSE EXTRACTION FAILED: ${e.message}")
            return emptyMatrix.toString()
        }
    }

    private val emptyMatrix = MatrixSchema(
        type = "",
        title = "",
        languageCompetencies =  mutableMapOf("" to Evaluation(0)),
        algorithmSkills = Evaluation(0),
        dbSkills = Evaluation(0),
        brokerSkills = Evaluation(0),
        summary = ""
    )
}
