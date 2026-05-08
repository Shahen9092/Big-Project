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

data class DeletedActivitySetBackup(
    val setNumber: Int,
    val amount: Double
)

data class DeletedActivityBackup(
    val userId: Int,
    val exerciseId: Int,
    val date: String,
    val notes: String?,
    val sets: List<DeletedActivitySetBackup>
)

val deletedActivityBackups = mutableMapOf<Int, DeletedActivityBackup>()

fun Route.activityRoutes() {

    get("/activities/new") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val search = call.request.queryParameters["q"]?.trim().orEmpty()
        val selectedCategory = call.request.queryParameters["category"]?.trim().orEmpty()

        val data = transaction {
            val allExercises = ExercisesTable
                .selectAll()
                .orderBy(ExercisesTable.name, SortOrder.ASC)
                .toList()

            val categories = allExercises
                .map { it[ExercisesTable.category] }
                .distinct()
                .sorted()

            val lowerSearch = search.lowercase()

            val filteredExercises = allExercises.filter { exercise ->
                val name = exercise[ExercisesTable.name].lowercase()
                val category = exercise[ExercisesTable.category].lowercase()

                val notes = try {
                    (exercise[ExercisesTable.notes] ?: "").lowercase()
                } catch (e: Exception) {
                    ""
                }

                val matchesSearch =
                    lowerSearch == "" ||
                            name.contains(lowerSearch) ||
                            category.contains(lowerSearch) ||
                            notes.contains(lowerSearch)

                val matchesCategory =
                    selectedCategory == "" ||
                            exercise[ExercisesTable.category] == selectedCategory

                matchesSearch && matchesCategory
            }

            Pair(filteredExercises, categories)
        }

        call.respondText(
            renderExerciseSearchPage(
                exercises = data.first,
                categories = data.second,
                selectedCategory = selectedCategory,
                search = search
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

        val cleanedAmounts = mutableListOf<Double>()

        for (amountText in amounts) {
            val value = amountText.toDoubleOrNull()

            if (value != null && value > 0) {
                cleanedAmounts.add(value)
            }
        }

        if (cleanedAmounts.isEmpty()) {
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
            val activityId = ActivitiesTable.insert {
                it[ActivitiesTable.userId] = session.userId
                it[ActivitiesTable.exerciseId] = exerciseId
                it[ActivitiesTable.date] = date
                it[ActivitiesTable.notes] = if (notes.isNullOrBlank()) null else notes
            }[ActivitiesTable.id]

            var setNumber = 1

            for (amount in cleanedAmounts) {
                ActivitySetsTable.insert {
                    it[ActivitySetsTable.activityId] = activityId
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
        val monthFromQuery = call.request.queryParameters["month"]?.trim().orEmpty()

        val allActivities = loadActivitiesForUser(session.userId)

        var selectedMonth = monthFromQuery

        if (selectedMonth == "") {
            if (allActivities.isNotEmpty()) {
                selectedMonth = getMonthKey(allActivities[0].date)
            } else {
                selectedMonth = YearMonth.now().toString()
            }
        }

        val activitiesForMonth = allActivities.filter {
            getMonthKey(it.date) == selectedMonth
        }

        val previousMonth = shiftMonth(selectedMonth, -1)
        val nextMonth = shiftMonth(selectedMonth, 1)

        val canUndoDelete = message == "deleted" && deletedActivityBackups.containsKey(session.userId)

        call.respondText(
            renderActivitiesPage(
                activities = activitiesForMonth,
                selectedMonth = selectedMonth,
                previousMonth = previousMonth,
                nextMonth = nextMonth,
                message = message,
                canUndoDelete = canUndoDelete
            ),
            ContentType.Text.Html
        )
    }

    post("/activities/undo-delete") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val backup = deletedActivityBackups.remove(session.userId)

        if (backup == null) {
            call.respondRedirect("/activities")
            return@post
        }

        transaction {
            val activityId = ActivitiesTable.insert {
                it[ActivitiesTable.userId] = backup.userId
                it[ActivitiesTable.exerciseId] = backup.exerciseId
                it[ActivitiesTable.date] = backup.date
                it[ActivitiesTable.notes] = backup.notes
            }[ActivitiesTable.id]

            for (set in backup.sets) {
                ActivitySetsTable.insert {
                    it[ActivitySetsTable.activityId] = activityId
                    it[ActivitySetsTable.setNumber] = set.setNumber
                    it[ActivitySetsTable.amount] = set.amount
                }
            }
        }

        call.respondRedirect("/activities?month=${getMonthKey(backup.date)}&msg=restored")
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

            if (activity == null || activity[ActivitiesTable.userId] != session.userId) {
                null
            } else {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                    .singleOrNull()

                if (exercise == null) {
                    null
                } else {
                    val sets = ActivitySetsTable
                        .selectAll()
                        .where { ActivitySetsTable.activityId eq activityId }
                        .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                        .toList()
                        .map { it[ActivitySetsTable.amount] }

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

            if (activity == null || activity[ActivitiesTable.userId] != session.userId) {
                null
            } else {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                    .singleOrNull()

                if (exercise == null) {
                    null
                } else {
                    EditPageData(
                        activityId = activityId,
                        exerciseName = exercise[ExercisesTable.name],
                        category = exercise[ExercisesTable.category],
                        unit = exercise[ExercisesTable.defaultUnit],
                        date = activity[ActivitiesTable.date],
                        notes = activity[ActivitiesTable.notes],
                        sets = amounts.mapNotNull { it.toDoubleOrNull() }
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

        val cleanedAmounts = mutableListOf<Double>()

        for (amountText in amounts) {
            val value = amountText.toDoubleOrNull()

            if (value != null && value > 0) {
                cleanedAmounts.add(value)
            }
        }

        if (cleanedAmounts.isEmpty()) {
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

            for (amount in cleanedAmounts) {
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

        val backup = transaction {
            val activity = ActivitiesTable
                .selectAll()
                .where { ActivitiesTable.id eq activityId }
                .singleOrNull()

            if (activity == null || activity[ActivitiesTable.userId] != session.userId) {
                null
            } else {
                val setRows = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activityId }
                    .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                    .toList()

                val setBackups = mutableListOf<DeletedActivitySetBackup>()

                for (setRow in setRows) {
                    setBackups.add(
                        DeletedActivitySetBackup(
                            setNumber = setRow[ActivitySetsTable.setNumber],
                            amount = setRow[ActivitySetsTable.amount]
                        )
                    )
                }

                val backupData = DeletedActivityBackup(
                    userId = activity[ActivitiesTable.userId],
                    exerciseId = activity[ActivitiesTable.exerciseId],
                    date = activity[ActivitiesTable.date],
                    notes = activity[ActivitiesTable.notes],
                    sets = setBackups
                )

                ActivitySetsTable.deleteWhere {
                    ActivitySetsTable.activityId eq activityId
                }

                ActivitiesTable.deleteWhere {
                    ActivitiesTable.id eq activityId
                }

                backupData
            }
        }

        if (backup == null) {
            call.respondRedirect("/activities")
            return@post
        }

        deletedActivityBackups[session.userId] = backup

        call.respondRedirect("/activities?month=${getMonthKey(backup.date)}&msg=deleted")
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

                activities.add(
                    ActivityDisplay(
                        activityId = activity[ActivitiesTable.id],
                        exerciseName = exercise[ExercisesTable.name],
                        category = exercise[ExercisesTable.category],
                        unit = exercise[ExercisesTable.defaultUnit],
                        date = activity[ActivitiesTable.date],
                        notes = activity[ActivitiesTable.notes],
                        sets = sets
                    )
                )
            }
        }

        activities
    }
}

fun shiftMonth(monthKey: String, offset: Long): String {
    return try {
        YearMonth.parse(monthKey).plusMonths(offset).toString()
    } catch (e: Exception) {
        YearMonth.now().plusMonths(offset).toString()
    }
}