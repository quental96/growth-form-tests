"""
Matrix3D class for 3x3 transformation matrices.
"""

from xyz import XYZ

class Matrix3D:
    """
    3x3 transformation matrix. Stored as three rows, each an XYZ vector.

    This class supports:
      - Construction from rows or columns
      - Transposition
      - Matrix-vector multiplication
      - Matrix-matrix multiplication
      - Construction of orthogonal transformation matrices from vector pairs or triples
    """

    def __init__(self, row1, row2, row3):
        """
        Initialize a Matrix3D from three row vectors.
        Args:
            row1 (XYZ): First row.
            row2 (XYZ): Second row.
            row3 (XYZ): Third row.
        """
        self.row1 = row1
        self.row2 = row2
        self.row3 = row3

    @staticmethod
    def from_columns(col1, col2, col3):
        """
        Construct a Matrix3D from three column vectors (XYZ).
        Internally, the matrix is stored by rows, so this method transposes the columns.
        Args:
            col1 (XYZ): First column.
            col2 (XYZ): Second column.
            col3 (XYZ): Third column.
        Returns:
            Matrix3D: The resulting matrix.
        """
        return Matrix3D.transpose(Matrix3D(col1, col2, col3))

    def __str__(self):
        """
        String representation of the matrix.
        Returns:
            str: String showing all three rows.
        """
        return f"Matrix with rows {self.row1} {self.row2} {self.row3}"

    @staticmethod
    def identity():
        """
        Return the 3x3 identity matrix.
        Returns:
            Matrix3D: The identity matrix.
        """
        return Matrix3D(XYZ(1, 0, 0), XYZ(0, 1, 0), XYZ(0, 0, 1))

    @staticmethod
    def transpose(m):
        """
        Return the transpose of matrix m.
        Args:
            m (Matrix3D): The matrix to transpose.
        Returns:
            Matrix3D: The transposed matrix.
        """
        return Matrix3D(
            XYZ(m.row1.x, m.row2.x, m.row3.x),
            XYZ(m.row1.y, m.row2.y, m.row3.y),
            XYZ(m.row1.z, m.row2.z, m.row3.z)
        )

    @staticmethod
    def mult(m, v):
        """
        Multiply matrix m by vector v (XYZ).
        Args:
            m (Matrix3D): The matrix.
            v (XYZ): The vector.
        Returns:
            XYZ: The resulting vector.
        """
        return XYZ(
            XYZ.dot(m.row1, v),
            XYZ.dot(m.row2, v),
            XYZ.dot(m.row3, v)
        )

    @staticmethod
    def product(m1, m2):
        """
        Multiply two matrices (m1 * m2).
        Args:
            m1 (Matrix3D): First matrix.
            m2 (Matrix3D): Second matrix.
        Returns:
            Matrix3D: The product matrix.
        """
        m2t = Matrix3D.transpose(m2)
        return Matrix3D.from_columns(
            Matrix3D.mult(m1, m2t.row1),
            Matrix3D.mult(m1, m2t.row2),
            Matrix3D.mult(m1, m2t.row3)
        )

    @staticmethod
    def from_pairs(t1, u1, t2, u2):
        """
        Construct an orthogonal transformation matrix M such that:
        M * t1 = t2 and M * u1 = u2.
        Assumes t's and u's are unit vectors (not colinear) with the same dot product.
        Args:
            t1, u1 (XYZ): Source vectors.
            t2, u2 (XYZ): Target vectors.
        Returns:
            Matrix3D: The resulting transformation matrix.
        """
        v1 = XYZ.unit(XYZ.cross(t1, u1))
        v2 = XYZ.unit(XYZ.cross(t2, u2))
        w1 = XYZ.cross(v1, u1)
        w2 = XYZ.cross(v2, u2)
        return Matrix3D.from_orthog3(u2, v2, w2, u1, v1, w1)

    @staticmethod
    def from_orthog3(u1, v1, w1, u2, v2, w2):
        """
        Construct an orthogonal transformation matrix M such that:
        M * u1 = u2, M * v1 = v2, M * w1 = w2.
        Assumes each (u, v, w) triple is an orthonormal set.
        If M1 has columns u1, v1, w1 and M2 has columns u2, v2, w2,
        then the answer is M2 * M1^T.
        Args:
            u1, v1, w1 (XYZ): Source orthonormal set.
            u2, v2, w2 (XYZ): Target orthonormal set.
        Returns:
            Matrix3D: The resulting transformation matrix.
        """
        return Matrix3D.product(
            Matrix3D.from_columns(u2, v2, w2),
            Matrix3D(u1, v1, w1)
        )
