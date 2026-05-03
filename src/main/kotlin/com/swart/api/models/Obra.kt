package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Obras : IntIdTable("Obra", "idObra") {
    val idExposicion = reference("idExposicion", Exposiciones, onDelete = ReferenceOption.CASCADE)
    val titulo = varchar("titulo", 150)
    val descrip = text("descrip")
    val dimensiones = varchar("dimensiones", 100).nullable()
    val tecnica = varchar("tecnica", 100).nullable()
    val anio = integer("anio").nullable()
    val precio = double("precio").nullable()
    val archivo = varchar("archivo", 500)
    val oculta = bool("oculta").default(false)
    val likes = integer("likes").default(0)
    val score = double("score").default(0.0)
}
