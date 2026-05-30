package com.swart.api.plugins

import com.swart.api.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val dbUrl = environment.config.propertyOrNull("storage.jdbcUrl")?.getString() ?: "jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0"
    val dbUser = environment.config.propertyOrNull("storage.dbUser")?.getString() ?: "postgres.bkrmqkpxidmemzxhefoc"
    val dbPassword = environment.config.propertyOrNull("storage.dbPassword")?.getString() ?: "bollopower.2424"
    
    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 5      // Supabase free tier: max 15 conexiones en session mode
        minimumIdle = 2          // Mantener pocas conexiones idle
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    val dataSource = HikariDataSource(config)
    
    Database.connect(dataSource)
    
    transaction {
        exec("ALTER TABLE \"Exposicion\" DROP COLUMN IF EXISTS \"idArtista\" CASCADE;")
        exec("DROP TABLE IF EXISTS \"Feed\" CASCADE;")
        exec("DROP TABLE IF EXISTS \"InteresadoTagPreference\" CASCADE;")
        exec("ALTER TABLE \"Exposicion\" ADD COLUMN IF NOT EXISTS \"es_colaborativa\" BOOLEAN DEFAULT FALSE;")
        exec("ALTER TABLE \"Obra\" ADD COLUMN IF NOT EXISTS \"idArtista\" BIGINT REFERENCES \"Artista\"(\"idArtista\") ON DELETE CASCADE;")
        SchemaUtils.create(
            Usuarios,
            Artistas,
            Exposiciones,
            Balizas,
            Obras,
            Tags,
            TagObras,
            Interesados,
            Likes,
            Feeds,
            InteresadoTagPreferences,
            Seguidores,
            ArtistaExposiciones,
            Invitaciones
        )

        // Data healing: Set Obras.idArtista to the creator artist (from ArtistaExposiciones) for any artworks where it is null
        try {
            val artworksWithNullArtist = Obras.select { Obras.idArtista.isNull() }.toList()
            artworksWithNullArtist.forEach { row ->
                val obraId = row[Obras.id]
                val idExpo = row[Obras.idExposicion]
                val firstArtistId = ArtistaExposiciones
                    .select { ArtistaExposiciones.idExposicion eq idExpo }
                    .firstOrNull()?.get(ArtistaExposiciones.idArtista)
                if (firstArtistId != null) {
                    Obras.update({ Obras.id eq obraId }) {
                        it[idArtista] = firstArtistId
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
