package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtworkDetailDTO(
    val idObra: Long,
    val titulo: String,
    val descrip: String?,
    val imgUrl: String?,
    val precio: Double?,
    val disponibleCompra: Boolean,
    val tags: List<String>,
    val categoriasExposicion: List<String>
)

@Serializable
data class UpdateArtworkRequest(
    val titulo: String,
    val descrip: String?,
    val precio: Double?,
    val disponibleCompra: Boolean,
    val tags: List<String>
)
