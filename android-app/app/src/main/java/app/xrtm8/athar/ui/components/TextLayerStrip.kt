package app.xrtm8.athar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.theme.*

/**
 * Overhauled Multi-Text Layer selector strip.
 * Keeps layer chips wide and readable via LazyRow auto-scrolling while moving
 * secondary layer actions into a collapsible horizontal action bar and full Layer Manager sheet.
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
    onEditActiveLayer: (() -> Unit)? = null,
    onOpenLayersManager: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current
    var showActionsBar by remember { mutableStateOf(false) }

    val stripBg = if (isAmoledWhite) GlassBackgroundWhite else GlassBackground
    val stripBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val surfaceBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val secondaryText = if (isAmoledWhite) DarkTextMuted else MutedWhite

    val listState = rememberLazyListState()
    val activeIdx = state.layers.indexOfFirst { it.id == state.activeLayer.id }.coerceAtLeast(0)
    LaunchedEffect(activeIdx, state.layers.size) {
        if (activeIdx in state.layers.indices) {
            listState.animateScrollToItem(activeIdx)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(stripBg)
            .border(0.5.dp, stripBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Row 1: Add Button + Wide LazyRow of Layers + Tools/Manager Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CherryAccent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddLayer()
                    }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة نص", tint = PureWhite, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("نص", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(state.layers, key = { _, l -> l.id }) { index, layer ->
                    val isActive = layer.id == state.activeLayer.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .widthIn(min = 64.dp, max = 150.dp)
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
                            text = layer.rawText.trim().ifBlank { layer.name.ifBlank { "نص ${index + 1}" } },
                            color = (if (isActive) primaryText else secondaryText).copy(alpha = if (layer.isVisible) 1f else 0.55f),
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (onOpenLayersManager != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceBg)
                        .border(0.5.dp, stripBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenLayersManager()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "إدارة الطبقات", tint = CherryAccent, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(5.dp))
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showActionsBar) CherryAccent.copy(alpha = 0.22f) else surfaceBg)
                    .border(0.5.dp, if (showActionsBar) CherryAccent else stripBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showActionsBar = !showActionsBar
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showActionsBar) Icons.Default.KeyboardArrowUp else Icons.Default.MoreHoriz,
                    contentDescription = "أدوات الطبقة",
                    tint = if (showActionsBar) CherryAccent else primaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Row 2: Collapsible Horizontal Scrollable Layer Actions (Zero Squishing)
        AnimatedVisibility(
            visible = showActionsBar,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEditActiveLayer != null) {
                    LayerActionPill(Icons.Default.Edit, "تعديل", surfaceBg, stripBorder, CherryAccent, primaryText) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEditActiveLayer()
                    }
                }
                if (onToggleVisibility != null) {
                    val vis = state.activeLayer.isVisible
                    LayerActionPill(
                        icon = if (vis) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        label = if (vis) "إخفاء" else "إظهار",
                        bg = surfaceBg, border = stripBorder, iconTint = CherryAccent, textCol = primaryText
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleVisibility(state.activeLayer.id)
                    }
                }
                LayerActionPill(Icons.Default.ContentCopy, "تكرار", surfaceBg, stripBorder, CherryAccent, primaryText) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDuplicateLayer()
                }
                if (onCopyStyle != null) {
                    LayerActionPill(Icons.Default.Brush, "نسخ النمط", surfaceBg, stripBorder, CherryAccent, primaryText) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCopyStyle()
                    }
                }
                if (hasCopiedStyle && onPasteStyle != null) {
                    LayerActionPill(Icons.Default.ContentPaste, "لصق النمط", CherryAccent.copy(alpha = 0.2f), CherryAccent, CherryAccent, primaryText) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPasteStyle()
                    }
                }
                if (state.layers.size > 1) {
                    if (onMoveLayerUp != null && activeIdx < state.layers.lastIndex) {
                        LayerActionPill(Icons.Default.KeyboardArrowUp, "للأمام", surfaceBg, stripBorder, secondaryText, primaryText) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveLayerUp()
                        }
                    }
                    if (onMoveLayerDown != null && activeIdx > 0) {
                        LayerActionPill(Icons.Default.KeyboardArrowDown, "للخلف", surfaceBg, stripBorder, secondaryText, primaryText) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveLayerDown()
                        }
                    }
                    LayerActionPill(Icons.Default.DeleteOutline, "حذف", surfaceBg, stripBorder, Color(0xFFEF4444), Color(0xFFEF4444)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteLayer()
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerActionPill(
    icon: ImageVector,
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    textCol: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = textCol, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
    }
}
