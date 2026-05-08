package org.example.pages

fun calendarPage(username: String, currentMonth: Int, currentYear: Int, events: List<Map<String, String>>): String {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Calculate calendar grid
    val firstDay = java.util.Calendar.getInstance().apply {
        set(currentYear, currentMonth - 1, 1)
    }
    val daysInMonth = firstDay.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    // Convert Sunday=1..Saturday=7 to Mon=0..Sun=6
    var startDayOfWeek = firstDay.get(java.util.Calendar.DAY_OF_WEEK) - 2
    if (startDayOfWeek < 0) startDayOfWeek = 6

    val today = java.util.Calendar.getInstance()
    val todayDay = today.get(java.util.Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(java.util.Calendar.MONTH) + 1
    val todayYear = today.get(java.util.Calendar.YEAR)

    // Previous and next month navigation
    val prevMonth = if (currentMonth == 1) 12 else currentMonth - 1
    val prevYear = if (currentMonth == 1) currentYear - 1 else currentYear
    val nextMonth = if (currentMonth == 12) 1 else currentMonth + 1
    val nextYear = if (currentMonth == 12) currentYear + 1 else currentYear

    // Build event map: day -> list of events
    val eventsByDay = mutableMapOf<Int, MutableList<Map<String, String>>>()
    for (event in events) {
        val day = event["day"]?.toIntOrNull() ?: continue
        eventsByDay.getOrPut(day) { mutableListOf() }.add(event)
    }

    // Build calendar cells
    val cells = StringBuilder()
    var dayCounter = 1
    val totalCells = startDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    for (row in 0 until rows) {
        cells.append("<tr>")
        for (col in 0 until 7) {
            val cellIndex = row * 7 + col
            if (cellIndex < startDayOfWeek || dayCounter > daysInMonth) {
                cells.append("<td class=\"cal-empty\"></td>")
            } else {
                val day = dayCounter
                val isToday = day == todayDay && currentMonth == todayMonth && currentYear == todayYear
                val dayEvents = eventsByDay[day] ?: emptyList()

                // Determine day type badge
                val dayType = dayEvents.firstOrNull()?.get("type") ?: ""
                val badgeHtml = when (dayType) {
                    "gym" -> "<span class=\"day-badge badge-gym\">Gym</span>"
                    "rest" -> "<span class=\"day-badge badge-rest\">Rest</span>"
                    "busy" -> "<span class=\"day-badge badge-busy\">Busy</span>"
                    else -> ""
                }

                val todayClass = if (isToday) " today" else ""
                val hasEventClass = if (dayEvents.isNotEmpty()) " has-event" else ""

                cells.append("""
                    <td class="cal-day$todayClass$hasEventClass" 
                        onclick="openDayModal($day, $currentMonth, $currentYear)"
                        data-day="$day">
                        <div class="day-number">$day</div>
                        $badgeHtml
                        <div class="event-dots">
                            ${dayEvents.take(3).joinToString("") { e ->
                    val color = when (e["type"]) {
                        "gym" -> "#2d6a4f"
                        "rest" -> "#74c69d"
                        "busy" -> "#e63946"
                        else -> "#aaa"
                    }
                    "<span class='dot' style='background:$color'></span>"
                }}
                        </div>
                    </td>
                """.trimIndent())
                dayCounter++
            }
        }
        cells.append("</tr>")
    }

    // Upcoming events list (next 5)
    val upcomingHtml = if (events.isEmpty()) {
        "<p class='no-events'>No upcoming events. Click any day to add one!</p>"
    } else {
        events.sortedBy { it["day"]?.toIntOrNull() ?: 0 }
            .take(5)
            .joinToString("") { e ->
                val typeLabel = when (e["type"]) {
                    "gym" -> "🏋️ Gym"
                    "rest" -> "😴 Rest"
                    "busy" -> "📅 Busy"
                    else -> "📌 Event"
                }
                val typeClass = "event-type-${e["type"] ?: "other"}"
                """
                <div class="upcoming-item $typeClass">
                    <div class="upcoming-left">
                        <span class="upcoming-day">${monthNames[currentMonth - 1].take(3)} ${e["day"]}</span>
                        <span class="upcoming-label">$typeLabel</span>
                    </div>
                    <div class="upcoming-note">${e["note"] ?: ""}</div>
                </div>
                """.trimIndent()
            }
    }

    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Calendar - Gym Tracker</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            background: #f0f4f0;
            color: #1b4332;
            min-height: 100vh;
        }

        /* ── Top nav (matches your site) ── */
        .top-bar {
            background: white;
            border-bottom: 2px solid #e8f5e9;
            padding: 12px 32px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .top-bar .logo {
            font-size: 1.5rem;
            color: #2d6a4f;
        }
        .top-bar a {
            color: #2d6a4f;
            text-decoration: none;
            font-weight: 600;
            font-size: 0.95rem;
        }
        .top-bar a:hover { text-decoration: underline; }
        .logout-btn {
            background: transparent;
            border: 2px solid #2d6a4f;
            color: #2d6a4f;
            padding: 6px 18px;
            border-radius: 8px;
            cursor: pointer;
            font-weight: 600;
            font-size: 0.9rem;
        }
        .logout-btn:hover { background: #2d6a4f; color: white; }

        /* ── Page wrapper ── */
        .page-wrap {
            max-width: 1100px;
            margin: 0 auto;
            padding: 32px 20px 60px;
        }

        .page-title {
            font-size: 2.2rem;
            font-weight: 800;
            color: #1b4332;
            margin-bottom: 4px;
        }
        .page-subtitle {
            color: #52796f;
            font-size: 1rem;
            margin-bottom: 28px;
        }

        /* ── Two-column layout ── */
        .cal-layout {
            display: grid;
            grid-template-columns: 1fr 320px;
            gap: 24px;
            align-items: start;
        }
        @media (max-width: 800px) {
            .cal-layout { grid-template-columns: 1fr; }
        }

        /* ── Calendar card ── */
        .cal-card {
            background: white;
            border-radius: 16px;
            box-shadow: 0 2px 12px rgba(45,106,79,0.10);
            overflow: hidden;
            border: 1.5px solid #e8f5e9;
        }

        .cal-header {
            background: #2d6a4f;
            color: white;
            padding: 20px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .cal-header h2 { font-size: 1.3rem; font-weight: 700; }
        .cal-nav-btn {
            background: rgba(255,255,255,0.2);
            border: none;
            color: white;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            font-size: 1.2rem;
            cursor: pointer;
            display: flex; align-items: center; justify-content: center;
            text-decoration: none;
            transition: background 0.2s;
        }
        .cal-nav-btn:hover { background: rgba(255,255,255,0.35); }

        /* Day headers */
        .cal-table {
            width: 100%;
            border-collapse: collapse;
        }
        .cal-table thead th {
            padding: 12px 0 8px;
            text-align: center;
            font-size: 0.78rem;
            font-weight: 700;
            color: #52796f;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            border-bottom: 1px solid #e8f5e9;
        }

        /* Calendar cells */
        .cal-table td {
            height: 76px;
            width: 14.28%;
            vertical-align: top;
            padding: 6px 5px;
            border: 1px solid #f0f4f0;
            cursor: default;
            transition: background 0.15s;
            position: relative;
        }
        .cal-day {
            cursor: pointer;
        }
        .cal-day:hover { background: #f0fff4 !important; }
        .cal-empty { background: #fafafa; cursor: default; }

        .day-number {
            font-size: 0.9rem;
            font-weight: 600;
            color: #2d6a4f;
            margin-bottom: 3px;
        }
        .today .day-number {
            background: #2d6a4f;
            color: white;
            width: 26px;
            height: 26px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.82rem;
        }
        .today { background: #f0fff4; }

        /* Day type badges */
        .day-badge {
            font-size: 0.62rem;
            font-weight: 700;
            padding: 1px 5px;
            border-radius: 4px;
            display: inline-block;
            margin-bottom: 2px;
            text-transform: uppercase;
            letter-spacing: 0.04em;
        }
        .badge-gym { background: #2d6a4f; color: white; }
        .badge-rest { background: #d8f3dc; color: #2d6a4f; }
        .badge-race { background: #f4a261; color: white; }
        .badge-busy { background: #e63946; color: white; }

        .event-dots { display: flex; gap: 3px; flex-wrap: wrap; margin-top: 2px; }
        .dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }

        /* ── Right sidebar ── */
        .sidebar { display: flex; flex-direction: column; gap: 20px; }

        /* Quick add card */
        .side-card {
            background: white;
            border-radius: 16px;
            padding: 22px 20px;
            box-shadow: 0 2px 12px rgba(45,106,79,0.10);
            border: 1.5px solid #e8f5e9;
        }
        .side-card h3 {
            font-size: 1rem;
            font-weight: 700;
            color: #1b4332;
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .form-group { margin-bottom: 12px; }
        .form-group label {
            display: block;
            font-size: 0.8rem;
            font-weight: 600;
            color: #52796f;
            margin-bottom: 4px;
        }
        .form-group input, .form-group select, .form-group textarea {
            width: 100%;
            padding: 9px 12px;
            border: 1.5px solid #d8f3dc;
            border-radius: 8px;
            font-size: 0.88rem;
            color: #1b4332;
            background: #f9fdf9;
            outline: none;
            transition: border 0.2s;
            font-family: inherit;
        }
        .form-group input:focus, .form-group select:focus, .form-group textarea:focus {
            border-color: #2d6a4f;
            background: white;
        }
        .form-group textarea { resize: vertical; min-height: 60px; }

        .btn-green {
            width: 100%;
            padding: 11px;
            background: #2d6a4f;
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            transition: background 0.2s;
        }
        .btn-green:hover { background: #1b4332; }

        /* Upcoming events */
        .upcoming-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 12px;
            border-radius: 10px;
            margin-bottom: 8px;
            border-left: 4px solid #2d6a4f;
            background: #f0fff4;
            font-size: 0.85rem;
        }
        .event-type-rest { border-left-color: #74c69d; background: #f0fff8; }
        .event-type-race { border-left-color: #f4a261; background: #fff8f0; }
        .event-type-busy { border-left-color: #e63946; background: #fff0f0; }

        .upcoming-left { display: flex; flex-direction: column; gap: 2px; }
        .upcoming-day { font-weight: 700; color: #1b4332; font-size: 0.82rem; }
        .upcoming-label { font-size: 0.78rem; color: #52796f; }
        .upcoming-note { font-size: 0.78rem; color: #888; max-width: 100px; text-align: right; }

        .no-events { color: #888; font-size: 0.85rem; font-style: italic; text-align: center; padding: 16px 0; }

        /* Legend */
        .legend {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 4px;
        }
        .legend-item {
            display: flex;
            align-items: center;
            gap: 5px;
            font-size: 0.75rem;
            color: #52796f;
        }
        .legend-dot {
            width: 10px; height: 10px;
            border-radius: 3px;
        }

        /* ── Modal ── */
        .modal-overlay {
            display: none;
            position: fixed; inset: 0;
            background: rgba(0,0,0,0.45);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }
        .modal-overlay.open { display: flex; }
        .modal {
            background: white;
            border-radius: 18px;
            padding: 28px 28px 24px;
            width: 90%;
            max-width: 440px;
            box-shadow: 0 8px 40px rgba(0,0,0,0.18);
            position: relative;
        }
        .modal h2 {
            font-size: 1.2rem;
            color: #1b4332;
            margin-bottom: 18px;
        }
        .modal-close {
            position: absolute;
            top: 16px; right: 18px;
            background: none; border: none;
            font-size: 1.5rem; cursor: pointer;
            color: #888; line-height: 1;
        }
        .modal-close:hover { color: #e63946; }

        /* Day timetable section */
        .timetable-section {
            margin-top: 14px;
            padding-top: 14px;
            border-top: 1px solid #e8f5e9;
        }
        .timetable-section h4 {
            font-size: 0.85rem;
            color: #52796f;
            font-weight: 700;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        .time-slots { display: flex; flex-direction: column; gap: 6px; }
        .time-slot {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        .time-slot input[type="time"] {
            width: 110px;
            padding: 7px 10px;
            border: 1.5px solid #d8f3dc;
            border-radius: 8px;
            font-size: 0.85rem;
            color: #1b4332;
            background: #f9fdf9;
        }
        .time-slot input[type="text"] {
            flex: 1;
            padding: 7px 10px;
            border: 1.5px solid #d8f3dc;
            border-radius: 8px;
            font-size: 0.85rem;
            color: #1b4332;
            background: #f9fdf9;
        }
        .add-slot-btn {
            background: none;
            border: 1.5px dashed #74c69d;
            color: #2d6a4f;
            padding: 6px 12px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 0.82rem;
            margin-top: 6px;
            font-weight: 600;
        }
        .add-slot-btn:hover { background: #f0fff4; }

        .modal-actions {
            display: flex;
            gap: 10px;
            margin-top: 20px;
        }
        .btn-save {
            flex: 1;
            padding: 11px;
            background: #2d6a4f;
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
        }
        .btn-save:hover { background: #1b4332; }
        .btn-cancel {
            padding: 11px 18px;
            background: #f0f4f0;
            color: #52796f;
            border: none;
            border-radius: 10px;
            font-size: 0.95rem;
            cursor: pointer;
        }
        .btn-cancel:hover { background: #e8f5e9; }
        .btn-delete {
            padding: 11px 14px;
            background: #fff0f0;
            color: #e63946;
            border: 1.5px solid #e63946;
            border-radius: 10px;
            font-size: 0.9rem;
            cursor: pointer;
            font-weight: 600;
        }
        .btn-delete:hover { background: #e63946; color: white; }

        /* ── Summary stats ── */
        .stats-row {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 14px;
            margin-bottom: 24px;
        }
        @media (max-width: 600px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
        .stat-box {
            background: white;
            border-radius: 14px;
            padding: 16px 14px;
            text-align: center;
            box-shadow: 0 2px 8px rgba(45,106,79,0.08);
            border: 1.5px solid #e8f5e9;
        }
        .stat-number {
            font-size: 1.8rem;
            font-weight: 800;
            color: #2d6a4f;
            line-height: 1;
        }
        .stat-label {
            font-size: 0.72rem;
            color: #52796f;
            margin-top: 4px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.04em;
        }

        .existing-events-list { margin-bottom: 10px; }
        .existing-event-tag {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 0.78rem;
            font-weight: 700;
            margin: 2px 3px;
        }
        .tag-gym { background: #2d6a4f; color: white; }
        .tag-rest { background: #d8f3dc; color: #1b4332; }
        .tag-race { background: #f4a261; color: white; }
        .tag-busy { background: #e63946; color: white; }
    </style>
</head>
<body>

<!-- Top bar matching your site -->
<div class="top-bar">
    <a href="/dashboard">&#8592; Back to Dashboard</a>
    <span class="logo">&#x1F4AA;</span>
    <form action="/logout" method="post" style="display:inline">
        <button type="submit" class="logout-btn">Logout</button>
    </form>
</div>

<div class="page-wrap">
    <div class="page-title">📅 My Calendar</div>
    <p class="page-subtitle">Plan your gym sessions, rest days, races, and busy days, $username.</p>

    <!-- Stats row -->
    <div class="stats-row">
        <div class="stat-box">
            <div class="stat-number" id="stat-gym">${events.count { it["type"] == "gym" }}</div>
            <div class="stat-label">🏋️ Gym Days</div>
        </div>
        <div class="stat-box">
            <div class="stat-number" id="stat-rest">${events.count { it["type"] == "rest" }}</div>
            <div class="stat-label">😴 Rest Days</div>
        </div>
        <div class="stat-box">
            <div class="stat-number" id="stat-busy">${events.count { it["type"] == "busy" }}</div>
            <div class="stat-label">📅 Busy Days</div>
        </div>
    </div>

    <div class="cal-layout">
        <!-- CALENDAR -->
        <div class="cal-card">
            <div class="cal-header">
                <a class="cal-nav-btn" href="/calendar?month=$prevMonth&year=$prevYear">&#8249;</a>
                <h2>${monthNames[currentMonth - 1]} $currentYear</h2>
                <a class="cal-nav-btn" href="/calendar?month=$nextMonth&year=$nextYear">&#8250;</a>
            </div>
            <div style="padding:0 4px 12px">
                <table class="cal-table">
                    <thead>
                        <tr>
                            ${dayNames.joinToString("") { "<th>$it</th>" }}
                        </tr>
                    </thead>
                    <tbody>
                        $cells
                    </tbody>
                </table>
            </div>
            <!-- Legend -->
            <div style="padding:0 16px 16px">
                <div class="legend">
                    <div class="legend-item"><span class="legend-dot" style="background:#2d6a4f"></span>Gym</div>
                    <div class="legend-item"><span class="legend-dot" style="background:#74c69d"></span>Rest</div>
                    <div class="legend-item"><span class="legend-dot" style="background:#e63946"></span>Busy</div>
                    <div class="legend-item"><span class="legend-dot" style="background:#2d6a4f;border-radius:50%"></span>Today</div>
                </div>
            </div>
        </div>

        <!-- SIDEBAR -->
        <div class="sidebar">
            <!-- Quick Add -->
            <div class="side-card">
                <h3>➕ Quick Add Event</h3>
                <form action="/calendar/add" method="post">
                    <input type="hidden" name="month" value="$currentMonth"/>
                    <input type="hidden" name="year" value="$currentYear"/>
                    <div class="form-group">
                        <label>Day</label>
                        <input type="number" name="day" min="1" max="$daysInMonth" placeholder="e.g. 15" required/>
                    </div>
                    <div class="form-group">
                        <label>Type</label>
                        <select name="type" required>
                            <option value="gym">🏋️ Gym Session</option>
                            <option value="rest">😴 Rest Day</option>
                            <option value="busy">📅 Busy Day</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Note (optional)</label>
                        <textarea name="note" placeholder="e.g. Leg day, 10K race, work meeting..."></textarea>
                    </div>
                    <button type="submit" class="btn-green">Add to Calendar</button>
                </form>
            </div>

            <!-- Upcoming Events -->
            <div class="side-card">
                <h3>📋 This Month</h3>
                <div class="existing-events-list">
                    $upcomingHtml
                </div>
            </div>
        </div>
    </div>
</div>

<!-- Day Modal -->
<div class="modal-overlay" id="dayModal">
    <div class="modal">
        <button class="modal-close" onclick="closeModal()">&#215;</button>
        <h2 id="modal-title">Day Details</h2>

        <div id="modal-existing" class="existing-events-list"></div>

        <div class="timetable-section">
            <h4>📆 Day Timetable</h4>
            <div class="time-slots" id="timeSlots">
                <div class="time-slot">
                    <input type="time" value="06:00" class="slot-time"/>
                    <input type="text" placeholder="e.g. Morning run" class="slot-task"/>
                </div>
                <div class="time-slot">
                    <input type="time" value="09:00" class="slot-time"/>
                    <input type="text" placeholder="e.g. Gym - chest day" class="slot-task"/>
                </div>
            </div>
            <button class="add-slot-btn" onclick="addTimeSlot()">+ Add time slot</button>
        </div>

        <div class="modal-actions">
            <button class="btn-save" onclick="saveTimetable()">Save Timetable</button>
            <button class="btn-cancel" onclick="closeModal()">Close</button>
        </div>
    </div>
</div>

<script>
    // Store events from server
    const serverEvents = ${buildEventJson(events)};
    const timetables = JSON.parse(localStorage.getItem('gymTimetables') || '{}');

    function openDayModal(day, month, year) {
        const key = year + '-' + String(month).padStart(2,'0') + '-' + String(day).padStart(2,'0');
        document.getElementById('modal-title').textContent = 
            new Date(year, month-1, day).toLocaleDateString('en-GB', {weekday:'long', day:'numeric', month:'long', year:'numeric'});

        // Show existing events for this day
        const dayEvents = serverEvents.filter(e => parseInt(e.day) === day);
        const existingDiv = document.getElementById('modal-existing');
        if (dayEvents.length > 0) {
            existingDiv.innerHTML = dayEvents.map(e => {
                const typeMap = {gym:'tag-gym',rest:'tag-rest',busy:'tag-busy'};
                const labelMap = {gym:'🏋️ Gym',rest:'😴 Rest',busy:'📅 Busy'};
                return '<span class="existing-event-tag ' + (typeMap[e.type]||'') + '">' +
                    (labelMap[e.type]||e.type) + (e.note ? ': ' + e.note : '') + '</span>';
            }).join('');
        } else {
            existingDiv.innerHTML = '<p style="color:#888;font-size:0.82rem;margin-bottom:8px">No events yet — add one using the form ➡</p>';
        }

        // Load saved timetable
        const saved = timetables[key] || [];
        const slotsDiv = document.getElementById('timeSlots');
        if (saved.length > 0) {
            slotsDiv.innerHTML = saved.map(s =>
                '<div class="time-slot">' +
                '<input type="time" value="' + s.time + '" class="slot-time"/>' +
                '<input type="text" value="' + s.task + '" placeholder="Activity..." class="slot-task"/>' +
                '</div>'
            ).join('');
        } else {
            slotsDiv.innerHTML = 
                '<div class="time-slot"><input type="time" value="06:00" class="slot-time"/><input type="text" placeholder="e.g. Morning run" class="slot-task"/></div>' +
                '<div class="time-slot"><input type="time" value="09:00" class="slot-time"/><input type="text" placeholder="e.g. Gym session" class="slot-task"/></div>';
        }

        document.getElementById('dayModal').dataset.key = key;
        document.getElementById('dayModal').classList.add('open');
    }

    function closeModal() {
        document.getElementById('dayModal').classList.remove('open');
    }

    function addTimeSlot() {
        const slotsDiv = document.getElementById('timeSlots');
        const div = document.createElement('div');
        div.className = 'time-slot';
        div.innerHTML = '<input type="time" class="slot-time"/><input type="text" placeholder="Activity..." class="slot-task"/>';
        slotsDiv.appendChild(div);
    }

    function saveTimetable() {
        const key = document.getElementById('dayModal').dataset.key;
        const times = document.querySelectorAll('.slot-time');
        const tasks = document.querySelectorAll('.slot-task');
        const slots = [];
        times.forEach((t, i) => {
            if (t.value || tasks[i].value) {
                slots.push({ time: t.value, task: tasks[i].value });
            }
        });
        timetables[key] = slots;
        localStorage.setItem('gymTimetables', JSON.stringify(timetables));
        closeModal();
        showToast('Timetable saved! ✓');
    }

    function showToast(msg) {
        const t = document.createElement('div');
        t.style.cssText = 'position:fixed;bottom:30px;left:50%;transform:translateX(-50%);background:#2d6a4f;color:white;padding:12px 24px;border-radius:12px;font-weight:700;z-index:9999;box-shadow:0 4px 20px rgba(0,0,0,0.2);';
        t.textContent = msg;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 2500);
    }

    // Close modal on overlay click
    document.getElementById('dayModal').addEventListener('click', function(e) {
        if (e.target === this) closeModal();
    });
</script>
</body>
</html>
    """.trimIndent()
}

private fun buildEventJson(events: List<Map<String, String>>): String {
    if (events.isEmpty()) return "[]"
    return "[" + events.joinToString(",") { e ->
        val day = e["day"] ?: ""
        val type = e["type"] ?: ""
        val note = (e["note"] ?: "").replace("\"", "\\\"")
        """{"day":"$day","type":"$type","note":"$note"}"""
    } + "]"
}
