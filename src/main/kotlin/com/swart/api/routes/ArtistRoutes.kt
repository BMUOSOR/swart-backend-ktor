package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.request.*

fun Route.artistRoutes() {
    route("/api/artists") {
        get {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
            val artists = transaction {
                val followedIds = if (userId != null) {
                    Seguidores.select { Seguidores.idUsuario eq userId }
                        .map { it[Seguidores.idArtista].value }
                        .toSet()
                } else emptySet()

                (Artistas innerJoin Usuarios).selectAll().map { row ->
                    val id = row[Artistas.id].value
                    val nombre = row[Usuarios.nombre]
                    val apellidos = row[Usuarios.apellidos] ?: ""
                    val nombreCompleto = "$nombre $apellidos".trim()
                    val avatarUrl = row[Usuarios.imgUrl]
                        ?: "https://ui-avatars.com/api/?name=${nombreCompleto.replace(" ", "+")}&background=random"

                    ArtistFollowDto(
                        id = id,
                        nombre = nombreCompleto,
                        avatarUrl = avatarUrl,
                        following = followedIds.contains(id)
                    )
                }
            }
            call.respond(artists)
        }
    }

    route("/api/artists/{id}") {
        get {
            val artistId = call.parameters["id"]?.toLongOrNull()
            if (artistId == null) {
                call.respond(HttpStatusCode.BadRequest, "ID de artista inválido")
                return@get
            }

            val artistProfile = transaction {
                val artistRow = (Artistas innerJoin Usuarios)
                    .select { Artistas.id eq artistId }
                    .singleOrNull()

                if (artistRow == null) {
                    null
                } else {
                    val nombre = artistRow[Usuarios.nombre]
                    val apellidos = artistRow[Usuarios.apellidos] ?: ""
                    val nombreCompleto = "$nombre $apellidos".trim()
                    val avatarUrl = artistRow[Usuarios.imgUrl]
                        ?: "https://ui-avatars.com/api/?name=${nombreCompleto.replace(" ", "+")}&background=random"

                    val seguidoresCount = Seguidores.select { Seguidores.idArtista eq artistId }.count().toInt()
                    val exposicionesCount = ArtistaExposiciones.select { ArtistaExposiciones.idArtista eq artistId }.count().toInt()

                    val activeExpositionsQuery = (Exposiciones innerJoin ArtistaExposiciones)
                        .select { ArtistaExposiciones.idArtista eq artistId }

                    val activeExhibitions = activeExpositionsQuery.map { row ->
                        val idExpoEntity = row[Exposiciones.id]
                        val obras = Obras.select { (Obras.idExposicion eq idExpoEntity) and (Obras.idArtista eq artistId) }.toList()
                        val obrasIds = obras.map { it[Obras.id] }

                        val artworks = obras.mapNotNull {
                            val imgUrl = it[Obras.imgUrl]
                            if (imgUrl != null) {
                                ArtworkDTO(
                                    idObra = it[Obras.id].value,
                                    idArtista = it[Obras.idArtista]?.value,
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
                            artistas = listOf(ArtistFeedDTO(artistId, nombreCompleto, avatarUrl)),
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

                    val worksForSaleQuery = Obras
                        .select { (Obras.idArtista eq artistId) and (Obras.precio greater 0.0) }

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

        // GET /api/artists/{id}/follow?userId=X  → obtener estado de seguimiento
        // POST /api/artists/{id}/follow?userId=X  → toggle seguimiento
        route("follow") {
            get {
                val artistId = call.parameters["id"]?.toLongOrNull()
                val userId = call.request.queryParameters["userId"]?.toLongOrNull()

                if (artistId == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID de artista o ID de usuario inválido")
                    return@get
                }

                val actualUserId = transaction {
                    val exists = Usuarios.select { Usuarios.id eq userId }.count() > 0
                    if (exists) userId
                    else Usuarios.selectAll().limit(1).map { it[Usuarios.id].value }.firstOrNull() ?: userId
                }

                val isFollowing = transaction {
                    Seguidores.select {
                        (Seguidores.idUsuario eq actualUserId) and (Seguidores.idArtista eq artistId)
                    }.any()
                }

                call.respond(HttpStatusCode.OK, mapOf("following" to isFollowing))
            }

            post {
                val artistId = call.parameters["id"]?.toLongOrNull()
                val userId = call.request.queryParameters["userId"]?.toLongOrNull()

                if (artistId == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID de artista o ID de usuario inválido")
                    return@post
                }

                val actualUserId = transaction {
                    val exists = Usuarios.select { Usuarios.id eq userId }.count() > 0
                    if (exists) userId
                    else Usuarios.selectAll().limit(1).map { it[Usuarios.id].value }.firstOrNull() ?: userId
                }

                if (artistId == actualUserId) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No puedes seguirte a ti mismo"))
                    return@post
                }

                val following = transaction {
                    val exists = Seguidores.select {
                        (Seguidores.idUsuario eq actualUserId) and (Seguidores.idArtista eq artistId)
                    }.any()

                    if (exists) {
                        Seguidores.deleteWhere { (idUsuario eq actualUserId) and (idArtista eq artistId) }
                        false
                    } else {
                        Seguidores.insert {
                            it[idUsuario] = actualUserId
                            it[idArtista] = artistId
                        }
                        true
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf("following" to following))
            }
        }

        // GET /api/artists/{id}/mutuals → artistas con seguimiento mutuo
        route("mutuals") {
            get {
                val artistId = call.parameters["id"]?.toLongOrNull()
                if (artistId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID de artista inválido")
                    return@get
                }

                val mutuals = transaction {
                    // Artistas que sigo (yo → ellos)
                    val iFollow = Seguidores
                        .select { Seguidores.idUsuario eq artistId }
                        .map { it[Seguidores.idArtista].value }

                    // Artistas que me siguen (ellos → yo)
                    val followMe = Seguidores
                        .select { Seguidores.idArtista eq artistId }
                        .map { it[Seguidores.idUsuario].value }

                    // Intersección: mutuos
                    val mutualIds = iFollow.intersect(followMe.toSet())

                    mutualIds.mapNotNull { mutualId ->
                        val row = (Artistas innerJoin Usuarios)
                            .select { Artistas.id eq mutualId }
                            .singleOrNull() ?: return@mapNotNull null

                        val nombre = row[Usuarios.nombre] + " " + (row[Usuarios.apellidos] ?: "")
                        MutualArtistDto(
                            id = mutualId,
                            nombre = nombre.trim(),
                            avatarUrl = row[Usuarios.imgUrl]
                        )
                    }
                }

                call.respond(mutuals)
            }
        }
    }
}
