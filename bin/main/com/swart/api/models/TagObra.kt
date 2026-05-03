package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object TagObras : Table("tagobra") {
    val idTag = reference("idtag", Tags, onDelete = ReferenceOption.CASCADE)
    val idObra = reference("idobra", Obras, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(idTag, idObra)
}
