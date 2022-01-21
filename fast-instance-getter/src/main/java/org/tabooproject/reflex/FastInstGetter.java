package org.tabooproject.reflex;

import java.io.ByteArrayOutputStream;

/**
 * TabooLib用instance获取器
 * 采用纯粹的byte数组操作动态生成类
 *
 * @author YiMiner
 */
public class FastInstGetter extends ClassLoader {

    public interface IGetter {
        Object getInstance();
    }

    private static final byte[] SECTION_1 = {-54, -2, -70, -66, 0, 0, 0, 52, 0, 26, 10, 0, 4, 0, 17, 9, 0, 18, 0, 19, 7,
            0, 20, 7, 0, 21, 7, 0, 22, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100,
            101, 1, 0, 15, 76, 105, 110, 101, 78, 117, 109, 98, 101, 114, 84, 97, 98, 108, 101, 1, 0, 18, 76, 111, 99,
            97, 108, 86, 97, 114, 105, 97, 98, 108, 101, 84, 97, 98, 108, 101, 1, 0, 4, 116, 104, 105, 115, 1, 0, 31,
            76, 116, 97, 98, 111, 111, 108, 105, 98, 47, 99, 111, 109, 109, 111, 110, 47, 105, 111, 47, 71, 101, 116,
            116, 101, 114, 73, 109, 112, 108, 59, 1, 0, 11, 103, 101, 116, 73, 110, 115, 116, 97, 110, 99, 101, 1, 0,
            20, 40, 41, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 59, 1, 0, 10, 83,
            111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 15, 71, 101, 116, 116, 101, 114, 73, 109, 112, 108, 46,
            106, 97, 118, 97, 12, 0, 6, 0, 7, 7, 0, 23, 12, 0, 24, 0, 25, 1, 0, 29, 116, 97, 98, 111, 111, 108, 105, 98,
            47, 99, 111, 109, 109, 111, 110, 47, 105, 111, 47, 71, 101, 116, 116, 101, 114, 73, 109, 112, 108, 1, 0, 16,
            106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 1};
    private static final byte[] SECTION_3 = {0, 33, 0, 3, 0, 4, 0, 1, 0, 5, 0, 0, 0, 2, 0, 1, 0, 6, 0, 7, 0, 1, 0, 8, 0,
            0, 0, 47, 0, 1, 0, 1, 0, 0, 0, 5, 42, -73, 0, 1, -79, 0, 0, 0, 2, 0, 9, 0, 0, 0, 6, 0, 1, 0, 0, 0, 5, 0, 10,
            0, 0, 0, 12, 0, 1, 0, 0, 0, 5, 0, 11, 0, 12, 0, 0, 0, 1, 0, 13, 0, 14, 0, 1, 0, 8, 0, 0, 0, 46, 0, 1, 0, 1,
            0, 0, 0, 4, -78, 0, 2, -80, 0, 0, 0, 2, 0, 9, 0, 0, 0, 6, 0, 1, 0, 0, 0, 8, 0, 10, 0, 0, 0, 12, 0, 1, 0, 0,
            0, 4, 0, 11, 0, 12, 0, 0, 0, 1, 0, 15, 0, 0, 0, 2, 0, 16};
    private IGetter getter = null;


    public FastInstGetter(String className, boolean isCompanion) {
        className = className.replace('.', '/');
        String interfaceName = IGetter.class.getName().replace('.', '/');
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String memberName = isCompanion ? "Companion" : "INSTANCE";
        try {
            outputStream.write(SECTION_1);
            outputStream.write(encode(interfaceName));
            outputStream.write(1);
            outputStream.write(encode(className));
            outputStream.write(1);
            outputStream.write(encode(memberName));
            outputStream.write(1);
            outputStream.write(encode("L" + className + (isCompanion ? "$Companion;" : ";")));
            outputStream.write(SECTION_3);
            byte[] synthetic = outputStream.toByteArray();
            Class<?> clazz = this.defineClass("taboolib.common.io.GetterImpl", synthetic, 0, synthetic.length);
            getter = (IGetter) clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] encode(String stringValue) {
        int charLength = stringValue.length();
        if (charLength > 65535) {
            throw new IllegalArgumentException("UTF8 string too large");
        } else {
            byte[] data = new byte[2 + charLength];
            int index = 0;
            data[index++] = (byte) (charLength >>> 8);
            data[index++] = (byte) (charLength);
            for (int i = 0; i < charLength; ++i) {
                char charValue = stringValue.charAt(i);
                if (charValue < 1 || charValue > 127) {
                    return encodeUtf8(data, stringValue, i);
                }
                data[index++] = ((byte) charValue);
            }
            return data;
        }
    }

    private static byte[] enlarge(byte[] origin, int size) {
        int minimalCapacity = origin.length + size;
        byte[] newData = new byte[minimalCapacity];
        System.arraycopy(origin, 0, newData, 0, origin.length);
        return newData;
    }

    public static byte[] encodeUtf8(byte[] data, String stringValue, int offset) {
        int charLength = stringValue.length();
        int byteLength = offset;

        int byteLengthOffset;
        for (byteLengthOffset = offset; byteLengthOffset < charLength; ++byteLengthOffset) {
            char charValue = stringValue.charAt(byteLengthOffset);
            if (charValue >= 1 && charValue <= 127) {
                ++byteLength;
            } else if (charValue <= 2047) {
                byteLength += 2;
            } else {
                byteLength += 3;
            }
        }

        if (byteLength > 65535) {
            throw new IllegalArgumentException("UTF8 string too large");
        } else {
            data[0] = (byte) (byteLength >>> 8);
            data[1] = (byte) byteLength;
            if (data.length + byteLength - offset > data.length) {
                data = enlarge(data, byteLength + 2 - data.length);
            }
            int currentLength = offset + 2;
            for (int i = offset; i < charLength; ++i) {
                char charValue = stringValue.charAt(i);
                if (charValue >= 1 && charValue <= 127) {
                    data[currentLength++] = (byte) charValue;
                } else if (charValue <= 2047) {
                    data[currentLength++] = (byte) (192 | charValue >> 6 & 31);
                    data[currentLength++] = (byte) (128 | charValue & 63);
                } else {
                    data[currentLength++] = (byte) (224 | charValue >> 12 & 15);
                    data[currentLength++] = (byte) (128 | charValue >> 6 & 63);
                    data[currentLength++] = (byte) (128 | charValue & 63);
                }
            }
            return data;
        }
    }

    public Object getInstance() {
        if (getter == null) {
            return null;
        } else {
            return getter.getInstance();
        }
    }
}