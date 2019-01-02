package com.qimeng.jace.dapingji;

import java.util.Arrays;

public class Buff {

    private final int DELIMITER_1 = 0xd; //回车符
    private final int DELIMITER_2 = 0xa; //换行符
    private byte[] buffer;

    public void append(byte[] data) {
        if (buffer == null) {
            buffer = new byte[data.length];
            System.arraycopy(data, 0, buffer, 0, data.length);
        } else {
            byte[] temp = new byte[buffer.length + data.length];
            System.arraycopy(buffer, 0, temp, 0, buffer.length);
            System.arraycopy(data, 0, temp, buffer.length, data.length);
            buffer = temp;
        }
    }

    public byte[] getCode() {
        if (buffer == null)
            return null;
        if (buffer.length > 2) {
            boolean found = false;
            int i = 0;
            for (; i < buffer.length - 1; i++) {
                if (buffer[i] == DELIMITER_1 && buffer[i + 1] == DELIMITER_2) {
                    found = true;
                    break;
                }
            }
            if (found) {
                byte[] result = Arrays.copyOf(buffer, i);
                if (i < buffer.length - 2) {
                    buffer = Arrays.copyOfRange(buffer, i + 2, buffer.length);
                } else {
                    buffer = null;
                }
                return result;
            }
        }
        return null;
    }
}
