package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Interesados : IntIdTable("Interesado", "idInteresado") {
    // Entidad ligera, por ahora sin campos adicionales
}
