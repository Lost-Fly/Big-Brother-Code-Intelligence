package com.brother.big.service

import com.brother.big.model.AnalysisResult
import com.brother.big.model.LanguageCompetencies
import com.brother.big.model.Report

class ReportGenerator {

    fun generateReport(analysisResult: AnalysisResult): Report {
        return Report(
            detailedAnalysis = analysisResult
        )
    }

    private fun generateSummary(result: AnalysisResult): String {
        return """
            ${result.totalFiles}
            ${generateLanguagesSummary(result.languagesAnalyse)}
        """.trimIndent()
    }

    private fun generateLanguagesSummary(languagesAnalyse: Map<String, LanguageCompetencies>): String {
        return languagesAnalyse.map { (language, competencies) ->
            """
           $language
           ${competencies.summary}
           ${mkCompetencies(competencies.languageCompetencies)}
           ${competencies.algorithmSkills}
           ${competencies.dbSkills}
           ${competencies.brokerSkills}
            """.trimIndent()
        }.joinToString("\n")
    }

    private fun mkCompetencies(competencies: Map<String, String>): String {
        return competencies.map { (competence, evaluation) ->
            "- $competence: $evaluation"
        }.joinToString("\n")
    }
}