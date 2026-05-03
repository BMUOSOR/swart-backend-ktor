package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Artistas : IntIdTable("artista", "idArtista") {
    val idUsuario = reference("idUsuario", Usuarios, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val instagram = varchar("instagram", 100).nullable()
    val whatsapp = varchar("whatsapp", 20).nullable()
    val correo = varchar("correo", 100).nullable()
    val x = varchar("x", 100).nullable()
    val bio = text("bio").nullable()
}
