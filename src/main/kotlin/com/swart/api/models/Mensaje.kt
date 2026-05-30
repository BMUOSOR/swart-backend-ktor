package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Mensajes : LongIdTable("\"Mensaje\"", "\"idMensaje\"") {
    val idConversacion = reference("\"idConversacion\"", Conversaciones, onDelete = ReferenceOption.CASCADE)
    val idSender = reference("\"idSender\"", Usuarios, onDelete = ReferenceOption.CASCADE)
    val contenido = text("\"contenido\"")
    val urlImagenObra = text("\"urlImagenObra\"").nullable()
    val fechaCreacion = text("\"fechaCreacion\"").default("")
}
