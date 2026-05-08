package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.pages.ActivityDisplay
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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