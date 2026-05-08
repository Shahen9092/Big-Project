package org.example.db

import org.example.db.tables.ActivitiesTable
import org.example.db.tables.ActivitySetsTable
import org.example.db.tables.ExercisesTable
import org.example.db.tables.FriendshipsTable
import org.example.db.tables.GoalsTable
import org.example.db.tables.TemplateSharesTable
import org.example.db.tables.UsersTable
import org.example.db.tables.WorkoutTemplateExercisesTable
import org.example.db.tables.WorkoutTemplatesTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {

        Database.connect(
            url = "jdbc:sqlite:fitness.db",
            driver = "org.sqlite.JDBC"
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                ExercisesTable,
                ActivitiesTable,
                ActivitySetsTable,
                FriendshipsTable,
                WorkoutTemplatesTable,
                WorkoutTemplateExercisesTable,
                GoalsTable,
                TemplateSharesTable
            )
        }
    }
}