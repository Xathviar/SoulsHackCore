package com.github.xathviar.SoulsHackCore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

public class TileFontGenerator {

    public static final void createFromGDFB(File outFile, InputStream ins, boolean extendedFormat) throws IOException {
        /* Only supports a architecture-dependent binary dump format
         * at the moment.
         * The file format is like this on machines with 32-byte integers:
         *
         * byte 0-3:   (int) number of characters in the font
         * byte 4-7:   (int) value of first character in the font (often 32, space)
         * byte 8-11:  (int) pixel width of each character
         * byte 12-15: (int) pixel height of each character
         * bytes 16-:  (char) array with character data, one byte per pixel
         *                    in each character, for a total of
         *                    (nchars*width*height) bytes.
         */
        /*---
         *
         *  extended format has byte[numofChars] charAdvanceWidth @ 0x10
         *  character data follows and is considered greyscale (anti-aliased)
         *  but 0x01 is considered full color in lecacy/interim mode.
         *
         */
        EndianDataInputStream edis = new EndianDataInputStream(ins);
        edis.order(ByteOrder.BIG_ENDIAN);
        int numChars = edis.readInt();
        if (numChars != (numChars & 0xffff)) {
            edis.order(ByteOrder.LITTLE_ENDIAN);
            numChars = ((numChars >>> 24) & 0xff) | ((numChars >>> 8) & 0xff00);
        }

        int startChar = edis.readInt();
        int charW = edis.readInt();
        int charH = edis.readInt();

        int charWstep = nextPowerOf2(charW);
        int charHstep = nextPowerOf2(charH);

        int numpChars = nextPowerOf2(numChars);
        int totalArea = nextPowerOf2(charHstep * charWstep * numpChars);
        int texRes = Integer.numberOfTrailingZeros(totalArea) + 1;
        texRes /= 2;
        texRes = 1 << texRes;
        int charPerRow = texRes / charWstep;

        BufferedImage pixmap = new BufferedImage(texRes, texRes, BufferedImage.TYPE_BYTE_GRAY);

//         ext format contains real char widths in front of bitmap data
        if (extendedFormat) {
            int[] charAdvance = new int[numChars];
            for (int ci = 0; ci < numChars; ci++) {
                charAdvance[ci] = (edis.readByte() & 0xff);
            }
        }

        int ci = 0;
        for (int c = startChar; c < (startChar + numChars); c++) {
            int srcX = (c % charPerRow) * charWstep;
            int srcY = (c / charPerRow) * charHstep;

            // ext format is grey-scale
            if (extendedFormat) {
                for (int y = 0; y < charH; y++) {
                    for (int x = 0; x < charW; x++) {
                        ;
                        long p = edis.readByte() & 0xff;
                        if (p == 1L) {
                            /* there is no good grey-scale at 0x01 */
                            /* so set to full color for legacy/interim format */
                            p = 0xFFFFffffL;
                        } else {
                            p |= (p << 8);
                            p |= (p << 16);
                        }
                        pixmap.setRGB(srcX + x, srcY + y, (int) (p & 0xFFFFffff));
                    }
                }
            } else {
                for (int y = 0; y < charH; y++) {
                    for (int x = 0; x < charW; x++) {
                        int b = edis.readByte() & 0xff;
                        pixmap.setRGB(srcX + x, srcY + y, (b == 0 ? 0 : 0xFFFFffff));
                    }
                }
            }
            ci++;
        }
        edis.close();
        exportImage(outFile, pixmap);
    }


    public static void main(String[] args) throws IOException {
//        createFromGDFB(new File("test1.png"), new GZIPInputStream(new FileInputStream("MapGen/src/main/resources/C_8x8_LE.gdf.gz")), false);
//        createFromGDFB(new File("ami8.png"), new FileInputStream("MapGen/src/main/resources/ami8.gdf"), false);
        System.out.println("Hello World!");
    }

    public static class EndianDataInputStream extends InputStream implements DataInput {
        DataInputStream dataIn;
        private ByteBuffer buffer = ByteBuffer.allocate(8);
        ByteOrder order = ByteOrder.BIG_ENDIAN;

        public EndianDataInputStream(InputStream stream) {
            dataIn = new DataInputStream(stream);
        }

        public EndianDataInputStream order(ByteOrder o) {
            order = o;
            return this;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return dataIn.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return dataIn.read(b, off, len);
        }

        @Deprecated
        @Override
        public String readLine() throws IOException {
            return dataIn.readLine();
        }

        @Override
        public boolean readBoolean() throws IOException {
            return dataIn.readBoolean();
        }

        @Override
        public byte readByte() throws IOException {
            return dataIn.readByte();
        }

        @Override
        public int read() throws IOException {
            return readByte();
        }

        @Override
        public boolean markSupported() {
            return dataIn.markSupported();
        }

        @Override
        public void mark(int readlimit) {
            dataIn.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            dataIn.reset();
        }

        @Override
        public char readChar() throws IOException {
            return dataIn.readChar();
        }

        @Override
        public void readFully(byte[] b) throws IOException {
            dataIn.readFully(b);
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            dataIn.readFully(b, off, len);
        }

        @Override
        public String readUTF() throws IOException {
            return dataIn.readUTF();
        }

        @Override
        public int skipBytes(int n) throws IOException {
            return dataIn.skipBytes(n);
        }

        @Override
        public double readDouble() throws IOException {
            long tmp = readLong();
            return Double.longBitsToDouble(tmp);
        }

        @Override
        public float readFloat() throws IOException {
            int tmp = readInt();
            return Float.intBitsToFloat(tmp);
        }

        @Override
        public int readInt() throws IOException {
            buffer.clear();
            buffer.order(ByteOrder.BIG_ENDIAN)
                    .putInt(dataIn.readInt())
                    .flip();
            return buffer.order(order).getInt();
        }

        @Override
        public long readLong() throws IOException {
            buffer.clear();
            buffer.order(ByteOrder.BIG_ENDIAN)
                    .putLong(dataIn.readLong())
                    .flip();
            return buffer.order(order).getLong();
        }

        @Override
        public short readShort() throws IOException {
            buffer.clear();
            buffer.order(ByteOrder.BIG_ENDIAN)
                    .putShort(dataIn.readShort())
                    .flip();
            return buffer.order(order).getShort();
        }

        @Override
        public int readUnsignedByte() throws IOException {
            return (int) dataIn.readByte();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return (int) readShort();
        }
    }

    public static int nextPowerOf2(int a) {
        int b = 1;
        while (b < a) {
            b <<= 1;
        }
        return b;
    }

    public static void exportImage(File path, BufferedImage image) {
        try {
            ImageIO.write(image, "png", path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
