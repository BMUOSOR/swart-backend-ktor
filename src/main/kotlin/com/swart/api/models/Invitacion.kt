package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Invitaciones : LongIdTable("\"Invitacion\"", "\"idInvitacion\"") {
    val idExposicion = reference("\"idExposicion\"", Exposiciones, onDelete = ReferenceOption.CASCADE)
    val idArtistaSender = reference("\"idArtistaSender\"", Artistas, onDelete = ReferenceOption.CASCADE)
    val idArtistaReceiver = reference("\"idArtistaReceiver\"", Artistas, onDelete = ReferenceOption.CASCADE)
    val estado = text("\"estado\"").default("pendiente") // pendiente / aceptada / rechazada
    val fechaCreacion = text("\"fechaCreacion\"").default("")
}
