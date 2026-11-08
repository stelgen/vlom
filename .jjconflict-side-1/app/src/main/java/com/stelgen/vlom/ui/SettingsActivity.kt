package com.stelgen.vlom.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.stelgen.vlom.R
import com.stelgen.vlom.shell.AppRepository
import com.stelgen.vlom.shell.BuiltInTiles
import com.stelgen.vlom.shell.HomeTile
import com.stelgen.vlom.shell.ThemeManager
import com.stelgen.vlom.shell.TileIconProvider
import com.stelgen.vlom.shell.TileRepository
import com.stelgen.vlom.shell.SharedPreferencesTileStore

/**
 * S2.4 / S2.2: настройки v1 в стиле MCE — управление тайтлами:
 * скрытие/показ, пересортировка (вверх/вниз), добавление приложений.
 * D-pad: UP/DOWN — выбор строки, OK — действия над тайтлом.
 */
class SettingsActivity : Activity() {

    private lateinit var store: SharedPreferencesTileStore
    private lateinit var settings: com.stelgen.vlom.shell.TileSettings
    private lateinit var adapter: TileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = SharedPreferencesTileStore(this)
        val apps = AppRepository(this).launchableApps()
        settings = TileRepository.load(store, apps.map { it.packageName }.toSet())

        val themeBg = ThemeManager.palette(this).backgroundBottom

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(themeBg)
            setPadding(px(48), px(36), px(48), px(36))
        }

        val title = TextView(this).apply {
            text = getString(R.string.settings_title)
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = FontLoader.get(context, FontLoader.Style.SEMIBOLD)
            setPadding(0, 0, 0, px(24))
        }
        root.addView(title)

        val addBtn = Button(this).apply {
            text = getString(R.string.settings_add_app)
            textSize = 20f
            setOnClickListener { startActivity(Intent(this@SettingsActivity, AddAppActivity::class.java)) }
        }
        root.addView(addBtn)

        // S4.3: выбор темы Blue/Black/Green
        val themeBtn = Button(this).apply {
            text = getString(R.string.settings_theme, ThemeManager.current(context).label)
            textSize = 20f
            setOnClickListener {
                val next = ThemeManager.Theme.entries
                    .let { list -> list[(list.indexOf(ThemeManager.current(context)) + 1) % list.size] }
                ThemeManager.set(context, next)
                text = getString(R.string.settings_theme, next.label)
                root.setBackgroundColor(ThemeManager.palette(context).backgroundBottom)
            }
        }
        root.addView(themeBtn)

        val listView = ListView(this).apply {
            adapter = TileAdapter().also { adapter = it }
            setDivider(null)
        }
        root.addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        val doneBtn = Button(this).apply {
            text = getString(R.string.settings_done)
            textSize = 20f
            setOnClickListener {
                store.save(settings)
                setResult(RESULT_OK)
                finish()
            }
        }
        root.addView(doneBtn)

        setContentView(root)
    }

    private inner class TileAdapter : BaseAdapter() {
        override fun getCount(): Int = settings.tiles.size
        override fun getItem(position: Int): HomeTile = settings.tiles[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tile = getItem(position)
            val row = convertView as? TextView ?: TextView(this@SettingsActivity).apply {
                textSize = 22f
                typeface = FontLoader.get(context)
                gravity = Gravity.CENTER_VERTICAL
                minHeight = px(72)
                setPadding(px(16), 0, px(16), 0)
            }
            row.text = buildString {
                append(tile.label)
                if (tile.hidden) append("  · скрыт")
                if (!tile.builtIn) append("  · приложение")
            }
            row.setTextColor(if (tile.hidden) 0xFF8899AA.toInt() else 0xFFFFFFFF.toInt())
            row.setOnClickListener { showActions(position) }
            return row
        }
    }

    private fun showActions(position: Int) {
        val tile = settings.tiles[position]
        val options = buildList {
            if (position > 0) add("Переместить вверх")
            if (position < settings.tiles.size - 1) add("Переместить вниз")
            add(if (tile.hidden) "Показать" else "Скрыть")
            if (!tile.builtIn) add("Удалить")
        }
        AlertDialog.Builder(this)
            .setTitle(tile.label)
            .setItems(options.toTypedArray()) { _, which ->
                val action = options[which]
                settings = when (action) {
                    "Переместить вверх" -> settings.move(position, position - 1)
                    "Переместить вниз" -> settings.move(position, position + 1)
                    "Показать" -> settings.setHidden(tile.id, false)
                    "Скрыть" -> settings.setHidden(tile.id, true)
                    "Удалить" -> settings.remove(tile.id)
                    else -> settings
                }
                store.save(settings)
                adapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
