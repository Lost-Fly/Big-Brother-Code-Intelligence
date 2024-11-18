package com.brother.big.utils


import org.slf4j.Logger
import org.slf4j.LoggerFactory

object BigLogger {
    private const val LOGGER_NAME = "BigBrotherAI"
    private val logger: Logger = LoggerFactory.getLogger(LOGGER_NAME)

    fun logInfo(message: String) {
        logger.info(message)
    }

    fun logWarn(message: String) {
        logger.warn(message)
    }

    fun logError(message: String) {
        logger.error(message)
    }

    fun logDebug(message: String) {
        logger.debug(message)
    }
}