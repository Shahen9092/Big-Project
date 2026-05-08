package org.example.routes

import org.example.db.tables.ExercisesTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.example.pages.TemplateExerciseItem
import org.example.pages.TemplateSummary
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun loadTemplatesPageData(userId: Int): TemplatesPageData {

    return transaction {
        val templates = loadTemplatesInsideTransaction(userId)
        val exercises = loadAllExercisesForTemplates()

        TemplatesPageData(
            templates = templates,
            exercises = exercises
        )
    }
}

fun loadTemplateDetailPageData(
    userId: Int,
    templateId: Int
): TemplateDetailPageData? {

    return transaction {
        var pageData: TemplateDetailPageData? = null

        val templateRow = WorkoutTemplatesTable
            .selectAll()
            .where { WorkoutTemplatesTable.id eq templateId }
            .singleOrNull()

        if (templateRow != null) {
            if (templateRow[WorkoutTemplatesTable.userId] == userId) {
                val exercises = loadExercisesForTemplateInsideTransaction(templateId)

                val summary = TemplateSummary(
                    id = templateRow[WorkoutTemplatesTable.id],
                    name = templateRow[WorkoutTemplatesTable.name],
                    description = templateRow[WorkoutTemplatesTable.description],
                    exerciseCount = exercises.size
                )

                pageData = TemplateDetailPageData(
                    template = summary,
                    exercises = exercises
                )
            }
        }

        pageData
    }
}

fun loadTemplates(userId: Int): List<TemplateSummary> {

    return transaction {
        loadTemplatesInsideTransaction(userId)
    }
}

fun loadTemplatesInsideTransaction(userId: Int): List<TemplateSummary> {

    val templateRows = WorkoutTemplatesTable
        .selectAll()
        .where { WorkoutTemplatesTable.userId eq userId }
        .orderBy(WorkoutTemplatesTable.id, SortOrder.DESC)
        .toList()

    val list = mutableListOf<TemplateSummary>()

    for (template in templateRows) {
        val templateId = template[WorkoutTemplatesTable.id]

        val count = WorkoutTemplateExercisesTable
            .selectAll()
            .where { WorkoutTemplateExercisesTable.templateId eq templateId }
            .count()
            .toInt()

        list.add(
            TemplateSummary(
                id = templateId,
                name = template[WorkoutTemplatesTable.name],
                description = template[WorkoutTemplatesTable.description],
                exerciseCount = count
            )
        )
    }

    return list
}

fun loadAllExercisesForTemplates(): List<TemplateExerciseItem> {

    val rows = ExercisesTable
        .selectAll()
        .orderBy(ExercisesTable.category, SortOrder.ASC)
        .orderBy(ExercisesTable.name, SortOrder.ASC)
        .toList()

    val list = mutableListOf<TemplateExerciseItem>()

    for (exercise in rows) {
        list.add(
            TemplateExerciseItem(
                id = exercise[ExercisesTable.id],
                name = exercise[ExercisesTable.name],
                category = exercise[ExercisesTable.category],
                unit = exercise[ExercisesTable.defaultUnit]
            )
        )
    }

    return list
}

fun loadExercisesForTemplateInsideTransaction(templateId: Int): List<TemplateExerciseItem> {

    val exerciseLinks = WorkoutTemplateExercisesTable
        .selectAll()
        .where { WorkoutTemplateExercisesTable.templateId eq templateId }
        .toList()

    val exercises = mutableListOf<TemplateExerciseItem>()

    for (link in exerciseLinks) {
        val exerciseId = link[WorkoutTemplateExercisesTable.exerciseId]

        val exercise = ExercisesTable
            .selectAll()
            .where { ExercisesTable.id eq exerciseId }
            .singleOrNull()

        if (exercise != null) {
            exercises.add(
                TemplateExerciseItem(
                    id = exercise[ExercisesTable.id],
                    name = exercise[ExercisesTable.name],
                    category = exercise[ExercisesTable.category],
                    unit = exercise[ExercisesTable.defaultUnit]
                )
            )
        }
    }

    return exercises
}