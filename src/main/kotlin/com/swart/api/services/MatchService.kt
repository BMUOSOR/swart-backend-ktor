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

    fun getDiscoverFeed(userId: Long): List<DiscoverArtworkDto> = transaction {
        // 1. Obtener los tags preferidos del usuario y sus pesos
        val userPrefs = InteresadoTagPreferences
            .select { InteresadoTagPreferences.idInteresado eq userId }
            .associate { it[InteresadoTagPreferences.idTag].value to it[InteresadoTagPreferences.peso] }

        val totalUserLikes = Likes.select { Likes.idInteresado eq userId }.count()
        val maxUserWeight = userPrefs.values.maxOrNull() ?: 0.0

        // 2. Obtener obras ya vistas por el usuario
        val seenArtworkIds = Feeds
            .select { Feeds.idInteresado eq userId }
            .map { it[Feeds.idObra].value }
            .toSet()

        // 3. Obtener obras no vistas (join con Exposicion, Artista, y pre-cargar tags)
        val maxGlobalLikes = Obras.selectAll().maxOfOrNull { it[Obras.likes] }?.coerceAtLeast(1L) ?: 1L

        val availableArtworks = (Obras innerJoin Exposiciones)
            .select { Obras.id notInList seenArtworkIds }
            .map { row ->
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
                    tags = tagsOfObra,
                    exhibitionId = expoId,
                    exhibitionTitle = row[Exposiciones.titulo],
                    nombreLugar = row[Exposiciones.nombreLugar],
                    ubicacion = row[Exposiciones.ubicacion],
                    artistName = artistName,
                    artistAvatar = artistAvatar
                )
            }

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

            val exploitationScorePercent = if (maxUserWeight > 0.0) {
                50.0 + 50.0 * (sumNormalizedWeights / tagsCount)
            } else {
                70.0 + 10.0 * (artwork.likes.toDouble() / maxGlobalLikes)
            }

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

        val limit = minOf(20, scoredArtworks.size)
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
                        matchScore = chosenScored.exploitationScore,
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

        result
    }

    fun recordSwipe(userId: Long, obraId: Long, liked: Boolean, matchScore: Double) = transaction {
        // 1. Guardar en Feeds (historial de vistos)
        val exists = Feeds.select { (Feeds.idInteresado eq userId) and (Feeds.idObra eq obraId) }.empty().not()
        if (!exists) {
            Feeds.insert {
                it[idInteresado] = userId
                it[idObra] = obraId
                it[fechaInteraccion] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                it[this.matchScore] = matchScore
            }
        }

        // 2. Si es Like, guardar en Likes y actualizar pesos de tags
        if (liked) {
            val likeExists = Likes.select { (Likes.idInteresado eq userId) and (Likes.idObra eq obraId) }.empty().not()
            if (!likeExists) {
                Likes.insert {
                    it[idInteresado] = userId
                    it[idObra] = obraId
                }
                
                val tagsOfObra = TagObras.select { TagObras.idObra eq obraId }.map { it[TagObras.idTag].value }
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

    private data class ArtworkData(
        val idObra: Long,
        val titulo: String,
        val imgUrl: String,
        val likes: Long,
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
