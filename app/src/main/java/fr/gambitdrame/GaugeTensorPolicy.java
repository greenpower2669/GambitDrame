package fr.gambitdrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Three gauge controls, no hidden skill multiplier.
 * Grey = desired rank within ALL scored legal moves (50 = middle).
 * Green = defensive threshold on the bot's centipawn scale (< 0);
 * Red = domination threshold on the bot's centipawn scale (> 0).
 * A smooth tensor moves the desired rank toward 100 (green) or 0 (red).
 */
public final class GaugeTensorPolicy {
    private GaugeTensorPolicy(){}
    private static int clamp(int x,int a,int b){return Math.max(a,Math.min(b,x));}
    private static double clip(double x){return Math.max(0,Math.min(1,x));}
    private static double smooth(double x){x=clip(x);return x*x*(3-2*x);}
    public static final class Tension {
        public final double green,red,targetRank;
        Tension(double green,double red,double targetRank){
            this.green=green;this.red=red;this.targetRank=targetRank;
        }
    }
    public static Tension tension(int rootCp,int greyLevel,int redCp,int greenCp){
        int red=clamp(redCp,50,1200);
        int green=clamp(greenCp,-1200,-50);
        double towardGreen=rootCp<0?smooth((-rootCp)/(double)(-green)):0;
        double towardRed=rootCp>0?smooth(rootCp/(double)red):0;
        double nominal=clamp(greyLevel,0,100);
        double desired=nominal+(100-nominal)*towardGreen-nominal*towardRed;
        return new Tension(towardGreen,towardRed,clip(desired/100.0)*100.0);
    }
    public static final class Decision {
        public final StockfishPrecision.Candidate move;
        public final int nominal,rootCp,greenCp,redCp,lossCp,capCp,rankCount;
        public final double greenTension,redTension,targetRank,drawnRank,chosenRank;
        public final boolean planPreserved;
        public final String previousPlan,plan,reason,alternatives,planLine;
        Decision(StockfishPrecision.Candidate move,int nominal,int rootCp,int greenCp,int redCp,
                 int lossCp,int capCp,int rankCount,Tension t,double drawn,double selected,
                 boolean preserved,String previousPlan,String plan,String reason,
                 String alternatives,String planLine){
            this.move=move;this.nominal=nominal;this.rootCp=rootCp;this.greenCp=greenCp;
            this.redCp=redCp;this.lossCp=lossCp;this.capCp=capCp;this.rankCount=rankCount;
            greenTension=t.green;redTension=t.red;targetRank=t.targetRank;drawnRank=drawn;
            chosenRank=selected;planPreserved=preserved;this.previousPlan=previousPlan;
            this.plan=plan;this.reason=reason;this.alternatives=alternatives;this.planLine=planLine;
        }
    }
    public static Decision choose(ChessGame.Position root,
                                  List<StockfishPrecision.Candidate> candidates,
                                  int nominal,int sigma,int rootCp,int redCp,int greenCp,
                                  AdaptiveOpponent.State history,Random random,
                                  int stability,java.util.Map<String,String> principalVariations){
        if(candidates.isEmpty())throw new IllegalArgumentException("aucun coup légal");
        final Tension t=tension(rootCp,nominal,redCp,greenCp);
        List<StockfishPrecision.Candidate> sorted=new ArrayList<>(candidates);
        // By actual cp evaluation rather than min/max relative quality.
        Collections.sort(sorted,(a,b)->{
            int cp=Integer.compare(b.scoreCp,a.scoreCp);
            return cp!=0?cp:a.uci.compareTo(b.uci);
        });
        final int best=sorted.get(0).scoreCp;
        final double drawn=clip((t.targetRank+random.nextGaussian()*clamp(sigma,0,30))/100.0)*100;
        final int cap=Math.min(650,Math.max(12,
            (int)Math.round(25+5.2*(100-t.targetRank))));
        final String previous=history==null?"":history.plan;
        final String expected=history==null?"":history.lastMove;
        List<ChessGame.Move> legal=ChessGame.legal(root);
        double min=Double.POSITIVE_INFINITY;
        StockfishPrecision.Candidate selected=null;
        String selectedPlan="";
        double chosenRank=100;
        int accepted=0,discarded=0;
        for(int i=0;i<sorted.size();i++){
            StockfishPrecision.Candidate c=sorted.get(i);
            final int loss=best-c.scoreCp;
            if(loss>cap){discarded++;continue;}
            accepted++;
            double rank=sorted.size()==1?100:100.0*(sorted.size()-1-i)/(sorted.size()-1);
            String plan="";
            for(ChessGame.Move m:legal)
                if(m.uci().equals(c.uci)){
                    plan=AdaptiveOpponent.planOf(root,m,rootCp);break;
                }
            double continuity=!previous.isEmpty()&&previous.equals(plan)?
                8.0*clamp(stability,0,100)/100.0:0;
            // A previous deep variation carries a little more weight, but
            // can never force a tactically inadmissible move.
            double variationBonus=expected!=null&&expected.equals(c.uci)?5:0;
            double distance=Math.abs(rank-drawn)-continuity-variationBonus;
            if(distance<min-1e-6){
                min=distance;selected=c;selectedPlan=plan;chosenRank=rank;
            }
        }
        if(selected==null)throw new IllegalStateException("aucun coup après filtre tactique");
        boolean kept=!previous.isEmpty()&&selectedPlan.equals(previous);
        String newLine=principalVariations==null?"":principalVariations.get(selected.uci);
        if(newLine==null)newLine="";
        String followup="";
        String[] pv=newLine.trim().split("\\s+");
        if(pv.length>=3&&pv[0].equals(selected.uci))followup=pv[2];
        String why;
        if(kept)why="plan conservé : cohérence et score admissible";
        else if(previous.isEmpty())why="premier plan sélectionné";
        else if(!expected.isEmpty()&&!expected.equals(selected.uci))
            why="variante précédente non retenue : nouveau rang/tension ou position réévaluée";
        else why="plan abandonné : préférence de rang/tension ou évaluation modifiée";
        StringBuilder alternatives=new StringBuilder();
        for(int i=0;i<Math.min(6,sorted.size());i++){
            StockfishPrecision.Candidate c=sorted.get(i);
            if(i>0)alternatives.append(", ");
            alternatives.append(c.uci).append(":").append(c.scoreCp).append("cp");
        }
        String reason=String.format(Locale.FRANCE,
            "%s ; gris=%d, rouge=%dcp tenseur=%.2f, vert=%dcp tenseur=%.2f ; "+
            "rang visé=%.1f, rang tiré=%.1f, rang choisi=%.1f ; "+
            "écart au meilleur=%d/%dcp, candidats admis=%d, filtrés=%d ; "+
            "plan ancien='%s', nouveau='%s' ; variante=%s",
            why,clamp(nominal,0,100),redCp,t.red,greenCp,t.green,
            t.targetRank,drawn,chosenRank,best-selected.scoreCp,cap,
            accepted,discarded,previous,selectedPlan,newLine);
        return new Decision(selected,clamp(nominal,0,100),rootCp,greenCp,redCp,
            best-selected.scoreCp,cap,sorted.size(),t,drawn,chosenRank,kept,
            previous,selectedPlan,reason,alternatives.toString(),followup);
    }
}
