package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object Balizas : Table("baliza") {
    val idExposicion = reference("idExposicion", Exposiciones, onDelete = ReferenceOption.CASCADE)
    val lat = double("lat")
    val lon = double("lon")
    
    override val primaryKey = PrimaryKey(idExposicion)
}
