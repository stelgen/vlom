package com.stelgen.vlom.ui

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.stelgen.vlom.R
import com.stelgen.vlom.shell.AppRepository
import com.stelgen.vlom.shell.SharedPreferencesTileStore
import com.stelgen.vlom.shell.TileRepository
import com.stelgen.vlom.shell.TileSettings

/**
 * S2.2 (T3): добавление приложения отдельным тайтлом — список установленных
 * APK (кроме системных), клик = добавить тайтл с автоиконкой.
 */
class AddAppActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = SharedPreferencesTileStore(this)
        val apps = AppRepository(this).launchableApps()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF06142E.toInt())
            setPadding(px(48), px(36), px(48), px(36))
        }

        val title = TextView(this).apply {
            text = "Добавить приложение"
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = FontLoader.get(context, FontLoader.Style.SEMIBOLD)
            setPadding(0, 0, 0, px(24))
        }
        root.addView(title)

        val addSite = TextView(this).apply {
            text = getString(R.string.add_site)
            textSize = 22f
            setTextColor(0xFF5B8FD9.toInt())
            typeface = FontLoader.get(context, FontLoader.Style.SEMIBOLD)
            setPadding(0, 0, 0, px(16))
            setOnClickListener { promptSite() }
        }
        root.addView(addSite)

        val listView = ListView(this)
        listView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = apps.size
            override fun getItem(position: Int): AppRepository.InstalledApp = apps[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val app = getItem(position)
                val row = convertView as? TextView ?: TextView(this@AddAppActivity).apply {
                    textSize = 22f
                    typeface = FontLoader.get(context)
                    gravity = Gravity.CENTER_VERTICAL
                    minHeight = px(72)
                    setPadding(px(16), 0, px(16), 0)
                }
                row.text = app.label
                row.setTextColor(0xFFFFFFFF.toInt())
                row.setOnClickListener {
                    addApp(app)
                }
                return row
            }
        }
        root.addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        setContentView(root)
    }

    /** S3.3: веб-тайтл — произвольный сайт. */
    private fun promptSite() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.site_url_hint)
            setSingleLine(true)
        }
        AlertDialogs.confirm(this, getString(R.string.add_site_title), input) {
            val raw = input.text.toString().trim()
            val url = com.stelgen.vlom.ui.FrigateActivity.normalizeUrl(raw)
            if (url != null) {
                val store = SharedPreferencesTileStore(this)
                val installed = AppRepository(this).launchableApps().map { it.packageName }.toSet()
                val current = TileRepository.load(store, installed)
                val label = java.net.URI(url).host ?: url
                store.save(current.addWebsite("web.$label", label, url))
            }
            finish()
        }
    }

    private fun addApp(app: AppRepository.InstalledApp) {
        val store = SharedPreferencesTileStore(this)
        val installed = AppRepository(this).launchableApps().map { it.packageName }.toSet()
        val current = TileRepository.load(store, installed)
        val id = "app.${app.packageName}"
        val updated = current.addApp(id, app.label, app.packageName, app.activityName)
        store.save(updated)
        finish()
    }

    private fun px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
