package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object Likes : Table("like") {
    val idInteresado = reference("idinteresado", Interesados, onDelete = ReferenceOption.CASCADE)
    val idObra = reference("idobra", Obras, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(idInteresado, idObra)
}
