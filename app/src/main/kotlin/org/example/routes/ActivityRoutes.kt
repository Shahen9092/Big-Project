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
import org.example.models.UserSession
import org.example.pages.ActivityDisplay
import org.example.pages.getMonthKey
import org.example.pages.renderActivitiesPage
import org.example.pages.renderEditActivityPage
import org.example.pages.renderExerciseSearchPage
import org.example.pages.renderLogExercisePage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.YearMonth

fun Route.activityRoutes() {

    get("/activities/new") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val searchText = call.request.queryParameters["q"]?.trim().orEmpty()
        val selectedCategory = call.request.queryParameters["category"]?.trim().orEmpty()

        val pageData = transaction {
            val exercises = ExercisesTable
                .selectAll()
                .orderBy(ExercisesTable.name, SortOrder.ASC)
                .toList()

            val categories = exercises
                .map { it[ExercisesTable.category] }
                .distinct()
                .sorted()

            val filteredExercises = mutableListOf<org.jetbrains.exposed.sql.ResultRow>()
            val searchLower = searchText.lowercase()

            for (exercise in exercises) {
                val name = exercise[ExercisesTable.name].lowercase()
                val category = exercise[ExercisesTable.category].lowercase()

                val notes = try {
                    (exercise[ExercisesTable.notes] ?: "").lowercase()
                } catch (e: Exception) {
                    ""
                }

                val searchMatches =
                    searchLower == "" ||
                            name.contains(searchLower) ||
                            category.contains(searchLower) ||
                            notes.contains(searchLower)

                val categoryMatches =
                    selectedCategory == "" ||
                            exercise[ExercisesTable.category] == selectedCategory

                if (searchMatches && categoryMatches) {
                    filteredExercises.add(exercise)
                }
            }

            Pair(filteredExercises, categories)
        }

        call.respondText(
            renderExerciseSearchPage(
                exercises = pageData.first,
                categories = pageData.second,
                selectedCategory = selectedCategory,
                search = searchText
            ),
            ContentType.Text.Html
        )
    }

    get("/activities/new/{exerciseId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()
        val templateId = call.request.queryParameters["templateId"]?.toIntOrNull()

        if (exerciseId == null) {
            call.respondRedirect("/activities/new")
            return@get
        }

        val exercise = transaction {
            ExercisesTable
                .selectAll()
                .where { ExercisesTable.id eq exerciseId }
                .singleOrNull()
        }

        if (exercise == null) {
            call.respondRedirect("/activities/new")
            return@get
        }

        call.respondText(
            renderLogExercisePage(
                exercise = exercise,
                today = LocalDate.now().toString(),
                templateId = templateId
            ),
            ContentType.Text.Html
        )
    }

    post("/activities/new/{exerciseId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val exerciseId = call.parameters["exerciseId"]?.toIntOrNull()

        if (exerciseId == null) {
            call.respondRedirect("/activities/new")
            return@post
        }

        val params = call.receiveParameters()

        val date = params["date"]?.trim().orEmpty()
        val notes = params["notes"]?.trim()
        val amounts = params.getAll("amount") ?: emptyList()
        val templateId = params["templateId"]?.toIntOrNull()

        val exercise = transaction {
            ExercisesTable
                .selectAll()
                .where { ExercisesTable.id eq exerciseId }
                .singleOrNull()
        }

        if (exercise == null) {
            call.respondRedirect("/activities/new")
            return@post
        }

        if (date == "") {
            call.respondText(
                renderLogExercisePage(
                    exercise = exercise,
                    today = LocalDate.now().toString(),
                    error = "Please enter a date.",
                    templateId = templateId
                ),
                ContentType.Text.Html
            )
            return@post
        }

        val validAmounts = mutableListOf<Double>()

        for (amount in amounts) {
            val amountValue = amount.toDoubleOrNull()

            if (amountValue != null && amountValue > 0) {
                validAmounts.add(amountValue)
            }
        }

        if (validAmounts.isEmpty()) {
            call.respondText(
                renderLogExercisePage(
                    exercise = exercise,
                    today = date,
                    error = "Please add at least one valid set.",
                    templateId = templateId
                ),
                ContentType.Text.Html
            )
            return@post
        }

        transaction {
            val newActivityId = ActivitiesTable.insert {
                it[ActivitiesTable.userId] = session.userId
                it[ActivitiesTable.exerciseId] = exerciseId
                it[ActivitiesTable.date] = date
                it[ActivitiesTable.notes] = if (notes.isNullOrBlank()) null else notes
            }[ActivitiesTable.id]

            var setNumber = 1

            for (amount in validAmounts) {
                ActivitySetsTable.insert {
                    it[ActivitySetsTable.activityId] = newActivityId
                    it[ActivitySetsTable.setNumber] = setNumber
                    it[ActivitySetsTable.amount] = amount
                }

                setNumber = setNumber + 1
            }
        }

        call.respondRedirect("/activities?month=${getMonthKey(date)}&msg=saved")
    }

    get("/activities") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = call.request.queryParameters["msg"]
        val monthQuery = call.request.queryParameters["month"]?.trim().orEmpty()

        val allActivities = loadActivitiesForUser(session.userId)

        var selectedMonth = monthQuery

        if (selectedMonth == "") {
            selectedMonth = if (allActivities.isNotEmpty()) {
                getMonthKey(allActivities[0].date)
            } else {
                YearMonth.now().toString()
            }
        }

        val activitiesForMonth = mutableListOf<ActivityDisplay>()

        for (activity in allActivities) {
            if (getMonthKey(activity.date) == selectedMonth) {
                activitiesForMonth.add(activity)
            }
        }

        val previousMonth = shiftMonth(selectedMonth, -1)
        val nextMonth = shiftMonth(selectedMonth, 1)

        call.respondText(
            renderActivitiesPage(
                activities = activitiesForMonth,
                selectedMonth = selectedMonth,
                previousMonth = previousMonth,
                nextMonth = nextMonth,
                message = message
            ),
            ContentType.Text.Html
        )
    }

    get("/activities/edit/{activityId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val activityId = call.parameters["activityId"]?.toIntOrNull()

        if (activityId == null) {
            call.respondRedirect("/activities")
            return@get
        }

        val pageData = transaction {
            val activity = ActivitiesTable
                .selectAll()
                .where { ActivitiesTable.id eq activityId }
                .singleOrNull()

            if (activity == null) {
                null
            } else if (activity[ActivitiesTable.userId] != session.userId) {
                null
            } else {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                    .singleOrNull()

                if (exercise == null) {
                    null
                } else {
                    val setRows = ActivitySetsTable
                        .selectAll()
                        .where { ActivitySetsTable.activityId eq activityId }
                        .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                        .toList()

                    val sets = mutableListOf<Double>()

                    for (setRow in setRows) {
                        sets.add(setRow[ActivitySetsTable.amount])
                    }

                    EditPageData(
                        activityId = activityId,
                        exerciseName = exercise[ExercisesTable.name],
                        category = exercise[ExercisesTable.category],
                        unit = exercise[ExercisesTable.defaultUnit],
                        date = activity[ActivitiesTable.date],
                        notes = activity[ActivitiesTable.notes],
                        sets = sets
                    )
                }
            }
        }

        if (pageData == null) {
            call.respondRedirect("/activities")
            return@get
        }

        call.respondText(
            renderEditActivityPage(
                activityId = pageData.activityId,
                exerciseName = pageData.exerciseName,
                category = pageData.category,
                unit = pageData.unit,
                date = pageData.date,
                notes = pageData.notes,
                sets = pageData.sets
            ),
            ContentType.Text.Html
        )
    }

    post("/activities/edit/{activityId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val activityId = call.parameters["activityId"]?.toIntOrNull()

        if (activityId == null) {
            call.respondRedirect("/activities")
            return@post
        }

        val params = call.receiveParameters()

        val date = params["date"]?.trim().orEmpty()
        val notes = params["notes"]?.trim()
        val amounts = params.getAll("amount") ?: emptyList()

        val pageData = transaction {
            val activity = ActivitiesTable
                .selectAll()
                .where { ActivitiesTable.id eq activityId }
                .singleOrNull()

            if (activity == null) {
                null
            } else if (activity[ActivitiesTable.userId] != session.userId) {
                null
            } else {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                    .singleOrNull()

                if (exercise == null) {
                    null
                } else {
                    val postedSets = mutableListOf<Double>()

                    for (amount in amounts) {
                        val amountValue = amount.toDoubleOrNull()

                        if (amountValue != null) {
                            postedSets.add(amountValue)
                        }
                    }

                    EditPageData(
                        activityId = activityId,
                        exerciseName = exercise[ExercisesTable.name],
                        category = exercise[ExercisesTable.category],
                        unit = exercise[ExercisesTable.defaultUnit],
                        date = activity[ActivitiesTable.date],
                        notes = activity[ActivitiesTable.notes],
                        sets = postedSets
                    )
                }
            }
        }

        if (pageData == null) {
            call.respondRedirect("/activities")
            return@post
        }

        if (date == "") {
            call.respondText(
                renderEditActivityPage(
                    activityId = pageData.activityId,
                    exerciseName = pageData.exerciseName,
                    category = pageData.category,
                    unit = pageData.unit,
                    date = pageData.date,
                    notes = notes,
                    sets = pageData.sets,
                    error = "Please enter a date."
                ),
                ContentType.Text.Html
            )
            return@post
        }

        val validAmounts = mutableListOf<Double>()

        for (amount in amounts) {
            val amountValue = amount.toDoubleOrNull()

            if (amountValue != null && amountValue > 0) {
                validAmounts.add(amountValue)
            }
        }

        if (validAmounts.isEmpty()) {
            call.respondText(
                renderEditActivityPage(
                    activityId = pageData.activityId,
                    exerciseName = pageData.exerciseName,
                    category = pageData.category,
                    unit = pageData.unit,
                    date = date,
                    notes = notes,
                    sets = pageData.sets,
                    error = "Please add at least one valid set."
                ),
                ContentType.Text.Html
            )
            return@post
        }

        transaction {
            ActivitiesTable.update({ ActivitiesTable.id eq activityId }) {
                it[ActivitiesTable.date] = date
                it[ActivitiesTable.notes] = if (notes.isNullOrBlank()) null else notes
            }

            ActivitySetsTable.deleteWhere {
                ActivitySetsTable.activityId eq activityId
            }

            var setNumber = 1

            for (amount in validAmounts) {
                ActivitySetsTable.insert {
                    it[ActivitySetsTable.activityId] = activityId
                    it[ActivitySetsTable.setNumber] = setNumber
                    it[ActivitySetsTable.amount] = amount
                }

                setNumber = setNumber + 1
            }
        }

        call.respondRedirect("/activities?month=${getMonthKey(date)}&msg=updated")
    }

    post("/activities/delete/{activityId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val activityId = call.parameters["activityId"]?.toIntOrNull()

        if (activityId == null) {
            call.respondRedirect("/activities")
            return@post
        }

        val monthToReturnTo = transaction {
            val activity = ActivitiesTable
                .selectAll()
                .where { ActivitiesTable.id eq activityId }
                .singleOrNull()

            if (activity == null) {
                null
            } else if (activity[ActivitiesTable.userId] != session.userId) {
                null
            } else {
                val month = getMonthKey(activity[ActivitiesTable.date])

                ActivitySetsTable.deleteWhere {
                    ActivitySetsTable.activityId eq activityId
                }

                ActivitiesTable.deleteWhere {
                    ActivitiesTable.id eq activityId
                }

                month
            }
        }

        if (monthToReturnTo == null) {
            call.respondRedirect("/activities")
            return@post
        }

        call.respondRedirect("/activities?month=$monthToReturnTo&msg=deleted")
    }
}

data class EditPageData(
    val activityId: Int,
    val exerciseName: String,
    val category: String,
    val unit: String,
    val date: String,
    val notes: String?,
    val sets: List<Double>
)

fun loadActivitiesForUser(userId: Int): List<ActivityDisplay> {
    return transaction {
        val rows = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.userId eq userId }
            .orderBy(ActivitiesTable.date, SortOrder.DESC)
            .orderBy(ActivitiesTable.id, SortOrder.DESC)
            .toList()

        val activities = mutableListOf<ActivityDisplay>()

        for (activity in rows) {
            val exercise = ExercisesTable
                .selectAll()
                .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                .singleOrNull()

            if (exercise != null) {
                val setRows = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                    .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                    .toList()

                val sets = mutableListOf<Double>()

                for (setRow in setRows) {
                    sets.add(setRow[ActivitySetsTable.amount])
                }

                val activityDisplay = ActivityDisplay(
                    activityId = activity[ActivitiesTable.id],
                    exerciseName = exercise[ExercisesTable.name],
                    category = exercise[ExercisesTable.category],
                    unit = exercise[ExercisesTable.defaultUnit],
                    date = activity[ActivitiesTable.date],
                    notes = activity[ActivitiesTable.notes],
                    sets = sets
                )

                activities.add(activityDisplay)
            }
        }

        activities
    }
}

fun shiftMonth(monthKey: String, offset: Long): String {
    return try {
        val month = YearMonth.parse(monthKey)
        month.plusMonths(offset).toString()
    } catch (e: Exception) {
        YearMonth.now().plusMonths(offset).toString()
    }
}