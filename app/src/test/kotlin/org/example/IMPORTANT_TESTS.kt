package org.example.testing.pages

import org.example.pages.ActivityDisplay
import org.example.pages.formatDate
import org.example.pages.formatMonth
import org.example.pages.getMonthKey
import org.example.pages.renderActivitiesPage
import org.example.pages.renderExerciseSearchPage
import org.example.pages.renderLoginPage
import org.example.pages.renderRegisterPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImportantTests {

    @Test
    fun `login page has required username and password fields`() {
        val html = renderLoginPage()

        assertTrue(html.contains("name=\"username\""))
        assertTrue(html.contains("name=\"password\""))
        assertTrue(html.contains("required"))
    }

    @Test
    fun `login page shows error message`() {
        val html = renderLoginPage("Invalid login details")

        assertTrue(html.contains("Invalid login details"))
        assertTrue(html.contains("role='alert'") || html.contains("role=\"alert\""))
    }

    @Test
    fun `register page has required account fields`() {
        val html = renderRegisterPage()

        assertTrue(html.contains("name=\"name\""))
        assertTrue(html.contains("name=\"surname\""))
        assertTrue(html.contains("name=\"username\""))
        assertTrue(html.contains("name=\"email\""))
        assertTrue(html.contains("name=\"password\""))
        assertTrue(html.contains("required"))
    }

    @Test
    fun `register page shows password guidance`() {
        val html = renderRegisterPage()

        assertTrue(html.contains("Use at least 8 characters"))
        assertTrue(html.contains("one capital letter"))
        assertTrue(html.contains("one number"))
    }

    @Test
    fun `date and month helpers work with normal values`() {
        assertEquals("08 Dec 2025", formatDate("2025-12-08"))
        assertEquals("December 2025", formatMonth("2025-12"))
        assertEquals("2025-12", getMonthKey("2025-12-08"))
    }

    @Test
    fun `date and month helpers do not crash with invalid values`() {
        assertEquals("invalid", formatDate("invalid"))
        assertEquals("invalid", formatMonth("invalid"))
    }

    @Test
    fun `activities page handles empty activity list`() {
        val html = renderActivitiesPage(
            activities = emptyList(),
            selectedMonth = "2025-12",
            previousMonth = "2025-11",
            nextMonth = "2026-01"
        )

        assertTrue(html.contains("No activities for December 2025"))
        assertTrue(html.contains("Log New Activity"))
    }

    @Test
    fun `activity notes with script tags are escaped`() {
        val activities = listOf(
            ActivityDisplay(
                activityId = 1,
                exerciseName = "Bench Press",
                category = "Chest",
                unit = "kg",
                date = "2025-12-08",
                notes = "<script>alert('bad')</script>",
                sets = listOf(80.0)
            )
        )

        val html = renderActivitiesPage(
            activities = activities,
            selectedMonth = "2025-12",
            previousMonth = "2025-11",
            nextMonth = "2026-01"
        )

        assertFalse(html.contains("<script>alert('bad')</script>"))
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&lt;script"))
    }

    @Test
    fun `exercise search page handles empty search`() {
        val html = renderExerciseSearchPage(
            exercises = emptyList(),
            categories = listOf("Chest", "Back", "Legs"),
            selectedCategory = "",
            search = ""
        )

        assertTrue(html.contains("No exercises found"))
        assertTrue(html.contains("All Categories"))
    }

    @Test
    fun `exercise search page handles special characters safely`() {
        val html = renderExerciseSearchPage(
            exercises = emptyList(),
            categories = listOf("Chest", "Back", "Legs"),
            selectedCategory = "",
            search = "<script>"
        )

        assertFalse(html.contains("value=\"<script>\""))
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&lt;script"))
    }

    @Test
    fun `exercise search page has labelled search and category fields`() {
        val html = renderExerciseSearchPage(
            exercises = emptyList(),
            categories = listOf("Chest", "Back"),
            selectedCategory = "",
            search = ""
        )

        assertTrue(html.contains("for=\"exercise-search\""))
        assertTrue(html.contains("id=\"exercise-search\""))
        assertTrue(html.contains("for=\"exercise-category\""))
        assertTrue(html.contains("id=\"exercise-category\""))
    }
}