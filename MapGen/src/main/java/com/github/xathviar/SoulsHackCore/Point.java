package com.github.xathviar.SoulsHackCore;

/**
 * Specifies a Point at a given Coordinate
 */
public class Point implements Comparable<Point> {
    private int x, y, z;

    /**
     * Constructor for {@link Point}
     * z = 0
     *
     * @param x at wich x-coordinate the Point will be
     * @param y at wich y-coordinate the Point will be
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    /**
     * Constructor for {@link Point}
     *
     * @param x at wich x-coordinate the Point will be
     * @param y at wich y-coordinate the Point will be
     * @param z at wich z-coordinate the Point will be
     */
    public Point(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Getter for x
     *
     * @return x-coordinate as a int
     */
    public int getX() {
        return x;
    }

    /**
     * Setter for x
     *
     * @param x the new x-coordinate for the point
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Getter for y
     *
     * @return y-coordinate as a int
     */
    public int getY() {
        return y;
    }

    /**
     * Setter for y
     *
     * @param y the new y-coordinate for the point
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Getter for z
     *
     * @return z-coordinate as a int
     */
    public int getZ() {
        return z;
    }

    /**
     * setter for z
     *
     * @param z the new z-coordinate for the point
     */
    public void setZ(int z) {
        this.z = z;
    }

    @Override
    public int compareTo(Point o) {
        return x == o.x ? Integer.compare(y, o.y) : Integer.compare(x, o.x);
    }

    @Override
    public String toString() {
        return String.format("(%d/%d/%d)", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point other = (Point) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        return super.equals(obj);
    }
}
