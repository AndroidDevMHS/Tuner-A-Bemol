package salimi.mohammad.testtwofortuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.filters.BandPass
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import salimi.mohammad.testtwofortuner.ui.theme.TestTwoForTunerTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private var audioRecord: AudioRecord? = null
    private var dispatcher: AudioDispatcher? = null
    private var isRecording = false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var lastDeviationUpdateTime = 0L
    val viewModel by viewModels<TunerViewModel>()

    companion object {
        private var cachedNotes: List<Note> = emptyList()
        private var cachedA4Frequency: Float = 440f
        //var UPDATE_INTERVAL_MS = 30L

        fun calculateUpdateIntervalMs(sampleRate: Int, bufferSize: Int): Long {
            // محاسبه زمان پردازش یک بافر (در میلی‌ثانیه)
            val bufferProcessingTimeMs = (bufferSize / 2.0 / sampleRate * 1000).toLong()
            return bufferProcessingTimeMs.coerceIn(10L, 30L)
        }

        fun getNotes(a4Frequency: Float): List<Note> {
            if (cachedNotes.isNotEmpty() && cachedA4Frequency == a4Frequency) {
                return cachedNotes
            }

            val notes = generateNotes(a4Frequency.toDouble())

            cachedNotes = notes
            cachedA4Frequency = a4Frequency

            return notes
        }

        fun generateNotes(a4Frequency: Double): List<Note> {
            val baseNotes = listOf("C ", "D ", "E ", "F ", "G ", "A ", "B ")
            val persianNames = listOf("دو", "ر", "می", "فا", "سل", "لا", "سی")
            val frenchNames = listOf("Do ", "Re ", "Mi ", "Fa ", "Sol ", "La ", "Si ")
            val semitoneOffsets = mapOf(
                "C " to -9, "D " to -7, "E " to -5, "F " to -4, "G " to -2, "A " to 0, "B " to 2
            )
            val variations = listOf(
                Pair("1", -1.0),  // بمل (نیم پرده پایین)
                Pair("2", -0.5), // کرن (ربع پرده پایین)
                Pair("", 0.0),    // بکار (اصلی)
                Pair("4", 0.5)    // سری (ربع پرده بالا)
            )
            val notes = mutableListOf<Note>()
            for (octave in 2..7) {
                for (noteName in baseNotes) {
                    val baseOffset = semitoneOffsets[noteName] ?: 0

                    for ((variationSymbol, variationOffset) in variations) {
                        val totalOffset = baseOffset + (octave - 4) * 12 + variationOffset
                        val frequency = a4Frequency * 2.0.pow(totalOffset / 12.0)
                        val finalName = "$noteName$variationSymbol".trim()
                        val noteNameIndex = baseNotes.indexOf(noteName)

                        notes.add(
                            Note(
                                name = noteName,
                                persianName = persianNames[noteNameIndex],
                                frenchName = frenchNames[noteNameIndex],
                                octave = octave,
                                variation = variationSymbol,
                                frequency = frequency
                            )
                        )
                    }
                }
            }
            return notes.sortedBy { it.frequency }
        }
    }


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.e("FCM", "Token: $it")
        }
        setContent {
            TestTwoForTunerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    TunerScreen(viewModel, innerPadding)
                    val on = viewModel.keepScreenOn.collectAsState()
                    Log.e("3636","$on")
                    if (on.value)
                        checkAndStartRecording()
                    else
                        stopPitchDetection()
                }
            }
        }
    }


    override fun onPause() {
        super.onPause()
        stopPitchDetection()
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

    fun stopPitchDetection() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        dispatcher?.stop()
        dispatcher = null
        viewModel.keepScreenOn.value = false
    }

    fun stopAndRestartPitchDetection() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        dispatcher?.stop()
        dispatcher = null
        startPitchDetection()
    }

    val kalmanFilter = SimpleKalmanFilterKalm()

    private fun startPitchDetection() {
        if (isRecording) return
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val sampleRateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val sampleRate = sampleRateStr?.toInt() ?: 44100

            val channelConfig = AudioFormat.CHANNEL_IN_STEREO
            val audioFormatB = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormatB
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(1024).coerceAtMost(8192)

            val updateIntervalMs = calculateUpdateIntervalMs(sampleRate, bufferSize)

            Log.e(
                "3636",
                "sampleRate:$sampleRate || buffer:$bufferSize || UPDATE_INTERVAL_MS:$updateIntervalMs"
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
                        val byteBuffer =
                            ByteBuffer.wrap(b, off, len).order(ByteOrder.LITTLE_ENDIAN)
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

            val filter = BandPass(50f, 1000f, sampleRate.toFloat())
            dispatcher?.addAudioProcessor(filter)


            var lastValidPitchTime = 0L
            var lastDeviationUpdateTime = 0L
            val pitchTimeoutMs = 5000L
            val hardResetAfterMs = 10_000L // مدت زمان نگه‌داشتن آخرین نت
            var lastValidTunerState: TunerState? = null

            val pdh = PitchDetectionHandler { result, audioEvent ->
                val pitchInHz = result.pitch
                val probability = result.probability
                val amplitude = audioEvent.rms
                val currentTime = System.currentTimeMillis()

                val isPitchReliable = probability > 0.8f && amplitude > 0.3f && amplitude < 15000f
                if (currentTime - lastDeviationUpdateTime < updateIntervalMs) return@PitchDetectionHandler

                CoroutineScope(Dispatchers.IO).launch {
                    if (pitchInHz > 50 && pitchInHz < 1000 && isPitchReliable) {
                        val effectivePitch = if (viewModel.isHighPrecision.value) {
                            kalmanFilter.update(pitchInHz)
                        } else {
                            pitchInHz
                        }

                        val tuningState = viewModel.tuningState.value
                        val standardFrequencies = getNotes(tuningState.referenceFrequency)
                        val closestNote = getClosestNote(effectivePitch, standardFrequencies)
                        val deviation = calculateDeviation(
                            effectivePitch.toDouble(),
                            closestNote.standardFrequency
                        )

                        val newTunerState = TunerState(
                            frequency = effectivePitch,
                            note = closestNote.name,
                            deviation = deviation,
                            hasValidPitch = true
                        )

                        lastValidPitchTime = currentTime
                        lastDeviationUpdateTime = currentTime
                        lastValidTunerState = newTunerState

                        withContext(Dispatchers.Main) {
                            viewModel.tunerState.value = newTunerState
                            viewModel.closestNoteState.value = closestNote
                        }
                    } else {
                        val timeSinceLastValid = currentTime - lastValidPitchTime

                        if (timeSinceLastValid > hardResetAfterMs) {
                            lastValidTunerState = null
                            withContext(Dispatchers.Main) {
                                viewModel.tunerState.value = TunerState(
                                    frequency = 0f,
                                    note = "---",
                                    deviation = 0f,
                                    hasValidPitch = false
                                )
                                viewModel.closestNoteState.value =
                                    ClosestNote("---", "---", "---", 0, "", 0.0)
                            }
                        } else if (lastValidTunerState != null) {
                            // نمایش آخرین نت بدون تغییر
                            withContext(Dispatchers.Main) {
                                viewModel.tunerState.value = lastValidTunerState!!
                            }
                        }

                        lastDeviationUpdateTime = currentTime
                    }
                }
            }
            val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.MPM,
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

    private fun getClosestNote(frequency: Float, standardNotes: List<Note>): ClosestNote {
        if (frequency <= 0) return ClosestNote("", "", "", 0, "", 0.0)
        val freqDouble = frequency.toDouble()
        val candidates = standardNotes
            .map { note -> note to abs(1200 * log2(freqDouble / note.frequency)) }
            .sortedBy { it.second }
            .take(3) // بررسی سه کاندید برای دقت بیشتر

        // انتخاب نت: ابتدا بکار، سپس کرن با انحراف بسیار کم
        val closest =
            candidates.firstOrNull { it.first.variation == "" && it.second < 25.0 }?.first
                ?: candidates.firstOrNull { it.first.variation == "2" && it.second < 15.0 }?.first
                ?: candidates.firstOrNull()?.first
                ?: return ClosestNote("", "", "", 0, "", 0.0)

        return ClosestNote(
            name = closest.name,
            persianName = closest.persianName,
            frenchName = closest.frenchName,
            octave = closest.octave,
            sign = closest.variation,
            standardFrequency = closest.frequency
        )
    }

    private fun calculateDeviation(frequency: Double, closestFreq: Double): Float {
        if (frequency <= 0 || closestFreq <= 0) return 0f
        val semitoneDiff = 12 * log2(frequency / closestFreq).toFloat()
        val corrected =
            if (viewModel.isHighPrecision.value && abs(semitoneDiff) < 0.009f) 0f else if (abs(
                    semitoneDiff
                ) < 0.001f
            ) 0f else semitoneDiff
        return corrected.coerceIn(-1f, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        dispatcher?.stop()

    }
}
