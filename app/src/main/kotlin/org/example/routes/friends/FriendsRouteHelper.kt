package org.example.routes

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import org.example.db.tables.FriendshipsTable
import org.example.db.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

fun getFriendQueryText(call: ApplicationCall, name: String): String? {

    val value = call.request.queryParameters[name]

    if (value == null) {
        return null
    }

    return value.trim()
}

fun getFriendRouteInt(call: ApplicationCall, name: String): Int? {

    val value = call.parameters[name]

    if (value == null) {
        return null
    }

    return value.toIntOrNull()
}

fun getFriendFormText(params: Parameters, name: String): String {

    var result = ""

    val value = params[name]

    if (value != null) {
        result = value.trim()
    }

    return result
}

fun getFriendFormInt(params: Parameters, name: String): Int? {

    val value = params[name]

    if (value == null) {
        return null
    }

    return value.toIntOrNull()
}

fun makeFriendFullName(user: ResultRow): String {

    val firstName = user[UsersTable.name]

    var surname = ""

    val surnameFromDatabase = user[UsersTable.surname]

    if (surnameFromDatabase != null) {
        surname = surnameFromDatabase
    }

    return "$firstName $surname".trim()
}

fun findUserByIdInsideTransaction(userId: Int): ResultRow? {

    val user = UsersTable
        .selectAll()
        .where { UsersTable.id eq userId }
        .singleOrNull()

    return user
}

fun findUserByIdentifierInsideTransaction(identifier: String): ResultRow? {

    var user = UsersTable
        .selectAll()
        .where { UsersTable.username eq identifier }
        .singleOrNull()

    if (user == null) {
        user = UsersTable
            .selectAll()
            .where { UsersTable.email eq identifier }
            .singleOrNull()
    }

    return user
}

fun findFriendshipBetweenUsersInsideTransaction(
    firstUserId: Int,
    secondUserId: Int
): ResultRow? {

    val friendships = FriendshipsTable
        .selectAll()
        .where {
            (FriendshipsTable.requesterId eq firstUserId) or
                    (FriendshipsTable.addresseeId eq firstUserId)
        }
        .toList()

    for (friendship in friendships) {
        val requesterId = friendship[FriendshipsTable.requesterId]
        val addresseeId = friendship[FriendshipsTable.addresseeId]

        var sameDirection = false
        var oppositeDirection = false

        if (requesterId == firstUserId && addresseeId == secondUserId) {
            sameDirection = true
        }

        if (requesterId == secondUserId && addresseeId == firstUserId) {
            oppositeDirection = true
        }

        if (sameDirection || oppositeDirection) {
            return friendship
        }
    }

    return null
}

fun usersAreFriendsInsideTransaction(
    firstUserId: Int,
    secondUserId: Int
): Boolean {

    val friendship = findFriendshipBetweenUsersInsideTransaction(
        firstUserId = firstUserId,
        secondUserId = secondUserId
    )

    if (friendship == null) {
        return false
    }

    if (friendship[FriendshipsTable.status] == "accepted") {
        return true
    }

    return false
}