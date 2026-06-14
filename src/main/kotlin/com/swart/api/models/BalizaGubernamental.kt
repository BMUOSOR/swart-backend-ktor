package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable

object BalizasGubernamentales : LongIdTable("\"BalizaGubernamental\"") {
    val nombre    = text("\"nombre\"")
    val direccion = text("\"direccion\"")
    val telefono  = text("\"telefono\"").nullable()
    val email     = text("\"email\"").nullable()
    val lat       = double("\"lat\"")
    val lon       = double("\"lon\"")
}
