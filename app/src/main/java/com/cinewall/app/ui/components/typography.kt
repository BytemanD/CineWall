package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class TextSize {
    Large, Medium, Small
}

@Composable
fun Content(content: String, style: TextStyle,
            modifier: Modifier = Modifier,
            fontWeight: FontWeight ? = null,
            maxLines : Int = 1, color: Color ? =  null,
            overflow: TextOverflow =  TextOverflow.Clip) {
    Text(content,
        modifier=modifier,
        color = color ?:  Color.Unspecified,
        fontWeight = fontWeight,
        overflow = overflow,
        style = style, maxLines = maxLines)
}

@Composable
fun TitleText(content: String, maxLines : Int = 1, size: TextSize = TextSize.Medium) {
    Content(content, style = when (size) {
        TextSize.Large -> MaterialTheme.typography.titleLarge
        TextSize.Medium -> MaterialTheme.typography.titleMedium
        TextSize.Small -> MaterialTheme.typography.titleSmall
    }, maxLines = maxLines)
}

@Composable
fun HeadlineText(content: String, size: TextSize = TextSize.Medium, maxLines : Int = 1) {
    Content(content, style = when (size) {
        TextSize.Large -> MaterialTheme.typography.headlineLarge
        TextSize.Medium -> MaterialTheme.typography.headlineMedium
        TextSize.Small -> MaterialTheme.typography.headlineSmall
    }, maxLines = maxLines)
}

@Composable
fun BodyText(content: String,
             modifier: Modifier = Modifier,
             fontWeight: FontWeight ? = null,
             overflow: TextOverflow ? = null,
             maxLines: Int = 1, size: TextSize = TextSize.Medium) {
    Content(
        content,
        modifier = modifier,
        fontWeight = fontWeight,
        style = when (size) {
            TextSize.Large -> MaterialTheme.typography.bodyLarge
            TextSize.Medium -> MaterialTheme.typography.bodyMedium
            TextSize.Small -> MaterialTheme.typography.bodySmall
        }, maxLines = maxLines
    )
}

@Composable
fun LabelText(content: String, modifier: Modifier = Modifier, maxLines : Int = 1,
              size: TextSize = TextSize.Medium,
              color: Color ? = null) {
    Content(content, when (size) {
        TextSize.Large -> MaterialTheme.typography.labelLarge
        TextSize.Medium -> MaterialTheme.typography.labelMedium
        TextSize.Small -> MaterialTheme.typography.labelSmall
    }, modifier=modifier,  color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines)
}

@Composable
fun Description(
    modifier: Modifier = Modifier,
    items: Map<String, String> = mutableMapOf(),
    title: String ? = null, subTitle: String ?= null
) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (title!= null)
            TitleText(title, size = TextSize.Large);
        if (subTitle!= null)
            TitleText(subTitle, size = TextSize.Small);
        if (title!= null || subTitle!= null)
            Spacer(Modifier.height(14.dp))

        items.forEach { (key, value) ->
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                BodyText(
                    key,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp)
                )
                BodyText(
                    value,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }


        }

    }
}
