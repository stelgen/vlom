package com.stelgen.vlom.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.stelgen.vlom.R
import com.stelgen.vlom.shell.CrashGuard
import com.stelgen.vlom.shell.CrashGuardInstaller

/**
 * S5.2: Recovery-экран — минимальный системный UI (никаких скинов/движка),
 * показывается, если HOME падал N раз подряд. Три кнопки:
 *  1. стоковый лаунчер (пользователь не заперт);
 *  2. сброс настроек VLOM;
 *  3. продолжить в VLOM (сброс счётчика сбоев).
 */
class RecoveryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF202020.toInt())
            setPadding(px(48), px(64), px(48), px(48))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(TextView(this).apply {
            text = getString(R.string.recovery_title)
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, px(16))
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.recovery_message)
            textSize = 18f
            setTextColor(0xFFBBBBBB.toInt())
            setPadding(0, 0, 0, px(32))
        })

        root.addView(Button(this).apply {
            text = getString(R.string.recovery_stock)
            setOnClickListener { launchStockLauncher() }
        }, LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = px(16) })

        root.addView(Button(this).apply {
            text = getString(R.string.recovery_reset)
            setOnClickListener { resetSettings() }
        }, LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = px(16) })

        root.addView(Button(this).apply {
            text = getString(R.string.recovery_continue)
            setOnClickListener {
                CrashGuardInstaller.guard(this@RecoveryActivity).exitRecovery()
                finish()
            }
        })

        setContentView(root)
    }

    /** Запуск стокового HOME: любой category=HOME кроме VLOM. */
    private fun launchStockLauncher() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val candidates = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != packageName }
        val target = candidates.firstOrNull()?.let {
            Intent().setClassName(it.activityInfo.packageName, it.activityInfo.name)
        }
        if (target != null) {
            startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CrashGuardInstaller.guard(this).exitRecovery()
            finish()
        } else {
            android.widget.Toast.makeText(this, getString(R.string.recovery_no_stock), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /** Сброс: все prefs VLOM (тайтлы/тема/погода/frigate) — приложение в состояние «из коробки». */
    private fun resetSettings() {
        (getSharedPreferences("vlom_shell", MODE_PRIVATE).edit().clear()).apply()
        getSharedPreferences("vlom_weather", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("vlom_frigate", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("vlom_guard", MODE_PRIVATE).edit().clear().apply()
        android.widget.Toast.makeText(this, getString(R.string.recovery_reset_done), android.widget.Toast.LENGTH_SHORT).show()
        CrashGuardInstaller.guard(this).exitRecovery()
    }

    private fun px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
