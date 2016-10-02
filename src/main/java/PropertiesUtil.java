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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;

public class PropertiesUtil {
    public static class Property {
        public String name;
        public Type type;
        public String struct;
        public boolean array;
        public int arrayIndex;
        public int pos;
        public ByteBuffer data;

        public Property(String name, Type type, String struct, boolean array, int arrayIndex, int pos, ByteBuffer data) {
            this.name = name;
            this.type = type;
            this.struct = struct;
            this.array = array;
            this.arrayIndex = arrayIndex;
            this.pos = pos;
            this.data = data;
        }
    }

    public static void iterateProperties(ByteBuffer buffer, UnrealPackage up, Consumer<Property> iterator) throws BufferUnderflowException {
        String name;
        while (!"None".equals(name = up.nameReference(getCompactInt(buffer)))) {
            byte info = buffer.get();
            Type type = Type.values()[info & 15];
            int size = (info & 112) >> 4;
            boolean array = (info & 128) == 128;
            String struct = type == Type.STRUCT ? up.nameReference(getCompactInt(buffer)) : null;
            size = getSize(size, buffer);
            int arrayIndex = array && type != Type.BOOL ? getCompactInt(buffer) : 0;

            byte[] obj = new byte[size];
            int pos = buffer.position();
            buffer.get(obj);

            iterator.accept(new Property(name, type, struct, array, arrayIndex, pos, ByteBuffer.wrap(obj).order(ByteOrder.LITTLE_ENDIAN)));
        }
    }

    public static int getSize(int size, ByteBuffer buffer) throws BufferUnderflowException {
        switch (size) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 12;
            case 4:
                return 16;
            case 5:
                return buffer.get() & 0xFF;
            case 6:
                return buffer.getShort() & 0xFFFF;
            case 7:
                return buffer.getInt();
        }
        throw new RuntimeException("invalid size " + size);
    }

    public enum Type {
        NONE,
        BYTE,
        INT,
        BOOL,
        FLOAT,
        OBJECT,
        NAME,
        _DELEGATE,
        _CLASS,
        ARRAY,
        STRUCT,
        _VECTOR,
        _ROTATOR,
        STR,
        _MAP,
        _FIXED_ARRAY
    }
}
