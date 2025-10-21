//describes bumpy dual texture from cell adjacency info
public class Texture {

   public double alpha;   //bump height
   public double beta;    //radius
   public double gamma;   //height at radius
   
   public static Texture FLAT = new Texture(0, 10, 0);
   public static Texture BUMP = new Texture(10, 17, 20);
   public static Texture SPIKE = new Texture(100, 60, 20);
   public static Texture WEB = new Texture(-30, 30, -30);
   public static Texture HAIRY = new Texture(100, 90, 0);
           
   public Texture(double alpha, double beta, double gamma) {
      this.alpha = alpha;
      this.beta = beta;
      this.gamma = gamma;
   }  

   //Constructor from integers.   0-100 becomes 0.0-1.0
   public Texture(int a, int b, int c) {
      this.alpha = Math.max(-1.0, Math.min(1.0,  a/100.0));
      this.beta  = Math.max(0.01, Math.min(0.99, b/100.0));
      this.gamma = Math.max(-1.0, Math.min(1.0,  c/100.0));
   }

}
