package com.swart.api.models

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Artistas : IdTable<Long>("artista") {
    override val id = reference("idartista", Usuarios, onDelete = ReferenceOption.CASCADE)
    val instagram = text("instagram").nullable()
    val whatsapp = text("whatsapp").nullable()
    val correo = text("correo").nullable()
    val x = text("x").nullable()
    val bio = text("bio").nullable()
}
