package com.kadiyya.engreader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs
import kotlin.math.roundToInt

class ReaderActivity : AppCompatActivity(), Choreographer.FrameCallback {

    private enum class State { IDLE, COUNTDOWN, PLAYING, PAUSED, FINISHED }

    private var level: Level? = null
    private lateinit var text: String
    private var wordCount = 0
    private var wpm = DEFAULT_WPM

    private lateinit var scrollView: ScrollView
    private lateinit var prompterText: TextView
    private lateinit var guideBand: View
    private lateinit var countdownText: TextView
    private lateinit var timerText: TextView
    private lateinit var wpmText: TextView
    private lateinit var hintText: TextView
    private lateinit var heardText: TextView
    private lateinit var micLabel: TextView
    private lateinit var micLevel: ProgressBar
    private lateinit var readProgress: ProgressBar
    private lateinit var playPauseButton: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var state = State.IDLE
    private var position = 0f
    private var lastFrameNanos = 0L
    private var elapsedMs = 0.0
    private var layoutReady = false

    private var tracker: SpeechTracker? = null
    private var lastLoudAt = 0L

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) setupSpeech() else showMicOff("Mikrofon izni yok")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val levelNumber = intent.getIntExtra(EXTRA_LEVEL, 0)
        level = Levels.all.firstOrNull { it.number == levelNumber }
        text = level?.text ?: intent.getStringExtra(EXTRA_CUSTOM_TEXT).orEmpty()
        wordCount = countWords(text).coerceAtLeast(1)
        wpm = level?.wpm ?: DEFAULT_WPM

        scrollView = findViewById(R.id.scrollView)
        prompterText = findViewById(R.id.prompterText)
        guideBand = findViewById(R.id.guideBand)
        countdownText = findViewById(R.id.countdownText)
        timerText = findViewById(R.id.timerText)
        wpmText = findViewById(R.id.wpmText)
        hintText = findViewById(R.id.hintText)
        heardText = findViewById(R.id.heardText)
        micLabel = findViewById(R.id.micLabel)
        micLevel = findViewById(R.id.micLevel)
        readProgress = findViewById(R.id.readProgress)
        playPauseButton = findViewById(R.id.playPauseButton)

        findViewById<TextView>(R.id.titleText).text =
            level?.let { "Seviye ${it.number} · ${it.title}" } ?: "Serbest mod"
        prompterText.text = text
        updateWpmText()

        findViewById<MaterialButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.slowerButton).setOnClickListener { changeWpm(-WPM_STEP) }
        findViewById<MaterialButton>(R.id.fasterButton).setOnClickListener { changeWpm(WPM_STEP) }
        playPauseButton.setOnClickListener { onPlayPause() }

        findViewById<FrameLayout>(R.id.prompterArea).post { configurePrompterLayout() }

        when {
            !SpeechTracker.isAvailable(this) -> showMicOff("Ses tanıma yok")
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> setupSpeech()
            else -> requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /** Places the reading band and pads the text so it starts on, and can scroll fully past, the band. */
    private fun configurePrompterLayout() {
        val height = scrollView.height
        val guideY = (height * GUIDE_POSITION).toInt()
        val lineHeight = prompterText.lineHeight
        val bandPad = (6 * resources.displayMetrics.density).toInt()

        prompterText.setPadding(
            prompterText.paddingLeft, guideY,
            prompterText.paddingRight, height - guideY,
        )
        guideBand.layoutParams = guideBand.layoutParams.apply { this.height = lineHeight + bandPad * 2 }
        guideBand.translationY = (guideY - bandPad).toFloat()
        layoutReady = true
    }

    private fun setupSpeech() {
        tracker = SpeechTracker(
            this,
            onLevel = { value ->
                micLevel.progress = (value * 100).roundToInt()
                if (value > LOUD_THRESHOLD) lastLoudAt = SystemClock.elapsedRealtime()
            },
            onWordsChanged = { count -> heardText.text = "Duyulan: $count" },
        )
        heardText.text = "Duyulan: 0"
    }

    private fun showMicOff(reason: String) {
        tracker = null
        micLabel.text = "🔇"
        micLevel.visibility = View.INVISIBLE
        heardText.text = reason
    }

    private fun contentHeight(): Int = prompterText.layout?.height ?: 0

    private fun pixelsPerSecond(): Float =
        contentHeight().toFloat() / wordCount * wpm / 60f

    private fun changeWpm(delta: Int) {
        wpm = (wpm + delta).coerceIn(MIN_WPM, MAX_WPM)
        updateWpmText()
    }

    private fun updateWpmText() {
        wpmText.text = "$wpm k/dk"
    }

    private fun onPlayPause() {
        when (state) {
            State.IDLE, State.FINISHED -> startCountdown()
            State.COUNTDOWN -> Unit
            State.PLAYING -> pause()
            State.PAUSED -> resume()
        }
    }

    private fun startCountdown() {
        if (!layoutReady) return
        position = 0f
        elapsedMs = 0.0
        scrollView.scrollTo(0, 0)
        readProgress.progress = 0
        updateTimer()
        tracker?.reset()
        tracker?.start()

        state = State.COUNTDOWN
        playPauseButton.text = "Hazır ol"
        countdownText.visibility = View.VISIBLE
        val steps = listOf("3", "2", "1", "GO!")
        steps.forEachIndexed { i, label ->
            handler.postDelayed({
                if (state != State.COUNTDOWN) return@postDelayed
                countdownText.text = label
                countdownText.alpha = 1f
                countdownText.animate().alpha(0.2f).setDuration(800).start()
            }, i * 900L)
        }
        handler.postDelayed({
            if (state != State.COUNTDOWN) return@postDelayed
            countdownText.visibility = View.GONE
            beginPlaying()
        }, steps.size * 900L - 300L)
    }

    private fun beginPlaying() {
        state = State.PLAYING
        playPauseButton.text = "Duraklat"
        lastLoudAt = SystemClock.elapsedRealtime()
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun pause() {
        state = State.PAUSED
        playPauseButton.text = "Devam"
        tracker?.stop()
        Choreographer.getInstance().removeFrameCallback(this)
    }

    private fun resume() {
        tracker?.start()
        beginPlaying()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (state != State.PLAYING) return
        if (lastFrameNanos != 0L) {
            val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000.0).coerceAtMost(0.1)
            // If the user dragged the text, continue from where they left it.
            if (abs(scrollView.scrollY - position) > 3f) position = scrollView.scrollY.toFloat()
            position += (pixelsPerSecond() * dt).toFloat()
            scrollView.scrollTo(0, position.toInt())
            elapsedMs += dt * 1000.0
            updateTimer()
            updateSilenceHint()

            val total = contentHeight()
            if (total > 0) {
                readProgress.progress = (position / total * 1000).toInt().coerceIn(0, 1000)
                if (position >= total) {
                    onFinished()
                    return
                }
            }
        }
        lastFrameNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun updateTimer() {
        val seconds = (elapsedMs / 1000).toInt()
        timerText.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun updateSilenceHint() {
        if (tracker == null) return
        val silent = SystemClock.elapsedRealtime() - lastLoudAt > SILENCE_WARNING_MS
        if (silent) {
            hintText.text = "Sesini duyamıyorum, yüksek sesle oku! 🔊"
            hintText.setTextColor(ContextCompat.getColor(this, R.color.bad))
        } else {
            hintText.text = "Sarı banttaki satırı yüksek sesle oku!"
            hintText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun onFinished() {
        state = State.FINISHED
        playPauseButton.text = "Tekrar"
        tracker?.stop()
        readProgress.progress = 1000

        val minutes = elapsedMs / 60_000.0
        val actualWpm = if (minutes > 0) (wordCount / minutes).roundToInt() else 0
        val accuracy = tracker?.let { accuracyPercent(text, it.heardWords) }
        val result = evaluate(actualWpm, accuracy)

        level?.let { if (result.passed) Progress(this).saveResult(it.number, result.stars) }
        showResultDialog(actualWpm, accuracy, result)
    }

    private data class Result(val passed: Boolean, val stars: Int, val message: String)

    private fun evaluate(actualWpm: Int, accuracy: Int?): Result {
        val target = level?.wpm ?: wpm
        val speedRatio = actualWpm.toFloat() / target
        if (speedRatio < 0.9f) {
            return Result(false, 0, "Hızı biraz düşürdün. Seviyeyi geçmek için en az ${(target * 0.9f).roundToInt()} k/dk ile bitir.")
        }
        if (accuracy != null && accuracy < PASS_ACCURACY) {
            return Result(false, 0, "Metnin yeterince duyulmadı. Daha net ve yüksek sesle okumayı dene.")
        }
        val stars = if (accuracy != null) {
            when {
                accuracy >= 85 && speedRatio >= 1f -> 3
                accuracy >= 70 -> 2
                else -> 1
            }
        } else {
            when {
                speedRatio >= 1.15f -> 3
                speedRatio >= 1f -> 2
                else -> 1
            }
        }
        val message = when (stars) {
            3 -> "Mükemmel! Akıcı ve hızlı okudun."
            2 -> "Çok iyi! Bir daha okuyarak 3 yıldızı yakala."
            else -> "Geçtin! Tekrar ederek daha akıcı hale getir."
        }
        return Result(true, stars, message)
    }

    private fun showResultDialog(actualWpm: Int, accuracy: Int?, result: Result) {
        val seconds = (elapsedMs / 1000).toInt()
        val lines = buildString {
            if (level != null) {
                append(if (result.passed) "★".repeat(result.stars) + "\n\n" else "Seviye geçilemedi\n\n")
            }
            append("Süre: ${seconds / 60} dk ${seconds % 60} sn\n")
            append("Ortalama hız: $actualWpm kelime/dk\n")
            append(
                if (accuracy != null) "Anlaşılan kelimeler: %$accuracy\n"
                else "Anlaşılan kelimeler: ölçülemedi (mikrofon kapalı)\n"
            )
            if (level != null) append("\n${result.message}")
        }

        val nextLevel = level?.let { current -> Levels.all.firstOrNull { it.number == current.number + 1 } }
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(if (level == null || result.passed) "Tebrikler!" else "Bir daha dene")
            .setMessage(lines)
            .setCancelable(false)
            .setNegativeButton("Menü") { _, _ -> finish() }
            .setNeutralButton("Tekrar") { _, _ -> startCountdown() }
        if (result.passed && nextLevel != null) {
            builder.setPositiveButton("Sonraki seviye") { _, _ ->
                startActivity(
                    Intent(this, ReaderActivity::class.java).putExtra(EXTRA_LEVEL, nextLevel.number)
                )
                finish()
            }
        }
        builder.show()
    }

    override fun onPause() {
        super.onPause()
        when (state) {
            State.PLAYING -> pause()
            State.COUNTDOWN -> {
                handler.removeCallbacksAndMessages(null)
                countdownText.visibility = View.GONE
                tracker?.stop()
                state = State.IDLE
                playPauseButton.text = "Başla"
            }
            else -> Unit
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        Choreographer.getInstance().removeFrameCallback(this)
        tracker?.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_LEVEL = "level"
        const val EXTRA_CUSTOM_TEXT = "custom_text"

        private const val DEFAULT_WPM = 140
        private const val MIN_WPM = 60
        private const val MAX_WPM = 300
        private const val WPM_STEP = 10
        private const val GUIDE_POSITION = 0.3f
        private const val PASS_ACCURACY = 50
        private const val LOUD_THRESHOLD = 0.45f
        private const val SILENCE_WARNING_MS = 4000L
    }
}
