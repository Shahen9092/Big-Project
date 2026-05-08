package org.example.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.set
import io.ktor.server.sessions.sessions
import org.example.db.tables.UsersTable
import org.example.models.UserSession
import org.example.pages.renderDashboardPage
import org.example.pages.renderHomePage
import org.example.pages.renderLoginPage
import org.example.pages.renderProgressPage
import org.example.pages.renderRegisterPage

fun Route.authRoutes() {

    get("/") {
        call.respondText(renderHomePage(), ContentType.Text.Html)
    }

    get("/login") {
        call.respondText(renderLoginPage(), ContentType.Text.Html)
    }

    post("/login") {
        val params = call.receiveParameters()

        val username = getAuthFormText(params, "username")
        val password = getAuthPasswordText(params, "password")

        if (username == "" || password == "") {
            call.respondText(
                renderLoginPage("Please enter your username and password."),
                ContentType.Text.Html
            )
            return@post
        }

        val user = loadUserByUsername(username)

        if (user == null) {
            call.respondText(
                renderLoginPage("Invalid username or password."),
                ContentType.Text.Html
            )
            return@post
        }

        if (user[UsersTable.password] != password) {
            call.respondText(
                renderLoginPage("Invalid username or password."),
                ContentType.Text.Html
            )
            return@post
        }

        val userSession = UserSession(
            userId = user[UsersTable.id],
            username = user[UsersTable.username]
        )

        call.sessions.set(userSession)

        call.respondRedirect("/dashboard")
    }

    get("/register") {
        call.respondText(renderRegisterPage(), ContentType.Text.Html)
    }

    post("/register") {
        val params = call.receiveParameters()

        val name = getAuthFormText(params, "name")
        val surname = getAuthFormText(params, "surname")
        val username = getAuthFormText(params, "username")
        val email = getAuthFormText(params, "email")
        val password = getAuthPasswordText(params, "password")

        if (name == "" || surname == "" || username == "" || email == "" || password == "") {
            call.respondText(
                renderRegisterPage("All fields are required."),
                ContentType.Text.Html
            )
            return@post
        }

        if (username.length < 3) {
            call.respondText(
                renderRegisterPage("Username must be at least 3 characters long."),
                ContentType.Text.Html
            )
            return@post
        }

        if (username.contains(" ")) {
            call.respondText(
                renderRegisterPage("Username cannot contain spaces."),
                ContentType.Text.Html
            )
            return@post
        }

        if (password.length < 8) {
            call.respondText(
                renderRegisterPage("Password must be at least 8 characters long."),
                ContentType.Text.Html
            )
            return@post
        }

        val passwordIsValid = passwordHasCapitalAndNumber(password)

        if (!passwordIsValid) {
            call.respondText(
                renderRegisterPage("Password must include at least one capital letter and one number."),
                ContentType.Text.Html
            )
            return@post
        }

        val oldUsername = loadUserByUsername(username)

        if (oldUsername != null) {
            call.respondText(
                renderRegisterPage("That username is already taken."),
                ContentType.Text.Html
            )
            return@post
        }

        val oldEmail = loadUserByEmail(email)

        if (oldEmail != null) {
            call.respondText(
                renderRegisterPage("That email is already registered."),
                ContentType.Text.Html
            )
            return@post
        }

        val newUserId = createUser(
            name = name,
            surname = surname,
            username = username,
            email = email,
            password = password
        )

        val userSession = UserSession(
            userId = newUserId,
            username = username
        )

        call.sessions.set(userSession)

        call.respondRedirect("/dashboard")
    }

    get("/dashboard") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val pageData = loadDashboardPageData(
            userId = session.userId,
            fallbackUsername = session.username
        )

        call.respondText(
            renderDashboardPage(
                fullName = pageData.fullName,
                stats = pageData.stats
            ),
            ContentType.Text.Html
        )
    }

    get("/progress") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val selectedExerciseFromQuery = getAuthQueryText(call, "exercise")

        val pageData = loadProgressPageData(
            userId = session.userId,
            fallbackUsername = session.username,
            selectedExerciseFromQuery = selectedExerciseFromQuery
        )

        call.respondText(
            renderProgressPage(
                fullName = pageData.fullName,
                exercises = pageData.exerciseNames,
                selectedExercise = pageData.selectedExercise,
                points = pageData.points
            ),
            ContentType.Text.Html
        )
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
}