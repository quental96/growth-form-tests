import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import quicktime.std.image.NearestPointInfo;

//packet of information inside certain cells.  controls nearby divisions
public class Bud {

   BudType budType;        //which group we belong to
   Cell cell;              //cell we are inside of
   boolean frozen;         //have we stopped growing?
   int fatness;           //up to how many hops from us does a cell divide
   boolean headXYZFlag;    //should we head in some direction?
   XYZ heading;             //heading used if flag is set
   boolean collisionCheckFlag;  //check for collisions and if so, freeze
   Line line;               //if we maintain a connected line of our type
   Ring ring;               //if we maintain a connected ring of our type
   Line trail;              //if we have a line trailing us
   int step;                //step in script to perform next. e.g., program counter
   int actCount;            //# acts left in current line of script
   boolean inwardFlag;      //do we create a concavity instead of a bump?  TODO: fix
   private Cell nextCellToDivide;  //choice of who to split next, or null if haven't chosen
   boolean sleepMode;        //don't grow even though it is time to grow
   boolean generalMode;      //any cell can grow, not just near bud
   static Random rnd = new Random();
   static final double COLLISION_DISTANCE = 1.5;   //in 3D
   static final double MAX_RADIUS = 1.8;
   static final double MIN_RADIUS = 0.2;
   static final int DEFAULT_FATNESS = 1;  //thin stems
  
   //create bud of given type and insert in given cell
   public Bud(BudType budType, Cell cell) {
      if (cell.isBud()) System.out.println("Bud bug");
      this.budType = budType;
      this.cell = cell;
      frozen = false;                 //default values:
      fatness = DEFAULT_FATNESS;
      inwardFlag = false;
      headXYZFlag = false;
      collisionCheckFlag = true;
      line = null;                    //not in a line
      ring = null;
      trail = null;
      nextCellToDivide = null; 
      sleepMode = false;
      generalMode = false;
      step = budType.stepStart;       //begin at beginning
      actCount = 0;                   //no actions pending
      cell.bud = this;
   }
   
   //do action for script: one growth if set up for it
   //or several set-up lines up until (but not including) a growth
   //step always points to next line to execute
   public void act(Parser[] ps, GrowthForm gf){
      if (actCount > 0){    //in middle of a line of growth
         if (generalMode) 
            chooseGeneralVertex();
         else
            chooseCell();
         if (nextCellToDivide == null) return;
         if (!sleepMode) divideChosenCell();
         actCount--;
         return;
      }
      sleepMode = false;     //after sleeping, restore to awake mode
      generalMode = false;   //after general growth, restore to bud-based
      if (step == budType.stepEnd){       //hit end of script.  done.
         frozen = true;                   //is this the right thing to do?
         return;
      }
      if (ps[step].getCommandWord().equals("grow") || 
          ps[step].getCommandWord().equals("sleep") ||
          ps[step].getCommandWord().equals("general")){
         if (ps[step].getCommandWord().equals("sleep")) sleepMode = true;
         if (ps[step].getCommandWord().equals("general")) generalMode = true;
         actCount = ps[step].getArg1();       //set up for growing
         step++;
         return;
      }
      gf.doParsedCommand(ps[step], this);
      step++;
      if (cell != null) act(ps, gf);  //if alive, do more until reach a grow command
   }
   
   //pick one and remember it
   public void chooseCell(){
      if (frozen) {
         nextCellToDivide = null;
         return;
      }
      nextCellToDivide = cell.neighbor(fatness);
      if (rnd.nextInt(2)==0){             //TODO: Experimental
         Cell better = nextCellToDivide;  //neighboring cell with larger n() is better
         for (Cell c : nextCellToDivide.adj) 
            if (c.n() > better.n()) better = c;
         nextCellToDivide=better;
      }
      if (nextCellToDivide.isBud() && nextCellToDivide.bud.ring!=null){
         nextCellToDivide=null;            //TODO: experiment.  don't grow ring
         return;
      }
      if (collisionCheckFlag){
         Cell collide = nextCellToDivide.collisionCheck(COLLISION_DISTANCE);
         if (collide!=null){          //collision course.  freeze
            frozen=true;
            nextCellToDivide=null;
         }       
      }
   }
   
   //create blob by n steps of general growth
   void makeBlob(int n, GrowthForm gf){
      for (int i = 0; i<n; i++){
         chooseGeneralVertex();
         divideChosenCell();
         gf.relax();             //hack    TODO: move this stuff
         gf.maybeGenerateGraphics();  //hack
         gf.maybeCapture();      //hack
      }
   }
   
   //For Starting blob or general growth.  Ignores buds.
   //If there is a 4, grow it to a 5 by choosing largest adj cell.
   //Or may choose any vertex with 8 neighbors.
   //Or randomly among the 7s.  If no 7's, randomly next to a 5.
   //Use sparingly for general overall growth and equilibration.
   //Doesn't know about frozen areas, so may cause collisions
   private Cell generalVertex(){
      for (Cell c : cell.form.cell)
         if (c.n() < 5){         //if find a 4
            int bestN = 0;
            Cell bestC = null;   //choose adj cell with high n
            for (Cell cadj : c.adj)
               if (cadj.n()>bestN){
                  bestN = cadj.n();
                  bestC = cadj;
               }
            return bestC;
         }
      List<Cell> choices = new ArrayList<Cell>();
      for (Cell c : cell.form.cell) {
         if (c.n() >= 8) return c;          //first 8 we find
         if (c.n() >= 7) choices.add(c);    //or a random 7
      }
      if (choices.size() > 0) return choices.get(rnd.nextInt(choices.size()));
      for (Cell c : cell.form.cell) {
         if (c.n() <= 5) choices.add(c);
      }
      if (choices.size() > 0)               //a neighbor of a 4
         return choices.get(rnd.nextInt(choices.size())).adj.get(0);
      return cell.form.cell.get(rnd.nextInt(cell.form.cell.size()));  //any
   }
   
   public void chooseGeneralVertex(){
      nextCellToDivide = generalVertex();
   }
   
   //create n initial buds of given type and disperse them
   //designed for use from start bud
   public void disperse(BudType bt, int n, GrowthForm gf) {
      for (Cell c : cell.form.cell)
         if (n>0 && !c.isBud()){
            bt.createBud(c); 
            n--;
         }
      for (int repeat=0; repeat<25; repeat++)  //spread them out
         for (Bud b : bt.getBuds()){
            b.repel(bt);
            gf.relax();       //hack
         }
   }
   
   //who's on deck?
   public Cell getChoosenCell(){
      return nextCellToDivide;
   }
   
   //divide the chosen cell
   public void divideChosenCell(){
      if (nextCellToDivide==null) return;   
      Cell oldC = nextCellToDivide;
      XYZ outDirection = oldC.normal();       //record outward direction
      Cell newC = oldC.divideTopology();
      
      oldC.setRadiusDontDiffuse(cell.getRadius());   //bud's current radius to each
      newC.setRadiusDontDiffuse(cell.getRadius());   
         
      if (oldC.isBud()) {             //assign both the color+texture of this bud
         newC.color = oldC.color;     //unless old was a bud
         newC.texture = oldC.texture;     
      }
      else{
         newC.color = cell.color;
         oldC.color = cell.color;
         newC.texture = cell.texture;  
         oldC.texture = cell.texture;
      }
      
      XYZ outward;                        //construct new coords for both
      if (inwardFlag) outward = XYZ.scale(-cell.getRadius(), outDirection);
      else outward = XYZ.scale(cell.getRadius(), outDirection);  
      XYZ newXYZ1 = oldC.avgNeighbors(); 
      XYZ newXYZ2 = newC.avgNeighbors();
      oldC.xyz = XYZ.plus(newXYZ1, outward);
      newC.xyz =  XYZ.plus(newXYZ2, outward); 
      for (int repeat=0; repeat<2; repeat++) {          //relax the neighborhood
         for (Cell u : oldC.adj) cell.form.springXYZ(u);
         for (Cell u : newC.adj) cell.form.springXYZ(u);
      }
 
      //can steer slightly when we split if no line constraint
      if (oldC.isBud() && oldC.bud==this && line == null && headXYZFlag)           
         if (XYZ.dot(newC.normal(), heading) > XYZ.dot(oldC.normal(), heading))
            budType.moveBud(oldC, newC);  
      
      if (oldC.isBud() && oldC.bud.line!=null) oldC.bud.line.maintain(oldC, newC);  //maintain line
      if (oldC.isBud() && oldC.bud==this && trail!=null) trail.traileeSplit(newC);  //special check when we split
      if (oldC.isBud() && oldC.bud.ring!=null) oldC.bud.ring.maintain(oldC, newC);  //maintain line

      nextCellToDivide = null;
   }

   //adjust radius of this cell and future dividing cells
   public void setSize(double newSize) {
      cell.setRadius(Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, newSize)));
   }   
   
   //nudge size up or down by given delta
   public void larger(double delta) {
      setSize(cell.getRadius()+delta);     
   }

   public void inwards() {
      inwardFlag = true;     
   }

   public void outwards() {
      inwardFlag = false;     
   }

   //surrounding ring become buds of given type, overwrites existing types
   public void ring(BudType bt, int k) {
      new Ring(cell.region, k, bt);
   }
   
   //all budless neighbors become buds of given type
   public void fill(BudType bt) {
      for (Cell c : cell.adj) {
         if (!c.isBud())
            bt.createBud(c);    
      }
   }

   //one non-bud neighbor becomes the given type.  perhaps it trails
   public void spawn(BudType bt, boolean trailFlag) {
      for (Cell c : cell.adj) {
         if (!c.isBud()) {
            bt.createBud(c);
            if (trailFlag){
               c.bud.line = new Line(c.bud, 0, this);
               trail = c.bud.line;
            }
            return;
         }
      }      //does nothing if all neighbors are buds.
   }

   //we become a new type
   public void become(BudType bt) {
      Cell ourCell = cell;
      budType.removeBud(ourCell);
      bt.createBud(ourCell);
   }

   //rip out our cell's bud object (not just freezing)
   public void die() {
      budType.removeBud(cell);
      //cell.bud = null;   //TODO
   }

   //set heading to head upwards, downwards, etc.  
   public void towards(XYZ heading) {
      headXYZFlag = true;
      this.heading = heading;   //head in Z direction
   }

   public void setCollisionCheck(boolean flag) {
      collisionCheckFlag = flag;
   }

   //we may already be part of a line if created to fill a gap
   public void formLine(int targetLength){
      if (line==null) line = new Line(this, targetLength, null);
   }

   //script colors 0-255 are bounded and scaled 0.0-1.0
   public void setBudColor(int arg1, int arg2, int arg3) {
      cell.color = new Color(arg1, arg2, arg3);
   }

   //script values, -100 to 100 converted to range -1.0 to 1.0
   public void setBudTexture(int arg1, int arg2, int arg3) {
      cell.texture = new Texture(arg1, arg2, arg3);
   }
 
   //move bud over to a neighbor if further from nearest of given type
   public void repel(BudType bt){
      for (int repeat=0; repeat<3; repeat++){
         Cell oldC = cell;
         Cell bestCell = null;
         int bestDist = oldC.nearest(bt);
         for (Cell c : oldC.adj){
            if (!c.isBud()){
               int dist = c.nearest(bt, oldC);
               if (dist >= bestDist){
                  bestDist = dist;
                  bestCell = c;
               }
            }
         }
         if (bestCell != null) bt.moveBud(oldC, bestCell);
      }
   }

   //die if not facing given direction
   void mustface(XYZ direction){
      if (XYZ.dot(cell.normal(), direction) <= 0) die();
   }

   //make a tube or tunnel between two cells
   public void tube(){
      boolean done = false;
      while (!done){
         Cell A = cell.neighbor(10);   //a rnd neighbor 4 hops away
         Cell B = cell.neighbor(10);
         done = A.tube(A, B);      //only sometimes compatible
      }
   }
   
}
