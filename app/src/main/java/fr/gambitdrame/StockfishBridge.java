package fr.gambitdrame;

import android.app.Activity;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Independent Stockfish 18 UCI analytical engine.
 * Scores and text are tagged with the analysed FEN so the UI drops stale replies.
 * HumanPolicy never reads Stockfish scores or PVs.
 */
public final class StockfishBridge {
    public interface Listener {
        void onAnalysis(String fen,String text,Integer whiteCp,Integer whiteMate);
    }
    private final Activity activity;
    private final Listener listener;
    private final Object lock=new Object();
    private Process process;
    private BufferedWriter writer;
    private volatile boolean ready=false;
    private volatile boolean closed=false;
    private volatile boolean paused=false;
    private volatile String desiredFen;
    private volatile String activeFen;
    private volatile boolean awaitingReady=false;
    private final Map<Integer,String> variants=new HashMap<>();
    private String currentWdl="WDL —";

    public StockfishBridge(Activity activity,Listener listener){
        this.activity=activity;
        this.listener=listener;
    }
    private void dispatch(String fen,String text,Integer cp,Integer mate){
        if(closed)return;
        activity.runOnUiThread(()->{
            if(!closed)listener.onAnalysis(fen,text,cp,mate);
        });
    }
    public void start(){
        boolean arm64=false;
        for(String abi:android.os.Build.SUPPORTED_ABIS)if("arm64-v8a".equals(abi))arm64=true;
        if(!arm64){dispatch(null,"Stockfish : cette APK requiert ARM64.",null,null);return;}
        File binary=new File(activity.getApplicationInfo().nativeLibraryDir,"libstockfish.so");
        if(!binary.isFile()){
            dispatch(null,"Stockfish : exécutable ARM64 absent de l'installation.",null,null);
            return;
        }
        try{
            process=new ProcessBuilder(binary.getAbsolutePath()).redirectErrorStream(true).start();
            writer=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),"UTF-8"));
            Thread reader=new Thread(this::readLoop,"Stockfish-UCI-reader");
            reader.setDaemon(true);reader.start();
            send("uci");
        }catch(Exception ex){
            dispatch(null,"Stockfish : démarrage impossible ("+ex.getClass().getSimpleName()+").",null,null);
        }
    }
    private void send(String message){
        synchronized(lock){
            if(closed||writer==null)return;
            try{writer.write(message);writer.newLine();writer.flush();}
            catch(Exception ex){
                dispatch(null,"Stockfish : erreur de communication.",null,null);
            }
        }
    }
    public void analyze(String fen){
        desiredFen=fen;
        paused=false;
        dispatch(fen,"Stockfish : analyse de cette position…",null,null);
        if(ready)requestLatest();
    }
    /**
     * Interrupt old search. The UCI isready barrier makes old info lines
     * distinguishable before starting a new search.
     */
    private void requestLatest(){
        synchronized(lock){
            if(closed||!ready||paused||awaitingReady)return;
            activeFen=null;
            awaitingReady=true;
            send("stop");
            send("isready");
        }
    }
    private void onReady(){
        String fen;
        synchronized(lock){
            ready=true;
            awaitingReady=false;
            if(paused||desiredFen==null)return;
            fen=desiredFen;
            activeFen=fen;
            variants.clear();
            currentWdl="WDL —";
            send("position fen "+fen);
            send("go depth 11");
        }
        dispatch(fen,"Stockfish 18 · analyse MultiPV 3…",null,null);
    }
    private void readLoop(){
        try(BufferedReader input=new BufferedReader(
                new InputStreamReader(process.getInputStream(),"UTF-8"))){
            String line;
            while(!closed&&(line=input.readLine())!=null){
                if(line.equals("uciok")){
                    send("setoption name Threads value 1");
                    send("setoption name Hash value 32");
                    send("setoption name MultiPV value 3");
                    send("setoption name UCI_ShowWDL value true");
                    awaitingReady=true;
                    send("isready");
                }else if(line.equals("readyok")){
                    onReady();
                    // If a newer FEN arrived during the barrier, onReady uses it.
                }else if(line.startsWith("info ")&&line.contains(" score ")&&line.contains(" pv ")){
                    if(!paused&&!awaitingReady&&activeFen!=null)onInfo(line);
                }
            }
            if(!closed)dispatch(null,"Stockfish : processus terminé.",null,null);
        }catch(Exception ex){
            if(!closed)dispatch(null,"Stockfish : flux d'analyse interrompu.",null,null);
        }
    }
    private static String tokenAfter(String s,String name){
        String key=" "+name+" ";
        int at=s.indexOf(key);
        if(at<0)return null;
        int start=at+key.length(),end=s.indexOf(' ',start);
        return end<0?s.substring(start):s.substring(start,end);
    }
    private void onInfo(String line){
        try{
            int rank=Integer.parseInt(tokenAfter(line,"multipv"));
            if(rank<1||rank>3)return;
            String fen=activeFen;
            if(fen==null)return;
            String pv=line.substring(line.indexOf(" pv ")+4);
            String[] pvMoves=pv.split(" ");
            StringBuilder pvShort=new StringBuilder();
            for(int i=0;i<Math.min(6,pvMoves.length);i++)
                pvShort.append(i==0?"":" ").append(pvMoves[i]);
            Integer whiteCp=null,whiteMate=null;
            String rawCp=tokenAfter(line,"cp");
            String rawMate=tokenAfter(line,"mate");
            int perspective=fen.contains(" w ")?1:-1;
            if(rawCp!=null)whiteCp=Integer.parseInt(rawCp)*perspective;
            if(rawMate!=null)whiteMate=Integer.parseInt(rawMate)*perspective;
            String score=EvaluationScale.label(whiteCp,whiteMate);
            String output;
            synchronized(lock){
                if(!fen.equals(activeFen)||awaitingReady||paused)return;
                if(rank==1){
                    int start=line.indexOf(" wdl ");
                    if(start>=0){
                        String[] p=line.substring(start+5).split(" ");
                        if(p.length>=3){
                            int wins=Integer.parseInt(p[0]),draws=Integer.parseInt(p[1]),losses=Integer.parseInt(p[2]);
                            if(perspective<0){int temp=wins;wins=losses;losses=temp;}
                            currentWdl=String.format(java.util.Locale.FRANCE,
                                "WDL : B %.1f%% · nulle %.1f%% · N %.1f%%",
                                wins/10.0,draws/10.0,losses/10.0);
                        }
                    }
                }
                variants.put(rank,rank+". "+pvShort+" ("+score+")");
                StringBuilder s=new StringBuilder("Stockfish 18 · profondeur "+
                    tokenAfter(line,"depth")+"\n"+currentWdl+"\n");
                for(int i=1;i<=3;i++)
                    if(variants.containsKey(i))s.append(variants.get(i)).append("\n");
                output=s.toString();
            }
            // Only the best PV updates the bar; subsequent PVs only update text.
            dispatch(fen,output,rank==1?whiteCp:null,rank==1?whiteMate:null);
        }catch(RuntimeException ignored){
            // Some UCI 'info' lines are incomplete or nonnumeric during startup.
        }
    }
    public void stop(){
        paused=true;
        activeFen=null;
        send("stop");
    }
    public void close(){
        closed=true;
        ready=false;
        synchronized(lock){
            if(writer!=null)
                try{writer.write("quit\n");writer.flush();}catch(Exception ignored){}
        }
        if(process!=null)process.destroy();
    }
}
