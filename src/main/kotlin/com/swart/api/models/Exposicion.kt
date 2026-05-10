package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Exposiciones : LongIdTable("\"Exposicion\"", "\"idExposicion\"") {
    val idArtista = reference("\"idArtista\"", Artistas, onDelete = ReferenceOption.CASCADE)
    val titulo = text("\"titulo\"")
    val descrip = text("\"descrip\"").nullable()
    val ubicacion = text("\"ubicacion\"").nullable()
    val precio = double("\"precio\"").nullable()
    val visitantes = long("\"visitantes\"").default(0L)
    val score = double("\"score\"").default(0.0)
    val activa = bool("\"activa\"")
    val imgUrl = text("\"img_url\"").nullable()
    val fechaInicio = text("\"fecha_inicio\"").nullable()
    val fechaFin = text("\"fecha_fin\"").nullable()
    val nombreLugar = text("\"nombre_lugar\"").nullable()
}
