package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object GoalsTable : Table("goals") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val title = varchar("title", 100)
    val goalType = varchar("goal_type", 50)
    val targetAmount = double("target_amount")
    val unit = varchar("unit", 30)
    val startDate = varchar("start_date", 20)
    val endDate = varchar("end_date", 20)
    val exerciseId = integer("exercise_id").nullable()

    override val primaryKey = PrimaryKey(id)
}