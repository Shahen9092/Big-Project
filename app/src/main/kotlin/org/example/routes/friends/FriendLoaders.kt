package org.example.routes

import org.example.db.tables.FriendshipsTable
import org.example.db.tables.TemplateSharesTable
import org.example.db.tables.UsersTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.example.pages.FriendDisplay
import org.example.pages.FriendRequestDisplay
import org.example.pages.FriendTemplateChoice
import org.example.pages.ReceivedTemplateShareDisplay
import org.example.pages.SentTemplateShareDisplay
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun loadFriendsPageData(userId: Int): FriendsPageData {

    return transaction {
        val friendships = FriendshipsTable
            .selectAll()
            .where {
                (FriendshipsTable.requesterId eq userId) or
                        (FriendshipsTable.addresseeId eq userId)
            }
            .toList()

        val friends = mutableListOf<FriendDisplay>()
        val incomingRequests = mutableListOf<FriendRequestDisplay>()
        val outgoingRequests = mutableListOf<FriendRequestDisplay>()

        for (friendship in friendships) {
            val friendshipId = friendship[FriendshipsTable.id]
            val requesterId = friendship[FriendshipsTable.requesterId]
            val addresseeId = friendship[FriendshipsTable.addresseeId]
            val status = friendship[FriendshipsTable.status]

            if (status == "accepted") {
                addFriendDisplayInsideTransaction(
                    friends = friends,
                    friendshipId = friendshipId,
                    currentUserId = userId,
                    requesterId = requesterId,
                    addresseeId = addresseeId
                )
            }

            if (status == "pending") {
                if (addresseeId == userId) {
                    addFriendRequestDisplayInsideTransaction(
                        requestList = incomingRequests,
                        friendshipId = friendshipId,
                        otherUserId = requesterId
                    )
                }

                if (requesterId == userId) {
                    addFriendRequestDisplayInsideTransaction(
                        requestList = outgoingRequests,
                        friendshipId = friendshipId,
                        otherUserId = addresseeId
                    )
                }
            }
        }

        val myTemplates = loadTemplateChoicesForFriendPageInsideTransaction(userId)
        val receivedTemplates = loadReceivedTemplateSharesInsideTransaction(userId)
        val sentTemplates = loadSentTemplateSharesInsideTransaction(userId)

        FriendsPageData(
            friends = friends,
            incomingRequests = incomingRequests,
            outgoingRequests = outgoingRequests,
            myTemplates = myTemplates,
            receivedTemplates = receivedTemplates,
            sentTemplates = sentTemplates
        )
    }
}

fun addFriendDisplayInsideTransaction(
    friends: MutableList<FriendDisplay>,
    friendshipId: Int,
    currentUserId: Int,
    requesterId: Int,
    addresseeId: Int
) {

    var otherUserId = requesterId

    if (requesterId == currentUserId) {
        otherUserId = addresseeId
    }

    val otherUser = findUserByIdInsideTransaction(otherUserId)

    if (otherUser != null) {
        friends.add(
            FriendDisplay(
                friendshipId = friendshipId,
                userId = otherUserId,
                fullName = makeFriendFullName(otherUser),
                username = otherUser[UsersTable.username],
                email = otherUser[UsersTable.email]
            )
        )
    }
}

fun addFriendRequestDisplayInsideTransaction(
    requestList: MutableList<FriendRequestDisplay>,
    friendshipId: Int,
    otherUserId: Int
) {

    val otherUser = findUserByIdInsideTransaction(otherUserId)

    if (otherUser != null) {
        requestList.add(
            FriendRequestDisplay(
                friendshipId = friendshipId,
                userId = otherUserId,
                fullName = makeFriendFullName(otherUser),
                username = otherUser[UsersTable.username],
                email = otherUser[UsersTable.email]
            )
        )
    }
}

fun loadTemplateChoicesForFriendPage(userId: Int): List<FriendTemplateChoice> {

    return transaction {
        loadTemplateChoicesForFriendPageInsideTransaction(userId)
    }
}

fun loadTemplateChoicesForFriendPageInsideTransaction(userId: Int): List<FriendTemplateChoice> {

    val rows = WorkoutTemplatesTable
        .selectAll()
        .where { WorkoutTemplatesTable.userId eq userId }
        .orderBy(WorkoutTemplatesTable.name, SortOrder.ASC)
        .toList()

    val list = mutableListOf<FriendTemplateChoice>()

    for (row in rows) {
        val count = WorkoutTemplateExercisesTable
            .selectAll()
            .where { WorkoutTemplateExercisesTable.templateId eq row[WorkoutTemplatesTable.id] }
            .count()
            .toInt()

        list.add(
            FriendTemplateChoice(
                templateId = row[WorkoutTemplatesTable.id],
                name = row[WorkoutTemplatesTable.name],
                exerciseCount = count
            )
        )
    }

    return list
}

fun loadReceivedTemplateShares(userId: Int): List<ReceivedTemplateShareDisplay> {

    return transaction {
        loadReceivedTemplateSharesInsideTransaction(userId)
    }
}

fun loadReceivedTemplateSharesInsideTransaction(userId: Int): List<ReceivedTemplateShareDisplay> {

    val shares = TemplateSharesTable
        .selectAll()
        .where { TemplateSharesTable.receiverId eq userId }
        .orderBy(TemplateSharesTable.id, SortOrder.DESC)
        .toList()

    val list = mutableListOf<ReceivedTemplateShareDisplay>()

    for (share in shares) {
        val status = share[TemplateSharesTable.status]

        if (status == "pending") {
            val sender = findUserByIdInsideTransaction(share[TemplateSharesTable.senderId])

            val template = WorkoutTemplatesTable
                .selectAll()
                .where { WorkoutTemplatesTable.id eq share[TemplateSharesTable.templateId] }
                .singleOrNull()

            if (sender != null && template != null) {
                val count = WorkoutTemplateExercisesTable
                    .selectAll()
                    .where { WorkoutTemplateExercisesTable.templateId eq template[WorkoutTemplatesTable.id] }
                    .count()
                    .toInt()

                list.add(
                    ReceivedTemplateShareDisplay(
                        shareId = share[TemplateSharesTable.id],
                        templateName = template[WorkoutTemplatesTable.name],
                        senderName = makeFriendFullName(sender),
                        senderUsername = sender[UsersTable.username],
                        exerciseCount = count
                    )
                )
            }
        }
    }

    return list
}

fun loadSentTemplateShares(userId: Int): List<SentTemplateShareDisplay> {

    return transaction {
        loadSentTemplateSharesInsideTransaction(userId)
    }
}

fun loadSentTemplateSharesInsideTransaction(userId: Int): List<SentTemplateShareDisplay> {

    val shares = TemplateSharesTable
        .selectAll()
        .where { TemplateSharesTable.senderId eq userId }
        .orderBy(TemplateSharesTable.id, SortOrder.DESC)
        .toList()

    val list = mutableListOf<SentTemplateShareDisplay>()

    for (share in shares) {
        val status = share[TemplateSharesTable.status]

        var shouldShow = false

        if (status == "pending") {
            shouldShow = true
        }

        if (status == "saved") {
            shouldShow = true
        }

        if (shouldShow) {
            val receiver = findUserByIdInsideTransaction(share[TemplateSharesTable.receiverId])

            val template = WorkoutTemplatesTable
                .selectAll()
                .where { WorkoutTemplatesTable.id eq share[TemplateSharesTable.templateId] }
                .singleOrNull()

            if (receiver != null && template != null) {
                var statusText = "Pending"

                if (status == "saved") {
                    statusText = "Saved"
                }

                list.add(
                    SentTemplateShareDisplay(
                        shareId = share[TemplateSharesTable.id],
                        templateName = template[WorkoutTemplatesTable.name],
                        receiverName = makeFriendFullName(receiver),
                        receiverUsername = receiver[UsersTable.username],
                        status = statusText
                    )
                )
            }
        }
    }

    return list
}