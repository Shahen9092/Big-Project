package org.example.routes

import org.jetbrains.exposed.sql.ResultRow

data class DeletedActivitySetBackup(
    val setNumber: Int,
    val amount: Double
)

data class DeletedActivityBackup(
    val userId: Int,
    val exerciseId: Int,
    val date: String,
    val notes: String?,
    val sets: List<DeletedActivitySetBackup>
)

data class EditPageData(
    val activityId: Int,
    val exerciseName: String,
    val category: String,
    val unit: String,
    val date: String,
    val notes: String?,
    val sets: List<Double>
)

data class ExerciseSearchData(
    val exercises: List<ResultRow>,
    val categories: List<String>
)

val deletedActivityBackups = mutableMapOf<Int, DeletedActivityBackup>()