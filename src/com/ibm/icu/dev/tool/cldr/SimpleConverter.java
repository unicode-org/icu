/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package com.ibm.icu.dev.tool.cldr;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ByteArrayWrapper;
;

/**
 * WARNING, DON'T USE.
 * This is very draft. Don't use outside of GenerateSidewaysView, for now.
 * @internal
 */
public class SimpleConverter {
    public SimpleConverter(Charset cs) {
        ce = cs
           .newEncoder()
           .onMalformedInput(CodingErrorAction.REPORT)
           .onUnmappableCharacter(CodingErrorAction.REPORT);
        cd = cs
           .newDecoder()
           .onMalformedInput(CodingErrorAction.REPORT)
           .onUnmappableCharacter(CodingErrorAction.REPORT);
    }
    public char[] cb = new char[100];
    public CharBuffer charBuffer = CharBuffer.wrap(cb);
    public byte[] bb = new byte[100];
    public ByteBuffer byteBuffer = ByteBuffer.wrap(bb);

    public CharsetEncoder ce;
    public CharsetDecoder cd;

    public UnicodeSet getCharset() {
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 0x10000; ++i) {
            if (0xD800 <= i && i < 0xE000) {
                if (i >= 0xDC00) continue;
                for (int j = 0xDC00; j <= 0xE000; ++j) {
                    cb[0] = (char) i;
                    cb[1] = (char) j;
                    ByteArrayWrapper ab = encode(2);
                    if (ab == null) continue;
                    String backMap = decode(ab.size);
                    if (backMap.length() != 2 || UTF16.charAt(backMap,0) != i) continue;
                    result.add(i);
                }
                continue;
            }
            cb[0] = (char) i;
            ByteArrayWrapper ab = encode(1);
            if (ab == null) {
                continue;
            }
            String backMap = decode(ab.size);
            if (backMap.length() != 1 || backMap.charAt(0) != i) {
                continue;
            }
            result.add(i);
        }
        return result;
    }

    public ByteArrayWrapper encode(int len) {
        charBuffer.limit(len);
        charBuffer.position(0);
        byteBuffer.clear();
        ce.reset();
        CoderResult result = ce.encode(charBuffer, byteBuffer, true);
        if (result.isError()) return null;
        byteBuffer.flip();
        return new ByteArrayWrapper(byteBuffer);
    }

    public String decode(int len) {
        byteBuffer.limit(len);
        byteBuffer.position(0);
        charBuffer.clear();
        cd.reset();
        CoderResult result = cd.decode(byteBuffer, charBuffer, true);
        if (result.isError()) return null;
        charBuffer.flip();
        return String.valueOf(cb,0,charBuffer.limit());
    }
}