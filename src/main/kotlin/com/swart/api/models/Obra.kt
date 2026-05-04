package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Obras : LongIdTable("\"Obra\"", "\"idObra\"") {
    val idExposicion = reference("\"idExposicion\"", Exposiciones, onDelete = ReferenceOption.CASCADE)
    val titulo = text("\"titulo\"").nullable()
    val descrip = text("\"descrip\"").nullable()
    val archivo = text("\"archivo\"")
    val oculta = bool("\"oculta\"")
    val likes = long("\"likes\"").default(0L)
    val dimensiones = text("\"dimensiones\"").nullable()
    val precio = double("\"precio\"").nullable()
    val anio = long("\"anio\"").nullable()
    val score = double("\"score\"").nullable()
    val imgUrl = text("\"img_url\"").nullable()
}
