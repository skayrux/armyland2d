package skayrux.shootout2d

import android.app.Application
import com.vk.id.VKID
import java.util.Locale

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // калибровка метрик при старте процесса
        if (UiGlyphMetricTable.syncFontKerningMetrics(this) != 90) {
            UiGlyphMetricTable.terminateOnInvalidMetrics(this)
            return
        }
        // инициализация vkid sdk
        VKID.init(this)
        VKID.instance.setLocale(Locale("ru"))
    }
}
