import java.io.Serializable;

// 3D vector operations with double components
// G. Hart  Jan 2003

class XYZ implements Serializable {
  private static final long serialVersionUID = 1L;
  public double x, y, z;                        // extract as .x .y and .z

  XYZ(double x0, double y0, double z0){         // constructor
    x=x0; y=y0; z=z0;
  }

  XYZ(int x0, int y0, int z0){                  // constructor from ints
    x=(double)x0; y=(double)y0; z=(double)z0;
  }
    
  public XYZ clone(){
     return new XYZ(x, y, z);
  }
  
  public String toString(){                     // "(x,y,z)" string form
    return "(" + Double.toString(this.x) + ","
               + Double.toString(this.y) + ","
               + Double.toString(this.z) + ")";
  }

  public boolean approxEQ(XYZ query){       // return true if EQ within flt. pt. tolerance
    final double tol = 0.00000001;
    return (Math.abs(this.x-query.x)<tol &&
            Math.abs(this.y-query.y)<tol &&
            Math.abs(this.z-query.z)<tol);
  }

  public static XYZ plus(XYZ a, XYZ b){          // sum
    return new XYZ(a.x+b.x, a.y+b.y, a.z+b.z);
  }

  public static XYZ minus(XYZ a, XYZ b){         // difference
    return new XYZ(a.x-b.x, a.y-b.y, a.z-b.z);
  }

  public static XYZ scale(double a, XYZ v){      // scalar prod
    return new XYZ(a*v.x, a*v.y, a*v.z);
  }

  public static XYZ triCenter(XYZ v1, XYZ v2, XYZ v3){  
     return new XYZ((v1.x+v2.x+v3.x)/3.0, (v1.y+v2.y+v3.y)/3.0, (v1.z+v2.z+v3.z)/3.0);
  }
  
  //alpha of the way from v0 to v1
  public static XYZ interpolate(double alpha, XYZ v0, XYZ v1){
     double beta = 1.0-alpha;
     return new XYZ(alpha*v0.x + beta*v1.x, alpha*v0.y + beta*v1.y, alpha*v0.z + beta*v1.z);
  }
 
  
  public static double dot(XYZ a, XYZ b){        // dot prod
    return a.x*b.x + a.y*b.y + a.z*b.z;
  }

  public static double mag2(XYZ a){              // magnitude squared
    return a.x*a.x + a.y*a.y + a.z*a.z;
  }

  public static double mag(XYZ a){               // magnitude
    return Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z);
  }

  public static XYZ unit(XYZ v){                 // unit length
    return scale(1.0/mag(v), v);                 // trust v isn't 0
  }

  public static XYZ recip(XYZ v){                // reciprocal in unit sphere
    return scale(1.0/mag2(v), v);                // trust v isn't 0
  }

  public static XYZ cross(XYZ a, XYZ b){         // cross product
    return new XYZ(a.y*b.z - b.y*a.z,  a.z*b.x - b.z*a.x,  a.x*b.y - b.x*a.y);
  }

  public static XYZ closest(XYZ p1, XYZ p2){     // point closest to orig. on line p1-p2
    XYZ d = minus(p2, p1);
    return XYZ.minus(p1, XYZ.scale(XYZ.dot(d,p1)/XYZ.mag2(d), d));
  }

  public static double pointPointDist2(XYZ v1, XYZ v2){     // dist squared between points
    return XYZ.mag2(XYZ.minus(v1,v2));
  }

//  public static double pointLineDist2(XYZ p, guide line){    // dist squared from point to line
//    XYZ unitDir = XYZ.unit(XYZ.minus(line.end2, line.end1));
//    double hypotenuse2 = XYZ.mag2(XYZ.minus(p, line.end1));
//    double oneSide = XYZ.dot(XYZ.minus(p, line.end1), unitDir);
//    return Math.max(0.0, hypotenuse2 - oneSide*oneSide);    // don't allow -epsilon
//  }

//  public static XYZ projectPointLine(XYZ p, guide line){    // project point to guide line
//    XYZ unitDir = XYZ.unit(XYZ.minus(line.end2, line.end1));
//    XYZ differ = XYZ.scale(XYZ.dot(XYZ.minus(p, line.end1),unitDir), unitDir);
//    return XYZ.plus(line.end1, differ);
//  }

  public static XYZ projectPointPlane(XYZ v, Plane p){      // project point to Plane
    XYZ diff = XYZ.scale(p.dist - XYZ.dot(v,p.normal), p.normal);
    return XYZ.plus(v, diff);
  }

  public static double pointPlaneDist2(XYZ v, Plane p){     // dist squared to Plane
    XYZ diff = XYZ.scale(p.dist - XYZ.dot(v,p.normal), p.normal);
    return XYZ.mag2(diff);
  }

  public static double dihedral(XYZ normal1, XYZ normal2){  // degree angle between unit vectors
    return (180/Math.PI) * Math.acos(XYZ.dot(normal1, normal2));
  }

  public static XYZ intersect3(Plane p1, Plane p2, Plane p3){  // intersection of 3 planes
    XYZ v1 = XYZ.cross(p2.normal, p3.normal);
    XYZ v2 = XYZ.cross(p3.normal, p1.normal);
    XYZ v3 = XYZ.cross(p1.normal, p2.normal);
    double c1 = XYZ.dot(p1.normal, v1);
    double c2 = XYZ.dot(p2.normal, v2);
    double c3 = XYZ.dot(p3.normal, v3);
    if (Math.abs(c1*c2*c3) < 0.00000001) return null;  // if any pair parallel or identical.
    v1 = XYZ.scale(1/c1, v1);
    v2 = XYZ.scale(1/c2, v2);
    v3 = XYZ.scale(1/c3, v3);
    Matrix3D m = Matrix3D.fromColumns(v1,v2,v3);
    XYZ dist = new XYZ(p1.dist, p2.dist, p3.dist);
    return Matrix3D.mult(m, dist);
  }

  //squared distance from here to there
  public double dist2To(XYZ a) {
     return XYZ.mag2(XYZ.minus(this, a));
  }

}

