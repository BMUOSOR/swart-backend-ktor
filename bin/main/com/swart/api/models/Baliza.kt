package com.swart.api.models

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Balizas : IdTable<Long>("baliza") {
    override val id = reference("idexposicion", Exposiciones, onDelete = ReferenceOption.CASCADE)
    val lat = double("lat")
    val lon = double("lon")
}
