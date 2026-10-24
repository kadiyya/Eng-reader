package com.kadiyya.engreader

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var progress: Progress
    private lateinit var mapView: MapView
    private lateinit var mapScroll: ScrollView
    private var scrolledToCurrent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress = Progress(this)
        mapView = findViewById(R.id.mapView)
        mapScroll = findViewById(R.id.mapScroll)
        mapView.onLevelClick = ::onLevelClick
        findViewById<MaterialButton>(R.id.freeModeButton).setOnClickListener { showFreeModeDialog() }
    }

    override fun onResume() {
        super.onResume()
        val total = Worlds.levels(this).size
        findViewById<TextView>(R.id.streakText).text = "🔥 ${progress.streak()}"
        findViewById<TextView>(R.id.starsText).text = "★ ${progress.totalStars()}/${total * 3}"
        findViewById<TextView>(R.id.scoreText).text = "🏆 ${progress.totalScore()}"
        mapView.setData(Worlds.all(this), progress.unlockedLevel, progress::stars)
        mapScroll.post {
            if (!scrolledToCurrent) {
                scrolledToCurrent = true
                mapScroll.scrollTo(0, (mapView.currentNodeY() - mapScroll.height / 2.5f).toInt().coerceAtLeast(0))
            }
        }
    }

    private fun onLevelClick(level: Level) {
        if (level.number > progress.unlockedLevel) {
            Toast.makeText(this, "Kilitli! Önce ${level.number - 1}. seviyeyi geç.", Toast.LENGTH_SHORT).show()
            return
        }
        val world = Worlds.world(this, level)
        val stars = progress.stars(level.number)
        val minutes = level.wordCount.toFloat() / level.wpm
        val message = buildString {
            append("${world.book}\n${world.author}\n\n")
            append("⚡ Hız: ${level.wpm} kelime/dk (giderek hızlanır)\n")
            append("📖 ${level.wordCount} kelime · yaklaşık ${"%.1f".format(minutes)} dk\n")
            append("❤️ 3 can · okunmayan her satır bir can götürür\n")
            append("🎯 Geçmek için: en az %${if (level.boss) 70 else 60} doğruluk\n")
            if (stars > 0) append("\nEn iyi: ${"★".repeat(stars)}${"☆".repeat(3 - stars)} · ${progress.bestScore(level.number)} puan")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle((if (level.boss) "👑 BOSS · " else "Seviye ${level.number} · ") + level.title)
            .setMessage(message)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Başla") { _, _ ->
                startActivity(
                    Intent(this, ReaderActivity::class.java)
                        .putExtra(ReaderActivity.EXTRA_LEVEL, level.number)
                )
            }
            .show()
    }

    private fun showFreeModeDialog() {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            hint = "İngilizce bir metin yapıştır…"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 5
            maxLines = 10
        }
        val wrapper = LinearLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(
                input,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Serbest mod (${ReaderActivity.FREE_MODE_WPM} k/dk)")
            .setView(wrapper)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Oku") { _, _ ->
                val text = input.text.toString().trim()
                if (countWords(text) < 5) {
                    Toast.makeText(this, "En az birkaç cümle gir", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(this, ReaderActivity::class.java)
                            .putExtra(ReaderActivity.EXTRA_CUSTOM_TEXT, text)
                    )
                }
            }
            .show()
    }
}
