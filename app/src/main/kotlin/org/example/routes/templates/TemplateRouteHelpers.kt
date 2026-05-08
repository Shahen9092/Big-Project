package org.example.routes

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall

fun getTemplateQueryText(call: ApplicationCall, name: String): String? {

    val text = call.request.queryParameters[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getTemplateRouteInt(call: ApplicationCall, name: String): Int? {

    val text = call.parameters[name]

    if (text == null) {
        return null
    }

    val number = text.toIntOrNull()

    return number
}

fun getTemplateFormText(params: Parameters, name: String): String {

    var result = ""

    val text = params[name]

    if (text != null) {
        result = text.trim()
    }

    return result
}

fun getTemplateOptionalFormText(params: Parameters, name: String): String? {

    val text = params[name]

    if (text == null) {
        return null
    }

    return text.trim()
}

fun getTemplateExerciseIds(params: Parameters): List<Int> {

    val exerciseIds = mutableListOf<Int>()

    val values = params.getAll("exerciseId")

    if (values != null) {
        for (value in values) {
            val number = value.toIntOrNull()

            if (number != null) {
                exerciseIds.add(number)
            }
        }
    }

    return exerciseIds
}

fun getTemplateAmountInputs(params: Parameters): List<String> {

    val amountInputs = mutableListOf<String>()

    val values = params.getAll("amount")

    if (values != null) {
        for (value in values) {
            amountInputs.add(value)
        }
    }

    return amountInputs
}

fun cleanTemplateNotesForDatabase(notes: String?): String? {

    if (notes == null) {
        return null
    }

    val trimmedNotes = notes.trim()

    if (trimmedNotes == "") {
        return null
    }

    return trimmedNotes
}

fun cleanTemplateAmounts(amountInputs: List<String>): List<Double> {

    val amounts = mutableListOf<Double>()

    for (amountText in amountInputs) {
        val amount = amountText.toDoubleOrNull()

        if (amount != null) {
            if (amount > 0) {
                amounts.add(amount)
            }
        }
    }

    return amounts
}