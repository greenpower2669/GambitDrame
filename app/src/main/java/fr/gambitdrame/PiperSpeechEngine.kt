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
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max

/**
 * One persistent AudioTrack, real SILENT warmup, and one serial sentence queue.
 * An incoming utterance NEVER cancels one already playing.
 * The gap starts AFTER the actual playback head reaches the final frame.
 */
class PiperSpeechEngine(context: Context, initialSpeakerId: Int,
                        private val report: Consumer<String>) {
    private val app = context.applicationContext
    private val ui = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r,"Gambit-Piper-serial").apply { isDaemon = true }
    }
    private val lock = Object()
    private val pending = ArrayDeque<String>()
    private val cancelled = AtomicInteger(0)
    @Volatile private var closed = false
    @Volatile private var usable = false
    @Volatile private var failed = false
    @Volatile private var speakerId = initialSpeakerId.coerceIn(0,1)
    @Volatile private var speed = 0.88f
    @Volatile private var gapMs = 500L
    @Volatile private var track: AudioTrack? = null
    private var playedFrames = 0L
    private var engine: OfflineTts? = null
    private var draining = false

    init {
        worker.execute {
            try {
                post("Piper : warm-up réel, muet…")
                val data = unpackData()
                val vits = OfflineTtsVitsModelConfig(
                    model="tts/piper/pierre/fr_FR-upmc-medium.onnx",
                    tokens="tts/piper/pierre/tokens.txt",
                    dataDir=data.absolutePath)
                val tts = OfflineTts(
                    assetManager=app.assets,
                    config=OfflineTtsConfig(
                        model=OfflineTtsModelConfig(vits=vits,numThreads=2,
                            debug=false,provider="cpu"),
                        maxNumSentences=1,silenceScale=0.2f))
                engine = tts
                val warmup = tts.generate("Bonjour, je suis prêt.",sid=speakerId,speed=1.0f)
                val marker = cancelled.get()
                // Do not declare READY before real muted playback has ended.
                if(!play(warmup.samples,warmup.sampleRate,marker,true))
                    throw IllegalStateException("Warm-up audio incomplet")
                if(closed)return@execute
                usable=true
                post("Piper prêt, warm-up terminé · ${if(speakerId==0)"Jessica" else "Pierre"}")
                drain()
            }catch(ex:Throwable){
                failed=true
                post("Piper indisponible : ${ex.javaClass.simpleName}. Secours Android.")
            }
        }
    }
    fun isReady():Boolean=usable&&!closed
    fun hasFailed():Boolean=failed
    fun setSpeakerId(value:Int){speakerId=value.coerceIn(0,1)}
    fun setSpeed(value:Float){speed=value.coerceIn(0.65f,1.3f)}
    fun setGapMs(value:Long){gapMs=value.coerceIn(100L,2000L)}

    fun speak(message:String) {
        if(closed||failed||message.isBlank())return
        synchronized(lock){
            // Avoid unbounded backlog: drop redundant quick help when saturated.
            if(pending.size>=12)pending.removeFirst()
            pending.addLast(message)
            if(usable&&!draining){
                draining=true
                worker.execute { drain() }
            }
        }
    }
    private fun drain(){
        if(closed||!usable)return
        synchronized(lock){draining=true}
        try{
            while(!closed){
                val message=synchronized(lock){if(pending.isEmpty())null else pending.removeFirst()} ?: break
                val marker=cancelled.get()
                val tts=engine ?: break
                try{
                    // Synthesize an entire utterance, never a series of interrupted fragments.
                    val pcm=tts.generate(message,sid=speakerId,speed=speed)
                    if(marker==cancelled.get()&&!closed){
                        play(pcm.samples,pcm.sampleRate,marker,false)
                        // Wait only after the real audio completion, not instead of it.
                        var remaining=gapMs
                        while(remaining>0&&!closed&&marker==cancelled.get()){
                            val step=minOf(remaining,40L)
                            Thread.sleep(step)
                            remaining-=step
                        }
                    }
                }catch(ex:Throwable){
                    post("Piper : lecture impossible (${ex.javaClass.simpleName})")
                }
            }
        }finally{
            synchronized(lock){
                draining=false
                if(usable&&!closed&&pending.isNotEmpty()){
                    draining=true
                    worker.execute {drain()}
                }
            }
        }
    }
    private fun makeTrack(rate:Int):AudioTrack{
        val minBytes=AudioTrack.getMinBufferSize(rate,
            AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(rate).build())
            .setBufferSizeInBytes(max(8192,minBytes))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }
    private fun play(samples:FloatArray,rate:Int,marker:Int,muted:Boolean):Boolean{
        if(samples.isEmpty()||rate<=0||closed)return false
        var player=track
        if(player==null||player.sampleRate!=rate){
            try{player?.release()}catch(_:Throwable){}
            player=makeTrack(rate)
            track=player
            playedFrames=0
        }
        player.setVolume(if(muted)0.0f else 1.0f)
        if(player.playState!=AudioTrack.PLAYSTATE_PLAYING)player.play()
        val data=ShortArray(2048)
        var index=0
        while(index<samples.size&&!closed&&marker==cancelled.get()){
            val n=minOf(data.size,samples.size-index)
            for(i in 0 until n)data[i]=(samples[index+i].coerceIn(-1f,1f)*32767f).toInt().toShort()
            val done=player.write(data,0,n,AudioTrack.WRITE_BLOCKING)
            if(done<=0)return false
            index+=done
            playedFrames+=done.toLong()
        }
        if(index!=samples.size)return false
        // Watch the device's frame cursor. Never drop trailing samples due to a timer.
        var lastHead=-1L
        var stagnant=0
        while(!closed&&marker==cancelled.get()){
            val played=player.playbackHeadPosition.toLong() and 0xffffffffL
            if(played>=playedFrames)return true
            stagnant=if(played==lastHead)stagnant+1 else 0
            if(stagnant>250)return false // Device hung: avoid indefinite wait.
            lastHead=played
            Thread.sleep(12)
        }
        return false
    }
    fun stop(){
        cancelled.incrementAndGet()
        synchronized(lock){pending.clear()}
        val player=track
        try{
            player?.pause()
            player?.flush()
        }catch(_:Throwable){}
        // Avoid stale absolute frame target after explicit cancellation.
        playedFrames=0
    }
    fun close(){
        closed=true
        stop()
        worker.execute{
            try{engine?.release()}catch(_:Throwable){}
            engine=null
            try{track?.release()}catch(_:Throwable){}
            track=null
        }
        worker.shutdown()
    }
    private fun post(status:String){
        ui.post{if(!closed)report.accept(status)}
    }
    private fun unpackData():File{
        val dest=File(app.filesDir,"gambit-piper/espeak-ng-data")
        val marker=File(dest,".ready-v1")
        if(marker.isFile)return dest
        if(dest.exists())dest.deleteRecursively()
        dest.mkdirs()
        copyAssets("tts/piper/pierre/espeak-ng-data",dest)
        marker.writeText("sherpa-onnx-1.13.8")
        return dest
    }
    private fun copyAssets(asset:String,target:File){
        val files=app.assets.list(asset).orEmpty()
        if(files.isEmpty()){
            target.parentFile?.mkdirs()
            app.assets.open(asset).use{input->
                FileOutputStream(target).use{output->input.copyTo(output)}
            }
        }else{
            target.mkdirs()
            for(name in files)copyAssets("$asset/$name",File(target,name))
        }
    }
}
