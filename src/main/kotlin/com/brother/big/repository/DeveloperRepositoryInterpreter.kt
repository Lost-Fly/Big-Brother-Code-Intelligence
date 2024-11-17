package com.brother.big.repository

import com.brother.big.model.AnalysisResult

class DeveloperRepositoryImpl( // TODO - add saveDeveloperByGitName and getDeveloperByGitName method
    private val mongoDbClient: MongoDbClient = MongoDbClient()
) : DeveloperRepository {

    override fun saveAnalysisResult(developerName: String, analysisResult: AnalysisResult): Boolean {
        return mongoDbClient.saveAnalysisResult(developerName, analysisResult)
    }

    override fun getAnalysisResultByDeveloper(developerName: String): AnalysisResult? {
        return mongoDbClient.getAnalysisResultByDeveloper(developerName)
    }

    override fun getAllAnalysisResults(): List<AnalysisResult> {
        return mongoDbClient.getAllAnalysisResults()
    }
}