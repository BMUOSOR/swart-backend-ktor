package com.swart.api.plugins

import com.swart.api.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to SWART API")
        }
        authRoutes()
        exhibitionRoutes()
        swipeRoutes()
        seedRoutes()
        curationRoutes()
        mapRoutes()
    }
}
