package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object Feeds : Table("feed") {
    val idInteresado = reference("idInteresado", Interesados, onDelete = ReferenceOption.CASCADE)
    val idObra = reference("idObra", Obras, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(idInteresado, idObra)
}
