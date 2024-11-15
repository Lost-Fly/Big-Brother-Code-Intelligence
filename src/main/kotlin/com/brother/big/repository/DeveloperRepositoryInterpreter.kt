package com.brother.big.repository

import com.brother.big.model.AnalysisResult
import com.brother.big.model.Developer

class DeveloperRepositoryImpl(
    private val mongoDbClient: MongoDbClient = MongoDbClient()  // TODO - chenge methods (there are extra \ add new)
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