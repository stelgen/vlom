package com.stelgen.vlom.shell

import android.content.Context

/**
 * S5.2: отказоустойчивость HOME. Если VLOM падает N раз подряд — включается
 * режим восстановления (минимальный системный UI: запуск стокового лаунчера,
 * сброс настроек), чтобы пользователь не остался запертым.
 *
 * Чистая логика — StateStore-интерфейс (тестируется в JVM), Android-обвязка —
 * в RecoveryActivity/MainActivity.
 */
interface CrashStateStore {
    fun getInt(key: String, def: Int): Int
    fun putInt(key: String, value: Int)
    fun getBool(key: String, def: Boolean): Boolean
    fun putBool(key: String, value: Boolean)
}

class CrashGuard(private val store: CrashStateStore) {

    fun onCrash(threshold: Int = DEFAULT_THRESHOLD) {
        val n = store.getInt(KEY_CRASH_STREAK, 0) + 1
        store.putInt(KEY_CRASH_STREAK, n)
        if (n >= threshold) store.putBool(KEY_RECOVERY, true)
    }

    /** Успешный запуск полноценного HOME — счётчик сбрасывается. */
    fun onHealthyHome() {
        store.putInt(KEY_CRASH_STREAK, 0)
    }

    fun isRecoveryMode(): Boolean = store.getBool(KEY_RECOVERY, false)

    fun exitRecovery() {
        store.putBool(KEY_RECOVERY, false)
        store.putInt(KEY_CRASH_STREAK, 0)
    }

    companion object {
        const val DEFAULT_THRESHOLD = 2
        const val KEY_CRASH_STREAK = "crash_streak"
        const val KEY_RECOVERY = "recovery_mode"
    }
}

/** Android-обёртка: необработанный обработчик + SharedPreferences-хранилище. */
object CrashGuardInstaller {

    class PrefsStore(private val prefs: android.content.SharedPreferences) : CrashStateStore {
        override fun getInt(key: String, def: Int): Int = prefs.getInt(key, def)
        override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
        override fun getBool(key: String, def: Boolean): Boolean = prefs.getBoolean(key, def)
        override fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    }

    fun install(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val guard = CrashGuard(PrefsStore(prefs))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            guard.onCrash()
            android.util.Log.e("CrashGuard", "crash recorded, streak>=${
                prefs.getInt(CrashGuard.KEY_CRASH_STREAK, 0)
            }", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun guard(context: Context): CrashGuard =
        CrashGuard(PrefsStore(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)))

    const val PREFS = "vlom_guard"
}
