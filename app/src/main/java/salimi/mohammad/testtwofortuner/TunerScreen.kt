package salimi.mohammad.testtwofortuner

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.sharp.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat

import androidx.core.net.toUri
import salimi.mohammad.testtwofortuner.ui.theme.MusicGold
import salimi.mohammad.testtwofortuner.ui.theme.PearlWhite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("DefaultLocale")
@Composable
fun TunerScreen(tunerViewModel: TunerViewModel, paddingValues: PaddingValues) {

    val context = LocalContext.current

    val state by tunerViewModel.tunerState.collectAsState()
    val closestNote by tunerViewModel.closestNoteState.collectAsState()
    var tuning by tunerViewModel.tuningState


    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    val aFre: Int = sharedPreferences.getInt("A-frequency", 440)
    val noteName: String = sharedPreferences.getString("NoteName", "ENGLISH") ?: "ENGLISH"
    val noteSign: String = sharedPreferences.getString("NoteSign", "SIGN") ?: "SIGN"
    val selectedNumber = remember { mutableIntStateOf(aFre) }
    //val  by remember { mutableStateOf(lan) }
    //val  by remember { mutableStateOf(sign) }


    var showDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSetting by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }

    var deviationRounded = (state.deviation * 100).toInt() / 100f
    var isHighPrecisionMode = tunerViewModel.isHighPrecision.collectAsState(initial = false)


    LaunchedEffect(keepScreenOn) {
        if (keepScreenOn) {
            (context as? MainActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            (context as? MainActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            deviationRounded = 0f
        }
    }

    LaunchedEffect(selectedNumber.intValue) {
        tuning = tuning.copy(referenceFrequency = selectedNumber.intValue.toFloat())
        MainActivity.getNotes(selectedNumber.intValue.toFloat())
        Log.e("Tuning", "Selected A4: ${selectedNumber.intValue} Hz")
    }


    val animatedDeviation by animateFloatAsState(
        targetValue = if (keepScreenOn) deviationRounded else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animatedDeviation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "(A) Frequency:",
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "${selectedNumber.intValue}",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFd9a648),
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .clickable { showDialog = true }
                )
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = PearlWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = isHighPrecisionMode.value,
                            onCheckedChange = { isChecked ->
                                tunerViewModel.isHighPrecision.value = isChecked
                                (context as? MainActivity)?.stopAndRestartPitchDetection()
                                Toast.makeText(
                                    context,
                                    if (isChecked) "حالت دقت بالا فعال شد" else "حالت دقت استاندارد فعال شد",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFd9a648),
                                checkmarkColor = Color(0xFF121212)
                            )
                        )
                        Text(
                            text = "High Precision",
                            fontSize = 16.sp,
                            color = PearlWhite,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                if (showDialog) {
                    SelectAFrequency(onDismiss = { showDialog = false }, selectedNumber)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            val gradientGold = Brush.verticalGradient(
                listOf(
                    Color(0xFF71542b),
                    Color(0xFFa7782f),
                    Color(0xFFd9a648),
                    Color(0xFFf0c976)
                )
            )

            val gradientGreen = Brush.verticalGradient(
                listOf(
                    Color(0xFF006400),
                    Color(0xFF228B22),
                    Color(0xFF32CD32),
                    Color(0xFF7CFC00)
                )
            )
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(brush = if (keepScreenOn) gradientGreen else gradientGold)
                    .clickable {
                        keepScreenOn = !keepScreenOn
                        tunerViewModel.keepScreenOn.value = !tunerViewModel.keepScreenOn.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (keepScreenOn) "Stop Tuner" else "Start Tuner",
                    color = Color(0xFF121212),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Transparent)
                        .align(Alignment.BottomCenter),
                ) {
                    val textPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 45f
                        color = PearlWhite.toArgb()
                        typeface = ResourcesCompat.getFont(context, R.font.digi_almas_bold)
                    }
                    val intPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 50f
                        color = Color(0xFFD7AE59).toArgb()
                        typeface = ResourcesCompat.getFont(context, R.font.digi_almas_bold)
                    }
                    val textMusic = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 50f
                        color = Color(0xFFd9a648).toArgb()
                        typeface = ResourcesCompat.getFont(context, R.font.mymusic)
                    }

                    val textNote = "Note"
                    val noteBounds = android.graphics.Rect()
                    textPaint.getTextBounds(textNote, 0, textNote.length, noteBounds)
                    val xNote = 60f
                    val yNote = 60f
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            textNote,
                            xNote,
                            yNote,
                            textPaint
                        )
                    }


                    val persianSign = when (closestNote.sign) {
                        "1" -> "بمل"
                        "2" -> "کرن"
                        "4" -> "سری"
                        else -> ""
                    }
                    val note =
                        if (closestNote.name == "" || keepScreenOn == false) "---" else
                            when (noteName) {
                                "ENGLISH" -> closestNote.name
                                "PERSIAN" -> closestNote.persianName
                                "FRENCH" -> closestNote.frenchName
                                else -> ""
                            }
                    val octave =
                        if (closestNote.name == "---" || keepScreenOn == false) "-" else "${closestNote.octave}"

                    val sign =
                        if (closestNote.name == "---" || keepScreenOn == false) "" else if (noteSign == "SIGN") closestNote.sign else persianSign
                    textMusic.getTextBounds(note, 0, note.length, noteBounds)

                    val xNoteV = 98f
                    val yNoteV = yNote + noteBounds.height() + 40f
                    textPaint.getTextBounds(note, 0, note.length, noteBounds)
                    val x = xNoteV + noteBounds.width() + 15f
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            octave,
                            60f,
                            yNoteV,
                            intPaint
                        )
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            note,
                            xNoteV,
                            yNoteV,
                            textMusic
                        )
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            sign,
                            x,
                            yNoteV,
                            textMusic
                        )
                    }

                    val cent = "CENT"
                    val centBounds = android.graphics.Rect()
                    textPaint.getTextBounds(cent, 0, cent.length, centBounds)
                    val xCent = size.width / 2 - centBounds.width() / 2
                    val yCent = 60f
                    drawContext.canvas.nativeCanvas.drawText(
                        cent,
                        xCent,
                        yCent,
                        textPaint
                    )

                    val centValue = if (keepScreenOn == true) String.format(
                        "%.2f",
                        animatedDeviation
                    ) else "0.00"
                    val centValueBounds = android.graphics.Rect()
                    textPaint.getTextBounds(centValue, 0, centValue.length, centValueBounds)
                    val xValue = size.width / 2 - centBounds.width() / 2
                    val yValue = yCent + centBounds.height() + 40f
                    drawContext.canvas.nativeCanvas.drawText(
                        centValue,
                        xValue,
                        yValue,
                        intPaint
                    )

                    val textFre = "Frequency"
                    val freBounds = android.graphics.Rect()
                    textPaint.getTextBounds(textFre, 0, textFre.length, freBounds)
                    val xFre = size.width - freBounds.width() - 60f
                    val yFre = 60f
                    drawContext.canvas.nativeCanvas.drawText(
                        textFre,
                        xFre,
                        yFre,
                        textPaint
                    )

                    val textFreV = if (keepScreenOn == true)
                        "${
                            if (state.frequency > 0) String.format(
                                "%.1f",
                                state.frequency
                            ) else "0.0"
                        } Hz"
                    else
                        "0.0 Hz"
                    val freVBounds = android.graphics.Rect()
                    textPaint.getTextBounds(textFreV, 0, textFreV.length, freVBounds)
                    val xx = size.width - freBounds.width() - 60f
                    val yy = yFre + freBounds.height() + 40f
                    drawContext.canvas.nativeCanvas.drawText(
                        textFreV,
                        xx,
                        yy,
                        intPaint
                    )

                    val center = Offset(size.width / 2, size.height / 1.2f)
                    val radius = size.minDimension / 2 * 1.1f

                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3F3F3F).copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = radius * 1.2f
                        ),
                        startAngle = -180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 2f)
                    )

                    for (angle in -180..0 step 5) {
                        val tickColor = when {
                            angle % 30 == 0 -> Color(0xFFd9a648)
                            angle % 10 == 0 -> Color(0xFFd9a648).copy(alpha = 0.7f)
                            angle == -95 -> Color(0xFF32CD32)
                            angle == -85 -> Color(0xFF32CD32)
                            else -> PearlWhite.copy(alpha = 0.7f)
                        }

                        val rad = Math.toRadians(angle.toDouble()).toFloat()
                        val tickLength = when {
                            angle % 30 == 0 -> 0.85f
                            angle % 10 == 0 -> 0.9f
                            angle == -95 -> 0.91f
                            angle == -85 -> 0.91f
                            else -> 0.95f
                        }

                        drawLine(
                            color = tickColor,
                            start = Offset(
                                center.x + radius * cos(rad),
                                center.y + radius * sin(rad)
                            ),
                            end = Offset(
                                center.x + (radius * tickLength) * cos(rad),
                                center.y + (radius * tickLength) * sin(rad)
                            ),
                            strokeWidth = when {
                                angle % 30 == 0 -> 5f
                                angle % 10 == 0 -> 3f
                                angle == -95 -> 5f
                                angle == -85 -> 5f
                                else -> 1f
                            }
                        )

                        if (angle % 30 == 0) {
                            val num = when (angle) {
                                -180 -> "-60"
                                -150 -> "-40"
                                -120 -> "-20"
                                -90 -> "0"
                                -60 -> "20"
                                -30 -> "40"
                                0 -> "60"
                                else -> ""
                            }
                            val textPaint = Paint().asFrameworkPaint().apply {
                                isAntiAlias = true
                                textSize = 40f
                                color = Color.White.toArgb()
                                typeface = ResourcesCompat.getFont(context, R.font.digi_almas_bold)
                            }

                            val textX = center.x + (radius * 0.75f) * cos(rad) - 15f
                            val textY = center.y + (radius * 0.8f) * sin(rad) + 15f
                            drawContext.canvas.nativeCanvas.drawText(
                                num,
                                textX,
                                textY,
                                textPaint
                            )
                        }
                    }

                    val needleAngle = animatedDeviation * 135f
                    val needleColor = when {
                        abs(animatedDeviation) < 0.05f -> Color(0xFF32CD32)
                        abs(animatedDeviation) < 0.2f -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }

                    rotate(needleAngle, pivot = center) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(center.x + 2f, center.y + 2f),
                            end = Offset(center.x + 2f, center.y - radius * 0.95f + 2f),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    needleColor,
                                    needleColor.copy(alpha = 0.8f)
                                )
                            ),
                            start = center,
                            end = Offset(center.x, center.y - radius * 0.95f),
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(needleColor, needleColor.copy(alpha = 0.7f))
                            ),
                            radius = 5f,
                            center = Offset(center.x, center.y - radius * 0.95f)
                        )
                    }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFd9a648), Color(0xFFB8860B))
                        ),
                        radius = 19f,
                        center = center
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 6f,
                        center = center
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { showAbout = true },
                modifier = Modifier
                    .size(48.dp)
                    .padding(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.information),
                    contentDescription = "About Dialog",
                    tint = Color(0xFFd9a648),
                )
            }
            IconButton(
                onClick = {
                    val packageName = context.packageName // نام پکیج اپلیکیشن شما
                    try {
                        // تلاش برای باز کردن کافه بازار
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                        )
                    } catch (e: ActivityNotFoundException) {
                        // در صورت عدم نصب کافه بازار، باز کردن مرورگر
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://cafebazaar.ir/app/$packageName".toUri()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .size(53.dp)
                    // .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.Sharp.Star,
                    contentDescription = "About Dialog",
                    tint = Color(0xFFd9a648)
                )
            }
            IconButton(
                onClick = {
                    showSetting = true
                    tunerViewModel.keepScreenOn.value = false
                    keepScreenOn = false
                },
                modifier = Modifier
                    .size(53.dp)
                    // .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Settings Dialog",
                    tint = Color(0xFFd9a648)
                )
            }
        }
    }
    if (showAbout) {
        AboutDialog { showAbout = false }
    }
    if (showSetting) {
        SettingDialog(onDismiss = { showSetting = false }, tunerViewModel = tunerViewModel)
    }
}


@Composable
fun SelectAFrequency(onDismiss: () -> Unit, selectedNumber: MutableState<Int>) {
    val context = LocalContext.current
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPreferences.edit()
    val aFre: Int = sharedPreferences.getInt("A-frequency", 440)
    val numbers = (435..445).toList()
    val listState = rememberLazyListState()
    val selectedIndex = numbers.indexOf(aFre)

    LaunchedEffect(Unit) {
        if (selectedIndex != -1) {
            listState.scrollToItem(
                index = selectedIndex,
                scrollOffset = -150
            )
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF1E1E1E))
                .alpha(0.8f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                items(numbers) { number ->
                    Text(
                        text = number.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedNumber.value = number
                                editor.putInt("A-frequency", number)
                                editor.apply()
                                onDismiss()
                            }
                            .animateItem()
                            .padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = if (number == selectedNumber.value) FontWeight.Bold else FontWeight.Normal,
                        color = if (number == selectedNumber.value)
                            Color(0xFFE7C484)
                        else
                            PearlWhite
                    )
                    if (number != numbers.last()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    ambientColor = Color(0xFF000000).copy(alpha = 0.3f),
                                    spotColor = Color(0xFF000000).copy(alpha = 0.2f)
                                ),
                            thickness = 1.5.dp,
                            color = Color(0xFF3C3C3C).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "درباره اپلیکیشن\n",
                    style = MaterialTheme.typography.displayMedium,
                    color = MusicGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "توسعه داده شده توسط محمدحسین سلیمی\n",
                    style = MaterialTheme.typography.displayMedium,
                    color = PearlWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "Email:\n" +
                            "Salimii.mohamadhosein@gmail.com\n",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 16.sp,
                    color = PearlWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TelegramID:\n" +
                            "Mohmmd_salimi\n",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 16.sp,
                    color = PearlWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "در صورتی که هرگونه پیشنهاد، انتقاد یا هرگونه مسئله در تشخیص تیونر مشاهده کردید به راه های ارتباطی بالا اطلاع دهید ",
                    style = MaterialTheme.typography.displayMedium,
                    color = PearlWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MusicGold)
                ) {
                    Text(
                        "بستن",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF1E1E1E)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingDialog(onDismiss: () -> Unit, tunerViewModel: TunerViewModel) {
    val context = LocalContext.current
    val notes = listOf("C,D,E", "دو, ر, می", "Do,Re,Mi")
    val notesSign = listOf(" 1 , 2 , 3 ", "بمل, کرن, سری")
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    val noteName: String = sharedPreferences.getString("NoteName", "ENGLISH") ?: "ENGLISH"
    val noteSign: String = sharedPreferences.getString("NoteSign", "SIGN") ?: "SIGN"
    val editor: SharedPreferences.Editor = sharedPreferences.edit()

    val n = when (noteName) {
        "ENGLISH" -> notes[0]
        "PERSIAN" -> notes[1]
        "FRENCH" -> notes[2]
        else -> notes[0]
    }
    val selectedOption = remember { mutableStateOf(n) }

    val m = when (noteSign) {
        "SIGN" -> notesSign[0]
        "PERSIAN" -> notesSign[1]
        else -> notesSign[0]
    }
    val selectedSign = remember { mutableStateOf(m) }

    // LaunchedEffect برای رصد تغییرات noteName و noteSign
    LaunchedEffect(selectedOption.value, selectedSign.value) {
        val newNoteName = when (selectedOption.value) {
            "C,D,E" -> "ENGLISH"
            "دو, ر, می" -> "PERSIAN"
            "Do,Re,Mi" -> "FRENCH"
            else -> "ENGLISH"
        }
        val newNoteSign = when (selectedSign.value) {
            " 1 , 2 , 3 " -> "SIGN"
            "بمل, کرن, سری" -> "PERSIAN"
            else -> "SIGN"
        }
        // ذخیره در SharedPreferences
        editor.putString("NoteName", newNoteName)
        editor.putString("NoteSign", newNoteSign)
        editor.apply()

        // فراخوانی تابع getNotes برای اعمال تغییرات
        val aFre: Int = sharedPreferences.getInt("A-frequency", 440)
        MainActivity.getNotes(aFre.toFloat())
        Log.d("SettingDialog", "Applied NoteName: $newNoteName, NoteSign: $newNoteSign")
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()

                    .wrapContentHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(48.dp))
                    Text(
                        text = "تنظیمات",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { onDismiss() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = Color(0xFFd9a648),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0xFF000000).copy(alpha = 0.3f),
                            spotColor = Color(0xFF000000).copy(alpha = 0.2f)
                        ),
                    thickness = 1.5.dp,
                    color = Color(0xFFFFC107).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(22.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "شیوه نمایش نام نت:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        notes.forEach { note ->
                            RadioButton(
                                selected = (selectedOption.value == note),
                                onClick = { selectedOption.value = note },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFd9a648)
                                )
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Rtl),
                                modifier = Modifier
                                    .padding(start = 0.dp)
                                    .clickable { selectedOption.value = note }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0xFF000000).copy(alpha = 0.3f),
                            spotColor = Color(0xFF000000).copy(alpha = 0.2f)
                        ),
                    thickness = 1.5.dp,
                    color = Color(0xFFFFC107).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(22.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "شیوه نمایش نماد:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        notesSign.forEach { noteSign ->
                            RadioButton(
                                selected = (selectedSign.value == noteSign),
                                onClick = { selectedSign.value = noteSign },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFd9a648)
                                )
                            )
                            Text(
                                text = noteSign,
                                style = if (noteSign == " 1 , 2 , 3 ") MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(start = 0.dp)
                                    .clickable { selectedSign.value = noteSign }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0xFF000000).copy(alpha = 0.3f),
                            spotColor = Color(0xFF000000).copy(alpha = 0.2f)
                        ),
                    thickness = 1.5.dp,
                    color = Color(0xFFFFC107).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}