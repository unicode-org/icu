//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
//import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
//import java.util.TreeSet;

//import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;

/**
 * Simple data output compressor. Nothing fancy, but much smaller footprint for ints and many strings.
 */
public final class DataOutputCompressor implements ObjectOutput {
    static final boolean SHOW = false;

    private ObjectOutput dataOutput;

    public DataOutputCompressor(ObjectOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    public DataOutput getDataOutput() {
        return dataOutput;
    }

    public void setDataOutput(ObjectOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    public void write(byte[] b) throws IOException {
        dataOutput.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        dataOutput.write(b, off, len);
    }

    public void write(int b) throws IOException {
        dataOutput.write(b);
    }

    public void writeBoolean(boolean v) throws IOException {
        dataOutput.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        dataOutput.writeByte(v);
    }

    public void writeBytes(String s) throws IOException {
        dataOutput.writeBytes(s);
    }

    public void writeDouble(double v) throws IOException {
        dataOutput.writeDouble(v);
    }

    public void writeFloat(float v) throws IOException {
        dataOutput.writeFloat(v);
    }

    public void close() throws IOException {
        dataOutput.close();
    }
    public void flush() throws IOException {
        dataOutput.flush();
    }
    public String toString() {
        return dataOutput.toString();
    }
    public void writeObject(Object obj) throws IOException {
        dataOutput.writeObject(obj);
    }
    // ==== New Routines ====

    public void writeChar(int v) throws IOException {
        writeULong(v);
    }

    public void writeShort(int v) throws IOException {
        writeLong(v);
    }

    public void writeUShort(int v) throws IOException {
        writeULong(v);
    }

    public void writeInt(int v) throws IOException {
        writeLong(v);
    }

    public void writeUInt(int v) throws IOException {
        writeULong(v);
    }

    public void writeUTF(String str) throws IOException {
        writeULong(UTF16.countCodePoint(str));
        writeChars(str);
    }

    public void writeChars(String s) throws IOException {
        int cp = 0;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            writeULong(cp);
        }
    }

    public void writeLong(long v) throws IOException {
        long flag = 0; // put sign bit at the bottom, and invert
        if (v < 0) {
            v = ~v;
            flag = 1;
        }
        v <<= 1;
        v |= flag;
        while (true) {
            if ((v & ~0x7FL) == 0) {
                dataOutput.writeByte((byte) v);
                break;
            }
            dataOutput.writeByte((byte) (0x80L | v));
            v >>>= 7;
        }
    }

    public void writeULong(long v) throws IOException {
        while (true) { // write sequence of 7 bits, with top bit = 1 for continuation
            if ((v & ~0x7FL) == 0) {
                dataOutput.writeByte((byte) v);
                break;
            }
            dataOutput.writeByte((byte) (0x80L | v));
            v >>>= 7;
        }
    }

    /**
     * 
     */
    public void writeStringSet(SortedSet c, Map object_index) throws IOException {
        if (SHOW) System.out.println("writeStringSet");
        writeUInt(c.size());
        int i = 0;
        object_index.put(null, new Integer(i++));
        WritePool trailingPool = new WritePool();
        String lastString = "";
        for (Iterator it = c.iterator(); it.hasNext();) {
            String s = (String) it.next();
            object_index.put(s, new Integer(i++));
            int common = UnicodeMap.findCommon(lastString, s); // runlength encode
            lastString = s;
            String piece = s.substring(common);
            if (SHOW) System.out.println(common);
            common <<= 1;
            int inPool = trailingPool.getIndex(piece);
            if (inPool < 0) {
                writeUInt(common);
                writeUTF(piece);
                trailingPool.put(piece);
            } else {
                writeUInt(common | 1);
                writeUInt(inPool);
                if (SHOW) System.out.println("\t" + inPool);
            }
            if (SHOW) System.out.println("\t\t" + lastString);
        }
    }
    
    public static class WritePool {
        private Map trailingPool = new HashMap();
        private int poolCount = 0;
        public int getIndex(Object o) {
            Integer inPool = (Integer) trailingPool.get(o);
            if (inPool == null) return -1;
            return inPool.intValue();
        }
        public void put(Object o) {
            trailingPool.put(o, new Integer(poolCount++));
        }
    }

    /**
     * @throws IOException
     * 
     */
    public void writeCollection(Collection c, Map object_index) throws IOException {
        writeUInt(c.size());
        int i = 0;
        object_index.put(null, new Integer(i++));
        for (Iterator it = c.iterator(); it.hasNext();) {
            Object s = it.next();
            dataOutput.writeObject(s);
            if (object_index != null) object_index.put(s, new Integer(i++));
        }
    }
}

//#endif
