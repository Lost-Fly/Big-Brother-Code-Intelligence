package com.brother.big.service

import com.brother.big.integration.LLMIntegration
import com.brother.big.model.Commit
import com.brother.big.model.llm.MatrixSchema
import com.brother.big.utils.Config
import com.brother.big.utils.FileUtils
import kotlinx.coroutines.*

class RepositoryAnalyzer(
    private val languageModelIntegration: LLMIntegration = LLMIntegration(),
    private val maxUnitLength: Int =  Config["utils.partSize"]?.toInt() ?: 5000
) {

    suspend fun analyzeCommit(commit: Commit): MutableMap<String, MatrixSchema> = coroutineScope {
        val analysisResults: MutableMap<String, MutableList<MatrixSchema>> = mutableMapOf()
        val contentByProgLang: MutableMap<String, MutableList<String>> = mutableMapOf()

        commit.files.forEach { file ->
            if (FileUtils.isValidFile(file)) {
                val progLang = FileUtils.getFileLanguage(file)
                val parts = FileUtils.splitFile(file)

                parts.forEach { part ->
                    val currentUnitList = contentByProgLang[progLang] ?: mutableListOf()

                    if (currentUnitList.isEmpty() || currentUnitList.last().length + part.length > maxUnitLength) {
                        currentUnitList.add(part)
                    } else {
                        currentUnitList[currentUnitList.size - 1] = currentUnitList.last() + part
                    }

                    contentByProgLang[progLang] = currentUnitList
                }
            }
        }

        val analysisJobs = contentByProgLang.flatMap { (progLang, units) ->
            units.map { unit ->
                async {
                    languageModelIntegration.analyzeCode(unit, progLang) to progLang
                }
            }
        }

        val completedJobs = analysisJobs.awaitAll()

        completedJobs.forEach { (result, progLang) ->
            analysisResults[progLang]?.add(result) ?: analysisResults.put(progLang, mutableListOf(result))
        }

        commit.files.forEach { file ->
            file.delete()
        }

        return@coroutineScope mergeAnalysisResults(analysisResults)
    }

    private suspend fun mergeAnalysisResults(
        results: MutableMap<String, MutableList<MatrixSchema>>
    ): MutableMap<String, MatrixSchema> {
        return languageModelIntegration.mergeResults(results)
    }
}