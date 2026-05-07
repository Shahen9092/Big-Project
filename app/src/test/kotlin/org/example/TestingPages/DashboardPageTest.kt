package org.example.testing.pages

import org.example.pages.DashboardStats
import org.example.pages.PersonalRecord
import org.example.pages.renderDashboardPage
import kotlin.test.Test
import kotlin.test.assertTrue

class DashboardPageTest {

    private fun testStats(): DashboardStats {
        return DashboardStats(
            totalActivities = 5,
            totalSets = 10,
            mostTrainedCategory = "Chest",
            lastActivity = "Bench Press",
            personalRecords = listOf(
                PersonalRecord("Bench Press", 80.0, "kg")
            )
        )
    }

    @Test
    fun `dashboard has basic accessibility fixes`() {
        val html = renderDashboardPage("Test User", testStats())

        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("alt=\"Fitness Tracker Logo\""))
    }

    @Test
    fun `dashboard shows the user name`() {
        val html = renderDashboardPage("Test User", testStats())

        assertTrue(html.contains("Test User"))
    }

    @Test
    fun `dashboard shows main feature buttons`() {
        val html = renderDashboardPage("Test User", testStats())

        assertTrue(html.contains("Add Activity"))
        assertTrue(html.contains("View Activities"))
        assertTrue(html.contains("View Progress"))
        assertTrue(html.contains("View Goals"))
        assertTrue(html.contains("View Friends"))
    }

    @Test
    fun `dashboard shows stats`() {
        val html = renderDashboardPage("Test User", testStats())

        assertTrue(html.contains("Total Activities"))
        assertTrue(html.contains("5"))
        assertTrue(html.contains("Total Sets"))
        assertTrue(html.contains("10"))
        assertTrue(html.contains("Chest"))
    }

    @Test
    fun `dashboard shows last activity and personal record`() {
        val html = renderDashboardPage("Test User", testStats())

        assertTrue(html.contains("Last Activity"))
        assertTrue(html.contains("Bench Press"))
        assertTrue(html.contains("Bench Press: 80 kg"))
    }

    @Test
    fun `dashboard shows message when there are no personal records`() {
        val stats = DashboardStats(
            totalActivities = 0,
            totalSets = 0,
            mostTrainedCategory = "None",
            lastActivity = "No activity yet",
            personalRecords = emptyList()
        )

        val html = renderDashboardPage("Test User", stats)

        assertTrue(html.contains("No personal records yet."))
    }
}