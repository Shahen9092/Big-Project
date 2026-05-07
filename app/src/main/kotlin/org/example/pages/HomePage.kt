package org.example.pages

fun renderHomePage(): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Fitness Tracker</title>
            ${pageCss()}
        </head>
        <body>
            <main>
                <div class="box home-box">

                    <div class="top-logo">
                        <img src="/logo.png" alt="Fitness Tracker Logo">
                    </div>

                    <h1>Fitness Tracker</h1>

                    <p class="home-text">
                        Plan workouts, record exercise, and keep track of your progress.
                    </p>

                    <div class="home-buttons">
                        <a class="btn big-btn" href="/login">Login</a>
                        <a class="btn big-btn" href="/register">Register</a>
                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}