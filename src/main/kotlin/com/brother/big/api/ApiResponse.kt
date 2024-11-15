package com.brother.big.api

data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null
)
