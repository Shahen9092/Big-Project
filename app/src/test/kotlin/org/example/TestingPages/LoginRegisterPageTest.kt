package org.example.testing.pages

import org.example.pages.renderLoginPage
import org.example.pages.renderRegisterPage
import kotlin.test.Test
import kotlin.test.assertTrue

class LoginRegisterPageTest {

    @Test
    fun `login page has basic accessibility fixes`() {
        val html = renderLoginPage()

        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("alt=\"Fitness Tracker Logo\""))
        assertTrue(html.contains("for=\"username\""))
        assertTrue(html.contains("id=\"username\""))
        assertTrue(html.contains("for=\"password\""))
        assertTrue(html.contains("id=\"password\""))
    }

    @Test
    fun `login page has correct form fields`() {
        val html = renderLoginPage()

        assertTrue(html.contains("name=\"username\""))
        assertTrue(html.contains("name=\"password\""))
        assertTrue(html.contains("autocomplete=\"username\""))
        assertTrue(html.contains("autocomplete=\"current-password\""))
    }

    @Test
    fun `login page shows error message`() {
        val html = renderLoginPage("Invalid login details")

        assertTrue(html.contains("Invalid login details"))
        assertTrue(html.contains("role='alert'") || html.contains("role=\"alert\""))
    }

    @Test
    fun `register page has basic accessibility fixes`() {
        val html = renderRegisterPage()

        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("alt=\"Fitness Tracker Logo\""))
        assertTrue(html.contains("for=\"first-name\""))
        assertTrue(html.contains("id=\"first-name\""))
        assertTrue(html.contains("for=\"email\""))
        assertTrue(html.contains("id=\"email\""))
        assertTrue(html.contains("for=\"password\""))
        assertTrue(html.contains("id=\"password\""))
    }

    @Test
    fun `register page has correct form fields`() {
        val html = renderRegisterPage()

        assertTrue(html.contains("name=\"name\""))
        assertTrue(html.contains("name=\"surname\""))
        assertTrue(html.contains("name=\"username\""))
        assertTrue(html.contains("name=\"email\""))
        assertTrue(html.contains("name=\"password\""))
    }

    @Test
    fun `register page includes password help and error message`() {
        val normalHtml = renderRegisterPage()
        val errorHtml = renderRegisterPage("Username already exists")

        assertTrue(normalHtml.contains("Use at least 8 characters"))
        assertTrue(normalHtml.contains("id=\"password-help\""))
        assertTrue(errorHtml.contains("Username already exists"))
        assertTrue(errorHtml.contains("role='alert'") || errorHtml.contains("role=\"alert\""))
    }
}