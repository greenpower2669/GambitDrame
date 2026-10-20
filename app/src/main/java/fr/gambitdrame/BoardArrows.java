package fr.gambitdrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Translucent arrows belonging to individual pieces, not to white/black sides
 * or merely to the square occupied at drawing time.
 */
public final class BoardArrows {
    public static final class Arrow {
        public final int from, to, color;
        public Arrow(int from,int to,int color){this.from=from;this.to=to;this.color=color;}
    }
    private final List<Arrow> arrows=new ArrayList<>();
    public List<Arrow> all(){return new ArrayList<>(arrows);}
    public void clear(){arrows.clear();}

    /** Same segment toggles off; all surviving arrows keep their captured color. */
    public void toggle(int from,int to,int color){
        if(from<0||from>=64||to<0||to>=64||from==to)return;
        for(int i=0;i<arrows.size();i++)
            if(arrows.get(i).from==from&&arrows.get(i).to==to){arrows.remove(i);return;}
        if(arrows.size()>=12)arrows.remove(0);
        arrows.add(new Arrow(from,to,color));
    }
    /** Compatibility with existing callers; an origin-square hue for empty squares. */
    public void toggle(int from,int to){
        toggle(from,to,colorForIdentity(32+from));
    }

    /**
     * Piece index from the starting FEN, maintained through the actual UCI history.
     * Captures remove identities, rooks follow castling, pawn identity survives
     * promotion and en-passant is handled. Replay/trial can supply a prefix of moves.
     */
    public static int identityAt(ChessGame.Position initial,List<String> moves,int square) {
        if(initial==null||square<0||square>=64)return -1;
        int[] ids=new int[64];
        Arrays.fill(ids,-1);
        int next=0;
        for(int i=0;i<64;i++)if(initial.board[i]!='.')ids[i]=next++;
        if(moves==null)return ids[square];
        ChessGame.Position p=initial;
        for(String u:moves){
            if(u==null||u.length()<4)break;
            int from=ChessGame.square(u.substring(0,2)),to=ChessGame.square(u.substring(2,4));
            if(from<0||to<0||p.board[from]=='.')break;
            char piece=p.board[from];
            // Pawn moving diagonally into an empty square: en passant.
            if(Character.toUpperCase(piece)=='P' && to==p.ep &&
                from%8!=to%8 && p.board[to]=='.'){
                ids[to+(p.white?8:-8)]=-1;
            }
            ids[to]=ids[from];
            ids[from]=-1;
            if(Character.toUpperCase(piece)=='K' && Math.abs(to-from)==2){
                int rookFrom=-1,rookTo=-1;
                if(to==62){rookFrom=63;rookTo=61;}
                if(to==58){rookFrom=56;rookTo=59;}
                if(to==6){rookFrom=7;rookTo=5;}
                if(to==2){rookFrom=0;rookTo=3;}
                if(rookFrom>=0){ids[rookTo]=ids[rookFrom];ids[rookFrom]=-1;}
            }
            try{p=ChessGame.play(p,u);}
            catch(IllegalArgumentException ex){break;} // Invalid input, fail closed.
        }
        return ids[square];
    }

    /**
     * Thirty-two vivid, independent hues. Golden-angle spacing plus
     * saturation/value modulation makes same-type pieces distinguishable.
     * The alpha channel retains the underlying board and pieces.
     */
    public static int colorForIdentity(int identity){
        int index=Math.max(0,identity);
        float hue=(float)((index*137.50776405)%360.0);
        float saturation=0.76f+0.08f*(index%3);
        float value=0.83f+0.065f*(index%3);
        float h=hue/60f;
        float chroma=value*saturation;
        float x=chroma*(1-Math.abs(h%2-1));
        float m=value-chroma;
        float r=0,g=0,b=0;
        if(h<1){r=chroma;g=x;}
        else if(h<2){r=x;g=chroma;}
        else if(h<3){g=chroma;b=x;}
        else if(h<4){g=x;b=chroma;}
        else if(h<5){r=x;b=chroma;}
        else {r=chroma;b=x;}
        int rr=Math.min(255,Math.round((r+m)*255));
        int gg=Math.min(255,Math.round((g+m)*255));
        int bb=Math.min(255,Math.round((b+m)*255));
        return 0xC8000000|(rr<<16)|(gg<<8)|bb;
    }
}
