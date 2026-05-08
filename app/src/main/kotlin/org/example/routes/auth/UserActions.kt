package org.example.routes

import org.example.db.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun loadUserByUsername(username: String): ResultRow? {

    return transaction {
        val user = UsersTable
            .selectAll()
            .where { UsersTable.username eq username }
            .singleOrNull()

        user
    }
}

fun loadUserByEmail(email: String): ResultRow? {

    return transaction {
        val user = UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()

        user
    }
}

fun createUser(
    name: String,
    surname: String,
    username: String,
    email: String,
    password: String
): Int {

    return transaction {
        val newUserId = UsersTable.insert {
            it[UsersTable.name] = name
            it[UsersTable.surname] = surname
            it[UsersTable.username] = username
            it[UsersTable.email] = email
            it[UsersTable.password] = password
        }[UsersTable.id]

        newUserId
    }
}

fun loadFullNameInsideTransaction(
    userId: Int,
    fallbackUsername: String
): String {

    var fullName = fallbackUsername

    val user = UsersTable
        .selectAll()
        .where { UsersTable.id eq userId }
        .singleOrNull()

    if (user != null) {
        val firstName = user[UsersTable.name]

        var surname = ""
        val surnameFromDatabase = user[UsersTable.surname]

        if (surnameFromDatabase != null) {
            surname = surnameFromDatabase
        }

        fullName = "$firstName $surname".trim()
    }

    return fullName
}