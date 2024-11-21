package com.brother.big.utils

import com.brother.big.utils.BigLogger.logError
import java.io.File

object FileUtils {

    private val partSize = Config["utils.partSize"]?.toInt() ?: 5000

    fun isValidFile(file: File): Boolean {
        if (file.exists()) {
            val excludedExtensions = Config["excluded.file.extensions"]?.split(",") ?: emptyList()
            return file.extension !in excludedExtensions
        } else {
            logError("FileUtils#isValidFile Error - file: ${file.name} NOT FOUND")
            return true
        }
    }

    fun splitFile(file: File): List<String> {
        if (file.exists()) {
            val fileContent = file.readText()
            return fileContent.chunked(partSize)
        } else {
            logError("FileUtils#splitFile Error - file: ${file.name} NOT FOUND")
            return listOf("")
        }
    }

    fun getFileLanguage(file: File): String {
        if (file.exists()) {
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
        } else {
            logError("FileUtils#getFileLanguage Error - file: ${file.name} NOT FOUND")
            return "default"
        }
    }
}