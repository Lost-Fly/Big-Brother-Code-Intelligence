package com.brother.big.integration

import com.brother.big.integration.llm_utils.LLMPromptConsts.MOTIVATION_BASICS
import com.brother.big.integration.llm_utils.LLMUtils.generateMergePrompt
import com.brother.big.integration.llm_utils.LLMUtils.generatePrompt
import com.brother.big.integration.llm_utils.LLMUtils.loadSchema
import com.brother.big.model.llm.MatrixSchema
import com.brother.big.utils.BigLogger.logError
import com.brother.big.utils.BigLogger.logInfo
import com.brother.big.utils.Config
import com.brother.big.utils.JsonUtils.cleanRawString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
    }

    val llmClient = HttpClient { // TODO - ADD Proxy to llmClient (use proxy server for openAI requests)
        install(HttpTimeout) {
            requestTimeoutMillis = Config["llm.requestTimeoutMillis"]?.toLong() ?: 10000
        }
    }

    private val mapper = jacksonObjectMapper()

    fun getRequestBody(prompt: String): String {
        return mapper.writeValueAsString(mapOf(
            "model" to MODEL,
            "messages" to listOf(
                mapOf("role" to SYSTEM_ROLE, "content" to MOTIVATION_BASICS),
                mapOf("role" to USER_ROLE, "content" to prompt)
            ),
            "max_tokens" to MAX_TOKENS,
            "temperature" to TEMPERATURE
        ))
    }

    suspend fun getResponse(requestBody: String): HttpResponse = llmClient.post(LLM_URL) {
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $API_KEY")
        setBody(requestBody)
    }

    fun deserializeMatrixSchemaJac(jsonString: String): MatrixSchema {
        val cleanedJson = cleanRawString(jsonString)
        logInfo("CLEANED JSON: $cleanedJson")
        return try {
            mapper.readValue<MatrixSchema>(cleanedJson)
        } catch (e: Exception) {
            logError("DESERIALIZATION ERROR: ${e.message}")
            throw e
        }
    }

    suspend fun analyzeCode(code: String, language: String): MatrixSchema {
        val prompt = generatePrompt(code, language)
        val requestBody = getRequestBody(prompt)

        val response: HttpResponse = getResponse(requestBody)
        val rawResponseBody = response.bodyAsText()

        logInfo("LLM RESPONSE: $rawResponseBody")

        val content = extractGptResponse(rawResponseBody)
        requireNotNull(content) { logError("GPT RESPONSE EXTRACTION FAILED") }

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

            val response = getResponse(requestBody)
            val rawResponseBody = response.bodyAsText()

            logInfo("LLM MERGING: $rawResponseBody")

            val content = extractGptResponse(rawResponseBody)
            requireNotNull(content) { logError("GPT RESPONSE EXTRACTION FAILED") }

            val mergedMatrixSchema = deserializeMatrixSchemaJac(content)
            mergedResults[language] = mergedMatrixSchema
        }

        return mergedResults
    }

    fun extractGptResponse(rawResp: String): String? {
        val jsonNode: JsonNode = mapper.readTree(rawResp)
        val content = jsonNode["choices"]?.get(0)?.get("message")?.get("content")?.asText()
        return content
    }
}
