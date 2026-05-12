package com.swart.api.plugins

import com.swart.api.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    val dataSource = HikariDataSource(config)
    
    Database.connect(dataSource)
    
    transaction {
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
