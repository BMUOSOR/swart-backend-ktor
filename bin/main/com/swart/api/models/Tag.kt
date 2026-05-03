package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable

object Tags : LongIdTable("tag", "idtag") {
    val nombre = text("nombre").uniqueIndex()
    val descrip = text("descrip")
}
