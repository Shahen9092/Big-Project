package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun saveNewActivity(
    userId: Int,
    exerciseId: Int,
    date: String,
    notesForDatabase: String?,
    cleanedAmounts: List<Double>
) {

    transaction {
        val activityId = ActivitiesTable.insert {
            it[ActivitiesTable.userId] = userId
            it[ActivitiesTable.exerciseId] = exerciseId
            it[ActivitiesTable.date] = date
            it[ActivitiesTable.notes] = notesForDatabase
        }[ActivitiesTable.id]

        var setNumber = 1

        for (amount in cleanedAmounts) {
            ActivitySetsTable.insert {
                it[ActivitySetsTable.activityId] = activityId
                it[ActivitySetsTable.setNumber] = setNumber
                it[ActivitySetsTable.amount] = amount
            }

            setNumber = setNumber + 1
        }
    }
}

fun updateExistingActivity(
    activityId: Int,
    date: String,
    notesForDatabase: String?,
    cleanedAmounts: List<Double>
) {

    transaction {
        ActivitiesTable.update({ ActivitiesTable.id eq activityId }) {
            it[ActivitiesTable.date] = date
            it[ActivitiesTable.notes] = notesForDatabase
        }

        ActivitySetsTable.deleteWhere {
            ActivitySetsTable.activityId eq activityId
        }

        var setNumber = 1

        for (amount in cleanedAmounts) {
            ActivitySetsTable.insert {
                it[ActivitySetsTable.activityId] = activityId
                it[ActivitySetsTable.setNumber] = setNumber
                it[ActivitySetsTable.amount] = amount
            }

            setNumber = setNumber + 1
        }
    }
}