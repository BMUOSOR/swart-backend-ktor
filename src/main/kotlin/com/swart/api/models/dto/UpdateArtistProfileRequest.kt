package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateArtistProfileRequest(
    val bio: String? = null,
    val instagram: String? = null,
    val twitter: String? = null,
    val correo: String? = null
)
