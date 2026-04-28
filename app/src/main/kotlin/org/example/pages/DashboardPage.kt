package org.example.pages

data class PersonalRecord(
    val exerciseName: String,
    val amount: Double,
    val unit: String
)

data class DashboardStats(
    val totalActivities: Int,
    val totalSets: Int,
    val mostTrainedCategory: String,
    val lastActivity: String,
    val personalRecords: List<PersonalRecord>
)

fun renderDashboardPage(fullName: String, stats: DashboardStats): String {

    var recordHtml = ""

    if (stats.personalRecords.isEmpty()) {
        recordHtml = "<p class='muted'>No personal records yet.</p>"
    } else {
        recordHtml = "<ul class='record-list'>"

        for (record in stats.personalRecords) {
            recordHtml += "<li>${record.exerciseName}: ${formatAmount(record.amount)} ${record.unit}</li>"
        }

        recordHtml += "</ul>"
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Dashboard</title>
            ${pageCss()}
        </head>
        <body>
            <main>
                <div class="box">

                    <div class="top-logo">
                        <img src="/logo.png" alt="Fitness Tracker Logo">
                    </div>

                    <div class="nav">
                        <div></div>
                        <div>
                            <a class="btn-light" href="/logout">Logout</a>
                        </div>
                    </div>

                    <h1>Dashboard</h1>

                    <p class="hero">
                        Welcome <strong>$fullName</strong>. Here is your fitness overview.
                    </p>

                    <div class="action-grid">

                        <div class="action-card">
                            <h3>Log Activity</h3>
                            <p class="muted">Search for an exercise and record your sets.</p>
                            <a class="btn" href="/activities/new">Add Activity</a>
                        </div>

                        <div class="action-card">
                            <h3>My Activities</h3>
                            <p class="muted">View, edit and delete your logged sessions.</p>
                            <a class="btn" href="/activities">View Activities</a>
                        </div>

                        <div class="action-card">
                            <h3>Progress</h3>
                            <p class="muted">View progress for a specific exercise.</p>
                            <a class="btn" href="/progress">View Progress</a>
                        </div>

                        <div class="action-card">
                            <h3>Templates</h3>
                            <p class="muted">Create reusable workout routines.</p>
                            <a class="btn" href="/templates">View Templates</a>
                        </div>

                        <div class="action-card">
                            <h3>Goals</h3>
                            <p class="muted">Set targets and track completion.</p>
                            <a class="btn" href="/goals">View Goals</a>
                        </div>

                        <div class="action-card">
                            <h3>Friends</h3>
                            <p class="muted">Add friends and manage friend requests.</p>
                            <a class="btn" href="/friends">View Friends</a>
                        </div>

                    </div>

                    <div class="stats-grid">

                        <div class="mini-card stat-card">
                            <h3>Total Activities</h3>
                            <p class="big-stat">${stats.totalActivities}</p>
                            <p class="muted">Activities logged</p>
                        </div>

                        <div class="mini-card stat-card">
                            <h3>Total Sets</h3>
                            <p class="big-stat">${stats.totalSets}</p>
                            <p class="muted">Sets completed</p>
                        </div>

                        <div class="mini-card stat-card">
                            <h3>Top Category</h3>
                            <p class="big-stat small-stat">${stats.mostTrainedCategory}</p>
                            <p class="muted">Most trained area</p>
                        </div>

                    </div>

                    <div class="info-grid">

                        <div class="mini-card">
                            <h3>Last Activity</h3>
                            <p>${stats.lastActivity}</p>
                        </div>

                        <div class="mini-card">
                            <h3>Personal Records</h3>
                            $recordHtml
                        </div>

                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}

fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toInt().toString()
    } else {
        amount.toString()
    }
}