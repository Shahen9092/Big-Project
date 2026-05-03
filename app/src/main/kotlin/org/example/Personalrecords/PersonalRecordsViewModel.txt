package com.example.bigproject.personalrecords

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class PersonalRecordsViewModel : ViewModel() {

    private var nextId = 4

    private val _records = mutableStateListOf(
        PersonalRecord(
            id = 1,
            exerciseName = "Bench Press",
            category = "Strength",
            value = 80.0,
            unit = "kg",
            date = "2026-03-19",
            note = "New best after 6 weeks of training"
        ),
        PersonalRecord(
            id = 2,
            exerciseName = "5K Run",
            category = "Cardio",
            value = 24.5,
            unit = "min",
            date = "2026-03-16",
            note = "Steady improvement"
        ),
        PersonalRecord(
            id = 3,
            exerciseName = "Single-Leg Press",
            category = "Recovery",
            value = 45.0,
            unit = "kg",
            date = "2026-03-14",
            note = "Post-injury progress"
        )
    )

    val records: List<PersonalRecord> = _records

    fun addOrUpdateRecord(
        exerciseName: String,
        category: String,
        value: Double,
        unit: String,
        date: String,
        note: String
    ) {
        val existingIndex = _records.indexOfFirst {
            it.exerciseName.equals(exerciseName, ignoreCase = true) &&
            it.unit.equals(unit, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existing = _records[existingIndex]

            // Only update if the new record is better.
            // For "min", smaller is better. For most others, bigger is better.
            val isBetter = if (unit.lowercase() == "min") {
                value < existing.value
            } else {
                value > existing.value
            }

            if (isBetter) {
                _records[existingIndex] = existing.copy(
                    category = category,
                    value = value,
                    date = date,
                    note = note
                )
            }
        } else {
            _records.add(
                PersonalRecord(
                    id = nextId++,
                    exerciseName = exerciseName,
                    category = category,
                    value = value,
                    unit = unit,
                    date = date,
                    note = note
                )
            )
        }
    }

    fun deleteRecord(id: Int) {
        _records.removeAll { it.id == id }
    }
}