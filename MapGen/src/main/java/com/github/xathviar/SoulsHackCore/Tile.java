package com.github.xathviar.SoulsHackCore;

import java.awt.*;


public enum Tile {
    FLOOR((char) 250, Color.CYAN),
    CORRIDOR((char) 250, Color.RED),
    WALL((char) 0, Color.WHITE),
    WALLHORIZONTAL((char) 197, new Color(255, 197, 0)),
    WALLVERTICAL((char) 180, new Color(255, 180, 0)),
    WALLEDGELEFTOP((char) 219, new Color(255, 219, 0)),
    WALLEDGELEFBOTTOM((char) 193, new Color(255, 193, 0)),
    WALLEDGERIGHTTOP((char) 192, new Color(255, 192, 0)),
    WALLEDGERIGHTBOTTOM((char) 218, new Color(255, 218, 0)),
    WALLEDGENORMAL((char) 10, new Color(255, 10, 0)),
    DOORCLOSED((char) 198, Color.GREEN),
    DOOROPENVERTICAL((char) 197, Color.LIGHT_GRAY),
    DOOROPENHORIZONTAL((char) 180, Color.LIGHT_GRAY),
    HIDDENDOORHORIZONTAL((char) 197, Color.GRAY),
    HIDDENDOORVERTICAL((char) 180, Color.GRAY),
    CHEST((char) 156, Color.MAGENTA),
    TRAPFLOOR((char) 250, Color.ORANGE),
    STAIRSDOWN((char) 26, new Color(254, 1, 1)),
    STAIRSUP((char) 25, new Color(2, 254, 2)),
    BED((char) 211, new Color(2, 2, 254));

    private char character;
    private Color color;

    Tile(char character, Color color) {
        this.character = character;
        this.color = color;
    }

    public char getCharacter() {
        return character;
    }

    public Color getColor() {
        return color;
    }

    public static Tile convertColorToTile(int color) {
        Tile[] tiles = Tile.class.getEnumConstants();
        for (Tile tile : tiles) {
            if (tile.color.getRGB() == color) {
                return tile;
            }
        }
        return null;
    }
}