/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/UTF8StreamWriter.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;
import java.io.*;

/**
 * Utility class that writes UTF8.<br>
 * Main purpose is to supplant OutputStreamWriter(x, "UTF8"), since that has serious errors.
 * <br>
 * Example of Usage:
 * <pre>
 * PrintWriter log = new PrintWriter(
 *   new UTF8StreamWriter(new FileOutputStream(fileName), 32*1024));
 * </pre>
 * NB: unsynchronized for simplicity and speed. The same object must NOT be used in multiple threads.
 */
 // TODO: Fix case of surrogate pair crossing input buffer boundary

public final class UTF8StreamWriter extends Writer {

    private OutputStream output;
    private byte[] bBuffer; // do a bit of buffering ourselves for efficiency
    private int bSafeEnd;
    private int bEnd;
    private int bIndex = 0;
    private int highSurrogate = 0;

    public UTF8StreamWriter(OutputStream stream, int buffersize) {
        if (buffersize < 5) {
            throw new IllegalArgumentException("UTF8StreamWriter buffersize must be >= 5");
        }
        output = stream;
        bBuffer = new byte[buffersize];
        bEnd = buffersize;
        bSafeEnd = buffersize - 4;
    }

    private static final int
        NEED_2_BYTES = 1<<7,
        NEED_3_BYTES = 1<<(2*5 + 1),
        NEED_4_BYTES = 1<<(3*5 + 1);

    private static final int
        TRAILING_BOTTOM_MASK = 0x3F,
        TRAILING_TOP = 0x80;

    private static final int MAGIC = 0x10000 + ((0 - 0xD800) << 10) + (0 - 0xDC00);

    public final void write(char[] buffer, int cStart, int cLength) throws IOException {
        int cEnd = cStart + cLength;
        while (cStart < cEnd) {

            // write if we need to

            if (bIndex > bSafeEnd) {
                output.write(bBuffer, 0, bIndex);
                bIndex = 0;
            }

            // get code point

            int utf32 = buffer[cStart++];

            // special check for surrogates

            if (highSurrogate != 0) {
                if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) {
                    writeCodePoint((highSurrogate << 10) + utf32 + MAGIC);
                    highSurrogate = 0;
                    continue;
                }
                writeCodePoint(highSurrogate);
                highSurrogate = 0;
            }

            if (0xD800 <= utf32 && utf32 <= 0xDBFF) {
                highSurrogate = utf32;
                continue;
            }

            // normal case

            writeCodePoint(utf32);
        }
    }

    private final void writeCodePoint(int utf32) {

        // convert to bytes

		if (utf32 < NEED_2_BYTES) {
		    bBuffer[bIndex++] = (byte)utf32;
		    return;
        }

		// Find out how many bytes we need to write
		// At this point, it is at least 2.

	    //int count;
		int backIndex;
		int firstByteMark;
		if (utf32 < NEED_3_BYTES) {
		    backIndex = bIndex += 2;
		    firstByteMark = 0xC0;
		} else if (utf32 < NEED_4_BYTES) {
		    backIndex = bIndex += 3;
		    firstByteMark = 0xE0;
			bBuffer[--backIndex] = (byte)(TRAILING_TOP | (utf32 & TRAILING_BOTTOM_MASK));
			utf32 >>= 6;
		} else {
		    backIndex = bIndex += 4;
		    firstByteMark = 0xF0;
			bBuffer[--backIndex] = (byte)(TRAILING_TOP | (utf32 & TRAILING_BOTTOM_MASK));
			utf32 >>= 6;
			bBuffer[--backIndex] = (byte)(TRAILING_TOP | (utf32 & TRAILING_BOTTOM_MASK));
			utf32 >>= 6;
		};
		bBuffer[--backIndex] = (byte)(TRAILING_TOP | (utf32 & TRAILING_BOTTOM_MASK));
		utf32 >>= 6;
		bBuffer[--backIndex] = (byte)(firstByteMark | utf32);
    }

    private void internalFlush() throws IOException {
        if (highSurrogate != 0) {
            if (bIndex > bEnd) {
                output.write(bBuffer, 0, bIndex);
                bIndex = 0;
            }
            writeCodePoint(highSurrogate);
            highSurrogate = 0;
        }

        // write buffer if we need to
        if (bIndex != 0) {
            output.write(bBuffer, 0, bIndex);
            bIndex = 0;
        }
    }

    public void close() throws IOException {
        internalFlush();
        output.close();
    }

    public void flush() throws IOException {
        internalFlush();
        output.flush();
    }
}
