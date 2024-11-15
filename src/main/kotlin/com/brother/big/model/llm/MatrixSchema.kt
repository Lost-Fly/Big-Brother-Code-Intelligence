package com.brother.big.model.llm

data class MatrixSchema(
    val type: String?,
    val title: String?,
    val languageCompetencies: Map<String, Evaluation>,
    val algorithmSkills: Evaluation?,
    val dbSkills: Evaluation?,
    val brokerSkills: Evaluation?,
    val summary: String?,
)

data class Evaluation(
    val evaluation: Int
)