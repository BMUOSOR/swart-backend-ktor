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

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            
            try {
                val response = transaction {
                    // Check if user exists
                    val existing = Usuarios.select { Usuarios.usuario eq req.usuario }.singleOrNull()
                    if (existing != null) {
                        throw IllegalArgumentException("User already exists")
                    }

                    // Insert into Usuarios
                    val newUserId = Usuarios.insertAndGetId {
                        it[usuario] = req.usuario
                        it[password] = req.password // Storing in plain text as requested for MVP
                        it[nombre] = req.nombre
                        it[apellidos] = req.apellidos
                    }

                    // Insert into role specific table
                    if (req.role.lowercase() == "artista") {
                        Artistas.insertAndGetId {
                            it[id] = newUserId
                        }
                    } else {
                        Interesados.insertAndGetId {
                            it[id] = newUserId
                        }
                    }

                    AuthResponse(
                        id = newUserId.value,
                        nombre = req.nombre,
                        apellidos = req.apellidos,
                        usuario = req.usuario,
                        role = req.role.lowercase()
                    )
                }
                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Registration failed"))
            }
        }
        
        post("/login") {
            val req = call.receive<LoginRequest>()
            
            try {
                val response = transaction {
                    val userRow = Usuarios.select { Usuarios.usuario eq req.usuario }.singleOrNull()
                        ?: throw IllegalArgumentException("Invalid credentials")

                    if (userRow[Usuarios.password] != req.password) {
                        throw IllegalArgumentException("Invalid credentials")
                    }
                    
                    val userId = userRow[Usuarios.id]
                    
                    val isArtista = Artistas.select { Artistas.id eq userId }.empty().not()
                    val role = if (isArtista) "artista" else "interesado"

                    AuthResponse(
                        id = userId.value,
                        nombre = userRow[Usuarios.nombre],
                        apellidos = userRow[Usuarios.apellidos],
                        usuario = userRow[Usuarios.usuario],
                        role = role
                    )
                }
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(e.message ?: "Login failed"))
            }
        }
    }
    
    route("/api/users") {
        put("/{id}/preferences") {
            call.respondText("Update preferences endpoint")
        }
    }
}
