package org.example.pages

fun renderActivitiesPage(
    activities: List<ActivityDisplay>,
    selectedMonth: String,
    previousMonth: String,
    nextMonth: String,
    message: String? = null,
    canUndoDelete: Boolean = false
): String {

    var messageText = ""

    if (message == "saved") {
        messageText = "<p class='success' role='status'>Activity saved successfully.</p>"
    } else if (message == "updated") {
        messageText = "<p class='success' role='status'>Activity updated successfully.</p>"
    } else if (message == "restored") {
        messageText = "<p class='success' role='status'>Activity restored successfully.</p>"
    } else if (message == "deleted") {
        if (canUndoDelete) {
            messageText = """
                <div class="success" role="status">
                    <p style="margin-bottom:10px;">Activity deleted successfully.</p>

                    <form method="post" action="/activities/undo-delete" style="margin:0;">
                        <button class="btn-light btn-small" type="submit">Undo Delete</button>
                    </form>
                </div>
            """.trimIndent()
        } else {
            messageText = "<p class='success' role='status'>Activity deleted successfully.</p>"
        }
    }

    var activitiesText = ""

    if (activities.isEmpty()) {
        val monthText = formatMonth(selectedMonth)

        activitiesText = """
            <div class="mini-card">
                <h3>No activities for $monthText</h3>
                <p class="muted">Try the arrows to move between months, or log a new activity.</p>
            </div>
        """.trimIndent()
    } else {
        val dates = mutableListOf<String>()

        for (activity in activities) {
            if (!dates.contains(activity.date)) {
                dates.add(activity.date)
            }
        }

        for (date in dates) {
            var dayText = ""

            for (activity in activities) {
                if (activity.date == date) {

                    val setCount = activity.sets.size

                    var setText = ""

                    if (setCount == 1) {
                        setText = "1 set"
                    } else {
                        setText = "$setCount sets"
                    }

                    var bestText = ""

                    if (activity.sets.isNotEmpty()) {
                        var bestAmount = activity.sets[0]

                        for (amount in activity.sets) {
                            if (amount > bestAmount) {
                                bestAmount = amount
                            }
                        }

                        val safeUnit = escapeActivityHtml(activity.unit)
                        val bestAmountText = formatAmount(bestAmount)

                        bestText = "Best: $bestAmountText $safeUnit"
                    }

                    var notesText = ""

                    val activityNotes = activity.notes

                    if (activityNotes != null) {
                        if (activityNotes != "") {
                            val safeNotes = escapeActivityHtml(activityNotes)
                            notesText = "<p class='muted small-note'>$safeNotes</p>"
                        }
                    }

                    val safeExerciseName = escapeActivityHtml(activity.exerciseName)
                    val safeCategory = escapeActivityHtml(activity.category)

                    dayText += """
                        <div class="calendar-entry">
                            <p><strong>• $safeExerciseName</strong> - $setText</p>
                            <p class="muted">$safeCategory</p>
                            <p class="muted">$bestText</p>
                            $notesText

                            <div class="tiny-actions">
                                <a class="btn btn-small" href="/activities/edit/${activity.activityId}" aria-label="Edit $safeExerciseName activity">Edit</a>

                                <form method="post" action="/activities/delete/${activity.activityId}" onsubmit="return confirm('Are you sure you want to delete this activity?');">
                                    <button class="btn-danger btn-small" type="submit" aria-label="Delete $safeExerciseName activity">Delete</button>
                                </form>
                            </div>
                        </div>
                    """
                }
            }

            val formattedDate = formatDate(date)

            activitiesText += """
                <div class="calendar-card">
                    <h3>$formattedDate</h3>
                    $dayText
                </div>
            """
        }
    }

    val selectedMonthText = formatMonth(selectedMonth)

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>My Activities</title>
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
                            <a href="/dashboard" aria-label="Back to dashboard">← Back to Dashboard</a>
                        </div>

                        <div>
                            <a class="btn-light" href="/logout" aria-label="Log out of your account">Logout</a>
                        </div>
                    </div>

                    <h1>My Activities</h1>

                    <p class="hero">
                        Your logged activities for the selected month.
                    </p>

                    $messageText

                    <p>
                        <a class="btn" href="/activities/new" aria-label="Log a new activity">Log New Activity</a>
                    </p>

                    <div class="month-switcher">
                        <div class="month-arrow-left">
                            <a class="btn-light month-arrow" href="/activities?month=$previousMonth" aria-label="View previous month">←</a>
                        </div>

                        <div class="month-title-centre">
                            <h2>$selectedMonthText</h2>
                        </div>

                        <div class="month-arrow-right">
                            <a class="btn-light month-arrow" href="/activities?month=$nextMonth" aria-label="View next month">→</a>
                        </div>
                    </div>

                    <div class="calendar-grid">
                        $activitiesText
                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}