/**
*******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.text;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Locale;

import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.Trie;
import com.ibm.icu.impl.CharTrie;

/**
* <p>Internal class used for Rule Based Break Iterators</p>
* <p>This class provides access to the compiled break rule data, as
* it is stored in a .brk file.  
* 
*/


public class RBBIDataWrapper {
    //
    // These fields are the ready-to-use compiled rule data, as
    //   read from the file.
    //
    public RBBIDataHeader fHeader;
    public short          fFTable[];
    public short          fRTable[];
    public short          fSFTable[];
    public short          fSRTable[];
    public CharTrie       fTrie;
    public String         fRuleSource;
    public int            fStatusTable[];
    
    // Index offsets to the fields in a state table row.
    //    Corresponds to struct RBBIStateTableRow in the C version.
    //   
    final static int      ACCEPTING  = 0;
    final static int      LOOKAHEAD  = 1;
    final static int      TAGIDX     = 2;
    final static int      RESERVED   = 3;
    final static int      NEXTSTATES = 4;
    
    /**
     * Data Header.  A struct-like class with the fields from the RBBI data file header.
     */
    static class RBBIDataHeader {
        int         fMagic;         //  == 0xbla0 
        int         fVersion;       //  == 1 
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
        };
    };
    
    /**
     * RBBI State Table Indexing Function.  Given a state number, return the
     * array index of the start of the state table row for that state.
     * 
     */
    int getRowIndex(int state){
        return state * (fHeader.fCatCount + 4);
    }
    
    static class TrieFoldingFunc implements  Trie.DataManipulate {
        public int getFoldingOffset(int data) {
            if ((data & 0x8000) == 0) {
                return data & 0x7fff;
            } else {
                return 0;
            }
        }
    };
    static TrieFoldingFunc  fTrieFoldingFunc;
 
    
    RBBIDataWrapper() {
     };

    static RBBIDataWrapper get(String name) throws IOException {
        String  fullName = "data/" + name;
        InputStream is = ICUData.getRequiredStream(fullName);
        return get(is);
    }
    
    /*
     *  Get an RBBIDataWrapper from an InputStream onto a pre-compiled set
     *  of RBBI rules.
     */
    static RBBIDataWrapper get(InputStream is) throws IOException {
        int i;
        
        DataInputStream dis = new DataInputStream(is);
        RBBIDataWrapper This = new RBBIDataWrapper();
        
        // Seek past the ICU data header.
        //   TODO:  verify that it looks good.
        dis.skip(0x80);
        
        // Read in the RBBI data header...
        This.fHeader = new  RBBIDataHeader();
        This.fHeader.fMagic          = dis.readInt();
        This.fHeader.fVersion        = dis.readInt();
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
        
        
        if (This.fHeader.fMagic != 0xb1a0) {
            throw new IOException("Break Iterator Rule Data Magic Number Incorrect");
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
        dis.skip(This.fHeader.fTrie - pos);
        pos = This.fHeader.fTrie;
        dis.mark(This.fHeader.fTrieLen+100);
        This.fTrie = new CharTrie(dis, fTrieFoldingFunc);
        dis.reset();
        
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
        StringBuffer sb = new StringBuffer(This.fHeader.fRuleSourceLen / 2);
        for (i=0; i<This.fHeader.fRuleSourceLen; i+=2) {
            sb.append(dis.readChar()); 
            pos += 2;
        }
        This.fRuleSource = sb.toString();
        
        
        return This;
    }
    
    
    
    /** Debug function to display the break iterator data.  */
    void dump() {
        System.out.println("RBBI Data Wrapper dump ...");
        System.out.println("Source Rules: " + fRuleSource);
    }
    
    public static void main(String[] args) {
        String s;
        if (args.length == 0) {
            s = "icudt28b_char.brk";
        } else {
            s = args[0];
        }
        System.out.println("RBBIDataWrapper.main(" + s + ") ");
        try {
            RBBIDataWrapper This = RBBIDataWrapper.get(s);
            This.dump();
        }
       catch (Exception e) {
           System.out.println("Exception: " + e.toString());
       }
           
    }

}
