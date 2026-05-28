package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateExhibitionRequest(
    val titulo: String,
    val descrip: String? = null,
    val nombreLugar: String? = null,
    val ubicacion: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val imgUrl: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class ExhibitionDetailDTO(
    val idExposicion: Long,
    val titulo: String,
    val descrip: String?,
    val nombreLugar: String?,
    val ubicacion: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val imgUrl: String?,
    val precio: Double?,
    val score: Double?,
    val activa: Boolean,
    val obras: List<ArtworkDTO>,
    val tags: List<String>,
    val esColaborativa: Boolean = false,
    val artistas: List<ArtistFeedDTO> = emptyList()
)
