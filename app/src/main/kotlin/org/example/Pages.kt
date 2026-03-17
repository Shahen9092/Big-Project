package org.example

fun renderLoginPage(error: String? = null): String {
    val errorHtml = error?.let { "<p style='color:red;'>$it</p>" } ?: ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Login</title>
        </head>
        <body>
            <h1>Login</h1>
            $errorHtml

            <form method="post" action="/login">
                <div>
                    <label>Email</label>
                    <input type="email" name="email" required>
                </div>

                <br>

                <div>
                    <label>Password</label>
                    <input type="password" name="password" required>
                </div>

                <br>

                <button type="submit">Login</button>
            </form>

            <p>
                Don't have an account?
                <a href="/register">Register</a>
            </p>
        </body>
        </html>
    """.trimIndent()
}

fun renderRegisterPage(error: String? = null): String {
    val errorHtml = error?.let { "<p style='color:red;'>$it</p>" } ?: ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Register</title>
        </head>
        <body>
            <h1>Register</h1>
            $errorHtml

            <form method="post" action="/register">
                <div>
                    <label>Name</label>
                    <input type="text" name="name" required>
                </div>

                <br>

                <div>
                    <label>Email</label>
                    <input type="email" name="email" required>
                </div>

                <br>

                <div>
                    <label>Password</label>
                    <input type="password" name="password" required>
                </div>

                <br>

                <button type="submit">Register</button>
            </form>

            <p>
                Already have an account?
                <a href="/login">Login</a>
            </p>
        </body>
        </html>
    """.trimIndent()
}

fun renderDashboardPage(email: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Dashboard</title>
        </head>
        <body>
            <h1>Dashboard</h1>
            <p>Welcome $email</p>
            <p><a href="/logout">Logout</a></p>
        </body>
        </html>
    """.trimIndent()
}