package com.brother.big.service

import com.brother.big.integration.LLMIntegration
import com.brother.big.model.Commit
import com.brother.big.model.llm.MatrixSchema
import com.brother.big.utils.FileUtils

class RepositoryAnalyzer(
    private val languageModelIntegration: LLMIntegration = LLMIntegration()
) {

    suspend fun analyzeCommit(commit: Commit): MutableMap<String, MatrixSchema> {
        val analysisResults: MutableMap<String, MutableList<MatrixSchema>> = mutableMapOf()

        commit.files.forEach { file ->
            if (FileUtils.isValidFile(file)) {
                val parts = FileUtils.splitFile(file)
                val progLang = FileUtils.getFileLanguage(file)

                parts.forEach { part ->
                    val result = languageModelIntegration.analyzeCode(part, progLang)
                    analysisResults[progLang]?.add(result) ?: analysisResults.put(progLang, mutableListOf(result))
                }
            }
        }

        return mergeAnalysisResults(analysisResults)
    }

    private suspend fun mergeAnalysisResults(results: MutableMap<String, MutableList<MatrixSchema>>): MutableMap<String, MatrixSchema> {
        return languageModelIntegration.mergeResults(results)
    }
}