package com.swart.api.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.exhibitionRoutes() {
    route("/api/exhibitions") {
        get("/feed") {
            call.respondText("Exhibitions feed endpoint")
        }
        get("/{id}") {
            call.respondText("Exhibition details endpoint")
        }
    }
}
