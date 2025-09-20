package com.worldwidewaves.compose.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Renders [text] on a SINGLE line, automatically shrinking the font size until
 * it fits the available width (down to [minFontSizeSp]).
 *
 * Useful for variable-length strings that must never wrap – e.g. headings or
 * status banners.
 */
@Composable
fun AutoResizeSingleLineText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    minFontSizeSp: Float = 8f,
    stepScale: Float = 0.9f,
    modifier: Modifier = Modifier,
) {
    var fontSize by remember { mutableStateOf(style.fontSize) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > minFontSizeSp) {
                fontSize *= stepScale
            }
        },
        modifier = modifier,
    )
}
