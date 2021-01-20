package com.github.xathviar.SoulsHackCore;

import java.awt.*;


public enum Tile {
    FLOOR('.', Color.CYAN),
    CORRIDOR('.', Color.RED),
    WALL('#', Color.WHITE),
    DOORCLOSED('▓', Color.GREEN),
    DOOROPEN('░', Color.LIGHT_GRAY),
    HIDDENDOOR('#', Color.GRAY),
    CHEST('©', Color.MAGENTA),
    TRAPFLOOR(':', Color.ORANGE),
    STAIRSDOWN('<', new Color(254, 1, 1)),
    STAIRSUP('>', new Color(2, 254, 2)),
    BED('~', new Color(2,2,254));

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
