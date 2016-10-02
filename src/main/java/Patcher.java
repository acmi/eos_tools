/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.io.DataInput;

import java.io.*;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Optional;

public class Patcher {
    private static DataInput wrap(RandomAccessFile raf) {
        return new DataInput() {
            @Override
            public int readUnsignedByte() throws UncheckedIOException {
                try {
                    return raf.readUnsignedByte();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public int readInt() throws UncheckedIOException {
                try {
                    return Integer.reverseBytes(raf.readInt());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void readFully(byte[] b, int off, int len) throws UncheckedIOException {
                try {
                    raf.readFully(b, off, len);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public Charset getCharset() {
                return UnrealPackage.getDefaultCharset();
            }

            @Override
            public int getPosition() throws UncheckedIOException {
                try {
                    return (int) raf.getFilePointer();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    public static void processFileOrFolder(File file) {
        if (file.isDirectory()) {
            for (File sub : Optional.ofNullable(file.listFiles()).orElse(new File[0]))
                processFileOrFolder(sub);
        } else {
            if (file.length() < 4)
                return;

            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                if (dis.readInt() != 0xc1832a9e)
                    return;
                long version = dis.readInt() & 0xFFFFFFFFL;
                if (version < 0x81003100L)
                    return;
                if (version <= 0x81003200L) {
                    processFile0(file);
                } else {
                    processFile1(file);
                }
            } catch (IOException e) {
                System.err.println("Couldn't open " + file + ": " + e.getMessage());
            }
        }
    }

    private static void processFile0(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(0x0c);
            int nsize = Integer.reverseBytes(raf.readInt());
            int noff = Integer.reverseBytes(raf.readInt());

            raf.seek(0x24);
            int size = Integer.reverseBytes(raf.readInt());
            int[][] generations = new int[size][2];
            for (int i = 0; i < size; i++) {
                generations[i][0] = raf.readInt();
                generations[i][1] = raf.readInt();
            }
            if (raf.getFilePointer() != noff)
                throw new IOException("WTF");
            DataInput di = wrap(raf);
            for (int i = 0; i < nsize; i++) {
                di.readLine();
                di.readInt();
            }
            byte[] nametable = new byte[di.getPosition() - noff];
            raf.seek(noff);
            raf.readFully(nametable);
            raf.seek(noff);
            raf.write(new byte[nametable.length]);
            raf.seek(0x24);
            raf.write(new byte[16]);
            raf.writeInt(Integer.reverseBytes(size));
            for (int i = 0; i < size; i++) {
                raf.writeInt(generations[i][0]);
                raf.writeInt(generations[i][1]);
            }
            raf.seek(0x10);
            raf.writeInt(Integer.reverseBytes((int) raf.length()));
            raf.seek(raf.length());
            raf.write(nametable);

            System.out.println("Patched: " + file);
        } catch (IOException e) {
            System.err.println("Couldn't write " + file + ": " + e.getMessage());
        } catch (Throwable e) {
            System.err.println(file);
            e.printStackTrace();
        }
    }

    private static void processFile1(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(0x24);
            int size = Integer.reverseBytes(raf.readInt());
            int[][] generations = new int[size][2];
            for (int i = 0; i < size; i++) {
                generations[i][0] = raf.readInt();
                generations[i][1] = raf.readInt();
            }
            byte[] guid = new byte[16];
            raf.readFully(guid);

            raf.seek(0x24);
            raf.write(guid);
            raf.writeInt(Integer.reverseBytes(size));
            for (int i = 0; i < size; i++) {
                raf.writeInt(generations[i][0]);
                raf.writeInt(generations[i][1]);
            }
            System.out.println("Patched: " + file);
        } catch (IOException e) {
            System.err.println("Couldn't write " + file + ": " + e.getMessage());
        } catch (Throwable e) {
            System.err.println(file);
            while (e.getCause() != null)
                e = e.getCause();
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        processFileOrFolder(new File(args.length == 0 ? "" : args[0]).getAbsoluteFile());
    }
}
