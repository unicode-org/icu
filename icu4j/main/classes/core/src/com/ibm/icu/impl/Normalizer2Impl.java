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
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.VersionInfo;

public final class Normalizer2Impl {
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

        public static boolean isHangul(int c) {
            return HANGUL_BASE<=c && c<HANGUL_LIMIT;
        }
        public static boolean isHangulWithoutJamoT(char c) {
            c-=HANGUL_BASE;
            return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
        }
        public static boolean isJamoL(int c) {
            return JAMO_L_BASE<=c && c<JAMO_L_LIMIT;
        }
        public static boolean isJamoV(int c) {
            return JAMO_V_BASE<=c && c<JAMO_V_LIMIT;
        }

        /**
         * Decomposes c, which must be a Hangul syllable, into buffer
         * and returns the length of the decomposition (2 or 3).
         */
        public static int decompose(int c, Appendable buffer) {
            try {
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
            } catch(IOException e) {
                // Will not occur because we do not write to I/O.
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Writable buffer that takes care of canonical ordering.
     * Its Appendable methods behave like the C++ implementation's
     * appendZeroCC() methods.
     * <p>
     * If dest is a StringBuilder, then the buffer writes directly to it.
     * Otherwise, the buffer maintains a StringBuilder for intermediate text segments
     * until no further changes are necessary and whole segments are appended.
     * append() methods that take combining-class values always write to the StringBuilder.
     * Other append() methods flush and append to the Appendable.
     */
    public static final class ReorderingBuffer implements Appendable {
        public ReorderingBuffer(Normalizer2Impl ni, Appendable dest, int destCapacity) {
            impl=ni;
            app=dest;
            if(app instanceof StringBuilder) {
                appIsStringBuilder=writeToStringBuilder=true;
                str=(StringBuilder)dest;
                // In Java, the constructor subsumes public void init(int destCapacity) {
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
            } else {
                appIsStringBuilder=writeToStringBuilder=false;
                str=new StringBuilder();
                reorderStart=0;
                lastCC=0;
            }
        }

        public boolean isEmpty() { return str.length()==0; }
        public int length() { return str.length(); }
        public int getLastCC() { return lastCC; }

        public void flush() {
            if(!appIsStringBuilder && str.length()!=0) {
                try {
                    app.append(str);
                } catch(IOException e) {
                    throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                }
                str.delete(0, 0x7fffffff);
                reorderStart=0;
                lastCC=0;
                writeToStringBuilder=appIsStringBuilder;
            }
        }

        // For Hangul composition, replacing the Leading consonant Jamo with the syllable.
        public void setLastChar(char c) {
            str.setCharAt(str.length()-1, c);
        }

        public void append(int c, int cc) {
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
        public void append(CharSequence s, int start, int limit,
                           int leadCC, int trailCC) {
            if(start==limit) {
                return;
            }
            if(lastCC<=leadCC || leadCC==0) {
                if(trailCC<=1) {
                    reorderStart=str.length()+(limit-start);
                } else if(leadCC<=1) {
                    reorderStart=str.length()+1;  // Ok if not a code point boundary.
                }
                str.append(s, start, limit);
                lastCC=trailCC;
            } else {
                int c=Character.codePointAt(s, start);
                start+=Character.charCount(c);
                insert(c, leadCC);  // insert first code point
                while(start<limit) {
                    c=Character.codePointAt(s, start);
                    start+=Character.charCount(c);
                    if(start<limit) {
                        // s must be in NFD, otherwise we need to use getCC().
                        leadCC=getCCFromYesOrMaybe(impl.getNorm16(c));
                    } else {
                        leadCC=trailCC;
                    }
                    append(c, leadCC);
                }
            }
        }
        // The following append() methods work like C++ appendZeroCC().
        // They assume that the cc or trailCC of their input is 0.
        // Most of them implement Appendable interface methods.
        // @Override when we switch to Java 6
        public ReorderingBuffer append(char c) {
            if(writeToStringBuilder) {
                str.append(c);
                reorderStart=str.length();
            } else {
                try {
                    app.append(str).append(c);
                    str.delete(0, 0x7fffffff);
                    reorderStart=0;
                } catch(IOException e) {
                    throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                }
            }
            lastCC=0;
            return this;
        }
        public void appendZeroCC(int c) {
            if(writeToStringBuilder) {
                str.appendCodePoint(c);
                reorderStart=str.length();
            } else {
                try {
                    app.append(str);
                    if(c<=0xffff) {
                        app.append((char)c);
                    } else {
                        char[] pair=Character.toChars(c);
                        app.append(pair[0]).append(pair[1]);
                    }
                    str.delete(0, 0x7fffffff);
                    reorderStart=0;
                } catch(IOException e) {
                    throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                }
            }
            lastCC=0;
        }
        // @Override when we switch to Java 6
        public ReorderingBuffer append(CharSequence s) {
            if(s.length()!=0) {
                if(writeToStringBuilder) {
                    str.append(s);
                    reorderStart=str.length();
                } else {
                    try {
                        app.append(str).append(s);
                        str.delete(0, 0x7fffffff);
                        reorderStart=0;
                    } catch(IOException e) {
                        throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                    }
                }
                lastCC=0;
            }
            return this;
        }
        // @Override when we switch to Java 6
        public ReorderingBuffer append(CharSequence s, int start, int limit) {
            if(start!=limit) {
                if(writeToStringBuilder) {
                    str.append(s, start, limit);
                    reorderStart=str.length();
                } else {
                    try {
                        app.append(str).append(s, start, limit);
                        str.delete(0, 0x7fffffff);
                        reorderStart=0;
                    } catch(IOException e) {
                        throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                    }
                }
                lastCC=0;
            }
            return this;
        }
        public void removeSuffix(int length) {
            int oldLength=str.length();
            str.delete(oldLength-length, oldLength);
            lastCC=0;
            reorderStart=str.length();
        }
        public void forceWriteToStringBuilder() {
            writeToStringBuilder=true;
        }
        public void setReorderingLimit(int newLimit) {
            writeToStringBuilder=appIsStringBuilder;
            if(!appIsStringBuilder) {
                try {
                    app.append(str, 0, newLimit);
                    newLimit=0;
                } catch(IOException e) {
                    throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                }
            }
            str.delete(newLimit, 0x7fffffff);
            reorderStart=newLimit;
            lastCC=0;
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
        private void insert(int c, int cc) {
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

        private final Normalizer2Impl impl;
        private final Appendable app;
        private final StringBuilder str;
        private final boolean appIsStringBuilder;
        private boolean writeToStringBuilder;
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
            if(c<MIN_CCC_LCCC_CP) {
                return 0;
            }
            return getCCFromYesOrMaybe(impl.getNorm16(c));
        }

        private int codePointStart, codePointLimit;
    }

    // TODO: Propose as public API on the UTF16 class.
    public static final class UTF16Plus {
        /**
         * Assuming c is a surrogate code point (UTF16.isSurrogate(c)),
         * is it a lead surrogate?
         * @param c code unit or code point
         * @return true or false
         * @draft ICU 4.6
         */
        public static boolean isSurrogateLead(int c) { return (c&0x400)==0; }
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
    public Normalizer2Impl load(InputStream data) throws IOException {
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
        return this;
    }
    public Normalizer2Impl load(String name) throws IOException {
        return load(ICUData.getRequiredStream(name));
    }

    public void addPropertyStarts(UnicodeSet sa) {
        throw new UnsupportedOperationException();  // TODO
    }

    // low-level properties ------------------------------------------------ ***

    public Trie2_16 getNormTrie() { return normTrie; }
    public Trie2_16 getFCDTrie() {
        throw new UnsupportedOperationException();  // TODO
        // return fcdTrie;  // TODO: build if necessary, with synchronization
    }

    public int getNorm16(int c) { return normTrie.get(c); }

    public Normalizer.QuickCheckResult getCompQuickCheck(int norm16) {
        if(norm16<minNoNo || MIN_YES_YES_WITH_CC<=norm16) {
            return Normalizer.YES;
        } else if(minMaybeYes<=norm16) {
            return Normalizer.MAYBE;
        } else {
            return Normalizer.NO;
        }
    }
    public boolean isCompNo(int norm16) { return minNoNo<=norm16 && norm16<minMaybeYes; }
    public boolean isDecompYes(int norm16) { return norm16<minYesNo || minMaybeYes<=norm16; }

    public int getCC(int norm16) {
        if(norm16>=MIN_NORMAL_MAYBE_YES) {
            return norm16&0xff;
        }
        if(norm16<minNoNo || limitNoNo<=norm16) {
            return 0;
        }
        return getCCFromNoNo(norm16);
    }
    public static int getCCFromYesOrMaybe(int norm16) {
        return norm16>=MIN_NORMAL_MAYBE_YES ? norm16&0xff : 0;
    }

    int getFCD16(int c) { return fcdTrie.get(c); }
    int getFCD16FromSingleLead(char c) { return fcdTrie.getFromU16SingleLead(c); }
/*
    void setFCD16FromNorm16(int start, int end, uint16_t norm16,
                            UTrie2 *newFCDTrie) const;
*/
    /**
     * Get the decomposition for one code point.
     * @param c code point
     * @param buffer out-only buffer gets the decomposition appended
     * @return true if c has a decomposition
     */
    public boolean getDecomposition(int c, Appendable buffer) {
        try {
            int decomp=-1;
            int norm16;
            for(;;) {
                if(c<minDecompNoCP || isDecompYes(norm16=getNorm16(c))) {
                    // TODO: combine the two conditions into one in C++ as well
                    // c does not decompose
                } else if(isHangul(norm16)) {
                    // Hangul syllable: decompose algorithmically
                    Hangul.decompose(c, buffer);
                    return true;
                } else if(isDecompNoAlgorithmic(norm16)) {
                    decomp=c=mapAlgorithmic(c, norm16);
                    continue;
                } else {
                    // c decomposes, get everything from the variable-length extra data
                    int firstUnit=extraData.charAt(norm16++);
                    int length=firstUnit&MAPPING_LENGTH_MASK;
                    if((firstUnit&MAPPING_HAS_CCC_LCCC_WORD)!=0) {
                        ++norm16;
                    }
                    buffer.append(extraData, norm16, norm16+length);
                    return true;
                }
                if(decomp<0) {
                    return false;
                } else if(decomp<=0xffff) {
                    buffer.append((char)decomp);
                } else {
                    char[] surrogatePair=Character.toChars(decomp);
                    buffer.append(surrogatePair[0]).append(surrogatePair[1]);
                }
                return true;
            }
        } catch(IOException e) {
            // Will not occur because we do not write to I/O.
            throw new RuntimeException(e);
        }
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

    public int decompose(CharSequence s, int src, int limit,
                         ReorderingBuffer buffer) {
        int minNoCP=minDecompNoCP;

        int prevSrc;
        int c=0;
        int norm16=0;

        // only for quick check
        int prevBoundary=src;
        int prevCC=0;

        for(;;) {
            // count code units below the minimum or with irrelevant data for the quick check
            for(prevSrc=src; src!=limit;) {
                if( (c=s.charAt(src))<minNoCP ||
                    isMostDecompYesAndZeroCC(norm16=normTrie.getFromU16SingleLead((char)c))
                ) {
                    ++src;
                } else if(!UTF16.isSurrogate((char)c)) {
                    break;
                } else {
                    char c2;
                    if(UTF16Plus.isSurrogateLead(c)) {
                        if((src+1)!=limit && Character.isLowSurrogate(c2=s.charAt(src+1))) {
                            c=Character.toCodePoint((char)c, c2);
                        }
                    } else /* trail surrogate */ {
                        if(prevSrc<src && Character.isHighSurrogate(c2=s.charAt(src-1))) {
                            --src;
                            c=Character.toCodePoint(c2, (char)c);
                        }
                    }
                    if(isMostDecompYesAndZeroCC(norm16=getNorm16(c))) {
                        src+=Character.charCount(c);
                    } else {
                        break;
                    }
                }
            }
            // copy these code units all at once
            if(src!=prevSrc) {
                if(buffer!=null) {
                    buffer.append(s, prevSrc, src);
                } else {
                    prevCC=0;
                    prevBoundary=src;
                }
            }
            if(src==limit) {
                break;
            }

            // Check one above-minimum, relevant code point.
            src+=Character.charCount(c);
            if(buffer!=null) {
                decompose(c, norm16, buffer);
            } else {
                if(isDecompYes(norm16)) {
                    int cc=getCCFromYesOrMaybe(norm16);
                    if(prevCC<=cc || cc==0) {
                        prevCC=cc;
                        if(cc<=1) {
                            prevBoundary=src;
                        }
                        continue;
                    }
                }
                return prevBoundary;  // "no" or cc out of order
            }
        }
        return src;
    }
    public void decomposeAndAppend(CharSequence s, int src, int limit,
                                   boolean doDecompose,
                                   ReorderingBuffer buffer) {
        throw new UnsupportedOperationException();  // TODO
    }
    public boolean compose(CharSequence s, int src, int limit,
                           boolean onlyContiguous,
                           boolean doCompose,
                           ReorderingBuffer buffer) {
        // TODO: use forceWriteToStringBuilder()
        throw new UnsupportedOperationException();  // TODO
    }
    /**
     * @return bits 31..1: spanQuickCheckYes (==s.length() if "yes") and
     *         bit 0: set if "maybe"; if the span length&lt;s.length() and not "maybe"
     *         then the quick check result is "no"
     */
    public int composeQuickCheck(CharSequence s, int src, int limit,
                                 boolean onlyContiguous, boolean doSpan) {
        throw new UnsupportedOperationException();  // TODO
    }
    public void composeAndAppend(CharSequence s, int src, int limit,
                                 boolean doCompose,
                                 boolean onlyContiguous,
                                 ReorderingBuffer buffer) {
        throw new UnsupportedOperationException();  // TODO
    }
    public int makeFCD(CharSequence s, int src, int limit, ReorderingBuffer buffer) {
        throw new UnsupportedOperationException();  // TODO
    }
    public void makeFCDAndAppend(CharSequence s, int src, int limit,
                                 boolean doMakeFCD,
                                 ReorderingBuffer buffer) {
        throw new UnsupportedOperationException();  // TODO
    }

    public boolean hasDecompBoundary(int c, boolean before) {
        throw new UnsupportedOperationException();  // TODO
    }
    public boolean isDecompInert(int c) { return isDecompYesAndZeroCC(getNorm16(c)); }

    public boolean hasCompBoundaryBefore(int c) {
        return c<minCompNoMaybeCP || hasCompBoundaryBefore(c, getNorm16(c));
    }
    public boolean hasCompBoundaryAfter(int c, boolean onlyContiguous, boolean testInert) {
        throw new UnsupportedOperationException();  // TODO
    }

    public boolean hasFCDBoundaryBefore(int c) { return c<MIN_CCC_LCCC_CP || getFCD16(c)<=0xff; }
    public boolean hasFCDBoundaryAfter(int c) {
        int fcd16=getFCD16(c);
        return fcd16<=1 || (fcd16&0xff)==0;
    }
    public boolean isFCDInert(int c) { return getFCD16(c)<=1; }

    private boolean isMaybe(int norm16) { return minMaybeYes<=norm16 && norm16<=JAMO_VT; }
    private boolean isMaybeOrNonZeroCC(int norm16) { return norm16>=minMaybeYes; }
    private static boolean isInert(int norm16) { return norm16==0; }
    // static UBool isJamoL(uint16_t norm16) const { return norm16==1; }
    private static boolean isJamoVT(int norm16) { return norm16==JAMO_VT; }
    private boolean isHangul(int norm16) { return norm16==minYesNo; }
    private boolean isCompYesAndZeroCC(int norm16) { return norm16<minNoNo; }
    // UBool isCompYes(uint16_t norm16) const {
    //     return norm16>=MIN_YES_YES_WITH_CC || norm16<minNoNo;
    // }
    // UBool isCompYesOrMaybe(uint16_t norm16) const {
    //     return norm16<minNoNo || minMaybeYes<=norm16;
    // }
    // private boolean hasZeroCCFromDecompYes(int norm16) {
    //     return norm16<=MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    // }
    private boolean isDecompYesAndZeroCC(int norm16) {
        return norm16<minYesNo ||
               norm16==JAMO_VT ||
               (minMaybeYes<=norm16 && norm16<=MIN_NORMAL_MAYBE_YES);
    }
    /**
     * A little faster and simpler than isDecompYesAndZeroCC() but does not include
     * the MaybeYes which combine-forward and have ccc=0.
     * (Standard Unicode 5.2 normalization does not have such characters.)
     */
    private boolean isMostDecompYesAndZeroCC(int norm16) {
        return norm16<minYesNo || norm16==MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    }
    private boolean isDecompNoAlgorithmic(int norm16) { return norm16>=limitNoNo; }

    // For use with isCompYes().
    // Perhaps the compiler can combine the two tests for MIN_YES_YES_WITH_CC.
    // static uint8_t getCCFromYes(uint16_t norm16) {
    //     return norm16>=MIN_YES_YES_WITH_CC ? (uint8_t)norm16 : 0;
    // }
    private int getCCFromNoNo(int norm16) {
        if((extraData.charAt(norm16)&MAPPING_HAS_CCC_LCCC_WORD)!=0) {
            return extraData.charAt(norm16+1)&0xff;
        } else {
            return 0;
        }
    }
    // requires that the [cpStart..cpLimit[ character passes isCompYesAndZeroCC()
    int getTrailCCFromCompYesAndZeroCC(CharSequence s, int cpStart, int cpLimit) {
        int c;
        if(cpStart==(cpLimit-1)) {
            c=s.charAt(cpStart);
        } else {
            c=Character.codePointAt(s, cpStart);
        }
        int prevNorm16=getNorm16(c);
        if(prevNorm16<=minYesNo) {
            return 0;  // yesYes and Hangul LV/LVT have ccc=tccc=0
        } else {
            return extraData.charAt(prevNorm16)>>8;  // tccc from yesNo
        }
    }

    // Requires algorithmic-NoNo.
    private int mapAlgorithmic(int c, int norm16) {
        return c+norm16-(minMaybeYes-MAX_DELTA-1);
    }

    // Requires minYesNo<norm16<limitNoNo.
    // private int getMapping(int norm16) { return /*extraData+*/norm16; }

    /**
     * @return index into maybeYesCompositions, or -1
     */
    private int getCompositionsListForDecompYesAndZeroCC(int norm16) {
        if(norm16==0 || MIN_NORMAL_MAYBE_YES<=norm16) {
            return -1;
        } else {
            if((norm16-=minMaybeYes)<0) {
                // norm16<minMaybeYes: index into extraData which is a substring at
                //     maybeYesCompositions[MIN_NORMAL_MAYBE_YES-minMaybeYes]
                // same as (MIN_NORMAL_MAYBE_YES-minMaybeYes)+norm16
                norm16+=MIN_NORMAL_MAYBE_YES;  // for yesYes; if Jamo L: harmless empty list
            }
            return norm16;
        }
    }
    /**
     * @return index into maybeYesCompositions
     */
    private int getCompositionsListForComposite(int norm16) {
        // composite has both mapping & compositions list
        int firstUnit=extraData.charAt(norm16);
        return (MIN_NORMAL_MAYBE_YES-minMaybeYes)+norm16+  // mapping in maybeYesCompositions
            1+  // +1 to skip the first unit with the mapping lenth
            (firstUnit&MAPPING_LENGTH_MASK)+  // + mapping length
            ((firstUnit>>7)&1);  // +1 if MAPPING_HAS_CCC_LCCC_WORD
    }

    // Decompose a short piece of text which is likely to contain characters that
    // fail the quick check loop and/or where the quick check loop's overhead
    // is unlikely to be amortized.
    // Called by the compose() and makeFCD() implementations.
    private void decomposeShort(CharSequence s, int src, int limit,
                                ReorderingBuffer buffer) {
        while(src<limit) {
            // TODO: use trie string iterator?? C++ uses UTRIE2_U16_NEXT16(normTrie, src, limit, c, norm16);
            int c=Character.codePointAt(s, src);
            src+=Character.charCount(c);
            decompose(c, getNorm16(c), buffer);
        }
    }
    private void decompose(int c, int norm16,
                           ReorderingBuffer buffer) {
        // Only loops for 1:1 algorithmic mappings.
        for(;;) {
            // get the decomposition and the lead and trail cc's
            if(isDecompYes(norm16)) {
                // c does not decompose
                buffer.append(c, getCCFromYesOrMaybe(norm16));
            } else if(isHangul(norm16)) {
                // Hangul syllable: decompose algorithmically
                Hangul.decompose(c, buffer);
            } else if(isDecompNoAlgorithmic(norm16)) {
                c=mapAlgorithmic(c, norm16);
                norm16=getNorm16(c);
                continue;
            } else {
                // c decomposes, get everything from the variable-length extra data
                int firstUnit=extraData.charAt(norm16++);
                int length=firstUnit&MAPPING_LENGTH_MASK;
                int leadCC, trailCC;
                trailCC=firstUnit>>8;
                if((firstUnit&MAPPING_HAS_CCC_LCCC_WORD)!=0) {
                    leadCC=extraData.charAt(norm16++)>>8;
                } else {
                    leadCC=0;
                }
                buffer.append(extraData, norm16, norm16+length, leadCC, trailCC);
            }
            return;
        }
    }

    private static int combine(CharSequence compositions, int list, int trail) {
        throw new UnsupportedOperationException();  // TODO
    }
    private void recompose(ReorderingBuffer buffer, int recomposeStartIndex,
                           boolean onlyContiguous) {
        throw new UnsupportedOperationException();  // TODO
    }

    private boolean hasCompBoundaryBefore(int c, int norm16) {
        throw new UnsupportedOperationException();  // TODO
    }
    private int findPreviousCompBoundary(CharSequence s, int start, int p) {
        throw new UnsupportedOperationException();  // TODO
    }
    private int findNextCompBoundary(CharSequence s, int p, int limit) {
        throw new UnsupportedOperationException();  // TODO
    }

    private int findPreviousFCDBoundary(CharSequence s, int start, int p) {
        throw new UnsupportedOperationException();  // TODO
    }
    private int findNextFCDBoundary(CharSequence s, int p, int limit) {
        throw new UnsupportedOperationException();  // TODO
    }

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
