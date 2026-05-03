package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable

object Usuarios : LongIdTable("usuario", "idusuario") {
    val usuario = text("usuario").uniqueIndex()
    val password = text("password")
    val nombre = text("nombre")
    val apellidos = text("apellidos").nullable()
}
