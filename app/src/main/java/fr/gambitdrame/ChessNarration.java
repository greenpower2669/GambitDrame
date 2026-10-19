package fr.gambitdrame;

/** Short, unambiguous French speech for low-vision chess. No Android dependency. */
public final class ChessNarration {
    private ChessNarration(){}
    private static final String[] FILES={"a","bé","cé","dé","e","effe","gé","ache"};
    public static String square(int square){
        if(square<0||square>=64)return "case inconnue";
        return FILES[square%8]+" "+(8-square/8);
    }
    public static String piece(char p){
        switch(Character.toUpperCase(p)){
            case 'K':return "roi";
            case 'Q':return "dame";
            case 'R':return "tour";
            case 'B':return "fou";
            case 'N':return "cavalier";
            case 'P':return "pion";
            default:return "pièce";
        }
    }
    public static String squareContents(ChessGame.Position p,int sq){
        if(sq<0||sq>=64)return "Hors de l'échiquier";
        char c=p.board[sq];
        if(c=='.')return "Case "+square(sq)+", libre";
        return (Character.isUpperCase(c)?"Blancs, ":"Noirs, ")+piece(c)+" en "+square(sq);
    }
    public static String move(ChessGame.Position from,ChessGame.Move m,ChessGame.Position to){
        char p=from.board[m.from];
        char captured=from.board[m.to];
        boolean castle=Character.toUpperCase(p)=='K'&&Math.abs(m.to-m.from)==2;
        boolean enPassant=Character.toUpperCase(p)=='P'&&
            m.to==from.ep&&captured=='.'&&m.from%8!=m.to%8;
        String side=from.white?"Blancs":"Noirs";
        String result;
        if(castle){
            result=side+": "+(m.to%8==6?"petit roque":"grand roque");
        }else{
            result=side+": "+piece(p)+" de "+square(m.from)+" vers "+square(m.to);
            if(captured!='.')result+=", prend "+piece(captured);
            if(enPassant)result+=", prise en passant";
            if(m.promotion!=0)result+=", promotion en "+piece(m.promotion);
        }
        // Announce mate/pat with higher priority than check.
        if(ChessGame.legal(to).isEmpty()){
            if(ChessGame.inCheck(to,to.white))result+=". Échec et mat";
            else result+=". Pat, partie nulle";
        }else if(ChessGame.inCheck(to,to.white))result+=". Échec";
        else if(ChessGame.insufficient(to))result+=". Nulle, matériel insuffisant";
        return result;
    }
    public static String evaluation(Integer whiteCp,Integer whiteMate){
        if(whiteMate!=null){
            if(whiteMate==0)return "Mat annoncé.";
            return whiteMate>0?"Mat pour les blancs dans "+whiteMate+" coups":
                "Mat pour les noirs dans "+Math.abs(whiteMate)+" coups";
        }
        if(whiteCp==null)return "Stockfish n'a pas encore donné d'évaluation.";
        int cp=Math.abs(whiteCp);
        if(cp<=30)return "Position à peu près équilibrée.";
        String color=whiteCp>0?"blancs":"noirs";
        String weight=cp<110?"un léger avantage":cp<300?"un avantage net":"un très grand avantage";
        return "Les "+color+" ont "+weight+", environ "+
            String.format(java.util.Locale.FRANCE,"%.1f",cp/100.0)+" pion.";
    }
}
