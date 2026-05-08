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
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class DashboardPageData(
    val fullName: String,
    val stats: DashboardStats
)

data class ProgressPageData(
    val fullName: String,
    val exerciseNames: List<String>,
    val selectedExercise: String,
    val points: List<ProgressPoint>
)

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

fun getAuthFormText(params: Parameters, name: String): String {

    var result = ""

    val value = params[name]

    if (value != null) {
        result = value.trim()
    }

    return result
}

fun getAuthPasswordText(params: Parameters, name: String): String {

    var result = ""

    val value = params[name]

    if (value != null) {
        result = value
    }

    return result
}

fun getAuthQueryText(call: ApplicationCall, name: String): String {

    var result = ""

    val value = call.request.queryParameters[name]

    if (value != null) {
        result = value.trim()
    }

    return result
}

fun passwordHasCapitalAndNumber(password: String): Boolean {

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

    if (hasCapital && hasNumber) {
        return true
    }

    return false
}

fun loadUserByUsername(username: String): ResultRow? {

    return transaction {
        val user = UsersTable
            .selectAll()
            .where { UsersTable.username eq username }
            .singleOrNull()

        user
    }
}

fun loadUserByEmail(email: String): ResultRow? {

    return transaction {
        val user = UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()

        user
    }
}

fun createUser(
    name: String,
    surname: String,
    username: String,
    email: String,
    password: String
): Int {

    return transaction {
        val newUserId = UsersTable.insert {
            it[UsersTable.name] = name
            it[UsersTable.surname] = surname
            it[UsersTable.username] = username
            it[UsersTable.email] = email
            it[UsersTable.password] = password
        }[UsersTable.id]

        newUserId
    }
}

fun loadDashboardPageData(
    userId: Int,
    fallbackUsername: String
): DashboardPageData {

    return transaction {
        val fullName = loadFullNameInsideTransaction(userId, fallbackUsername)

        val activityRows = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.userId eq userId }
            .orderBy(ActivitiesTable.date, SortOrder.DESC)
            .orderBy(ActivitiesTable.id, SortOrder.DESC)
            .toList()

        var totalSets = 0
        val categoryCount = mutableMapOf<String, Int>()
        val records = mutableMapOf<String, PersonalRecord>()

        var lastActivity = "No activity logged yet."

        for (activity in activityRows) {
            val exerciseId = activity[ActivitiesTable.exerciseId]
            val exercise = loadExerciseInsideTransaction(exerciseId)

            if (exercise != null) {
                val exerciseName = exercise[ExercisesTable.name]
                val category = exercise[ExercisesTable.category]
                val unit = exercise[ExercisesTable.defaultUnit]
                val date = activity[ActivitiesTable.date]

                if (lastActivity == "No activity logged yet.") {
                    lastActivity = "$exerciseName on $date"
                }

                addOneToCategoryCount(categoryCount, category)

                val sets = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                    .toList()

                totalSets = totalSets + sets.size

                for (set in sets) {
                    val amount = set[ActivitySetsTable.amount]
                    val oldRecord = records[exerciseName]

                    if (oldRecord == null) {
                        records[exerciseName] = PersonalRecord(
                            exerciseName = exerciseName,
                            amount = amount,
                            unit = unit
                        )
                    } else {
                        if (amount > oldRecord.amount) {
                            records[exerciseName] = PersonalRecord(
                                exerciseName = exerciseName,
                                amount = amount,
                                unit = unit
                            )
                        }
                    }
                }
            }
        }

        val mostTrainedCategory = findMostTrainedCategory(categoryCount)
        val recordList = getTopFivePersonalRecords(records)

        val stats = DashboardStats(
            totalActivities = activityRows.size,
            totalSets = totalSets,
            mostTrainedCategory = mostTrainedCategory,
            lastActivity = lastActivity,
            personalRecords = recordList
        )

        DashboardPageData(
            fullName = fullName,
            stats = stats
        )
    }
}

fun loadProgressPageData(
    userId: Int,
    fallbackUsername: String,
    selectedExerciseFromQuery: String
): ProgressPageData {

    return transaction {
        val fullName = loadFullNameInsideTransaction(userId, fallbackUsername)

        val activityRows = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.userId eq userId }
            .orderBy(ActivitiesTable.date, SortOrder.ASC)
            .orderBy(ActivitiesTable.id, SortOrder.ASC)
            .toList()

        val exerciseNames = mutableListOf<String>()

        for (activity in activityRows) {
            val exerciseId = activity[ActivitiesTable.exerciseId]
            val exercise = loadExerciseInsideTransaction(exerciseId)

            if (exercise != null) {
                val exerciseName = exercise[ExercisesTable.name]

                if (!exerciseNames.contains(exerciseName)) {
                    exerciseNames.add(exerciseName)
                }
            }
        }

        exerciseNames.sort()

        var selectedExercise = selectedExerciseFromQuery

        if (selectedExercise == "") {
            if (exerciseNames.isNotEmpty()) {
                selectedExercise = exerciseNames[0]
            }
        }

        val groupedPoints = linkedMapOf<String, Double>()

        if (selectedExercise != "") {
            for (activity in activityRows) {
                val exerciseId = activity[ActivitiesTable.exerciseId]
                val exercise = loadExerciseInsideTransaction(exerciseId)

                if (exercise != null) {
                    val exerciseName = exercise[ExercisesTable.name]

                    if (exerciseName == selectedExercise) {
                        val date = activity[ActivitiesTable.date]
                        val activityId = activity[ActivitiesTable.id]

                        val bestForThisActivity = findBestSetForActivity(activityId)

                        if (groupedPoints.containsKey(date)) {
                            var oldValue = 0.0

                            val valueFromMap = groupedPoints[date]

                            if (valueFromMap != null) {
                                oldValue = valueFromMap
                            }

                            if (bestForThisActivity > oldValue) {
                                groupedPoints[date] = bestForThisActivity
                            }
                        } else {
                            groupedPoints[date] = bestForThisActivity
                        }
                    }
                }
            }
        }

        val points = mutableListOf<ProgressPoint>()

        for (entry in groupedPoints) {
            points.add(
                ProgressPoint(
                    date = entry.key,
                    value = entry.value
                )
            )
        }

        ProgressPageData(
            fullName = fullName,
            exerciseNames = exerciseNames,
            selectedExercise = selectedExercise,
            points = points
        )
    }
}

fun loadFullNameInsideTransaction(
    userId: Int,
    fallbackUsername: String
): String {

    var fullName = fallbackUsername

    val user = UsersTable
        .selectAll()
        .where { UsersTable.id eq userId }
        .singleOrNull()

    if (user != null) {
        val firstName = user[UsersTable.name]

        var surname = ""
        val surnameFromDatabase = user[UsersTable.surname]

        if (surnameFromDatabase != null) {
            surname = surnameFromDatabase
        }

        fullName = "$firstName $surname".trim()
    }

    return fullName
}

fun loadExerciseInsideTransaction(exerciseId: Int): ResultRow? {

    val exercise = ExercisesTable
        .selectAll()
        .where { ExercisesTable.id eq exerciseId }
        .singleOrNull()

    return exercise
}

fun addOneToCategoryCount(
    categoryCount: MutableMap<String, Int>,
    category: String
) {

    var oldCount = 0

    val countFromMap = categoryCount[category]

    if (countFromMap != null) {
        oldCount = countFromMap
    }

    val newCount = oldCount + 1

    categoryCount[category] = newCount
}

fun findMostTrainedCategory(categoryCount: MutableMap<String, Int>): String {

    if (categoryCount.isEmpty()) {
        return "None yet"
    }

    var bestCategory = "None yet"
    var bestCount = 0

    for (entry in categoryCount) {
        val category = entry.key
        val count = entry.value

        if (count > bestCount) {
            bestCategory = category
            bestCount = count
        }
    }

    return bestCategory
}

fun getTopFivePersonalRecords(
    records: MutableMap<String, PersonalRecord>
): List<PersonalRecord> {

    val allRecords = mutableListOf<PersonalRecord>()

    for (record in records.values) {
        allRecords.add(record)
    }

    val topRecords = mutableListOf<PersonalRecord>()

    while (topRecords.size < 5 && allRecords.isNotEmpty()) {
        var bestIndex = 0
        var bestRecord = allRecords[0]

        var index = 0

        for (record in allRecords) {
            if (record.amount > bestRecord.amount) {
                bestRecord = record
                bestIndex = index
            }

            index = index + 1
        }

        topRecords.add(bestRecord)
        allRecords.removeAt(bestIndex)
    }

    return topRecords
}

fun findBestSetForActivity(activityId: Int): Double {

    val sets = ActivitySetsTable
        .selectAll()
        .where { ActivitySetsTable.activityId eq activityId }
        .toList()

    var bestAmount = 0.0

    for (set in sets) {
        val amount = set[ActivitySetsTable.amount]

        if (amount > bestAmount) {
            bestAmount = amount
        }
    }

    return bestAmount
}