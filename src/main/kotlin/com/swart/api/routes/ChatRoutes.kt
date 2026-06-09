package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Route.chatRoutes() {
    route("/api/chats") {
        
        // POST /api/chats/start -> Inicia un chat o recupera uno existente y manda el mensaje inicial
        post("/start") {
            val req = try {
                call.receive<StartChatRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            val result = transaction {
                // Buscar si ya existe
                val existing = Conversaciones.select {
                    ((Conversaciones.idUsuario1 eq req.senderId) and (Conversaciones.idUsuario2 eq req.receiverId)) or
                    ((Conversaciones.idUsuario1 eq req.receiverId) and (Conversaciones.idUsuario2 eq req.senderId))
                }.firstOrNull()

                val chatId = if (existing != null) {
                    existing[Conversaciones.id].value
                } else {
                    Conversaciones.insertAndGetId {
                        it[idUsuario1] = req.senderId
                        it[idUsuario2] = req.receiverId
                        it[fechaUltimoMensaje] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    }.value
                }

                // Insertar mensaje inicial solo si se proporcionó
                if (!req.initialMessage.isNullOrBlank()) {
                    val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    Mensajes.insertAndGetId {
                        it[idConversacion] = chatId
                        it[idSender] = req.senderId
                        it[contenido] = req.initialMessage
                        it[urlImagenObra] = req.urlImagenObra
                        it[fechaCreacion] = nowStr
                    }
                    Conversaciones.update({ Conversaciones.id eq chatId }) {
                        it[fechaUltimoMensaje] = nowStr
                    }
                }

                mapOf("idConversacion" to chatId)
            }

            call.respond(HttpStatusCode.Created, result)
        }

        // GET /api/chats/user/{userId} -> Lista de conversaciones del usuario
        get("/user/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "ID de usuario inválido")

            val conversations = transaction {
                Conversaciones.select {
                    (Conversaciones.idUsuario1 eq userId) or (Conversaciones.idUsuario2 eq userId)
                }.orderBy(Conversaciones.fechaUltimoMensaje to SortOrder.DESC).mapNotNull { row ->
                    val chatId = row[Conversaciones.id].value
                    val u1 = row[Conversaciones.idUsuario1].value
                    val u2 = row[Conversaciones.idUsuario2].value
                    
                    val otherUserId = if (u1 == userId) u2 else u1
                    
                    val otherUserRow = Usuarios.select { Usuarios.id eq otherUserId }.singleOrNull()
                        ?: return@mapNotNull null
                    
                    val otherNombre = (otherUserRow[Usuarios.nombre] + " " + (otherUserRow[Usuarios.apellidos] ?: "")).trim()
                    val otherAvatar = otherUserRow[Usuarios.imgUrl]
                    
                    // Buscar último mensaje
                    val lastMsgRow = Mensajes.select { Mensajes.idConversacion eq chatId }
                        .orderBy(Mensajes.id to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()
                    
                    val lastMsgText = lastMsgRow?.get(Mensajes.contenido) ?: ""
                    val lastMsgDate = lastMsgRow?.get(Mensajes.fechaCreacion) ?: row[Conversaciones.fechaUltimoMensaje]
                    
                    ConversationDto(
                        idConversacion = chatId,
                        otherUserId = otherUserId,
                        otherUserNombre = otherNombre,
                        otherUserAvatar = otherAvatar,
                        ultimoMensaje = lastMsgText,
                        fechaUltimoMensaje = lastMsgDate
                    )
                }
            }

            call.respond(conversations)
        }

        // GET /api/chats/{chatId}/messages -> Mensajes de una conversación
        get("/{chatId}/messages") {
            val chatId = call.parameters["chatId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "ID de chat inválido")

            val messages = transaction {
                Mensajes.select { Mensajes.idConversacion eq chatId }
                    .orderBy(Mensajes.id to SortOrder.ASC)
                    .map { row ->
                        MessageDto(
                            idMensaje = row[Mensajes.id].value,
                            idSender = row[Mensajes.idSender].value,
                            contenido = row[Mensajes.contenido],
                            urlImagenObra = row[Mensajes.urlImagenObra],
                            fechaCreacion = row[Mensajes.fechaCreacion]
                        )
                    }
            }

            call.respond(messages)
        }

        // POST /api/chats/{chatId}/messages -> Mandar un mensaje en un chat
        post("/{chatId}/messages") {
            val chatId = call.parameters["chatId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "ID de chat inválido")

            val req = try {
                call.receive<SendMessageRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val msgId = transaction {
                val newId = Mensajes.insertAndGetId {
                    it[idConversacion] = chatId
                    it[idSender] = req.senderId
                    it[contenido] = req.contenido
                    it[urlImagenObra] = null
                    it[fechaCreacion] = nowStr
                }.value

                Conversaciones.update({ Conversaciones.id eq chatId }) {
                    it[fechaUltimoMensaje] = nowStr
                }
                newId
            }

            call.respond(HttpStatusCode.Created, mapOf("idMensaje" to msgId, "fechaCreacion" to nowStr))
        }
    }
}
