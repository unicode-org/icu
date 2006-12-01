/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/CompactShortArray.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

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
import java.lang.*;
/**
 * class CompactATypeArray : use only on primitive data types
 * Provides a compact way to store information that is indexed by Unicode
 * values, such as character properties, types, keyboard values, etc.This
 * is very useful when you have a block of Unicode data that contains
 * significant values while the rest of the Unicode data is unused in the
 * application or when you have a lot of redundance, such as where all 21,000
 * Han ideographs have the same value.  However, lookup is much faster than a
 * hash table.
 * A compact array of any primitive data type serves two purposes:
 * <UL type = round>
 *     <LI>Fast access of the indexed values.
 *     <LI>Smaller memory footprint.
 * </UL>
 * A compact array is composed of a index array and value array.  The index
 * array contains the indicies of Unicode characters to the value array.
 * @see                CompactByteArray
 * @see                CompactIntArray
 * @see                CompactCharArray
 * @see                CompactStringArray
 * @version            %I% %G%
 * @author             Helena Shih
 */
public final class CompactShortArray implements Serializable {


    /**
     * The total number of Unicode characters.
     */
    public static  final int UNICODECOUNT =65536;

    /**
     * Default constructor for CompactShortArray, the default value of the
     * compact array is 0.
     */
    public CompactShortArray()
    {
        this((short)0);
    }
    /**
     * Constructor for CompactShortArray.
     * @param defaultValue the default value of the compact array.
     */
    public CompactShortArray(short defaultValue)
    {
        int i;
        values = new short[UNICODECOUNT];
        indices = new short[INDEXCOUNT];
        for (i = 0; i < UNICODECOUNT; ++i) {
            values[i] = defaultValue;
        }
        for (i = 0; i < INDEXCOUNT; ++i) {
            indices[i] = (short)(i<<BLOCKSHIFT);
        }
        isCompact = false;
    }
    /**
     * Constructor for CompactShortArray.
     * @param indexArray the indicies of the compact array.
     * @param newValues the values of the compact array.
     * @exception IllegalArgumentException If the index is out of range.
     */
    public CompactShortArray(short indexArray[],
                             short newValues[]) throws IllegalArgumentException
    {
        int i;
        if (indexArray.length != INDEXCOUNT)
            throw new IllegalArgumentException("Index out of bounds.");
        for (i = 0; i < INDEXCOUNT; ++i) {
            short index = indexArray[i];
            if ((index < 0) || (index >= newValues.length+BLOCKCOUNT))
                throw new IllegalArgumentException("Index out of bounds.");
        }
        indices = indexArray;
        values = newValues;
    }
    /**
     * Get the mapped value of a Unicode character.
     * @param index the character to get the mapped value with
     * @return the mapped value of the given character
     */
    public short elementAt(char index) // parameterized on short
    {
        return (values[(indices[index >> BLOCKSHIFT] & 0xFFFF)
                       + (index & BLOCKMASK)]);
    }
    /**
     * Set a new value for a Unicode character.
     * Set automatically expands the array if it is compacted.
     * @param index the character to set the mapped value with
     * @param value the new mapped value
     */
    public void setElementAt(char index, short value)
    {
        if (isCompact)
            expand();
        values[(int)index] = value;
    }
    /**
     * Set new values for a range of Unicode character.
     * @param start the starting offset of the range
     * @param end the ending offset of the range
     * @param value the new mapped value
     */
    public void setElementAt(char start, char end, short value)
    {
        int i;
        if (isCompact) {
            expand();
        }
        for (i = start; i <= end; ++i) {
            values[i] = value;
        }
    }
    /**
      *Compact the array.
      */
    public void compact()
    {
        if (isCompact == false) {
            char[]      tempIndex;
            int                     tempIndexCount;
            short[]         tempArray;
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
                firstPosition = FindOverlappingPosition(block, tempIndex,
                                                        tempIndexCount);

                newCount = firstPosition + BLOCKCOUNT;
                if (newCount > tempIndexCount) {
                    for (iIndex = (short)tempIndexCount;
                         iIndex < newCount;
                         ++iIndex) {
                        tempIndex[iIndex]
                            = (char)(iIndex - firstPosition + block);
                    } // endfor (iIndex = tempIndexCount....)
                    tempIndexCount = newCount;
                } // endif (newCount > tempIndexCount)
                indices[iBlock] = (short)firstPosition;
            } // endfor (iBlock = 1.....)

            // now allocate and copy the items into the array
            tempArray = new short[tempIndexCount];
            for (iIndex = 0; iIndex < tempIndexCount; ++iIndex) {
                tempArray[iIndex] = values[tempIndex[iIndex]];
            }
            values = null;
            values = tempArray;
            isCompact = true;
        } // endif (isCompact != false)
    }
    /** For internal use only.  Do not modify the result, the behavior of
      * modified results are undefined.
      */
    public short getIndexArray()[]
    {
        return indices;
    }
    /** For internal use only.  Do not modify the result, the behavior of
      * modified results are undefined.
      */
    public short getStringArray()[]
    {
        return values;
    }
    // --------------------------------------------------------------
    // package private
    // --------------------------------------------------------------
    void writeArrays()
    {
        int i;
        int cnt = ((values.length > 0) ? values.length :
                   (values.length + UNICODECOUNT));
        System.out.println("{");
        for (i = 0; i < INDEXCOUNT-1; i++)
        {
            System.out.print("(short)" + (int)((getIndexArrayValue(i) >= 0) ?
                (int)getIndexArrayValue(i) :
                (int)(getIndexArrayValue(i)+UNICODECOUNT)) + ", ");
            if (i != 0)
                if (i % 10 == 0)
                    System.out.println();
        }
        System.out.println("(short)" +
                           (int)((getIndexArrayValue(INDEXCOUNT-1) >= 0) ?
                                 (int)getIndexArrayValue(i) :
                                 (int)(getIndexArrayValue(i)+UNICODECOUNT)) +
                           " }");
        System.out.println("{");
        for (i = 0; i < cnt-1; i++)
        {
            System.out.print("(short)" + (int)getArrayValue(i) + ", ");
            if (i != 0)
                if (i % 10 == 0)
                    System.out.println();
        }
        System.out.println("(short)" + (int)getArrayValue(cnt-1) + " }");
    }
    // Print char Array  : Debug only
    void printIndex(short start, short count)
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
    void printPlainArray(int start,int count, char[] tempIndex)
    {
        int iIndex;
        if (tempIndex != null)
        {
            for (iIndex     = start; iIndex < start + count; ++iIndex)
            {
                System.out.print(" " + (int)getArrayValue(tempIndex[iIndex]));
            }
        }
        else
        {
            for (iIndex = start; iIndex < start + count; ++iIndex)
            {
                System.out.print(" " + (int)getArrayValue(iIndex));
            }
        }
        System.out.println("    Range: start " + start + " , count " + count);
    }
    // --------------------------------------------------------------
    // private
    // --------------------------------------------------------------
    /**
      * Expanding takes the array back to a 65536 element array.
      */
    private void expand()
    {
        int i;
        if (isCompact) {
            short[] tempArray;
            tempArray = new short[UNICODECOUNT];
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
    // # of elements in the indexed array
    private short capacity()
    {
        return (short)values.length;
    }
    public int storage()
    {
        return values.length * 2 + indices.length * 2 + 12;
    }

    private short getArrayValue(int n)
    {
        return values[n];
    }
    private short getIndexArrayValue(int n)
    {
        return indices[n];
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
    private static  final int BLOCKSHIFT =7;
    private static  final int BLOCKCOUNT =(1<<BLOCKSHIFT);
    private static  final int INDEXSHIFT =(16-BLOCKSHIFT);
    private static  final int INDEXCOUNT =(1<<INDEXSHIFT);
    private static  final int BLOCKMASK = BLOCKCOUNT - 1;

    private short values[];  // char -> short (char parameterized short)
    private short indices[];
    private boolean isCompact;
};
