package app.xrtm8.athar.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.xrtm8.athar.project.AtharProjectManager
import app.xrtm8.athar.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Settings & About Dialog:
 * - Cache management (storage size calculation and one-tap cleanup)
 * - Open Font Licenses (OFL) transparency
 * - Version & App Information
 * - Share App intent
 */
@Composable
fun AtharSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current
    val coroutineScope = rememberCoroutineScope()

    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        cacheSizeBytes = AtharProjectManager.getCacheSizeBytes(context)
    }

    val dialogBg = if (isAmoledWhite) AmoledWhite else GlassBackground
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val subtleText = if (isAmoledWhite) DarkTextSubtle else SubtleGray

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background((if (isAmoledWhite) AmoledWhite else AmoledBlack).copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(dialogBg)
                    .border(1.dp, if (isAmoledWhite) WhiteBorder else GlassBorderActive, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "الإعدادات والمعلومات",
                            color = CherryAccent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = primaryText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Cache Management
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(cardBg).border(1.dp, cardBorder, RoundedCornerShape(16.dp)).padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "الذاكرة المؤقتة", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        val mb = cacheSizeBytes / (1024f * 1024f)
                        Text(text = String.format("%.1f ميجابايت", mb), color = subtleText, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تنظيف الملفات المؤقتة الناتجة عن عمليات التصدير والمشاركات السابقة لتوفير مساحة التخزين.",
                        color = subtleText, fontSize = 11.5.sp, lineHeight = 16.sp
                    )

                    if (cacheMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = cacheMessage ?: "", color = Color(0xFF10B981), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (!isClearingCache) {
                                coroutineScope.launch {
                                    isClearingCache = true
                                    val freed = AtharProjectManager.clearAppCache(context)
                                    cacheSizeBytes = AtharProjectManager.getCacheSizeBytes(context)
                                    val freedMb = freed / (1024f * 1024f)
                                    cacheMessage = "تم إخلاء ${String.format("%.1f", freedMb)} ميجابايت بنجاح"
                                    isClearingCache = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isClearingCache
                    ) {
                        Text(text = if (isClearingCache) "جارٍ التنظيف..." else "تنظيف الذاكرة المؤقتة الآن", color = primaryText, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Font Licenses
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تراخيص الخطوط العربية", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "كافة الخطوط العربية المضمنة في أثر (أميري، عارف رقعة، ريم كوفي، تجوال، كايرو، مروة وغيرها) مرخصة برخصة الخطوط المفتوحة الدولية SIL Open Font License 1.1، وهي مجانية ومتاحة للاستخدام الشخصي والتجاري بالكامل.",
                        color = subtleText,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: App Info & Share
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "أثر (Athar)", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "الإصدار ${app.xrtm8.athar.BuildConfig.VERSION_NAME}", color = CherryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "استوديو التايبوغرافي وتصميم النصوص العربية الأصيلة بتقنيات المد والكشيدة الذكية ومعالجة الطبقات الرقمية.",
                        color = subtleText, fontSize = 11.5.sp, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "تطبيق أثر للتايبوغرافي العربي")
                                putExtra(Intent.EXTRA_TEXT, "أثر - استوديو التايبوغرافي وتصميم النصوص العربية\nhttps://github.com/mjd-20/ATHAR")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة التطبيق"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CherryAccent)
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "مشاركة التطبيق مع الأصدقاء", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
