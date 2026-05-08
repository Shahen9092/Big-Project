package org.example.pages

import org.example.db.tables.ExercisesTable
import org.jetbrains.exposed.sql.ResultRow

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

    var resultText = ""

    if (exercises.size == 1) {
        resultText = "Showing 1 exercise"
    } else {
        resultText = "Showing ${exercises.size} exercises"
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