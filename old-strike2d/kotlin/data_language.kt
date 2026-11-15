package skayrux.shootout2d

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

object AppLanguageManager {
    var currentLanguage by mutableStateOf("ru")
        private set

    fun init(context: Context) {
        val saved = AppSettings.getString(context, "app_language", "")
        currentLanguage = if (saved.isNotEmpty()) {
            saved
        } else {
            val devLang = Locale.getDefault().language
            if (devLang.startsWith("en")) "en" else "ru"
        }
        applyLocale(context, currentLanguage)
    }

    fun getLanguage(context: Context): String {
        val saved = AppSettings.getString(context, "app_language", "")
        return if (saved.isNotEmpty()) saved else currentLanguage
    }

    fun setLanguage(context: Context, langCode: String) {
        AppSettings.setString(context, "app_language", langCode)
    }

    fun applyLocale(context: Context, langCode: String) {
        val targetLang = if (langCode.isNotEmpty()) langCode else "ru"
        val locale = Locale(targetLang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localeList = LocaleList(locale)
        config.setLocales(localeList)
        LocaleList.setDefault(localeList)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        val appCtx = context.applicationContext
        if (appCtx != null && appCtx != context) {
            appCtx.resources.updateConfiguration(config, appCtx.resources.displayMetrics)
        }
    }

    // получение локализованной конфигурации экрана
    fun getLocalizedConfiguration(context: Context, langCode: String, baseConfig: Configuration? = null): Configuration {
        val locale = Locale(langCode)
        val config = Configuration(baseConfig ?: context.resources.configuration)
        val wDp = maxOf(config.screenWidthDp, config.screenHeightDp)
        val hDp = minOf(config.screenWidthDp, config.screenHeightDp)
        if (wDp > 0 && hDp > 0) {
            config.screenWidthDp = wDp
            config.screenHeightDp = hDp
        }
        config.orientation = Configuration.ORIENTATION_LANDSCAPE
        config.setLocale(locale)
        val localeList = LocaleList(locale)
        config.setLocales(localeList)
        return config
    }
}

