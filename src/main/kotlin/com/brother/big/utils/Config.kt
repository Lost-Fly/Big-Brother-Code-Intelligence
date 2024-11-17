package com.brother.big.utils

import java.io.InputStream
import java.util.*

object Config {
    private val properties = Properties()

    init {
        val inputStream: InputStream = this::class.java.classLoader.getResourceAsStream("config.properties")
            ?: throw IllegalArgumentException("Config not found")
        inputStream.use { stream ->
            properties.load(stream)
        }
    }

    operator fun get(key: String): String? = properties.getProperty(key)
}