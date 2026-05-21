package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateExhibitionRequest(
    val titulo: String,
    val descrip: String?,
    val nombreLugar: String?,
    val ubicacion: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val tags: List<String> = emptyList()
)

@Serializable
data class ExhibitionDetailDTO(
    val idExposicion: Int,
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
    val tags: List<String>
)
