package com.personalaiagent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.personalaiagent.ui.util.MarkdownLite
import com.personalaiagent.ui.util.MdBlock
import com.personalaiagent.ui.util.MdInline
import com.personalaiagent.ui.util.MdStyle

/**
 * Renders the Markdown subset the agent produces with modern typography:
 * headings, bullet lists, quotes and code blocks instead of one wall of text.
 */
@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(text) { MarkdownLite.parse(text) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    if (index > 0) Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        text = annotated(block.inlines, color),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        color = color,
                    )
                }

                is MdBlock.Paragraph -> {
                    if (index > 0) Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        text = annotated(block.inlines, color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }

                is MdBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = block.marker,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = annotated(block.inlines, color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }

                is MdBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = annotated(block.inlines, color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is MdBlock.Code -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(12.dp),
                ) {
                    block.language?.let { language ->
                        Text(
                            text = language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(top = 2.dp))
                    }
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = color,
                    )
                }
            }
        }
    }
}

private fun annotated(inlines: List<MdInline>, color: Color): AnnotatedString = buildAnnotatedString {
    inlines.forEach { run ->
        when (run.style) {
            MdStyle.PLAIN -> append(run.text)
            MdStyle.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = color)) {
                append(run.text)
            }
            MdStyle.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(run.text) }
            MdStyle.CODE -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = color.copy(alpha = 0.08f),
                ),
            ) {
                append(run.text)
            }
        }
    }
}
