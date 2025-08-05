package salimi.mohammad.testtwofortuner

import kotlin.math.abs

class SimpleKalmanFilterKalm(
    private var processNoise: Double = 0.005, // کاهش برای دقت بالاتر
    private var measurementNoise: Double = 0.05, // مناسب برای محیط آرام
    private var estimate: Float = 0.0f,
    private var errorCovariance: Double = 1.0
) {
    private val recentMeasurements = ArrayDeque<Float>(5)
    private val maxMeasurements = 5

    fun update(measurement: Float): Float {
        recentMeasurements.add(measurement)
        if (recentMeasurements.size > maxMeasurements) recentMeasurements.removeFirst()

        val isStable = if (recentMeasurements.size == maxMeasurements) {
            val mean = recentMeasurements.average()
            recentMeasurements.all { abs(it - mean) < 1.0 } // آستانه دقیق‌تر
        } else false

        if (abs(measurement - estimate) > 5.0 || !isStable) { // کاهش آستانه ریست
            estimate = measurement
            errorCovariance = 1.0
            return estimate
        }

        val predictedEstimate = estimate
        val predictedCovariance = errorCovariance + processNoise
        val kalmanGain = predictedCovariance / (predictedCovariance + measurementNoise)
        estimate = (predictedEstimate + kalmanGain * (measurement - predictedEstimate)).toFloat()
        errorCovariance = (1 - kalmanGain) * predictedCovariance
        return estimate
    }
}
