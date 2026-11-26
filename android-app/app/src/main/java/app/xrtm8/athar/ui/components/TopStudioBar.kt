package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.theme.*

@Composable
fun TopStudioBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isLocked: Boolean,
    showGrid: Boolean,
    onOpenProjects: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLock: () -> Unit,
    onResetTransform: () -> Unit,
    onExportClick: () -> Unit,
    onTogglePreview: () -> Unit,
    onBackToHome: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current

    val barBackground = if (isAmoledWhite) GlassBackgroundWhite else GlassBackground
    val barBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val secondaryText = if (isAmoledWhite) DarkTextMuted else MutedWhite
    val subtleIcon = if (isAmoledWhite) DarkTextSubtle else SubtleGray
    val pillBackground = if (isAmoledWhite) WhiteSurface else GlassSurface
    val pillBorder = if (isAmoledWhite) WhiteBorder else GlassBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(barBackground)
            .border(width = 0.5.dp, color = barBorder, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Right: Back to Home, Brand Name & Project Switcher
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackToHome != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isAmoledWhite) WhiteSurfaceElevated else StudioSurface)
                            .border(0.5.dp, pillBorder, CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackToHome()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة للرئيسية",
                            tint = BurgundyAccent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = "أثر",
                    color = primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenProjects()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "مشاريعي",
                        tint = BurgundyAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (onToggleTheme != null) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTheme()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isAmoledWhite) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "تبديل الثيم",
                            tint = if (isAmoledWhite) DarkText else BurgundyAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center: Studio Quick Controls Pill (Undo, Redo, Preview, Grid, Lock, Reset)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(pillBackground)
                    .border(0.5.dp, pillBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onUndo() },
                    enabled = canUndo,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "تراجع", tint = if (canUndo) primaryText else subtleIcon, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRedo() },
                    enabled = canRedo,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "إعادة", tint = if (canRedo) primaryText else subtleIcon, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTogglePreview() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "معاينة التصميم", tint = BurgundyAccent, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleGrid() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff, contentDescription = "الشبكة", tint = if (showGrid) BurgundyAccent else subtleIcon, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggleLock() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = "قفل", tint = if (isLocked) BurgundyAccent else subtleIcon, modifier = Modifier.size(15.dp))
                }

                IconButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onResetTransform() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "توسيط", tint = secondaryText, modifier = Modifier.size(15.dp))
                }
            }

            // Left: High-Contrast Velvet Burgundy Export CTA
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurgundyAccent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.IosShare, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تصدير", color = PureWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
