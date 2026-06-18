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
import com.swart.api.models.Balizas
import com.swart.api.models.Invitaciones
import com.swart.api.models.dto.CreateExhibitionRequest
import com.swart.api.models.dto.CreateArtworkRequest

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
                val artworks = obras.map {
                    val imgUrl = it[Obras.imgUrl]
                    ArtworkDTO(
                        idObra = it[Obras.id].value,
                        idArtista = it[Obras.idArtista]?.value,
                        titulo = it[Obras.titulo] ?: "Sin Título",
                        imgUrl = imgUrl ?: ""
                    )
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
                    score = row[Exposiciones.score],
                    idBalizaVacia = row[Exposiciones.idBalizaVacia]
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

    // ── Crear nueva exposición ────────────────────────────────────────────
    // POST /api/exhibitions → crea nueva exposición
    route("/api/exhibitions") {
        post {
            val req = try {
                call.receive<CreateExhibitionRequest>()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos inválidos"))
            }

            // Resolver coordenadas ANTES de la transacción (suspend call)
            val resolvedLat: Double?
            val resolvedLon: Double?
            if (req.lat != null && req.lon != null) {
                // Coordenadas directas (desde baliza vacía)
                resolvedLat = req.lat
                resolvedLon = req.lon
            } else if (!req.ubicacion.isNullOrBlank()) {
                // Geocodificar la dirección escrita
                val geoResult = runCatching { GeocodingService.verifyAddress(req.ubicacion) }.getOrNull()?.firstOrNull()
                resolvedLat = geoResult?.lat?.toDoubleOrNull()
                resolvedLon = geoResult?.lon?.toDoubleOrNull()
            } else {
                resolvedLat = null
                resolvedLon = null
            }

            val newId = transaction {
                val idExpo = Exposiciones.insertAndGetId {
                    it[titulo] = req.titulo
                    it[descrip] = req.descrip
                    it[nombreLugar] = req.nombreLugar
                    it[ubicacion] = req.ubicacion
                    it[categoria] = req.categoria
                    it[fechaInicio] = req.fechaInicio
                    it[fechaFin] = req.fechaFin
                    it[imgUrl] = req.imgUrl
                    it[precio] = req.precio
                    it[activa] = req.activa
                    it[esColaborativa] = req.esColaborativa
                    it[visitantes] = 0L
                    it[score] = 0.0
                }

                // Asociar artista creador
                ArtistaExposiciones.insert {
                    it[idArtista] = req.artistaId
                    it[idExposicion] = idExpo
                }

                // Crear entrada en Balizas para que aparezca en el mapa
                if (resolvedLat != null && resolvedLon != null) {
                    Balizas.insert {
                        it[id] = idExpo
                        it[lat] = resolvedLat
                        it[lon] = resolvedLon
                    }
                }

                // Si colaborativa, enviar invitaciones
                if (req.esColaborativa) {
                    req.artistasInvitadosIds.forEach { idInvitado ->
                        try {
                            Invitaciones.insert {
                                it[idExposicion] = idExpo
                                it[idArtistaSender] = req.artistaId
                                it[idArtistaReceiver] = idInvitado
                                it[estado] = "pendiente"
                                it[fechaCreacion] = java.time.LocalDate.now().toString()
                            }
                        } catch (e: Exception) { /* ignorar */ }
                    }
                }

                idExpo.value
            }

            call.respond(HttpStatusCode.Created, mapOf("idExposicion" to newId))
        }
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

                val artworks = obras.map {
                    val imgUrl = it[Obras.imgUrl]
                    ArtworkDTO(
                        idObra = it[Obras.id].value,
                        idArtista = it[Obras.idArtista]?.value,
                        titulo = it[Obras.titulo] ?: "Sin Título",
                        imgUrl = imgUrl ?: ""
                    )
                }

                val tags = if (obrasIds.isNotEmpty()) {
                    (Tags innerJoin TagObras)
                        .select { TagObras.idObra inList obrasIds }
                        .withDistinct()
                        .map { it[Tags.nombre] }
                } else emptyList()

                val artistas = (ArtistaExposiciones innerJoin Artistas innerJoin Usuarios)
                    .select { ArtistaExposiciones.idExposicion eq idExpoEntity }
                    .map { artistRow ->
                        val aId = artistRow[ArtistaExposiciones.idArtista].value
                        val aNom = artistRow[Usuarios.nombre]
                        val aApe = artistRow[Usuarios.apellidos] ?: ""
                        val aFullNom = "$aNom $aApe".trim()
                        val aAvatar = artistRow[Usuarios.imgUrl]
                            ?: "https://ui-avatars.com/api/?name=${aFullNom.replace(" ", "+")}&background=random"
                        ArtistFeedDTO(id = aId, nombre = aFullNom, avatarUrl = aAvatar)
                    }

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
                    tags = tags,
                    esColaborativa = expoRow[Exposiciones.esColaborativa],
                    artistas = artistas,
                    idBalizaVacia = expoRow[Exposiciones.idBalizaVacia]
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
                if (result.isEmpty()) {
                    return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La dirección no es válida o no existe"))
                }
                result.firstOrNull()
            } else null

            val updated = transaction {
                val rows = Exposiciones.update({ Exposiciones.id eq id }) {
                    it[titulo] = req.titulo
                    it[descrip] = req.descrip
                    it[nombreLugar] = req.nombreLugar
                    it[ubicacion] = req.ubicacion
                    it[fechaInicio] = req.fechaInicio
                    it[fechaFin] = req.fechaFin
                    if (req.imgUrl != null) {
                        it[imgUrl] = req.imgUrl
                    }
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

        // POST /api/exhibitions/{id}/view → incrementa el contador de visitantes
        route("{id}/view") {
            post {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                transaction {
                    Exposiciones.update({ Exposiciones.id eq id }) {
                        with(SqlExpressionBuilder) {
                            it.update(Exposiciones.visitantes, Exposiciones.visitantes + 1)
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }
        }
    }
}
