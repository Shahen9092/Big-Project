package org.example.routes

import org.example.db.tables.FriendshipsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun sendFriendRequest(
    currentUserId: Int,
    identifier: String
): String {

    return transaction {
        var result = "notfound"

        val targetUser = findUserByIdentifierInsideTransaction(identifier)

        if (targetUser != null) {
            val targetUserId = targetUser[org.example.db.tables.UsersTable.id]

            if (targetUserId == currentUserId) {
                result = "self"
            } else {
                val existingFriendship = findFriendshipBetweenUsersInsideTransaction(
                    firstUserId = currentUserId,
                    secondUserId = targetUserId
                )

                if (existingFriendship != null) {
                    result = "exists"
                } else {
                    FriendshipsTable.insert {
                        it[FriendshipsTable.requesterId] = currentUserId
                        it[FriendshipsTable.addresseeId] = targetUserId
                        it[FriendshipsTable.status] = "pending"
                    }

                    result = "sent"
                }
            }
        }

        result
    }
}

fun acceptFriendRequest(
    friendshipId: Int,
    currentUserId: Int
) {

    transaction {
        val request = FriendshipsTable
            .selectAll()
            .where { FriendshipsTable.id eq friendshipId }
            .singleOrNull()

        if (request != null) {
            if (request[FriendshipsTable.addresseeId] == currentUserId) {
                FriendshipsTable.update({ FriendshipsTable.id eq friendshipId }) {
                    it[FriendshipsTable.status] = "accepted"
                }
            }
        }
    }
}

fun declineFriendRequest(
    friendshipId: Int,
    currentUserId: Int
) {

    transaction {
        val request = FriendshipsTable
            .selectAll()
            .where { FriendshipsTable.id eq friendshipId }
            .singleOrNull()

        if (request != null) {
            if (request[FriendshipsTable.addresseeId] == currentUserId) {
                FriendshipsTable.deleteWhere {
                    FriendshipsTable.id eq friendshipId
                }
            }
        }
    }
}

fun cancelFriendRequest(
    friendshipId: Int,
    currentUserId: Int
) {

    transaction {
        val request = FriendshipsTable
            .selectAll()
            .where { FriendshipsTable.id eq friendshipId }
            .singleOrNull()

        if (request != null) {
            if (request[FriendshipsTable.requesterId] == currentUserId) {
                FriendshipsTable.deleteWhere {
                    FriendshipsTable.id eq friendshipId
                }
            }
        }
    }
}

fun removeFriend(
    friendshipId: Int,
    currentUserId: Int
) {

    transaction {
        val friendship = FriendshipsTable
            .selectAll()
            .where { FriendshipsTable.id eq friendshipId }
            .singleOrNull()

        if (friendship != null) {
            val requesterId = friendship[FriendshipsTable.requesterId]
            val addresseeId = friendship[FriendshipsTable.addresseeId]

            var userIsPartOfFriendship = false

            if (requesterId == currentUserId) {
                userIsPartOfFriendship = true
            }

            if (addresseeId == currentUserId) {
                userIsPartOfFriendship = true
            }

            if (userIsPartOfFriendship) {
                FriendshipsTable.deleteWhere {
                    FriendshipsTable.id eq friendshipId
                }
            }
        }
    }
}