package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun createWorkoutTemplate(
    userId: Int,
    name: String,
    descriptionForDatabase: String?,
    exerciseIds: List<Int>
) {

    transaction {
        val templateId = WorkoutTemplatesTable.insert {
            it[WorkoutTemplatesTable.userId] = userId
            it[WorkoutTemplatesTable.name] = name
            it[WorkoutTemplatesTable.description] = descriptionForDatabase
        }[WorkoutTemplatesTable.id]

        for (exerciseId in exerciseIds) {
            WorkoutTemplateExercisesTable.insert {
                it[WorkoutTemplateExercisesTable.templateId] = templateId
                it[WorkoutTemplateExercisesTable.exerciseId] = exerciseId
            }
        }
    }
}

fun userCanLogExerciseFromTemplate(
    userId: Int,
    templateId: Int,
    exerciseId: Int
): Boolean {

    return transaction {
        var allowed = false

        val template = WorkoutTemplatesTable
            .selectAll()
            .where { WorkoutTemplatesTable.id eq templateId }
            .singleOrNull()

        if (template != null) {
            if (template[WorkoutTemplatesTable.userId] == userId) {
                val templateHasExercise = templateContainsExerciseInsideTransaction(
                    templateId = templateId,
                    exerciseId = exerciseId
                )

                if (templateHasExercise) {
                    allowed = true
                }
            }
        }

        allowed
    }
}

fun templateContainsExerciseInsideTransaction(
    templateId: Int,
    exerciseId: Int
): Boolean {

    val links = WorkoutTemplateExercisesTable
        .selectAll()
        .where { WorkoutTemplateExercisesTable.templateId eq templateId }
        .toList()

    for (link in links) {
        val linkedExerciseId = link[WorkoutTemplateExercisesTable.exerciseId]

        if (linkedExerciseId == exerciseId) {
            return true
        }
    }

    return false
}

fun saveActivityFromTemplate(
    userId: Int,
    exerciseId: Int,
    date: String,
    notesForDatabase: String?,
    amounts: List<Double>
) {

    transaction {
        val activityId = ActivitiesTable.insert {
            it[ActivitiesTable.userId] = userId
            it[ActivitiesTable.exerciseId] = exerciseId
            it[ActivitiesTable.date] = date
            it[ActivitiesTable.notes] = notesForDatabase
        }[ActivitiesTable.id]

        var setNumber = 1

        for (amount in amounts) {
            ActivitySetsTable.insert {
                it[ActivitySetsTable.activityId] = activityId
                it[ActivitySetsTable.setNumber] = setNumber
                it[ActivitySetsTable.amount] = amount
            }

            setNumber = setNumber + 1
        }
    }
}

fun deleteTemplateIfOwned(
    templateId: Int,
    userId: Int
) {

    transaction {
        val template = WorkoutTemplatesTable
            .selectAll()
            .where { WorkoutTemplatesTable.id eq templateId }
            .singleOrNull()

        if (template != null) {
            if (template[WorkoutTemplatesTable.userId] == userId) {
                WorkoutTemplateExercisesTable.deleteWhere {
                    WorkoutTemplateExercisesTable.templateId eq templateId
                }

                WorkoutTemplatesTable.deleteWhere {
                    WorkoutTemplatesTable.id eq templateId
                }
            }
        }
    }
}