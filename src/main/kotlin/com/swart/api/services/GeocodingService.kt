package com.swart.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String
)

object GeocodingService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun verifyAddress(address: String): NominatimResult? {
        if (address.isBlank()) return null
        return try {
            val response: HttpResponse = client.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", address)
                parameter("format", "json")
                parameter("limit", "1")
                headers {
                    append(HttpHeaders.UserAgent, "SwartApp/1.0 (antigravity)")
                }
            }
            if (response.status.isSuccess()) {
                val results: List<NominatimResult> = response.body()
                results.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
