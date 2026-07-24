package com.appswithlove.updraft.ui.feedback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appswithlove.updraft.FeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class FeedbackScreenState(
    private val send: (screenshot: ByteArray, type: FeedbackType, description: String, email: String) -> Flow<Double>,
    private val scope: CoroutineScope,
) {
    var selectedType: FeedbackType? by mutableStateOf(null)
    var description: String by mutableStateOf("")
    var email: String by mutableStateOf("")
    var uploadProgress: Double? by mutableStateOf(null)
        private set
    var result: Result<Unit>? by mutableStateOf(null)
        private set

    val canSend: Boolean get() = selectedType != null && uploadProgress == null

    fun sendFeedback(screenshotPng: ByteArray) {
        val type = selectedType ?: return
        uploadProgress = 0.0
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            send(screenshotPng, type, description, email)
                .catch { t ->
                    uploadProgress = null
                    result = Result.failure(t)
                }
                .onCompletion { cause ->
                    if (cause == null && result == null) {
                        uploadProgress = null
                        result = Result.success(Unit)
                    }
                }
                .collect { progress -> uploadProgress = progress }
        }
    }
}
