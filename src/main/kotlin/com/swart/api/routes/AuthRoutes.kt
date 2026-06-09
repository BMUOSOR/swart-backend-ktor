package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            
            try {
                var missingInfo: ErrorResponse? = null
                val response = transaction {
                    // Check if user exists
                    val existing = Usuarios.select { Usuarios.usuario eq req.usuario }.singleOrNull()
                    
                    var userId = existing?.get(Usuarios.id)
                    val existingImgUrl = existing?.get(Usuarios.imgUrl)
                    val userImgUrl = existingImgUrl ?: "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/public/Imagenes/usuario_chica_3.jpg"

                    if (existing != null) {
                        // User exists, verify password
                        if (existing[Usuarios.password] != req.password) {
                            throw IllegalArgumentException("Usuario ya en uso")
                        }
                        // Password matches, check if they already have the role
                        val isArtista = Artistas.select { Artistas.id eq userId!! }.empty().not()
                        val isInteresado = Interesados.select { Interesados.id eq userId!! }.empty().not()
                        
                        if ((req.role.lowercase() == "artista" && isArtista) || (req.role.lowercase() == "interesado" && isInteresado)) {
                            missingInfo = ErrorResponse(
                                error = "USER_EXISTS_SAME_ROLE",
                                nombre = existing[Usuarios.nombre],
                                apellidos = existing[Usuarios.apellidos]
                            )
                            return@transaction null
                        }
                        
                        // User exists but doesn't have the requested role
                        // If they haven't confirmed yet, we ask
                        if (!req.confirmAddRole) {
                            missingInfo = ErrorResponse(
                                error = "USER_EXISTS_DIFFERENT_ROLE",
                                nombre = existing[Usuarios.nombre],
                                apellidos = existing[Usuarios.apellidos]
                            )
                            return@transaction null
                        }
                        // If they confirmed, we proceed to insert into role table below
                    } else {
                        // Insert into Usuarios
                        userId = Usuarios.insertAndGetId {
                            it[usuario] = req.usuario
                            it[password] = req.password // Storing in plain text as requested for MVP
                            it[nombre] = req.nombre
                            it[apellidos] = req.apellidos
                            it[imgUrl] = userImgUrl
                        }
                    }

                    // Insert into role specific table if not already there
                    // This will only be reached if user is NEW
                    if (req.role.lowercase() == "artista") {
                        Artistas.insertAndGetId {
                            it[id] = userId!!
                        }
                    } else {
                        Interesados.insertAndGetId {
                            it[id] = userId!!
                        }
                    }

                    AuthResponse(
                        id = userId!!.value,
                        nombre = req.nombre,
                        apellidos = req.apellidos,
                        usuario = req.usuario,
                        role = req.role.lowercase(),
                        imgUrl = userImgUrl
                    )
                }
                
                if (missingInfo != null) {
                    call.respond(HttpStatusCode.Forbidden, missingInfo!!)
                } else if (response != null) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Registration failed"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Registration failed"))
            }
        }
        
        post("/login") {
            val req = call.receive<LoginRequest>()
            
            try {
                var missingInfo: ErrorResponse? = null
                val response = transaction {
                    val userRow = Usuarios.select { Usuarios.usuario eq req.usuario }.singleOrNull()
                        ?: throw IllegalArgumentException("INVALID_CREDENTIALS")

                    if (userRow[Usuarios.password] != req.password) {
                        throw IllegalArgumentException("INVALID_CREDENTIALS")
                    }
                    
                    val userId = userRow[Usuarios.id]
                    val isArtista = Artistas.select { Artistas.id eq userId }.empty().not()
                    val isInteresado = Interesados.select { Interesados.id eq userId }.empty().not()
                    
                    val requestedRole = req.role.lowercase()
                    if ((requestedRole == "artista" && !isArtista) || (requestedRole == "interesado" && !isInteresado)) {
                        missingInfo = ErrorResponse(
                            error = "ROLE_MISSING",
                            nombre = userRow[Usuarios.nombre],
                            apellidos = userRow[Usuarios.apellidos]
                        )
                        return@transaction null
                    }

                    AuthResponse(
                        id = userId.value,
                        nombre = userRow[Usuarios.nombre],
                        apellidos = userRow[Usuarios.apellidos],
                        usuario = userRow[Usuarios.usuario],
                        role = requestedRole,
                        imgUrl = userRow[Usuarios.imgUrl]
                    )
                }

                if (missingInfo != null) {
                    call.respond(HttpStatusCode.Forbidden, missingInfo!!)
                } else if (response != null) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_CREDENTIALS"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(e.message ?: "Login failed"))
            }
        }
    }
    
    route("/api/users") {
        put("/{id}/preferences") {
            call.respondText("Update preferences endpoint")
        }

        put("/{id}/avatar") {
            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            val body = call.receive<Map<String, String>>()
            val newUrl = body["imgUrl"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "imgUrl requerido"))

            transaction {
                Usuarios.update({ Usuarios.id eq userId }) {
                    it[imgUrl] = newUrl
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("imgUrl" to newUrl))
        }
    }
}
