package com.swart.api.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    // Database configuration for Supabase / PostgreSQL
    // You should use environment variables for these in a real deployment
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/swart"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
    
    Database.connect(
        url = dbUrl,
        user = dbUser,
        password = dbPassword,
        driver = "org.postgresql.Driver"
    )
    
    // Here you would also initialize your tables, e.g.:
    // transaction {
    //     SchemaUtils.create(Users, Exhibitions, Swipes)
    // }
}
