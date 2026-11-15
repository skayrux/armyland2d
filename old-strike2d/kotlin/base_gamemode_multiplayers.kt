package skayrux.shootout2d

import android.content.Context

object BaseGamemodeMultiplayers {
    val modes = listOf(
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game_score),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.free_for_all),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.base_defence),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.capture_the_flag),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bugs_attack),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.zombie_infection),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.juggernaut),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bomb),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.arms_race),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ghost),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.battle_royale)
    )

    data class ModeItem(val typeId: Int, val name: String)

    fun getGameModeName(context: Context, typeId: Int): String {
        return when (typeId) {
            1 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game)
            2 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.free_for_all)
            3 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.base_defence)
            4 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.capture_the_flag)
            5 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bugs_attack)
            6 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game_score)
            7 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.zombie_infection)
            8 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.juggernaut)
            9 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bomb)
            10 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.arms_race)
            11 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ghost)
            12 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.battle_royale)
            else -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game)
        }
    }

    fun getGameModesList(context: Context): List<String> {
        val list = listOf(1, 6, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12)
        return list.map { getGameModeName(context, it) }
    }

    fun getGameModeItems(context: Context): List<ModeItem> {
        val list = listOf(1, 6, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12)
        return list.map { ModeItem(it, getGameModeName(context, it)) }
    }

    fun isTeamGame(typeId: Int): Boolean {
        return typeId == 1 || typeId == 3 || typeId == 4 || typeId == 6 || typeId == 9 || typeId == 10
    }

    fun isSingleBotGame(typeId: Int): Boolean {
        return typeId == 2 || typeId == 7 || typeId == 8 || typeId == 11
    }

    fun isBugsAttack(typeId: Int): Boolean {
        return typeId == 5
    }

    fun isBattleRoyale(typeId: Int): Boolean {
        return typeId == 12
    }

    fun getOfficialMapName(context: Context, mapId: Int): String {
        return when (mapId) {
            -1 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.battle_royale)
            1 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.armory_room)
            2 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.forest)
            3 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.garden)
            4 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.sands)
            5 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.survival)
            6 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.trenches)
            7 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.military_warehouses)
            8 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map8)
            9 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map9)
            10 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map10)
            11 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map11)
            12 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map12)
            13 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map13)
            14 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map14)
            15 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map15)
            16 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map16)
            else -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map_not_found)
        }
    }

    fun getOfficialMapsList(context: Context, typeId: Int = 1): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (typeId == 12) {
            list.add(Pair("official_-1", getOfficialMapName(context, -1)))
        }
        for (i in 1..16) {
            list.add(Pair("official_$i", getOfficialMapName(context, i)))
        }
        return list
    }

    const val MIN_BR_MAP_W = 50
    const val MIN_BR_MAP_H = 30

    fun isMapSuitableForMode(context: Context, typeId: Int, mapUid: String, width: Int? = null, height: Int? = null): Boolean {
        if (typeId != 12) {
            if (mapUid == "official_-1") return false
            return true
        }
        if (mapUid == "official_-1") return true
        if (mapUid.startsWith("official_")) return false
        if (width != null && height != null) {
            return width >= MIN_BR_MAP_W && height >= MIN_BR_MAP_H
        }
        val dims = getMapDimensions(context, mapUid) ?: return false
        return dims.first >= MIN_BR_MAP_W && dims.second >= MIN_BR_MAP_H
    }

    fun getMapDimensions(context: Context, uid: String): Pair<Int, Int>? {
        if (uid == "official_-1") return Pair(100, 100)
        if (uid.startsWith("official_")) return Pair(30, 20)
        val file = java.io.File(context.filesDir, "$uid.map").takeIf { it.exists() }
            ?: java.io.File(java.io.File(context.filesDir, "from_my_map"), "$uid.map").takeIf { it.exists() }
            ?: java.io.File(java.io.File(context.filesDir, "from_online_map"), "$uid.map").takeIf { it.exists() }
            ?: java.io.File(java.io.File(context.filesDir, "maps"), "$uid.map").takeIf { it.exists() }
            ?: return null
        return try {
            java.io.DataInputStream(java.io.FileInputStream(file)).use { dais ->
                dais.readByte()
                try { dais.readUTF() } catch (e: Exception) {}
                try { dais.readUTF() } catch (e: Exception) {}
                try { dais.readInt() } catch (e: Exception) {}
                try { dais.readInt() } catch (e: Exception) {}
                val w = dais.readShort().toInt()
                val h = dais.readShort().toInt()
                Pair(w, h)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getBotDifficulties(context: Context): List<String> {
        return listOf(
            skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.low),
            skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.normal),
            skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.high)
        )
    }
}
