package skayrux.shootout2d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

object AvatarkiCacheManager {
    // вычисление отпечатка аватарки по содержимому
    fun calcAvatarHash(base64: String): String {
        val clean = base64.trim().replace("\n", "").replace("\r", "")
        if (clean.isEmpty() || clean == "none" || clean == "LOADING") return "EMPTY"
        val crc = java.util.zip.CRC32()
        crc.update(clean.toByteArray(Charsets.UTF_8))
        return "${clean.length}_${crc.value.toString(16)}"
    }

    // очистка идентификатора от спецсимволов пути
    private fun safeId(rawId: String): String {
        return rawId.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "default" }
    }

    fun getAvatarFile(context: Context, userId: String): File {
        val cleanId = safeId(userId)
        val avatarsDir = File(context.filesDir, "avatarki/users")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }
        val target = File(avatarsDir, "$cleanId.jpeg")
        if (!target.exists()) {
            try {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null) {
                    val legacy = File(extDir, "avatarki/users/$cleanId.jpeg")
                    if (legacy.exists()) {
                        legacy.copyTo(target, overwrite = true)
                        legacy.delete()
                    }
                }
            } catch (e: Exception) {}
        }
        return target
    }

    fun getCachedAvatarBase64(context: Context, userId: String): String {
        val file = getAvatarFile(context, userId)
        if (!file.exists()) return ""
        if (file.length() == 0L) return "none"
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun deleteAvatar(context: Context, userId: String) {
        val file = getAvatarFile(context, userId)
        if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {}
        }
    }

    fun saveAvatar(context: Context, userId: String, base64: String) {
        val file = getAvatarFile(context, userId)
        val clean = base64.trim().replace("\n", "").replace("\r", "")
        if (clean.isEmpty() || clean == "none") {
            try {
                FileOutputStream(file).use { }
            } catch (e: Exception) {}
            return
        }
        try {
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            file.writeBytes(bytes)
        } catch (e: Exception) {}
    }

    fun getGroupAvatarFile(context: Context, groupId: String): File {
        val cleanId = safeId(groupId)
        val avatarsDir = File(context.filesDir, "avatarki/groups")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }
        val target = File(avatarsDir, "$cleanId.jpeg")
        if (!target.exists()) {
            try {
                val extDir = context.getExternalFilesDir(null)
                if (extDir != null) {
                    val legacy = File(extDir, "avatarki/groups/$cleanId.jpeg")
                    if (legacy.exists()) {
                        legacy.copyTo(target, overwrite = true)
                        legacy.delete()
                    }
                }
            } catch (e: Exception) {}
        }
        return target
    }

    fun getCachedGroupAvatarBase64(context: Context, groupId: String): String {
        val file = getGroupAvatarFile(context, groupId)
        if (!file.exists()) return ""
        if (file.length() == 0L) return "none"
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun deleteGroupAvatar(context: Context, groupId: String) {
        val file = getGroupAvatarFile(context, groupId)
        if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {}
        }
    }

    fun saveGroupAvatar(context: Context, groupId: String, base64: String) {
        val file = getGroupAvatarFile(context, groupId)
        val clean = base64.trim().replace("\n", "").replace("\r", "")
        if (clean.isEmpty() || clean == "none") {
            try {
                FileOutputStream(file).use { }
            } catch (e: Exception) {}
            return
        }
        try {
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            file.writeBytes(bytes)
        } catch (e: Exception) {}
    }
}

object AvatarBitmapCacheManager {
    private val memoryCache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(120)

    fun getBitmap(base64: String): androidx.compose.ui.graphics.ImageBitmap? {
        if (base64.isEmpty() || base64 == "none") return null
        val cacheKey = base64.hashCode().toString()
        val cached = memoryCache.get(cacheKey)
        if (cached != null) return cached

        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache.put(cacheKey, imageBitmap)
                imageBitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
