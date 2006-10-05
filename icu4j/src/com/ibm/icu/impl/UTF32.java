/*
 *******************************************************************************
 * Copyright (C) 2005-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.impl;

import com.ibm.icu.text.UTF16;

/**
 * This class converts between an array of bytes in UTF-32 encoding (BE or LE) and
 * Java Strings.
 * 
 * @internal
 */
public abstract class UTF32
{
    /**
     * This method packs a 32-bit Unicode code point into the byte array. It is
     * implemented by subclasses that implement the BE and LE encodings.
     * 
     * @param bytes the destination byte array
     * @param codePoint the 32-bit Unicode code point
     * @param out the destination index in <code>bytes</code>.
     * 
     * @internal
     */
    abstract protected void pack(byte[] bytes, int codePoint, int out);
    
    /**
     * This method unpacks bytes from the encoded byte array into a 32-bit
     * Unicode code point. It is implmeented by subclasses that implmeent the BE and LE encodings.
     * 
     * @param bytes the source byte array.
     * @param index the index of the first source byte.
     * @return the 32-bit Unicode code point.
     * 
     * @internal
     */
    abstract protected int unpack(byte[] bytes, int index);
    
    
    /**
     * Convert a Java String into an array of UTF-32 encoded bytes. Calls
     * the <code>pack</code> method to do the encoding.
     * 
     * @param utf16 the source Java String.
     * @return an array of UTF-32 encoded bytes.
     * 
     * @internal
     */
    public byte[] toBytes(String utf16)
    {
        int codePoints = UTF16.countCodePoint(utf16);
        byte[] bytes = new byte[codePoints * 4];
        int out = 0;

        for (int cp = 0; cp < codePoints; out += 4) {
            int codePoint = UTF16.charAt(utf16, cp);
            
            pack(bytes, codePoint, out);
            cp += UTF16.getCharCount(codePoint);
        }
        
        return bytes;
    }
    
    /**
     * This method converts a sequence of UTF-32 encoded bytes into
     * a Java String. It calls the <code>unpack</code> method to implement
     * the encoding.
     * 
     * @param bytes the source byte array.
     * @param offset the starting offset in the byte array.
     * @param count the number of bytes to process.
     * @return the Java String.
     * 
     * @internal
     */
    public String fromBytes(byte[] bytes, int offset, int count)
    {
        StringBuffer buffer = new StringBuffer();
        int limit = offset + count;
        
        for (int cp = offset; cp < limit; cp += 4) {
            int codePoint = unpack(bytes, cp);
            
            UTF16.append(buffer, codePoint);
        }
        
        return buffer.toString();
    }
    
    /**
     * A convenience method that converts an entire byte array
     * into a Java String.
     * 
     * @param bytes the source byte array.
     * @return the Java String.
     * 
     * @internal
     */
    public String fromBytes(byte[] bytes)
    {
        return fromBytes(bytes, 0, bytes.length);
    }
    
    /**
     * Get an instance that implements UTF-32BE encoding.
     * 
     * @return the instance.
     * 
     * @internal
     */
    static public UTF32 getBEInstance()
    {
        if (beInstance == null) {
            beInstance = new BE();
        }
        
        return beInstance;
    }
    
    /**
     * Get an instance that implemnts the UTF-32LE encoding.
     * 
     * @return the instance.
     * 
     * @internal
     */
    static public UTF32 getLEInstance()
    {
        if (leInstance == null) {
            leInstance = new LE();
        }
        
        return leInstance;
    }
    
    /**
     * Get an instance that implements either UTF-32BE or UTF32-LE,
     * depending on the encoding name suppled.
     * 
     * @param encoding the encoding name - must be <code>"UTF-32BE"</code> or <code>"UTF-32LE"</code>.
     * @return the instance.
     * 
     * @internal
     */
    static public UTF32 getInstance(String encoding)
    {
        if (encoding.equals("UTF-32BE")) {
            return getBEInstance();
        }
        
        if (encoding.equals("UTF-32LE")) {
            return getLEInstance();
        }
        
        return null;
    }
    
    /**
     * This sublcass implements the UTF-32BE encoding via the
     * <code>pack</code> and <code>unpack</code> methods.
     * 
     * @internal
     */
    static class BE extends UTF32
    {
        /**
         * This method packs a 32-bit Unicode code point into the byte array using
         * the UTF-32BE encoding.
         * 
         * @param bytes the destination byte array
         * @param codePoint the 32-bit Unicode code point
         * @param out the destination index in <code>bytes</code>.
         * 
         * @internal
         */
        public void pack(byte[] bytes, int codePoint, int out)
        {
            bytes[out + 0] = (byte) ((codePoint >> 24) & 0xFF);
            bytes[out + 1] = (byte) ((codePoint >> 16) & 0xFF);
            bytes[out + 2] = (byte) ((codePoint >>  8) & 0xFF);
            bytes[out + 3] = (byte) ((codePoint >>  0) & 0xFF);
        }
        
        /**
         * This method unpacks bytes from the UTF-32BE encoded byte array into a 32-bit
         * Unicode code point.
         * 
         * @param bytes the source byte array.
         * @param index the index of the first source byte.
         * @return the 32-bit Unicode code point.
         * 
         * @internal
         */
        public int unpack(byte[] bytes, int index)
        {
            return (bytes[index + 0] & 0xFF) << 24 | (bytes[index + 1] & 0xFF) << 16 |
                   (bytes[index + 2] & 0xFF) <<  8 | (bytes[index + 3] & 0xFF);
        }
    }
    
    /**
     * This sublcass implements the UTF-32LE encoding via the
     * <code>pack</code> and <code>unpack</code> methods.
     * 
     * @internal
     */
    static class LE extends UTF32
    {
        /**
         * This method packs a 32-bit Unicode code point into the byte array using
         * the UTF-32LE encoding.
         * 
         * @param bytes the destination byte array
         * @param codePoint the 32-bit Unicode code point
         * @param out the destination index in <code>bytes</code>.
         * 
         * @internal
         */
        public void pack(byte[] bytes, int codePoint, int out)
        {
            bytes[out + 3] = (byte) ((codePoint >> 24) & 0xFF);
            bytes[out + 2] = (byte) ((codePoint >> 16) & 0xFF);
            bytes[out + 1] = (byte) ((codePoint >>  8) & 0xFF);
            bytes[out + 0] = (byte) ((codePoint >>  0) & 0xFF);
        }
        
        /**
         * This method unpacks bytes from the UTF-32LE encoded byte array into a 32-bit
         * Unicode code point.
         * 
         * @param bytes the source byte array.
         * @param index the index of the first source byte.
         * @return the 32-bit Unicode code point.
         * 
         * @internal
         */
        public int unpack(byte[] bytes, int index)
        {
            return (bytes[index + 3] & 0xFF) << 24 | (bytes[index + 2] & 0xFF) << 16 |
                   (bytes[index + 1] & 0xFF) <<  8 | (bytes[index + 0] & 0xFF);
        }
    }
    
    private static UTF32 beInstance = null;
    private static UTF32 leInstance = null;
}
