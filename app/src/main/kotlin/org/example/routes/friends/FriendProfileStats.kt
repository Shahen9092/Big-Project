package org.example.routes

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.pages.FriendProfileStats
import org.example.pages.FriendRecentActivity
import org.example.pages.PersonalRecord
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun loadFriendProfilePageData(
    currentUserId: Int,
    friendUserId: Int
): FriendProfilePageData? {

    return transaction {
        var pageData: FriendProfilePageData? = null

        val usersAreFriends = usersAreFriendsInsideTransaction(
            firstUserId = currentUserId,
            secondUserId = friendUserId
        )

        if (usersAreFriends) {
            val friendUser = findUserByIdInsideTransaction(friendUserId)

            if (friendUser != null) {
                val fullName = makeFriendFullName(friendUser)
                val username = friendUser[org.example.db.tables.UsersTable.username]
                val stats = buildFriendProfileStatsInsideTransaction(friendUserId)

                pageData = FriendProfilePageData(
                    fullName = fullName,
                    username = username,
                    stats = stats
                )
            }
        }

        pageData
    }
}

fun buildFriendProfileStats(userId: Int): FriendProfileStats {

    return transaction {
        buildFriendProfileStatsInsideTransaction(userId)
    }
}

fun buildFriendProfileStatsInsideTransaction(userId: Int): FriendProfileStats {

    val activityRows = ActivitiesTable
        .selectAll()
        .where { ActivitiesTable.userId eq userId }
        .orderBy(ActivitiesTable.date, SortOrder.DESC)
        .orderBy(ActivitiesTable.id, SortOrder.DESC)
        .toList()

    var totalSets = 0
    val categoryCount = mutableMapOf<String, Int>()
    val records = mutableMapOf<String, PersonalRecord>()
    val recentActivities = mutableListOf<FriendRecentActivity>()

    for (activity in activityRows) {
        val exercise = ExercisesTable
            .selectAll()
            .where { ExercisesTable.id eq activity[ActivitiesTable.exerciseId] }
            .singleOrNull()

        if (exercise != null) {
            val exerciseName = exercise[ExercisesTable.name]
            val category = exercise[ExercisesTable.category]
            val unit = exercise[ExercisesTable.defaultUnit]

            addOneToFriendCategoryCount(categoryCount, category)

            val sets = ActivitySetsTable
                .selectAll()
                .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                .toList()

            totalSets = totalSets + sets.size

            var bestAmount = 0.0

            for (set in sets) {
                val amount = set[ActivitySetsTable.amount]

                if (amount > bestAmount) {
                    bestAmount = amount
                }

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

            if (recentActivities.size < 5) {
                recentActivities.add(
                    FriendRecentActivity(
                        date = activity[ActivitiesTable.date],
                        exerciseName = exerciseName,
                        category = category,
                        setCount = sets.size,
                        bestAmount = bestAmount,
                        unit = unit
                    )
                )
            }
        }
    }

    val topCategory = findFriendTopCategory(categoryCount)
    val recordList = getTopFiveFriendRecords(records)

    return FriendProfileStats(
        totalActivities = activityRows.size,
        totalSets = totalSets,
        topCategory = topCategory,
        personalRecords = recordList,
        recentActivities = recentActivities
    )
}

fun addOneToFriendCategoryCount(
    categoryCount: MutableMap<String, Int>,
    category: String
) {

    var oldCount = 0

    val countFromMap = categoryCount[category]

    if (countFromMap != null) {
        oldCount = countFromMap
    }

    categoryCount[category] = oldCount + 1
}

fun findFriendTopCategory(categoryCount: MutableMap<String, Int>): String {

    if (categoryCount.isEmpty()) {
        return "None yet"
    }

    var topCategory = "None yet"
    var topCount = 0

    for (entry in categoryCount) {
        val category = entry.key
        val count = entry.value

        if (count > topCount) {
            topCategory = category
            topCount = count
        }
    }

    return topCategory
}

fun getTopFiveFriendRecords(
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