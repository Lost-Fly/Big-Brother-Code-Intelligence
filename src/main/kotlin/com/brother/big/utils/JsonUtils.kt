package com.brother.big.utils

object JsonUtils {
    fun cleanRawString(rawString: String): String {
        return rawString.trim() // TODO - refactor | optimize
            .replace("\\","")                       // Убираем переводы строк
            .replace("\r", "")                       // Убираем возвраты каретки
            .replace("\\s+".toRegex(), " ")           // Сжимаем лишние пробелы
            .replace("(\\w+):".toRegex(), "\"$1\":")
            .replace("""\s*\n\s*""".toRegex(), "") // Убираем переходы строк и лишние пробелы
            .replace("""([a-zA-Z0-9_]+)\s*:\s*""".toRegex(), "\"$1\": ") // Обрабатываем ключи без кавычек
            .replace("""\s+""".toRegex(), " ") // Устраняем лишние пробелы
            .replace("\"evaluation\": (\\d)".toRegex(), "\"evaluation\": $1")
            .replace("""\s*\r?\n\s*""".toRegex(), "") // Убираем пробелы вокруг новой строки
            .replace("""([a-zA-Z0-9_]+)\s*:\s*""".toRegex(), "\"$1\": ") // Замена ключей без кавычек на ключи с кавычками
            .replace("""\s+""".toRegex(), " ") // Лишние пробелы между элементами
            .replace("\"evaluation\": (\\d)".toRegex(), "\"evaluation\": $1")// Числа для evaluation// Ключи без кавычек исправляем, добавляем их
    }
}