package org.example.testing.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.example.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTest {

    @Test
    fun `home route works`() = testApplication {
        application {
            module()
        }

        val response = client.get("/")
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.isNotBlank())
    }

    @Test
    fun `login route works`() = testApplication {
        application {
            module()
        }

        val response = client.get("/login")
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Login"))
        assertTrue(html.contains("Username"))
        assertTrue(html.contains("Password"))
    }

    @Test
    fun `register route works`() = testApplication {
        application {
            module()
        }

        val response = client.get("/register")
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Register"))
        assertTrue(html.contains("Username"))
        assertTrue(html.contains("Email"))
        assertTrue(html.contains("Password"))
    }

    @Test
    fun `unknown route returns not found`() = testApplication {
        application {
            module()
        }

        val response = client.get("/this-page-does-not-exist")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}