import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

from form import Form
from bud_type import BudType
from bud import Bud

def visualize_form(form, title="Cell Structure"):
    """
    Visualize the cells and their adjacency in 3D.
    """
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    xs, ys, zs = [], [], []
    bud_xs, bud_ys, bud_zs = [], [], []

    # Draw edges to neighbors (only once per pair)
    for cell in form.cell:
        for neighbor in cell.adj:
            if cell.index < neighbor.index:
                ax.plot(
                    [cell.xyz.x, neighbor.xyz.x],
                    [cell.xyz.y, neighbor.xyz.y],
                    [cell.xyz.z, neighbor.xyz.z],
                    color='gray', alpha=0.5
                )

    # Collect cell and bud coordinates
    for cell in form.cell:
        xs.append(cell.xyz.x)
        ys.append(cell.xyz.y)
        zs.append(cell.xyz.z)
        if cell.bud is not None:
            bud_xs.append(cell.xyz.x)
            bud_ys.append(cell.xyz.y)
            bud_zs.append(cell.xyz.z)

    # Draw cells and buds
    ax.scatter(xs, ys, zs, c='yellow', edgecolors='k', s=80, label='Cells')
    if bud_xs:
        ax.scatter(bud_xs, bud_ys, bud_zs, c='red', s=120, label='Bud')

    ax.set_title(title)
    ax.set_xlabel("X")
    ax.set_ylabel("Y")
    ax.set_zlabel("Z")
    ax.legend()
    plt.show()

def main():
    # Simple tetrahedron: 4 vertices, each connected to the other 3
    verts = [
        [1, 1, 1],
        [-1, -1, 1],
        [-1, 1, -1],
        [1, -1, -1]
    ]
    adjs = [
        [1, 2, 3],
        [0, 2, 3],
        [0, 1, 3],
        [0, 1, 2]
    ]
    region_r = 2

    # Create form and bud type
    form = Form(verts, adjs, region_r)
    bud_type = BudType("A")
    form.bud_types.append(bud_type)

    # Visualize initial form
    visualize_form(form, title="Initial Cell Structure")

    # Place a bud in the first cell
    bud = Bud(bud_type, form.cell[0])

    # Run make_blob to grow the structure
    bud.make_blob(10)

    # Print results
    print(f"Number of cells after growth: {len(form.cell)}")
    for i, cell in enumerate(form.cell):
        print(f"Cell {i}: xyz={cell.xyz}, adj={[form.cell.index(n) for n in cell.adj]}, bud={cell.bud is not None}")

    # Visualize after growth
    visualize_form(form, title="Cell Structure After make_blob")

if __name__ == "__main__":
    main()
