package com.swart.api.routes

import com.swart.api.models.dto.SwipeRequestDto
import com.swart.api.services.MatchService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.swipeRoutes() {
    route("/api/swipes") {
        post {
            try {
                val req = call.receive<SwipeRequestDto>()
                MatchService.recordSwipe(req.idInteresado, req.idObra, req.liked, req.matchScore)
                call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }
    
    route("/api/users") {
        get("/{id}/discover") {
            try {
                val userId = call.parameters["id"]?.toLongOrNull()
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                    return@get
                }
                val feed = MatchService.getDiscoverFeed(userId)
                call.respond(HttpStatusCode.OK, feed)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
        get("/{id}/matches") {
            call.respondText("User matches endpoint")
        }
    }
}
