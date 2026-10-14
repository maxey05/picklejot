package com.maxeydev.picklejot.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val ONLINE_CARD_MIN_HEIGHT = 56.dp
private val ONLINE_ICON_SIZE = 20.dp
private val HEADING_TOP_PADDING = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    document: LegalDocument,
    onBack: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val blocks = remember(document) { loadBlocks(context, document) }
    val openFailed = stringResource(R.string.link_open_failed)
    val onLink =
        remember(context, openFailed, onOpenDocument) {
            { link: String ->
                val target = LegalDocument.fromLink(link)
                when {
                    target != null -> onOpenDocument(target)
                    !openExternalLink(context, link) -> Toast.makeText(context, openFailed, Toast.LENGTH_SHORT).show()
                    else -> Unit
                }
            }
        }
    Scaffold(
        modifier = modifier.testTag(document.screenTag),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(document.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().testTag(LegalTestTags.CONTENT),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { OnlineCard(onClick = { onLink(document.onlineUrl) }) }
            if (blocks == null) {
                item {
                    Text(
                        text = stringResource(R.string.legal_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(LegalTestTags.LOAD_FAILED),
                    )
                }
            } else {
                items(blocks) { block -> BlockContent(block = block, onLink = onLink) }
            }
        }
    }
}

private fun loadBlocks(
    context: Context,
    document: LegalDocument,
): List<LegalBlock>? =
    runCatching {
        context.assets
            .open(document.assetPath)
            .bufferedReader()
            .use { reader -> LegalMarkdown.parse(reader.readText()) }
    }.getOrNull()

@Composable
private fun OnlineCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = SETTINGS_CARD_SHAPE,
        color = PicklejotTheme.colors.header,
        contentColor = PicklejotTheme.colors.onHeader,
        modifier = Modifier.fillMaxWidth().testTag(LegalTestTags.ONLINE_LINK),
    ) {
        Row(
            modifier = Modifier.heightIn(min = ONLINE_CARD_MIN_HEIGHT).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.legal_read_online),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_open_in_new),
                contentDescription = null,
                tint = PicklejotTheme.colors.headerAccent,
                modifier = Modifier.size(ONLINE_ICON_SIZE),
            )
        }
    }
}

@Composable
private fun BlockContent(
    block: LegalBlock,
    onLink: (String) -> Unit,
) {
    when (block) {
        is LegalBlock.Heading -> HeadingText(block)
        is LegalBlock.Paragraph ->
            LegalText(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                onLink = onLink,
            )
        is LegalBlock.Bullets -> BulletList(block, onLink)
        is LegalBlock.Table -> TableCard(block, onLink)
    }
}

@Composable
private fun HeadingText(block: LegalBlock.Heading) {
    when (block.level) {
        1 -> Unit
        2 ->
            Text(
                text = block.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = HEADING_TOP_PADDING).semantics { heading() },
            )
        else ->
            Text(
                text = block.text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
    }
}

@Composable
private fun BulletList(
    block: LegalBlock.Bullets,
    onLink: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        block.items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "•", style = MaterialTheme.typography.bodyMedium)
                LegalText(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    onLink = onLink,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TableCard(
    block: LegalBlock.Table,
    onLink: (String) -> Unit,
) {
    val labelled = block.header.size > 2
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = settingsCardColor(),
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            block.rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LegalText(
                        text = row.firstOrNull().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        onLink = onLink,
                        fontWeight = FontWeight.SemiBold,
                    )
                    row.drop(1).forEachIndexed { cellIndex, cell ->
                        val label = block.header.getOrNull(cellIndex + 1)
                        LegalText(
                            text = if (labelled && label != null) "$label: $cell" else cell,
                            style = MaterialTheme.typography.bodyMedium,
                            onLink = onLink,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalText(
    text: String,
    style: TextStyle,
    onLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor, onLink) { annotate(text, linkColor, onLink) }
    Text(
        text = annotated,
        style = style,
        color = color,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

private fun annotate(
    text: String,
    linkColor: Color,
    onLink: (String) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        LegalMarkdown.inline(text).forEach { span ->
            val link = span.link
            when {
                link != null ->
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = link,
                            styles =
                                TextLinkStyles(
                                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                                ),
                            linkInteractionListener = { onLink(link) },
                        ),
                    ) { append(span.text) }
                span.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                else -> append(span.text)
            }
        }
    }
