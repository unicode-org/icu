/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import com.ibm.icu.util.CompactByteArray;

/**
 * This is the class that represents the list of known words used by
 * DictionaryBasedBreakIterator.  The conceptual data structure used
 * here is a trie: there is a node hanging off the root node for every
 * letter that can start a word.  Each of these nodes has a node hanging
 * off of it for every letter that can be the second letter of a word
 * if this node is the first letter, and so on.  The trie is represented
 * as a two-dimensional array that can be treated as a table of state
 * transitions.  Indexes are used to compress this array, taking
 * advantage of the fact that this array will always be very sparse.
 */
class BreakDictionary {
    //=================================================================================
    // testing and debugging
    //=================================================================================
    ///CLOVER:OFF
    //The main method looks like it was useful once but now seems worthless. It is not used by any method or class.
    public static void main(String args[])
            throws FileNotFoundException, UnsupportedEncodingException, IOException {
        String filename = args[0];

        BreakDictionary dictionary = new BreakDictionary(new FileInputStream(filename));

        PrintWriter out = null;

        if(args.length >= 2) {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UnicodeLittle"));
        }

        dictionary.printWordList("", 0, out);

        if (out != null) {
            out.close();
        }
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /* public */ void printWordList(String partialWord, int state, PrintWriter out)
            throws IOException {
        if (state == 0xFFFF) {
            System.out.println(partialWord);
            if (out != null) {
                out.println(partialWord);
            }
        }
        else {
            for (int i = 0; i < numCols; i++) {
                int newState = (at(state, i)) & 0xFFFF;

                if (newState != 0) {
                    char newChar = reverseColumnMap[i];
                    String newPartialWord = partialWord;

                    if (newChar != 0) {
                        newPartialWord += newChar;
                    }

                    printWordList(newPartialWord, newState, out);
                }
            }
        }
    }
    ///CLOVER:ON

    /**
     * A map used to go from column numbers to characters.  Used only
     * for debugging right now.
     */
    private char[] reverseColumnMap = null;

    //=================================================================================
    // data members
    //=================================================================================

    /**
     * Maps from characters to column numbers.  The main use of this is to
     * avoid making room in the array for empty columns.
     */
    private CompactByteArray columnMap = null;

    /**
     * The number of actual columns in the table
     */
    private int numCols;

    /*
     * Columns are organized into groups of 32.  This says how many
     * column groups.  (We could calculate this, but we store the
     * value to avoid having to repeatedly calculate it.)
     */
    //private int numColGroups;

    /**
     * The actual compressed state table.  Each conceptual row represents
     * a state, and the cells in it contain the row numbers of the states
     * to transition to for each possible letter.  0 is used to indicate
     * an illegal combination of letters (i.e., the error state).  The
     * table is compressed by eliminating all the unpopulated (i.e., zero)
     * cells.  Multiple conceptual rows can then be doubled up in a single
     * physical row by sliding them up and possibly shifting them to one
     * side or the other so the populated cells don't collide.  Indexes
     * are used to identify unpopulated cells and to locate populated cells.
     */
    private short[] table = null;

    /**
     * This index maps logical row numbers to physical row numbers
     */
    private short[] rowIndex = null;

    /**
     * A bitmap is used to tell which cells in the comceptual table are
     * populated.  This array contains all the unique bit combinations
     * in that bitmap.  If the table is more than 32 columns wide,
     * successive entries in this array are used for a single row.
     */
    private int[] rowIndexFlags = null;

    /**
     * This index maps from a logical row number into the bitmap table above.
     * (This keeps us from storing duplicate bitmap combinations.)  Since there
     * are a lot of rows with only one populated cell, instead of wasting space
     * in the bitmap table, we just store a negative number in this index for
     * rows with one populated cell.  The absolute value of that number is
     * the column number of the populated cell.
     */
    private short[] rowIndexFlagsIndex = null;

    /**
     * For each logical row, this index contains a constant that is added to
     * the logical column number to get the physical column number
     */
    private byte[] rowIndexShifts = null;

    //=================================================================================
    // deserialization
    //=================================================================================

    /* public */ BreakDictionary(InputStream dictionaryStream) throws IOException {
        readDictionaryFile(new DataInputStream(dictionaryStream));
    }

    /* public */ void readDictionaryFile(DataInputStream in) throws IOException {
        int l;

        // read in the version number (right now we just ignore it)
        in.readInt();

        // read in the column map (this is serialized in its internal form:
        // an index array followed by a data array)
        l = in.readInt();
        char[] temp = new char[l];
        for (int i = 0; i < temp.length; i++)
            temp[i] = (char)in.readShort();
        l = in.readInt();
        byte[] temp2 = new byte[l];
        for (int i = 0; i < temp2.length; i++)
            temp2[i] = in.readByte();
        columnMap = new CompactByteArray(temp, temp2);

        // read in numCols and numColGroups
        numCols = in.readInt();
        /*numColGroups = */in.readInt();

        // read in the row-number index
        l = in.readInt();
        rowIndex = new short[l];
        for (int i = 0; i < rowIndex.length; i++)
            rowIndex[i] = in.readShort();

        // load in the populated-cells bitmap: index first, then bitmap list
        l = in.readInt();
        rowIndexFlagsIndex = new short[l];
        for (int i = 0; i < rowIndexFlagsIndex.length; i++)
            rowIndexFlagsIndex[i] = in.readShort();
        l = in.readInt();
        rowIndexFlags = new int[l];
        for (int i = 0; i < rowIndexFlags.length; i++)
            rowIndexFlags[i] = in.readInt();

        // load in the row-shift index
        l = in.readInt();
        rowIndexShifts = new byte[l];
        for (int i = 0; i < rowIndexShifts.length; i++)
            rowIndexShifts[i] = in.readByte();

        // finally, load in the actual state table
        l = in.readInt();
        table = new short[l];
        for (int i = 0; i < table.length; i++)
            table[i] = in.readShort();

        // this data structure is only necessary for testing and debugging purposes
        reverseColumnMap = new char[numCols];
        for (char c = 0; c < 0xffff; c++) {
            int col = columnMap.elementAt(c);
            if (col != 0) {
               reverseColumnMap[col] = c;
            }
        }

        // close the stream
        in.close();
    }

    //=================================================================================
    // access to the words
    //=================================================================================

    /**
     * Uses the column map to map the character to a column number, then
     * passes the row and column number to the other version of at()
     * @param row The current state
     * @param ch The character whose column we're interested in
     * @return The new state to transition to
     */
    /* public */ final short at(int row, char ch) {
        int col = columnMap.elementAt(ch);
        return at(row, col);
    }

    /**
     * Returns the value in the cell with the specified (logical) row and
     * column numbers.  In DictionaryBasedBreakIterator, the row number is
     * a state number, the column number is an input, and the return value
     * is the row number of the new state to transition to.  (0 is the
     * "error" state, and -1 is the "end of word" state in a dictionary)
     * @param row The row number of the current state
     * @param col The column number of the input character (0 means "not a
     * dictionary character")
     * @return The row number of the new state to transition to
     */
    /* public */ final short at(int row, int col) {
        if (cellIsPopulated(row, col)) {
            // we map from logical to physical row number by looking up the
            // mapping in rowIndex; we map from logical column number to
            // physical column number by looking up a shift value for this
            // logical row and offsetting the logical column number by
            // the shift amount.  Then we can use internalAt() to actually
            // get the value out of the table.
            return internalAt(rowIndex[row], col + rowIndexShifts[row]);
        }
        else {
            return 0;
        }
    }

    /**
     * Given (logical) row and column numbers, returns true if the
     * cell in that position is populated
     */
    private final boolean cellIsPopulated(int row, int col) {
        // look up the entry in the bitmap index for the specified row.
        // If it's a negative number, it's the column number of the only
        // populated cell in the row
        if (rowIndexFlagsIndex[row] < 0) {
            return col == -rowIndexFlagsIndex[row];
        }

        // if it's a positive number, it's the offset of an entry in the bitmap
        // list.  If the table is more than 32 columns wide, the bitmap is stored
        // successive entries in the bitmap list, so we have to divide the column
        // number by 32 and offset the number we got out of the index by the result.
        // Once we have the appropriate piece of the bitmap, test the appropriate
        // bit and return the result.
        else {
            int flags = rowIndexFlags[rowIndexFlagsIndex[row] + (col >> 5)];
            return (flags & (1 << (col & 0x1f))) != 0;
        }
    }

    /**
     * Implementation of at() when we know the specified cell is populated.
     * @param row The PHYSICAL row number of the cell
     * @param col The PHYSICAL column number of the cell
     * @return The value stored in the cell
     */
    private final short internalAt(int row, int col) {
        // the table is a one-dimensional array, so this just does the math necessary
        // to treat it as a two-dimensional array (we don't just use a two-dimensional
        // array because two-dimensional arrays are inefficient in Java)
        return table[row * numCols + col];
    }
}

