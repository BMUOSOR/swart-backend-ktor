package com.swart.api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val usuario: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val nombre: String,
    val apellidos: String,
    val usuario: String,
    val password: String,
    val role: String
)

@Serializable
data class AuthResponse(
    val id: Long,
    val nombre: String,
    val apellidos: String?,
    val usuario: String,
    val role: String,
    val token: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)
