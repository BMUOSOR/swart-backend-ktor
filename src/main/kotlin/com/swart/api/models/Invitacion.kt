package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Invitaciones : LongIdTable("\"Invitacion\"", "\"idInvitacion\"") {
    val idExposicion = reference("\"idExposicion\"", Exposiciones, onDelete = ReferenceOption.CASCADE, fkName = "fk_invitaciones_exposicion")
    val idArtistaSender = reference("\"idArtistaSender\"", Artistas, onDelete = ReferenceOption.CASCADE, fkName = "fk_invitaciones_artista_sender")
    val idArtistaReceiver = reference("\"idArtistaReceiver\"", Artistas, onDelete = ReferenceOption.CASCADE, fkName = "fk_invitaciones_artista_receiver")
    val estado = text("\"estado\"").default("pendiente") // pendiente / aceptada / rechazada
    val fechaCreacion = text("\"fechaCreacion\"").default("")
}
