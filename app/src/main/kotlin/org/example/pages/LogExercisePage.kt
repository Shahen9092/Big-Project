package org.example.pages

import org.example.db.tables.ExercisesTable
import org.jetbrains.exposed.sql.ResultRow

fun renderLogExercisePage(
    exercise: ResultRow,
    today: String,
    error: String? = null,
    templateId: Int? = null
): String {

    var errorText = ""

    if (error != null) {
        val safeError = escapeActivityHtml(error)
        errorText = "<p class='error' role='alert'>$safeError</p>"
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