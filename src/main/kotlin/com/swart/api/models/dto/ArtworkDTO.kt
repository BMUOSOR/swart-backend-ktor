package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtworkDTO(
    val idObra: Long,
    val titulo: String,
    val imgUrl: String
)
