

class Plane implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  public XYZ normal;   // unit normal
  public double dist;  // distance from origin

  public Plane(XYZ normal0, double dist0){  // constructor
    normal = normal0;
    dist = dist0;
    if (Math.abs(XYZ.mag(normal)-1.0) > 0.000001)
      System.out.println("Non-unitvector when creating " + this);
    if (dist <= 0.0)
      System.out.println("Bad distance when creating " + this);
  }

  public String toString(){
    return "Plane " + this.normal + " at dist " + this.dist;
  }

  public static XYZ center(Plane p) {           // point at center (= closest to origin)
    return XYZ.scale(p.dist, p.normal);
  }

}