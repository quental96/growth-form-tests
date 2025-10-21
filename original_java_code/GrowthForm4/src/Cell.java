import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

//one cell of a form.  Once created, never goes away.
//might contain a bud which controls nearby growth
//might be told (by a nearby bud) to divide.
public class Cell {

   Form form;                  //our containing form
   Bud bud;                    //GrowthBud inside us, or null if none
   List<Cell> adj;             //adjacent vertices (neighbors) in ccw order
   Map<Cell, Integer> region;  //neighborhood v's and their hop distance
   XYZ xyz;                    //position
   int index;                  //our birth order position (0 based)
   private double radius;      //ideal cell growSize.  should be approx 0.5
   Texture texture;            //bump shape of cell
   Color color;                //assigned color during growth
   static Random rnd = new Random();    
      
   //constructor.  Adds the constructed cell to the given form
   Cell(Form form, XYZ xyz) {
      this.form = form;                //record the surface we are part of
      index = form.cell.size();        //record our position in list
      this.xyz = xyz;                  //initial position
      setColor(Color.yellow);          //default color
      texture = Texture.BUMP;          //default texture
      adj = new ArrayList<Cell>(8);    //no neighbors yet
      region = new LinkedHashMap<Cell, Integer>();
      radius = 0.5;                    //default for unit length edges
      bud = null;                      //later a bud may be inserted in us
      form.cell.add(this);             //adds us to parent form
   }
   
   //accessor for number of neighbors
   public int n(){
      return adj.size();
   }
   
   //return cell after v in adjacency list
   public Cell next(Cell v){
      int i = adj.indexOf(v);
      if (i<0) throw new IllegalArgumentException();
      return adj.get((i+1)%n());
   }
   
   //return cell previous to v in adjacency list
   public Cell prev(Cell v){
      int i = adj.indexOf(v);
      if (i<0) throw new IllegalArgumentException();
      return adj.get((i-1+adj.size())%n());     //fixes % bug
   }
   
   //return cell opposite given cell in adjacency list
   public Cell opp(Cell v){
      int i = adj.indexOf(v);
      if (i<0) throw new IllegalArgumentException();
      return adj.get((i+adj.size()/2)%n());  
   }
   
   //return random cell up to k hops away
   public Cell neighbor(int k){
      if (k==0) return this;
      if (k==1) return adj.get(rnd.nextInt(adj.size()));
      return neighbor(1).neighbor(k-1);    //TODO better way
   }  
   
   //divide this cell into two, i.e., do mitosis.  Most of our
   //neighbors will connect one or the other, but two of them
   //will connect to both, and thus increase their valence by 1.
   //So choose a neighbor of min valence as this boundry.  
   //If there is an even number of neighbors, the opposite boundary
   //is determined, but if odd we can round n/2 up or down to choose
   //one of lower valence as the opposite boundary.
   //This method only handles the topology.
   //Called from the bud, which should set xyz, color, radius, etc.
   //Returns the new cell so its parameters can be set
   public Cell divideTopology(){
      synchronized(form){
         Cell sib = new Cell(form, xyz);     //create our sibling.  Adds to form.
         List<Cell> adjOld = adj;            //store old adj list...
         int n = n();                          //  ...and # neighbors
         adj = new ArrayList<Cell>();        //we get a new list
         int minI = 0;                         //find a neighbor with...
         int minCount = Integer.MAX_VALUE;     //  ...smallest valence
         for (int i=0; i<n; i++){
            Cell c1 = adjOld.get(i);
            if (c1.n() < minCount){
               minCount = c1.n();
               minI = i;
            }
         }
         int oppI = (minI + n/2)%n;
         if (n%2 == 1)                           //move over one if odd number and useful
            if (adjOld.get((oppI+1)%n).n() < adjOld.get(oppI).n())
               oppI = (oppI+1)%n;
         for (int i=0; i<=(n+oppI-minI)%n; i++)  //we get half the neighbors plus sibling
            adj.add(adjOld.get((minI+i)%n));
         adj.add(sib);
         for (int i=0; i<=(n+minI-oppI)%n; i++)  //sib gets half the neighbors plus us
            sib.adj.add(adjOld.get((oppI+i)%n));
         sib.adj.add(this);

         int j = adjOld.get(oppI).adj.indexOf(this);
         if (j<0) throw new IllegalArgumentException("Bug 1");
         adjOld.get(oppI).adj.add(j, sib);                 //extra neighbor to oppI

         int k = adjOld.get(minI).adj.indexOf(this);
         if (k<0) 
            throw new IllegalArgumentException("Bug 2");
         adjOld.get(minI).adj.add(k+1, sib);              //extra neighbor to minI

         for (int i=(oppI+1)%n; (n+i-minI)%n!=0; i=(i+1)%n){  //change neighbor from this to sib where needed
            int m = adjOld.get(i).adj.indexOf(this);
            if (m<0) throw new IllegalArgumentException("Bug 3");
            adjOld.get(i).adj.set(m, sib);
         }
                
         Map<Cell, Integer> regionOld = region;      //fix regions
         int maxDepth = form.getRegionR();
         for (Cell u : regionOld.keySet()) u.region = calcRegion(u, maxDepth);
         sib.region = calcRegion(sib, maxDepth);
           
         return sib;
      }
   }

   //BFS to construct local region and hop distances based on topology
   public static Map<Cell, Integer> calcRegion(Cell v0, int maxDepth) {
      Map<Cell, Integer> r = new LinkedHashMap<Cell, Integer>();
      Queue<Cell> Q = new LinkedList<Cell>();
      r.put(v0, 0);                      //we are dist 0 from us
      Q.add(v0);                         //maintain queue of v's to do
      while (!Q.isEmpty()){
         Cell v = Q.remove();            //dequeue
         int d = r.get(v);               //dist of known v
         for (Cell u : v.adj)            //examine neighbors
            if (r.get(u) == null){       //if new...
               r.put(u, d+1);            //...it is 1 hop further
               if (d < maxDepth-1) Q.add(u);  //more to do
            }
      }
      return r;
   }

   //Join two cells with a tube of the cells that surround them
   //increases genus by 1.  The two given cells are removed.
   //cells A and B should be both in front of each other's normal (for a tube)
   //or both behind each other's normal (for a tunnel).
   //A and B should have same number of neighbors.
   //returns true if created tube, false if topology unchanged
   public boolean tube(Cell A, Cell B){
      synchronized(form){
         int n = A.adj.size();                 //n sided antiprism tube or tunnel
         if (B.adj.size() != n) return false;       //do nothing if incompatible
         if (A.dist(B) < form.getRegionR()) return false;           //do nothing if too close  TODO: how close?
         if (A.isBud() || B.isBud()) return false;  //don't kill a bud

         int iAclosest = -1;                 //find neighbor of A to start stitching with
         double distClosest = Integer.MAX_VALUE;
         for (int i=0; i<n; i++){
            double dist = A.adj.get(i).xyz.dist2To(B.xyz);
            if (dist < distClosest) {
               distClosest = dist;
               iAclosest = i;
            }
         }
         int iBclosest = -1;           //find neighbor of B to start stitching with
         distClosest = Integer.MAX_VALUE;
         for (int i=0; i<n; i++){
            double dist = B.adj.get(i).xyz.dist2To(A.adj.get(iAclosest).xyz);
            if (dist < distClosest) {
               distClosest = dist;
               iBclosest = i;
            }
         }

         Cell[] cycleA = new Cell[n];  //rim of antiprism. counterclockwise
         Cell[] cycleB = new Cell[n];  //other rim.  clockwise
         for (int i=0; i<n; i++){
            cycleA[i] = A.adj.get((iAclosest + i) % n);
            cycleB[i] = B.adj.get((iBclosest - i + n) %n);  
         }

         for (int i=0; i<n; i++){              //connect edges of antiprism
            int k = cycleA[i].adj.indexOf(A);
            if (k<0) throw new IllegalArgumentException("Error k1");
            cycleA[i].adj.set(k, cycleB[(n+i-1) % n]);
            cycleA[i].adj.add(k, cycleB[i]);
            k = cycleB[i].adj.indexOf(B);
            if (k<0) throw new IllegalArgumentException("Error k2");
            cycleB[i].adj.set(k, cycleA[(i+1) % n]);
            cycleB[i].adj.add(k, cycleA[i]);
         }

         int maxDepth = A.form.getRegionR();               //re-build regions
         for (Cell u : cycleA) u.region = calcRegion(u, maxDepth);
         for (Cell u : cycleB) u.region = calcRegion(u, maxDepth);

         for (int repeat=0; repeat<3; repeat++) {          //relax the neighborhood
            for (Cell u : cycleA) A.form.springXYZ(u);
            for (Cell u : cycleB) B.form.springXYZ(u);
         }

         A.adj.clear();                //delete A and B
         B.adj.clear();
         A.form.removeCell(A);
         B.form.removeCell(B);
         form.genus++;                //genus increments
         return true;
      }
   }
   
   //distance to nearest bud of given type, not including self, or cell x
   //Cell x can be null to ignore the option
   //returns maxint if no buds of that type exist.  BFS
   public int nearest(BudType bt, Cell x){
      Map<Cell, Integer> r = new LinkedHashMap<Cell, Integer>();
      Queue<Cell> Q = new LinkedList<Cell>();
      r.put(this, 0);                    //we are dist 0 from us
      Q.add(this);                       //maintain queue of cells to do
      while (!Q.isEmpty()){
         Cell c = Q.remove();            //dequeue
         int d = r.get(c);               //dist of known v
         for (Cell u : c.adj) {
            if (u != x){                 //don't count x
               if (u.isBud() && u.bud.budType==bt) return d+1;
               if (r.get(u) == null) {      //if new...
                  r.put(u, d+1);            //...it is 1 hop further
                  Q.add(u);                 //more to do
               }
            }
         }
      }
      return Integer.MAX_VALUE;          //none exist
   }
   
   //same as above, with no x option
   public int nearest(BudType bt){
      return nearest(bt, null);
   }
   
   //TODO: speed this up by dividing space into regions
   //look for imminent collision, within tolerance, with a cell not in our region. 
   //return null if no collision, or some cell we are too near
   public Cell collisionCheck(double tol){
      for (Cell v : form.cell){
         if (XYZ.mag2(XYZ.minus(xyz, v.xyz)) < tol*tol && region.get(v)==null){
            return v;
         }
      }
      return null;
   }
   
   public boolean isBud(){
	   return bud!=null;
   }
   
   //how many adj cells are buds of given bud type?
   public int countBudAdj(BudType bt){
	   int ans = 0;
	   for (Cell c : adj) if (c.isBud() && c.bud.budType==bt) ans++;
	   return ans;
   }
   
   public void setColor(Color color) {
//      colorRed = color.getRed()/255.0f;
//      colorGreen = color.getGreen()/255.0f;
//      colorBlue = color.getBlue()/255.0f; 
      this.color = color;
   }
   
   public Color getColor(){
      return color;  //new Color(colorRed, colorGreen, colorBlue);
   }
   
   public String toString(){
      return "Cell" + index ;//+ xyz.toString();
   }

   //position determined by average with neighboring positions
   public XYZ avgNeighbors() {
      XYZ sum = xyz;                   // new XYZ(0,0,0);
      for (Cell v : adj) sum = XYZ.plus(sum, v.xyz);
      return XYZ.scale(1.0/(adj.size()+1), sum);
   }
   
   //outward unit normal (avg n triangle normals)
   public XYZ normal(){
      XYZ sum = new XYZ(0,0,0);
      XYZ oldDiff = XYZ.minus(adj.get(adj.size()-1).xyz, xyz);
      for (Cell v : adj) {
         XYZ newDiff = XYZ.minus(v.xyz, xyz);
         sum = XYZ.plus(sum, XYZ.cross(oldDiff, newDiff));
         oldDiff = newDiff;
      }
      return XYZ.unit(sum);
   }
   
   public void printRegion(){
      System.out.print("Cell " + index + ": ");
      System.out.println(region);
   }

   public int dist(Cell c){   //distance to another cell, in surface hops
      return form.dist(this, c);
   }
   
   public double getRadius(){
      return radius;
   }
   
   //sets our radius and diffuses it to adj v's for smooth gradation
   public void setRadius(double cellRadius) {
      radius = cellRadius;
      for (Cell v : adj) 
         v.setRadiusDontDiffuse(0.5 * (radius + v.getRadius()));
   }

   //sets our radius but doesn't diffuse it any further
   public void setRadiusDontDiffuse(double cellRadius) {
      radius = cellRadius;
   }

   //make blended color alpha of the way from c1 to c2
   public static Color colorBlend(double alpha, Color c1, Color c2) {
      alpha = Math.max(0.0, Math.min(1.0, alpha));    // 0 <= alpha <= 1
      return new Color((int)((1-alpha)*c1.getRed()   + alpha*c2.getRed()),
                       (int)((1-alpha)*c1.getGreen() + alpha*c2.getGreen()),
                       (int)((1-alpha)*c1.getBlue()  + alpha*c2.getBlue()));
   }
   
   public void setTextureFlat(){
      texture = Texture.FLAT;
   }
   public void setTextureBump(){
      texture = Texture.BUMP;
   }
   public void setTextureSpike(){
      texture = Texture.SPIKE;
   }
   public void setTextureWeb(){
      texture = Texture.WEB;
   }
   public void setTextureHairy() {
      texture = Texture.HAIRY;
      
   }
   
}
