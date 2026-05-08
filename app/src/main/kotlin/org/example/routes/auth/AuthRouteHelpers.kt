package org.example.routes

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall

fun getAuthFormText(params: Parameters, name: String): String {

    var result = ""

    val value = params[name]

    if (value != null) {
        result = value.trim()
    }

    return result
}

fun getAuthPasswordText(params: Parameters, name: String): String {

    var result = ""

    val value = params[name]

    if (value != null) {
        result = value
    }

    return result
}

fun getAuthQueryText(call: ApplicationCall, name: String): String {

    var result = ""

    val value = call.request.queryParameters[name]

    if (value != null) {
        result = value.trim()
    }

    return result
}

fun passwordHasCapitalAndNumber(password: String): Boolean {

    var hasCapital = false
    var hasNumber = false

    for (letter in password) {
        if (letter.isUpperCase()) {
            hasCapital = true
        }

        if (letter.isDigit()) {
            hasNumber = true
        }
    }

    if (hasCapital && hasNumber) {
        return true
    }

    return false
}