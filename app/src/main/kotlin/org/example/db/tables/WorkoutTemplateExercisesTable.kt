package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object WorkoutTemplateExercisesTable : Table("workout_template_exercises") {
    val id = integer("id").autoIncrement()
    val templateId = integer("template_id")
    val exerciseId = integer("exercise_id")

    override val primaryKey = PrimaryKey(id)
}