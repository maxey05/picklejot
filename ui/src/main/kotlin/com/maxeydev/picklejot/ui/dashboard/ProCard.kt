package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing

internal const val LARGE_FONT_SCALE = 1.3f

@Composable
internal fun isLargeFont(): Boolean = LocalDensity.current.fontScale > LARGE_FONT_SCALE

@Composable
internal fun ProCard(
    reveal: SectionReveal,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    StatsCard(modifier = modifier.then(reveal.modifier)) {
        Column(
            modifier = Modifier.padding(PicklejotSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
            content = content,
        )
    }
}

@Composable
internal fun ProCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}
