package com.brother.big.model

data class AnalysisResult(
    val developerName: String,
    val totalFiles: Int?,
    val languagesAnalyse: Map<String, LanguageCompetencies>
)

data class LanguageCompetencies(
    val progLang: String?,
    val languageCompetencies: Map<String, String>,
    val algorithmSkills: Int?,
    val dbSkills: Int?,
    val brokerSkills: Int?,
    val summary: String?,
)