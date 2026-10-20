package fr.gambitdrame;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public final class BoardArrowsTest {
    private static int count=0;
    private static void check(boolean good,String label){
        if(!good)throw new AssertionError(label);
        count++;
    }
    public static void main(String[] args){
        BoardArrows a=new BoardArrows();
        int color=BoardArrows.colorForIdentity(9);
        a.toggle(52,36,color);
        check(a.all().size()==1&&a.all().get(0).color==color,"arrow captured hue");
        a.toggle(52,36,color);
        check(a.all().isEmpty(),"toggle remove");
        for(int i=0;i<64;i++)a.toggle(i,(i+1)%64);
        check(a.all().size()==12,"bounded arrows");
        a.clear();
        a.toggle(0,0);
        check(a.all().isEmpty(),"invalid arrow");
        HashSet<Integer> colors=new HashSet<>();
        for(int i=0;i<32;i++){
            int c=BoardArrows.colorForIdentity(i);
            check((c>>>24)==200,"semitransparent");
            colors.add(c);
        }
        check(colors.size()==32,"32 unique piece colors");
        ChessGame.Position start=ChessGame.fromFen(ChessGame.START);
        int e2=ChessGame.square("e2"),e4=ChessGame.square("e4"),f2=ChessGame.square("f2");
        int pawn=BoardArrows.identityAt(start,Collections.emptyList(),e2);
        check(pawn>=0&&pawn!=BoardArrows.identityAt(start,Collections.emptyList(),f2),
              "two pawns different");
        check(pawn==BoardArrows.identityAt(start,Arrays.asList("e2e4"),e4),
              "identity follows a moved pawn");
        check(pawn==BoardArrows.identityAt(start,Arrays.asList("e2e4","e7e5"),e4),
              "identity stays through other player's move");
        ChessGame.Position castles=ChessGame.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        int rook=BoardArrows.identityAt(castles,Collections.emptyList(),ChessGame.square("h1"));
        int king=BoardArrows.identityAt(castles,Collections.emptyList(),ChessGame.square("e1"));
        check(rook==BoardArrows.identityAt(castles,Arrays.asList("e1g1"),ChessGame.square("f1")),"rook castling");
        check(king==BoardArrows.identityAt(castles,Arrays.asList("e1g1"),ChessGame.square("g1")),"king castling");
        ChessGame.Position ep=ChessGame.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        int epPawn=BoardArrows.identityAt(ep,Collections.emptyList(),ChessGame.square("e5"));
        check(epPawn==BoardArrows.identityAt(ep,Arrays.asList("e5d6"),ChessGame.square("d6")),"en passant pawn");
        check(BoardArrows.identityAt(ep,Arrays.asList("e5d6"),ChessGame.square("d5"))==-1,"en passant capture removed");
        ChessGame.Position promote=ChessGame.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        check(BoardArrows.identityAt(promote,Collections.emptyList(),ChessGame.square("a7"))==
              BoardArrows.identityAt(promote,Arrays.asList("a7a8q"),ChessGame.square("a8")),
              "promotion retains pawn identity");
        a.toggle(e2,e4,BoardArrows.colorForIdentity(pawn));
        check(a.all().get(0).color==BoardArrows.colorForIdentity(pawn),
              "arrow retains hue after piece moves");
        System.out.println("BOARD_ARROWS_TESTS_OK "+count);
    }
}
