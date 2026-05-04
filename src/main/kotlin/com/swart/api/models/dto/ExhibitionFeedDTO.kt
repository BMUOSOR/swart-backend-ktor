package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExhibitionFeedDTO(
    val idExposicion: Long,
    val titulo: String,
    val descrip: String?,
    val artistaNombre: String,
    val artistaAvatar: String,
    val isNew: Boolean,
    val obrasCount: Int,
    val obrasImages: List<String>,
    val exposicionImgUrl: String?
)
