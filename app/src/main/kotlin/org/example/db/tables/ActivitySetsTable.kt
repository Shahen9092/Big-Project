package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object ActivitySetsTable : Table("activity_sets") {
    val id = integer("id").autoIncrement()
    val activityId = integer("activity_id")
    val setNumber = integer("set_number")
    val amount = double("amount")

    override val primaryKey = PrimaryKey(id)
}