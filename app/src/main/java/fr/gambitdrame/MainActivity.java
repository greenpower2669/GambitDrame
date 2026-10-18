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

/** Accessible local chess prototype. No Stockfish/Lichess data is bundled. */
public final class MainActivity extends Activity {
    private final List<ChessGame.Position> positions=new ArrayList<>();
    private final List<String> moves=new ArrayList<>();
    private final Random random=new Random();
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
        restore();
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
        label(root,"Version 0.2 — échecs jouables hors ligne",16);
        label(root,"Le bot est un adversaire de démonstration NON calibré sur Lichess.",14);
        chessView=new ChessView();
        root.addView(chessView,new LinearLayout.LayoutParams(-1,-2));
        status=label(root,"",18);
        status.setTypeface(null,Typeface.BOLD);
        botSwitch=new Switch(this);
        botSwitch.setText("Jouer contre le bot de démonstration");
        botSwitch.setTextColor(Color.BLACK);
        botSwitch.setTextSize(16);
        botSwitch.setChecked(botEnabled);
        botSwitch.setOnCheckedChangeListener((button,checked)->{
            handler.removeCallbacks(botRunnable);
            botEnabled=checked;
            prefs.edit().putBoolean("bot",checked).apply();
            selected=-1;refresh();scheduleBot();
        });
        root.addView(botSwitch);
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
            cm.setPrimaryClip(ClipData.newPlainText("FEN",current().fen()));
            Toast.makeText(this,"Position FEN copiée",Toast.LENGTH_SHORT).show();
        });
        heading(root,"Entraînement → Ouvertures",18);
        button(root,"Gambit de dame",()->new AlertDialog.Builder(this)
            .setTitle("Gambit de dame")
            .setMessage("Position d'entraînement : 1.d4 d5 2.c4. Charger la position comme une nouvelle partie ?")
            .setPositiveButton("Charger",(d,w)->loadOpening())
            .setNegativeButton("Annuler",null).show());
        heading(root,"Profils humains Lichess — pas encore disponibles",17);
        Spinner levels=new Spinner(this);
        levels.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"400 Lichess","500 Lichess","600 Lichess","700 Lichess","800 Lichess","900 Lichess"}));
        levels.setEnabled(false);
        levels.setContentDescription("Niveaux Lichess non disponibles : collecte et calibration non réalisées");
        root.addView(levels);
        label(root,"Stockfish : analyse, jauge, WDL et MultiPV non intégrés. Aucun classement simulé.",14);
        setContentView(scroller);
        refresh();
        scheduleBot();
    }
    private ChessGame.Position current(){return positions.get(positions.size()-1);}
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
        ChessGame.Position p=current();
        String outcome=ChessGame.outcome(p,repetitionCount());
        String turn=p.white?"Blancs":"Noirs";
        status.setText(!outcome.isEmpty()?outcome:"Au trait : "+turn+(botEnabled&&!p.white?" (bot)":""));
        StringBuilder sb=new StringBuilder("Historique (coordonnées) : ");
        for(int i=0;i<moves.size();i++){
            if(i%2==0)sb.append("  ").append(i/2+1).append(". ");
            sb.append(moves.get(i)).append(" ");
        }
        history.setText(sb.toString());
        chessView.invalidate();
    }
    private void playMove(ChessGame.Move m){
        positions.add(ChessGame.apply(current(),m));moves.add(m.uci());
        selected=-1;persist();refresh();scheduleBot();
    }
    private void tryMove(String uci){
        if(finished()){Toast.makeText(this,"Partie terminée",Toast.LENGTH_SHORT).show();return;}
        if(botEnabled&&!current().white){Toast.makeText(this,"Tour du bot",Toast.LENGTH_SHORT).show();return;}
        for(ChessGame.Move m:ChessGame.legal(current()))if(m.uci().equals(uci)){playMove(m);return;}
        Toast.makeText(this,"Coup illégal : "+uci,Toast.LENGTH_SHORT).show();
    }
    private void choose(int sq){
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
        if(botEnabled&&!current().white&&!finished())handler.postDelayed(botRunnable,300);
    }
    private void botMove(){
        if(!botEnabled||current().white||finished())return;
        List<ChessGame.Move> legal=ChessGame.legal(current());
        if(legal.isEmpty())return;
        // Deliberately labelled as uncalibrated, not a reconstructed human Elo profile.
        ChessGame.Move chosen=legal.get(random.nextInt(legal.size()));
        playMove(chosen);
    }
    private void reset(){
        handler.removeCallbacks(botRunnable);
        prefs.edit().remove("baseFen").apply();
        moves.clear();positions.clear();positions.add(ChessGame.fromFen(ChessGame.START));
        selected=-1;persist();refresh();scheduleBot();
    }
    private void undo(){
        handler.removeCallbacks(botRunnable);
        int count=botEnabled&&current().white?2:1;
        while(count-->0&&!moves.isEmpty()){
            moves.remove(moves.size()-1);positions.remove(positions.size()-1);
        }
        selected=-1;persist();refresh();scheduleBot();
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
                    handler.removeCallbacks(botRunnable);
                    positions.clear();positions.add(p);moves.clear();selected=-1;
                    prefs.edit().putString("baseFen",p.fen()).remove("history").apply();
                    refresh();scheduleBot();
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
        persist();refresh();scheduleBot();
    }
    @Override protected void onDestroy(){handler.removeCallbacks(botRunnable);super.onDestroy();}
    private final class ChessView extends View {
        private final Paint paint=new Paint(3);
        private final String symbols="♔♕♖♗♘♙♚♛♜♝♞♟";
        ChessView(){super(MainActivity.this);setContentDescription("Échiquier tactile. Alternative accessible : saisir e2e4 dans le champ Coup.");}
        @Override protected void onMeasure(int w,int h){int size=MeasureSpec.getSize(w);setMeasuredDimension(size,size);}
        @Override protected void onDraw(Canvas canvas){
            int side=getWidth();float cell=side/8f;char[] b=current().board;
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
        }
        private String glyph(char p){
            String all="KQRBNPkqrbnp";
            int i=all.indexOf(p);
            return i<0?"":symbols.substring(i,i+1);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            int x=(int)(e.getX()*8/getWidth()),y=(int)(e.getY()*8/getHeight());
            if(x>=0&&x<8&&y>=0&&y<8)choose(y*8+x);
            performClick();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
