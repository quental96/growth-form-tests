import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


//Geometric form stored as a triangulated manifold
//methods which modify or read the str synchronize on it
public class Form {

   List<Cell> cell;   //cells in the form
   private int regionR;   //region radius from center
   List<BudType> budTypes;    //the various bud types that exist
   static Random rnd = new Random();
   static Color BLUE_FREEZE = Color.blue.brighter().brighter();
   int genus;          // number of holes 
   
   //constructor from cell coords and adj lists.  E.g., for square pyramid:
   //verts: {{0,0,1}, {1,0,0}, {0,1,0}, {-1,0,0}, {0,-1,0}}
   //adj: {{1,2,3,4}, {0,4,3,2}, {0,1,3}, {0,2,1,4}, {0,3,1}}
   //regionR is max # hops from v to other v's in its region
   public Form(int[][] verts, int[][] adj, int regionR) {
      this.regionR = regionR;
      cell = new ArrayList<Cell>();
      budTypes = new ArrayList<BudType>();    //initially no buds
      genus = 0;            // assume a simple object is given
      
      for (int i=0; i<verts.length; i++)
         new Cell(this, new XYZ(verts[i][0], verts[i][1], verts[i][2]));
      for (int i=0; i<adj.length; i++)
         for (int j : adj[i]) 
            cell.get(i).adj.add(cell.get(j));
      
      int[][] hops = countHops();      //all pairs distances
      for (int i=0; i<verts.length; i++)
         for (int j=0; j<verts.length; j++)
            if (hops[i][j] <= regionR){
               cell.get(i).region.put(cell.get(j), hops[i][j]);
               cell.get(j).region.put(cell.get(i), hops[i][j]);
            }
   }
   
   //remove cell from our list of cells.  
   //assumes it is already eliminated from the topology
   public void removeCell(Cell c){
      synchronized(this){
         System.out.println("removing " + c.index);
            for (Cell c1 : cell)
            if (c1.adj.contains(c))
               throw new IllegalArgumentException("Can't remove active cell");
         for (Cell c1 : cell)
            c1.region.remove(c);      //remove it from all distance maps
         if (c.isBud()) c.bud.die();  //kill its bud, if any
         c.index = -1;                //mark cell as no longer used
         c.region.clear();
         c.adj.clear();
         //cleanup();
      }
   }
   
   //distance as measured by region map
   public int dist(Cell v1, Cell v2){
      Integer d = v1.region.get(v2);
      if (d == null) return regionR+1;  
      return d;
   }

   //calc hop counts based on cell topology
   //Floyd's all-pair dist alg.  Slow: O(n^3)
   public synchronized int[][] countHops(){
      int n=cell.size();
      int[][] hops = new int[n][n];   //all pairs distances
      for (int i = 0; i<n; i++){
         hops[i] = new int[n];
         Arrays.fill(hops[i], 1000000);            
      }
      for (int i=0; i<n; i++) hops[i][i]=0;
      for (Cell v1 : cell)
         for (Cell v2 : v1.adj)
            hops[v1.index][v2.index]=1;
      for (int k=0; k<n; k++)
         for (int i=0; i<n; i++)
            for (int j=0; j<n; j++)
               hops[i][j] = Math.min(hops[i][j], hops[i][k]+hops[k][j]);
      return hops;
   }
   
//   public synchronized void printHops(){
//      for (int i=0; i<hops.length; i++){
//         System.out.print(i + ":");
//         for (int j=0; j<hops.length; j++)
//            System.out.print(" " + hops[i][j]);
//         System.out.println();
//      }
//   }
   
   //create list of cell XYZ's after k rounds of smoothing
   public synchronized XYZ[] makeSmooth(int k) {
      synchronized(this){
         XYZ[] ans = new XYZ[cell.size()];
         int i=0;
         for (Cell c : cell) ans[i++] = c.xyz;
         return makeSmooth(ans, k);
      }
   }
   
   //same as above, with array parameter to pass
   private XYZ[] makeSmooth(XYZ[] input, int k) {
      synchronized(this){
         if (k<=0) return input;
         XYZ[] ans = new XYZ[input.length];
         for (int i=0; i<cell.size(); i++){
            ans[i] = input[i];         //averalg neighbors and self
            for (Cell c : cell.get(i).adj) ans[i] = XYZ.plus(ans[i], input[c.index]);
            ans[i] = XYZ.scale(1.0/(cell.get(i).adj.size()+1), ans[i]);
         }
         return makeSmooth(ans, k-1);
      }
   }
   
   //use spring model to nudge xyz location of given cell
   //only points in our region affect us
   public synchronized void springXYZ(Cell v){
      double dEdx = 0;     //coefficients for energy minimization eqs.
      double dEdy = 0;     //want 3 partial derivs to be zero simultaneously
      double dEdz = 0;     //Newton-Raphson iteration using Jacobian
      double d2Edx2 = 0;
      double d2Edy2 = 0;
      double d2Edz2 = 0;
      double d2Edxdy = 0;
      double d2Edxdz = 0;
      double d2Edydz = 0;
      for (Cell u : v.region.keySet())
         if (v != u){
            double dx = v.xyz.x - u.xyz.x;
            double dy = v.xyz.y - u.xyz.y;
            double dz = v.xyz.z - u.xyz.z;
            double dx2 = dx*dx;
            double dy2 = dy*dy;
            double dz2 = dz*dz;
            double d2 = dx2 + dy2 + dz2;
            double d = Math.sqrt(d2);
            double L = dist(v, u) * (v.getRadius() + u.getRadius()); 
            double K = 1.0/L;                 //spring constant
            double coeff = K*(1.0 - L/d);
            dEdx += coeff * dx;
            dEdy += coeff * dy;
            dEdz += coeff * dz;
            double coeffL = L / (d2*d);             // L / d^(3/2)
            d2Edx2 += K * (1.0 - coeffL * (dy2 + dz2));   
            d2Edy2 += K * (1.0 - coeffL * (dx2 + dz2));
            d2Edz2 += K * (1.0 - coeffL * (dx2 + dy2));
            double coeffK = K * coeffL;
            d2Edxdy += coeffK * dx * dy;
            d2Edxdz += coeffK * dx * dz;
            d2Edydz += coeffK * dy * dz;
           }
      XYZ delta = solve3(d2Edx2, d2Edxdy, d2Edxdz, d2Edy2, d2Edydz, d2Edz2, -dEdx, -dEdy, -dEdz);
      double max = 0.5;                      //don't move too far, for stability
      delta.x = Math.max(-max, Math.min(delta.x, max));
      delta.y = Math.max(-max, Math.min(delta.y, max));
      delta.z = Math.max(-max, Math.min(delta.z, max));
      v.xyz = XYZ.plus(v.xyz, delta);   
}
   
//   //bisect edges, make 4 small triangles for each orig. triangle
//   public synchronized Form subdivide(){
//      int[][] mid = new int[cell.size()][cell.size()];    //table of midpoint indices
//      Form f = new Form();
//      for (Cell v : cell){            //copy original vertices
//         Cell vNew = new Cell(f, v.xyz.clone());
//         vNew.setColor(new Color(v.colorRed, v.colorGreen, v.colorBlue));
//      }
//      for (Cell v1 : cell)          //create midpoints
//         for (Cell v2 : v1.adj)
//            if (v1.index < v2.index){
//               Cell vMid = new Cell(f, XYZ.scale(0.5, XYZ.plus(v1.xyz, v2.xyz)));
//               mid[v1.index][v2.index]=vMid.index;   //store in table
//               mid[v2.index][v1.index]=vMid.index;
//            }     
//      for (Cell v1 : cell)   
//         for (Cell v2 : v1.adj){        //connect clones to midpoints
//            Cell vMid = f.vertex.get(mid[v1.index][v2.index]);
//            f.vertex.get(v1.index).adj.add(vMid);
//            if (v1.index < v2.index) {    //connect midpoints to 6 adj pts:
//               vMid.adj.add(f.vertex.get(v1.index));
//               vMid.adj.add(f.vertex.get(mid[v1.index][v1.prev(v2).index]));
//               vMid.adj.add(f.vertex.get(mid[v2.index][v2.next(v1).index]));
//               vMid.adj.add(f.vertex.get(v2.index));
//               vMid.adj.add(f.vertex.get(mid[v2.index][v2.prev(v1).index]));
//               vMid.adj.add(f.vertex.get(mid[v1.index][v1.next(v2).index]));
//            }
//         }
//      f.countHops();
//      return f;
//   }
   
   //translates so mean of vertices is at origin
   public synchronized void center(){
      XYZ sum = new XYZ(0,0,0);
      for (Cell v : cell) sum = XYZ.plus(sum, v.xyz);
      XYZ avg = XYZ.scale(-1.0/cell.size(), sum);
      for (Cell v : cell) v.xyz = XYZ.plus(v.xyz, avg);      
   }
   
   //project points to sphere so edges approx unit length avg
   public synchronized void sphericalize(){
      double R = Math.sqrt(cell.size()/12.);
      center();
      for (Cell v : cell) v.xyz = XYZ.scale(R, XYZ.unit(v.xyz));
   }

   //solve symmetric 3x3 system of simultaneous equations:
   // [A B C] [x] = G   //(can adapt to nonsymmetric case)
   // [B D E] [y] = H
   // [C E F] [z] = I
   public static XYZ solve3(double A, double B, double C, double D, double E, double F, double G, double H, double I){
      double BBmAD = B*B - A*D;  
      double BCmAE = B*C - A*E;
      double BGmAH = B*G - A*H;
      double CBmAE = C*B - A*E;
      double CCmAF = C*C - A*F;
      double CGmAI = C*G - A*I;
      double z = (BGmAH * CBmAE - BBmAD * CGmAI) / (CBmAE * BCmAE - BBmAD * CCmAF);
      double y = (BGmAH - BCmAE*z) / BBmAD;
      double x = (G - B*y - C*z) / A;
      return new XYZ(x, y, z);
   }
   
   //solid color for all vertices
   public Color[] colorAll(Color color){
      Color[] ans = new Color[cell.size()];
      for (int i=0; i<ans.length; i++) ans[i]=color;
      return ans;
   }

   //color to show buds, fatness, and local regions
   public synchronized Color[] colorAllByGrowers(Color color, Color backgroundColor) {
      Color[] ans = colorAll(backgroundColor);
      for (BudType bt : budTypes){
         for (Bud b : bt.getBuds())
            for (Cell c : b.cell.region.keySet()) {
               int dist = b.cell.region.get(c);
               if (dist > b.fatness) ans[c.index] = color;     //first the regions
            } 
      }
      for (BudType bt : budTypes){
         for (Bud b : bt.getBuds())
            for (Cell c : b.cell.region.keySet()) {
               int dist = b.cell.region.get(c);
               if (dist<=b.fatness) ans[c.index] = color.darker();  //then growing neighborhood
            } 
      }
      for (BudType bt : budTypes){
         for (Bud b : bt.getBuds()) ans[b.cell.index] = Color.BLACK;  //then the buds
      }
      return ans;
   }

   //color as assigned in script
   public synchronized Color[] colorAllByAssigned() {
      Color[] ans = new Color[cell.size()];
      for (Cell c : cell) ans[c.index] = c.color;  //stored colors
      return ans;
   }
   
   //color by number of neighbors
   public synchronized Color[] colorAllByN() {
      Color[] ans = colorAll(Color.WHITE);               //under 5 vertices
      for (Cell v : cell){
         if (v.n() == 5) ans[v.index] = Color.YELLOW;       //5 vertices
         else if (v.n() == 6) ans[v.index] = Color.GREEN;
         else if (v.n() == 7) ans[v.index] = Color.BLUE;
         else if (v.n() > 7)  ans[v.index] = Color.MAGENTA;
      }
      return ans;
   }
   
   //color by the physical size of the cell
   public synchronized Color[] colorAllByRadius() {
      Color[] ans = new Color[cell.size()];
      for (Cell v : cell){
         double avg = 0.0;
         for (Cell u : v.adj) avg += XYZ.mag(XYZ.minus(v.xyz, u.xyz));
         avg = avg / v.adj.size();
         double alpha = (avg-0.3) / 1.4;         //usually 0.2<avg<1.8
         ans[v.index] = Cell.colorBlend(alpha, Color.ORANGE, Color.MAGENTA);
      }
      return ans;
   }

   //color to indicate if still growing or sterile
   public Color[] colorAllBySterile() {
      Color[] ans = colorAll(BLUE_FREEZE);
      for (Cell v : cell)
         if (v.isBud() && v.bud.frozen) ans[v.index] = Color.WHITE; 
      return ans;
   } 

   
    //total # of triangle vertices = # cells
    public int countVertices(){
      return cell.size();       
   }
    
   //total # of triangle edges
   public int countEdges(){
      int sum = 0;
      for (Cell v : cell) sum += v.adj.size();
      return sum/2;
   }
   
   //total # of triangle faces
   public int countFaces(){
     return countEdges() + 2 - countVertices() - 2*genus;        //V+F=E+2-2g  
  }

   
   //size of region never changes
   public int getRegionR() {
      return regionR;
   }

   public void add(BudType newType) {
      budTypes.add(newType);       
   } 
   
   public Bud chooseNextToDivide(){
      int nBuds = 0;
      for (BudType bt : budTypes) nBuds += bt.countLiveBuds();
      if (nBuds == 0) return null;
      int k = rnd.nextInt(nBuds);   //will choose kth one of all
      int m = 0;
      for (BudType bt : budTypes){
         m += bt.getBuds().size();
         if (m > k) return bt.chooseNextBudToDivide();
      }
      System.out.println("Choosing Bud program error");
      return budTypes.get(0).chooseNextBudToDivide();  
   }

   //informative text
   public String budCountString() {
      String ans = "";
      for (BudType bt : budTypes) 
         ans += ", " + bt.getBuds().size() + " " + bt.getName();
      return ans;
   }

   public Texture[] getSolidTexture(double alpha, double beta, double gamma) {
      Texture[] ans = new Texture[cell.size()];
      Texture t = new Texture(alpha, beta, gamma);
      for (int i=0; i<ans.length; i++) ans[i] = t;
      return ans;
   }
   
   public Texture[] getAssignedTexture() {
      Texture[] ans = new Texture[cell.size()];
      for (int i=0; i<ans.length; i++) ans[i] = cell.get(i).texture;
      return ans;
   }

   public Color[] blendColor(Color[] dispColor) {
      Color[] ans = new Color[dispColor.length];
      for (Cell c : cell){
         int red = 2*dispColor[c.index].getRed();       //this weighted twice
         int green = 2*dispColor[c.index].getGreen();
         int blue = 2*dispColor[c.index].getBlue();
         for (Cell cadj : c.adj){
            red += dispColor[cadj.index].getRed();
            green += dispColor[cadj.index].getGreen();
            blue += dispColor[cadj.index].getBlue();
                   }
         double n = 2 + c.adj.size();
         ans[c.index] = new Color((int)(red/n), (int)(green/n), (int)(blue/n));
      }
      return ans;
   }

   public Texture[] blendTexture(Texture[] dispTexture) {
      Texture[] ans = new Texture[dispTexture.length];
      for (Cell c : cell){
         double alph = dispTexture[c.index].alpha;
         double beta = dispTexture[c.index].beta;
         double gamm = dispTexture[c.index].gamma;

         for (Cell cadj : c.adj){
            alph += dispTexture[cadj.index].alpha;
            beta += dispTexture[cadj.index].beta;
            gamm += dispTexture[cadj.index].gamma;
                   }
         double n = 1 + c.adj.size();
         ans[c.index] = new Texture(alph/n, beta/n, gamm/n);
      }
      return ans;
   }

   //remove any unused cells, due to tubes
   public synchronized void cleanup() {
      Iterator<Cell> it = cell.iterator();
      while (it.hasNext()){
         Cell c = it.next();
         if (c.adj.isEmpty()) it.remove();
      } 
      for (Cell c : cell) c.index = cell.indexOf(c);
   }
   

   
}
