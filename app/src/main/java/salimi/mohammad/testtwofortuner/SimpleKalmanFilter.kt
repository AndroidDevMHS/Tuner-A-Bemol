package salimi.mohammad.testtwofortuner

class SimpleKalmanFilter(
    private val processNoise: Double = 0.08, // کاهش نویز فرآیند برای پایداری
    private val measurementNoise: Double = 1.0, // افزایش نویز اندازه‌گیری برای هموارسازی
    private val estimationError: Double = 0.1
) {
    private var estimate = 0.0
    private var errorCovariance = estimationError

    fun update(measurement: Double): Double {
        // پیش‌بینی
        val predictedEstimate = estimate
        val predictedErrorCovariance = errorCovariance + processNoise

        // به‌روزرسانی
        val kalmanGain = predictedErrorCovariance / (predictedErrorCovariance + measurementNoise)
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate)
        errorCovariance = (1 - kalmanGain) * predictedErrorCovariance

        return estimate
    }
}