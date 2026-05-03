package com.swart.api.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            call.respondText("Register user endpoint")
        }
        post("/login") {
            call.respondText("Login endpoint")
        }
    }
    
    route("/api/users") {
        put("/{id}/preferences") {
            call.respondText("Update preferences endpoint")
        }
    }
}
