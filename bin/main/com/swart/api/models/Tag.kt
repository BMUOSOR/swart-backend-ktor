package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Tags : IntIdTable("tag", "idTag") {
    val nombre = varchar("nombre", 50)
    val descrip = text("descrip").nullable()
}
