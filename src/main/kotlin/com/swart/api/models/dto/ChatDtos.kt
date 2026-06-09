package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StartChatRequest(
    val senderId: Long,
    val receiverId: Long,
    val initialMessage: String? = null,
    val urlImagenObra: String? = null
)

@Serializable
data class ConversationDto(
    val idConversacion: Long,
    val otherUserId: Long,
    val otherUserNombre: String,
    val otherUserAvatar: String?,
    val ultimoMensaje: String,
    val fechaUltimoMensaje: String
)

@Serializable
data class MessageDto(
    val idMensaje: Long,
    val idSender: Long,
    val contenido: String,
    val urlImagenObra: String?,
    val fechaCreacion: String
)

@Serializable
data class SendMessageRequest(
    val senderId: Long,
    val contenido: String
)
