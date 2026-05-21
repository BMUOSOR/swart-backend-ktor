package com.swart.api.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object InteresadoTagPreferences : Table("\"InteresadoTagPreference\"") {
    val idInteresado = reference(
        name = "\"idInteresado\"", 
        foreign = Interesados, 
        onDelete = ReferenceOption.CASCADE, 
        fkName = "fk_pref_interesado"
    )
    val idTag = reference(
        name = "\"idTag\"", 
        foreign = Tags, 
        onDelete = ReferenceOption.CASCADE, 
        fkName = "fk_pref_tag"
    )
    val peso = double("\"peso\"").default(0.0)

    override val primaryKey = PrimaryKey(idInteresado, idTag)
}
