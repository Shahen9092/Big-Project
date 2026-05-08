package org.example.db.tables

import org.jetbrains.exposed.sql.Table

object TemplateSharesTable : Table("template_shares") {
    val id = integer("id").autoIncrement()

    val senderId = integer("sender_id")

    val receiverId = integer("receiver_id")

    val templateId = integer("template_id")

    val status = varchar("status", 20)

    override val primaryKey = PrimaryKey(id)
}