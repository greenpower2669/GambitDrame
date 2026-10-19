package fr.gambitdrame

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max

/**
 * Actual local neural Piper VITS speech using Sherpa-ONNX 1.13.8.
 *
 * Voice model: vits-piper-fr_FR-upmc-medium (0 Jessica, 1 Pierre).
 * No INTERNET requirement; data downloaded by CI and embedded in the APK.
 * A silent audio warmup runs before declaring the engine usable.
 */
class PiperSpeechEngine(
    context: Context,
    initialSpeakerId: Int,
    private val report: Consumer<String>
) {
    private val app = context.applicationContext
    private val ui = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Gambit-Piper-voice").apply { isDaemon = true }
    }
    private val generation = AtomicInteger()
    @Volatile private var closed = false
    @Volatile private var usable = false
    @Volatile private var speakerId = initialSpeakerId.coerceIn(0, 1)
    @Volatile private var speed = 0.88f
    @Volatile private var track: AudioTrack? = null
    private var tts: OfflineTts? = null

    init {
        executor.execute {
            try {
                post("Piper : préparation de la voix hors ligne…")
                val data = unpackData()
                val model = OfflineTtsVitsModelConfig(
                    model = "tts/piper/pierre/fr_FR-upmc-medium.onnx",
                    tokens = "tts/piper/pierre/tokens.txt",
                    dataDir = data.absolutePath
                )
                val engine = OfflineTts(
                    assetManager = app.assets,
                    config = OfflineTtsConfig(
                        model = OfflineTtsModelConfig(
                            vits = model, numThreads = 2, debug = false, provider = "cpu"
                        ),
                        maxNumSentences = 1,
                        silenceScale = 0.2f
                    )
                )
                tts = engine
                val muted = engine.generate("Bonjour, prêt.", sid = speakerId, speed = 1.0f)
                // Play the warmup silently through the same Android audio path.
                play(muted.samples, muted.sampleRate, generation.get(), muted = true)
                if (closed) return@execute
                usable = true
                post("Voix Piper hors ligne prête : ${if (speakerId == 0) "Jessica" else "Pierre"}")
            } catch (ex: Throwable) {
                usable = false
                post("Piper indisponible : ${ex.javaClass.simpleName}. Voix Android de secours.")
            }
        }
    }

    fun isReady(): Boolean = usable && !closed

    fun setSpeakerId(id: Int) {
        speakerId = id.coerceIn(0, 1)
        if (isReady())
            post("Voix Piper : ${if (speakerId == 0) "Jessica" else "Pierre"}")
    }

    fun setSpeed(next: Float) {
        speed = next.coerceIn(0.65f, 1.3f)
    }

    fun speak(text: String) {
        if (!isReady() || text.isBlank()) return
        val token = generation.incrementAndGet()
        try { track?.pause(); track?.flush() } catch (_: Throwable) {}
        executor.execute {
            try {
                val engine = tts ?: return@execute
                if (closed || token != generation.get()) return@execute
                val result = engine.generate(text, sid = speakerId, speed = speed)
                if (closed || token != generation.get()) return@execute
                play(result.samples, result.sampleRate, token, muted = false)
            } catch (ex: Throwable) {
                if (!closed && token == generation.get())
                    post("Lecture Piper interrompue : ${ex.javaClass.simpleName}")
            }
        }
    }

    private fun play(samples: FloatArray, sampleRate: Int, token: Int, muted: Boolean) {
        if (samples.isEmpty() || sampleRate <= 0 || closed) return
        val minBytes = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = max(8192, minBytes)
        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRate)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        )
        track = player
        try {
            if (muted) player.setVolume(0f) else player.setVolume(1f)
            player.play()
            var offset = 0
            val chunk = ShortArray(2048)
            while (offset < samples.size && !closed && (muted || token == generation.get())) {
                val size = minOf(chunk.size, samples.size - offset)
                for (i in 0 until size) {
                    val v = samples[offset + i].coerceIn(-1f, 1f)
                    chunk[i] = (v * 32767f).toInt().toShort()
                }
                val written = player.write(chunk, 0, size, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }
            if (offset == samples.size && !closed && (muted || token == generation.get())) {
                // Wait for real audio consumption, not a fixed playback timer.
                var last = -1
                var stalled = 0
                while (!closed && (muted || token == generation.get()) &&
                    player.playbackHeadPosition < samples.size) {
                    val head = player.playbackHeadPosition
                    stalled = if (head == last) stalled + 1 else 0
                    if (stalled > 80) break // Guard against unavailable audio device.
                    last = head
                    Thread.sleep(15)
                }
            }
        } finally {
            track = null
            try {player.stop()} catch (_: Throwable) {}
            player.release()
        }
    }

    private fun post(status: String) {
        ui.post { if (!closed) report.accept(status) }
    }

    private fun unpackData(): File {
        val root = File(app.filesDir, "gambit-piper/espeak-ng-data")
        val marker = File(root, ".ready-v1")
        if (marker.isFile) return root
        if (root.exists()) root.deleteRecursively()
        root.mkdirs()
        copyAssets("tts/piper/pierre/espeak-ng-data", root)
        marker.writeText("sherpa-onnx-1.13.8-upmc-medium")
        return root
    }

    private fun copyAssets(assetName: String, dst: File) {
        val names = app.assets.list(assetName).orEmpty()
        if (names.isEmpty()) {
            dst.parentFile?.mkdirs()
            app.assets.open(assetName).use { input ->
                FileOutputStream(dst).use { output -> input.copyTo(output) }
            }
        } else {
            dst.mkdirs()
            for (name in names) copyAssets("$assetName/$name", File(dst, name))
        }
    }

    fun stop() {
        generation.incrementAndGet()
        try { track?.pause(); track?.flush() } catch (_: Throwable) {}
    }

    fun close() {
        closed = true
        stop()
        executor.execute {
            try { tts?.release() } catch (_: Throwable) {}
            tts = null
        }
        executor.shutdown()
    }
}
