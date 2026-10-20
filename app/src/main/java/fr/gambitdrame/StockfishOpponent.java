package fr.gambitdrame;

import android.content.Context;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Separate Stockfish ENGINE OPPONENT (not the Lichess human policy).
 * Every legal move is evaluated using MultiPV=legalMoveCount at fixed depth.
 * If full coverage fails, no false "precision" move is returned.
 */
public final class StockfishOpponent {
    public static final class Result {
        public final String uci,details;
        public final double selectedQuality,requestedQuality;
        public Result(String uci,double quality,double requested,int legal){
            this.uci=uci;selectedQuality=quality;requestedQuality=requested;
            details=String.format(java.util.Locale.FRANCE,
                "Stockfish : coup %s, qualité relative %.1f%% (cible tirée %.1f%%), %d coups évalués",
                uci,quality,requested,legal);
        }
    }
    private final Context context;
    private final Random random=new Random();
    private volatile Process active;
    private volatile boolean closed=false;
    public StockfishOpponent(Context context){this.context=context.getApplicationContext();}
    private static void send(BufferedWriter writer,String cmd)throws Exception{
        writer.write(cmd);writer.newLine();writer.flush();
    }
    private static String token(String line,String key){
        String prefix=" "+key+" ";
        int start=line.indexOf(prefix);
        if(start<0)return null;
        start+=prefix.length();
        int end=line.indexOf(' ',start);
        return end<0?line.substring(start):line.substring(start,end);
    }
    public Result select(ChessGame.Position position,int mean,int sigma)throws Exception{
        if(closed)throw new IllegalStateException("Moteur arrêté");
        List<ChessGame.Move> legal=ChessGame.legal(position);
        if(legal.isEmpty())return null;
        if(legal.size()==1)return new Result(legal.get(0).uci(),100,100,1);
        File binary=new File(context.getApplicationInfo().nativeLibraryDir,"libstockfish.so");
        if(!binary.exists())throw new IllegalStateException("Stockfish ARM64 non installé");
        Process p=new ProcessBuilder(binary.getAbsolutePath()).redirectErrorStream(true).start();
        active=p;
        final long deadline=System.nanoTime()+25_000_000_000L;
        Map<Integer,Integer> cpByRank=new HashMap<>();
        Map<Integer,String> moveByRank=new HashMap<>();
        int depth=-1;
        try(BufferedReader input=new BufferedReader(new InputStreamReader(p.getInputStream(),"UTF-8"));
            BufferedWriter output=new BufferedWriter(new OutputStreamWriter(p.getOutputStream(),"UTF-8"))){
            send(output,"uci");
            await(input,"uciok",deadline);
            send(output,"setoption name Threads value 1");
            send(output,"setoption name Hash value 16");
            send(output,"setoption name MultiPV value "+legal.size());
            send(output,"isready");
            await(input,"readyok",deadline);
            send(output,"position fen "+position.fen());
            send(output,"go depth 7");
            boolean done=false;
            while(!closed&&System.nanoTime()<deadline){
                String line=pollLine(input,deadline);
                if(line==null)break;
                if(line.startsWith("bestmove ")){done=true;break;}
                if(!line.startsWith("info ")||!line.contains(" multipv ")||
                    !line.contains(" score ")||!line.contains(" pv "))continue;
                try{
                    int rank=Integer.parseInt(token(line,"multipv"));
                    int d=Integer.parseInt(token(line,"depth"));
                    if(rank<1||rank>legal.size())continue;
                    if(d>depth){depth=d;cpByRank.clear();moveByRank.clear();}
                    if(d!=depth)continue;
                    String first=token(line,"pv");
                    if(first==null)continue;
                    boolean valid=false;
                    for(ChessGame.Move m:legal)if(m.uci().equals(first)){valid=true;break;}
                    if(!valid)continue;
                    String raw=token(line,"cp"),mate=token(line,"mate");
                    if(raw==null&&mate==null)continue;
                    int cp=raw!=null?Integer.parseInt(raw):
                        (Integer.parseInt(mate)>0?10000:-10000);
                    cpByRank.put(rank,cp);
                    moveByRank.put(rank,first);
                }catch(RuntimeException ignored){}
            }
            if(!done||cpByRank.size()!=legal.size())
                throw new IllegalStateException("Analyse MultiPV incomplète ("+
                    cpByRank.size()+"/"+legal.size()+", profondeur "+depth+")");
            List<String> names=new ArrayList<>();
            List<Integer> scores=new ArrayList<>();
            for(int rank=1;rank<=legal.size();rank++){
                String name=moveByRank.get(rank);
                Integer cp=cpByRank.get(rank);
                if(name==null||cp==null)throw new IllegalStateException("MultiPV manque un coup");
                names.add(name);scores.add(cp);
            }
            StockfishPrecision.Pick pick=StockfishPrecision.choose(
                StockfishPrecision.normalize(names,scores),mean,sigma,random);
            return new Result(pick.move.uci,pick.move.relativeQuality,pick.requested,legal.size());
        }finally{
            try{p.destroy();}catch(RuntimeException ignored){}
            if(active==p)active=null;
        }
    }
    private static void await(BufferedReader reader,String token,long deadline)throws Exception{
        while(System.nanoTime()<deadline){
            String line=pollLine(reader,deadline);
            if(line==null)break;
            if(line.equals(token))return;
        }
        throw new IllegalStateException("Stockfish ne répond pas : "+token);
    }
    private static String pollLine(BufferedReader input,long deadline)throws Exception{
        while(System.nanoTime()<deadline){
            if(input.ready())return input.readLine();
            Thread.sleep(15);
        }
        return null;
    }
    public void close(){
        closed=true;
        Process p=active;
        if(p!=null)p.destroy();
    }
}
