package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Exposiciones : IntIdTable("exposicion", "idExposicion") {
    val idArtista = reference("idArtista", Artistas, onDelete = ReferenceOption.CASCADE)
    val titulo = varchar("titulo", 150)
    val descrip = text("descrip")
    val ubicacion = varchar("ubicacion", 255).nullable()
    val precio = double("precio").default(0.0)
    val visitantes = integer("visitantes").default(0)
    val score = double("score").default(0.0)
    val activa = bool("activa").default(true)
}
