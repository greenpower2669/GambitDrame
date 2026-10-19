package fr.gambitdrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Experimental human-inspired policy (not a calibrated Lichess Elo engine).
 * Uses observed moves from the Lichess <1000 pool when available; otherwise
 * applies a stochastic chess-specific fallback. NEVER requests Stockfish moves.
 */
public final class HumanPolicy {
    public static final class Choice {
        public final ChessGame.Move move;
        public final String source;
        public Choice(ChessGame.Move m,String s){move=m;source=s;}
    }
    private final Random rng;
    private final int targetRating;
    private final double aggression;
    public HumanPolicy(int rating,long seed,double aggression){
        this.targetRating=Math.max(400,Math.min(900,rating));
        this.rng=new Random(seed);
        this.aggression=Math.max(0,Math.min(1,aggression));
    }
    private static int value(char c){
        switch(Character.toUpperCase(c)){
            case 'P':return 1;
            case 'N':return 3;
            case 'B':return 3;
            case 'R':return 5;
            case 'Q':return 9;
            default:return 0;
        }
    }
    private static boolean homeMinor(char piece,int from){
        if(piece=='n'||piece=='b')return from/8==0;
        if(piece=='N'||piece=='B')return from/8==7;
        return false;
    }
    public Choice choose(ChessGame.Position p,Map<String,Integer> observed,int ply){
        List<ChessGame.Move> legal=ChessGame.legal(p);
        if(legal.isEmpty())return null;
        if(observed!=null&&!observed.isEmpty()){
            List<ChessGame.Move> source=new ArrayList<>();
            List<Double> weights=new ArrayList<>();
            for(ChessGame.Move m:legal){
                Integer count=observed.get(m.uci());
                if(count!=null&&count>0){
                    // Counts reflect real human choices, not a majority-vote policy.
                    // Exponent <1 avoids overconcentrating on the most common move.
                    source.add(m);
                    weights.add(Math.pow(count,0.8));
                }
            }
            if(!source.isEmpty())
                return new Choice(draw(source,weights),"Coups réels Lichess <1000, non calibrés");
        }
        List<Double> weights=new ArrayList<>();
        double temperature=2.2-(targetRating-400)*0.0017; // experimental, NOT Elo calibration
        for(ChessGame.Move m:legal){
            double score=heuristic(p,m,ply);
            // Bound scores for numerical stability and ensure human diversity.
            score=Math.max(-7,Math.min(7,score));
            weights.add(Math.exp(score/temperature));
        }
        return new Choice(draw(legal,weights),"Politique locale expérimentale, non calibrée");
    }
    private ChessGame.Move draw(List<ChessGame.Move> moves,List<Double> weights){
        double sum=0;for(double w:weights)sum+=w;
        double r=rng.nextDouble()*sum;
        for(int i=0;i<moves.size();i++)if((r-=weights.get(i))<=0)return moves.get(i);
        return moves.get(moves.size()-1);
    }
    private double heuristic(ChessGame.Position p,ChessGame.Move m,int ply){
        char piece=p.board[m.from],target=p.board[m.to];
        char role=Character.toUpperCase(piece);
        int fy=m.from/8,fx=m.from%8,ty=m.to/8,tx=m.to%8;
        ChessGame.Position next=ChessGame.apply(p,m);
        double score=rng.nextGaussian()*0.23;
        double capture=value(target);
        if(role=='P'&&m.to==p.ep&&target=='.'&&fx!=tx)capture=1;
        score+=capture*(0.65+aggression*0.28);
        if(m.promotion!=0)score+=value(m.promotion)*1.25;
        if(ChessGame.inCheck(next,next.white))score+=0.5+aggression*0.5;
        // After the move next.white is the opponent; protect valuable moved pieces.
        boolean enPrise=ChessGame.attacked(next,m.to,next.white);
        boolean guarded=ChessGame.attacked(next,m.to,!next.white);
        if(enPrise)score-=value(piece)*(guarded?0.32:0.85);
        if(role=='P'){
            if(tx>=2&&tx<=5)score+=0.6;
            score+=0.12*(p.white?fy-ty:ty-fy);
            if(ply<12&&tx>=3&&tx<=4)score+=0.6;
        }
        if(ply<22){
            if(homeMinor(piece,m.from))score+=0.95;
            if(role=='Q')score-=0.45;
            if(role=='K'&&Math.abs(m.from-m.to)==2)score+=1.7;
            if(role=='K'&&Math.abs(m.from-m.to)!=2)score-=1.0;
        }
        if(ply>=24&&role=='K')score+=0.04;
        double center=3.5-(Math.abs(tx-3.5)+Math.abs(ty-3.5))/2;
        if(role=='N'||role=='B')score+=center*0.15;
        if(role=='P'&&((p.white&&ty==1)||(!p.white&&ty==6)))score+=0.7;
        // Avoid repeating the immediate last rank/file without a purpose.
        if(m.to==m.from)score-=10;
        return score;
    }
}
