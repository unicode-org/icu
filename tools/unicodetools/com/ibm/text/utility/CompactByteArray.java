package com.ibm.text.utility;

/*
 * %W% %E%
 *
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 * Portions copyright (c) 1996 Sun Microsystems, Inc. All Rights Reserved.
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

import java.io.*;

/**
 *
 * Provides a compact way to store information that is indexed by Unicode
 * values, such as character properties, types, keyboard values, etc.
 * only for internal use for now. Made public for discussion purposes.
 *
 * @see                CompactIntArray
 * @see                CompactShortArray
 * @version            %I% %G%
 * @author             Helena Shih
 */
public final class CompactByteArray implements Serializable {


    public static  final int UNICODECOUNT =65536;

    public CompactByteArray()
    {
        this((byte)0);
    }
    public CompactByteArray(byte defaultValue)
    {
        int i;
        values = new byte[UNICODECOUNT];
        indices = new short[INDEXCOUNT];
        for (i = 0; i < UNICODECOUNT; ++i) {
            values[i] = defaultValue;
        }
        for (i = 0; i < INDEXCOUNT; ++i) {
            indices[i] = (short)(i<<BLOCKSHIFT);
        }
        isCompact = false;
    }
    public CompactByteArray(short indexArray[],
                            byte newValues[]) throws IllegalArgumentException
    {
        int i;
        if (indexArray.length != INDEXCOUNT)
            throw new IllegalArgumentException();
        for (i = 0; i < INDEXCOUNT; ++i) {
            short index = indexArray[i];
            if ((index < 0) || (index >= newValues.length+BLOCKCOUNT))
                throw new IllegalArgumentException();
        }
        indices = indexArray;
        values = newValues;
        isCompact = true;
    }

    public void writeArrays(PrintWriter output)
    {
        int i;
        output.println("package com.ibm.text.unicode;");
        output.println("import com.ibm.text.collections.*;");

        output.println("public final class GeneralCategory {");

        output.println("    public static byte getCategory (char ch) {");
        output.println("	    return compactArray.elementAt(ch);");
        output.println("    }");

        output.println("    static CompactByteArray compactArray;");

        output.println("    static void init () {");
        output.println("        short[] index = {");
        for (i = 0; i < indices.length; i++) {
            if (i % 8 == 0) output.println();
            output.print("(short)" + (indices[i] & 0xFFFF) + ", ");
        }
        output.println("    };");

        output.println("        byte[] data = {");
        for (i = 0; i < values.length; i++) {
            if (i % 8 == 0) output.println();
            output.print("(byte)" + (values[i] & 0xFF) + ", ");
        }
        output.println(" };");
        output.println("	    compactArray = new CompactByteArray(index, data);");
        output.println("    }");
        output.println("}");
        output.close();
    }

    public byte elementAt(char index) // parameterized on byte
    {
        return (values[(indices[index >>> BLOCKSHIFT] & 0xFFFF) +
                      (index & BLOCKMASK)]);
    }
    // Set automatically expands the array if it is compacted.
    // parameterized on value (byte)
    public void setElementAt(char index, byte value)
    {
        if (isCompact)
            expand();
        values[(int)index] = value;
    }
    public void setElementAt(char start, char end, byte value)
    {
        int i;
        if (isCompact) {
            expand();
        }
        for (i = start; i <= end; ++i) {
            values[i] = value;
        }
    }
    // Compact the array.
    // The value of cycle determines how large the overlap can be.
    // A cycle of 1 is the most compacted, but takes the most time to do.
    // If values stored in the array tend to repeat in cycles of, say, 16,
    // then using that will be faster than cycle = 1, and get almost the
    // same compression.  cycle is hardcoded as BLOCKCOUNT now.
    public void compact()
    {
        if (isCompact == false) {
            char[]      tempIndex;
            int                     tempIndexCount;
            byte[]          tempArray;
            short           iBlock, iIndex;

            // make temp storage, larger than we need
            tempIndex = new char[UNICODECOUNT];
            // set up first block.
            tempIndexCount = BLOCKCOUNT;
            for (iIndex = 0; iIndex < BLOCKCOUNT; ++iIndex) {
                tempIndex[iIndex] = (char)iIndex;
            }; // endfor (iIndex = 0; .....)
            indices[0] = (short)0;

            // for each successive block, find out its first position
            // in the compacted array
            for (iBlock = 1; iBlock < INDEXCOUNT; ++iBlock) {
                int     newCount, firstPosition, block;
                block = iBlock<<BLOCKSHIFT;
                if (DEBUGSMALL) if (block > DEBUGSMALLLIMIT) break;
                firstPosition = FindOverlappingPosition( block, tempIndex,
                                                         tempIndexCount );

                newCount = firstPosition + BLOCKCOUNT;
                if (newCount > tempIndexCount) {
                    for (iIndex = (short)tempIndexCount;
                         iIndex < newCount;
                         ++iIndex) {
                        tempIndex[iIndex] = (char)
                                            (iIndex - firstPosition + block);
                    } // endfor (iIndex = tempIndexCount....)
                    tempIndexCount = newCount;
                } // endif (newCount > tempIndexCount)
                indices[iBlock] = (short)firstPosition;
            } // endfor (iBlock = 1.....)

            // now allocate and copy the items into the array
            tempArray = new byte[tempIndexCount];
            for (iIndex = 0; iIndex < tempIndexCount; ++iIndex) {
                tempArray[iIndex] = values[tempIndex[iIndex]];
            }
            values = null;
            values = tempArray;
            isCompact = true;
        } // endif (isCompact != false)
    }
    // Expanded takes the array back to a 65536 element array
    public void expand()
    {
        int i;
        if (isCompact) {
            byte[]  tempArray;
            tempArray = new byte[UNICODECOUNT];
            for (i = 0; i < UNICODECOUNT; ++i) {
                tempArray[i] = elementAt((char)i);
            }
            for (i = 0; i < INDEXCOUNT; ++i) {
                indices[i] = (short)(i<<BLOCKSHIFT);
            }
            values = null;
            values = tempArray;
            isCompact = false;
        }
    }
    // Print char Array  : Debug only
    public void printIndex(short start, short count)
    {
        int i;
        for (i = start; i < count; ++i)
        {
            System.out.println(i + " -> : " +
                               (int)((indices[i] >= 0) ?
                                     indices[i] :
                                     indices[i] + UNICODECOUNT));
        }
        System.out.println();
    }
    public void printPlainArray(int start,int count, char[] tempIndex)
    {
        int iIndex;
        if (tempIndex != null)
        {
            for (iIndex     = start; iIndex < start + count; ++iIndex)
            {
                System.out.print(" " + (int)values[tempIndex[iIndex]]);
            }
        }
        else
        {
            for (iIndex = start; iIndex < start + count; ++iIndex)
            {
                System.out.print(" " + (int)values[iIndex]);
            }
        }
        System.out.println("    Range: start " + start + " , count " + count);
    }
    // # of elements in the indexed array
    public short capacity()
    {
        return (short)values.length;
    }

    public int storage()
    {
        return values.length * 1 + indices.length * 2 + 12;
    }

    private byte[] getArray()
    {
        return values;
    }
    private int
    FindOverlappingPosition(int start, char[] tempIndex, int tempIndexCount)
    {
        int i;
        short j;
        short currentCount;

        if (DEBUGOVERLAP && start < DEBUGSHOWOVERLAPLIMIT) {
            printPlainArray(start, BLOCKCOUNT, null);
            printPlainArray(0, tempIndexCount, tempIndex);
        }
        for (i = 0; i < tempIndexCount; i += BLOCKCOUNT) {
            currentCount = (short)BLOCKCOUNT;
            if (i + BLOCKCOUNT > tempIndexCount) {
                currentCount = (short)(tempIndexCount - i);
            }
            for (j = 0; j < currentCount; ++j) {
                if (values[start + j] != values[tempIndex[i + j]]) break;
            }
            if (j == currentCount) break;
        }
        if (DEBUGOVERLAP && start < DEBUGSHOWOVERLAPLIMIT) {
            for (j = 1; j < i; ++j) {
                System.out.print(" ");
            }
            printPlainArray(start, BLOCKCOUNT, null);
            System.out.println("    Found At: " + i);
        }
        return i;
    }
    private static  final int DEBUGSHOWOVERLAPLIMIT = 100;
    private static  final boolean DEBUGTRACE = false;
    private static  final boolean DEBUGSMALL = false;
    private static  final boolean DEBUGOVERLAP = false;
    private static  final int DEBUGSMALLLIMIT = 30000;
    private static  final int BLOCKSHIFT =6;
    private static  final int BLOCKCOUNT =(1<<BLOCKSHIFT);
    private static  final int INDEXSHIFT =(16-BLOCKSHIFT);
    private static  final int INDEXCOUNT =(1<<INDEXSHIFT);
    private static  final int BLOCKMASK = BLOCKCOUNT - 1;

    private byte[] values;  // char -> short (char parameterized short)
    private short indices[];
    private boolean isCompact;
};
