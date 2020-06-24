package quasar6.main;

import java.util.*;

/**
 * Utility class for the game field.
 * The utility class pattern is chosen over singleton
 * because of runtime concerns.
 */
public final class Field {

    public static final String BEGINNER = "Beginner";
    public static final String INTERMEDIATE = "Intermediate";
    public static final String EXPERT = "Expert";
    private static int sizeX;
    private static int sizeY;
    private static Tile[][] field;
    private static final Random rand = new Random();

    /**
     * Debug method for printing the current field to the console.
     */
    @SuppressWarnings("unused")
    public static void printField()
    {
        System.out.println();
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                if (field[i][j].isMine())
                    System.out.print("M ");
                else
                    System.out.print(field[i][j].getRank() + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Calculates the ranks of tiles based on the rules of minesweeper.
     */
    private static void calculateTiles()
    {
        for (int i = 0; i < sizeX; i++)
            for (int j = 0; j < sizeY; j++)
                if (field[i][j].isMine()) {
                    Map.Entry<Integer, Integer> ij = new AbstractMap.SimpleImmutableEntry<>(i, j);
                    for (int k = Math.max(0, i - 1); k < Math.min(sizeX, i + 2); k++)
                        for (int l = Math.max(0, j - 1); l < Math.min(sizeY, j + 2); l++) {
                            Map.Entry<Integer, Integer> kl = new AbstractMap.SimpleImmutableEntry<>(k, l);
                            if (!ij.equals(kl) && !field[k][l].isMine())
                                field[k][l].setRank(field[k][l].getRank() + 1);
                        }
                }
    }

    /**
     * Reveals all the tiles neighbouring the one given in the parameters
     * according to the rules of minesweeper.
     *
     * @param x  The x coordinate of the tile
     * @param y  The y coordinate of the tile
     */
    public static void revealTiles(int x, int y)
    {
        if (field[x][y].getRank() > 0)
            field[x][y].setHidden(false);
        else {
            Map.Entry<Integer, Integer> xy = new AbstractMap.SimpleImmutableEntry<>(x, y);
            field[x][y].setHidden(false);
            for (int i = Math.max(0, x - 1); i < Math.min(sizeX, x + 2); i++)
                for (int j = Math.max(0, y - 1); j < Math.min(sizeY, y + 2); j++) {
                    Map.Entry<Integer, Integer> ij = new AbstractMap.SimpleImmutableEntry<>(i, j);
                    if (!ij.equals(xy) && field[i][j].isHidden()) {
                        field[i][j].setHidden(false);
                        if (field[i][j].getRank() == 0)
                            revealTiles(i, j);
                    }
                }
        }
    }

    /**
     * @return  True if all the mines have been revealed.
     */
    public static boolean isWinningState()
    {
        int allTiles = sizeX * sizeY;
        int mineCount;
        if (sizeY == 9)
            mineCount = 10;
        else if (sizeY == 16)
            mineCount = 40;
        else
            mineCount = 99;
        int revealedTiles = 0;
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++)
                if (!field[i][j].isHidden())
                    ++revealedTiles;
        }
        return revealedTiles == allTiles - mineCount;
    }

    /**
     * Generates the mine field based on the difficulty.
     * This or {@link #customGenerate(int, int, int)} must be called at least once,
     * otherwise the game state will be illegal.
     *
     * @param difficulty  The difficulty to generate. Valid values: Beginner, Intermediate, Expert
     * @throws IllegalArgumentException if the difficulty is not correct.
     */
    public static void generate(String difficulty)
    {
        if (difficulty == null || !BEGINNER.equals(difficulty) && !INTERMEDIATE.equals(difficulty) && !EXPERT.equals(difficulty))
            throw new IllegalArgumentException("Difficulty cannot be null and must be one of the following: Beginner, Intermediate, Expert");
        if (BEGINNER.equals(difficulty))
            customGenerate(9, 9, 10);
        else if (INTERMEDIATE.equals(difficulty))
            customGenerate(16, 16, 40);
        else
            customGenerate(16, 30, 99);
    }

    /**
     * This method initializes the field, the sizeX and sizeY variables.
     * After that, it calls {@link #setMinesOnRandomPos(int)} and {@link #calculateTiles()}.
     *
     * @param rows the amount of rows to generate
     * @param cols the amount of columns to generate
     * @param mines the amount of mines to place on the field
     * @throws IllegalArgumentException if rows > 24 or cols > 30 or if there are more mines than Tiles
     */
    public static void customGenerate(int rows, int cols, int mines)
    {
        if (mines > rows * cols || mines < 10)
            throw new IllegalArgumentException("Can't place more mines than Tiles!");
        if (rows > 24 || cols > 30 || rows < 9 || cols < 9)
            throw new IllegalArgumentException("Provided size too big!");
        sizeX = rows;
        sizeY = cols;
        field = new Tile[sizeX][sizeY];
        for (int i = 0; i < sizeX; i++)
            for (int j = 0; j < sizeY; j++)
                field[i][j] = new Tile();

        setMinesOnRandomPos(mines);
        calculateTiles();
    }

    /**
     * Sets N mines on the field.
     *
     * @param mines  The number of mines to generate
     */
    private static void setMinesOnRandomPos(int mines)
    {
        List<Map.Entry<Integer, Integer>> cache = new ArrayList<>();
        for (int i = 0; i < mines; i++) {
            int x = rand.nextInt(sizeX);
            int y = rand.nextInt(sizeY);
            Map.Entry<Integer, Integer> pos = new AbstractMap.SimpleImmutableEntry<>(x, y);
            if (cache.contains(pos)) {
                ++mines;
            } else {
                cache.add(pos);
                field[x][y].setRank(9);
            }
        }
    }

    /**
     * Debug method for counting the generated mines.
     */
    @SuppressWarnings("unused")
    public static void calcMines()
    {
        int count = 0;
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                if (field[i][j].isMine())
                    ++count;
            }
        }
        System.out.println(count);
    }

    /**
     * @return  The amount of rows
     */
    public static int getSizeX()
    {
        return sizeX;
    }

    /**
     * @return  The amount of columns
     */
    public static int getSizeY()
    {
        return sizeY;
    }

    /**
     * @param x  The x coordinate
     * @param y  The y coordinate
     * @return The {@link quasar6.main.Tile Tile} object at the given coordinates
     */
    public static Tile getTileAt(int x, int y)
    {
        return field[x][y];
    }
}
