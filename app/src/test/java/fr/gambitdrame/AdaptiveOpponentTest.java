package fr.gambitdrame;
import java.util.*;
public final class AdaptiveOpponentTest {
    static int n;
    static void check(boolean ok,String msg){n++;if(!ok)throw new AssertionError(msg);}
    public static void main(String[] args){
        ChessGame.Position p=ChessGame.fromFen(ChessGame.START);
        List<String> names=Arrays.asList("e2e4","d2d4","a2a3","h2h4");
        List<StockfishPrecision.Candidate> ranked=StockfishPrecision.normalize(names,Arrays.asList(30,20,-80,-900));
        AdaptiveOpponent.State state=new AdaptiveOpponent.State();
        check(!AdaptiveOpponent.updateRecovery(state,-100,-100,70),"at goal recovery inactive");
        check(!AdaptiveOpponent.updateRecovery(state,-200,-100,70),"small deficit no hysteresis entry");
        check(AdaptiveOpponent.updateRecovery(state,-300,-100,70),"large deficit activates");
        check(AdaptiveOpponent.updateRecovery(state,-130,-100,70),"recovery persists below goal");
        check(!AdaptiveOpponent.updateRecovery(state,-100,-100,70),"arriving at gauge threshold ends recovery");
        check(AdaptiveOpponent.updateRecovery(state,-300,-100,70),"can re-enter after fall");
        check(!AdaptiveOpponent.updateRecovery(state,-300,-400,70),"changing goal resets threshold state");
        check(!AdaptiveOpponent.updateRecovery(state,0,-100,70),"advantage has no recovery");
        check(AdaptiveOpponent.lossBudget(30,false,95)>AdaptiveOpponent.lossBudget(90,false,95),"30 allows more ordinary errors");
        check(AdaptiveOpponent.selectionTemperature(30,10,false,95)>
              AdaptiveOpponent.selectionTemperature(90,10,false,95),"30 has broader selection");
        int better=0,lowLoss=0,highLoss=0,lowBest=0,highBest=0;
        for(int j=0;j<400;j++){
            AdaptiveOpponent.Choice low=AdaptiveOpponent.choose(p,ranked,30,10,70,95,70,
                new AdaptiveOpponent.State(),new Random(j*13L+5),0,-100);
            AdaptiveOpponent.Choice high=AdaptiveOpponent.choose(p,ranked,90,10,70,95,70,
                new AdaptiveOpponent.State(),new Random(j*13L+5),0,-100);
            check(low.effectiveTarget==30&&high.effectiveTarget==90,"nominal fixed");
            check(low.lossCp<=low.limitCp&&high.lossCp<=high.limitCp,"per-move safety budget");
            lowLoss+=low.lossCp;highLoss+=high.lossCp;
            if(low.lossCp==0)lowBest++;
            if(high.lossCp==0)highBest++;
        }
        check(lowLoss>highLoss*2,"low skill measurably less accurate on repeated choices");
        check(lowBest<highBest,"low skill selects the best move less often");
        for(int j=0;j<100;j++){
            AdaptiveOpponent.Choice bad=AdaptiveOpponent.choose(p,ranked,30,10,70,95,70,
                new AdaptiveOpponent.State(),new Random(j),-350,-100);
            check(bad.recovery,"recovery enabled");
            check(bad.effectiveTarget==30,"recovery never replaces 30 with 97");
            check(bad.lossCp<=bad.limitCp,"recovery preserves cap");
        }
        AdaptiveOpponent.Choice reached=AdaptiveOpponent.choose(p,ranked,30,10,70,95,70,
            new AdaptiveOpponent.State(),new Random(19),-80,-100);
        check(!reached.recovery,"at or above bar return to nominal style");
        System.out.println("ADAPTIVE_OPPONENT_TESTS_OK "+n);
    }
}
