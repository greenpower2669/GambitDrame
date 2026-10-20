package fr.gambitdrame;

import android.app.Activity;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    private final Random guardRandom=new Random();
    private final AdaptiveOpponent.State strategy=new AdaptiveOpponent.State();
    private volatile int gaugeRootCp=0;
    public void resetPlan(){strategy.clear();trace("PLAN","Réinitialisé");}
    private EngineDiagnostics diagnostics;
    private int phase=0; // 0 idle, 1 gauge, 2 candidate ranking
    private GuardRequest request;
    private static final class GuardRequest {
        final ChessGame.Position root; final int target,sigma;
        final boolean adaptive;
        final int sensitivity,recovery,stability;
        final List<String> legal=new ArrayList<>();
        final Map<String,Integer> cp=new HashMap<>();
        final CountDownLatch latch=new CountDownLatch(1);
        StockfishOpponent.Result result; Exception failure;
        GuardRequest(ChessGame.Position root,int target,int sigma,
                     boolean adaptive,int sensitivity,int recovery,int stability){
            this.root=root;this.target=target;this.sigma=sigma;
            this.adaptive=adaptive;this.sensitivity=sensitivity;
            this.recovery=recovery;this.stability=stability;
            for(ChessGame.Move m:ChessGame.legal(root))legal.add(m.uci());
        }
    }
    public void setDiagnostics(EngineDiagnostics diagnostics){this.diagnostics=diagnostics;}
    private void trace(String label,String detail){
        EngineDiagnostics d=diagnostics;if(d!=null)d.record(label,detail);
    }
    private void failRequest(String text){
        GuardRequest r=request;request=null;
        if(r!=null){r.failure=new IllegalStateException(text);r.latch.countDown();trace("GUARD_ERR",text);}
    }
    public void cancelGuard(){
        synchronized(lock){
            if(request==null)return;
            failRequest("Position ou réglages changés");
            if(phase==2&&!awaitingReady){
                phase=0;activeFen=null;awaitingReady=true;send("stop");send("isready");
            }
        }
    }
    public StockfishOpponent.Result selectGuarded(ChessGame.Position root,int target,int sigma)throws Exception{
        return selectGuarded(root,target,sigma,false,75,95,70);
    }
    public StockfishOpponent.Result selectGuarded(ChessGame.Position root,int target,int sigma,
                           boolean adaptive,int sensitivity,int recovery,int stability)throws Exception{
        GuardRequest r=new GuardRequest(root,target,sigma,adaptive,sensitivity,recovery,stability);
        if(r.legal.isEmpty())return null;
        if(r.legal.size()==1)return new StockfishOpponent.Result(r.legal.get(0),100,100,1);
        synchronized(lock){
            if(closed||writer==null)throw new IllegalStateException("Moteur de jauge absent");
            if(request!=null)throw new IllegalStateException("Garde-fou précédent actif");
            if(!root.fen().equals(desiredFen))throw new IllegalStateException("FEN jauge différente de la partie");
            request=r;
            trace("GUARD_START","Réutilisation moteur et hash jauge11→MultiPV12, coups="+r.legal.size()+
                " target="+target+" sigma="+sigma+" adaptive="+adaptive+
                " sensitivity="+sensitivity+" recovery="+recovery+" stability="+stability+
                " fen="+root.fen());
            if(ready&&!awaitingReady&&phase==0&&root.fen().equals(activeFen))startGuard();
        }
        boolean ok;
        try{ok=r.latch.await(90,TimeUnit.SECONDS);}
        catch(InterruptedException ex){cancelGuard();Thread.currentThread().interrupt();throw ex;}
        if(!ok){cancelGuard();throw new IllegalStateException("MultiPV12 dépasse 90 secondes, aucun coup non vérifié joué");}
        if(r.failure!=null)throw r.failure;
        if(r.result==null)throw new IllegalStateException("Garde-fou sans résultat");
        return r.result;
    }
    private void startGuard(){
        GuardRequest r=request;
        if(r==null||phase!=0||paused||awaitingReady||closed)return;
        if(!r.root.fen().equals(activeFen)){failRequest("FEN active incompatible");return;}
        phase=2;r.cp.clear();
        send("setoption name MultiPV value "+r.legal.size());
        send("position fen "+activeFen);
        send("go depth 12");
        trace("GUARD_SEARCH","same Stockfish process + hash, MultiPV="+r.legal.size()+" depth=12");
    }
    private void guardInfo(String line){
        synchronized(lock){
            GuardRequest r=request;
            if(r==null||phase!=2||awaitingReady)return;
            try{
                String depth=tokenAfter(line,"depth"),move=tokenAfter(line,"pv");
                if(depth==null||!depth.equals("12")||move==null||!r.legal.contains(move))return;
                String cp=tokenAfter(line,"cp"),mate=tokenAfter(line,"mate");
                if(cp==null&&mate==null)return;
                r.cp.put(move,cp!=null?Integer.parseInt(cp):
                    (Integer.parseInt(mate)>0?10000:-10000));
            }catch(RuntimeException ex){trace("GUARD_PARSE_ERR",String.valueOf(ex));}
        }
    }
    private void finishedSearch(){
        synchronized(lock){
            if(awaitingReady)return;
            if(phase==1){
                phase=0;
                if(request!=null&&request.root.fen().equals(activeFen))startGuard();
            }else if(phase==2){
                GuardRequest r=request;request=null;phase=0;
                if(r==null)return;
                try{
                    if(r.cp.size()!=r.legal.size())
                        throw new IllegalStateException("MultiPV 12 incomplet ("+r.cp.size()+"/"+r.legal.size()+")");
                    List<Integer> scores=new ArrayList<>();
                    for(String m:r.legal){
                        Integer cp=r.cp.get(m);
                        if(cp==null)throw new IllegalStateException("coup sans score "+m);
                        scores.add(cp);
                    }
                    List<StockfishPrecision.Candidate> ranking=StockfishPrecision.normalize(r.legal,scores);
                    TacticalGuard.Decision choice=null;
                    AdaptiveOpponent.Choice strategic=null;
                    if(r.adaptive) strategic=AdaptiveOpponent.choose(r.root,ranking,r.target,r.sigma,
                        r.sensitivity,r.recovery,r.stability,strategy,guardRandom,gaugeRootCp);
                    else choice=TacticalGuard.choose(ranking,r.target,r.sigma,guardRandom);
                    int whiteSign=r.root.white?1:-1;
                    for(StockfishPrecision.Candidate m:ranking)
                        trace("GUARD_RANK",m.uci+" cp="+m.scoreCp+
                            " relative="+String.format(java.util.Locale.US,"%.1f",m.relativeQuality)+"%");
                    if(strategic!=null){
                        trace("ADAPTIVE","rootCp="+strategic.rootCp+" effective="+strategic.effectiveTarget+
                            " bestCp="+strategic.bestCp+" chosen="+strategic.move.uci+
                            " choiceCp="+strategic.move.scoreCp+
                            " lossCp="+strategic.lossCp+" cap="+strategic.limitCp+
                            " recovery="+strategic.recovery+" plan="+strategic.plan+
                            " reason="+strategic.reason+" alternatives="+strategic.alternatives);
                        strategy.lastMove=strategic.move.uci;
                        strategy.plan=strategic.plan;
                        strategy.lastScoreCp=strategic.move.scoreCp;
                        r.result=new StockfishOpponent.Result(strategic.move.uci,
                            strategic.move.relativeQuality,r.target,r.legal.size(),
                            strategic.move.scoreCp*whiteSign,strategic.bestCp*whiteSign,
                            strategic.lossCp,strategic.limitCp,false);
                    }else{
                        trace("GUARD_CHOSEN","raw="+choice.original.move.uci+" chosen="+choice.chosen.move.uci+
                            " draw="+choice.chosen.requested+" lossCp="+choice.lossCp+
                            " limitCp="+choice.limitCp+" override="+choice.intervened);
                        r.result=new StockfishOpponent.Result(choice.chosen.move.uci,
                            choice.chosen.move.relativeQuality,choice.chosen.requested,r.legal.size(),
                            choice.chosen.move.scoreCp*whiteSign,choice.bestCp*whiteSign,
                            choice.lossCp,choice.limitCp,choice.intervened);
                    }
                }catch(Exception ex){r.failure=ex;trace("GUARD_ERR",String.valueOf(ex));}
                finally{r.latch.countDown();}
            }
        }
    }

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
        if(desiredFen!=null&&!desiredFen.equals(fen))cancelGuard();
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
            phase=0;
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
            gaugeRootCp=0;
            phase=1;
            send("setoption name MultiPV value 3");
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
                }else if(line.startsWith("bestmove ")){
                    finishedSearch();
                }else if(line.startsWith("info ")&&line.contains(" score ")&&line.contains(" pv ")){
                    if(!paused&&!awaitingReady&&activeFen!=null){
                        if(phase==2)guardInfo(line);
                        else if(phase==1)onInfo(line);
                    }
                }
            }
            synchronized(lock){failRequest("Stockfish terminé");}
            if(!closed)dispatch(null,"Stockfish : processus terminé.",null,null);
        }catch(Exception ex){
            synchronized(lock){failRequest("Stockfish lecture UCI interrompue: "+ex);}
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
            if(rank==1&&rawCp!=null&&"11".equals(tokenAfter(line,"depth")))
                gaugeRootCp=Integer.parseInt(rawCp);
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
        cancelGuard();
        paused=true;
        phase=0;
        activeFen=null;
        send("stop");
    }
    public void close(){
        cancelGuard();
        closed=true;
        ready=false;
        synchronized(lock){
            if(writer!=null)
                try{writer.write("quit\n");writer.flush();}catch(Exception ignored){}
        }
        if(process!=null)process.destroy();
    }
}
