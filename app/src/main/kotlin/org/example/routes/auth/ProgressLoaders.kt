package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.pages.ProgressPoint
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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