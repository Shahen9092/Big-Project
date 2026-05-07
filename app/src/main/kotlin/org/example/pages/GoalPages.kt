package org.example.pages

data class GoalDisplay(
    val id: Int,
    val title: String,
    val typeLabel: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val unit: String,
    val percentage: Int
)

fun renderGoalsPage(
    goals: List<GoalDisplay>,
    exercises: List<TemplateExerciseItem>,
    message: String? = null,
    error: String? = null
): String {

    var messageHtml = ""

    if (message == "created") {
        messageHtml = "<p class='success'>Goal created.</p>"
    }

    if (message == "deleted") {
        messageHtml = "<p class='success'>Goal deleted.</p>"
    }

    if (error == "title") {
        messageHtml = "<p class='error'>Please enter a goal title.</p>"
    }

    if (error == "target") {
        messageHtml = "<p class='error'>Please enter a valid target above 0.</p>"
    }

    if (error == "exercise") {
        messageHtml = "<p class='error'>Please choose an exercise for this goal type.</p>"
    }

    var datalistOptions = ""

    for (exercise in exercises) {
        datalistOptions += """
            <option value="${exercise.id} - ${exercise.name} (${exercise.unit})">
        """
    }

    var goalsHtml = ""

    if (goals.isEmpty()) {
        goalsHtml = """
            <div class="mini-card">
                <h3>No goals yet</h3>
                <p class="muted">Create a goal to track what you want to achieve.</p>
            </div>
        """.trimIndent()
    } else {
        for (goal in goals) {
            goalsHtml += """
                <div class="mini-card">
                    <h3>${goal.title}</h3>
                    <p class="muted">${goal.typeLabel}</p>

                    <p>
                        <strong>${formatAmount(goal.currentAmount)}</strong> / 
                        <strong>${formatAmount(goal.targetAmount)} ${goal.unit}</strong>
                    </p>

                    <div style="background:#dce9dd; border-radius:999px; overflow:hidden; height:18px; margin-bottom:10px;">
                        <div style="background:#2e7d32; height:18px; width:${goal.percentage}%;"></div>
                    </div>

                    <p class="muted">${goal.percentage}% complete</p>

                    <form method="post" action="/goals/delete/${goal.id}" onsubmit="return confirm('Delete this goal?');">
                        <button class="btn-danger btn-small" type="submit">Delete</button>
                    </form>
                </div>
            """
        }
    }

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Goals</title>
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

                    <h1>Goals</h1>

                    <p class="hero">
                        Set training goals and track your all-time progress.
                    </p>

                    $messageHtml

                    <div class="mini-card">
                        <h3>Create Goal</h3>

                        <form method="post" action="/goals/create">
                            <label>Goal Title</label><br>
                            <input type="text" name="title" placeholder="Example: Bench Press 80kg" required>

                            <br><br>

                            <label>Goal Type</label><br>
                            <select name="goalType">
                                <option value="activities">Number of activities</option>
                                <option value="sets">Number of sets</option>
                                <option value="exercise_best">Best amount for an exercise</option>
                            </select>

                            <br><br>

                            <label>Exercise</label><br>
                            <input 
                                type="text"
                                name="exerciseChoice"
                                list="goalExerciseList"
                                placeholder="Search and choose an exercise..."
                            >

                            <datalist id="goalExerciseList">
                                $datalistOptions
                            </datalist>


                            <label>Target Amount</label><br>
                            <input type="number" step="0.1" name="targetAmount" required>

                            <br><br>

                            <button type="submit">Create Goal</button>
                        </form>
                    </div>

                    <h2>Your Goals</h2>

                    <div class="exercise-list">
                        $goalsHtml
                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}