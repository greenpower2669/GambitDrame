package fr.gambitdrame;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/** Offline background study. Evaluates all saved positions in one persistent Stockfish process. */
public final class GameStudyEngine {
    private GameStudyEngine(){}
    public interface Progress {void update(int done,int total);}
    private static final int DEPTH=9;
    private static void send(BufferedWriter out,String cmd)throws Exception{
        out.write(cmd);out.newLine();out.flush();
    }
    private static String read(BufferedReader in,long deadline,BooleanSupplier cancelled)throws Exception{
        while(System.nanoTime()<deadline){
            if(cancelled.getAsBoolean())throw new InterruptedException("Analyse annulée");
            if(in.ready())return in.readLine();
            Thread.sleep(15);
        }
        throw new IllegalStateException("Stockfish : délai d'analyse dépassé");
    }
    private static void await(BufferedReader in,String wanted,BooleanSupplier cancelled)throws Exception{
        long deadline=System.nanoTime()+15_000_000_000L;
        while(System.nanoTime()<deadline){
            String line=read(in,deadline,cancelled);
            if(line==null)throw new IllegalStateException("Stockfish a fermé le flux");
            if(line.equals(wanted))return;
        }
        throw new IllegalStateException("Stockfish : "+wanted+" absent");
    }
    private static Integer score(String line){
        int cp=line.indexOf(" score cp ");
        if(cp>=0){
            String tail=line.substring(cp+10).split(" ")[0];
            return Integer.parseInt(tail);
        }
        int mate=line.indexOf(" score mate ");
        if(mate>=0){
            int plies=Integer.parseInt(line.substring(mate+12).split(" ")[0]);
            return plies>=0?10000:-10000;
        }
        return null;
    }
    private static int evaluate(ChessGame.Position position,BufferedReader in,
                                BufferedWriter out,BooleanSupplier cancelled)throws Exception{
        if(ChessGame.legal(position).isEmpty())
            return ChessGame.inCheck(position,position.white)?-10000:0;
        send(out,"position fen "+position.fen());
        send(out,"go depth "+DEPTH);
        long deadline=System.nanoTime()+25_000_000_000L;
        Integer latest=null;boolean complete=false;
        while(System.nanoTime()<deadline){
            String line=read(in,deadline,cancelled);
            if(line==null)break;
            if(line.startsWith("bestmove ")){complete=true;break;}
            if(line.startsWith("info ")&&line.contains(" score ")&&line.contains(" depth ")){
                Integer value=score(line);
                if(value!=null)latest=value;
            }
        }
        if(!complete||latest==null)
            throw new IllegalStateException("Stockfish : évaluation incomplète pour "+position.fen());
        return latest;
    }
    public static JSONObject analyze(Context ctx,List<ChessGame.Position> positions,
                        List<String> moves,Progress progress,
                        BooleanSupplier cancelled)throws Exception{
        if(positions==null||moves==null||moves.size()==0||
           positions.size()!=moves.size()+1)
            throw new IllegalArgumentException("Historique de partie incomplet");
        File binary=new File(ctx.getApplicationInfo().nativeLibraryDir,"libstockfish.so");
        if(!binary.isFile())throw new IllegalStateException("Stockfish ARM64 absent");
        Process process=new ProcessBuilder(binary.getAbsolutePath())
            .redirectErrorStream(true).start();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(process.getInputStream(),"UTF-8"));
            BufferedWriter out=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),"UTF-8"))){
            send(out,"uci");await(in,"uciok",cancelled);
            send(out,"setoption name Threads value 1");
            send(out,"setoption name Hash value 16");
            send(out,"setoption name MultiPV value 1");
            send(out,"isready");await(in,"readyok",cancelled);
            List<Integer> scores=new ArrayList<>();
            for(int i=0;i<positions.size();i++){
                scores.add(evaluate(positions.get(i),in,out,cancelled));
                if(progress!=null)progress.update(i+1,positions.size());
            }
            List<Integer> losses=new ArrayList<>();
            List<Boolean> whiteToMove=new ArrayList<>();
            JSONArray perMove=new JSONArray(),uciMoves=new JSONArray();
            for(int i=0;i<moves.size();i++){
                boolean white=positions.get(i).white;
                // Root best score is for mover; following score is for opponent.
                int loss=(int)Math.min(2000L,Math.max(0L,
                    (long)scores.get(i)+(long)scores.get(i+1)));
                losses.add(loss);
                whiteToMove.add(white);
                JSONObject row=new JSONObject();
                row.put("uci",moves.get(i));
                row.put("white",white);
                row.put("lossCp",loss);
                row.put("accuracy",Math.round(StudyMetrics.accuracy(loss)*10.0)/10.0);
                perMove.put(row);uciMoves.put(moves.get(i));
            }
            StudyMetrics.Summary ws=StudyMetrics.summarize(
                StudyMetrics.select(losses,whiteToMove,true));
            StudyMetrics.Summary bs=StudyMetrics.summarize(
                StudyMetrics.select(losses,whiteToMove,false));
            OpeningTheory.Result opening=OpeningTheory.measure(moves);
            JSONObject record=new JSONObject();
            record.put("schema",1);
            record.put("createdAt",System.currentTimeMillis());
            record.put("baseFen",positions.get(0).fen());
            record.put("uciMoves",uciMoves);
            record.put("perMove",perMove);
            record.put("whiteAccuracy",round(ws.mean));
            record.put("whiteSigma",round(ws.sigma));
            record.put("blackAccuracy",round(bs.mean));
            record.put("blackSigma",round(bs.sigma));
            record.put("whiteMoves",ws.count);
            record.put("blackMoves",bs.count);
            record.put("whiteBlunders",ws.blunders);
            record.put("blackBlunders",bs.blunders);
            record.put("whiteRecovery",StudyMetrics.recovery(losses,whiteToMove,true));
            record.put("blackRecovery",StudyMetrics.recovery(losses,whiteToMove,false));
            record.put("opening",opening.name);
            record.put("theoryMatches",opening.matches);
            record.put("theoryCompared",opening.compared);
            record.put("theoryPercent",opening.percent);
            record.put("theoryText",opening.display());
            ChessGame.Position last=positions.get(positions.size()-1);
            int repeated=0;
            for(ChessGame.Position pos:positions)
                if(pos.repetitionKey().equals(last.repetitionKey()))repeated++;
            String outcome=ChessGame.outcome(last,repeated);
            String result=outcome.startsWith("Échec et mat")?
                (last.white?"0-1":"1-0"):
                ((outcome.startsWith("Pat")||outcome.startsWith("Nulle"))?"1/2-1/2":"*");
            record.put("result",result);
            record.put("outcome",result.equals("*")?"Partie non terminée":outcome);
            record.put("depth",DEPTH);
            record.put("metric","exp(-centipawnLoss/180) × 100; sigma=écart-type des précisions par coup (points), pas sigma du curseur adverse");
            return record;
        }finally{
            process.destroy();
        }
    }
    private static double round(double a){return Math.round(a*10.0)/10.0;}
}
