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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val MIN_TOUCH_TARGET = 48.dp
private val CHIP_HEIGHT = 40.dp
private val CHIP_ICON_SIZE = 18.dp

@Composable
fun PillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showsCheck: Boolean = false,
    trailingIcon: Int? = null,
    filled: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val selectedContainer = if (filled) colors.primary else colors.primaryContainer
    val selectedContent = if (filled) colors.onPrimary else colors.onPrimaryContainer
    Box(
        modifier =
            modifier
                .heightIn(min = MIN_TOUCH_TARGET)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) selectedContainer else colors.surfaceContainerLowest,
            contentColor = if (isSelected) selectedContent else colors.onSurface,
            border = BorderStroke(1.dp, if (isSelected) colors.primary else PicklejotTheme.colors.cardBorder),
        ) {
            Row(
                modifier = Modifier.heightIn(min = CHIP_HEIGHT).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showsCheck) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(CHIP_ICON_SIZE),
                    )
                }
                Text(text = label, style = MaterialTheme.typography.labelLarge)
                if (trailingIcon != null) {
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(CHIP_ICON_SIZE),
                    )
                }
            }
        }
    }
}
