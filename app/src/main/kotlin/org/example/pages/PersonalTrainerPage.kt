package org.example.pages

fun personalTrainerPage(goal: String? = null, saved: String? = null): String {

    val workoutHtml = if (goal != null) {
    when (goal) {
        "strength" -> """
            <ul>
                <li>🏋️ Bench Press – 4x8</li>
                <li>🏋️ Squats – 4x8</li>
                <li>🏋️ Deadlift – 3x5</li>
                <li>🏋️ Shoulder Press – 3x10</li>
                <li>🔥 Rest: 90 sec between sets</li>
            </ul>
        """.trimIndent()

        "weight_loss" -> """
            <ul>
                <li>🏃 Running – 20 min</li>
                <li>🔥 Jump Rope – 10 min</li>
                <li>🚴 Cycling – 15 min</li>
                <li>💪 Bodyweight Squats – 3x15</li>
                <li>🔥 Rest: 30 sec</li>
            </ul>
        """.trimIndent()

        "endurance" -> """
            <ul>
                <li>🚴 Cycling – 30 min</li>
                <li>🏃 Jogging – 20 min</li>
                <li>🏊 Swimming – 15 min</li>
                <li>💪 Plank – 3x1 min</li>
                <li>🔥 Focus: steady pace</li>
            </ul>
        """.trimIndent()

        else -> "<p>Select a valid goal.</p>"
    }
} else {
    "<p>Select a goal to generate a workout.</p>"
}
val motivation = when (goal) {
    "strength" -> "💪 Push heavy, build muscle, stay strong!"
    "weight_loss" -> "🔥 Burn calories and stay consistent!"
    "endurance" -> "🏃 Keep going, build stamina!"
    else -> "👋 Choose a goal to begin your journey!"
}
val savedMessage = if (saved == "true") {
    "<p style='color: green; font-weight: bold;'>✅ Workout Saved!</p>"
} else {
    ""
}
    return """
<!DOCTYPE html>
<html>
<head>
    <title>Personal Trainer</title>
    ${pageCss()}
</head>
<body>
    <main>
        <div class="box">

            <h1>💪 Personal Trainer</h1>

            <div class="mini-card">
                <h3>Select your goal</h3>

                <form method="get" action="/trainer">
                    <select name="goal">
                        <option value="">--Choose--</option>
                        <option value="strength">💪 Strength</option>
                        <option value="weight_loss">🔥 Weight Loss</option>
                        <option value="endurance">🏃 Endurance</option>
                    </select>

                    <br><br>

                    <button class="btn">Generate Workout</button>
                </form>
            </div>

            <div class="mini-card">
               <h3>Workout Plan</h3>

$savedMessage

<p style="font-weight: bold; color: #2e7d32;">$motivation</p>

$workoutHtml

<form method="get" action="/trainer">
    <input type="hidden" name="goal" value="$goal">
    <input type="hidden" name="saved" value="true">
    <button type="submit">💾 Save Workout</button>
</form>
            </div>

            <a class="btn" href="/dashboard">← Back to Dashboard</a>

<br><br>

<a href="/calendar">
    <button style="padding:10px; background:#2e7d32; color:white; border:none; border-radius:5px;">
        📅 Open Calendar
    </button>
</a>
        </div>
    </main>
</body>
</html>
""".trimIndent()
}

