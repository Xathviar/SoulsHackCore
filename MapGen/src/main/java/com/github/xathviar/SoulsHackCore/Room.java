package com.github.xathviar.SoulsHackCore;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * A Class which handles Rooms
 */
@Data
@NoArgsConstructor
public class Room {
    private Point coordinates;
    private int width, height;
    private int radius;


    /**
     * Getter for the coordinates of the room
     *
     * @return {@link Point}
     */
    public Point getCoordinates() {
        return coordinates;
    }

    /**
     * Getter for the width of the room
     *
     * @return the width of the room as int
     */
    public int getWidth() {
        return width;
    }

    /**
     * Getter for the height of the room
     *
     * @return the height of the room as int
     */
    public int getHeight() {
        return height;
    }

    /**
     * Getter for the radius of the room
     *
     * @return the radius of the room as int
     */
    public int getRadius() {
        return radius;
    }

    /**
     * Creates a Line between two Rooms
     *
     * @param room1 the first Room
     * @param room2 the second Room
     * @return {@link Line}
     */
    public static Line connectRooms(Room room1, Room room2) {
        return new Line(room1.getCoordinates(), room2.getCoordinates());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Room) {
            Room other = (Room) obj;
            if (this.radius == 0) {
                if (other.radius == 0) {
                    return width == other.width && height == other.height && coordinates.equals(other.coordinates);
                } else {
                    return false;
                }
            } else {
                if (other.radius == 0) {
                    return false;
                } else {
                    return radius == other.radius && coordinates.equals(other.coordinates);
                }
            }
        }
        return super.equals(obj);
    }

    public static Room generateSpecificRoundRoom(Point coordinates, int radius) {
        Room room = new Room();
        room.coordinates = coordinates;
        room.radius = radius;
        return room;
    }

    public static Room generateSpecificRectangledRoom(Point coordinates, int width, int height) {
        Room room = new Room();
        if (width % 2 == 0 || height % 2 == 0) {
            System.err.printf("Width and height must be uneven for Room %s with Width %d and heigth %d", coordinates.toString(), width, height);
        }
        room.coordinates = coordinates;
        room.width = width;
        room.height = height;
        return room;
    }

    public static Room generateRandomRoom(int worldWidth, int worldHeight, int dx, int dy, int[] kernel, int roomNumber) {
        return Math.random() > 0.5 ? generateRandomRectangledRoom(worldWidth, worldHeight, dx, dy, kernel, roomNumber)
                : generateRandomRoundRoom(worldWidth, worldHeight, dx, dy, kernel, roomNumber);
    }

    public static Room generateRandomRectangledRoom(int worldWidth, int worldHeight, int dx, int dy, int[] kernel, int roomNumber) {
        Room room = new Room();
        int width = (int) ((Math.abs(PerlinScalar.snoise(kernel, (dx ^ dy) + 1 + roomNumber)) * (worldWidth / 2))) + 5;
        if (width % 2 == 0) {
            width--;
        }

        int height = (int) ((Math.abs(PerlinScalar.snoise(kernel, (dx ^ dy) + 2 + roomNumber)) * (worldHeight / 2))) + 5;
        if (height % 2 == 0) {
            height--;
        }
        int x = (int) (Math.random() * (worldWidth - width) + (width / 2)) + dx;
        int y = (int) (Math.random() * (worldHeight - height) + (height / 2)) + dy;
        room.coordinates = new Point(x, y);
        room.width = width;
        room.height = height;
        return room;
    }

    public static Room generateRandomRoundRoom(int worldWidth, int worldHeight, int dx, int dy, int[] kernel, int roomNumber) {
        Room room = new Room();
        int radius = (int) (Math.abs(PerlinScalar.snoise(kernel, (dx ^ dy) + 1 + roomNumber)) * ((Math.min(worldWidth, worldHeight) / 3))) + 4;
        int x = (int) (Math.random() * (worldWidth - radius * 2) + radius) + dx;
        int y = (int) (Math.random() * (worldHeight - radius * 2) + radius) + dy;
        room.coordinates = new Point(x, y);
        room.radius = radius;
        return room;
    }

    @Override
    public String toString() {
        return radius != 0 ? String.format("R(%d/%d/%dr)", coordinates.getX(), coordinates.getY(), radius) : String.format("R(%d/%d/%dw/%dh)", coordinates.getX(), coordinates.getY(), width, height);
    }
}