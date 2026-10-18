package fr.gambitdrame;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
public class MainActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);ScrollView s=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(12,12,12,12);s.addView(root);
 label(root,"GambitDrame ♟",26);label(root,"Adversaires humains : prototypes à calibrer",15);
 Spinner levels=new Spinner(this);levels.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"400 Elo","500 Elo","600 Elo","700 Elo","800 Elo","900 Elo"}));root.addView(levels);
 String[] board={"♜♞♝♛♚♝♞♜","♟♟♟♟♟♟♟♟","        ","        ","        ","        ","♙♙♙♙♙♙♙♙","♖♘♗♕♔♗♘♖"};
 for(int y=0;y<8;y++){LinearLayout rank=new LinearLayout(this);root.addView(rank);for(int x=0;x<8;x++){TextView t=new TextView(this);t.setText(""+board[y].charAt(x));t.setTextSize(26);t.setGravity(Gravity.CENTER);t.setBackgroundColor((x+y)%2==0?0xfff0d9b5:0xffb58863);rank.addView(t,new LinearLayout.LayoutParams(0,52,1));}}
 label(root,"Stockfish : jauge, WDL et MultiPV à intégrer",16);label(root,"Lichess : aucune donnée téléchargée pour le moment",14);label(root,"Entraînement → Ouvertures → Gambit de dame (à développer)",15);setContentView(s);
 }
 void label(LinearLayout p,String v,int size){TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(Color.BLACK);t.setPadding(8,12,8,12);p.addView(t);}
}
