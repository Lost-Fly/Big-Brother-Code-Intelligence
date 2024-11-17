package com.brother.big.repository

import com.brother.big.model.AnalysisResult

interface DeveloperRepository { // TODO - add saveDeveloperByGitName and getDeveloperByGitName method
    fun saveAnalysisResult(developerName: String, analysisResult: AnalysisResult): Boolean
    fun getAnalysisResultByDeveloper(developerName: String): AnalysisResult?
    fun getAllAnalysisResults(): List<AnalysisResult>
}