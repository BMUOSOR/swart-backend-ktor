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

                // Nombres y descripciones realistas
                val userNames = listOf(
                    "Elena" to "Valle",
                    "Lucía" to "Mendoza",
                    "Marina" to "Soto",
                    "Clara" to "Ríos",
                    "Sofía" to "Luna"
                )

                // 1. Usuarios (5 chicas)
                val userIds = (0..4).map { i ->
                    Usuarios.insertAndGetId {
                        it[usuario] = "chica_${i + 1}"
                        it[password] = "123456"
                        it[nombre] = userNames[i].first
                        it[apellidos] = userNames[i].second
                        it[imgUrl] = "${baseUrl}usuario_chica_${i + 1}.jpg"
                    }
                }

                // 2. Artistas (las 2 primeras)
                val artist1Id = userIds[0]
                val artist2Id = userIds[1]
                
                Artistas.insert {
                    it[this.id] = artist1Id
                    it[bio] = "Artista multidisciplinar obsesionada con la luz y las texturas de la naturaleza clásica y moderna."
                }
                Artistas.insert {
                    it[this.id] = artist2Id
                    it[bio] = "Exploradora de volúmenes y sombras. Sus obras desafían la gravedad física y la narrativa visual contemporánea."
                }

                // 3. Interesados (las 3 restantes)
                (2..4).forEach { index ->
                    Interesados.insert {
                        it[id] = userIds[index]
                    }
                }

                // 4. Tags
                val tagsData = listOf(
                    "Pintura Clásica" to "Pintura tradicional y técnica maestra.",
                    "Pintura Moderna" to "Exploración vibrante y abstracta.",
                    "Escultura Clásica" to "Perfección de la forma en mármol y bronce.",
                    "Escultura Moderna" to "Experimentación geométrica y espacial.",
                    "Fotografía Clásica" to "Instantes eternos capturados en plata y monocromo.",
                    "Fotografía Moderna" to "Visiones digitales y perspectivas inusuales."
                )
                val tagIds = tagsData.map { (nombreTag, desc) ->
                    Tags.insertAndGetId {
                        it[nombre] = nombreTag
                        it[descrip] = desc
                    }
                }

                // Datos enriquecidos para Exposiciones
                val expoData = listOf(
                    Triple("Ecos del Renacimiento", "Una inmersión profunda en las técnicas maestras y los colores que definieron una era de iluminación artística. Obras que respiran historia.", artist1Id),
                    Triple("Trazos de lo Abstracto", "Exploración caótica y vibrante de la emoción humana. Un viaje a través de la pintura contemporánea donde el color es el verdadero protagonista.", artist1Id),
                    Triple("Piedra y Alma", "Figuras esculpidas con precisión milimétrica que capturan la perfección de la forma humana, haciendo eco de la mitología antigua.", artist1Id),
                    Triple("Volúmenes Rotos", "Muestra escultórica que desafía la gravedad y la percepción espacial. Estructuras de tensión que rompen los moldes tradicionales.", artist2Id),
                    Triple("Luz en Plata", "Retrospectiva fotográfica documentando la esencia pura de la realidad. Instantes irrepetibles bañados en el romanticismo del blanco y negro.", artist2Id),
                    Triple("Lentes del Mañana", "Composiciones vanguardistas y visiones digitales que reescriben las reglas de la narrativa visual contemporánea urbana.", artist2Id)
                )

                // 5. Exposiciones (6 exposiciones, 3 para cada artista)
                val expoIds = expoData.mapIndexed { i, data ->
                    Exposiciones.insertAndGetId {
                        it[idArtista] = data.third
                        it[titulo] = data.first
                        it[descrip] = data.second
                        it[imgUrl] = "${baseUrl}exhibition_${i + 1}.jpg"
                        it[activa] = true
                        it[score] = 4.8
                        it[fechaInicio] = "02/09/2026"
                        it[fechaFin] = "02/10/2026"
                        it[nombreLugar] = "Museo Nacional de Arte Contemporáneo"
                        it[ubicacion] = "Calle de las Artes, 45, 28014 Madrid, España"
                        it[precio] = 12.0
                    }
                }

                // Nombres enriquecidos para las obras
                val obrasNombres = listOf(
                    listOf("El Retrato del Alma", "Bodegón al Atardecer", "Luz de Otoño"),
                    listOf("Caos Primordial", "Sueño Neón", "Fragmentación de la Realidad"),
                    listOf("El Pensador Silencioso", "Venus Eterna", "Guerrero Caído"),
                    listOf("Esfera de Vacío", "Tensión Metálica", "Estructura Espacial #4"),
                    listOf("Mirada de 1920", "Sombras en la Calle", "Reflejos de París"),
                    listOf("Ciudad Ciberpunk", "Perspectiva Invertida", "Contraste Urbano")
                )
                
                val obrasDescripciones = listOf(
                    "Obra magistral que captura la esencia de una época pasada con pinceladas finas.",
                    "Una explosión de creatividad que rompe las barreras de la interpretación.",
                    "Pieza imponente donde el material cobra vida propia y cuenta su historia.",
                    "Desafío absoluto a los materiales modernos y al concepto de estabilidad.",
                    "Captura única de un instante efímero, congelado para siempre en el tiempo.",
                    "Visión futurista y cruda de la sociedad moderna a través de la lente."
                )

                val obrasPrefixes = listOf(
                    "obra_pintura1", "obra_pintura2", "obra_escultura1", "obra_escultura2", "obra_fotografia1", "obra_fotografia2"
                )

                // 6. Obras (18 en total, 3 por exposición)
                for (i in 0 until 6) {
                    val expoId = expoIds[i]
                    val tagId = tagIds[i]
                    val prefix = obrasPrefixes[i]

                    for (j in 1..3) {
                        val obraId = Obras.insertAndGetId {
                            it[idExposicion] = expoId
                            it[titulo] = obrasNombres[i][j - 1]
                            it[descrip] = obrasDescripciones[i] // Misma descripción base por estilo
                            it[archivo] = ""
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

                // 7. Balizas (demo en Madrid)
                val madridCoords = listOf(
                    40.4168 to -3.7038, // Puerta del Sol
                    40.4137 to -3.6921, // Museo del Prado
                    40.4087 to -3.6945, // Reina Sofia
                    40.4160 to -3.6949, // Thyssen
                    40.4241 to -3.7118, // Plaza de España
                    40.4221 to -3.6924  // Biblioteca Nacional
                )

                expoIds.forEachIndexed { i, expoId ->
                    if (i < madridCoords.size) {
                        Balizas.insert {
                            it[id] = expoId
                            it[lat] = madridCoords[i].first
                            it[lon] = madridCoords[i].second
                        }
                    }
                }
            }
            call.respondText("Seed completado: Textos y descripciones artísticas generadas con éxito.", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText("Error poblando DB: ${e.localizedMessage}", status = HttpStatusCode.InternalServerError)
        }
    }
}
