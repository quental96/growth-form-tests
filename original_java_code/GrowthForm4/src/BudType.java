import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//holds set of buds of one type
//if name ends in a digit, that is the inhibit distance
public class BudType {
   
   private String name;            //A, B, C, ...
   private List<Bud> set;          //set of buds of our type
   static Random rnd = new Random();
   int stepStart;                  //1st step of our part of script
   int stepEnd;                    //1st step after our part of script
   int inhibitDistance;            //no closer than this.  0 means no inhibitions
   
   public BudType(String name) {
      this.name = name;
      set = new ArrayList<Bud>();
      stepStart = -1;             //-1 flags no script.  
      stepEnd = -1;
      inhibitDistance = 0;        //set inhibition based on last char if digit
      char last = name.charAt(name.length()-1);
      if ("123456789".indexOf(last) >= 0) inhibitDistance = last-'0';  //inhibit          
   }

   //create new bud of this type in given cell
   public boolean createBud(Cell cell){
      if (cell==null) System.out.println("BT bug 0");
      if (cell.isBud()) System.out.println("BT bug 1");
      if (cell.isBud()) return false;
      if (inhibitDistance>0 && cell.nearest(this)<inhibitDistance) return false;  //inhibit          
      Bud b = new Bud(this, cell);
      cell.bud = b;
      set.add(b);
      return true;          //successful
   }
   
   public List<Bud> getBuds() {
      return set;
   }
   
   public String getName() {
      return name;
   }
   
   //destroy bud
   public void removeBud(Cell c){
      Bud b = c.bud;
      if (set.remove(b)) c.bud = null;    //to remove it, it should be in our set   
      else throw new IllegalArgumentException();
      b.cell = null;
   }
   
   //how many are not frozen
   public int countLiveBuds(){
      int ans = 0;
      for (Bud b : set) if (!b.frozen) ans++;
      return ans;
   }
   
   //Choose a random unfrozen bud in our set
   public Bud chooseNextBudToDivide(){
      if (set.size() == 0) return null;
      if (countLiveBuds() == 0) return null;
      Bud nextBudToDivide;
      do{                          //TODO: speed this up
         nextBudToDivide = set.get(rnd.nextInt(set.size())); 
      } while (nextBudToDivide.frozen);
      nextBudToDivide.chooseCell();
      if (nextBudToDivide.getChoosenCell() == null) return null;
      return nextBudToDivide;
   }

   //check for name match, ignoring case
   public boolean matchName(String string) {
       return name.toLowerCase().equals(string.toLowerCase());
   }

   //bud transplant
   public void moveBud(Cell oldC, Cell newC) {
      if (!(oldC.isBud() && !newC.isBud())) System.out.println("BT bug 2");
      newC.bud = oldC.bud;
      oldC.bud = null;
      newC.bud.cell = newC;
   }
   
}
