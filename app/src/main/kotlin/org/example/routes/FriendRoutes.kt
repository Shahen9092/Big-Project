package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.db.tables.FriendshipsTable
import org.example.db.tables.TemplateSharesTable
import org.example.db.tables.UsersTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.example.models.UserSession
import org.example.pages.FriendDisplay
import org.example.pages.FriendProfileStats
import org.example.pages.FriendRecentActivity
import org.example.pages.FriendRequestDisplay
import org.example.pages.FriendTemplateChoice
import org.example.pages.PersonalRecord
import org.example.pages.ReceivedTemplateShareDisplay
import org.example.pages.SentTemplateShareDisplay
import org.example.pages.renderFriendProfilePage
import org.example.pages.renderFriendsPage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Route.friendRoutes() {

    get("/friends") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val message = call.request.queryParameters["msg"]
        val error = call.request.queryParameters["error"]

        val pageData = loadFriendsPageData(session.userId)

        call.respondText(
            renderFriendsPage(
                friends = pageData.friends,
                incomingRequests = pageData.incomingRequests,
                outgoingRequests = pageData.outgoingRequests,
                myTemplates = pageData.myTemplates,
                receivedTemplates = pageData.receivedTemplates,
                sentTemplates = pageData.sentTemplates,
                message = message,
                error = error
            ),
            ContentType.Text.Html
        )
    }

    get("/friends/profile/{userId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val friendUserId = call.parameters["userId"]?.toIntOrNull()

        if (friendUserId == null) {
            call.respondRedirect("/friends")
            return@get
        }

        val pageData = transaction {
            val isFriend = FriendshipsTable
                .selectAll()
                .where {
                    (FriendshipsTable.status eq "accepted") and
                            (
                                    ((FriendshipsTable.requesterId eq session.userId) and
                                            (FriendshipsTable.addresseeId eq friendUserId)) or
                                            ((FriendshipsTable.requesterId eq friendUserId) and
                                                    (FriendshipsTable.addresseeId eq session.userId))
                                    )
                }
                .singleOrNull()

            if (isFriend == null) {
                null
            } else {
                val friendUser = UsersTable
                    .selectAll()
                    .where { UsersTable.id eq friendUserId }
                    .singleOrNull()

                if (friendUser == null) {
                    null
                } else {
                    val firstName = friendUser[UsersTable.name]
                    val surname = friendUser[UsersTable.surname] ?: ""
                    val fullName = "$firstName $surname".trim()
                    val username = friendUser[UsersTable.username]

                    val stats = buildFriendProfileStats(friendUserId)

                    Triple(fullName, username, stats)
                }
            }
        }

        if (pageData == null) {
            call.respondRedirect("/friends")
            return@get
        }

        call.respondText(
            renderFriendProfilePage(
                fullName = pageData.first,
                username = pageData.second,
                stats = pageData.third
            ),
            ContentType.Text.Html
        )
    }

    post("/friends/add") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()
        val identifier = params["identifier"]?.trim().orEmpty()

        if (identifier == "") {
            call.respondRedirect("/friends?error=empty")
            return@post
        }

        val result = transaction {
            val targetUser = UsersTable
                .selectAll()
                .where {
                    (UsersTable.username eq identifier) or
                            (UsersTable.email eq identifier)
                }
                .singleOrNull()

            if (targetUser == null) {
                "notfound"
            } else {
                val targetUserId = targetUser[UsersTable.id]

                if (targetUserId == session.userId) {
                    "self"
                } else {
                    val existingFriendship = FriendshipsTable
                        .selectAll()
                        .where {
                            ((FriendshipsTable.requesterId eq session.userId) and
                                    (FriendshipsTable.addresseeId eq targetUserId)) or
                                    ((FriendshipsTable.requesterId eq targetUserId) and
                                            (FriendshipsTable.addresseeId eq session.userId))
                        }
                        .singleOrNull()

                    if (existingFriendship != null) {
                        "exists"
                    } else {
                        FriendshipsTable.insert {
                            it[FriendshipsTable.requesterId] = session.userId
                            it[FriendshipsTable.addresseeId] = targetUserId
                            it[FriendshipsTable.status] = "pending"
                        }

                        "sent"
                    }
                }
            }
        }

        if (result == "sent") {
            call.respondRedirect("/friends?msg=sent")
        } else {
            call.respondRedirect("/friends?error=$result")
        }
    }

    post("/friends/accept/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = call.parameters["friendshipId"]?.toIntOrNull()

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        transaction {
            val request = FriendshipsTable
                .selectAll()
                .where { FriendshipsTable.id eq friendshipId }
                .singleOrNull()

            if (request != null && request[FriendshipsTable.addresseeId] == session.userId) {
                FriendshipsTable.update({ FriendshipsTable.id eq friendshipId }) {
                    it[FriendshipsTable.status] = "accepted"
                }
            }
        }

        call.respondRedirect("/friends?msg=accepted")
    }

    post("/friends/decline/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = call.parameters["friendshipId"]?.toIntOrNull()

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        transaction {
            val request = FriendshipsTable
                .selectAll()
                .where { FriendshipsTable.id eq friendshipId }
                .singleOrNull()

            if (request != null && request[FriendshipsTable.addresseeId] == session.userId) {
                FriendshipsTable.deleteWhere {
                    FriendshipsTable.id eq friendshipId
                }
            }
        }

        call.respondRedirect("/friends?msg=declined")
    }

    post("/friends/cancel/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = call.parameters["friendshipId"]?.toIntOrNull()

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        transaction {
            val request = FriendshipsTable
                .selectAll()
                .where { FriendshipsTable.id eq friendshipId }
                .singleOrNull()

            if (request != null && request[FriendshipsTable.requesterId] == session.userId) {
                FriendshipsTable.deleteWhere {
                    FriendshipsTable.id eq friendshipId
                }
            }
        }

        call.respondRedirect("/friends?msg=cancelled")
    }

    post("/friends/remove/{friendshipId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val friendshipId = call.parameters["friendshipId"]?.toIntOrNull()

        if (friendshipId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        transaction {
            val friendship = FriendshipsTable
                .selectAll()
                .where { FriendshipsTable.id eq friendshipId }
                .singleOrNull()

            if (friendship != null &&
                (friendship[FriendshipsTable.requesterId] == session.userId ||
                        friendship[FriendshipsTable.addresseeId] == session.userId)
            ) {
                FriendshipsTable.deleteWhere {
                    FriendshipsTable.id eq friendshipId
                }
            }
        }

        call.respondRedirect("/friends?msg=removed")
    }

    post("/friends/templates/send") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()

        val friendUserId = params["friendUserId"]?.toIntOrNull()
        val templateId = params["templateId"]?.toIntOrNull()

        if (friendUserId == null || templateId == null) {
            call.respondRedirect("/friends?error=template_data")
            return@post
        }

        val result = transaction {
            val friendship = FriendshipsTable
                .selectAll()
                .where {
                    (FriendshipsTable.status eq "accepted") and
                            (
                                    ((FriendshipsTable.requesterId eq session.userId) and
                                            (FriendshipsTable.addresseeId eq friendUserId)) or
                                            ((FriendshipsTable.requesterId eq friendUserId) and
                                                    (FriendshipsTable.addresseeId eq session.userId))
                                    )
                }
                .singleOrNull()

            if (friendship == null) {
                "notfriend"
            } else {
                val template = WorkoutTemplatesTable
                    .selectAll()
                    .where { WorkoutTemplatesTable.id eq templateId }
                    .singleOrNull()

                if (template == null || template[WorkoutTemplatesTable.userId] != session.userId) {
                    "template_notfound"
                } else {
                    val oldShare = TemplateSharesTable
                        .selectAll()
                        .where {
                            (TemplateSharesTable.senderId eq session.userId) and
                                    (TemplateSharesTable.receiverId eq friendUserId) and
                                    (TemplateSharesTable.templateId eq templateId)
                        }
                        .toList()
                        .filter {
                            val status = it[TemplateSharesTable.status]
                            status == "pending" || status == "saved"
                        }
                        .firstOrNull()

                    if (oldShare != null) {
                        "templateexists"
                    } else {
                        TemplateSharesTable.insert {
                            it[TemplateSharesTable.senderId] = session.userId
                            it[TemplateSharesTable.receiverId] = friendUserId
                            it[TemplateSharesTable.templateId] = templateId
                            it[TemplateSharesTable.status] = "pending"
                        }

                        "sent"
                    }
                }
            }
        }

        if (result == "sent") {
            call.respondRedirect("/friends?msg=template_sent")
        } else {
            call.respondRedirect("/friends?error=$result")
        }
    }

    post("/friends/templates/save/{shareId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val shareId = call.parameters["shareId"]?.toIntOrNull()

        if (shareId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        val result = transaction {
            val share = TemplateSharesTable
                .selectAll()
                .where { TemplateSharesTable.id eq shareId }
                .singleOrNull()

            if (share == null ||
                share[TemplateSharesTable.receiverId] != session.userId ||
                share[TemplateSharesTable.status] != "pending"
            ) {
                "bad"
            } else {
                val originalTemplate = WorkoutTemplatesTable
                    .selectAll()
                    .where { WorkoutTemplatesTable.id eq share[TemplateSharesTable.templateId] }
                    .singleOrNull()

                if (originalTemplate == null) {
                    "bad"
                } else {
                    val sender = UsersTable
                        .selectAll()
                        .where { UsersTable.id eq share[TemplateSharesTable.senderId] }
                        .singleOrNull()

                    var senderUsername = "friend"

                    if (sender != null) {
                        senderUsername = sender[UsersTable.username]
                    }

                    var copiedName = originalTemplate[WorkoutTemplatesTable.name] + " (from @$senderUsername)"

                    if (copiedName.length > 100) {
                        copiedName = copiedName.substring(0, 100)
                    }

                    val copiedTemplateId = WorkoutTemplatesTable.insert {
                        it[WorkoutTemplatesTable.userId] = session.userId
                        it[WorkoutTemplatesTable.name] = copiedName
                        it[WorkoutTemplatesTable.description] = originalTemplate[WorkoutTemplatesTable.description]
                    }[WorkoutTemplatesTable.id]

                    val originalExercises = WorkoutTemplateExercisesTable
                        .selectAll()
                        .where { WorkoutTemplateExercisesTable.templateId eq originalTemplate[WorkoutTemplatesTable.id] }
                        .toList()

                    for (exercise in originalExercises) {
                        WorkoutTemplateExercisesTable.insert {
                            it[WorkoutTemplateExercisesTable.templateId] = copiedTemplateId
                            it[WorkoutTemplateExercisesTable.exerciseId] = exercise[WorkoutTemplateExercisesTable.exerciseId]
                        }
                    }

                    TemplateSharesTable.update({ TemplateSharesTable.id eq shareId }) {
                        it[TemplateSharesTable.status] = "saved"
                    }

                    "saved"
                }
            }
        }

        if (result == "saved") {
            call.respondRedirect("/friends?msg=template_saved")
        } else {
            call.respondRedirect("/friends")
        }
    }

    post("/friends/templates/decline/{shareId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val shareId = call.parameters["shareId"]?.toIntOrNull()

        if (shareId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        transaction {
            val share = TemplateSharesTable
                .selectAll()
                .where { TemplateSharesTable.id eq shareId }
                .singleOrNull()

            if (share != null &&
                share[TemplateSharesTable.receiverId] == session.userId &&
                share[TemplateSharesTable.status] == "pending"
            ) {
                TemplateSharesTable.update({ TemplateSharesTable.id eq shareId }) {
                    it[TemplateSharesTable.status] = "declined"
                }
            }
        }

        call.respondRedirect("/friends?msg=template_declined")
    }

    post("/friends/templates/cancel/{shareId}") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val shareId = call.parameters["shareId"]?.toIntOrNull()

        if (shareId == null) {
            call.respondRedirect("/friends")
            return@post
        }

        transaction {
            val share = TemplateSharesTable
                .selectAll()
                .where { TemplateSharesTable.id eq shareId }
                .singleOrNull()

            if (share != null &&
                share[TemplateSharesTable.senderId] == session.userId &&
                share[TemplateSharesTable.status] == "pending"
            ) {
                TemplateSharesTable.deleteWhere {
                    TemplateSharesTable.id eq shareId
                }
            }
        }

        call.respondRedirect("/friends?msg=template_cancelled")
    }
}

data class FriendsPageData(
    val friends: List<FriendDisplay>,
    val incomingRequests: List<FriendRequestDisplay>,
    val outgoingRequests: List<FriendRequestDisplay>,
    val myTemplates: List<FriendTemplateChoice>,
    val receivedTemplates: List<ReceivedTemplateShareDisplay>,
    val sentTemplates: List<SentTemplateShareDisplay>
)

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
                val otherUserId = if (requesterId == userId) {
                    addresseeId
                } else {
                    requesterId
                }

                val otherUser = UsersTable
                    .selectAll()
                    .where { UsersTable.id eq otherUserId }
                    .singleOrNull()

                if (otherUser != null) {
                    val firstName = otherUser[UsersTable.name]
                    val surname = otherUser[UsersTable.surname] ?: ""
                    val fullName = "$firstName $surname".trim()

                    friends.add(
                        FriendDisplay(
                            friendshipId = friendshipId,
                            userId = otherUserId,
                            fullName = fullName,
                            username = otherUser[UsersTable.username],
                            email = otherUser[UsersTable.email]
                        )
                    )
                }
            }

            if (status == "pending" && addresseeId == userId) {
                val requester = UsersTable
                    .selectAll()
                    .where { UsersTable.id eq requesterId }
                    .singleOrNull()

                if (requester != null) {
                    val firstName = requester[UsersTable.name]
                    val surname = requester[UsersTable.surname] ?: ""
                    val fullName = "$firstName $surname".trim()

                    incomingRequests.add(
                        FriendRequestDisplay(
                            friendshipId = friendshipId,
                            userId = requesterId,
                            fullName = fullName,
                            username = requester[UsersTable.username],
                            email = requester[UsersTable.email]
                        )
                    )
                }
            }

            if (status == "pending" && requesterId == userId) {
                val addressee = UsersTable
                    .selectAll()
                    .where { UsersTable.id eq addresseeId }
                    .singleOrNull()

                if (addressee != null) {
                    val firstName = addressee[UsersTable.name]
                    val surname = addressee[UsersTable.surname] ?: ""
                    val fullName = "$firstName $surname".trim()

                    outgoingRequests.add(
                        FriendRequestDisplay(
                            friendshipId = friendshipId,
                            userId = addresseeId,
                            fullName = fullName,
                            username = addressee[UsersTable.username],
                            email = addressee[UsersTable.email]
                        )
                    )
                }
            }
        }

        val myTemplates = loadTemplateChoicesForFriendPage(userId)
        val receivedTemplates = loadReceivedTemplateShares(userId)
        val sentTemplates = loadSentTemplateShares(userId)

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

fun loadTemplateChoicesForFriendPage(userId: Int): List<FriendTemplateChoice> {
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
    val shares = TemplateSharesTable
        .selectAll()
        .where {
            (TemplateSharesTable.receiverId eq userId) and
                    (TemplateSharesTable.status eq "pending")
        }
        .orderBy(TemplateSharesTable.id, SortOrder.DESC)
        .toList()

    val list = mutableListOf<ReceivedTemplateShareDisplay>()

    for (share in shares) {
        val sender = UsersTable
            .selectAll()
            .where { UsersTable.id eq share[TemplateSharesTable.senderId] }
            .singleOrNull()

        val template = WorkoutTemplatesTable
            .selectAll()
            .where { WorkoutTemplatesTable.id eq share[TemplateSharesTable.templateId] }
            .singleOrNull()

        if (sender != null && template != null) {
            val firstName = sender[UsersTable.name]
            val surname = sender[UsersTable.surname] ?: ""
            val senderName = "$firstName $surname".trim()

            val count = WorkoutTemplateExercisesTable
                .selectAll()
                .where { WorkoutTemplateExercisesTable.templateId eq template[WorkoutTemplatesTable.id] }
                .count()
                .toInt()

            list.add(
                ReceivedTemplateShareDisplay(
                    shareId = share[TemplateSharesTable.id],
                    templateName = template[WorkoutTemplatesTable.name],
                    senderName = senderName,
                    senderUsername = sender[UsersTable.username],
                    exerciseCount = count
                )
            )
        }
    }

    return list
}

fun loadSentTemplateShares(userId: Int): List<SentTemplateShareDisplay> {
    val shares = TemplateSharesTable
        .selectAll()
        .where { TemplateSharesTable.senderId eq userId }
        .orderBy(TemplateSharesTable.id, SortOrder.DESC)
        .toList()
        .filter {
            val status = it[TemplateSharesTable.status]
            status == "pending" || status == "saved"
        }

    val list = mutableListOf<SentTemplateShareDisplay>()

    for (share in shares) {
        val receiver = UsersTable
            .selectAll()
            .where { UsersTable.id eq share[TemplateSharesTable.receiverId] }
            .singleOrNull()

        val template = WorkoutTemplatesTable
            .selectAll()
            .where { WorkoutTemplatesTable.id eq share[TemplateSharesTable.templateId] }
            .singleOrNull()

        if (receiver != null && template != null) {
            val firstName = receiver[UsersTable.name]
            val surname = receiver[UsersTable.surname] ?: ""
            val receiverName = "$firstName $surname".trim()

            var statusText = "Pending"

            if (share[TemplateSharesTable.status] == "saved") {
                statusText = "Saved"
            }

            list.add(
                SentTemplateShareDisplay(
                    shareId = share[TemplateSharesTable.id],
                    templateName = template[WorkoutTemplatesTable.name],
                    receiverName = receiverName,
                    receiverUsername = receiver[UsersTable.username],
                    status = statusText
                )
            )
        }
    }

    return list
}

fun buildFriendProfileStats(userId: Int): FriendProfileStats {
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

            categoryCount[category] = (categoryCount[category] ?: 0) + 1

            val sets = ActivitySetsTable
                .selectAll()
                .where { ActivitySetsTable.activityId eq activity[ActivitiesTable.id] }
                .orderBy(ActivitySetsTable.setNumber, SortOrder.ASC)
                .toList()

            totalSets += sets.size

            var bestAmount = 0.0

            for (set in sets) {
                val amount = set[ActivitySetsTable.amount]

                if (amount > bestAmount) {
                    bestAmount = amount
                }

                val oldRecord = records[exerciseName]

                if (oldRecord == null || amount > oldRecord.amount) {
                    records[exerciseName] = PersonalRecord(
                        exerciseName = exerciseName,
                        amount = amount,
                        unit = unit
                    )
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

    var topCategory = "None yet"

    if (categoryCount.isNotEmpty()) {
        topCategory = categoryCount.maxByOrNull { it.value }!!.key
    }

    val recordList = records.values
        .sortedByDescending { it.amount }
        .take(5)

    return FriendProfileStats(
        totalActivities = activityRows.size,
        totalSets = totalSets,
        topCategory = topCategory,
        personalRecords = recordList,
        recentActivities = recentActivities
    )
}