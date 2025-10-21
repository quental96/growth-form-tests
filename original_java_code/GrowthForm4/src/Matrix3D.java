

// 3x3 transformation matrix.  Stored as three rows, each an XYZ

class Matrix3D implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  XYZ row1;
  XYZ row2;
  XYZ row3;

  public Matrix3D(XYZ newrow1, XYZ newrow2, XYZ newrow3){  // constructor from rows
    row1 = newrow1;
    row2 = newrow2;
    row3 = newrow3;
  }

  public static Matrix3D fromColumns(XYZ col1, XYZ col2, XYZ col3){ // construct from columns
    return transpose(new Matrix3D(col1, col2, col3));
  }

  public String toString(){
    return "Matrix with rows " + this.row1 + this.row2 + this.row3;
  }

  public static Matrix3D identity(){
    return new Matrix3D(new XYZ(1,0,0),
                        new XYZ(0,1,0),
                        new XYZ(0,0,1));
  }

  public static Matrix3D transpose(Matrix3D m){
    return new Matrix3D(new XYZ(m.row1.x, m.row2.x, m.row3.x),
                        new XYZ(m.row1.y, m.row2.y, m.row3.y),
                        new XYZ(m.row1.z, m.row2.z, m.row3.z));
  }

  public static XYZ mult(Matrix3D m, XYZ v) {  // matrix-vector multiplication
    return new XYZ(XYZ.dot(m.row1,v), XYZ.dot(m.row2,v), XYZ.dot(m.row3,v));
  }

  public static Matrix3D product(Matrix3D m1, Matrix3D m2){   // matrix-matrix prod
    Matrix3D m2t = transpose(m2);     // so row of m2t is really column of m2
    return fromColumns(mult(m1,m2t.row1), mult(m1,m2t.row2), mult(m1,m2t.row3));
  }

  // the orthog transformation matrix M such that M t1 = t2 and M u1 = u2
  // assumes t's and u's are unit vectors (not colinear) with same dot prod.
  public static Matrix3D fromPairs(XYZ t1, XYZ u1, XYZ t2, XYZ u2){
    XYZ v1 = XYZ.unit(XYZ.cross(t1,u1));
    XYZ v2 = XYZ.unit(XYZ.cross(t2,u2));
    XYZ w1 = XYZ.cross(v1,u1);
    XYZ w2 = XYZ.cross(v2,u2);
    return fromOrthog3(u2,v2,w2, u1,v1,w1);
  }

  // the orthog transformation matrix M such that M u1 = u2 and M v1 = v2
  // and M w1 = w2.  Assumes each uvw triple is an orthonormal set.
  // If M1 has as columns u1v1w1 and M2 has columns u2v2w2,  Ans = M2 M1^T
  public static Matrix3D fromOrthog3(XYZ u1, XYZ v1, XYZ w1,
                                     XYZ u2, XYZ v2, XYZ w2){
    return product(fromColumns(u2,v2,w2), new Matrix3D(u1,v1,w1));
  }
}