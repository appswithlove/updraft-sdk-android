package com.appswithlove.updraft.ui.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.ui.drawing.DrawingCanvas
import com.appswithlove.updraft.ui.drawing.DrawingController
import com.appswithlove.updraft.ui.resources.Res
import com.appswithlove.updraft.ui.resources.icn_exit_2_white
import com.appswithlove.updraft.ui.resources.icn_exit_color
import com.appswithlove.updraft.ui.resources.updraft_button_cancel
import com.appswithlove.updraft.ui.resources.updraft_button_next
import com.appswithlove.updraft.ui.resources.updraft_button_previous
import com.appswithlove.updraft.ui.resources.updraft_button_sendFeedback
import com.appswithlove.updraft.ui.resources.updraft_feedback_description_label
import com.appswithlove.updraft.ui.resources.updraft_feedback_drawDescription
import com.appswithlove.updraft.ui.resources.updraft_feedback_email_placeholder
import com.appswithlove.updraft.ui.resources.updraft_feedback_send_failure_description
import com.appswithlove.updraft.ui.resources.updraft_feedback_title
import com.appswithlove.updraft.ui.resources.updraft_feedback_type_title
import com.appswithlove.updraft.ui.resources.updraft_logo_white
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val CharcoalGrey = Color(0xFF38383C)
internal val CharcoalGrey70 = Color(0xB338383C)
internal val UpdraftYellow = Color(0xFFF6B33E)
internal val UpdraftRed = Color(0xFFEB6764)
internal val UpdraftBlue = Color(0xFF4A90E2)
internal val Greyish = Color(0xFFA6A6A6)

@Composable
fun FeedbackScreen(
    screenshotPng: ByteArray?,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = remember {
        FeedbackScreenState(
            send = { screenshot, type, description, email ->
                Updraft.sendFeedback(screenshot, type, description, email)
            },
            scope = scope,
        )
    }
    val density = LocalDensity.current
    val drawingController = remember { DrawingController(initialStrokeWidthPx = with(density) { 4.dp.toPx() }) }
    var annotating by remember { mutableStateOf(screenshotPng != null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.result) {
        if (state.result?.isSuccess == true) onClose()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(CharcoalGrey).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, end = 6.dp)) {
            Image(
                painter = painterResource(Res.drawable.updraft_logo_white),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterStart).height(28.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(Res.string.updraft_feedback_title),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
            Image(
                painter = painterResource(Res.drawable.icn_exit_2_white),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).clickable { onClose() }.padding(12.dp),
            )
        }

        if (screenshotPng != null && annotating) {
            val screenshotBitmap = remember(screenshotPng) { decodePng(screenshotPng) }
            Box(modifier = Modifier.weight(1f).onSizeChanged { canvasSize = it }) {
                Image(
                    bitmap = screenshotBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
                DrawingCanvas(drawingController, modifier = Modifier.fillMaxSize())
                if (drawingController.paths.isEmpty() && drawingController.currentStroke.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(CharcoalGrey70),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.updraft_feedback_drawDescription),
                            fontSize = 28.sp,
                            fontStyle = FontStyle.Italic,
                            color = UpdraftYellow,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(Color.Black, Color.White, UpdraftYellow, UpdraftRed).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(48.dp)
                            .clickable { drawingController.color = swatch },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (drawingController.color == swatch) {
                            Box(modifier = Modifier.size(44.dp).border(3.dp, UpdraftBlue, CircleShape))
                        }
                        Box(modifier = Modifier.size(38.dp).background(swatch, CircleShape).border(1.dp, Color.White, CircleShape))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.White,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                                ),
                            )
                        }
                        .clickable { drawingController.undo() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.icn_exit_color),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { annotating = false }) {
                    Text(stringResource(Res.string.updraft_button_next), fontSize = 16.sp, color = UpdraftYellow)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 60.dp, bottom = 30.dp),
            ) {
                FeedbackTypeDropdown(selected = state.selectedType, onSelect = { state.selectedType = it })
                TextField(
                    value = state.description,
                    onValueChange = { state.description = it },
                    placeholder = { Text(stringResource(Res.string.updraft_feedback_description_label), color = Greyish) },
                    minLines = 6,
                    singleLine = false,
                    shape = RectangleShape,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontStyle = FontStyle.Italic, color = CharcoalGrey),
                    colors = feedbackTextFieldColors(),
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                )
                TextField(
                    value = state.email,
                    onValueChange = { state.email = it },
                    placeholder = { Text(stringResource(Res.string.updraft_feedback_email_placeholder), color = Greyish) },
                    singleLine = true,
                    shape = RectangleShape,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontStyle = FontStyle.Italic, color = CharcoalGrey),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = feedbackTextFieldColors(),
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                )
                state.uploadProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        color = UpdraftYellow,
                        modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                    )
                }
                if (state.result?.isFailure == true) {
                    Text(
                        text = stringResource(Res.string.updraft_feedback_send_failure_description),
                        color = UpdraftRed,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 30.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { if (screenshotPng != null) annotating = true else onClose() },
                    ) {
                        Text(
                            text = stringResource(
                                if (screenshotPng != null) Res.string.updraft_button_previous else Res.string.updraft_button_cancel,
                            ),
                            fontSize = 16.sp,
                            color = UpdraftYellow,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        enabled = state.canSend,
                        onClick = {
                            val base = screenshotPng ?: ByteArray(0)
                            val annotated = if (screenshotPng != null) {
                                renderAnnotated(base, drawingController.paths, canvasSize)
                            } else {
                                base
                            }
                            state.sendFeedback(annotated)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UpdraftYellow,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 17.dp),
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        if (state.uploadProgress != null) {
                            CircularProgressIndicator()
                        } else {
                            Text(stringResource(Res.string.updraft_button_sendFeedback), color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun feedbackTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = CharcoalGrey,
    unfocusedTextColor = CharcoalGrey,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackTypeDropdown(selected: FeedbackType?, onSelect: (FeedbackType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(stringResource(Res.string.updraft_feedback_type_title), color = Greyish) },
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontStyle = FontStyle.Italic),
            trailingIcon = {
                CompositionLocalProvider(LocalContentColor provides UpdraftYellow) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            shape = RectangleShape,
            colors = feedbackTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FeedbackType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}
