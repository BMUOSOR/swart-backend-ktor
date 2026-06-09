package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoverArtworkDto(
    val idObra: Long,
    val titulo: String,
    val imgUrl: String,
    val matchScore: Double,
    val isExploration: Boolean,
    val exhibitionId: Long,
    val exhibitionTitle: String,
    val nombreLugar: String?,
    val ubicacion: String?,
    val artistName: String,
    val artistAvatar: String,
    val artistId: Long = 0L
)

@Serializable
data class SwipeRequestDto(
    val idInteresado: Long,
    val idObra: Long,
    val liked: Boolean,
    val matchScore: Double
)
