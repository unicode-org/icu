/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import com.ibm.icu.text.UTF16;

/**
 * Simple data input compressor. Nothing fancy, but much smaller footprint for
 * ints and many strings.
 */
public final class DataInputCompressor implements ObjectInput {
    static final boolean SHOW = false;

    private ObjectInput dataInput;

    private transient StringBuffer stringBuffer = new StringBuffer();

    public DataInputCompressor(ObjectInput dataInput) {
        this.dataInput = dataInput;
    }

    public DataInput getDataInput() {
        return dataInput;
    }

    public void setDataInput(ObjectInput dataInput) {
        this.dataInput = dataInput;
    }

    public boolean readBoolean() throws IOException {
        return dataInput.readBoolean();
    }

    public byte readByte() throws IOException {
        return dataInput.readByte();
    }

    public int readUnsignedByte() throws IOException {
        return dataInput.readUnsignedByte();
    }

    public double readDouble() throws IOException {
        return dataInput.readDouble();
    }

    public float readFloat() throws IOException {
        return dataInput.readFloat();
    }

    public void readFully(byte[] b) throws IOException {
        dataInput.readFully(b);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        dataInput.readFully(b, off, len);
    }

    public int skipBytes(int n) throws IOException {
        return dataInput.skipBytes(n);
    }

    public String readLine() throws IOException {
        return dataInput.readLine();
    }

    public int available() throws IOException {
        return dataInput.available();
    }
    public void close() throws IOException {
        dataInput.close();
    }
    public int read() throws IOException {
        return dataInput.read();
    }
    public int read(byte[] b) throws IOException {
        return dataInput.read(b);
    }
    public int read(byte[] b, int off, int len) throws IOException {
        return dataInput.read(b, off, len);
    }
    public Object readObject() throws ClassNotFoundException, IOException {
        return dataInput.readObject();
    }
    public long skip(long n) throws IOException {
        return dataInput.skip(n);
    }
    public String toString() {
        return dataInput.toString();
    }
    // ==== New Routines ====

    public char readChar() throws IOException {
        return (char) readULong();
    }

    public short readShort() throws IOException {
        return (short) readLong();
    }

    public int readUnsignedShort() throws IOException {
        return (int) readULong();
    }

    public int readUShort() throws IOException {
        return (int) readULong();
    }

    public int readInt() throws IOException {
        return (int) readLong();
    }

    public int readUInt() throws IOException {
        return (int) readULong();
    }

    public String readChars(int len) throws IOException {
        stringBuffer.setLength(0);
        for (int i = 0; i < len; ++i) {
            int cp = (int) readULong();
            UTF16.append(stringBuffer, cp);
        }
        return stringBuffer.toString();
    }

    public String readUTF() throws IOException {
        int len = (int) readULong();
        return readChars(len);
    }

    public long readLong() throws IOException {
        long result = 0;
        int offset = 0;
        while (true) {
            long input = readByte();
            result |= (input & 0x7F) << offset;
            if ((input & 0x80) == 0)
                break;
            offset += 7;
        }
        boolean negative = (result & 1) != 0; // get sign bit from the bottom,
                                              // and invert
        result >>>= 1;
        if (negative)
            result = ~result;
        return result;
    }

    public long readULong() throws IOException {
        long result = 0;
        int offset = 0;
        while (true) { // read sequence of 7 bits, with top bit = 1 for
                       // continuation
            int input = readByte();
            result |= (input & 0x7F) << offset;
            if ((input & 0x80) == 0)
                return result;
            offset += 7;
        }
    }

    /**
     *  
     */
    public Object[] readStringSet(Collection availableValues)
            throws IOException {
        int size = readUInt();
        if (SHOW) System.out.println("readStringSet");
        Object[] valuesList = new Object[size + 1];
        // first item is null
        String lastString = "";
        ReadPool trailingPool = new ReadPool();
        for (int i = 0; i < size; ++i) {
            int common = readUInt();
            boolean inPool = (common & 1) != 0;
            common >>>= 1;
            if (SHOW) System.out.println(common);
            String current;
            if (inPool) {
                int poolIndex = readUInt();
                if (SHOW) System.out.println("\t" + poolIndex);
                current = (String) trailingPool.get(poolIndex);
            } else {
                current = readUTF();
                trailingPool.add(current);
            }
            valuesList[i + 1] = lastString = lastString.substring(0, common)
                    + current;
            if (SHOW) System.out.println("\t\t" + lastString);
            if (availableValues != null) availableValues.add(current);
        }
        return valuesList;
    }
    
    public static class ReadPool {
        private List trailingPool = new ArrayList();
        public Object get(int index) {
            return trailingPool.get(index);
        }
        public void add(Object o) {
            trailingPool.add(o);
        }
    }

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     * 
     */
    public Object[] readCollection(LinkedHashSet availableValues) throws ClassNotFoundException, IOException {
        int size = readUInt();
        Object[] valuesList = new Object[size + 1];
        for (int i = 0; i < size; ++i) {
            valuesList[i + 1] = readObject();
        }
       return valuesList;
    }
}
