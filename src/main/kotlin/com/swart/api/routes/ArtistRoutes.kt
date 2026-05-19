package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.artistRoutes() {
    route("/api/artists/{id}") {
        get {
            val artistId = call.parameters["id"]?.toLongOrNull()
            if (artistId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID de artista inválido")
                return@get
            }

            val artistProfile = transaction {
                // 1. Obtener datos básicos del artista y usuario
                val artistRow = (Artistas innerJoin Usuarios)
                    .select { Artistas.id eq artistId }
                    .singleOrNull()

                if (artistRow == null) {
                    null
                } else {
                    val nombre = artistRow[Usuarios.nombre]
                    val apellidos = artistRow[Usuarios.apellidos] ?: ""
                    val nombreCompleto = "$nombre $apellidos".trim()
                    val avatarUrl = artistRow[Usuarios.imgUrl] ?: "https://ui-avatars.com/api/?name=${nombreCompleto.replace(" ", "+")}&background=random"

                    // 2. Conteo de seguidores y exposiciones
                    val seguidoresCount = Seguidores.select { Seguidores.idArtista eq artistId }.count().toInt()
                    val exposicionesCount = Exposiciones.select { Exposiciones.idArtista eq artistId }.count().toInt()

                    // 3. Obtener exposiciones activas
                    val activeExpositionsQuery = Exposiciones
                        .select { (Exposiciones.idArtista eq artistId) and (Exposiciones.activa eq true) }

                    val activeExhibitions = activeExpositionsQuery.map { row ->
                        val idExpoEntity = row[Exposiciones.id]
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
                            idArtista = artistId,
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

                    // 4. Obtener obras en venta (precio > 0)
                    val worksForSaleQuery = (Obras innerJoin Exposiciones)
                        .select { (Exposiciones.idArtista eq artistId) and (Obras.precio greater 0.0) }

                    val worksForSale = worksForSaleQuery.mapNotNull { row ->
                        val imgUrl = row[Obras.imgUrl]
                        if (imgUrl != null) {
                            ArtworkForSaleDTO(
                                idObra = row[Obras.id].value,
                                titulo = row[Obras.titulo] ?: "Sin Título",
                                imgUrl = imgUrl,
                                precio = row[Obras.precio] ?: 0.0
                            )
                        } else null
                    }

                    ArtistProfileDTO(
                        id = artistId,
                        nombre = nombre,
                        apellidos = artistRow[Usuarios.apellidos],
                        avatarUrl = avatarUrl,
                        bio = artistRow[Artistas.bio],
                        seguidores = seguidoresCount,
                        exposicionesCount = exposicionesCount,
                        instagram = artistRow[Artistas.instagram],
                        twitter = artistRow[Artistas.x],
                        correo = artistRow[Artistas.correo],
                        activeExhibitions = activeExhibitions,
                        worksForSale = worksForSale
                    )
                }
            }

            if (artistProfile == null) {
                call.respond(HttpStatusCode.NotFound, "Artista no encontrado")
            } else {
                call.respond(artistProfile)
            }
        }
    }

    route("/api/artists/{id}/follow") {
        post {
            val artistId = call.parameters["id"]?.toLongOrNull()
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()

            if (artistId == null || userId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID de artista o ID de usuario inválido")
                return@post
            }

            val following = transaction {
                val exists = Seguidores
                    .select { (Seguidores.idUsuario eq userId) and (Seguidores.idArtista eq artistId) }
                    .any()

                if (exists) {
                    Seguidores.deleteWhere { (idUsuario eq userId) and (idArtista eq artistId) }
                    false
                } else {
                    Seguidores.insert {
                        it[idUsuario] = userId
                        it[idArtista] = artistId
                    }
                    true
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("following" to following))
        }
    }
}
