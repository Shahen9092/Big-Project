package org.example.pages

fun renderRegisterPage(error: String? = null): String {
    var errorHtml = ""

    if (error != null) {
        errorHtml = "<p class='error'>$error</p>"
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Register</title>
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

                    <h1>Register</h1>

                    $errorHtml

                    <form method="post" action="/register">
                        <label>First Name</label><br>
                        <input type="text" name="name" required>

                        <br><br>

                        <label>Surname</label><br>
                        <input type="text" name="surname" required>

                        <br><br>

                        <label>Username</label><br>
                        <input type="text" name="username" required>

                        <br>

                        <label>Email</label><br>
                        <input type="email" name="email" required>

                        <br><br>

                        <label>Password</label><br>
                        <input type="password" name="password" required>
                        <p class="muted">Use at least 8 characters, one capital letter and one number.</p>

                        <br>

                        <button type="submit">Register</button>
                    </form>

                    <p>
                        Already have an account?
                        <a href="/login">Login</a>
                    </p>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}