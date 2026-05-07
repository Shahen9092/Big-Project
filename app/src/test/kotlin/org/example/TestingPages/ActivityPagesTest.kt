package org.example.testing.pages

import org.example.pages.ActivityDisplay
import org.example.pages.formatAmount
import org.example.pages.formatDate
import org.example.pages.formatMonth
import org.example.pages.getMonthKey
import org.example.pages.renderActivitiesPage
import org.example.pages.renderExerciseSearchPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityPagesTest {

    @Test
    fun `format helpers work properly`() {
        assertEquals("10", formatAmount(10.0))
        assertEquals("10.5", formatAmount(10.5))
        assertEquals("2025-12", getMonthKey("2025-12-08"))
        assertEquals("December 2025", formatMonth("2025-12"))
        assertEquals("08 Dec 2025", formatDate("2025-12-08"))
    }

    @Test
    fun `exercise search page has accessibility fixes`() {
        val html = renderExerciseSearchPage(
            exercises = emptyList(),
            categories = listOf("Chest", "Back", "Legs"),
            selectedCategory = "",
            search = ""
        )

        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("alt=\"Fitness Tracker Logo\""))
        assertTrue(html.contains("for=\"exercise-search\""))
        assertTrue(html.contains("id=\"exercise-search\""))
        assertTrue(html.contains("for=\"exercise-category\""))
        assertTrue(html.contains("id=\"exercise-category\""))
    }

    @Test
    fun `exercise search page shows no results message`() {
        val html = renderExerciseSearchPage(
            exercises = emptyList(),
            categories = listOf("Chest", "Back"),
            selectedCategory = "",
            search = ""
        )

        assertTrue(html.contains("No exercises found"))
        assertTrue(html.contains("Try changing the search or category filter"))
    }

    @Test
    fun `activities page shows empty state`() {
        val html = renderActivitiesPage(
            activities = emptyList(),
            selectedMonth = "2025-12",
            previousMonth = "2025-11",
            nextMonth = "2026-01"
        )

        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("No activities for December 2025"))
    }

    @Test
    fun `activities page shows success messages`() {
        val savedHtml = renderActivitiesPage(emptyList(), "2025-12", "2025-11", "2026-01", "saved")
        val updatedHtml = renderActivitiesPage(emptyList(), "2025-12", "2025-11", "2026-01", "updated")
        val deletedHtml = renderActivitiesPage(emptyList(), "2025-12", "2025-11", "2026-01", "deleted")

        assertTrue(savedHtml.contains("Activity saved successfully."))
        assertTrue(updatedHtml.contains("Activity updated successfully."))
        assertTrue(deletedHtml.contains("Activity deleted successfully."))
    }

    @Test
    fun `activities page shows logged activity details`() {
        val activities = listOf(
            ActivityDisplay(
                activityId = 1,
                exerciseName = "Bench Press",
                category = "Chest",
                unit = "kg",
                date = "2025-12-08",
                notes = "Felt strong",
                sets = listOf(60.0, 70.0, 80.0)
            )
        )

        val html = renderActivitiesPage(
            activities = activities,
            selectedMonth = "2025-12",
            previousMonth = "2025-11",
            nextMonth = "2026-01"
        )

        assertTrue(html.contains("Bench Press"))
        assertTrue(html.contains("Chest"))
        assertTrue(html.contains("3 sets"))
        assertTrue(html.contains("Best: 80 kg"))
        assertTrue(html.contains("Felt strong"))
    }
}