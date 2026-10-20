package fr.gambitdrame;

import java.util.List;

/** Concise conditional hints, not fabricated theory or a remote AI answer. */
public final class OpeningCoach {
    private OpeningCoach(){}
    public static String explain(ChessGame.Position p,ChessGame.Move m,boolean detail){
        char piece=p.board[m.from];
        ChessGame.Position next=ChessGame.apply(p,m);
        String s=ChessNarration.piece(piece)+" "+ChessNarration.square(m.from)+
            " vers "+ChessNarration.square(m.to)+". ";
        if(ChessGame.inCheck(next,next.white))
            s+="Met le roi adverse en échec. ";
        else if(p.board[m.to]!='.')
            s+="Capture une pièce ou un pion adverse. ";
        else if(Character.toUpperCase(piece)=='K'&&Math.abs(m.to-m.from)==2)
            s+="Met le roi à l'abri par le roque. ";
        else if(Character.toUpperCase(piece)=='P')
            s+="Modifie la structure de pions et peut agir sur le centre. ";
        else if(Character.toUpperCase(piece)=='N'||Character.toUpperCase(piece)=='B')
            s+="Active une pièce mineure. ";
        else if(Character.toUpperCase(piece)=='Q')
            s+="Réorganise la dame, à vérifier face aux menaces. ";
        else s+="Cherche à améliorer l'activité des pièces. ";
        if(detail)
            s+="La pertinence exacte dépend des réponses et menaces de l'adversaire. "+
                "L'analyse Stockfish est une évaluation, et non une garantie de gain. ";
        return s;
    }
}
