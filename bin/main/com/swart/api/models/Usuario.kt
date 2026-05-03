package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Usuarios : IntIdTable("usuario", "idUsuario") {
    val usuario = varchar("usuario", 50).uniqueIndex()
    val password = varchar("password", 255)
    val nombre = varchar("nombre", 100)
    val apellidos = varchar("apellidos", 150)
}
