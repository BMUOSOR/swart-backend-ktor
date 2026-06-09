package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Likes : Table("\"Like\"") {
    val idInteresado = reference("\"idInteresado\"", Interesados, onDelete = ReferenceOption.CASCADE, fkName = "fk_likes_interesado")
    val idObra       = reference("\"idObra\"", Obras, onDelete = ReferenceOption.CASCADE, fkName = "fk_likes_obra")
    val fechaLike    = datetime("\"fecha_like\"").nullable()

    override val primaryKey = PrimaryKey(idInteresado, idObra)
}
