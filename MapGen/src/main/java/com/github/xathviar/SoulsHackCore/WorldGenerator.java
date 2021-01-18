package com.github.xathviar.SoulsHackCore;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class WorldGenerator {
    private Room[] rooms;
    private int width;
    private int height;
    private BufferedImage image;
    private Graphics2D graphics2D;
    private int[] kernel;
    private ArrayList<Edge> edges;

    public WorldGenerator(int width, int height) {
        rooms = new Room[width * height / 100];
        this.width = width;
        this.height = height;
        image = new BufferedImage(width * 4, height * 4, BufferedImage.TYPE_INT_RGB);
        this.graphics2D = image.createGraphics();
        graphics2D.setColor(Tile.WALL.getColor());
        graphics2D.fillRect(0, 0, width, height);
        kernel = PerlinScalar.permutation(8943575);
        createRooms();
        connectRooms();
        connectLinesToTileArray();
        addRoomsToTileArray();
        createDoors();
    }


    private void createRooms() {
        int hw = (int) Math.sqrt(rooms.length);
        for (int i = 0; i < rooms.length; i++) {
            int j = i;
            int dx = j / height;
            int dy = j % width;
            rooms[i] = Room.generateRandomRoom(width >> 1, height >> 1, dx, dy, kernel, i * 1000);
        }
    }

    private void connectRooms() {
        Kruskal kruskal = new Kruskal();
        ArrayList<Edge> connections = new ArrayList<>();
        for (int i = 0; i < rooms.length; i++) {
            for (int j = 0; j < rooms.length; j++) {
                if (i != j) {
                    connections.add(new Edge(i, j, Room.connectRooms(rooms[i], rooms[j]).length));
                }
            }
        }
        edges = kruskal.kruskalMST(connections, rooms.length * 2);
    }

    private void addRoomsToTileArray() {
        graphics2D.setColor(Tile.FLOOR.getColor());
        for (Room room : rooms) {
            if (room.getRadius() == 0) {
                graphics2D.fillRect(room.getCoordinates().getX() - (room.getWidth() / 2)
                        , room.getCoordinates().getY() - (room.getHeight() / 2), room.getWidth(), room.getHeight());
            } else {
                graphics2D.fillRoundRect(room.getCoordinates().getX() - (room.getRadius() / 2)
                        , room.getCoordinates().getY() - (room.getRadius() / 2)
                        , room.getRadius(), room.getRadius(), room.getRadius(), room.getRadius());
            }
        }
    }

    private void connectLinesToTileArray() {
        graphics2D.setColor(Tile.CORRIDOR.getColor());
        graphics2D.setStroke(new BasicStroke(1.5f));
        for (Edge edge : edges) {
            Point p1 = rooms[edge.getVertex1()].getCoordinates();
            Point p2 = rooms[edge.getVertex2()].getCoordinates();
            graphics2D.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
    }

    private void createDoors() {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int clr = image.getRGB(x, y);
                if (clr == Tile.CORRIDOR.getColor().getRGB()) {
                    if (image.getRGB(x + 1, y) == Tile.WALL.getColor().getRGB() && image.getRGB(x - 1, y) == Tile.WALL.getColor().getRGB()
                            || image.getRGB(x, y + 1) == Tile.WALL.getColor().getRGB() && image.getRGB(x, y - 1) == Tile.WALL.getColor().getRGB()) {
                        continue;
                    }
                    if (image.getRGB(x, y + 1) == Tile.FLOOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORVERTICAL.getColor().getRGB());
                    }
                    if (image.getRGB(x, y - 1) == Tile.FLOOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORVERTICAL.getColor().getRGB());
                    }
                    if (image.getRGB(x + 1, y) == Tile.FLOOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORHORIZONTAL.getColor().getRGB());
                    }
                    if (image.getRGB(x - 1, y) == Tile.FLOOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORHORIZONTAL.getColor().getRGB());
                    }
                }
            }
        }
    }

    private void soutTiles() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                System.out.printf("%3s", Tile.convertColorToTile(image.getRGB(x, y)).getCharacter());
            }
            System.out.println();
        }
        try {
            ImageIO.write(image, "png", new File("image.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean doesNeighborContain(Tile tile, int x, int y) {
        return image.getRGB(x, y + 1) == tile.getColor().getRGB() ||
                image.getRGB(x, y - 1) == tile.getColor().getRGB() ||
                image.getRGB(x + 1, y) == tile.getColor().getRGB() ||
                image.getRGB(x - 1, y) == tile.getColor().getRGB();
    }


    public static void main(String[] args) {
        WorldGenerator generator = new WorldGenerator(120, 80);
        generator.soutTiles();

    }
}
