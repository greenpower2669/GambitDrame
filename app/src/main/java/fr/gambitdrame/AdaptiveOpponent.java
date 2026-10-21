package fr.gambitdrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Nominal playing level is NOT the score sought on the evaluation gauge.
 * The recovery goal is a centipawn threshold for the BOT's side of the board.
 * No "nominal 30 -> effective 97" conversion is permitted.
 */
public final class AdaptiveOpponent {
    private AdaptiveOpponent(){}
    public static final class State {
        public String plan="";
        public String lastMove="";
        public int lastScoreCp=0;
        public boolean recovering=false;
        public int recoveryGoalCp=-100;
        public void clear(){
            plan="";lastMove="";lastScoreCp=0;
            recovering=false;recoveryGoalCp=-100;
        }
    }
    public static final class Choice {
        public final StockfishPrecision.Candidate move;
        public final String plan,reason,alternatives;
        public final int rootCp,bestCp,lossCp,limitCp,effectiveTarget,recoveryGoalCp;
        public final boolean recovery,planPreserved;
        Choice(StockfishPrecision.Candidate chosen,String plan,String reason,String alternatives,
               int root,int best,int cap,int nominal,boolean recovering,boolean kept,int goal){
            move=chosen;this.plan=plan;this.reason=reason;this.alternatives=alternatives;
            rootCp=root;bestCp=best;lossCp=best-chosen.scoreCp;limitCp=cap;
            effectiveTarget=nominal;recovery=recovering;planPreserved=kept;recoveryGoalCp=goal;
        }
    }
    private static int clamp(int n,int lo,int hi){return Math.max(lo,Math.min(hi,n));}
    /** A recovery objective is a position threshold, never a higher difficulty. */
    public static boolean updateRecovery(State state,int rootCp,int goalCp,int sensitivity){
        int goal=clamp(goalCp,-500,0);
        int hysteresis=80+2*(100-clamp(sensitivity,0,100));
        if(state==null)return rootCp<=goal-hysteresis;
        if(state.recoveryGoalCp!=goal){
            state.recoveryGoalCp=goal;
            state.recovering=false;
        }
        if(state.recovering){
            // Stop only when the POSITION really reaches the gauge objective.
            if(rootCp>=goal)state.recovering=false;
        }else if(rootCp<=goal-hysteresis){
            state.recovering=true;
        }
        return state.recovering;
    }
    /** Human-like distribution breadth depends on level, not the recovery goal. */
    static double selectionTemperature(int nominal,int sigma,boolean recovering,int recoveryIntensity){
        int level=clamp(nominal,0,100),deviation=clamp(sigma,0,30);
        double normal=6.0+0.018*(100-level)*(100-level);
        normal*=1.0+deviation/15.0;
        // Recovery modestly favors defence, without changing nominal level.
        if(recovering)normal*=1.0-0.23*clamp(recoveryIntensity,0,100)/100.0;
        return Math.max(2.0,normal);
    }
    static int lossBudget(int nominal,boolean recovering,int recoveryIntensity){
        int level=clamp(nominal,0,100);
        int cap=15+5*(100-level); // 30%: 365cp; 90%: 65cp.
        if(recovering)cap=(int)Math.round(cap*(1.0-0.22*clamp(recoveryIntensity,0,100)/100.0));
        return Math.max(12,cap);
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
            State history,Random random,int rootCp,int goalCp){
        if(scores.isEmpty())throw new IllegalArgumentException("Aucun coup candidat");
        int level=clamp(nominal,0,100);
        int goal=clamp(goalCp,-500,0);
        boolean recovering=updateRecovery(history,rootCp,goal,sensitivity);
        int best=Integer.MIN_VALUE;
        for(StockfishPrecision.Candidate c:scores)best=Math.max(best,c.scoreCp);
        int limit=lossBudget(level,recovering,recoveryIntensity);
        double temp=selectionTemperature(level,sigma,recovering,recoveryIntensity);
        String priorPlan=history==null?"":history.plan;
        List<ChessGame.Move> legal=ChessGame.legal(root);
        double total=0;
        double[] weights=new double[scores.size()];
        for(int i=0;i<scores.size();i++){
            StockfishPrecision.Candidate c=scores.get(i);
            int lost=best-c.scoreCp;
            if(lost>limit||(level==100&&lost>0))continue;
            String plan="";
            for(ChessGame.Move m:legal)
                if(m.uci().equals(c.uci)){plan=planOf(root,m,rootCp);break;}
            double continuity=!priorPlan.isEmpty()&&priorPlan.equals(plan)?
                clamp(stability,0,100)/100.0:0;
            double progress=0;
            if(recovering&&rootCp<goal){
                // Encourage progress TOWARD a reachable threshold without
                // claiming the target must be reached in one move.
                double remaining=Math.max(1,goal-rootCp);
                progress=Math.max(0,Math.min(1,(c.scoreCp-rootCp)/remaining));
            }
            double intentBonus=1.0+continuity*0.60+
                progress*0.40*clamp(recoveryIntensity,0,100)/100.0;
            weights[i]=Math.exp(-lost/temp)*intentBonus;
            total+=weights[i];
        }
        if(total<=0)throw new IllegalStateException("Aucun candidat stratégique admissible");
        double choice=random.nextDouble()*total;
        StockfishPrecision.Candidate selected=null;
        for(int i=0;i<scores.size();i++){
            if(weights[i]<=0)continue;
            selected=scores.get(i);
            choice-=weights[i];
            if(choice<=0)break;
        }
        if(selected==null)throw new IllegalStateException("Sélection impossible");
        String selectedPlan="";
        for(ChessGame.Move m:legal)
            if(m.uci().equals(selected.uci)){selectedPlan=planOf(root,m,rootCp);break;}
        boolean preserved=!priorPlan.isEmpty()&&priorPlan.equals(selectedPlan);
        StringBuilder alts=new StringBuilder();
        int n=0;
        for(StockfishPrecision.Candidate c:scores){
            if(n++>=4)break;
            if(alts.length()>0)alts.append(", ");
            alts.append(c.uci).append("=").append(c.scoreCp);
        }
        String reason=(recovering?"Récupération vers "+goal+"cp":"Jeu nominal")+
            "; niveau fixé "+level+"% (non augmenté)"+
            "; "+(preserved?"plan maintenu":"plan réévalué")+
            "; perte "+(best-selected.scoreCp)+"/"+limit+"cp";
        return new Choice(selected,selectedPlan,reason,alts.toString(),
            rootCp,best,limit,level,recovering,preserved,goal);
    }
}
