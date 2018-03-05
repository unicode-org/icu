// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.ICUBinary.Authenticate;
import com.ibm.icu.impl.Trie2;

/**
* <p>Internal class used for Rule Based Break Iterators.</p>
* <p>This class provides access to the compiled break rule data, as
* it is stored in a .brk file. Refer to the file common/rbbidata.h from
* ICU4C for further details.
* Not intended for public use; declared public for testing purposes only.
* @internal
* @deprecated This API is ICU internal only.
*/
@Deprecated
public final class RBBIDataWrapper {

    /**
     * A RBBI State Transition table, the form of the data used at run time in Java.
     * These can be created from stored ICU data, or built from rules.
     * The structure corresponds closely to struct RBBIStateTable in ICU4C.
     * Not intended for public use; declared public for testing purposes only.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    static public class RBBIStateTable {
        /**
         * Number of states (rows) in this table.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public int     fNumStates;
        /**
         * Length of a table row in bytes. Note mismatch with table data, which is short[].
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public int     fRowLen;
        /**
         * Option Flags for this state table.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public int     fFlags;
        /**
         * Option Flags for this state table.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public int     fReserved;
        /**
         * Linear array of next state values, accessed as short[state, char_class]
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public short[] fTable;

        static RBBIStateTable get(ByteBuffer bytes, int length) throws IOException {
            if (length == 0) {
                return null;
            }
            if (length < 16) {
                throw new IOException("Invalid RBBI state table length.");
            }
            RBBIStateTable This = new RBBIStateTable();
            This.fNumStates = bytes.getInt();
            This.fRowLen    = bytes.getInt();
            This.fFlags     = bytes.getInt();
            This.fReserved  = bytes.getInt();
            int lengthOfShorts = length - 16;   // length in bytes.
            This.fTable     = ICUBinary.getShorts(bytes, lengthOfShorts / 2, lengthOfShorts & 1);
            return This;
        }

        int put(DataOutputStream bytes) throws IOException {
            bytes.writeInt(fNumStates);
            bytes.writeInt(fRowLen);
            bytes.writeInt(fFlags);
            bytes.writeInt(fReserved);
            int tableLen = fRowLen * fNumStates / 2;  // fRowLen is bytes.
            for (int i = 0; i < tableLen; i++) {
                bytes.writeShort(fTable[i]);
            }
            int bytesWritten = 16 + fRowLen * fNumStates;   // total bytes written,
                                                            // including 16 for the header.
            while (bytesWritten % 8 != 0) {
                bytes.writeByte(0);
                ++bytesWritten;
            }
            return bytesWritten;
        }

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
            if (fFlags     != otherST.fFlags)     return false;
            if (fReserved  != otherST.fReserved)  return false;
            return Arrays.equals(fTable, otherST.fTable);
        }
    }

    /**
     * Equals helper for state tables, including null handling.
     * Not intended for public use; declared public for testing purposes only.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
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
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RBBIDataHeader fHeader;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RBBIStateTable   fFTable;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RBBIStateTable   fRTable;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RBBIStateTable   fSFTable;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public RBBIStateTable   fSRTable;

    Trie2          fTrie;
    String         fRuleSource;
    int            fStatusTable[];

    static final int DATA_FORMAT = 0x42726b20;     // "Brk "
    static final int FORMAT_VERSION = 0x04000000;  // 4.0.0.0

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
    final static int    DH_SIZE           = 24;
    final static int    DH_MAGIC          = 0;
    final static int    DH_FORMATVERSION  = 1;
    final static int    DH_LENGTH         = 2;
    final static int    DH_CATCOUNT       = 3;
    final static int    DH_FTABLE         = 4;
    final static int    DH_FTABLELEN      = 5;
    final static int    DH_RTABLE         = 6;
    final static int    DH_RTABLELEN      = 7;
    final static int    DH_SFTABLE        = 8;
    final static int    DH_SFTABLELEN     = 9;
    final static int    DH_SRTABLE        = 10;
    final static int    DH_SRTABLELEN     = 11;
    final static int    DH_TRIE           = 12;
    final static int    DH_TRIELEN        = 13;
    final static int    DH_RULESOURCE     = 14;
    final static int    DH_RULESOURCELEN  = 15;
    final static int    DH_STATUSTABLE    = 16;
    final static int    DH_STATUSTABLELEN = 17;


    // Index offsets to the fields in a state table row.
    //    Corresponds to struct RBBIStateTableRow in the C version.
    //
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public final static int      ACCEPTING  = 0;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public final static int      LOOKAHEAD  = 1;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public final static int      TAGIDX     = 2;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public final static int      RESERVED   = 3;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public final static int      NEXTSTATES = 4;

    //  Bit selectors for the "FLAGS" field of the state table header
    //     enum RBBIStateTableFlags in the C version.
    //
    final static int      RBBI_LOOKAHEAD_HARD_BREAK = 1;
    final static int      RBBI_BOF_REQUIRED         = 2;

    /**
     * Data Header.  A struct-like class with the fields from the RBBI data file header.
     * Not intended for public use, declared public for testing purposes only.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public final static class RBBIDataHeader {
        int         fMagic;         //  == 0xbla0
        byte[]      fFormatVersion; //  For ICU 3.4 and later.
        int         fLength;        //  Total length in bytes of this RBBI Data,
                                       //      including all sections, not just the header.
        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
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
        int         fSFTable;        //  safe point forward transition table
        int         fSFTableLen;
        int         fSRTable;        //  safe point reverse transition table
        int         fSRTableLen;
        int         fTrie;           //  Offset to Trie data for character categories
        int         fTrieLen;
        int         fRuleSource;     //  Offset to the source for for the break
        int         fRuleSourceLen;  //    rules.  Stored UChar *.
        int         fStatusTable;    // Offset to the table of rule status values
        int         fStatusTableLen;

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public RBBIDataHeader() {
            fMagic = 0;
            fFormatVersion = new byte[4];
        }
    }


    /**
     * RBBI State Table Indexing Function.  Given a state number, return the
     * array index of the start of the state table row for that state.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public int getRowIndex(int state){
        return state * (fHeader.fCatCount + 4);
    }

    RBBIDataWrapper() {
    }

    /*
     *  Get an RBBIDataWrapper from an InputStream onto a pre-compiled set
     *  of RBBI rules.
     */
    static RBBIDataWrapper get(ByteBuffer bytes) throws IOException {
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
        This.fHeader.fSFTable        = bytes.getInt();
        This.fHeader.fSFTableLen     = bytes.getInt();
        This.fHeader.fSRTable        = bytes.getInt();
        This.fHeader.fSRTableLen     = bytes.getInt();
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
        int pos = 24 * 4;     // offset of end of header, which has 24 fields, all int32_t (4 bytes)

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
        // Read in the Safe Forward state table
        //
        if (This.fHeader.fSFTableLen > 0) {
            // Skip over any padding in the file
            ICUBinary.skipBytes(bytes, This.fHeader.fSFTable - pos);
            pos = This.fHeader.fSFTable;

            // Create & fill the table itself.
            This.fSFTable = RBBIStateTable.get(bytes, This.fHeader.fSFTableLen);
            pos += This.fHeader.fSFTableLen;
        }

        //
        // Read in the Safe Reverse state table
        //
        if (This.fHeader.fSRTableLen > 0) {
            // Skip over any padding in the file
            ICUBinary.skipBytes(bytes, This.fHeader.fSRTable - pos);
            pos = This.fHeader.fSRTable;

            // Create & fill the table itself.
            This.fSRTable = RBBIStateTable.get(bytes, This.fHeader.fSRTableLen);
            pos += This.fHeader.fSRTableLen;
        }

        // Rule Compatibility Hacks
        //    If a rule set includes reverse rules but does not explicitly include safe reverse rules,
        //    the reverse rules are to be treated as safe reverse rules.

        if (This.fSRTable == null && This.fRTable != null) {
            This.fSRTable = This.fRTable;
            This.fRTable = null;
        }

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

        This.fTrie = Trie2.createFromSerialized(bytes);  // Deserialize the TRIE, leaving buffer
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
        This.fRuleSource = ICUBinary.getString(
                bytes, This.fHeader.fRuleSourceLen / 2, This.fHeader.fRuleSourceLen & 1);

        if (RuleBasedBreakIterator.fDebugEnv!=null && RuleBasedBreakIterator.fDebugEnv.indexOf("data")>=0) {
            This.dump(System.out);
        }
        return This;
    }

    ///CLOVER:OFF
    /* Debug function to display the break iterator data. */
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
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
        out.println("Forward Safe Points Table");
        dumpTable(out, fSFTable);
        out.println("Reverse Safe Points Table");
        dumpTable(out, fSRTable);

        dumpCharCategories(out);
        out.println("Source Rules: " + fRuleSource);

    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /* Fixed width int-to-string conversion. */
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    static public String intToString(int n, int width) {
        StringBuilder  dest = new StringBuilder(width);
        dest.append(n);
        while (dest.length() < width) {
           dest.insert(0, ' ');
        }
        return dest.toString();
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /* Fixed width int-to-string conversion. */
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    static public String intToHexString(int n, int width) {
        StringBuilder  dest = new StringBuilder(width);
        dest.append(Integer.toHexString(n));
        while (dest.length() < width) {
           dest.insert(0, ' ');
        }
        return dest.toString();
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /** Dump a state table.  (A full set of RBBI rules has 4 state tables.)  */
    private void dumpTable(java.io.PrintStream out, RBBIStateTable table) {
        if (table == null || table.fTable.length == 0)   {
            out.println("  -- null -- ");
        } else {
            int n;
            int state;
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
    ///CLOVER:ON

    ///CLOVER:OFF
    /**
     * Dump (for debug) a single row of an RBBI state table
     * @param table
     * @param state
     */
    private void dumpRow(java.io.PrintStream out, RBBIStateTable table, int   state) {
        StringBuilder dest = new StringBuilder(fHeader.fCatCount*5 + 20);
        dest.append(intToString(state, 4));
        int row = getRowIndex(state);
        if (table.fTable[row+ACCEPTING] != 0) {
           dest.append(intToString(table.fTable[row+ACCEPTING], 5));
        }else {
            dest.append("     ");
        }
        if (table.fTable[row+LOOKAHEAD] != 0) {
            dest.append(intToString(table.fTable[row+LOOKAHEAD], 5));
        }else {
            dest.append("     ");
        }
        dest.append(intToString(table.fTable[row+TAGIDX], 5));

        for (int col=0; col<fHeader.fCatCount; col++) {
            dest.append(intToString(table.fTable[row+NEXTSTATES+col], 5));
        }

        out.println(dest);
    }
    ///CLOVER:ON

    ///CLOVER:OFF
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
            category &= ~0x4000;            // Mask off dictionary bit.
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
    ///CLOVER:ON

    /*static RBBIDataWrapper get(String name) throws IOException {
        String  fullName = "data/" + name;
        InputStream is = ICUData.getRequiredStream(fullName);
        return get(is);
    }

    public static void main(String[] args) {
        String s;
        if (args.length == 0) {
            s = "char";
        } else {
            s = args[0];
        }
        System.out.println("RBBIDataWrapper.main(" + s + ") ");

        String versionedName = ICUResourceBundle.ICU_BUNDLE+"/"+ s + ".brk";

        try {
            RBBIDataWrapper This = RBBIDataWrapper.get(versionedName);
            This.dump();
        }
       catch (Exception e) {
           System.out.println("Exception: " + e.toString());
       }

    }*/
}
