package com.example.bigproject.personalrecords

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsScreen(
    viewModel: PersonalRecordsViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Records") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Track your best performances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Useful for strength, cardio, and recovery progress.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.records.isEmpty()) {
                Text("No records yet. Add your first personal record.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.records) { record ->
                        PersonalRecordCard(
                            record = record,
                            onDelete = { viewModel.deleteRecord(record.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddRecordDialog(
            onDismiss = { showDialog = false },
            onSave = { exercise, category, value, unit, date, note ->
                viewModel.addOrUpdateRecord(
                    exerciseName = exercise,
                    category = category,
                    value = value,
                    unit = unit,
                    date = date,
                    note = note
                )
                showDialog = false
            }
        )
    }
}

@Composable
fun PersonalRecordCard(
    record: PersonalRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = record.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("Category: ${record.category}")
            Text("Record: ${record.value} ${record.unit}")
            Text("Date: ${record.date}")

            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Note: ${record.note}")
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (
        exercise: String,
        category: String,
        value: Double,
        unit: String,
        date: String,
        note: String
    ) -> Unit
) {
    var exercise by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val parsedValue = value.toDoubleOrNull()
                    if (
                        exercise.isNotBlank() &&
                        category.isNotBlank() &&
                        parsedValue != null &&
                        unit.isNotBlank() &&
                        date.isNotBlank()
                    ) {
                        onSave(
                            exercise.trim(),
                            category.trim(),
                            parsedValue,
                            unit.trim(),
                            date.trim(),
                            note.trim()
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text("Add Personal Record")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = exercise,
                    onValueChange = { exercise = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Strength, Cardio, Recovery)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Record Value") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg, reps, min, sec)") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}