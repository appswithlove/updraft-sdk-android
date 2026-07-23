package com.appswithlove.updraft.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.appswithlove.updraft.Updraft
import com.appswithlove.updraft.UpdraftEvent
import com.appswithlove.updraft.ui.resources.Res
import com.appswithlove.updraft.ui.resources.updraft_button_cancel
import com.appswithlove.updraft.ui.resources.updraft_button_ok
import com.appswithlove.updraft.ui.resources.updraft_feedbackDialog_description
import com.appswithlove.updraft.ui.resources.updraft_feedbackDialog_title
import com.appswithlove.updraft.ui.resources.updraft_feedbackDisabled_description
import com.appswithlove.updraft.ui.resources.updraft_feedbackDisabled_title
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_description
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_laterButton
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_openButton
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_title
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_titleWithVersion
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_yourVersion
import com.appswithlove.updraft.ui.resources.updraft_relative_daysAgo
import com.appswithlove.updraft.ui.resources.updraft_relative_hoursAgo
import com.appswithlove.updraft.ui.resources.updraft_relative_justNow
import com.appswithlove.updraft.ui.resources.updraft_relative_monthsAgo
import com.appswithlove.updraft.ui.resources.updraft_relative_weeksAgo
import com.appswithlove.updraft.ui.resources.updraft_relative_yearsAgo
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_descriptionFull
import com.appswithlove.updraft.ui.resources.updraft_updateAvailable_releasedRelative
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun relativeAgePhrase(age: RelativeAge): String = when (age) {
    is RelativeAge.JustNow -> stringResource(Res.string.updraft_relative_justNow)
    is RelativeAge.Hours -> pluralStringResource(Res.plurals.updraft_relative_hoursAgo, age.count, age.count)
    is RelativeAge.Days -> pluralStringResource(Res.plurals.updraft_relative_daysAgo, age.count, age.count)
    is RelativeAge.Weeks -> pluralStringResource(Res.plurals.updraft_relative_weeksAgo, age.count, age.count)
    is RelativeAge.Months -> pluralStringResource(Res.plurals.updraft_relative_monthsAgo, age.count, age.count)
    is RelativeAge.Years -> pluralStringResource(Res.plurals.updraft_relative_yearsAgo, age.count, age.count)
}

/**
 * Shows the new-version message, with `event.createAt` rendered as a human-relative
 * age ("Released 3 weeks ago.") when it is present and parseable.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun UpdateAvailableDialog(
    event: UpdraftEvent.UpdateAvailable,
    onOpen: (String) -> Unit,
    onLater: () -> Unit,
) {
    val title = if (!event.version.isNullOrBlank()) {
        stringResource(Res.string.updraft_updateAvailable_titleWithVersion, event.version!!)
    } else {
        stringResource(Res.string.updraft_updateAvailable_title)
    }
    val agePhrase = event.createAt
        ?.let { parseCreateAt(it) }
        ?.let { relativeAgePhrase(relativeAge(it, Clock.System.now())) }
    val yourVersion = event.yourVersion
    val message = when {
        agePhrase != null && !yourVersion.isNullOrBlank() ->
            stringResource(Res.string.updraft_updateAvailable_descriptionFull, agePhrase, yourVersion)
        agePhrase != null ->
            stringResource(Res.string.updraft_updateAvailable_releasedRelative, agePhrase)
        !yourVersion.isNullOrBlank() ->
            stringResource(Res.string.updraft_updateAvailable_yourVersion, yourVersion)
        else ->
            stringResource(Res.string.updraft_updateAvailable_description)
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onOpen(event.url) }) {
                Text(stringResource(Res.string.updraft_updateAvailable_openButton))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(Res.string.updraft_updateAvailable_laterButton))
            }
        },
    )
}

@Composable
fun FeedbackHintDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.updraft_feedbackDialog_title)) },
        text = { Text(stringResource(Res.string.updraft_feedbackDialog_description)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.updraft_button_ok)) }
        },
    )
}

@Composable
fun FeedbackDisabledDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.updraft_feedbackDisabled_title)) },
        text = { Text(stringResource(Res.string.updraft_feedbackDisabled_description)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.updraft_button_cancel)) }
        },
    )
}

@Composable
fun UpdraftEventHost(events: Flow<UpdraftEvent>, onFeedbackRequested: () -> Unit) {
    var updateEvent by remember { mutableStateOf<UpdraftEvent.UpdateAvailable?>(null) }
    var showHint by remember { mutableStateOf(false) }
    var showDisabled by remember { mutableStateOf(false) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UpdraftEvent.UpdateAvailable -> updateEvent = event
                UpdraftEvent.ShowFeedbackHint -> showHint = true
                UpdraftEvent.FeedbackDisabled -> showDisabled = true
                UpdraftEvent.FeedbackRequested -> onFeedbackRequested()
                UpdraftEvent.CloseFeedback -> Unit
                is UpdraftEvent.Error -> Unit
            }
        }
    }

    updateEvent?.let { event ->
        UpdateAvailableDialog(
            event = event,
            onOpen = { url -> Updraft.openUpdateUrl(url); updateEvent = null },
            onLater = { updateEvent = null },
        )
    }
    if (showHint) FeedbackHintDialog(onDismiss = { showHint = false })
    if (showDisabled) FeedbackDisabledDialog(onDismiss = { showDisabled = false })
}
