package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object TagObras : Table("tag_obra") {
    val idTag = reference("idTag", Tags, onDelete = ReferenceOption.CASCADE)
    val idObra = reference("idObra", Obras, onDelete = ReferenceOption.CASCADE)
    
    override val primaryKey = PrimaryKey(idTag, idObra)
}
