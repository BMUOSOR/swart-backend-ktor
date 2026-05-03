package com.swart.api.plugins

import com.swart.api.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val dbUrl = System.getenv("DB_JDBC_URL") ?: "jdbc:postgresql://localhost:5432/swart"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
    
    val config = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    val dataSource = HikariDataSource(config)
    
    Database.connect(dataSource)
    
    transaction {
        println("=== INICIO DE DEBUG DE TABLAS EN SUPABASE ===")
        try {
            val stmt = connection.prepareStatement("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname = 'public';", false)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                println("TABLA ENCONTRADA EN PUBLIC: '${rs.getString(1)}'")
            }
        } catch (e: Exception) {
            println("ERROR LEYENDO TABLAS: ${e.message}")
        }
        println("=== FIN DE DEBUG DE TABLAS ===")

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
            Feeds
        )
    }
}
