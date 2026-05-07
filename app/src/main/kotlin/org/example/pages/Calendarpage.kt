package org.example.pages

fun calendarPage(selectedDay: String? = null): String {

    val daysHtml = (1..30).joinToString("") { day ->
        """
        <a href="/calendar?day=$day">
            <div class="day">$day</div>
        </a>
        """
    }

    val message = if (selectedDay != null) {
        "<p style='color:#2e7d32; font-weight:bold;'>📅 You selected Day $selectedDay. Stay consistent! 💪</p>"
    } else {
        "<p>Select a day to view activity.</p>"
    }

    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <title>Calendar</title>
        <style>
            body {
                font-family: Arial;
                background: #f1f8f4;
                padding: 40px;
            }
            .container {
                background: white;
                padding: 20px;
                border-radius: 10px;
                max-width: 700px;
                margin: auto;
                border-top: 6px solid #2e7d32;
                text-align: center;
            }
            h1 {
                color: #2e7d32;
            }
            .calendar {
                display: grid;
                grid-template-columns: repeat(7, 1fr);
                gap: 10px;
                margin-top: 20px;
            }
            .day {
                padding: 15px;
                background: #e8f5e9;
                border-radius: 8px;
                cursor: pointer;
                font-weight: bold;
            }
            .day:hover {
                background: #c8e6c9;
            }
            a {
                text-decoration: none;
                color: black;
            }
            .back {
                display: inline-block;
                margin-top: 20px;
                color: #2e7d32;
                font-weight: bold;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>📅 Workout Calendar</h1>

            $message

            <div class="calendar">
                $daysHtml
            </div>

            <a class="back" href="/trainer">← Back to Trainer</a>
        </div>
    </body>
    </html>
    """
}