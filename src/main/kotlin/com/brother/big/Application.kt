package com.brother.big

import com.brother.big.api.DeveloperController
import com.brother.big.service.DeveloperService
import com.brother.big.utils.Config
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.seconds


fun main() {
    val port = Config["server.port"]?.toInt() ?: 8869
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson { }
    }

    install(RateLimit) {
        register(RateLimitName("get_developer_analyse")) {
            val limit = Config["rateLimiter.limitGetAnalyse"]?.toInt() ?: 2
            val refillPeriod = Config["rateLimiter.refillGetAnalyse"]?.toInt() ?: 60
            rateLimiter(limit = limit, refillPeriod = refillPeriod.seconds)
            requestKey { applicationCall ->
                applicationCall.request.origin.remoteHost
            }
        }
        register(RateLimitName("get_developers_list")) {
            val limit = Config["rateLimiter.limitGetDevs"]?.toInt() ?: 5
            val refillPeriod = Config["rateLimiter.refillGetDevs"]?.toInt() ?: 30
            rateLimiter(limit = limit, refillPeriod = refillPeriod.seconds)
            requestKey { applicationCall ->
                applicationCall.request.origin.remoteHost
            }
        }
    }

    val developerService = DeveloperService()
    val developerController = DeveloperController(developerService)

    routing {
        developerController.registerRoutes(this)
    }
}

