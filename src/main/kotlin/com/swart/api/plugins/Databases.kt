package com.swart.api.plugins

import com.swart.api.models.*
import com.swart.api.routes.seedDatabase
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
    
    var isPostgres = true
    val dataSource = try {
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5      // Supabase free tier: max 15 conexiones en session mode
            minimumIdle = 2          // Mantener pocas conexiones idle
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout = 5000 // 5 seconds connection timeout to fall back quickly if offline
            validate()
        }
        HikariDataSource(config)
    } catch (e: Exception) {
        println("ERROR: Falló la conexión a la base de datos PostgreSQL en la nube (${e.message}).")
        println("Iniciando fallback local: base de datos H2 en memoria...")
        isPostgres = false
        val h2Config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:file:./swart_local_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDER=HIGH"
            username = "sa"
            password = ""
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 5
            isAutoCommit = false
            validate()
        }
        HikariDataSource(h2Config)
    }
    
    Database.connect(dataSource)
    
    transaction {
        if (isPostgres) {
            try {
                exec("ALTER TABLE \"Exposicion\" DROP COLUMN IF EXISTS \"idArtista\" CASCADE;")
                exec("DROP TABLE IF EXISTS \"Feed\" CASCADE;")
                exec("DROP TABLE IF EXISTS \"InteresadoTagPreference\" CASCADE;")
                exec("ALTER TABLE \"Exposicion\" ADD COLUMN IF NOT EXISTS \"es_colaborativa\" BOOLEAN DEFAULT FALSE;")
                exec("ALTER TABLE \"Exposicion\" ADD COLUMN IF NOT EXISTS \"categoria\" TEXT DEFAULT NULL;")
                exec("ALTER TABLE \"Obra\" ADD COLUMN IF NOT EXISTS \"idArtista\" BIGINT REFERENCES \"Artista\"(\"idArtista\") ON DELETE CASCADE;")
                exec("ALTER TABLE \"Like\" ADD COLUMN IF NOT EXISTS \"fecha_like\" TIMESTAMP DEFAULT NOW();")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
            Invitaciones,
            Conversaciones,
            Mensajes,
            BalizasVacias,
            PropuestasBalizaVacia,
            BalizasGubernamentales
        )

        if (isPostgres) {
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
        } else {
            // Seed H2 in-memory database with default records to allow testing locally
            try {
                seedDatabase()
                println("Base de datos H2 en memoria poblada con datos de prueba con éxito.")
            } catch (e: Exception) {
                println("ERROR al poblar base de datos H2: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
