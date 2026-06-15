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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// ─── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class EmptyBalizaRequest(val lat: Double, val lon: Double, val idPropietario: Long)

@Serializable
data class EmptyBalizaDto(val id: Long, val lat: Double, val lon: Double, val idPropietario: Long)

@Serializable
data class GovBalizaDto(
    val id: Long,
    val nombre: String,
    val direccion: String,
    val telefono: String?,
    val email: String?,
    val lat: Double,
    val lon: Double
)

@Serializable
data class PropuestaRequest(
    val idArtista: Long,
    val titulo: String,
    val descrip: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val precio: Double? = null,
    val categoria: String? = null
)

@Serializable
data class PropuestaDto(
    val id: Long,
    val idBaliza: Long,
    val idArtista: Long,
    val titulo: String,
    val descrip: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val precio: Double?,
    val categoria: String?,
    val estado: String,
    val fechaCreacion: String
)

@Serializable
data class PropuestaEstadoRequest(val estado: String) // "aceptada" | "rechazada"

// ─── Routes ───────────────────────────────────────────────────────────────────

fun Route.mapRoutes() {
    route("/api/map") {

        // ── Balizas de exposiciones ──────────────────────────────────────────
        get("/balizas") {
            try {
                val pins = transaction {
                    val query = (Balizas innerJoin Exposiciones)
                        .selectAll().where { Exposiciones.activa eq true }

                    query.map { row ->
                        val idExp = row[Exposiciones.id].value

                        val categoriaExpo = row[Exposiciones.categoria]?.lowercase()?.trim()

                        val tagsQuery = (Obras innerJoin TagObras innerJoin Tags)
                            .selectAll().where { Obras.idExposicion eq idExp }
                            .map { it[Tags.nombre].lowercase() }

                        val mainTag = when {
                            categoriaExpo == "escultura" || tagsQuery.any { it.contains("escultura") } -> "escultura"
                            categoriaExpo == "fotografía" || categoriaExpo == "fotografia" ||
                                tagsQuery.any { it.contains("fotografía") || it.contains("fotografia") } -> "fotografía"
                            else -> "pintura"
                        }

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

        // ── Balizas vacías ───────────────────────────────────────────────────
        get("/balizas-vacias") {
            try {
                val list = transaction {
                    BalizasVacias.selectAll().map { row ->
                        EmptyBalizaDto(
                            id             = row[BalizasVacias.id].value,
                            lat            = row[BalizasVacias.lat],
                            lon            = row[BalizasVacias.lon],
                            idPropietario  = row[BalizasVacias.idPropietario].value
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
                println("[DEBUG] POST /baliza-vacia -> lat=${req.lat}, lon=${req.lon}, idPropietario=${req.idPropietario}")

                val newId = transaction {
                    // Determinar propietario válido: verificar que existe en la tabla Usuario
                    val propietarioFinal: Long = run {
                        // Primero intentar con el ID recibido si es positivo
                        if (req.idPropietario > 0) {
                            val exists = com.swart.api.models.Usuarios
                                .select { com.swart.api.models.Usuarios.id eq req.idPropietario }
                                .count() > 0
                            if (exists) return@run req.idPropietario
                            println("[DEBUG] Usuario ${req.idPropietario} no encontrado en BD, usando fallback")
                        }
                        // Fallback: buscar el primer usuario disponible
                        val firstUser = com.swart.api.models.Usuarios.selectAll()
                            .firstOrNull()?.get(com.swart.api.models.Usuarios.id)?.value
                        if (firstUser != null) {
                            println("[DEBUG] Usando primer usuario disponible: $firstUser")
                            return@run firstUser
                        }
                        // Último recurso: crear un usuario dummy
                        val dummyId = com.swart.api.models.Usuarios.insertAndGetId {
                            it[usuario] = "dummy_${System.currentTimeMillis()}"
                            it[password] = "dummy"
                            it[nombre] = "Usuario Demo"
                        }.value
                        println("[DEBUG] Creado usuario dummy con id=$dummyId")
                        dummyId
                    }

                    BalizasVacias.insertAndGetId {
                        it[lat]           = req.lat
                        it[lon]           = req.lon
                        it[idPropietario] = propietarioFinal
                    }.value
                }
                println("[DEBUG] Baliza vacía creada con id=$newId")
                call.respond(HttpStatusCode.Created, EmptyBalizaDto(id = newId, lat = req.lat, lon = req.lon, idPropietario = req.idPropietario))
            } catch (e: Exception) {
                e.printStackTrace()
                println("[ERROR] POST /baliza-vacia: ${e::class.simpleName}: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error desconocido")))
            }
        }

        delete("/baliza-vacia/{id}") {
            try {
                val balizaId = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                transaction {
                    // Cancel pending proposals and notify (set estado = "cancelada")
                    PropuestasBalizaVacia.update({ PropuestasBalizaVacia.idBaliza eq balizaId and (PropuestasBalizaVacia.estado eq "pendiente") }) {
                        it[estado] = "cancelada"
                    }
                    BalizasVacias.deleteWhere { BalizasVacias.id eq balizaId }
                }
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ── Propuestas de baliza vacía ────────────────────────────────────────
        post("/baliza-vacia/{id}/propuesta") {
            try {
                val balizaId = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val req = call.receive<PropuestaRequest>()
                val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val newId = transaction {
                    PropuestasBalizaVacia.insertAndGetId {
                        it[idBaliza]      = balizaId
                        it[idArtista]     = req.idArtista
                        it[titulo]        = req.titulo
                        it[descrip]       = req.descrip
                        it[fechaInicio]   = req.fechaInicio
                        it[fechaFin]      = req.fechaFin
                        it[precio]        = req.precio
                        it[categoria]     = req.categoria
                        it[estado]        = "pendiente"
                        it[fechaCreacion] = now
                    }.value
                }
                call.respond(HttpStatusCode.Created, mapOf("idPropuesta" to newId))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Propuestas recibidas por el propietario de una baliza vacía
        get("/baliza-vacia/{id}/propuestas") {
            try {
                val balizaId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val list = transaction {
                    PropuestasBalizaVacia.selectAll().where { PropuestasBalizaVacia.idBaliza eq balizaId }
                        .map { row ->
                            PropuestaDto(
                                id            = row[PropuestasBalizaVacia.id].value,
                                idBaliza      = row[PropuestasBalizaVacia.idBaliza].value,
                                idArtista     = row[PropuestasBalizaVacia.idArtista].value,
                                titulo        = row[PropuestasBalizaVacia.titulo],
                                descrip       = row[PropuestasBalizaVacia.descrip],
                                fechaInicio   = row[PropuestasBalizaVacia.fechaInicio],
                                fechaFin      = row[PropuestasBalizaVacia.fechaFin],
                                precio        = row[PropuestasBalizaVacia.precio],
                                categoria     = row[PropuestasBalizaVacia.categoria],
                                estado        = row[PropuestasBalizaVacia.estado],
                                fechaCreacion = row[PropuestasBalizaVacia.fechaCreacion]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Propuestas enviadas por un artista (para ver su estado)
        get("/propuestas/artista/{idArtista}") {
            try {
                val artistaId = call.parameters["idArtista"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val list = transaction {
                    PropuestasBalizaVacia.selectAll().where { PropuestasBalizaVacia.idArtista eq artistaId }
                        .map { row ->
                            PropuestaDto(
                                id            = row[PropuestasBalizaVacia.id].value,
                                idBaliza      = row[PropuestasBalizaVacia.idBaliza].value,
                                idArtista     = row[PropuestasBalizaVacia.idArtista].value,
                                titulo        = row[PropuestasBalizaVacia.titulo],
                                descrip       = row[PropuestasBalizaVacia.descrip],
                                fechaInicio   = row[PropuestasBalizaVacia.fechaInicio],
                                fechaFin      = row[PropuestasBalizaVacia.fechaFin],
                                precio        = row[PropuestasBalizaVacia.precio],
                                categoria     = row[PropuestasBalizaVacia.categoria],
                                estado        = row[PropuestasBalizaVacia.estado],
                                fechaCreacion = row[PropuestasBalizaVacia.fechaCreacion]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Aceptar o rechazar propuesta
        put("/propuesta/{id}/estado") {
            try {
                val propuestaId = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val req = call.receive<PropuestaEstadoRequest>()
                if (req.estado !in listOf("aceptada", "rechazada")) {
                    return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado inválido"))
                }
                transaction {
                    PropuestasBalizaVacia.update({ PropuestasBalizaVacia.id eq propuestaId }) {
                        it[estado] = req.estado
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ── Balizas gubernamentales ───────────────────────────────────────────
        get("/balizas-gubernamentales") {
            try {
                val list = transaction {
                    BalizasGubernamentales.selectAll().map { row ->
                        GovBalizaDto(
                            id        = row[BalizasGubernamentales.id].value,
                            nombre    = row[BalizasGubernamentales.nombre],
                            direccion = row[BalizasGubernamentales.direccion],
                            telefono  = row[BalizasGubernamentales.telefono],
                            email     = row[BalizasGubernamentales.email],
                            lat       = row[BalizasGubernamentales.lat],
                            lon       = row[BalizasGubernamentales.lon]
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // ── Geocoding ─────────────────────────────────────────────────────────
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
            if (result.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "La dirección no es válida o no existe"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}
