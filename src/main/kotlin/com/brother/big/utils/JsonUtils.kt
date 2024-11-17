package com.brother.big.utils

object JsonUtils {
    fun cleanRawString(rawString: String): String {
        val isPrefixJson = rawString.startsWith("```json", true)
        val isPostfixJson = rawString.endsWith("```", true)

        val predStr = when {
            isPrefixJson && !isPostfixJson -> rawString.drop(7)
            isPostfixJson && !isPrefixJson -> rawString.dropLast(3)
            isPrefixJson &&  isPostfixJson -> rawString.drop(7).dropLast(3)
            else -> rawString
        }

        val resString =  predStr
            .trim() // Убираем пробелы по краям
            .replace("\\","")  // Убираем переводы строк
            .replace("\r", "") // Убираем возвраты каретки
            .replace("\\s+".toRegex(), " ") // Сжимаем лишние пробелы
            .replace("(\\w+):".toRegex(), "\"$1\":") // Добавляем кавычки и двоеточия ключам без него
            .replace("""\s*\n\s*""".toRegex(), "") // Убираем переходы строк и лишние пробелы
            .replace("""([a-zA-Z0-9_]+)\s*:\s*""".toRegex(), "\"$1\": ") // Обрабатываем ключи без кавычек
            .replace("""\s+""".toRegex(), " ") // Устраняем лишние пробелы
            .replace("\"evaluation\": (\\d)".toRegex(), "\"evaluation\": $1")
            .replace("""\s*\r?\n\s*""".toRegex(), "") // Убираем пробелы вокруг новой строки
            .replace("""([a-zA-Z0-9_]+)\s*:\s*""".toRegex(), "\"$1\": ") // Замена ключей без кавычек на ключи с кавычками
            .replace("""\s+""".toRegex(), " ") // Лишние пробелы между элементами
            .replace("\"evaluation\": (\\d)".toRegex(), "\"evaluation\": $1")// Числа для evaluation + ключи без кавычек исправляем, добавляем их

        return resString
    }
}