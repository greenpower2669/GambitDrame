package fr.gambitdrame;
import java.util.ArrayList;
import java.util.List;
/** Immutable vector-arrow segments, independent of the chess history. */
public final class BoardArrows {
    public static final class Arrow {
        public final int from,to;
        public Arrow(int from,int to){this.from=from;this.to=to;}
    }
    private final List<Arrow> arrows=new ArrayList<>();
    public List<Arrow> all(){return new ArrayList<>(arrows);}
    public void clear(){arrows.clear();}
    public void toggle(int from,int to){
        if(from<0||from>=64||to<0||to>=64||from==to)return;
        for(int i=0;i<arrows.size();i++)if(arrows.get(i).from==from&&arrows.get(i).to==to){arrows.remove(i);return;}
        if(arrows.size()>=12)arrows.remove(0);
        arrows.add(new Arrow(from,to));
    }
}
