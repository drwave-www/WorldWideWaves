package com.worldwidewaves.compose.common

import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Horizontal white divider reused across screens. */
@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.width(Dimensions.DIVIDER_WIDTH.dp),
        color = Color.White,
        thickness = Dimensions.DIVIDER_THICKNESS.dp,
    )
}
