package com.brother.big.integration.llm_utils

import com.brother.big.integration.llm_utils.LLMPromptConsts.ANALYSE_REQUEST
import com.brother.big.integration.llm_utils.LLMPromptConsts.JSON_EXPECTATION
import com.brother.big.integration.llm_utils.LLMPromptConsts.MERGE_REQUEST
import com.brother.big.model.llm.MatrixSchema
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream

object LLMUtils {
    private val mapper = jacksonObjectMapper()

    fun generateMergePrompt(matrixSchemasList: List<MatrixSchema>, jsonSche: String): String {
        val prompt = matrixSchemasList.joinToString("\n") { matrixSchema ->
            """
                
            Competency matrix:
            ${mapper.writeValueAsString(matrixSchema)}
            
            """.trimIndent()
        }

        val mrPrompt = """
            Below are the matrix's with analysed developer skills:  
            $prompt
            
            And also there is a JSON schema for answer to be made, you have to strictly follow it:
            $jsonSche
            
            $MERGE_REQUEST  
            
            $JSON_EXPECTATION
        """.trimIndent()

        return mrPrompt
    }

    fun generatePrompt(code: String, language: String): String {
        val filePath = "prompt_utils/$language-hint.txt"
        val filePathJso = "prompt_utils/$language.json"

        val inputStream: InputStream? = this::class.java.classLoader.getResourceAsStream(filePath)
        val additionalInfo = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalArgumentException("File not found: $filePath")

        val inputStream2: InputStream? = this::class.java.classLoader.getResourceAsStream(filePathJso)
        val jsonSche = inputStream2?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalArgumentException("File not found: $filePathJso")

        return """
            Below are the lines from the developer's commit.
            $code
            
            And also there is a JSON schema for answer to be made, you have to strictly follow it:
            $jsonSche
            
            There is also a list of criteria that define the developer's competence matrix.
            For each criterion, there is also a hint for its evaluation, which explains what you should pay attention to when rating.
            $additionalInfo
            
            $ANALYSE_REQUEST
            
            $JSON_EXPECTATION
        """.trimIndent()
    }


    fun loadSchema(language: String): String {
        val schemaFilePath = "prompt_utils/$language.json"

        val inputStream: InputStream = this::class.java.classLoader.getResourceAsStream(schemaFilePath)
            ?: throw IllegalArgumentException("Schema file not found: $schemaFilePath")

        val schema: MatrixSchema = mapper.readValue(inputStream)

        return mapper.writeValueAsString(schema)
    }
}