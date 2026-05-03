package com.swart.api.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Interesados : IntIdTable("interesado", "idInteresado") {
    // Entidad ligera, por ahora sin campos adicionales
}
