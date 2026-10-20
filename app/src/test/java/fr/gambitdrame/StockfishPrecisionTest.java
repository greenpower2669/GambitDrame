package fr.gambitdrame;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
public final class StockfishPrecisionTest {
    private static int n;
    private static void check(boolean value,String message){n++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        List<StockfishPrecision.Candidate> moves=StockfishPrecision.normalize(
            Arrays.asList("e2e4","d2d4","g1f3","a2a3","h2h4"),
            Arrays.asList(120,80,20,-40,-120));
        check(moves.get(0).relativeQuality==100.0,"best 100");
        check(moves.get(4).relativeQuality==0.0,"worst 0");
        check(moves.get(2).relativeQuality>50,"ranked intermediate");
        List<StockfishPrecision.Candidate> equal=StockfishPrecision.normalize(
            Arrays.asList("e2e4","d2d4"),Arrays.asList(10,10));
        check(equal.get(0).relativeQuality==100.0&&equal.get(1).relativeQuality==100,"all equivalent = all optimal");
        StockfishPrecision.Pick p=StockfishPrecision.choose(moves,75,0,new Random(7));
        check(p.requested==75,"zero spread target");
        check(p.move.uci.equals("d2d4"),"closest");
        Random rand=new Random(1234);
        int near=0,veryFar=0;
        for(int i=0;i<10000;i++){
            double wanted=StockfishPrecision.choose(moves,75,10,rand).requested;
            if(Math.abs(wanted-75)<=10)near++;
            if(Math.abs(wanted-75)>30)veryFar++;
        }
        check(near>5800,"Gaussian bulk nearby");
        check(veryFar<100,"outliers rare");
        check(StockfishPrecision.choose(moves,-12,0,new Random(3)).requested==0,"clamp below 0");
        check(StockfishPrecision.choose(moves,120,0,new Random(3)).requested==100,"clamp above 100");
        System.out.println("STOCKFISH_PRECISION_TESTS_OK "+n);
    }
}
