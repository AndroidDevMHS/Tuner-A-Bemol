package salimi.mohammad.testtwofortuner

import kotlin.math.abs

class SimpleKalmanFilter(
    private var processNoise: Double = 0.01, // Balanced for stability
    private var measurementNoise: Double = 0.01, // Balanced for noise
    private var estimate: Double = 0.0,
    private var errorCovariance: Double = 1.0
) {
    private val recentMeasurements = mutableListOf<Double>()
    private val maxMeasurements = 8

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