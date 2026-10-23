package com.kadiyya.engreader

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var progress: Progress
    private lateinit var levelContainer: LinearLayout
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress = Progress(this)
        levelContainer = findViewById(R.id.levelContainer)
        progressText = findViewById(R.id.progressText)
        findViewById<MaterialButton>(R.id.freeModeButton).setOnClickListener { showFreeModeDialog() }
    }

    override fun onResume() {
        super.onResume()
        renderLevels()
    }

    private fun renderLevels() {
        levelContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val unlocked = progress.unlockedLevel
        progressText.text = "İlerleme: ${progress.completedCount()} / ${Levels.all.size} seviye tamamlandı"

        for (level in Levels.all) {
            val item = inflater.inflate(R.layout.item_level, levelContainer, false)
            val isLocked = level.number > unlocked
            item.findViewById<TextView>(R.id.levelNumber).text = level.number.toString()
            item.findViewById<TextView>(R.id.levelTitle).text = level.title
            item.findViewById<TextView>(R.id.levelMeta).text =
                "${level.stage} · ${level.wpm} kelime/dk · ${level.wordCount} kelime"
            item.findViewById<TextView>(R.id.levelStatus).text = when {
                isLocked -> "🔒"
                progress.isCompleted(level.number) -> "★".repeat(progress.stars(level.number)).ifEmpty { "✓" }
                else -> "▶"
            }
            if (isLocked) {
                item.setBackgroundResource(R.drawable.card_locked_bg)
                item.alpha = 0.55f
                item.setOnClickListener {
                    Toast.makeText(this, "Önce önceki seviyeyi tamamla", Toast.LENGTH_SHORT).show()
                }
            } else {
                item.setOnClickListener {
                    startActivity(
                        Intent(this, ReaderActivity::class.java)
                            .putExtra(ReaderActivity.EXTRA_LEVEL, level.number)
                    )
                }
            }
            levelContainer.addView(item)
        }
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
            .setTitle("Serbest mod")
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
