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
data class HfMessage(
    val role: String,
    val content: List<HfContent>
)

@Serializable
data class HfContent(
    val type: String,
    val text: String? = null,
    val image_url: HfImageUrl? = null
)

@Serializable
data class HfImageUrl(
    val url: String
)

@Serializable
data class HfRequest(
    val model: String,
    val messages: List<HfMessage>,
    val max_tokens: Int
)

@Serializable
data class HfResponseChoice(
    val message: HfResponseMessage
)

@Serializable
data class HfResponseMessage(
    val content: String
)

@Serializable
data class HfResponse(
    val choices: List<HfResponseChoice>
)

object CuratorService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private const val HF_TOKEN = "TU_HF_TOKEN_AQUI" // In a real app this should be an env variable
    private const val MODEL_ID = "zai-org/GLM-4.5V"
    private const val HF_API_URL = "https://api-inference.huggingface.co/models/$MODEL_ID/v1/chat/completions"

    private val taxonomias = mapOf(
        "Pintura" to """
        - Tipo de obra: retrato, autorretrato, paisaje, paisaje urbano, naturaleza muerta, escena histórica, escena religiosa, escena mitológica, escena de género, abstracción, marina, interior, mural.
        - Estilo artístico: realismo, naturalismo, impresionismo, postimpresionismo, expresionismo, simbolismo, cubismo, surrealismo, fauvismo, abstracto, arte pop, minimalismo, contemporáneo, barroco, renacimiento, neoclasicismo, romanticismo.
        - Técnica: óleo, acrílico, acuarela, gouache, temple, fresco, tinta, pastel, técnica mixta, esmalte, aerosol.
        - Temática: figura humana, retrato psicológico, paisaje natural, paisaje urbano, flora, fauna, mar, arquitectura, vida cotidiana, trabajo, religión, mito, política, memoria, identidad, cuerpo, naturaleza, conflicto, muerte, intimidad.
        """,
        "Escultura" to """
        - Tipo de obra: busto, estatua, relieve, bajorrelieve, alto relieve, escultura exenta, escultura monumental, instalación, ensamblaje, escultura pública, objeto escultórico.
        - Estilo artístico: clásico, realismo, barroco, neoclásico, modernismo, expresionismo, cubismo, abstracto, minimalismo, conceptual, contemporáneo, arte povera, orgánico, geométrico.
        - Técnica: talla, modelado, fundición, ensamblaje, soldadura, vaciado, impresión 3D, talla directa, técnica mixta.
        - Temática: figura humana, cuerpo, retrato, monumento, memoria, mito, religión, naturaleza, animal, forma abstracta, espacio, movimiento, identidad, política.
        """,
        "Fotografía" to """
        - Tipo de obra: retrato, autorretrato, paisaje, paisaje urbano, documental, fotoperiodismo, naturaleza muerta, arquitectura, callejera, conceptual, moda, experimental, abstracta.
        - Estilo artístico: documental, realista, pictorialista, modernista, minimalista, conceptual, contemporáneo, experimental, surreal, abstracto.
        - Técnica: analógica, digital, blanco y negro, color, larga exposición, doble exposición, cianotipia, colodión, fotomontaje, intervención digital, macro, estudio.
        - Temática: identidad, cuerpo, memoria, familia, ciudad, paisaje, arquitectura, vida cotidiana, trabajo, política, conflicto, migración, naturaleza, intimidad, tiempo, archivo, comunidad.
        """
    )

    suspend fun classifyArtwork(imageUrl: String, disciplina: String): String {
        if (!taxonomias.containsKey(disciplina)) {
            return "{\"error\": \"Disciplina no válida\"}"
        }

        val instrucciones = """
        Actúa como un experto curador de arte. Analiza la imagen proporcionada sabiendo que es una obra de la disciplina: $disciplina.
        Tu tarea es clasificarla ESTRICTAMENTE utilizando SOLO las etiquetas de la siguiente lista. No puedes inventar ninguna palabra que no esté aquí.

        ETIQUETAS PERMITIDAS PARA ${disciplina.uppercase()}:
        ${taxonomias[disciplina]}

        FORMATO DE SALIDA REQUERIDO:
        Debes responder ÚNICAMENTE con un objeto JSON válido, sin texto adicional y sin formato Markdown (sin ```json), con la siguiente estructura exacta:
        {
          "tipo_de_obra": "Una sola etiqueta de la lista",
          "estilo_artistico": "Una sola etiqueta de la lista",
          "tecnica": "Una sola etiqueta de la lista",
          "tematica": "Una sola etiqueta de la lista"
        }
        """.trimIndent()

        val requestBody = HfRequest(
            model = MODEL_ID,
            messages = listOf(
                HfMessage(
                    role = "user",
                    content = listOf(
                        HfContent(type = "text", text = instrucciones),
                        HfContent(type = "image_url", image_url = HfImageUrl(url = imageUrl))
                    )
                )
            ),
            max_tokens = 200
        )

        return try {
            val response: HttpResponse = client.post(HF_API_URL) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${System.getenv("HF_TOKEN") ?: HF_TOKEN}")
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val hfResponse = response.body<HfResponse>()
                hfResponse.choices.firstOrNull()?.message?.content ?: "{\"error\": \"Respuesta vacía\"}"
            } else {
                "{\"error\": \"Fallo de la API: ${response.status} - ${response.bodyAsText()}\"}"
            }
        } catch (e: Exception) {
            "{\"error\": \"Excepción: ${e.message}\"}"
        }
    }
}
