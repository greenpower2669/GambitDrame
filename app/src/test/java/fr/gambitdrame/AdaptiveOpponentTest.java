package fr.gambitdrame;
import java.util.*;
public final class AdaptiveOpponentTest{
    static int n;
    static void check(boolean ok,String reason){n++;if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args){
        ChessGame.Position p=ChessGame.fromFen(ChessGame.START);
        List<String> names=Arrays.asList("e2e4","d2d4","a2a3","h2h4");
        List<StockfishPrecision.Candidate> cs=StockfishPrecision.normalize(names,Arrays.asList(30,20,5,-900));
        AdaptiveOpponent.State state=new AdaptiveOpponent.State();
        check(AdaptiveOpponent.effectiveTarget(80,-800,80,100)==100,"full recovery plays strongly");
        check(AdaptiveOpponent.effectiveTarget(80,0,80,100)==80,"balanced nominal preserved");
        check(AdaptiveOpponent.lossBudget(80,-800,80,100)<AdaptiveOpponent.lossBudget(80,100,80,100),"defense safety tightens");
        for(int j=0;j<200;j++){
            AdaptiveOpponent.Choice c=AdaptiveOpponent.choose(p,cs,80,5,75,95,70,state,new Random(j),0);
            check(!c.move.uci.equals("h2h4"),"catastrophic move forbidden at 80");
            check(c.lossCp<=c.limitCp,"bounded loss");
        }
        for(int j=0;j<50;j++){
            AdaptiveOpponent.Choice c=AdaptiveOpponent.choose(p,cs,80,5,80,100,70,state,new Random(j),-800);
            check(c.move.scoreCp>=20,"severe disadvantage asks for strongest defense");
            check(c.recovery,"recovery flag");
        }
        check(AdaptiveOpponent.planOf(p,ChessGame.legal(p).get(0),0)!=null,"strategy label");
        System.out.println("ADAPTIVE_OPPONENT_TESTS_OK "+n);
    }
}
