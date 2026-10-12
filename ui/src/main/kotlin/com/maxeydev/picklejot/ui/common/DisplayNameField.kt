package com.maxeydev.picklejot.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R

@Composable
fun DisplayNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    labelAbove: Boolean = false,
    showRequiredError: Boolean = false,
) {
    val label = stringResource(R.string.name_field_label)
    val isMissing = value.isBlank()
    val isError = isMissing && ((required && value.isNotEmpty()) || showRequiredError)
    val supportingText: @Composable () -> Unit = {
        if (isMissing && (required || showRequiredError)) {
            Text(
                text = stringResource(R.string.name_field_required),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else {
            Text(stringResource(R.string.name_field_hint))
        }
    }
    val keyboardOptions =
        KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        )
    val keyboardActions = KeyboardActions(onDone = { onDone() })
    if (labelAbove) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = label },
                placeholder = { Text(stringResource(R.string.name_field_placeholder)) },
                singleLine = true,
                isError = isError,
                supportingText = supportingText,
                shape = RoundedCornerShape(14.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
            )
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            supportingText = supportingText,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}
