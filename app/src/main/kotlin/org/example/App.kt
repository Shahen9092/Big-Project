package org.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.http.content.*
import org.example.db.DatabaseFactory
import org.example.db.ExerciseSeeder
import org.example.models.UserSession
import org.example.routes.activityRoutes
import org.example.routes.authRoutes
import org.example.routes.friendRoutes
import org.example.routes.goalRoutes
import org.example.routes.templateRoutes

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {

    DatabaseFactory.init()

    ExerciseSeeder.seedExercises()

    install(Sessions) {
        cookie<UserSession>("user_session")
    }

    routing {
        staticResources("/", "static")
        authRoutes()
        activityRoutes()
        friendRoutes()
        templateRoutes()
        goalRoutes()
    }
}