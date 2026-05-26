package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.MapPinDto
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

fun Route.mapRoutes() {
    route("/api/map") {
        get("/balizas") {
            try {
                val pins = transaction {
                    // Query all Balizas with their corresponding Exposicion
                    val query = (Balizas innerJoin Exposiciones)
                        .select { Exposiciones.activa eq true }
                    
                    query.map { row ->
                        val idExp = row[Exposiciones.id].value
                        
                        // Subquery to find the most frequent tag for this exhibition
                        // Join Obras -> TagObras -> Tags
                        val tagsQuery = (Obras innerJoin TagObras innerJoin Tags)
                            .select { Obras.idExposicion eq idExp }
                            .map { it[Tags.nombre].lowercase() }
                        
                        // Filter for only the allowed tags and count frequencies
                        // Determine the main tag based on keywords
                        val mainTag = when {
                            tagsQuery.any { it.contains("escultura") } -> "escultura"
                            tagsQuery.any { it.contains("fotografía") } -> "fotografía"
                            else -> "pintura"
                        }
                        
                        // Mock distance and match for MVP
                        val distanceStr = "${Random.nextInt(1, 15)} km"
                        val matchPct = Random.nextInt(70, 100)
                        
                        val now = System.currentTimeMillis()
                        val dayMillis = 24 * 60 * 60 * 1000L
                        val sDate = now + Random.nextLong(-5, 5) * dayMillis
                        val eDate = sDate + Random.nextLong(2, 10) * dayMillis

                        MapPinDto(
                            idExposicion = idExp,
                            lat = row[Balizas.lat],
                            lon = row[Balizas.lon],
                            titulo = row[Exposiciones.titulo],
                            galeria = row[Exposiciones.nombreLugar] ?: "Galería Desconocida",
                            imagen = row[Exposiciones.imgUrl],
                            distancia = distanceStr,
                            match = matchPct,
                            mainTag = mainTag,
                            startDate = sDate,
                            endDate = eDate
                        )
                    }
                }
                
                call.respond(HttpStatusCode.OK, pins)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error fetching map pins: ${e.message}"))
            }
        }

        get("/verify-address") {
            val address = call.request.queryParameters["address"]
            if (address.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Dirección vacía"))
            }
            val result = com.swart.api.services.GeocodingService.verifyAddress(address)
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "La dirección no es válida o no existe"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}
