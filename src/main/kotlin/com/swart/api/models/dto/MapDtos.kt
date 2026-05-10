package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class MapPinDto(
    val idExposicion: Long,
    val lat: Double,
    val lon: Double,
    val titulo: String,
    val galeria: String,
    val imagen: String?,
    val distancia: String, // Mocked for MVP
    val match: Int, // Mocked for MVP
    val mainTag: String // "pintura", "escultura", "fotografía"
)
