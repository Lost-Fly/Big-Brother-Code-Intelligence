package com.brother.big.integration.llm_utils

import com.brother.big.model.llm.Evaluation
import com.brother.big.model.llm.MatrixSchema
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream

object LLMUtils {
    private val mapper = jacksonObjectMapper()

    fun generateMergePrompt(matrixSchemasList: List<MatrixSchema>, jsonSche: String): String {
        val prompt = matrixSchemasList.joinToString("\n") { matrixSchema ->
            """
            ${mapper.writeValueAsString(matrixSchema)}
            """.trimIndent()
        }

        // TODO - separate consts
        val mrPrompt = """
            Below are the matrix's with analysed developer skills:  
            $prompt
            
            And also there is a JSON schema for answer to be made, you have to strcitly follow it:
            $jsonSche
            
            You need to make one resulting json matrix out of these json matrix schemas according to answer JSON Schema. This final scheme should be a 
            combination of developer skills based on the schemes given to you. The scheme must have the same structure 
            as the input scheme. For numerical parameters, the final scheme should have the largest value of all the 
            values of these parameters in the input schemes, but not over than 10. For the 'summary' parameter, you need to collect short summary of 
            all incoming summaries, which will be based on the analysis of incoming summaries and include all descriptions
            of the skills mentioned in the incoming summaries from the 'summary' parameter of json schemes. There should be no 
            repetitions in the 'summary' text, but there should be detailed information about each skill, you have to write summary in 1-8 sentences.
            Make such a resulting matrix.    
            
            DO NOT WRITE ANYTHING EXCEPT JSON OUTPUT
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
            
            And also there is a JSON schema for answer to be made, you have to strcitly follow it:
            $jsonSche
            
            There is also a list of criteria that define the developer's competence matrix.
            For each criterion, there is also a hint for its evaluation, which explains what you should pay attention to when rating.
            $additionalInfo
            
            The score for each field should be a number from 1 to 10. 
            It is necessary that you select as many competencies as possible from the lines of code and fill out the appropriate competency assessments for them.
            If there is no data at all for any competence, specify the score as 0.
            The assessment should primarily depend on the complexity, accuracy of the written code and depth of knowledge, and only then on the number of lines corresponding to the competence.
            The score cannot be high without demonstrating a deep level of knowledge.
            You also need to give a detailed brief comment on the entire competence matrix in 'summary' field.
            In the 'summary', briefly summarize developer skills(in 1-3 sentences), and also add to 'summary' what grade you would give the developer based on his competencies.
            
            DO NOT WRITE ANYTHING EXCEPT JSON OUTPUT
        """.trimIndent()
    }


    fun loadSchema(language: String): String {
        val schemaFilePath = "prompt_utils/$language.json"

        val inputStream: InputStream = this::class.java.classLoader.getResourceAsStream(schemaFilePath)
            ?: throw IllegalArgumentException("Schema file not found: $schemaFilePath")

        val schema: MatrixSchema = mapper.readValue(inputStream)

        return mapper.writeValueAsString(schema)
    }

    val MOTIVATION_BASICS = "You are a strong code line analyzer"
}