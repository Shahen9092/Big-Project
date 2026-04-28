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
import org.example.db.tables.GoalsTable
import org.example.models.UserSession
import org.example.pages.GoalDisplay
import org.example.pages.renderGoalsPage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

fun Route.goalRoutes() {

    get("/goals") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = call.request.queryParameters["msg"]
        val error = call.request.queryParameters["error"]

        val pageData = transaction {
            val goals = loadGoalsForUser(session.userId)
            val exercises = loadAllExercisesForTemplates()

            Pair(goals, exercises)
        }

        call.respondText(
            renderGoalsPage(
                goals = pageData.first,
                exercises = pageData.second,
                message = message,
                error = error
            ),
            ContentType.Text.Html
        )
    }

    post("/goals/create") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()

        val title = params["title"]?.trim().orEmpty()
        val goalType = params["goalType"]?.trim().orEmpty()
        val exerciseChoice = params["exerciseChoice"]?.trim().orEmpty()
        val exerciseId = exerciseChoice.substringBefore(" - ").toIntOrNull()
        val targetAmount = params["targetAmount"]?.toDoubleOrNull()

        if (title == "") {
            call.respondRedirect("/goals?error=title")
            return@post
        }

        if (targetAmount == null || targetAmount <= 0) {
            call.respondRedirect("/goals?error=target")
            return@post
        }

        if (goalType == "exercise_best" && exerciseId == null) {
            call.respondRedirect("/goals?error=exercise")
            return@post
        }

        val unit = transaction {
            if (goalType == "activities") {
                "activities"
            } else if (goalType == "sets") {
                "sets"
            } else {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq exerciseId!! }
                    .singleOrNull()

                exercise?.get(ExercisesTable.defaultUnit) ?: "units"
            }
        }

        transaction {
            GoalsTable.insert {
                it[GoalsTable.userId] = session.userId
                it[GoalsTable.title] = title
                it[GoalsTable.goalType] = goalType
                it[GoalsTable.targetAmount] = targetAmount
                it[GoalsTable.unit] = unit

                // Dates are kept in the database because the table already has them,
                // but the goal page now treats goals as all-time.
                it[GoalsTable.startDate] = "0000-01-01"
                it[GoalsTable.endDate] = "9999-12-31"

                it[GoalsTable.exerciseId] = exerciseId
            }
        }

        call.respondRedirect("/goals?msg=created")
    }

    post("/goals/delete/{goalId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val goalId = call.parameters["goalId"]?.toIntOrNull()

        if (goalId == null) {
            call.respondRedirect("/goals")
            return@post
        }

        transaction {
            val goal = GoalsTable
                .selectAll()
                .where { GoalsTable.id eq goalId }
                .singleOrNull()

            if (goal != null && goal[GoalsTable.userId] == session.userId) {
                GoalsTable.deleteWhere {
                    GoalsTable.id eq goalId
                }
            }
        }

        call.respondRedirect("/goals?msg=deleted")
    }
}

fun loadGoalsForUser(userId: Int): List<GoalDisplay> {
    val goalRows = GoalsTable
        .selectAll()
        .where { GoalsTable.userId eq userId }
        .orderBy(GoalsTable.id, SortOrder.DESC)
        .toList()

    val displays = mutableListOf<GoalDisplay>()

    for (goal in goalRows) {
        val current = calculateGoalProgress(
            userId = userId,
            goalType = goal[GoalsTable.goalType],
            exerciseId = goal[GoalsTable.exerciseId]
        )

        val target = goal[GoalsTable.targetAmount]

        var percent = ((current / target) * 100).roundToInt()

        if (percent > 100) {
            percent = 100
        }

        val typeLabel = when (goal[GoalsTable.goalType]) {
            "activities" -> "Number of activities"
            "sets" -> "Number of sets"
            "exercise_best" -> "Best amount for an exercise"
            else -> goal[GoalsTable.goalType]
        }

        displays.add(
            GoalDisplay(
                id = goal[GoalsTable.id],
                title = goal[GoalsTable.title],
                typeLabel = typeLabel,
                currentAmount = current,
                targetAmount = target,
                unit = goal[GoalsTable.unit],
                percentage = percent
            )
        )
    }

    return displays
}

fun calculateGoalProgress(
    userId: Int,
    goalType: String,
    exerciseId: Int?
): Double {

    val activityRows = ActivitiesTable
        .selectAll()
        .where { ActivitiesTable.userId eq userId }
        .toList()

    if (goalType == "activities") {
        return activityRows.size.toDouble()
    }

    if (goalType == "sets") {
        var setCount = 0

        for (activity in activityRows) {
            val sets = ActivitySetsTable
                .selectAll()
                .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                .count()
                .toInt()

            setCount += sets
        }

        return setCount.toDouble()
    }

    if (goalType == "exercise_best") {
        if (exerciseId == null) {
            return 0.0
        }

        var best = 0.0

        for (activity in activityRows) {
            if (activity[ActivitiesTable.exerciseId] == exerciseId) {
                val sets = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                    .toList()

                for (set in sets) {
                    val amount = set[ActivitySetsTable.amount]

                    if (amount > best) {
                        best = amount
                    }
                }
            }
        }

        return best
    }

    return 0.0
}