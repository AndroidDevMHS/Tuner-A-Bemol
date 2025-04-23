package salimi.mohammad.testtwofortuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.filters.BandPass
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import salimi.mohammad.testtwofortuner.ui.theme.TestTwoForTunerTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private var audioRecord: AudioRecord? = null
    private var dispatcher: AudioDispatcher? = null
    private var isRecording = false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var lastDeviationUpdateTime = 0L

    companion object {
        private var cachedNotes: List<Note> = emptyList()
        private var cachedA4Frequency: Float = 440f
        val deviationHistory = CopyOnWriteArrayList<Float>()
        private const val MAX_HISTORY_SIZE = 50
        private const val UPDATE_INTERVAL_MS = 100L

        fun getNotes(a4Frequency: Float): List<Note> {
            if (cachedNotes.isNotEmpty() && cachedA4Frequency == a4Frequency) {
                return cachedNotes
            }

            val notes = generateNotes(a4Frequency.toDouble())

            cachedNotes = notes
            cachedA4Frequency = a4Frequency

            return notes
        }

        private fun generateNotes(a4Frequency: Double): List<Note> {
            val baseNotes = listOf("C ", "D ", "E ", "F ", "G ", "A ", "B ")
            val semitoneOffsets = mapOf(
                "C " to -9, "D " to -7, "E " to -5, "F " to -4, "G " to -2, "A " to 0, "B " to 2
            )
            val variations = listOf(
                Pair("1", -1.0), // بمل
                Pair("2", -0.5), // کرن
                Pair("", 0.0),   // بکار
                Pair("3", 1.0),  // دیز
                Pair("4", 0.5)   // سری
            )

            val notes = mutableListOf<Note>()

            for (octave in 0..7) {
                for (noteName in baseNotes) {
                    val baseOffset = semitoneOffsets[noteName] ?: 0
                    for ((variationSymbol, variationOffset) in variations) {
                        if (noteName == "E " && variationSymbol == "3") continue
                        if (noteName == "B " && variationSymbol == "3") continue
                        val totalOffset = baseOffset + (octave - 4) * 12 + variationOffset
                        val frequency = a4Frequency * 2.0.pow(totalOffset / 12.0)
                        val finalName = "$noteName$variationSymbol".trim()
                        notes.add(Note(finalName, octave, variationSymbol, frequency))
                    }
                }
            }
            notes.forEach {
                Log.e("not22", "${it.name} ${it.octave} → ${"%.2f".format(it.frequency)} Hz")
            }
            return notes.sortedBy { it.frequency }
        }
    }

    val viewModel by viewModels<TunerViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startPitchDetection()
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "اجازه دسترسی به میکروفن داده نشد. اپ نمی‌تواند کار کند.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        setContent {
            TestTwoForTunerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TuneerScreen(viewModel)
                }
            }
        }

        checkAndStartRecording()
    }

    private fun checkAndStartRecording() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startPitchDetection()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "برای استفاده از تیونر، لطفاً اجازه دسترسی به میکروفن را بدهید.",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun stopAndRestartPitchDetection() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        dispatcher?.stop()
        dispatcher = null
        deviationHistory.clear()
        startPitchDetection()
    }

    /*private fun startPitchDetection() {
        if (isRecording) return

        try {
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 4

            Log.e(
                "PitchDetection",
                "Starting: SampleRate=$sampleRate, BufferSize=$bufferSize, Mode=${if (isHighPrecisionMode.value) "HighPrecision" else "Standard"}, Algorithm=FFT_YIN"
            )

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkAndStartRecording()
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val audioFormat = TarsosDSPAudioFormat(
                sampleRate.toFloat(),
                16,
                1,
                true,
                false
            )

            val audioInputStream = object : TarsosDSPAudioInputStream {
                private val buffer = ShortArray(bufferSize)
                private var isClosed = false

                override fun getFormat(): TarsosDSPAudioFormat = audioFormat
                override fun getFrameLength(): Long = -1
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (!isRecording || isClosed) return -1
                    try {
                        val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        val byteBuffer = ByteBuffer.wrap(b, off, len).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) {
                            byteBuffer.putShort(buffer[i])
                        }
                        return len
                    } catch (e: SecurityException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "خطا در دسترسی به میکروفن: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return -1
                    }
                }

                override fun skip(n: Long): Long = 0
                override fun close() {
                    isClosed = true
                }
            }

            dispatcher = AudioDispatcher(audioInputStream, bufferSize, 0)

            val filter = BandPass(50f, 2500f, sampleRate.toFloat())
            dispatcher?.addAudioProcessor(filter)

            Log.e("Tuning", "A4 Frequency: ${tuningState.value.referenceFrequency} Hz")
            val pdh = PitchDetectionHandler { result, _ ->
                val pitchInHz = result.pitch
                Log.e("Pitch", "Input frequency: $pitchInHz Hz")
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastDeviationUpdateTime < UPDATE_INTERVAL_MS) return@PitchDetectionHandler

                if (pitchInHz > 100 && pitchInHz < 2000) {
                    runOnUiThread {
                        val tuningState = tuningState.value
                        val standardFrequencies =
                            getNotes( tuningState.referenceFrequency)
                        val effectivePitch = pitchInHz
                        val closestNote = getClosestNote(effectivePitch, standardFrequencies)
                        val deviation = calculateDeviation(
                            effectivePitch.toDouble(),
                            closestNote.standardFrequency
                        )
                        tunerState.value = TunerState(
                            frequency = effectivePitch,
                            note = closestNote.name,
                            deviation = deviation,
                            hasValidPitch = true
                        )
                        closestNoteState.value = closestNote
                        deviationHistory.add(deviation * 100) // انحراف واقعی
                        if (deviationHistory.size > MAX_HISTORY_SIZE) {
                            deviationHistory.removeAt(0)
                        }
                        lastDeviationUpdateTime = currentTime
                        Log.e(
                            "PitchResult",
                            "Pitch=$effectivePitch Hz, Note=${closestNote.name} ${closestNote.octave}, Deviation=${deviation * 100} cents"
                        )
                    }
                } else {
                    runOnUiThread {
                        deviationHistory.add(0f) // انحراف صفر وقتی صدا نیست
                        if (deviationHistory.size > MAX_HISTORY_SIZE) {
                            deviationHistory.removeAt(0)
                        }
                        lastDeviationUpdateTime = currentTime
                        Log.e("Pitch", "Invalid frequency: $pitchInHz Hz, Adding deviation=0")
                    }
                }
            }

            val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                sampleRate.toFloat(),
                bufferSize,
                pdh
            )

            dispatcher?.addAudioProcessor(pitchProcessor)

            isRecording = true
            try {
                audioRecord?.startRecording()
            } catch (e: SecurityException) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "خطا در شروع ضبط: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    checkAndStartRecording()
                }
                return
            }

            Thread(dispatcher, "Audio Dispatcher").start()
        } catch (e: SecurityException) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "خطا در دسترسی به میکروفن: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                checkAndStartRecording()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "خطا در راه‌اندازی تیونر: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e("PitchDetection", "Error: ${e.message}")
        }
    }*/
    private fun startPitchDetection() {
        if (isRecording) return

        try {
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 2

            Log.e(
                "PitchDetection",
                "Starting: SampleRate=$sampleRate, BufferSize=$bufferSize, Mode=${if (viewModel.isHighPrecision.value) "HighPrecision" else "Standard"}, Algorithm=FFT_YIN"
            )

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkAndStartRecording()
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val audioFormat = TarsosDSPAudioFormat(
                sampleRate.toFloat(),
                16,
                1,
                true,
                false
            )

            val audioInputStream = object : TarsosDSPAudioInputStream {
                private val buffer = ShortArray(bufferSize)
                private var isClosed = false

                override fun getFormat(): TarsosDSPAudioFormat = audioFormat
                override fun getFrameLength(): Long = -1
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (!isRecording || isClosed) return -1
                    try {
                        val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        val byteBuffer = ByteBuffer.wrap(b, off, len).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) {
                            byteBuffer.putShort(buffer[i])
                        }
                        return len
                    } catch (e: SecurityException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "خطا در دسترسی به میکروفن: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return -1
                    }
                }

                override fun skip(n: Long): Long = 0
                override fun close() {
                    isClosed = true
                }
            }

            dispatcher = AudioDispatcher(audioInputStream, bufferSize, 0)

            val filter = BandPass(50f, 2500f, sampleRate.toFloat())
            dispatcher?.addAudioProcessor(filter)



            val recentPitches = mutableListOf<Float>()
            val smoothingWindow = 2
            var lastValidPitchTime = 0L // زمان آخرین فرکانس معتبر
            val pitchTimeoutMs = 5000L // 5 ثانیه تایم‌اوت برای ریست

            val pdh = PitchDetectionHandler { result, audioEvent ->
                val pitchInHz = result.pitch
                val probability = result.probability
                val amplitude = audioEvent.rms
                val isPitchReliable = probability > 0.85f && amplitude > 0.5f && amplitude < 10000f

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastDeviationUpdateTime < UPDATE_INTERVAL_MS) return@PitchDetectionHandler

                if (pitchInHz > 50 && pitchInHz < 3000 && isPitchReliable) {
                    runOnUiThread {
                        recentPitches.add(pitchInHz)
                        if (recentPitches.size > smoothingWindow) {
                            recentPitches.removeAt(0)
                        }

                        val smoothedPitch = recentPitches.average().toFloat()
                        val kalmanFilter = SimpleKalmanFilter()
                        val effectivePitch = if (viewModel.isHighPrecision.value) {
                            kalmanFilter.update(smoothedPitch.toDouble()).toFloat()
                        } else {
                            smoothedPitch
                        }

                        Log.d(
                            "TunerDebug",
                            "Raw Pitch: $pitchInHz, Smoothed Pitch: $smoothedPitch, Effective Pitch: $effectivePitch, Amplitude: $amplitude, Probability: $probability"
                        )

                        val tuningState = viewModel.tuningState.value
                        val standardFrequencies = getNotes(tuningState.referenceFrequency)
                        val closestNote = getClosestNote(effectivePitch, standardFrequencies)
                        val deviation = calculateDeviation(
                            effectivePitch.toDouble(),
                            closestNote.standardFrequency
                        )

                        viewModel.tunerState.value = TunerState(
                            frequency = effectivePitch,
                            note = closestNote.name,
                            deviation = deviation,
                            hasValidPitch = true
                        )
                        viewModel.closestNoteState.value = closestNote

                        deviationHistory.add(deviation * 100)
                        if (deviationHistory.size > MAX_HISTORY_SIZE) {
                            deviationHistory.removeAt(0)
                        }

                        lastValidPitchTime = currentTime // به‌روزرسانی زمان آخرین فرکانس معتبر
                        lastDeviationUpdateTime = currentTime
                        Log.e(
                            "PitchResult",
                            "Pitch=$effectivePitch Hz, Note=${closestNote.name} ${closestNote.octave}, Deviation=${deviation * 100} cents, Latency=${currentTime - lastDeviationUpdateTime} ms"
                        )
                    }
                } else {
                    runOnUiThread {
                        // اگر زمان زیادی از آخرین فرکانس معتبر گذشته، ریست کن
                        if (currentTime - lastValidPitchTime > pitchTimeoutMs) {
                            viewModel.tunerState.value = TunerState(
                                frequency = 0f,
                                note = "---",
                                deviation = 0f,
                                hasValidPitch = false
                            )
                            viewModel.closestNoteState.value = ClosestNote("---", 0, 0.0)
                            recentPitches.clear() // پاک کردن میانگین متحرک
                        }
                        // در غیر این صورت، tunerState تغییر نمی‌کنه و آخرین مقدار معتبر حفظ می‌شه

                        deviationHistory.add(0f)
                        if (deviationHistory.size > MAX_HISTORY_SIZE) {
                            deviationHistory.removeAt(0)
                        }
                        lastDeviationUpdateTime = currentTime
                        Log.e(
                            "Pitch",
                            "Invalid frequency: $pitchInHz Hz, Amplitude: $amplitude, Probability: $probability"
                        )
                    }
                }
            }

            val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                sampleRate.toFloat(),
                bufferSize,
                pdh
            )

            dispatcher?.addAudioProcessor(pitchProcessor)

            isRecording = true
            try {
                audioRecord?.startRecording()
            } catch (e: SecurityException) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "خطا در شروع ضبط: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    checkAndStartRecording()
                }
                return
            }

            Thread(dispatcher, "Audio Dispatcher").start()
        } catch (e: SecurityException) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "خطا در دسترسی به میکروفن: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                checkAndStartRecording()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "خطا در راه‌اندازی تیونر: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e("PitchDetection", "Error: ${e.message}")
        }
    }

    data class Note(
        val name: String,
        val octave: Int,
        val variation: String,
        val frequency: Double
    )

    private fun getClosestNote(frequency: Float, standardNotes: List<Note>): ClosestNote {
        if (frequency <= 0) return ClosestNote("---", 0, 0.0)
        val freqDouble = frequency.toDouble()
        val candidates = standardNotes
            .map { note -> note to abs(1200 * log2(freqDouble / note.frequency)) }
            .sortedBy { it.second }
            .take(3) // بررسی سه کاندید برای دقت بیشتر

        // لاگ برای دیباگ
        Log.d(
            "TunerDebug",
            "Input Freq: $freqDouble, Candidates: ${candidates.map { "${it.first.name}: ${it.second} cents" }}"
        )

        // انتخاب نت: ابتدا بکار، سپس کرن با انحراف بسیار کم
        val closest = candidates.firstOrNull { it.first.variation == "" && it.second < 30.0 }?.first
            ?: candidates.firstOrNull { it.first.variation == "2" && it.second < 10.0 }?.first
            ?: candidates.firstOrNull()?.first
            ?: return ClosestNote("---", 0, 0.0)

        Log.d(
            "TunerDebug",
            "Selected Note: ${closest.name}, Freq: ${closest.frequency}, Deviation: ${
                1200 * log2(freqDouble / closest.frequency)
            } cents"
        )

        val displayName = when (closest.name) {
            "C 3" -> "D 1"
            "D 3" -> "E 1"
            "F 3" -> "G 1"
            "G 3" -> "A 1"
            "A 3" -> "B 1"
            else -> closest.name
        }

        return ClosestNote(
            name = displayName,
            octave = closest.octave,
            standardFrequency = closest.frequency
        )
    }
    /* private fun getClosestNote(frequency: Float, standardNotes: List<Note>): ClosestNote {
         if (frequency <= 0) return ClosestNote("---", 0, 0.0)
         val freqDouble = frequency.toDouble()
         val candidates = standardNotes
             .map { note -> note to abs(1200 * log2(freqDouble / note.frequency)) }
             .sortedBy { it.second }
             .take(2)
         val closest = candidates.firstOrNull { it.first.variation == "2" }?.first
             ?: candidates.firstOrNull()?.first
             ?: return ClosestNote("---", 0, 0.0)

         val displayName = when (closest.name) {
             "C 3" -> "D 1"
             "D 3" -> "E 1"
             "F 3" -> "G 1"
             "G 3" -> "A 1"
             "A 3" -> "B 1"
             else -> closest.name
         }

         return ClosestNote(
             name = displayName,
             octave = closest.octave,
             standardFrequency = closest.frequency
         )
     }*/

    private fun calculateDeviation(frequency: Double, closestFreq: Double): Float {
        if (frequency <= 0 || closestFreq <= 0) return 0f
        val semitoneDiff = 12 * log2(frequency / closestFreq).toFloat()
        val corrected =
            if (viewModel.isHighPrecision.value) semitoneDiff else if (abs(semitoneDiff) < 0.005f) 0f else semitoneDiff
        return corrected.coerceIn(-1f, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        dispatcher?.stop()
        deviationHistory.clear()
    }
}
