package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.example.renderDashboardPage
import org.example.renderLoginPage
import org.example.renderRegisterPage
import org.example.db.tables.UsersTable
import org.example.models.UserSession
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.authRoutes() {

    get("/login") {
        call.respondText(renderLoginPage(), ContentType.Text.Html)
    }

    post("/login") {
        val params = call.receiveParameters()
        val email = params["email"]?.trim().orEmpty()
        val password = params["password"].orEmpty()

        if (email.isBlank() || password.isBlank()) {
            call.respondText(
                renderLoginPage("Email and password are required."),
                ContentType.Text.Html
            )
            return@post
        }

        val user = transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
        }

        if (user == null || user[UsersTable.password] != password) {
            call.respondText(
                renderLoginPage("Invalid email or password."),
                ContentType.Text.Html
            )
            return@post
        }

        call.sessions.set(
            UserSession(
                userId = user[UsersTable.id],
                email = user[UsersTable.email]
            )
        )

        call.respondRedirect("/dashboard")
    }

    get("/register") {
        call.respondText(renderRegisterPage(), ContentType.Text.Html)
    }

    post("/register") {
        val params = call.receiveParameters()
        val enteredName = params["name"]?.trim().orEmpty()
        val email = params["email"]?.trim().orEmpty()
        val password = params["password"].orEmpty()

        if (enteredName.isBlank() || email.isBlank() || password.isBlank()) {
            call.respondText(
                renderRegisterPage("All fields are required."),
                ContentType.Text.Html
            )
            return@post
        }

        val existingUser = transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
        }

        if (existingUser != null) {
            call.respondText(
                renderRegisterPage("That email is already registered."),
                ContentType.Text.Html
            )
            return@post
        }

        val userId = transaction {
            UsersTable.insert {
                it[name] = enteredName
                it[UsersTable.email] = email
                it[UsersTable.password] = password
            }[UsersTable.id]
        }

        call.sessions.set(UserSession(userId, email))
        call.respondRedirect("/dashboard")
    }

    get("/dashboard") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        call.respondText(
            renderDashboardPage(session.email),
            ContentType.Text.Html
        )
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/login")
    }
}