package fr.gambitdrame;
public final class BoardArrowsTest {
    public static void main(String[] args){
        BoardArrows a=new BoardArrows();
        a.toggle(52,36);
        if(a.all().size()!=1)throw new AssertionError("add");
        a.toggle(52,36);
        if(!a.all().isEmpty())throw new AssertionError("toggle remove");
        for(int i=0;i<64;i++)a.toggle(i,(i+1)%64);
        if(a.all().size()!=12)throw new AssertionError("bounded");
        a.clear();
        a.toggle(0,0);
        if(!a.all().isEmpty())throw new AssertionError("invalid");
        System.out.println("BOARD_ARROWS_TESTS_OK 4");
    }
}
