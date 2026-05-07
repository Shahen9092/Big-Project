package org.example.pages

fun renderLoginPage(error: String? = null): String {
    var errorHtml = ""
    var errorDescription = ""

    if (error != null) {
        errorHtml = "<p id='login-error' class='error' role='alert'>${escapeLoginHtml(error)}</p>"
        errorDescription = "aria-describedby=\"login-error\""
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
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
                            <a href="/" aria-label="Go back to home page">← Back</a>
                        </div>
                    </div>

                    <h1>Login</h1>

                    $errorHtml

                    <form method="post" action="/login">
                        <label for="username">Username</label><br>
                        <input 
                            id="username"
                            type="text" 
                            name="username"
                            placeholder="Enter your username"
                            autocomplete="username"
                            $errorDescription
                            required
                        >

                        <br><br>

                        <label for="password">Password</label><br>
                        <input 
                            id="password"
                            type="password" 
                            name="password"
                            placeholder="Enter your password"
                            autocomplete="current-password"
                            $errorDescription
                            required
                        >

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

fun escapeLoginHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
}