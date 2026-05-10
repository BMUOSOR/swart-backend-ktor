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
                        val validTags = setOf("pintura", "escultura", "fotografía")
                        val frequencies = tagsQuery
                            .filter { it in validTags }
                            .groupingBy { it }
                            .eachCount()
                        
                        // Get the most frequent tag, default to "pintura"
                        val mainTag = frequencies.maxByOrNull { it.value }?.key ?: "pintura"
                        
                        // Mock distance and match for MVP
                        val distanceStr = "${Random.nextInt(1, 15)} km"
                        val matchPct = Random.nextInt(70, 100)
                        
                        MapPinDto(
                            idExposicion = idExp,
                            lat = row[Balizas.lat],
                            lon = row[Balizas.lon],
                            titulo = row[Exposiciones.titulo],
                            galeria = row[Exposiciones.nombreLugar] ?: "Galería Desconocida",
                            imagen = row[Exposiciones.imgUrl],
                            distancia = distanceStr,
                            match = matchPct,
                            mainTag = mainTag
                        )
                    }
                }
                
                call.respond(HttpStatusCode.OK, pins)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error fetching map pins: ${e.message}"))
            }
        }
    }
}
