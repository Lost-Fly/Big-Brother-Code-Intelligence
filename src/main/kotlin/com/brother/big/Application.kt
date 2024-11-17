package com.brother.big

import com.brother.big.api.DeveloperController
import com.brother.big.service.DeveloperService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*


fun main() {
    // TODO - ADD RATE LIMITER TO SERVER with configured connections and RPS number, configs in properties
    embeddedServer(Netty, port = 8869, module = Application::module).start(wait = true) // TODO - PORT move to properties
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

