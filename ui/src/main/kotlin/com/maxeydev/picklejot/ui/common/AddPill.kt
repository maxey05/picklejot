package com.maxeydev.picklejot.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val MIN_TOUCH_TARGET = 48.dp
private val PILL_HEIGHT = 40.dp
private val ICON_SIZE = 20.dp

@Composable
fun AddPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val semanticsModifier =
        if (description != null) {
            Modifier.semantics { contentDescription = description }
        } else {
            Modifier
        }
    Box(
        modifier =
            modifier
                .heightIn(min = MIN_TOUCH_TARGET)
                .clickable(role = Role.Button, onClick = onClick)
                .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceContainerLowest,
            contentColor = colors.primary,
            border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        ) {
            Row(
                modifier = Modifier.heightIn(min = PILL_HEIGHT).padding(start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(ICON_SIZE),
                )
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
