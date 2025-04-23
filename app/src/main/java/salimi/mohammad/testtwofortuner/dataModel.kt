package salimi.mohammad.testtwofortuner

data class Note(
    val name: String,
    val octave: Int,
    val variation: String,
    val frequency: Double
)

data class TunerState(
    val frequency: Float = 0f,
    val note: String = "---",
    val deviation: Float = 0f,
    val hasValidPitch: Boolean = false
)

data class TuningState(
    val referenceFrequency: Float = 440f
)

data class ClosestNote(
    val name: String,
    val octave: Int,
    val standardFrequency: Double
)