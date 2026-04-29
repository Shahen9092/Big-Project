package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object ExercisesTable : Table("exercises") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val category = varchar("category", 50)
    val defaultUnit = varchar("default_unit", 30)
    val notes = varchar("notes", 255).nullable()
    
    override val primaryKey = PrimaryKey(id)
}