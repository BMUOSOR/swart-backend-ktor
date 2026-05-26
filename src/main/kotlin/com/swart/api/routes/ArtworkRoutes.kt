package com.swart.api.routes

import com.swart.api.models.*
import com.swart.api.models.dto.ArtworkDetailDTO
import com.swart.api.models.dto.UpdateArtworkRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.artworkRoutes() {
    route("/api/artworks/{id}") {
        
        // GET → Obtiene el detalle de la obra y sus tags asociados
        get {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de obra inválido"))

            val detail = transaction {
                val artworkRow = Obras.select { Obras.id eq id }.singleOrNull() ?: return@transaction null
                val parentExId = artworkRow[Obras.idExposicion]

                // Buscar todos los tags asignados a esta obra
                val allTags = (Tags innerJoin TagObras)
                    .select { TagObras.idObra eq id }
                    .map { it[Tags.nombre] }

                val categoriesList = setOf("Pintura", "Escultura", "Fotografía")
                val categories = allTags.filter { it in categoriesList }
                val subTags = allTags.filter { it !in categoriesList }

                // Obtener las categorías de la exposición padre para saber qué sub-tags mostrar
                val exhibitionCategories = if (categories.isEmpty()) {
                    val siblingObraIds = Obras.select { Obras.idExposicion eq parentExId }.map { it[Obras.id] }
                    if (siblingObraIds.isNotEmpty()) {
                        (Tags innerJoin TagObras)
                            .select { (TagObras.idObra inList siblingObraIds) and (Tags.nombre inList categoriesList) }
                            .withDistinct()
                            .map { it[Tags.nombre] }
                    } else emptyList()
                } else categories

                val precioVal = artworkRow[Obras.precio]
                val disponible = precioVal != null && precioVal > 0.0

                ArtworkDetailDTO(
                    idObra = id,
                    titulo = artworkRow[Obras.titulo] ?: "Sin Título",
                    descrip = artworkRow[Obras.descrip],
                    imgUrl = artworkRow[Obras.imgUrl],
                    precio = precioVal,
                    disponibleCompra = disponible,
                    tags = subTags,
                    categoriasExposicion = if (exhibitionCategories.isEmpty()) listOf("Pintura") else exhibitionCategories
                )
            }

            if (detail == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Obra no encontrada"))
            } else {
                call.respond(detail)
            }
        }

        // PUT → Actualiza los datos de la obra
        put {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de obra inválido"))

            val req = try {
                call.receive<UpdateArtworkRequest>()
            } catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Datos de obra inválidos"))
            }

            val updated = transaction {
                val rows = Obras.update({ Obras.id eq id }) {
                    it[titulo] = req.titulo
                    it[descrip] = req.descrip
                    it[precio] = if (req.disponibleCompra) (req.precio ?: 0.0) else 0.0
                }

                if (rows > 0) {
                    val categoriesList = setOf("Pintura", "Escultura", "Fotografía")
                    val categoryTagIds = Tags.select { Tags.nombre inList categoriesList }.map { it[Tags.id] }

                    // Eliminar únicamente los sub-tags, preservando los tags de categoría de la obra
                    if (categoryTagIds.isNotEmpty()) {
                        TagObras.deleteWhere { (idObra eq id) and (idTag notInList categoryTagIds) }
                    } else {
                        TagObras.deleteWhere { idObra eq id }
                    }

                    // Insertar los nuevos sub-tags seleccionados
                    req.tags.forEach { tagNombre ->
                        if (tagNombre !in categoriesList) {
                            val tagId = Tags.select { Tags.nombre eq tagNombre }
                                .firstOrNull()?.get(Tags.id)
                                ?: Tags.insertAndGetId {
                                    it[nombre] = tagNombre
                                    it[descrip] = ""
                                }
                            try {
                                TagObras.insert {
                                    it[TagObras.idTag] = tagId
                                    it[TagObras.idObra] = id
                                }
                            } catch (e: Exception) { /* ignorar duplicados */ }
                        }
                    }
                }
                rows > 0
            }

            call.respond(mapOf("success" to updated))
        }

        // DELETE → Borra la obra de la base de datos
        delete {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de obra inválido"))

            val deleted = transaction {
                Obras.deleteWhere { Obras.id eq id } > 0
            }

            call.respond(mapOf("success" to deleted))
        }
    }
}
