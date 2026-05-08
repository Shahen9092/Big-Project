package org.example.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.models.UserSession
import org.example.pages.renderTemplateDetailPage
import org.example.pages.renderTemplatesPage
import java.time.LocalDate

fun Route.templateRoutes() {

    get("/templates") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = getTemplateQueryText(call, "msg")
        val error = getTemplateQueryText(call, "error")

        val pageData = loadTemplatesPageData(session.userId)

        call.respondText(
            renderTemplatesPage(
                templates = pageData.templates,
                exercises = pageData.exercises,
                message = message,
                error = error
            ),
            ContentType.Text.Html
        )
    }

    post("/templates/create") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()

        val name = getTemplateFormText(params, "name")
        val description = getTemplateOptionalFormText(params, "description")
        val exerciseIds = getTemplateExerciseIds(params)

        if (name == "") {
            call.respondRedirect("/templates?error=name")
            return@post
        }

        if (exerciseIds.isEmpty()) {
            call.respondRedirect("/templates?error=exercises")
            return@post
        }

        val descriptionForDatabase = cleanTemplateNotesForDatabase(description)

        createWorkoutTemplate(
            userId = session.userId,
            name = name,
            descriptionForDatabase = descriptionForDatabase,
            exerciseIds = exerciseIds
        )

        call.respondRedirect("/templates?msg=created")
    }

    get("/templates/{templateId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val templateId = getTemplateRouteInt(call, "templateId")

        if (templateId == null) {
            call.respondRedirect("/templates")
            return@get
        }

        val pageData = loadTemplateDetailPageData(
            userId = session.userId,
            templateId = templateId
        )

        if (pageData == null) {
            call.respondRedirect("/templates")
            return@get
        }

        call.respondText(
            renderTemplateDetailPage(
                template = pageData.template,
                exercises = pageData.exercises,
                today = LocalDate.now().toString()
            ),
            ContentType.Text.Html
        )
    }

    post("/templates/{templateId}/log/{exerciseId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondText(
                "You must be logged in.",
                ContentType.Text.Plain,
                HttpStatusCode.Unauthorized
            )
            return@post
        }

        val templateId = getTemplateRouteInt(call, "templateId")
        val exerciseId = getTemplateRouteInt(call, "exerciseId")

        if (templateId == null) {
            call.respondText(
                "Invalid template or exercise.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        if (exerciseId == null) {
            call.respondText(
                "Invalid template or exercise.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val params = call.receiveParameters()

        val date = getTemplateFormText(params, "date")
        val notes = getTemplateOptionalFormText(params, "notes")
        val amountInputs = getTemplateAmountInputs(params)

        if (date == "") {
            call.respondText(
                "Please enter a date.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val amounts = cleanTemplateAmounts(amountInputs)

        if (amounts.isEmpty()) {
            call.respondText(
                "Please enter at least one valid set amount.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val allowed = userCanLogExerciseFromTemplate(
            userId = session.userId,
            templateId = templateId,
            exerciseId = exerciseId
        )

        if (!allowed) {
            call.respondText(
                "You cannot log this exercise from this template.",
                ContentType.Text.Plain,
                HttpStatusCode.Forbidden
            )
            return@post
        }

        val notesForDatabase = cleanTemplateNotesForDatabase(notes)

        saveActivityFromTemplate(
            userId = session.userId,
            exerciseId = exerciseId,
            date = date,
            notesForDatabase = notesForDatabase,
            amounts = amounts
        )

        call.respondText(
            "Exercise logged.",
            ContentType.Text.Plain,
            HttpStatusCode.OK
        )
    }

    post("/templates/delete/{templateId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val templateId = getTemplateRouteInt(call, "templateId")

        if (templateId == null) {
            call.respondRedirect("/templates")
            return@post
        }

        deleteTemplateIfOwned(
            templateId = templateId,
            userId = session.userId
        )

        call.respondRedirect("/templates?msg=deleted")
    }
}