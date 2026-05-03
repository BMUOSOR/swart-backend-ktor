package com.swart.api.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.swipeRoutes() {
    route("/api/swipes") {
        post {
            call.respondText("Swipe action endpoint")
        }
    }
    
    route("/api/users") {
        get("/{id}/matches") {
            call.respondText("User matches endpoint")
        }
    }
}
