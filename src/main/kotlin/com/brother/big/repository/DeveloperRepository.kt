package com.brother.big.repository

import com.brother.big.model.AnalysisResult
import com.brother.big.model.Developer

interface DeveloperRepository {
    fun saveDeveloperByGitName(developer: Developer): Boolean
    fun getDeveloperByGitName(name: String): Developer?
    fun saveAnalysisResult(developerName: String, analysisResult: AnalysisResult): Boolean
    fun getAnalysisResultByDeveloper(developerName: String): AnalysisResult?
    fun getAllAnalysisResults(): List<AnalysisResult>
}