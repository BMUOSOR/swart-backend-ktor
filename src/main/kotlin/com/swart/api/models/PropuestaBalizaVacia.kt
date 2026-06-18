package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object PropuestasBalizaVacia : LongIdTable("\"PropuestaBalizaVacia\"") {
    val idBaliza      = reference("\"idBaliza\"",  BalizasVacias, onDelete = ReferenceOption.CASCADE, fkName = "fk_propuesta_baliza")
    val idArtista     = reference("\"idArtista\"", Artistas,      onDelete = ReferenceOption.CASCADE, fkName = "fk_propuesta_artista")
    val titulo        = text("\"titulo\"")
    val descrip       = text("\"descrip\"").nullable()
    val fechaInicio   = text("\"fechaInicio\"").nullable()
    val fechaFin      = text("\"fechaFin\"").nullable()
    val precio        = double("\"precio\"").nullable()
    val categoria     = text("\"categoria\"").nullable()
    val estado        = text("\"estado\"").default("pendiente") // pendiente / aceptada / rechazada
    val fechaCreacion = text("\"fechaCreacion\"").default("")
    val archivoPdf    = text("\"archivoPdf\"").nullable()        // URL del PDF adjunto en Supabase Storage
}
