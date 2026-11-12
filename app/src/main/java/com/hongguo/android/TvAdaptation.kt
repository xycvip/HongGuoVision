package com.hongguo.android

import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.composed
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** True only on Android TV hardware; phone and tablet layouts keep their original behavior. */
internal val LocalTvMode = staticCompositionLocalOf { false }

/** Persistent focus ring for existing clickable/selectable controls when navigating with a remote. */
internal fun Modifier.tvFocusVisual(): Modifier = composed {
    if (!LocalTvMode.current) this else {
        var focused by remember { mutableStateOf(false) }
        this.onFocusChanged { focused = it.isFocused }
            .then(if (focused) Modifier.border(2.dp, Color(0xFFFF7043), RoundedCornerShape(10.dp)) else Modifier)
    }
}
