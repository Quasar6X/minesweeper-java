package quasar6.main;

import java.awt.Color;

/**
 * Simple class for handling Minesweeper tiles.
 */
public class Tile {

    /**
     * This determines the rank of the tile.
     * If {@code rank == 9} than the tile is a mine.
     * @see #isMine()
     */
    private int rank;

    /**
     * This determines whether this tile has already been revealed.
     */
    private boolean hidden;

    /**
     * By default, all tiles are not mines and hidden.
     * The rank is calculated in {@link Field}.
     * The tiles are revealed (more formally: {@code setHidden(false)}) in {@link Field}.
     * @link Field#calculateTiles()
     * @link Field#revealeTiles()
     */
    public Tile()
    {
        this.rank = 0;
        this.hidden = true;
    }

    /**
     * @return the rank of this tile
     */
    public int getRank()
    {
        return rank;
    }

    /**
     * @param rank the rank to set
     */
    public void setRank(int rank)
    {
        this.rank = rank;
    }

    /**
     * @return {@code true} if this tile is hidden
     */
    public boolean isHidden()
    {
        return hidden;
    }

    /**
     * @param hidden the visibility to set
     */
    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    /**
     * Determines if this tile is a mine.
     *
     * @return {@code true} if this tile is a mine
     */
    public boolean isMine()
    {
        return this.rank == 9;
    }

    /**
     * @return a color based on the tile's rank
     */
    public Color getColor()
    {
        return switch (this.rank) {
            case 1 -> Color.BLUE;
            case 2 -> Color.GREEN;
            case 3 -> Color.RED;
            case 4 -> Color.MAGENTA;
            case 5 -> new Color(128, 0, 0); //maroon
            case 6 -> new Color(64, 224, 208); //turquoise
            case 7 -> Color.BLACK;
            default -> Color.GRAY;
        };
    }
}
