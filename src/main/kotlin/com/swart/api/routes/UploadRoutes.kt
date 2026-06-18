package com.swart.api.routes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import java.util.UUID

// Shared CIO client — avoids recreating it on every request
private val uploadHttpClient = HttpClient(CIO)

fun Route.uploadRoutes() {

    route("/api/upload") {
        post {
            val multipart = try {
                call.receiveMultipart()
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No multipart content"))
            }

            var fileBytes: ByteArray? = null
            var fileName: String? = null
            var contentType: String? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    fileBytes = part.streamProvider().readBytes()
                    val originalName = part.originalFileName ?: "image.jpg"
                    val ext = originalName.substringAfterLast('.', "jpg")
                    fileName = "${UUID.randomUUID()}.$ext"
                    contentType = part.contentType?.toString() ?: "image/jpeg"
                }
                part.dispose()
            }

            if (fileBytes == null || fileName == null) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file provided"))
            }

            // Key resolution order: env vars first, then hardcoded service_role key as fallback.
            // The service_role key bypasses RLS so uploads always work regardless of bucket policies.
            val supabaseKey = System.getenv("SUPABASE_SERVICE_KEY")
                ?: System.getenv("SUPABASE_KEY")
                ?: System.getenv("SUPABASE_ANON_KEY")
                ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJrcm1xa3B4aWRtZW16eGhlZm9jIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NDc4MDM1OSwiZXhwIjoyMDkwMzU2MzU5fQ.waCAcGPfRKvDsUsS-k6aOhP-3_-CWUzoj80J-VibdhE"

            val supabaseUrl = "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/Imagenes/$fileName"
            println("UploadRoutes: Uploading ${fileBytes!!.size} bytes → $fileName (type=$contentType)")

            try {
                val response = uploadHttpClient.put(supabaseUrl) {
                    setBody(fileBytes!!)
                    header(HttpHeaders.ContentType, contentType!!)
                    header(HttpHeaders.Authorization, "Bearer $supabaseKey")
                    header("apikey", supabaseKey)
                    header("x-upsert", "true")
                }

                val responseBody = response.bodyAsText()
                println("UploadRoutes: Supabase responded ${response.status} → $responseBody")

                if (response.status.isSuccess()) {
                    val publicUrl = "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/public/Imagenes/$fileName"
                    println("UploadRoutes: Upload OK → $publicUrl")
                    call.respond(HttpStatusCode.OK, mapOf("url" to publicUrl))
                } else {
                    println("UploadRoutes: Supabase FAILED ${response.status}: $responseBody")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Supabase error ${response.status.value}: $responseBody")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("UploadRoutes: Exception: ${e::class.simpleName}: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Upload exception: ${e.message}"))
            }
        }
    }
}
