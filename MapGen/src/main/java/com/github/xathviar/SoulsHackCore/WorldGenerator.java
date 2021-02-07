package com.github.xathviar.SoulsHackCore;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.Buffer;
import java.util.ArrayList;

public class WorldGenerator {
    private Room[] rooms;
    private int width;
    private int height;
    private BufferedImage image;
    private Graphics2D graphics2D;
    private int[] kernel;
    private int[] kernel2;
    private int[] kernel3;
    private int[] kernel4;
    private ArrayList<Edge> edges;

    public WorldGenerator(int width, int height, String seed) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_INT_RGB);
        this.graphics2D = image.createGraphics();
        graphics2D.setColor(Tile.WALL.getColor());
        graphics2D.fillRect(0, 0, width + 2, height + 2);
        BigInteger bigI = new BigInteger(DigestUtils.sha512(seed));
        kernel = PerlinScalar.permutation(bigI.intValue());
        kernel2 = PerlinScalar.permutation(bigI.shiftRight(32).intValue());
        kernel3 = PerlinScalar.permutation(bigI.shiftRight(64).intValue());
        kernel4 = PerlinScalar.permutation(bigI.shiftRight(96).intValue());
        edges = new ArrayList<>();
        if ((bigI.intValue() & 1) == 1) {
            rooms = new Room[width * height / 200];
            createRooms();
        } else {
            rooms = new Room[width * height / 300];
            createRooms2();
        }
        fixRooms();
        connectRooms();
        connectLinesToTileArray();
        addRoomsToTileArray();
        createDoors();
        populateRoom();
        drawWalls();
    }

    private void createRooms2() {
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int i = 0; i < rooms.length; i++) {
            double j = Math.PI * 2 * ((double) i / (double) rooms.length);
            rooms[i] = Room.generateRandomRoom(halfWidth, halfHeight,
                    halfWidth + (int) (halfWidth * Math.sin(j) * (0.1 + ((PerlinScalar.pickByte(kernel2, i * 315) / 256f) * 0.9))),
                    halfHeight + (int) (halfHeight * Math.cos(j) * (0.1 + ((PerlinScalar.pickByte(kernel2, (i + 1) * 315) / 256f) * 0.9))),
                    kernel, i * 1000, 6.0);
        }
    }

    private void createRooms() {
        int[] rekursiv = new int[rooms.length];
        for (int i = 0; i < rekursiv.length; i++) {
            rekursiv[i] = i;
        }
        createRoom(5, rekursiv, width, height, 0, 0, 0);
    }

    private void createRoom(int abort, int[] rekursiv, int halfWidth, int halfHeight, int offsetWidth, int offsetHeight, int k) {
        if (rekursiv.length == 1) {
            int i = rekursiv[0];
            double j = Math.PI * 2 * ((double) i / (double) rooms.length);
            rooms[i] = Room.generateRandomRoom(halfWidth, halfHeight,
                    offsetWidth + (halfWidth / 2) + (int) ((halfWidth / 2) * Math.sin(j) * (PerlinScalar.pickByte(kernel2, i * 315) / 256f) * 0.9),
                    offsetHeight + (halfHeight / 2) + (int) ((halfHeight / 2) * Math.cos(j) * (PerlinScalar.pickByte(kernel2, (i + 1) * 315) / 256f) * 0.9)
                    , kernel, i * 1000, 1.0);
            return;
        }
        if (rekursiv.length == 0) {
            return;
        }
        if (abort < 1) {
            for (int i : rekursiv) {
                createRoom(0, new int[]{i}, halfWidth, halfHeight, offsetWidth, offsetHeight, k++);
            }
        } else {
            int offset = rekursiv.length / 4;
            if (offset == 0) {
                offset++;
            }
            int[] split1 = ArrayUtils.subarray(rekursiv, 0, offset);
            int[] split2 = ArrayUtils.subarray(rekursiv, offset, offset * 2);
            int[] split3 = ArrayUtils.subarray(rekursiv, offset * 2, offset * 3);
            int[] split4 = ArrayUtils.subarray(rekursiv, offset * 3, rekursiv.length);
            abort--;
            createRoom(abort - (PerlinScalar.pickByte(kernel3, k++) & 1), split1, halfWidth / 2, halfHeight / 2, offsetWidth, offsetHeight, k++);
            createRoom(abort - (PerlinScalar.pickByte(kernel3, k++) & 1), split2, halfWidth / 2, halfHeight / 2, offsetWidth + (halfWidth / 2), offsetHeight, k++);
            createRoom(abort - (PerlinScalar.pickByte(kernel3, k++) & 1), split3, halfWidth / 2, halfHeight / 2, offsetWidth, offsetHeight + (halfHeight / 2), k++);
            createRoom(abort - (PerlinScalar.pickByte(kernel3, k++) & 1), split4, halfWidth / 2, halfHeight / 2, offsetWidth + (halfWidth / 2), offsetHeight + (halfHeight / 2), k++);
        }
    }

    private void fixRooms() {
        for (Room room : rooms) {
            while (room.getCoordinates().getX() + room.getWidth() / 2 >= width) {
                room.setWidth(room.getWidth() - 1);
            }
            while (room.getCoordinates().getY() + room.getHeight() / 2 >= height) {
                room.setHeight(room.getHeight() - 1);
            }
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
        edges.addAll(kruskal.kruskalMST(connections, rooms.length * 2));
        connections.removeAll(edges);
        ArrayList<Edge> blackList = kruskal.kruskalMST(connections, rooms.length * 2);
        connections.removeAll(blackList);
        edges.addAll(blackList);
    }

    private void addRoomsToTileArray() {
        graphics2D.setColor(Tile.FLOOR.getColor());
        for (Room room : rooms) {
            if (room.getRadius() == 0) {
                graphics2D.fillRect(room.getCoordinates().getX() - (room.getWidth() / 2)
                        , room.getCoordinates().getY() - (room.getHeight() / 2), room.getWidth(), room.getHeight());
            } else {
                graphics2D.fillRoundRect(room.getCoordinates().getX() - (room.getRadius())
                        , room.getCoordinates().getY() - (room.getRadius())
                        , room.getRadius() * 2, room.getRadius() * 2, room.getRadius() * 2, room.getRadius() * 2);
            }
        }
    }

    private void connectLinesToTileArray() {
        graphics2D.setColor(Tile.CORRIDOR.getColor());
        graphics2D.setStroke(new BasicStroke(1.5f));
        for (Edge edge : edges) {
            Point p1 = rooms[edge.getVertex1() % rooms.length].getCoordinates();
            Point p2 = rooms[edge.getVertex2() % rooms.length].getCoordinates();
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
                    if (image.getRGB(x, y + 1) == Tile.FLOOR.getColor().getRGB() && image.getRGB(x, y - 1) == Tile.CORRIDOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORCLOSED.getColor().getRGB());
                    } else if (image.getRGB(x, y - 1) == Tile.FLOOR.getColor().getRGB() && image.getRGB(x, y + 1) == Tile.CORRIDOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORCLOSED.getColor().getRGB());
                    } else if (image.getRGB(x + 1, y) == Tile.FLOOR.getColor().getRGB() && image.getRGB(x - 1, y) == Tile.CORRIDOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORCLOSED.getColor().getRGB());
                    } else if (image.getRGB(x - 1, y) == Tile.FLOOR.getColor().getRGB() && image.getRGB(x + 1, y) == Tile.CORRIDOR.getColor().getRGB()) {
                        image.setRGB(x, y, Tile.DOORCLOSED.getColor().getRGB());
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

    private void populateRoom() {
        for (int i = 0, j = 0; i < rooms.length; i++, j++) {
            int flag = PerlinScalar.pickByte(kernel4, i * 315);
            if (flag % 3 == 0) {
                for (Tile t : new Tile[]{Tile.CHEST, Tile.TRAPFLOOR, Tile.BED}) {
                    if ((flag & t.ordinal()) > 0) {
                        Point point = getRandomCoordinateFromRoom(rooms[i], j++);
                        image.setRGB(point.getX(), point.getY(), t.getColor().getRGB());
                    }
                }
            }
        }
    }

    private Point getRandomCoordinateFromRoom(Room room, int j) {
        int x, y;
        if (room.getRadius() > 0) {
            double d = Math.PI * 2f * (PerlinScalar.pickByte(kernel4, (j++) * 315) / 256f);
            x = (int) (Math.sin(d) * (room.getRadius() / 2f)) + room.getCoordinates().getX();
            y = (int) (Math.cos(d) * (room.getRadius() / 2f)) + room.getCoordinates().getY();
        } else {
            x = (int) ((PerlinScalar.pickByte(kernel4, (j++) * 315) / 256f) * (room.getWidth())) + room.getCoordinates().getX() - (room.getWidth() / 2);
            y = (int) ((PerlinScalar.pickByte(kernel4, (j++) * 315) / 256f) * (room.getHeight())) + room.getCoordinates().getY() - (room.getHeight() / 2);
        }
        return new Point(x, y);
    }

    private void drawWalls() {
        BufferedImage copy = copyImage(image);
        for (int x = 0; x < width + 2; x++) {
            for (int y = 0; y < height + 1; y++) {
                Tile tile = Tile.convertColorToTile(copy.getRGB(x, y));
                try {
                    if (tile != Tile.WALL) {
                        continue;
                    }
                    Tile belowNeighbor = Tile.convertColorToTile(copy.getRGB(x, y + 1));
                    if (belowNeighbor != Tile.WALL) {
                        image.setRGB(x, y, Tile.WALLVERTICAL.getColor().getRGB());
                    }
                    Tile aboveNeighbor = Tile.convertColorToTile(copy.getRGB(x, y - 1));
                    if (aboveNeighbor != Tile.WALL) {
                        image.setRGB(x, y, Tile.WALLVERTICAL.getColor().getRGB());
                    }
                } catch (Exception e) {

                }
            }
        }
        for (int y = 0; y < height + 2; y++) {
            for (int x = 0; x < width + 1; x++) {
                Tile tile = Tile.convertColorToTile(copy.getRGB(x, y));
                try {
                    if (tile != Tile.WALL) {
                        continue;
                    }
                    Tile belowNeighbor = Tile.convertColorToTile(copy.getRGB(x + 1, y));
                    if (belowNeighbor != Tile.WALL) {
                        image.setRGB(x, y, Tile.WALLHORIZONTAL.getColor().getRGB());
                    }
                    Tile aboveNeighbor = Tile.convertColorToTile(copy.getRGB(x - 1, y));
                    if (aboveNeighbor != Tile.WALL) {
                        image.setRGB(x, y, Tile.WALLHORIZONTAL.getColor().getRGB());
                    }
                } catch (Exception e) {

                }
            }
        }
        for (int x = 0; x < width + 1; x++) {
            for (int y = 0; y < height + 1; y++) {
                Tile tile = Tile.convertColorToTile(copy.getRGB(x, y));
                try {
                    if (tile != Tile.WALL) {
                        continue;
                    }
                    int count = countNeighboursContainingTile(x, y, Tile.WALL, copy);
                    if (count == 1 || count == 5) {
                        image.setRGB(x, y, Tile.WALLEDGENORMAL.getColor().getRGB());
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    public int countNeighboursContainingTile(int x, int y, Tile tile, BufferedImage clonedImage) {
        int c = 0;
        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iy = y - 1; iy <= y + 1; iy++) {
                try {
                    if (ix == x && iy == y) {
                        continue;
                    }
                    if (Tile.convertColorToTile(clonedImage.getRGB(ix, iy)) != tile) {
                        c++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return c;
    }


    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void createTiledMap(File outFile) {
        StringBuilder data = new StringBuilder();
        data.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        data.append("<map version=\"1.4\" tiledversion=\"1.4.3\" orientation=\"orthogonal\" renderorder=\"right-down\" width=\"").append(width + 2).append("\" height=\"").append(height + 2).append("\" tilewidth=\"8\" tileheight=\"8\" infinite=\"0\" nextlayerid=\"2\" nextobjectid=\"1\">\n");
        data.append(" <tileset firstgid=\"1\" source=\"ami8.tsx\"/>\n");
        data.append(" <layer id=\"1\" name=\"Tile Layer 1\" width=\"").append(width + 2).append("\" height=\"").append(height + 2).append("\">\n");
        data.append("  <data encoding=\"csv\">\n");
        for (int x = 0; x < width + 2; x++) {
            for (int y = 0; y < height + 2; y++) {
                if (!(x == y && x == 0)) {
                    data.append(",");
                }
                try {
                    data.append((int) Tile.convertColorToTile(image.getRGB(x, y)).getCharacter());
                } catch (NullPointerException e) {
                    data.append(0);
                }
            }
            data.append("\n");
        }
        data.append("</data>\n");
        data.append("</layer>\n");
        data.append("</map>");
        String dataS = data.toString();
        try (FileOutputStream fos = new FileOutputStream(outFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] bytes = dataS.getBytes();
            bos.write(bytes);
            bos.close();
            fos.close();
            System.out.print("Data written to file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String seed = "WirdDiyarDia? Natürlich!!!";
        WorldGenerator generator = new WorldGenerator(128, 128, seed);
        generator.soutTiles();
        TileFontGenerator.exportImage(new File("image.png"), generator.getImage());
        generator.createTiledMap(new File("MapGen/src/main/resources/test2.tmx"));
    }
}
