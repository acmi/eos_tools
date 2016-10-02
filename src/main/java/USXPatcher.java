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

import acmi.l2.clientmod.io.UnrealPackage;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;

public class USXPatcher {
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
            } catch (Exception e) {
                System.err.println("Couldn't open " + file + ": " + e.getMessage());
                return;
            }

            try (UnrealPackage up = new UnrealPackage(file, false)) {
                if (up.importReferenceByName("Engine.StaticMesh", c -> c.equalsIgnoreCase("Core.Class")) == 0)
                    return;
            } catch (Exception e) {
                System.err.println("Invalid unreal package " + file + ": " + e.getMessage());
                return;
            }

            processFile(file);
        }
    }

    private static void processFile(File file) {
        try (UnrealPackage up = new UnrealPackage(file, false)) {
            up.getFile().setPosition(16);
            int nameStart = up.getFile().readInt();

            for (UnrealPackage.ExportEntry entry : up.getExportTable()) {
                if (!entry.getFullClassName().equalsIgnoreCase("Engine.StaticMesh"))
                    continue;

                byte[] data = entry.getObjectRawData();
                byte[] newData = new byte[data.length + 21];

                ByteBuffer dataBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                ByteBuffer newDataBuffer = ByteBuffer.wrap(newData).order(ByteOrder.LITTLE_ENDIAN);

                PropertiesUtil.iterateProperties(dataBuffer, up, p -> {
                });
                byte[] box = new byte[25];
                dataBuffer.get(box);
                dataBuffer.position(dataBuffer.position() + 16);

                newDataBuffer.put(data, 0, dataBuffer.position());

                dataBuffer.position(dataBuffer.position() + 4);

                int pos = dataBuffer.position();
                int size = getCompactInt(dataBuffer);
                dataBuffer.position(dataBuffer.position() + 14 * size);
                newDataBuffer.put(data, pos, dataBuffer.position() - pos);

                newDataBuffer.put(box);

                newDataBuffer.put(data, dataBuffer.position(), dataBuffer.limit() - dataBuffer.position());

                entry.setObjectRawData(newData);
            }

            up.getDataStartOffset().ifPresent(start -> {
                up.getFile().setPosition(nameStart);
                up.getFile().writeBytes(new byte[start - nameStart]);
            });

            System.out.println("Patched: " + file);
        } catch (Throwable e) {
            System.err.println(file);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        processFileOrFolder(new File(args.length == 0 ? "" : args[0]).getAbsoluteFile());
    }
}
