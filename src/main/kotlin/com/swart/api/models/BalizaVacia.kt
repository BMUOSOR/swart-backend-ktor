package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object BalizasVacias : LongIdTable("\"BalizaVacia\"") {
    val lat           = double("\"lat\"")
    val lon           = double("\"lon\"")
    val idPropietario = reference("\"idPropietario\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_balizavacias_propietario")
    // Campos de detalle del espacio
    val titulo        = text("\"titulo\"").nullable()
    val descripcion   = text("\"descripcion\"").nullable()
    val categorias    = text("\"categorias\"").nullable()     // CSV: "Pintura,Escultura"
    val dimensiones   = text("\"dimensiones\"").nullable()   // texto libre: "200m²"
    val plantas       = text("\"plantas\"").nullable()       // texto libre: "3"
    val salas         = text("\"salas\"").nullable()          // JSON: [{"nombre":"Sala A","superficie":"80m²"}]
    val fotos         = text("\"fotos\"").nullable()           // JSON: ["url1","url2"]
    val activa        = bool("\"activa\"").default(true)       // false cuando su propuesta fue aceptada
}
