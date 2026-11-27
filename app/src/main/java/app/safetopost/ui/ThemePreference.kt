package app.safetopost.ui

import android.content.Context

object ThemePreference {
    private const val PREFS = "safe_to_post"
    private const val KEY = "palette"

    fun load(context: Context): AppPalette {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return runCatching { AppPalette.valueOf(name ?: "") }.getOrDefault(AppPalette.EMBER)
    }

    fun save(context: Context, palette: AppPalette) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, palette.name)
            .apply()
    }
}
