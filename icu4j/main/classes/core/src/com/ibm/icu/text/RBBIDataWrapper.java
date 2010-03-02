/**
*******************************************************************************
* Copyright (C) 1996-2010, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.text;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ibm.icu.impl.CharTrie;
import com.ibm.icu.impl.Trie;

/**
* <p>Internal class used for Rule Based Break Iterators</p>
* <p>This class provides access to the compiled break rule data, as
* it is stored in a .brk file.  
*/
final class RBBIDataWrapper {
    //
    // These fields are the ready-to-use compiled rule data, as
    //   read from the file.
    //
    RBBIDataHeader fHeader;
    short          fFTable[];
    short          fRTable[];
    short          fSFTable[];
    short          fSRTable[];
    CharTrie       fTrie;
    String         fRuleSource;
    int            fStatusTable[];
    
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
    final static int      ACCEPTING  = 0;
    final static int      LOOKAHEAD  = 1;
    final static int      TAGIDX     = 2;
    final static int      RESERVED   = 3;
    final static int      NEXTSTATES = 4;
    
    // Index offsets to header fields of a state table
    //     struct RBBIStateTable {...   in the C version.
    //
    final static int      NUMSTATES  = 0;
    final static int      ROWLEN     = 2;
    final static int      FLAGS      = 4;
    final static int      RESERVED_2 = 6;
    final static int      ROW_DATA   = 8;
    
    //  Bit selectors for the "FLAGS" field of the state table header
    //     enum RBBIStateTableFlags in the C version.
    //
    final static int      RBBI_LOOKAHEAD_HARD_BREAK = 1;
    final static int      RBBI_BOF_REQUIRED         = 2;  
    
    /**
     * Data Header.  A struct-like class with the fields from the RBBI data file header.
     */
    final static class RBBIDataHeader {
        int         fMagic;         //  == 0xbla0 
        int         fVersion;       //  == 1 (for ICU 3.2 and earlier. 
        byte[]      fFormatVersion; //  For ICU 3.4 and later.
        int         fLength;        //  Total length in bytes of this RBBI Data, 
                                       //      including all sections, not just the header. 
        int         fCatCount;      //  Number of character categories. 

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

        public RBBIDataHeader() {
            fMagic = 0;
            fFormatVersion = new byte[4];
        }
    }
    
    
    /**
     * RBBI State Table Indexing Function.  Given a state number, return the
     * array index of the start of the state table row for that state.
     * 
     */
    int getRowIndex(int state){
        return ROW_DATA + state * (fHeader.fCatCount + 4);
    }
    
    static class TrieFoldingFunc implements  Trie.DataManipulate {
        public int getFoldingOffset(int data) {
            if ((data & 0x8000) != 0) {
                return data & 0x7fff;
            } else {
                return 0;
            }
        }
    }
    static TrieFoldingFunc  fTrieFoldingFunc = new TrieFoldingFunc();
 
    
    RBBIDataWrapper() {
    }

    /*
     *  Get an RBBIDataWrapper from an InputStream onto a pre-compiled set
     *  of RBBI rules.
     */
    static RBBIDataWrapper get(InputStream is) throws IOException {
        int i;
        
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        RBBIDataWrapper This = new RBBIDataWrapper();
        
        // Seek past the ICU data header.
        //   TODO:  verify that the header looks good.
        dis.skip(0x80);
        
        // Read in the RBBI data header...
        This.fHeader = new  RBBIDataHeader();
        This.fHeader.fMagic          = dis.readInt();
        This.fHeader.fVersion        = dis.readInt();
        This.fHeader.fFormatVersion[0] = (byte) (This.fHeader.fVersion >> 24);
        This.fHeader.fFormatVersion[1] = (byte) (This.fHeader.fVersion >> 16);
        This.fHeader.fFormatVersion[2] = (byte) (This.fHeader.fVersion >> 8);
        This.fHeader.fFormatVersion[3] = (byte) (This.fHeader.fVersion);
        This.fHeader.fLength         = dis.readInt();
        This.fHeader.fCatCount       = dis.readInt();
        This.fHeader.fFTable         = dis.readInt();
        This.fHeader.fFTableLen      = dis.readInt();
        This.fHeader.fRTable         = dis.readInt();
        This.fHeader.fRTableLen      = dis.readInt();
        This.fHeader.fSFTable        = dis.readInt();
        This.fHeader.fSFTableLen     = dis.readInt();
        This.fHeader.fSRTable        = dis.readInt();
        This.fHeader.fSRTableLen     = dis.readInt();
        This.fHeader.fTrie           = dis.readInt();
        This.fHeader.fTrieLen        = dis.readInt();
        This.fHeader.fRuleSource     = dis.readInt();
        This.fHeader.fRuleSourceLen  = dis.readInt();
        This.fHeader.fStatusTable    = dis.readInt();
        This.fHeader.fStatusTableLen = dis.readInt();
        dis.skip(6 * 4);    // uint32_t  fReserved[6];
        
        
        if (This.fHeader.fMagic != 0xb1a0 || 
                ! (This.fHeader.fVersion == 1  ||         // ICU 3.2 and earlier
                   This.fHeader.fFormatVersion[0] == 3)   // ICU 3.4
            ) {
            throw new IOException("Break Iterator Rule Data Magic Number Incorrect, or unsupported data version.");
        }
        
        // Current position in input stream.  
        int pos = 24 * 4;     // offset of end of header, which has 24 fields, all int32_t (4 bytes)
        
        //
        // Read in the Forward state transition table as an array of shorts.
        //
        
        //   Quick Sanity Check
        if (This.fHeader.fFTable < pos || This.fHeader.fFTable > This.fHeader.fLength) {
             throw new IOException("Break iterator Rule data corrupt");
        }
        
        //    Skip over any padding preceding this table
        dis.skip(This.fHeader.fFTable - pos);
        pos = This.fHeader.fFTable;
        
        This.fFTable = new short[This.fHeader.fFTableLen / 2];
        for ( i=0; i<This.fFTable.length; i++) {
            This.fFTable[i] = dis.readShort(); 
            pos += 2;
        }
        
        //
        // Read in the Reverse state table
        //
        
        // Skip over any padding in the file
        dis.skip(This.fHeader.fRTable - pos);
        pos = This.fHeader.fRTable;
        
        // Create & fill the table itself.
        This.fRTable = new short[This.fHeader.fRTableLen / 2];
        for (i=0; i<This.fRTable.length; i++) {
            This.fRTable[i] = dis.readShort(); 
            pos += 2;
        }
        
        //
        // Read in the Safe Forward state table
        // 
        if (This.fHeader.fSFTableLen > 0) {
            // Skip over any padding in the file
            dis.skip(This.fHeader.fSFTable - pos);
            pos = This.fHeader.fSFTable;
            
            // Create & fill the table itself.
            This.fSFTable = new short[This.fHeader.fSFTableLen / 2];
            for (i=0; i<This.fSFTable.length; i++) {
                This.fSFTable[i] = dis.readShort(); 
                pos += 2;
            }           
        }
        
        //
        // Read in the Safe Reverse state table
        // 
        if (This.fHeader.fSRTableLen > 0) {
            // Skip over any padding in the file
            dis.skip(This.fHeader.fSRTable - pos);
            pos = This.fHeader.fSRTable;
            
            // Create & fill the table itself.
            This.fSRTable = new short[This.fHeader.fSRTableLen / 2];
            for (i=0; i<This.fSRTable.length; i++) {
                This.fSRTable[i] = dis.readShort(); 
                pos += 2;
            }           
        }
        
        //
        // Unserialize the Character categories TRIE
        //     Because we can't be absolutely certain where the Trie deserialize will
        //     leave the input stream, leave position unchanged.
        //     The seek to the start of the next item following the TRIE will get us
        //     back in sync.
        //
        dis.skip(This.fHeader.fTrie - pos);     // seek input stream from end of previous section to
        pos = This.fHeader.fTrie;               //   to the start of the trie
    
        dis.mark(This.fHeader.fTrieLen+100);    // Mark position of start of TRIE in the input
                                                //  and tell Java to keep the mark valid so long
                                                //  as we don't go more than 100 bytes past the
                                                //  past the end of the TRIE.
    
        This.fTrie = new CharTrie(dis, fTrieFoldingFunc);  // Deserialize the TRIE, leaving input
                                                //  stream at an unknown position, preceding the
                                                //  padding between TRIE and following section.
    
        dis.reset();                            // Move input stream back to marked position at
                                                //   the start of the serialized TRIE.  Now our
                                                //   "pos" variable and the input stream are in
                                                //   agreement.
        
        //
        // Read the Rule Status Table
        //
        if (pos > This.fHeader.fStatusTable) {
            throw new IOException("Break iterator Rule data corrupt");            
        }
        dis.skip(This.fHeader.fStatusTable - pos);
        pos = This.fHeader.fStatusTable;
        This.fStatusTable = new int[This.fHeader.fStatusTableLen / 4];
        for (i=0; i<This.fStatusTable.length; i++) {
            This.fStatusTable[i] = dis.readInt(); 
            pos += 4;
        }
        
        //
        // Put the break rule source into a String
        //
        if (pos > This.fHeader.fRuleSource) {
            throw new IOException("Break iterator Rule data corrupt");            
        }
        dis.skip(This.fHeader.fRuleSource - pos);
        pos = This.fHeader.fRuleSource;
        StringBuilder sb = new StringBuilder(This.fHeader.fRuleSourceLen / 2);
        for (i=0; i<This.fHeader.fRuleSourceLen; i+=2) {
            sb.append(dis.readChar()); 
            pos += 2;
        }
        This.fRuleSource = sb.toString();
        
        if (RuleBasedBreakIterator.fDebugEnv!=null && RuleBasedBreakIterator.fDebugEnv.indexOf("data")>=0) {
            This.dump();
        }
        return This;
    }

    ///CLOVER:OFF
    //  Getters for fields from the state table header
    //
    final static int   getNumStates(short  table[]) {
        int  hi = table[NUMSTATES];
        int  lo = table[NUMSTATES+1];
        int  val = (hi<<16) + (lo&0x0000ffff);
        return val;
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /* Debug function to display the break iterator data. */
    void dump() {
        System.out.println("RBBI Data Wrapper dump ...");
        System.out.println();
        System.out.println("Forward State Table");
        dumpTable(fFTable);
        System.out.println("Reverse State Table");
        dumpTable(fRTable);
        System.out.println("Forward Safe Points Table");
        dumpTable(fSFTable);
        System.out.println("Reverse Safe Points Table");
        dumpTable(fSRTable);
        
        dumpCharCategories();
        System.out.println("Source Rules: " + fRuleSource);
        
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /* Fixed width int-to-string conversion. */
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
    private void dumpTable(short table[]) {
        if (table == null)   {
            System.out.println("  -- null -- ");
        } else {
            int n;
            int state;
            String header = " Row  Acc Look  Tag";
            for (n=0; n<fHeader.fCatCount; n++) {
                header += intToString(n, 5);     
            }
            System.out.println(header);
            for (n=0; n<header.length(); n++) {
                System.out.print("-");
            }
            System.out.println();
            for (state=0; state< getNumStates(table); state++) {
                dumpRow(table, state);   
            }
            System.out.println();
        }
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    /**
     * Dump (for debug) a single row of an RBBI state table
     * @param table
     * @param state
     */
    private void dumpRow(short table[], int   state) {
        StringBuilder dest = new StringBuilder(fHeader.fCatCount*5 + 20);
        dest.append(intToString(state, 4));
        int row = getRowIndex(state);
        if (table[row+ACCEPTING] != 0) {
           dest.append(intToString(table[row+ACCEPTING], 5)); 
        }else {
            dest.append("     ");
        }
        if (table[row+LOOKAHEAD] != 0) {
            dest.append(intToString(table[row+LOOKAHEAD], 5)); 
        }else {
            dest.append("     ");
        }
        dest.append(intToString(table[row+TAGIDX], 5)); 
        
        for (int col=0; col<fHeader.fCatCount; col++) {
            dest.append(intToString(table[row+NEXTSTATES+col], 5));   
        }

        System.out.println(dest);
    }
    ///CLOVER:ON

    ///CLOVER:OFF
    private void dumpCharCategories() {
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
        System.out.println("\nCharacter Categories");
        System.out.println("--------------------");
        for (char32 = 0; char32<=0x10ffff; char32++) {
            category = fTrie.getCodePointValue(char32);
            category &= ~0x4000;            // Mask off dictionary bit.
            if (category < 0 || category > fHeader.fCatCount) {
                System.out.println("Error, bad category " + Integer.toHexString(category) + 
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
            System.out.println (intToString(category, 5) + "  " + catStrings[category]);   
        }
        System.out.println();
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
