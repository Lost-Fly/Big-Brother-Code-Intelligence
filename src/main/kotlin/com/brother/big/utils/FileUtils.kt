package com.brother.big.utils

import java.io.File

object FileUtils {

    fun isValidFile(file: File): Boolean {
        val excludedExtensions =
            listOf(
                "conf", "md", "json",
                "yml", "yaml", "xml",
                "txt", "lock", "gitignore",
                "gitattributes", "gitmodules",
                "gitkeep", "git", "editorconfig",
                "gitlab-ci.yml", "gitlab-ci.yaml",
                "gitlab-ci.json", "gitlab-ci",
                "gitlab", "github", "idea",
                "vscode", "vs", "", " ", '.', ' ', "properties",
                "mod", "sum", "gradle", "gradlew", "gradlew.bat",
            ) // TODO - optimize and add more
        return file.extension !in excludedExtensions
    }

    fun splitFile(file: File, partSize: Int = 7000): List<String> {
        val fileContent = file.readText()
        return fileContent.chunked(partSize)
    }

    fun getFileLanguage(file: File): String {
        val extension = file.extension

        println("DETER FILE ext: " + extension) // TODO - use slf4j logger
        return when (extension) { // TODO - add more
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