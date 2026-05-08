package org.example.routes

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import java.time.YearMonth

fun getRouteInt(call: ApplicationCall, name: String): Int? {

    val text = call.parameters[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getQueryText(call: ApplicationCall, name: String): String {

    var result = ""

    val text = call.request.queryParameters[name]

    if (text != null) {
        result = text.trim()
    }

    return result
}

fun getQueryOptionalText(call: ApplicationCall, name: String): String? {

    val text = call.request.queryParameters[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getQueryInt(call: ApplicationCall, name: String): Int? {

    val text = call.request.queryParameters[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getFormText(params: Parameters, name: String): String {

    var result = ""

    val text = params[name]

    if (text != null) {
        result = text.trim()
    }

    return result
}

fun getOptionalFormText(params: Parameters, name: String): String? {

    val text = params[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getFormInt(params: Parameters, name: String): Int? {

    val text = params[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getFormList(params: Parameters, name: String): List<String> {

    val result = mutableListOf<String>()

    val values = params.getAll(name)

    if (values != null) {
        for (value in values) {
            result.add(value)
        }
    }

    return result
}

fun cleanNotesForDatabase(notes: String?): String? {

    if (notes == null) {
        return null
    }

    val trimmedNotes = notes.trim()

    if (trimmedNotes == "") {
        return null
    }

    return trimmedNotes
}

fun cleanAmounts(amounts: List<String>): List<Double> {

    val cleanedAmounts = mutableListOf<Double>()

    for (amountText in amounts) {
        val value = amountText.toDoubleOrNull()

        if (value != null) {
            if (value > 0) {
                cleanedAmounts.add(value)
            }
        }
    }

    return cleanedAmounts
}

fun convertAmountsForDisplay(amounts: List<String>): List<Double> {

    val displayAmounts = mutableListOf<Double>()

    for (amountText in amounts) {
        val value = amountText.toDoubleOrNull()

        if (value != null) {
            displayAmounts.add(value)
        }
    }

    return displayAmounts
}

fun shiftMonth(monthKey: String, offset: Long): String {

    var shiftedMonth = ""

    try {
        shiftedMonth = YearMonth.parse(monthKey).plusMonths(offset).toString()
    } catch (e: Exception) {
        shiftedMonth = YearMonth.now().plusMonths(offset).toString()
    }

    return shiftedMonth
}