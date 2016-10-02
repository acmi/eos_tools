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

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class Decrypt {
    public static final byte[] EOS_HASH_DATA = {(byte) 0xFF, (byte) 0x41, (byte) 0x54, (byte) 0x73, (byte) 0x84, (byte) 0x9A, (byte) 0xC8, (byte) 0xA6};
    public static final byte[] XOR_KEY = {(byte) 0x88, (byte) 0xFF, (byte) 0xA7, (byte) 0xA4, (byte) 0x23, (byte) 0x78, (byte) 0x44, (byte) 0xA0};

    public static byte[] generateKey() {
        try {
            return MessageDigest.getInstance("MD5").digest(EOS_HASH_DATA);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher cipher;

    static {
        try {
            cipher = Cipher.getInstance("RC4");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(generateKey(), "RC4"));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static void decrypt(byte[] block, int offset, int size) throws GeneralSecurityException {
        cipher.doFinal(block, offset, size, block, offset);

        for (int i = 0; i < size; i++) {
            int o = offset + i;
            if (block[o] == 127)
                block[o] = 0;
            else if (block[o] == -128)
                block[o] = -1;
            else {
                if (block[o] >= 0)
                    block[o]++;
                else
                    block[o]--;
            }
            block[o] ^= XOR_KEY[i];
        }
    }

    public static void processFileOrFolder(File file) {
        if (file.isDirectory()) {
            for (File sub : Optional.ofNullable(file.listFiles()).orElse(new File[0]))
                processFileOrFolder(sub);
        } else {
            processFile(file);
        }
    }

    private static void processFile(File file) {
        if (file.length() < 4)
            return;

        byte[] data;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (dis.readInt() != 0xcca0b7d0)
                return;

            int size = (int) file.length() - 4;
            data = new byte[size];
            dis.readFully(data);
        } catch (IOException e) {
            System.err.println("Couldn't open " + file + ": " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try {
            for (int i = 0; i < (data.length & 0xFFFFFFF8); i += 8) {
                decrypt(data, i, 8);
            }
        } catch (GeneralSecurityException e) {
            System.err.println("Decrypt error: " + file);
            return;
        }

        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(file))) {
            fos.write(data);
        } catch (IOException e) {
            System.err.println("Couldn't write " + file + ": " + e.getMessage());
            return;
        }

        System.out.println("Decrypted: " + file);
    }

    public static void main(String[] args) {
        processFileOrFolder(new File(args.length == 0 ? "" : args[0]).getAbsoluteFile());
    }
}
