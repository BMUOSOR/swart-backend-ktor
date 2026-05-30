package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Conversaciones : LongIdTable("\"Conversacion\"", "\"idConversacion\"") {
    val idUsuario1 = reference("\"idUsuario1\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_conversacion_usuario1")
    val idUsuario2 = reference("\"idUsuario2\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_conversacion_usuario2")
    val fechaUltimoMensaje = text("\"fechaUltimoMensaje\"").default("")
}
