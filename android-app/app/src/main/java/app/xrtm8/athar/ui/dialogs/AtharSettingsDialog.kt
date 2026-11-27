package app.xrtm8.athar.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Tune
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
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch
import java.util.Locale

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

    val dialogBg = if (isAmoledWhite) AmoledWhite else StudioSurface
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.84f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(dialogBg)
                    .border(0.75.dp, cardBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Sticky Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الإعدادات والمعلومات",
                        color = primaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(0.5.dp, cardBorder, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = primaryText, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = cardBorder, thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Section 1: Studio & Export Preferences (Primary user settings first)
                var appPrefs by remember { mutableStateOf(app.xrtm8.athar.project.AtharAppSettings.load(context)) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(0.75.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(18.dp))
                        Text(text = "تفضيلات الاستوديو والتصدير", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "صيغة التصدير الافتراضية", color = subtleText, fontSize = 11.5.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAmoledWhite) WhiteSurfaceElevated else AmoledBlack.copy(alpha = 0.45f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        app.xrtm8.athar.project.ExportImageFormat.entries.forEach { fmt ->
                            val sel = appPrefs.defaultFormat == fmt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) CherryAccent else Color.Transparent)
                                    .clickable {
                                        val next = appPrefs.copy(defaultFormat = fmt)
                                        appPrefs = next
                                        app.xrtm8.athar.project.AtharAppSettings.save(context, next)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fmt.label, color = if (sel) PureWhite else subtleText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = cardBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("إظهار الشبكة تلقائياً", color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("عرض خطوط المحاذاة عند فتح مشروع جديد", color = subtleText, fontSize = 10.5.sp)
                        }
                        Switch(
                            checked = appPrefs.autoGridGuides,
                            onCheckedChange = {
                                val next = appPrefs.copy(autoGridGuides = it)
                                appPrefs = next
                                app.xrtm8.athar.project.AtharAppSettings.save(context, next)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = CherryAccent)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("الحفظ التلقائي للمسودة", color = primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("حفظ التعديلات تلقائياً عند الخروج من الاستوديو", color = subtleText, fontSize = 10.5.sp)
                        }
                        Switch(
                            checked = appPrefs.autoSaveDrafts,
                            onCheckedChange = {
                                val next = appPrefs.copy(autoSaveDrafts = it)
                                appPrefs = next
                                app.xrtm8.athar.project.AtharAppSettings.save(context, next)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = CherryAccent)
                        )
                    }
                }

                // Section 2: Cache Management
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(cardBg).border(0.75.dp, cardBorder, RoundedCornerShape(16.dp)).padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "الذاكرة المؤقتة", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        val mb = cacheSizeBytes / (1024f * 1024f)
                        Text(text = String.format(Locale.US, "%.1f MB", mb), color = CherryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
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
                                    cacheMessage = "تم إخلاء ${String.format(Locale.US, "%.1f", freedMb)} ميجابايت بنجاح"
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

                // Section 3: Collapsible Font Licenses (Fixed width weight so label never wraps vertically)
                var showLicenses by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(0.75.dp, cardBorder, RoundedCornerShape(16.dp))
                        .clickable { showLicenses = !showLicenses }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تراخيص الخطوط العربية (OFL 1.1)",
                                color = primaryText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (showLicenses) "إخفاء" else "عرض",
                                color = CherryAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = if (showLicenses) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CherryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showLicenses) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "كافة الخطوط المضمنة في أثر (أميري، أميري قرآني، عارف رقعة، شهرزاد، المسيري، ريم كوفي، قاهري، كوفام، لاليزار، رقاص، كايرو، الإسكندرية، ريدكس برو، المراعي، تجوال، مرحي، ليمونادا، وجلزار) مرخصة برخصة الخطوط المفتوحة SIL Open Font License 1.1 ومتاحة للاستخدام الشخصي والتجاري. أما الخطوط المستوردة يدوياً فتخضع لترخيص ملفاتها من قِبل المستخدم.",
                                color = subtleText,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Section 4: App Info & Share
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(0.75.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "أثر (Athar)", color = primaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "الإصدار ${app.xrtm8.athar.BuildConfig.VERSION_NAME}", color = CherryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "أداة تصميم وكتابة عربية تدعم الكشيدة السياقية وتنسيق الطبقات والتصدير متعدد الصيغ.",
                        color = subtleText, fontSize = 11.5.sp, lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "تطبيق أثر للتصميم والخط العربي")
                                putExtra(Intent.EXTRA_TEXT, "تطبيق أثر للتصميم والخط العربي\n${AtharUpdateManager.REPO_URL}")
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
}
