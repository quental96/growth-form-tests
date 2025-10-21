import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TODO: BUG: list not necessarily in cyclic order, given Map in constructor
//FIX by sorting in constructor or modifying maintain()

//like Line, but no length limit and do index calcs modulo the length
public class Ring {

   BudType bt;
   ArrayList<Bud> list; //list of buds in line order

   //initial list will be the adjacency list of a cell that spawns this ring
   //make a bud for each and keep them in our list
   //Destroys any bud already in those cells (may lead to trouble)
//   public Ring(List<Cell>listCell, BudType bt){
//      this.bt = bt;
//      list = new ArrayList<Bud>();
//      for (Cell c : listCell){
//         if (c.isBud()) c.bud.budType.removeBud(c);
//         bt.createBud(c);
//         list.add(c.bud);
//         c.bud.ring = this;
//      }
//   }

   
   //distance k from bud.  Caller provides region mapping
   public Ring(Map<Cell, Integer>region, int k, BudType bt){
      this.bt = bt;
      list = new ArrayList<Bud>();
      for (Cell c : region.keySet()){
         if (region.get(c) == k){
            if (c.isBud()) c.bud.budType.removeBud(c);
            if (bt.createBud(c)){    //if successful
               list.add(c.bud);
               c.bud.ring = this;
            }
         }
      }
   }

   
   //maintain ring when cell splits to create a new cell.
   //dist between cells of adj buds is always 1.  If it grows > 1,
   //then the new cell must be the separator, so put a bud in it.
   public void maintain(Cell oldCell, Cell newCell){
      int n = list.size();
      if (!(oldCell.isBud() && oldCell.bud.ring!=null && oldCell.bud.ring==this)) System.out.println("R bug 1"); 
      if (newCell.isBud()) System.out.println("R bug 2");
      int iOld = list.indexOf(oldCell.bud);
      int iNext = (iOld+1)%n;
      int iPrev = (iOld-1+n)%n;
      if (iOld<0) 
         System.out.println("R bug 3");      //we're supposed to be in the list
      boolean nextGap = false;     //flags for where gap is (before and/or after split cell)
      boolean prevGap = false ;
      if (oldCell.dist(list.get(iNext).cell) > 1) nextGap = true;
      if (oldCell.dist(list.get(iPrev).cell) > 1) prevGap = true;
      if (!nextGap && !prevGap) return;           //no gaps, easy.
      if ((nextGap && prevGap) ||        //for two gaps, must move the bud over to the sib
          ((newCell.dist(list.get(iNext).cell)==1) && //often fix gap by making sib the bud
           (newCell.dist(list.get(iPrev).cell)==1))){ 
         bt.moveBud(oldCell, newCell);    
         return;
      }
      bt.createBud(newCell);                 //insert new bud to fill single gap
      newCell.bud.ring = this;
      if (nextGap) list.add(iNext, newCell.bud);     //put after or before us
      else         list.add(iOld, newCell.bud);

   }

   
}
