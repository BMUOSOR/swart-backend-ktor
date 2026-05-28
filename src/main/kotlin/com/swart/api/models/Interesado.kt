package com.swart.api.models

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Interesados : IdTable<Long>("\"Interesado\"") {
    override val id = reference("\"idInteresado\"", Usuarios, onDelete = ReferenceOption.CASCADE, fkName = "fk_interesado_usuario")
}
