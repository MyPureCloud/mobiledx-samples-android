package com.genesys.cloud.messenger.sample.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesys.cloud.messenger.sample.R

/**
 * Actions the host fragment can perform on the active tracker. Each maps to a button on
 * [TrackingScreen]; the handler returns a status string that is shown to the user / Appium.
 */
enum class TrackingAction {
    APPLY_SETTERS,
    SCREEN_VIEWED,
    SEARCH_PERFORMED,
    CUSTOM_EVENT,
    SET_TRAITS,
    GET_SESSION_ID,
    CLEAR,
    START_CHAT,
}

/**
 * Minimal, automation-friendly tracking screen: two free-text inputs (setters + traits) parsed by
 * [TrackingInputParser], a button per [TrackingAction], and a selectable status line (e.g. for the
 * session id). All inputs/outputs are reachable via `testTag` for Appium.
 *
 * @param onAction invoked with the chosen action and the current setters/traits text; returns the
 * status string to display.
 */
@Composable
fun TrackingScreen(
    onAction: (action: TrackingAction, setters: String, traits: String) -> String,
    modifier: Modifier = Modifier,
) {
    var setters by remember { mutableStateOf("") }
    var traits by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var showSettersInfo by remember { mutableStateOf(false) }
    var showTraitsInfo by remember { mutableStateOf(false) }

    fun run(action: TrackingAction) {
        status = onAction(action, setters, traits)
    }

    Column(
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.tracking_title),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = setters,
            onValueChange = { setters = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(INPUT_HEIGHT)
                .testTag(TAG_SETTERS_INPUT),
            label = { Text(stringResource(R.string.tracking_setters_label)) },
            placeholder = { Text(stringResource(R.string.tracking_setters_hint)) },
            trailingIcon = {
                Row {
                    TemplateButton(TAG_SETTERS_TEMPLATE_BUTTON) {
                        setters = TrackingInputParser.SETTERS_TEMPLATE
                    }
                    InfoButton(TAG_SETTERS_INFO_BUTTON) { showSettersInfo = true }
                }
            },
        )

        OutlinedTextField(
            value = traits,
            onValueChange = { traits = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(INPUT_HEIGHT)
                .testTag(TAG_TRAITS_INPUT),
            label = { Text(stringResource(R.string.tracking_traits_label)) },
            placeholder = { Text(stringResource(R.string.tracking_traits_hint)) },
            trailingIcon = {
                Row {
                    TemplateButton(TAG_TRAITS_TEMPLATE_BUTTON) {
                        traits = TrackingInputParser.TRAITS_TEMPLATE
                    }
                    InfoButton(TAG_TRAITS_INFO_BUTTON) { showTraitsInfo = true }
                }
            },
        )

        ButtonRow {
            ActionButton(R.string.tracking_apply_setters, TAG_APPLY_SETTERS) { run(TrackingAction.APPLY_SETTERS) }
            ActionButton(R.string.tracking_set_traits, TAG_SET_TRAITS) { run(TrackingAction.SET_TRAITS) }
        }
        ButtonRow {
            ActionButton(R.string.tracking_screen_viewed, TAG_SCREEN_VIEWED) { run(TrackingAction.SCREEN_VIEWED) }
            ActionButton(R.string.tracking_search_performed, TAG_SEARCH_PERFORMED) { run(TrackingAction.SEARCH_PERFORMED) }
        }
        ButtonRow {
            ActionButton(R.string.tracking_custom_event, TAG_CUSTOM_EVENT) { run(TrackingAction.CUSTOM_EVENT) }
            ActionButton(R.string.tracking_get_session_id, TAG_GET_SESSION_ID) { run(TrackingAction.GET_SESSION_ID) }
        }
        ButtonRow {
            ActionButton(R.string.tracking_start_chat, TAG_START_CHAT) { run(TrackingAction.START_CHAT) }
            ActionButton(R.string.tracking_clear, TAG_CLEAR) { run(TrackingAction.CLEAR) }
        }

        SelectionContainer {
            Text(
                text = status,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_STATUS),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showSettersInfo) {
        InfoDialog(
            titleRes = R.string.tracking_setters_info_title,
            bodyRes = R.string.tracking_setters_info_body,
            testTag = TAG_SETTERS_INFO_DIALOG,
            onDismiss = { showSettersInfo = false },
        )
    }
    if (showTraitsInfo) {
        InfoDialog(
            titleRes = R.string.tracking_traits_info_title,
            bodyRes = R.string.tracking_traits_info_body,
            testTag = TAG_TRAITS_INFO_DIALOG,
            onDismiss = { showTraitsInfo = false },
        )
    }
}

/**
 * Small trailing-icon button rendered inside a field to surface [InfoDialog] explaining its
 * accepted keys.
 */
@Composable
private fun InfoButton(testTag: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = stringResource(R.string.tracking_info_content_description),
        )
    }
}

/**
 * Small trailing-icon button rendered inside a field that fills it with a placeholder listing
 * every supported key with no value, so testers can fill in only what they need; untouched
 * (empty) keys are a no-op.
 */
@Composable
private fun TemplateButton(testTag: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.tracking_template_content_description),
        )
    }
}

/**
 * Explains the keys accepted by a tracking input field and reminds the user that they only take
 * effect in memory once the matching action button is tapped.
 */
@Composable
private fun InfoDialog(titleRes: Int, bodyRes: Int, testTag: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(testTag),
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(bodyRes)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@Composable
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RowScope.ActionButton(labelRes: Int, testTag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 40.dp)
            .testTag(testTag),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
        )
    }
}

private val INPUT_HEIGHT = 110.dp

const val TAG_SETTERS_INPUT = "tracking_setters_input"
const val TAG_TRAITS_INPUT = "tracking_traits_input"
const val TAG_SETTERS_INFO_BUTTON = "tracking_setters_info_button"
const val TAG_TRAITS_INFO_BUTTON = "tracking_traits_info_button"
const val TAG_SETTERS_TEMPLATE_BUTTON = "tracking_setters_template_button"
const val TAG_TRAITS_TEMPLATE_BUTTON = "tracking_traits_template_button"
const val TAG_SETTERS_INFO_DIALOG = "tracking_setters_info_dialog"
const val TAG_TRAITS_INFO_DIALOG = "tracking_traits_info_dialog"
const val TAG_STATUS = "tracking_status"
const val TAG_APPLY_SETTERS = "tracking_apply_setters_button"
const val TAG_SCREEN_VIEWED = "tracking_screen_viewed_button"
const val TAG_SEARCH_PERFORMED = "tracking_search_performed_button"
const val TAG_CUSTOM_EVENT = "tracking_custom_event_button"
const val TAG_SET_TRAITS = "tracking_set_traits_button"
const val TAG_GET_SESSION_ID = "tracking_get_session_id_button"
const val TAG_START_CHAT = "tracking_start_chat_button"
const val TAG_CLEAR = "tracking_clear_button"
