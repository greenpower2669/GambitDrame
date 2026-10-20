package fr.gambitdrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Experimental, reproducible CP-loss based study metrics. NOT Lichess Accuracy. */
public final class StudyMetrics {
    private StudyMetrics() {}
    public static final class Summary {
        public final int count, blunders, mistakes, inaccuracies;
        public final double mean, sigma, averageLoss;
        Summary(int n,int b,int m,int i,double a,double s,double l){
            count=n;blunders=b;mistakes=m;inaccuracies=i;mean=a;sigma=s;averageLoss=l;
        }
        public String display(String player){
            return String.format(Locale.FRANCE,
                "%s : précision indicative %.1f%% · variabilité %.1f points · perte %.0f cp/coup · %d coups · %d erreurs graves",
                player,mean,sigma,averageLoss,count,blunders);
        }
    }
    public static double accuracy(int lossCp){
        return 100.0*Math.exp(-Math.max(0,lossCp)/180.0);
    }
    public static Summary summarize(List<Integer> losses){
        if(losses==null||losses.isEmpty())return new Summary(0,0,0,0,0,0,0);
        int n=losses.size(),blunders=0,mistakes=0,inaccuracies=0;
        double sum=0,lossSum=0;
        for(int loss:losses){
            sum+=accuracy(loss);lossSum+=Math.max(0,loss);
            if(loss>=300)blunders++;
            else if(loss>=150)mistakes++;
            else if(loss>=70)inaccuracies++;
        }
        double avg=sum/n,variance=0;
        for(int loss:losses){
            double diff=accuracy(loss)-avg;
            variance+=diff*diff;
        }
        return new Summary(n,blunders,mistakes,inaccuracies,avg,
            Math.sqrt(variance/n),lossSum/n);
    }
    public static List<Integer> select(List<Integer> losses,List<Boolean> whiteToMove,boolean white){
        List<Integer> selected=new ArrayList<>();
        if(losses==null||whiteToMove==null)return selected;
        int n=Math.min(losses.size(),whiteToMove.size());
        for(int i=0;i<n;i++)if(whiteToMove.get(i)==white)selected.add(losses.get(i));
        return selected;
    }
    /** Following a serious error, count the next own move as a recovery if its loss <=40cp.
     * This measures quality of follow-up decisions, not objective rescue of a lost game. */
    public static String recovery(List<Integer> losses,List<Boolean> whiteToMove,boolean white){
        int chances=0,success=0;
        for(int i=0;i+2<losses.size()&&i+2<whiteToMove.size();i++){
            if(whiteToMove.get(i)!=white||losses.get(i)<150)continue;
            for(int j=i+1;j<Math.min(i+4,losses.size());j++){
                if(whiteToMove.get(j)==white){
                    chances++;
                    if(losses.get(j)<=40)success++;
                    break;
                }
            }
        }
        return chances==0?"aucun rattrapage mesurable":
            success+"/"+chances+" prochains coups solides après une erreur";
    }
}
