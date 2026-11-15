package skayrux.shootout2d

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File

data class MenuBannerItem(
    val id: String,
    val versiya: Int,
    val fileName: String,
    val ssylka: String,
    val perehodnikMenu: String,
    val localFile: File? = null,
    val isVideo: Boolean = false
)

object banners_cache_manager {
    var bannersList by mutableStateOf<List<MenuBannerItem>>(emptyList())
    var isLoaded by mutableStateOf(false)
    private val syncMutex = Mutex()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val bitmapCache = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()
    private var lastSyncTimeMs = 0L

    // мгновенная загрузка баннеров с диска при старте
    fun loadFromDiskInitial(context: Context) {
        if (bannersList.isNotEmpty()) return
        try {
            val bannersDir = File(context.filesDir, "banners_menu")
            if (!bannersDir.exists()) return
            val subDirs = bannersDir.listFiles { f -> f.isDirectory } ?: return
            val mediaExts = listOf(".mp4", ".mkv", ".webm", ".3gp")
            val cached = mutableListOf<MenuBannerItem>()
            for (sub in subDirs) {
                val bannerFile = sub.listFiles { f ->
                    f.isFile && f.name != "version.txt" && f.name != "meta.txt" && !f.name.endsWith(".tmp")
                }?.firstOrNull() ?: continue
                if (bannerFile.length() == 0L) continue
                val versionFile = File(sub, "version.txt")
                val ver = if (versionFile.exists()) versionFile.readText().trim().toIntOrNull() ?: 1 else 1
                val isVid = mediaExts.any { bannerFile.name.lowercase().endsWith(it) }

                var ssylka = ""
                var perehodnikMenu = ""
                val metaFile = File(sub, "meta.txt")
                if (metaFile.exists()) {
                    val lines = metaFile.readLines()
                    ssylka = lines.getOrNull(0) ?: ""
                    perehodnikMenu = lines.getOrNull(1) ?: ""
                }

                cached.add(
                    MenuBannerItem(
                        id = sub.name,
                        versiya = ver,
                        fileName = bannerFile.name,
                        ssylka = ssylka,
                        perehodnikMenu = perehodnikMenu,
                        localFile = bannerFile,
                        isVideo = isVid
                    )
                )
            }
            if (cached.isNotEmpty()) {
                bannersList = cached
                isLoaded = true
            }
        } catch (e: Exception) {}
    }

    // запуск фонового скачивания независимо от экрана
    fun startBackgroundSync(context: Context) {
        val appContext = context.applicationContext
        loadFromDiskInitial(appContext)
        backgroundScope.launch {
            syncBanners(appContext)
        }
    }

    // проверка версии и скачивание баннеров в фоне
    suspend fun syncBanners(context: Context) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastSyncTimeMs < 5000L && bannersList.isNotEmpty()) return@withContext
        if (!syncMutex.tryLock()) return@withContext
        try {
            lastSyncTimeMs = now
            val bannersDir = File(context.filesDir, "banners_menu")
            if (!bannersDir.exists()) bannersDir.mkdirs()

            val onlineList = sys_menu_socket_repository.getBannersMenuOnline() ?: return@withContext
            if (onlineList.isEmpty()) {
                val subDirs = bannersDir.listFiles { f -> f.isDirectory } ?: emptyArray()
                subDirs.forEach { it.deleteRecursively() }
                bitmapCache.clear()
                bannersList = emptyList()
                isLoaded = true
                return@withContext
            }

            val validIds = onlineList.map { it.id }.toSet()
            val existingDirs = bannersDir.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (d in existingDirs) {
                if (d.name !in validIds) {
                    d.deleteRecursively()
                }
            }

            val resolved = mutableListOf<MenuBannerItem>()
            val mediaExts = listOf(".mp4", ".mkv", ".webm", ".3gp")

            for (item in onlineList) {
                val subDir = File(bannersDir, item.id)
                if (!subDir.exists()) subDir.mkdirs()

                val versionFile = File(subDir, "version.txt")
                val localVer = if (versionFile.exists()) versionFile.readText().trim().toIntOrNull() ?: -1 else -1
                val targetFile = File(subDir, item.fileName)
                val metaFile = File(subDir, "meta.txt")

                // сверка версии: если версия одинаковая и файл существует, не скачиваем
                val isSameVersion = (localVer == item.versiya) && targetFile.exists() && targetFile.length() > 0L

                if (isSameVersion) {
                    metaFile.writeText("${item.ssylka}\n${item.perehodnikMenu}")
                    val isVid = mediaExts.any { item.fileName.lowercase().endsWith(it) }
                    resolved.add(item.copy(localFile = targetFile, isVideo = isVid))
                    bannersList = resolved.toList()
                } else {
                    // версия разная, скачиваем баннер в фоне
                    val byteList = mutableListOf<ByteArray>()
                    var chunkIdx = 0
                    var totalChunks = 1
                    var downloadSuccess = true
                    while (chunkIdx < totalChunks) {
                        val resp = sys_menu_socket_repository.getBannerFileChunkOnline(item.id, item.fileName, chunkIdx)
                        if (resp != null && resp.startsWith("BANNER_FILE_CHUNK|")) {
                            val parts = resp.split("|")
                            if (parts.size >= 6) {
                                totalChunks = parts[4].toIntOrNull() ?: 1
                                val b64 = parts[5]
                                val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
                                byteList.add(bytes)
                                chunkIdx++
                            } else {
                                downloadSuccess = false
                                break
                            }
                        } else {
                            downloadSuccess = false
                            break
                        }
                    }

                    if (downloadSuccess && chunkIdx == totalChunks && byteList.isNotEmpty()) {
                        val tmpFile = File(subDir, "${item.fileName}.tmp")
                        tmpFile.outputStream().use { out -> byteList.forEach { out.write(it) } }
                        subDir.listFiles()?.forEach { f ->
                            if (f.name != "version.txt" && f.name != "meta.txt" && f.name != "${item.fileName}.tmp") {
                                f.delete()
                            }
                        }
                        if (targetFile.exists()) targetFile.delete()
                        if (!tmpFile.renameTo(targetFile)) {
                            tmpFile.copyTo(targetFile, overwrite = true)
                            tmpFile.delete()
                        }
                        versionFile.writeText(item.versiya.toString())
                        metaFile.writeText("${item.ssylka}\n${item.perehodnikMenu}")
                        bitmapCache.remove(targetFile.absolutePath)

                        val isVid = mediaExts.any { item.fileName.lowercase().endsWith(it) }
                        resolved.add(item.copy(localFile = targetFile, isVideo = isVid))
                        bannersList = resolved.toList()
                    } else {
                        // при неудаче скачивания используем текущий файл, если есть
                        if (targetFile.exists() && targetFile.length() > 0L) {
                            val isVid = mediaExts.any { item.fileName.lowercase().endsWith(it) }
                            resolved.add(item.copy(localFile = targetFile, isVideo = isVid))
                            bannersList = resolved.toList()
                        }
                    }
                }
            }

            if (resolved.isNotEmpty()) {
                bannersList = resolved
                isLoaded = true
            }
        } catch (e: Exception) {
            // при ошибке сети не сбрасываем баннеры
        } finally {
            syncMutex.unlock()
        }
    }
}
