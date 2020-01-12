package edu.ucsb.cs.cs184.bdarnell.sudoku;

public class Puzzle {

    private int[][] grid = new int[9][9];
    private int[][][] pencil = new int[9][9][9];

    public Puzzle(int[][] fill) {
        // Initialize Grid:
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                grid[i][j] = fill[i][j];
            }
        }
        // Initialize Pencil:
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                for (int k = 0; k < 9; k++) {
                    pencil[i][j][k] = 1;
                }
            }
        }

    }

    public int[][] getGrid() {
        return grid;
    }

    /**
     * @return number filled in the grid
     */
    public int getCount() {
        int count = 0;
        for (int[] row : grid) {
            for (int num : row) {
                if (num != 0) count++;
            }
        }
        return count;
    }

    public boolean solved() {
        return this.getCount() == 81;
    }

    public void initialize() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] != 0) {
                    for (int k = 0; k < 9; k++) {
                        pencil[i][j][k] = 0;
                    }
                    pencil[i][j][grid[i][j] - 1] = 1;
                }
            }
        }
    }

    /**
     * Deduce what numbers can be filled in the grid
     */
    public void eliminate() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] != 0) {
                    // Rows:
                    for (int p = 0; p < 9; p++) {
                        pencil[i][p][grid[i][j] - 1] = 0;
                    }
                    // Columns:
                    for (int p = 0; p < 9; p++) {
                        pencil[p][j][grid[i][j] - 1] = 0;
                    }
                    // Squares:
                    for (int p = (i / 3) * 3; p < ((i / 3) + 1) * 3; ++p) {
                        for (int q = (j / 3) * 3; q < ((j / 3) + 1) * 3; ++q) {
                            pencil[p][q][grid[i][j] - 1] = 0;
                        }
                    }
                    // Reset original square:
                    for (int k = 0; k < 9; k++) {
                        pencil[i][j][k] = 0;
                    }
                    pencil[i][j][grid[i][j] - 1] = 1;
                }
            }
        }
    }

    /**
     * Update the pencil based on the grid
     */
    public void reduce() throws InvalidPuzzleException {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int count = 0;
                int value = 0;
                for (int k = 0; k < 9; k++) {
                    if (pencil[i][j][k] == 1) {
                        count += 1;
                        value = k + 1;
                    }
                }
                if (count == 0) throw new InvalidPuzzleException();
                if (count == 1) grid[i][j] = value;
            }
        }
        // Rows and Columns:
        for (int k = 0; k < 9; k++) {
            // By row:
            for (int i = 0; i < 9; i++) {
                int count = 0;
                int column = 0;
                for (int j = 0; j < 9; j++) {
                    if (pencil[i][j][k] == 1) {
                        count += 1;
                        column = j;
                    }
                }
                if (count == 0) throw new InvalidPuzzleException();
                if (count == 1) grid[i][column] = k + 1;
            }
            // By column:
            for (int j = 0; j < 9; j++) {
                int count = 0;
                int row = 0;
                for (int i = 0; i < 9; i++) {
                    if (pencil[i][j][k] == 1) {
                        count += 1;
                        row = i;
                    }
                }
                if (count == 0) throw new InvalidPuzzleException();
                if (count == 1) grid[row][j] = k + 1;
            }
        }
        // By square:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 9; k++) {
                    int count = 0;
                    int row = 0, column = 0;
                    for (int p = i * 3; p < (i + 1) * 3; p++) {
                        for (int q = j * 3; q < (j + 1) * 3; q++) {
                            if (pencil[p][q][k] == 1) {
                                count += 1;
                                row = p;
                                column = q;
                            }
                        }
                    }
                    if (count == 0) throw new InvalidPuzzleException();
                    if (count == 1) grid[row][column] = k + 1;
                }
            }
        }

    }

    public void display() {
        for (int[] row : grid) {
            for (int num : row) {
                if (num == 0) {
                    System.out.print(" ");
                } else {
                    System.out.print(num);
                }
                System.out.print(" ");
            }
            System.out.println();
        }
    }

    public void displayPencil() {
        for (int[][] row : pencil) {
            for (int[] num : row) {
                for (int opt : num) {
                    System.out.print(opt);
                }
                System.out.print(" ");
            }
            System.out.println();
        }
    }

    public int[][] solve() throws InvalidPuzzleException {
        for (int n = 0; n < 81 && !this.solved(); n++) {
            this.eliminate();
            this.reduce();
        }
        if (!this.solved()) {
            // Find the best square to guess on (Nishio)
            int best = 9;
            int row = 0, column = 0;
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (grid[i][j] == 0) {
                        int count = 0;
                        for (int k = 0; k < 9; k++) {
                            if (pencil[i][j][k] == 1) count++;
                        }
                        if (count < best) {
                            best = count;
                            row = i;
                            column = j;
                        }
                    }
                }
            }
            // Perform the Nishio method
            for (int k = 0; k < 9; k++) {
                if (pencil[row][column][k] == 1) {
                    int[][] newGrid = grid.clone();
                    newGrid[row][column] = k + 1;
                    Puzzle newPuzzle = new Puzzle(grid);
                    try {
                        this.grid = newPuzzle.solve();
                        this.pencil = newPuzzle.pencil;
                        return this.grid;
                    } catch (InvalidPuzzleException exception) {
                        continue;
                    }
                }
            }
            if (!this.solved()) {
                throw new InvalidPuzzleException();
            }
        }
        return this.grid;
    }

}
