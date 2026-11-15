package skayrux.shootout2d

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatCacheManager {

    // очистка идентификатора от спецсимволов пути
    private fun safeId(rawId: String): String {
        return rawId.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    fun savePersonalMessages(context: Context, friendId: String, messages: List<ChatMessage>) {
        val cleanId = safeId(friendId)
        if (cleanId.isBlank()) return
        try {
            val dir = File(context.filesDir, "cache/chats/personal")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$cleanId.json")
            val array = JSONArray()
            messages.forEach { msg ->
                val obj = JSONObject().apply {
                    put("sender", msg.sender)
                    put("text", msg.text)
                    put("isMe", msg.isMe)
                    put("isSystem", msg.isSystem)
                    put("timestamp", msg.timestamp)
                }
                array.put(obj)
            }
            file.writeText(AppSettings.encryptText(array.toString()))
            try {
                val legacyExt = context.getExternalFilesDir(null)
                if (legacyExt != null) {
                    val legacyFile = File(legacyExt, "cache/chats/personal/$cleanId.json")
                    if (legacyFile.exists()) legacyFile.delete()
                }
            } catch (e: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadPersonalMessages(context: Context, friendId: String): List<ChatMessage> {
        val cleanId = safeId(friendId)
        if (cleanId.isBlank()) return emptyList()
        try {
            val internalDir = File(context.filesDir, "cache/chats/personal")
            var file = File(internalDir, "$cleanId.json")
            var fromLegacy = false
            if (!file.exists()) {
                val legacyExt = try { context.getExternalFilesDir(null) } catch (e: Exception) { null }
                val legacyFile = if (legacyExt != null) File(legacyExt, "cache/chats/personal/$cleanId.json") else null
                if (legacyFile != null && legacyFile.exists()) {
                    file = legacyFile
                    fromLegacy = true
                } else {
                    return emptyList()
                }
            }
            val rawContent = file.readText().trim()
            if (rawContent.isBlank()) return emptyList()
            val jsonText = if (rawContent.startsWith("enc_v1$")) {
                AppSettings.decryptText(rawContent)
            } else {
                rawContent
            }
            if (jsonText.isBlank()) return emptyList()
            val array = JSONArray(jsonText)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        sender = obj.optString("sender", ""),
                        text = obj.optString("text", ""),
                        isMe = obj.optBoolean("isMe", false),
                        isSystem = obj.optBoolean("isSystem", false),
                        timestamp = obj.optString("timestamp", "")
                    )
                )
            }
            if (fromLegacy || !rawContent.startsWith("enc_v1$")) {
                savePersonalMessages(context, cleanId, list)
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun saveGroupMessages(context: Context, groupId: String, messages: List<ChatMessage>) {
        val cleanId = safeId(groupId)
        if (cleanId.isBlank()) return
        try {
            val dir = File(context.filesDir, "cache/chats/group")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$cleanId.json")
            val array = JSONArray()
            messages.forEach { msg ->
                val obj = JSONObject().apply {
                    put("sender", msg.sender)
                    put("text", msg.text)
                    put("isMe", msg.isMe)
                    put("isSystem", msg.isSystem)
                    put("timestamp", msg.timestamp)
                }
                array.put(obj)
            }
            file.writeText(AppSettings.encryptText(array.toString()))
            try {
                val legacyExt = context.getExternalFilesDir(null)
                if (legacyExt != null) {
                    val legacyFile = File(legacyExt, "cache/chats/group/$cleanId.json")
                    if (legacyFile.exists()) legacyFile.delete()
                }
            } catch (e: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGroupMessages(context: Context, groupId: String): List<ChatMessage> {
        val cleanId = safeId(groupId)
        if (cleanId.isBlank()) return emptyList()
        try {
            val internalDir = File(context.filesDir, "cache/chats/group")
            var file = File(internalDir, "$cleanId.json")
            var fromLegacy = false
            if (!file.exists()) {
                val legacyExt = try { context.getExternalFilesDir(null) } catch (e: Exception) { null }
                val legacyFile = if (legacyExt != null) File(legacyExt, "cache/chats/group/$cleanId.json") else null
                if (legacyFile != null && legacyFile.exists()) {
                    file = legacyFile
                    fromLegacy = true
                } else {
                    return emptyList()
                }
            }
            val rawContent = file.readText().trim()
            if (rawContent.isBlank()) return emptyList()
            val jsonText = if (rawContent.startsWith("enc_v1$")) {
                AppSettings.decryptText(rawContent)
            } else {
                rawContent
            }
            if (jsonText.isBlank()) return emptyList()
            val array = JSONArray(jsonText)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        sender = obj.optString("sender", ""),
                        text = obj.optString("text", ""),
                        isMe = obj.optBoolean("isMe", false),
                        isSystem = obj.optBoolean("isSystem", false),
                        timestamp = obj.optString("timestamp", "")
                    )
                )
            }
            if (fromLegacy || !rawContent.startsWith("enc_v1$")) {
                saveGroupMessages(context, cleanId, list)
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
