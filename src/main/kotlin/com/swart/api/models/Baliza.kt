package com.swart.api.models

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Balizas : IdTable<Long>("\"Baliza\"") {
    override val id = reference("\"idExposicion\"", Exposiciones, onDelete = ReferenceOption.CASCADE, fkName = "fk_balizas_exposicion")
    val lat = double("\"lat\"")
    val lon = double("\"lon\"")
}
