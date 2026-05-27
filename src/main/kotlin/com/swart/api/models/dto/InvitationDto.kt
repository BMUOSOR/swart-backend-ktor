package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(
    val idInvitacion: Long,
    val idExposicion: Long,
    val tituloExposicion: String,
    val exposicionImgUrl: String?,
    val idArtistaSender: Long,
    val nombreArtistaSender: String,
    val avatarArtistaSender: String?,
    val estado: String // pendiente / aceptada / rechazada
)

@Serializable
data class RespondInvitationRequest(
    val aceptar: Boolean
)

@Serializable
data class MutualArtistDto(
    val id: Long,
    val nombre: String,
    val avatarUrl: String?
)

@Serializable
data class ArtistFollowDto(
    val id: Long,
    val nombre: String,
    val avatarUrl: String?,
    val following: Boolean
)
