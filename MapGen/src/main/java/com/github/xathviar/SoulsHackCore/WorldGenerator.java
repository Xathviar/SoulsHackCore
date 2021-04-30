package com.github.xathviar.SoulsHackCore;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;

public class WorldGenerator {
    private ArrayList<Room> rooms;
    private int roomsLength;
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
        rooms = new ArrayList<>();
        if ((bigI.intValue() & 1) == 1) {
            roomsLength = width * height / 200;
            createRooms();
        } else {
            roomsLength = width * height / 300;
            createRooms2();
        }
        fixRooms();
        connectRooms();
        connectLinesToTileArray();
        addRoomsToTileArray();
//        createDoors();
        populateRoom();
        drawWalls();
    }

    private void createRooms2() {
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int i = 0; i < roomsLength; i++) {
            double j = Math.PI * 2 * ((double) i / (double) roomsLength);
            rooms.add(Room.generateRandomRoom(halfWidth, halfHeight,
                    halfWidth + (int) (halfWidth * Math.sin(j) * (0.1 + ((PerlinScalar.pickByte(kernel2, i * 315) / 256f) * 0.9))),
                    halfHeight + (int) (halfHeight * Math.cos(j) * (0.1 + ((PerlinScalar.pickByte(kernel2, (i + 1) * 315) / 256f) * 0.9))),
                    kernel, i * 1000, 6.0));
        }
    }

    private void createRooms() {
        int[] rekursiv = new int[roomsLength];
        for (int i = 0; i < rekursiv.length; i++) {
            rekursiv[i] = i;
        }
        createRoom(5, rekursiv, width, height, 0, 0, 0);
    }

    private void createRoom(int abort, int[] rekursiv, int halfWidth, int halfHeight, int offsetWidth, int offsetHeight, int k) {
        if (rekursiv.length == 1) {
            if (PerlinScalar.pickByte(kernel4, k * 315) < 50) {
                return;
            }
            int i = rekursiv[0];
            double j = Math.PI * 2 * ((double) i / (double) roomsLength);
            rooms.add(Room.generateRandomRoom(halfWidth, halfHeight,
                    offsetWidth + (halfWidth / 2) + (int) ((halfWidth / 2) * Math.sin(j) * (PerlinScalar.pickByte(kernel2, i * 315) / 256f) * 0.9),
                    offsetHeight + (halfHeight / 2) + (int) ((halfHeight / 2) * Math.cos(j) * (PerlinScalar.pickByte(kernel2, (i + 1) * 315) / 256f) * 0.9)
                    , kernel, i * 1000, 1.0));
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
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = 0; j < rooms.size(); j++) {
                if (i != j) {
                    connections.add(new Edge(i, j, Room.connectRooms(rooms.get(i), rooms.get(j)).length));
                }
            }
        }
        edges.addAll(kruskal.kruskalMST(connections, rooms.size() * 2));
        connections.removeAll(edges);
        ArrayList<Edge> blackList = kruskal.kruskalMST(connections, rooms.size() * 2);
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
            Point p1 = rooms.get(edge.getVertex1() % rooms.size()).getCoordinates();
            Point p2 = rooms.get(edge.getVertex2() % rooms.size()).getCoordinates();
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
        for (int i = 0, j = 0; i < rooms.size(); i++, j++) {
            int flag = PerlinScalar.pickByte(kernel4, i * 315);
            if (flag % 3 == 0) {
                for (Tile t : new Tile[]{Tile.CHEST, Tile.TRAPFLOOR, Tile.BED}) {
                    if ((flag & t.ordinal()) > 0) {
                        Point point = getRandomCoordinateFromRoom(rooms.get(i), j++);
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
        for (int y = 0; y < height + 2; y++) {
            for (int x = 0; x < width + 2; x++) {
                Tile tile = Tile.convertColorToTile(image.getRGB(x, y));
                if (tile != Tile.WALL) {
                    continue;
                }
                int value = countNeighboursContainingTileAsByte(x, y, Tile.WALL, copy);
                image.setRGB(x, y, makeTileFromByte(value).getColor().getRGB());
            }
        }
    }

    private Tile makeTileFromByte(int value) {
        switch (value) {
            case 0:
                return Tile.PILLAR;
            case 1:
            case 69:
            case 101:
            case 128:
            case 160:
            case 224:
            case 2:
            case 8:
            case 3:
            case 6:
            case 7:
            case 9:
            case 12:
            case 13:
            case 16:
            case 17:
            case 20:
            case 21:
            case 32:
            case 33:
            case 34:
            case 35:
            case 38:
            case 39:
            case 40:
            case 41:
            case 48:
            case 49:
            case 50:
            case 51:
            case 64:
            case 96:
            case 97:
            case 132:
            case 133:
            case 136:
            case 140:
            case 141:
            case 144:
            case 161:
            case 162:
            case 163:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 172:
            case 173:
            case 176:
            case 177:
            case 196:
            case 180:
            case 181:
            case 192:
            case 193:
            case 197:
            case 229:
            case 37:
                return Tile.WALLEDGENORMAL;
            case 10:
            case 11:
            case 14:
            case 15:
            case 42:
            case 43:
            case 138:
            case 142:
            case 143:
            case 170:
            case 171:
            case 174:
            case 175:
            case 254:
            case 47:
            case 46:
                return Tile.WALLEDGERIGHTBOTTOM;
            case 18:
            case 19:
            case 22:
            case 23:
            case 146:
            case 147:
            case 150:
            case 151:
            case 178:
            case 179:
            case 182:
            case 183:
            case 55:
            case 54:
            case 251:
                return Tile.WALLEDGELEFTBOTTOM;
            case 24:
            case 25:
            case 28:
            case 29:
            case 31:
            case 56:
            case 57:
            case 60:
            case 61:
            case 63:
            case 152:
            case 153:
            case 156:
            case 157:
            case 159:
            case 184:
            case 185:
            case 188:
            case 189:
            case 248:
            case 249:
            case 252:
            case 253:
            case 191:
                return Tile.WALLHORIZONTAL;
            case 26:
            case 27:
            case 30:
            case 58:
            case 59:
            case 62:
            case 154:
            case 155:
            case 158:
            case 186:
            case 187:
            case 190:
            case 250:
                return Tile.WALLTTOP;
            case 36:
            case 44:
            case 45:
            case 52:
            case 53:
            case 68:
            case 100:
            case 164:
            case 228:
                return Tile.WALLDIAGONALDOWNUP;
            case 65:
            case 129:
            case 130:
            case 131:
            case 134:
            case 135:
            case 137:
            case 139:
            case 145:
            case 149:
            case 225:
                return Tile.WALLDIAGONALLUPDOWN;
            case 67:
            case 66:
            case 70:
            case 71:
            case 98:
            case 99:
            case 102:
            case 103:
            case 107:
            case 111:
            case 194:
            case 195:
            case 198:
            case 199:
            case 214:
            case 215:
            case 226:
            case 227:
            case 230:
            case 231:
            case 235:
            case 239:
            case 246:
            case 247:
                return Tile.WALLVERTICAL;
            case 72:
            case 73:
            case 76:
            case 77:
            case 104:
            case 105:
            case 108:
            case 109:
            case 200:
            case 201:
            case 204:
            case 205:
            case 232:
            case 233:
            case 236:
            case 237:
            case 223:
                return Tile.WALLEDGERIGHTTOP;
            case 74:
            case 75:
            case 78:
            case 79:
            case 106:
            case 110:
            case 202:
            case 203:
            case 206:
            case 207:
            case 222:
            case 234:
            case 238:
                return Tile.WALLTLEFT;
            case 80:
            case 81:
            case 84:
            case 85:
            case 112:
            case 113:
            case 117:
            case 127:
            case 148:
            case 208:
            case 209:
            case 212:
            case 213:
            case 240:
            case 241:
            case 244:
            case 245:
            case 116:
                return Tile.WALLEDGELEFTOP;
            case 82:
            case 83:
            case 86:
            case 87:
            case 114:
            case 115:
            case 118:
            case 119:
            case 210:
            case 211:
            case 242:
            case 243:
            case 123:
                return Tile.WALLTRIGHT;
            case 89:
            case 88:
            case 92:
            case 93:
            case 95:
            case 120:
            case 121:
            case 124:
            case 125:
            case 217:
            case 216:
            case 220:
            case 221:
                return Tile.WALLTBOTTOM;
            case 91:
            case 90:
            case 94:
            case 122:
            case 126:
            case 218:
            case 219:
                return Tile.WALLCROSS;
            case 255:
                return Tile.WALL;
        }
        System.out.println(value);
        return null;
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

    public int countNeighboursContainingTileAsByte(int x, int y, Tile tile, BufferedImage clonedImage) {
        int c = 0;
        int bit = 1;
        for (int iy = y - 1; iy <= y + 1; iy++) {
            for (int ix = x - 1; ix <= x + 1; ix++) {
                try {
                    if (ix == x && iy == y) {
                        continue;
                    }
                    if (Tile.convertColorToTile(clonedImage.getRGB(ix, iy)) == tile) {
                        c |= bit;
                    }
                } catch (Exception ignored) {
                }
                bit <<= 1;
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
        String dataS = generateTiledMap().toString();
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

    public StringBuilder generateTiledMap() {
        StringBuilder data = new StringBuilder();
        data.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        data.append("<map version=\"1.4\" tiledversion=\"1.4.3\" orientation=\"orthogonal\" renderorder=\"right-down\" width=\"").append(width + 2).append("\" height=\"").append(height + 2).append("\" tilewidth=\"32\" tileheight=\"32\" infinite=\"0\" nextlayerid=\"2\" nextobjectid=\"1\">\n");
        data.append(" <tileset firstgid=\"1\" source=\"ami8.tsx\"/>\n");
        data.append(" <layer id=\"1\" name=\"Floor\" width=\"").append(width + 2).append("\" height=\"").append(height + 2).append("\">\n");
        data.append("  <data encoding=\"csv\">\n");
        for (int x = 0; x < width + 2; x++) {
            for (int y = 0; y < height + 2; y++) {
                if (!(x == y && x == 0)) {
                    data.append(",");
                }
                try {
                    int i = (int) Tile.convertColorToTile(image.getRGB(y, x)).getCharacter();
                    if (i == 250 || i == 156 || i == 211) {
                        data.append(i);
                    } else {
                        data.append(1);
                    }
                } catch (NullPointerException e) {
                    data.append(1);
                }
            }
            data.append("\n");
        }
        data.append("</data>\n");
        data.append("</layer>\n");
        data.append(" <layer id=\"2\" name=\"Walls\" width=\"").append(width + 2).append("\" height=\"").append(height + 2).append("\">\n");
        data.append("  <data encoding=\"csv\">\n");
        for (int x = 0; x < width + 2; x++) {
            for (int y = 0; y < height + 2; y++) {
                if (!(x == y && x == 0)) {
                    data.append(",");
                }
                try {
                    int i = (int) Tile.convertColorToTile(image.getRGB(y, x)).getCharacter();
                    if (i != 250 && i != 156 && i != 211) {
                        data.append(i);
                    } else {
                        data.append(1);
                    }
                } catch (NullPointerException e) {
                    data.append(1);
                }
            }
            data.append("\n");
        }
        data.append("</data>\n");
        data.append("</layer>\n");
        data.append("</map>");
        return data;
    }

    public static void main(String[] args) {
        String seed = "TerefangIsInDaHood";
        WorldGenerator generator = new WorldGenerator(128, 128, seed);
        TileFontGenerator.exportImage(new File("image.png"), generator.getImage());
        generator.createTiledMap(new File("MapGen/src/main/resources/test2.tmx"));
    }
}
