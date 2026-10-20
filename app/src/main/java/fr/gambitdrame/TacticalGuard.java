package fr.gambitdrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Tactical risk cap on a precision-relative selection.
 * All scores are centipawns for the player making the move. */
public final class TacticalGuard {
    private TacticalGuard(){}
    public static final class Decision {
        public final StockfishPrecision.Pick original,chosen;
        public final int bestCp,lossCp,limitCp;
        public final boolean intervened;
        Decision(StockfishPrecision.Pick initial,StockfishPrecision.Pick finalPick,
                 int best,int loss,int limit) {
            original=initial;chosen=finalPick;bestCp=best;lossCp=loss;
            limitCp=limit;intervened=!initial.move.uci.equals(finalPick.move.uci);
        }
    }
    /** At 80%: at most 60 cp worse than the best. At 0%: 1020 cp.
     * Deliberately uncalibrated: these are safety limits, not Elo or human accuracy. */
    public static int allowableLossCp(int precision) {
        int p=Math.max(0,Math.min(100,precision));
        int d=100-p;
        return 20+d*d/10;
    }
    public static Decision choose(List<StockfishPrecision.Candidate> candidates,
                                  int target,int sigma,Random random) {
        if(candidates.isEmpty())throw new IllegalArgumentException("empty candidates");
        StockfishPrecision.Pick unconstrained=
            StockfishPrecision.choose(candidates,target,sigma,random);
        int best=Integer.MIN_VALUE;
        for(StockfishPrecision.Candidate c:candidates)best=Math.max(best,c.scoreCp);
        int limit=allowableLossCp(target);
        List<StockfishPrecision.Candidate> safe=new ArrayList<>();
        for(StockfishPrecision.Candidate c:candidates)
            if((long)best-c.scoreCp<=limit)safe.add(c);
        if(safe.isEmpty())throw new IllegalStateException("no safe move");
        StockfishPrecision.Candidate chosen=unconstrained.move;
        if((long)best-chosen.scoreCp>limit){
            double closest=Double.POSITIVE_INFINITY;
            List<StockfishPrecision.Candidate> ties=new ArrayList<>();
            for(StockfishPrecision.Candidate c:safe){
                double distance=Math.abs(c.relativeQuality-unconstrained.requested);
                if(distance<closest-1e-8){closest=distance;ties.clear();ties.add(c);}
                else if(Math.abs(distance-closest)<1e-8)ties.add(c);
            }
            chosen=ties.get(random.nextInt(ties.size()));
        }
        StockfishPrecision.Pick finalPick=new StockfishPrecision.Pick(chosen,unconstrained.requested);
        return new Decision(unconstrained,finalPick,best,best-chosen.scoreCp,limit);
    }
}
