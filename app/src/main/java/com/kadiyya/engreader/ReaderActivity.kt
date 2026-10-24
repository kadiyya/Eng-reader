package com.kadiyya.engreader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.style.ForegroundColorSpan
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
import kotlin.math.roundToInt

/**
 * The prompter. Text scrolls up at the level's fixed, slowly rising speed;
 * the player reads aloud, heard words turn green, and every line that passes
 * the band mostly unread costs a heart.
 */
class ReaderActivity : AppCompatActivity(), Choreographer.FrameCallback {

    private enum class State { IDLE, COUNTDOWN, PLAYING, PAUSED, FINISHING, OVER }

    private var level: Level? = null
    private lateinit var text: String
    private lateinit var tokens: List<Token>
    private lateinit var aligner: WordAligner
    private var baseWpm = FREE_MODE_WPM

    private lateinit var scrollView: ScrollView
    private lateinit var prompterText: TextView
    private lateinit var guideBand: View
    private lateinit var countdownText: TextView
    private lateinit var timerText: TextView
    private lateinit var wpmText: TextView
    private lateinit var hintText: TextView
    private lateinit var heardText: TextView
    private lateinit var heartsText: TextView
    private lateinit var comboText: TextView
    private lateinit var scoreText: TextView
    private lateinit var micLabel: TextView
    private lateinit var micLevel: ProgressBar
    private lateinit var readProgress: ProgressBar
    private lateinit var playPauseButton: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var state = State.IDLE
    private var position = 0f
    private var lastFrameNanos = 0L
    private var lastSpanRefresh = 0L
    private var elapsedMs = 0.0
    private var layoutReady = false

    // Line index of every token, filled in once the text is laid out.
    private var tokenLine = IntArray(0)
    private var tokenState = IntArray(0)
    private var tokenSpan = arrayOfNulls<ForegroundColorSpan>(0)
    private var nextLineToScore = 0

    private var hearts = MAX_HEARTS
    private var goodLines = 0
    private var score = 0

    private var tracker: SpeechTracker? = null
    private var lastLoudAt = 0L

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) setupSpeech() else showMicOff("Mikrofon izni yok · pratik modu")
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        level = Worlds.level(this, intent.getIntExtra(EXTRA_LEVEL, 0))
        text = level?.text ?: intent.getStringExtra(EXTRA_CUSTOM_TEXT).orEmpty()
        tokens = tokenize(text)
        aligner = WordAligner(tokens)
        baseWpm = level?.wpm ?: FREE_MODE_WPM

        scrollView = findViewById(R.id.scrollView)
        prompterText = findViewById(R.id.prompterText)
        guideBand = findViewById(R.id.guideBand)
        countdownText = findViewById(R.id.countdownText)
        timerText = findViewById(R.id.timerText)
        wpmText = findViewById(R.id.wpmText)
        hintText = findViewById(R.id.hintText)
        heardText = findViewById(R.id.heardText)
        heartsText = findViewById(R.id.heartsText)
        comboText = findViewById(R.id.comboText)
        scoreText = findViewById(R.id.scoreText)
        micLabel = findViewById(R.id.micLabel)
        micLevel = findViewById(R.id.micLevel)
        readProgress = findViewById(R.id.readProgress)
        playPauseButton = findViewById(R.id.playPauseButton)

        findViewById<TextView>(R.id.titleText).text = level?.let {
            (if (it.boss) "👑 " else "${it.number}. ") + it.title
        } ?: "Serbest mod"
        prompterText.setText(text, TextView.BufferType.SPANNABLE)
        // The prompter sets the pace: no dragging the text.
        scrollView.setOnTouchListener { _, _ -> true }
        updateHud()
        updateWpmText(baseWpm * START_SPEED)

        findViewById<MaterialButton>(R.id.backButton).setOnClickListener { finish() }
        playPauseButton.setOnClickListener { onPlayPause() }

        findViewById<FrameLayout>(R.id.prompterArea).post { configurePrompterLayout() }

        when {
            !SpeechTracker.isAvailable(this) -> showMicOff("Ses tanıma yok · pratik modu")
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
        prompterText.post {
            val layout = prompterText.layout ?: return@post
            tokenLine = IntArray(tokens.size) { layout.getLineForOffset(tokens[it].start) }
            tokenState = IntArray(tokens.size)
            tokenSpan = arrayOfNulls(tokens.size)
            layoutReady = true
        }
    }

    private fun setupSpeech() {
        tracker = SpeechTracker(
            this,
            onLevel = { value ->
                micLevel.progress = (value * 100).roundToInt()
                if (value > LOUD_THRESHOLD) lastLoudAt = SystemClock.elapsedRealtime()
            },
            onPartial = { words -> onHeard { aligner.preview(words, bandTokenIndex()) } },
            onFinal = { words -> onHeard { aligner.commit(words, bandTokenIndex()) } },
        )
        updateHud()
    }

    private fun onHeard(update: () -> Unit) {
        if (state == State.IDLE || state == State.OVER) return
        update()
        refreshSpans()
        updateHud()
    }

    private fun showMicOff(reason: String) {
        tracker = null
        micLabel.text = "🔇"
        micLevel.visibility = View.INVISIBLE
        hintText.text = reason
        updateHud()
    }

    private fun contentHeight(): Int = prompterText.layout?.height ?: 0

    private fun progressFraction(): Float {
        val total = contentHeight()
        return if (total > 0) (position / total).coerceIn(0f, 1f) else 0f
    }

    /** The speed rises from 90% to 110% of the level's pace as the text goes on. */
    private fun currentWpm(): Float = baseWpm * (START_SPEED + (END_SPEED - START_SPEED) * progressFraction())

    private fun pixelsPerSecond(): Float =
        contentHeight().toFloat() / tokens.size.coerceAtLeast(1) * currentWpm() / 60f

    private fun bandLine(): Int {
        val layout = prompterText.layout ?: return 0
        return layout.getLineForVertical(position.toInt() + prompterText.lineHeight / 2)
    }

    private fun bandTokenIndex(): Int {
        if (!layoutReady) return 0
        val line = bandLine()
        val index = tokenLine.indexOfFirst { it >= line }
        return if (index < 0) tokens.size else index
    }

    private fun updateWpmText(wpm: Float) {
        wpmText.text = "⚡ ${wpm.roundToInt()} k/dk"
    }

    private fun updateHud() {
        if (tracker == null) {
            heartsText.text = "Pratik"
            comboText.text = ""
            scoreText.text = ""
            heardText.text = ""
            return
        }
        heartsText.text = "❤️".repeat(hearts) + "🖤".repeat(MAX_HEARTS - hearts)
        val multiplier = comboMultiplier()
        comboText.text = if (multiplier > 1) "🔥 Kombo x$multiplier" else ""
        scoreText.text = "$score"
        heardText.text = "✓ ${aligner.matchedCount}/${tokens.size}"
    }

    private fun comboMultiplier(): Int = minOf(1 + goodLines / 2, MAX_MULTIPLIER)

    private fun onPlayPause() {
        when (state) {
            State.IDLE, State.OVER -> startCountdown()
            State.COUNTDOWN, State.FINISHING -> Unit
            State.PLAYING -> pause()
            State.PAUSED -> resume()
        }
    }

    private fun resetRun() {
        position = 0f
        elapsedMs = 0.0
        hearts = MAX_HEARTS
        goodLines = 0
        score = 0
        nextLineToScore = 0
        aligner.reset()
        scrollView.scrollTo(0, 0)
        readProgress.progress = 0
        refreshSpans()
        updateTimer()
        updateHud()
        updateWpmText(currentWpm())
    }

    private fun startCountdown() {
        if (!layoutReady) return
        resetRun()
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
            position += (pixelsPerSecond() * dt).toFloat()
            scrollView.scrollTo(0, position.toInt())
            elapsedMs += dt * 1000.0
            updateTimer()
            updateSilenceHint()

            val now = SystemClock.elapsedRealtime()
            if (now - lastSpanRefresh > SPAN_REFRESH_MS) {
                lastSpanRefresh = now
                updateWpmText(currentWpm())
                refreshSpans()
                scorePassedLines(bandLine() - SCORE_LAG_LINES)
                if (state != State.PLAYING) return
            }

            val total = contentHeight()
            if (total > 0) {
                readProgress.progress = (progressFraction() * 1000).toInt()
                if (position >= total) {
                    onTextEnd()
                    return
                }
            }
        }
        lastFrameNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    /** Colors heard words green and words left behind unread red. */
    private fun refreshSpans() {
        if (!layoutReady) return
        val spannable = prompterText.text as? Spannable ?: return
        val missedBefore = if (state == State.IDLE) -1 else bandLine() - 1
        val matched = aligner.matched
        for (i in tokens.indices) {
            val desired = when {
                matched[i] -> STATE_HEARD
                tracker != null && tokenLine[i] < missedBefore -> STATE_MISSED
                else -> STATE_PLAIN
            }
            if (desired == tokenState[i]) continue
            tokenSpan[i]?.let { spannable.removeSpan(it) }
            tokenSpan[i] = when (desired) {
                STATE_HEARD -> ForegroundColorSpan(ContextCompat.getColor(this, R.color.good))
                STATE_MISSED -> ForegroundColorSpan(ContextCompat.getColor(this, R.color.missed))
                else -> null
            }?.also { spannable.setSpan(it, tokens[i].start, tokens[i].end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
            tokenState[i] = desired
        }
    }

    /** Scores every line up to [lastLine] that has not been scored yet. */
    private fun scorePassedLines(lastLine: Int) {
        if (tracker == null || !layoutReady) return
        val upTo = minOf(lastLine, tokenLine.lastOrNull() ?: -1)
        while (nextLineToScore <= upTo && state != State.OVER) {
            scoreLine(nextLineToScore++)
        }
    }

    private fun scoreLine(line: Int) {
        val indices = tokens.indices.filter { tokenLine[it] == line }
        if (indices.isEmpty()) return
        val heard = indices.count { aligner.matched[it] }
        val ratio = heard.toFloat() / indices.size
        when {
            indices.size >= MIN_WORDS_FOR_HEART && ratio < MISS_RATIO -> {
                goodLines = 0
                loseHeart()
            }
            ratio >= GOOD_RATIO -> goodLines++
            else -> goodLines = 0
        }
        score += heard * POINTS_PER_WORD * comboMultiplier()
        updateHud()
    }

    private fun loseHeart() {
        hearts--
        updateHud()
        vibrate()
        guideBand.setBackgroundResource(R.drawable.guide_band_hit)
        handler.postDelayed({ guideBand.setBackgroundResource(R.drawable.guide_band) }, 450)
        if (hearts <= 0) gameOver()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
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

    /** The last line has passed the band; give the recognizer a moment to catch up, then score. */
    private fun onTextEnd() {
        state = State.FINISHING
        playPauseButton.text = "…"
        readProgress.progress = 1000
        handler.postDelayed({
            if (state != State.FINISHING) return@postDelayed
            tracker?.stop()
            refreshSpans()
            scorePassedLines(Int.MAX_VALUE)
            if (state == State.FINISHING) onFinished()
        }, FINISH_GRACE_MS)
    }

    private fun gameOver() {
        state = State.OVER
        playPauseButton.text = "Tekrar"
        tracker?.stop()
        Choreographer.getInstance().removeFrameCallback(this)
        if (isFinishing) return
        MaterialAlertDialogBuilder(this)
            .setTitle("💀 Canın bitti!")
            .setMessage(
                "Prompter senden hızlıydı.\n\n" +
                    "İlerleme: %${(progressFraction() * 100).roundToInt()}\n" +
                    "Puan: $score\n\n" +
                    "İpucu: Takılsan bile durma, bandın üzerindeki satıra atla ve okumaya devam et."
            )
            .setCancelable(false)
            .setNegativeButton("Harita") { _, _ -> finish() }
            .setPositiveButton("Tekrar dene") { _, _ -> startCountdown() }
            .show()
    }

    private data class Result(val passed: Boolean, val stars: Int, val accuracy: Int?, val finalScore: Int)

    private fun evaluate(): Result {
        if (tracker == null) return Result(true, 1, null, 0)
        val accuracy = aligner.matchedCount * 100 / tokens.size.coerceAtLeast(1)
        val needed = if (level?.boss == true) BOSS_PASS_ACCURACY else PASS_ACCURACY
        if (accuracy < needed) return Result(false, 0, accuracy, score)
        val stars = when {
            accuracy >= 90 && hearts == MAX_HEARTS -> 3
            accuracy >= 78 -> 2
            else -> 1
        }
        return Result(true, stars, accuracy, score + hearts * HEART_BONUS)
    }

    private fun onFinished() {
        state = State.OVER
        playPauseButton.text = "Tekrar"
        val result = evaluate()
        val current = level
        val isRecord = current != null && result.passed &&
            Progress(this).savePass(current.number, result.stars, result.finalScore)
        if (!isFinishing) showResultDialog(result, isRecord)
    }

    private fun showResultDialog(result: Result, isRecord: Boolean) {
        val current = level
        val needed = if (current?.boss == true) BOSS_PASS_ACCURACY else PASS_ACCURACY
        val message = buildString {
            if (result.passed) {
                append("★".repeat(result.stars) + "☆".repeat(3 - result.stars) + "\n\n")
            }
            if (result.accuracy != null) {
                append("🎯 Doğruluk: %${result.accuracy}")
                if (!result.passed) append(" (gereken %$needed)")
                append("\n❤️ Kalan can: $hearts/$MAX_HEARTS\n")
                append("🏆 Puan: ${result.finalScore}")
                if (result.passed) append(" (can bonusu +${hearts * HEART_BONUS})")
                append("\n")
                if (isRecord) append("\n✨ Yeni rekor!\n")
            } else {
                append("Mikrofon kapalı olduğu için doğruluk ölçülemedi. Seviye 1 yıldızla geçildi.\n")
            }
            if (result.passed && result.stars < 3 && result.accuracy != null) {
                append("\n3 yıldız için: %90 doğruluk ve hiç can kaybetmemek.")
            }
            if (!result.passed) append("\nDaha net ve yüksek sesle okumayı dene.")
        }

        val levels = Worlds.levels(this)
        val next = current?.let { c -> levels.firstOrNull { it.number == c.number + 1 } }
        val newWorld = next != null && current?.boss == true
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(
                when {
                    !result.passed -> "Bir daha dene"
                    current?.boss == true -> "👑 Boss yenildi!"
                    else -> "Tebrikler!"
                }
            )
            .setMessage(if (newWorld) "$message\n\n🗺️ Yeni dünya açıldı: ${Worlds.world(this, next!!).name}" else message)
            .setCancelable(false)
            .setNegativeButton("Harita") { _, _ -> finish() }
            .setNeutralButton("Tekrar") { _, _ -> startCountdown() }
        if (result.passed && next != null) {
            builder.setPositiveButton("Sonraki") { _, _ ->
                startActivity(Intent(this, ReaderActivity::class.java).putExtra(EXTRA_LEVEL, next.number))
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
        const val FREE_MODE_WPM = 150

        private const val START_SPEED = 0.9f
        private const val END_SPEED = 1.1f
        private const val GUIDE_POSITION = 0.3f
        private const val PASS_ACCURACY = 60
        private const val BOSS_PASS_ACCURACY = 70
        private const val MAX_HEARTS = 3
        private const val HEART_BONUS = 100
        private const val POINTS_PER_WORD = 10
        private const val MAX_MULTIPLIER = 5
        private const val MIN_WORDS_FOR_HEART = 3
        private const val MISS_RATIO = 0.34f
        private const val GOOD_RATIO = 0.7f
        private const val SCORE_LAG_LINES = 2
        private const val SPAN_REFRESH_MS = 150L
        private const val FINISH_GRACE_MS = 1500L
        private const val LOUD_THRESHOLD = 0.45f
        private const val SILENCE_WARNING_MS = 4000L

        private const val STATE_PLAIN = 0
        private const val STATE_HEARD = 1
        private const val STATE_MISSED = 2
    }
}
