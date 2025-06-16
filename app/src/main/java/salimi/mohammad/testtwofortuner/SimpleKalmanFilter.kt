package salimi.mohammad.testtwofortuner

import kotlin.math.abs

class SimpleKalmanFilter(
    private var processNoise: Double = 0.01, // Balanced for stability
    private var measurementNoise: Double = 0.2, // Balanced for noise
    private var estimate: Double = 0.0,
    private var errorCovariance: Double = 1.0
) {
    private val recentMeasurements = mutableListOf<Double>()
    private val maxMeasurements = 5

    fun update(measurement: Double): Double {
        recentMeasurements.add(measurement)
        if (recentMeasurements.size > maxMeasurements) {
            recentMeasurements.removeAt(0)
        }

        val isStable = if (recentMeasurements.size == maxMeasurements) {
            val mean = recentMeasurements.average()
            recentMeasurements.all { abs(it - mean) < 2.0 } // Balanced for C3
        } else {
            false
        }

        if (abs(measurement - estimate) > 10.0 || !isStable) {
            estimate = measurement
            errorCovariance = 1.0
            return estimate
        }

        val predictedEstimate = estimate
        val predictedCovariance = errorCovariance + processNoise
        val kalmanGain = predictedCovariance / (predictedCovariance + measurementNoise)
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate)
        errorCovariance = (1 - kalmanGain) * predictedCovariance
        return estimate
    }
}

class MovingAverage(val windowSize: Int =2) {
    private val values = mutableListOf<Float>()
    fun update(newValue: Float): Float {
        values.add(newValue)
        if (values.size > windowSize) values.removeAt(0)
        return values.average().toFloat()
    }
}
class SimpleKalmanFilterKalm(
    private var processNoise: Double = 0.005, // کاهش برای دقت بالاتر
    private var measurementNoise: Double = 0.05, // مناسب برای محیط آرام
    private var estimate: Double = 0.0,
    private var errorCovariance: Double = 1.0
) {
    private val recentMeasurements = ArrayDeque<Double>(5)
    private val maxMeasurements = 5

    fun update(measurement: Double): Double {
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
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate)
        errorCovariance = (1 - kalmanGain) * predictedCovariance
        return estimate
    }
}
