package com.swart.api.routes

import com.swart.api.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.seedRoutes() {
    get("/seed") {
        try {
            transaction {
                // Limpiar base de datos
                TagObras.deleteAll()
                Likes.deleteAll()
                Feeds.deleteAll()
                Obras.deleteAll()
                Balizas.deleteAll()
                Exposiciones.deleteAll()
                Tags.deleteAll()
                Artistas.deleteAll()
                Interesados.deleteAll()
                Usuarios.deleteAll()

                val baseUrl = "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/public/Imagenes/"

                // 1. Usuarios (5 chicas)
                val userIds = (1..5).map { i ->
                    Usuarios.insertAndGetId {
                        it[usuario] = "chica_$i"
                        it[password] = "123456"
                        it[nombre] = "Artista/Usuaria"
                        it[apellidos] = "$i"
                        it[imgUrl] = "${baseUrl}usuario_chica_$i.jpg"
                    }
                }

                // 2. Artistas (las 2 primeras)
                val artist1Id = userIds[0]
                val artist2Id = userIds[1]
                
                listOf(artist1Id, artist2Id).forEach { id ->
                    Artistas.insert {
                        it[this.id] = id
                        it[bio] = "Artista creativa"
                    }
                }

                // 3. Interesados (las 3 restantes)
                (2..4).forEach { index ->
                    Interesados.insert {
                        it[id] = userIds[index]
                    }
                }

                // 4. Tags
                val tagsData = listOf(
                    "Pintura Clásica" to "Pintura tradicional",
                    "Pintura Moderna" to "Pintura contemporánea",
                    "Escultura Clásica" to "Escultura en mármol y bronce",
                    "Escultura Moderna" to "Escultura abstracta",
                    "Fotografía Clásica" to "Fotografía analógica",
                    "Fotografía Moderna" to "Fotografía digital"
                )
                val tagIds = tagsData.map { (nombreTag, desc) ->
                    Tags.insertAndGetId {
                        it[nombre] = nombreTag
                        it[descrip] = desc
                    }
                }

                // 5. Exposiciones (6 exposiciones, 3 para cada artista)
                val expoIds = (1..6).map { i ->
                    Exposiciones.insertAndGetId {
                        it[idArtista] = if (i <= 3) artist1Id else artist2Id
                        it[titulo] = "Exposición $i"
                        it[descrip] = "Muestra de arte de la exposición $i"
                        it[imgUrl] = "${baseUrl}exhibition_$i.jpg"
                        it[activa] = true
                        it[score] = 4.5
                    }
                }

                // 6. Obras (18 en total, 3 por exposición)
                val obrasPrefixes = listOf(
                    "obra_pintura1",     // Para Expo 1 (Tag: Pintura Clásica)
                    "obra_pintura2",     // Para Expo 2 (Tag: Pintura Moderna)
                    "obra_escultura1",   // Para Expo 3 (Tag: Escultura Clásica)
                    "obra_escultura2",   // Para Expo 4 (Tag: Escultura Moderna)
                    "obra_fotografia1",  // Para Expo 5 (Tag: Fotografía Clásica)
                    "obra_fotografia2"   // Para Expo 6 (Tag: Fotografía Moderna)
                )

                for (i in 0 until 6) {
                    val expoId = expoIds[i]
                    val tagId = tagIds[i]
                    val prefix = obrasPrefixes[i]

                    for (j in 1..3) {
                        val obraId = Obras.insertAndGetId {
                            it[idExposicion] = expoId
                            it[titulo] = "Obra $prefix $j"
                            it[archivo] = "" // Dejamos archivo vacío o ponemos lo mismo, ya que usamos imgUrl
                            it[imgUrl] = "${baseUrl}${prefix}_${j}.jpg"
                            it[oculta] = false
                        }

                        // Asignamos el tag a la obra
                        TagObras.insert {
                            it[idTag] = tagId
                            it[idObra] = obraId
                        }
                    }
                }

            }
            call.respondText("Seed completado: 5 usuarias, 6 tags, 6 exposiciones, 18 obras.", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText("Error poblando DB: ${e.localizedMessage}", status = HttpStatusCode.InternalServerError)
        }
    }
}
