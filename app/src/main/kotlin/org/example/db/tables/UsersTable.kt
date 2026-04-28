package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val surname = varchar("surname", 100).nullable()
    val username = varchar("username", 100).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)

    override val primaryKey = PrimaryKey(id)
}