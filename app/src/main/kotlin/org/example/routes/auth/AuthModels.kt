package org.example.routes

import org.example.pages.DashboardStats
import org.example.pages.ProgressPoint

data class DashboardPageData(
    val fullName: String,
    val stats: DashboardStats
)

data class ProgressPageData(
    val fullName: String,
    val exerciseNames: List<String>,
    val selectedExercise: String,
    val points: List<ProgressPoint>
)