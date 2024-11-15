package com.brother.big.integration

import com.brother.big.integration.llm_utils.LLMUtils.MOTIVATION_BASICS
import com.brother.big.integration.llm_utils.LLMUtils.generateMergePrompt
import com.brother.big.integration.llm_utils.LLMUtils.generatePrompt
import com.brother.big.integration.llm_utils.LLMUtils.loadSchema
import com.brother.big.model.llm.Evaluation
import com.brother.big.model.llm.LLMRequest
import com.brother.big.model.llm.LLMResponse
import com.brother.big.model.llm.MatrixSchema
import com.brother.big.utils.JsonUtils.cleanRawString
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import java.io.InputStream
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator
import kotlin.collections.listOf
import kotlin.collections.mutableMapOf
import kotlin.collections.reduce
import kotlin.collections.set

class LLMIntegration {
    companion object LLMIntegration {
        // TODO - move to properties file
        const val MODEL = "gpt-4o-mini"
        const val MAX_TOKENS = 5000
        const val TEMPERATURE = 0.1
        const val SYSTEM_ROLE = "system"
        const val USER_ROLE = "user"
        const val API_KEY = "NONONO MISTER FISH" // TODO - move to properties
        const val openAiUrl = "https://api.openai.com/v1/chat/completions" // TODO - move to prtoperties
    }

    val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 3600_000 // TODO - saperate param to properties
        }
    }

    private val mapper = jacksonObjectMapper()

    fun getRequestBody(prompt: String, schema: String): String {
        return mapper.writeValueAsString(mapOf(
            "model" to MODEL, // TODO - use consts
            "messages" to listOf(
                mapOf("role" to SYSTEM_ROLE, "content" to MOTIVATION_BASICS),
                mapOf("role" to USER_ROLE, "content" to prompt)
            ),
            "max_tokens" to MAX_TOKENS,
            "temperature" to TEMPERATURE,
            "response_format" to mapper.readValue("""{"type": "json_object", "json_schema": $schema}""")
        ))
    }

    suspend fun getResponse(requestBody: String): HttpResponse = client.post(openAiUrl) {
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $API_KEY")
        setBody(requestBody)
    }

    fun deserializeMatrixSchemaJac(jsonString: String): MatrixSchema {
        val cleanedJson = cleanRawString(jsonString) // .drop(1).dropLast(1)
        println("Очищенный JSON: $cleanedJson") // TODO - use slf4j logger
        return try {
            mapper.readValue<MatrixSchema>(cleanedJson)
        } catch (e: Exception) {
            println("Ошибка при десериализации: ${e.message}") // TODO - use slf4j logger
            throw e
        }
    }

    suspend fun analyzeCode(code: String, language: String): MatrixSchema {
        val prompt = generatePrompt(code, language)
        val schema = loadSchema(language)

        println("GETTED SCHEMA: $schema") // TODO - use slf4j logger

        val requestBody = getRequestBody(prompt, schema)

        val response: HttpResponse = getResponse(requestBody)

        val rawResponseBody = response.bodyAsText()

        println("Ответ GPT: $rawResponseBody") // TODO - use slf4j logger

        val jsonNode: JsonNode = mapper.readTree(rawResponseBody)

        val content = jsonNode["choices"]?.get(0)?.get("message")?.get("content")?.asText()

        requireNotNull(content) { "Не удалось извлечь поле 'content' из ответа GPT" } // TODO - use slf4j logger

        println("Ответ content: $content") // TODO - use slf4j logger

        return deserializeMatrixSchemaJac(content)
    }

    suspend fun mergeResults(results: MutableMap<String, MutableList<MatrixSchema>>): MutableMap<String, MatrixSchema> {
        val mergedResults: MutableMap<String, MatrixSchema> = mutableMapOf()

        val limitedResults: Map<String, List<MatrixSchema>> = results.mapValues { entry ->
            entry.value.take(30)
        }

        for ((language, matrixSchemasList) in limitedResults) {

            val schema = loadSchema(language)
            val prompt = generateMergePrompt(matrixSchemasList, schema)

            val requestBody = getRequestBody(prompt, schema)

            val response = getResponse(requestBody)

            val rawResponseBody = response.bodyAsText()

            println("Ответ МЕРЖИНГ GPT: $rawResponseBody") // TODO - use slf4j logger


            val jsonNode: JsonNode = mapper.readTree(rawResponseBody)

            val content = jsonNode["choices"]?.get(0)?.get("message")?.get("content")?.asText() // TODO - use consts

            requireNotNull(content) { "Не удалось извлечь поле 'content' из ответа GPT" } // TODO - use slf4j logger

            println("Ответ content: $content") // TODO - use slf4j logger

            println("Ответ МЕРЖИНГ GPT: $response") // TODO - use slf4j logger

            val mergedMatrixSchema = deserializeMatrixSchemaJac(content)
            mergedResults[language] = mergedMatrixSchema
        }

        return mergedResults
    }
}
