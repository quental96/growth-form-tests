"""
XYZ class for 3D vector operations."""


import math

class XYZ:
    """
    3D vector with double components.
    Provides static and instance methods for common vector operations.
    """

    def __init__(self, x, y, z):
        """
        Initialize a 3D vector.
        Args:
            x (float): X component.
            y (float): Y component.
            z (float): Z component.
        """
        self.x = float(x)
        self.y = float(y)
        self.z = float(z)

    def clone(self):
        """
        Return a copy of this vector.
        """
        return XYZ(self.x, self.y, self.z)

    def __str__(self):
        """
        String representation of the vector.
        """
        return f"({self.x},{self.y},{self.z})"

    def approx_eq(self, other, tol=1e-8):
        """
        Check if this vector is approximately equal to another.
        Args:
            other (XYZ): The vector to compare.
            tol (float): Tolerance for comparison.
        Returns:
            bool: True if vectors are approximately equal.
        """
        return (abs(self.x - other.x) < tol and
                abs(self.y - other.y) < tol and
                abs(self.z - other.z) < tol)

    @staticmethod
    def plus(a, b):
        """
        Add two vectors.
        Args:
            a (XYZ): First vector.
            b (XYZ): Second vector.
        Returns:
            XYZ: The sum vector.
        """
        return XYZ(a.x + b.x, a.y + b.y, a.z + b.z)

    @staticmethod
    def minus(a, b):
        """
        Subtract vector b from vector a.
        Args:
            a (XYZ): First vector.
            b (XYZ): Second vector.
        Returns:
            XYZ: The difference vector.
        """
        return XYZ(a.x - b.x, a.y - b.y, a.z - b.z)

    @staticmethod
    def scale(a, v):
        """
        Scale vector v by scalar a.
        Args:
            a (float): Scalar value.
            v (XYZ): Vector to scale.
        Returns:
            XYZ: The scaled vector.
        """
        return XYZ(a * v.x, a * v.y, a * v.z)

    @staticmethod
    def tri_center(v1, v2, v3):
        """
        Compute the centroid of three vectors (triangle center).
        Args:
            v1, v2, v3 (XYZ): The three vectors.
        Returns:
            XYZ: The centroid.
        """
        return XYZ((v1.x + v2.x + v3.x) / 3.0,
                   (v1.y + v2.y + v3.y) / 3.0,
                   (v1.z + v2.z + v3.z) / 3.0)

    @staticmethod
    def interpolate(alpha, v0, v1):
        """
        Linear interpolation between two vectors.
        Args:
            alpha (float): Interpolation parameter.
            v0 (XYZ): Start vector.
            v1 (XYZ): End vector.
        Returns:
            XYZ: Interpolated vector.
        """
        beta = 1.0 - alpha
        return XYZ(alpha * v0.x + beta * v1.x,
                   alpha * v0.y + beta * v1.y,
                   alpha * v0.z + beta * v1.z)

    @staticmethod
    def dot(a, b):
        """
        Dot product of two vectors.
        Args:
            a (XYZ): First vector.
            b (XYZ): Second vector.
        Returns:
            float: Dot product.
        """
        return a.x * b.x + a.y * b.y + a.z * b.z

    @staticmethod
    def mag2(a):
        """
        Squared magnitude of a vector.
        Args:
            a (XYZ): The vector.
        Returns:
            float: Squared magnitude.
        """
        return a.x * a.x + a.y * a.y + a.z * a.z

    @staticmethod
    def mag(a):
        """
        Magnitude (length) of a vector.
        Args:
            a (XYZ): The vector.
        Returns:
            float: Magnitude.
        """
        return math.sqrt(XYZ.mag2(a))

    @staticmethod
    def unit(v):
        """
        Return the unit (normalized) vector.
        Args:
            v (XYZ): The vector.
        Returns:
            XYZ: Unit vector.
        Raises:
            ValueError: If the vector has zero length.
        """
        m = XYZ.mag(v)
        if m == 0:
            raise ValueError("Cannot normalize zero-length vector")
        return XYZ.scale(1.0 / m, v)

    @staticmethod
    def recip(v):
        """
        Return the reciprocal vector (scaled by 1/mag2).
        Args:
            v (XYZ): The vector.
        Returns:
            XYZ: Reciprocal vector.
        Raises:
            ValueError: If the vector has zero length.
        """
        m2 = XYZ.mag2(v)
        if m2 == 0:
            raise ValueError("Cannot take reciprocal of zero-length vector")
        return XYZ.scale(1.0 / m2, v)

    @staticmethod
    def cross(a, b):
        """
        Cross product of two vectors.
        Args:
            a (XYZ): First vector.
            b (XYZ): Second vector.
        Returns:
            XYZ: Cross product vector.
        """
        return XYZ(a.y * b.z - b.y * a.z,
                   a.z * b.x - b.z * a.x,
                   a.x * b.y - b.x * a.y)

    @staticmethod
    def closest(p1, p2):
        """
        Find the closest point on the line through p2 to the origin p1.
        Args:
            p1 (XYZ): Reference point.
            p2 (XYZ): Point on the line.
        Returns:
            XYZ: Closest point.
        """
        d = XYZ.minus(p2, p1)
        d_mag2 = XYZ.mag2(d)
        if d_mag2 == 0:
            return p1.clone()
        return XYZ.minus(p1, XYZ.scale(XYZ.dot(d, p1) / d_mag2, d))

    @staticmethod
    def point_point_dist2(v1, v2):
        """
        Squared distance between two points.
        Args:
            v1 (XYZ): First point.
            v2 (XYZ): Second point.
        Returns:
            float: Squared distance.
        """
        return XYZ.mag2(XYZ.minus(v1, v2))

    def dist2_to(self, a):
        """
        Squared distance from this vector to another.
        Args:
            a (XYZ): The other vector.
        Returns:
            float: Squared distance.
        """
        return XYZ.mag2(XYZ.minus(self, a))

    @staticmethod
    def project_point_plane(v, plane):
        """
        Project a point v (XYZ) onto the given plane.

        Args:
            v (XYZ): The point to project.
            plane: An object with .normal (XYZ) and .dist (float) attributes.

        Returns:
            XYZ: The projected point on the plane.
        """
        diff = XYZ.scale(plane.dist - XYZ.dot(v, plane.normal), plane.normal)
        return XYZ.plus(v, diff)

    @staticmethod
    def point_plane_dist2(v, plane):
        """
        Compute the squared distance from point v (XYZ) to the given plane.

        Args:
            v (XYZ): The point.
            plane: An object with .normal (XYZ) and .dist (float) attributes.

        Returns:
            float: The squared distance from v to the plane.
        """
        diff = XYZ.scale(plane.dist - XYZ.dot(v, plane.normal), plane.normal)
        return XYZ.mag2(diff)

    @staticmethod
    def dihedral(normal1, normal2):
        """
        Compute the dihedral angle (in degrees) between two unit normal vectors.

        Args:
            normal1 (XYZ): First unit normal vector.
            normal2 (XYZ): Second unit normal vector.

        Returns:
            float: The angle in degrees between the two normals.
        """
        return (180.0 / math.pi) * math.acos(XYZ.dot(normal1, normal2))

    @staticmethod
    def intersect3(p1, p2, p3):
        """
        Compute the intersection point of three planes.

        Args:
            p1, p2, p3: Plane objects, each with .normal (XYZ) and .dist (float) attributes.

        Returns:
            XYZ or None: The intersection point as an XYZ, 
            or None if planes are parallel or identical.

        Note:
            Requires Matrix3D class with from_columns and mult methods.
        """

        from matrix_3d import Matrix3D

        v1 = XYZ.cross(p2.normal, p3.normal)
        v2 = XYZ.cross(p3.normal, p1.normal)
        v3 = XYZ.cross(p1.normal, p2.normal)
        c1 = XYZ.dot(p1.normal, v1)
        c2 = XYZ.dot(p2.normal, v2)
        c3 = XYZ.dot(p3.normal, v3)
        if abs(c1 * c2 * c3) < 1e-8:
            return None  # Planes are parallel or identical
        v1 = XYZ.scale(1 / c1, v1)
        v2 = XYZ.scale(1 / c2, v2)
        v3 = XYZ.scale(1 / c3, v3)
        # Build the matrix from the scaled vectors as columns
        m = Matrix3D.from_columns(v1, v2, v3)
        # Create a vector of the plane distances
        dist = XYZ(p1.dist, p2.dist, p3.dist)
        # Multiply matrix by the distance vector to get intersection point
        return Matrix3D.mult(m, dist)
