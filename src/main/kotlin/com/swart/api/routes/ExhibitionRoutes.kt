package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.exhibitionRoutes() {
    route("/api/feed") {
        get {
            val feed = transaction {
                // Obtenemos todas las exposiciones
                val query = Exposiciones.selectAll()
                
                query.map { row ->
                    val idExpoEntity = row[Exposiciones.id]
                    
                    // Buscamos todos los artistas asociados a esta exposición
                    val artistsData = (ArtistaExposiciones innerJoin Artistas innerJoin Usuarios)
                        .select { ArtistaExposiciones.idExposicion eq idExpoEntity }
                        .map { artistRow ->
                            val aId = artistRow[ArtistaExposiciones.idArtista].value
                            val aNom = artistRow[Usuarios.nombre]
                            val aApe = artistRow[Usuarios.apellidos] ?: ""
                            val aFullNom = "$aNom $aApe".trim()
                            val aAvatar = artistRow[Usuarios.imgUrl] ?: "https://ui-avatars.com/api/?name=${aFullNom.replace(" ", "+")}&background=random"
                            ArtistFeedDTO(id = aId, nombre = aFullNom, avatarUrl = aAvatar)
                        }
                    
                    val primaryArtist = artistsData.firstOrNull() ?: ArtistFeedDTO(0L, "Artista Desconocido", "")
                    
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

                    ExhibitionFeedDTO(
                        idExposicion = idExpoEntity.value,
                        idArtista = primaryArtist.id,
                        titulo = row[Exposiciones.titulo],
                        descrip = row[Exposiciones.descrip],
                        artistaNombre = primaryArtist.nombre,
                        artistaAvatar = primaryArtist.avatarUrl,
                        artistas = artistsData,
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
