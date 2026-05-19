package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistProfileDTO(
    val id: Long,
    val nombre: String,
    val apellidos: String?,
    val avatarUrl: String?,
    val bio: String?,
    val seguidores: Int,
    val exposicionesCount: Int,
    val instagram: String?,
    val twitter: String?, // x
    val correo: String?,
    val activeExhibitions: List<ExhibitionFeedDTO>,
    val worksForSale: List<ArtworkForSaleDTO>
)

@Serializable
data class ArtworkForSaleDTO(
    val idObra: Long,
    val titulo: String,
    val imgUrl: String,
    val precio: Double
)
