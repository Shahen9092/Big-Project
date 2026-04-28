package org.example.pages

data class FriendRecentActivity(
    val date: String,
    val exerciseName: String,
    val category: String,
    val setCount: Int,
    val bestAmount: Double,
    val unit: String
)

data class FriendProfileStats(
    val totalActivities: Int,
    val totalSets: Int,
    val topCategory: String,
    val personalRecords: List<PersonalRecord>,
    val recentActivities: List<FriendRecentActivity>
)

fun renderFriendProfilePage(
    fullName: String,
    username: String,
    stats: FriendProfileStats
): String {

    var recordsHtml = ""

    if (stats.personalRecords.isEmpty()) {
        recordsHtml = "<p class='muted'>No personal records yet.</p>"
    } else {
        recordsHtml = "<ul class='record-list'>"

        for (record in stats.personalRecords) {
            recordsHtml += "<li>${record.exerciseName}: ${formatAmount(record.amount)} ${record.unit}</li>"
        }

        recordsHtml += "</ul>"
    }

    var recentHtml = ""

    if (stats.recentActivities.isEmpty()) {
        recentHtml = "<p class='muted'>No activities logged yet.</p>"
    } else {
        for (activity in stats.recentActivities) {
            val setText = if (activity.setCount == 1) {
                "1 set"
            } else {
                "${activity.setCount} sets"
            }

            recentHtml += """
                <div class="calendar-entry">
                    <p><strong>${activity.exerciseName}</strong> - $setText</p>
                    <p class="muted">${activity.category}</p>
                    <p class="muted">${activity.date}</p>
                    <p class="muted">Best: ${formatAmount(activity.bestAmount)} ${activity.unit}</p>
                </div>
            """
        }
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>$fullName</title>
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
                            <a href="/friends">← Back to Friends</a>
                        </div>

                        <div>
                            <a class="btn-light" href="/logout">Logout</a>
                        </div>
                    </div>

                    <h1>$fullName</h1>

                    <p class="hero">
                        @${username}'s public fitness profile.
                    </p>

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
                            <p class="big-stat small-stat">${stats.topCategory}</p>
                            <p class="muted">Most trained area</p>
                        </div>

                    </div>

                    <div class="info-grid">

                        <div class="mini-card">
                            <h3>Personal Records</h3>
                            $recordsHtml
                        </div>

                        <div class="mini-card">
                            <h3>Recent Activities</h3>
                            $recentHtml
                        </div>

                    </div>

                </div>
            </main>
        </body>
        </html>
    """.trimIndent()
}