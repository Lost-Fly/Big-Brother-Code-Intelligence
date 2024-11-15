package com.brother.big.model

import java.io.File

data class Commit(
    val id: String,
    val developer: String,
    val message: String,
    val timestamp: Long,
    val files: List<File>
)