package org.example.db

import org.example.db.tables.ExercisesTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.BufferedReader
import java.io.InputStreamReader

object ExerciseSeeder {

    fun seedExercises() {

        val alreadyExists = transaction {
            ExercisesTable.selectAll().count() > 0
        }

        if (alreadyExists) {
            return
        }

        val inputStream = ExerciseSeeder::class.java
            .classLoader
            .getResourceAsStream("exercises.csv")

        if (inputStream == null) {
            return
        }

        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.readLine()

        var line = reader.readLine()

        while (line != null) {

            val parts = line.split(",")

            if (parts.size >= 4) {

                val name = parts[0].trim()
                val category = parts[1].trim()
                val unit = parts[2].trim()
                val notes = parts[3].trim()

                transaction {
                    ExercisesTable.insert {
                        it[ExercisesTable.name] = name
                        it[ExercisesTable.category] = category
                        it[ExercisesTable.defaultUnit] = unit
                        it[ExercisesTable.notes] = notes
                    }
                }
            }

            line = reader.readLine()
        }
    }
}