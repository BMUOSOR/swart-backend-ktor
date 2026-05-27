package com.swart.api.models

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.date

object Exposiciones : LongIdTable("\"Exposicion\"", "\"idExposicion\"") {
    val titulo = text("\"titulo\"")
    val descrip = text("\"descrip\"").nullable()
    val ubicacion = text("\"ubicacion\"").nullable()
    val precio = double("\"precio\"").nullable()
    val visitantes = long("\"visitantes\"").default(0L)
    val score = double("\"score\"").default(0.0)
    val activa = bool("\"activa\"")
    val imgUrl = text("\"img_url\"").nullable()
    val fechaInicio = pgDate("\"fecha_inicio\"").nullable()
    val fechaFin = pgDate("\"fecha_fin\"").nullable()
    val nombreLugar = text("\"nombre_lugar\"").nullable()
    val esColaborativa = bool("\"es_colaborativa\"").default(false)
}

object ArtistaExposiciones : org.jetbrains.exposed.sql.Table("\"ArtistaExposicion\"") {
    val idArtista = reference("\"idArtista\"", Artistas, onDelete = ReferenceOption.CASCADE, fkName = "fk_artistaexposiciones_artista")
    val idExposicion = reference("\"idExposicion\"", Exposiciones, onDelete = ReferenceOption.CASCADE, fkName = "fk_artistaexposiciones_exposicion")
    override val primaryKey = PrimaryKey(idArtista, idExposicion, name = "ArtistaExposicion_pkey")
}

class PGDateColumnType : org.jetbrains.exposed.sql.ColumnType() {
    override fun sqlType(): String = "DATE"

    override fun valueFromDB(value: Any): Any {
        return value.toString()
    }

    override fun notNullValueToDB(value: Any): Any {
        return if (value is String) java.sql.Date.valueOf(value) else value
    }

    override fun nonNullValueToString(value: Any): String {
        return "'$value'"
    }
}

fun org.jetbrains.exposed.sql.Table.pgDate(name: String): org.jetbrains.exposed.sql.Column<String> = registerColumn(name, PGDateColumnType())
