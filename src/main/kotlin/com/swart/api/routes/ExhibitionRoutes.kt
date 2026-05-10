package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.ArtworkDTO
import com.swart.api.models.dto.ExhibitionFeedDTO
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.exhibitionRoutes() {
    route("/api/feed") {
        get {
            val feed = transaction {
                // Unimos Exposicion con Artista y Usuario para obtener el nombre del artista
                val query = (Exposiciones innerJoin Artistas innerJoin Usuarios).selectAll()
                
                query.map { row ->
                    val idExpoEntity = row[Exposiciones.id]
                    
                    // Buscamos las obras de esta exposición concreta
                    val obras = Obras.select { Obras.idExposicion eq idExpoEntity }.toList()
                    val obrasIds = obras.map { it[Obras.id] }
                    val artworks = obras.mapNotNull { 
                        val imgUrl = it[Obras.imgUrl]
                        if (imgUrl != null) {
                            ArtworkDTO(
                                idObra = it[Obras.id].value,
                                titulo = it[Obras.titulo] ?: "Sin Título",
                                imgUrl = imgUrl
                            )
                        } else null
                    }
                    
                    // Extraer los tags únicos de las obras de esta exposición
                    val tags = if (obrasIds.isNotEmpty()) {
                        (Tags innerJoin TagObras)
                            .select { TagObras.idObra inList obrasIds }
                            .withDistinct()
                            .map { it[Tags.nombre] }
                    } else {
                        emptyList()
                    }
                    
                    val nombre = row[Usuarios.nombre]
                    val apellidos = row[Usuarios.apellidos] ?: ""
                    val nombreCompleto = "$nombre $apellidos".trim()
                    
                    // Usamos la foto del artista si existe, sino un fallback
                    val avatarUrl = row[Usuarios.imgUrl] ?: "https://ui-avatars.com/api/?name=${nombreCompleto.replace(" ", "+")}&background=random"

                    ExhibitionFeedDTO(
                        idExposicion = idExpoEntity.value,
                        titulo = row[Exposiciones.titulo],
                        descrip = row[Exposiciones.descrip],
                        artistaNombre = nombreCompleto,
                        artistaAvatar = avatarUrl,
                        isNew = true, 
                        obrasCount = obras.size,
                        obras = artworks,
                        exposicionImgUrl = row[Exposiciones.imgUrl],
                        tags = tags,
                        fechaInicio = row[Exposiciones.fechaInicio],
                        fechaFin = row[Exposiciones.fechaFin],
                        nombreLugar = row[Exposiciones.nombreLugar],
                        ubicacion = row[Exposiciones.ubicacion],
                        precio = row[Exposiciones.precio],
                        score = row[Exposiciones.score]
                    )
                }
            }
            call.respond(feed)
        }
    }
}
