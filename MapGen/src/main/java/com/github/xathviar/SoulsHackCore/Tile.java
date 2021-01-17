package com.github.xathviar.SoulsHackCore;

import java.awt.*;


public enum Tile {
    FLOOR('.', Color.CYAN),
    DARKNESS('#', Color.WHITE);

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
