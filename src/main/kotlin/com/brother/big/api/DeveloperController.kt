package com.brother.big.api

import com.brother.big.model.Developer
import com.brother.big.service.DeveloperService
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class DeveloperController(private val developerService: DeveloperService) {

    fun registerRoutes(route: Route) {
        route.route("/get_developer_analyse") {
            post {
                try {
                    val developer = call.receive<Developer>()

                    println("Received developer: $developer") // TODO - use slf4j logger

                    val result = developerService.evaluateDeveloper(developer)

                    println("Result: $result") // TODO - use slf4j logger

                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = e.message))
                }
            }
        }

        route.route("/get_developers_list") {
            get {
                try {
                    val results = developerService.getAllAnalysisResults() // TODO - use slf4j logger

                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = results))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = e.message))
                }
            }
        }
    }
}