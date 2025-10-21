import java.util.ArrayList;
import java.util.Random;

//data for a series of buds to form a line
//not to exceed a target length
public class Line {

   BudType bt;
   int targetLength;    //0 (or 1) means no limit
   ArrayList<Bud> list; //list of buds in line order
   boolean trailFlag;         //are we trailing a bud that created us?
   static Random rnd = new Random();
   
   //initially create a line of length 1 containing the given bud
   //it might be set to trail another bud, which is head of list
   public Line(Bud bud, int targetLength, Bud trailee) {
      bt = bud.budType;
      this.targetLength = targetLength;
      if (targetLength == 1) targetLength = 0;   // 1 makes no sense
      list = new ArrayList<Bud>();
      trailFlag = (trailee!=null);
      if (trailFlag) list.add(trailee);    
      list.add(bud);
      bud.line=this;
   }
   
   void print(){
      for (Bud b : list) System.out.print(b.cell);
      System.out.println();
   }
   
   //maintain line when cell splits to create a new cell.
   //dist between cells of adj buds is always 1.  If it grows > 1,
   //then the new cell must be the separator, so put a bud in it.
   public void maintain(Cell oldCell, Cell newCell){
//      System.out.println("M: " + oldCell + " " + newCell);
//      print();
      if (!(oldCell.isBud() && oldCell.bud.line!=null && oldCell.bud.line==this)) System.out.println("M bug 1"); 
      if (newCell.isBud()) System.out.println("M bug 2");
//      if (trailee != null){    //keep trail connected first
//         if (trailee.cell.dist(list.get(0).cell) > 1){
//            bt.createBud(newCell);                 //insert new bud to fill single gap
//            newCell.bud.line = this;
//            list.add(0, newCell.bud);
//            return;
//         }
//      }
      if (list.size() == 1){                          // 1 becomes 2
//         if (trailee!=null && newCell.dist(trailee.cell)==1) return;  //don't make triangle
         bt.createBud(newCell);
         newCell.bud.line = this;
         list.add(newCell.bud);
         return;
      }
      int iOld = list.indexOf(oldCell.bud);
      if (iOld<0) System.out.println("M bug 3");      //we're supposed to be in the list
      boolean first = (iOld==0);                     //flags for are we at either end
      boolean last = (iOld==list.size()-1);
      boolean nextGap = false;     //flags for where gap is (before and/or after split cell)
      boolean prevGap = false ;
      if (!last && oldCell.dist(list.get(iOld+1).cell) > 1) nextGap = true;
      if (!first && oldCell.dist(list.get(iOld-1).cell) > 1) prevGap = true;
      if (!nextGap && !prevGap) return;           //no gaps, easy.
      if ((nextGap && prevGap) ||        //for two gaps, must move the bud over to the sib
          ((last || newCell.dist(list.get(iOld+1).cell)==1) && //often fix gap by making sib the bud
          (first || newCell.dist(list.get(iOld-1).cell)==1)
          && !((first && trailFlag) || ((first || last) && list.size()<targetLength)))){ //but don't always move in the ends
         bt.moveBud(oldCell, newCell);    
         return;
      }
      bt.createBud(newCell);                 //insert new bud to fill single gap
      newCell.bud.line = this;
      if (nextGap) list.add(iOld+1, newCell.bud);     //put after or before us
      else         list.add(iOld, newCell.bud);

      if (targetLength>0 && list.size()>targetLength){    //snip if needed
         if (trailFlag || rnd.nextInt(2) == 0){       //randomly either:
            bt.removeBud(list.get(list.size()-1).cell);     //tail...
            list.remove(list.size()-1);
         }
         else{
            bt.removeBud(list.get(0).cell);                 //...or head
            list.remove(0);
         }
      }
   }

   //when our trailee splits, be sure to keep head connected.
   public void traileeSplit(Cell newC) {
//      System.out.println("Tip: " + newC);
//      print();
      if (list.get(0).cell.dist(list.get(1).cell) > 1){
         bt.createBud(newC);                 //insert new bud to fill  gap
         newC.bud.line = this;
         list.add(1, newC.bud);
      }
      
   }

}
