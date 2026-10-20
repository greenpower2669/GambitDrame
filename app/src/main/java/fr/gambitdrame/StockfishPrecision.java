package fr.gambitdrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Normalized relative Stockfish move quality; not game-accuracy Elo or ACPL. */
public final class StockfishPrecision {
    public static final class Candidate {
        public final String uci;
        public final int scoreCp; // score from side-to-move perspective (mate clipped to ±10000)
        public final double relativeQuality;
        public Candidate(String uci,int cp,double quality){this.uci=uci;scoreCp=cp;relativeQuality=quality;}
    }
    public static List<Candidate> normalize(List<String> moves,List<Integer> scores){
        if(moves.size()!=scores.size()||moves.isEmpty())throw new IllegalArgumentException("candidates");
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(Integer cp:scores){if(cp==null)throw new IllegalArgumentException("missing engine score");min=Math.min(min,cp);max=Math.max(max,cp);}
        List<Candidate> ranked=new ArrayList<>();
        for(int i=0;i<moves.size();i++){
            double quality=max==min?100.0:100.0*(scores.get(i)-min)/(max-min);
            ranked.add(new Candidate(moves.get(i),scores.get(i),quality));
        }
        return ranked;
    }
    public static final class Pick {
        public final Candidate move;
        public final double requested;
        public Pick(Candidate c,double q){move=c;requested=q;}
    }
    public static Pick choose(List<Candidate> ranked,double target,double spread,Random random){
        if(ranked.isEmpty())throw new IllegalArgumentException("No ranked Stockfish candidates");
        double mean=Math.max(0,Math.min(100,target));
        // Most deviations are small; large outliers are possible but unusual.
        double sigma=Math.max(0,Math.min(35,spread));
        double requested=Math.max(0,Math.min(100,mean+random.nextGaussian()*sigma));
        double bestDistance=Double.POSITIVE_INFINITY;
        List<Candidate> ties=new ArrayList<>();
        for(Candidate c:ranked){
            double distance=Math.abs(c.relativeQuality-requested);
            if(distance+1e-8<bestDistance){bestDistance=distance;ties.clear();ties.add(c);}
            else if(Math.abs(distance-bestDistance)<1e-8)ties.add(c);
        }
        return new Pick(ties.get(random.nextInt(ties.size())),requested);
    }
}
