package com.brother.big.service

import com.brother.big.integration.GitIntegration
import com.brother.big.integration.LLMIntegration
import com.brother.big.model.*
import com.brother.big.model.llm.MatrixSchema
import com.brother.big.repository.DeveloperRepository
import com.brother.big.repository.DeveloperRepositoryImpl
import com.brother.big.utils.Config
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class DeveloperService(
    private val gitIntegration: GitIntegration = GitIntegration(),
    private val repositoryAnalyzer: RepositoryAnalyzer = RepositoryAnalyzer(),
    private val reportGenerator: ReportGenerator = ReportGenerator(),
    private val languageModelIntegration: LLMIntegration = LLMIntegration(),
    private val developerRepository: DeveloperRepository = DeveloperRepositoryImpl()
) {
    companion object {
        private val PERMITS: Int = Config["async.permits"]?.toInt() ?: 50
        private val BATCH_SIZE: Int = Config["async.batchSize"]?.toInt() ?: 5
    }

    private val semaphore = Semaphore(PERMITS)

    suspend fun evaluateDeveloper(developer: Developer): Report = coroutineScope {
        val allCommitsResults: MutableMap<String, MutableList<MatrixSchema>> = mutableMapOf()

        developer.repositories.chunked(BATCH_SIZE).forEach { repositoriesBatch ->
            val deferredResults = repositoriesBatch.map { repositoryUrl ->
                async {
                    val commits: List<Commit> =
                        gitIntegration.getCommits(developer.name, developer.token, repositoryUrl)

                    val commitAnalysisResults = commits.map { commit ->
                        async {
                            semaphore.withPermit {
                                repositoryAnalyzer.analyzeCommit(commit)
                            }
                        }
                    }.awaitAll()

                    commitAnalysisResults.forEach { commitAnalysisResult ->
                        commitAnalysisResult.forEach { (language, analysisResult) ->
                            allCommitsResults.computeIfAbsent(language) { mutableListOf() }.add(analysisResult)
                        }
                    }
                }
            }
            deferredResults.awaitAll()
        }

        val mergedResult = mergeAnalysisResults(allCommitsResults)
        val convertedAnalysisResult = convertToAnalysisResult(developer.name, mergedResult, allCommitsResults.size)
        val report = reportGenerator.generateReport(convertedAnalysisResult)

        developerRepository.saveAnalysisResult(developer.name, convertedAnalysisResult)
        return@coroutineScope report
    }

    fun getAllAnalysisResults(): List<AnalysisResult> {
        return developerRepository.getAllAnalysisResults()
    }

    private suspend fun mergeAnalysisResults(results: MutableMap<String, MutableList<MatrixSchema>>): MutableMap<String, MatrixSchema> {
        return languageModelIntegration.mergeResults(results)
    }

    private fun convertToAnalysisResult(devName: String, languagesAnalyses: Map<String, MatrixSchema>, commitsAmount: Int): AnalysisResult {
        return AnalysisResult(
            developerName = devName,
            totalFiles = commitsAmount,
            languagesAnalyse = languagesAnalyses.mapValues { (_, matrixSchema) ->
                convertToLanguageCompetencies(matrixSchema)
            }
        )
    }

    private fun convertToLanguageCompetencies(matrixSchema: MatrixSchema): LanguageCompetencies {
        return LanguageCompetencies(
            progLang = matrixSchema.title,
            languageCompetencies = matrixSchema.languageCompetencies.map { (competence, evaluation) ->
                competence to evaluation.evaluation.toString()
            }.toMap(),
            algorithmSkills = matrixSchema.algorithmSkills?.evaluation,
            dbSkills = matrixSchema.dbSkills?.evaluation,
            brokerSkills = matrixSchema.brokerSkills?.evaluation,
            summary = matrixSchema.summary,
        )
    }
}