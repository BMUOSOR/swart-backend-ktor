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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import com.swart.api.services.GeocodingService

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

    // ── Detalle / Edición / Eliminación de exposición ─────────────────────
    route("/api/exhibitions/{id}") {

        // GET → devuelve los datos completos de una exposición para editar
        get {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val detail = transaction {
                val expoRow = Exposiciones.select { Exposiciones.id eq id }.singleOrNull()
                    ?: return@transaction null

                val idExpoEntity = expoRow[Exposiciones.id]
                val obras = Obras.select { Obras.idExposicion eq idExpoEntity }.toList()
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
                    idExposicion = idExpoEntity.value,
                    titulo = expoRow[Exposiciones.titulo],
                    descrip = expoRow[Exposiciones.descrip],
                    nombreLugar = expoRow[Exposiciones.nombreLugar],
                    ubicacion = expoRow[Exposiciones.ubicacion],
                    fechaInicio = expoRow[Exposiciones.fechaInicio],
                    fechaFin = expoRow[Exposiciones.fechaFin],
                    imgUrl = expoRow[Exposiciones.imgUrl],
                    precio = expoRow[Exposiciones.precio],
                    score = expoRow[Exposiciones.score],
                    activa = expoRow[Exposiciones.activa],
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
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val req = try {
                call.receive<UpdateExhibitionRequest>()
            } catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            // Verificar dirección antes de la transacción si se provee una ubicación
            val coordinates = if (!req.ubicacion.isNullOrBlank()) {
                val result = GeocodingService.verifyAddress(req.ubicacion)
                if (result == null) {
                    return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La dirección no es válida o no existe"))
                }
                result
            } else null

            val updated = transaction {
                val rows = Exposiciones.update({ Exposiciones.id eq id }) {
                    it[titulo] = req.titulo
                    it[descrip] = req.descrip
                    it[nombreLugar] = req.nombreLugar
                    it[ubicacion] = req.ubicacion
                    it[fechaInicio] = req.fechaInicio
                    it[fechaFin] = req.fechaFin
                }

                if (rows > 0) {
                    // Actualizar coordenadas en la tabla Balizas si se resolvió la dirección
                    if (coordinates != null) {
                        val latVal = coordinates.lat.toDoubleOrNull() ?: 0.0
                        val lonVal = coordinates.lon.toDoubleOrNull() ?: 0.0
                        val exists = Balizas.select { Balizas.id eq id }.count() > 0
                        if (exists) {
                            Balizas.update({ Balizas.id eq id }) {
                                it[lat] = latVal
                                it[lon] = lonVal
                            }
                        } else {
                            Balizas.insert {
                                it[this.id] = id
                                it[lat] = latVal
                                it[lon] = lonVal
                            }
                        }
                    }

                    val expoRow = Exposiciones.select { Exposiciones.id eq id }.singleOrNull()
                    if (expoRow != null) {
                        val idExpoEntity = expoRow[Exposiciones.id]
                        val obrasRows = Obras.select { Obras.idExposicion eq idExpoEntity }.toList()
                        val categoryTagIds = Tags.select { Tags.nombre inList setOf("Pintura", "Escultura", "Fotografía") }
                            .map { it[Tags.id] }

                        obrasRows.forEach { obraRow ->
                            val obraId = obraRow[Obras.id]
                            if (categoryTagIds.isNotEmpty()) {
                                TagObras.deleteWhere { (idObra eq obraId) and (idTag inList categoryTagIds) }
                            }

                            req.tags.forEach { tagNombre ->
                                val tagId = Tags.select { Tags.nombre eq tagNombre }
                                    .firstOrNull()?.get(Tags.id)
                                    ?: Tags.insertAndGetId {
                                        it[nombre] = tagNombre
                                        it[descrip] = ""
                                    }
                                try {
                                    TagObras.insert {
                                        it[TagObras.idTag] = tagId
                                        it[TagObras.idObra] = obraId
                                    }
                                } catch (e: Exception) { /* ignorar duplicados */ }
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
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val deleted = transaction {
                val expoRow = Exposiciones.select { Exposiciones.id eq id }.singleOrNull()
                    ?: return@transaction false

                val idExpoEntity = expoRow[Exposiciones.id]

                // 1. Obtener obras y eliminar sus tags obra por obra
                val obrasRows = Obras.select { Obras.idExposicion eq idExpoEntity }.toList()
                obrasRows.forEach { obraRow ->
                    val obraId = obraRow[Obras.id]
                    TagObras.deleteWhere { idObra eq obraId }
                }

                // 2. Eliminar obras
                Obras.deleteWhere { idExposicion eq idExpoEntity }

                // 3. Eliminar relación artista-exposición
                ArtistaExposiciones.deleteWhere { idExposicion eq idExpoEntity }

                // 4. Eliminar la exposición
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
