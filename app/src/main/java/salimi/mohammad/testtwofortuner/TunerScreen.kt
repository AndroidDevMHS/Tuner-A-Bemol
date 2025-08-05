package salimi.mohammad.testtwofortuner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.delay
import salimi.mohammad.testtwofortuner.ui.theme.Green
import salimi.mohammad.testtwofortuner.ui.theme.MusicGold
import salimi.mohammad.testtwofortuner.ui.theme.PearBlack
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
    val screenState by tunerViewModel.keepScreenOn.collectAsState()
    val isHighPrecisionMode by tunerViewModel.isHighPrecision.collectAsState()

    // خواندن SharedPreferences و به‌روزرسانی پویا
    val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    var noteName by remember { mutableStateOf(sharedPreferences.getString("NoteName", "ENGLISH") ?: "ENGLISH") }
    var noteSign by remember { mutableStateOf(sharedPreferences.getString("NoteSign", "SIGN") ?: "SIGN") }
    val aFre = remember { sharedPreferences.getInt("A-frequency", 440) }
    val selectedNumber = remember { mutableIntStateOf(aFre) }
    var showDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSetting by remember { mutableStateOf(false) }
    var isInitialAnimationRunning by remember { mutableStateOf(false) }

    // رصد تغییرات SharedPreferences
    LaunchedEffect(showSetting) {
        // وقتی SettingDialog بسته می‌شود، noteName و noteSign را دوباره بخوان
        if (!showSetting) {
            noteName = sharedPreferences.getString("NoteName", "ENGLISH") ?: "ENGLISH"
            noteSign = sharedPreferences.getString("NoteSign", "SIGN") ?: "SIGN"
            Log.d("TunerScreen", "Refreshed - noteName: $noteName, noteSign: $noteSign")
        }
    }

    // لاگ برای دیباگ
    LaunchedEffect(noteName, closestNote) {
        Log.d("TunerScreen", "noteName: $noteName, closestNote.name: ${closestNote.name}, closestNote.persianName: ${closestNote.persianName}")
        if (noteName == "PERSIAN" && closestNote.persianName == null) {
            Log.e("TunerScreen", "Error: persianName is null for closestNote!")
        }
    }

    LaunchedEffect(Unit, screenState, selectedNumber.intValue) {
        if (!isInitialAnimationRunning) {
            delay(50)
            isInitialAnimationRunning = true
        }
        (context as? MainActivity)?.window?.let { window ->
            if (screenState) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        tuning = tuning.copy(referenceFrequency = selectedNumber.intValue.toFloat())
        MainActivity.getNotes(selectedNumber.intValue.toFloat())
        Log.e("Tuning", "Selected A4: ${selectedNumber.intValue} Hz")
    }

    val animatedDeviation by animateFloatAsState(
        targetValue = if (screenState) state.deviation else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animatedDeviation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 5.dp, top = 8.dp, start = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showAbout = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(PearlWhite.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Rounded.Info, "About Dialog", tint = MusicGold)
                }
                Icon(
                    painter = if (screenState) painterResource(R.drawable.ic_microphone) else painterResource(R.drawable.mute_microphone),
                    contentDescription = null,
                    tint = if (screenState) Green else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                IconButton(
                    onClick = {
                        showSetting = true
                        tunerViewModel.keepScreenOn.value = false
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(PearlWhite.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Rounded.Settings, "Settings Dialog", tint = MusicGold)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Checkbox(
                        checked = isHighPrecisionMode,
                        onCheckedChange = { isChecked ->
                            tunerViewModel.isHighPrecision.value = isChecked
                            (context as? MainActivity)?.stopAndRestartPitchDetection()
                            Toast.makeText(
                                context,
                                if (isChecked) "دقت بالا فعال شد" else "دقت استاندارد فعال شد",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MusicGold,
                            checkmarkColor = Color(0xFF121212)
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "کاهش نویز",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PearlWhite,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "(مناسب محیط‌های شلوغ)",
                            fontSize = 9.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PearlWhite,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp, horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Rounded.ArrowDropDown, null, tint = MusicGold, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = " ${selectedNumber.intValue}",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MusicGold,
                        modifier = Modifier.padding(start = 5.dp).clickable { showDialog = true }
                    )
                    Text(
                        text = "فرکانس نت (A):",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PearlWhite,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // استفاده از derivedStateOf برای محاسبات نت
            val noteData = remember(closestNote, screenState, noteName, noteSign) {
                derivedStateOf {
                    val persianSign = when (closestNote.sign) {
                        "1" -> "بمل"
                        "2" -> "کرن"
                        "4" -> "سری"
                        else -> ""
                    }
                    val note = if (closestNote.name == "" || !screenState) "--" else
                        when (noteName) {
                            "ENGLISH" -> closestNote.name
                            "PERSIAN" -> closestNote.persianName ?: "خطا: نام فارسی موجود نیست"
                            "FRENCH" -> closestNote.frenchName ?: closestNote.name
                            else -> closestNote.name
                        }
                    val sign = if (closestNote.name == "--" || !screenState) "" else if (noteSign == "SIGN") closestNote.sign else persianSign
                    Triple(note, sign, persianSign)
                }
            }

            val centValue = if (screenState) String.format("%.2f", animatedDeviation) else "-"
            val octave = if (closestNote.name == "---" || !screenState) "-" else "${closestNote.octave}"
            val textFreVV = if (screenState) String.format("%.1f", state.frequency) else "-"

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(0.33f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cent",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        color = MusicGold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = centValue,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().weight(0.33f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${noteData.value.first} ${noteData.value.second}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if ((centValue.toFloatOrNull() ?: 0f) in -0.03f..0.03f) Green else PearlWhite,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().weight(0.33f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Freq",
                        modifier = Modifier.fillMaxWidth(),
                        color = MusicGold,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$textFreVV Hz",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(5.dp, bottom = 50.dp),
                contentAlignment = Alignment.Center
            ) {
                val tickData = remember {
                    (-180..0 step 5).map { angle ->
                        val tickColor = when {
                            angle % 30 == 0 -> MusicGold
                            angle % 10 == 0 -> MusicGold.copy(alpha = 0.7f)
                            angle == -95 || angle == -85 -> Color(0xFF2ECC71)
                            else -> PearlWhite.copy(alpha = 0.7f)
                        }
                        val rad = Math.toRadians(angle.toDouble()).toFloat()
                        val tickLength = when {
                            angle % 30 == 0 -> 0.85f
                            angle % 10 == 0 -> 0.90f
                            angle == -95 || angle == -85 -> 0.91f
                            else -> 0.95f
                        }
                        val strokeWidth = when {
                            angle % 30 == 0 -> 5f
                            angle % 10 == 0 -> 3f
                            angle == -95 || angle == -85 -> 5f
                            else -> 1.5f
                        }
                        Triple(tickColor, tickLength, strokeWidth)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                        .background(PearBlack.copy(alpha = 0.001f))
                        .align(Alignment.TopCenter)
                        .height(300.dp)
                ) {
                    val center = Offset(size.width / 2, size.height / 1.5f)
                    val radius = size.minDimension / 2 * 1.15f

                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF3F3F3F).copy(alpha = 0.2f), Color.Transparent),
                            radius = radius * 1.2f
                        ),
                        startAngle = -180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 3f)
                    )

                    tickData.forEachIndexed { index, (tickColor, tickLength, strokeWidth) ->
                        val angle = -180 + index * 5
                        val rad = Math.toRadians(angle.toDouble()).toFloat()
                        drawLine(
                            color = tickColor,
                            start = Offset(center.x + radius * cos(rad), center.y + radius * sin(rad)),
                            end = Offset(center.x + (radius * tickLength) * cos(rad), center.y + (radius * tickLength) * sin(rad)),
                            strokeWidth = strokeWidth
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
                            val numPaint = Paint().asFrameworkPaint().apply {
                                isAntiAlias = true
                                textSize = 40f
                                color = PearlWhite.toArgb()
                                typeface = ResourcesCompat.getFont(context, R.font.digi_almas_bold)
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                num,
                                center.x + (radius * 0.75f) * cos(rad) - 15f,
                                center.y + (radius * 0.80f) * sin(rad) + 15f,
                                numPaint
                            )
                        }
                    }

                    val needleAngle = animatedDeviation * 135f
                    val needleColor = when {
                        abs(animatedDeviation) < 0.05f -> Color(0xFF2ECC71)
                        abs(animatedDeviation) < 0.2f -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }

                    rotate(needleAngle, pivot = center) {
                        drawLine(
                            brush = Brush.verticalGradient(colors = listOf(needleColor, needleColor.copy(alpha = 0.8f))),
                            start = center,
                            end = Offset(center.x, center.y - radius * 0.95f),
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            brush = Brush.radialGradient(colors = listOf(needleColor, needleColor.copy(alpha = 0.7f))),
                            radius = 5f,
                            center = Offset(center.x, center.y - radius * 0.95f)
                        )
                    }

                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(MusicGold, Color(0xFFB8860B))),
                        radius = 18f,
                        center = center
                    )
                    drawCircle(color = Color.Black, radius = 6f, center = center)
                }

                val pulseAnimation by animateFloatAsState(
                    targetValue = if (!screenState && isInitialAnimationRunning) 1.05f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "pulseAnimation"
                )
                val glowAnimation by animateFloatAsState(
                    targetValue = if (!screenState && isInitialAnimationRunning) 12f else 8f,
                    animationSpec = tween(durationMillis = 150),
                    label = "glowAnimation"
                )
                val colorAnimation by animateColorAsState(
                    targetValue = if (!screenState && isInitialAnimationRunning) MusicGold else Color(0xFFD39500),
                    animationSpec = tween(durationMillis = 150),
                    label = "colorAnimation"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(alignment = Alignment.BottomCenter)
                        .height(48.dp)
                        .scale(pulseAnimation)
                        .shadow(
                            elevation = glowAnimation.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color(0xFF000000).copy(alpha = 0.3f),
                            spotColor = Color(0xFFFFC107).copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (screenState)
                                    listOf(Color(0xFF2ECC71), Color(0xFF27AE60))
                                else
                                    listOf(colorAnimation, Color(0xFFD39500))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(interactionSource = interactionSource, indication = null) {
                            tunerViewModel.keepScreenOn.value = !tunerViewModel.keepScreenOn.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (screenState) "توقف تیونر" else "شروع تیونر",
                        color = Color.White,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showDialog) {
        SelectAFrequency(onDismiss = { showDialog = false }, selectedNumber)
    }
    if (showAbout) {
        AboutDialog { showAbout = false }
    }
    if (showSetting) {
        SettingDialog(onDismiss = { showSetting = false })
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
            listState.scrollToItem(index = selectedIndex, scrollOffset = -150)
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .shadow(4.dp, RoundedCornerShape(12.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                items(numbers) { number ->
                    Text(
                        text = "$number Hz",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedNumber.value = number
                                editor.putInt("A-frequency", number)
                                editor.apply()
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = if (number == selectedNumber.value) FontWeight.Bold else FontWeight.Normal,
                        color = if (number == selectedNumber.value) MusicGold else PearlWhite
                    )
                    if (number != numbers.last()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MusicGold.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val telegramId = "Mohmmd_salimi"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "درباره تیونر",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MusicGold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "توسعه داده شده توسط محمدحسین سلیمی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PearlWhite,
                    textAlign = TextAlign.Center
                )

                Divider(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(0.9f),
                    thickness = 0.5.dp,
                    color = Color.Gray
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MusicGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Salimii.mohamadhosein@gmail.com",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = PearlWhite,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://t.me/$telegramId")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "تلگرام نصب نیست یا لینک باز نمی‌شود",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Telegram", tint = MusicGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "@$telegramId",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp,
                        color = MusicGold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "برای پیشنهادات، انتقادات یا گزارش مشکلات، به آیدی تلگرام پیام بدهید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MusicGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(
                        "بستن",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SettingDialog(onDismiss: () -> Unit) {
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
        editor.putString("NoteName", newNoteName)
        editor.putString("NoteSign", newNoteSign)
        editor.apply()
        val aFre: Int = sharedPreferences.getInt("A-frequency", 440)
        MainActivity.getNotes(aFre.toFloat())
        Log.d("SettingDialog", "Applied NoteName: $newNoteName, NoteSign: $newNoteSign")
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .shadow(4.dp, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(48.dp))
                    Text(
                        text = "تنظیمات",
                        style = MaterialTheme.typography.titleLarge,
                        color = MusicGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = MusicGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MusicGold.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "شیوه نمایش نام نت:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PearlWhite,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.End)
                    )
                    notes.forEach { note ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption.value = note }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedOption.value == note),
                                onClick = { selectedOption.value = note },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MusicGold,
                                    unselectedColor = PearlWhite.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Rtl),
                                fontSize = 16.sp,
                                color = if (selectedOption.value == note) MusicGold else PearlWhite,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MusicGold.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "شیوه نمایش نماد:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PearlWhite,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.End)
                    )
                    notesSign.forEach { noteSign ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSign.value = noteSign }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedSign.value == noteSign),
                                onClick = { selectedSign.value = noteSign },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MusicGold,
                                    unselectedColor = PearlWhite.copy(alpha = 0.7f)
                                )
                            )
                            Text(
                                text = noteSign,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 16.sp,
                                color = if (selectedSign.value == noteSign) MusicGold else PearlWhite,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MusicGold,
                        contentColor = Color(0xFF121212)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تأیید",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun TunerScreenPreview() {
    // نمونه فرضی از ViewModel با داده‌های ساختگی یا حالت اولیه
    val fakeViewModel =
        TunerViewModel() // اگر پارامتر دارد، باید آن‌ها را mock کنیم یا از نسخه‌ی fake استفاده کنیم

    // استفاده از remember برای جلوگیری از ایجاد مجدد
    val padding = PaddingValues(16.dp)

    TunerScreen(tunerViewModel = fakeViewModel, paddingValues = padding)
}