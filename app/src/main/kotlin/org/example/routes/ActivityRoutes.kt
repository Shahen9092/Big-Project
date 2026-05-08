package org.example.routes

import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
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
import org.jetbrains.exposed.sql.ResultRow
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

data class EditPageData(
    val activityId: Int,
    val exerciseName: String,
    val category: String,
    val unit: String,
    val date: String,
    val notes: String?,
    val sets: List<Double>
)

data class ExerciseSearchData(
    val exercises: List<ResultRow>,
    val categories: List<String>
)

val deletedActivityBackups = mutableMapOf<Int, DeletedActivityBackup>()

fun Route.activityRoutes() {

    get("/activities/new") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val search = getQueryText(call, "q")
        val selectedCategory = getQueryText(call, "category")

        val data = loadExerciseSearchData(search, selectedCategory)

        call.respondText(
            renderExerciseSearchPage(
                exercises = data.exercises,
                categories = data.categories,
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

        val exerciseId = getRouteInt(call, "exerciseId")
        val templateId = getQueryInt(call, "templateId")

        if (exerciseId == null) {
            call.respondRedirect("/activities/new")
            return@get
        }

        val exercise = loadExerciseById(exerciseId)

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

        val exerciseId = getRouteInt(call, "exerciseId")

        if (exerciseId == null) {
            call.respondRedirect("/activities/new")
            return@post
        }

        val params = call.receiveParameters()

        val date = getFormText(params, "date")
        val notes = getOptionalFormText(params, "notes")
        val amounts = getFormList(params, "amount")
        val templateId = getFormInt(params, "templateId")

        val exercise = loadExerciseById(exerciseId)

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

        val cleanedAmounts = cleanAmounts(amounts)

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

        val notesForDatabase = cleanNotesForDatabase(notes)

        transaction {
            val activityId = ActivitiesTable.insert {
                it[ActivitiesTable.userId] = session.userId
                it[ActivitiesTable.exerciseId] = exerciseId
                it[ActivitiesTable.date] = date
                it[ActivitiesTable.notes] = notesForDatabase
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

        val message = getQueryOptionalText(call, "msg")
        val monthFromQuery = getQueryText(call, "month")

        val allActivities = loadActivitiesForUser(session.userId)

        var selectedMonth = monthFromQuery

        if (selectedMonth == "") {
            if (allActivities.isNotEmpty()) {
                selectedMonth = getMonthKey(allActivities[0].date)
            } else {
                selectedMonth = YearMonth.now().toString()
            }
        }

        val activitiesForMonth = mutableListOf<ActivityDisplay>()

        for (activity in allActivities) {
            val activityMonth = getMonthKey(activity.date)

            if (activityMonth == selectedMonth) {
                activitiesForMonth.add(activity)
            }
        }

        val previousMonth = shiftMonth(selectedMonth, -1)
        val nextMonth = shiftMonth(selectedMonth, 1)

        var canUndoDelete = false

        if (message == "deleted") {
            if (deletedActivityBackups.containsKey(session.userId)) {
                canUndoDelete = true
            }
        }

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

        val activityId = getRouteInt(call, "activityId")

        if (activityId == null) {
            call.respondRedirect("/activities")
            return@get
        }

        val pageData = loadEditPageData(activityId, session.userId)

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

        val activityId = getRouteInt(call, "activityId")

        if (activityId == null) {
            call.respondRedirect("/activities")
            return@post
        }

        val params = call.receiveParameters()

        val date = getFormText(params, "date")
        val notes = getOptionalFormText(params, "notes")
        val amounts = getFormList(params, "amount")

        val pageData = loadEditPageData(activityId, session.userId)

        if (pageData == null) {
            call.respondRedirect("/activities")
            return@post
        }

        val submittedAmounts = convertAmountsForDisplay(amounts)

        if (date == "") {
            call.respondText(
                renderEditActivityPage(
                    activityId = pageData.activityId,
                    exerciseName = pageData.exerciseName,
                    category = pageData.category,
                    unit = pageData.unit,
                    date = pageData.date,
                    notes = notes,
                    sets = submittedAmounts,
                    error = "Please enter a date."
                ),
                ContentType.Text.Html
            )
            return@post
        }

        val cleanedAmounts = cleanAmounts(amounts)

        if (cleanedAmounts.isEmpty()) {
            call.respondText(
                renderEditActivityPage(
                    activityId = pageData.activityId,
                    exerciseName = pageData.exerciseName,
                    category = pageData.category,
                    unit = pageData.unit,
                    date = date,
                    notes = notes,
                    sets = submittedAmounts,
                    error = "Please add at least one valid set."
                ),
                ContentType.Text.Html
            )
            return@post
        }

        val notesForDatabase = cleanNotesForDatabase(notes)

        transaction {
            ActivitiesTable.update({ ActivitiesTable.id eq activityId }) {
                it[ActivitiesTable.date] = date
                it[ActivitiesTable.notes] = notesForDatabase
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

        val activityId = getRouteInt(call, "activityId")

        if (activityId == null) {
            call.respondRedirect("/activities")
            return@post
        }

        val backup = deleteActivityAndCreateBackup(activityId, session.userId)

        if (backup == null) {
            call.respondRedirect("/activities")
            return@post
        }

        deletedActivityBackups[session.userId] = backup

        call.respondRedirect("/activities?month=${getMonthKey(backup.date)}&msg=deleted")
    }
}

fun getRouteInt(call: ApplicationCall, name: String): Int? {

    val text = call.parameters[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getQueryText(call: ApplicationCall, name: String): String {

    var result = ""

    val text = call.request.queryParameters[name]

    if (text != null) {
        result = text.trim()
    }

    return result
}

fun getQueryOptionalText(call: ApplicationCall, name: String): String? {

    val text = call.request.queryParameters[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getQueryInt(call: ApplicationCall, name: String): Int? {

    val text = call.request.queryParameters[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getFormText(params: Parameters, name: String): String {

    var result = ""

    val text = params[name]

    if (text != null) {
        result = text.trim()
    }

    return result
}

fun getOptionalFormText(params: Parameters, name: String): String? {

    val text = params[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getFormInt(params: Parameters, name: String): Int? {

    val text = params[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getFormList(params: Parameters, name: String): List<String> {

    val values = params.getAll(name)

    if (values == null) {
        return emptyList()
    }

    return values
}

fun cleanNotesForDatabase(notes: String?): String? {

    if (notes == null) {
        return null
    }

    val trimmedNotes = notes.trim()

    if (trimmedNotes == "") {
        return null
    }

    return trimmedNotes
}

fun cleanAmounts(amounts: List<String>): List<Double> {

    val cleanedAmounts = mutableListOf<Double>()

    for (amountText in amounts) {
        val value = amountText.toDoubleOrNull()

        if (value != null) {
            if (value > 0) {
                cleanedAmounts.add(value)
            }
        }
    }

    return cleanedAmounts
}

fun convertAmountsForDisplay(amounts: List<String>): List<Double> {

    val displayAmounts = mutableListOf<Double>()

    for (amountText in amounts) {
        val value = amountText.toDoubleOrNull()

        if (value != null) {
            displayAmounts.add(value)
        }
    }

    return displayAmounts
}

fun loadExerciseById(exerciseId: Int): ResultRow? {

    return transaction {
        val exerciseQuery = ExercisesTable
            .selectAll()
            .where { ExercisesTable.id eq exerciseId }

        val exercise = exerciseQuery.singleOrNull()

        exercise
    }
}

fun loadExerciseSearchData(search: String, selectedCategory: String): ExerciseSearchData {

    return transaction {
        val allExercises = ExercisesTable
            .selectAll()
            .orderBy(ExercisesTable.name, SortOrder.ASC)
            .toList()

        val categories = mutableListOf<String>()

        for (exercise in allExercises) {
            val category = exercise[ExercisesTable.category]

            if (!categories.contains(category)) {
                categories.add(category)
            }
        }

        categories.sort()

        val filteredExercises = mutableListOf<ResultRow>()
        val lowerSearch = search.lowercase()

        for (exercise in allExercises) {
            val name = exercise[ExercisesTable.name].lowercase()
            val category = exercise[ExercisesTable.category].lowercase()

            var notes = ""

            try {
                val notesFromDatabase = exercise[ExercisesTable.notes]

                if (notesFromDatabase != null) {
                    notes = notesFromDatabase.lowercase()
                }
            } catch (e: Exception) {
                notes = ""
            }

            var matchesSearch = false

            if (lowerSearch == "") {
                matchesSearch = true
            }

            if (name.contains(lowerSearch)) {
                matchesSearch = true
            }

            if (category.contains(lowerSearch)) {
                matchesSearch = true
            }

            if (notes.contains(lowerSearch)) {
                matchesSearch = true
            }

            var matchesCategory = false

            if (selectedCategory == "") {
                matchesCategory = true
            }

            if (exercise[ExercisesTable.category] == selectedCategory) {
                matchesCategory = true
            }

            if (matchesSearch) {
                if (matchesCategory) {
                    filteredExercises.add(exercise)
                }
            }
        }

        ExerciseSearchData(
            exercises = filteredExercises,
            categories = categories
        )
    }
}

fun loadEditPageData(activityId: Int, userId: Int): EditPageData? {

    return transaction {
        var pageData: EditPageData? = null

        val activityQuery = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.id eq activityId }

        val activity = activityQuery.singleOrNull()

        if (activity != null) {
            if (activity[ActivitiesTable.userId] == userId) {

                val exerciseId = activity[ActivitiesTable.exerciseId]

                val exerciseQuery = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq exerciseId }

                val exercise = exerciseQuery.singleOrNull()

                if (exercise != null) {
                    val sets = loadSetAmountsForActivity(activityId)

                    pageData = EditPageData(
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

        pageData
    }
}

fun loadSetAmountsForActivity(activityId: Int): List<Double> {

    val setRows = ActivitySetsTable
        .selectAll()
        .where { ActivitySetsTable.activityId eq activityId }
        .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
        .toList()

    val sets = mutableListOf<Double>()

    for (setRow in setRows) {
        sets.add(setRow[ActivitySetsTable.amount])
    }

    return sets
}

fun deleteActivityAndCreateBackup(activityId: Int, userId: Int): DeletedActivityBackup? {

    return transaction {
        var backupData: DeletedActivityBackup? = null

        val activityQuery = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.id eq activityId }

        val activity = activityQuery.singleOrNull()

        if (activity != null) {
            if (activity[ActivitiesTable.userId] == userId) {

                val setRows = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activityId }
                    .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                    .toList()

                val setBackups = mutableListOf<DeletedActivitySetBackup>()

                for (setRow in setRows) {
                    val backupSet = DeletedActivitySetBackup(
                        setNumber = setRow[ActivitySetsTable.setNumber],
                        amount = setRow[ActivitySetsTable.amount]
                    )

                    setBackups.add(backupSet)
                }

                backupData = DeletedActivityBackup(
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
            }
        }

        backupData
    }
}

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
            val exerciseId = activity[ActivitiesTable.exerciseId]

            val exerciseQuery = ExercisesTable
                .selectAll()
                .where { ExercisesTable.id eq exerciseId }

            val exercise = exerciseQuery.singleOrNull()

            if (exercise != null) {
                val activityId = activity[ActivitiesTable.id]
                val sets = loadSetAmountsForActivity(activityId)

                val displayActivity = ActivityDisplay(
                    activityId = activityId,
                    exerciseName = exercise[ExercisesTable.name],
                    category = exercise[ExercisesTable.category],
                    unit = exercise[ExercisesTable.defaultUnit],
                    date = activity[ActivitiesTable.date],
                    notes = activity[ActivitiesTable.notes],
                    sets = sets
                )

                activities.add(displayActivity)
            }
        }

        activities
    }
}

fun shiftMonth(monthKey: String, offset: Long): String {

    var shiftedMonth = ""

    try {
        shiftedMonth = YearMonth.parse(monthKey).plusMonths(offset).toString()
    } catch (e: Exception) {
        shiftedMonth = YearMonth.now().plusMonths(offset).toString()
    }

    return shiftedMonth
}