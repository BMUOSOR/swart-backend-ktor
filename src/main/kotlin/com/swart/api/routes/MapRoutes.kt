package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.MapPinDto
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

@Serializable
data class EmptyBalizaRequest(val lat: Double, val lon: Double)

@Serializable
data class EmptyBalizaDto(val id: Long, val lat: Double, val lon: Double)

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
                        
                        // 1. Categoría directa de la exposición (establecida al crear)
                        val categoriaExpo = row[Exposiciones.categoria]?.lowercase()?.trim()

                        // 2. Tags de las obras como fallback
                        val tagsQuery = (Obras innerJoin TagObras innerJoin Tags)
                            .select { Obras.idExposicion eq idExp }
                            .map { it[Tags.nombre].lowercase() }

                        // Prioridad: categoría propia > tags de obras > "pintura" por defecto
                        val mainTag = when {
                            categoriaExpo == "escultura" || tagsQuery.any { it.contains("escultura") } -> "escultura"
                            categoriaExpo == "fotografía" || categoriaExpo == "fotografia" ||
                                tagsQuery.any { it.contains("fotografía") || it.contains("fotografia") } -> "fotografía"
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

        // Empty balizas
        get("/balizas-vacias") {
            try {
                val list = transaction {
                    BalizasVacias.selectAll().map { row ->
                        EmptyBalizaDto(
                            id  = row[BalizasVacias.id].value,
                            lat = row[BalizasVacias.lat],
                            lon = row[BalizasVacias.lon]
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/baliza-vacia") {
            try {
                val req = call.receive<EmptyBalizaRequest>()
                val newId = transaction {
                    BalizasVacias.insertAndGetId {
                        it[lat] = req.lat
                        it[lon] = req.lon
                    }.value
                }
                call.respond(HttpStatusCode.Created, EmptyBalizaDto(id = newId, lat = req.lat, lon = req.lon))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        delete("/baliza-vacia/{id}") {
            try {
                val balizaId = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                transaction { BalizasVacias.deleteWhere { BalizasVacias.id eq balizaId } }
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/reverse-geocode") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull()
            if (lat == null || lon == null) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lat/lon requeridos"))
            }
            val result = com.swart.api.services.GeocodingService.reverseGeocode(lat, lon)
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No se pudo geolocalizar"))
            } else {
                call.respond(HttpStatusCode.OK, result)
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
