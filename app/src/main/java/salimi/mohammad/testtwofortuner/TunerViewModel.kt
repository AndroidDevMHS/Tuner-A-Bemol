package salimi.mohammad.testtwofortuner

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class TunerViewModel : ViewModel() {

    var isHighPrecision = MutableStateFlow(false)
    val keepScreenOn = MutableStateFlow(false)
    val tunerState = MutableStateFlow(TunerState())
    val tuningState = mutableStateOf(TuningState())
    val closestNoteState = MutableStateFlow(ClosestNote("---", "", "", 0, "", 0.0))

    val nameLan=MutableStateFlow("")
    val sign=MutableStateFlow("")

}