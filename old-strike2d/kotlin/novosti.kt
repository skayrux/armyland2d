package skayrux.shootout2d

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image

sealed class NewsBlock {
    data class H1(val text: AnnotatedString) : NewsBlock()
    data class H2(val text: AnnotatedString) : NewsBlock()
    data class H3(val text: AnnotatedString) : NewsBlock()
    data class Bullet(val text: AnnotatedString) : NewsBlock()
    data class Quote(val text: AnnotatedString) : NewsBlock()
    data class BoxBlock(val text: AnnotatedString) : NewsBlock()
    data class Spoiler(val title: String, val text: AnnotatedString) : NewsBlock()
    data class DetailsBlock(val summary: String, val content: AnnotatedString) : NewsBlock()
    data class CodeBlock(val codeText: String) : NewsBlock()
    data class Paragraph(val text: AnnotatedString) : NewsBlock()
    data class MediaBlock(val filename: String, val width: Int, val align: String, val borderStyle: String, val isVideo: Boolean, val companionText: AnnotatedString? = null) : NewsBlock()
    object SpacerBlock : NewsBlock()
}

object NewsCacheState {
    var newsList by mutableStateOf<List<String>>(emptyList())
    var selectedNewsIndex by mutableIntStateOf(0)
    var selectedNewsContent by mutableStateOf("")
    var isLoadingList by mutableStateOf(false)
    var isLoadingContent by mutableStateOf(false)
    var isInitialized = false
}

@Composable
fun novosti_menu(onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("news_cache", android.content.Context.MODE_PRIVATE) }

    if (!NewsCacheState.isInitialized) {
        val cachedListStr = sharedPrefs.getString("news_list", "") ?: ""
        if (cachedListStr.isNotEmpty()) {
            NewsCacheState.newsList = cachedListStr.split("|")
            val currentTitle = NewsCacheState.newsList.getOrNull(NewsCacheState.selectedNewsIndex)
            if (currentTitle != null) {
                NewsCacheState.selectedNewsContent = sharedPrefs.getString("content_$currentTitle", "") ?: ""
            }
        }
        NewsCacheState.isInitialized = true
    }

    LaunchedEffect(Unit) {
        val cachedListStr = sharedPrefs.getString("news_list", "") ?: ""
        if (cachedListStr.isNotEmpty()) {
            if (NewsCacheState.newsList.isEmpty()) {
                NewsCacheState.newsList = cachedListStr.split("|")
            }
            NewsCacheState.isLoadingList = false
        } else {
            if (NewsCacheState.newsList.isEmpty()) {
                NewsCacheState.isLoadingList = true
            }
        }
        val list = sys_menu_socket_repository.getNewsListOnline()
        val numberRegex = Regex("""^\[(\d+)\]""")
        val numbered = mutableListOf<Pair<Int, String>>()
        val other = mutableListOf<String>()
        for (item in list) {
            val match = numberRegex.find(item)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null) {
                    numbered.add(Pair(num, item))
                } else {
                    other.add(item)
                }
            } else {
                other.add(item)
            }
        }
        numbered.sortBy { it.first }
        other.sort()
        val sortedList = numbered.map { it.second } + other
        if (sortedList.isNotEmpty()) {
            NewsCacheState.newsList = sortedList
            val oldPrefs = sharedPrefs.all.keys.filter { it.startsWith("content_") }
            val editor = sharedPrefs.edit()
            val validKeys = sortedList.map { "content_$it" }.toSet()
            oldPrefs.forEach { key ->
                if (key !in validKeys) {
                    editor.remove(key)
                }
            }
            editor.putString("news_list", sortedList.joinToString("|"))
            editor.apply()
        }
        NewsCacheState.isLoadingList = false

        // очистка кэшированных медиафайлов старше 7 дней
        try {
            val now = System.currentTimeMillis()
            val maxAge = 7 * 86_400_000L
            context.cacheDir.listFiles { f ->
                f.name.startsWith("news_media_") && !f.name.endsWith(".tmp") && (now - f.lastModified() > maxAge)
            }?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    LaunchedEffect(NewsCacheState.selectedNewsIndex, NewsCacheState.newsList) {
        val currentTitle = NewsCacheState.newsList.getOrNull(NewsCacheState.selectedNewsIndex)
        if (currentTitle != null) {
            val cachedContent = sharedPrefs.getString("content_$currentTitle", "") ?: ""
            if (cachedContent.isNotEmpty()) {
                NewsCacheState.selectedNewsContent = cachedContent
                NewsCacheState.isLoadingContent = false
            } else if (NewsCacheState.selectedNewsContent.isEmpty()) {
                NewsCacheState.isLoadingContent = true
            }
            val onlineContent = sys_menu_socket_repository.getNewsContentOnline(currentTitle)
            if (sys_menu_socket_repository.isServerOnline && onlineContent.isNotBlank()) {
                NewsCacheState.selectedNewsContent = onlineContent
                sharedPrefs.edit().putString("content_$currentTitle", onlineContent).apply()
            } else {
                if (cachedContent.isNotEmpty()) {
                    NewsCacheState.selectedNewsContent = cachedContent
                }
            }
            NewsCacheState.isLoadingContent = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NewsTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back), false) { onBack() }
                
                if (NewsCacheState.isLoadingList && NewsCacheState.newsList.isEmpty()) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
                        color = AppColors.TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                } else {
                    NewsCacheState.newsList.forEachIndexed { index, title ->
                        val cleanedTitle = title.replaceFirst(Regex("""^\[\d+\]"""), "")
                        NewsTabButton(
                            text = cleanedTitle,
                            isActive = NewsCacheState.selectedNewsIndex == index
                        ) {
                            NewsCacheState.selectedNewsIndex = index
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.PanelDark)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                if (NewsCacheState.isLoadingContent && NewsCacheState.selectedNewsContent.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppColors.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
                                color = AppColors.TextGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    val blocks = remember(NewsCacheState.selectedNewsContent) {
                        parseNewsBlocks(NewsCacheState.selectedNewsContent)
                    }
                    if (blocks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vyberite_novost_iz_spiska_6a91),
                                color = AppColors.TextGray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            blocks.forEach { block ->
                                NewsBlockRenderer(block = block, uriHandler = uriHandler)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsTabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) AppColors.TabActive else AppColors.PanelDark
    val borderColor = if (isActive) AppColors.BorderActive else AppColors.Border

    Box(
        modifier = Modifier
            .widthIn(min = 110.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun NewsBlockRenderer(block: NewsBlock, uriHandler: androidx.compose.ui.platform.UriHandler) {
    when (block) {
        is NewsBlock.H1 -> {
            Text(
                text = block.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        is NewsBlock.H2 -> {
            Text(
                text = block.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        is NewsBlock.H3 -> {
            Text(
                text = block.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
        is NewsBlock.Bullet -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "• ",
                    color = AppColors.TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.weight(1f)) {
                    ClickableBBCodeText(
                        text = block.text,
                        uriHandler = uriHandler
                    )
                }
            }
        }
        is NewsBlock.Quote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.Overlay11)
                    .border(width = 1.dp, color = AppColors.Border, shape = RoundedCornerShape(2.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(AppColors.BorderActive)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ClickableBBCodeText(
                        text = block.text,
                        uriHandler = uriHandler
                    )
                }
            }
        }
        is NewsBlock.BoxBlock -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.Panel)
                    .border(width = 1.dp, color = AppColors.Border, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                ClickableBBCodeText(
                    text = block.text,
                    uriHandler = uriHandler
                )
            }
        }
        is NewsBlock.Spoiler -> {
            var expanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (expanded) "▼ " else "▶ ",
                        color = AppColors.BorderActive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = block.title,
                        color = AppColors.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (expanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        ClickableBBCodeText(
                            text = block.text,
                            uriHandler = uriHandler
                        )
                    }
                }
            }
        }
        is NewsBlock.MediaBlock -> {
            MediaBlockRenderer(block = block)
        }
        is NewsBlock.DetailsBlock -> {
            var expanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (expanded) "▼ " else "▶ ",
                        color = AppColors.BorderActive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = block.summary,
                        color = AppColors.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (expanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        ClickableBBCodeText(
                            text = block.content,
                            uriHandler = uriHandler
                        )
                    }
                }
            }
        }
        is NewsBlock.CodeBlock -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(AppColors.DarkGray1E)
                    .border(width = 1.dp, color = AppColors.Border, shape = RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = block.codeText,
                    fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily,
                    fontSize = 12.sp,
                    color = AppColors.PureGreen,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                        .clickable {
                            try {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("News Code", block.codeText)
                                clipboard.setPrimaryClip(clip)
                            } catch (e: Exception) {}
                        }
                        .padding(vertical = 7.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_skopirovat),
                        color = AppColors.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
        is NewsBlock.Paragraph -> {
            Box(modifier = Modifier.padding(vertical = 2.dp)) {
                ClickableBBCodeText(
                    text = block.text,
                    uriHandler = uriHandler
                )
            }
        }
        is NewsBlock.SpacerBlock -> {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun MediaBlockRenderer(block: NewsBlock.MediaBlock) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var fileUri by remember { mutableStateOf<java.io.File?>(null) }
    val localFile = remember(block.filename) {
        java.io.File(context.cacheDir, "news_media_${block.filename}")
    }

    LaunchedEffect(block.filename) {
        if (localFile.exists() && localFile.length() > 0L) {
            fileUri = localFile
        } else {
            downloadProgress = 0f
            val tempFile = java.io.File(context.cacheDir, "news_media_${block.filename}.tmp")
            try {
                if (tempFile.exists()) tempFile.delete()
                var chunkIndex = 0
                var totalChunks = 1
                // потоковая запись чанков прямо на диск
                java.io.FileOutputStream(tempFile).use { outStream ->
                    while (chunkIndex < totalChunks) {
                        val resp = sys_menu_socket_repository.getNewsFileChunkOnline(block.filename, chunkIndex)
                        if (resp != null && resp.startsWith("NEWS_FILE_CHUNK|")) {
                            val parts = resp.split("|")
                            if (parts.size >= 5) {
                                val tot = parts[3].toIntOrNull() ?: 1
                                totalChunks = tot
                                val base64 = parts[4]
                                val chunkBytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                                outStream.write(chunkBytes)
                                chunkIndex++
                                downloadProgress = chunkIndex.toFloat() / totalChunks.toFloat()
                            } else {
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                if (chunkIndex == totalChunks && tempFile.length() > 0L) {
                    tempFile.renameTo(localFile)
                    fileUri = localFile
                } else {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                tempFile.delete()
            } finally {
                downloadProgress = null
            }
        }
    }

    // безопасное декодирование битмапа
    val bitmap = remember(fileUri) {
        if (fileUri != null && !block.isVideo) {
            try {
                val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(fileUri!!.absolutePath, boundsOpts)
                var inSample = 1
                val maxDim = 1920
                while (boundsOpts.outWidth / (inSample * 2) >= maxDim || boundsOpts.outHeight / (inSample * 2) >= maxDim) {
                    inSample *= 2
                }
                val decodeOpts = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = inSample
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                android.graphics.BitmapFactory.decodeFile(fileUri!!.absolutePath, decodeOpts)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        val targetWidth = block.width.dp
        val finalWidth: androidx.compose.ui.unit.Dp
        val finalHeight: androidx.compose.ui.unit.Dp

        if (block.isVideo) {
            val calculatedHeight = targetWidth * (9f / 16f)
            if (targetWidth > maxWidth) {
                val scale = maxWidth / targetWidth
                finalWidth = maxWidth
                finalHeight = calculatedHeight * scale
            } else {
                finalWidth = targetWidth
                finalHeight = calculatedHeight
            }
        } else {
            if (bitmap != null) {
                val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
                val calculatedHeight = targetWidth * aspect
                if (targetWidth > maxWidth) {
                    val scale = maxWidth / targetWidth
                    finalWidth = maxWidth
                    finalHeight = calculatedHeight * scale
                } else {
                    finalWidth = targetWidth
                    finalHeight = calculatedHeight
                }
            } else {
                if (targetWidth > maxWidth) {
                    finalWidth = maxWidth
                    finalHeight = maxWidth * 0.75f
                } else {
                    finalWidth = targetWidth
                    finalHeight = targetWidth * 0.75f
                }
            }
        }

        val mediaContent = @Composable {
            val hasBorder = block.borderStyle != "bezramki"
            Box(
                modifier = Modifier
                    .size(finalWidth, finalHeight)
                    .then(
                        if (hasBorder) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Panel)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val progress = downloadProgress
                val file = fileUri
                if (progress != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppColors.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = AppColors.White,
                            fontSize = 11.sp
                        )
                    }
                } else if (file != null) {
                    if (block.isVideo) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                android.widget.VideoView(ctx).apply {
                                    setVideoPath(file.absolutePath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = block.filename,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_oshibka_zagruzki_e0e3), color = AppColors.Red, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_fayl_otsutstvuet_f6da), color = AppColors.TextGray, fontSize = 12.sp)
                }
            }
        }

        when (block.align) {
            "sleva" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    mediaContent()
                    if (block.companionText != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                            ClickableBBCodeText(
                                text = block.companionText,
                                uriHandler = uriHandler
                            )
                        }
                    }
                }
            }
            "sprava" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (block.companionText != null) {
                        Box(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                            ClickableBBCodeText(
                                text = block.companionText,
                                uriHandler = uriHandler
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    mediaContent()
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    mediaContent()
                    if (block.companionText != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        ClickableBBCodeText(
                            text = block.companionText,
                            uriHandler = uriHandler
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableBBCodeText(text: AnnotatedString, uriHandler: androidx.compose.ui.platform.UriHandler) {
    val processedText = remember(text) {
        buildAnnotatedString {
            append(text.text)
            text.getStringAnnotations(0, text.length).forEach { annot ->
                addStringAnnotation(annot.tag, annot.item, annot.start, annot.end)
            }
            text.spanStyles.forEach { span ->
                addStyle(span.item, span.start, span.end)
            }
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = processedText,
        style = androidx.compose.ui.text.TextStyle(
            color = AppColors.TextGray,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        onClick = { offset ->
            processedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item.trim()
                    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {}
                    }
                }
        }
    )
}

fun parseNewsBlocks(rawText: String): List<NewsBlock> {
    val blocks = mutableListOf<NewsBlock>()
    val lines = rawText.removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n").split("\n")
    var inMultilineBlock: String? = null
    var multilineParam: String? = null
    val multilineContent = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()

        if (inMultilineBlock != null) {
            val closeTag = "[/$inMultilineBlock]"
            if (trimmed.endsWith(closeTag, ignoreCase = true)) {
                val endOffset = trimmed.length - closeTag.length
                val beforeClose = trimmed.substring(0, endOffset)
                if (beforeClose.isNotEmpty()) {
                    multilineContent.append(beforeClose)
                }
                val cleanMultiText = multilineContent.toString().trim()
                val blockText = parseInlineBBCode(cleanMultiText)
                when (inMultilineBlock) {
                    "spoiler" -> blocks.add(NewsBlock.Spoiler(multilineParam ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_skrytyy_tekst_6499), blockText))
                    "quote" -> blocks.add(NewsBlock.Quote(blockText))
                    "box" -> blocks.add(NewsBlock.BoxBlock(blockText))
                    "details" -> blocks.add(NewsBlock.DetailsBlock(multilineParam ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_podrobnee_db5f), blockText))
                    "code" -> blocks.add(NewsBlock.CodeBlock(cleanMultiText))
                    "bullet" -> {
                        val bulletLines = cleanMultiText.split("\n")
                        for (bLine in bulletLines) {
                            var cleanBLine = bLine.trim()
                            if (cleanBLine.isNotBlank()) {
                                if (cleanBLine.startsWith("•") || cleanBLine.startsWith("·") || cleanBLine.startsWith("-") || cleanBLine.startsWith("*")) {
                                    cleanBLine = cleanBLine.substring(1).trim()
                                }
                                blocks.add(NewsBlock.Bullet(parseInlineBBCode(cleanBLine)))
                            }
                        }
                    }
                    else -> blocks.add(NewsBlock.Paragraph(blockText))
                }
                inMultilineBlock = null
                multilineParam = null
                multilineContent.setLength(0)
            } else {
                multilineContent.append(line).append("\n")
            }
            continue
        }

        val imgIndex = line.indexOf("[image=", ignoreCase = true)
        val videoIndex = line.indexOf("[video=", ignoreCase = true)
        if (imgIndex != -1) {
            val closeIndex = line.indexOf(']', imgIndex)
            if (closeIndex != -1) {
                val tagContent = line.substring(imgIndex + 7, closeIndex)
                val parts = tagContent.split(",")
                val filename = parts.getOrNull(0)?.trim() ?: ""
                val sizeStr = parts.getOrNull(1)?.trim() ?: "300"
                val alignStr = parts.getOrNull(2)?.trim()?.lowercase() ?: "seredina"
                val borderStyle = parts.getOrNull(3)?.trim()?.lowercase() ?: "sramkoy"
                val w = extractInt(sizeStr)
                val beforeText = line.substring(0, imgIndex)
                val afterText = line.substring(closeIndex + 1)
                val companionRaw = (beforeText + afterText).replace("[/image]", "", ignoreCase = true).replace("[/video]", "", ignoreCase = true)
                val companionText = if (companionRaw.isNotBlank()) parseInlineBBCode(companionRaw) else null
                blocks.add(NewsBlock.MediaBlock(filename, w, alignStr, borderStyle, false, companionText))
            } else {
                if (line.isNotBlank()) {
                    blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
                } else {
                    blocks.add(NewsBlock.SpacerBlock)
                }
            }
        } else if (videoIndex != -1) {
            val closeIndex = line.indexOf(']', videoIndex)
            if (closeIndex != -1) {
                val tagContent = line.substring(videoIndex + 7, closeIndex)
                val parts = tagContent.split(",")
                val filename = parts.getOrNull(0)?.trim() ?: ""
                val sizeStr = parts.getOrNull(1)?.trim() ?: "300"
                val alignStr = parts.getOrNull(2)?.trim()?.lowercase() ?: "seredina"
                val borderStyle = parts.getOrNull(3)?.trim()?.lowercase() ?: "sramkoy"
                val w = extractInt(sizeStr)
                val beforeText = line.substring(0, videoIndex)
                val afterText = line.substring(closeIndex + 1)
                val companionRaw = (beforeText + afterText).replace("[/image]", "", ignoreCase = true).replace("[/video]", "", ignoreCase = true)
                val companionText = if (companionRaw.isNotBlank()) parseInlineBBCode(companionRaw) else null
                blocks.add(NewsBlock.MediaBlock(filename, w, alignStr, borderStyle, true, companionText))
            } else {
                if (line.isNotBlank()) {
                    blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
                } else {
                    blocks.add(NewsBlock.SpacerBlock)
                }
            }
        } else if (trimmed.startsWith("[h1]", ignoreCase = true) && trimmed.endsWith("[/h1]", ignoreCase = true)) {
            blocks.add(NewsBlock.H1(parseInlineBBCode(trimmed.substring(4, trimmed.length - 5).trim())))
        } else if (trimmed.startsWith("[h2]", ignoreCase = true) && trimmed.endsWith("[/h2]", ignoreCase = true)) {
            blocks.add(NewsBlock.H2(parseInlineBBCode(trimmed.substring(4, trimmed.length - 5).trim())))
        } else if (trimmed.startsWith("[h3]", ignoreCase = true) && trimmed.endsWith("[/h3]", ignoreCase = true)) {
            blocks.add(NewsBlock.H3(parseInlineBBCode(trimmed.substring(4, trimmed.length - 5).trim())))
        } else if (trimmed.startsWith("[bullet]", ignoreCase = true) && trimmed.endsWith("[/bullet]", ignoreCase = true)) {
            blocks.add(NewsBlock.Bullet(parseInlineBBCode(trimmed.substring(8, trimmed.length - 9).trim())))
        } else if (trimmed.startsWith("[spoiler=", ignoreCase = true) && trimmed.endsWith("[/spoiler]", ignoreCase = true)) {
            val eqIdx = trimmed.indexOf('=')
            val closeIdx = trimmed.indexOf(']')
            if (eqIdx != -1 && closeIdx != -1 && eqIdx < closeIdx) {
                val sum = trimmed.substring(eqIdx + 1, closeIdx)
                val body = trimmed.substring(closeIdx + 1, trimmed.length - 10).trim()
                blocks.add(NewsBlock.Spoiler(sum, parseInlineBBCode(body)))
            } else {
                blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
            }
        } else if (trimmed.startsWith("[spoiler]", ignoreCase = true) && trimmed.endsWith("[/spoiler]", ignoreCase = true)) {
            blocks.add(NewsBlock.Spoiler(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_skrytyy_tekst_6499), parseInlineBBCode(trimmed.substring(9, trimmed.length - 10).trim())))
        } else if (trimmed.startsWith("[quote]", ignoreCase = true) && trimmed.endsWith("[/quote]", ignoreCase = true)) {
            blocks.add(NewsBlock.Quote(parseInlineBBCode(trimmed.substring(7, trimmed.length - 8).trim())))
        } else if (trimmed.startsWith("[box]", ignoreCase = true) && trimmed.endsWith("[/box]", ignoreCase = true)) {
            blocks.add(NewsBlock.BoxBlock(parseInlineBBCode(trimmed.substring(5, trimmed.length - 6).trim())))
        } else if (trimmed.startsWith("[details=", ignoreCase = true) && trimmed.endsWith("[/details]", ignoreCase = true)) {
            val eqIdx = trimmed.indexOf('=')
            val closeIdx = trimmed.indexOf(']')
            if (eqIdx != -1 && closeIdx != -1 && eqIdx < closeIdx) {
                val sum = trimmed.substring(eqIdx + 1, closeIdx)
                val body = trimmed.substring(closeIdx + 1, trimmed.length - 10).trim()
                blocks.add(NewsBlock.DetailsBlock(sum, parseInlineBBCode(body)))
            } else {
                blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
            }
        } else if (trimmed.startsWith("[code]", ignoreCase = true) && trimmed.endsWith("[/code]", ignoreCase = true)) {
            blocks.add(NewsBlock.CodeBlock(trimmed.substring(6, trimmed.length - 7).trim()))
        } else if (trimmed.startsWith("[spoiler=", ignoreCase = true)) {
            val eqIdx = trimmed.indexOf('=')
            val closeIdx = trimmed.indexOf(']')
            if (eqIdx != -1 && closeIdx != -1 && eqIdx < closeIdx) {
                inMultilineBlock = "spoiler"
                multilineParam = trimmed.substring(eqIdx + 1, closeIdx)
                val rest = trimmed.substring(closeIdx + 1)
                if (rest.isNotEmpty()) {
                    multilineContent.append(rest).append("\n")
                }
            } else {
                blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
            }
        } else if (trimmed.startsWith("[spoiler]", ignoreCase = true)) {
            inMultilineBlock = "spoiler"
            multilineParam = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_skrytyy_tekst_6499)
            val rest = trimmed.substring(9)
            if (rest.isNotEmpty()) {
                multilineContent.append(rest).append("\n")
            }
        } else if (trimmed.startsWith("[quote]", ignoreCase = true)) {
            inMultilineBlock = "quote"
            val rest = trimmed.substring(7)
            if (rest.isNotEmpty()) {
                multilineContent.append(rest).append("\n")
            }
        } else if (trimmed.startsWith("[box]", ignoreCase = true)) {
            inMultilineBlock = "box"
            val rest = trimmed.substring(5)
            if (rest.isNotEmpty()) {
                multilineContent.append(rest).append("\n")
            }
        } else if (trimmed.startsWith("[bullet]", ignoreCase = true)) {
            inMultilineBlock = "bullet"
            val rest = trimmed.substring(8)
            if (rest.isNotEmpty()) {
                multilineContent.append(rest).append("\n")
            }
        } else if (trimmed.startsWith("[details=", ignoreCase = true)) {
            val eqIdx = trimmed.indexOf('=')
            val closeIdx = trimmed.indexOf(']')
            if (eqIdx != -1 && closeIdx != -1 && eqIdx < closeIdx) {
                inMultilineBlock = "details"
                multilineParam = trimmed.substring(eqIdx + 1, closeIdx)
                val rest = trimmed.substring(closeIdx + 1)
                if (rest.isNotEmpty()) {
                    multilineContent.append(rest).append("\n")
                }
            } else {
                blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
            }
        } else if (trimmed.startsWith("[code]", ignoreCase = true)) {
            inMultilineBlock = "code"
            val rest = trimmed.substring(6)
            if (rest.isNotEmpty()) {
                multilineContent.append(rest).append("\n")
            }
        } else {
            if (line.isNotBlank()) {
                blocks.add(NewsBlock.Paragraph(parseInlineBBCode(line)))
            } else {
                blocks.add(NewsBlock.SpacerBlock)
            }
        }
    }

    if (inMultilineBlock != null) {
        val cleanMultiText = multilineContent.toString().trim()
        val blockText = parseInlineBBCode(cleanMultiText)
        when (inMultilineBlock) {
            "spoiler" -> blocks.add(NewsBlock.Spoiler(multilineParam ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_skrytyy_tekst_6499), blockText))
            "quote" -> blocks.add(NewsBlock.Quote(blockText))
            "box" -> blocks.add(NewsBlock.BoxBlock(blockText))
            "details" -> blocks.add(NewsBlock.DetailsBlock(multilineParam ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_podrobnee_db5f), blockText))
            "code" -> blocks.add(NewsBlock.CodeBlock(cleanMultiText))
            else -> blocks.add(NewsBlock.Paragraph(blockText))
        }
    }

    return blocks
}

fun extractInt(s: String): Int {
    val digits = s.takeWhile { it.isDigit() }
    return digits.toIntOrNull() ?: 300
}

fun preserveConsecutiveSpaces(text: String): String {
    val sb = java.lang.StringBuilder()
    var spaceCount = 0
    for (char in text) {
        if (char == ' ') {
            spaceCount++
        } else {
            if (spaceCount > 0) {
                for (i in 0 until spaceCount) {
                    if (i == spaceCount - 1) {
                        sb.append(' ')
                    } else {
                        sb.append('\u00A0')
                    }
                }
                spaceCount = 0
            }
            sb.append(char)
        }
    }
    if (spaceCount > 0) {
        for (i in 0 until spaceCount) {
            if (i == spaceCount - 1) {
                sb.append(' ')
            } else {
                sb.append('\u00A0')
            }
        }
    }
    return sb.toString()
}

fun parseInlineBBCode(text: String): AnnotatedString {
    val processed = preserveConsecutiveSpaces(text)
    return buildAnnotatedString {
        var index = 0
        val len = processed.length
        val styleStack = java.util.Stack<Pair<Int, String>>()

        while (index < len) {
            if (processed[index] == '[' && index + 1 < len) {
                val closeBracketIndex = processed.indexOf(']', index)
                if (closeBracketIndex != -1) {
                    val tagContent = processed.substring(index + 1, closeBracketIndex)
                    if (tagContent.lowercase() == "br") {
                        append('\n')
                        index = closeBracketIndex + 1
                        continue
                    }
                    if (tagContent.startsWith("/")) {
                        val tagName = tagContent.substring(1)
                        var foundMatch = false
                        val tempStack = java.util.Stack<Pair<Int, String>>()
                        while (!styleStack.isEmpty()) {
                            val top = styleStack.pop()
                            if (top.second.substringBefore('=').substringBefore(' ').equals(tagName, ignoreCase = true)) {
                                foundMatch = true
                                val startPos = top.first
                                val endPos = length
                                val tagFull = top.second
                                applySpanStyle(tagFull, startPos, endPos)
                                break
                            } else {
                                tempStack.push(top)
                            }
                        }
                        while (!tempStack.isEmpty()) {
                            styleStack.push(tempStack.pop())
                        }
                        if (foundMatch) {
                            index = closeBracketIndex + 1
                            continue
                        }
                    } else {
                        styleStack.push(Pair(length, tagContent))
                        index = closeBracketIndex + 1
                        continue
                    }
                }
            }
            append(processed[index])
            index++
        }
    }
}

fun AnnotatedString.Builder.applySpanStyle(tag: String, start: Int, end: Int) {
    val name = tag.substringBefore('=').substringBefore(' ').lowercase()
    val param = if (tag.contains("=")) tag.substringAfter("=") else ""

    when (name) {
        "h1" -> addStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.White), start, end)
        "h2" -> addStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.White), start, end)
        "h3" -> addStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.White), start, end)
        "b" -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
        "i" -> addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, end)
        "u" -> addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), start, end)
        "s" -> addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), start, end)
        "mono", "code" -> addStyle(SpanStyle(fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily, background = AppColors.CodeBg), start, end)
        "color" -> {
            val color = try {
                if (param.startsWith("#")) {
                    Color(android.graphics.Color.parseColor(param))
                } else {
                    when (param.lowercase()) {
                        "red" -> AppColors.Red
                        "green" -> AppColors.Green
                        "blue" -> AppColors.Blue
                        "yellow" -> AppColors.Yellow
                        "cyan" -> AppColors.Cyan
                        "magenta" -> AppColors.Magenta
                        "white" -> AppColors.White
                        "black" -> AppColors.Black
                        "gray" -> AppColors.Gray
                        "darkgray" -> AppColors.DarkGray
                        "lightgray" -> AppColors.LightGray
                        else -> Color.Unspecified
                    }
                }
            } catch (e: Exception) {
                Color.Unspecified
            }
            if (color != Color.Unspecified) {
                addStyle(SpanStyle(color = color), start, end)
            }
        }
        "size" -> {
            val sizeSp = param.toIntOrNull()?.sp
            if (sizeSp != null) {
                addStyle(SpanStyle(fontSize = sizeSp), start, end)
            }
        }
        "bg" -> {
            val color = try {
                if (param.startsWith("#")) {
                    Color(android.graphics.Color.parseColor(param))
                } else {
                    Color.Unspecified
                }
            } catch (e: Exception) {
                Color.Unspecified
            }
            if (color != Color.Unspecified) {
                addStyle(SpanStyle(background = color), start, end)
            }
        }
        "sup" -> {
            addStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript), start, end)
        }
        "sub" -> {
            addStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript), start, end)
        }
        "shadow" -> {
            addStyle(
                SpanStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = AppColors.Black,
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        
                    )
                ),
                start,
                end
            )
        }
        "glow" -> {
            addStyle(
                SpanStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = AppColors.Cyan,
                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                        
                    )
                ),
                start,
                end
            )
        }
        "url", "link" -> {
            addStyle(
                SpanStyle(
                    color = AppColors.LinkBlue,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                start,
                end
            )
            if (param.isNotBlank()) {
                addStringAnnotation(
                    tag = "URL",
                    annotation = param,
                    start = start,
                    end = end
                )
            }
        }
        "center" -> {
            addStyle(
                androidx.compose.ui.text.ParagraphStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                start,
                end
            )
        }
        "right" -> {
            addStyle(
                androidx.compose.ui.text.ParagraphStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right
                ),
                start,
                end
            )
        }
        "left" -> {
            addStyle(
                androidx.compose.ui.text.ParagraphStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Left
                ),
                start,
                end
            )
        }
    }
}
