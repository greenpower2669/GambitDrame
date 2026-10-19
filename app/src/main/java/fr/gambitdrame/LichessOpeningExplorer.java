package fr.gambitdrame;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Optional opening-explorer sample, not a dataset of verified 1000-game players. */
public final class LichessOpeningExplorer {
    private final SharedPreferences prefs;
    public LichessOpeningExplorer(Context context){
        prefs=context.getSharedPreferences("gd-lichess-cache-1",Context.MODE_PRIVATE);
    }
    public Map<String,Integer> moves(ChessGame.Position p,int ply,boolean online){
        // The explorer indexes mainly early games. Do not block for obscure middlegames.
        if(ply>=24)return Collections.emptyMap();
        String fen=p.fen();
        String cacheKey="fen-"+Integer.toHexString(fen.substring(0,fen.lastIndexOf(' ')).hashCode());
        String raw=prefs.getString(cacheKey,null);
        if(raw!=null)return parse(raw);
        if(!online)return Collections.emptyMap();
        HttpURLConnection conn=null;
        try{
            String url="https://explorer.lichess.ovh/lichess?variant=standard"
                +"&speeds=blitz,rapid&ratings=0&moves=20&topGames=0&recentGames=0"
                +"&fen="+URLEncoder.encode(fen,"UTF-8");
            conn=(HttpURLConnection)new URL(url).openConnection();
            conn.setConnectTimeout(1800);conn.setReadTimeout(1800);
            conn.setRequestProperty("Accept","application/json");
            conn.setRequestProperty("User-Agent","GambitDrame/0.3.0");
            if(conn.getResponseCode()!=200)return Collections.emptyMap();
            try(BufferedInputStream input=new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream output=new ByteArrayOutputStream()){
                byte[] block=new byte[2048];int read;
                while((read=input.read(block))!=-1){
                    output.write(block,0,read);
                    if(output.size()>80000)return Collections.emptyMap();
                }
                raw=output.toString("UTF-8");
            }
            Map<String,Integer> counts=parse(raw);
            if(!counts.isEmpty())prefs.edit().putString(cacheKey,raw).apply();
            return counts;
        }catch(Exception ex){return Collections.emptyMap();}
        finally{if(conn!=null)conn.disconnect();}
    }
    private Map<String,Integer> parse(String raw){
        try{
            JSONObject json=new JSONObject(raw);
            JSONArray ms=json.optJSONArray("moves");
            if(ms==null)return Collections.emptyMap();
            Map<String,Integer> result=new HashMap<>();
            for(int i=0;i<ms.length();i++){
                JSONObject m=ms.getJSONObject(i);
                int n=m.optInt("white",0)+m.optInt("draws",0)+m.optInt("black",0);
                String u=m.optString("uci","");
                if(n>0&&u.matches("[a-h][1-8][a-h][1-8][qrbn]?"))result.put(u,n);
            }
            return result;
        }catch(Exception ex){return Collections.emptyMap();}
    }
}
