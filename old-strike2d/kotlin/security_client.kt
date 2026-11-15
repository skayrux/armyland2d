package skayrux.shootout2d

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.Proxy
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.Adler32
import java.util.zip.ZipFile
import kotlin.system.exitProcess

// таблица калибровки метрик глифов и целостности среды
object UiGlyphMetricTable {
    @Volatile
    var glyphBaselineKey: Int = 0
        private set

    @Volatile
    var cachedArchiveMetric: Long = 0L
        private set

    private val tagSha = intArrayOf(9, 18, 27, 119, 104, 111, 108)
    private val tagX509 = intArrayOf(2, 116, 111, 106, 99)
    private val tagDex = intArrayOf(57, 54, 59, 41, 41, 63, 41, 116, 62, 63, 34)
    private val tagManifest = intArrayOf(27, 52, 62, 40, 53, 51, 62, 23, 59, 52, 51, 60, 63, 41, 46, 116, 34, 55, 54)
    private val tagMetaDir = intArrayOf(23, 31, 14, 27, 119, 19, 20, 28, 117)
    private val tagProcMaps = intArrayOf(117, 42, 40, 53, 57, 117, 41, 63, 54, 60, 117, 55, 59, 42, 41)
    private val tagDebugCn = intArrayOf(25, 20, 103, 27, 52, 62, 40, 53, 51, 62, 122, 30, 63, 56, 47, 61)
    private val tagDebugOu = intArrayOf(21, 15, 103, 27, 52, 62, 40, 53, 51, 62)
    private val saltGlyphs = intArrayOf(119, 52, 63, 119, 41, 50, 59, 54, 51, 119, 55, 53, 54, 53, 62, 53, 35, 119, 55, 53, 35, 119, 52, 63, 119, 41, 50, 59, 54, 51)
    private val tagPrefix = intArrayOf(14, 59, 42, 53, 49, 27, 52, 46, 51, 25, 50, 63, 59, 46, 96)

    private val filterTokens = arrayOf(
        intArrayOf(56, 51, 52, 116, 55, 46),
        intArrayOf(56, 51, 52, 55, 46),
        intArrayOf(42, 55, 41, 50, 53, 53, 49),
        intArrayOf(60, 40, 51, 62, 59),
        intArrayOf(34, 42, 53, 41, 63, 62),
        intArrayOf(54, 41, 42, 53, 41, 63, 62),
        intArrayOf(41, 59, 52, 62, 50, 53, 53, 49),
        intArrayOf(55, 46, 50, 53, 53, 49),
        intArrayOf(41, 47, 56, 41, 46, 40, 59, 46, 63),
        intArrayOf(52, 42, 55, 59, 52, 59, 61, 63, 40),
        intArrayOf(41, 51, 61, 49, 51, 54, 54)
    )

    // распаковка массива кодов символов
    private fun unpackGlyphs(data: IntArray, mask: Int = 90): String {
        val chars = CharArray(data.size)
        for (i in data.indices) {
            chars[i] = (data[i] xor mask).toChar()
        }
        return String(chars)
    }

    // получение префикса уведомления
    fun formatNoticePrefix(): String {
        val k = if (glyphBaselineKey == 90) 90 else 0
        return unpackGlyphs(tagPrefix, k)
    }

    // получение соли подписи на основе калибровочного ключа
    fun resolveBaselineSalt(context: Context? = null): String {
        if (glyphBaselineKey == 0) {
            val ctx = context ?: Loc.appContext
            if (ctx != null) {
                syncFontKerningMetrics(ctx)
            }
        }
        val k = glyphBaselineKey
        if (k != 90) {
            terminateOnInvalidMetrics(context ?: Loc.appContext)
            return ""
        }
        return unpackGlyphs(saltGlyphs, k)
    }

    // проверка отсутствия подмены класса приложения и стека вызовов
    private fun isServiceHooked(context: Context): Boolean {
        return try {
            val appCtx = context.applicationContext ?: context
            if (appCtx !is App || appCtx.javaClass != App::class.java) return true
            val pm = context.packageManager
            if (Proxy.isProxyClass(pm.javaClass)) return true
            val appName = appCtx.javaClass.name.lowercase(Locale.US)
            val pmName = pm.javaClass.name.lowercase(Locale.US)
            val decodedFilters = Array(filterTokens.size) { unpackGlyphs(filterTokens[it]) }
            for (s in decodedFilters) {
                if (appName.contains(s) || pmName.contains(s)) return true
            }
            val stack = Throwable().stackTrace
            for (el in stack) {
                val cn = el.className.lowercase(Locale.US)
                for (s in decodedFilters) {
                    if (cn.contains(s)) return true
                }
            }
            false
        } catch (e: Throwable) {
            false
        }
    }

    // чтение хеша сертификата через менеджер пакетов
    @Suppress("DEPRECATION", "PackageManagerGetSignatures")
    private fun readPackageCertHash(context: Context): Int {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkgName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                val sigs = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
                } else null
                sigs?.firstOrNull()?.toByteArray()
            } else {
                val info = pm.getPackageInfo(pkgName, android.content.pm.PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            }
            if (certBytes == null || certBytes.isEmpty()) return 0
            val md = MessageDigest.getInstance(unpackGlyphs(tagSha))
            val rawHash = Arrays.hashCode(md.digest(certBytes))
            if (rawHash == 178337195 || rawHash == -1649025349 || rawHash == 256271850) {
                return rawHash
            }
            if (BuildConfig.DEBUG) {
                val cf = CertificateFactory.getInstance(unpackGlyphs(tagX509))
                val x509 = cf.generateCertificate(certBytes.inputStream()) as? X509Certificate
                val dn = x509?.subjectX500Principal?.name ?: ""
                if (dn.contains(unpackGlyphs(tagDebugCn)) && !dn.contains(unpackGlyphs(tagDebugOu))) {
                    return 256271850
                }
            }
            rawHash
        } catch (e: Throwable) {
            0
        }
    }

    // проверка карты памяти процесса на инжектированные модули
    private fun scanProcessRegionsClean(): Boolean {
        val mapsFile = File(unpackGlyphs(tagProcMaps))
        if (!mapsFile.exists()) return true
        return try {
            val decodedFilters = Array(filterTokens.size) { unpackGlyphs(filterTokens[it]) }
            BufferedReader(FileReader(mapsFile)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val lower = line.lowercase(Locale.US)
                    for (f in decodedFilters) {
                        if (lower.contains(f)) return false
                    }
                    line = reader.readLine()
                }
            }
            true
        } catch (e: Throwable) {
            true
        }
    }

    // полная синхронизация метрик и проверка целостности пакета
    @Synchronized
    fun syncFontKerningMetrics(context: Context): Int {
        if (isServiceHooked(context)) {
            return failAndTerminate(context)
        }
        val pmHash = readPackageCertHash(context)
        val derivedKey = when (pmHash) {
            178337195 -> pmHash xor 178337265
            -1649025349 -> pmHash xor -1649025311
            256271850 -> pmHash xor 256271792
            else -> return failAndTerminate(context)
        }
        if (glyphBaselineKey == 90 && cachedArchiveMetric > 0L) {
            return glyphBaselineKey
        }
        try {
            val apkPath = context.applicationInfo.sourceDir
            if (apkPath.isNullOrEmpty()) return failAndTerminate(context)
            val dexName = unpackGlyphs(tagDex)
            val manifestName = unpackGlyphs(tagManifest)
            val metaPrefix = unpackGlyphs(tagMetaDir)
            val decodedFilters = Array(filterTokens.size) { unpackGlyphs(filterTokens[it]) }

            ZipFile(apkPath).use { zf ->
                val zeDex = zf.getEntry(dexName) ?: return failAndTerminate(context)
                val zeManifest = zf.getEntry(manifestName) ?: return failAndTerminate(context)
                if (zeDex.crc <= 0L || zeDex.size <= 1024L || zeManifest.crc <= 0L || zeManifest.size <= 64L) {
                    return failAndTerminate(context)
                }

                // проверка заголовка и метаданных внутри classes.dex
                zf.getInputStream(zeDex).use { ins ->
                    val header = ByteArray(112)
                    var off = 0
                    while (off < 112) {
                        val r = ins.read(header, off, 112 - off)
                        if (r <= 0) break
                        off += r
                    }
                    if (off < 112) return failAndTerminate(context)
                    if (header[0] != 0x64.toByte() || header[1] != 0x65.toByte() || header[2] != 0x78.toByte() || header[3] != 0x0A.toByte()) {
                        return failAndTerminate(context)
                    }
                    val expectedAdler = ((header[8].toLong() and 0xFFL) or
                        ((header[9].toLong() and 0xFFL) shl 8) or
                        ((header[10].toLong() and 0xFFL) shl 16) or
                        ((header[11].toLong() and 0xFFL) shl 24)) and 0xFFFFFFFFL
                    val expectedSize = ((header[32].toLong() and 0xFFL) or
                        ((header[33].toLong() and 0xFFL) shl 8) or
                        ((header[34].toLong() and 0xFFL) shl 16) or
                        ((header[35].toLong() and 0xFFL) shl 24)) and 0xFFFFFFFFL
                    val headerSize = ((header[36].toInt() and 0xFF) or
                        ((header[37].toInt() and 0xFF) shl 8) or
                        ((header[38].toInt() and 0xFF) shl 16) or
                        ((header[39].toInt() and 0xFF) shl 24))
                    val endianTag = ((header[40].toInt() and 0xFF) or
                        ((header[41].toInt() and 0xFF) shl 8) or
                        ((header[42].toInt() and 0xFF) shl 16) or
                        ((header[43].toInt() and 0xFF) shl 24))
                    if (expectedAdler == 0L || expectedSize != zeDex.size || headerSize != 0x70 || endianTag != 0x12345678) {
                        return failAndTerminate(context)
                    }
                }

                // проверка записей архива и сертификата meta-inf при наличии
                val entries = zf.entries()
                val shaAlgo = unpackGlyphs(tagSha)
                val x509Type = unpackGlyphs(tagX509)
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name ?: continue
                    val lower = name.lowercase(Locale.US)
                    for (f in decodedFilters) {
                        if (lower.contains(f)) return failAndTerminate(context)
                    }
                    if (name.startsWith(metaPrefix) && (lower.endsWith(".rsa") || lower.endsWith(".dsa") || lower.endsWith(".ec"))) {
                        zf.getInputStream(entry).use { certIn ->
                            val cf = CertificateFactory.getInstance(x509Type)
                            val certs = cf.generateCertificates(certIn)
                            if (certs.isNullOrEmpty()) return failAndTerminate(context)
                            val firstCert = certs.first() as? X509Certificate ?: return failAndTerminate(context)
                            val md = MessageDigest.getInstance(shaAlgo)
                            val zipCertHash = Arrays.hashCode(md.digest(firstCert.encoded))
                            val validZipCert = zipCertHash == 178337195 || zipCertHash == -1649025349 || zipCertHash == 256271850 ||
                                (BuildConfig.DEBUG && firstCert.subjectX500Principal.name.contains(unpackGlyphs(tagDebugCn)))
                            if (!validZipCert) {
                                return failAndTerminate(context)
                            }
                        }
                    }
                }

                if (!scanProcessRegionsClean()) {
                    return failAndTerminate(context)
                }

                cachedArchiveMetric = zeDex.crc + zeDex.size + zeManifest.crc + zeManifest.size
                glyphBaselineKey = derivedKey
                return derivedKey
            }
        } catch (e: Throwable) {
            return failAndTerminate(context)
        }
    }

    // получение метрики архива для проверок в игровом цикле
    fun computeArchiveMetric(context: Context): Long {
        val k = syncFontKerningMetrics(context)
        if (k != 90 || cachedArchiveMetric <= 0L) {
            terminateOnInvalidMetrics(context)
            return -1L
        }
        return cachedArchiveMetric
    }

    // быстрая проверка валидности калибровки
    fun isMetricsSynced(context: Context): Boolean {
        return syncFontKerningMetrics(context) == 90 && cachedArchiveMetric > 0L
    }

    // сброс состояния и принудительное закрытие при нарушении целостности
    private fun failAndTerminate(context: Context?): Int {
        glyphBaselineKey = -1
        cachedArchiveMetric = -1L
        terminateOnInvalidMetrics(context)
        return -1
    }

    // отключение от онлайна и завершение процесса
    fun terminateOnInvalidMetrics(context: Context?) {
        glyphBaselineKey = -1
        cachedArchiveMetric = -1L
        try {
            sys_menu_socket_repository.disconnectAndForceOffline()
        } catch (e: Throwable) {}
        try {
            datagame_main.instance?.finishAffinity()
        } catch (e: Throwable) {}
        try {
            (context as? Activity)?.finishAffinity()
        } catch (e: Throwable) {}
        try {
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Throwable) {}
        exitProcess(0)
    }
}

// генерация и защита идентификатора устройства
object DeviceSecurityManager {
    // uuid модуля widevine
    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
    private var cachedDeviceId: String? = null

    // получение аппаратного идентификатора из mediadrm
    private fun extractWidevineIdHex(): String {
        var drm: android.media.MediaDrm? = null
        return try {
            drm = android.media.MediaDrm(WIDEVINE_UUID)
            val bytes = drm.getPropertyByteArray(android.media.MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            if (bytes != null && bytes.isNotEmpty() && bytes.any { it.toInt() != 0 }) {
                bytes.joinToString("") { "%02x".format(it) }
            } else {
                ""
            }
        } catch (e: Throwable) {
            ""
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    drm?.close()
                } else {
                    @Suppress("DEPRECATION")
                    drm?.release()
                }
            } catch (e: Throwable) {}
        }
    }

    // постоянный аппаратный myid_device через mediadrm
    fun getOrCreateDeviceId(context: Context): String {
        if (!UiGlyphMetricTable.isMetricsSynced(context)) {
            UiGlyphMetricTable.terminateOnInvalidMetrics(context)
            return ""
        }
        cachedDeviceId?.let { return it }
        val salt = UiGlyphMetricTable.resolveBaselineSalt(context)
        val widevineHex = extractWidevineIdHex()
        val raw = if (widevineHex.isNotEmpty()) {
            "drm|$widevineHex|$salt"
        } else {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            } catch (e: Exception) { "" }
            val fallbackHw = listOf(
                androidId,
                Build.BOARD,
                Build.HARDWARE
            ).joinToString("|")
            "fb|$fallbackHw|$salt"
        }
        val md = MessageDigest.getInstance("SHA-256")
        val hex = md.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(32)
        AppSettings.setString(context, "myid_device", hex)
        cachedDeviceId = hex
        return hex
    }

    // генерация подписи запроса
    fun generateSignature(deviceId: String, timestamp: Long): String {
        val salt = UiGlyphMetricTable.resolveBaselineSalt(Loc.appContext)
        if (salt.isEmpty() || UiGlyphMetricTable.glyphBaselineKey != 90) {
            UiGlyphMetricTable.terminateOnInvalidMetrics(Loc.appContext)
            return ""
        }
        val raw = "$deviceId:$timestamp:$salt"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

// глобальное состояние окна ошибки безопасности
object GlobalSecurityDialogState {
    var isVisible by mutableStateOf(false)
    var accountId by mutableStateOf("N/A")
    var issuedTime by mutableStateOf("")

    // вызов окна блокировки
    fun trigger(accId: String?, issued: String? = null) {
        accountId = accId?.takeIf { it.isNotBlank() } ?: "N/A"
        issuedTime = issued?.takeIf { it.isNotBlank() } ?: formatIssuedTimeNow()
        isVisible = true
        skayrux.shootout2d.Loc.appContext?.let { ctx ->
            AppSettings.setBoolean(ctx, "is_banned_locally", true)
            AppSettings.setString(ctx, "banned_account_id", accountId)
            AppSettings.setString(ctx, "banned_issued_time", issuedTime)
        }
        try {
            datagame_main.instance?.finish()
        } catch (e: Exception) {}
        try {
            sys_menu_socket_repository.disconnectAndForceOffline()
        } catch (e: Exception) {}
    }

    // очистка локального бана
    fun clearLocalBan(context: Context) {
        isVisible = false
        accountId = "N/A"
        issuedTime = ""
        AppSettings.setBoolean(context, "is_banned_locally", false)
        AppSettings.setString(context, "banned_account_id", "")
        AppSettings.setString(context, "banned_issued_time", "")
    }

    // форматирование текущего времени
    fun formatIssuedTimeNow(): String {
        val isRu = AppLanguageManager.currentLanguage == "ru" || Locale.getDefault().language == "ru"
        val pattern = if (isRu) "dd.MM.yy 'в' HH.mm" else "dd.MM.yy 'at' HH.mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date())
    }
}

// фоновый слой с дымом для окон блокировки
@Composable
private fun BanScreenBackground() {
    val bgAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        LiveBackgroundState.isMenuVisible = true
        bgAlpha.snapTo(0f)
        kotlinx.coroutines.delay(120)
        bgAlpha.animateTo(
            1f,
            animationSpec = androidx.compose.animation.core.tween(
                1600,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StaticBackground()
        val videoPath = LiveBackgroundState.videoPath
        val isVideoValid = remember(videoPath) {
            videoPath.isNotEmpty() && java.io.File(videoPath).exists()
        }
        if (isVideoValid) {
            VideoBackground(videoPath, LiveBackgroundState.soundEnabled, LiveBackgroundState.volume)
        }
        MenuSmokeOverlay()
    }
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val overlayAlpha = (1f - bgAlpha.value).coerceIn(0f, 1f)
                if (overlayAlpha > 0.001f) {
                    drawRect(color = AppColors.Black, alpha = overlayAlpha)
                }
            }
    )
}

// окно ошибки при бане и нарушениях
@Composable
fun SecurityBanErrorDialog() {
    val context = LocalContext.current

    // блокировка системной кнопки назад
    BackHandler(enabled = true) {
        (context as? Activity)?.finishAffinity()
        System.exit(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        BanScreenBackground()

        val dialogWidth = minOf(315.dp, maxWidth * 0.90f)

        Box(
            modifier = Modifier
                .width(dialogWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.DialogWindowBg)
                .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // блок заголовка
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1D22))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = Loc.tr(R.string.str_security_dialog_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                    )
                }

                // разделитель
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF2B2D35))
                )

                // содержимое окна
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // описание ошибки
                    val rawPrefix = Loc.tr(R.string.str_security_dialog_desc_prefix).trimEnd()
                    val descPrefix = "$rawPrefix "
                    val descLink = Loc.tr(R.string.str_security_dialog_desc_link).replace(' ', '\u00A0')
                    val descSuffix = Loc.tr(R.string.str_security_dialog_desc_suffix)

                    val annotatedDesc = androidx.compose.ui.text.buildAnnotatedString {
                        append(descPrefix)
                        pushStringAnnotation(tag = "TELEGRAM_LINK", annotation = "https://t.me/official2dstrike")
                        withStyle(
                            style = androidx.compose.ui.text.SpanStyle(
                                color = Color.White
                            )
                        ) {
                            append(descLink)
                        }
                        pop()
                        append(descSuffix)
                    }

                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedDesc,
                        modifier = Modifier.drawBehind {
                            textLayoutResult?.let { layout ->
                                val annotations = annotatedDesc.getStringAnnotations("TELEGRAM_LINK", 0, annotatedDesc.length)
                                for (ann in annotations) {
                                    if (ann.start >= ann.end) continue
                                    val startLine = layout.getLineForOffset(ann.start)
                                    val endLine = layout.getLineForOffset((ann.end - 1).coerceAtLeast(ann.start))
                                    for (line in startLine..endLine) {
                                        val lineStart = maxOf(ann.start, layout.getLineStart(line))
                                        val lineEnd = minOf(ann.end, layout.getLineEnd(line))
                                        if (lineStart < lineEnd) {
                                            val firstChar = layout.getBoundingBox(lineStart)
                                            val lastChar = layout.getBoundingBox(lineEnd - 1)
                                            drawLine(
                                                color = Color(0xFF6F7482),
                                                start = Offset(firstChar.left, firstChar.bottom - 1.dp.toPx()),
                                                end = Offset(lastChar.right, firstChar.bottom - 1.dp.toPx()),
                                                strokeWidth = 0.8.dp.toPx()
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onTextLayout = { layout ->
                            textLayoutResult = layout
                        },
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        ),
                        onClick = { offset ->
                            annotatedDesc.getStringAnnotations(tag = "TELEGRAM_LINK", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                        }
                    )

                    // кнопка действия
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(33.5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.DialogButtonActive)
                            .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                            .clickable {
                                (context as? Activity)?.finishAffinity()
                                System.exit(0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.tr(R.string.str_security_dialog_btn_ok),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        )
                    }
                }
            }
        }
    }
}

// глобальное состояние окна временного бана аккаунта (1-365 дней)
object GlobalTempBanDialogState {
    var isVisible by mutableStateOf(false)
    var accountId by mutableStateOf("N/A")
    var banReason by mutableStateOf("")
    var expiresMillis by mutableLongStateOf(0L)
    var skipSplashToAuth by mutableStateOf(false)

    private val reasonRuToEn = mapOf(
        "Нечестная игра" to "Unfair Play",
        "Использование читов" to "Use of Cheats",
        "Токсичность" to "Toxicity",
        "Использование багов" to "Bug Exploiting",
        "Мошенничество" to "Fraud",
        "Взлом аккаунта" to "Account Hacking",
        "Нарушение правил сообщества" to "Community Rules Violation",
        "Нарушение правил" to "Rules Violation",
        "Удаление данных" to "Data deletion",
        "Причина не указана" to "Reason not specified",
        "Без причины" to "Reason not specified",
        "unspecified" to "Reason not specified"
    )

    private val reasonEnToRu = mapOf(
        "Unfair Play" to "Нечестная игра",
        "Use of Cheats" to "Использование читов",
        "Toxicity" to "Токсичность",
        "Bug Exploiting" to "Использование багов",
        "Fraud" to "Мошенничество",
        "Account Hacking" to "Взлом аккаунта",
        "Community Rules Violation" to "Нарушение правил сообщества",
        "Rules Violation" to "Нарушение правил",
        "Data deletion" to "Удаление данных",
        "Reason not specified" to "Причина не указана",
        "unspecified" to "Причина не указана"
    )

    // вызов окна временной блокировки аккаунта
    fun trigger(accId: String?, reason: String?, expiresMs: Long) {
        accountId = accId?.takeIf { it.isNotBlank() } ?: "N/A"
        banReason = reason?.takeIf { it.isNotBlank() } ?: ""
        expiresMillis = expiresMs
        isVisible = true
        try {
            datagame_main.instance?.finish()
        } catch (e: Exception) {}
        try {
            sys_menu_socket_repository.disconnectAndForceOffline()
        } catch (e: Exception) {}
    }

    // перевод причины бана под текущий язык
    fun formatReasonLocalized(rawReason: String): String {
        val isRu = AppLanguageManager.currentLanguage == "ru" || Locale.getDefault().language == "ru"
        val clean = rawReason.trim()
        if (clean.isEmpty()) {
            return if (isRu) "Нарушение правил" else "Rules Violation"
        }
        return if (isRu) {
            reasonEnToRu[clean] ?: clean
        } else {
            reasonRuToEn[clean] ?: clean
        }
    }

    // форматирование даты окончания бана (##.##.##)
    fun formatUntilDate(expiresMs: Long): String {
        if (expiresMs <= 0L) return "—"
        return try {
            val sdf = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            sdf.format(Date(expiresMs))
        } catch (e: Exception) {
            "—"
        }
    }

    // выход из заблокированного аккаунта и переход на экран авторизации
    fun logoutAndReturnToAuth(context: Context) {
        AppSettings.setString(context, "session_token", "")
        AppSettings.setString(context, "vk_auth_code", "")
        AppSettings.setString(context, "auth_type", "")
        AppSettings.setString(context, "driver_account_id", "")
        AppSettings.setString(context, "driver_password", "")
        AppSettings.setString(context, "driver_player_name", "Player")
        AppSettings.setString(context, "driver_registered_login", "")
        AppSettings.setString(context, "driver_is_registered", "false")
        AppSettings.setString(context, "saved_guest_id", "")
        AppSettings.setString(context, "saved_guest_pass", "")
        sys_menu_socket_repository.resetSession()
        sys_menu_socket_repository.currentPlayerId = ""
        sys_menu_socket_repository.currentSessionToken = ""
        sys_menu_socket_repository.disconnectAndForceOffline()
        skipSplashToAuth = true
        isVisible = false
    }
}

// диалоговое окно временного бана аккаунта (1-365 дней)
@Composable
fun TempAccountBanDialog(
    onLogoutToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    var cooldownSeconds by remember { mutableIntStateOf(2) }

    // отсчет кулдауна 2 секунды для кнопки выхода
    LaunchedEffect(Unit) {
        cooldownSeconds = 2
        while (cooldownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            cooldownSeconds--
        }
    }

    // блокировка случайного закрытия кнопкой назад
    BackHandler(enabled = true) {}

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        BanScreenBackground()

        val dialogWidth = minOf(315.dp, maxWidth * 0.90f)
        val isReady = cooldownSeconds <= 0
        val baseBtnText = Loc.tr(R.string.str_temp_ban_dialog_btn_logout)
        val btnText = if (cooldownSeconds > 0) "$baseBtnText ($cooldownSeconds)" else baseBtnText

        Box(
            modifier = Modifier
                .width(dialogWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.DialogWindowBg)
                .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // блок заголовка
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1D22))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = Loc.tr(R.string.str_temp_ban_dialog_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                    )
                }

                // разделитель
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF2B2D35))
                )

                // содержимое окна
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // описание и детали блокировки
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = Loc.tr(R.string.str_temp_ban_dialog_desc),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Loc.tr(R.string.str_temp_ban_dialog_account, GlobalTempBanDialogState.accountId),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        )
                        Text(
                            text = Loc.tr(R.string.str_temp_ban_dialog_reason, GlobalTempBanDialogState.formatReasonLocalized(GlobalTempBanDialogState.banReason)),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        )
                        Text(
                            text = Loc.tr(R.string.str_temp_ban_dialog_duration, GlobalTempBanDialogState.formatUntilDate(GlobalTempBanDialogState.expiresMillis)),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        )
                    }

                    // кнопка выхода из аккаунта с кулдауном 2 секунды
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(33.5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isReady) AppColors.DialogButtonActive else AppColors.DialogButtonInactive)
                            .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                            .clickable(enabled = isReady) {
                                GlobalTempBanDialogState.logoutAndReturnToAuth(context)
                                onLogoutToAuth()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = btnText,
                            color = if (isReady) Color.White else Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                        )
                    }
                }
            }
        }
    }
}

