package com.brother.big.model.llm

data class LLMRequest(
    val prompt: List<String>,
    val system_prompt: String,
    val max_tokens: Int,
    val n: Int,
    val temperature: Double,
    val schema: String
)
