package fr.gambitdrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Regression for 3 direct gauge controls, rank-based level and positional tensors. */
public final class GaugeTensorPolicyTest {
    static int checks;
    private static void check(boolean ok,String reason){
        checks++;if(!ok)throw new AssertionError(reason);
    }
    static List<StockfishPrecision.Candidate> ranking(ChessGame.Position p){
        List<String> moves=new ArrayList<>();
        List<Integer> scores=new ArrayList<>();
        int i=0;
        for(ChessGame.Move m:ChessGame.legal(p)){
            moves.add(m.uci());scores.add(100-i++*10);
        }
        return StockfishPrecision.normalize(moves,scores);
    }
    public static void main(String[] args){
        GaugeTensorPolicy.Tension neutral=GaugeTensorPolicy.tension(0,50,250,-250);
        check(neutral.targetRank==50,"middle grey sets middle of ranked moves");
        check(neutral.red==0&&neutral.green==0,"no tension near even score");
        GaugeTensorPolicy.Tension red=GaugeTensorPolicy.tension(250,50,250,-250);
        check(red.targetRank==0&&red.red==1,"red dominance pulls rank to weakest");
        GaugeTensorPolicy.Tension green=GaugeTensorPolicy.tension(-250,50,250,-250);
        check(green.targetRank==100&&green.green==1,"green submission pulls rank best");
        GaugeTensorPolicy.Tension halfRed=GaugeTensorPolicy.tension(125,50,250,-250);
        check(halfRed.targetRank>0&&halfRed.targetRank<50,"red tensor is continuous");
        GaugeTensorPolicy.Tension halfGreen=GaugeTensorPolicy.tension(-125,50,250,-250);
        check(halfGreen.targetRank>50&&halfGreen.targetRank<100,"green tensor is continuous");
        check(GaugeTensorPolicy.tension(0,30,100,-100).targetRank==30,
            "30 remains nominal in equilibrium");
        check(GaugeTensorPolicy.tension(0,90,100,-100).targetRank==90,
            "90 remains nominal in equilibrium");
        ChessGame.Position position=ChessGame.fromFen(ChessGame.START);
        List<StockfishPrecision.Candidate> moves=ranking(position);
        Map<String,String> variations=new HashMap<>();
        for(StockfishPrecision.Candidate candidate:moves)
            variations.put(candidate.uci,candidate.uci+" e7e5 "+candidate.uci);
        GaugeTensorPolicy.Decision middle=GaugeTensorPolicy.choose(position,moves,50,0,0,
            250,-250,new AdaptiveOpponent.State(),new Random(6),70,variations);
        check(Math.abs(middle.chosenRank-50)<4,"50 selects middle of scored legal move list");
        check(middle.nominal==50&&middle.redTension==0&&middle.greenTension==0,
            "neutral logs cannot silently replace nominal");
        GaugeTensorPolicy.Decision strong=GaugeTensorPolicy.choose(position,moves,30,0,-250,
            250,-250,new AdaptiveOpponent.State(),new Random(6),70,variations);
        check(strong.chosenRank==100,"submission threshold chooses best");
        GaugeTensorPolicy.Decision relaxed=GaugeTensorPolicy.choose(position,moves,30,0,250,
            250,-250,new AdaptiveOpponent.State(),new Random(6),70,variations);
        check(relaxed.chosenRank==0,"domination threshold chooses weakest admitted");
        check(relaxed.move.scoreCp<=middle.move.scoreCp,"domination reduces strength");
        // Dangerous isolated queen gift must be excluded despite low desired rank.
        List<String> options=new ArrayList<>();
        List<Integer> s=new ArrayList<>();
        int i=0;
        for(ChessGame.Move m:ChessGame.legal(position)){
            options.add(m.uci());s.add(i++==1?-3000:50-i);
        }
        GaugeTensorPolicy.Decision safe=GaugeTensorPolicy.choose(position,
            StockfishPrecision.normalize(options,s),40,0,250,250,-250,
            new AdaptiveOpponent.State(),new Random(0),70,null);
        check(safe.lossCp<=safe.capCp&&safe.capCp<=650,"catastrophic gift filtered");
        AdaptiveOpponent.State state=new AdaptiveOpponent.State();
        state.plan="structure de pions";
        GaugeTensorPolicy.Decision continued=GaugeTensorPolicy.choose(position,moves,50,0,0,
            250,-250,state,new Random(6),100,variations);
        check(continued.previousPlan.equals("structure de pions"),
            "previous strategy taken into account");
        check(continued.reason.contains("plan"),"detailed plan decisions are explainable");
        check(continued.reason.contains("rouge")&&continued.reason.contains("vert")&&
            continued.reason.contains("rang"),"GET ERR explains both tensors and ranks");
        System.out.println("GAUGE_TENSOR_TESTS_OK "+checks);
    }
}
