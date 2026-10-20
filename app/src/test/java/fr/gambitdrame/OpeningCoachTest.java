package fr.gambitdrame;
import java.util.*;
public final class OpeningCoachTest {
    static int n;
    static void ok(boolean good,String why){n++;if(!good)throw new AssertionError(why);}
    public static void main(String[] args){
        ChessGame.Position start=ChessGame.fromFen(ChessGame.START);
        List<OpeningTheory.Next> first=OpeningTheory.next(Collections.emptyList(),start);
        ok(!first.isEmpty(),"opening repertoire begins");
        ChessGame.Position p=ChessGame.play(start,"d2d4");
        p=ChessGame.play(p,"d7d5");p=ChessGame.play(p,"c2c4");
        List<OpeningTheory.Next> book=OpeningTheory.next(Arrays.asList("d2d4","d7d5","c2c4"),p);
        ok(book.size()>=2,"queen gambit accepted and declined");
        boolean accepted=false,declined=false;
        for(OpeningTheory.Next n:book){
            if(n.opening.contains("accepté"))accepted=true;
            if(n.opening.contains("refusé"))declined=true;
            ok(!n.idea.isEmpty(),"theoretical idea present");
        }
        ok(accepted&&declined,"distinguish queen gambit branches");
        ChessGame.Move m=ChessGame.legal(start).get(0);
        ok(OpeningCoach.explain(start,m,false).contains("vers"),"coordinate hint");
        ok(OpeningTheory.next(Arrays.asList("g1f3","a7a6"),p).isEmpty(),"out of book not fabricated");
        System.out.println("OPENING_COACH_TESTS_OK "+n);
    }
}
