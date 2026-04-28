package org.example.pages

data class ProgressPoint(
    val date: String,
    val value: Double
)

fun renderProgressPage(
    fullName: String,
    exercises: List<String>,
    selectedExercise: String,
    points: List<ProgressPoint>
): String {

    var optionsHtml = "<option value=''>Choose exercise</option>"

    for (exercise in exercises) {
        if (exercise == selectedExercise) {
            optionsHtml += "<option value='$exercise' selected>$exercise</option>"
        } else {
            optionsHtml += "<option value='$exercise'>$exercise</option>"
        }
    }

    val graphHtml = renderLineGraph(selectedExercise, points)

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Progress</title>
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
                            <a href="/dashboard">← Back to Dashboard</a>
                        </div>
                        <div>
                            <a class="btn-light" href="/logout">Logout</a>
                        </div>
                    </div>

                    <h1>Progress</h1>

                    <p class="hero">
                        Track how a specific exercise changes over time, $fullName.
                    </p>

                    <form class="graph-form" method="get" action="/progress">
                        <div class="graph-form-row">
                            <div>
                                <label>Exercise</label><br>
                                <select name="exercise">
                                    $optionsHtml
                                </select>
                            </div>

                            <div>
                                <button type="submit">Show Graph</button>
                            </div>
                        </div>
                    </form>

                    $graphHtml

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}

private fun renderLineGraph(selectedExercise: String, points: List<ProgressPoint>): String {
    if (selectedExercise == "") {
        return """
            <div class="mini-card">
                <h3>No exercise selected</h3>
                <p class="muted">Choose an exercise to view your progress graph.</p>
            </div>
        """.trimIndent()
    }

    if (points.isEmpty()) {
        return """
            <div class="mini-card">
                <h3>$selectedExercise</h3>
                <p class="muted">No logged data found for this exercise.</p>
            </div>
        """.trimIndent()
    }

    val width = 820
    val height = 320
    val leftPadding = 60
    val rightPadding = 20
    val topPadding = 20
    val bottomPadding = 50

    val graphWidth = width - leftPadding - rightPadding
    val graphHeight = height - topPadding - bottomPadding

    var maxValue = 1.0

    for (point in points) {
        if (point.value > maxValue) {
            maxValue = point.value
        }
    }

    val stepX = if (points.size == 1) {
        0.0
    } else {
        graphWidth.toDouble() / (points.size - 1)
    }

    var polylinePoints = ""
    var circlesHtml = ""
    var xLabelsHtml = ""

    for (i in points.indices) {
        val point = points[i]

        val x = leftPadding + (i * stepX)

        val yRatio = point.value / maxValue
        val y = topPadding + graphHeight - (yRatio * graphHeight)

        polylinePoints += "${x},${y} "

        circlesHtml += """
            <circle cx="$x" cy="$y" r="5" fill="#2e7d32"></circle>
        """.trimIndent()

        circlesHtml += """
            <text x="$x" y="${y - 10}" text-anchor="middle" font-size="12" fill="#1b5e20">
                ${formatAmount(point.value)}
            </text>
        """.trimIndent()

        xLabelsHtml += """
            <text x="$x" y="${height - 18}" text-anchor="middle" font-size="12" fill="#5e7560">
                ${shortDate(point.date)}
            </text>
        """.trimIndent()
    }

    var yAxisLabels = ""
    val steps = 4

    for (i in 0..steps) {
        val value = (maxValue / steps) * (steps - i)
        val y = topPadding + (graphHeight.toDouble() / steps) * i

        yAxisLabels += """
            <line x1="$leftPadding" y1="$y" x2="${width - rightPadding}" y2="$y" stroke="#d9e7da" stroke-width="1"></line>
            <text x="${leftPadding - 10}" y="${y + 4}" text-anchor="end" font-size="12" fill="#5e7560">
                ${formatAmount(value)}
            </text>
        """.trimIndent()
    }

    var pointRows = ""

    for (point in points) {
        pointRows += """
            <tr>
                <td>${point.date}</td>
                <td>${formatAmount(point.value)}</td>
            </tr>
        """
    }

    return """
        <div class="graph-box">
            <h3>$selectedExercise</h3>
            <p class="muted">This graph shows your best logged set for each day you recorded this exercise.</p>

            <div class="graph-wrap">
                <svg viewBox="0 0 $width $height" class="graph-svg">

                    $yAxisLabels

                    <line x1="$leftPadding" y1="$topPadding" x2="$leftPadding" y2="${height - bottomPadding}" stroke="#7aa87d" stroke-width="2"></line>
                    <line x1="$leftPadding" y1="${height - bottomPadding}" x2="${width - rightPadding}" y2="${height - bottomPadding}" stroke="#7aa87d" stroke-width="2"></line>

                    <polyline
                        fill="none"
                        stroke="#2e7d32"
                        stroke-width="3"
                        points="$polylinePoints">
                    </polyline>

                    $circlesHtml
                    $xLabelsHtml
                </svg>
            </div>

            <table class="progress-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Best Set</th>
                    </tr>
                </thead>
                <tbody>
                    $pointRows
                </tbody>
            </table>
        </div>
    """.trimIndent()
}

private fun shortDate(date: String): String {
    val parts = date.split("-")

    if (parts.size != 3) {
        return date
    }

    val year = parts[0]
    val month = parts[1]
    val day = parts[2]

    val monthName = when (month) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Aug"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dec"
        else -> month
    }

    return "$day $monthName"
}