package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object ActivitiesTable : Table("logged_activities") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val exerciseId = integer("exercise_id")
    val date = varchar("date", 20)
    val notes = varchar("notes", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}