package fr.gambitdrame;

/** White advantage meter and immutable game-history cursor regression tests. */
public final class ReviewTest {
    private static int checks=0;
    private static void check(boolean yes,String label){
        checks++;
        if(!yes)throw new AssertionError(label);
    }
    public static void main(String[] args){
        ReviewCursor c=new ReviewCursor(5);
        check(!c.reviewing()&&c.ply()==5,"live cursor");
        c.first();check(c.reviewing()&&c.ply()==0,"first");
        c.previous();check(c.ply()==0,"lower bound");
        c.next();check(c.ply()==1,"next");
        c.seek(3);check(c.ply()==3,"seek");
        c.last();check(c.ply()==5,"last");
        c.next();check(c.ply()==5,"upper bound");
        c.setHistorySize(2);check(c.ply()==2,"history truncated");
        c.live();check(!c.reviewing()&&c.ply()==2,"return live");
        c.setHistorySize(4);check(c.ply()==4,"live follows new moves");
        check(EvaluationScale.whiteFraction(null,null)==0.5,"unknown");
        check(EvaluationScale.whiteFraction(0,null)==0.5,"even");
        check(EvaluationScale.whiteFraction(200,null)>0.65,"white advantage");
        check(EvaluationScale.whiteFraction(-200,null)<0.35,"black advantage");
        check(EvaluationScale.whiteFraction(null,3)>0.98,"white mate");
        check(EvaluationScale.whiteFraction(null,-3)<0.02,"black mate");
        check(EvaluationScale.whiteFraction(1500,null)<1,"bounded");
        check(EvaluationScale.whiteFraction(-1500,null)>0,"bounded negative");
        check("—".equals(EvaluationScale.label(null,null)),"pending label");
        check("+1,5".equals(EvaluationScale.label(150,null)),"French CP");
        check("M -4".equals(EvaluationScale.label(null,-4)),"mate label");
        System.out.println("REVIEW_AND_GAUGE_TESTS_OK "+checks);
    }
}
