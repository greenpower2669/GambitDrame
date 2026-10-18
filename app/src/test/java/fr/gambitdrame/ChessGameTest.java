package fr.gambitdrame;
import java.util.List;
public final class ChessGameTest {
    private static int checks=0;
    private static void yes(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static boolean has(ChessGame.Position p,String move){
        for(ChessGame.Move m:ChessGame.legal(p))if(m.uci().equals(move))return true;
        return false;
    }
    private static ChessGame.Position sequence(String...moves){
        ChessGame.Position p=ChessGame.fromFen(ChessGame.START);
        for(String m:moves)p=ChessGame.play(p,m);
        return p;
    }
    private static long perft(ChessGame.Position p,int depth){
        if(depth==0)return 1L;
        long total=0;
        for(ChessGame.Move m:ChessGame.legal(p))
            total+=perft(ChessGame.apply(p,m),depth-1);
        return total;
    }
    public static void main(String[]args){
        ChessGame.Position start=ChessGame.fromFen(ChessGame.START);
        yes(start.fen().equals(ChessGame.START),"FEN initial");
        yes(ChessGame.legal(start).size()==20,"20 coups initiaux");
        yes(perft(start,2)==400L,"perft départ profondeur 2");
        yes(perft(start,3)==8902L,"perft départ profondeur 3");
        yes(!has(start,"e2e5"),"saut de pion illégal");
        yes(ChessGame.legal(sequence("e2e4","e7e5")).size()==29,"coups après e4 e5");
        ChessGame.Position mat=sequence("f2f3","e7e5","g2g4","d8h4");
        yes(ChessGame.outcome(mat,1).startsWith("Échec et mat"),"mat du berger inversé");
        ChessGame.Position castle=ChessGame.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        yes(has(castle,"e1g1")&&has(castle,"e1c1"),"roques");
        yes(ChessGame.play(castle,"e1g1").board[61]=='R',"déplacement tour du roque");
        ChessGame.Position exposed=ChessGame.fromFen("r3kr1r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        yes(!has(exposed,"e1g1"),"roque à travers échec refusé");
        ChessGame.Position ep=sequence("e2e4","a7a6","e4e5","d7d5");
        yes(has(ep,"e5d6"),"prise en passant autorisée");
        yes(ChessGame.play(ep,"e5d6").board[27]=='.',"pion pris en passant retiré");
        ChessGame.Position promotion=ChessGame.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        yes(has(promotion,"a7a8q")&&has(promotion,"a7a8n"),"quatre promotions");
        yes(ChessGame.play(promotion,"a7a8n").board[0]=='N',"promotion cavalier");
        ChessGame.Position pat=ChessGame.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        yes(ChessGame.outcome(pat,1).startsWith("Pat"),"pat");
        ChessGame.Position pinned=ChessGame.fromFen("4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1");
        yes(!has(pinned,"e2d2"),"pièce clouée");
        ChessGame.Position d=ChessGame.fromFen("7k/8/8/8/8/8/8/K7 w - - 0 1");
        yes(ChessGame.insufficient(d),"matériel insuffisant");
        ChessGame.Position r=sequence("g1f3","g8f6","f3g1","f6g8");
        yes(r.fen().startsWith(start.fen().substring(0,start.fen().indexOf(" 0 "))),"retour position initiale");
        System.out.println("CHESS_TESTS_OK "+checks);
    }
}
