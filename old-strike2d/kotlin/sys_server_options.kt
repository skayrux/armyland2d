package skayrux.shootout2d

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class ServerConfigData(
    val startMoney: Int = 100,
    val roundTimeMinutes: Int = 5,
    val slotWeapon0: String? = null,
    val slotWeapon1: String? = "ob_p1",
    val slotWeapon2: String? = null,
    val disabledItems: Set<String> = emptySet()
)

object ServerConfigManager {
    private const val FILE_NAME = "server_options.json"
    private var cachedData: ServerConfigData? = null

    private val slotAllowed0 = setOf(
        "ob_dr1", "ob_dr2", "ob_dr3", "ob_rifle", "ob_p90", "ob_famas",
        "ob_ak47", "ob_m16", "ob_aug", "ob_arbalet", "ob_vintorez", "ob_svd",
        "ob_osv96", "ob_minig", "ob_grl", "ob_rpg", "ob_ognemet"
    )
    private val slotAllowed1 = setOf(
        "ob_p1", "ob_p2", "ob_p3", "ob_deagle", "ob_usi", "ob_knife", "ob_axe"
    )
    private val slotAllowed2 = setOf(
        "ob_gr", "ob_oskgr", "ob_grslp", "ob_grdim", "ob_molotov",
        "ob_tur_k", "ob_sack_use", "ob_kor_use", "ob_wall_use", "ob_bug10", "ob_mina"
    )

    // нормализация и проверка границ параметров сервера
    private fun sanitize(raw: ServerConfigData): ServerConfigData {
        val validItemNames = getOrderedItems().map { it.name }.toSet()
        val clampedMoney = raw.startMoney.coerceIn(100, 25000)
        val clampedRoundTime = raw.roundTimeMinutes.coerceIn(1, 120)
        val cleanSlot0 = raw.slotWeapon0?.takeIf { it in slotAllowed0 }
        val cleanSlot1 = raw.slotWeapon1?.takeIf { it in slotAllowed1 } ?: "ob_p1"
        val cleanSlot2 = raw.slotWeapon2?.takeIf { it in slotAllowed2 }
        val cleanDisabled = raw.disabledItems.filter { it in validItemNames }.toSet()
        return ServerConfigData(
            startMoney = clampedMoney,
            roundTimeMinutes = clampedRoundTime,
            slotWeapon0 = cleanSlot0,
            slotWeapon1 = cleanSlot1,
            slotWeapon2 = cleanSlot2,
            disabledItems = cleanDisabled
        )
    }

    fun load(context: Context): ServerConfigData {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            try {
                val text = file.readText()
                val json = JSONObject(text)
                val money = json.optInt("start_money", 100)
                val roundTime = json.optInt("round_time", 5)
                val slot0 = if (json.has("slot_0") && !json.isNull("slot_0")) json.getString("slot_0").takeIf { it.isNotBlank() } else null
                val slot1 = if (json.has("slot_1") && !json.isNull("slot_1")) json.getString("slot_1").takeIf { it.isNotBlank() } ?: "ob_p1" else "ob_p1"
                val slot2 = if (json.has("slot_2") && !json.isNull("slot_2")) json.getString("slot_2").takeIf { it.isNotBlank() } else null
                val disabledSet = mutableSetOf<String>()
                val arr = json.optJSONArray("disabled_items")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val name = arr.getString(i)
                        if (name.isNotBlank()) disabledSet.add(name)
                    }
                }
                val data = sanitize(
                    ServerConfigData(
                        startMoney = money,
                        roundTimeMinutes = roundTime,
                        slotWeapon0 = slot0,
                        slotWeapon1 = slot1,
                        slotWeapon2 = slot2,
                        disabledItems = disabledSet
                    )
                )
                cachedData = data
                return data
            } catch (e: Exception) {
            }
        }
        val money = AppSettings.getInt(context, "start_money", 100)
        val roundTime = AppSettings.getInt(context, "round_time", 5)
        val allItems = getOrderedItems()
        val p1Idx = allItems.indexOfFirst { it.name == "ob_p1" }.let { if (it != -1) it else allItems.indexOfFirst { item -> item.name == "ob_axe" } }
        val s0Idx = AppSettings.getInt(context, "slot_weapon_0", -1)
        val s1Idx = AppSettings.getInt(context, "slot_weapon_1", p1Idx)
        val s2Idx = AppSettings.getInt(context, "slot_weapon_2", -1)
        val s0 = if (s0Idx in allItems.indices) allItems[s0Idx].name else null
        val s1 = if (s1Idx in allItems.indices) allItems[s1Idx].name else "ob_p1"
        val s2 = if (s2Idx in allItems.indices) allItems[s2Idx].name else null
        val disabledSet = mutableSetOf<String>()
        allItems.forEach {
            if (!AppSettings.getBoolean(context, "item_enabled_${it.name}", true)) {
                disabledSet.add(it.name)
            }
        }
        val data = sanitize(
            ServerConfigData(
                startMoney = money,
                roundTimeMinutes = roundTime,
                slotWeapon0 = s0,
                slotWeapon1 = s1,
                slotWeapon2 = s2,
                disabledItems = disabledSet
            )
        )
        cachedData = data
        return data
    }

    fun save(context: Context, rawData: ServerConfigData) {
        val data = sanitize(rawData)
        cachedData = data
        try {
            val json = JSONObject().apply {
                put("start_money", data.startMoney)
                put("round_time", data.roundTimeMinutes)
                put("slot_0", data.slotWeapon0 ?: JSONObject.NULL)
                put("slot_1", data.slotWeapon1 ?: JSONObject.NULL)
                put("slot_2", data.slotWeapon2 ?: JSONObject.NULL)
                val arr = JSONArray()
                data.disabledItems.forEach { arr.put(it) }
                put("disabled_items", arr)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json.toString(2))
        } catch (e: Exception) {
        }
        AppSettings.setInt(context, "start_money", data.startMoney)
        AppSettings.setInt(context, "round_time", data.roundTimeMinutes)
        val allItems = getOrderedItems()
        val s0Idx = if (data.slotWeapon0 != null) allItems.indexOfFirst { it.name == data.slotWeapon0 } else -1
        val s1Idx = if (data.slotWeapon1 != null) allItems.indexOfFirst { it.name == data.slotWeapon1 } else -1
        val s2Idx = if (data.slotWeapon2 != null) allItems.indexOfFirst { it.name == data.slotWeapon2 } else -1
        AppSettings.setInt(context, "slot_weapon_0", s0Idx)
        AppSettings.setInt(context, "slot_weapon_1", s1Idx)
        AppSettings.setInt(context, "slot_weapon_2", s2Idx)
        allItems.forEach {
            AppSettings.setBoolean(context, "item_enabled_${it.name}", !data.disabledItems.contains(it.name))
        }
    }

    fun isItemEnabled(name: String): Boolean {
        val disabled = cachedData?.disabledItems ?: return true
        return !disabled.contains(name)
    }

    fun isItemEnabled(context: Context, name: String): Boolean {
        if (cachedData == null) {
            load(context)
        }
        return isItemEnabled(name)
    }

    fun canBuy(context: Context, name: String): Boolean {
        return isItemEnabled(context, name)
    }

    fun getOrderedItems(): List<MapObjectData> {
        val orderedNames = listOf(
            "ob_p1", "ob_p2", "ob_p3", "ob_deagle", "ob_usi",
            "ob_dr1", "ob_dr2", "ob_dr3", "ob_rifle", "ob_p90", "ob_famas",
            "ob_ak47", "ob_m16", "ob_aug", "ob_arbalet", "ob_vintorez", "ob_svd", "ob_osv96",
            "ob_minig", "ob_grl", "ob_rpg", "ob_ognemet",
            "ob_gr", "ob_oskgr", "ob_grslp", "ob_grdim", "ob_molotov",
            "ob_knife", "ob_axe", "ob_apt", "ob_patr", "ob_armor",
            "ob_sack_use", "ob_kor_use", "ob_wall_use", "ob_tur_k", "ob_bug10", "ob_mina",
            "ob_car1", "ob_car2"
        )
        return ObjectDatabase.objects.filter { it.price != null && it.name in orderedNames }.sortedBy { item ->
            val index = orderedNames.indexOf(item.name)
            if (index != -1) index else 999
        }
    }
}

// база игровых объектов и оружия
data class MapObjectData(
    val name: String = "",
    val displayName: String = "",
    val atlasX: Int = 0,
    val atlasY: Int = 0,
    val atlasW: Int = 32,
    val atlasH: Int = 32,
    val price: Int? = null,
    val defaultSlot: Int = 0,
    val isWeapon: Boolean = true
)

object ObjectDatabase {
    val objects = listOf(
        MapObjectData("ob_p1", "P1", 983, 766, 64, 64, 100, 0),
        MapObjectData("ob_p2", "P2", 983, 833, 64, 64, 150, 0),
        MapObjectData("ob_p3", "P3", 898, 309, 70, 70, 200, 0),
        MapObjectData("ob_deagle", "DEAGLE", 898, 508, 96, 96, 350, 0),
        MapObjectData("ob_usi", "USI", 900, 699, 80, 80, 400, 0),
        MapObjectData("ob_dr1", "DR1", 894, 0, 100, 100, 500, 1),
        MapObjectData("ob_dr2", "DR2", 652, 560, 120, 120, 700, 1),
        MapObjectData("ob_dr3", "DR3", 775, 560, 120, 120, 800, 1),
        MapObjectData("ob_rifle", "RIFLE", 779, 385, 120, 120, 900, 1),
        MapObjectData("ob_ak47", "AK47", 777, 683, 120, 120, 1000, 1),
        MapObjectData("ob_arbalet", "ARBALET", 515, 429, 130, 130, 1050, 1),
        MapObjectData("ob_aug", "AUG", 509, 296, 130, 130, 1100, 1),
        MapObjectData("ob_p90", "P90", 896, 103, 100, 100, 1150, 1),
        MapObjectData("ob_m16", "M16", 642, 296, 130, 130, 1200, 1),
        MapObjectData("ob_famas", "FAMAS", 183, 971, 130, 130, 1100, 1),
        MapObjectData("ob_minig", "MINIGUN", 509, 153, 140, 140, 2000, 1),
        MapObjectData("ob_ognemet", "OGNEMET", 898, 206, 100, 100, 1500, 1),
        MapObjectData("ob_osv96", "OSV96", 203, 0, 160, 160, 1800, 1),
        MapObjectData("ob_grl", "GL", 515, 761, 128, 128, 1600, 1),
        MapObjectData("ob_rpg", "RPG", 366, 0, 150, 150, 1900, 1),
        MapObjectData("ob_svd", "SVD", 203, 469, 130, 130, 1300, 1),
        MapObjectData("ob_vintorez", "VINTOREZ", 781, 0, 110, 110, 1400, 1),
        MapObjectData("ob_gr", "FRAG GRENADE", 817, 1052, 40, 40, 150, 2),
        MapObjectData("ob_grdim", "SMOKE GRENADE", 779, 508, 40, 40, 100, 2),
        MapObjectData("ob_grslp", "FLASHBANG", 316, 1057, 40, 40, 100, 2),
        MapObjectData("ob_molotov", "MOLOTOV", 681, 1023, 80, 80, 200, 2),
        MapObjectData("ob_oskgr", "FRAG GRENADE", 316, 971, 40, 40, 250, 2),
        MapObjectData("ob_knife", "KNIFE", 598, 1023, 80, 80, 50, 1),
        MapObjectData("ob_axe", "AXE", 900, 782, 80, 80, 50, 1),
        MapObjectData("ob_apt", "Medkit", 726, 683, 40, 40, 100, 2, false),
        MapObjectData("ob_armor", "Body Armor", 1034, 1034, 64, 64, 200, 2, false),
        MapObjectData("ob_patr", "Ammo Box", 983, 699, 64, 64, 50, 2, false),
        MapObjectData("ob_sack_use", "Sandbags", 992, 607, 64, 64, 100, 2, false),
        MapObjectData("ob_kor_use", "Wooden Crate", 971, 309, 64, 64, 100, 2, false),
        MapObjectData("ob_wall_use", "Reinforced Wall", 997, 516, 64, 64, 150, 2, false),
        MapObjectData("ob_bug10", "Bug Nest", 1034, 967, 64, 64, 300, 2, false),
        MapObjectData("ob_tur_k", "Turret", 997, 449, 64, 64, 500, 2, false),
        MapObjectData("ob_mina", "Landmine", 1036, 376, 52, 52, 200, 2, false),
        MapObjectData("ob_car1", "Vehicle", 777, 806, 120, 120, 800, 2, false),
        MapObjectData("ob_car2", "Vehicle 2", 203, 163, 150, 150, 3500, 2, false),
        MapObjectData("blue_spawn", "Blue Spawn", 178, 975, 2, 2, 0, 2, false),
        MapObjectData("red_spawn", "Red Spawn", 178, 980, 2, 2, 0, 2, false)
    )
}

class SessionWorld

