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
import java.util.Iterator;

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
                appIsStringBuilder=true;
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
                appIsStringBuilder=false;
                str=new StringBuilder();
                reorderStart=0;
                lastCC=0;
            }
        }

        public boolean isEmpty() { return str.length()==0; }
        public int length() { return str.length(); }
        public int getLastCC() { return lastCC; }

        public StringBuilder getStringBuilder() { return str; }

        public boolean equals(CharSequence s, int start, int limit) {
            return UTF16Plus.equal(str, 0, str.length(), s, start, limit);
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
            str.append(c);
            lastCC=0;
            reorderStart=str.length();
            return this;
        }
        public void appendZeroCC(int c) {
            str.appendCodePoint(c);
            lastCC=0;
            reorderStart=str.length();
        }
        // @Override when we switch to Java 6
        public ReorderingBuffer append(CharSequence s) {
            if(s.length()!=0) {
                str.append(s);
                lastCC=0;
                reorderStart=str.length();
            }
            return this;
        }
        // @Override when we switch to Java 6
        public ReorderingBuffer append(CharSequence s, int start, int limit) {
            if(start!=limit) {
                str.append(s, start, limit);
                lastCC=0;
                reorderStart=str.length();
            }
            return this;
        }
        /**
         * Flushes from the intermediate StringBuilder to the Appendable,
         * if they are different objects.
         * Used after recomposition.
         * Must be called at the end when writing to a non-StringBuilder Appendable.
         */
        public void flush() {
            if(appIsStringBuilder) {
                reorderStart=str.length();
            } else {
                try {
                    app.append(str);
                    str.delete(0, 0x7fffffff);
                    reorderStart=0;
                } catch(IOException e) {
                    throw new RuntimeException(e);  // Avoid declaring "throws IOException".
                }
            }
            lastCC=0;
        }
        /**
         * Flushes from the intermediate StringBuilder to the Appendable,
         * if they are different objects.
         * Then appends the new text to the Appendable or StringBuilder.
         * Normally used after quick check loops find a non-empty sequence.
         */
        public ReorderingBuffer flushAndAppendZeroCC(CharSequence s, int start, int limit) {
            if(appIsStringBuilder) {
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
            return this;
        }
        public void remove() {
            str.delete(0, 0x7fffffff);
            lastCC=0;
            reorderStart=0;
        }
        public void removeSuffix(int length) {
            int oldLength=str.length();
            str.delete(oldLength-length, oldLength);
            lastCC=0;
            reorderStart=str.length();
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
        /**
         * Compares two CharSequence subsequences for binary equality.
         * @param s1 first sequence
         * @param start1 start offset in first sequence
         * @param limit1 limit offset in first sequence
         * @param s2 second sequence
         * @param start2 start offset in second sequence
         * @param limit2 limit offset in second sequence
         * @return true if s1.subSequence(start1, limit1) contains the same text
         *              as s2.subSequence(start2, limit2)
         * @draft ICU 4.6
         */
        public static boolean equal(CharSequence s1, int start1, int limit1,
                                    CharSequence s2, int start2, int limit2) {
            if((limit1-start1)!=(limit2-start2)) {
                return false;
            }
            while(start1<limit1) {
                if(s1.charAt(start1++)!=s2.charAt(start2++)) {
                    return false;
                }
            }
            return true;
        }
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

    public void addPropertyStarts(UnicodeSet set) {
        /* add the start code point of each same-value range of each trie */
        Iterator<Trie2.Range> trieIterator=normTrie.iterator();
        while(trieIterator.hasNext()) {
            /* add the start code point to the USet */
            set.add(trieIterator.next().startCodePoint);
        }

        /* add Hangul LV syllables and LV+1 because of skippables */
        for(int c=Hangul.HANGUL_BASE; c<Hangul.HANGUL_LIMIT; c+=Hangul.JAMO_T_COUNT) {
            set.add(c);
            set.add(c+1);
        }
        set.add(Hangul.HANGUL_LIMIT); /* add Hangul+1 to continue with other properties */
    }

    // low-level properties ------------------------------------------------ ***

    public Trie2_16 getNormTrie() { return normTrie; }
    public synchronized Trie2_16 getFCDTrie() {
        if(fcdTrie!=null) {
            return fcdTrie;
        }
        Trie2Writable newFCDTrie=new Trie2Writable(0, 0);
        Iterator<Trie2.Range> trieIterator=normTrie.iterator();
        while(trieIterator.hasNext()) {
            // Set the FCD value for a range of same-norm16 charcters.
            Trie2.Range range=trieIterator.next();
            if(range.value!=0) {
                setFCD16FromNorm16(range.startCodePoint, range.endCodePoint, range.value, newFCDTrie);
            }
        }
        for(char lead=0xd800; lead<0xdc00; ++lead) {
            // Collect (OR together) the FCD values for a range of supplementary characters,
            // for their lead surrogate code unit.
            int oredValue=newFCDTrie.get(lead);
            trieIterator=normTrie.iteratorForLeadSurrogate(lead);
            while(trieIterator.hasNext()) {
                oredValue|=trieIterator.next().value;
            }
            if(oredValue!=0) {
                // Set a "bad" value for makeFCD() to break the quick check loop
                // and look up the value for the supplementary code point.
                // If there is any lccc, then set the worst-case lccc of 1.
                // The ORed-together value's tccc is already the worst case.
                if(oredValue>0xff) {
                    oredValue=0x100|(oredValue&0xff);
                }
                newFCDTrie.setForLeadSurrogateCodeUnit(lead, oredValue);
            }
        }
        return fcdTrie=newFCDTrie.toTrie2_16();
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

    void setFCD16FromNorm16(int start, int end, int norm16, Trie2Writable newFCDTrie) {
        // Only loops for 1:1 algorithmic mappings.
        for(;;) {
            if(norm16>=MIN_NORMAL_MAYBE_YES) {
                norm16&=0xff;
                norm16|=norm16<<8;
            } else if(norm16<=minYesNo || minMaybeYes<=norm16) {
                // no decomposition or Hangul syllable, all zeros
                break;
            } else if(limitNoNo<=norm16) {
                int delta=norm16-(minMaybeYes-MAX_DELTA-1);
                if(start==end) {
                    start+=delta;
                    norm16=getNorm16(start);
                } else {
                    // the same delta leads from different original characters to different mappings
                    do {
                        int c=start+delta;
                        setFCD16FromNorm16(c, c, getNorm16(c), newFCDTrie);
                    } while(++start<=end);
                    break;
                }
            } else {
                // c decomposes, get everything from the variable-length extra data
                int firstUnit=extraData.charAt(norm16);
                if((firstUnit&MAPPING_LENGTH_MASK)==0) {
                    // A character that is deleted (maps to an empty string) must
                    // get the worst-case lccc and tccc values because arbitrary
                    // characters on both sides will become adjacent.
                    norm16=0x1ff;
                } else {
                    if((firstUnit&MAPPING_HAS_CCC_LCCC_WORD)!=0) {
                        norm16=extraData.charAt(norm16+1)&0xff00;  // lccc
                    } else {
                        norm16=0;
                    }
                    norm16|=firstUnit>>8;  // tccc
                }
            }
            newFCDTrie.setRange(start, end, norm16, true);
            break;
        }
    }

    /**
     * Get the decomposition for one code point.
     * @param c code point
     * @param buffer out-only buffer gets the decomposition appended
     * @return true if c has a decomposition
     * TODO: Look at the call sites and see if it would be better to return a String
     * rather than writing to an Appendable.
     */
    public boolean getDecomposition(int c, Appendable buffer) {
        try {
            int decomp=-1;
            int norm16;
            for(;;) {
                if(c<minDecompNoCP || isDecompYes(norm16=getNorm16(c))) {
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
                    buffer.flushAndAppendZeroCC(s, prevSrc, src);
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
    // Very similar to composeQuickCheck(): Make the same changes in both places if relevant.
    // doCompose: normalize
    // !doCompose: isNormalized (buffer must be empty and initialized)
    public boolean compose(CharSequence s, int src, int limit,
                           boolean onlyContiguous,
                           boolean doCompose,
                           ReorderingBuffer buffer) {
        int minNoMaybeCP=minCompNoMaybeCP;

        /*
         * prevBoundary points to the last character before the current one
         * that has a composition boundary before it with ccc==0 and quick check "yes".
         * Keeping track of prevBoundary saves us looking for a composition boundary
         * when we find a "no" or "maybe".
         *
         * When we back out from prevSrc back to prevBoundary,
         * then we also remove those same characters (which had been simply copied
         * or canonically-order-inserted) from the ReorderingBuffer.
         * Therefore, at all times, the [prevBoundary..prevSrc[ source units
         * must correspond 1:1 to destination units at the end of the destination buffer.
         */
        int prevBoundary=src;
        int prevSrc;
        int c=0;
        int norm16=0;

        // only for isNormalized
        int prevCC=0;

        for(;;) {
            // count code units below the minimum or with irrelevant data for the quick check
            for(prevSrc=src; src!=limit;) {
                if( (c=s.charAt(src))<minNoMaybeCP ||
                    isCompYesAndZeroCC(norm16=normTrie.getFromU16SingleLead((char)c))
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
                    if(isCompYesAndZeroCC(norm16=getNorm16(c))) {
                        src+=Character.charCount(c);
                    } else {
                        break;
                    }
                }
            }
            // copy these code units all at once
            if(src!=prevSrc) {
                if(src==limit) {
                    if(doCompose) {
                        buffer.flushAndAppendZeroCC(s, prevSrc, src);
                    }
                    break;
                }
                // Set prevBoundary to the last character in the quick check loop.
                prevBoundary=src-1;
                if( Character.isLowSurrogate(s.charAt(prevBoundary)) && prevSrc<prevBoundary &&
                    Character.isHighSurrogate(s.charAt(prevBoundary-1))
                ) {
                    --prevBoundary;
                }
                if(doCompose) {
                    // The last "quick check yes" character is excluded from the
                    // flush-and-append call in case it needs to be modified.
                    buffer.flushAndAppendZeroCC(s, prevSrc, prevBoundary);
                    buffer.append(s, prevBoundary, src);
                } else {
                    prevCC=0;
                }
                // The start of the current character (c).
                prevSrc=src;
            } else if(src==limit) {
                break;
            }

            src+=Character.charCount(c);
            /*
             * isCompYesAndZeroCC(norm16) is false, that is, norm16>=minNoNo.
             * c is either a "noNo" (has a mapping) or a "maybeYes" (combines backward)
             * or has ccc!=0.
             * Check for Jamo V/T, then for regular characters.
             * c is not a Hangul syllable or Jamo L because those have "yes" properties.
             */
            if(isJamoVT(norm16) && prevBoundary!=prevSrc) {
                char prev=s.charAt(prevSrc-1);
                boolean needToDecompose=false;
                if(c<Hangul.JAMO_T_BASE) {
                    // c is a Jamo Vowel, compose with previous Jamo L and following Jamo T.
                    prev-=Hangul.JAMO_L_BASE;
                    if(prev<Hangul.JAMO_L_COUNT) {
                        if(!doCompose) {
                            return false;
                        }
                        char syllable=(char)
                            (Hangul.HANGUL_BASE+
                             (prev*Hangul.JAMO_V_COUNT+(c-Hangul.JAMO_V_BASE))*
                             Hangul.JAMO_T_COUNT);
                        char t;
                        if(src!=limit && (t=(char)(s.charAt(src)-Hangul.JAMO_T_BASE))<Hangul.JAMO_T_COUNT) {
                            ++src;
                            syllable+=t;  // The next character was a Jamo T.
                            prevBoundary=src;
                            buffer.setLastChar(syllable);
                            continue;
                        }
                        // If we see L+V+x where x!=T then we drop to the slow path,
                        // decompose and recompose.
                        // This is to deal with NFKC finding normal L and V but a
                        // compatibility variant of a T. We need to either fully compose that
                        // combination here (which would complicate the code and may not work
                        // with strange custom data) or use the slow path -- or else our replacing
                        // two input characters (L+V) with one output character (LV syllable)
                        // would violate the invariant that [prevBoundary..prevSrc[ has the same
                        // length as what we appended to the buffer since prevBoundary.
                        needToDecompose=true;
                    }
                } else if(Hangul.isHangulWithoutJamoT(prev)) {
                    // c is a Jamo Trailing consonant,
                    // compose with previous Hangul LV that does not contain a Jamo T.
                    if(!doCompose) {
                        return false;
                    }
                    buffer.setLastChar((char)(prev+c-Hangul.JAMO_T_BASE));
                    prevBoundary=src;
                    continue;
                }
                if(!needToDecompose) {
                    // The Jamo V/T did not compose into a Hangul syllable.
                    if(doCompose) {
                        buffer.append((char)c);
                    } else {
                        prevCC=0;
                    }
                    continue;
                }
            }
            /*
             * Source buffer pointers:
             *
             *  all done      quick check   current char  not yet
             *                "yes" but     (c)           processed
             *                may combine
             *                forward
             * [-------------[-------------[-------------[-------------[
             * |             |             |             |             |
             * orig. src     prevBoundary  prevSrc       src           limit
             *
             *
             * Destination buffer pointers inside the ReorderingBuffer:
             *
             *  all done      might take    not filled yet
             *                characters for
             *                reordering
             * [-------------[-------------[-------------[
             * |             |             |             |
             * start         reorderStart  limit         |
             *                             +remainingCap.+
             */
            if(norm16>=MIN_YES_YES_WITH_CC) {
                int cc=norm16&0xff;  // cc!=0
                if( onlyContiguous &&  // FCC
                    (doCompose ? buffer.getLastCC() : prevCC)==0 &&
                    prevBoundary<prevSrc &&
                    // buffer.getLastCC()==0 && prevBoundary<prevSrc tell us that
                    // [prevBoundary..prevSrc[ (which is exactly one character under these conditions)
                    // passed the quick check "yes && ccc==0" test.
                    // Check whether the last character was a "yesYes" or a "yesNo".
                    // If a "yesNo", then we get its trailing ccc from its
                    // mapping and check for canonical order.
                    // All other cases are ok.
                    getTrailCCFromCompYesAndZeroCC(s, prevBoundary, prevSrc)>cc
                ) {
                    // Fails FCD test, need to decompose and contiguously recompose.
                    if(!doCompose) {
                        return false;
                    }
                } else if(doCompose) {
                    buffer.append(c, cc);
                    continue;
                } else if(prevCC<=cc) {
                    prevCC=cc;
                    continue;
                } else {
                    return false;
                }
            } else if(!doCompose && !isMaybeOrNonZeroCC(norm16)) {
                return false;
            }

            /*
             * Find appropriate boundaries around this character,
             * decompose the source text from between the boundaries,
             * and recompose it.
             *
             * We may need to remove the last few characters from the ReorderingBuffer
             * to account for source text that was copied or appended
             * but needs to take part in the recomposition.
             */

            /*
             * Find the last composition boundary in [prevBoundary..src[.
             * It is either the decomposition of the current character (at prevSrc),
             * or prevBoundary.
             */
            if(hasCompBoundaryBefore(c, norm16)) {
                prevBoundary=prevSrc;
            } else if(doCompose) {
                buffer.removeSuffix(prevSrc-prevBoundary);
            }

            // Find the next composition boundary in [src..limit[ -
            // modifies src to point to the next starter.
            src=findNextCompBoundary(s, src, limit);

            // Decompose [prevBoundary..src[ into the buffer and then recompose that part of it.
            int recomposeStartIndex=buffer.length();
            decomposeShort(s, prevBoundary, src, buffer);
            recompose(buffer, recomposeStartIndex, onlyContiguous);
            if(!doCompose) {
                if(!buffer.equals(s, prevBoundary, src)) {
                    return false;
                }
                buffer.remove();
                prevCC=0;
            }

            // Move to the next starter. We never need to look back before this point again.
            prevBoundary=src;
        }
        return true;
    }
    /**
     * Very similar to compose(): Make the same changes in both places if relevant.
     * doSpan: spanQuickCheckYes (ignore bit 0 of the return value)
     * !doSpan: quickCheck
     * @return bits 31..1: spanQuickCheckYes (==s.length() if "yes") and
     *         bit 0: set if "maybe"; otherwise, if the span length&lt;s.length()
     *         then the quick check result is "no"
     */
    public int composeQuickCheck(CharSequence s, int src, int limit,
                                 boolean onlyContiguous, boolean doSpan) {
        int qcResult=0;
        int minNoMaybeCP=minCompNoMaybeCP;

        /*
         * prevBoundary points to the last character before the current one
         * that has a composition boundary before it with ccc==0 and quick check "yes".
         */
        int prevBoundary=src;
        int prevSrc;
        int c=0;
        int norm16=0;
        int prevCC=0;

        for(;;) {
            // count code units below the minimum or with irrelevant data for the quick check
            for(prevSrc=src;;) {
                if(src==limit) {
                    return (src<<1)|qcResult;  // "yes" or "maybe"
                }
                if( (c=s.charAt(src))<minNoMaybeCP ||
                    isCompYesAndZeroCC(norm16=normTrie.getFromU16SingleLead((char)c))
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
                    if(isCompYesAndZeroCC(norm16=getNorm16(c))) {
                        src+=Character.charCount(c);
                    } else {
                        break;
                    }
                }
            }
            if(src!=prevSrc) {
                // Set prevBoundary to the last character in the quick check loop.
                prevBoundary=src-1;
                if( Character.isLowSurrogate(s.charAt(prevBoundary)) && prevSrc<prevBoundary &&
                        Character.isHighSurrogate(s.charAt(prevBoundary-1))
                ) {
                    --prevBoundary;
                }
                prevCC=0;
                // The start of the current character (c).
                prevSrc=src;
            }

            src+=Character.charCount(c);
            /*
             * isCompYesAndZeroCC(norm16) is false, that is, norm16>=minNoNo.
             * c is either a "noNo" (has a mapping) or a "maybeYes" (combines backward)
             * or has ccc!=0.
             */
            if(isMaybeOrNonZeroCC(norm16)) {
                int cc=getCCFromYesOrMaybe(norm16);
                if( onlyContiguous &&  // FCC
                    cc!=0 &&
                    prevCC==0 &&
                    prevBoundary<prevSrc &&
                    // prevCC==0 && prevBoundary<prevSrc tell us that
                    // [prevBoundary..prevSrc[ (which is exactly one character under these conditions)
                    // passed the quick check "yes && ccc==0" test.
                    // Check whether the last character was a "yesYes" or a "yesNo".
                    // If a "yesNo", then we get its trailing ccc from its
                    // mapping and check for canonical order.
                    // All other cases are ok.
                    getTrailCCFromCompYesAndZeroCC(s, prevBoundary, prevSrc)>cc
                ) {
                    // Fails FCD test.
                } else if(prevCC<=cc || cc==0) {
                    prevCC=cc;
                    if(norm16<MIN_YES_YES_WITH_CC) {
                        if(!doSpan) {
                            qcResult=1;
                        } else {
                            return prevBoundary<<1;  // spanYes does not care to know it's "maybe"
                        }
                    }
                    continue;
                }
            }
            return prevBoundary<<1;  // "no"
        }
    }
    public void composeAndAppend(CharSequence s, int src, int limit,
                                 boolean doCompose,
                                 boolean onlyContiguous,
                                 ReorderingBuffer buffer) {
        throw new UnsupportedOperationException();  // TODO
    }
    public int makeFCD(CharSequence s, int src, int limit, ReorderingBuffer buffer) {
        // Note: In this function we use buffer->appendZeroCC() because we track
        // the lead and trail combining classes here, rather than leaving it to
        // the ReorderingBuffer.
        // The exception is the call to decomposeShort() which uses the buffer
        // in the normal way.

        // Tracks the last FCD-safe boundary, before lccc=0 or after properly-ordered tccc<=1.
        // Similar to the prevBoundary in the compose() implementation.
        int prevBoundary=src;
        int prevSrc;
        int c=0;
        int prevFCD16=0;
        int fcd16=0;

        for(;;) {
            // count code units with lccc==0
            for(prevSrc=src; src!=limit;) {
                if((c=s.charAt(src))<MIN_CCC_LCCC_CP) {
                    prevFCD16=~c;
                    ++src;
                } else if((fcd16=fcdTrie.getFromU16SingleLead((char)c))<=0xff) {
                    prevFCD16=fcd16;
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
                    if((fcd16=getFCD16(c))<=0xff) {
                        prevFCD16=fcd16;
                        src+=Character.charCount(c);
                    } else {
                        break;
                    }
                }
            }
            // copy these code units all at once
            if(src!=prevSrc) {
                if(src==limit) {
                    if(buffer!=null) {
                        buffer.flushAndAppendZeroCC(s, prevSrc, src);
                    }
                    break;
                }
                prevBoundary=src;
                // We know that the previous character's lccc==0.
                if(prevFCD16<0) {
                    // Fetching the fcd16 value was deferred for this below-U+0300 code point.
                    prevFCD16=getFCD16FromSingleLead((char)~prevFCD16);
                    if(prevFCD16>1) {
                        --prevBoundary;
                    }
                } else {
                    int p=src-1;
                    if( Character.isLowSurrogate(s.charAt(p)) && prevSrc<p &&
                        Character.isHighSurrogate(s.charAt(p-1))
                    ) {
                        --p;
                        // Need to fetch the previous character's FCD value because
                        // prevFCD16 was just for the trail surrogate code point.
                        prevFCD16=getFCD16(Character.toCodePoint(s.charAt(p), s.charAt(p+1)));
                        // Still known to have lccc==0 because its lead surrogate unit had lccc==0.
                    }
                    if(prevFCD16>1) {
                        prevBoundary=p;
                    }
                }
                if(buffer!=null) {
                    // The last lccc==0 character is excluded from the
                    // flush-and-append call in case it needs to be modified.
                    buffer.flushAndAppendZeroCC(s, prevSrc, prevBoundary);
                    buffer.append(s, prevBoundary, src);
                }
                // The start of the current character (c).
                prevSrc=src;
            } else if(src==limit) {
                break;
            }

            src+=Character.charCount(c);
            // The current character (c) at [prevSrc..src[ has a non-zero lead combining class.
            // Check for proper order, and decompose locally if necessary.
            if((prevFCD16&0xff)<=(fcd16>>8)) {
                // proper order: prev tccc <= current lccc
                if((fcd16&0xff)<=1) {
                    prevBoundary=src;
                }
                if(buffer!=null) {
                    buffer.appendZeroCC(c);
                }
                prevFCD16=fcd16;
                continue;
            } else if(buffer==null) {
                return prevBoundary;  // quick check "no"
            } else {
                /*
                 * Back out the part of the source that we copied or appended
                 * already but is now going to be decomposed.
                 * prevSrc is set to after what was copied/appended.
                 */
                buffer.removeSuffix(prevSrc-prevBoundary);
                /*
                 * Find the part of the source that needs to be decomposed,
                 * up to the next safe boundary.
                 */
                src=findNextFCDBoundary(s, src, limit);
                /*
                 * The source text does not fulfill the conditions for FCD.
                 * Decompose and reorder a limited piece of the text.
                 */
                decomposeShort(s, prevBoundary, src, buffer);
                prevBoundary=src;
                prevFCD16=0;
            }
        }
        return src;
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

    /*
     * Finds the recomposition result for
     * a forward-combining "lead" character,
     * specified with a pointer to its compositions list,
     * and a backward-combining "trail" character.
     *
     * If the lead and trail characters combine, then this function returns
     * the following "compositeAndFwd" value:
     * Bits 21..1  composite character
     * Bit      0  set if the composite is a forward-combining starter
     * otherwise it returns -1.
     *
     * The compositions list has (trail, compositeAndFwd) pair entries,
     * encoded as either pairs or triples of 16-bit units.
     * The last entry has the high bit of its first unit set.
     *
     * The list is sorted by ascending trail characters (there are no duplicates).
     * A linear search is used.
     *
     * See normalizer2impl.h for a more detailed description
     * of the compositions list format.
     */
    private static int combine(String compositions, int list, int trail) {
        int key1, firstUnit;
        if(trail<COMP_1_TRAIL_LIMIT) {
            // trail character is 0..33FF
            // result entry may have 2 or 3 units
            key1=(trail<<1);
            while(key1>(firstUnit=compositions.charAt(list))) {
                list+=2+(firstUnit&COMP_1_TRIPLE);
            }
            if(key1==(firstUnit&COMP_1_TRAIL_MASK)) {
                if((firstUnit&COMP_1_TRIPLE)!=0) {
                    return ((int)compositions.charAt(list+1)<<16)|compositions.charAt(list+2);
                } else {
                    return compositions.charAt(list+1);
                }
            }
        } else {
            // trail character is 3400..10FFFF
            // result entry has 3 units
            key1=COMP_1_TRAIL_LIMIT+((trail>>COMP_1_TRAIL_SHIFT))&~COMP_1_TRIPLE;
            int key2=(trail<<COMP_2_TRAIL_SHIFT)&0xffff;
            int secondUnit;
            for(;;) {
                if(key1>(firstUnit=compositions.charAt(list))) {
                    list+=2+(firstUnit&COMP_1_TRIPLE);
                } else if(key1==(firstUnit&COMP_1_TRAIL_MASK)) {
                    if(key2>(secondUnit=compositions.charAt(list+1))) {
                        if((firstUnit&COMP_1_LAST_TUPLE)!=0) {
                            break;
                        } else {
                            list+=3;
                        }
                    } else if(key2==(secondUnit&COMP_2_TRAIL_MASK)) {
                        return ((secondUnit&~COMP_2_TRAIL_MASK)<<16)|compositions.charAt(list+2);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return -1;
    }
    /*
     * Recomposes the buffer text starting at recomposeStartIndex
     * (which is in NFD - decomposed and canonically ordered),
     * and truncates the buffer contents.
     *
     * Note that recomposition never lengthens the text:
     * Any character consists of either one or two code units;
     * a composition may contain at most one more code unit than the original starter,
     * while the combining mark that is removed has at least one code unit.
     */
    private void recompose(ReorderingBuffer buffer, int recomposeStartIndex,
                           boolean onlyContiguous) {
        StringBuilder sb=buffer.getStringBuilder();
        int p=recomposeStartIndex;
        if(p==sb.length()) {
            return;
        }

        int starter, pRemove;
        int compositionsList;
        int c, compositeAndFwd;
        int norm16;
        int cc, prevCC;
        boolean starterIsSupplementary;

        // Some of the following variables are not used until we have a forward-combining starter
        // and are only initialized now to avoid compiler warnings.
        compositionsList=-1;  // used as indicator for whether we have a forward-combining starter
        starter=-1;
        starterIsSupplementary=false;
        prevCC=0;

        for(;;) {
            c=sb.codePointAt(p);
            p+=Character.charCount(c);
            norm16=getNorm16(c);
            // TODO: use trie string iterator?? C++ uses UTRIE2_U16_NEXT16(normTrie, p, limit, c, norm16);
            cc=getCCFromYesOrMaybe(norm16);
            if( // this character combines backward and
                isMaybe(norm16) &&
                // we have seen a starter that combines forward and
                compositionsList>=0 &&
                // the backward-combining character is not blocked
                (prevCC<cc || prevCC==0)
            ) {
                if(isJamoVT(norm16)) {
                    // c is a Jamo V/T, see if we can compose it with the previous character.
                    if(c<Hangul.JAMO_T_BASE) {
                        // c is a Jamo Vowel, compose with previous Jamo L and following Jamo T.
                        char prev=(char)(sb.charAt(starter)-Hangul.JAMO_L_BASE);
                        if(prev<Hangul.JAMO_L_COUNT) {
                            pRemove=p-1;
                            char syllable=(char)
                                (Hangul.HANGUL_BASE+
                                 (prev*Hangul.JAMO_V_COUNT+(c-Hangul.JAMO_V_BASE))*
                                 Hangul.JAMO_T_COUNT);
                            char t;
                            if(p!=sb.length() && (t=(char)(sb.charAt(p)-Hangul.JAMO_T_BASE))<Hangul.JAMO_T_COUNT) {
                                ++p;
                                syllable+=t;  // The next character was a Jamo T.
                            }
                            sb.setCharAt(starter, syllable);
                            // remove the Jamo V/T
                            sb.delete(pRemove, p);
                            p=pRemove;
                        }
                    }
                    /*
                     * No "else" for Jamo T:
                     * Since the input is in NFD, there are no Hangul LV syllables that
                     * a Jamo T could combine with.
                     * All Jamo Ts are combined above when handling Jamo Vs.
                     */
                    if(p==sb.length()) {
                        break;
                    }
                    compositionsList=-1;
                    continue;
                } else if((compositeAndFwd=combine(maybeYesCompositions, compositionsList, c))>=0) {
                    // The starter and the combining mark (c) do combine.
                    int composite=compositeAndFwd>>1;

                    // Remove the combining mark.
                    pRemove=p-Character.charCount(c);  // pRemove & p: start & limit of the combining mark
                    sb.delete(pRemove, p);
                    p=pRemove;
                    // Replace the starter with the composite.
                    if(starterIsSupplementary) {
                        if(composite>0xffff) {
                            // both are supplementary
                            sb.setCharAt(starter, UTF16.getLeadSurrogate(composite));
                            sb.setCharAt(starter+1, UTF16.getTrailSurrogate(composite));
                        } else {
                            sb.setCharAt(starter, (char)c);
                            sb.deleteCharAt(starter+1);
                            // The composite is shorter than the starter,
                            // move the intermediate characters forward one.
                            starterIsSupplementary=false;
                            --p;
                        }
                    } else if(composite>0xffff) {
                        // The composite is longer than the starter,
                        // move the intermediate characters back one.
                        starterIsSupplementary=true;
                        sb.setCharAt(starter, UTF16.getLeadSurrogate(composite));
                        sb.insert(starter+1, UTF16.getTrailSurrogate(composite));
                        ++p;
                    } else {
                        // both are on the BMP
                        sb.setCharAt(starter, (char)composite);
                    }

                    // Keep prevCC because we removed the combining mark.

                    if(p==sb.length()) {
                        break;
                    }
                    // Is the composite a starter that combines forward?
                    if((compositeAndFwd&1)!=0) {
                        compositionsList=
                            getCompositionsListForComposite(getNorm16(composite));
                    } else {
                        compositionsList=-1;
                    }

                    // We combined; continue with looking for compositions.
                    continue;
                }
            }

            // no combination this time
            prevCC=cc;
            if(p==sb.length()) {
                break;
            }

            // If c did not combine, then check if it is a starter.
            if(cc==0) {
                // Found a new starter.
                if((compositionsList=getCompositionsListForDecompYesAndZeroCC(norm16))>=0) {
                    // It may combine with something, prepare for it.
                    if(c<=0xffff) {
                        starterIsSupplementary=false;
                        starter=p-1;
                    } else {
                        starterIsSupplementary=true;
                        starter=p-2;
                    }
                }
            } else if(onlyContiguous) {
                // FCC: no discontiguous compositions; any intervening character blocks.
                compositionsList=-1;
            }
        }
        buffer.flush();
    }

    /**
     * Does c have a composition boundary before it?
     * True if its decomposition begins with a character that has
     * ccc=0 && NFC_QC=Yes (isCompYesAndZeroCC()).
     * As a shortcut, this is true if c itself has ccc=0 && NFC_QC=Yes
     * (isCompYesAndZeroCC()) so we need not decompose.
     */
    private boolean hasCompBoundaryBefore(int c, int norm16) {
        for(;;) {
            if(isCompYesAndZeroCC(norm16)) {
                return true;
            } else if(isMaybeOrNonZeroCC(norm16)) {
                return false;
            } else if(isDecompNoAlgorithmic(norm16)) {
                c=mapAlgorithmic(c, norm16);
                norm16=getNorm16(c);
            } else {
                // c decomposes, get everything from the variable-length extra data
                int firstUnit=extraData.charAt(norm16++);
                if((firstUnit&MAPPING_LENGTH_MASK)==0) {
                    return false;
                }
                if((firstUnit&MAPPING_HAS_CCC_LCCC_WORD)!=0 && (extraData.charAt(norm16++)&0xff00)!=0) {
                    return false;  // non-zero leadCC
                }
                return isCompYesAndZeroCC(getNorm16(Character.codePointAt(extraData, norm16)));
            }
        }
    }
    private int findPreviousCompBoundary(CharSequence s, int start, int p) {
        throw new UnsupportedOperationException();  // TODO
    }
    private int findNextCompBoundary(CharSequence s, int p, int limit) {
        while(p<limit) {
            int c=Character.codePointAt(s, p);
            int norm16=normTrie.get(c);
            if(hasCompBoundaryBefore(c, norm16)) {
                break;
            }
            p+=Character.charCount(c);
        }
        return p;
    }

    private int findPreviousFCDBoundary(CharSequence s, int start, int p) {
        throw new UnsupportedOperationException();  // TODO
    }
    private int findNextFCDBoundary(CharSequence s, int p, int limit) {
        while(p<limit) {
            int c=Character.codePointAt(s, p);
            int fcd16=fcdTrie.get(c);
            if(fcd16<=0xff) {
                break;
            }
            p+=Character.charCount(c);
        }
        return p;
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
