package com.github.xathviar.SoulsHackCore;

/**
 * To connect two Points or two Rooms
 */
public class Line implements Comparable<Line> {
    public Point point1, point2;

    public int length;

    /**
     * Constructor for {@link Line}
     *
     * @param point1 the first Point
     * @param point2 the second Point
     */
    public Line(Point point1, Point point2) {
        this.point1 = point1;
        this.point2 = point2;
        calcLength();
    }

    /**
     * Getter for Point1
     *
     * @return {@link Point}
     */
    public Point getPoint1() {
        return point1;
    }

    /**
     * Getter for Point2
     *
     * @return {@link Point}
     */
    public Point getPoint2() {
        return point2;
    }

    /**
     * Changes the x-coordinate of point 1 and then re-calculates the length of the line
     *
     * @param x the new coordinate of point 1
     */
    public void changePoint1X(int x) {
        point1.setX(x);
        calcLength();
    }

    /**
     * Changes the y-coordinate of point 1 and then re-calculates the length of the line
     *
     * @param y the new coordinate of point 1
     */
    public void changePoint1Y(int y) {
        point1.setY(y);
        calcLength();
    }

    /**
     * Changes the x-coordinate of point 2 and then re-calculates the length of the line
     *
     * @param x the new coordinate of point 2
     */
    public void changePoint2X(int x) {
        point2.setX(x);
        calcLength();
    }

    /**
     * Changes the y-coordinate of point 2 and then re-calculates the length of the line
     *
     * @param y the new coordinate of point 2
     */
    public void changePoint2Y(int y) {
        point2.setY(y);
        calcLength();
    }

    /**
     * Gets the other Side of the point
     *
     * @param x
     * @param y
     * @return
     */
    public Point getOtherSideOfLine(int x, int y) {
        return x == point1.getX() && y == point1.getY() ? point2 : point1;
    }

    public Point getPointAtCoordinate(int x, int y) {
        if (x == point1.getX() && y == point1.getY()) {
            return point1;
        } else if (x == point2.getX() && y == point2.getY()) {
            return point2;
        }
        return null;
    }

    /**
     * Calculates the length of the line
     */
    public void calcLength() {
        int a = Math.abs(point2.getX() - point1.getX());
        int b = Math.abs(point2.getY() - point1.getY());
        length = (a * a) + (b * b);
    }

    /**
     * Getter for the length of the line
     *
     * @return the length of the line
     */
    public double getLength() {
        return length;
    }

    @Override
    public int compareTo(Line o) {
        return Integer.compare((int) Math.round(this.length), (int) Math.round(o.length));
    }

    @Override
    public String toString() {
        return String.format("%s->%s|%d|", point1, point2, Math.round(length));
    }
}
