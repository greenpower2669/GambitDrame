package fr.gambitdrame;
import java.util.Arrays;
import java.util.Collections;
public final class StudyMetricsTest {
    private static void check(boolean yes){if(!yes)throw new AssertionError("Study metrics failed");}
    public static void main(String[] args){
        check(StudyMetrics.accuracy(0)==100.0);
        check(StudyMetrics.accuracy(300)<StudyMetrics.accuracy(100));
        StudyMetrics.Summary a=StudyMetrics.summarize(Arrays.asList(0,0,0));
        check(a.mean==100&&a.sigma==0&&a.count==3);
        StudyMetrics.Summary b=StudyMetrics.summarize(Arrays.asList(0,300));
        check(b.blunders==1&&b.mean<100&&b.sigma>0);
        check(StudyMetrics.summarize(Collections.emptyList()).count==0);
        check(StudyMetrics.select(Arrays.asList(12,40,5),Arrays.asList(true,false,true),true).size()==2);
        check(OpeningTheory.measure(Arrays.asList("d2d4","d7d5","c2c4","d5c4")).matches==4);
        check(OpeningTheory.measure(Arrays.asList("a2a3")).matches==0);
        check(OpeningTheory.measure(Arrays.asList("d2d4","d7d5","c2c4","a7a6")).matches==3);
        System.out.println("STUDY_METRICS_TESTS_OK 9");
    }
}
