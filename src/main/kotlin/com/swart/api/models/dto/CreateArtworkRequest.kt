package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateArtworkRequest(
    val idExposicion: Long,
    val idArtista: Long,
    val titulo: String,
    val descrip: String? = null,
    val imgUrl: String? = null,
    val precio: Double? = null,
    val disponibleCompra: Boolean = false,
    val tags: List<String> = emptyList()
)
