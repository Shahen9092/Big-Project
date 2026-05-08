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
import org.example.pages.TemplateExerciseItem
import org.example.pages.renderGoalsPage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

data class GoalsPageData(
    val goals: List<GoalDisplay>,
    val exercises: List<TemplateExerciseItem>
)

fun Route.goalRoutes() {

    get("/goals") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = getGoalQueryText(call, "msg")
        val error = getGoalQueryText(call, "error")

        val pageData = loadGoalsPageData(session.userId)

        call.respondText(
            renderGoalsPage(
                goals = pageData.goals,
                exercises = pageData.exercises,
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

        val title = getGoalFormText(params, "title")
        val goalType = getGoalFormText(params, "goalType")
        val exerciseChoice = getGoalFormText(params, "exerciseChoice")
        val exerciseId = getExerciseIdFromChoice(exerciseChoice)
        val targetAmount = getGoalFormDouble(params, "targetAmount")

        if (title == "") {
            call.respondRedirect("/goals?error=title")
            return@post
        }

        if (targetAmount == null) {
            call.respondRedirect("/goals?error=target")
            return@post
        }

        if (targetAmount <= 0) {
            call.respondRedirect("/goals?error=target")
            return@post
        }

        if (goalType == "exercise_best") {
            if (exerciseId == null) {
                call.respondRedirect("/goals?error=exercise")
                return@post
            }
        }

        val unit = loadGoalUnit(
            goalType = goalType,
            exerciseId = exerciseId
        )

        createGoal(
            userId = session.userId,
            title = title,
            goalType = goalType,
            targetAmount = targetAmount,
            unit = unit,
            exerciseId = exerciseId
        )

        call.respondRedirect("/goals?msg=created")
    }

    post("/goals/delete/{goalId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val goalId = getGoalRouteInt(call, "goalId")

        if (goalId == null) {
            call.respondRedirect("/goals")
            return@post
        }

        deleteGoalIfOwned(
            goalId = goalId,
            userId = session.userId
        )

        call.respondRedirect("/goals?msg=deleted")
    }
}

fun getGoalQueryText(call: ApplicationCall, name: String): String? {

    val text = call.request.queryParameters[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getGoalRouteInt(call: ApplicationCall, name: String): Int? {

    val text = call.parameters[name]

    if (text == null) {
        return null
    }

    return text.toIntOrNull()
}

fun getGoalFormText(params: Parameters, name: String): String {

    var result = ""

    val text = params[name]

    if (text != null) {
        result = text.trim()
    }

    return result
}

fun getGoalFormDouble(params: Parameters, name: String): Double? {

    val text = params[name]

    if (text == null) {
        return null
    }

    return text.toDoubleOrNull()
}

fun getExerciseIdFromChoice(exerciseChoice: String): Int? {

    if (exerciseChoice == "") {
        return null
    }

    var idText = ""

    for (letter in exerciseChoice) {
        if (letter == ' ') {
            break
        }

        if (letter == '-') {
            break
        }

        idText = idText + letter
    }

    if (idText == "") {
        return null
    }

    return idText.toIntOrNull()
}

fun loadGoalsPageData(userId: Int): GoalsPageData {

    return transaction {
        val goals = loadGoalsForUser(userId)
        val exercises = loadAllExercisesForTemplates()

        GoalsPageData(
            goals = goals,
            exercises = exercises
        )
    }
}

fun loadGoalUnit(
    goalType: String,
    exerciseId: Int?
): String {

    return transaction {
        var unit = "units"

        if (goalType == "activities") {
            unit = "activities"
        } else if (goalType == "sets") {
            unit = "sets"
        } else {
            if (exerciseId != null) {
                val exercise = ExercisesTable
                    .selectAll()
                    .where { ExercisesTable.id eq exerciseId }
                    .singleOrNull()

                if (exercise != null) {
                    unit = exercise[ExercisesTable.defaultUnit]
                }
            }
        }

        unit
    }
}

fun createGoal(
    userId: Int,
    title: String,
    goalType: String,
    targetAmount: Double,
    unit: String,
    exerciseId: Int?
) {

    transaction {
        GoalsTable.insert {
            it[GoalsTable.userId] = userId
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
}

fun deleteGoalIfOwned(
    goalId: Int,
    userId: Int
) {

    transaction {
        val goal = GoalsTable
            .selectAll()
            .where { GoalsTable.id eq goalId }
            .singleOrNull()

        if (goal != null) {
            if (goal[GoalsTable.userId] == userId) {
                GoalsTable.deleteWhere {
                    GoalsTable.id eq goalId
                }
            }
        }
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
        val goalType = goal[GoalsTable.goalType]
        val exerciseId = goal[GoalsTable.exerciseId]

        val current = calculateGoalProgress(
            userId = userId,
            goalType = goalType,
            exerciseId = exerciseId
        )

        val target = goal[GoalsTable.targetAmount]
        val percent = calculateGoalPercentage(current, target)
        val typeLabel = getGoalTypeLabel(goalType)

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

fun calculateGoalPercentage(
    current: Double,
    target: Double
): Int {

    var percent = ((current / target) * 100).roundToInt()

    if (percent > 100) {
        percent = 100
    }

    return percent
}

fun getGoalTypeLabel(goalType: String): String {

    var typeLabel = goalType

    if (goalType == "activities") {
        typeLabel = "Number of activities"
    } else if (goalType == "sets") {
        typeLabel = "Number of sets"
    } else if (goalType == "exercise_best") {
        typeLabel = "Best amount for an exercise"
    }

    return typeLabel
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
        val totalSets = countTotalSets(activityRows)
        return totalSets.toDouble()
    }

    if (goalType == "exercise_best") {
        if (exerciseId == null) {
            return 0.0
        }

        val bestAmount = findBestAmountForExercise(
            activityRows = activityRows,
            exerciseId = exerciseId
        )

        return bestAmount
    }

    return 0.0
}

fun countTotalSets(activityRows: List<org.jetbrains.exposed.sql.ResultRow>): Int {

    var setCount = 0

    for (activity in activityRows) {
        val activityId = activity[ActivitiesTable.id]

        val setsForActivity = ActivitySetsTable
            .selectAll()
            .where { ActivitySetsTable.activityId eq activityId }
            .count()
            .toInt()

        setCount = setCount + setsForActivity
    }

    return setCount
}

fun findBestAmountForExercise(
    activityRows: List<org.jetbrains.exposed.sql.ResultRow>,
    exerciseId: Int
): Double {

    var best = 0.0

    for (activity in activityRows) {
        if (activity[ActivitiesTable.exerciseId] == exerciseId) {
            val activityId = activity[ActivitiesTable.id]

            val sets = ActivitySetsTable
                .selectAll()
                .where { ActivitySetsTable.activityId eq activityId }
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