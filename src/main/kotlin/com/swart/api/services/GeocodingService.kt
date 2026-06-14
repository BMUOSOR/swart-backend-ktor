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

    suspend fun reverseGeocode(lat: Double, lon: Double): NominatimResult? {
        return try {
            val response: HttpResponse = client.get("https://nominatim.openstreetmap.org/reverse") {
                parameter("lat", lat.toString())
                parameter("lon", lon.toString())
                parameter("format", "json")
                headers { append(HttpHeaders.UserAgent, "SwartApp/1.0 (antigravity)") }
            }
            if (response.status.isSuccess()) response.body<NominatimResult>() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun verifyAddress(address: String): List<NominatimResult> {
        if (address.isBlank()) return emptyList()
        return try {
            val response: HttpResponse = client.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", address)
                parameter("format", "json")
                parameter("limit", "5")
                headers {
                    append(HttpHeaders.UserAgent, "SwartApp/1.0 (antigravity)")
                }
            }
            if (response.status.isSuccess()) {
                val results: List<NominatimResult> = response.body()
                results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
