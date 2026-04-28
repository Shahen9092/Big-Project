package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object FriendshipsTable : Table("friendships") {
    val id = integer("id").autoIncrement()
    val requesterId = integer("requester_id")
    val addresseeId = integer("addressee_id")
    val status = varchar("status", 20)

    override val primaryKey = PrimaryKey(id)
}