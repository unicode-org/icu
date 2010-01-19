/*
*******************************************************************************
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.ibm.icu.impl.ICUBinary;
import com.ibm.icu.impl.Trie2_16;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

class Normalizer2Impl {
    public static final class Hangul {
        /* Korean Hangul and Jamo constants */
        public static final int JAMO_L_BASE=0x1100;     /* "lead" jamo */
        public static final int JAMO_V_BASE=0x1161;     /* "vowel" jamo */
        public static final int JAMO_T_BASE=0x11a7;     /* "trail" jamo */

        public static final int HANGUL_BASE=0xac00;

        public static final int JAMO_L_COUNT=19;
        public static final int JAMO_V_COUNT=21;
        public static final int JAMO_T_COUNT=28;

        public static final int JAMO_L_LIMIT=JAMO_L_BASE+JAMO_L_COUNT;
        public static final int JAMO_V_LIMIT=JAMO_V_BASE+JAMO_V_COUNT;

        public static final int HANGUL_COUNT=JAMO_L_COUNT*JAMO_V_COUNT*JAMO_T_COUNT;
        public static final int HANGUL_LIMIT=HANGUL_BASE+HANGUL_COUNT;

        public static final boolean isHangul(int c) {
            return HANGUL_BASE<=c && c<HANGUL_LIMIT;
        }
        public static final boolean isHangulWithoutJamoT(char c) {
            c-=HANGUL_BASE;
            return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
        }
        public static final boolean isJamoL(int c) {
            return JAMO_L_BASE<=c && c<JAMO_L_LIMIT;
        }
        public static final boolean isJamoV(int c) {
            return JAMO_V_BASE<=c && c<JAMO_V_LIMIT;
        }

        /**
         * Decomposes c, which must be a Hangul syllable, into buffer
         * and returns the length of the decomposition (2 or 3).
         */
        public static final int decompose(int c, StringBuilder buffer) {
            c-=HANGUL_BASE;
            int c2=c%JAMO_T_COUNT;
            c/=JAMO_T_COUNT;
            buffer.append((char)(JAMO_L_BASE+c/JAMO_V_COUNT));
            buffer.append((char)(JAMO_V_BASE+c%JAMO_V_COUNT));
            if(c2==0) {
                return 2;
            } else {
                buffer.append((char)(JAMO_T_BASE+c2));
                return 3;
            }
        }
    }

    public static final class ReorderingBuffer {
        public ReorderingBuffer(Normalizer2Impl ni, StringBuilder dest) {
            impl=ni;
            str=dest;
        }
        public final void init(int destCapacity) {
            str.ensureCapacity(destCapacity);
            reorderStart=0;
            if(str.length()==0) {
                lastCC=0;
            } else {
                setIterator();
                lastCC=previousCC();
                // Set reorderStart after the last code point with cc<=1 if there is one.
                if(lastCC>1) {
                    while(previousCC()>1) {}
                }
                reorderStart=codePointLimit;
            }
        }

        public final boolean isEmpty() { return str.length()==0; }
        public final int length() { return str.length(); }
        public final int getLastCC() { return lastCC; }

        public final void append(int c, int cc) {
            if(lastCC<=cc || cc==0) {
                str.appendCodePoint(c);
                lastCC=cc;
                if(cc<=1) {
                    reorderStart=str.length();
                }
            } else {
                insert(c, cc);
            }
        }
        // s must be in NFD, otherwise change the implementation.
        public final void append(CharSequence s, int start, int length,
                                 int leadCC, int trailCC) {
            if(length==0) {
                return;
            }
            if(lastCC<=leadCC || leadCC==0) {
                if(trailCC<=1) {
                    reorderStart=str.length()+length;
                } else if(leadCC<=1) {
                    reorderStart=str.length()+1;  // Ok if not a code point boundary.
                }
                str.append(s, start, start+length);
                lastCC=trailCC;
            } else {
                int limit=start+length;
                int c=Character.codePointAt(s, start);
                start+=Character.charCount(c);
                insert(c, leadCC);  // insert first code point
                while(start<limit) {
                    c=Character.codePointAt(s, start);
                    start+=Character.charCount(c);
                    if(start<limit) {
                        // s must be in NFD, otherwise we need to use getCC().
                        leadCC=Normalizer2Impl.getCCFromYesOrMaybe(impl.getNorm16(c));
                    } else {
                        leadCC=trailCC;
                    }
                    append(c, leadCC);
                }
            }
        }
        public final void appendZeroCC(int c) {
            str.appendCodePoint(c);
            lastCC=0;
            reorderStart=str.length();
        }
        public final void appendZeroCC(CharSequence s, int start, int length) {
            if(length!=0) {
                str.append(s, start, start+length);
                lastCC=0;
                reorderStart=str.length();
            }
        }
        public final void removeZeroCCSuffix(int length) {
            int oldLength=str.length();
            str.delete(oldLength-length, oldLength);
            lastCC=0;
            reorderStart=str.length();
        }
        public final void setReorderingLimitAndLastCC(int newLimit, int newLastCC) {
            str.delete(newLimit, str.length());
            reorderStart=newLimit;
            lastCC=newLastCC;
        }

        /*
         * TODO: Revisit whether it makes sense to track reorderStart.
         * It is set to after the last known character with cc<=1,
         * which stops previousCC() before it reads that character and looks up its cc.
         * previousCC() is normally only called from insert().
         * In other words, reorderStart speeds up the insertion of a combining mark
         * into a multi-combining mark sequence where it does not belong at the end.
         * This might not be worth the trouble.
         * On the other hand, it's not a huge amount of trouble.
         *
         * We probably need it for UNORM_SIMPLE_APPEND.
         */

        // Inserts c somewhere before the last character.
        // Requires 0<cc<lastCC which implies reorderStart<limit.
        private final void insert(int c, int cc) {
            for(setIterator(), skipPrevious(); previousCC()>cc;) {}
            // insert c at codePointLimit, after the character with prevCC<=cc
            if(c<=0xffff) {
                str.insert(codePointLimit, (char)c);
                if(cc<=1) {
                    reorderStart=codePointLimit+1;
                }
            } else {
                str.insert(codePointLimit, Character.toChars(c));
                if(cc<=1) {
                    reorderStart=codePointLimit+2;
                }
            }
        }

        private Normalizer2Impl impl;
        private StringBuilder str;
        private int reorderStart;
        private int lastCC;

        // private backward iterator
        private void setIterator() { codePointStart=str.length(); }
        private void skipPrevious() {  // Requires 0<codePointStart.
            codePointLimit=codePointStart;
            codePointStart=str.offsetByCodePoints(codePointStart, -1);
        }
        private int previousCC() {  // Returns 0 if there is no previous character.
            codePointLimit=codePointStart;
            if(reorderStart>=codePointStart) {
                return 0;
            }
            int c=str.codePointBefore(codePointStart);
            codePointStart-=Character.charCount(c);
            if(c<Normalizer2Impl.MIN_CCC_LCCC_CP) {
                return 0;
            }
            return Normalizer2Impl.getCCFromYesOrMaybe(impl.getNorm16(c));
        }

        private int codePointStart, codePointLimit;
    }

    public Normalizer2Impl() {}

    private static final class Reader implements ICUBinary.Authenticate {
        // @Override when we switch to Java 6
        public boolean isDataVersionAcceptable(byte version[]) {
            return version[0]==1;
        }
        public VersionInfo readHeader(InputStream data) throws IOException {
            byte[] dataVersion=ICUBinary.readHeader(data, DATA_FORMAT, this);
            return VersionInfo.getInstance(dataVersion[0], dataVersion[1],
                                           dataVersion[2], dataVersion[3]);
        }
        private static final byte DATA_FORMAT[] = { 0x4e, 0x72, 0x6d, 0x32  };  // "Nrm2"
    }
    private static final Reader READER=new Reader();
    public final void load(InputStream data) throws IOException {
        BufferedInputStream bis=new BufferedInputStream(data);
        dataVersion=READER.readHeader(bis);
        DataInputStream ds=new DataInputStream(bis);
        int indexesLength=ds.readInt()/4;  // inIndexes[IX_NORM_TRIE_OFFSET]/4
        if(indexesLength<=IX_MIN_MAYBE_YES) {
            throw new IOException("Normalizer2 data: not enough indexes");
        }
        int[] inIndexes=new int[indexesLength];
        inIndexes[0]=indexesLength*4;
        for(int i=1; i<indexesLength; ++i) {
            inIndexes[i]=ds.readInt();
        }

        minDecompNoCP=inIndexes[IX_MIN_DECOMP_NO_CP];
        minCompNoMaybeCP=inIndexes[IX_MIN_COMP_NO_MAYBE_CP];

        minYesNo=inIndexes[IX_MIN_YES_NO];
        minNoNo=inIndexes[IX_MIN_NO_NO];
        limitNoNo=inIndexes[IX_LIMIT_NO_NO];
        minMaybeYes=inIndexes[IX_MIN_MAYBE_YES];

        // Read the normTrie.
        int offset=inIndexes[IX_NORM_TRIE_OFFSET];
        int nextOffset=inIndexes[IX_EXTRA_DATA_OFFSET];
        normTrie=Trie2_16.createFromSerialized(ds);
        int trieLength=normTrie.getSerializedLength();
        if(trieLength>(nextOffset-offset)) {
            throw new IOException("Normalizer2 data: not enough bytes for normTrie");
        }
        ds.skipBytes((nextOffset-offset)-trieLength);  // skip padding after trie bytes

        // Read the composition and mapping data.
        offset=nextOffset;
        nextOffset=inIndexes[IX_RESERVED2_OFFSET];
        int numChars=(nextOffset-offset)/2;
        char[] chars;
        if(numChars!=0) {
            chars=new char[numChars];
            for(int i=0; i<numChars; ++i) {
                chars[i]=ds.readChar();
            }
            maybeYesCompositions=new String(chars);
            extraData=maybeYesCompositions.substring(MIN_NORMAL_MAYBE_YES-minMaybeYes);
        }
        data.close();
    }
    public final void load(ClassLoader root, String name) throws IOException {
        load(ICUData.getRequiredStream(root, name));
    }

    public final void addPropertyStarts(UnicodeSet sa) {
        // TODO
    }

    // low-level properties ------------------------------------------------ ***

    public final Trie2_16 getNormTrie() { return normTrie; }
    public final Trie2_16 getFCDTrie() {
        return fcdTrie;  // TODO: build if necessary, with synchronization
    }

    public final int getNorm16(int c) { return normTrie.get(c); }
/*
    UNormalizationCheckResult getCompQuickCheck(uint16_t norm16) const {
        if(norm16<minNoNo || MIN_YES_YES_WITH_CC<=norm16) {
            return UNORM_YES;
        } else if(minMaybeYes<=norm16) {
            return UNORM_MAYBE;
        } else {
            return UNORM_NO;
        }
    }
    UBool isCompNo(uint16_t norm16) const { return minNoNo<=norm16 && norm16<minMaybeYes; }
    UBool isDecompYes(uint16_t norm16) const { return norm16<minYesNo || minMaybeYes<=norm16; }

    public final int getCC(int norm16) {
        if(norm16>=MIN_NORMAL_MAYBE_YES) {
            return norm16&0xff;
        }
        if(norm16<minNoNo || limitNoNo<=norm16) {
            return 0;
        }
        return getCCFromNoNo(norm16);
    }
*/
    public static final int getCCFromYesOrMaybe(int norm16) {
        return norm16>=MIN_NORMAL_MAYBE_YES ? norm16&0xff : 0;
    }
/*
    uint16_t getFCD16(UChar32 c) const { return UTRIE2_GET16(fcdTrie(), c); }
    uint16_t getFCD16FromBMP(UChar c) const { return UTRIE2_GET16(fcdTrie(), c); }
    uint16_t getFCD16FromSingleLead(UChar c) const {
        return UTRIE2_GET16_FROM_U16_SINGLE_LEAD(fcdTrie(), c);
    }
    uint16_t getFCD16FromSupplementary(UChar32 c) const {
        return UTRIE2_GET16_FROM_SUPP(fcdTrie(), c);
    }
    uint16_t getFCD16FromSurrogatePair(UChar c, UChar c2) const {
        return getFCD16FromSupplementary(U16_GET_SUPPLEMENTARY(c, c2));
    }

    void setFCD16FromNorm16(UChar32 start, UChar32 end, uint16_t norm16,
                            UTrie2 *newFCDTrie) const;
*/
    /**
     * Get the decomposition for one code point.
     * @param c code point
     * @param buffer out-only buffer gets the decomposition appended
     * @return true if c has a decomposition
     */
    public final boolean getDecomposition(int c, StringBuilder buffer) {
        return false; // TODO
    }

    public static final int MIN_CCC_LCCC_CP=0x300;

    public static final int MIN_YES_YES_WITH_CC=0xff01;
    public static final int JAMO_VT=0xff00;
    public static final int MIN_NORMAL_MAYBE_YES=0xfe00;
    public static final int JAMO_L=1;
    public static final int MAX_DELTA=0x40;

    // Byte offsets from the start of the data, after the generic header.
    public static final int IX_NORM_TRIE_OFFSET=0;
    public static final int IX_EXTRA_DATA_OFFSET=1;
    public static final int IX_RESERVED2_OFFSET=2;
    public static final int IX_TOTAL_SIZE=7;

    // Code point thresholds for quick check codes.
    public static final int IX_MIN_DECOMP_NO_CP=8;
    public static final int IX_MIN_COMP_NO_MAYBE_CP=9;

    // Norm16 value thresholds for quick check combinations and types of extra data.
    public static final int IX_MIN_YES_NO=10;
    public static final int IX_MIN_NO_NO=11;
    public static final int IX_LIMIT_NO_NO=12;
    public static final int IX_MIN_MAYBE_YES=13;

    public static final int IX_COUNT=16;

    public static final int MAPPING_HAS_CCC_LCCC_WORD=0x80;
    public static final int MAPPING_PLUS_COMPOSITION_LIST=0x40;
    public static final int MAPPING_NO_COMP_BOUNDARY_AFTER=0x20;
    public static final int MAPPING_LENGTH_MASK=0x1f;

    public static final int COMP_1_LAST_TUPLE=0x8000;
    public static final int COMP_1_TRIPLE=1;
    public static final int COMP_1_TRAIL_LIMIT=0x3400;
    public static final int COMP_1_TRAIL_MASK=0x7ffe;
    public static final int COMP_1_TRAIL_SHIFT=9;  // 10-1 for the "triple" bit
    public static final int COMP_2_TRAIL_SHIFT=6;
    public static final int COMP_2_TRAIL_MASK=0xffc0;

    // higher-level functionality ------------------------------------------ ***
/*
    const UChar *decompose(const UChar *src, const UChar *limit,
                           ReorderingBuffer *buffer) const;
    void decomposeAndAppend(const UChar *src, const UChar *limit,
                            UBool doDecompose,
                            ReorderingBuffer &buffer,
                            UErrorCode &errorCode) const;
    UBool compose(const UChar *src, const UChar *limit,
                  UBool onlyContiguous,
                  UBool doCompose,
                  ReorderingBuffer &buffer,
                  UErrorCode &errorCode) const;
    const UChar *composeQuickCheck(const UChar *src, const UChar *limit,
                                   UBool onlyContiguous,
                                   UNormalizationCheckResult *pQCResult) const;
    void composeAndAppend(const UChar *src, const UChar *limit,
                          UBool doCompose,
                          UBool onlyContiguous,
                          ReorderingBuffer &buffer,
                          UErrorCode &errorCode) const;
    const UChar *makeFCD(const UChar *src, const UChar *limit,
                         ReorderingBuffer *buffer) const;
    void makeFCDAndAppend(const UChar *src, const UChar *limit,
                          UBool doMakeFCD,
                          ReorderingBuffer &buffer,
                          UErrorCode &errorCode) const;

    UBool hasDecompBoundary(UChar32 c, UBool before) const;
    UBool isDecompInert(UChar32 c) const { return isDecompYesAndZeroCC(getNorm16(c)); }

    UBool hasCompBoundaryBefore(UChar32 c) const {
        return c<minCompNoMaybeCP || hasCompBoundaryBefore(c, getNorm16(c));
    }
    UBool hasCompBoundaryAfter(UChar32 c, UBool onlyContiguous, UBool testInert) const;

    UBool hasFCDBoundaryBefore(UChar32 c) const { return c<MIN_CCC_LCCC_CP || getFCD16(c)<=0xff; }
    UBool hasFCDBoundaryAfter(UChar32 c) const {
        uint16_t fcd16=getFCD16(c);
        return fcd16<=1 || (fcd16&0xff)==0;
    }
    UBool isFCDInert(UChar32 c) const { return getFCD16(c)<=1; }
*/
/* private ----
    static UBool U_CALLCONV
    isAcceptable(void *context, const char *type, const char *name, const UDataInfo *pInfo);

    UBool isMaybe(uint16_t norm16) const { return minMaybeYes<=norm16 && norm16<=JAMO_VT; }
    UBool isMaybeOrNonZeroCC(uint16_t norm16) const { return norm16>=minMaybeYes; }
    static UBool isInert(uint16_t norm16) { return norm16==0; }
    // static UBool isJamoL(uint16_t norm16) const { return norm16==1; }
    static UBool isJamoVT(uint16_t norm16) { return norm16==JAMO_VT; }
    UBool isHangul(uint16_t norm16) const { return norm16==minYesNo; }
    UBool isCompYesAndZeroCC(uint16_t norm16) const { return norm16<minNoNo; }
    // UBool isCompYes(uint16_t norm16) const {
    //     return norm16>=MIN_YES_YES_WITH_CC || norm16<minNoNo;
    // }
    // UBool isCompYesOrMaybe(uint16_t norm16) const {
    //     return norm16<minNoNo || minMaybeYes<=norm16;
    // }
    UBool hasZeroCCFromDecompYes(uint16_t norm16) {
        return norm16<=MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    }
    UBool isDecompYesAndZeroCC(uint16_t norm16) const {
        return norm16<minYesNo ||
               norm16==JAMO_VT ||
               (minMaybeYes<=norm16 && norm16<=MIN_NORMAL_MAYBE_YES);
    }
*/
    /**
     * A little faster and simpler than isDecompYesAndZeroCC() but does not include
     * the MaybeYes which combine-forward and have ccc=0.
     * (Standard Unicode 5.2 normalization does not have such characters.)
     */
/*
    UBool isMostDecompYesAndZeroCC(uint16_t norm16) const {
        return norm16<minYesNo || norm16==MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    }
    UBool isDecompNoAlgorithmic(uint16_t norm16) const { return norm16>=limitNoNo; }

    // For use with isCompYes().
    // Perhaps the compiler can combine the two tests for MIN_YES_YES_WITH_CC.
    // static uint8_t getCCFromYes(uint16_t norm16) {
    //     return norm16>=MIN_YES_YES_WITH_CC ? (uint8_t)norm16 : 0;
    // }
    uint8_t getCCFromNoNo(uint16_t norm16) const {
        const uint16_t *mapping=getMapping(norm16);
        if(*mapping&MAPPING_HAS_CCC_LCCC_WORD) {
            return (uint8_t)mapping[1];
        } else {
            return 0;
        }
    }
    // requires that the [cpStart..cpLimit[ character passes isCompYesAndZeroCC()
    uint8_t getTrailCCFromCompYesAndZeroCC(const UChar *cpStart, const UChar *cpLimit) const;

    // Requires algorithmic-NoNo.
    UChar32 mapAlgorithmic(UChar32 c, uint16_t norm16) const {
        return c+norm16-(minMaybeYes-MAX_DELTA-1);
    }

    // Requires minYesNo<norm16<limitNoNo.
    const uint16_t *getMapping(uint16_t norm16) const { return extraData+norm16; }
    const uint16_t *getCompositionsListForDecompYesAndZeroCC(uint16_t norm16) const {
        if(norm16==0 || MIN_NORMAL_MAYBE_YES<=norm16) {
            return NULL;
        } else if(norm16<minMaybeYes) {
            return extraData+norm16;  // for yesYes; if Jamo L: harmless empty list
        } else {
            return maybeYesCompositions+norm16-minMaybeYes;
        }
    }
    const uint16_t *getCompositionsListForComposite(uint16_t norm16) const {
        const uint16_t *list=extraData+norm16;  // composite has both mapping & compositions list
        return list+  // mapping pointer
            1+  // +1 to skip the first unit with the mapping lenth
            (*list&MAPPING_LENGTH_MASK)+  // + mapping length
            ((*list>>7)&1);  // +1 if MAPPING_HAS_CCC_LCCC_WORD
    }

    const UChar *copyLowPrefixFromNulTerminated(const UChar *src,
                                                UChar32 minNeedDataCP,
                                                ReorderingBuffer *buffer,
                                                UErrorCode &errorCode) const;
    UBool decomposeShort(const UChar *src, const UChar *limit,
                         ReorderingBuffer &buffer) const;
    UBool decompose(UChar32 c, uint16_t norm16,
                    ReorderingBuffer &buffer) const;

    static int32_t combine(const uint16_t *list, UChar32 trail);
    void recompose(ReorderingBuffer &buffer, int32_t recomposeStartIndex,
                   UBool onlyContiguous) const;

    UBool hasCompBoundaryBefore(UChar32 c, uint16_t norm16) const;
    const UChar *findPreviousCompBoundary(const UChar *start, const UChar *p) const;
    const UChar *findNextCompBoundary(const UChar *p, const UChar *limit) const;

    const UTrie2 *fcdTrie() const { return (const UTrie2 *)fcdTrieSingleton.fInstance; }

    const UChar *findPreviousFCDBoundary(const UChar *start, const UChar *p) const;
    const UChar *findNextFCDBoundary(const UChar *p, const UChar *limit) const;
*/
    VersionInfo dataVersion;

    // Code point thresholds for quick check codes.
    int minDecompNoCP;
    int minCompNoMaybeCP;

    // Norm16 value thresholds for quick check combinations and types of extra data.
    int minYesNo;
    int minNoNo;
    int limitNoNo;
    int minMaybeYes;

    Trie2_16 normTrie;
    String maybeYesCompositions;
    String extraData;  // mappings and/or compositions for yesYes, yesNo & noNo characters

    Trie2_16 fcdTrie;
}

// TODO: Copy parts of normalizer2impl.h starting with Normalizer2Factory??
