package fr.gambitdrame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;

/** Accessible native chart of saved study accuracy, never synthetic data. */
public final class StudyTrendView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private JSONArray records=new JSONArray();
    public StudyTrendView(Context context){super(context);setContentDescription("Aucune partie analysée");}
    public void setRecords(JSONArray value){
        records=value==null?new JSONArray():value;
        int n=records.length();
        if(n>0){
            JSONObject last=records.optJSONObject(n-1);
            setContentDescription("Graphique de "+n+" parties. Dernière : Blancs "+
                last.optDouble("whiteAccuracy",0)+" %, Noirs "+
                last.optDouble("blackAccuracy",0)+" % de précision indicative.");
        }else setContentDescription("Aucune partie analysée pour le graphique");
        invalidate();
    }
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){
        p.setColor(color);p.setStrokeWidth(dp(width));p.setStyle(Paint.Style.STROKE);
        c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        if(w<dp(180)||h<dp(100))return;
        float left=dp(45),right=w-dp(12),top=dp(28),bottom=h-dp(30);
        p.setColor(Color.rgb(25,41,40));p.setTextSize(dp(13));p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("PRÉCISION INDICATIVE PAR PARTIE",left,dp(17),p);
        if(records.length()==0){
            p.setTypeface(android.graphics.Typeface.DEFAULT);
            c.drawText("Analyse une partie pour afficher sa progression.",left,top+dp(30),p);
            return;
        }
        for(int tick=0;tick<=100;tick+=25){
            float y=bottom-(bottom-top)*tick/100f;
            line(c,left,y,right,y,0xffccd4cf,0.8f);
            p.setColor(0xff334444);p.setTextSize(dp(11));p.setTypeface(android.graphics.Typeface.DEFAULT);
            c.drawText(tick+"",dp(8),y+dp(4),p);
        }
        int n=Math.min(24,records.length()),start=records.length()-n;
        int[] colors={0xff116c73,0xffc16a1c};
        String[] keys={"whiteAccuracy","blackAccuracy"};
        for(int group=0;group<2;group++){
            Path path=new Path();
            for(int i=0;i<n;i++){
                JSONObject r=records.optJSONObject(start+i);
                float x=n==1?(left+right)/2:left+(right-left)*i/(n-1);
                float y=bottom-(bottom-top)*(float)Math.max(0,Math.min(100,r.optDouble(keys[group],0)))/100;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
                p.setColor(colors[group]);p.setStyle(Paint.Style.FILL);
                c.drawCircle(x,y,dp(3.8f),p);
            }
            p.setColor(colors[group]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.2f));
            c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
        p.setTextSize(dp(11));p.setColor(0xff334444);
        c.drawText("Partie "+(start+1),left,bottom+dp(18),p);
        String last=""+records.length();
        c.drawText("Partie "+last,right-dp(62),bottom+dp(18),p);
        p.setTextSize(dp(12));
        p.setColor(colors[0]);c.drawText("● Blancs",left+dp(5),top+dp(14),p);
        p.setColor(colors[1]);c.drawText("● Noirs",left+dp(82),top+dp(14),p);
    }
}
