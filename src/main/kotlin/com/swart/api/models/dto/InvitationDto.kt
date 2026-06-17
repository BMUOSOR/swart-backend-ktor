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
    val estado: String, // pendiente / aceptada / rechazada
    val tipo: String = "invitacion",
    val descrip: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val precio: Double? = null
)

@Serializable
data class RespondInvitationRequest(
    val aceptar: Boolean
)

@Serializable
data class CreateInvitationRequest(
    val idExposicion: Long,
    val idArtistaSender: Long,
    val idArtistaReceiver: Long
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
