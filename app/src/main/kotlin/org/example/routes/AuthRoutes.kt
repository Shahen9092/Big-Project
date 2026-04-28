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
import org.example.db.tables.UsersTable
import org.example.models.UserSession
import org.example.pages.DashboardStats
import org.example.pages.PersonalRecord
import org.example.pages.ProgressPoint
import org.example.pages.renderDashboardPage
import org.example.pages.renderHomePage
import org.example.pages.renderLoginPage
import org.example.pages.renderProgressPage
import org.example.pages.renderRegisterPage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.authRoutes() {

    get("/") {
        call.respondText(renderHomePage(), ContentType.Text.Html)
    }

    get("/login") {
        call.respondText(renderLoginPage(), ContentType.Text.Html)
    }

    post("/login") {
        val params = call.receiveParameters()

        val username = params["username"]?.trim().orEmpty()
        val password = params["password"].orEmpty()

        if (username == "" || password == "") {
            call.respondText(
                renderLoginPage("Please enter your username and password."),
                ContentType.Text.Html
            )
            return@post
        }

        val user = transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username }
                .singleOrNull()
        }

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

        call.sessions.set(
            UserSession(
                user[UsersTable.id],
                user[UsersTable.username]
            )
        )

        call.respondRedirect("/dashboard")
    }

    get("/register") {
        call.respondText(renderRegisterPage(), ContentType.Text.Html)
    }

    post("/register") {
        val params = call.receiveParameters()

        val name = params["name"]?.trim().orEmpty()
        val surname = params["surname"]?.trim().orEmpty()
        val username = params["username"]?.trim().orEmpty()
        val email = params["email"]?.trim().orEmpty()
        val password = params["password"].orEmpty()

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

        var hasCapital = false
        var hasNumber = false

        for (letter in password) {
            if (letter.isUpperCase()) {
                hasCapital = true
            }

            if (letter.isDigit()) {
                hasNumber = true
            }
        }

        if (!hasCapital || !hasNumber) {
            call.respondText(
                renderRegisterPage("Password must include at least one capital letter and one number."),
                ContentType.Text.Html
            )
            return@post
        }

        val oldUsername = transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username }
                .singleOrNull()
        }

        if (oldUsername != null) {
            call.respondText(
                renderRegisterPage("That username is already taken."),
                ContentType.Text.Html
            )
            return@post
        }

        val oldEmail = transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
        }

        if (oldEmail != null) {
            call.respondText(
                renderRegisterPage("That email is already registered."),
                ContentType.Text.Html
            )
            return@post
        }

        val newUserId = transaction {
            UsersTable.insert {
                it[UsersTable.name] = name
                it[UsersTable.surname] = surname
                it[UsersTable.username] = username
                it[UsersTable.email] = email
                it[UsersTable.password] = password
            }[UsersTable.id]
        }

        call.sessions.set(
            UserSession(
                newUserId,
                username
            )
        )

        call.respondRedirect("/dashboard")
    }

    get("/dashboard") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val pageData = transaction {
            val user = UsersTable
                .selectAll()
                .where { UsersTable.id eq session.userId }
                .singleOrNull()

            var fullName = session.username

            if (user != null) {
                val firstName = user[UsersTable.name]
                val surname = user[UsersTable.surname] ?: ""
                fullName = "$firstName $surname".trim()
            }

            val activityRows = ActivitiesTable
                .selectAll()
                .where { ActivitiesTable.userId eq session.userId }
                .orderBy(ActivitiesTable.date, SortOrder.DESC)
                .orderBy(ActivitiesTable.id, SortOrder.DESC)
                .toList()

            var totalSets = 0
            val categoryCount = mutableMapOf<String, Int>()
            val records = mutableMapOf<String, PersonalRecord>()

            var lastActivity = "No activity logged yet."

            for (activity in activityRows) {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                    .singleOrNull()

                if (exercise != null) {
                    val exerciseName = exercise[ExercisesTable.name]
                    val category = exercise[ExercisesTable.category]
                    val unit = exercise[ExercisesTable.defaultUnit]
                    val date = activity[ActivitiesTable.date]

                    if (lastActivity == "No activity logged yet.") {
                        lastActivity = "$exerciseName on $date"
                    }

                    categoryCount[category] = (categoryCount[category] ?: 0) + 1

                    val sets = ActivitySetsTable
                        .selectAll()
                        .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                        .toList()

                    totalSets += sets.size

                    for (set in sets) {
                        val amount = set[ActivitySetsTable.amount]
                        val oldRecord = records[exerciseName]

                        if (oldRecord == null || amount > oldRecord.amount) {
                            records[exerciseName] = PersonalRecord(
                                exerciseName = exerciseName,
                                amount = amount,
                                unit = unit
                            )
                        }
                    }
                }
            }

            var mostTrainedCategory = "None yet"

            if (categoryCount.isNotEmpty()) {
                mostTrainedCategory = categoryCount.maxByOrNull { it.value }!!.key
            }

            val recordList = records.values
                .sortedByDescending { it.amount }
                .take(5)

            val stats = DashboardStats(
                totalActivities = activityRows.size,
                totalSets = totalSets,
                mostTrainedCategory = mostTrainedCategory,
                lastActivity = lastActivity,
                personalRecords = recordList
            )

            Pair(fullName, stats)
        }

        call.respondText(
            renderDashboardPage(pageData.first, pageData.second),
            ContentType.Text.Html
        )
    }

    get("/progress") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val selectedExerciseFromQuery = call.request.queryParameters["exercise"]?.trim().orEmpty()

        val pageData = transaction {
            val user = UsersTable
                .selectAll()
                .where { UsersTable.id eq session.userId }
                .singleOrNull()

            var fullName = session.username

            if (user != null) {
                val firstName = user[UsersTable.name]
                val surname = user[UsersTable.surname] ?: ""
                fullName = "$firstName $surname".trim()
            }

            val activityRows = ActivitiesTable
                .selectAll()
                .where { ActivitiesTable.userId eq session.userId }
                .orderBy(ActivitiesTable.date, SortOrder.ASC)
                .orderBy(ActivitiesTable.id, SortOrder.ASC)
                .toList()

            val exerciseNames = mutableListOf<String>()

            for (activity in activityRows) {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                    .singleOrNull()

                if (exercise != null) {
                    val exerciseName = exercise[ExercisesTable.name]

                    if (!exerciseNames.contains(exerciseName)) {
                        exerciseNames.add(exerciseName)
                    }
                }
            }

            exerciseNames.sort()

            var selectedExercise = selectedExerciseFromQuery

            if (selectedExercise == "" && exerciseNames.isNotEmpty()) {
                selectedExercise = exerciseNames[0]
            }

            val groupedPoints = linkedMapOf<String, Double>()

            if (selectedExercise != "") {
                for (activity in activityRows) {
                    val exercise = ExercisesTable
                        .selectAll()
                        .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
                        .singleOrNull()

                    if (exercise != null && exercise[ExercisesTable.name] == selectedExercise) {
                        val date = activity[ActivitiesTable.date]

                        val sets = ActivitySetsTable
                            .selectAll()
                            .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                            .toList()

                        var bestForThisActivity = 0.0

                        for (set in sets) {
                            val amount = set[ActivitySetsTable.amount]

                            if (amount > bestForThisActivity) {
                                bestForThisActivity = amount
                            }
                        }

                        if (groupedPoints.containsKey(date)) {
                            val oldValue = groupedPoints[date] ?: 0.0

                            if (bestForThisActivity > oldValue) {
                                groupedPoints[date] = bestForThisActivity
                            }
                        } else {
                            groupedPoints[date] = bestForThisActivity
                        }
                    }
                }
            }

            val points = mutableListOf<ProgressPoint>()

            for ((date, value) in groupedPoints) {
                points.add(
                    ProgressPoint(
                        date = date,
                        value = value
                    )
                )
            }

            Triple(fullName, exerciseNames, Pair(selectedExercise, points))
        }

        val fullName = pageData.first
        val exerciseNames = pageData.second
        val selectedExercise = pageData.third.first
        val points = pageData.third.second

        call.respondText(
            renderProgressPage(
                fullName = fullName,
                exercises = exerciseNames,
                selectedExercise = selectedExercise,
                points = points
            ),
            ContentType.Text.Html
        )
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
}