package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object CalendarEventsTable : Table("calendar_events") {
    val id     = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val day    = integer("day")
    val month  = integer("month")
    val year   = integer("year")
    val type   = varchar("type", 20)
    val note   = varchar("note", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}
