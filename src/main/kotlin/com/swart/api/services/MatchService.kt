package com.swart.api.services

import com.swart.api.models.*
import com.swart.api.models.dto.DiscoverArtworkDto
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object MatchService {

    /**
     * Ensures the user has a record in the Interesados table.
     * This is required because Feeds, Likes, and InteresadoTagPreferences
     * all have foreign keys referencing Interesados.
     * If the user is only registered as Artista (or has no role sub-table entry),
     * swipe recording silently fails due to FK violations.
     */
    private fun org.jetbrains.exposed.sql.Transaction.ensureInteresadoExists(userId: Long) {
        val exists = Interesados.select { Interesados.id eq userId }.empty().not()
        if (!exists) {
            // Verify user exists in Usuarios first
            val userExists = Usuarios.select { Usuarios.id eq userId }.empty().not()
            if (userExists) {
                exec("INSERT INTO \"Interesado\" (\"idInteresado\") VALUES ($userId)")
                println("MatchService: Created Interesado record for userId=$userId")
            } else {
                println("MatchService: WARNING - userId=$userId does not exist in Usuarios!")
            }
        }
    }

    fun getDiscoverFeed(userId: Long): List<DiscoverArtworkDto> = transaction {
        // 0. Ensure the user has an Interesado record so FK queries work
        ensureInteresadoExists(userId)

        // 1. Obtener los tags preferidos del usuario y sus pesos
        val userPrefs = InteresadoTagPreferences
            .select { InteresadoTagPreferences.idInteresado eq userId }
            .associate { it[InteresadoTagPreferences.idTag].value to it[InteresadoTagPreferences.peso] }

        val totalUserLikes = Likes.select { Likes.idInteresado eq userId }.count()
        val maxUserWeight = userPrefs.values.maxOrNull() ?: 0.0

        println("MatchService: getDiscoverFeed userId=$userId, prefs=${userPrefs.size} tags, totalLikes=$totalUserLikes, maxWeight=$maxUserWeight")

        // 2. Obtener obras ya vistas por el usuario
        val seenArtworkIds = Feeds
            .select { Feeds.idInteresado eq userId }
            .map { it[Feeds.idObra].value }
            .toSet()

        // Garantía adicional: nunca mostrar obras que el usuario ya haya dado like,
        // aunque por algún motivo no tengan entrada en Feeds
        val likedArtworkIds = Likes
            .select { Likes.idInteresado eq userId }
            .map { it[Likes.idObra].value }
            .toSet()

        val excludedIds = seenArtworkIds + likedArtworkIds

        println("MatchService: seenArtworkIds=${seenArtworkIds.size}, likedIds=${likedArtworkIds.size}, totalExcluded=${excludedIds.size}")

        // 3. Obtener obras no vistas ni gustadas (join con Exposicion, Artista, y pre-cargar tags)
        val maxGlobalLikes = Obras.selectAll().maxOfOrNull { it[Obras.likes] }?.coerceAtLeast(1L) ?: 1L

        val availableArtworksQuery = if (excludedIds.isEmpty()) {
            (Obras innerJoin Exposiciones).selectAll()
        } else {
            (Obras innerJoin Exposiciones).select { Obras.id notInList excludedIds }
        }

        val availableArtworks = availableArtworksQuery.map { row ->
                val obraId = row[Obras.id].value
                val expoId = row[Exposiciones.id].value
                val tagsOfObra = TagObras
                    .select { TagObras.idObra eq obraId }
                    .map { it[TagObras.idTag].value }

                // Obtener Artista de la exposicion
                val artistRow = (ArtistaExposiciones innerJoin Artistas innerJoin Usuarios)
                    .select { ArtistaExposiciones.idExposicion eq expoId }
                    .firstOrNull()
                
                val artistName = artistRow?.let { "${it[Usuarios.nombre]} ${it[Usuarios.apellidos] ?: ""}".trim() } ?: "Unknown"
                val artistAvatar = artistRow?.get(Usuarios.imgUrl) ?: ""

                ArtworkData(
                    idObra = obraId,
                    titulo = row[Obras.titulo] ?: "Sin título",
                    imgUrl = row[Obras.imgUrl] ?: "",
                    likes = row[Obras.likes],
                    score = row[Obras.score] ?: 0.0,
                    tags = tagsOfObra,
                    exhibitionId = expoId,
                    exhibitionTitle = row[Exposiciones.titulo],
                    nombreLugar = row[Exposiciones.nombreLugar],
                    ubicacion = row[Exposiciones.ubicacion],
                    artistName = artistName,
                    artistAvatar = artistAvatar
                )
            }

        println("MatchService: availableArtworks=${availableArtworks.size} obras no vistas")

        // 4. Calcular scores para cada obra
        val scoredArtworks = availableArtworks.map { artwork ->
            var sumNormalizedWeights = 0.0
            var maxFamiliaridad = 0.0
            var maxNovedad = 0.0
            var sumNovelty = 0.0

            val tagsCount = artwork.tags.size.coerceAtLeast(1)

            for (tagId in artwork.tags) {
                val weight = userPrefs[tagId] ?: 0.0
                val normalizedWeight = if (maxUserWeight > 0.0) (weight / maxUserWeight).coerceIn(0.0, 1.0) else 0.0
                val novelty = 1.0 - normalizedWeight

                sumNormalizedWeights += normalizedWeight
                sumNovelty += novelty
                
                if (normalizedWeight > maxFamiliaridad) maxFamiliaridad = normalizedWeight
                if (novelty > maxNovedad) maxNovedad = novelty
            }

            val baseScore = if (maxUserWeight > 0.0) {
                50.0 + 50.0 * (sumNormalizedWeights / tagsCount)
            } else {
                val popularityFactor = artwork.likes.toDouble() / maxGlobalLikes
                val globalScore = artwork.score.coerceIn(0.0, 1.0)
                // Per-artwork noise so cold-start scores spread even when DB signals are zero
                val coldNoise = Random.nextDouble(0.0, 20.0)
                (45.0 + 25.0 * globalScore + 15.0 * popularityFactor + coldNoise).coerceIn(0.0, 100.0)
            }
            // Jitter ±8% so scores feel alive on every load and vary which artworks surface
            val jitter = (Random.nextDouble() - 0.5) * 16.0
            val exploitationScorePercent = (baseScore + jitter).coerceIn(0.0, 100.0)

            val bridgeScore = (maxFamiliaridad * maxNovedad) + 0.1 * (sumNovelty / tagsCount)
            val explorationScore = bridgeScore + 0.1 * (artwork.likes.toDouble() / maxGlobalLikes)

            ScoredArtwork(
                artwork = artwork,
                exploitationScore = exploitationScorePercent,
                explorationScore = explorationScore
            )
        }

        // 5. Epsilon-Greedy Mix
        val epsilon = maxOf(0.20, 0.70 - 0.05 * totalUserLikes)
        
        val exploitationPool = scoredArtworks.sortedByDescending { it.exploitationScore }.toMutableList()
        val explorationPool = scoredArtworks.sortedByDescending { it.explorationScore }.toMutableList()

        val result = mutableListOf<DiscoverArtworkDto>()
        val selectedIds = mutableSetOf<Long>()

        val limit = minOf(5, scoredArtworks.size)
        while (result.size < limit && (exploitationPool.isNotEmpty() || explorationPool.isNotEmpty())) {
            val explore = Random.nextDouble() < epsilon
            
            val chosenScored = if (explore && explorationPool.isNotEmpty()) {
                val item = explorationPool.first()
                explorationPool.removeAt(0)
                exploitationPool.removeAll { it.artwork.idObra == item.artwork.idObra }
                item.copy(isExploration = true)
            } else if (exploitationPool.isNotEmpty()) {
                val item = exploitationPool.first()
                exploitationPool.removeAt(0)
                explorationPool.removeAll { it.artwork.idObra == item.artwork.idObra }
                item.copy(isExploration = false)
            } else {
                val item = explorationPool.first()
                explorationPool.removeAt(0)
                item.copy(isExploration = true)
            }

            if (selectedIds.add(chosenScored.artwork.idObra)) {
                result.add(
                    DiscoverArtworkDto(
                        idObra = chosenScored.artwork.idObra,
                        titulo = chosenScored.artwork.titulo,
                        imgUrl = chosenScored.artwork.imgUrl,
                        matchScore = if (chosenScored.isExploration) {
                            // Normalize explorationScore (0..~1) to percentage (30..70%)
                            30.0 + (chosenScored.explorationScore.coerceIn(0.0, 1.0) * 40.0)
                        } else {
                            chosenScored.exploitationScore
                        },
                        isExploration = chosenScored.isExploration,
                        exhibitionId = chosenScored.artwork.exhibitionId,
                        exhibitionTitle = chosenScored.artwork.exhibitionTitle,
                        nombreLugar = chosenScored.artwork.nombreLugar,
                        ubicacion = chosenScored.artwork.ubicacion,
                        artistName = chosenScored.artwork.artistName,
                        artistAvatar = chosenScored.artwork.artistAvatar
                    )
                )
            }
        }

        println("MatchService: Returning ${result.size} artworks for userId=$userId (epsilon=${"%.2f".format(epsilon)})")
        result
    }

    fun recordSwipe(userId: Long, obraId: Long, liked: Boolean, matchScore: Double) = transaction {
        // 0. Ensure the user has an Interesado record so FK inserts work
        ensureInteresadoExists(userId)

        println("MatchService: recordSwipe userId=$userId, obraId=$obraId, liked=$liked, matchScore=$matchScore")

        // 1. Guardar en Feeds (historial de vistos)
        val exists = Feeds.select { (Feeds.idInteresado eq userId) and (Feeds.idObra eq obraId) }.empty().not()
        if (!exists) {
            Feeds.insert {
                it[idInteresado] = userId
                it[idObra] = obraId
                it[fechaInteraccion] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                it[this.matchScore] = matchScore
            }
            println("MatchService: Feed record inserted for obra $obraId")
        } else {
            println("MatchService: Feed record already exists for obra $obraId")
        }

        // 2. Si es Like, guardar en Likes y actualizar pesos de tags
        if (liked) {
            val likeExists = Likes.select { (Likes.idInteresado eq userId) and (Likes.idObra eq obraId) }.empty().not()
            if (!likeExists) {
                Likes.insert {
                    it[idInteresado] = userId
                    it[idObra]       = obraId
                    it[fechaLike]    = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
                
                val tagsOfObra = TagObras.select { TagObras.idObra eq obraId }.map { it[TagObras.idTag].value }
                println("MatchService: Like recorded. Updating weights for ${tagsOfObra.size} tags")
                for (tagId in tagsOfObra) {
                    val prefRow = InteresadoTagPreferences.select {
                        (InteresadoTagPreferences.idInteresado eq userId) and (InteresadoTagPreferences.idTag eq tagId)
                    }.singleOrNull()

                    if (prefRow != null) {
                        val currentPeso = prefRow[InteresadoTagPreferences.peso]
                        InteresadoTagPreferences.update({
                            (InteresadoTagPreferences.idInteresado eq userId) and (InteresadoTagPreferences.idTag eq tagId)
                        }) {
                            it[peso] = currentPeso + 1.0
                        }
                    } else {
                        InteresadoTagPreferences.insert {
                            it[idInteresado] = userId
                            it[idTag] = tagId
                            it[peso] = 1.0
                        }
                    }
                }
            }
        }
    }

    fun getLikedArtworks(userId: Long): List<DiscoverArtworkDto> = transaction {
        // Paso 1: obtener idObra + fecha, ordenados del más reciente al más antiguo
        val likedRows = Likes
            .select { Likes.idInteresado eq userId }
            .orderBy(Likes.fechaLike, SortOrder.DESC_NULLS_LAST)

        val likedObraIds = likedRows.map { it[Likes.idObra].value }
        // Mapa para recuperar la fecha y restaurar el orden tras el segundo join
        val fechaPorObra = likedRows.associate {
            it[Likes.idObra].value to it[Likes.fechaLike]
        }

        if (likedObraIds.isEmpty()) return@transaction emptyList()

        // Paso 2: cargar datos de obra + exposición y reordenar por fecha de like
        (Obras innerJoin Exposiciones)
            .select { Obras.id inList likedObraIds }
            .map { row ->
                val obraId  = row[Obras.id].value
                val expoId  = row[Exposiciones.id].value

                val artistRow = (ArtistaExposiciones innerJoin Artistas innerJoin Usuarios)
                    .select { ArtistaExposiciones.idExposicion eq expoId }
                    .firstOrNull()

                val artistId     = artistRow?.get(Artistas.id)?.value ?: 0L
                val artistName   = artistRow?.let { "${it[Usuarios.nombre]} ${it[Usuarios.apellidos] ?: ""}".trim() } ?: "Unknown"
                val artistAvatar = artistRow?.get(Usuarios.imgUrl) ?: ""

                DiscoverArtworkDto(
                    idObra          = obraId,
                    titulo          = row[Obras.titulo] ?: "Sin título",
                    imgUrl          = row[Obras.imgUrl] ?: "",
                    matchScore      = 100.0,
                    isExploration   = false,
                    exhibitionId    = expoId,
                    exhibitionTitle = row[Exposiciones.titulo],
                    nombreLugar     = row[Exposiciones.nombreLugar],
                    ubicacion       = row[Exposiciones.ubicacion],
                    artistName      = artistName,
                    artistAvatar    = artistAvatar,
                    artistId        = artistId
                )
            }
            // Restaurar el orden cronológico inverso (más reciente primero)
            // El SELECT con inList no garantiza orden, así que lo aplicamos aquí
            .sortedByDescending { dto -> fechaPorObra[dto.idObra] }
    }

    private data class ArtworkData(
        val idObra: Long,
        val titulo: String,
        val imgUrl: String,
        val likes: Long,
        val score: Double,
        val tags: List<Long>,
        val exhibitionId: Long,
        val exhibitionTitle: String,
        val nombreLugar: String?,
        val ubicacion: String?,
        val artistName: String,
        val artistAvatar: String
    )

    private data class ScoredArtwork(
        val artwork: ArtworkData,
        val exploitationScore: Double,
        val explorationScore: Double,
        val isExploration: Boolean = false
    )
}
