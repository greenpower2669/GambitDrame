package fr.gambitdrame;

/** Replay navigation that never mutates the actual recorded game. */
public final class ReviewCursor {
    private int historySize;
    private int selected=-1; // -1 means the live game, which can advance.
    public ReviewCursor(int size){setHistorySize(size);}
    public void setHistorySize(int size){
        if(size<0)throw new IllegalArgumentException("history size");
        historySize=size;
        if(selected>historySize)selected=historySize;
    }
    public int size(){return historySize;}
    public boolean reviewing(){return selected>=0;}
    public int ply(){return selected>=0?selected:historySize;}
    public void seek(int ply){selected=Math.max(0,Math.min(historySize,ply));}
    public void first(){seek(0);}
    public void previous(){seek(ply()-1);}
    public void next(){seek(ply()+1);}
    public void last(){seek(historySize);}
    public void live(){selected=-1;}
}
