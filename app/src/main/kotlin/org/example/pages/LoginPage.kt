package org.example.pages

fun renderLoginPage(error: String? = null): String {
    var errorHtml = ""

    if (error != null) {
        errorHtml = "<p class='error'>$error</p>"
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Login</title>
            ${pageCss()}
        </head>
        <body>
            <main>
                <div class="box">

                    <div class="top-logo">
                        <img src="/logo.png" alt="Fitness Tracker Logo">
                    </div>

                    <div class="nav">
                        <div>
                            <a href="/">← Back</a>
                        </div>
                    </div>

                    <h1>Login</h1>

                    $errorHtml

                    <form method="post" action="/login">
                        <label>Username</label><br>
                        <input type="text" name="username" required>

                        <br><br>

                        <label>Password</label><br>
                        <input type="password" name="password" required>

                        <br><br>

                        <button type="submit">Login</button>
                    </form>

                    <p>
                        Don't have an account?
                        <a href="/register">Register</a>
                    </p>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}