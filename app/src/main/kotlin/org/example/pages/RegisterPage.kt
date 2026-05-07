package org.example.pages

fun renderRegisterPage(error: String? = null): String {
    var errorHtml = ""
    var errorDescription = ""

    if (error != null) {
        errorHtml = "<p id='register-error' class='error' role='alert'>${escapeRegisterHtml(error)}</p>"
        errorDescription = "aria-describedby=\"register-error\""
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
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
                            <a href="/" aria-label="Go back to home page">← Back</a>
                        </div>
                    </div>

                    <h1>Register</h1>

                    $errorHtml

                    <form method="post" action="/register">
                        <label for="first-name">First Name</label><br>
                        <input 
                            id="first-name"
                            type="text" 
                            name="name"
                            placeholder="Enter your first name"
                            autocomplete="given-name"
                            $errorDescription
                            required
                        >

                        <br><br>

                        <label for="surname">Surname</label><br>
                        <input 
                            id="surname"
                            type="text" 
                            name="surname"
                            placeholder="Enter your surname"
                            autocomplete="family-name"
                            $errorDescription
                            required
                        >

                        <br><br>

                        <label for="username">Username</label><br>
                        <input 
                            id="username"
                            type="text" 
                            name="username"
                            placeholder="Choose a username"
                            autocomplete="username"
                            $errorDescription
                            required
                        >

                        <br><br>

                        <label for="email">Email</label><br>
                        <input 
                            id="email"
                            type="email" 
                            name="email"
                            placeholder="Enter your email address"
                            autocomplete="email"
                            $errorDescription
                            required
                        >

                        <br><br>

                        <label for="password">Password</label><br>
                        <input 
                            id="password"
                            type="password" 
                            name="password"
                            placeholder="Create a password"
                            autocomplete="new-password"
                            aria-describedby="password-help${if (error != null) " register-error" else ""}"
                            required
                        >
                        <p id="password-help" class="muted">Use at least 8 characters, one capital letter and one number.</p>

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

fun escapeRegisterHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
}