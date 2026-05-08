package org.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.example.db.tables.CalendarEventsTable
import org.example.models.UserSession
import org.example.pages.calendarPage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.calendarRoutes() {

    get("/calendar") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val now = java.util.Calendar.getInstance()
        val month = call.request.queryParameters["month"]?.toIntOrNull()
            ?: (now.get(java.util.Calendar.MONTH) + 1)
        val year = call.request.queryParameters["year"]?.toIntOrNull()
            ?: now.get(java.util.Calendar.YEAR)

        val events: List<Map<String, String>> = transaction {
            CalendarEventsTable.selectAll()
                .where {
                    (CalendarEventsTable.userId eq session.userId) and
                    (CalendarEventsTable.month eq month) and
                    (CalendarEventsTable.year eq year)
                }
                .map { row ->
                    mapOf(
                        "day"  to row[CalendarEventsTable.day].toString(),
                        "type" to row[CalendarEventsTable.type],
                        "note" to (row[CalendarEventsTable.note] ?: "")
                    )
                }
        }

        call.respondText(
            calendarPage(session.username, month, year, events),
            ContentType.Text.Html
        )
    }

    post("/calendar/add") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()
        val day   = params["day"]?.toIntOrNull()   ?: return@post call.respondRedirect("/calendar")
        val month = params["month"]?.toIntOrNull() ?: return@post call.respondRedirect("/calendar")
        val year  = params["year"]?.toIntOrNull()  ?: return@post call.respondRedirect("/calendar")
        val type  = params["type"] ?: "gym"
        val note  = params["note"] ?: ""

        transaction {
            CalendarEventsTable.deleteWhere {
                (CalendarEventsTable.userId eq session.userId) and
                (CalendarEventsTable.day    eq day) and
                (CalendarEventsTable.month  eq month) and
                (CalendarEventsTable.year   eq year)
            }
            CalendarEventsTable.insert {
                it[CalendarEventsTable.userId] = session.userId
                it[CalendarEventsTable.day]    = day
                it[CalendarEventsTable.month]  = month
                it[CalendarEventsTable.year]   = year
                it[CalendarEventsTable.type]   = type
                it[CalendarEventsTable.note]   = note.ifBlank { null }
            }
        }

        call.respondRedirect("/calendar?month=$month&year=$year")
    }

    post("/calendar/delete") {
        val session = call.sessions.get<UserSession>()
        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()
        val day   = params["day"]?.toIntOrNull()   ?: return@post call.respondRedirect("/calendar")
        val month = params["month"]?.toIntOrNull() ?: return@post call.respondRedirect("/calendar")
        val year  = params["year"]?.toIntOrNull()  ?: return@post call.respondRedirect("/calendar")

        transaction {
            CalendarEventsTable.deleteWhere {
                (CalendarEventsTable.userId eq session.userId) and
                (CalendarEventsTable.day    eq day) and
                (CalendarEventsTable.month  eq month) and
                (CalendarEventsTable.year   eq year)
            }
        }

        call.respondRedirect("/calendar?month=$month&year=$year")
    }
}

