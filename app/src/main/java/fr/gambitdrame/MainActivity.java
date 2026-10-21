package fr.gambitdrame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.text.DateFormat;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Chess trainer: experimental human policy + independent local Stockfish analysis. */
public final class MainActivity extends Activity {
    private final List<ChessGame.Position> positions=new ArrayList<>();
    private final List<String> moves=new ArrayList<>();
    // Review the already recorded moves without rewinding or corrupting the live game.
    private final ReviewCursor review=new ReviewCursor(0);
    private final Runnable replayTick=this::advanceReplay;
    private boolean replayPlaying=false;
    private Button playbackButton;
    private TextView replayPosition;
    private final ExecutorService botWorker=Executors.newSingleThreadExecutor();
    private HumanPolicy humanPolicy;
    private StockfishOpponent stockfishOpponent;
    private int opponentMode=1; // 0 = Lichess, 1 = Stockfish
    private int precisionTarget=80,precisionSigma=5;
    private boolean adaptiveOpponent=true;
    private int adaptiveSensitivity=70,adaptiveRecovery=95,adaptiveStability=70,adaptiveMaxDepth=14;
    private int recoveryGoalCp=-100;
    private boolean gaugeTensorMode=true;
    private int gaugeRedCp=250,gaugeGreenCp=-250;
    private TextView gaugeLegend;
    private SeekBar legacyPrecisionSlider;
    private TextView legacyPrecisionLabel;
    private boolean speechPending=false;
    private long speechSequence=0;
    private Runnable waitingBotApply;
    private final Handler speechHandler=new Handler(Looper.getMainLooper());
    private boolean moveHighlights=true;
    private boolean deepMode=false;
    private int deepDepth=7;
    private boolean tacticalGuard=true;
    private String guardPreviewFen;
    private Integer guardPreviewWhiteCp,guardPreviewBestWhiteCp;
    private CheckBox tacticalCheck;
    private TextView guardInfo;
    private long failedBotRevision=-1;
    private int pieceTheme=3;
    private boolean trialMode=false,arrowMode=false;
    private final BoardArrows annotations=new BoardArrows();
    private final List<ChessGame.Position> trialPositions=new ArrayList<>();
    private final List<String> trialMoves=new ArrayList<>();
    private Button trialButton;
    private TextView trialStatus;
    private LichessOpeningExplorer explorer;
    private StockfishBridge stockfishBridge;
    private AccessibleAudio audio;
    private TextView speechStatus;
    private Integer lastWhiteCp,lastWhiteMate;
    private String lastEvaluatedFen;
    private String lastSpokenEvaluationFen;
    private boolean announceEvaluation=false;
    private TextView analysisPanel, botInfo;
    private Spinner levels;
    private Switch onlineSwitch;
    private boolean botRunning=false;
    private long revision=0;
    private int targetRating=700;
    private double aggression=0.4;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable botRunnable=this::botMove;
    private ChessView chessView;
    private ChessView fullscreenChessView;
    private Dialog fullscreenDialog;
    private Switch normalArrowSwitch;
    private Button fullscreenArrowsButton;
    private TextView status,history;
    private EditText moveInput;
    private Switch botSwitch;
    private int selected=-1;
    private boolean botEnabled=true;
    private SharedPreferences prefs;
    private EngineDiagnostics engineLog;
    private TextView engineDebugStatus;
    private StudyTrendView studyChart;
    private TextView studyStatus;
    private final ExecutorService studyWorker=Executors.newSingleThreadExecutor();
    private volatile boolean studyCancelled=false;
    private boolean studyBusy=false;
    private static final int STUDY_EXPORT=801, STUDY_IMPORT=802;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("gambitdrame-v1",MODE_PRIVATE);
        engineLog=new EngineDiagnostics(this);
        if(!prefs.getBoolean("gd-v041-preset",false)){
            prefs.edit().putBoolean("gd-v041-preset",true)
                .putInt("opponentMode",1).putInt("precisionTarget",80)
                .putInt("precisionSigma",5).putInt("pieceTheme",3)
                .putBoolean("precisionDeep",false).putInt("precisionDepth",7).apply();
        }
        botEnabled=prefs.getBoolean("bot",true);
        targetRating=prefs.getInt("targetRating",700);
        aggression=prefs.getFloat("aggression",0.4f);
        opponentMode=prefs.getInt("opponentMode",1);
        precisionTarget=prefs.getInt("precisionTarget",80);
        precisionSigma=prefs.getInt("precisionSigma",5);
        adaptiveOpponent=prefs.getBoolean("gd015Adaptive",true);
        adaptiveSensitivity=prefs.getInt("gd015Sensitivity",70);
        adaptiveRecovery=prefs.getInt("gd015Recovery",95);
        adaptiveStability=prefs.getInt("gd015Stability",70);
        adaptiveMaxDepth=prefs.getInt("gd015MaxDepth",14);
        recoveryGoalCp=Math.max(-500,Math.min(0,prefs.getInt("gd016RecoveryGoalCp",-100)));
        gaugeTensorMode=prefs.getBoolean("gd017TensorMode",true);
        gaugeRedCp=Math.max(50,Math.min(800,prefs.getInt("gd017RedCp",250)));
        gaugeGreenCp=Math.max(-800,Math.min(-50,prefs.getInt("gd017GreenCp",-250)));
        deepMode=prefs.getBoolean("precisionDeep",false);
        deepDepth=prefs.getInt("precisionDepth",7);
        tacticalGuard=prefs.getBoolean("tacticalGuard",true);
        pieceTheme=prefs.getInt("pieceTheme",3);
        stockfishOpponent=new StockfishOpponent(this);
        stockfishOpponent.setDiagnostics(engineLog);
        engineLog.record("APP","GambitDrame v0.4.4 launched; mode="+opponentMode+
            " target="+precisionTarget+" sigma="+precisionSigma+
            " deep="+deepMode+" depth="+deepDepth);
        long seed=prefs.getLong("humanSeed",0L);
        if(seed==0L){seed=new Random().nextLong();prefs.edit().putLong("humanSeed",seed).apply();}
        humanPolicy=new HumanPolicy(targetRating,seed,aggression);
        explorer=new LichessOpeningExplorer(this);
        restore();
        review.setHistorySize(moves.size());
        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xfff7f7ef);
        root.setPadding(12,10,12,22);
        scroller.addView(root);
        scroller.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
                root.setPadding(12,Math.max(10,bars.top),12,Math.max(22,bars.bottom));
            }
            return insets;
        });
        heading(root,"♟ GambitDrame",25);
        label(root,"Version 0.4.8 — jauge à trois curseurs colorés et tenseurs stratégiques",16);
        label(root,"Niveaux 400–900 EXPÉRIMENTAUX, pas encore calibrés par parties complètes.",14);
        chessView=new ChessView();
        analysisPanel=new TextView(this);
        analysisPanel.setText("Stockfish 18 : lancement de l'analyse…");
        analysisPanel.setTextColor(Color.BLACK);
        analysisPanel.setTextSize(15);
        analysisPanel.setPadding(8,10,8,10);
        analysisPanel.setBackgroundColor(0xffe5edeb);
        analysisPanel.setTextIsSelectable(true);
        boolean wide=getResources().getConfiguration().screenWidthDp>=650;
        if(wide){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            root.addView(row);
            LinearLayout side=new LinearLayout(this);
            side.setOrientation(LinearLayout.VERTICAL);
            side.addView(analysisPanel);
            row.addView(side,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(chessView,new LinearLayout.LayoutParams(0,-2,2));
        }else{
            root.addView(chessView,new LinearLayout.LayoutParams(-1,-2));
        }
        button(root,"⛶ ÉCHIQUIER PLEIN ÉCRAN — jouer ou tracer",this::openFullscreenBoard);
        tacticalCheck=new CheckBox(this);
        tacticalCheck.setText("Garde-fou tactique (profondeur 11 → 12, toutes pièces)");
        tacticalCheck.setTextSize(16);
        tacticalCheck.setTextColor(Color.BLACK);
        tacticalCheck.setChecked(tacticalGuard);
        root.addView(tacticalCheck);
        tacticalCheck.setVisibility(View.GONE);
        guardInfo=label(root,"Jauge Stockfish : position réelle et prévision tactique ; les trois curseurs colorés règlent la façon de choisir.",14);
        gaugeLegend=label(root,gaugeLegendText(),16);
        gaugeLegend.setTypeface(null,Typeface.BOLD);
        tacticalCheck.setOnCheckedChangeListener((button,on)->{
            tacticalGuard=on;
            prefs.edit().putBoolean("tacticalGuard",on).apply();
            engineLog.record("SETTING","tacticalGuard="+on+" mainGauge=11 guardGauge=12");
            if(stockfishBridge!=null)stockfishBridge.cancelGuard();
            if(!on){guardPreviewFen=null;guardPreviewWhiteCp=null;guardPreviewBestWhiteCp=null;}
            applyGuardOverlay();
            revision++;failedBotRevision=-1;
            scheduleBot();
        });
        engineDebugStatus=label(root,"Stockfish : mode="+(opponentMode==1?"moteur":"Lichess")+
            ", cible="+precisionTarget+" %, variabilité="+precisionSigma,15);
        engineDebugStatus.setTypeface(null,Typeface.BOLD);
        button(root,"🐞 GET ERR — JOURNAL STOCKFISH / COPIER",this::showEngineDebug);
        // All five replay controls sit next to the board, including TalkBack labels.
        LinearLayout playbackControls=new LinearLayout(this);
        playbackControls.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(playbackControls,new LinearLayout.LayoutParams(-1,-2));
        replayButton(playbackControls,"⏮\nDébut","Revoir le début de la partie",()->manualSeek(0));
        replayButton(playbackControls,"◀\nAvant","Voir le coup précédent",
            ()->manualSeek(review.ply()-1));
        playbackButton=replayButton(playbackControls,"▶\nLecture","Lecture automatique de la partie",
            this::toggleReplay);
        replayButton(playbackControls,"Après\n▶","Voir le coup suivant",
            ()->manualSeek(review.ply()+1));
        replayButton(playbackControls,"Fin\n⏭","Voir le dernier coup",
            ()->manualSeek(moves.size()));
        replayPosition=label(root,"Partie en direct",15);
        heading(root,"Atelier d'analyse — sans modifier la partie",17);
        trialButton=new Button(this);
        trialButton.setText("Essayer des coups sur une copie de la position");
        trialButton.setTextSize(15);
        trialButton.setOnClickListener(v->toggleTrial());
        root.addView(trialButton);
        trialStatus=label(root,"Essai désactivé",15);
        LinearLayout trialButtons=new LinearLayout(this);
        trialButtons.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(trialButtons);
        button(trialButtons,"↶ Annuler essai",this::undoTrial);
        button(trialButtons,"Quitter essai",this::exitTrial);
        Switch arrowSwitch=new Switch(this);
        normalArrowSwitch=arrowSwitch;
        arrowSwitch.setText("Tracer des flèches vectorielles (glisser d'une case à l'autre)");
        arrowSwitch.setTextColor(Color.BLACK);
        arrowSwitch.setTextSize(15);
        arrowSwitch.setChecked(false);
        root.addView(arrowSwitch);
        arrowSwitch.setOnCheckedChangeListener((b,on)->setArrowMode(on));
        button(root,"Effacer les flèches",()->{
            annotations.clear();
            chessView.invalidate();
        });
        button(root,"Reprendre la partie (sortir de la relecture)",this::resumeGame);
        heading(root,"Styles des pièces lisibles",17);
        Spinner pieceStyles=new Spinner(this);
        pieceStyles.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Ivoire et ébène · stylisé","Minimal · formes épurées","Très fort contraste · gros contours","Bois 3D · demi-pièces à plat","Bois 3D · perspective"}));
        pieceStyles.setSelection(pieceTheme);
        pieceStyles.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int index,long id){
                pieceTheme=index;
                prefs.edit().putInt("pieceTheme",index).apply();
                chessView.invalidate();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        root.addView(pieceStyles);
        heading(root,"Accessibilité · écoute et sons",19);
        label(root,"Voix IA Piper locale intégrée : Jessica ou Pierre, sans Internet. La voix française Android reste un secours si Piper ne démarre pas.",13);
        speechStatus=label(root,"Voix : initialisation…",14);
        Switch voiceToggle=new Switch(this);
        voiceToggle.setText("Parler : annoncer les coups joués");
        voiceToggle.setTextColor(Color.BLACK);
        voiceToggle.setTextSize(16);
        voiceToggle.setChecked(prefs.getBoolean("voiceOn",true));
        voiceToggle.setContentDescription("Activer ou couper les annonces parlées des coups");
        root.addView(voiceToggle);
        voiceToggle.setOnCheckedChangeListener((button,on)->{
            if(audio!=null)audio.setVoiceOn(on);
        });
        TextView speakerLabel=label(root,"Choisir une voix Piper française",15);
        Spinner speakers=new Spinner(this);
        speakers.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Jessica · voix féminine","Pierre · voix masculine"}));
        speakers.setSelection(Math.max(0,Math.min(1,prefs.getInt("piperVoiceSid",0))));
        root.addView(speakers);
        speakers.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            private boolean first=true;
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    View view,int index,long id){
                if(first){first=false;return;}
                if(audio!=null)audio.setPiperSpeaker(index);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        Switch soundToggle=new Switch(this);
        soundToggle.setText("Sons : sélection, coup, prise, échec, erreur");
        soundToggle.setTextColor(Color.BLACK);
        soundToggle.setTextSize(16);
        soundToggle.setChecked(prefs.getBoolean("soundsOn",true));
        soundToggle.setContentDescription("Activer ou couper les effets sonores du jeu");
        root.addView(soundToggle);
        soundToggle.setOnCheckedChangeListener((button,on)->{
            if(audio!=null)audio.setSoundsOn(on);
        });
        // Historical autoEvalSpeech preference remains stored for reversibility;
        // GD-015 requires no automatic score/variation announcements.
        label(root,"Évaluations et variantes : uniquement par les boutons d'aide ou d'écoute.",13);
        TextView speedLabel=label(root,"Vitesse de parole",14);
        SeekBar speed=new SeekBar(this);
        speed.setMax(65);
        speed.setProgress(Math.max(0,Math.min(65,Math.round((prefs.getFloat("speechRate",0.88f)-0.65f)*100))));
        speed.setContentDescription("Régler la vitesse de la voix");
        root.addView(speed);
        TextView silenceLabel=label(root,"Pause ENTRE phrases après lecture complète (ms)",14);
        SeekBar sentenceDelay=new SeekBar(this);
        sentenceDelay.setMax(19);
        sentenceDelay.setProgress(Math.max(0,Math.min(19,(prefs.getInt("voiceGapMs",500)-100)/100)));
        sentenceDelay.setContentDescription("Pause entre deux annonces, de 100 à 2000 millisecondes");
        root.addView(sentenceDelay);
        sentenceDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser){
                if(fromUser&&audio!=null)audio.setGapMs(100+100*value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){
                if(audio!=null)audio.say("Pause entre annonces enregistrée.");
            }
        });
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean fromUser){
                if(fromUser&&audio!=null)audio.setSpeechRate(0.65f+value/100.0f);
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){
                if(audio!=null)audio.say("Vitesse de voix réglée.");
            }
        });
        LinearLayout listening=new LinearLayout(this);
        listening.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(listening);
        button(listening,"Décrire la position",this::describePosition);
        button(listening,"Répéter",()->audio.repeat());
        LinearLayout detailedHelp=new LinearLayout(this);
        detailedHelp.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(detailedHelp);
        button(detailedHelp,"Lire l’échiquier",this::readFullBoard);
        button(detailedHelp,"Coups possibles",this::readLegalMoves);
        button(root,"Écouter l’avantage Stockfish",this::describeEvaluation);
        button(root,"Quel coup jouer ?",()->showMoveAdvice(false));
        button(root,"Quels coups sont possibles et pourquoi ?",()->showMoveAdvice(true));
        if(!wide)root.addView(analysisPanel,new LinearLayout.LayoutParams(-1,-2));
        status=label(root,"",18);
        status.setTypeface(null,Typeface.BOLD);
        botSwitch=new Switch(this);
        botSwitch.setText("Jouer contre un adversaire (sinon : deux humains)");
        botSwitch.setTextColor(Color.BLACK);
        botSwitch.setTextSize(16);
        botSwitch.setChecked(botEnabled);
        botSwitch.setOnCheckedChangeListener((button,checked)->{
            handler.removeCallbacks(botRunnable);
            revision++;
            botEnabled=checked;
            prefs.edit().putBoolean("bot",checked).apply();
            selected=-1;refresh();scheduleBot();
        });
        root.addView(botSwitch);
        botInfo=label(root,"Profil en préparation",14);
        heading(root,"Choix du moteur adversaire",17);
        Spinner opponents=new Spinner(this);
        opponents.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Coups humains Lichess expérimentaux","Stockfish · qualité relative des coups"}));
        opponents.setSelection(opponentMode);
        opponents.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int index,long id){
                if(opponentMode==index)return;
                opponentMode=index;
                prefs.edit().putInt("opponentMode",index).apply();
                engineLog.record("SETTING","opponentMode="+index+" (0=Lichess,1=Stockfish)");
                updateEngineDebugStatus("Mode adversaire modifié");
                revision++;
                failedBotRevision=-1;
                handler.removeCallbacks(botRunnable);
                if(botInfo!=null)botInfo.setText(index==1?"Stockfish : précision variable (0–100 relatif)":"Lichess : profil expérimental non calibré");
                scheduleBot();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        root.addView(opponents);
        // The visual gauge is the primary control. Legacy knobs remain available
        // under one collapsible advanced section, preserving saved preferences.
        LinearLayout advancedControls=new LinearLayout(this);
        advancedControls.setOrientation(LinearLayout.VERTICAL);
        advancedControls.setVisibility(View.GONE);
        button(root,"⚙ Réglages avancés et anciens algorithmes",
            ()->advancedControls.setVisibility(advancedControls.getVisibility()==View.VISIBLE?
                View.GONE:View.VISIBLE));
        root.addView(advancedControls);
        CheckBox tensorModeBox=new CheckBox(this);
        tensorModeBox.setText("Trois curseurs sur la jauge (mode actuel) — décocher : ancien algorithme");
        tensorModeBox.setChecked(gaugeTensorMode);
        tensorModeBox.setTextColor(Color.BLACK);
        advancedControls.addView(tensorModeBox);
        tensorModeBox.setOnCheckedChangeListener((button,enabled)->{
            gaugeTensorMode=enabled;
            prefs.edit().putBoolean("gd017TensorMode",enabled).apply();
            engineLog.record("SETTING","gaugeTensorMode="+enabled);
            invalidateAllBoards();
            if(stockfishBridge!=null)stockfishBridge.cancelGuard();
            revision++;failedBotRevision=-1;scheduleBot();
        });
        ((android.view.ViewGroup)tacticalCheck.getParent()).removeView(tacticalCheck);
        tacticalCheck.setVisibility(View.VISIBLE);
        advancedControls.addView(tacticalCheck);
        CheckBox adaptiveBox=new CheckBox(this);
        adaptiveBox.setText("Adversaire stratégique adaptatif (défaut) — ancien algorithme accessible");
        adaptiveBox.setChecked(adaptiveOpponent);adaptiveBox.setTextColor(Color.BLACK);
        advancedControls.addView(adaptiveBox);
        adaptiveBox.setOnCheckedChangeListener((b,on)->{
            adaptiveOpponent=on;prefs.edit().putBoolean("gd015Adaptive",on).apply();
            engineLog.record("SETTING","adaptative="+on);
            if(stockfishBridge!=null)stockfishBridge.cancelGuard();
            revision++;failedBotRevision=-1;scheduleBot();
        });
        addAdaptiveSlider(advancedControls,"Sensibilité aux positions déséquilibrées",adaptiveSensitivity,
            n->{adaptiveSensitivity=n;prefs.edit().putInt("gd015Sensitivity",n).apply();});
        addAdaptiveSlider(advancedControls,"Intensité de récupération",adaptiveRecovery,
            n->{adaptiveRecovery=n;prefs.edit().putInt("gd015Recovery",n).apply();});
        label(advancedControls,"Barre turquoise sur la jauge de gauche : objectif de récupération (score du camp Stockfish), indépendant du niveau de jeu.",14);
        TextView goalLabel=label(advancedControls,"Objectif de récupération : "+
            String.format(java.util.Locale.FRANCE,"%.2f",recoveryGoalCp/100.0)+" pion",16);
        SeekBar goalSlider=new SeekBar(this);
        goalSlider.setMax(20);goalSlider.setProgress((recoveryGoalCp+500)/25);
        goalSlider.setContentDescription("Seuil positionnel de sortie de récupération, entre moins cinq et zéro pion");
        advancedControls.addView(goalSlider);
        goalSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                if(!fromUser)return;
                recoveryGoalCp=-500+progress*25;
                goalLabel.setText("Objectif de récupération : "+
                    String.format(java.util.Locale.FRANCE,"%.2f",recoveryGoalCp/100.0)+" pion");
                prefs.edit().putInt("gd016RecoveryGoalCp",recoveryGoalCp).apply();
                engineLog.record("SETTING","recoveryGoalCp="+recoveryGoalCp+
                    " nominalRemains="+precisionTarget);
                if(stockfishBridge!=null)stockfishBridge.cancelGuard();
                revision++;failedBotRevision=-1;scheduleBot();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        addAdaptiveSlider(advancedControls,"Stabilité du plan",adaptiveStability,
            n->{adaptiveStability=n;prefs.edit().putInt("gd015Stability",n).apply();});
        TextView depthTitle=label(advancedControls,"Profondeur maximale adaptative : "+adaptiveMaxDepth,14);
        SeekBar maxDepthBar=new SeekBar(this);
        maxDepthBar.setMax(4);maxDepthBar.setProgress(adaptiveMaxDepth-12);
        advancedControls.addView(maxDepthBar);
        maxDepthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                if(!fromUser)return;
                adaptiveMaxDepth=12+value;
                depthTitle.setText("Profondeur maximale adaptative : "+adaptiveMaxDepth);
                prefs.edit().putInt("gd015MaxDepth",adaptiveMaxDepth).apply();
                engineLog.record("SETTING","adaptiveMaxDepth="+adaptiveMaxDepth);
                if(stockfishBridge!=null)stockfishBridge.cancelGuard();
                revision++;failedBotRevision=-1;scheduleBot();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        Switch advanced=new Switch(this);
        advanced.setText("Activer MultiPV approfondi (désactivé par défaut)");
        advanced.setTextColor(Color.BLACK);
        advanced.setTextSize(16);advanced.setChecked(deepMode);
        advancedControls.addView(advanced);
        TextView advancedText=label(advancedControls,"Profondeur du mode approfondi : "+deepDepth,15);
        SeekBar advancedDepth=new SeekBar(this);
        advancedDepth.setMax(19);advancedDepth.setProgress(deepDepth-1);
        advancedDepth.setEnabled(deepMode);
        tacticalCheck.setEnabled(!deepMode);
        advancedControls.addView(advancedDepth);
        advancedDepth.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int progress,boolean fromUser){
                if(!fromUser)return;
                deepDepth=progress+1;
                advancedText.setText("Profondeur du mode approfondi : "+deepDepth);
                prefs.edit().putInt("precisionDepth",deepDepth).apply();
                engineLog.record("SETTING","MultiPV depth="+deepDepth);
                revision++;failedBotRevision=-1;scheduleBot();
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){}
        });
        advanced.setOnCheckedChangeListener((b,on)->{
            deepMode=on;advancedDepth.setEnabled(on);
            tacticalCheck.setEnabled(!on);
            prefs.edit().putBoolean("precisionDeep",on).apply();
            engineLog.record("SETTING","advancedMultiPV="+on+" depth="+deepDepth);
            updateEngineDebugStatus("Mode de calcul modifié");
            revision++;failedBotRevision=-1;scheduleBot();
        });
        label(advancedControls,"Mode adaptatif : niveau habituel constant ; la récupération cherche uniquement à atteindre la barre en centipions. Décochez adaptatif pour comparer les modes anciens (garde-fou et MultiPV).",14);
        label(advancedControls,"Stockfish classe TOUS les coups légaux entre pire=0 et meilleur=100. Il s'agit d'une qualité RELATIVE à cette position, pas de l'indice de précision Chess.com ni d'un Elo.",13);
        TextView precisionLabel=label(advancedControls,"Niveau habituel Stockfish : "+precisionTarget+" %",16);
        legacyPrecisionLabel=precisionLabel;
        SeekBar targetSlider=new SeekBar(this);
        legacyPrecisionSlider=targetSlider;
        targetSlider.setMax(100);
        targetSlider.setProgress(precisionTarget);
        advancedControls.addView(targetSlider);
        targetSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                if(!fromUser)return;
                precisionTarget=value;
                precisionLabel.setText("Niveau habituel Stockfish : "+value+" %");
                prefs.edit().putInt("precisionTarget",value).apply();
                engineLog.record("SETTING","precisionTarget="+value+"%");
                updateEngineDebugStatus("Cible modifiée");
                invalidateAllBoards();
                if(gaugeLegend!=null)gaugeLegend.setText(gaugeLegendText());
                revision++;failedBotRevision=-1;scheduleBot();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        TextView deviationLabel=label(advancedControls,"Variabilité habituelle : ± "+precisionSigma+" points",16);
        SeekBar varianceSlider=new SeekBar(this);
        varianceSlider.setMax(30);
        varianceSlider.setProgress(precisionSigma);
        advancedControls.addView(varianceSlider);
        varianceSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                if(!fromUser)return;
                precisionSigma=value;
                deviationLabel.setText("Variabilité habituelle : ± "+value+" points");
                prefs.edit().putInt("precisionSigma",value).apply();
                engineLog.record("SETTING","precisionSigma="+value);
                updateEngineDebugStatus("Variabilité modifiée");
                revision++;failedBotRevision=-1;scheduleBot();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        heading(root,"Niveau cible (EXPÉRIMENTAL, pas Elo vérifié)",17);
        levels=new Spinner(this);
        levels.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"400","500","600","700","800","900"}));
        levels.setSelection(Math.max(0,Math.min(5,(targetRating-400)/100)));
        levels.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int index,long id){
                targetRating=400+index*100;
                prefs.edit().putInt("targetRating",targetRating).apply();
                reconfigurePolicy();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        root.addView(levels);
        onlineSwitch=new Switch(this);
        onlineSwitch.setText("Coups humains réels Lichess quand Internet est disponible");
        onlineSwitch.setTextSize(14);
        onlineSwitch.setTextColor(Color.BLACK);
        onlineSwitch.setChecked(prefs.getBoolean("lichessOnline",true));
        onlineSwitch.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("lichessOnline",on).apply());
        root.addView(onlineSwitch);
        label(root,"Lichess : tranche publique inférieure à 1000, blitz/rapid ; joueurs et stabilités non filtrés. Hors ligne : politique locale heuristique.",13);
        TextView risk=label(root,"Style : positionnel ↔ agressif",15);
        SeekBar styleSlider=new SeekBar(this);
        styleSlider.setMax(100);
        styleSlider.setProgress((int)(aggression*100));
        styleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar sb,int n,boolean fromUser){
                if(!fromUser)return;
                aggression=n/100.0;
                prefs.edit().putFloat("aggression",(float)aggression).apply();
                reconfigurePolicy();
            }
            public void onStartTrackingTouch(SeekBar sb){}
            public void onStopTrackingTouch(SeekBar sb){}
        });
        root.addView(styleSlider);
        LinearLayout commands=new LinearLayout(this);
        root.addView(commands);
        button(commands,"Nouvelle partie",()->new AlertDialog.Builder(this)
            .setTitle("Nouvelle partie ?").setMessage("Recommencer depuis la position initiale ?")
            .setPositiveButton("Recommencer",(d,w)->reset()).setNegativeButton("Annuler",null).show());
        button(commands,"Annuler",this::undo);
        LinearLayout entry=new LinearLayout(this);
        root.addView(entry);
        moveInput=new EditText(this);
        moveInput.setSingleLine(true);
        moveInput.setTextSize(18);
        moveInput.setHint("Coup : e2e4");
        moveInput.setContentDescription("Entrer un coup UCI, exemple e2e4");
        entry.addView(moveInput,new LinearLayout.LayoutParams(0,-2,1));
        button(entry,"Jouer",()->{
            String uci=moveInput.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            moveInput.setText("");
            tryMove(uci);
        });
        history=label(root,"",15);
        heading(root,"📊 Étudier mes parties",18);
        studyStatus=label(root,"Analyse Stockfish locale, hors ligne. Précision indicative différente de Lichess.",16);
        button(root,"📊 ANALYSER LA PARTIE (profondeur 9)",this::runStudy);
        button(root,"📖 JOURNAL — revoir les analyses",this::showStudyJournal);
        button(root,"♟ RÉUSSITE PAR OUVERTURE",this::showOpeningSuccess);
        studyChart=new StudyTrendView(this);
        root.addView(studyChart,new LinearLayout.LayoutParams(-1,dp(205)));
        studyChart.setRecords(studyEntries());
        label(root,"Graphique : précision estimée des Blancs et des Noirs par partie analysée. "+
            "Variabilité : écart-type des qualités de coups, différent du réglage adverse.",14);
        button(root,"💾 EXPORTER toutes les analyses (.json)",this::exportStudyJournal);
        button(root,"📂 IMPORTER un journal (.json)",this::importStudyJournal);
        button(root,"Importer FEN",this::importFen);
        button(root,"Copier FEN",()->{
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("FEN",displayed().fen()));
            Toast.makeText(this,"Position FEN copiée",Toast.LENGTH_SHORT).show();
        });
        heading(root,"Entraînement → Ouvertures",18);
        button(root,"Gambit de dame",()->new AlertDialog.Builder(this)
            .setTitle("Gambit de dame")
            .setMessage("Position d'entraînement : 1.d4 d5 2.c4. Charger la position comme une nouvelle partie ?")
            .setPositiveButton("Charger",(d,w)->loadOpening())
            .setNegativeButton("Annuler",null).show());
        button(root,"À propos de Stockfish GPLv3",()->new AlertDialog.Builder(this)
            .setTitle("Stockfish 18 — analyse et adversaire")
            .setMessage("Moteur libre GPLv3. Source exacte : github.com/official-stockfish/Stockfish/tree/sf_18. Le binaire est compilé sans modification depuis ce tag par GitHub Actions. Licence incluse dans les ressources APK. Le mode Stockfish joue sur évaluation positionnelle. Le mode humain Lichess reste indépendant de Stockfish.")
            .setPositiveButton("Code source",(d,w)->{
                android.content.Intent intent=new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/official-stockfish/Stockfish/tree/sf_18"));
                startActivity(intent);
            }).setNegativeButton("Fermer",null).show());
        setContentView(scroller);
        audio=new AccessibleAudio(this,prefs,value->speechStatus.setText(value));
        stockfishBridge=new StockfishBridge(this,(fen,message,whiteCp,whiteMate)->{
            if(fen!=null&&!fen.equals(displayed().fen()))return;
            analysisPanel.setText(message);
            if(message!=null&&message.contains("profondeur 11")&&
               (whiteCp!=null||whiteMate!=null)){
                engineLog.record("GAUGE_DEPTH11","fen="+fen+
                    " whiteCp="+whiteCp+" whiteMate="+whiteMate+
                    " (NB: jauge profondeur 11, choix rapide profondeur 1)");
            }
            if(message!=null&&message.startsWith("Stockfish :")&&
                (message.contains("interrompu")||message.contains("impossible")||
                 message.contains("absent")||message.contains("terminé")||
                 message.contains("communication"))){
                engineLog.record("GAUGE_ERR",message+" fen="+fen);
            }
            if(whiteCp!=null||whiteMate!=null){
                chessView.setEvaluation(whiteCp,whiteMate);
                if(fullscreenChessView!=null)fullscreenChessView.setEvaluation(whiteCp,whiteMate);
                lastWhiteCp=whiteCp;
                lastWhiteMate=whiteMate;
                lastEvaluatedFen=fen;
                // GD-015: analysis/gauge is silent unless the user presses
                // the explicit "Écouter l’avantage Stockfish" button.
            }
        });
        stockfishBridge.setDiagnostics(engineLog);
        stockfishBridge.start();
        refresh();
        analyzeDisplayed();
        scheduleBot();
    }
    private void setArrowMode(boolean on){
        if(arrowMode==on)return;
        arrowMode=on;selected=-1;
        if(normalArrowSwitch!=null&&normalArrowSwitch.isChecked()!=on)
            normalArrowSwitch.setChecked(on);
        if(fullscreenArrowsButton!=null)
            fullscreenArrowsButton.setText(on?"✓ Flèches : ON":"↗ Flèches : OFF");
        if(chessView!=null)chessView.invalidate();
        if(fullscreenChessView!=null)fullscreenChessView.invalidate();
        if(audio!=null)audio.say(on?"Mode flèches activé. Faites glisser le doigt.":"Mode flèches désactivé.");
    }
    private void openFullscreenBoard(){
        if(fullscreenDialog!=null&&fullscreenDialog.isShowing())return;
        Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        fullscreenDialog=dialog;
        LinearLayout area=new LinearLayout(this);
        area.setOrientation(LinearLayout.VERTICAL);
        area.setGravity(Gravity.CENTER_VERTICAL);
        area.setPadding(dp(4),dp(10),dp(4),dp(16));
        area.setBackgroundColor(0xfff7f7ef);
        TextView title=new TextView(this);
        title.setText("♟ GambitDrame · plein écran");
        title.setGravity(Gravity.CENTER);title.setTextSize(19);
        title.setTextColor(Color.BLACK);area.addView(title);
        View space=new View(this);
        area.addView(space,new LinearLayout.LayoutParams(1,0,1));
        fullscreenChessView=new ChessView();
        fullscreenChessView.setEvaluation(lastWhiteCp,lastWhiteMate);
        area.addView(fullscreenChessView,new LinearLayout.LayoutParams(-1,-2));
        View bottom=new View(this);
        area.addView(bottom,new LinearLayout.LayoutParams(1,0,1));
        LinearLayout controls=new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        area.addView(controls);
        fullscreenArrowsButton=new Button(this);
        fullscreenArrowsButton.setText(arrowMode?"✓ Flèches : ON":"↗ Flèches : OFF");
        controls.addView(fullscreenArrowsButton,new LinearLayout.LayoutParams(0,-2,1));
        fullscreenArrowsButton.setOnClickListener(v->setArrowMode(!arrowMode));
        button(controls,"Effacer",()->{
            annotations.clear();
            if(chessView!=null)chessView.invalidate();
            if(fullscreenChessView!=null)fullscreenChessView.invalidate();
        });
        button(controls,"Quitter ⤢",dialog::dismiss);
        dialog.setContentView(area);
        dialog.setOnDismissListener(d->{
            fullscreenDialog=null;fullscreenChessView=null;fullscreenArrowsButton=null;
            if(chessView!=null)chessView.invalidate();
        });
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    private void applyGuardOverlay(){
        boolean show=tacticalGuard&&guardPreviewFen!=null&&
            guardPreviewFen.equals(displayed().fen());
        Integer chosen=show?guardPreviewWhiteCp:null;
        Integer best=show?guardPreviewBestWhiteCp:null;
        if(chessView!=null)chessView.setGuardEstimate(chosen,best);
        if(fullscreenChessView!=null)fullscreenChessView.setGuardEstimate(chosen,best);
    }
    private String gaugeLegendText(){
        return "● GRIS — niveau : "+precisionTarget+" % (rang des coups)"+
            "\n● ROUGE — limite d'écrasement : +"+
            String.format(java.util.Locale.FRANCE,"%.2f",gaugeRedCp/100.0)+" pion"+
            "\n● VERT — limite de soumission : "+
            String.format(java.util.Locale.FRANCE,"%.2f",gaugeGreenCp/100.0)+" pion"+
            "\nGlisser N, R ou V directement sur la jauge. Rouge et vert"+
            " agissent progressivement sur le choix des coups.";
    }
    private void invalidateAllBoards(){
        if(chessView!=null)chessView.invalidate();
        if(fullscreenChessView!=null)fullscreenChessView.invalidate();
    }
    private void updateEngineDebugStatus(String description){
        if(engineDebugStatus!=null)
            engineDebugStatus.setText("Stockfish : "+(opponentMode==1?
                (gaugeTensorMode?"jauge colorée":adaptiveOpponent?"adaptatif ancien":deepMode?"MultiPV":"rapide"):"Lichess")+
                " | niveau "+precisionTarget+" % | sigma "+precisionSigma+
                (gaugeTensorMode?" | rouge +"+gaugeRedCp+"cp | vert "+gaugeGreenCp+"cp":
                 adaptiveOpponent?" | objectif jauge "+
                    String.format(java.util.Locale.FRANCE,"%.2f",recoveryGoalCp/100.0):"")+
                " | "+description);
    }
    private String currentEngineReport(){
        return "GambitDrame 0.4.8 / Android "+Build.VERSION.RELEASE+
            "\nJauge 3 curseurs="+gaugeTensorMode+" gris="+precisionTarget+
            "% rouge=+"+gaugeRedCp+"cp vert="+gaugeGreenCp+"cp"+
            "\\nAdaptatif="+adaptiveOpponent+" paramètres="+adaptiveSensitivity+"/"+adaptiveRecovery+"/"+adaptiveStability+"/"+adaptiveMaxDepth+
            "\\nBut positionnel distinct du niveau (camp IA)="+recoveryGoalCp+"cp"+
            "\nMode adversaire="+opponentMode+" (0=Lichess, 1=Stockfish)"+
            "\nMoteur approfondi="+deepMode+" / profondeur="+deepDepth+
            "\nGarde fou partagé prof11→12="+tacticalGuard+
            "\nCurseur précision="+precisionTarget+" % / variabilité sigma="+precisionSigma+
            "\nPersisté="+prefs.getInt("precisionTarget",-1)+"/"+prefs.getInt("precisionSigma",-1)+
            " mode="+prefs.getInt("opponentMode",-1)+
            "\nHistorique coups="+android.text.TextUtils.join(" ",moves)+
            "\nFEN="+current().fen()+
            "\nDernière jauge cp blanc="+lastWhiteCp+" mate blanc="+lastWhiteMate+
            " (profondeur 11 différente des candidats à profondeur 1)"+
            "\nNombre de demi-coups="+moves.size()+
            "\n=== JOURNAL STOCKFISH CONSERVÉ LOCALEMENT ===\n"+
            engineLog.text();
    }
    private void showEngineDebug(){
        final String report=currentEngineReport();
        ScrollView scroller=new ScrollView(this);
        TextView data=new TextView(this);
        data.setText(report);
        data.setTextColor(Color.BLACK);
        data.setTextSize(14);
        data.setTextIsSelectable(true);
        data.setPadding(dp(12),dp(10),dp(12),dp(10));
        scroller.addView(data);
        new AlertDialog.Builder(this).setTitle("🐞 GET ERR — diagnostic Stockfish")
            .setView(scroller)
            .setPositiveButton("COPIER TOUT",(d,w)->{
                ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("GambitDrame GET ERR",report));
                Toast.makeText(this,"Journal copié : coller dans la conversation",Toast.LENGTH_LONG).show();
            })
            .setNeutralButton("PARTAGER",(d,w)->{
                Intent send=new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT,"GambitDrame 0.4.8 GET ERR");
                send.putExtra(Intent.EXTRA_TEXT,report);
                startActivity(Intent.createChooser(send,"Partager le diagnostic GambitDrame"));
            })
            .setNegativeButton("Fermer",null).show();
    }
    private int[] highlightedSquares(){
        String move=null;
        if(trialMode&&!trialMoves.isEmpty())move=trialMoves.get(trialMoves.size()-1);
        else {
            int ply=Math.min(review.ply(),moves.size());
            if(ply>0)move=moves.get(ply-1);
        }
        if(move==null||move.length()<4)return new int[]{-1,-1};
        int f1=move.charAt(0)-'a',r1=move.charAt(1)-'0';
        int f2=move.charAt(2)-'a',r2=move.charAt(3)-'0';
        if(f1<0||f1>7||f2<0||f2>7||r1<1||r1>8||r2<1||r2>8)
            return new int[]{-1,-1};
        return new int[]{(8-r1)*8+f1,(8-r2)*8+f2};
    }
    private ChessGame.Position current(){return positions.get(positions.size()-1);}
    private ChessGame.Position displayed(){
        return trialMode?trialPositions.get(trialPositions.size()-1):positions.get(review.ply());
    }
    private void toggleTrial(){
        if(trialMode){exitTrial();return;}
        stopReplay();
        trialPositions.clear();trialMoves.clear();
        trialPositions.add(displayed());
        trialMode=true;
        revision++;handler.removeCallbacks(botRunnable);
        selected=-1;
        if(trialButton!=null)trialButton.setText("Quitter le mode essais");
        refresh();analyzeDisplayed();
        if(audio!=null)audio.say("Mode essais activé. Vous jouez les deux couleurs sur une copie. La partie reste intacte.");
    }
    private void exitTrial(){
        if(!trialMode)return;
        trialMode=false;
        trialPositions.clear();trialMoves.clear();
        revision++;selected=-1;
        if(trialButton!=null)trialButton.setText("Essayer des coups sur une copie de la position");
        refresh();analyzeDisplayed();scheduleBot();
        if(audio!=null)audio.say("Essais terminés. Partie conservée.");
    }
    private void undoTrial(){
        if(!trialMode)return;
        if(trialPositions.size()>1){
            trialPositions.remove(trialPositions.size()-1);
            trialMoves.remove(trialMoves.size()-1);
            selected=-1;refresh();analyzeDisplayed();
            if(audio!=null)audio.say("Dernier coup d'essai annulé.");
        }
    }
    private void analyzeDisplayed(){
        applyGuardOverlay();
        if(chessView!=null)chessView.setEvaluation(null,null);
        if(fullscreenChessView!=null)fullscreenChessView.setEvaluation(null,null);
        lastWhiteCp=null;lastWhiteMate=null;
        lastEvaluatedFen=null;
        lastSpokenEvaluationFen=null;
        if(stockfishBridge!=null)stockfishBridge.analyze(displayed().fen());
    }
    private int dp(float n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private String adviceText(boolean many,boolean detailed){
        ChessGame.Position p=displayed();
        List<String> played=new ArrayList<>(moves.subList(0,Math.min(review.ply(),moves.size())));
        if(trialMode)played.addAll(trialMoves);
        List<OpeningTheory.Next> theoretical=OpeningTheory.next(played,p);
        List<StockfishBridge.Suggestion> suggestions=stockfishBridge==null?
            new ArrayList<>():stockfishBridge.suggestions(p.fen(),many?3:1);
        StringBuilder sb=new StringBuilder();
        if(!suggestions.isEmpty()){
            sb.append("Conseil Stockfish, profondeur ").append(suggestions.get(0).depth)
                .append(". ");
            int i=0;
            for(StockfishBridge.Suggestion suggestion:suggestions){
                ChessGame.Move matched=null;
                for(ChessGame.Move m:ChessGame.legal(p))
                    if(m.uci().equals(suggestion.uci)){matched=m;break;}
                if(matched==null)continue;
                sb.append(++i).append(". ").append(OpeningCoach.explain(p,matched,detailed))
                    .append(" Évaluation des Blancs : ")
                    .append(String.format(java.util.Locale.FRANCE,"%.2f",
                        suggestion.whiteCp/100.0)).append(" pions. ");
            }
        }else sb.append("Stockfish n'a pas encore terminé l'analyse de cette position. "+
            "Aucun meilleur coup ne peut être certifié pour le moment. ");
        if(!theoretical.isEmpty()){
            sb.append("\nRépertoire théorique local (non exhaustif) : ");
            int n=0;
            for(OpeningTheory.Next next:theoretical){
                if(!many&&n>=1)break;
                ChessGame.Move matched=null;
                for(ChessGame.Move m:ChessGame.legal(p))
                    if(m.uci().equals(next.uci)){matched=m;break;}
                if(matched==null)continue;
                sb.append(next.opening).append(" : ")
                    .append(OpeningCoach.explain(p,matched,detailed))
                    .append(" Idée : ").append(next.idea).append(" ");
                n++;
            }
            sb.append("Un coup hors du petit livre n'est pas nécessairement mauvais.");
        }else if(detailed){
            sb.append("\nAucune suite reconnue dans le petit répertoire local. "+
                "L'analyse Stockfish peut rester valable hors théorie.");
        }
        return sb.toString();
    }
    private void showMoveAdvice(boolean many){
        String shortText=adviceText(many,false),longText=adviceText(many,true);
        engineLog.record("HELP","userRequested multi="+many+" fen="+displayed().fen());
        TextView t=new TextView(this);
        t.setText(shortText);t.setTextSize(17);t.setTextColor(Color.BLACK);
        t.setPadding(dp(12),dp(10),dp(12),dp(12));
        ScrollView scroll=new ScrollView(this);scroll.addView(t);
        new AlertDialog.Builder(this).setTitle(many?
            "Quels coups sont possibles et pourquoi ?":"Quel coup jouer ?")
            .setView(scroll)
            .setPositiveButton("ÉCOUTER",(d,w)->{if(audio!=null)audio.say(shortText);})
            .setNeutralButton("DÉTAILS",(d,w)->new AlertDialog.Builder(this)
                .setTitle("Explications et provenance")
                .setMessage(longText)
                .setPositiveButton("ÉCOUTER",(dd,ww)->{
                    if(audio!=null)audio.say(longText);
                }).setNegativeButton("Fermer",null).show())
            .setNegativeButton("Fermer",null).show();
    }
    private void describeEvaluation(){
        if(audio==null)return;
        String fen=displayed().fen();
        if(!fen.equals(lastEvaluatedFen))audio.say("L'analyse Stockfish est en cours.");
        else audio.say(ChessNarration.evaluation(lastWhiteCp,lastWhiteMate));
    }
    private void readFullBoard(){
        if(audio==null)return;
        ChessGame.Position p=displayed();
        StringBuilder full=new StringBuilder("Lecture de l'échiquier. ");
        for(int rank=0;rank<8;rank++){
            StringBuilder row=new StringBuilder();
            for(int file=0;file<8;file++){
                int sq=rank*8+file;
                char piece=p.board[sq];
                if(piece=='.')continue;
                if(row.length()>0)row.append(". ");
                row.append(Character.isUpperCase(piece)?"blanc ":"noir ")
                    .append(ChessNarration.piece(piece))
                    .append(" ").append(ChessNarration.square(sq));
            }
            if(row.length()>0)full.append("Rangée ").append(8-rank).append(": ")
                .append(row).append(". ");
        }
        audio.say(full.toString());
    }
    private void readLegalMoves(){
        if(audio==null)return;
        ChessGame.Position p=displayed();
        if(review.reviewing()&&!trialMode){
            audio.say("Relecture active. Appuyer sur Reprendre la partie avant de sélectionner un coup.");
            return;
        }
        if(selected<0){
            audio.say("Sélectionner d'abord une pièce pour entendre ses destinations.");
            return;
        }
        StringBuilder desc=new StringBuilder(ChessNarration.squareContents(p,selected));
        desc.append(". Destinations possibles : ");
        int count=0;
        for(ChessGame.Move m:ChessGame.legal(p))if(m.from==selected){
            if(count++>0)desc.append(", ");
            desc.append(ChessNarration.square(m.to));
            if(m.promotion!=0)desc.append(" promotion en ").append(ChessNarration.piece(m.promotion));
        }
        if(count==0)desc.append("aucune.");
        audio.say(desc.toString());
    }
    private void describePosition(){
        if(audio==null)return;
        ChessGame.Position p=displayed();
        int bCount=0,nCount=0,kingWhite=-1,kingBlack=-1;
        for(int i=0;i<64;i++){
            char piece=p.board[i];
            if(piece=='K')kingWhite=i;
            if(piece=='k')kingBlack=i;
            if(piece!='.'){
                if(Character.isUpperCase(piece))bCount++;
                else nCount++;
            }
        }
        String message="Position "+(trialMode?"en essai":review.reviewing()?"en relecture, coup "+review.ply():"en direct")+
            ". Aux "+(p.white?"blancs":"noirs")+" de jouer. "+
            "Roi blanc en "+ChessNarration.square(kingWhite)+
            ". Roi noir en "+ChessNarration.square(kingBlack)+
            ". "+bCount+" pièces blanches, "+nCount+" pièces noires.";
        if(ChessGame.inCheck(p,p.white))message+=" Attention, échec.";
        if(!trialMode&&review.ply()>0)message+=" Dernier coup : "+spokenAt(review.ply());
        audio.say(message);
    }
    private String spokenAt(int ply){
        if(ply<=0||ply>moves.size())return "début de partie";
        ChessGame.Position before=positions.get(ply-1);
        ChessGame.Position after=positions.get(ply);
        for(ChessGame.Move m:ChessGame.legal(before)){
            if(m.uci().equals(moves.get(ply-1)))
                return ChessNarration.move(before,m,after);
        }
        return moves.get(ply-1);
    }
    private Button replayButton(LinearLayout container,String title,String speech,Runnable callback){
        Button button=new Button(this);
        button.setText(title);button.setTextSize(12);
        button.setAllCaps(false);button.setMinHeight(dp(52));
        button.setContentDescription(speech);
        button.setPadding(0,dp(4),0,dp(4));
        container.addView(button,new LinearLayout.LayoutParams(0,dp(66),1));
        button.setOnClickListener(v->callback.run());
        return button;
    }
    private void updatePlaybackUi(){
        if(playbackButton!=null){
            playbackButton.setText(replayPlaying?"⏸\nPause":"▶\nLecture");
            playbackButton.setContentDescription(replayPlaying?
                "Mettre la lecture en pause":"Démarrer la lecture automatique");
        }
        if(replayPosition!=null){
            replayPosition.setText(trialMode?"Mode essais indépendant":
                review.reviewing()?
                "Relecture : "+review.ply()+" / "+moves.size()+" demi-coups":
                "Partie en direct : "+moves.size()+" demi-coups");
        }
    }
    private void stopReplay(){
        handler.removeCallbacks(replayTick);
        replayPlaying=false;
        updatePlaybackUi();
    }
    private void manualSeek(int ply){
        if(trialMode)exitTrial();
        stopReplay();
        seekReview(ply);
    }
    private void seekReview(int ply){
        clearSpeechGate("relecture");
        handler.removeCallbacks(botRunnable);
        revision++; // Drop pending network/bot replies; do not alter recorded moves.
        review.setHistorySize(moves.size());
        review.seek(ply);
        selected=-1;
        refresh();
        analyzeDisplayed();
        if(audio!=null){
            audio.play(AccessibleAudio.Sound.NAVIGATE);
            // GD-015: manual playback navigation is visual; speech only on explicit request.
        }
    }
    private void toggleReplay(){
        if(trialMode)exitTrial();
        if(replayPlaying){stopReplay();return;}
        if(moves.isEmpty()){
            Toast.makeText(this,"Aucun coup à rejouer",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!review.reviewing()||review.ply()>=moves.size())seekReview(0);
        replayPlaying=true;
        updatePlaybackUi();
        handler.postDelayed(replayTick,(audio!=null&&audio.voiceOn())?1850:900);
    }
    private void advanceReplay(){
        if(!replayPlaying)return;
        if(review.ply()<moves.size())seekReview(review.ply()+1);
        if(review.ply()<moves.size())handler.postDelayed(replayTick,(audio!=null&&audio.voiceOn())?1850:900);
        else stopReplay();
    }
    private void leaveReview(){
        stopReplay();
        review.live();
    }
    private void resumeGame(){
        if(trialMode)exitTrial();
        leaveReview();
        revision++;
        selected=-1;
        refresh();
        analyzeDisplayed();
        if(audio!=null)audio.say("Retour à la partie en cours.");
        scheduleBot();
    }
    private void restore(){
        positions.clear();moves.clear();
        positions.add(ChessGame.fromFen(ChessGame.START));
        String raw=prefs.getString("history","");
        try{
            String initial=prefs.getString("baseFen",ChessGame.START);
            if(!ChessGame.START.equals(initial)){
                positions.clear();
                positions.add(ChessGame.fromFen(initial));
            }
            if(!raw.isEmpty())for(String u:raw.split(" ")){
                ChessGame.Position next=ChessGame.play(current(),u);
                positions.add(next);moves.add(u);
            }
        }catch(RuntimeException ex){
            positions.clear();moves.clear();
            positions.add(ChessGame.fromFen(ChessGame.START));
            prefs.edit().remove("history").remove("baseFen").apply();
        }
    }
    private void persist(){prefs.edit().putString("history",android.text.TextUtils.join(" ",moves)).apply();}
    private TextView label(LinearLayout p,String text,int size){
        TextView t=new TextView(this);
        t.setText(text);t.setTextSize(size);t.setTextColor(Color.BLACK);
        t.setPadding(7,8,7,8);p.addView(t);return t;
    }
    private void heading(LinearLayout p,String text,int size){
        TextView v=label(p,text,size);v.setTypeface(null,Typeface.BOLD);
    }
    private void button(LinearLayout p,String text,Runnable action){
        Button b=new Button(this);b.setText(text);b.setTextSize(15);
        p.addView(b,p.getOrientation()==LinearLayout.HORIZONTAL?
          new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2));
        b.setOnClickListener(v->action.run());
    }
    private int repetitionCount(){
        String key=current().repetitionKey();int n=0;
        for(ChessGame.Position p:positions)if(p.repetitionKey().equals(key))n++;
        return n;
    }
    private boolean finished(){return !ChessGame.outcome(current(),repetitionCount()).isEmpty()
        &&(ChessGame.legal(current()).isEmpty()||current().halfmove>=100||repetitionCount()>=3
          ||ChessGame.insufficient(current()));}
    private void refresh(){
        if(chessView==null)return;
        ChessGame.Position p=displayed();
        String outcome=review.reviewing()||trialMode?"":ChessGame.outcome(p,repetitionCount());
        String turn=p.white?"Blancs":"Noirs";
        status.setText(trialMode?"Essai : "+(trialMoves.size())+" coups · au trait : "+turn:
            review.reviewing()?
            "Relecture en pause · trait : "+turn:
            (!outcome.isEmpty()?outcome:"Au trait : "+turn+(botEnabled&&!p.white?" (profil humain)":"")));
        StringBuilder sb=new StringBuilder("Historique (coordonnées) : ");
        for(int i=0;i<moves.size();i++){
            if(i%2==0)sb.append("  ").append(i/2+1).append(". ");
            sb.append(moves.get(i)).append(" ");
        }
        history.setText(sb.toString());
        if(trialStatus!=null)trialStatus.setText(trialMode?
            "Mode essai : "+trialMoves.size()+" coups fictifs, partie inchangée":
            "Essai désactivé · flèches : "+annotations.all().size());
        updatePlaybackUi();
        chessView.invalidate();
        if(fullscreenChessView!=null)fullscreenChessView.invalidate();
    }
    private void reconfigurePolicy(){
        // Keep the random seed stable throughout one game unless Fab explicitly restarts.
        humanPolicy=new HumanPolicy(targetRating,prefs.getLong("humanSeed",42L),aggression);
        if(botInfo!=null)botInfo.setText("Profil visé "+targetRating+" (non calibré) · style "+(int)(aggression*100)+"% agressif");
    }
    private void playMove(ChessGame.Move m){
        if(review.reviewing()&&!trialMode)return;
        ChessGame.Position before=displayed();
        ChessGame.Position after=ChessGame.apply(before,m);
        String narration=ChessNarration.move(before,m,after);
        boolean capture=before.board[m.to]!='.'||
            (Character.toUpperCase(before.board[m.from])=='P'&&m.to==before.ep&&m.from%8!=m.to%8);
        boolean check=ChessGame.inCheck(after,after.white);
        if(trialMode){
            trialPositions.add(after);
            trialMoves.add(m.uci());
        }else{
            positions.add(after);
            moves.add(m.uci());
            review.setHistorySize(moves.size());
            persist();
        }
        selected=-1;revision++;refresh();
        if(audio!=null){
            audio.play(check?AccessibleAudio.Sound.CHECK:
                capture?AccessibleAudio.Sound.CAPTURE:AccessibleAudio.Sound.MOVE);
            String ending=ChessGame.outcome(after,trialMode?0:repetitionCount());
            if(!ending.isEmpty()&&!ending.startsWith("Échec")&&!narration.contains("Nulle"))
                narration+=". "+ending;
            if(!trialMode&&!before.white&&botEnabled){
                audio.sayTracked(narration,ok->engineLog.record("VOICE_BOT","played="+ok));
            }else if(!trialMode&&before.white&&botEnabled){
                speechPending=audio.voiceOn();
                final long generation=++speechSequence;
                engineLog.record("VOICE_WAIT","attente joueur, active="+speechPending+" rev="+revision);
                if(speechPending){
                    audio.sayTracked(narration,ok->releaseSpeechGate(generation,
                        ok?"AudioTrack/UtteranceProgress terminé":"audio interrompu ou échoué"));
                    // Emergency only. Actual audio-completion callback releases first.
                    speechHandler.postDelayed(()->releaseSpeechGate(generation,"secours 45s"),45000);
                }
            }else audio.say(narration);
        }
        analyzeDisplayed();
        scheduleBot();
    }
    private void tryMove(String uci){
        if(review.reviewing()&&!trialMode){
            Toast.makeText(this,"Quitter la relecture pour jouer",Toast.LENGTH_SHORT).show();
            if(audio!=null)audio.say("Quitter la relecture pour jouer.");
            return;
        }
        if(!trialMode&&finished()){
            Toast.makeText(this,"Partie terminée",Toast.LENGTH_SHORT).show();
            if(audio!=null)audio.say("Partie terminée.");
            return;
        }
        if(!trialMode&&botEnabled&&!current().white){
            Toast.makeText(this,"Tour du bot",Toast.LENGTH_SHORT).show();
            if(audio!=null)audio.say("Au tour des noirs.");
            return;
        }
        for(ChessGame.Move m:ChessGame.legal(displayed()))if(m.uci().equals(uci)){playMove(m);return;}
        Toast.makeText(this,"Coup illégal : "+uci,Toast.LENGTH_SHORT).show();
        if(audio!=null){
            audio.play(AccessibleAudio.Sound.INVALID);
            audio.say("Coup illégal : "+uci);
        }
    }
    private void choose(int sq){
        if(review.reviewing()&&!trialMode){
            if(audio!=null)audio.say("La partie est en relecture. Reprendre la partie pour jouer.");
            Toast.makeText(this,"Appuyer sur Reprendre la partie pour jouer",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!trialMode&&(finished()||(botEnabled&&!current().white)))return;
        ChessGame.Position p=displayed();
        char c=p.board[sq];
        if(selected<0){
            if(c!='.'&&Character.isUpperCase(c)==p.white){
                selected=sq;
                if(audio!=null)audio.play(AccessibleAudio.Sound.SELECT);
            }
        }else if(selected==sq){
            selected=-1;
            if(audio!=null)audio.play(AccessibleAudio.Sound.SELECT);
        }
        else {
            List<ChessGame.Move> candidates=new ArrayList<>();
            for(ChessGame.Move m:ChessGame.legal(p))
                if(m.from==selected&&m.to==sq)candidates.add(m);
            if(!candidates.isEmpty()){
                if(candidates.size()>1){
                    final ChessGame.Move[] ms=candidates.toArray(new ChessGame.Move[0]);
                    new AlertDialog.Builder(this).setTitle("Promotion")
                        .setItems(new String[]{"Dame","Tour","Fou","Cavalier"},(dialog,which)->playMove(ms[which]))
                        .show();
                }else playMove(candidates.get(0));
                return;
            }
            selected=(c!='.'&&Character.isUpperCase(c)==p.white)?sq:-1;
            if(audio!=null)audio.play(selected<0?
                AccessibleAudio.Sound.INVALID:AccessibleAudio.Sound.SELECT);
        }
        chessView.invalidate();
        if(fullscreenChessView!=null)fullscreenChessView.invalidate();
    }
    private void scheduleBot(){
        handler.removeCallbacks(botRunnable);
        if(studyBusy)return;
        if(!trialMode&&!review.reviewing()&&botEnabled&&revision!=failedBotRevision&&!current().white&&!finished())
            handler.postDelayed(botRunnable,300);
    }
    private void botMove(){
        if(studyBusy||botRunning||trialMode||review.reviewing()||!botEnabled||current().white||finished())return;
        final ChessGame.Position initial=current();
        final long expectedRevision=revision;
        final int ply=moves.size();
        final boolean online=onlineSwitch!=null&&onlineSwitch.isChecked();
        final HumanPolicy policy=humanPolicy;
        final int engineMode=opponentMode;
        final int target=precisionTarget,spread=precisionSigma;
        final boolean useDeep=deepMode;
        final int useDepth=deepDepth;
        final boolean useGuard=tacticalGuard&&!useDeep;
        final boolean useAdaptive=adaptiveOpponent&&engineMode==1;
        final boolean useTensor=gaugeTensorMode&&engineMode==1;
        final int sensitivity=adaptiveSensitivity,recovery=adaptiveRecovery,stability=adaptiveStability;
        final int maximumDepth=adaptiveMaxDepth;
        final int recoveryGoal=recoveryGoalCp;
        final int redCp=gaugeRedCp,greenCp=gaugeGreenCp;
        if(stockfishBridge!=null)stockfishBridge.setOpeningContext(
            OpeningTheory.measure(moves).name);
        engineLog.record("TURN","START ply="+ply+" rev="+expectedRevision+
            " mode="+engineMode+" target="+target+" sigma="+spread+
            " deep="+useDeep+" depth="+useDepth+" guard="+useGuard+
            " adaptive="+useAdaptive+" sensitivity="+sensitivity+" recovery="+recovery+
            " stability="+stability+" maxDepth="+maximumDepth+
            " goalCp="+recoveryGoal+" tensor="+useTensor+" redCp="+redCp+
            " greenCp="+greenCp+" fen="+initial.fen());
        botRunning=true;
        botInfo.setText(engineMode==1?(useTensor?"Jauge colorée : gris "+target+" %, rouge +"+redCp+"cp, vert "+greenCp+"cp…":useAdaptive?"Adversaire adaptatif : niveau "+target+" %, objectif jauge "+
            String.format(java.util.Locale.FRANCE,"%.2f",recoveryGoal/100.0)+" pion…":useDeep?"Stockfish MultiPV approfondi…":useGuard?"Jauge 11 → garde-fou MultiPV 12…":"Stockfish rapide ancien (profondeur 1)…"):
            "Profil Lichess expérimental : sélection d'un coup…");
        if(engineMode==1&&stockfishBridge!=null&&!useGuard&&!useAdaptive&&!useTensor)stockfishBridge.stop();
        botWorker.execute(()->{
            String chosenUci=null,detail=null,error=null;
            StockfishOpponent.Result engineResult=null;
            try{
                if(engineMode==1){
                    StockfishOpponent.Result result=useTensor?
                        stockfishBridge.selectGuarded(initial,target,spread,true,sensitivity,
                            recovery,stability,maximumDepth,recoveryGoal,true,redCp,greenCp):
                        useAdaptive?
                        stockfishBridge.selectGuarded(initial,target,spread,true,sensitivity,recovery,stability,maximumDepth,recoveryGoal):
                        useGuard?stockfishBridge.selectGuarded(initial,target,spread):
                        stockfishOpponent.select(initial,target,spread,useDeep,useDepth);
                    engineResult=result;
                    if(result!=null){
                        chosenUci=result.uci;detail=result.details;
                        engineLog.record("RESULT",result.details);
                    }
                }else{
                    Map<String,Integer> sampled=explorer.moves(initial,ply,online);
                    HumanPolicy.Choice choice=policy.choose(initial,sampled,ply);
                    if(choice!=null){
                        chosenUci=choice.move.uci();detail=choice.source;
                        engineLog.record("HUMAN_POLICY","move="+chosenUci+" source="+detail);
                    }
                }
            }catch(Exception ex){
                error=ex.getMessage();
                engineLog.error("STOCKFISH_SELECT",ex);
                android.util.Log.w("GambitOpponent","Moteur incapable de sélectionner le coup",ex);
            }
            final String moveUci=chosenUci,explanation=detail,failure=error;
            final StockfishOpponent.Result resolvedResult=engineResult;
            final Runnable applyComputedMove=()->{
                botRunning=false;
                if(trialMode||review.reviewing()||revision!=expectedRevision||!botEnabled||
                   current().white||!current().fen().equals(initial.fen())||opponentMode!=engineMode){
                    engineLog.record("STALE","Discarded pending move="+moveUci+
                        " oldRev="+expectedRevision+" newRev="+revision+
                        " expectedFen="+initial.fen()+" nowFen="+current().fen());
                    scheduleBot();
                    return;
                }
                if(moveUci==null){
                    failedBotRevision=revision;
                    engineLog.record("GET_ERR","Bot blocked: "+(failure==null?"null selected move":failure));
                    updateEngineDebugStatus("ERREUR — ouvrir GET ERR");
                    botInfo.setText("Adversaire indisponible : "+(failure==null?"analyse incomplète":failure)+
                        ". Ajuster un réglage ou recommencer.");
                    if(stockfishBridge!=null)analyzeDisplayed();
                    return;
                }
                for(ChessGame.Move m:ChessGame.legal(current())){
                    if(m.uci().equals(moveUci)){
                        botInfo.setText(explanation==null?moveUci:explanation);
                        if(resolvedResult!=null&&resolvedResult.guarded){
                            ChessGame.Position after=ChessGame.apply(initial,m);
                            guardPreviewFen=after.fen();
                            guardPreviewWhiteCp=resolvedResult.projectedWhiteCp;
                            guardPreviewBestWhiteCp=resolvedResult.bestWhiteCp;
                            guardInfo.setText((useAdaptive?"Mode adaptatif : niveau "+target+
                                " %, barre de récupération "+
                                String.format(java.util.Locale.FRANCE,"%.2f",recoveryGoal/100.0)+
                                " pion, ":"Garde-fou : ")+"perte "+
                                resolvedResult.lossCp+"/"+resolvedResult.limitCp+
                                " centipions. "+(resolvedResult.intervened?
                                    "Coup risqué remplacé.":"Coup accepté.")+
                                " Bande secondaire sur la jauge.");
                        }
                        updateEngineDebugStatus(engineMode==1?
                            "Coup "+moveUci+" : "+(explanation==null?"":explanation):
                            "Coup expérimental Lichess "+moveUci);
                        engineLog.record("APPLY","selected="+moveUci+" target="+target+
                            " sigma="+spread+" mode="+engineMode+" currentFen="+current().fen());
                        playMove(m);
                        return;
                    }
                }
                failedBotRevision=revision;
                engineLog.record("GET_ERR","Selected illegal move="+moveUci+
                    " currentFen="+current().fen());
                updateEngineDebugStatus("ERREUR : coup illégal, ouvrir GET ERR");
                botInfo.setText("Erreur : coup Stockfish devenu illégal. Aucun coup automatique joué.");
            };
            handler.post(()->{
                if(speechPending){
                    waitingBotApply=applyComputedMove;
                    engineLog.record("VOICE_WAIT","Calcul terminé, attente de la diffusion complète du coup humain");
                }else applyComputedMove.run();
            });
        });
    }
    private void reset(){
        clearSpeechGate("nouvelle partie");
        if(stockfishBridge!=null)stockfishBridge.resetPlan();
        engineLog.record("NEW_GAME","New game; kept settings opponent="+opponentMode+
            " target="+precisionTarget+" sigma="+precisionSigma+
            " deep="+deepMode+" depth="+deepDepth);
        updateEngineDebugStatus("Nouvelle partie — niveau et objectif conservés");
        if(trialMode)exitTrial();
        annotations.clear();
        leaveReview();
        revision++;
        handler.removeCallbacks(botRunnable);
        prefs.edit().remove("baseFen").apply();
        moves.clear();positions.clear();positions.add(ChessGame.fromFen(ChessGame.START));
        review.setHistorySize(moves.size());
        selected=-1;persist();refresh();analyzeDisplayed();
        // Voice stays silent for non-move housekeeping (GD-015).
        scheduleBot();
    }
    private void undo(){
        clearSpeechGate("annulation");
        if(trialMode){undoTrial();return;}
        leaveReview();
        revision++;
        handler.removeCallbacks(botRunnable);
        int count=botEnabled&&current().white?2:1;
        while(count-->0&&!moves.isEmpty()){
            moves.remove(moves.size()-1);positions.remove(positions.size()-1);
        }
        review.setHistorySize(moves.size());
        selected=-1;persist();refresh();analyzeDisplayed();
        if(audio!=null)audio.say("Dernier coup annulé. "+(current().white?"Aux blancs":"Aux noirs")+" de jouer.");
        scheduleBot();
    }
    private void importFen(){
        final EditText input=new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setTextSize(16);
        input.setHint("FEN : pièces trait roques en-passant compteurs");
        new AlertDialog.Builder(this).setTitle("Importer une position FEN")
            .setView(input).setPositiveButton("Charger",(d,w)->{
                try{
                    ChessGame.Position p=ChessGame.fromFen(input.getText().toString().trim());
                    if(trialMode)exitTrial();
                    annotations.clear();
                    leaveReview();
                    revision++;
                    handler.removeCallbacks(botRunnable);
                    positions.clear();positions.add(p);moves.clear();selected=-1;
                    review.setHistorySize(0);
                    prefs.edit().putString("baseFen",p.fen()).remove("history").apply();
                    refresh();analyzeDisplayed();
                    if(audio!=null)audio.say("Position chargée. "+(p.white?"Aux blancs":"Aux noirs")+" de jouer.");
                    scheduleBot();
                }catch(RuntimeException ex){
                    new AlertDialog.Builder(this).setTitle("FEN invalide")
                        .setMessage(ex.getMessage()).setPositiveButton("OK",null).show();
                }
            }).setNegativeButton("Annuler",null).show();
    }
    private void loadOpening(){
        reset();
        for(String m:new String[]{"d2d4","d7d5","c2c4"}){
            ChessGame.Position p=ChessGame.play(current(),m);
            moves.add(m);positions.add(p);
        }
        revision++;
        review.setHistorySize(moves.size());
        persist();refresh();analyzeDisplayed();
        if(audio!=null)audio.say("Gambit de dame. Blancs : dé deux, dé quatre. Noirs : dé sept, dé cinq. Blancs : cé deux, cé quatre.");
        scheduleBot();
    }

    private JSONArray studyEntries(){
        try{return new JSONArray(prefs.getString("study-journal-v1","[]"));}
        catch(Exception ignored){return new JSONArray();}
    }
    private void setStudyEntries(JSONArray entries){
        // Bound local storage. Complete JSON backups can be exported before pruning.
        JSONArray kept=new JSONArray();
        int start=Math.max(0,entries.length()-40);
        for(int i=start;i<entries.length();i++)kept.put(entries.opt(i));
        prefs.edit().putString("study-journal-v1",kept.toString()).apply();
        if(studyChart!=null)studyChart.setRecords(kept);
    }
    private void runStudy(){
        if(studyBusy){
            Toast.makeText(this,"Une analyse est déjà en cours.",Toast.LENGTH_SHORT).show();
            return;
        }
        if(botRunning){
            Toast.makeText(this,"L'adversaire réfléchit. Relancer après son coup.",Toast.LENGTH_LONG).show();
            return;
        }
        if(moves.isEmpty()){
            Toast.makeText(this,"Joue quelques coups avant l'analyse.",Toast.LENGTH_LONG).show();
            return;
        }
        if(trialMode){exitTrial();}
        stopReplay();
        revision++;handler.removeCallbacks(botRunnable);
        if(stockfishBridge!=null)stockfishBridge.stop();
        final List<ChessGame.Position> snapshotPositions=new ArrayList<>(positions);
        final List<String> snapshotMoves=new ArrayList<>(moves);
        studyCancelled=false;studyBusy=true;
        studyStatus.setText("Analyse en cours : 0/"+snapshotPositions.size()+" positions · Stockfish profondeur 9.");
        studyWorker.execute(()->{
            JSONObject result=null;String error=null;
            try{
                result=GameStudyEngine.analyze(this,snapshotPositions,snapshotMoves,
                    (done,total)->handler.post(()->{
                        if(!studyCancelled&&studyBusy&&studyStatus!=null)
                            studyStatus.setText("Analyse Stockfish : "+done+"/"+total+" positions.");
                    }),()->studyCancelled||Thread.currentThread().isInterrupted());
            }catch(Exception ex){error=ex.getMessage();}
            final JSONObject completed=result;
            final String failure=error;
            handler.post(()->{
                studyBusy=false;
                if(completed!=null&&!studyCancelled){
                    JSONArray entries=studyEntries();entries.put(completed);
                    setStudyEntries(entries);
                    studyStatus.setText("Analyse sauvegardée · Blancs "+
                        completed.optDouble("whiteAccuracy")+" % ±"+
                        completed.optDouble("whiteSigma")+
                        " · Noirs "+completed.optDouble("blackAccuracy")+" % ±"+
                        completed.optDouble("blackSigma")+
                        "\n"+completed.optString("theoryText"));
                    engineLog.record("STUDY","Analyse sauvegardée : "+snapshotMoves.size()+
                        " coups, profondeur9, "+completed.optString("opening"));
                }else{
                    studyStatus.setText(studyCancelled?"Analyse interrompue ; aucune donnée partielle sauvegardée.":
                        "Analyse impossible : "+(failure==null?"erreur inconnue":failure));
                    engineLog.record("STUDY_ERR",String.valueOf(failure));
                }
                if(stockfishBridge!=null)analyzeDisplayed();
                scheduleBot();
            });
        });
    }
    private String studyDescription(JSONObject item){
        if(item==null)return "Entrée invalide";
        String date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,
            java.util.Locale.FRANCE).format(new Date(item.optLong("createdAt",0L)));
        return date+" · "+item.optString("result","*")+" · "+item.optString("opening","ouverture inconnue")+
            " · Blancs "+item.optDouble("whiteAccuracy",0)+" % / Noirs "+
            item.optDouble("blackAccuracy",0)+" %";
    }
    private String studyReport(JSONObject item){
        StringBuilder b=new StringBuilder(studyDescription(item));
        b.append("\n").append(item.optString("theoryText"));
        b.append("\nBlancs : variabilité ").append(item.optDouble("whiteSigma"))
            .append(" points, erreurs graves ").append(item.optInt("whiteBlunders"))
            .append(", récupération : ").append(item.optString("whiteRecovery"));
        b.append("\nNoirs : variabilité ").append(item.optDouble("blackSigma"))
            .append(" points, erreurs graves ").append(item.optInt("blackBlunders"))
            .append(", récupération : ").append(item.optString("blackRecovery"));
        b.append("\nProfondeur moteur : ").append(item.optInt("depth",9))
            .append(". Précision indicative interne, pas Accuracy officielle ni sigma du curseur.");
        JSONArray rows=item.optJSONArray("perMove");
        if(rows!=null)for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);
            if(row!=null)b.append("\n").append(i/2+1).append(i%2==0?". Blancs ":"... Noirs ")
                .append(row.optString("uci")).append(" : perte ")
                .append(row.optInt("lossCp")).append(" cp, précision ")
                .append(row.optDouble("accuracy")).append(" %");
        }
        return b.toString();
    }
    private void showOpeningSuccess(){
        JSONArray entries=studyEntries();
        java.util.Map<String,int[]> openings=new java.util.TreeMap<>();
        for(int i=0;i<entries.length();i++){
            JSONObject r=entries.optJSONObject(i);
            if(r==null)continue;
            String outcome=r.optString("result","*");
            if(outcome.equals("*"))continue; // Unfinished matches must not count as defeats.
            String name=r.optString("opening","Ouverture non répertoriée");
            int[] stats=openings.get(name);
            if(stats==null){stats=new int[4];openings.put(name,stats);}
            stats[0]++;
            if(outcome.equals("1-0"))stats[1]++;
            else if(outcome.equals("0-1"))stats[2]++;
            else if(outcome.equals("1/2-1/2"))stats[3]++;
        }
        StringBuilder report=new StringBuilder("Résultats réels des parties terminées, par ouverture détectée. "+
            "L'ouverture détectée vient d'un petit répertoire local, pas d'une base exhaustive. "+
            "Le taux est calculé du point de vue des Blancs : (victoires + ½ nulles) / parties.\n\n");
        if(openings.isEmpty())report.append("Pas encore de partie terminée et analysée.");
        for(java.util.Map.Entry<String,int[]> e:openings.entrySet()){
            int[] st=e.getValue();
            double rate=100.0*(st[1]+0.5*st[3])/st[0];
            report.append(e.getKey()).append(" : ")
                .append(st[0]).append(" partie(s), ")
                .append(st[1]).append(" gagnée(s), ")
                .append(st[3]).append(" nulle(s), ")
                .append(st[2]).append(" perdue(s) · réussite Blancs ")
                .append(String.format(java.util.Locale.FRANCE,"%.1f%%",rate))
                .append(st[0]<10?" (échantillon faible)":"").append("\n");
        }
        TextView text=new TextView(this);text.setText(report.toString());text.setTextSize(17);
        text.setTextIsSelectable(true);text.setPadding(dp(12),dp(12),dp(12),dp(12));
        ScrollView scroll=new ScrollView(this);scroll.addView(text);
        new AlertDialog.Builder(this).setTitle("Réussite par ouverture")
            .setView(scroll).setPositiveButton("Fermer",null).show();
    }

    private void showStudyJournal(){
        JSONArray entries=studyEntries();
        if(entries.length()==0){
            new AlertDialog.Builder(this).setTitle("Journal des analyses")
                .setMessage("Aucune partie analysée. Le journal est conservé localement après chaque analyse terminée.")
                .setPositiveButton("Fermer",null).show();
            return;
        }
        String[] names=new String[entries.length()];
        for(int i=0;i<names.length;i++)
            names[i]=studyDescription(entries.optJSONObject(entries.length()-1-i));
        new AlertDialog.Builder(this).setTitle("Journal · "+names.length+" analyses")
            .setItems(names,(d,which)->{
                JSONObject record=entries.optJSONObject(entries.length()-1-which);
                if(record==null)return;
                String report=studyReport(record);
                TextView details=new TextView(this);
                details.setText(report);details.setTextSize(17);details.setTextIsSelectable(true);
                details.setPadding(dp(14),dp(12),dp(14),dp(12));
                ScrollView scroll=new ScrollView(this);scroll.addView(details);
                new AlertDialog.Builder(this).setTitle("Analyse détaillée")
                    .setView(scroll)
                    .setPositiveButton("Copier",(dialog,b)->{
                        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("Analyse GambitDrame",report));
                    }).setNegativeButton("Fermer",null).show();
            }).setNegativeButton("Fermer",null).show();
    }
    private JSONObject exportStudyData()throws Exception{
        JSONObject root=new JSONObject();
        root.put("format","GambitDrame-study-journal");
        root.put("version",1);
        root.put("exportedAt",System.currentTimeMillis());
        root.put("entries",studyEntries());
        return root;
    }
    private void exportStudyJournal(){
        if(studyEntries().length()==0){
            Toast.makeText(this,"Aucune analyse à exporter.",Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE,"GambitDrame-analyses.json");
        startActivityForResult(intent,STUDY_EXPORT);
    }
    private void importStudyJournal(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});
        startActivityForResult(intent,STUDY_IMPORT);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        if(request==STUDY_EXPORT){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                if(out==null)throw new IllegalStateException("Impossible d'ouvrir le fichier");
                byte[] json=exportStudyData().toString(2).getBytes(StandardCharsets.UTF_8);
                out.write(json);
                Toast.makeText(this,"Journal JSON exporté.",Toast.LENGTH_LONG).show();
            }catch(Exception ex){
                Toast.makeText(this,"Export échoué : "+ex.getMessage(),Toast.LENGTH_LONG).show();
            }
        }else if(request==STUDY_IMPORT){
            try(InputStream in=getContentResolver().openInputStream(data.getData())){
                if(in==null)throw new IllegalArgumentException("Fichier illisible");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                byte[] chunk=new byte[8192];int n;
                while((n=in.read(chunk))!=-1){
                    if(bytes.size()+n>2_000_000)throw new IllegalArgumentException("Journal supérieur à 2 Mo");
                    bytes.write(chunk,0,n);
                }
                JSONObject root=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
                if(!"GambitDrame-study-journal".equals(root.optString("format"))||
                    root.optInt("version")!=1)throw new IllegalArgumentException("Format de journal inconnu");
                JSONArray imported=root.getJSONArray("entries"),original=studyEntries();
                if(imported.length()>100)throw new IllegalArgumentException("Trop d'entrées");
                JSONArray combined=new JSONArray();
                java.util.HashSet<String> keys=new java.util.HashSet<>();
                for(int i=0;i<original.length();i++){
                    JSONObject record=original.optJSONObject(i);
                    if(record!=null){
                        combined.put(record);
                        keys.add(studyKey(record));
                    }
                }
                int added=0;
                for(int i=0;i<imported.length();i++){
                    JSONObject record=imported.optJSONObject(i);
                    if(!validStudy(record))throw new IllegalArgumentException("Partie "+(i+1)+" invalide");
                    String key=studyKey(record);
                    if(keys.add(key)){combined.put(record);added++;}
                }
                setStudyEntries(combined);
                studyStatus.setText(added+" analyse(s) importée(s), partie en cours conservée.");
                Toast.makeText(this,added+" analyses ajoutées au journal.",Toast.LENGTH_LONG).show();
            }catch(Exception ex){
                Toast.makeText(this,"Import annulé : "+ex.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
    }
    private static String studyKey(JSONObject study){
        JSONArray list=study.optJSONArray("uciMoves");
        return study.optLong("createdAt")+"|"+study.optString("baseFen")+"|"+
            (list==null?"":list.toString());
    }
    private static boolean validStudy(JSONObject study){
        if(study==null||study.optInt("schema")!=1)return false;
        JSONArray moves=study.optJSONArray("uciMoves"),rows=study.optJSONArray("perMove");
        if(moves==null||rows==null||moves.length()<1||moves.length()>512||
           moves.length()!=rows.length())return false;
        for(String metric:new String[]{"whiteAccuracy","blackAccuracy","whiteSigma","blackSigma"}){
            double v=study.optDouble(metric,Double.NaN);
            if(!Double.isFinite(v)||v<0||v>100)return false;
        }
        try{
            ChessGame.Position p=ChessGame.fromFen(study.getString("baseFen"));
            for(int i=0;i<moves.length();i++){
                JSONObject row=rows.optJSONObject(i);
                String uci=moves.getString(i);
                if(row==null||!uci.equals(row.optString("uci"))||row.optInt("lossCp",-1)<0)return false;
                p=ChessGame.play(p,uci);
            }
            return true;
        }catch(Exception ex){return false;}
    }

    @Override protected void onPause(){
        clearSpeechGate("pause Android");
        studyCancelled=true;
        stopReplay();
        if(audio!=null)audio.stopSpeech();
        if(stockfishBridge!=null)stockfishBridge.stop();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(stockfishBridge!=null)analyzeDisplayed();
        scheduleBot();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(botRunnable);
        stopReplay();
        revision++;
        botWorker.shutdownNow();
        studyCancelled=true;
        studyWorker.shutdownNow();
        if(stockfishOpponent!=null)stockfishOpponent.close();
        if(audio!=null)audio.close();
        if(stockfishBridge!=null)stockfishBridge.close();
        super.onDestroy();
    }
    private void addAdaptiveSlider(LinearLayout parent,String name,int initial,
                                   java.util.function.IntConsumer onChange){
        TextView title=label(parent,name+" : "+initial,14);
        SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(initial);
        parent.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                if(!fromUser)return;
                onChange.accept(value);title.setText(name+" : "+value);
                engineLog.record("SETTING",name+"="+value);
                if(stockfishBridge!=null)stockfishBridge.cancelGuard();
                revision++;failedBotRevision=-1;scheduleBot();
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private void releaseSpeechGate(long generation,String cause){
        if(generation!=speechSequence||!speechPending)return;
        speechPending=false;
        engineLog.record("VOICE_END",cause+" rev="+revision);
        Runnable run=waitingBotApply;waitingBotApply=null;
        if(run!=null)run.run();
    }
    private void clearSpeechGate(String why){
        ++speechSequence;
        speechPending=false;waitingBotApply=null;
        if(engineLog!=null)engineLog.record("VOICE_CANCEL",why);
    }
    private final class ChessView extends View {
        private final Paint paint=new Paint(3);
        private final String symbols="♔♕♖♗♘♙♚♛♜♝♞♟";
        private int arrowStart=-1,arrowEnd=-1,arrowColor=0;
        private Integer whiteCp=null,whiteMate=null;
        private double whiteFraction=0.5;
        private Double guardFraction=null,bestFraction=null;
        void setGuardEstimate(Integer projectedWhiteCp,Integer bestWhiteCp){
            guardFraction=projectedWhiteCp==null?null:
                EvaluationScale.whiteFraction(projectedWhiteCp,null);
            bestFraction=bestWhiteCp==null?null:
                EvaluationScale.whiteFraction(bestWhiteCp,null);
            invalidate();
        }
        private final int gaugeWidth=dp(75);
        private int draggedGaugeLane=-1;
        ChessView(){
            super(MainActivity.this);
            setContentDescription("Échiquier tactile avec jauge Stockfish. Alternative accessible : saisir e2e4.");
        }
        void setEvaluation(Integer cp,Integer mate){
            whiteCp=cp;whiteMate=mate;
            whiteFraction=EvaluationScale.whiteFraction(cp,mate);
            setContentDescription("Échiquier. Évaluation Stockfish côté blancs "+
                EvaluationScale.label(cp,mate)+". Entrer un coup par coordonnées si besoin.");
            invalidate();
        }
        @Override protected void onMeasure(int w,int h){
            int width=MeasureSpec.getSize(w);
            setMeasuredDimension(width,Math.max(1,width-gaugeWidth));
        }
        private int gaugeCpAt(float yFraction){
            // Monotone bounded inverse to the graphical sigmoid. It keeps a
            // finger on the same point of the gauge after a drag.
            double fraction=Math.min(.979999,Math.max(.020001,1-yFraction));
            double x=(.96/(fraction-.02))-1;
            return (int)Math.round(-240.0*Math.log(x));
        }
        private void drawColoredHandle(Canvas canvas,float x,float y,int color,String symbol,float height){
            float radius=dp(9);
            y=Math.max(radius+2,Math.min(height-radius-2,y));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff121b1e);
            canvas.drawCircle(x,y,radius+dp(2),paint);
            paint.setColor(color);
            canvas.drawCircle(x,y,radius,paint);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            paint.setTextSize(dp(11));paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(0xff101417);
            canvas.drawText(symbol,x,y-(paint.ascent()+paint.descent())/2,paint);
        }
        private void drawGauge(Canvas canvas,int boardSide){
            float h=boardSide,bar=gaugeWidth-3f;
            paint.setStyle(Paint.Style.FILL);
            // Black on top and white on bottom regardless of who has the move.
            paint.setColor(Color.BLACK);
            canvas.drawRect(1,0,bar,h,paint);
            paint.setColor(Color.WHITE);
            canvas.drawRect(1,(float)(h*(1.0-whiteFraction)),bar,h,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0xff4b514f);
            canvas.drawRect(1,1,bar,h-1,paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(dp(14));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            paint.setColor(Color.WHITE);
            canvas.drawText("N",bar/2,dp(17),paint);
            paint.setColor(Color.BLACK);
            canvas.drawText("B",bar/2,h-dp(7),paint);
            // Narrow independent gauge immediately adjacent to the main gauge.
            // Projected evaluation depth12 is drawn vertically with its own split,
            // while the main evaluation remains depth11.
            if(guardFraction!=null){
                float gLeft=bar-dp(11),gRight=bar-dp(2);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xff173e48);
                canvas.drawRect(gLeft,0,gRight,h,paint);
                paint.setColor(0xffb7eff0);
                canvas.drawRect(gLeft,(float)(h*(1-guardFraction)),gRight,h,paint);
                paint.setColor(0xffed9a25);
                float guardCut=(float)(h*(1-guardFraction));
                canvas.drawRect(gLeft-dp(1),guardCut-dp(1),gRight+dp(1),guardCut+dp(1),paint);
                if(bestFraction!=null){
                    float bestY=(float)(h*(1-bestFraction));
                    paint.setColor(0xffff5b6a);
                    canvas.drawRect(gLeft-dp(2),bestY-dp(1),gRight+dp(1),bestY+dp(1),paint);
                }
            }
            // Clear split marker makes small shifts perceptible.
            float cut=(float)(h*(1.0-whiteFraction));
            paint.setColor(0xffd0a332);
            canvas.drawRect(1,Math.max(0,cut-dp(1)),bar,Math.min(h,cut+dp(1)),paint);
            if(gaugeTensorMode&&opponentMode==1){
                float x1=dp(12),x2=dp(36),x3=dp(60);
                float greyY=(float)(h*(1-precisionTarget/100.0));
                float redY=(float)(h*(1-EvaluationScale.whiteFraction(-gaugeRedCp,null)));
                float greenY=(float)(h*(1-EvaluationScale.whiteFraction(-gaugeGreenCp,null)));
                // Three separate thumb lanes; the board itself remains untouched.
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));
                paint.setColor(0x77a0a0a0);
                canvas.drawLine(x1,dp(2),x1,h-dp(2),paint);
                canvas.drawLine(x2,dp(2),x2,h-dp(2),paint);
                canvas.drawLine(x3,dp(2),x3,h-dp(2),paint);
                paint.setStyle(Paint.Style.FILL);
                drawColoredHandle(canvas,x1,greyY,0xffaeb0b5,"N",h);
                drawColoredHandle(canvas,x2,redY,0xfff25b50,"R",h);
                drawColoredHandle(canvas,x3,greenY,0xff55cf7d,"V",h);
            }
            if(!gaugeTensorMode&&adaptiveOpponent&&opponentMode==1){
                // Turquoise goal is a positional threshold for BLACK; it is
                // deliberately independent of the nominal playing-level slider.
                float goalY=(float)(h*(1-EvaluationScale.whiteFraction(-recoveryGoalCp,null)));
                paint.setColor(0xff153640);
                canvas.drawRect(1,Math.max(0,goalY-dp(3)),bar,Math.min(h,goalY+dp(3)),paint);
                paint.setColor(0xff35d6e2);
                canvas.drawRect(1,Math.max(0,goalY-dp(2)),bar,Math.min(h,goalY+dp(2)),paint);
            }
        }
        @Override protected void onDraw(Canvas canvas){
            int side=getWidth()-gaugeWidth;
            if(side<=0)return;
            drawGauge(canvas,side);
            canvas.save();
            canvas.translate(gaugeWidth,0);
            float cell=side/8f;char[] b=displayed().board;
            int[] last=highlightedSquares();
            for(int i=0;i<64;i++){
                int x=i%8,y=i/8;float left=x*cell,top=y*cell;
                boolean light=(x+y)%2==0;
                paint.setStyle(Paint.Style.FILL);
                if(pieceTheme==2)paint.setColor(light?0xfff5f6ee:0xff586b75);
                else if(pieceTheme==1)paint.setColor(light?0xffeae5d9:0xff789087);
                else if(pieceTheme>=3)paint.setColor(light?0xffe3e6d2:0xff78927b);
                else paint.setColor(light?0xffdde4cf:0xff7d9582);
                canvas.drawRect(left,top,left+cell,top+cell,paint);
                if(i==last[0]||i==last[1]){
                    // Bold contrasting square corners remain visible on light and dark
                    // squares without covering the wooden piece or colored arrows.
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(dp(3),cell*.07f));
                    paint.setColor(i==last[0]?0xff1861c9:0xffffa50c);
                    float inset=cell*.055f;
                    canvas.drawRoundRect(left+inset,top+inset,
                        left+cell-inset,top+cell-inset,cell*.11f,cell*.11f,paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                if(selected==i){
                    paint.setColor(0xfffac740);
                    canvas.drawRect(left+2,top+2,left+cell-2,top+cell-2,paint);
                }
                char piece=b[i];
                if(piece!='.'){
                    if(pieceTheme>=3){
                        WoodPieces.draw(canvas,piece,left+cell/2f,top+cell/2f,cell,pieceTheme==4);
                    }else{
                    boolean white=Character.isUpperCase(piece);
                    String glyph=glyph(piece);
                    float cx=left+cell/2f, cy=top+cell/2f;
                    // A coherent contrasting base makes Unicode chess glyphs legible
                    // even on a textured or similarly-colored square.
                    if(pieceTheme!=1){
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(white?
                            (pieceTheme==2?0xffffffff:0xfffff4d8):
                            (pieceTheme==2?0xff090c10:0xff342118));
                        canvas.drawCircle(cx,cy,cell*.42f,paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(cell*(pieceTheme==2?.075f:.042f));
                        paint.setColor(white?0xff282019:0xffffe4ab);
                        canvas.drawCircle(cx,cy,cell*.42f,paint);
                    }
                    paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(cell*(pieceTheme==1?.81f:.67f));
                    float base=cy-(paint.ascent()+paint.descent())*.5f;
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(dp(2),cell*(pieceTheme==2?.09f:.065f)));
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    paint.setColor(white?0xff211c17:0xffffe8bd);
                    canvas.drawText(glyph,cx,base,paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(white?(pieceTheme==2?0xffffffff:0xfffff3d0):
                        (pieceTheme==2?0xff080c12:0xff2c1b15));
                    canvas.drawText(glyph,cx,base,paint);
                    }
                }
            }
            if(selected>=0){
                paint.setStyle(Paint.Style.FILL);paint.setColor(0x9996d644);
                for(ChessGame.Move m:ChessGame.legal(displayed()))if(m.from==selected){
                    int x=m.to%8,y=m.to/8;
                    canvas.drawCircle((x+.5f)*cell,(y+.5f)*cell,cell*.13f,paint);
                }
            }
            drawArrows(canvas,cell);
            canvas.restore();
        }
        private void drawArrows(Canvas canvas,float cell){
            for(BoardArrows.Arrow arrow:annotations.all())
                drawVector(canvas,cell,arrow.from,arrow.to,arrow.color);
            // Preview and stored arrow must have exactly the same piece-specific hue.
            if(arrowMode&&arrowStart>=0&&arrowEnd>=0&&arrowEnd!=arrowStart)
                drawVector(canvas,cell,arrowStart,arrowEnd,arrowColor);
        }
        private void drawVector(Canvas canvas,float cell,int from,int to,int color){
            float ax=(from%8+.5f)*cell,ay=(from/8+.5f)*cell;
            float bx=(to%8+.5f)*cell,by=(to/8+.5f)*cell;
            float dx=bx-ax,dy=by-ay,d=(float)Math.hypot(dx,dy);
            if(d<cell*.5f)return;
            float ux=dx/d,uy=dy/d;
            float tipX=bx-ux*cell*.18f,tipY=by-uy*cell*.18f;
            float lineEndX=tipX-ux*cell*.22f,lineEndY=tipY-uy*cell*.22f;
            // Thin graphite underlay adds contrast to vivid translucent hues on both squares.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0x88502719);
            paint.setStrokeWidth(Math.max(dp(4),cell*.16f));
            canvas.drawLine(ax,ay,lineEndX,lineEndY,paint);
            paint.setColor(color);
            paint.setStrokeWidth(Math.max(dp(3),cell*.105f));
            canvas.drawLine(ax,ay,lineEndX,lineEndY,paint);
            Path head=new Path();
            head.moveTo(tipX,tipY);
            head.lineTo(tipX-ux*cell*.41f-uy*cell*.24f,
                tipY-uy*cell*.41f+ux*cell*.24f);
            head.lineTo(tipX-ux*cell*.41f+uy*cell*.24f,
                tipY-uy*cell*.41f-ux*cell*.24f);
            head.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x99502719);
            canvas.drawPath(head,paint);
            // Slightly inset colored head over the dark outer shape.
            paint.setColor(color);
            canvas.drawPath(head,paint);
        }
        private String glyph(char p){
            String all="KQRBNPkqrbnp";
            int i=all.indexOf(p);
            return i<0?"":symbols.substring(i,i+1);
        }
        private int squareAt(float x,float y){
            int side=getWidth()-gaugeWidth;
            if(side<=0||x<gaugeWidth||y<0||y>=side||x>=getWidth())return -1;
            int file=(int)((x-gaugeWidth)*8/side);
            int rank=(int)(y*8/side);
            return file>=0&&file<8&&rank>=0&&rank<8?rank*8+file:-1;
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked();
            if(gaugeTensorMode&&opponentMode==1&&
               (draggedGaugeLane>=0||e.getX()<gaugeWidth)){
                if(action==MotionEvent.ACTION_DOWN){
                    draggedGaugeLane=Math.min(2,Math.max(0,(int)(e.getX()*3/gaugeWidth)));
                    if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                if(draggedGaugeLane>=0&&(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP)){
                    float pos=Math.max(0,Math.min(1,e.getY()/Math.max(1f,getHeight())));
                    if(draggedGaugeLane==0)
                        precisionTarget=Math.max(0,Math.min(100,Math.round((1-pos)*100)));
                    else {
                        // Gauge height tracks centipawns through EvaluationScale.
                        // Snap to 25 cp and constrain red/green to their own side.
                        int whiteCp=gaugeCpAt(pos);
                        int blackCp=-whiteCp;
                        if(draggedGaugeLane==1)
                            gaugeRedCp=25*Math.max(2,Math.min(32,Math.round(blackCp/25f)));
                        else gaugeGreenCp=-25*Math.max(2,Math.min(32,Math.round(-blackCp/25f)));
                    }
                    prefs.edit().putInt("precisionTarget",precisionTarget)
                        .putInt("gd017RedCp",gaugeRedCp)
                        .putInt("gd017GreenCp",gaugeGreenCp).apply();
                    if(legacyPrecisionLabel!=null)
                        legacyPrecisionLabel.setText("Niveau habituel Stockfish : "+precisionTarget+" %");
                    if(legacyPrecisionSlider!=null)
                        legacyPrecisionSlider.setProgress(precisionTarget);
                    updateEngineDebugStatus("Réglage sur jauge");
                    if(gaugeLegend!=null)gaugeLegend.setText(gaugeLegendText());
                    invalidateAllBoards();
                    if(action==MotionEvent.ACTION_UP){
                        engineLog.record("GAUGE_TOUCH","lane="+draggedGaugeLane+
                            " nominal="+precisionTarget+" redCp="+gaugeRedCp+
                            " greenCp="+gaugeGreenCp+" currentWhiteCp="+lastWhiteCp);
                        draggedGaugeLane=-1;
                        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                        if(stockfishBridge!=null)stockfishBridge.cancelGuard();
                        revision++;failedBotRevision=-1;scheduleBot();
                        performClick();
                    }
                    return true;
                }
                if(action==MotionEvent.ACTION_CANCEL){
                    draggedGaugeLane=-1;
                    if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
            }
            if(arrowMode){
                if(action==MotionEvent.ACTION_DOWN){
                    getParent().requestDisallowInterceptTouchEvent(true);
                    arrowStart=squareAt(e.getX(),e.getY());
                    arrowEnd=-1;
                    if(arrowStart>=0){
                        List<String> path=new ArrayList<>(moves.subList(0,
                            Math.min(review.ply(),moves.size())));
                        if(trialMode)path.addAll(trialMoves);
                        int pieceId=BoardArrows.identityAt(positions.get(0),path,arrowStart);
                        // An empty origin remains colorful but does not impersonate a piece.
                        arrowColor=BoardArrows.colorForIdentity(
                            pieceId>=0?pieceId:32+arrowStart);
                    }
                    return true;
                }
                if(action==MotionEvent.ACTION_MOVE){
                    int next=squareAt(e.getX(),e.getY());
                    if(arrowEnd!=next){arrowEnd=next;invalidate();}
                    return true;
                }
                if(action==MotionEvent.ACTION_UP){
                    int target=squareAt(e.getX(),e.getY());
                    if(arrowStart>=0&&target>=0&&target!=arrowStart){
                        annotations.toggle(arrowStart,target,arrowColor);
                        // Fab: announce only the destination, not the full vector.
                        if(audio!=null)audio.say(ChessNarration.square(target));
                        refresh();
                    }
                    arrowStart=-1;arrowEnd=-1;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    invalidate();
                    performClick();return true;
                }
                if(action==MotionEvent.ACTION_CANCEL){arrowStart=-1;arrowEnd=-1;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;}
                return true;
            }
            if(action!=MotionEvent.ACTION_UP)return true;
            int sq=squareAt(e.getX(),e.getY());
            if(sq>=0)choose(sq);
            performClick();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
