package com.swart.api.routes

import com.swart.api.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

val ALL_NEW_TAGS = listOf(
    "Pintura", "Escultura", "Fotografía",
    // Pintura sub-tags
    "retrato", "autorretrato", "paisaje", "paisaje urbano", "naturaleza muerta", "escena histórica", "escena religiosa", "escena mitológica", "escena de género", "abstracción", "marina", "interior", "mural",
    "realismo", "naturalismo", "impresionismo", "postimpresionismo", "expresionismo", "simbolismo", "cubismo", "surrealismo", "fauvismo", "abstracto", "arte pop", "minimalismo", "contemporáneo", "barroco", "renacimiento", "neoclasicismo", "romanticismo",
    "óleo", "acrílico", "acuarela", "gouache", "temple", "fresco", "tinta", "pastel", "técnica mixta", "esmalte", "aerosol",
    "figura humana", "retrato psicológico", "paisaje natural", "flora", "fauna", "mar", "arquitectura", "vida cotidiana", "trabajo", "religión", "mito", "política", "memoria", "identidad", "cuerpo", "naturaleza", "conflicto", "muerte", "intimidad",
    // Escultura sub-tags
    "busto", "estatua", "relieve", "bajorrelieve", "alto relieve", "escultura exenta", "escultura monumental", "instalación", "ensamblaje", "escultura pública", "objeto escultórico",
    "clásico", "neoclásico", "modernismo", "conceptual", "arte povera", "orgánico", "geométrico",
    "talla", "modelado", "fundición", "soldadura", "vaciado", "impresión 3D", "talla directa",
    "monumento", "animal", "forma abstracta", "espacio", "movimiento",
    // Fotografía sub-tags
    "documental", "fotoperiodismo", "callejera", "moda", "experimental", "abstracta",
    "realista", "pictorialista", "modernista", "surreal",
    "analógica", "digital", "blanco y negro", "color", "larga exposición", "doble exposición", "cianotipia", "colodión", "fotomontaje", "intervención digital", "macro", "estudio",
    "familia", "ciudad", "migración", "tiempo", "archivo", "comunidad"
).distinct()

val SUBTAGS_PINTURA = listOf(
    "retrato", "autorretrato", "paisaje", "paisaje urbano", "naturaleza muerta", "escena histórica", "escena religiosa", "escena mitológica", "escena de género", "abstracción", "marina", "interior", "mural",
    "realismo", "naturalismo", "impresionismo", "postimpresionismo", "expresionismo", "simbolismo", "cubismo", "surrealismo", "fauvismo", "abstracto", "arte pop", "minimalismo", "contemporáneo", "barroco", "renacimiento", "neoclasicismo", "romanticismo",
    "óleo", "acrílico", "acuarela", "gouache", "temple", "fresco", "tinta", "pastel", "técnica mixta", "esmalte", "aerosol",
    "figura humana", "retrato psicológico", "paisaje natural", "flora", "fauna", "mar", "arquitectura", "vida cotidiana", "trabajo", "religión", "mito", "política", "memoria", "identidad", "cuerpo", "naturaleza", "conflicto", "muerte", "intimidad"
)

val SUBTAGS_ESCULTURA = listOf(
    "busto", "estatua", "relieve", "bajorrelieve", "alto relieve", "escultura exenta", "escultura monumental", "instalación", "ensamblaje", "escultura pública", "objeto escultórico",
    "clásico", "realismo", "barroco", "neoclásico", "modernismo", "expresionismo", "cubismo", "abstracto", "minimalismo", "conceptual", "contemporáneo", "arte povera", "orgánico", "geométrico",
    "talla", "modelado", "fundición", "ensamblaje", "soldadura", "vaciado", "impresión 3D", "talla directa", "técnica mixta",
    "figura humana", "cuerpo", "retrato", "monumento", "memoria", "mito", "religión", "naturaleza", "animal", "forma abstracta", "espacio", "movimiento", "identidad", "política"
)

val SUBTAGS_FOTOGRAFIA = listOf(
    "retrato", "autorretrato", "paisaje", "paisaje urbano", "documental", "fotoperiodismo", "naturaleza muerta", "arquitectura", "callejera", "conceptual", "moda", "experimental", "abstracta",
    "documental", "realista", "pictorialista", "modernista", "minimalista", "conceptual", "contemporáneo", "experimental", "surreal", "abstracto",
    "analógica", "digital", "blanco y negro", "color", "larga exposición", "doble exposición", "cianotipia", "colodión", "fotomontaje", "intervención digital", "macro", "estudio",
    "identidad", "cuerpo", "memoria", "familia", "ciudad", "paisaje", "arquitectura", "vida cotidiana", "trabajo", "política", "conflicto", "migración", "naturaleza", "intimidad", "tiempo", "archivo", "comunidad"
)

fun seedDatabase() {
    Mensajes.deleteAll()
    Conversaciones.deleteAll()
    Invitaciones.deleteAll()
    Likes.deleteAll()
    Feeds.deleteAll()
    Obras.deleteAll()
    Balizas.deleteAll()
    ArtistaExposiciones.deleteAll()
    Exposiciones.deleteAll()
    Tags.deleteAll()
    Artistas.deleteAll()
    Interesados.deleteAll()
    Usuarios.deleteAll()

        val baseUrl = "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/public/Imagenes/"

        val userNames = listOf(
            "Elena" to "Valle",
            "Lucía" to "Mendoza",
            "Marina" to "Soto",
            "Clara" to "Ríos",
            "Sofía" to "Luna"
        )

        // Usuarios
        val userIds = (0..4).map { i ->
            Usuarios.insertAndGetId {
                it[usuario] = "chica_${i + 1}"
                it[password] = "123456"
                it[nombre] = userNames[i].first
                it[apellidos] = userNames[i].second
                it[imgUrl] = "${baseUrl}usuario_chica_${i + 1}.jpg"
            }
        }

        // Artistas
        val artist1Id = userIds[0]
        val artist2Id = userIds[1]

        Artistas.insert {
            it[id] = artist1Id
            it[bio] = "Artista multidisciplinar obsesionada con la luz y las texturas de la naturaleza clásica y moderna."
            it[instagram] = "@elena_valle"
            it[x] = "@elena_valle_art"
            it[correo] = "elena.valle@swart.com"
        }
        Artistas.insert {
            it[id] = artist2Id
            it[bio] = "Exploradora de volúmenes y sombras. Sus obras desafían la gravedad física y la narrativa visual contemporánea."
            it[instagram] = "@lucia_mendoza"
            it[x] = "@lucia_m_art"
            it[correo] = "lucia.mendoza@swart.com"
        }

        // Interesados
        (2..4).forEach { index ->
            Interesados.insert {
                it[id] = userIds[index]
            }
        }

        // Seguidores
        Seguidores.insert {
            it[idUsuario] = userIds[2]
            it[idArtista] = artist1Id
        }
        Seguidores.insert {
            it[idUsuario] = userIds[3]
            it[idArtista] = artist1Id
        }
        Seguidores.insert {
            it[idUsuario] = userIds[4]
            it[idArtista] = artist2Id
        }

        // Insertar nuevos tags
        val tagIdByName = ALL_NEW_TAGS.associateWith { name ->
            Tags.insertAndGetId {
                it[nombre] = name
                it[descrip] = ""
            }
        }

        // Exposiciones
        val expoData = listOf(
            Triple("Ecos del Renacimiento", "Una inmersión profunda en las técnicas maestras y los colores que definieron una era de iluminación artística. Obras que respiran historia.", artist1Id),
            Triple("Trazos de lo Abstracto", "Exploración caótica y vibrante de la emoción humana. Un viaje a través de la pintura contemporánea donde el color es el verdadero protagonista.", artist1Id),
            Triple("Piedra y Alma", "Figuras esculpidas con precisión milimétrica que capturan la perfección de la forma humana, haciendo eco de la mitología antigua.", artist1Id),
            Triple("Volúmenes Rotos", "Muestra escultórica que desafía la gravedad y la percepción espacial. Estructuras de tensión que rompen los moldes tradicionales.", artist2Id),
            Triple("Luz en Plata", "Retrospectiva fotográfica documentando la esencia pura de la realidad. Instantes irrepetibles bañados en el romanticismo del blanco y negro.", artist2Id),
            Triple("Lentes del Mañana", "Composiciones vanguardistas y visiones digitales que reescriben las reglas de la narrativa visual contemporánea urbana.", artist2Id),
            Triple("Bronce Inmortal", "Colección de esculturas en bronce que exploran la resistencia y la forma en el espacio público.", artist2Id),
            Triple("Madrid Nocturno", "Capturas atmosféricas de la capital bajo las luces de neón y las sombras de la noche.", artist1Id)
        )

        val expoIds = expoData.mapIndexed { i, data ->
            val expoId = Exposiciones.insertAndGetId {
                it[titulo] = data.first
                it[descrip] = data.second
                it[imgUrl] = "${baseUrl}exhibition_${i + 1}.jpg"
                it[activa] = true
                it[score] = 4.8
                it[fechaInicio] = "2026-09-02"
                it[fechaFin] = "2026-10-02"
                it[nombreLugar] = "Museo Nacional de Arte Contemporáneo"
                it[ubicacion] = "Calle de las Artes, 45, 28014 Madrid, España"
                it[precio] = 12.0
            }

            ArtistaExposiciones.insert {
                it[idArtista] = data.third
                it[idExposicion] = expoId
            }

            if (i == 0) {
                ArtistaExposiciones.insert {
                    it[idArtista] = artist2Id
                    it[idExposicion] = expoId
                }
            }

            expoId
        }

        // Obras
        val obrasNombres = listOf(
            listOf("El Retrato del Alma", "Bodegón al Atardecer", "Luz de Otoño"),
            listOf("Caos Primordial", "Sueño Neón", "Fragmentación de la Realidad"),
            listOf("El Pensador Silencioso", "Venus Eterna", "Guerrero Caído"),
            listOf("Esfera de Vacío", "Tensión Metálica", "Estructura Espacial #4"),
            listOf("Mirada de 1920", "Sombras en la Calle", "Reflejos de París"),
            listOf("Ciudad Ciberpunk", "Perspectiva Invertida", "Contraste Urbano"),
            listOf("Gigante Dormido", "El Abrazo del Metal", "Formas en el Aire"),
            listOf("Cielo de Madrid", "Luces de Gran Vía", "Silencio Urbano")
        )

        val obrasDescripciones = listOf(
            "Obra magistral que captura la esencia de una época pasada con pinceladas finas.",
            "Una explosión de creatividad que rompe las barreras de la interpretación.",
            "Pieza imponente donde el material cobra vida propia y cuenta su historia.",
            "Desafío absoluto a los materiales modernos y al concepto de estabilidad.",
            "Captura única de un instante efímero, congelado para siempre en el tiempo.",
            "Visión futurista y cruda de la sociedad moderna a través de la lente.",
            "Escultura de gran formato que domina el espacio con su presencia.",
            "Fotografía de alta exposición que revela detalles invisibles al ojo humano."
        )

                val obrasPrefixes = listOf(
                    "obra_pintura1", "obra_pintura2", "obra_escultura1", "obra_escultura2", "obra_fotografia1", "obra_fotografia2", "obra_escultura2", "obra_fotografia1"
                )

        for (i in 0 until 8) {
            val expoId = expoIds[i]
            val prefix = obrasPrefixes[i]
            val artistId = expoData[i].third

            val (category, subTagsList) = when (prefix) {
                "obra_pintura1", "obra_pintura2" -> "Pintura" to SUBTAGS_PINTURA
                "obra_escultura1", "obra_escultura2" -> "Escultura" to SUBTAGS_ESCULTURA
                "obra_fotografia1", "obra_fotografia2" -> "Fotografía" to SUBTAGS_FOTOGRAFIA
                else -> "Pintura" to SUBTAGS_PINTURA
            }

            val obraScores = listOf(0.9, 0.55, 0.3, 0.75, 0.4, 0.85, 0.6, 0.2)
            val obraLikes  = listOf(120L, 34L, 8L, 67L, 15L, 200L, 45L, 3L)

            for (j in 1..3) {
                val obraId = Obras.insertAndGetId {
                    it[idExposicion] = expoId
                    it[idArtista] = artistId
                    it[titulo] = obrasNombres[i][j - 1]
                    it[descrip] = obrasDescripciones[i]
                    it[archivo] = ""
                    it[imgUrl] = "${baseUrl}${prefix}_${j}.jpg"
                    it[oculta] = false
                    it[precio] = if (j % 2 == 0) (j * 1400.0) else 0.0
                    it[score] = obraScores[i] * (1.0 - j * 0.1)
                    it[likes] = obraLikes[i] / j
                }

                // Asignar categoría
                tagIdByName[category]?.let { tagId ->
                    TagObras.insert {
                        it[idTag] = tagId
                        it[idObra] = obraId
                    }
                }

                // Asignar 3 sub-tags aleatorios
                val chosen = subTagsList.shuffled().take(3)
                chosen.forEach { tagNombre ->
                    if (tagNombre != category) {
                        tagIdByName[tagNombre]?.let { tagId ->
                            TagObras.insert {
                                it[idTag] = tagId
                                it[idObra] = obraId
                            }
                        }
                    }
                }
            }
        }

        // Balizas
        val madridCoords = listOf(
            40.4168 to -3.7038,
            40.4137 to -3.6921,
            40.4087 to -3.6945,
            40.4160 to -3.6949,
            40.4241 to -3.7118,
            40.4221 to -3.6924,
            40.4150 to -3.6840,
            40.4200 to -3.7030
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

        // Invitación: chica_1 (Elena) invita a chica_2 (Lucía) a colaborar en "Ecos del Renacimiento"
        Invitaciones.insert {
            it[idExposicion] = expoIds[0]
            it[idArtistaSender] = artist1Id
            it[idArtistaReceiver] = artist2Id
            it[estado] = "pendiente"
            it[fechaCreacion] = "2026-06-05 10:00:00"
        }

        // Invitación: chica_2 (Lucía) invita a chica_1 (Elena) a colaborar en "Volúmenes Rotos"
        Invitaciones.insert {
            it[idExposicion] = expoIds[3]
            it[idArtistaSender] = artist2Id
            it[idArtistaReceiver] = artist1Id
            it[estado] = "pendiente"
            it[fechaCreacion] = "2026-06-05 11:00:00"
        }

        // Conversación: chica_5 (Sofía) interesada en una obra de chica_1 (Elena)
        val chica1Id = userIds[0]
        val chica5Id = userIds[4]
        val obraImagenUrl = "${baseUrl}obra_pintura1_1.jpg"

        val mensajes = listOf(
            Triple(chica5Id, "¡Hola, Elena! He visto tu obra 'El Retrato del Alma' en la exposición 'Ecos del Renacimiento' y me ha dejado sin palabras. Es absolutamente preciosa. ¿Está disponible para adquisición?", obraImagenUrl),
            Triple(chica1Id, "¡Hola, Sofía! Muchísimas gracias, me alegra enormemente que te haya llegado de esa manera. Sí, la pieza sigue disponible. ¿Te gustaría que habláramos de los detalles?", null),
            Triple(chica5Id, "¡Me encantaría! Creo que encajaría perfectamente en el espacio que tengo en mente. ¿Podríamos quedar para verla en persona esta semana?", null),
            Triple(chica1Id, "Claro que sí. El jueves por la tarde me va perfecto. Podemos vernos directamente en el museo. ¡Estaré encantada de mostrártela!", null)
        )

        val conversacionId = Conversaciones.insertAndGetId {
            it[idUsuario1] = chica5Id
            it[idUsuario2] = chica1Id
            it[fechaUltimoMensaje] = "2026-06-05 18:00:00"
        }.value

        mensajes.forEachIndexed { i, (senderId, contenido, urlImagen) ->
            val hora = "2026-06-05 ${17 + i}:${if (i == 0) "30" else "0$i"}:00"
            Mensajes.insert {
                it[idConversacion] = conversacionId
                it[idSender] = senderId
                it[Mensajes.contenido] = contenido
                it[urlImagenObra] = urlImagen
                it[fechaCreacion] = hora
            }
        }

        Conversaciones.update({ Conversaciones.id eq conversacionId }) {
            it[fechaUltimoMensaje] = "2026-06-05 20:03:00"
        }
}

fun Route.seedRoutes() {
    get("/seed") {
        try {
            transaction {
                seedDatabase()
            }
            call.respondText("Seed completado: Textos y descripciones artísticas generadas con éxito.", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText("Error poblando DB: ${e.localizedMessage}", status = HttpStatusCode.InternalServerError)
        }
    }
}
