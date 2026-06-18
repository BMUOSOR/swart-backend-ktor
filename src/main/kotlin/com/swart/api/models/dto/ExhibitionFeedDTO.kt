package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistFeedDTO(
    val id: Long,
    val nombre: String,
    val avatarUrl: String
)

@Serializable
data class ExhibitionFeedDTO(
    val idExposicion: Long,
    val idArtista: Long,
    val titulo: String,
    val descrip: String?,
    val artistaNombre: String,
    val artistaAvatar: String,
    val artistas: List<ArtistFeedDTO>,
    val isNew: Boolean,
    val obrasCount: Int,
    val obras: List<ArtworkDTO>,
    val exposicionImgUrl: String?,
    val tags: List<String>,
    val fechaInicio: String?,
    val fechaFin: String?,
    val nombreLugar: String?,
    val ubicacion: String?,
    val precio: Double?,
    val score: Double?,
    val visitantes: Long = 0,
    val favoritosCount: Int = 0,
    val idBalizaVacia: Long? = null
)
