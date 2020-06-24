package quasar6.main;

import javax.swing.*;

/**
 * Simple extension of {@link javax.swing.JButton}.
 * This JButton stores its matrix position.
 * It is also immutable.
 */
public final class MatrixJButton extends JButton {

    private final int row;
    private final int col;

    /**
     * Creates a button.
     *
     * @param row the row this button is part of
     * @param col the column this button is part of.
     */
    public MatrixJButton(int row, int col)
    {
        this.row = row;
        this.col = col;
    }

    /**
     * @return the row corresponding to this button
     */
    public int getRow()
    {
        return row;
    }

    /**
     * @return the column corresponding to this button
     */
    public int getCol()
    {
        return col;
    }

    /**
     * Compares the specified object with this button for equality.
     * Returns {@code true} if the given object is also a {@link MatrixJButton}
     * and the two buttons have the exact same row and column positions.
     *
     * @param o object to be compared for equality with this button
     * @return {@code true} if the specified object is equal to this button
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof MatrixJButton))
            return false;
        MatrixJButton b = (MatrixJButton)o;
        return row == b.getRow() && col == b.getCol();
    }

    /**
     * Returns the String representation of this {@link MatrixJButton}
     * in the following form: "{row}_{col}". E.g. 1_3
     *
     * @return a String representation of this button
     */
    public String toString()
    {
        return row + "_" + col;
    }
}
