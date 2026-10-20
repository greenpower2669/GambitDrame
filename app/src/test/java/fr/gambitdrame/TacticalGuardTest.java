package fr.gambitdrame;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
public final class TacticalGuardTest {
    private static int n=0;
    static void ok(boolean b,String text){n++;if(!b)throw new AssertionError(text);}
    public static void main(String[] args){
        ok(TacticalGuard.allowableLossCp(80)==60,"80 percent loss cap 60 cp");
        ok(TacticalGuard.allowableLossCp(0)==1020,"0 percent allows serious blunders");
        ok(TacticalGuard.allowableLossCp(100)==20,"100 percent very strict");
        // Real reported move: best -98cp, 6...Qf5 -343cp, loss 245cp;
        // old normalization used -648 as 0%, + precision 75% picked Qf5.
        List<StockfishPrecision.Candidate> pos=StockfishPrecision.normalize(
           Arrays.asList("f6f5","f6e7","f6d8","e8e7"),
           Arrays.asList(-343,-98,-121,-648));
        TacticalGuard.Decision d=TacticalGuard.choose(pos,80,0,new Random(1));
        ok(!d.chosen.move.uci.equals("f6f5"),"refuse exposed queen");
        ok(d.lossCp<=60,"chosen move within safe cp loss");
        ok(d.bestCp==-98,"best score as black");
        List<StockfishPrecision.Candidate> equal=StockfishPrecision.normalize(
            Arrays.asList("a","b"),Arrays.asList(30,30));
        TacticalGuard.Decision tied=TacticalGuard.choose(equal,80,0,new Random(2));
        ok(tied.lossCp==0,"ties cannot induce large error");
        List<StockfishPrecision.Candidate> extremes=StockfishPrecision.normalize(
            Arrays.asList("safe","terrible"),Arrays.asList(10,-950));
        TacticalGuard.Decision low=TacticalGuard.choose(extremes,0,0,new Random(1));
        ok(low.chosen.move.uci.equals("terrible"),"very low target permits blunders");
        System.out.println("TACTICAL_GUARD_TESTS_OK "+n);
    }
}
