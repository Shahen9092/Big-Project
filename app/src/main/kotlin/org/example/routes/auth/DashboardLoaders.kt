package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.pages.DashboardStats
import org.example.pages.PersonalRecord
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun loadDashboardPageData(
    userId: Int,
    fallbackUsername: String
): DashboardPageData {

    return transaction {
        val fullName = loadFullNameInsideTransaction(userId, fallbackUsername)

        val activityRows = ActivitiesTable
            .selectAll()
            .where { ActivitiesTable.userId eq userId }
            .orderBy(ActivitiesTable.date, SortOrder.DESC)
            .orderBy(ActivitiesTable.id, SortOrder.DESC)
            .toList()

        var totalSets = 0
        val categoryCount = mutableMapOf<String, Int>()
        val records = mutableMapOf<String, PersonalRecord>()

        var lastActivity = "No activity logged yet."

        for (activity in activityRows) {
            val exerciseId = activity[ActivitiesTable.exerciseId]
            val exercise = loadExerciseInsideTransaction(exerciseId)

            if (exercise != null) {
                val exerciseName = exercise[ExercisesTable.name]
                val category = exercise[ExercisesTable.category]
                val unit = exercise[ExercisesTable.defaultUnit]
                val date = activity[ActivitiesTable.date]

                if (lastActivity == "No activity logged yet.") {
                    lastActivity = "$exerciseName on $date"
                }

                addOneToCategoryCount(categoryCount, category)

                val sets = ActivitySetsTable
                    .selectAll()
                    .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                    .toList()

                totalSets = totalSets + sets.size

                for (set in sets) {
                    val amount = set[ActivitySetsTable.amount]
                    val oldRecord = records[exerciseName]

                    if (oldRecord == null) {
                        records[exerciseName] = PersonalRecord(
                            exerciseName = exerciseName,
                            amount = amount,
                            unit = unit
                        )
                    } else {
                        if (amount > oldRecord.amount) {
                            records[exerciseName] = PersonalRecord(
                                exerciseName = exerciseName,
                                amount = amount,
                                unit = unit
                            )
                        }
                    }
                }
            }
        }

        val mostTrainedCategory = findMostTrainedCategory(categoryCount)
        val recordList = getTopFivePersonalRecords(records)

        val stats = DashboardStats(
            totalActivities = activityRows.size,
            totalSets = totalSets,
            mostTrainedCategory = mostTrainedCategory,
            lastActivity = lastActivity,
            personalRecords = recordList
        )

        DashboardPageData(
            fullName = fullName,
            stats = stats
        )
    }
}

fun loadExerciseInsideTransaction(exerciseId: Int): ResultRow? {

    val exercise = ExercisesTable
        .selectAll()
        .where { ExercisesTable.id eq exerciseId }
        .singleOrNull()

    return exercise
}

fun addOneToCategoryCount(
    categoryCount: MutableMap<String, Int>,
    category: String
) {

    var oldCount = 0

    val countFromMap = categoryCount[category]

    if (countFromMap != null) {
        oldCount = countFromMap
    }

    val newCount = oldCount + 1

    categoryCount[category] = newCount
}

fun findMostTrainedCategory(categoryCount: MutableMap<String, Int>): String {

    if (categoryCount.isEmpty()) {
        return "None yet"
    }

    var bestCategory = "None yet"
    var bestCount = 0

    for (entry in categoryCount) {
        val category = entry.key
        val count = entry.value

        if (count > bestCount) {
            bestCategory = category
            bestCount = count
        }
    }

    return bestCategory
}

fun getTopFivePersonalRecords(
    records: MutableMap<String, PersonalRecord>
): List<PersonalRecord> {

    val allRecords = mutableListOf<PersonalRecord>()

    for (record in records.values) {
        allRecords.add(record)
    }

    val topRecords = mutableListOf<PersonalRecord>()

    while (topRecords.size < 5 && allRecords.isNotEmpty()) {
        var bestIndex = 0
        var bestRecord = allRecords[0]

        var index = 0

        for (record in allRecords) {
            if (record.amount > bestRecord.amount) {
                bestRecord = record
                bestIndex = index
            }

            index = index + 1
        }

        topRecords.add(bestRecord)
        allRecords.removeAt(bestIndex)
    }

    return topRecords
}