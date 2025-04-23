package salimi.mohammad.testtwofortuner

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class TunerViewModel: ViewModel() {
    var isHighPrecision= MutableStateFlow(false)
    val keepScreenOn= MutableStateFlow(false)
    val tunerState = mutableStateOf(TunerState())
    val tuningState = mutableStateOf(TuningState())
    val closestNoteState = mutableStateOf(ClosestNote("---", 0, 0.0))
}