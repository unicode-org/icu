package com.ibm.text.utility;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;

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

public final class UTF8StreamReader extends Reader {

    private InputStream input;
    private boolean checkIrregular = true;

    UTF8StreamReader(InputStream stream, int buffersize) {
        if (buffersize < 1) {
            throw new IllegalArgumentException("UTF8StreamReader buffersize must be >= 1");
        }
        input = stream;
        bBuffer = new byte[buffersize];
    }

    private static final int MAGIC = 0x10000 + ((0 - 0xD800) << 10) + (0 - 0xDC00);

    private byte[] bBuffer; // do a bit of buffering ourselves for efficiency
    private int
        bIndex = 0,
        bEnd = 0,
        bRemaining = 0,
        currentPoint = 0,
        lastPoint,
        shortestFormTest = 0;
    private char cCarry = 0;

    private static final byte[] BYTES_REMAINING = {
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 0-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 1-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 2-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 3-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 4-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 5-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 6-
        0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  // 7-
       -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,  // 8-
       -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,  // 9-
       -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,  // A-
       -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,  // B-
       -1,-1, 1, 1,  1, 1, 1, 1,  1, 1, 1, 1,  1, 1, 1, 1,  // C-
        1, 1, 1, 1,  1, 1, 1, 1,  1, 1, 1, 1,  1, 1, 1, 1,  // D-
        2, 2, 2, 2,  2, 2, 2, 2,  2, 2, 2, 2,  2, 2, 2, 2,  // E-
        3, 3, 3, 3, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1   // F-
    };

    public int read(char cbuf[], int off, int len) throws IOException {

        // check input arguments

        if (len <= 0) return 0;
        if (off > len) return 0;

        int cIndex = off;
        int cEnd = off + len;

        // if we had a low surrogate from the last call, get it first

        if (cCarry != 0 && len > 0) {
            cbuf[cIndex++] = cCarry;
            cCarry = 0;
        }

        // now loop, filling in the output

        while (cIndex < cEnd) {

            // get more bytes if we run out

            if (bIndex >= bEnd) {
                bIndex = 0;
                bEnd = input.read(bBuffer, 0, bBuffer.length);
                if (bEnd < 0) {
                    if (cIndex == off) return -1;
                    return cIndex - off;
                }
            }

            // process the current byte (mask because Java doesn't have unsigned byte)

            int b = bBuffer[bIndex++] & 0xFF;

            switch (bRemaining) {
              // First Byte case
              case 0:
                bRemaining = BYTES_REMAINING[b];
                switch (bRemaining) {
                  case 0:
	                cbuf[cIndex++] = (char) (lastPoint = b);
	                break;
            	  case 1:
	                currentPoint = b & 0x1F;
                    shortestFormTest = 0x80;
	                break;
	              case 2:
	                currentPoint = b & 0xF;
                    shortestFormTest = 0x800;
	                break;
                  case 3:
	                currentPoint = b & 0x7;
                    shortestFormTest = 0x10000;
	                break;
                  default:
                    throw new IllegalArgumentException("illegal lead code unit: " + b);
                }
                break;

              // Trailing bytes
              case 2: case 3:
                b ^= 0x80;
                if (b > 0x3F) {
                    throw new IllegalArgumentException("illegal trail code unit: " + (b ^ 0x80));
                }
                currentPoint = (currentPoint << 6) | b;
                --bRemaining;
                break;

              // Last trailing byte, time to assemble
              case 1:
                b ^= 0x80;
                if (b > 0x3F) {
                    throw new IllegalArgumentException("illegal trail code unit: " + (b ^ 0x80));
                }
                currentPoint = (currentPoint << 6) | b;
                --bRemaining;

                // we have gotten the code, so check and stash it

                if (currentPoint < shortestFormTest) {
                    throw new IllegalArgumentException("illegal sequence, not shortest form: " + currentPoint);
                }
                if (checkIrregular && 0xD800 <= lastPoint && lastPoint <= 0xDC00
                        && 0xDC00 <= currentPoint && currentPoint <= 0xDFFF) {
                    throw new IllegalArgumentException("irregular sequence, surrogate pair: " + currentPoint);
                }
                lastPoint = currentPoint;
                if (currentPoint >= 0x10000) {
                    if (currentPoint > 0x10FFFF) {
                        throw new IllegalArgumentException("illegal code point, too large: " + currentPoint);
                    }
                    currentPoint -= 0x10000;
                    cbuf[cIndex++] = (char)(0xD800 + (currentPoint >> 10));
                    currentPoint = 0xDC00 + (currentPoint & 0x3FF);
                    if (cIndex >= cEnd) {
                        cCarry = (char)currentPoint;
                        return cIndex - off;
                    }
                }
                cbuf[cIndex++] = (char)currentPoint;
                currentPoint = 0;
                break;
            }
        }
        return cIndex - off;
    }

    public void close() throws IOException {
        input.close();
    }
}
