package com.brother.big

import com.brother.big.api.DeveloperController
import com.brother.big.service.DeveloperService
import com.brother.big.utils.Config
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*


fun main() {
    // TODO - ADD RATE LIMITER TO SERVER with configured connections and RPS number, configs in properties
    val port = Config["server.port"]?.toInt() ?: 8869 // Default to 8869 if not found
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        jackson { }
    }

    val developerService = DeveloperService()
    val developerController = DeveloperController(developerService)

    routing {
        developerController.registerRoutes(this)
    }
}

