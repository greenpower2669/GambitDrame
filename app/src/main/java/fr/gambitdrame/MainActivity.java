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
    private boolean deepMode=false;
    private int deepDepth=7;
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
        deepMode=prefs.getBoolean("precisionDeep",false);
        deepDepth=prefs.getInt("precisionDepth",7);
        pieceTheme=prefs.getInt("pieceTheme",3);
        stockfishOpponent=new StockfishOpponent(this);
        stockfishOpponent.setDiagnostics(engineLog);
        engineLog.record("APP","GambitDrame v0.4.2 launched; mode="+opponentMode+
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
        label(root,"Version 0.4.2 — diagnostic Stockfish / GET ERR, bois 3D et plein écran",16);
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
        voiceToggle.setText("Parler : annoncer les coups et les cases");
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
        Switch evaluationToggle=new Switch(this);
        evaluationToggle.setText("Annoncer automatiquement une évaluation Stockfish par position");
        evaluationToggle.setTextColor(Color.BLACK);
        evaluationToggle.setTextSize(14);
        announceEvaluation=prefs.getBoolean("autoEvalSpeech",false);
        evaluationToggle.setChecked(announceEvaluation);
        root.addView(evaluationToggle);
        evaluationToggle.setOnCheckedChangeListener((button,on)->{
            announceEvaluation=on;
            prefs.edit().putBoolean("autoEvalSpeech",on).apply();
            if(on)describeEvaluation();
        });
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
        Switch advanced=new Switch(this);
        advanced.setText("Activer MultiPV approfondi (désactivé par défaut)");
        advanced.setTextColor(Color.BLACK);
        advanced.setTextSize(16);advanced.setChecked(deepMode);
        root.addView(advanced);
        TextView advancedText=label(root,"Profondeur du mode approfondi : "+deepDepth,15);
        SeekBar advancedDepth=new SeekBar(this);
        advancedDepth.setMax(19);advancedDepth.setProgress(deepDepth-1);
        advancedDepth.setEnabled(deepMode);
        root.addView(advancedDepth);
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
            prefs.edit().putBoolean("precisionDeep",on).apply();
            engineLog.record("SETTING","advancedMultiPV="+on+" depth="+deepDepth);
            updateEngineDebugStatus("Mode de calcul modifié");
            revision++;failedBotRevision=-1;scheduleBot();
        });
        label(root,"Rapide (défaut) : chaque coup légal est joué sur une copie, puis sa position est évaluée à profondeur 1.",14);
        label(root,"Stockfish classe TOUS les coups légaux entre pire=0 et meilleur=100. Il s'agit d'une qualité RELATIVE à cette position, pas de l'indice de précision Chess.com ni d'un Elo.",13);
        TextView precisionLabel=label(root,"Précision cible Stockfish : "+precisionTarget+" %",16);
        SeekBar targetSlider=new SeekBar(this);
        targetSlider.setMax(100);
        targetSlider.setProgress(precisionTarget);
        root.addView(targetSlider);
        targetSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                if(!fromUser)return;
                precisionTarget=value;
                precisionLabel.setText("Précision cible Stockfish : "+value+" %");
                prefs.edit().putInt("precisionTarget",value).apply();
                engineLog.record("SETTING","precisionTarget="+value+"%");
                updateEngineDebugStatus("Cible modifiée");
                revision++;failedBotRevision=-1;scheduleBot();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        TextView deviationLabel=label(root,"Variabilité habituelle : ± "+precisionSigma+" points",16);
        SeekBar varianceSlider=new SeekBar(this);
        varianceSlider.setMax(30);
        varianceSlider.setProgress(precisionSigma);
        root.addView(varianceSlider);
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
            if(whiteCp!=null||whiteMate!=null){
                chessView.setEvaluation(whiteCp,whiteMate);
                if(fullscreenChessView!=null)fullscreenChessView.setEvaluation(whiteCp,whiteMate);
                lastWhiteCp=whiteCp;
                lastWhiteMate=whiteMate;
                lastEvaluatedFen=fen;
                if(announceEvaluation&&fen!=null&&!fen.equals(lastSpokenEvaluationFen)&&
                   message.contains("profondeur 11")){
                    lastSpokenEvaluationFen=fen;
                    audio.say(ChessNarration.evaluation(whiteCp,whiteMate));
                }
            }
        });
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
    private void updateEngineDebugStatus(String description){
        if(engineDebugStatus!=null)
            engineDebugStatus.setText("Stockfish : "+(opponentMode==1?(deepMode?"MultiPV":"rapide"):"Lichess")+
                " | cible "+precisionTarget+" % | sigma "+precisionSigma+
                " | "+description);
    }
    private String currentEngineReport(){
        return "GambitDrame 0.4.2 / Android "+Build.VERSION.RELEASE+
            "\nMode adversaire="+opponentMode+" (0=Lichess, 1=Stockfish)"+
            "\nMoteur approfondi="+deepMode+" / profondeur="+deepDepth+
            "\nCurseur précision="+precisionTarget+" % / variabilité sigma="+precisionSigma+
            "\nPersisté="+prefs.getInt("precisionTarget",-1)+"/"+prefs.getInt("precisionSigma",-1)+
            " mode="+prefs.getInt("opponentMode",-1)+
            "\nHistorique coups="+android.text.TextUtils.join(" ",moves)+
            "\nFEN="+current().fen()+
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
                send.putExtra(Intent.EXTRA_SUBJECT,"GambitDrame 0.4.2 GET ERR");
                send.putExtra(Intent.EXTRA_TEXT,report);
                startActivity(Intent.createChooser(send,"Partager le diagnostic GambitDrame"));
            })
            .setNegativeButton("Fermer",null).show();
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
        if(chessView!=null)chessView.setEvaluation(null,null);
        if(fullscreenChessView!=null)fullscreenChessView.setEvaluation(null,null);
        lastWhiteCp=null;lastWhiteMate=null;
        lastEvaluatedFen=null;
        lastSpokenEvaluationFen=null;
        if(stockfishBridge!=null)stockfishBridge.analyze(displayed().fen());
    }
    private int dp(float n){return Math.round(getResources().getDisplayMetrics().density*n);}
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
        handler.removeCallbacks(botRunnable);
        revision++; // Drop pending network/bot replies; do not alter recorded moves.
        review.setHistorySize(moves.size());
        review.seek(ply);
        selected=-1;
        refresh();
        analyzeDisplayed();
        if(audio!=null){
            audio.play(AccessibleAudio.Sound.NAVIGATE);
            audio.say(review.ply()==0?"Début de la partie.":spokenAt(review.ply()));
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
            audio.say(narration);
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
                if(audio!=null){
                    audio.play(AccessibleAudio.Sound.SELECT);
                    audio.say(ChessNarration.squareContents(p,sq)+", sélectionné.");
                }
            }else if(audio!=null)audio.say(ChessNarration.squareContents(p,sq));
        }else if(selected==sq){
            selected=-1;
            if(audio!=null)audio.say("Sélection annulée.");
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
            if(audio!=null){
                audio.play(selected<0?AccessibleAudio.Sound.INVALID:AccessibleAudio.Sound.SELECT);
                audio.say(selected<0?"Destination illégale. "+ChessNarration.squareContents(p,sq):
                    ChessNarration.squareContents(p,sq)+", sélectionné.");
            }
        }
        chessView.invalidate();
        if(fullscreenChessView!=null)fullscreenChessView.invalidate();
    }
    private void scheduleBot(){
        handler.removeCallbacks(botRunnable);
        if(!trialMode&&!review.reviewing()&&botEnabled&&revision!=failedBotRevision&&!current().white&&!finished())
            handler.postDelayed(botRunnable,300);
    }
    private void botMove(){
        if(botRunning||trialMode||review.reviewing()||!botEnabled||current().white||finished())return;
        final ChessGame.Position initial=current();
        final long expectedRevision=revision;
        final int ply=moves.size();
        final boolean online=onlineSwitch!=null&&onlineSwitch.isChecked();
        final HumanPolicy policy=humanPolicy;
        final int engineMode=opponentMode;
        final int target=precisionTarget,spread=precisionSigma;
        final boolean useDeep=deepMode;
        final int useDepth=deepDepth;
        engineLog.record("TURN","START ply="+ply+" rev="+expectedRevision+
            " mode="+engineMode+" target="+target+" sigma="+spread+
            " deep="+useDeep+" depth="+useDepth+" fen="+initial.fen());
        botRunning=true;
        botInfo.setText(engineMode==1?(useDeep?"Stockfish MultiPV approfondi…":"Stockfish : toutes les positions à profondeur 1…"):
            "Profil Lichess expérimental : sélection d'un coup…");
        if(engineMode==1&&stockfishBridge!=null)stockfishBridge.stop();
        botWorker.execute(()->{
            String chosenUci=null,detail=null,error=null;
            try{
                if(engineMode==1){
                    StockfishOpponent.Result result=stockfishOpponent.select(initial,target,spread,useDeep,useDepth);
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
            handler.post(()->{
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
            });
        });
    }
    private void reset(){
        engineLog.record("NEW_GAME","New game; kept settings opponent="+opponentMode+
            " target="+precisionTarget+" sigma="+precisionSigma+
            " deep="+deepMode+" depth="+deepDepth);
        updateEngineDebugStatus("Nouvelle partie — réglages conservés");
        if(trialMode)exitTrial();
        annotations.clear();
        leaveReview();
        revision++;
        handler.removeCallbacks(botRunnable);
        prefs.edit().remove("baseFen").apply();
        moves.clear();positions.clear();positions.add(ChessGame.fromFen(ChessGame.START));
        review.setHistorySize(moves.size());
        selected=-1;persist();refresh();analyzeDisplayed();
        if(audio!=null)audio.say("Nouvelle partie. Les blancs commencent.");
        scheduleBot();
    }
    private void undo(){
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
    @Override protected void onPause(){
        stopReplay();
        if(audio!=null)audio.stopSpeech();
        if(stockfishBridge!=null)stockfishBridge.stop();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(stockfishBridge!=null)analyzeDisplayed();
    }
    @Override protected void onDestroy(){
        handler.removeCallbacks(botRunnable);
        stopReplay();
        revision++;
        botWorker.shutdownNow();
        if(stockfishOpponent!=null)stockfishOpponent.close();
        if(audio!=null)audio.close();
        if(stockfishBridge!=null)stockfishBridge.close();
        super.onDestroy();
    }
    private final class ChessView extends View {
        private final Paint paint=new Paint(3);
        private final String symbols="♔♕♖♗♘♙♚♛♜♝♞♟";
        private int arrowStart=-1,arrowEnd=-1;
        private Integer whiteCp=null,whiteMate=null;
        private double whiteFraction=0.5;
        private final int gaugeWidth=dp(44);
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
            // Clear split marker makes small shifts perceptible.
            float cut=(float)(h*(1.0-whiteFraction));
            paint.setColor(0xffd0a332);
            canvas.drawRect(1,Math.max(0,cut-dp(1)),bar,Math.min(h,cut+dp(1)),paint);
        }
        @Override protected void onDraw(Canvas canvas){
            int side=getWidth()-gaugeWidth;
            if(side<=0)return;
            drawGauge(canvas,side);
            canvas.save();
            canvas.translate(gaugeWidth,0);
            float cell=side/8f;char[] b=displayed().board;
            for(int i=0;i<64;i++){
                int x=i%8,y=i/8;float left=x*cell,top=y*cell;
                boolean light=(x+y)%2==0;
                paint.setStyle(Paint.Style.FILL);
                if(pieceTheme==2)paint.setColor(light?0xfff5f6ee:0xff586b75);
                else if(pieceTheme==1)paint.setColor(light?0xffeae5d9:0xff789087);
                else if(pieceTheme>=3)paint.setColor(light?0xffe3e6d2:0xff78927b);
                else paint.setColor(light?0xffdde4cf:0xff7d9582);
                canvas.drawRect(left,top,left+cell,top+cell,paint);
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
                drawVector(canvas,cell,arrow.from,arrow.to);
            // Show a live translucent preview as the finger moves.
            if(arrowMode&&arrowStart>=0&&arrowEnd>=0&&arrowEnd!=arrowStart)
                drawVector(canvas,cell,arrowStart,arrowEnd);
        }
        private void drawVector(Canvas canvas,float cell,int from,int to){
            float ax=(from%8+.5f)*cell,ay=(from/8+.5f)*cell;
            float bx=(to%8+.5f)*cell,by=(to/8+.5f)*cell;
            float dx=bx-ax,dy=by-ay,d=(float)Math.hypot(dx,dy);
            if(d<cell*.5f)return;
            float ux=dx/d,uy=dy/d;
            float tipX=bx-ux*cell*.18f,tipY=by-uy*cell*.18f;
            paint.setColor(0x88ffae23);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(dp(3),cell*.12f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(ax,ay,tipX-ux*cell*.22f,tipY-uy*cell*.22f,paint);
            Path head=new Path();
            head.moveTo(tipX,tipY);
            head.lineTo(tipX-ux*cell*.41f-uy*cell*.24f,
                tipY-uy*cell*.41f+ux*cell*.24f);
            head.lineTo(tipX-ux*cell*.41f+uy*cell*.24f,
                tipY-uy*cell*.41f-ux*cell*.24f);
            head.close();
            paint.setStyle(Paint.Style.FILL);
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
            if(arrowMode){
                if(action==MotionEvent.ACTION_DOWN){
                    getParent().requestDisallowInterceptTouchEvent(true);
                    arrowStart=squareAt(e.getX(),e.getY());
                    arrowEnd=-1;
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
                        annotations.toggle(arrowStart,target);
                        if(audio!=null)audio.say("Flèche "+ChessNarration.square(arrowStart)+
                            " vers "+ChessNarration.square(target));
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
