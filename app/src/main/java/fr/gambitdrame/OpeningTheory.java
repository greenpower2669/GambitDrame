package fr.gambitdrame;

import java.util.List;
import java.util.Locale;

/** Tiny offline opening reference. Departure from this repertoire is NOT an error. */
public final class OpeningTheory {
    private OpeningTheory(){}
    private static final String[][] BOOK={
        {"Gambit de dame refusé","d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7"},
        {"Gambit de dame accepté","d2d4 d7d5 c2c4 d5c4 g1f3 g8f6 e2e3 e7e6"},
        {"Défense slave","d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3"},
        {"Défense semi-slave","d2d4 d7d5 c2c4 e7e6 b1c3 c7c6 g1f3 g8f6"},
        {"Gambit Albin","d2d4 d7d5 c2c4 e7e5 d4e5 d5d4"},
        {"Défense Tchigorine","d2d4 d7d5 c2c4 b8c6 b1c3"},
        {"Défense indienne du roi","d2d4 g8f6 c2c4 g7g6 b1c3 f8g7"},
        {"Défense nimzo-indienne","d2d4 g8f6 c2c4 e7e6 b1c3 f8b4"},
        {"Ouverture catalane","d2d4 g8f6 c2c4 e7e6 g2g3"},
        {"Défense néerlandaise","d2d4 f7f5 c2c4 g8f6"},
        {"Gambit Blackmar-Diemer","d2d4 d7d5 e2e4 d5e4 b1c3 g8f6"},
        {"Défense scandinave","e2e4 d7d5 e4d5 d8d5"},
        {"Partie italienne","e2e4 e7e5 g1f3 b8c6 f1c4 f8c5"},
        {"Défense sicilienne","e2e4 c7c5 g1f3 d7d6 d2d4"},
        {"Partie espagnole","e2e4 e7e5 g1f3 b8c6 f1b5"}
    };
    public static final class Result {
        public final String name;
        public final int matches,compared,percent;
        Result(String s,int n,int d){
            name=s;matches=n;compared=d;
            percent=d==0?0:(int)Math.round(100.0*n/d);
        }
        public String display(){
            if(compared==0)return "Ouverture : pas assez de coups pour comparer.";
            if(matches<3)return "Ouverture : hors du petit répertoire local (non considéré comme erreur).";
            return String.format(Locale.FRANCE,
                "%s · %d/%d premiers demi-coups concordants (%d%% du début comparé). Sortir du livre n'est pas une faute.",
                name,matches,compared,percent);
        }
    }
    public static Result measure(List<String> played){
        int best=-1;String name="Ouverture non répertoriée";
        if(played==null||played.isEmpty())return new Result(name,0,0);
        for(String[] entry:BOOK){
            String[] line=entry[1].split(" ");
            int matching=0;
            while(matching<played.size()&&matching<line.length&&
                  line[matching].equalsIgnoreCase(played.get(matching)))matching++;
            if(matching>best){best=matching;name=entry[0];}
        }
        return new Result(name,Math.max(0,best),Math.min(12,played.size()));
    }
}
