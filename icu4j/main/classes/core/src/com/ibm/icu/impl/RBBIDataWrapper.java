// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.ibm.icu.impl.ICUBinary.Authenticate;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.util.CodePointTrie;

/**
* <p>Internal class used for Rule Based Break Iterators.</p>
* <p>This class provides access to the compiled break rule data, as
* it is stored in a .brk file. Refer to the file common/rbbidata.h from
* ICU4C for further details.
*/
public final class RBBIDataWrapper {

    /**
     * A RBBI State Transition table, the form of the data used at run time in Java.
     * These can be created from stored ICU data, or built from rules.
     * The structure corresponds closely to struct RBBIStateTable in ICU4C.
     */
    static public class RBBIStateTable {
        /**
         * Number of states (rows) in this table.
         */
        public int     fNumStates;
        /**
         * Length of a table row in bytes. Note mismatch with table data, which is short[].
         */
        public int     fRowLen;
        /**
         * Char category number of the first dictionary char class,
         * or the the largest category number + 1 if there are no dictionary categories.
         */
        public int     fDictCategoriesStart;
        /**
         * Size of run-time array required for holding
         * look-ahead results. Indexed by row.fLookAhead.
         */
        public int     fLookAheadResultsSize;
        /**
         * Option Flags for this state table.
         */
        public int     fFlags;
        /**
         * Length in bytes of the state table header, of all the int32 fields
         * preceding fTable in the serialized form.
         */
        public static int fHeaderSize = 20;
        /**
         * Linear array of next state values, accessed as short[state, char_class]
         */
        public char[] fTable;

        public RBBIStateTable() {
        }

        static RBBIStateTable get(ByteBuffer bytes, int length) throws IOException {
            if (length == 0) {
                return null;
            }
            if (length < fHeaderSize) {
                throw new IOException("Invalid RBBI state table length.");
            }
            RBBIStateTable This = new RBBIStateTable();
            This.fNumStates = bytes.getInt();
            This.fRowLen    = bytes.getInt();
            This.fDictCategoriesStart = bytes.getInt();
            This.fLookAheadResultsSize = bytes.getInt();
            This.fFlags     = bytes.getInt();
            int lengthOfTable = length - fHeaderSize;   // length in bytes.
            boolean use8Bits = (This.fFlags & RBBIDataWrapper.RBBI_8BITS_ROWS) == RBBIDataWrapper.RBBI_8BITS_ROWS;
            if (use8Bits) {
                This.fTable = new char[lengthOfTable];
                for (int i = 0; i < lengthOfTable; i++) {
                    byte b = bytes.get();
                    This.fTable[i] = (char)(0xff & b); // Treat b as unsigned.
                }
                ICUBinary.skipBytes(bytes, lengthOfTable & 1);
            } else {
                This.fTable    = ICUBinary.getChars(bytes, lengthOfTable / 2, lengthOfTable & 1);
            }
            return This;
        }

        public int put(DataOutputStream bytes) throws IOException {
            bytes.writeInt(fNumStates);
            bytes.writeInt(fRowLen);
            bytes.writeInt(fDictCategoriesStart);
            bytes.writeInt(fLookAheadResultsSize);
            bytes.writeInt(fFlags);
            if ((fFlags & RBBIDataWrapper.RBBI_8BITS_ROWS) == RBBIDataWrapper.RBBI_8BITS_ROWS) {
                int tableLen = fRowLen * fNumStates;  // fRowLen is bytes.
                for (int i = 0; i < tableLen; i++) {
                    byte b = (byte)(fTable[i] & 0x00ff);
                    bytes.writeByte(b);
                }
            } else {
                int tableLen = fRowLen * fNumStates / 2;  // fRowLen is bytes.
                for (int i = 0; i < tableLen; i++) {
                    bytes.writeChar(fTable[i]);
                }
            }
            int bytesWritten = fHeaderSize + fRowLen * fNumStates;   // total bytes written,
                                                                     // including the header.
            while (bytesWritten % 8 != 0) {
                bytes.writeByte(0);
                ++bytesWritten;
            }
            return bytesWritten;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals (Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof RBBIStateTable)) {
                return false;
            }
            RBBIStateTable otherST = (RBBIStateTable)other;
            if (fNumStates != otherST.fNumStates) return false;
            if (fRowLen    != otherST.fRowLen)    return false;
            if (fDictCategoriesStart != otherST.fDictCategoriesStart) return false;
            if (fLookAheadResultsSize != otherST.fLookAheadResultsSize) return false;
            if (fFlags     != otherST.fFlags)     return false;
            return Arrays.equals(fTable, otherST.fTable);
        }
    }

    /**
     * Equals helper for state tables, including null handling.
     */
    static public boolean equals(RBBIStateTable left, RBBIStateTable right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }


    //
    // These fields are the ready-to-use compiled rule data, as
    //   read from the file.
    //
    public RBBIDataHeader fHeader;

    public RBBIStateTable   fFTable;

    public RBBIStateTable   fRTable;

    public CodePointTrie    fTrie;
    public String  fRuleSource;
    public int     fStatusTable[];

    public static final int DATA_FORMAT = 0x42726b20;     // "Brk "
    public static final int FORMAT_VERSION = 0x06000000;  // 6.0.0.0

    private static final class IsAcceptable implements Authenticate {
        @Override
        public boolean isDataVersionAcceptable(byte version[]) {
            int intVersion = (version[0] << 24) + (version[1] << 16) + (version[2] << 8) + version[3];
            return intVersion == FORMAT_VERSION;
        }
    }
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();

    //
    // Indexes to fields in the ICU4C style binary form of the RBBI Data Header
    //   Used by the rule compiler when flattening the data.
    //
    public final static int    DH_SIZE           = 20;
    public final static int    DH_MAGIC          = 0;
    public final static int    DH_FORMATVERSION  = 1;
    public final static int    DH_LENGTH         = 2;
    public final static int    DH_CATCOUNT       = 3;
    public final static int    DH_FTABLE         = 4;
    public final static int    DH_FTABLELEN      = 5;
    public final static int    DH_RTABLE         = 6;
    public final static int    DH_RTABLELEN      = 7;
    public final static int    DH_TRIE           = 8;
    public final static int    DH_TRIELEN        = 9;
    public final static int    DH_RULESOURCE     = 10;
    public final static int    DH_RULESOURCELEN  = 11;
    public final static int    DH_STATUSTABLE    = 12;
    public final static int    DH_STATUSTABLELEN = 13;


    // Index offsets to the fields in a state table row.
    //    Corresponds to struct RBBIStateTableRow in the C version.
    //
    /**
     * offset to the "accepting" field in a state table row.
     */
    public final static int      ACCEPTING  = 0;
    /**
     * offset to the "lookahead" field in a state table row.
     */
    public final static int      LOOKAHEAD  = 1;
    /**
     * offset to the "tagIndex" field in a state table row.
     */
    public final static int      TAGSIDX    = 2;
    /**
     * offset to the start of the next states array in a state table row.
     */
    public final static int      NEXTSTATES = 3;

    /**
     *  value constant for the ACCEPTING field of a state table row.
     */
    public final static int      ACCEPTING_UNCONDITIONAL = 1;

    //  Bit selectors for the "FLAGS" field of the state table header
    //     enum RBBIStateTableFlags in the C version.
    //
    public final static int      RBBI_LOOKAHEAD_HARD_BREAK = 1;
    public final static int      RBBI_BOF_REQUIRED         = 2;
    public final static int      RBBI_8BITS_ROWS           = 4;

    /**
     * Data Header.  A struct-like class with the fields from the RBBI data file header.
     * Not intended for public use, declared public for testing purposes only.
     */
    public final static class RBBIDataHeader {
        int         fMagic;         //  == 0xbla0
        byte[]      fFormatVersion; //  For ICU 3.4 and later.
        int         fLength;        //  Total length in bytes of this RBBI Data,
                                    //      including all sections, not just the header.
        /**
         * the number of character categories.
         */
        public int  fCatCount;      //  Number of character categories.

        //
        //  Offsets and sizes of each of the subsections within the RBBI data.
        //  All offsets are bytes from the start of the RBBIDataHeader.
        //  All sizes are in bytes.
        //
        int         fFTable;         //  forward state transition table.
        int         fFTableLen;
        int         fRTable;         //  Offset to the reverse state transition table.
        int         fRTableLen;
        int         fTrie;           //  Offset to Trie data for character categories
        int         fTrieLen;
        int         fRuleSource;     //  Offset to the source for for the break
        int         fRuleSourceLen;  //    rules.  Stored UChar *.
        int         fStatusTable;    // Offset to the table of rule status values
        int         fStatusTableLen;

        public RBBIDataHeader() {
            fMagic = 0;
            fFormatVersion = new byte[4];
        }
    }


    /**
     * RBBI State Table Indexing Function.  Given a state number, return the
     * array index of the start of the state table row for that state.
     */
    public int getRowIndex(int state){
        return state * (fHeader.fCatCount + NEXTSTATES);
    }

    RBBIDataWrapper() {
    }

    /**
     *  Get an RBBIDataWrapper from an InputStream onto a pre-compiled set
     *  of RBBI rules.
     */
    public static RBBIDataWrapper get(ByteBuffer bytes) throws IOException {
        RBBIDataWrapper This = new RBBIDataWrapper();

        ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);

        // Read in the RBBI data header...
        This.fHeader = new  RBBIDataHeader();
        This.fHeader.fMagic          = bytes.getInt();
        This.fHeader.fFormatVersion[0] = bytes.get();
        This.fHeader.fFormatVersion[1] = bytes.get();
        This.fHeader.fFormatVersion[2] = bytes.get();
        This.fHeader.fFormatVersion[3] = bytes.get();
        This.fHeader.fLength         = bytes.getInt();
        This.fHeader.fCatCount       = bytes.getInt();
        This.fHeader.fFTable         = bytes.getInt();
        This.fHeader.fFTableLen      = bytes.getInt();
        This.fHeader.fRTable         = bytes.getInt();
        This.fHeader.fRTableLen      = bytes.getInt();
        This.fHeader.fTrie           = bytes.getInt();
        This.fHeader.fTrieLen        = bytes.getInt();
        This.fHeader.fRuleSource     = bytes.getInt();
        This.fHeader.fRuleSourceLen  = bytes.getInt();
        This.fHeader.fStatusTable    = bytes.getInt();
        This.fHeader.fStatusTableLen = bytes.getInt();
        ICUBinary.skipBytes(bytes, 6 * 4);    // uint32_t  fReserved[6];


        if (This.fHeader.fMagic != 0xb1a0 || !IS_ACCEPTABLE.isDataVersionAcceptable(This.fHeader.fFormatVersion)) {
            throw new IOException("Break Iterator Rule Data Magic Number Incorrect, or unsupported data version.");
        }

        // Current position in the buffer.
        int pos = DH_SIZE * 4;     // offset of end of header, which has DH_SIZE fields, all int32_t (4 bytes)

        //
        // Read in the Forward state transition table as an array of shorts.
        //

        //   Quick Sanity Check
        if (This.fHeader.fFTable < pos || This.fHeader.fFTable > This.fHeader.fLength) {
             throw new IOException("Break iterator Rule data corrupt");
        }

        //    Skip over any padding preceding this table
        ICUBinary.skipBytes(bytes, This.fHeader.fFTable - pos);
        pos = This.fHeader.fFTable;

        This.fFTable = RBBIStateTable.get(bytes, This.fHeader.fFTableLen);
        pos += This.fHeader.fFTableLen;

        //
        // Read in the Reverse state table
        //

        // Skip over any padding in the file
        ICUBinary.skipBytes(bytes, This.fHeader.fRTable - pos);
        pos = This.fHeader.fRTable;

        // Create & fill the table itself.
        This.fRTable = RBBIStateTable.get(bytes, This.fHeader.fRTableLen);
        pos += This.fHeader.fRTableLen;

        //
        // Unserialize the Character categories TRIE
        //     Because we can't be absolutely certain where the Trie deserialize will
        //     leave the buffer, leave position unchanged.
        //     The seek to the start of the next item following the TRIE will get us
        //     back in sync.
        //
        ICUBinary.skipBytes(bytes, This.fHeader.fTrie - pos);  // seek buffer from end of
        pos = This.fHeader.fTrie;               // previous section to the start of the trie

        bytes.mark();                           // Mark position of start of TRIE in the input
                                                //  and tell Java to keep the mark valid so long
                                                //  as we don't go more than 100 bytes past the
                                                //  past the end of the TRIE.

        This.fTrie = CodePointTrie.fromBinary(
            CodePointTrie.Type.FAST,
            null,
            bytes);  // Deserialize the TRIE, leaving buffer
                                                //  at an unknown position, preceding the
                                                //  padding between TRIE and following section.

        bytes.reset();                          // Move buffer back to marked position at
                                                //   the start of the serialized TRIE.  Now our
                                                //   "pos" variable and the buffer are in
                                                //   agreement.

        //
        // Read the Rule Status Table
        //
        if (pos > This.fHeader.fStatusTable) {
            throw new IOException("Break iterator Rule data corrupt");
        }
        ICUBinary.skipBytes(bytes, This.fHeader.fStatusTable - pos);
        pos = This.fHeader.fStatusTable;
        This.fStatusTable = ICUBinary.getInts(
                bytes, This.fHeader.fStatusTableLen / 4, This.fHeader.fStatusTableLen & 3);
        pos += This.fHeader.fStatusTableLen;

        //
        // Put the break rule source into a String
        //
        if (pos > This.fHeader.fRuleSource) {
            throw new IOException("Break iterator Rule data corrupt");
        }
        ICUBinary.skipBytes(bytes, This.fHeader.fRuleSource - pos);
        pos = This.fHeader.fRuleSource;
        This.fRuleSource = new String(
            ICUBinary.getBytes(bytes, This.fHeader.fRuleSourceLen, 0), StandardCharsets.UTF_8);

        if (RuleBasedBreakIterator.fDebugEnv!=null && RuleBasedBreakIterator.fDebugEnv.indexOf("data")>=0) {
            This.dump(System.out);
        }
        return This;
    }

    /** Debug function to display the break iterator data. */
    public void dump(java.io.PrintStream out) {
        if (fFTable == null) {
            // There is no table. Fail early for testing purposes.
            throw new NullPointerException();
        }
        out.println("RBBI Data Wrapper dump ...");
        out.println();
        out.println("Forward State Table");
        dumpTable(out, fFTable);
        out.println("Reverse State Table");
        dumpTable(out, fRTable);

        dumpCharCategories(out);
        out.println("Source Rules: " + fRuleSource);

    }

    /** Fixed width int-to-string conversion. */
    static public String intToString(int n, int width) {
        StringBuilder  dest = new StringBuilder(width);
        dest.append(n);
        while (dest.length() < width) {
           dest.insert(0, ' ');
        }
        return dest.toString();
    }

    static public String charToString(char n, int width) {
        StringBuilder  dest = new StringBuilder(width);
        dest.append(n);
        while (dest.length() < width) {
           dest.insert(0, ' ');
        }
        return dest.toString();
    }

    /** Fixed width int-to-string conversion. */
    static public String intToHexString(int n, int width) {
        StringBuilder  dest = new StringBuilder(width);
        dest.append(Integer.toHexString(n));
        while (dest.length() < width) {
           dest.insert(0, ' ');
        }
        return dest.toString();
    }

    /** Dump a state table.  (A full set of RBBI rules has 4 state tables.)  */
    private void dumpTable(java.io.PrintStream out, RBBIStateTable table) {
        if (table == null || (table.fTable.length == 0)) {
            out.println("  -- null -- ");
        } else {
            char n;
            char state;
            StringBuilder header = new StringBuilder(" Row  Acc Look  Tag");
            for (n=0; n<fHeader.fCatCount; n++) {
                header.append(intToString(n, 5));
            }
            out.println(header.toString());
            for (n=0; n<header.length(); n++) {
                out.print("-");
            }
            out.println();
            for (state=0; state < table.fNumStates; state++) {
                dumpRow(out, table, state);
            }
            out.println();
        }
    }

    /**
     * Dump (for debug) a single row of an RBBI state table
     * @param table
     * @param state
     */
    private void dumpRow(java.io.PrintStream out, RBBIStateTable table, char   state) {
        StringBuilder dest = new StringBuilder(fHeader.fCatCount*5 + 20);
        dest.append(intToString(state, 4));
        int row = getRowIndex(state);
        if (table.fTable[row+ACCEPTING] != 0) {
            dest.append(intToString(table.fTable[row+ACCEPTING], 5));
        } else {
            dest.append("     ");
        }
        if (table.fTable[row+LOOKAHEAD] != 0) {
            dest.append(intToString(table.fTable[row+LOOKAHEAD], 5));
        } else {
            dest.append("     ");
        }
        dest.append(intToString(table.fTable[row+TAGSIDX], 5));

        for (int col=0; col<fHeader.fCatCount; col++) {
            dest.append(intToString(table.fTable[row+NEXTSTATES+col], 5));
        }

        out.println(dest);
    }

    private void dumpCharCategories(java.io.PrintStream out) {
        int n = fHeader.fCatCount;
        String   catStrings[] = new  String[n+1];
        int      rangeStart = 0;
        int      rangeEnd = 0;
        int      lastCat = -1;
        int      char32;
        int      category;
        int      lastNewline[] = new int[n+1];

        for (category = 0; category <= fHeader.fCatCount; category ++) {
            catStrings[category] = "";
        }
        out.println("\nCharacter Categories");
        out.println("--------------------");
        for (char32 = 0; char32<=0x10ffff; char32++) {
            category = fTrie.get(char32);
            if (category < 0 || category > fHeader.fCatCount) {
                out.println("Error, bad category " + Integer.toHexString(category) +
                        " for char " + Integer.toHexString(char32));
                break;
            }
            if (category == lastCat ) {
                rangeEnd = char32;
            } else {
                if (lastCat >= 0) {
                    if (catStrings[lastCat].length() > lastNewline[lastCat] + 70) {
                        lastNewline[lastCat] = catStrings[lastCat].length() + 10;
                        catStrings[lastCat] += "\n       ";
                    }

                    catStrings[lastCat] += " " + Integer.toHexString(rangeStart);
                    if (rangeEnd != rangeStart) {
                        catStrings[lastCat] += "-" + Integer.toHexString(rangeEnd);
                    }
                }
                lastCat = category;
                rangeStart = rangeEnd = char32;
            }
        }
        catStrings[lastCat] += " " + Integer.toHexString(rangeStart);
        if (rangeEnd != rangeStart) {
            catStrings[lastCat] += "-" + Integer.toHexString(rangeEnd);
        }

        for (category = 0; category <= fHeader.fCatCount; category ++) {
            out.println (intToString(category, 5) + "  " + catStrings[category]);
        }
        out.println();
    }

}
