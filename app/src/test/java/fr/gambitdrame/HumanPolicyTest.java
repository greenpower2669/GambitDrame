package fr.gambitdrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JVM tests for the human policy; cannot claim Elo calibration from these. */
public final class HumanPolicyTest {
    public static void main(String[] args){
        ChessGame.Position p=ChessGame.fromFen(ChessGame.START);
        Map<String,Integer> observed=new HashMap<>();
        observed.put("e2e4",900);
        observed.put("d2d4",500);
        observed.put("e2e5",99999);
        HumanPolicy sampler=new HumanPolicy(700,12345L,0.4);
        int e4=0,d4=0;
        for(int i=0;i<100;i++){
            HumanPolicy.Choice c=sampler.choose(p,observed,0);
            if(c==null)throw new AssertionError("No choice");
            if(c.move.uci().equals("e2e4"))e4++;
            else if(c.move.uci().equals("d2d4"))d4++;
            else throw new AssertionError("Illegal / missing human move: "+c.move);
        }
        if(e4==0||d4==0)throw new AssertionError("Sampling degenerates to majority vote");
        HumanPolicy offline=new HumanPolicy(500,99L,0.2);
        for(int i=0;i<30;i++){
            HumanPolicy.Choice c=offline.choose(p,null,0);
            if(c==null)throw new AssertionError("No fallback");
            boolean legal=false;
            for(ChessGame.Move m:ChessGame.legal(p))if(m.uci().equals(c.move.uci()))legal=true;
            if(!legal)throw new AssertionError("Illegal fallback "+c.move);
            if(!c.source.toLowerCase().contains("non calibr"))throw new AssertionError("Missing limitation label");
        }
        System.out.println("HUMAN_POLICY_TESTS_OK 130 choices e4="+e4+" d4="+d4);
    }
}
