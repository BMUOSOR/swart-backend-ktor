package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExhibitionFeedDTO(
    val idExposicion: Long,
    val idArtista: Long,
    val titulo: String,
    val descrip: String?,
    val artistaNombre: String,
    val artistaAvatar: String,
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
    val score: Double?
)
