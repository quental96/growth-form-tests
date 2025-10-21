"""
Cell class representing a single cell in a biological form.
"""

import random

from xyz import XYZ
from form import Form
from texture import Texture

RND = random.Random()
RADIUS = 0.5  # Default radius, can be adjusted as needed
COLOR = 'yellow'  # Default color, can be replaced with a color object if needed

class Cell:
    """
    Represents a single cell in a form, including its geometry, topology, and biological state.

    Each Cell is part of a parent Form and maintains references to its spatial position,
    neighboring cells (adjacency), and region (cells within a certain hop distance).
    Cells may contain a bud, which can control growth or division. The class provides
    methods for navigating the cell's neighborhood, dividing the cell (mitosis), and
    computing geometric properties such as the outward normal and average neighbor position.

    Attributes:
        form (Form): The parent form containing this cell.
        index (int): The cell's birth order position within the form.
        xyz (XYZ): The spatial position of the cell.
        adj (list[Cell]): List of adjacent neighbor cells in counter-clockwise order.
        region (dict[Cell, int]): Mapping of neighboring cells to their hop distance.
        bud: The bud inside this cell, or None if there is none.
        radius (float): The ideal size of the cell.
        texture (str): The bump shape or texture of the cell (placeholder).
        color: The assigned color of the cell during growth.

    Methods:
        n(): Return the number of adjacent neighbors.
        next(v): Return the neighbor after v in the adjacency list (cyclic order).
        prev(v): Return the neighbor before v in the adjacency list (cyclic order).
        opp(v): Return the neighbor opposite v in the adjacency list.
        neighbor(k): Return a random cell up to k hops away.
        is_bud(): Return True if this cell contains a bud.
        __str__(): Return a string representation of the cell.
        normal(): Compute the outward unit normal vector for this cell.
        divide_topology(): Divide this cell into two (mitosis), updating topology only.
        calc_region(v0, max_depth): (static) BFS to construct local region and hop distances.
        avg_neighbors(): Return the average position (XYZ) of this cell and its neighbors.
    """

    def __init__(self, form: Form, xyz: XYZ):
        # Parent and identity
        self.form = form                # our containing form
        self.index = len(form.cell)     # our birth order position (0 based)

        # Geometry and topology
        self.xyz = xyz                  # position
        self.adj = []                   # adjacent vertices (neighbors) in ccw order
        self.region = {}                # neighborhood cells and their hop distance

        # Biological properties
        self.bud = None                 # bud inside us, or None if none

        # Appearance
        self.radius = RADIUS            # ideal cell size
        self.texture = Texture.BUMP     # bump shape of cell
        self.color = COLOR              # assigned color during growth

        # Registration
        form.cell.append(self)          # adds us to parent form

    def n(self):
        """Return the number of adjacent neighbors."""
        return len(self.adj)

    def next(self, v):
        """Return the neighbor after v in the adjacency list (cyclic order)."""
        i = self.adj.index(v)
        return self.adj[(i + 1) % self.n()]

    def prev(self, v):
        """Return the neighbor before v in the adjacency list (cyclic order)."""
        i = self.adj.index(v)
        return self.adj[(i - 1 + len(self.adj)) % self.n()]

    def opp(self, v):
        """Return the neighbor opposite v in the adjacency list."""
        i = self.adj.index(v)
        return self.adj[(i + len(self.adj) // 2) % self.n()]

    def neighbor(self, k):
        """
        Return a random cell up to k hops away.
        k=0 returns self, k=1 returns a random neighbor, etc.
        """
        if k == 0:
            return self
        if k == 1:
            return self.adj[RND.randint(0, len(self.adj) - 1)]
        return self.neighbor(1).neighbor(k - 1)

    def is_bud(self):
        """Return True if this cell contains a bud."""
        return self.bud is not None

    def __str__(self):
        """Return a string representation of the cell."""
        return f"Cell{self.index}"

    def normal(self):
        """
        Compute the outward unit normal vector for this cell.

        The normal is calculated as the average of the normals of the triangles
        formed by the cell center and each pair of adjacent neighbors, following
        the right-hand rule for cross products. This is useful for rendering or
        geometric computations on the cell surface.

        Returns:
            XYZ: The outward unit normal vector as an XYZ object.
        """
        sum_xyz = XYZ(0, 0, 0)
        if not self.adj:
            # No neighbors, return zero vector
            return sum_xyz
        # Start with the vector from this cell to the last neighbor
        old_diff = XYZ.minus(self.adj[-1].xyz, self.xyz)
        for v in self.adj:
            # Vector from this cell to the current neighbor
            new_diff = XYZ.minus(v.xyz, self.xyz)
            # Add the cross product to the sum (triangle normal)
            sum_xyz = XYZ.plus(sum_xyz, XYZ.cross(old_diff, new_diff))
            old_diff = new_diff
        # Normalize the resulting vector to get the unit normal
        return XYZ.unit(sum_xyz)

    def divide_topology(self):
        """
        Divide this cell into two (mitosis), updating topology only.

        Most neighbors will connect to one or the other cell, but two will connect to both,
        increasing their valence by 1. The split is chosen to minimize the valence of the
        boundary neighbors. This method only updates the topology; the caller should set
        geometry and appearance for the new cell.

        Returns:
            Cell: The new sibling cell created by the division.
        """
        sib = Cell(self.form, self.xyz)  # Create sibling cell and add to form
        adj_old = self.adj               # Store old adjacency list
        n = self.n()                     # Number of neighbors
        self.adj = []                    # Reset adjacency for this cell

        # Find neighbor with smallest valence (fewest neighbors)
        min_i = 0
        min_count = float('inf')
        for i in range(n):
            c1 = adj_old[i]
            if c1.n() < min_count:
                min_count = c1.n()
                min_i = i

        # Find the opposite boundary index
        opp_i = (min_i + n // 2) % n
        if n % 2 == 1:
            # For odd n, choose the lower-valence neighbor as the opposite boundary
            next_opp = (opp_i + 1) % n
            if adj_old[next_opp].n() < adj_old[opp_i].n():
                opp_i = next_opp

        # Assign half the neighbors plus sibling to this cell
        for i in range((n + opp_i - min_i) % n + 1):
            self.adj.append(adj_old[(min_i + i) % n])
        self.adj.append(sib)

        # Assign the other half plus this cell to the sibling
        for i in range((n + min_i - opp_i) % n + 1):
            sib.adj.append(adj_old[(opp_i + i) % n])
        sib.adj.append(self)

        # Insert sibling into the adjacency list of the opposite boundary neighbor
        j = adj_old[opp_i].adj.index(self)
        if j < 0:
            raise ValueError("Bug 1: self not found in opp_i's adjacency")
        adj_old[opp_i].adj.insert(j, sib)

        # Insert sibling into the adjacency list of the min boundary neighbor
        k = adj_old[min_i].adj.index(self)
        if k < 0:
            raise ValueError("Bug 2: self not found in min_i's adjacency")
        adj_old[min_i].adj.insert(k + 1, sib)

        # Update neighbors between min_i and opp_i to point to sib instead of self
        i = (opp_i + 1) % n
        while (n + i - min_i) % n != 0:
            m = adj_old[i].adj.index(self)
            if m < 0:
                raise ValueError("Bug 3: self not found in neighbor's adjacency")
            adj_old[i].adj[m] = sib
            i = (i + 1) % n

        # Recompute regions for all affected cells
        region_old = self.region
        max_depth = self.form.getRegionR()
        for u in region_old.keys():
            u.region = Cell.calc_region(u, max_depth)
        sib.region = Cell.calc_region(sib, max_depth)
        return sib

    @staticmethod
    def calc_region(v0, max_depth):
        """
        Breadth-first search to construct the local region and hop distances based on topology.

        Args:
            v0 (Cell): The starting cell.
            max_depth (int): The maximum hop distance to include in the region.

        Returns:
            dict: A dictionary mapping each cell in the region to its hop distance from v0.
        """
        r = {}      # Maps cell to hop distance
        q = [v0]    # Queue for BFS
        r[v0] = 0   # Distance to self is 0
        while q:
            v = q.pop(0)
            d = r[v]
            for u in v.adj:
                if u not in r:
                    r[u] = d + 1
                    if d < max_depth - 1:
                        q.append(u)
        return r

    def avg_neighbors(self):
        """
        Return the average position (XYZ) of this cell and its neighbors.
        """
        sum_xyz = self.xyz
        for v in self.adj:
            sum_xyz = XYZ.plus(sum_xyz, v.xyz)
        return XYZ.scale(1.0 / (len(self.adj) + 1), sum_xyz)
