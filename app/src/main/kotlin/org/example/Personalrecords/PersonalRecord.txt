data class PersonalRecord(
    val id: Int,
    val exerciseName: String,
    val category: String,
    val value: Double,
    val unit: String,
    val date: String,
    val note: String = ""
)