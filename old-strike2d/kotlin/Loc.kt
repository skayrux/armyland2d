package skayrux.shootout2d

import android.content.Context

object Loc {
    var appContext: Context? = null
    
    fun tr(resId: Int, vararg args: Any): String {
        val context = appContext ?: return "???"
        return try {
            if (args.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *args)
            }
        } catch (e: Exception) {
            "???"
        }
    }
}
