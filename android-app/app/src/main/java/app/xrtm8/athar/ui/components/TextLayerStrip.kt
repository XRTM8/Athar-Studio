package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.theme.*

/**
 * Multi-Text Layer selector strip.
 * Provides instant layer switching, duplicate, delete, and "+ نص جديد" layer addition.
 */
@Composable
fun TextLayerStrip(
    state: CanvasState,
    hasCopiedStyle: Boolean = false,
    onAddLayer: () -> Unit,
    onDuplicateLayer: () -> Unit,
    onCopyStyle: (() -> Unit)? = null,
    onPasteStyle: (() -> Unit)? = null,
    onDeleteLayer: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onMoveLayerUp: (() -> Unit)? = null,
    onMoveLayerDown: (() -> Unit)? = null,
    onToggleVisibility: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current

    val stripBg = if (isAmoledWhite) GlassBackgroundWhite else GlassBackground
    val stripBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val surfaceBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val secondaryText = if (isAmoledWhite) DarkTextMuted else MutedWhite

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(stripBg)
            .border(0.5.dp, stripBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "+ نص" Add Layer Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CherryAccent)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddLayer()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "إضافة نص",
                tint = PureWhite,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "نص جديد",
                color = PureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Layers Horizontal Scroll
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.layers.forEachIndexed { index, layer ->
                val isActive = layer.id == state.activeLayer.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isActive) CherryAccent.copy(alpha = if (isAmoledWhite) 0.15f else 0.22f)
                            else surfaceBg
                        )
                        .border(
                            width = if (isActive) 1.2.dp else 0.5.dp,
                            color = if (isActive) CherryAccent else stripBorder,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectLayer(layer.id)
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when {
                            !layer.isVisible -> Icons.Default.VisibilityOff
                            layer.isLocked -> Icons.Default.Lock
                            else -> Icons.Default.TextFields
                        },
                        contentDescription = null,
                        tint = when {
                            !layer.isVisible -> secondaryText.copy(alpha = 0.55f)
                            isActive -> CherryAccent
                            else -> secondaryText
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = layer.rawText.take(10).ifBlank { layer.name.ifBlank { "نص ${index + 1}" } },
                        color = (if (isActive) primaryText else secondaryText).copy(alpha = if (layer.isVisible) 1f else 0.55f),
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Toggle Active Layer Visibility
        if (onToggleVisibility != null) {
            val activeVisible = state.activeLayer.isVisible
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceBg)
                    .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleVisibility(state.activeLayer.id)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (activeVisible) "إخفاء الطبقة" else "إظهار الطبقة",
                    tint = if (activeVisible) CherryAccent else secondaryText,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
        }

        // Duplicate Active Layer Action
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceBg)
                .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDuplicateLayer()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "تكرار الطبقة",
                tint = CherryAccent,
                modifier = Modifier.size(14.dp)
            )
        }

        // Copy Style Action
        if (onCopyStyle != null) {
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceBg)
                    .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCopyStyle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = "نسخ التنسيق",
                    tint = CherryAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Paste Style Action
        if (onPasteStyle != null && hasCopiedStyle) {
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceBg)
                    .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPasteStyle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "لصق التنسيق",
                    tint = CherryAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (state.layers.size > 1) {
            if (onMoveLayerUp != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceBg)
                        .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveLayerUp()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "تقديم الطبقة",
                        tint = CherryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (onMoveLayerDown != null) {
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceBg)
                        .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveLayerDown()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "تأخير الطبقة",
                        tint = CherryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete Active Layer Action
            Box(
                modifier = Modifier
                    .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceBg)
                .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteLayer()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف الطبقة",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
