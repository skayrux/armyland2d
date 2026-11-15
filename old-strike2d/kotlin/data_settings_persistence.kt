package skayrux.shootout2d

import android.content.Context
import android.content.SharedPreferences
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppSettings {
    // имя хранилища настроек
    private const val PREFS_NAME = "loc-strike2d"
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun syncGameSettingsFile(context: Context) {
        scope.launch {
            saveSettingsBinary(context)
            saveToConfigTxt(context)
        }
    }

    private fun saveToConfigTxt(context: Context) {
        try {
            val file = File(context.filesDir, "config.txt")
            val prefs = getPrefs(context)
            
            val friendlyFire = prefs.getBoolean("friendlyFire", false)
            val autobalance = prefs.getBoolean("autobalance", true)
            val weaponsAtStart = prefs.getBoolean("weaponsAtStart", true)
            
            val bloom = prefs.getBoolean("bloom", false)
            val shadows = prefs.getBoolean("shadows", false)
            val centerCamera = prefs.getBoolean("centerCamera", true)
            val smoothCamera = prefs.getBoolean("smoothCamera", false)
            
            val showFps = prefs.getBoolean("show_fps", false)
            val showPing = prefs.getBoolean("show_ping", false)
            
            val liveBackgroundPath = prefs.getString("live_background_path", "") ?: ""
            val liveBackgroundImagePath = prefs.getString("live_background_image_path", "") ?: ""
            val liveBackgroundSound = prefs.getBoolean("live_background_sound", false)
            val smokeBackground = prefs.getBoolean("smoke_background", true)
            
            val selectedCrosshair = prefs.getString("selectedCrosshair", "Красный") ?: "Красный"
            
            val soundOn = prefs.getBoolean("soundOn", true)
            val volume = prefs.getFloat("volume", 100.0f)
            val hudGlobalAlpha = prefs.getInt("hud_global_alpha", 90)

            val roundTimeSec = prefs.getInt("round_time_sec", 180)
            val startMoney = prefs.getInt("startmoney", 800)
            val spectateEnemy = prefs.getBoolean("spectate_enemy", true)
            val uiEnable = prefs.getBoolean("ui_enable", true)
            val autoUse = prefs.getBoolean("auto_use", true)
            val blackText = prefs.getBoolean("black_text", false)
            val timerLost = prefs.getBoolean("show_ping", prefs.getBoolean("timer_lost", false))
            val drawAlive = prefs.getBoolean("draw_alive", true)
            val noGroup = prefs.getBoolean("nogroup", false)

            val content = """
                # config 2D Shootout

                friendlyFire=$friendlyFire
                autobalance=$autobalance
                weaponsAtStart=$weaponsAtStart

                bloom=$bloom
                shadows=$shadows
                centerCamera=$centerCamera
                smoothCamera=$smoothCamera

                show_fps=$showFps
                show_ping=$showPing

                live_background_path=$liveBackgroundPath
                live_background_image_path=$liveBackgroundImagePath
                live_background_sound=$liveBackgroundSound
                smoke_background=$smokeBackground

                selectedCrosshair=$selectedCrosshair

                soundOn=$soundOn
                volume=$volume
                hud_global_alpha=$hudGlobalAlpha

                round_time_sec=$roundTimeSec
                startmoney=$startMoney
                spectate_enemy=$spectateEnemy
                ui_enable=$uiEnable
                auto_use=$autoUse
                black_text=$blackText
                timer_lost=$timerLost
                draw_alive=$drawAlive
                nogroup=$noGroup
            """.trimIndent()
            
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSettingsBinary(context: Context) {
        try {
            val prefs = getPrefs(context)
            val plname = prefs.getString("driver_player_name", "Player") ?: "Player"
            val soundOn = prefs.getBoolean("soundOn", true)
            val soundVolume = prefs.getFloat("volume", 100.0f).toInt().coerceIn(0, 100)
            val gtype = prefs.getInt("gtype", 0)
            val nmap = prefs.getInt("nmap", 0)
            val bbc = prefs.getInt("bbc", 0)
            val rbc = prefs.getInt("rbc", 0)
            val bbr = prefs.getInt("bbr", 1)
            val rbr = prefs.getInt("rbr", 1)
            val gameversion = 39
            val sealedPair = verifyOrRestoreSealedIdentity(context, prefs)
            var gamerid = sealedPair.first
            var usertmpid = sealedPair.second

            val lrcx = prefs.getInt("lrcx", 130)
            val lrcy = prefs.getInt("lrcy", 130)
            val prcx = prefs.getInt("prcx", 670)
            val prcy = prefs.getInt("prcy", 130)
            val lkrugrazd2 = prefs.getInt("lkrugrazd2", 95)
            val pkrugrazd2 = prefs.getInt("pkrugrazd2", 95)

            val bsetx = intArrayOf(475, 485, 500, 235, 443, 275, 359, 401, 485, 317, 2)
            val bsety = intArrayOf(152, 204, 256, 180, 82, 82, 82, 82, 82, 82, 270)
            val bsetw = intArrayOf(86, 86, 86, 60, 42, 42, 42, 42, 42, 42, 42)
            val bseth = intArrayOf(43, 43, 43, 43, 42, 42, 42, 42, 42, 42, 42)

            val friendlyFire = prefs.getBoolean("friendlyFire", false)
            val centerCamera = prefs.getBoolean("centerCamera", true)
            val weaponsAtStart = prefs.getBoolean("weaponsAtStart", true)
            val autobalance = prefs.getBoolean("autobalance", true)
            val noGroup = prefs.getBoolean("nogroup", false)
            val startMoney = prefs.getInt("startmoney", 800).coerceIn(100, 25000)

            val slotIds = intArrayOf(0, 0, 0)
            val roundTimeSec = prefs.getInt("round_time_sec", 180).coerceIn(30, 7200)
            val egperiod = (roundTimeSec * 30).coerceIn(900, 18000)

            val crosshairStr = prefs.getString("selectedCrosshair", "Красный") ?: "Красный"
            val crosshairAlpha = prefs.getFloat("crosshairAlpha", 0.8f)
            val crosshairEnabled = prefs.getBoolean("crosshairEnabled", true)
            val prizIsOn = crosshairEnabled && !crosshairStr.contains("Без", ignoreCase = true) && !crosshairStr.contains("No", ignoreCase = true) && (crosshairAlpha > 0.001f)
            val colorId = prefs.getString("crosshair_color_id", "") ?: ""
            val prizType = when {
                colorId == "white" || crosshairStr.contains("Бел", ignoreCase = true) || crosshairStr.contains("White", ignoreCase = true) -> 0
                colorId == "red" || crosshairStr.contains("Красн", ignoreCase = true) || crosshairStr.contains("Red", ignoreCase = true) -> 1
                colorId == "blue" || crosshairStr.contains("Син", ignoreCase = true) || crosshairStr.contains("Blue", ignoreCase = true) -> 2
                colorId == "lightblue" || crosshairStr.contains("Голуб", ignoreCase = true) || crosshairStr.contains("Light", ignoreCase = true) -> 3
                colorId == "green" || crosshairStr.contains("Зелен", ignoreCase = true) || crosshairStr.contains("Зелён", ignoreCase = true) || crosshairStr.contains("Green", ignoreCase = true) -> 4
                colorId == "orange" || crosshairStr.contains("Оранж", ignoreCase = true) || crosshairStr.contains("Orange", ignoreCase = true) -> 5
                colorId == "purple" || crosshairStr.contains("Фиолет", ignoreCase = true) || crosshairStr.contains("Purple", ignoreCase = true) -> 6
                colorId == "black" || crosshairStr.contains("Черн", ignoreCase = true) || crosshairStr.contains("Чёрн", ignoreCase = true) || crosshairStr.contains("Black", ignoreCase = true) -> 7
                colorId == "yellow" || crosshairStr.contains("Желт", ignoreCase = true) || crosshairStr.contains("Жёлт", ignoreCase = true) || crosshairStr.contains("Yellow", ignoreCase = true) -> 8
                else -> 1
            }

            val uiEnable = prefs.getBoolean("ui_enable", true)
            val autoUse = prefs.getBoolean("auto_use", true)
            val blackText = prefs.getBoolean("black_text", false)
            val timerLost = prefs.getBoolean("show_ping", prefs.getBoolean("timer_lost", false))
            val smooth = prefs.getBoolean("smoothCamera", false)
            val drawAlive = prefs.getBoolean("draw_alive", true)
            val plInfo = prefs.getString("driver_description", "Let's play 2DS!") ?: "Let's play 2DS!"
            val spectateEnemy = prefs.getBoolean("spectate_enemy", true)

            val file = File(context.filesDir, "settings")
            val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(file)))
            bw.write(plname + "\n")
            bw.write(1)
            bw.write(if (soundOn) 1 else 0)
            bw.write(gtype)
            bw.write(nmap)
            bw.write(bbc)
            bw.write(rbc)
            bw.write(bbr)
            bw.write(rbr)
            bw.write(gameversion)
            bw.write(gamerid.toString() + "\n")
            bw.write(usertmpid + "\n")
            bw.write(lrcx)
            bw.write(lrcy)
            bw.write(prcx)
            bw.write(prcy)
            bw.write(lkrugrazd2)
            bw.write(pkrugrazd2)
            for (i in 0 until 11) {
                bw.write(prefs.getInt("bsetx_$i", bsetx[i]))
                bw.write(prefs.getInt("bsety_$i", bsety[i]))
                bw.write(prefs.getInt("bsetw_$i", bsetw[i]))
                bw.write(prefs.getInt("bseth_$i", bseth[i]))
            }
            bw.write(if (friendlyFire) 1 else 0)
            bw.write(if (centerCamera) 1 else 0)
            bw.write(if (weaponsAtStart) 1 else 0)
            bw.write(if (autobalance) 1 else 0)
            bw.write(if (noGroup) 1 else 0)
            bw.write(startMoney)
            for (i in 0 until 3) {
                bw.write(prefs.getInt("slot_$i", slotIds[i]))
            }
            for (i in 0 until 40) {
                bw.write(if (prefs.getBoolean("weapon_avail_$i", true)) 1 else 0)
            }
            bw.write(egperiod)
            bw.write(soundVolume)
            bw.write(if (prizIsOn) 1 else 0)
            bw.write(if (uiEnable) 1 else 0)
            bw.write(if (autoUse) 1 else 0)
            bw.write(if (blackText) 1 else 0)
            bw.write(prizType)
            bw.write(if (timerLost) 1 else 0)
            bw.write(if (smooth) 1 else 0)
            bw.write(if (drawAlive) 1 else 0)
            bw.write(plInfo + "\n")
            bw.write(if (spectateEnemy) 1 else 0)
            bw.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSettingsBinary(context: Context) {
        try {
            val file = File(context.filesDir, "settings")
            if (!file.exists()) {
                saveSettingsBinary(context)
                return
            }
            val br = BufferedReader(InputStreamReader(FileInputStream(file)))
            val plname = br.readLine()
            br.read() // autopriz
            val soundOn = (br.read() == 1)
            val gtype = br.read()
            val nmap = br.read()
            val bbc = br.read()
            val rbc = br.read()
            val bbr = br.read()
            val rbr = br.read()
            val gameversion = br.read()
            val rid = br.readLine()
            val uid = br.readLine()

            val lrcx = br.read()
            val lrcy = br.read()
            val prcx = br.read()
            val prcy = br.read()
            val lkrugrazd2 = br.read()
            val pkrugrazd2 = br.read()

            val prefs = getPrefs(context)
            val editor = prefs.edit()
            if (plname != null && plname.isNotEmpty()) {
                editor.putString("driver_player_name", plname)
            }
            editor.putBoolean("soundOn", soundOn)
            editor.putInt("gtype", gtype)
            editor.putInt("nmap", nmap)
            editor.putInt("bbc", bbc)
            editor.putInt("rbc", rbc)
            editor.putInt("bbr", bbr)
            editor.putInt("rbr", rbr)

            val sealedPair = readSealedIdentity(context, prefs)
            if (sealedPair != null) {
                editor.putInt("gamerid", sealedPair.first)
                editor.putString("session_usertmpid", sealedPair.second)
            } else {
                val parsedRid = rid?.toIntOrNull()?.takeIf { it > 123456 } ?: kotlin.random.Random.nextInt(100000, 999999999)
                val hexChars = "0123456789abcdef"
                val cleanUid = (uid ?: "").trim().lowercase()
                val validUid = if (cleanUid.length == 12 && cleanUid.all { hexChars.indexOf(it) != -1 }) {
                    cleanUid
                } else {
                    java.util.UUID.randomUUID().toString().replace("-", "").lowercase().substring(0, 12)
                }
                editor.putInt("gamerid", parsedRid)
                editor.putString("session_usertmpid", validUid)
                writeSealedIdentity(context, editor, parsedRid, validUid)
            }
            editor.putInt("lrcx", lrcx)
            editor.putInt("lrcy", lrcy)
            editor.putInt("prcx", prcx)
            editor.putInt("prcy", prcy)
            editor.putInt("lkrugrazd2", lkrugrazd2)
            editor.putInt("pkrugrazd2", pkrugrazd2)

            for (i in 0 until 11) {
                editor.putInt("bsetx_$i", br.read())
                editor.putInt("bsety_$i", br.read())
                editor.putInt("bsetw_$i", br.read())
                editor.putInt("bseth_$i", br.read())
            }

            editor.putBoolean("friendlyFire", br.read() == 1)
            editor.putBoolean("centerCamera", br.read() == 1)
            editor.putBoolean("weaponsAtStart", br.read() == 1)
            editor.putBoolean("autobalance", br.read() == 1)
            editor.putBoolean("nogroup", br.read() == 1)
            editor.putInt("startmoney", br.read().coerceIn(100, 25000))

            if (gameversion == 39) {
                for (i in 0 until 3) {
                    editor.putInt("slot_$i", br.read())
                }
                for (i in 0 until 40) {
                    editor.putBoolean("weapon_avail_$i", br.read() == 1)
                }
                val egperiod = br.read().coerceIn(900, 18000)
                editor.putInt("round_time_sec", egperiod / 30)
                editor.putFloat("volume", br.read().toFloat().coerceIn(0f, 100f))
                val prizOn = br.read() == 1
                editor.putBoolean("crosshairEnabled", prizOn)
                editor.putBoolean("ui_enable", br.read() == 1)
                editor.putBoolean("auto_use", br.read() == 1)
                editor.putBoolean("black_text", br.read() == 1)
                val prizType = br.read()
                editor.putInt("priz_type", prizType)
                val colorId = colorIdFromPrizType(prizType)
                editor.putString("crosshair_color_id", colorId)
                editor.putBoolean("timer_lost", br.read() == 1)
                editor.putBoolean("smoothCamera", br.read() == 1)
                editor.putBoolean("draw_alive", br.read() == 1)
                val info = br.readLine()
                if (info != null && info.isNotEmpty()) {
                    editor.putString("driver_description", info)
                }
                editor.putBoolean("spectate_enemy", br.read() == 1)
            }
            editor.apply()
            br.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun ensureSettingsFile(context: Context) {
        val file = File(context.filesDir, "settings")
        if (!file.exists()) {
            saveSettingsBinary(context)
        }
    }

    fun resetGameSettingsToDefaults(context: Context) {
        val editor = getPrefs(context).edit()
        editor.putBoolean("friendlyFire", false)
        editor.putBoolean("autobalance", true)
        editor.putBoolean("weaponsAtStart", true)
        editor.putBoolean("centerCamera", true)
        editor.putBoolean("smoothCamera", false)
        editor.putBoolean("bloom", false)
        editor.putBoolean("shadows", false)
        editor.putBoolean("show_fps", false)
        editor.putBoolean("show_ping", false)
        editor.putBoolean("soundOn", true)
        editor.putFloat("volume", 100.0f)
        editor.putInt("round_time_sec", 180)
        editor.putInt("startmoney", 800)
        editor.putBoolean("spectate_enemy", true)
        editor.putBoolean("ui_enable", true)
        editor.putBoolean("auto_use", true)
        editor.putBoolean("black_text", false)
        editor.putBoolean("timer_lost", false)
        editor.putBoolean("draw_alive", true)
        editor.putBoolean("smoke_background", true)
        editor.putBoolean("nogroup", false)
        editor.putString("selectedCrosshair", "Красный")
        editor.putFloat("crosshairAlpha", 0.8f)
        editor.apply()
        syncGameSettingsFile(context)
    }

    private const val KEYSTORE_ALIAS = "strike2d_session_token_key_v1"
    private const val ENC_PREFIX = "enc_v1$"
    private const val BAN_SEAL_KEY = "sec_ban_seal"
    private const val ID_SEAL_KEY = "sec_id_seal"
    private const val BAN_BACKUP_FILE = ".sys_sec_state"
    private const val ID_BACKUP_FILE = ".sys_id_state"
    private val encryptedTokenKeys = setOf(
        "session_token",
        "saved_guest_token",
        "vk_auth_code",
        "driver_password",
        "saved_guest_pass",
        "vk_code_verifier",
        "myid_device",
        "banned_account_id"
    )

    // получение аппаратного ключа шифрования токена
    private fun getOrCreateTokenSecretKey(): javax.crypto.SecretKey? {
        return try {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            val existing = ks.getEntry(KEYSTORE_ALIAS, null) as? java.security.KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
                val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            null
        }
    }

    // шифрование токена перед сохранением
    private fun encryptTokenValue(plain: String): String {
        if (plain.isEmpty() || plain.startsWith(ENC_PREFIX)) return plain
        return try {
            val secretKey = getOrCreateTokenSecretKey() ?: return plain
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            ENC_PREFIX + android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            plain
        }
    }

    // расшифровка токена при чтении
    private fun decryptTokenValue(stored: String): String {
        if (stored.isEmpty() || !stored.startsWith(ENC_PREFIX)) return stored
        return try {
            val secretKey = getOrCreateTokenSecretKey() ?: return ""
            val rawB64 = stored.removePrefix(ENC_PREFIX)
            val combined = android.util.Base64.decode(rawB64, android.util.Base64.NO_WRAP)
            if (combined.size < 14) return ""
            val ivLen = combined[0].toInt() and 0xFF
            if (ivLen <= 0 || combined.size <= 1 + ivLen) return ""
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val ciphertext = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    // внешние методы шифрования и расшифровки для кэша и файлов
    fun encryptText(plain: String): String = encryptTokenValue(plain)

    fun decryptText(stored: String): String = decryptTokenValue(stored)

    // защита идентификаторов игрока от ручной подмены в файлах
    private fun readSealedIdentity(context: Context, prefs: SharedPreferences): Pair<Int, String>? {
        val candidates = mutableListOf<String>()
        prefs.getString(ID_SEAL_KEY, null)?.takeIf { it.isNotEmpty() }?.let { candidates.add(it) }
        try {
            val backupFile = File(context.noBackupFilesDir, ID_BACKUP_FILE)
            if (backupFile.exists()) {
                val diskSeal = backupFile.readText().trim()
                if (diskSeal.isNotEmpty()) candidates.add(diskSeal)
            }
        } catch (e: Exception) {}
        val hexChars = "0123456789abcdef"
        for (enc in candidates) {
            if (!enc.startsWith(ENC_PREFIX)) continue
            val dec = decryptTokenValue(enc)
            if (dec.startsWith("ID_V1:")) {
                val parts = dec.removePrefix("ID_V1:").split(":")
                if (parts.size == 2) {
                    val rid = parts[0].toIntOrNull()
                    val uid = parts[1].lowercase()
                    if (rid != null && rid > 123456 && uid.length == 12 && uid.all { hexChars.indexOf(it) != -1 }) {
                        return Pair(rid, uid)
                    }
                }
            }
        }
        return null
    }

    private fun writeSealedIdentity(context: Context, editor: SharedPreferences.Editor?, gamerid: Int, usertmpid: String) {
        val seal = encryptTokenValue("ID_V1:$gamerid:$usertmpid")
        if (seal.startsWith(ENC_PREFIX)) {
            if (editor != null) {
                editor.putString(ID_SEAL_KEY, seal)
            } else {
                getPrefs(context).edit().putString(ID_SEAL_KEY, seal).apply()
            }
            try {
                File(context.noBackupFilesDir, ID_BACKUP_FILE).writeText(seal)
            } catch (e: Exception) {}
        }
    }

    private fun verifyOrRestoreSealedIdentity(context: Context, prefs: SharedPreferences): Pair<Int, String> {
        val sealed = readSealedIdentity(context, prefs)
        if (sealed != null) {
            val curRid = prefs.getInt("gamerid", 0)
            val curUid = (prefs.getString("session_usertmpid", "") ?: "").lowercase()
            if (curRid != sealed.first || curUid != sealed.second) {
                val ed = prefs.edit()
                ed.putInt("gamerid", sealed.first)
                ed.putString("session_usertmpid", sealed.second)
                writeSealedIdentity(context, ed, sealed.first, sealed.second)
                ed.apply()
            }
            return sealed
        }
        var gamerid = prefs.getInt("gamerid", 0)
        if (gamerid <= 123456 || gamerid == 0 || gamerid == 1 || gamerid == -1) {
            gamerid = kotlin.random.Random.nextInt(100000, 999999999)
        }
        var usertmpid = (prefs.getString("session_usertmpid", "") ?: "").lowercase()
        val hexChars = "0123456789abcdef"
        val isValidHex12 = usertmpid.length == 12 && usertmpid.all { hexChars.indexOf(it) != -1 }
        if (!isValidHex12) {
            usertmpid = java.util.UUID.randomUUID().toString().replace("-", "").lowercase().substring(0, 12)
        }
        val ed = prefs.edit()
        ed.putInt("gamerid", gamerid)
        ed.putString("session_usertmpid", usertmpid)
        writeSealedIdentity(context, ed, gamerid, usertmpid)
        ed.apply()
        return Pair(gamerid, usertmpid)
    }

    // проверка целостности локального бана
    private fun verifyLocalBanState(context: Context, prefs: SharedPreferences): Boolean {
        val rawFlag = prefs.getBoolean("is_banned_locally", false)
        val candidates = mutableListOf<String>()
        prefs.getString(BAN_SEAL_KEY, null)?.takeIf { it.isNotEmpty() }?.let { candidates.add(it) }
        try {
            val backupFile = File(context.noBackupFilesDir, BAN_BACKUP_FILE)
            if (backupFile.exists()) {
                val diskSeal = backupFile.readText().trim()
                if (diskSeal.isNotEmpty()) candidates.add(diskSeal)
            }
        } catch (e: Exception) {}
        for (enc in candidates) {
            if (!enc.startsWith(ENC_PREFIX)) continue
            val dec = decryptTokenValue(enc)
            if (dec.startsWith("BANNED_V1:")) {
                val sealedAcc = dec.removePrefix("BANNED_V1:")
                if (!rawFlag) {
                    val ed = prefs.edit().putBoolean("is_banned_locally", true)
                    if (sealedAcc.isNotEmpty()) {
                        ed.putString("banned_account_id", encryptTokenValue(sealedAcc))
                    }
                    ed.putString(BAN_SEAL_KEY, enc).apply()
                }
                return true
            }
        }
        if (rawFlag) {
            val acc = getString(context, "banned_account_id", "")
            val seal = encryptTokenValue("BANNED_V1:$acc")
            if (seal.startsWith(ENC_PREFIX)) {
                prefs.edit().putString(BAN_SEAL_KEY, seal).apply()
                try {
                    File(context.noBackupFilesDir, BAN_BACKUP_FILE).writeText(seal)
                } catch (e: Exception) {}
            }
            return true
        }
        return false
    }

    fun getString(context: Context, key: String, defaultValue: String): String {
        val prefs = getPrefs(context)
        if (key == "session_usertmpid") {
            return verifyOrRestoreSealedIdentity(context, prefs).second
        }
        val raw = prefs.getString(key, null) ?: return defaultValue
        if (key in encryptedTokenKeys) {
            if (raw.isEmpty()) return defaultValue
            return if (raw.startsWith(ENC_PREFIX)) {
                decryptTokenValue(raw).ifEmpty { defaultValue }
            } else {
                val enc = encryptTokenValue(raw)
                if (enc != raw) {
                    prefs.edit().putString(key, enc).apply()
                }
                raw
            }
        }
        return raw
    }

    fun setString(context: Context, key: String, value: String) {
        val prefs = getPrefs(context)
        if (key == "session_usertmpid") {
            val hexChars = "0123456789abcdef"
            val clean = value.trim().lowercase()
            if (clean.length == 12 && clean.all { hexChars.indexOf(it) != -1 }) {
                val curRid = verifyOrRestoreSealedIdentity(context, prefs).first
                val ed = prefs.edit().putString(key, clean)
                writeSealedIdentity(context, ed, curRid, clean)
                ed.apply()
                syncGameSettingsFile(context)
            }
            return
        }
        val toStore = if (key in encryptedTokenKeys && value.isNotEmpty()) {
            encryptTokenValue(value)
        } else {
            value
        }
        val ed = prefs.edit().putString(key, toStore)
        if (key == "banned_account_id" && value.isNotEmpty() && verifyLocalBanState(context, prefs)) {
            val seal = encryptTokenValue("BANNED_V1:$value")
            if (seal.startsWith(ENC_PREFIX)) {
                ed.putString(BAN_SEAL_KEY, seal)
                try {
                    File(context.noBackupFilesDir, BAN_BACKUP_FILE).writeText(seal)
                } catch (e: Exception) {}
            }
        }
        ed.apply()
        syncGameSettingsFile(context)
    }

    fun getInt(context: Context, key: String, defaultValue: Int): Int {
        val prefs = getPrefs(context)
        if (key == "gamerid") {
            return verifyOrRestoreSealedIdentity(context, prefs).first
        }
        return prefs.getInt(key, defaultValue)
    }

    @Volatile
    private var cachedCrosshairArgb: Int? = null
    @Volatile
    private var cachedCrosshairEnabled: Boolean? = null
    @Volatile
    private var cachedUiEnable: Boolean? = null

    fun setInt(context: Context, key: String, value: Int) {
        if (key == "crosshairColorArgb") {
            cachedCrosshairArgb = null
        }
        val prefs = getPrefs(context)
        if (key == "gamerid" && value > 123456) {
            val curUid = verifyOrRestoreSealedIdentity(context, prefs).second
            val ed = prefs.edit().putInt(key, value)
            writeSealedIdentity(context, ed, value, curUid)
            ed.apply()
            syncGameSettingsFile(context)
            return
        }
        prefs.edit().putInt(key, value).apply()
        syncGameSettingsFile(context)
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        if (key == "ui_enable" && cachedUiEnable != null) {
            return cachedUiEnable!!
        }
        val prefs = getPrefs(context)
        if (key == "is_banned_locally") {
            return verifyLocalBanState(context, prefs)
        }
        return prefs.getBoolean(key, defaultValue)
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        if (key == "ui_enable") {
            cachedUiEnable = value
        }
        val prefs = getPrefs(context)
        val ed = prefs.edit().putBoolean(key, value)
        if (key == "is_banned_locally") {
            if (value) {
                val acc = getString(context, "banned_account_id", "")
                val seal = encryptTokenValue("BANNED_V1:$acc")
                if (seal.startsWith(ENC_PREFIX)) {
                    ed.putString(BAN_SEAL_KEY, seal)
                    try {
                        File(context.noBackupFilesDir, BAN_BACKUP_FILE).writeText(seal)
                    } catch (e: Exception) {}
                }
            } else {
                ed.remove(BAN_SEAL_KEY)
                try {
                    val f = File(context.noBackupFilesDir, BAN_BACKUP_FILE)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {}
            }
        }
        ed.apply()
        syncGameSettingsFile(context)
        if (key == "ui_enable" || key == "draw_alive" || key == "smoothCamera" || key == "centerCamera" || key == "auto_use" || key == "black_text" || key == "show_ping" || key == "timer_lost") {
            datagame_main.instance?.reloadInGameSettings()
        }
    }

    fun setUiEnable(context: Context, enabled: Boolean) {
        cachedUiEnable = enabled
        getPrefs(context).edit().putBoolean("ui_enable", enabled).apply()
        syncGameSettingsFile(context)
        datagame_main.instance?.reloadInGameSettings()
    }

    fun getUiEnable(context: Context): Boolean {
        cachedUiEnable?.let { return it }
        val res = getPrefs(context).getBoolean("ui_enable", true)
        cachedUiEnable = res
        return res
    }

    fun getFloat(context: Context, key: String, defaultValue: Float): Float {
        return getPrefs(context).getFloat(key, defaultValue)
    }

    fun setFloat(context: Context, key: String, value: Float) {
        if (key == "crosshairAlpha") {
            cachedCrosshairArgb = null
            cachedCrosshairEnabled = null
        }
        getPrefs(context).edit().putFloat(key, value).apply()
        syncGameSettingsFile(context)
    }

    fun colorIdFromPrizType(type: Int): String = when (type) {
        0 -> "white"
        1 -> "red"
        2 -> "blue"
        3 -> "lightblue"
        4 -> "green"
        5 -> "orange"
        6 -> "purple"
        7 -> "black"
        8 -> "yellow"
        else -> "red"
    }

    fun prizTypeFromColorId(colorId: String): Int = when (colorId.lowercase()) {
        "white" -> 0
        "red" -> 1
        "blue" -> 2
        "lightblue" -> 3
        "green" -> 4
        "orange" -> 5
        "purple" -> 6
        "black" -> 7
        "yellow" -> 8
        else -> 1
    }

    fun getCrosshairColorId(context: Context): String {
        val prefs = getPrefs(context)
        val savedId = prefs.getString("crosshair_color_id", null)
        if (!savedId.isNullOrEmpty()) {
            return savedId
        }
        if (prefs.contains("priz_type")) {
            val pt = prefs.getInt("priz_type", 1)
            return colorIdFromPrizType(pt)
        }
        val crosshairStr = prefs.getString("selectedCrosshair", "Красный") ?: "Красный"
        return when {
            crosshairStr.contains("Бел", ignoreCase = true) || crosshairStr.contains("White", ignoreCase = true) -> "white"
            crosshairStr.contains("Красн", ignoreCase = true) || crosshairStr.contains("Red", ignoreCase = true) -> "red"
            crosshairStr.contains("Желт", ignoreCase = true) || crosshairStr.contains("Жёлт", ignoreCase = true) || crosshairStr.contains("Yellow", ignoreCase = true) -> "yellow"
            crosshairStr.contains("Голуб", ignoreCase = true) || crosshairStr.contains("Light", ignoreCase = true) -> "lightblue"
            crosshairStr.contains("Син", ignoreCase = true) || crosshairStr.contains("Blue", ignoreCase = true) -> "blue"
            crosshairStr.contains("Зелен", ignoreCase = true) || crosshairStr.contains("Зелён", ignoreCase = true) || crosshairStr.contains("Green", ignoreCase = true) -> "green"
            crosshairStr.contains("Оранж", ignoreCase = true) || crosshairStr.contains("Orange", ignoreCase = true) -> "orange"
            crosshairStr.contains("Фиолет", ignoreCase = true) || crosshairStr.contains("Purple", ignoreCase = true) -> "purple"
            crosshairStr.contains("Черн", ignoreCase = true) || crosshairStr.contains("Чёрн", ignoreCase = true) || crosshairStr.contains("Black", ignoreCase = true) -> "black"
            else -> "red"
        }
    }

    fun setCrosshair(context: Context, colorName: String, colorArgb: Int, alpha: Float) {
        val crosshairStr = colorName
        val colorId = when {
            crosshairStr.contains("Бел", ignoreCase = true) || crosshairStr.contains("White", ignoreCase = true) -> "white"
            crosshairStr.contains("Красн", ignoreCase = true) || crosshairStr.contains("Red", ignoreCase = true) -> "red"
            crosshairStr.contains("Желт", ignoreCase = true) || crosshairStr.contains("Жёлт", ignoreCase = true) || crosshairStr.contains("Yellow", ignoreCase = true) -> "yellow"
            crosshairStr.contains("Голуб", ignoreCase = true) || crosshairStr.contains("Light", ignoreCase = true) -> "lightblue"
            crosshairStr.contains("Син", ignoreCase = true) || crosshairStr.contains("Blue", ignoreCase = true) -> "blue"
            crosshairStr.contains("Зелен", ignoreCase = true) || crosshairStr.contains("Зелён", ignoreCase = true) || crosshairStr.contains("Green", ignoreCase = true) -> "green"
            crosshairStr.contains("Оранж", ignoreCase = true) || crosshairStr.contains("Orange", ignoreCase = true) -> "orange"
            crosshairStr.contains("Фиолет", ignoreCase = true) || crosshairStr.contains("Purple", ignoreCase = true) -> "purple"
            crosshairStr.contains("Черн", ignoreCase = true) || crosshairStr.contains("Чёрн", ignoreCase = true) || crosshairStr.contains("Black", ignoreCase = true) -> "black"
            else -> "red"
        }
        setCrosshair(context, colorId, colorArgb, alpha, colorName)
    }

    fun setCrosshair(context: Context, colorId: String, colorArgb: Int, alpha: Float, colorName: String) {
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        val isEnabled = clampedAlpha > 0.001f
        val rgb = colorArgb and 0x00FFFFFF
        val a = (clampedAlpha * 255f).toInt().coerceIn(0, 255)
        val finalArgb = if (isEnabled) ((a shl 24) or rgb) else 0

        cachedCrosshairArgb = finalArgb
        cachedCrosshairEnabled = isEnabled

        val name = if (colorName.isNotEmpty()) colorName else colorId
        val prizType = prizTypeFromColorId(colorId)

        getPrefs(context).edit()
            .putString("crosshair_color_id", colorId)
            .putInt("priz_type", prizType)
            .putInt("crosshairColorArgb", colorArgb)
            .putFloat("crosshairAlpha", clampedAlpha)
            .putBoolean("crosshairEnabled", isEnabled)
            .putString("selectedCrosshair", if (isEnabled) name else "Без прицела")
            .putString("lastSelectedColorName", name)
            .apply()

        syncGameSettingsFile(context)
        datagame_main.instance?.reloadInGameSettings()
    }

    fun getCrosshairArgb(context: Context): Int {
        cachedCrosshairArgb?.let { return it }
        val prefs = getPrefs(context)
        val alpha = prefs.getFloat("crosshairAlpha", 0.9f).coerceIn(0f, 1f)
        if (alpha <= 0.001f) {
            cachedCrosshairArgb = 0
            return 0
        }
        val enabled = prefs.getBoolean("crosshairEnabled", true)
        if (!enabled) {
            cachedCrosshairArgb = 0
            return 0
        }
        val crosshairStr = prefs.getString("selectedCrosshair", "Красный") ?: "Красный"
        if (crosshairStr.contains("Без", ignoreCase = true) || crosshairStr.contains("No", ignoreCase = true)) {
            cachedCrosshairArgb = 0
            return 0
        }
        val colorId = getCrosshairColorId(context)
        val rgb = when (colorId) {
            "white" -> 0xFFFFFF
            "red" -> 0xFF0000
            "yellow" -> 0xFFFFD600.toInt() and 0x00FFFFFF
            "blue" -> 0x0000FF
            "lightblue" -> 0x0257C7
            "green" -> 0x00C853
            "orange" -> 0xD18100
            "purple" -> 0x5B00A6
            "black" -> 0x000000
            else -> 0xFF0000
        }
        val a = (alpha * 255f).toInt().coerceIn(0, 255)
        val res = (a shl 24) or rgb
        cachedCrosshairArgb = res
        return res
    }

    fun isCrosshairEnabled(context: Context): Boolean {
        cachedCrosshairEnabled?.let { return it }
        val prefs = getPrefs(context)
        val alpha = prefs.getFloat("crosshairAlpha", 0.9f)
        if (alpha <= 0.001f) {
            cachedCrosshairEnabled = false
            return false
        }
        val enabled = prefs.getBoolean("crosshairEnabled", true)
        if (!enabled) {
            cachedCrosshairEnabled = false
            return false
        }
        val crosshairStr = prefs.getString("selectedCrosshair", "Красный") ?: "Красный"
        if (crosshairStr.contains("Без", ignoreCase = true) || crosshairStr.contains("No", ignoreCase = true)) {
            cachedCrosshairEnabled = false
            return false
        }
        cachedCrosshairEnabled = true
        return true
    }

    fun getLong(context: Context, key: String, defaultValue: Long): Long {
        return getPrefs(context).getLong(key, defaultValue)
    }

    fun setLong(context: Context, key: String, value: Long) {
        getPrefs(context).edit().putLong(key, value).apply()
    }
}
