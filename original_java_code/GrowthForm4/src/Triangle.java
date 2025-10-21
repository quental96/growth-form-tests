

//represent Triangle by three points, which may have non-zero reps.
//Note: ccw sequence not enforced here.  That must be done elsewhere.

class Triangle implements java.io.Serializable {
   private static final long serialVersionUID = 1L;
   public Cell v1, v2, v3;         // three vertices

   public Triangle(Cell p1, Cell p2, Cell p3){  // constructor
      v1=p1; v2=p2; v3=p3;
   }

   public String toString(){
      return "Triangle[" + v1.toString() + ","
                         + v2.toString() + ","
                         + v3.toString() + "]";
   }

}