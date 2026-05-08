package org.example.pages

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
        val safeError = escapeActivityHtml(error)
        errorText = "<p class='error' role='alert'>$safeError</p>"
    }

    val safeExerciseName = escapeActivityHtml(exerciseName)
    val safeCategory = escapeActivityHtml(category)
    val safeUnit = escapeActivityHtml(unit)
    val safeDate = escapeActivityHtml(date)

    var notesText = ""

    if (notes != null) {
        notesText = escapeActivityHtml(notes)
    }

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

    var currentSetNumber = 1

    if (sets.isEmpty()) {
        currentSetNumber = 1
    } else {
        currentSetNumber = sets.size
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