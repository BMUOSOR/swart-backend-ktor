package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable

object BalizasVacias : LongIdTable("\"BalizaVacia\"") {
    val lat  = double("\"lat\"")
    val lon  = double("\"lon\"")
}
