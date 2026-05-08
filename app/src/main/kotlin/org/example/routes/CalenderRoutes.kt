package org.example.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.example.db.tables.CalendarEventsTable
import org.example.models.UserSession
import org.example.pages.calendarPage
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.Calendar

fun Route.calendarRoutes() {

    get("/calendar") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@get
        }

        val now = Calendar.getInstance()

        var month = call.request.queryParameters["month"]?.toIntOrNull()
        var year = call.request.queryParameters["year"]?.toIntOrNull()

        if (month == null) {
            month = now.get(Calendar.MONTH) + 1
        }

        if (year == null) {
            year = now.get(Calendar.YEAR)
        }

        if (month < 1) {
            month = 1
        }

        if (month > 12) {
            month = 12
        }

        val events = transaction {
            CalendarEventsTable
                .selectAll()
                .where {
                    (CalendarEventsTable.userId eq session.userId) and
                    (CalendarEventsTable.month eq month) and
                    (CalendarEventsTable.year eq year)
                }
                .orderBy(CalendarEventsTable.day to SortOrder.ASC)
                .map { row ->
                    mapOf(
                        "day" to row[CalendarEventsTable.day].toString(),
                        "type" to row[CalendarEventsTable.type],
                        "note" to (row[CalendarEventsTable.note] ?: "")
                    )
                }
        }

        val html = calendarPage(session.username, month, year, events)

        call.respondText(html, ContentType.Text.Html)
    }

    post("/calendar/add") {
        val session = call.sessions.get<UserSession>()

        if (session == null) {
            call.respondRedirect("/login")
            return@post
        }

        val params = call.receiveParameters()

        val day = params["day"]?.toIntOrNull()
        val month = params["month"]?.toIntOrNull()
        val year = params["year"]?.toIntOrNull()
        val type = params["type"]
        val note = params["note"]

        if (day == null || month == null || year == null || type == null) {
            call.respondRedirect("/calendar")
            return@post
        }

        transaction {
            CalendarEventsTable.deleteWhere {
                (CalendarEventsTable.userId eq session.userId) and
                (CalendarEventsTable.day eq day) and
                (CalendarEventsTable.month eq month) and
                (CalendarEventsTable.year eq year)
            }

            CalendarEventsTable.insert {
                it[CalendarEventsTable.userId] = session.userId
                it[CalendarEventsTable.day] = day
                it[CalendarEventsTable.month] = month
                it[CalendarEventsTable.year] = year
                it[CalendarEventsTable.type] = type

                if (note == null || note.isBlank()) {
                    it[CalendarEventsTable.note] = null
                } else {
                    it[CalendarEventsTable.note] = note
                }
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

        val day = params["day"]?.toIntOrNull()
        val month = params["month"]?.toIntOrNull()
        val year = params["year"]?.toIntOrNull()

        if (day == null || month == null || year == null) {
            call.respondRedirect("/calendar")
            return@post
        }

        transaction {
            CalendarEventsTable.deleteWhere {
                (CalendarEventsTable.userId eq session.userId) and
                (CalendarEventsTable.day eq day) and
                (CalendarEventsTable.month eq month) and
                (CalendarEventsTable.year eq year)
            }
        }

        call.respondRedirect("/calendar?month=$month&year=$year")
    }
}