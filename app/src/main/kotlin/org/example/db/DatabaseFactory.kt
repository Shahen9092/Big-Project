package org.example.db

import org.example.db.tables.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:sqlite:fitness.db",
            driver = "org.sqlite.JDBC"
        )

        transaction {
            SchemaUtils.create(UsersTable)
        }
    }
}