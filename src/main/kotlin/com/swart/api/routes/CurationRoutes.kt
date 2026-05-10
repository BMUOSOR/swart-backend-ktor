package com.swart.api.routes

import com.swart.api.services.CuratorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CurationRequest(
    val imageUrl: String,
    val disciplina: String
)

fun Route.curationRoutes() {
    route("/api/curation") {
        post("/classify") {
            try {
                val req = call.receive<CurationRequest>()
                val resultJson = CuratorService.classifyArtwork(req.imageUrl, req.disciplina)
                call.respondText(resultJson, ContentType.Application.Json)
            } catch (e: Exception) {
                call.respondText("{\"error\": \"Error procesando la solicitud: ${e.message}\"}", ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }
    }
}
