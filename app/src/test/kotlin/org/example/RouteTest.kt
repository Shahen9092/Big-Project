package org.example.testing.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.example.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTest {

    @Test
    fun `home page route works`() = testApplication {
        application {
            module()
        }

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().isNotBlank())
    }

    @Test
    fun `login page route works`() = testApplication {
        application {
            module()
        }

        val response = client.get("/login")
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Login"))
    }

    @Test
    fun `register page route works`() = testApplication {
        application {
            module()
        }

        val response = client.get("/register")
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Register"))
    }

    @Test
    fun `dashboard redirects to login when user is not logged in`() = testApplication {
        application {
            module()
        }

        val response = client.get("/dashboard")

        assertTrue(
            response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther
        )

        assertEquals("/login", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `activities page redirects to login when user is not logged in`() = testApplication {
        application {
            module()
        }

        val response = client.get("/activities")

        assertTrue(
            response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther
        )

        assertEquals("/login", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `new activity page redirects to login when user is not logged in`() = testApplication {
        application {
            module()
        }

        val response = client.get("/activities/new")

        assertTrue(
            response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther
        )

        assertEquals("/login", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `goals page redirects to login when user is not logged in`() = testApplication {
        application {
            module()
        }

        val response = client.get("/goals")

        assertTrue(
            response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther
        )

        assertEquals("/login", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `templates page redirects to login when user is not logged in`() = testApplication {
        application {
            module()
        }

        val response = client.get("/templates")

        assertTrue(
            response.status == HttpStatusCode.Found ||
                response.status == HttpStatusCode.SeeOther
        )

        assertEquals("/login", response.headers[HttpHeaders.Location])
    }
}