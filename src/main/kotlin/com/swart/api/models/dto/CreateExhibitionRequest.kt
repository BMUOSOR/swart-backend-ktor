package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateExhibitionRequest(
    val titulo: String,
    val descrip: String? = null,
    val nombreLugar: String? = null,
    val ubicacion: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val imgUrl: String? = null,
    val precio: Double? = null,
    val categoria: String? = null,
    val activa: Boolean = true,
    val esColaborativa: Boolean = false,
    val artistaId: Long,
    val artistasInvitadosIds: List<Long> = emptyList()
)
