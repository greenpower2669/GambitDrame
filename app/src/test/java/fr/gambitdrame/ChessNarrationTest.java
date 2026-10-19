package fr.gambitdrame;

/** Regression tests for clear French chess speech, captures and promotions. */
public final class ChessNarrationTest {
    private static int checks=0;
    private static void expect(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
    private static String play(String... moves){
        ChessGame.Position p=ChessGame.fromFen(ChessGame.START);
        String last="";
        for(String u:moves){
            ChessGame.Move chosen=null;
            for(ChessGame.Move m:ChessGame.legal(p))if(m.uci().equals(u)){chosen=m;break;}
            if(chosen==null)throw new AssertionError("Illegal setup "+u);
            ChessGame.Position after=ChessGame.apply(p,chosen);
            last=ChessNarration.move(p,chosen,after);
            p=after;
        }
        return last;
    }
    public static void main(String[] args){
        ChessGame.Position start=ChessGame.fromFen(ChessGame.START);
        expect(ChessNarration.square(52).equals("e 2"),"coordinate e2");
        expect(ChessNarration.square(0).equals("a 8"),"coordinate a8");
        expect(ChessNarration.squareContents(start,52).contains("pion"),"pawn spoken");
        expect(ChessNarration.squareContents(start,36).contains("libre"),"empty spoken");
        expect(play("e2e4").contains("pion de e 2 vers e 4"),"move spoken");
        expect(play("e2e4","d7d5","e4d5").contains("prend pion"),"capture spoken");
        expect(play("f2f3","e7e5","g2g4","d8h4").contains("Échec et mat"),"mate spoken");
        expect(play("e2e4","e7e5","g1f3").contains("cavalier"),"knight spoken");
        expect(ChessNarration.evaluation(0,null).contains("équilibrée"),"equal");
        expect(ChessNarration.evaluation(170,null).contains("blancs"),"white");
        expect(ChessNarration.evaluation(-170,null).contains("noirs"),"black");
        expect(ChessNarration.evaluation(null,2).contains("blancs"),"white mate");
        expect(ChessNarration.evaluation(null,-4).contains("noirs"),"black mate");
        ChessGame.Position castle=ChessGame.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        ChessGame.Move move=null;
        for(ChessGame.Move m:ChessGame.legal(castle))if(m.uci().equals("e1g1"))move=m;
        expect(move!=null&&ChessNarration.move(castle,move,ChessGame.apply(castle,move)).contains("petit roque"),"castle");
        ChessGame.Position promo=ChessGame.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        ChessGame.Move promotion=null;
        for(ChessGame.Move m:ChessGame.legal(promo))if(m.uci().equals("a7a8q"))promotion=m;
        expect(promotion!=null&&ChessNarration.move(promo,promotion,ChessGame.apply(promo,promotion)).contains("promotion en dame"),"promotion");
        System.out.println("CHESS_NARRATION_TESTS_OK "+checks);
    }
}
