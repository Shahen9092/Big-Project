package org.example.pages

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

    var monthName = month

    if (month == "01") {
        monthName = "January"
    } else if (month == "02") {
        monthName = "February"
    } else if (month == "03") {
        monthName = "March"
    } else if (month == "04") {
        monthName = "April"
    } else if (month == "05") {
        monthName = "May"
    } else if (month == "06") {
        monthName = "June"
    } else if (month == "07") {
        monthName = "July"
    } else if (month == "08") {
        monthName = "August"
    } else if (month == "09") {
        monthName = "September"
    } else if (month == "10") {
        monthName = "October"
    } else if (month == "11") {
        monthName = "November"
    } else if (month == "12") {
        monthName = "December"
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

    var monthName = month

    if (month == "01") {
        monthName = "Jan"
    } else if (month == "02") {
        monthName = "Feb"
    } else if (month == "03") {
        monthName = "Mar"
    } else if (month == "04") {
        monthName = "Apr"
    } else if (month == "05") {
        monthName = "May"
    } else if (month == "06") {
        monthName = "Jun"
    } else if (month == "07") {
        monthName = "Jul"
    } else if (month == "08") {
        monthName = "Aug"
    } else if (month == "09") {
        monthName = "Sep"
    } else if (month == "10") {
        monthName = "Oct"
    } else if (month == "11") {
        monthName = "Nov"
    } else if (month == "12") {
        monthName = "Dec"
    }

    return "$day $monthName $year"
}

fun escapeActivityHtml(text: String): String {

    var safeText = text

    safeText = safeText.replace("&", "&amp;")
    safeText = safeText.replace("<", "&lt;")
    safeText = safeText.replace(">", "&gt;")
    safeText = safeText.replace("\"", "&quot;")
    safeText = safeText.replace("'", "&#x27;")

    return safeText
}