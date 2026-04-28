package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object WorkoutTemplatesTable : Table("workout_templates") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val name = varchar("name", 100)
    val description = varchar("description", 255).nullable()
    
    override val primaryKey = PrimaryKey(id)
}