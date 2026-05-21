package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.exhibitionRoutes() {
    suspend fun io.ktor.util.pipeline.PipelineContext<Unit, ApplicationCall>.handleFeed() {
        val feed = transaction {
            val query = Exposiciones.selectAll()

            query.map { row ->
                val idExpoEntity = row[Exposiciones.id]

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

    route("/api/feed") {
        get { handleFeed() }
    }
    route("/api/feed/") {
        get { handleFeed() }
    }

    // ── Detalle / Edición / Eliminación de exposición ──────────────────────
    route("/api/exhibitions/{id}") {

        // GET → devuelve los datos completos de una exposición para editar
        get {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val detail = transaction {
                val expoRow = Exposiciones.select { Exposiciones.id eq id }.singleOrNull()
                    ?: return@transaction null

                val obras = Obras.select { Obras.idExposicion eq Exposiciones.id.wrap(id) }.toList()
                val obrasIds = obras.map { it[Obras.id] }

                val artworks = obras.mapNotNull {
                    val imgUrl = it[Obras.imgUrl] ?: return@mapNotNull null
                    ArtworkDTO(
                        idObra = it[Obras.id].value,
                        titulo = it[Obras.titulo] ?: "Sin Título",
                        imgUrl = imgUrl
                    )
                }

                val tags = if (obrasIds.isNotEmpty()) {
                    (Tags innerJoin TagObras)
                        .select { TagObras.idObra inList obrasIds }
                        .withDistinct()
                        .map { it[Tags.nombre] }
                } else emptyList()

                ExhibitionDetailDTO(
                    idExposicion = expoRow[Exposiciones.id].value,
                    titulo = expoRow[Exposiciones.titulo],
                    descrip = expoRow[Exposiciones.descrip],
                    nombreLugar = expoRow[Exposiciones.nombreLugar],
                    ubicacion = expoRow[Exposiciones.ubicacion],
                    fechaInicio = expoRow[Exposiciones.fechaInicio],
                    fechaFin = expoRow[Exposiciones.fechaFin],
                    imgUrl = expoRow[Exposiciones.imgUrl],
                    precio = expoRow[Exposiciones.precio],
                    score = expoRow[Exposiciones.score],
                    activa = expoRow[Exposiciones.activa] ?: true,
                    obras = artworks,
                    tags = tags
                )
            }

            if (detail == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exposición no encontrada"))
            } else {
                call.respond(detail)
            }
        }

        // PUT → actualiza los datos de la exposición
        put {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val req = try {
                call.receive<UpdateExhibitionRequest>()
            } catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            val updated = transaction {
                val rows = Exposiciones.update({ Exposiciones.id eq id }) {
                    it[titulo] = req.titulo
                    it[descrip] = req.descrip
                    it[nombreLugar] = req.nombreLugar
                    it[ubicacion] = req.ubicacion
                    it[fechaInicio] = req.fechaInicio
                    it[fechaFin] = req.fechaFin
                }

                if (rows > 0 && req.tags.isNotEmpty()) {
                    // Actualizar tags: eliminar los viejos y añadir los nuevos
                    val obrasIds = Obras.select { Obras.idExposicion eq Exposiciones.id.wrap(id) }
                        .map { it[Obras.id] }

                    if (obrasIds.isNotEmpty()) {
                        TagObras.deleteWhere { TagObras.idObra inList obrasIds }

                        req.tags.forEach { tagNombre ->
                            val tagId = Tags.select { Tags.nombre eq tagNombre }
                                .firstOrNull()?.get(Tags.id)
                                ?: Tags.insertAndGetId {
                                    it[nombre] = tagNombre
                                    it[descrip] = null
                                }

                            obrasIds.forEach { obraId ->
                                TagObras.insert {
                                    it[TagObras.idTag] = tagId
                                    it[TagObras.idObra] = obraId
                                }
                            }
                        }
                    }
                }
                rows > 0
            }

            if (updated) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exposición no encontrada"))
            }
        }

        // DELETE → elimina la exposición y sus relaciones
        delete {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val deleted = transaction {
                val expoIdWrapped = Exposiciones.id.wrap(id)

                // 1. Obtener obras asociadas
                val obrasIds = Obras.select { Obras.idExposicion eq expoIdWrapped }.map { it[Obras.id] }

                // 2. Eliminar tags de obras
                if (obrasIds.isNotEmpty()) {
                    TagObras.deleteWhere { TagObras.idObra inList obrasIds }
                }

                // 3. Eliminar obras
                Obras.deleteWhere { Obras.idExposicion eq expoIdWrapped }

                // 4. Eliminar relación artista-exposición
                ArtistaExposiciones.deleteWhere { ArtistaExposiciones.idExposicion eq expoIdWrapped }

                // 5. Eliminar la exposición
                Exposiciones.deleteWhere { Exposiciones.id eq id } > 0
            }

            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Exposición no encontrada"))
            }
        }
    }
}
