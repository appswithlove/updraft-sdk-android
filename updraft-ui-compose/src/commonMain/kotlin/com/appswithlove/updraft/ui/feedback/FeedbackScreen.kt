package com.appswithlove.updraft.ui.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.appswithlove.updraft.FeedbackType
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.ui.drawing.DrawingCanvas
import com.appswithlove.updraft.ui.drawing.DrawingController
import com.appswithlove.updraft.ui.resources.Res
import com.appswithlove.updraft.ui.resources.updraft_button_cancel
import com.appswithlove.updraft.ui.resources.updraft_button_ok
import com.appswithlove.updraft.ui.resources.updraft_button_redo
import com.appswithlove.updraft.ui.resources.updraft_button_sendFeedback
import com.appswithlove.updraft.ui.resources.updraft_button_undo
import com.appswithlove.updraft.ui.resources.updraft_feedback_description_label
import com.appswithlove.updraft.ui.resources.updraft_feedback_email_placeholder
import com.appswithlove.updraft.ui.resources.updraft_feedback_send_failure_description
import com.appswithlove.updraft.ui.resources.updraft_feedback_type_title
import org.jetbrains.compose.resources.stringResource

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
    val drawingController = remember { DrawingController() }
    var annotating by remember { mutableStateOf(screenshotPng != null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.result) {
        if (state.result?.isSuccess == true) onClose()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (annotating && screenshotPng != null) {
            val screenshotBitmap = remember(screenshotPng) { decodePng(screenshotPng) }
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).onSizeChanged { canvasSize = it }) {
                    Image(
                        bitmap = screenshotBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DrawingCanvas(drawingController, modifier = Modifier.fillMaxSize())
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = { drawingController.undo() },
                        enabled = drawingController.canUndo,
                    ) { Text(stringResource(Res.string.updraft_button_undo)) }
                    TextButton(
                        onClick = { drawingController.redo() },
                        enabled = drawingController.canRedo,
                    ) { Text(stringResource(Res.string.updraft_button_redo)) }
                    Button(onClick = { annotating = false }) { Text(stringResource(Res.string.updraft_button_ok)) }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeedbackTypeDropdown(selected = state.selectedType, onSelect = { state.selectedType = it })
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { state.description = it },
                    label = { Text(stringResource(Res.string.updraft_feedback_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { state.email = it },
                    label = { Text(stringResource(Res.string.updraft_feedback_email_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.uploadProgress?.let { progress ->
                    LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
                if (state.result?.isFailure == true) {
                    Text(
                        stringResource(Res.string.updraft_feedback_send_failure_description),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClose) { Text(stringResource(Res.string.updraft_button_cancel)) }
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
                    ) {
                        if (state.uploadProgress != null) {
                            CircularProgressIndicator()
                        } else {
                            Text(stringResource(Res.string.updraft_button_sendFeedback))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackTypeDropdown(selected: FeedbackType?, onSelect: (FeedbackType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.updraft_feedback_type_title)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
