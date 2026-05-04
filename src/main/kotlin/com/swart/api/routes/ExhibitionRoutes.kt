package com.swart.api.routes

import com.swart.api.models.*
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
                    val images = obras.map { it[Obras.archivo] }
                    
                    val nombre = row[Usuarios.nombre]
                    val apellidos = row[Usuarios.apellidos] ?: ""
                    val nombreCompleto = "$nombre $apellidos".trim()
                    
                    // Generamos un avatar por defecto basado en las iniciales del autor
                    val avatarUrl = "https://ui-avatars.com/api/?name=${nombreCompleto.replace(" ", "+")}&background=random"

                    ExhibitionFeedDTO(
                        idExposicion = idExpoEntity.value,
                        titulo = row[Exposiciones.titulo],
                        descrip = row[Exposiciones.descrip],
                        artistaNombre = nombreCompleto,
                        artistaAvatar = avatarUrl,
                        isNew = true, // Podríamos basarnos en una fecha en el futuro
                        obrasCount = obras.size,
                        obrasImages = images
                    )
                }
            }
            call.respond(feed)
        }
    }
}
