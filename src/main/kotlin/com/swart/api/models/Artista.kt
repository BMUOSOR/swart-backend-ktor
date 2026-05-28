package com.swart.api.models

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Artistas : IdTable<Long>("\"Artista\"") {
    override val id = reference("\"idArtista\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_artista_usuario")
    val instagram = text("\"instagram\"").nullable()
    val whatsapp = text("\"whatsapp\"").nullable()
    val correo = text("\"correo\"").nullable()
    val x = text("\"x\"").nullable()
    val bio = text("\"bio\"").nullable()
}

object Seguidores : org.jetbrains.exposed.sql.Table("\"Seguidores\"") {
    val idUsuario = reference("\"idUsuario\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_seguidores_usuario")
    val idArtista = reference("\"idArtista\"", Artistas, onDelete = ReferenceOption.CASCADE, fkName = "fk_seguidores_artista")
    override val primaryKey = PrimaryKey(idUsuario, idArtista)
}

