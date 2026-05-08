package org.example.routes

import org.example.db.tables.TemplateSharesTable
import org.example.db.tables.UsersTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun sendTemplateToFriend(
    currentUserId: Int,
    friendUserId: Int,
    templateId: Int
): String {

    return transaction {
        var result = "notfriend"

        val usersAreFriends = usersAreFriendsInsideTransaction(
            firstUserId = currentUserId,
            secondUserId = friendUserId
        )

        if (usersAreFriends) {
            val template = WorkoutTemplatesTable
                .selectAll()
                .where { WorkoutTemplatesTable.id eq templateId }
                .singleOrNull()

            if (template == null) {
                result = "template_notfound"
            } else {
                if (template[WorkoutTemplatesTable.userId] != currentUserId) {
                    result = "template_notfound"
                } else {
                    val shareExists = templateShareAlreadyExistsInsideTransaction(
                        senderId = currentUserId,
                        receiverId = friendUserId,
                        templateId = templateId
                    )

                    if (shareExists) {
                        result = "templateexists"
                    } else {
                        TemplateSharesTable.insert {
                            it[TemplateSharesTable.senderId] = currentUserId
                            it[TemplateSharesTable.receiverId] = friendUserId
                            it[TemplateSharesTable.templateId] = templateId
                            it[TemplateSharesTable.status] = "pending"
                        }

                        result = "sent"
                    }
                }
            }
        }

        result
    }
}

fun templateShareAlreadyExistsInsideTransaction(
    senderId: Int,
    receiverId: Int,
    templateId: Int
): Boolean {

    val shares = TemplateSharesTable
        .selectAll()
        .where { TemplateSharesTable.senderId eq senderId }
        .toList()

    for (share in shares) {
        val shareReceiverId = share[TemplateSharesTable.receiverId]
        val shareTemplateId = share[TemplateSharesTable.templateId]
        val status = share[TemplateSharesTable.status]

        var sameReceiver = false
        var sameTemplate = false
        var activeStatus = false

        if (shareReceiverId == receiverId) {
            sameReceiver = true
        }

        if (shareTemplateId == templateId) {
            sameTemplate = true
        }

        if (status == "pending") {
            activeStatus = true
        }

        if (status == "saved") {
            activeStatus = true
        }

        if (sameReceiver && sameTemplate && activeStatus) {
            return true
        }
    }

    return false
}

fun saveReceivedTemplate(
    currentUserId: Int,
    shareId: Int
): String {

    return transaction {
        var result = "bad"

        val share = TemplateSharesTable
            .selectAll()
            .where { TemplateSharesTable.id eq shareId }
            .singleOrNull()

        if (share != null) {
            val receiverId = share[TemplateSharesTable.receiverId]
            val status = share[TemplateSharesTable.status]

            if (receiverId == currentUserId && status == "pending") {
                val originalTemplate = WorkoutTemplatesTable
                    .selectAll()
                    .where { WorkoutTemplatesTable.id eq share[TemplateSharesTable.templateId] }
                    .singleOrNull()

                if (originalTemplate != null) {
                    val senderUsername = findSenderUsernameInsideTransaction(
                        senderId = share[TemplateSharesTable.senderId]
                    )

                    var copiedName = originalTemplate[WorkoutTemplatesTable.name] + " (from @$senderUsername)"

                    if (copiedName.length > 100) {
                        copiedName = copiedName.substring(0, 100)
                    }

                    val copiedTemplateId = WorkoutTemplatesTable.insert {
                        it[WorkoutTemplatesTable.userId] = currentUserId
                        it[WorkoutTemplatesTable.name] = copiedName
                        it[WorkoutTemplatesTable.description] = originalTemplate[WorkoutTemplatesTable.description]
                    }[WorkoutTemplatesTable.id]

                    copyTemplateExercisesInsideTransaction(
                        originalTemplateId = originalTemplate[WorkoutTemplatesTable.id],
                        copiedTemplateId = copiedTemplateId
                    )

                    TemplateSharesTable.update({ TemplateSharesTable.id eq shareId }) {
                        it[TemplateSharesTable.status] = "saved"
                    }

                    result = "saved"
                }
            }
        }

        result
    }
}

fun findSenderUsernameInsideTransaction(senderId: Int): String {

    var senderUsername = "friend"

    val sender = UsersTable
        .selectAll()
        .where { UsersTable.id eq senderId }
        .singleOrNull()

    if (sender != null) {
        senderUsername = sender[UsersTable.username]
    }

    return senderUsername
}

fun copyTemplateExercisesInsideTransaction(
    originalTemplateId: Int,
    copiedTemplateId: Int
) {

    val originalExercises = WorkoutTemplateExercisesTable
        .selectAll()
        .where { WorkoutTemplateExercisesTable.templateId eq originalTemplateId }
        .toList()

    for (exercise in originalExercises) {
        WorkoutTemplateExercisesTable.insert {
            it[WorkoutTemplateExercisesTable.templateId] = copiedTemplateId
            it[WorkoutTemplateExercisesTable.exerciseId] = exercise[WorkoutTemplateExercisesTable.exerciseId]
        }
    }
}

fun declineTemplateShare(
    currentUserId: Int,
    shareId: Int
) {

    transaction {
        val share = TemplateSharesTable
            .selectAll()
            .where { TemplateSharesTable.id eq shareId }
            .singleOrNull()

        if (share != null) {
            val receiverId = share[TemplateSharesTable.receiverId]
            val status = share[TemplateSharesTable.status]

            if (receiverId == currentUserId) {
                if (status == "pending") {
                    TemplateSharesTable.update({ TemplateSharesTable.id eq shareId }) {
                        it[TemplateSharesTable.status] = "declined"
                    }
                }
            }
        }
    }
}

fun cancelTemplateShare(
    currentUserId: Int,
    shareId: Int
) {

    transaction {
        val share = TemplateSharesTable
            .selectAll()
            .where { TemplateSharesTable.id eq shareId }
            .singleOrNull()

        if (share != null) {
            val senderId = share[TemplateSharesTable.senderId]
            val status = share[TemplateSharesTable.status]

            if (senderId == currentUserId) {
                if (status == "pending") {
                    TemplateSharesTable.deleteWhere {
                        TemplateSharesTable.id eq shareId
                    }
                }
            }
        }
    }
}