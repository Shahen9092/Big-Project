package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.example.models.UserSession
import org.example.pages.TemplateExerciseItem
import org.example.pages.TemplateSummary
import org.example.pages.renderTemplateDetailPage
import org.example.pages.renderTemplatesPage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

fun Route.templateRoutes() {

    get("/templates") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = call.request.queryParameters["msg"]
        val error = call.request.queryParameters["error"]

        val pageData = transaction {
            val templates = loadTemplates(session.userId)
            val exercises = loadAllExercisesForTemplates()

            Pair(templates, exercises)
        }

        call.respondText(
            renderTemplatesPage(
                templates = pageData.first,
                exercises = pageData.second,
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

        val name = params["name"]?.trim().orEmpty()
        val description = params["description"]?.trim()
        val exerciseIds = params.getAll("exerciseId")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

        if (name == "") {
            call.respondRedirect("/templates?error=name")
            return@post
        }

        if (exerciseIds.isEmpty()) {
            call.respondRedirect("/templates?error=exercises")
            return@post
        }

        transaction {
            val templateId = WorkoutTemplatesTable.insert {
                it[WorkoutTemplatesTable.userId] = session.userId
                it[WorkoutTemplatesTable.name] = name
                it[WorkoutTemplatesTable.description] = if (description.isNullOrBlank()) null else description
            }[WorkoutTemplatesTable.id]

            for (exerciseId in exerciseIds) {
                WorkoutTemplateExercisesTable.insert {
                    it[WorkoutTemplateExercisesTable.templateId] = templateId
                    it[WorkoutTemplateExercisesTable.exerciseId] = exerciseId
                }
            }
        }

        call.respondRedirect("/templates?msg=created")
    }

    get("/templates/{templateId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val templateId = call.parameters["templateId"]?.toIntOrNull()

        if (templateId == null) {
            call.respondRedirect("/templates")
            return@get
        }

        val pageData = transaction {
            val templateRow = WorkoutTemplatesTable
                .selectAll()
                .where { WorkoutTemplatesTable.id eq templateId }
                .singleOrNull()

            if (templateRow == null || templateRow[WorkoutTemplatesTable.userId] != session.userId) {
                null
            } else {
                val exerciseLinks = WorkoutTemplateExercisesTable
                    .selectAll()
                    .where { WorkoutTemplateExercisesTable.templateId eq templateId }
                    .toList()

                val exercises = mutableListOf<TemplateExerciseItem>()

                for (link in exerciseLinks) {
                    val exercise = ExercisesTable
                        .selectAll()
                        .where { ExercisesTable.id eq link[WorkoutTemplateExercisesTable.exerciseId] }
                        .singleOrNull()

                    if (exercise != null) {
                        exercises.add(
                            TemplateExerciseItem(
                                id = exercise[ExercisesTable.id],
                                name = exercise[ExercisesTable.name],
                                category = exercise[ExercisesTable.category],
                                unit = exercise[ExercisesTable.defaultUnit]
                            )
                        )
                    }
                }

                val summary = TemplateSummary(
                    id = templateRow[WorkoutTemplatesTable.id],
                    name = templateRow[WorkoutTemplatesTable.name],
                    description = templateRow[WorkoutTemplatesTable.description],
                    exerciseCount = exercises.size
                )

                Pair(summary, exercises)
            }
        }

        if (pageData == null) {
            call.respondRedirect("/templates")
            return@get
        }

        call.respondText(
            renderTemplateDetailPage(
                template = pageData.first,
                exercises = pageData.second,
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

        val templateId = call.parameters["templateId"]?.toIntOrNull()
        val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()

        if (templateId == null || exerciseId == null) {
            call.respondText(
                "Invalid template or exercise.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val params = call.receiveParameters()

        val date = params["date"]?.trim().orEmpty()
        val notes = params["notes"]?.trim()
        val amountInputs = params.getAll("amount") ?: emptyList()

        if (date == "") {
            call.respondText(
                "Please enter a date.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val amounts = mutableListOf<Double>()

        for (amountText in amountInputs) {
            val amount = amountText.toDoubleOrNull()

            if (amount != null && amount > 0) {
                amounts.add(amount)
            }
        }

        if (amounts.isEmpty()) {
            call.respondText(
                "Please enter at least one valid set amount.",
                ContentType.Text.Plain,
                HttpStatusCode.BadRequest
            )
            return@post
        }

        val allowed = transaction {
            val template = WorkoutTemplatesTable
                .selectAll()
                .where { WorkoutTemplatesTable.id eq templateId }
                .singleOrNull()

            val link = WorkoutTemplateExercisesTable
                .selectAll()
                .where {
                    (WorkoutTemplateExercisesTable.templateId eq templateId) and
                            (WorkoutTemplateExercisesTable.exerciseId eq exerciseId)
                }
                .singleOrNull()

            template != null &&
                    template[WorkoutTemplatesTable.userId] == session.userId &&
                    link != null
        }

        if (!allowed) {
            call.respondText(
                "You cannot log this exercise from this template.",
                ContentType.Text.Plain,
                HttpStatusCode.Forbidden
            )
            return@post
        }

        transaction {
            val activityId = ActivitiesTable.insert {
                it[ActivitiesTable.userId] = session.userId
                it[ActivitiesTable.exerciseId] = exerciseId
                it[ActivitiesTable.date] = date
                it[ActivitiesTable.notes] = if (notes.isNullOrBlank()) null else notes
            }[ActivitiesTable.id]

            var setNumber = 1

            for (amount in amounts) {
                ActivitySetsTable.insert {
                    it[ActivitySetsTable.activityId] = activityId
                    it[ActivitySetsTable.setNumber] = setNumber
                    it[ActivitySetsTable.amount] = amount
                }

                setNumber = setNumber + 1
            }
        }

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

        val templateId = call.parameters["templateId"]?.toIntOrNull()

        if (templateId == null) {
            call.respondRedirect("/templates")
            return@post
        }

        transaction {
            val template = WorkoutTemplatesTable
                .selectAll()
                .where { WorkoutTemplatesTable.id eq templateId }
                .singleOrNull()

            if (template != null && template[WorkoutTemplatesTable.userId] == session.userId) {
                WorkoutTemplateExercisesTable.deleteWhere {
                    WorkoutTemplateExercisesTable.templateId eq templateId
                }

                WorkoutTemplatesTable.deleteWhere {
                    WorkoutTemplatesTable.id eq templateId
                }
            }
        }

        call.respondRedirect("/templates?msg=deleted")
    }
}

fun loadTemplates(userId: Int): List<TemplateSummary> {
    val templateRows = WorkoutTemplatesTable
        .selectAll()
        .where { WorkoutTemplatesTable.userId eq userId }
        .orderBy(WorkoutTemplatesTable.id, SortOrder.DESC)
        .toList()

    val list = mutableListOf<TemplateSummary>()

    for (template in templateRows) {
        val count = WorkoutTemplateExercisesTable
            .selectAll()
            .where { WorkoutTemplateExercisesTable.templateId eq template[WorkoutTemplatesTable.id] }
            .count()
            .toInt()

        list.add(
            TemplateSummary(
                id = template[WorkoutTemplatesTable.id],
                name = template[WorkoutTemplatesTable.name],
                description = template[WorkoutTemplatesTable.description],
                exerciseCount = count
            )
        )
    }

    return list
}

fun loadAllExercisesForTemplates(): List<TemplateExerciseItem> {
    val rows = ExercisesTable
        .selectAll()
        .orderBy(ExercisesTable.category, SortOrder.ASC)
        .orderBy(ExercisesTable.name, SortOrder.ASC)
        .toList()

    val list = mutableListOf<TemplateExerciseItem>()

    for (exercise in rows) {
        list.add(
            TemplateExerciseItem(
                id = exercise[ExercisesTable.id],
                name = exercise[ExercisesTable.name],
                category = exercise[ExercisesTable.category],
                unit = exercise[ExercisesTable.defaultUnit]
            )
        )
    }

    return list
}