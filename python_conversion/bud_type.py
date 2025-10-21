"""
BudType class for managing a set of buds of one type.
"""

import random

class BudType:
    """
    Represents a group of buds of a single type.
    Manages creation, removal, and selection of buds, as well as inhibition logic.
    """

    rnd = random.Random()

    def __init__(self, name):
        """
        Initialize a BudType with a given name.
        If the name ends with a digit, that digit is used as the inhibition distance.
        Args:
            name (str): Name of the bud type (e.g., "A", "B", "C", ...).
        """
        self.name = name  # e.g., "A", "B", "C", ...
        self.set = []     # List of Buds of this type
        self.step_start = -1  # Start index for this type's script (if any)
        self.step_end = -1    # End index for this type's script (if any)
        self.inhibit_distance = 0  # Minimum allowed distance between buds of this type

        # Set inhibition distance based on last character if it's a digit
        last = name[-1]
        if last in "123456789":
            self.inhibit_distance = int(last)

    def create_bud(self, cell):
        """
        Create a new bud of this type in the given cell.
        Returns True if successful, False otherwise.
        Args:
            cell (Cell): The cell in which to create the bud.
        """

        from bud import Bud

        if cell is None:
            print("BT bug 0: Cell is None")
            return False
        if cell.is_bud():
            print("BT bug 1: Cell already contains a bud")
            return False
        if self.inhibit_distance > 0 and cell.nearest(self) < self.inhibit_distance:
            # Inhibition: too close to another bud of this type
            return False
        b = Bud(self, cell)
        cell.bud = b
        self.set.append(b)
        return True

    def get_buds(self):
        """
        Return the list of buds of this type.
        Returns:
            list: List of Bud objects.
        """
        return self.set

    def get_name(self):
        """
        Return the name of this BudType.
        Returns:
            str: The name of the bud type.
        """
        return self.name

    def remove_bud(self, cell):
        """
        Destroy the bud in the given cell and remove it from this BudType.
        Args:
            cell (Cell): The cell whose bud should be removed.
        Raises:
            ValueError: If the bud is not found in this BudType.
        """
        b = cell.bud
        if b in self.set:
            self.set.remove(b)
            cell.bud = None
            b.cell = None
        else:
            raise ValueError("Bud not found in set")

    def count_live_buds(self):
        """
        Return the number of buds that are not frozen (i.e., still active).
        Returns:
            int: Number of active (not frozen) buds.
        """
        return sum(1 for b in self.set if not b.frozen)

    def choose_next_bud_to_divide(self):
        """
        Choose a random unfrozen bud in this set and prepare it to divide.
        Returns:
            Bud or None: The chosen Bud, or None if none are available.
        """
        if not self.set or self.count_live_buds() == 0:
            return None
        while True:
            next_bud = self.rnd.choice(self.set)
            if not next_bud.frozen:
                break
        next_bud.choose_cell()
        # Check if a cell was actually chosen for division
        if getattr(next_bud, "get_choosen_cell", lambda: None)() is None:
            return None
        return next_bud

    def match_name(self, string):
        """
        Check if the provided string matches this BudType's name (case-insensitive).
        Args:
            string (str): The string to compare.
        Returns:
            bool: True if names match, False otherwise.
        """
        return self.name.lower() == string.lower()

    def move_bud(self, old_cell, new_cell):
        """
        Transplant a bud from old_cell to new_cell.
        Args:
            old_cell (Cell): The cell currently containing the bud.
            new_cell (Cell): The cell to move the bud to.
        """
        if not (old_cell.is_bud() and not new_cell.is_bud()):
            print("BT bug 2: Invalid move operation")
        new_cell.bud = old_cell.bud
        old_cell.bud = None
        new_cell.bud.cell = new_cell
