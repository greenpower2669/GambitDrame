package fr.gambitdrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local chess rules engine. Independent of Android and of the future analysis engine. */
public final class ChessGame {
    public static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private ChessGame() {}

    public static final class Position {
        public final char[] board;
        public final boolean white;
        public final String castle;
        public final int ep, halfmove, fullmove;
        Position(char[] b, boolean w, String c, int e, int h, int f) {
            board=b; white=w; castle=c; ep=e; halfmove=h; fullmove=f;
        }
        public String fen() {
            StringBuilder s=new StringBuilder();
            for(int y=0;y<8;y++) {
                int empty=0;
                for(int x=0;x<8;x++) {
                    char c=board[y*8+x];
                    if(c=='.') empty++;
                    else { if(empty>0){s.append(empty);empty=0;} s.append(c); }
                }
                if(empty>0)s.append(empty);
                if(y<7)s.append('/');
            }
            return s+" "+(white?"w":"b")+" "+(castle.isEmpty()?"-":castle)+" "+(ep<0?"-":squareName(ep))+" "+halfmove+" "+fullmove;
        }
        public String repetitionKey() {
            String[] a=fen().split(" ");
            return a[0]+" "+a[1]+" "+a[2]+" "+a[3];
        }
    }
    public static final class Move {
        public final int from,to;
        public final char promotion;
        Move(int f,int t,char p){from=f;to=t;promotion=p;}
        public String uci(){ return squareName(from)+squareName(to)+(promotion==0?"":String.valueOf(Character.toLowerCase(promotion))); }
        @Override public String toString(){return uci();}
    }
    public static int square(String name) {
        if(name==null||name.length()!=2)return -1;
        char file=name.charAt(0), rank=name.charAt(1);
        if(file<'a'||file>'h'||rank<'1'||rank>'8')return -1;
        return (8-(rank-'0'))*8+file-'a';
    }
    public static String squareName(int sq) {
        return ""+(char)('a'+sq%8)+(char)('8'-sq/8);
    }
    public static Position fromFen(String fen) {
        if(fen==null)throw new IllegalArgumentException("FEN null");
        String[] a=fen.trim().split("\\s+");
        if(a.length<4)throw new IllegalArgumentException("FEN: quatre champs requis");
        char[] b=new char[64]; int i=0,wk=0,bk=0;
        String[] ranks=a[0].split("/");
        if(ranks.length!=8)throw new IllegalArgumentException("FEN: huit rangées requises");
        for(String r:ranks) {
            int n=0;
            for(char c:r.toCharArray()){
                if(c>='1'&&c<='8'){int z=c-'0';for(int j=0;j<z;j++)b[i++]='.';n+=z;}
                else if("KQRBNPkqrbnp".indexOf(c)>=0){b[i++]=c;n++;if(c=='K')wk++;if(c=='k')bk++;}
                else throw new IllegalArgumentException("FEN: pièce inconnue");
                if(n>8)throw new IllegalArgumentException("FEN: rangée trop longue");
            }
            if(n!=8)throw new IllegalArgumentException("FEN: rangée incomplète");
        }
        if(wk!=1||bk!=1||(!a[1].equals("w")&&!a[1].equals("b")))
            throw new IllegalArgumentException("FEN: rois ou trait invalides");
        String c=a[2].equals("-")?"":a[2];
        for(char v:c.toCharArray())if("KQkq".indexOf(v)<0)throw new IllegalArgumentException("FEN: roque invalide");
        int e=a[3].equals("-")?-1:square(a[3]);
        if(e==-1&&!a[3].equals("-"))throw new IllegalArgumentException("FEN: en passant invalide");
        int h=a.length>4?Integer.parseInt(a[4]):0;
        int f=a.length>5?Integer.parseInt(a[5]):1;
        if(h<0||f<1)throw new IllegalArgumentException("FEN: compteurs invalides");
        return new Position(b,a[1].equals("w"),c,e,h,f);
    }
    private static boolean same(char c,boolean w){return c!='.'&&(Character.isUpperCase(c)==w);}
    private static boolean enemy(char c,boolean w){return c!='.'&&Character.isUpperCase(c)!=w&&Character.toUpperCase(c)!='K';}
    private static boolean inside(int y,int x){return y>=0&&y<8&&x>=0&&x<8;}
    private static void add(List<Move> m,int f,int t,char piece) {
        int rank=t/8;
        if(Character.toUpperCase(piece)=='P'&&(rank==0||rank==7)) {
            for(char p:new char[]{'Q','R','B','N'})m.add(new Move(f,t,p));
        } else m.add(new Move(f,t,(char)0));
    }
    private static List<Move> pseudo(Position p) {
        List<Move> out=new ArrayList<>();
        for(int i=0;i<64;i++){
            char piece=p.board[i];
            if(!same(piece,p.white))continue;
            int y=i/8,x=i%8; char k=Character.toUpperCase(piece);
            if(k=='P'){
                int d=p.white?-1:1, ny=y+d, start=p.white?6:1;
                if(inside(ny,x)&&p.board[ny*8+x]=='.'){
                    add(out,i,ny*8+x,piece);
                    if(y==start&&p.board[(y+2*d)*8+x]=='.')add(out,i,(y+2*d)*8+x,piece);
                }
                for(int dx:new int[]{-1,1})if(inside(ny,x+dx)){
                    int t=ny*8+x+dx;
                    if(enemy(p.board[t],p.white)||(t==p.ep&&p.board[t]=='.'&&
                         p.board[y*8+x+dx]==(p.white?'p':'P')))
                        add(out,i,t,piece);
                }
            } else if(k=='N'||k=='K'){
                int[][] dirs=k=='N'?new int[][]{{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}}:
                    new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
                for(int[] d:dirs)if(inside(y+d[0],x+d[1])){
                    int t=(y+d[0])*8+x+d[1];
                    if(p.board[t]=='.'||enemy(p.board[t],p.white))add(out,i,t,piece);
                }
            }
            if(k=='R'||k=='B'||k=='Q'){
                int[][] ds={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
                for(int z=0;z<8;z++){
                    if(k=='R'&&z>=4)continue;
                    if(k=='B'&&z<4)continue;
                    int ny=y+ds[z][0],nx=x+ds[z][1];
                    while(inside(ny,nx)){
                        int t=ny*8+nx;
                        if(p.board[t]=='.')add(out,i,t,piece);
                        else {if(enemy(p.board[t],p.white))add(out,i,t,piece);break;}
                        ny+=ds[z][0];nx+=ds[z][1];
                    }
                }
            }
        }
        // A king may not castle through, out of, or into check.
        if(p.white&&p.board[60]=='K'&&!inCheck(p,true)){
            if(p.castle.contains("K")&&p.board[63]=='R'&&p.board[61]=='.'&&p.board[62]=='.'&&
               !attacked(p,61,false)&&!attacked(p,62,false))out.add(new Move(60,62,(char)0));
            if(p.castle.contains("Q")&&p.board[56]=='R'&&p.board[59]=='.'&&p.board[58]=='.'&&p.board[57]=='.'&&
               !attacked(p,59,false)&&!attacked(p,58,false))out.add(new Move(60,58,(char)0));
        }
        if(!p.white&&p.board[4]=='k'&&!inCheck(p,false)){
            if(p.castle.contains("k")&&p.board[7]=='r'&&p.board[5]=='.'&&p.board[6]=='.'&&
               !attacked(p,5,true)&&!attacked(p,6,true))out.add(new Move(4,6,(char)0));
            if(p.castle.contains("q")&&p.board[0]=='r'&&p.board[3]=='.'&&p.board[2]=='.'&&p.board[1]=='.'&&
               !attacked(p,3,true)&&!attacked(p,2,true))out.add(new Move(4,2,(char)0));
        }
        return out;
    }
    public static boolean attacked(Position p,int sq,boolean byWhite) {
        int y=sq/8,x=sq%8, py=y+(byWhite?1:-1);
        for(int dx:new int[]{-1,1})if(inside(py,x+dx)&&p.board[py*8+x+dx]==(byWhite?'P':'p'))return true;
        int[][] n={{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
        for(int[] d:n)if(inside(y+d[0],x+d[1])&&p.board[(y+d[0])*8+x+d[1]]==(byWhite?'N':'n'))return true;
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for(int z=0;z<8;z++){
            int yy=y+dirs[z][0],xx=x+dirs[z][1],distance=1;
            while(inside(yy,xx)){
                char c=p.board[yy*8+xx];
                if(c!='.'){
                    if(same(c,byWhite)){
                        char k=Character.toUpperCase(c);
                        if(k=='Q'||(z<4&&k=='R')||(z>=4&&k=='B')||(distance==1&&k=='K'))return true;
                    }
                    break;
                }
                yy+=dirs[z][0];xx+=dirs[z][1];distance++;
            }
        }
        return false;
    }
    public static boolean inCheck(Position p,boolean white){
        char king=white?'K':'k';
        for(int i=0;i<64;i++)if(p.board[i]==king)return attacked(p,i,!white);
        throw new IllegalArgumentException("Roi absent");
    }
    public static List<Move> legal(Position p){
        List<Move> out=new ArrayList<>();
        for(Move m:pseudo(p))if(!inCheck(apply(p,m),p.white))out.add(m);
        return out;
    }
    public static Position apply(Position p,Move m){
        char[] b=p.board.clone(); char piece=b[m.from], target=b[m.to];
        boolean pawn=Character.toUpperCase(piece)=='P';
        b[m.from]='.';
        if(pawn&&m.to==p.ep&&target=='.'&&m.from%8!=m.to%8)
            b[m.to+(p.white?8:-8)]='.';
        if(m.promotion!=0)piece=p.white?Character.toUpperCase(m.promotion):Character.toLowerCase(m.promotion);
        b[m.to]=piece;
        if(Character.toUpperCase(piece)=='K'&&Math.abs(m.to-m.from)==2){
            if(m.to==62){b[61]='R';b[63]='.';}
            if(m.to==58){b[59]='R';b[56]='.';}
            if(m.to==6){b[5]='r';b[7]='.';}
            if(m.to==2){b[3]='r';b[0]='.';}
        }
        String c=p.castle;
        if(m.from==60||m.to==60)c=c.replace("K","").replace("Q","");
        if(m.from==4||m.to==4)c=c.replace("k","").replace("q","");
        if(m.from==63||m.to==63)c=c.replace("K","");
        if(m.from==56||m.to==56)c=c.replace("Q","");
        if(m.from==7||m.to==7)c=c.replace("k","");
        if(m.from==0||m.to==0)c=c.replace("q","");
        int ep=pawn&&Math.abs(m.to-m.from)==16?(m.to+m.from)/2:-1;
        int h=pawn||target!='.'||m.to==p.ep&&pawn?0:p.halfmove+1;
        return new Position(b,!p.white,c,ep,h,p.fullmove+(p.white?0:1));
    }
    public static Position play(Position p,String uci){
        for(Move m:legal(p))if(m.uci().equalsIgnoreCase(uci))return apply(p,m);
        throw new IllegalArgumentException("Coup illégal: "+uci);
    }
    public static boolean insufficient(Position p) {
        int minors=0,firstBishopColor=-1;
        for(int i=0;i<64;i++){
            char c=Character.toUpperCase(p.board[i]);
            if(c=='P'||c=='R'||c=='Q')return false;
            if(c=='B'||c=='N') {
                minors++;
                if(c=='N'&&minors>1)return false;
                if(c=='B'){
                    int color=(i/8+i%8)%2;
                    if(firstBishopColor>=0&&firstBishopColor!=color)return false;
                    firstBishopColor=color;
                }
            }
        }
        return minors<=1 || (firstBishopColor>=0 && !containsKnight(p));
    }
    private static boolean containsKnight(Position p){
        for(char c:p.board)if(c=='n'||c=='N')return true;
        return false;
    }
    public static String outcome(Position p,int repetitions) {
        List<Move> lm=legal(p);
        if(lm.isEmpty())return inCheck(p,p.white)?"Échec et mat — "+(p.white?"Noirs":"Blancs")+" gagnent":"Pat — partie nulle";
        if(p.halfmove>=100)return "Nulle — règle des 50 coups (réclamation simplifiée)";
        if(repetitions>=3)return "Nulle — répétition triple (réclamation simplifiée)";
        if(insufficient(p))return "Nulle — matériel insuffisant";
        return inCheck(p,p.white)?"Échec aux "+(p.white?"Blancs":"Noirs"):"";
    }
}
