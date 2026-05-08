package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun restoreDeletedActivity(backup: DeletedActivityBackup) {

    transaction {
        val activityId = ActivitiesTable.insert {
            it[ActivitiesTable.userId] = backup.userId
            it[ActivitiesTable.exerciseId] = backup.exerciseId
            it[ActivitiesTable.date] = backup.date
            it[ActivitiesTable.notes] = backup.notes
        }[ActivitiesTable.id]

        for (set in backup.sets) {
            ActivitySetsTable.insert {
                it[ActivitySetsTable.activityId] = activityId
                it[ActivitySetsTable.setNumber] = set.setNumber
                it[ActivitySetsTable.amount] = set.amount
            }
        }
    }
}

fun deleteActivityAndCreateBackup(activityId: Int, userId: Int): DeletedActivityBackup? {

    return transaction {
        var backupData: DeletedActivityBackup? = null

        val activityQuery = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.id eq activityId }

        val activity = activityQuery.singleOrNull()

        if (activity != null) {
            if (activity[ActivitiesTable.userId] == userId) {

                val setRows = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activityId }
                    .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                    .toList()

                val setBackups = mutableListOf<DeletedActivitySetBackup>()

                for (setRow in setRows) {
                    val backupSet = DeletedActivitySetBackup(
                        setNumber = setRow[ActivitySetsTable.setNumber],
                        amount = setRow[ActivitySetsTable.amount]
                    )

                    setBackups.add(backupSet)
                }

                backupData = DeletedActivityBackup(
                    userId = activity[ActivitiesTable.userId],
                    exerciseId = activity[ActivitiesTable.exerciseId],
                    date = activity[ActivitiesTable.date],
                    notes = activity[ActivitiesTable.notes],
                    sets = setBackups
                )

                ActivitySetsTable.deleteWhere {
                    ActivitySetsTable.activityId eq activityId
                }

                ActivitiesTable.deleteWhere {
                    ActivitiesTable.id eq activityId
                }
            }
        }

        backupData
    }
}