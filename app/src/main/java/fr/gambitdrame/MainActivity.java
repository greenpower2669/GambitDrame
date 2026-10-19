package fr.gambitdrame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
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
    private LichessOpeningExplorer explorer;
    private StockfishBridge stockfishBridge;
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
    private TextView status,history;
    private EditText moveInput;
    private Switch botSwitch;
    private int selected=-1;
    private boolean botEnabled=true;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("gambitdrame-v1",MODE_PRIVATE);
        botEnabled=prefs.getBoolean("bot",true);
        targetRating=prefs.getInt("targetRating",700);
        aggression=prefs.getFloat("aggression",0.4f);
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
        label(root,"Version 0.3.1 — jauge Stockfish et navigation de partie",16);
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
        button(root,"Reprendre la partie (sortir de la relecture)",this::resumeGame);
        if(!wide)root.addView(analysisPanel,new LinearLayout.LayoutParams(-1,-2));
        status=label(root,"",18);
        status.setTypeface(null,Typeface.BOLD);
        botSwitch=new Switch(this);
        botSwitch.setText("Jouer contre le profil humain expérimental");
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
            .setTitle("Stockfish 18 — analyse seulement")
            .setMessage("Moteur libre GPLv3. Source exacte : github.com/official-stockfish/Stockfish/tree/sf_18. Le binaire est compilé sans modification depuis ce tag par GitHub Actions. Licence incluse dans les ressources APK. L'adversaire humain n'utilise jamais ce moteur pour choisir ses coups.")
            .setPositiveButton("Code source",(d,w)->{
                android.content.Intent intent=new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/official-stockfish/Stockfish/tree/sf_18"));
                startActivity(intent);
            }).setNegativeButton("Fermer",null).show());
        setContentView(scroller);
        stockfishBridge=new StockfishBridge(this,(fen,message,whiteCp,whiteMate)->{
            if(fen!=null&&!fen.equals(displayed().fen()))return;
            analysisPanel.setText(message);
            if(whiteCp!=null||whiteMate!=null)
                chessView.setEvaluation(whiteCp,whiteMate);
        });
        stockfishBridge.start();
        refresh();
        analyzeDisplayed();
        scheduleBot();
    }
    private ChessGame.Position current(){return positions.get(positions.size()-1);}
    private ChessGame.Position displayed(){
        return positions.get(review.ply());
    }
    private void analyzeDisplayed(){
        if(chessView!=null)chessView.setEvaluation(null,null);
        if(stockfishBridge!=null)stockfishBridge.analyze(displayed().fen());
    }
    private int dp(float n){return Math.round(getResources().getDisplayMetrics().density*n);}
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
            replayPosition.setText(review.reviewing()?
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
    }
    private void toggleReplay(){
        if(replayPlaying){stopReplay();return;}
        if(moves.isEmpty()){
            Toast.makeText(this,"Aucun coup à rejouer",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!review.reviewing()||review.ply()>=moves.size())seekReview(0);
        replayPlaying=true;
        updatePlaybackUi();
        handler.postDelayed(replayTick,900);
    }
    private void advanceReplay(){
        if(!replayPlaying)return;
        if(review.ply()<moves.size())seekReview(review.ply()+1);
        if(review.ply()<moves.size())handler.postDelayed(replayTick,900);
        else stopReplay();
    }
    private void leaveReview(){
        stopReplay();
        review.live();
    }
    private void resumeGame(){
        leaveReview();
        revision++;
        selected=-1;
        refresh();
        analyzeDisplayed();
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
        String outcome=review.reviewing()?"":ChessGame.outcome(p,repetitionCount());
        String turn=p.white?"Blancs":"Noirs";
        status.setText(review.reviewing()?
            "Relecture en pause · trait : "+turn:
            (!outcome.isEmpty()?outcome:"Au trait : "+turn+(botEnabled&&!p.white?" (profil humain)":"")));
        StringBuilder sb=new StringBuilder("Historique (coordonnées) : ");
        for(int i=0;i<moves.size();i++){
            if(i%2==0)sb.append("  ").append(i/2+1).append(". ");
            sb.append(moves.get(i)).append(" ");
        }
        history.setText(sb.toString());
        updatePlaybackUi();
        chessView.invalidate();
    }
    private void reconfigurePolicy(){
        // Keep the random seed stable throughout one game unless Fab explicitly restarts.
        humanPolicy=new HumanPolicy(targetRating,prefs.getLong("humanSeed",42L),aggression);
        if(botInfo!=null)botInfo.setText("Profil visé "+targetRating+" (non calibré) · style "+(int)(aggression*100)+"% agressif");
    }
    private void playMove(ChessGame.Move m){
        if(review.reviewing())return;
        positions.add(ChessGame.apply(current(),m));
        moves.add(m.uci());
        review.setHistorySize(moves.size());
        selected=-1;revision++;persist();refresh();
        analyzeDisplayed();
        scheduleBot();
    }
    private void tryMove(String uci){
        if(review.reviewing()){
            Toast.makeText(this,"Quitter la relecture pour jouer",Toast.LENGTH_SHORT).show();
            return;
        }
        if(finished()){Toast.makeText(this,"Partie terminée",Toast.LENGTH_SHORT).show();return;}
        if(botEnabled&&!current().white){Toast.makeText(this,"Tour du bot",Toast.LENGTH_SHORT).show();return;}
        for(ChessGame.Move m:ChessGame.legal(current()))if(m.uci().equals(uci)){playMove(m);return;}
        Toast.makeText(this,"Coup illégal : "+uci,Toast.LENGTH_SHORT).show();
    }
    private void choose(int sq){
        if(review.reviewing()){
            Toast.makeText(this,"Appuyer sur Reprendre la partie pour jouer",Toast.LENGTH_SHORT).show();
            return;
        }
        if(finished()||(botEnabled&&!current().white))return;
        ChessGame.Position p=current();
        char c=p.board[sq];
        if(selected<0){
            if(c!='.'&&Character.isUpperCase(c)==p.white)selected=sq;
        }else if(selected==sq) selected=-1;
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
        }
        chessView.invalidate();
    }
    private void scheduleBot(){
        handler.removeCallbacks(botRunnable);
        if(!review.reviewing()&&botEnabled&&!current().white&&!finished())
            handler.postDelayed(botRunnable,300);
    }
    private void botMove(){
        if(botRunning||review.reviewing()||!botEnabled||current().white||finished())return;
        final ChessGame.Position initial=current();
        final long expectedRevision=revision;
        final int ply=moves.size();
        final boolean online=onlineSwitch!=null&&onlineSwitch.isChecked();
        final HumanPolicy policy=humanPolicy;
        botRunning=true;
        botInfo.setText("Profil "+targetRating+" : recherche d'un coup humain…");
        botWorker.execute(()->{
            HumanPolicy.Choice candidate=null;
            try{
                Map<String,Integer> sampled=explorer.moves(initial,ply,online);
                candidate=policy.choose(initial,sampled,ply);
            }catch(RuntimeException ex){
                android.util.Log.w("GambitHuman","Échec du choix, code="+ex.getClass().getSimpleName());
            }
            final HumanPolicy.Choice choice=candidate;
            handler.post(()->{
                botRunning=false;
                if(choice==null||review.reviewing()||revision!=expectedRevision||!botEnabled||
                   current().white||!current().fen().equals(initial.fen())){
                    scheduleBot();
                    return;
                }
                botInfo.setText("Coup "+choice.move.uci()+" · "+choice.source);
                playMove(choice.move);
            });
        });
    }
    private void reset(){
        leaveReview();
        revision++;
        handler.removeCallbacks(botRunnable);
        prefs.edit().remove("baseFen").apply();
        moves.clear();positions.clear();positions.add(ChessGame.fromFen(ChessGame.START));
        review.setHistorySize(moves.size());
        selected=-1;persist();refresh();analyzeDisplayed();scheduleBot();
    }
    private void undo(){
        leaveReview();
        revision++;
        handler.removeCallbacks(botRunnable);
        int count=botEnabled&&current().white?2:1;
        while(count-->0&&!moves.isEmpty()){
            moves.remove(moves.size()-1);positions.remove(positions.size()-1);
        }
        review.setHistorySize(moves.size());
        selected=-1;persist();refresh();analyzeDisplayed();scheduleBot();
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
                    leaveReview();
                    revision++;
                    handler.removeCallbacks(botRunnable);
                    positions.clear();positions.add(p);moves.clear();selected=-1;
                    review.setHistorySize(0);
                    prefs.edit().putString("baseFen",p.fen()).remove("history").apply();
                    refresh();analyzeDisplayed();scheduleBot();
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
        persist();refresh();analyzeDisplayed();scheduleBot();
    }
    @Override protected void onPause(){
        stopReplay();
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
        if(stockfishBridge!=null)stockfishBridge.close();
        super.onDestroy();
    }
    private final class ChessView extends View {
        private final Paint paint=new Paint(3);
        private final String symbols="♔♕♖♗♘♙♚♛♜♝♞♟";
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
                paint.setColor(light?0xffe4e8d0:0xff678574);
                canvas.drawRect(left,top,left+cell,top+cell,paint);
                if(selected==i){
                    paint.setColor(0xfff3c74f);
                    canvas.drawRect(left+2,top+2,left+cell-2,top+cell-2,paint);
                }
                char piece=b[i];
                if(piece!='.'){
                    String glyph=glyph(piece);
                    paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
                    paint.setTextSize(cell*.76f);paint.setTextAlign(Paint.Align.CENTER);
                    float base=top+cell*.77f;
                    if(Character.isUpperCase(piece)){
                        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1.8f,cell*.055f));
                        paint.setColor(Color.BLACK);canvas.drawText(glyph,left+cell/2,base,paint);
                        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
                    }else{paint.setStyle(Paint.Style.FILL);paint.setColor(0xff101c19);}
                    canvas.drawText(glyph,left+cell/2,base,paint);
                }
            }
            if(selected>=0){
                paint.setStyle(Paint.Style.FILL);paint.setColor(0x9996d644);
                for(ChessGame.Move m:ChessGame.legal(current()))if(m.from==selected){
                    int x=m.to%8,y=m.to/8;
                    canvas.drawCircle((x+.5f)*cell,(y+.5f)*cell,cell*.13f,paint);
                }
            }
            canvas.restore();
        }
        private String glyph(char p){
            String all="KQRBNPkqrbnp";
            int i=all.indexOf(p);
            return i<0?"":symbols.substring(i,i+1);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            int side=getWidth()-gaugeWidth;
            if(side<=0||e.getX()<gaugeWidth){
                performClick();
                return true;
            }
            int x=(int)((e.getX()-gaugeWidth)*8/side);
            int y=(int)(e.getY()*8/side);
            if(x>=0&&x<8&&y>=0&&y<8)choose(y*8+x);
            performClick();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
