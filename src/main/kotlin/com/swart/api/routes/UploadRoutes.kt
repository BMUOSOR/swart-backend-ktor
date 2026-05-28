package com.swart.api.routes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import java.util.UUID

fun Route.uploadRoutes() {
    val client = HttpClient()

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

            // Upload to Supabase Storage
            val supabaseUrl = "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/Imagenes/$fileName"
            val supabaseKey = System.getenv("SUPABASE_KEY") ?: System.getenv("SUPABASE_ANON_KEY")

            try {
                val response = client.put(supabaseUrl) {
                    setBody(fileBytes!!)
                    header(HttpHeaders.ContentType, contentType!!)
                    if (!supabaseKey.isNullOrEmpty()) {
                        header("Authorization", "Bearer $supabaseKey")
                    }
                }

                if (response.status.isSuccess()) {
                    val publicUrl = "https://bkrmqkpxidmemzxhefoc.supabase.co/storage/v1/object/public/Imagenes/$fileName"
                    call.respond(HttpStatusCode.OK, mapOf("url" to publicUrl))
                } else {
                    println("Supabase upload failed: ${response.status} - ${response.bodyAsText()}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to upload to Supabase: ${response.status}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Upload error: ${e.message}"))
            }
        }
    }
}
