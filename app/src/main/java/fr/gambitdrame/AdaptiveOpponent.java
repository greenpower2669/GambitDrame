package fr.gambitdrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Position-aware opponent. The nominal percent is a style/strength setting,
 * NOT a mandatory centipawn mistake and NOT an Elo calibration.
 * Engine scores are all from the playing side's perspective.
 */
public final class AdaptiveOpponent {
    private AdaptiveOpponent(){}
    public static final class State {
        public String plan="";
        public String lastMove="";
        public int lastScoreCp=0;
        public void clear(){plan="";lastMove="";lastScoreCp=0;}
    }
    public static final class Choice {
        public final StockfishPrecision.Candidate move;
        public final String plan,reason,alternatives;
        public final int rootCp,bestCp,lossCp,limitCp,effectiveTarget;
        public final boolean recovery,planPreserved;
        Choice(StockfishPrecision.Candidate m,String plan,String why,String alts,
               int root,int best,int limit,int effective,boolean recovery,boolean kept){
            move=m;this.plan=plan;reason=why;alternatives=alts;rootCp=root;
            bestCp=best;lossCp=best-m.scoreCp;limitCp=limit;
            effectiveTarget=effective;this.recovery=recovery;planPreserved=kept;
        }
    }
    static int effectiveTarget(int requested,int rootCp,int sensitivity,int recoveryIntensity){
        int t=Math.max(0,Math.min(100,requested));
        int deficit=Math.max(0,-rootCp-50);
        double urgency=Math.min(1.0,deficit/(double)Math.max(70,440-(sensitivity*3)));
        return Math.min(100,t+(int)Math.round((100-t)*urgency*recoveryIntensity/100.0));
    }
    static int lossBudget(int target,int rootCp,int sensitivity,int recovery){
        int baseline=12+(100-target)*2; // 80%: 52cp, 0%: 212cp.
        int danger=Math.max(0,-rootCp-60);
        double urgency=Math.min(1.0,danger/(double)Math.max(70,440-(sensitivity*3)));
        return Math.max(8,(int)Math.round(baseline*(1.0-urgency*recovery/110.0)));
    }
    static String planOf(ChessGame.Position p,ChessGame.Move m,int rootCp){
        char piece=Character.toUpperCase(p.board[m.from]);
        ChessGame.Position next=ChessGame.apply(p,m);
        if(ChessGame.inCheck(next,next.white))return "attaque du roi";
        if(p.board[m.to]!='.')return rootCp< -100?"contre-jeu tactique":"pression tactique";
        if(piece=='K')return "sécurité du roi";
        if(piece=='P')return "structure de pions";
        if(piece=='N'||piece=='B')return "développement";
        if(piece=='R')return "activité des tours";
        return rootCp< -100?"défense active":"pression positionnelle";
    }
    public static Choice choose(ChessGame.Position root,
            List<StockfishPrecision.Candidate> scores,int nominal,int sigma,
            int sensitivity,int recoveryIntensity,int stability,
            State history,Random random,int rootCp){
        if(scores.isEmpty())throw new IllegalArgumentException("Aucun coup candidat");
        int best=Integer.MIN_VALUE;
        for(StockfishPrecision.Candidate c:scores)best=Math.max(best,c.scoreCp);
        int effective=effectiveTarget(nominal,rootCp,sensitivity,recoveryIntensity);
        int limit=lossBudget(effective,rootCp,sensitivity,recoveryIntensity);
        // A real positional deficit tightens both strength and error budget,
        // allowing Stockfish to play the objectively best defence.
        boolean recovery=rootCp < -90;
        List<StockfishPrecision.Candidate> allowed=new ArrayList<>();
        double[] weights=new double[scores.size()];
        String previous=history==null?"":history.plan;
        double total=0;
        for(int i=0;i<scores.size();i++){
            StockfishPrecision.Candidate c=scores.get(i);
            int lost=best-c.scoreCp;
            if(lost>limit)continue;
            String plan="";
            ChessGame.Move candidate=null;
            for(ChessGame.Move m:ChessGame.legal(root))
                if(m.uci().equals(c.uci)){candidate=m;break;}
            if(candidate!=null)plan=planOf(root,candidate,rootCp);
            double continuity=!previous.isEmpty()&&previous.equals(plan)?
                Math.max(0,Math.min(100,stability))/100.0:0.0;
            // A softmax over small centipawn differences, not a forced target
            // for a different mistake on every ply. Persistent plan only biases
            // near-equivalent moves; it never overrides the safety budget.
            double temp=Math.max(7.0,8+(100-effective)*.47+
                Math.max(0,Math.min(30,sigma))*.8);
            double w=Math.exp(-lost/temp)*(1.0+continuity*.75);
            allowed.add(c);weights[i]=w;total+=w;
        }
        if(allowed.isEmpty())throw new IllegalStateException("Aucun candidat stratégique valide");
        double draw=random.nextDouble()*total;
        StockfishPrecision.Candidate selected=allowed.get(allowed.size()-1);
        for(int i=0;i<scores.size();i++){
            if(weights[i]<=0)continue;
            draw-=weights[i];
            if(draw<=0){selected=scores.get(i);break;}
        }
        String selectedPlan="";
        for(ChessGame.Move m:ChessGame.legal(root))
            if(m.uci().equals(selected.uci)){selectedPlan=planOf(root,m,rootCp);break;}
        boolean preserved=!previous.isEmpty()&&previous.equals(selectedPlan);
        String alternatives="";
        for(StockfishPrecision.Candidate c:scores){
            if(alternatives.length()>0)alternatives+=", ";
            alternatives+=c.uci+"="+c.scoreCp;
            if(alternatives.split(",").length>=4)break;
        }
        String reason=(recovery?"Défense et contre-jeu prioritaires":"Plan selon force nominale")+
            "; "+(preserved?"plan conservé":"plan réévalué")+
            "; perte "+(best-selected.scoreCp)+"cp sous limite "+limit+"cp";
        return new Choice(selected,selectedPlan,reason,alternatives,
            rootCp,best,limit,effective,recovery,preserved);
    }
}
