"""
Texture module for defining and manipulating textures.
"""

class Texture:
    """
    Describes bumpy dual texture from cell adjacency info.

    Attributes:
        alpha (float): Bump height.
        beta (float): Radius.
        gamma (float): Height at radius.
    """

    def __init__(self, alpha, beta, gamma):
        """
        Initialize a Texture with float values.

        Args:
            alpha (float): Bump height.
            beta (float): Radius.
            gamma (float): Height at radius.
        """
        self.alpha = alpha
        self.beta = beta
        self.gamma = gamma

    @classmethod
    def from_ints(cls, a, b, c):
        """
        Alternative constructor from integer values (0-100 mapped to 0.0-1.0).

        Args:
            a (int): Alpha value (0-100 mapped to -1.0 to 1.0).
            b (int): Beta value (0-100 mapped to 0.01 to 0.99).
            c (int): Gamma value (0-100 mapped to -1.0 to 1.0).

        Returns:
            Texture: A new Texture instance.
        """
        alpha = max(-1.0, min(1.0, a / 100.0))
        beta = max(0.01, min(0.99, b / 100.0))
        gamma = max(-1.0, min(1.0, c / 100.0))
        return cls(alpha, beta, gamma)

# Predefined textures
Texture.FLAT = Texture(0, 10, 0)
Texture.BUMP = Texture(10, 17, 20)
Texture.SPIKE = Texture(100, 60, 20)
Texture.WEB = Texture(-30, 30, -30)
Texture.HAIRY = Texture(100, 90, 0)
