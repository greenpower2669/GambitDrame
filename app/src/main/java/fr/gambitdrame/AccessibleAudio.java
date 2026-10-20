package fr.gambitdrame;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.speech.tts.UtteranceProgressListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;
import java.util.Set;

/**
 * Offline neural Piper speech with the Android offline TTS engine as fallback.
 * Speech is optional and never controls gameplay; no network is used for voices.
 */
public final class AccessibleAudio {
    public interface StatusListener {void onStatus(String value);}
    public enum Sound { SELECT, MOVE, CAPTURE, CHECK, INVALID, NAVIGATE }
    private final Activity activity;
    private final SharedPreferences prefs;
    private final StatusListener statusListener;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private PiperSpeechEngine piper;
    private String piperStatus="Piper : initialisation…";
    private ToneGenerator tones;
    private boolean initialized=false;
    private boolean closed=false;
    private String state="Voix : initialisation…";
    private String lastMessage="";
    private String pendingMessage=null;
    private final ConcurrentHashMap<String,java.util.function.Consumer<Boolean>> tracked=new ConcurrentHashMap<>();
    private final AtomicLong utteranceIds=new AtomicLong();
    private void finishSpeech(String id,boolean ok){
        java.util.function.Consumer<Boolean> cb=tracked.remove(id);
        if(cb!=null)handler.post(()->cb.accept(ok));
    }

    public AccessibleAudio(Activity activity,SharedPreferences prefs,StatusListener listener){
        this.activity=activity;this.prefs=prefs;this.statusListener=listener;
        try{
            piper=new PiperSpeechEngine(activity,
                prefs.getInt("piperVoiceSid",0),
                message->handler.post(()->{
                    piperStatus=message;
                    if(!closed){
                        statusListener.onStatus(message);
                        if(piper!=null&&piper.hasFailed()&&initialized&&pendingMessage!=null){
                            String last=pendingMessage;pendingMessage=null;say(last);
                        }
                    }
                }));
            piper.setSpeed(prefs.getFloat("speechRate",0.88f));
            piper.setGapMs(prefs.getInt("voiceGapMs",500));
        }catch(Throwable ex){
            piperStatus="Piper indisponible : "+ex.getClass().getSimpleName();
        }
        try{tones=new ToneGenerator(AudioManager.STREAM_MUSIC,65);}
        catch(RuntimeException ignored){tones=null;}
        try{
            tts=new TextToSpeech(activity,status->handler.post(()->initialize(status)));
        }catch(RuntimeException ex){
            initialized=false;
            state="Synthèse Android indisponible : "+ex.getClass().getSimpleName();
            statusListener.onStatus(state);
        }
    }
    private void initialize(int result){
        if(closed)return;
        if(result!=TextToSpeech.SUCCESS||tts==null){
            state="Voix indisponible : vérifier la synthèse vocale Android.";
            statusListener.onStatus(state);
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            @Override public void onStart(String id){}
            @Override public void onDone(String id){finishSpeech(id,true);}
            @Override public void onError(String id){finishSpeech(id,false);}
            @Override public void onStop(String id,boolean interrupted){finishSpeech(id,false);}
        });
        int code=tts.setLanguage(Locale.FRANCE);
        if(code==TextToSpeech.LANG_MISSING_DATA||
           code==TextToSpeech.LANG_NOT_SUPPORTED){
            state="Voix française absente : installer une voix hors ligne dans Android.";
            statusListener.onStatus(state);
            return;
        }
        Voice chosen=null;
        Set<Voice> installed=tts.getVoices();
        if(installed!=null){
            for(Voice voice:installed){
                if(voice.getLocale()!=null&&
                   "fr".equals(voice.getLocale().getLanguage())&&
                   !voice.isNetworkConnectionRequired()&&
                   (chosen==null||voice.getQuality()>chosen.getQuality())){
                    chosen=voice;
                }
            }
        }
        if(chosen!=null){
            tts.setVoice(chosen);
            state="Secours Android local disponible ("+chosen.getName()+")";
            if(piper==null)statusListener.onStatus(state);
        }else{
            // Do not ask a potentially network-only voice to synthesize speech.
            state="Secours Android hors ligne absent. Piper reste prioritaire.";
            if(piper==null)statusListener.onStatus(state);
            return;
        }
        tts.setSpeechRate(prefs.getFloat("speechRate",0.88f));
        tts.setPitch(1.0f);
        tts.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
        initialized=true;
        if(piper==null||!piper.isReady())statusListener.onStatus(state);
        if(pendingMessage!=null&&(piper==null||piper.hasFailed())){
            String toSay=pendingMessage;
            pendingMessage=null;
            say(toSay);
        }
    }
    public String status(){
        return piper!=null&&piper.isReady()?piperStatus:state;
    }
    public void setPiperSpeaker(int id){
        int chosen=Math.max(0,Math.min(1,id));
        prefs.edit().putInt("piperVoiceSid",chosen).apply();
        if(piper!=null)piper.setSpeakerId(chosen);
        say("Voix "+(chosen==0?"Jessica":"Pierre")+".");
    }
    public boolean voiceOn(){return prefs.getBoolean("voiceOn",true);}
    public boolean soundsOn(){return prefs.getBoolean("soundsOn",true);}
    public void setVoiceOn(boolean on){
        prefs.edit().putBoolean("voiceOn",on).apply();
        if(!on)stopSpeech();
        if(on)say("Guidage vocal activé.");
    }
    public void setSoundsOn(boolean on){
        prefs.edit().putBoolean("soundsOn",on).apply();
        if(on)play(Sound.NAVIGATE);
    }
    public void setGapMs(int pause){
        int value=Math.max(100,Math.min(2000,pause));
        prefs.edit().putInt("voiceGapMs",value).apply();
        if(piper!=null)piper.setGapMs(value);
    }
    public void setSpeechRate(float rate){
        float value=Math.max(0.65f,Math.min(1.3f,rate));
        prefs.edit().putFloat("speechRate",value).apply();
        if(tts!=null&&initialized)tts.setSpeechRate(value);
        if(piper!=null)piper.setSpeed(value);
    }
    public void say(String message){
        if(closed||message==null||message.isEmpty()||!voiceOn())return;
        lastMessage=message;
        // Never use Android TTS during Piper warm-up: it would speak over the
        // muted warm-up and the first real sentence could be interrupted.
        if(piper!=null&&!piper.hasFailed()){
            piper.speak(message);
            return;
        }
        if(!initialized){pendingMessage=message;return;}
        tts.speak(message,TextToSpeech.QUEUE_ADD,null,
            "gambit-"+System.nanoTime());
    }
    /**
     * The callback indicates REAL completed playback: Piper waits for AudioTrack
     * frame-head drainage, Android TTS uses the utterance onDone signal.
     * Cancellation/errors deliver false, so the game never remains blocked.
     */
    public void sayTracked(String message,java.util.function.Consumer<Boolean> finished){
        if(finished==null){say(message);return;}
        if(closed||!voiceOn()||message==null||message.isEmpty()){
            handler.post(()->finished.accept(false));return;
        }
        lastMessage=message;
        if(piper!=null&&!piper.hasFailed()){
            piper.speakTracked(message,finished);
            return;
        }
        if(!initialized||tts==null){
            handler.post(()->finished.accept(false));return;
        }
        String id="gambit-move-"+utteranceIds.incrementAndGet();
        tracked.put(id,finished);
        int code=tts.speak(message,TextToSpeech.QUEUE_ADD,null,id);
        if(code!=TextToSpeech.SUCCESS)finishSpeech(id,false);
    }
    public void repeat(){
        if(lastMessage.isEmpty())say("Aucun message à répéter.");
        else say(lastMessage);
    }
    public void play(Sound kind){
        if(closed||!soundsOn()||tones==null)return;
        int tone,duration;
        switch(kind){
            case SELECT:tone=ToneGenerator.TONE_PROP_ACK;duration=45;break;
            case CAPTURE:tone=ToneGenerator.TONE_PROP_BEEP2;duration=135;break;
            case CHECK:tone=ToneGenerator.TONE_PROP_NACK;duration=185;break;
            case INVALID:tone=ToneGenerator.TONE_SUP_ERROR;duration=120;break;
            case NAVIGATE:tone=ToneGenerator.TONE_PROP_BEEP;duration=55;break;
            default:tone=ToneGenerator.TONE_PROP_BEEP;duration=85;break;
        }
        try{tones.startTone(tone,duration);}catch(RuntimeException ignored){}
    }
    public void stopSpeech(){
        pendingMessage=null;
        for(String id:tracked.keySet())finishSpeech(id,false);
        if(piper!=null)piper.stop();
        if(tts!=null&&initialized)tts.stop();
    }
    public void close(){
        closed=true;
        stopSpeech();
        if(tts!=null){tts.shutdown();tts=null;}
        if(piper!=null){piper.close();piper=null;}
        if(tones!=null){tones.release();tones=null;}
    }
}
