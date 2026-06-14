package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object BalizasVacias : LongIdTable("\"BalizaVacia\"") {
    val lat           = double("\"lat\"")
    val lon           = double("\"lon\"")
    val idPropietario = reference("\"idPropietario\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_balizavacias_propietario")
}
