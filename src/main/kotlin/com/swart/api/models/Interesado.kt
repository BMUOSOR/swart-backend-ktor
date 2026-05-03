package com.swart.api.models

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Interesados : IdTable<Long>("interesado") {
    override val id = reference("idinteresado", Usuarios, onDelete = ReferenceOption.CASCADE)
}
