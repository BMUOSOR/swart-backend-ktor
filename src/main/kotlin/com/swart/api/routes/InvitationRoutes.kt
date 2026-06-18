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

fun Route.invitationRoutes() {

    // GET /api/invitations/{userId} → invitaciones pendientes del artista
    route("/api/invitations/{userId}") {
        get {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val invitations = transaction {
                Invitaciones
                    .select { (Invitaciones.idArtistaReceiver eq userId) and (Invitaciones.estado eq "pendiente") }
                    .mapNotNull { row ->
                        val idExpo = row[Invitaciones.idExposicion].value
                        val expoRow = Exposiciones.select { Exposiciones.id eq idExpo }.singleOrNull()
                            ?: return@mapNotNull null

                        val senderId = row[Invitaciones.idArtistaSender].value
                        val senderRow = (Artistas innerJoin Usuarios)
                            .select { Artistas.id eq senderId }
                            .singleOrNull() ?: return@mapNotNull null

                        val senderNombre = senderRow[Usuarios.nombre] + " " + (senderRow[Usuarios.apellidos] ?: "")
                        val senderAvatar = senderRow[Usuarios.imgUrl]

                        InvitationDto(
                            idInvitacion = row[Invitaciones.id].value,
                            idExposicion = idExpo,
                            tituloExposicion = expoRow[Exposiciones.titulo],
                            exposicionImgUrl = expoRow[Exposiciones.imgUrl],
                            idArtistaSender = senderId,
                            nombreArtistaSender = senderNombre.trim(),
                            avatarArtistaSender = senderAvatar,
                            estado = row[Invitaciones.estado]
                        )
                    }
            }
            val propuestas = transaction {
                PropuestasBalizaVacia
                    .innerJoin(BalizasVacias, onColumn = { PropuestasBalizaVacia.idBaliza }, otherColumn = { BalizasVacias.id })
                    .select { (BalizasVacias.idPropietario eq userId) and (PropuestasBalizaVacia.estado eq "pendiente") }
                    .mapNotNull { row ->
                        val senderId = row[PropuestasBalizaVacia.idArtista].value
                        val senderRow = (Artistas innerJoin Usuarios).select { Artistas.id eq senderId }.singleOrNull() ?: return@mapNotNull null
                        val senderNombre = senderRow[Usuarios.nombre] + " " + (senderRow[Usuarios.apellidos] ?: "")
                        val senderAvatar = senderRow[Usuarios.imgUrl]

                        InvitationDto(
                            idInvitacion = row[PropuestasBalizaVacia.id].value,
                            idExposicion = row[PropuestasBalizaVacia.idBaliza].value,
                            tituloExposicion = row[PropuestasBalizaVacia.titulo],
                            exposicionImgUrl = row[PropuestasBalizaVacia.categoria],
                            idArtistaSender = senderId,
                            nombreArtistaSender = senderNombre.trim(),
                            avatarArtistaSender = senderAvatar,
                            estado = row[PropuestasBalizaVacia.estado],
                            tipo = "propuesta",
                            descrip = row[PropuestasBalizaVacia.descrip],
                            fechaInicio = row[PropuestasBalizaVacia.fechaInicio],
                            fechaFin = row[PropuestasBalizaVacia.fechaFin],
                            precio = row[PropuestasBalizaVacia.precio],
                            categoria = row[PropuestasBalizaVacia.categoria],
                            idUsuarioSender = senderRow[Usuarios.id].value,
                            nombreEspacio = row[BalizasVacias.titulo],
                            archivoPdf = row[PropuestasBalizaVacia.archivoPdf]
                        )
                    }
            }

            call.respond(invitations + propuestas)
        }
    }

    // POST /api/invitations → crear una invitación
    route("/api/invitations") {
        post {
            val req = try {
                call.receive<CreateInvitationRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            val id = transaction {
                Invitaciones.insertAndGetId {
                    it[idExposicion] = req.idExposicion
                    it[idArtistaSender] = req.idArtistaSender
                    it[idArtistaReceiver] = req.idArtistaReceiver
                    it[estado] = "pendiente"
                    it[fechaCreacion] = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                }.value
            }

            call.respond(HttpStatusCode.Created, mapOf("idInvitacion" to id))
        }
    }

    // PUT /api/invitations/{id}/respond → aceptar o rechazar
    route("/api/invitations/{id}/respond") {
        put {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val req = try {
                call.receive<RespondInvitationRequest>()
            } catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            val updated = transaction {
                val invRow = Invitaciones.select { Invitaciones.id eq id }.singleOrNull()
                    ?: return@transaction false

                val nuevoEstado = if (req.aceptar) "aceptada" else "rechazada"
                Invitaciones.update({ Invitaciones.id eq id }) {
                    it[estado] = nuevoEstado
                }

                // Si acepta, añadir al artista en ArtistaExposiciones
                if (req.aceptar) {
                    val idExpo = invRow[Invitaciones.idExposicion].value
                    val idArtista = invRow[Invitaciones.idArtistaReceiver].value
                    val alreadyExists = ArtistaExposiciones
                        .select { (ArtistaExposiciones.idArtista eq idArtista) and (ArtistaExposiciones.idExposicion eq idExpo) }
                        .any()
                    if (!alreadyExists) {
                        ArtistaExposiciones.insert {
                            it[ArtistaExposiciones.idArtista] = idArtista
                            it[ArtistaExposiciones.idExposicion] = idExpo
                        }
                    }
                }

                true
            }

            call.respond(mapOf("success" to updated))
        }
    }

    // GET /api/invitations/{userId}/unread-count → conteo de propuestas pendientes (badge)
    route("/api/invitations/{userId}/unread-count") {
        get {
            val userId = call.parameters["userId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val count = transaction {
                val invCount = Invitaciones
                    .select { (Invitaciones.idArtistaReceiver eq userId) and (Invitaciones.estado eq "pendiente") }
                    .count()

                val propuestaCount = PropuestasBalizaVacia
                    .innerJoin(BalizasVacias, onColumn = { PropuestasBalizaVacia.idBaliza }, otherColumn = { BalizasVacias.id })
                    .select { (BalizasVacias.idPropietario eq userId) and (PropuestasBalizaVacia.estado eq "pendiente") }
                    .count()

                invCount + propuestaCount
            }
            call.respond(mapOf("count" to count))
        }
    }
}
