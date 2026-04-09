package com.example.brainclean.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.brainclean.MainActivity
import com.example.brainclean.QuickAddActivity
import com.example.brainclean.R
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus

class BrainCleanHomeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(56.dp, 56.dp),
            DpSize(120.dp, 56.dp),
            DpSize(120.dp, 120.dp),
            DpSize(250.dp, 200.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val latestInboxThoughts = BrainCleanDatabase.getDatabase(context)
            .thoughtDao()
            .getLatestThoughtsByStatus(ThoughtStatus.INBOX.name, 3)
            .map { it.toThought() }

        provideContent {
            BrainCleanHomeWidgetContent(latestInboxThoughts)
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            BrainCleanHomeWidget().updateAll(context)
        }
    }
}

@Composable
private fun BrainCleanHomeWidgetContent(thoughts: List<Thought>) {
    val currentSize = LocalSize.current
    val widgetLayout = when {
        currentSize.width <= 70.dp && currentSize.height <= 70.dp -> WidgetLayout.IconOnly
        currentSize.height <= 70.dp -> WidgetLayout.Compact
        currentSize.width >= 220.dp || currentSize.height >= 180.dp -> WidgetLayout.Large
        else -> WidgetLayout.Medium
    }

    when (widgetLayout) {
        WidgetLayout.IconOnly -> QuickAddOnlyWidget()
        WidgetLayout.Compact -> CompactQuickAddWidget()
        WidgetLayout.Medium -> SummaryWidget(thoughts = thoughts, isLarge = false)
        WidgetLayout.Large -> SummaryWidget(thoughts = thoughts, isLarge = true)
    }
}

@Composable
private fun QuickAddOnlyWidget() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF4F6F8)))
            .clickable(actionStartActivity<QuickAddActivity>())
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF111827))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            )
        }
    }
}

@Composable
private fun CompactQuickAddWidget() {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF4F6F8)))
            .clickable(actionStartActivity<QuickAddActivity>())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.mipmap.ic_launcher),
            contentDescription = "BrainClean logo"
        )

        Spacer(modifier = GlanceModifier.width(10.dp))

        Text(
            text = "+ Add",
            style = TextStyle(
                color = ColorProvider(Color(0xFF111827)),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun SummaryWidget(
    thoughts: List<Thought>,
    isLarge: Boolean
) {
    val displayedThoughts = thoughts.take(3)
    val contentPadding = if (isLarge) 16.dp else 12.dp
    val thoughtCardPadding = if (isLarge) 12.dp else 10.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF4F6F8)))
            .clickable(actionStartActivity<MainActivity>())
            .padding(contentPadding)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = "BrainClean logo"
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = "Inbox",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF111827)),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLarge) 18.sp else 16.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF111827)))
                .clickable(actionStartActivity<QuickAddActivity>())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "+ Add",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFFFFF)),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        if (displayedThoughts.isEmpty()) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xFFFFFFFF)))
                    .padding(thoughtCardPadding)
            ) {
                Text(
                    text = "Inbox is clear",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF111827)),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = "Tap Add to capture a thought.",
                    maxLines = if (isLarge) 3 else 2,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6B7280)),
                        fontSize = 12.sp
                    )
                )
            }
        } else {
            displayedThoughts.forEachIndexed { index, thought ->
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xFFFFFFFF)))
                        .padding(thoughtCardPadding)
                ) {
                    Text(
                        text = thought.content,
                        maxLines = if (isLarge) 2 else 1,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF1F2937)),
                            fontSize = 14.sp
                        )
                    )
                }

                if (index < displayedThoughts.lastIndex) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

private enum class WidgetLayout {
    IconOnly,
    Compact,
    Medium,
    Large
}
