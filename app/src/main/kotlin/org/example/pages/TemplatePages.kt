package org.example.pages

data class TemplateSummary(
    val id: Int,
    val name: String,
    val description: String?,
    val exerciseCount: Int
)

data class TemplateExerciseItem(
    val id: Int,
    val name: String,
    val category: String,
    val unit: String
)

fun renderTemplatesPage(
    templates: List<TemplateSummary>,
    exercises: List<TemplateExerciseItem>,
    message: String? = null,
    error: String? = null
): String {

    var messageHtml = ""

    if (message == "created") {
        messageHtml = "<p class='success'>Workout template created.</p>"
    }

    if (message == "deleted") {
        messageHtml = "<p class='success'>Workout template deleted.</p>"
    }

    if (error == "name") {
        messageHtml = "<p class='error'>Please enter a template name.</p>"
    }

    if (error == "exercises") {
        messageHtml = "<p class='error'>Please choose at least one exercise.</p>"
    }

    var templatesHtml = ""

    if (templates.isEmpty()) {
        templatesHtml = """
            <div class="mini-card">
                <h3>No templates yet</h3>
                <p class="muted">Create a workout template to quickly plan repeated sessions.</p>
            </div>
        """.trimIndent()
    } else {
        for (template in templates) {
            val desc = template.description ?: ""

            templatesHtml += """
                <div class="exercise-card">
                    <h3>${template.name}</h3>
                    <p class="muted">$desc</p>
                    <p class="muted">${template.exerciseCount} exercises</p>

                    <div class="tiny-actions">
                        <a class="btn btn-small" href="/templates/${template.id}">View</a>

                        <form method="post" action="/templates/delete/${template.id}" onsubmit="return confirm('Delete this template?');">
                            <button class="btn-danger btn-small" type="submit">Delete</button>
                        </form>
                    </div>
                </div>
            """
        }
    }

    var exerciseCheckboxes = ""

    for (exercise in exercises) {
        val searchText = "${exercise.name} ${exercise.category} ${exercise.unit}".lowercase()

        exerciseCheckboxes += """
            <label class="checkbox-row template-exercise-row" data-search="$searchText">
                <input type="checkbox" name="exerciseId" value="${exercise.id}">
                <span>${exercise.name} <span class="muted">(${exercise.category}, ${exercise.unit})</span></span>
            </label>
        """
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Workout Templates</title>
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

                    <h1>Workout Templates</h1>

                    <p class="hero">
                        Create reusable workouts such as Push Day, Pull Day or Leg Day.
                    </p>

                    $messageHtml

                    <div class="mini-card">
                        <h3>Create Template</h3>

                        <form method="post" action="/templates/create">
                            <label>Template Name</label><br>
                            <input type="text" name="name" placeholder="Example: Push Day" required>

                            <br><br>

                            <label>Description</label><br>
                            <input type="text" name="description" placeholder="Optional description">

                            <br><br>

                            <label>Choose Exercises</label><br>
                            <input
                                type="text"
                                id="templateExerciseSearch"
                                placeholder="Search and choose exercises..."
                                onkeyup="filterTemplateExercises()"
                            >

                            <div class="checkbox-list">
                                $exerciseCheckboxes
                            </div>

                            <br>

                            <button type="submit">Create Template</button>
                        </form>
                    </div>

                    <h2>Your Templates</h2>

                    <div class="exercise-list">
                        $templatesHtml
                    </div>

                </div>
            </main>

            <script>
                function filterTemplateExercises() {
                    var input = document.getElementById("templateExerciseSearch");
                    var rows = document.getElementsByClassName("template-exercise-row");

                    var search = input.value.toLowerCase();

                    for (var i = 0; i < rows.length; i++) {
                        var text = rows[i].getAttribute("data-search");

                        if (text.indexOf(search) > -1) {
                            rows[i].style.display = "flex";
                        } else {
                            rows[i].style.display = "none";
                        }
                    }
                }
            </script>

        </body>
        </html>
    """.trimIndent()
}

fun renderTemplateDetailPage(
    template: TemplateSummary,
    exercises: List<TemplateExerciseItem>,
    today: String
): String {

    var exerciseHtml = ""

    if (exercises.isEmpty()) {
        exerciseHtml = "<p class='muted'>This template has no exercises.</p>"
    } else {
        for (exercise in exercises) {
            exerciseHtml += """
                <div class="template-log-card" id="exerciseCard-${exercise.id}">

                    <div class="exercise-card-top">
                        <div>
                            <h3>${exercise.name}</h3>
                            <p class="muted">${exercise.category}</p>
                        </div>

                        <span class="unit-tag">${exercise.unit}</span>
                    </div>

                    <button 
                        class="btn enter-details-btn" 
                        id="enterButton-${exercise.id}" 
                        type="button" 
                        onclick="openExerciseForm(${exercise.id})">
                        Enter Details
                    </button>

                    <form 
                        class="inline-log-form" 
                        id="logForm-${exercise.id}" 
                        data-unit="${exercise.unit}"
                        onsubmit="submitTemplateExercise(event, ${template.id}, ${exercise.id})"
                    >

                        <label>Date</label><br>
                        <input type="date" name="date" value="$today" required>

                        <br><br>

                        <h3>Sets</h3>

                        <div id="setsArea-${exercise.id}">
                            <div class="set-row">
                                <label>Set 1</label><br>
                                <div class="amount-line">
                                    <input type="number" step="0.1" name="amount" required>
                                    <span>${exercise.unit}</span>
                                </div>
                            </div>
                        </div>

                        <button type="button" onclick="addTemplateSet(${exercise.id})">+ Add Set</button>

                        <br><br>

                        <label>Notes</label><br>
                        <input type="text" name="notes" placeholder="Optional notes">

                        <br><br>

                        <button type="submit">Log Exercise</button>
                    </form>

                    <div class="logged-banner" id="loggedBanner-${exercise.id}">
                        Exercise has been logged.
                    </div>

                </div>
            """
        }
    }

    val desc = template.description ?: ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>${template.name}</title>
            ${pageCss()}

            <style>
                .template-header-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    gap: 16px;
                    margin-bottom: 10px;
                }

                .template-header-row h1 {
                    margin-bottom: 6px;
                }

                .finish-workout-btn {
                    white-space: nowrap;
                    margin-top: 4px;
                }

                .template-log-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 16px;
                    align-items: start;
                    margin-top: 22px;
                }

                .template-log-card {
                    background: #f7fbf7;
                    border: 1px solid #d6e8d7;
                    border-left: 6px solid #2e7d32;
                    border-radius: 16px;
                    padding: 20px;
                    transition: 0.2s;
                }

                .template-log-card.logged {
                    opacity: 0.85;
                    border-left-color: #1b5e20;
                    background: #f0faf1;
                }

                .enter-details-btn {
                    margin-top: 8px;
                }

                .inline-log-form {
                    display: none;
                    margin-top: 18px;
                    padding-top: 18px;
                    border-top: 1px solid #dce9dd;
                }

                .logged-banner {
                    display: none;
                    margin-top: 16px;
                    background: #e8f5e9;
                    color: #1b5e20;
                    padding: 12px;
                    border-radius: 10px;
                    border-left: 5px solid #2e7d32;
                    font-weight: bold;
                }

                .template-help-text {
                    background: #f4fbf4;
                    border: 1px solid #d6e8d7;
                    border-radius: 14px;
                    padding: 14px;
                    margin-top: 12px;
                    margin-bottom: 20px;
                }
            </style>
        </head>
        <body>
            <main>
                <div class="box">

                    <div class="top-logo">
                        <img src="/logo.png" alt="Fitness Tracker Logo">
                    </div>

                    <div class="nav">
                        <div>
                            <a href="/templates">← Back to Templates</a>
                        </div>

                        <div>
                            <a class="btn-light" href="/logout">Logout</a>
                        </div>
                    </div>

                    <div class="template-header-row">
                        <div>
                            <h1>${template.name}</h1>
                            <p class="hero">$desc</p>
                        </div>

                        <a class="btn finish-workout-btn" href="/activities">Finish Workout</a>
                    </div>



                    <div class="template-log-grid">
                        $exerciseHtml
                    </div>

                </div>
            </main>

            <script>
                var templateSetNumbers = {};

                function openExerciseForm(exerciseId) {
                    var form = document.getElementById("logForm-" + exerciseId);

                    if (form.style.display == "block") {
                        form.style.display = "none";
                    } else {
                        form.style.display = "block";
                    }
                }

                function addTemplateSet(exerciseId) {
                    if (templateSetNumbers[exerciseId] == null) {
                        templateSetNumbers[exerciseId] = 1;
                    }

                    templateSetNumbers[exerciseId] = templateSetNumbers[exerciseId] + 1;

                    var form = document.getElementById("logForm-" + exerciseId);
                    var unit = form.getAttribute("data-unit");

                    var area = document.getElementById("setsArea-" + exerciseId);

                    var row = document.createElement("div");
                    row.className = "set-row";

                    row.innerHTML =
                        "<label>Set " + templateSetNumbers[exerciseId] + "</label><br>" +
                        "<div class='amount-line'>" +
                        "<input type='number' step='0.1' name='amount' required>" +
                        "<span>" + unit + "</span>" +
                        "</div>";

                    area.appendChild(row);
                }

                function submitTemplateExercise(event, templateId, exerciseId) {
                    event.preventDefault();

                    var form = document.getElementById("logForm-" + exerciseId);
                    var button = document.getElementById("enterButton-" + exerciseId);
                    var card = document.getElementById("exerciseCard-" + exerciseId);
                    var banner = document.getElementById("loggedBanner-" + exerciseId);

                    var formData = new FormData(form);

                    fetch("/templates/" + templateId + "/log/" + exerciseId, {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/x-www-form-urlencoded"
                        },
                        body: new URLSearchParams(formData)
                    })
                    .then(function(response) {
                        if (response.ok) {
                            form.style.display = "none";
                            card.classList.add("logged");

                            button.disabled = true;
                            button.innerText = "Logged";
                            button.className = "btn-light enter-details-btn";

                            var inputs = form.querySelectorAll("input, button");

                            for (var i = 0; i < inputs.length; i++) {
                                inputs[i].disabled = true;
                            }

                            banner.style.display = "block";
                        } else {
                            response.text().then(function(text) {
                                alert(text);
                            });
                        }
                    })
                    .catch(function() {
                        alert("Could not log exercise. Please try again.");
                    });
                }
            </script>

        </body>
        </html>
    """.trimIndent()
}