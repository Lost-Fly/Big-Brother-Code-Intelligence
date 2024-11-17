package com.brother.big.utils

import java.io.File

object FileUtils {

    private val partSize = Config["utils.partSize"]?.toInt() ?: 5000

    fun isValidFile(file: File): Boolean {
        val excludedExtensions = Config["excluded.file.extensions"]?.split(",") ?: emptyList()
        return file.extension !in excludedExtensions
    }

    fun splitFile(file: File): List<String> {
        val fileContent = file.readText()
        return fileContent.chunked(partSize)
    }

    fun getFileLanguage(file: File): String {
        val extension = file.extension

        return when (extension) {
            "kt" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "go" -> "go"
            "c" -> "c"
            "cpp" -> "cpp"
            "cs" -> "csharp"
            "php" -> "php"
            "rb" -> "ruby"
            "rs" -> "rust"
            "swift" -> "swift"
            "scala" -> "scala"
            "pl" -> "perl"
            "sh" -> "shell"
            "bash" -> "shell"
            else -> "default"
        }
    }
}