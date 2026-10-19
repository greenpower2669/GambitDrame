package fr.gambitdrame;

/** Graphical black/white evaluation scale, independent of Android and the bot. */
public final class EvaluationScale {
    private EvaluationScale(){}
    /** White at bottom; 0.5 is equal, near 1.0 is White winning. */
    public static double whiteFraction(Integer whiteCentipawns,Integer whiteMate){
        if(whiteMate!=null){
            if(whiteMate>0)return 0.985;
            if(whiteMate<0)return 0.015;
            return 0.5;
        }
        if(whiteCentipawns==null)return 0.5;
        double cp=Math.max(-1500.0,Math.min(1500.0,whiteCentipawns.doubleValue()));
        return 0.02+0.96/(1.0+Math.exp(-cp/240.0));
    }
    public static String label(Integer whiteCentipawns,Integer whiteMate){
        if(whiteMate!=null)return whiteMate>0?"M +"+whiteMate:"M "+whiteMate;
        if(whiteCentipawns==null)return "—";
        return String.format(java.util.Locale.FRANCE,"%+.1f",whiteCentipawns/100.0);
    }
}
