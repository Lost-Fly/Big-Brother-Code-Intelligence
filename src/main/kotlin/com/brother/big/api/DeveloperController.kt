package com.brother.big.api

import com.brother.big.api.errors.BuzinaErrors.OPS_ERROR
import com.brother.big.model.Developer
import com.brother.big.service.DeveloperService
import com.brother.big.utils.BigLogger.logInfo
import com.brother.big.utils.BigLogger.logWarn
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class DeveloperController(private val developerService: DeveloperService) {

    private fun logException(call: RoutingCall, e: Exception) =
        logWarn("Error occured while request processing: " +
                "(url - ${call.request.uri}, query params - ${call.request.rawQueryParameters}, " +
                "origin - ${call.request.origin}) ERROR: ${e.message}")

    fun registerRoutes(route: Route) {
        route.route("/v1/get_developer_analyse") {
            post {
                try {
                    val developer = call.receive<Developer>()

                    logInfo("Received developer: $developer")

                    val result = developerService.evaluateDeveloper(developer)

                    logInfo("Result of /v1/get_developer_analyse request: (url - ${call.request.uri}, " +
                        "query params - ${call.request.rawQueryParameters}, origin - ${call.request.origin}) processing: $result")

                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result))
                } catch (e: Exception) {
                    logException(call, e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = OPS_ERROR))
                }
            }
        }

        route.route("/v1/get_developers_list") {
            get {
                try {
                    val results = developerService.getAllAnalysisResults()

                    logInfo("Result of /v1/get_developers_list request (url - ${call.request.uri}, " +
                        "query params - ${call.request.rawQueryParameters}, origin - ${call.request.origin}) processing: $results")

                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = results))
                } catch (e: Exception) {
                    logException(call, e)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse(success = false, message = OPS_ERROR))
                }
            }
        }
    }
}