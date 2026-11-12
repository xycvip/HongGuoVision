package com.hongguo.android

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextUnit

/**
 * Apple TV layout measurements translated from the reference tvOS layout system.
 * Values are proportions of the available 1080p canvas, not phone breakpoints.
 */
@Composable
internal fun appleTVDp(points: Float): Dp =
    (points * androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp / 1080f).dp

@Composable
internal fun appleTVSp(points: Float): TextUnit =
    (points * androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp / 1080f).sp

internal object AppleTVLayout {
    const val NAV_VH = 0.085f
    const val FILTER_VH = 0.065f
    const val CARD_VH = 0.70f
    const val STATUS_VH = 0.045f
    const val GAP_VH = 0.02f

    fun height(vh: Float, canvasHeight: Dp): Dp = canvasHeight * vh
    const val FOCUS_SCALE = 1.05f
}

internal object AppleTVColors {
    val background = Color.Black
    val normalFill = Color.White.copy(alpha = .08f)
    val normalText = Color.White.copy(alpha = .6f)
    val focusedFill = Color.White.copy(alpha = .20f)
    val focusedText = Color.White
    val selectedFill = Color.White.copy(alpha = .88f)
    val selectedText = Color(0xFF171717)
    val accent = Color(0xFF0A84FF)
    val focusShadow = Color.Black.copy(alpha = .5f)
    val secondary = Color.White.copy(alpha = .6f)
}

@Composable
internal fun AppleTVControl(
    title: String,
    selected: Boolean = false,
    icon: ImageVector? = null,
    paddingH: Dp = 14.dp,
    paddingV: Dp = 7.dp,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) AppleTVLayout.FOCUS_SCALE else 1f,
        animationSpec = tween(150),
        label = "apple_tv_control_scale",
    )
    val fill = when {
        focused -> AppleTVColors.focusedFill
        selected -> AppleTVColors.selectedFill
        else -> AppleTVColors.normalFill
    }
    val foreground = when {
        focused -> AppleTVColors.focusedText
        selected -> AppleTVColors.selectedText
        else -> AppleTVColors.normalText
    }
    Row(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayerScale(scale)
            .shadow(if (focused) appleTVDp(12f) else 0.dp, CircleShape,
                ambientColor = AppleTVColors.focusShadow, spotColor = AppleTVColors.focusShadow)
            .clip(CircleShape)
            .background(fill)
            .then(if (focused) Modifier.border(appleTVDp(2f), AppleTVColors.accent, CircleShape) else Modifier)
            .alpha(if (enabled) 1f else .4f)
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled)
            .padding(horizontal = appleTVDp(paddingH.value), vertical = appleTVDp(paddingV.value)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(appleTVDp(8f)),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.width(appleTVDp(18f)), tint = foreground)
        Text(title, color = foreground, fontSize = appleTVSp(16f), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
internal fun AppleTVTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) AppleTVLayout.FOCUS_SCALE else 1f,
        animationSpec = tween(150),
        label = "apple_tv_tab_scale",
    )
    val fill = when {
        focused -> AppleTVColors.focusedFill
        selected -> AppleTVColors.selectedFill
        else -> AppleTVColors.normalFill
    }
    val foreground = when {
        focused -> AppleTVColors.focusedText
        selected -> AppleTVColors.selectedText
        else -> AppleTVColors.normalText
    }
    Row(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayerScale(scale)
            .shadow(if (focused) appleTVDp(12f) else 0.dp, CircleShape,
                ambientColor = AppleTVColors.focusShadow, spotColor = AppleTVColors.focusShadow)
            .clip(CircleShape)
            .background(fill)
            .then(if (focused) Modifier.border(appleTVDp(2f), AppleTVColors.accent, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .focusable()
            .widthIn(min = appleTVDp(140f))
            .padding(horizontal = appleTVDp(20f), vertical = appleTVDp(14f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(appleTVDp(18f)),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.width(appleTVDp(22f)), tint = foreground)
        Text(title, color = foreground, fontSize = appleTVSp(16f), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
internal fun AppleTVPosterCard(
    card: DramaCard,
    subtitle: String = if (card.episodeCount > 0) "${card.episodeCount} 集" else "",
    rankText: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember(card.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) AppleTVLayout.FOCUS_SCALE else 1f,
        animationSpec = tween(180),
        label = "apple_tv_poster_scale",
    )
    Column(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayerScale(scale)
            .shadow(if (focused) appleTVDp(12f) else 0.dp, RoundedCornerShape(appleTVDp(16f)),
            ambientColor = AppleTVColors.focusShadow, spotColor = AppleTVColors.focusShadow)
            .clip(RoundedCornerShape(appleTVDp(16f)))
            .background(if (focused) AppleTVColors.focusedFill else Color.Transparent)
            .then(if (focused) Modifier.border(appleTVDp(2f), AppleTVColors.accent, RoundedCornerShape(appleTVDp(16f))) else Modifier)
            .clickable(onClick = onClick)
            .focusable()
            .padding(appleTVDp(8f))
            .width(coverWidthForTV()),
        verticalArrangement = Arrangement.spacedBy(appleTVDp(8f)),
    ) {
        Cover(
            card.cover,
            card.title,
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(appleTVDp(12f))),
        )
        Text(card.title, color = Color.White, fontSize = appleTVSp(18f), fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = appleTVSp(22f), modifier = Modifier.fillMaxWidth().height(appleTVDp(38f)))
        Text(subtitle, color = AppleTVColors.secondary, fontSize = appleTVSp(13f), maxLines = 1, modifier = Modifier.height(appleTVDp(18f)))
        Text(rankText, color = AppleTVColors.accent, fontSize = appleTVSp(13f), maxLines = 1, modifier = Modifier.height(appleTVDp(18f)))
    }
}

@Composable
private fun coverWidthForTV(): Dp {
    val canvasHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val coverHeight = ((AppleTVLayout.height(AppleTVLayout.CARD_VH, canvasHeight) - appleTVDp(140f)) / AppleTVLayout.FOCUS_SCALE)
        .coerceAtLeast(appleTVDp(160f))
    return coverHeight * 2f / 3f
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.graphicsLayer { scaleX = scale; scaleY = scale }
