package com.swart.api.routes

import com.swart.api.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.seedRoutes() {
    get("/seed") {
        try {
            transaction {
                // 1. Insertar Usuarios
                val adminId = Usuarios.insertAndGetId {
                    it[usuario] = "admin"
                    it[password] = "123456" // En un caso real iría hasheado
                    it[nombre] = "Admin"
                    it[apellidos] = "Swart"
                }

                val visitorId = Usuarios.insertAndGetId {
                    it[usuario] = "visitor"
                    it[password] = "123456"
                    it[nombre] = "John"
                    it[apellidos] = "Doe"
                }

                // 2. Insertar Artista (1:1 con Admin)
                Artistas.insert {
                    it[id] = adminId
                    it[instagram] = "@picasso_official"
                    it[whatsapp] = "+34600000000"
                    it[correo] = "pablo@picasso.com"
                    it[x] = "@picasso"
                    it[bio] = "Pintor y escultor español, creador del cubismo."
                }

                // 3. Insertar Exposición
                val expoId = Exposiciones.insertAndGetId {
                    it[idArtista] = adminId // El id del artista es el mismo que el id de usuario
                    it[titulo] = "Cubismo Contemporáneo"
                    it[descrip] = "Una muestra de las mejores obras cubistas de la década."
                    it[ubicacion] = "Museo del Prado, Madrid"
                    it[precio] = 15.0
                    it[visitantes] = 0L
                    it[score] = 4.8
                    it[activa] = true
                }

                // 4. Insertar Baliza (1:1 con Exposicion)
                Balizas.insert {
                    it[id] = expoId
                    it[lat] = 40.41378
                    it[lon] = -3.692127
                }

                // 5. Insertar Obras
                val obra1Id = Obras.insertAndGetId {
                    it[idExposicion] = expoId
                    it[titulo] = "Guernica"
                    it[descrip] = "Alegato contra la guerra."
                    it[dimensiones] = "349x776 cm"
                    it[anio] = 1937L
                    it[precio] = 0.0
                    it[likes] = 0L
                    it[score] = 5.0
                    it[archivo] = "https://ejemplo.com/guernica.jpg"
                    it[oculta] = false
                }

                val obra2Id = Obras.insertAndGetId {
                    it[idExposicion] = expoId
                    it[titulo] = "Las señoritas de Avignon"
                    it[descrip] = "Inicio del periodo cubista."
                    it[dimensiones] = "243x233 cm"
                    it[anio] = 1907L
                    it[precio] = 0.0
                    it[likes] = 0L
                    it[score] = 4.9
                    it[archivo] = "https://ejemplo.com/avignon.jpg"
                    it[oculta] = false
                }

                // 6. Insertar Tags
                val tagOleoId = Tags.insertAndGetId {
                    it[nombre] = "Óleo"
                    it[descrip] = "Pintura al óleo"
                }

                val tagCubismoId = Tags.insertAndGetId {
                    it[nombre] = "Cubismo"
                    it[descrip] = "Estilo vanguardista"
                }

                // 7. Relacionar Obras con Tags
                TagObras.insert {
                    it[idTag] = tagOleoId
                    it[idObra] = obra1Id
                }
                TagObras.insert {
                    it[idTag] = tagCubismoId
                    it[idObra] = obra1Id
                }
                TagObras.insert {
                    it[idTag] = tagOleoId
                    it[idObra] = obra2Id
                }
                TagObras.insert {
                    it[idTag] = tagCubismoId
                    it[idObra] = obra2Id
                }

                // 8. Insertar Interesado (1:1 con Visitor)
                Interesados.insert {
                    it[id] = visitorId
                }

                // 9. Insertar Interacción de prueba (Like al Guernica por el Interesado)
                Likes.insert {
                    it[idInteresado] = visitorId // El id del interesado es el mismo que el visitorId
                    it[idObra] = obra1Id
                }
            }
            call.respondText("Base de datos poblada con éxito!", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText("Error poblando DB: ${e.localizedMessage}", status = HttpStatusCode.InternalServerError)
        }
    }
}
