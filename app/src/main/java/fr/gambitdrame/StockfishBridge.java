package fr.gambitdrame;

import android.app.Activity;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * One native Stockfish UCI subprocess, strictly for ANALYSIS.
 * No position or bestmove is ever passed to HumanPolicy.
 */
public final class StockfishBridge {
    public interface Listener { void onUpdate(String value); }
    private final Activity activity;
    private final Listener listener;
    private final Object lock=new Object();
    private Process process;
    private BufferedWriter writer;
    private volatile boolean ready=false;
    private volatile boolean closed=false;
    private volatile String desiredFen=null;
    private volatile boolean whiteToMove=true;
    private final Map<Integer,String> variants=new HashMap<>();
    private String status="Stockfish : initialisation…";

    public StockfishBridge(Activity a,Listener l){activity=a;listener=l;}
    private void display(String text){
        if(!closed)activity.runOnUiThread(()->{if(!closed)listener.onUpdate(text);});
    }
    public void start(){
        if(!"arm64-v8a".equals(android.os.Build.SUPPORTED_ABIS[0])){
            display("Stockfish : cette préversion ne contient que le moteur ARM64.");
            return;
        }
        String absolute=activity.getApplicationInfo().nativeLibraryDir+"/libstockfish.so";
        File engine=new File(absolute);
        if(!engine.isFile()){
            display("Stockfish : exécutable natif absent. Vérifier l'APK avec moteur ARM64.");
            return;
        }
        try{
            process=new ProcessBuilder(absolute).redirectErrorStream(true).start();
            writer=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),"UTF-8"));
            Thread output=new Thread(this::readLoop,"Stockfish-UCI-reader");
            output.setDaemon(true);output.start();
            send("uci");
        }catch(Exception e){
            display("Stockfish : lancement impossible : "+e.getClass().getSimpleName());
        }
    }
    private void send(String message){
        synchronized(lock){
            if(closed||writer==null)return;
            try{writer.write(message);writer.newLine();writer.flush();}
            catch(Exception e){display("Stockfish : erreur communication");}
        }
    }
    public void analyze(String fen){
        desiredFen=fen;
        whiteToMove=fen.contains(" w ");
        if(ready)submit(fen);
    }
    private void submit(String fen){
        synchronized(lock){variants.clear();}
        send("stop");
        send("position fen "+fen);
        // Finite search: one thread, small hash, 3 PV. Prevent unbounded heating.
        send("go depth 11");
    }
    private void readLoop(){
        try(BufferedReader input=new BufferedReader(new InputStreamReader(process.getInputStream(),"UTF-8"))){
            String line;
            while(!closed&&(line=input.readLine())!=null){
                if(line.equals("uciok")){
                    send("setoption name Threads value 1");
                    send("setoption name Hash value 32");
                    send("setoption name MultiPV value 3");
                    send("setoption name UCI_ShowWDL value true");
                    send("isready");
                }else if(line.equals("readyok")){
                    ready=true;
                    display("Stockfish prêt : analyse MultiPV 3…");
                    if(desiredFen!=null)submit(desiredFen);
                }else if(line.startsWith("info ")&&line.contains(" score ")&&line.contains(" pv ")){
                    onInfo(line);
                }
            }
            if(!closed)display("Stockfish : processus terminé.");
        }catch(Exception e){if(!closed)display("Stockfish : arrêt du flux d'analyse.");}
    }
    private static String tokenAfter(String s,String name){
        String key=" "+name+" ";
        int start=s.indexOf(key);
        if(start<0)return null;
        start+=key.length();
        int end=s.indexOf(' ',start);
        return end<0?s.substring(start):s.substring(start,end);
    }
    private void onInfo(String line){
        try{
            String depth=tokenAfter(line,"depth");
            String pv=tokenAfter(line,"pv");
            String rank=tokenAfter(line,"multipv");
            if(rank==null||pv==null)return;
            int idx=Integer.parseInt(rank);
            if(idx<1||idx>3)return;
            String eval="—";
            String cp=tokenAfter(line,"cp");
            String mate=tokenAfter(line,"mate");
            if(cp!=null){
                int whiteCp=Integer.parseInt(cp)*(whiteToMove?1:-1);
                eval=String.format(java.util.Locale.FRANCE,"%+.2f",whiteCp/100.0);
            }else if(mate!=null)eval="mat "+(whiteToMove?"":"-")+mate;
            String wdl="WDL —";
            int start=line.indexOf(" wdl ");
            if(start>=0){
                String[] parts=line.substring(start+5).split(" ");
                if(parts.length>=3){
                    int win=Integer.parseInt(parts[0]),draw=Integer.parseInt(parts[1]),loss=Integer.parseInt(parts[2]);
                    if(!whiteToMove){int swap=win;win=loss;loss=swap;}
                    wdl="WDL blancs "+(win/10.0)+"% / nulle "+(draw/10.0)+"% / noirs "+(loss/10.0)+"%";
                }
            }
            synchronized(lock){
                variants.put(idx,idx+". "+pv+" ("+eval+")");
                StringBuilder s=new StringBuilder("Stockfish 18 · profondeur "+(depth==null?"?":depth)+"\n");
                if(idx==1)s.append("Évaluation blancs ").append(eval).append("\n").append(wdl).append("\n");
                for(int i=1;i<=3;i++)if(variants.containsKey(i))s.append(variants.get(i)).append("\n");
                status=s.toString();
            }
            display(status);
        }catch(Exception ignored){}
    }
    /** Halt expensive analysis whenever Android leaves the foreground. */
    public void stop(){ send("stop"); }
    public void close(){
        closed=true;ready=false;
        synchronized(lock){
            if(writer!=null)try{writer.write("quit\n");writer.flush();}catch(Exception ignored){}
        }
        if(process!=null)process.destroy();
    }
}
