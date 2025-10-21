"""
Bud class representing a growth unit in a cellular structure.
"""

import random

from xyz import XYZ
from bud_type import BudType
from cell import Cell

RND = random.Random()
COLLISION_DISTANCE = 1.5   # in 3D
MAX_RADIUS = 1.8
MIN_RADIUS = 0.2
DEFAULT_FATNESS = 1  # thin stems

class Bud:
    """

    Bud class representing a growth unit in a cellular structure.
    Each bud is associated with a specific type (BudType) and resides within a Cell.
    Buds can grow, divide, and interact with their environment based on their type and parameters.

    Attributes:
        bud_type (BudType): The type of the bud, defining its behavior and growth script.
        cell (Cell): The cell in which the bud resides.
        frozen (bool): Indicates if the bud has stopped growing.
        sleep_mode (bool): If True, the bud does not grow even when it is time to grow.
        general_mode (bool): If True, any cell can grow, not just those near the bud.
        fatness (int): Defines how many hops from the bud a cell can divide.
        inward_flag (bool): If True, the bud creates a concavity instead of a bump.
        head_xyz_flag (bool): If True, the bud heads in a specific direction.
        collision_check_flag (bool): If True, checks for collisions and freezes if necessary.
        step (int): Current step in the bud's growth script.
        act_count (int): Number of actions pending for the bud.
        line (object): Reference to a connected line of the same bud type, if applicable.
        ring (object): Reference to a connected ring of the same bud type, if applicable.
        trail (object): Reference to a line trailing the bud, if applicable.
        next_cell_to_divide (Cell): The next cell chosen to divide, or None if not chosen.

    Methods:
        make_blob(n, growth_form): Creates a blob by performing n steps of general growth.
        choose_general_vertex(): Selects the next cell to divide.
        divide_chosen_cell(): Divides the chosen cell and updates its properties.
        _general_vertex(): Internal method to find a suitable cell to divide.
    """

    def __init__(self, bud_type: BudType, cell: Cell):
        # Validation
        if cell.is_bud():
            raise ValueError("Bud bug: Bud cannot be created inside a bud cell")

        # Core references
        self.bud_type = bud_type        # which group we belong to (BudType)
        self.cell = cell                # cell we are inside of (Cell)
        self.cell.bud = self            # link back to the cell

        # Growth state
        self.frozen = False             # have we stopped growing?
        self.sleep_mode = False         # don't grow even though it is time to grow
        self.general_mode = False       # any cell can grow, not just near bud

        # Growth parameters
        self.fatness = DEFAULT_FATNESS  # up to how many hops from us does a cell divide
        self.inward_flag = False        # do we create a concavity instead of a bump?
        self.head_xyz_flag = False      # should we head in some direction?
        self.heading = XYZ(0, 0, 0)     # direction to head towards, if head_xyz_flag is True
        self.collision_check_flag = True  # check for collisions and if so, freeze

        # Script/Action state
        self.step = bud_type.step_start # begin at beginning of script
        self.act_count = 0              # no actions pending

        # Structure/Topology references
        self.line = None                # if we maintain a connected line of our type
        self.ring = None                # if we maintain a connected ring of our type
        self.trail = None               # if we have a line trailing us
        self.next_cell_to_divide = None # choice of who to split next, or None if haven't chosen

    def make_blob(self, n):
        """
        Perform n steps of general growth to create a blob-like structure.

        Args:
            n (int): Number of growth steps to perform.
        """

        for _ in range(n):
            self.choose_general_vertex()
            self.divide_chosen_cell()
            # Optionally relax and update graphics/capture in the growth form:
            # growth_form.relax()
            # growth_form.maybe_generate_graphics()
            # growth_form.maybe_capture()

    def choose_general_vertex(self):
        """
        Select and assign the next cell to divide using the general growth strategy.
        Sets self.next_cell_to_divide to the chosen cell.
        """

        self.next_cell_to_divide = self._general_vertex()

    def _general_vertex(self):
        """
        Heuristic for selecting a cell to divide during general growth.
        Preference order:
        1. Find a cell with fewer than 5 neighbors and pick its most connected neighbor.
        2. Find a cell with 8 or more neighbors, or randomly select among those with 7.
        3. Randomly select a neighbor of a cell with 5 or fewer neighbors.
        4. Fallback: pick any cell at random.

        Returns:
            Cell: The selected cell to divide.
        """

        # 1. Prefer cells with n < 5 neighbors
        for c in self.cell.form.cell:
            if c.n() < 5:
                best_n = 0
                best_c = None
                for cadj in c.adj:
                    if cadj.n() > best_n:
                        best_n = cadj.n()
                        best_c = cadj
                return best_c

        choices = []

        # 2. Prefer cells with n >= 8, or randomly among those with n == 7
        for c in self.cell.form.cell:
            if c.n() >= 8:
                return c
            if c.n() >= 7:
                choices.append(c)
        if choices:
            return RND.choice(choices)

        # 3. Randomly select a neighbor of a cell with n <= 5
        for c in self.cell.form.cell:
            if c.n() <= 5:
                choices.append(c)
        if choices:
            return RND.choice(choices).adj[0]

        # 4. Fallback: pick any cell
        return RND.choice(self.cell.form.cell)

    def divide_chosen_cell(self):
        """
        Divide the cell selected by choose_general_vertex and update its properties.
        Handles geometry, color, texture, and local relaxation.
        """

        if self.next_cell_to_divide is None:
            return
        old_c = self.next_cell_to_divide
        out_direction = old_c.normal()
        new_c = old_c.divide_topology()

        # Set radius for both cells
        old_c.set_radius_dont_diffuse(self.cell.get_radius())
        new_c.set_radius_dont_diffuse(self.cell.get_radius())

        # Set color and texture for new and old cells
        if old_c.is_bud():
            new_c.color = old_c.color
            new_c.texture = old_c.texture
        else:
            new_c.color = self.cell.color
            old_c.color = self.cell.color
            new_c.texture = self.cell.texture
            old_c.texture = self.cell.texture

        # Set coordinates for new and old cells
        if self.inward_flag:
            outward = XYZ.scale(-self.cell.get_radius(), out_direction)
        else:
            outward = XYZ.scale(self.cell.get_radius(), out_direction)
        new_xyz1 = old_c.avg_neighbors()
        new_xyz2 = new_c.avg_neighbors()
        old_c.xyz = XYZ.plus(new_xyz1, outward)
        new_c.xyz = XYZ.plus(new_xyz2, outward)

        # Relax the neighborhood to smooth geometry
        for _ in range(2):
            for u in old_c.adj:
                self.cell.form.spring_xyz(u)
            for u in new_c.adj:
                self.cell.form.spring_xyz(u)

        # Can steer slightly when we split if no line constraint
        if (old_c.is_bud() and old_c.bud == self and self.line is None and self.head_xyz_flag):
            if XYZ.dot(new_c.normal(), self.heading) > XYZ.dot(old_c.normal(), self.heading):
                self.bud_type.move_bud(old_c, new_c)

        # Maintain line if present
        if old_c.is_bud() and old_c.bud.line is not None:
            old_c.bud.line.maintain(old_c, new_c)
        # Special check for trail
        if old_c.is_bud() and old_c.bud == self and self.trail is not None:
            self.trail.trailee_split(new_c)
        # Maintain ring if present
        if old_c.is_bud() and old_c.bud.ring is not None:
            old_c.bud.ring.maintain(old_c, new_c)

        # Reset for next division
        self.next_cell_to_divide = None

    def towards(self, heading: XYZ):
        """
        Set the bud to head in a specific direction.

        Args:
            heading (XYZ): The direction vector to head towards.
        """
        self.head_xyz_flag = True
        self.heading = heading
