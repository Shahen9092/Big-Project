package org.example.routes

import io.ktor.http.ContentType
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
import org.example.pages.ActivityDisplay
import org.example.pages.getMonthKey
import org.example.pages.renderActivitiesPage
import org.example.pages.renderEditActivityPage
import org.example.pages.renderExerciseSearchPage
import org.example.pages.renderLogExercisePage
import java.time.LocalDate
import java.time.YearMonth

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

        saveNewActivity(
            userId = session.userId,
            exerciseId = exerciseId,
            date = date,
            notesForDatabase = notesForDatabase,
            cleanedAmounts = cleanedAmounts
        )

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

        restoreDeletedActivity(backup)

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

        updateExistingActivity(
            activityId = activityId,
            date = date,
            notesForDatabase = notesForDatabase,
            cleanedAmounts = cleanedAmounts
        )

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