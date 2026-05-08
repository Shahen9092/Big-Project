package org.example.pages

import org.example.db.tables.ExercisesTable
import org.jetbrains.exposed.sql.ResultRow

data class ActivityDisplay(
    val activityId: Int,
    val exerciseName: String,
    val category: String,
    val unit: String,
    val date: String,
    val notes: String?,
    val sets: List<Double>
)

fun renderExerciseSearchPage(
    exercises: List<ResultRow>,
    categories: List<String>,
    selectedCategory: String,
    search: String
): String {

    val safeSearch = escapeActivityHtml(search)

    var categoryOptions = "<option value=''>All Categories</option>"

    for (category in categories) {
        val safeCategory = escapeActivityHtml(category)

        var selectedText = ""

        if (category == selectedCategory) {
            selectedText = "selected"
        }

        categoryOptions += "<option value='$safeCategory' $selectedText>$safeCategory</option>"
    }

    var exercisesText = ""

    if (exercises.isEmpty()) {
        exercisesText = """
            <div class="mini-card">
                <h3>No exercises found</h3>
                <p class="muted">Try changing the search or category filter.</p>
            </div>
        """.trimIndent()
    } else {
        for (exercise in exercises) {
            val id = exercise[ExercisesTable.id]
            val name = escapeActivityHtml(exercise[ExercisesTable.name])
            val category = escapeActivityHtml(exercise[ExercisesTable.category])
            val unit = escapeActivityHtml(exercise[ExercisesTable.defaultUnit])

            exercisesText += """
                <div class="exercise-card">
                    <div class="exercise-card-top">
                        <div>
                            <h3>$name</h3>
                            <p class="muted">$category</p>
                        </div>

                        <span class="unit-tag">$unit</span>
                    </div>

                    <a class="btn choose-btn" href="/activities/new/$id" aria-label="Choose $name exercise">Choose Exercise</a>
                </div>
            """
        }
    }

    val resultText = if (exercises.size == 1) {
        "Showing 1 exercise"
    } else {
        "Showing ${exercises.size} exercises"
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Choose Exercise</title>
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

                    <h1>Log Activity</h1>

                    <p class="hero">
                        Search for an exercise, filter by category, then choose what you want to log.
                    </p>

                    <form class="activity-search-card" method="get" action="/activities/new">

                        <div class="filter-top">
                            <div>
                                <h3>Find Exercise</h3>
                                <p class="muted">Search by exercise name, category or related notes.</p>
                            </div>

                            <p class="results-count" aria-live="polite">$resultText</p>
                        </div>

                        <div class="activity-search-grid">

                            <div>
                                <label for="exercise-search">Search</label><br>

                                <div class="input-with-icon">
                                    <span aria-hidden="true">⌕</span>
                                    <input
                                        id="exercise-search"
                                        type="text"
                                        name="q"
                                        value="$safeSearch"
                                        placeholder="Bench press, running, chest..."
                                    >
                                </div>
                            </div>

                            <div>
                                <label for="exercise-category">Category</label><br>

                                <select id="exercise-category" name="category" class="category-select">
                                    $categoryOptions
                                </select>
                            </div>

                            <div class="filter-buttons">
                                <button type="submit">Search</button>
                                <a class="btn-light clear-filter" href="/activities/new" aria-label="Clear exercise search filters">Clear</a>
                            </div>

                        </div>
                    </form>

                    <div class="exercise-list">
                        $exercisesText
                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}

fun renderLogExercisePage(
    exercise: ResultRow,
    today: String,
    error: String? = null,
    templateId: Int? = null
): String {

    var errorText = ""

    if (error != null) {
        errorText = "<p class='error' role='alert'>${escapeActivityHtml(error)}</p>"
    }

    val id = exercise[ExercisesTable.id]
    val name = escapeActivityHtml(exercise[ExercisesTable.name])
    val category = escapeActivityHtml(exercise[ExercisesTable.category])
    val unit = escapeActivityHtml(exercise[ExercisesTable.defaultUnit])

    var backLink = "/activities/new"
    var backText = "Back to Exercises"
    var hiddenTemplateInput = ""

    if (templateId != null) {
        backLink = "/templates/$templateId"
        backText = "Back to Template"
        hiddenTemplateInput = "<input type='hidden' name='templateId' value='$templateId'>"
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Log $name</title>
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
                            <a href="$backLink" aria-label="$backText">← $backText</a>
                        </div>

                        <div>
                            <a class="btn-light" href="/logout" aria-label="Log out of your account">Logout</a>
                        </div>
                    </div>

                    <h1>$name</h1>

                    <p class="muted">$category</p>
                    <p><span class="unit-tag">$unit</span></p>

                    $errorText

                    <form method="post" action="/activities/new/$id">
                        $hiddenTemplateInput

                        <label for="activity-date">Date</label><br>
                        <input id="activity-date" type="date" name="date" value="$today" required>

                        <br><br>

                        <h2>Sets</h2>

                        <div id="setsArea">
                            <div class="set-row">
                                <label for="amount-1">Set 1</label><br>
                                <div class="amount-line">
                                    <input id="amount-1" type="number" step="0.1" name="amount" required>
                                    <span>$unit</span>
                                </div>
                            </div>
                        </div>

                        <button type="button" onclick="addSet()">+ Add Set</button>

                        <br><br>

                        <label for="activity-notes">Notes</label><br>
                        <input id="activity-notes" type="text" name="notes" placeholder="Optional notes">

                        <br><br>

                        <button type="submit">Save Activity</button>
                    </form>

                </div>
            </main>

            <script>
                var setNumber = 1;

                function addSet() {
                    setNumber = setNumber + 1;

                    var area = document.getElementById("setsArea");

                    var row = document.createElement("div");
                    row.className = "set-row";

                    row.innerHTML =
                        "<label for='amount-" + setNumber + "'>Set " + setNumber + "</label><br>" +
                        "<div class='amount-line'>" +
                        "<input id='amount-" + setNumber + "' type='number' step='0.1' name='amount' required>" +
                        "<span>$unit</span>" +
                        "</div>";

                    area.appendChild(row);
                }
            </script>

        </body>
        </html>
    """.trimIndent()
}

fun renderEditActivityPage(
    activityId: Int,
    exerciseName: String,
    category: String,
    unit: String,
    date: String,
    notes: String?,
    sets: List<Double>,
    error: String? = null
): String {

    var errorText = ""

    if (error != null) {
        errorText = "<p class='error' role='alert'>${escapeActivityHtml(error)}</p>"
    }

    val safeExerciseName = escapeActivityHtml(exerciseName)
    val safeCategory = escapeActivityHtml(category)
    val safeUnit = escapeActivityHtml(unit)
    val safeDate = escapeActivityHtml(date)
    val notesText = escapeActivityHtml(notes ?: "")

    var setsText = ""
    var setNumber = 1

    for (amount in sets) {
        setsText += """
            <div class="set-row">
                <label for="edit-amount-$setNumber">Set $setNumber</label><br>
                <div class="amount-line">
                    <input id="edit-amount-$setNumber" type="number" step="0.1" name="amount" value="$amount" required>
                    <span>$safeUnit</span>
                </div>
            </div>
        """

        setNumber = setNumber + 1
    }

    if (setsText == "") {
        setsText = """
            <div class="set-row">
                <label for="edit-amount-1">Set 1</label><br>
                <div class="amount-line">
                    <input id="edit-amount-1" type="number" step="0.1" name="amount" required>
                    <span>$safeUnit</span>
                </div>
            </div>
        """
    }

    val currentSetNumber = if (sets.isEmpty()) {
        1
    } else {
        sets.size
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Edit Activity</title>
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
                            <a href="/activities" aria-label="Back to my activities">← Back to My Activities</a>
                        </div>

                        <div>
                            <a class="btn-light" href="/logout" aria-label="Log out of your account">Logout</a>
                        </div>
                    </div>

                    <h1>Edit Activity</h1>

                    <div class="mini-card">
                        <h3>$safeExerciseName</h3>
                        <p class="muted">$safeCategory</p>
                        <p><span class="unit-tag">$safeUnit</span></p>
                    </div>

                    <br>

                    $errorText

                    <form method="post" action="/activities/edit/$activityId">

                        <label for="edit-date">Date</label><br>
                        <input id="edit-date" type="date" name="date" value="$safeDate" required>

                        <br><br>

                        <h2>Sets</h2>

                        <div id="setsArea">
                            $setsText
                        </div>

                        <button type="button" onclick="addSet()">+ Add Set</button>

                        <br><br>

                        <label for="edit-notes">Notes</label><br>
                        <input id="edit-notes" type="text" name="notes" value="$notesText" placeholder="Optional notes">

                        <br><br>

                        <button type="submit">Save Changes</button>
                    </form>

                    <form method="post" action="/activities/delete/$activityId" onsubmit="return confirm('Are you sure you want to delete this activity?');">
                        <button class="btn-danger" type="submit" aria-label="Delete this activity">Delete Activity</button>
                    </form>

                </div>
            </main>

            <script>
                var setNumber = $currentSetNumber;

                function addSet() {
                    setNumber = setNumber + 1;

                    var area = document.getElementById("setsArea");

                    var row = document.createElement("div");
                    row.className = "set-row";

                    row.innerHTML =
                        "<label for='edit-amount-" + setNumber + "'>Set " + setNumber + "</label><br>" +
                        "<div class='amount-line'>" +
                        "<input id='edit-amount-" + setNumber + "' type='number' step='0.1' name='amount' required>" +
                        "<span>$safeUnit</span>" +
                        "</div>";

                    area.appendChild(row);
                }
            </script>

        </body>
        </html>
    """.trimIndent()
}

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
    }

    if (message == "updated") {
        messageText = "<p class='success' role='status'>Activity updated successfully.</p>"
    }

    if (message == "restored") {
        messageText = "<p class='success' role='status'>Activity restored successfully.</p>"
    }

    if (message == "deleted") {
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
        activitiesText = """
            <div class="mini-card">
                <h3>No activities for ${formatMonth(selectedMonth)}</h3>
                <p class="muted">Try the arrows to move between months, or log a new activity.</p>
            </div>
        """.trimIndent()
    } else {
        val dates = activities.map { it.date }.distinct()

        for (date in dates) {
            val activitiesForThisDate = activities.filter { it.date == date }

            var dayText = ""

            for (activity in activitiesForThisDate) {
                val setCount = activity.sets.size

                val setText = if (setCount == 1) {
                    "1 set"
                } else {
                    "$setCount sets"
                }

                var bestText = ""

                if (activity.sets.isNotEmpty()) {
                    var bestAmount = activity.sets[0]

                    for (amount in activity.sets) {
                        if (amount > bestAmount) {
                            bestAmount = amount
                        }
                    }

                    bestText = "Best: ${formatAmount(bestAmount)} ${escapeActivityHtml(activity.unit)}"
                }

                var notesText = ""

                if (activity.notes != null && activity.notes != "") {
                    notesText = "<p class='muted small-note'>${escapeActivityHtml(activity.notes)}</p>"
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

            activitiesText += """
                <div class="calendar-card">
                    <h3>${formatDate(date)}</h3>
                    $dayText
                </div>
            """
        }
    }

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
                            <h2>${formatMonth(selectedMonth)}</h2>
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

fun getMonthKey(date: String): String {
    val parts = date.split("-")

    if (parts.size < 2) {
        return date
    }

    val year = parts[0]
    val month = parts[1]

    return "$year-$month"
}

fun formatMonth(monthKey: String): String {
    val parts = monthKey.split("-")

    if (parts.size < 2) {
        return monthKey
    }

    val year = parts[0]
    val month = parts[1]

    val monthName = when (month) {
        "01" -> "January"
        "02" -> "February"
        "03" -> "March"
        "04" -> "April"
        "05" -> "May"
        "06" -> "June"
        "07" -> "July"
        "08" -> "August"
        "09" -> "September"
        "10" -> "October"
        "11" -> "November"
        "12" -> "December"
        else -> month
    }

    return "$monthName $year"
}

fun formatDate(date: String): String {
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

    return "$day $monthName $year"
}

fun escapeActivityHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
}