package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Feeds : Table("\"Feed\"") {
    val idInteresado = reference(
        name = "\"idInteresado\"", 
        foreign = Interesados, 
        onDelete = ReferenceOption.CASCADE, 
        fkName = "fk_feed_interesado"
    )
    val idObra = reference(
        name = "\"idObra\"", 
        foreign = Obras, 
        onDelete = ReferenceOption.CASCADE, 
        fkName = "fk_feed_obra"
    )
    val fechaInteraccion = datetime("\"fechaInteraccion\"").nullable()
    val matchScore = double("\"matchScore\"").default(0.0)
    
    override val primaryKey = PrimaryKey(idInteresado, idObra)
}
