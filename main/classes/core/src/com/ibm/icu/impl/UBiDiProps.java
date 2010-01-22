/*
*******************************************************************************
*
*   Copyright (C) 2004-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  UBiDiProps.java
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005jan16
*   created by: Markus W. Scherer
*
*   Low-level Unicode bidi/shaping properties access.
*   Java port of ubidi_props.h/.c.
*/

package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import com.ibm.icu.util.RangeValueIterator;

import com.ibm.icu.text.UnicodeSet;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;

public final class UBiDiProps {
    // constructors etc. --------------------------------------------------- ***

    // port of ubidi_openProps()
    private UBiDiProps() throws IOException{
        InputStream is=ICUData.getStream(ICUResourceBundle.ICU_BUNDLE+"/"+DATA_FILE_NAME);
        BufferedInputStream b=new BufferedInputStream(is, 4096 /* data buffer size */);
        readData(b);
        b.close();
        is.close();
    }

    private UBiDiProps(boolean makeDummy) { // ignore makeDummy, only creates a unique signature
        indexes=new int[IX_TOP];
        indexes[0]=IX_TOP;
        trie=new CharTrie(0, 0, null); // dummy trie, always returns 0
    }


    private void readData(InputStream is) throws IOException {
        DataInputStream inputStream=new DataInputStream(is);

        // read the header
        ICUBinary.readHeader(inputStream, FMT, new IsAcceptable());

        // read indexes[]
        int i, count;
        count=inputStream.readInt();
        if(count<IX_INDEX_TOP) {
            throw new IOException("indexes[0] too small in "+DATA_FILE_NAME);
        }
        indexes=new int[count];

        indexes[0]=count;
        for(i=1; i<count; ++i) {
            indexes[i]=inputStream.readInt();
        }

        // read the trie
        trie=new CharTrie(inputStream, null);

        // read mirrors[]
        count=indexes[IX_MIRROR_LENGTH];
        if(count>0) {
            mirrors=new int[count];
            for(i=0; i<count; ++i) {
                mirrors[i]=inputStream.readInt();
            }
        }

        // read jgArray[]
        count=indexes[IX_JG_LIMIT]-indexes[IX_JG_START];
        jgArray=new byte[count];
        for(i=0; i<count; ++i) {
            jgArray[i]=inputStream.readByte();
        }
    }

    // implement ICUBinary.Authenticate
    private final class IsAcceptable implements ICUBinary.Authenticate {
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0]==1 &&
                   version[2]==Trie.INDEX_STAGE_1_SHIFT_ && version[3]==Trie.INDEX_STAGE_2_SHIFT_;
        }
    }

    // port of ubidi_getSingleton()
    //
    // Note: Do we really need this API?
    public static UBiDiProps getSingleton() throws IOException {
        if (FULL_INSTANCE == null) {
            synchronized (UBiDiProps.class) {
                if (FULL_INSTANCE == null) {
                    FULL_INSTANCE = new UBiDiProps();
                }
            }
        }
        return FULL_INSTANCE;
    }

    /**
     * Get a singleton dummy object, one that works with no real data.
     * This can be used when the real data is not available.
     * Using the dummy can reduce checks for available data after an initial failure.
     * Port of ucase_getDummy().
     */
    // Note: do we really need this API?
    public static UBiDiProps getDummy() {
        if (DUMMY_INSTANCE == null) {
            synchronized (UBiDiProps.class) {
                if (DUMMY_INSTANCE == null) {
                    DUMMY_INSTANCE = new UBiDiProps(true);
                }
            }
        }
        return DUMMY_INSTANCE;
    }

    // set of property starts for UnicodeSet ------------------------------- ***

    public final void addPropertyStarts(UnicodeSet set) {
        int i, length;
        int c, start, limit;

        byte prev, jg;

        /* add the start code point of each same-value range of the trie */
        TrieIterator iter=new TrieIterator(trie);
        RangeValueIterator.Element element=new RangeValueIterator.Element();

        while(iter.next(element)){
            set.add(element.start);
        }

        /* add the code points from the bidi mirroring table */
        length=indexes[IX_MIRROR_LENGTH];
        for(i=0; i<length; ++i) {
            c=getMirrorCodePoint(mirrors[i]);
            set.add(c, c+1);
        }

        /* add the code points from the Joining_Group array where the value changes */
        start=indexes[IX_JG_START];
        limit=indexes[IX_JG_LIMIT];
        length=limit-start;
        prev=0;
        for(i=0; i<length; ++i) {
            jg=jgArray[i];
            if(jg!=prev) {
                set.add(start);
                prev=jg;
            }
            ++start;
        }
        if(prev!=0) {
            /* add the limit code point if the last value was not 0 (it is now start==limit) */
            set.add(limit);
        }

        /* add code points with hardcoded properties, plus the ones following them */

        /* (none right now) */
    }

    // property access functions ------------------------------------------- ***

    public final int getMaxValue(int which) {
        int max;

        max=indexes[IX_MAX_VALUES];
        switch(which) {
        case UProperty.BIDI_CLASS:
            return (max&CLASS_MASK);
        case UProperty.JOINING_GROUP:
            return (max&MAX_JG_MASK)>>MAX_JG_SHIFT;
        case UProperty.JOINING_TYPE:
            return (max&JT_MASK)>>JT_SHIFT;
        default:
            return -1; /* undefined */
        }
    }

    public final int getClass(int c) {
        return getClassFromProps(trie.getCodePointValue(c));
    }

    public final boolean isMirrored(int c) {
        return getFlagFromProps(trie.getCodePointValue(c), IS_MIRRORED_SHIFT);
    }

    public final int getMirror(int c) {
        int props;
        int delta;

        props=trie.getCodePointValue(c);
        delta=((short)props)>>MIRROR_DELTA_SHIFT;
        if(delta!=ESC_MIRROR_DELTA) {
            return c+delta;
        } else {
            /* look for mirror code point in the mirrors[] table */
            int m;
            int i, length;
            int c2;

            length=indexes[IX_MIRROR_LENGTH];

            /* linear search */
            for(i=0; i<length; ++i) {
                m=mirrors[i];
                c2=getMirrorCodePoint(m);
                if(c==c2) {
                    /* found c, return its mirror code point using the index in m */
                    return getMirrorCodePoint(mirrors[getMirrorIndex(m)]);
                } else if(c<c2) {
                    break;
                }
            }

            /* c not found, return it itself */
            return c;
        }
    }

    public final boolean isBidiControl(int c) {
        return getFlagFromProps(trie.getCodePointValue(c), BIDI_CONTROL_SHIFT);
    }

    public final boolean isJoinControl(int c) {
        return getFlagFromProps(trie.getCodePointValue(c), JOIN_CONTROL_SHIFT);
    }

    public final int getJoiningType(int c) {
        return (trie.getCodePointValue(c)&JT_MASK)>>JT_SHIFT;
    }

    public final int getJoiningGroup(int c) {
        int start, limit;

        start=indexes[IX_JG_START];
        limit=indexes[IX_JG_LIMIT];
        if(start<=c && c<limit) {
            return (int)jgArray[c-start]&0xff;
        } else {
            return UCharacter.JoiningGroup.NO_JOINING_GROUP;
        }
    }

    // data members -------------------------------------------------------- ***
    private int indexes[];
    private int mirrors[];
    private byte jgArray[];

    private CharTrie trie;

    // data format constants ----------------------------------------------- ***
    private static final String DATA_NAME="ubidi";
    private static final String DATA_TYPE="icu";
    private static final String DATA_FILE_NAME=DATA_NAME+"."+DATA_TYPE;

    /* format "BiDi" */
    private static final byte FMT[]={ 0x42, 0x69, 0x44, 0x69 };

    /* indexes into indexes[] */
    private static final int IX_INDEX_TOP=0;
    //private static final int IX_LENGTH=1;
    //private static final int IX_TRIE_SIZE=2;
    private static final int IX_MIRROR_LENGTH=3;

    private static final int IX_JG_START=4;
    private static final int IX_JG_LIMIT=5;

    private static final int IX_MAX_VALUES=15;
    private static final int IX_TOP=16;

    // definitions for 16-bit bidi/shaping properties word ----------------- ***

                          /* CLASS_SHIFT=0, */     /* bidi class: 5 bits (4..0) */
    private static final int JT_SHIFT=5;           /* joining type: 3 bits (7..5) */

    /* private static final int _SHIFT=8, reserved: 2 bits (9..8) */

    private static final int JOIN_CONTROL_SHIFT=10;
    private static final int BIDI_CONTROL_SHIFT=11;

    private static final int IS_MIRRORED_SHIFT=12;         /* 'is mirrored' */
    private static final int MIRROR_DELTA_SHIFT=13;        /* bidi mirroring delta: 3 bits (15..13) */

    private static final int MAX_JG_SHIFT=16;              /* max JG value in indexes[MAX_VALUES_INDEX] bits 23..16 */

    private static final int CLASS_MASK=    0x0000001f;
    private static final int JT_MASK=       0x000000e0;

    private static final int MAX_JG_MASK=   0x00ff0000;

    private static final int getClassFromProps(int props) {
        return props&CLASS_MASK;
    }
    private static final boolean getFlagFromProps(int props, int shift) {
        return ((props>>shift)&1)!=0;
    }

    private static final int ESC_MIRROR_DELTA=-4;
    //private static final int MIN_MIRROR_DELTA=-3;
    //private static final int MAX_MIRROR_DELTA=3;

    // definitions for 32-bit mirror table entry --------------------------- ***

    /* the source Unicode code point takes 21 bits (20..0) */
    private static final int MIRROR_INDEX_SHIFT=21;
    //private static final int MAX_MIRROR_INDEX=0x7ff;

    private static final int getMirrorCodePoint(int m) {
        return m&0x1fffff;
    }
    private static final int getMirrorIndex(int m) {
        return m>>>MIRROR_INDEX_SHIFT;
    }


    /*
     * public singleton instance
     */
    public static final UBiDiProps INSTANCE;

    private static volatile UBiDiProps FULL_INSTANCE;
    private static volatile UBiDiProps DUMMY_INSTANCE;

    // This static initializer block must be placed after
    // other static member initialization
    static {
        UBiDiProps bp;
        try {
            bp = new UBiDiProps();
            FULL_INSTANCE = bp;
        } catch (IOException e) {
            // creating dummy
            bp = new UBiDiProps(true);
            DUMMY_INSTANCE = bp;
        }
        INSTANCE = bp;
    }
}
