package org.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.example.db.DatabaseFactory
import org.example.models.UserSession
import org.example.routes.authRoutes

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()

    install(Sessions) {
        cookie<UserSession>("user_session")
    }

    routing {
        authRoutes()
    }
}