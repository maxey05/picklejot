package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.maxeydev.picklejot.ui.R

@Composable
fun MatchSearchBar(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val container = MaterialTheme.colorScheme.surfaceContainerLowest
    TextField(
        value = text,
        onValueChange = onTextChanged,
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.search_label),
            )
        },
        trailingIcon =
            if (text.isEmpty()) {
                null
            } else {
                {
                    IconButton(
                        onClick = { onTextChanged("") },
                        modifier = Modifier.testTag(MatchListTestTags.SEARCH_CLEAR),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.search_clear),
                        )
                    }
                }
            },
        singleLine = true,
        shape = CircleShape,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                disabledContainerColor = container,
                errorContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier.fillMaxWidth().testTag(MatchListTestTags.SEARCH_FIELD),
    )
}
