import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class WorkoutSession(
    val date: String,
    val checkInTime: String,
    val checkOutTime: String?
)

object GymRepository {
    private val fileName = "gym_log.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun loadHistory(): MutableList<WorkoutSession> {
        val file = File(fileName)
        if (!file.exists()) return mutableListOf()

        return try {
            val content = file.readText()
            json.decodeFromString(content)
        } catch (e: Exception) {
            println("[Warn] log reading failed,will create a new log. error message: ${e.message}")
            mutableListOf()
        }
    }

    fun saveHistory(sessions: List<WorkoutSession>) {
        try {
            val content = json.encodeToString(sessions)
            File(fileName).writeText(content)
            println("[system] data has been saved to $fileName")
        } catch (e: Exception) {
            println("[error] data saving failed: ${e.message}")
        }
    }
}

class GymCalendar {
    private var currentSession: WorkoutSession? = null
    private val history: MutableList<WorkoutSession> = GymRepository.loadHistory()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        currentSession = history.lastOrNull { it.checkOutTime == null }
    }

    fun checkIn() {
        if (currentSession != null) {
            println("error：You are already in the gym! Please check in and leave.")
            return
        }

        val now = LocalDateTime.now()
        val newSession = WorkoutSession(
            date = now.format(dateFormatter),
            checkInTime = now.format(formatter),
            checkOutTime = null
        )

        history.add(newSession)
        currentSession = newSession
        GymRepository.saveHistory(history)
        println("Successfully check in! Arriving time：${newSession.checkInTime}")
    }

    fun checkOut() {
        val session = currentSession
        if (session == null) {
            println("Error：You are not in the gym, can't check in and leave")
            return
        }

        val now = LocalDateTime.now()
        val updatedSession = session.copy(checkOutTime = now.format(formatter))

        val index = history.indexOf(session)
        if (index != -1) {
            history[index] = updatedSession
        }

        currentSession = null
        GymRepository.saveHistory(history)

        val start = LocalDateTime.parse(session.checkInTime, formatter)
        val duration = ChronoUnit.MINUTES.between(start, now)

        println("Check in successfully! Leaving time：${updatedSession.checkOutTime}")
        println(" The duration of this exercise：$duration minutes")
    }

    fun showHistory() {
        if (history.isEmpty()) {
            println("no exercising record")
            return
        }

        println("\n--- fitness calendar record ---")
        history.reversed().forEachIndexed { index, session ->
            val status = if (session.checkOutTime == null) "🔴 doing exercise" else "🟢 finished"
            val durationStr = if (session.checkOutTime != null) {
                val start = LocalDateTime.parse(session.checkInTime, formatter)
                val end = LocalDateTime.parse(session.checkOutTime, formatter)
                "${ChronoUnit.MINUTES.between(start, end)} minutes"
            } else {
                "-"
            }

            println("${index + 1}. [${session.date}] $status")
            println("   arrive: ${session.checkInTime}")
            println("   leave: ${session.checkOutTime ?: "not leave"}")
            println("   time: $durationStr")
            println("-------------------------")
        }
    }
}

fun main() {
    val gym = GymCalendar()
    val scanner = java.util.Scanner(System.`in`)

    println("Welcome to use Kotlin Gym calendar system (CS Sophomore Edition)")

    while (true) {
        println("\n Please select the operation:")
        println("1. Enter the gym (Check In)")
        println("2. Leave the gym (Check Out)")
        println("3. Check the history (History)")
        println("4. Exit (Exit)")
        print("> ")

        val choice = scanner.nextLine().trim()

        when (choice) {
            "1" -> gym.checkIn()
            "2" -> gym.checkOut()
            "3" -> gym.showHistory()
            "4" -> {
                println("See you!Have a good day!")
                break
            }
            else -> println("Error, please enter 1-4.")
        }
    }
    scanner.close()
}