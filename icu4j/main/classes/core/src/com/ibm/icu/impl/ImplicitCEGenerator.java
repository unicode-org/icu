/**
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

/**
 * For generation of Implicit CEs
 * @author Mark Davis
 *
 * Cleaned up so that changes can be made more easily.
 * Old values:
# First Implicit: E26A792D
# Last Implicit: E3DC70C0
# First CJK: E0030300
# Last CJK: E0A9DD00
# First CJK_A: E0A9DF00
# Last CJK_A: E0DE3100
@internal
 */
public class ImplicitCEGenerator {
    
    /**
     * constants
     */
    static final boolean DEBUG = false;
    
    static final long topByte = 0xFF000000L;
    static final long bottomByte = 0xFFL;
    static final long fourBytes = 0xFFFFFFFFL;
    
    static final int MAX_INPUT = 0x220001; // 2 * Unicode range + 2

    public static final int CJK_BASE = 0x4E00;
    public static final int CJK_LIMIT = 0x9FFF+1;
    public static final int CJK_COMPAT_USED_BASE = 0xFA0E;
    public static final int CJK_COMPAT_USED_LIMIT = 0xFA2F+1;
    public static final int CJK_A_BASE = 0x3400;
    public static final int CJK_A_LIMIT = 0x4DBF+1;
    public static final int CJK_B_BASE = 0x20000;
    public static final int CJK_B_LIMIT = 0x2A6DF+1;
    
//    private void throwError(String title, int cp) {
//        throw new IllegalArgumentException(title + "\t" + Utility.hex(cp, 6) + "\t" + 
//                                           Utility.hex(getImplicitFromRaw(cp) & fourBytes));
//    }
//
//    private void throwError(String title, long ce) {
//        throw new IllegalArgumentException(title + "\t" + Utility.hex(ce & fourBytes));
//    }
//
//    private void show(int i) {
//        if (i >= 0 && i <= MAX_INPUT) {
//            System.out.println(Utility.hex(i) + "\t" + Utility.hex(getImplicitFromRaw(i) & fourBytes));
//        } 
//    }
    
    /**
     * Precomputed by constructor
     */
    int final3Multiplier;
    int final4Multiplier;
    int final3Count;
    int final4Count;
    int medialCount;
    int min3Primary;
    int min4Primary;
    int max4Primary;
    int minTrail;
    int maxTrail;
    int max3Trail;
    int max4Trail;
    int min4Boundary;
    
    public int getGap4() {
        return final4Multiplier - 1;
    }
    
    public int getGap3() {
        return final3Multiplier - 1;
    }
    
    // old comment
    // we must skip all 00, 01, 02, FF bytes, so most bytes have 252 values
    // we must leave a gap of 01 between all values of the last byte, so the last byte has 126 values (3 byte case)
    // we shift so that HAN all has the same first primary, for compression.
    // for the 4 byte case, we make the gap as large as we can fit.

    /**
     * Supply parameters for generating implicit CEs
     */
    public ImplicitCEGenerator(int minPrimary, int maxPrimary) {
        // 13 is the largest 4-byte gap we can use without getting 2 four-byte forms.
        this(minPrimary, maxPrimary, 0x04, 0xFE, 1, 1);
    }
    
    /**
     * Set up to generate implicits.
     * @param minPrimary The minimum primary value.
     * @param maxPrimary The maximum primary value.
     * @param minTrail final byte
     * @param maxTrail final byte
     * @param gap3 the gap we leave for tailoring for 3-byte forms
     * @param primaries3count number of 3-byte primarys we can use (normally 1)
     */
    public ImplicitCEGenerator(int minPrimary, int maxPrimary, int minTrail, int maxTrail, int gap3, int primaries3count) {
        // some simple parameter checks
        if (minPrimary < 0 || minPrimary >= maxPrimary || maxPrimary > 0xFF) {
            throw new IllegalArgumentException("bad lead bytes");
        }
        if (minTrail < 0 || minTrail >= maxTrail || maxTrail > 0xFF) {
            throw new IllegalArgumentException("bad trail bytes");
        }
        if (primaries3count < 1) {
            throw new IllegalArgumentException("bad three-byte primaries");
        }
        
        this.minTrail = minTrail;
        this.maxTrail = maxTrail;
        
        min3Primary = minPrimary;
        max4Primary = maxPrimary;
        // compute constants for use later.
        // number of values we can use in trailing bytes
        // leave room for empty values between AND above, e.g. if gap = 2
        // range 3..7 => +3 -4 -5 -6 -7: so 1 value
        // range 3..8 => +3 -4 -5 +6 -7 -8: so 2 values
        // range 3..9 => +3 -4 -5 +6 -7 -8 -9: so 2 values
        final3Multiplier = gap3 + 1;
        final3Count = (maxTrail - minTrail + 1) / final3Multiplier;
        max3Trail = minTrail + (final3Count - 1) * final3Multiplier;
        
        // medials can use full range
        medialCount = (maxTrail - minTrail + 1);
        // find out how many values fit in each form
        int threeByteCount = medialCount * final3Count;
        // now determine where the 3/4 boundary is.
        // we use 3 bytes below the boundary, and 4 above
        int primariesAvailable = maxPrimary - minPrimary + 1;
        int primaries4count = primariesAvailable - primaries3count;        
        
        int min3ByteCoverage = primaries3count * threeByteCount;
        min4Primary = minPrimary + primaries3count;
        min4Boundary = min3ByteCoverage;
        // Now expand out the multiplier for the 4 bytes, and redo.
 
        int totalNeeded = MAX_INPUT - min4Boundary;
        int neededPerPrimaryByte = divideAndRoundUp(totalNeeded, primaries4count);
        if (DEBUG) System.out.println("neededPerPrimaryByte: " + neededPerPrimaryByte);
        
        int neededPerFinalByte = divideAndRoundUp(neededPerPrimaryByte, medialCount * medialCount);
        if (DEBUG) System.out.println("neededPerFinalByte: " + neededPerFinalByte);
        
        int gap4 = (maxTrail - minTrail - 1) / neededPerFinalByte;
        if (DEBUG) System.out.println("expandedGap: " + gap4);
        if (gap4 < 1) throw new IllegalArgumentException("must have larger gap4s");
        
        final4Multiplier = gap4 + 1;
        final4Count = neededPerFinalByte;
        max4Trail = minTrail + (final4Count - 1) * final4Multiplier;
        
        if (primaries4count * medialCount * medialCount * final4Count < MAX_INPUT) {
            throw new IllegalArgumentException("internal error");
        } 
        if (DEBUG) {
            System.out.println("final4Count: " + final4Count);
            for (int counter = 0; counter < final4Count; ++counter) {
                int value = minTrail + (1 + counter)*final4Multiplier;
                System.out.println(counter + "\t" + value + "\t" + Utility.hex(value));
            }
        }
    }
    
    static public int divideAndRoundUp(int a, int b) {
        return 1 + (a-1)/b;
    }

    /**
     * Converts implicit CE into raw integer
     * @param implicit The implicit value passed.
     * @return -1 if illegal format
     */
    public int getRawFromImplicit(int implicit) {
        int result;
        int b3 = implicit & 0xFF;
        implicit >>= 8;
        int b2 = implicit & 0xFF;
        implicit >>= 8;
        int b1 = implicit & 0xFF;
        implicit >>= 8;
        int b0 = implicit & 0xFF;

        // simple parameter checks
        if (b0 < min3Primary || b0 > max4Primary
            || b1 < minTrail || b1 > maxTrail) return -1;
        // normal offsets
        b1 -= minTrail;

        // take care of the final values, and compose
        if (b0 < min4Primary) {
            if (b2 < minTrail || b2 > max3Trail || b3 != 0) return -1;
            b2 -= minTrail;
            int remainder = b2 % final3Multiplier;
            if (remainder != 0) return -1;
            b0 -= min3Primary;
            b2 /= final3Multiplier;
            result = ((b0 * medialCount) + b1) * final3Count + b2;
        } else {
            if (b2 < minTrail || b2 > maxTrail
                || b3 < minTrail || b3 > max4Trail) return -1;
            b2 -= minTrail;
            b3 -= minTrail;
            int remainder = b3 % final4Multiplier;
            if (remainder != 0) return -1;
            b3 /= final4Multiplier;
            b0 -= min4Primary;
            result = (((b0 * medialCount) + b1) * medialCount + b2) * final4Count + b3 + min4Boundary;
        }
        // final check
        if (result < 0 || result > MAX_INPUT) return -1;
        return result;
    }
    
    /**
     * Generate the implicit CE, from raw integer.
     * Left shifted to put the first byte at the top of an int.
     * @param cp code point
     * @return Primary implicit weight
     */
    public int getImplicitFromRaw(int cp) {
        if (cp < 0 || cp > MAX_INPUT) {
            throw new IllegalArgumentException("Code point out of range " + Utility.hex(cp));
        }
        int last0 = cp - min4Boundary;
        if (last0 < 0) {
            int last1 = cp / final3Count;
            last0 = cp % final3Count;
                        
            int last2 = last1 / medialCount;
            last1 %= medialCount;
            
            last0 = minTrail + last0*final3Multiplier; // spread out, leaving gap at start
            last1 = minTrail + last1; // offset
            last2 = min3Primary + last2; // offset
            
            if (last2 >= min4Primary) {
                throw new IllegalArgumentException("4-byte out of range: " + 
                                                   Utility.hex(cp) + ", " + Utility.hex(last2));
            } 
            
            return (last2 << 24) + (last1 << 16) + (last0 << 8);
        } else {
            int last1 = last0 / final4Count;
            last0 %= final4Count;
            
            int last2 = last1 / medialCount;
            last1 %= medialCount;
            
            int last3 = last2 / medialCount;
            last2 %= medialCount;
            
            last0 = minTrail + last0*final4Multiplier; // spread out, leaving gap at start           
            last1 = minTrail + last1; // offset
            last2 = minTrail + last2; // offset
            last3 = min4Primary + last3; // offset
            
            if (last3 > max4Primary) {
                throw new IllegalArgumentException("4-byte out of range: " + 
                                                   Utility.hex(cp) + ", " + Utility.hex(last3));
            } 
            
            return (last3 << 24) + (last2 << 16) + (last1 << 8) + last0;
        }
    }

    /**
     * Gets an Implicit from a code point. Internally, 
     * swaps (which produces a raw value 0..220000, 
     * then converts raw to implicit.
     * @param cp The code point to convert to implicit.
     * @return Primary implicit weight
     */
    public int getImplicitFromCodePoint(int cp) {
        if (DEBUG) System.out.println("Incoming: " + Utility.hex(cp));
        
        // Produce Raw value
        // note, we add 1 so that the first value is always empty!!
        cp = ImplicitCEGenerator.swapCJK(cp) + 1;
        // we now have a range of numbers from 0 to 220000.
            
        if (DEBUG) System.out.println("CJK swapped: " + Utility.hex(cp));
            
        return getImplicitFromRaw(cp);
    }

    /**
     * Function used to: 
     * a) collapse the 2 different Han ranges from UCA into one (in the right order), and
     * b) bump any non-CJK characters by 10FFFF.
     * The relevant blocks are:
     * A:    4E00..9FFF; CJK Unified Ideographs
     *       F900..FAFF; CJK Compatibility Ideographs
     * B:    3400..4DBF; CJK Unified Ideographs Extension A
     *       20000..XX;  CJK Unified Ideographs Extension B (and others later on)
     * As long as
     *   no new B characters are allocated between 4E00 and FAFF, and
     *   no new A characters are outside of this range,
     * (very high probability) this simple code will work.
     * The reordered blocks are:
     * Block1 is CJK
     * Block2 is CJK_COMPAT_USED
     * Block3 is CJK_A
     * (all contiguous)
     * Any other CJK gets its normal code point
     * Any non-CJK gets +10FFFF
     * When we reorder Block1, we make sure that it is at the very start,
     * so that it will use a 3-byte form.
     * Warning: the we only pick up the compatibility characters that are
     * NOT decomposed, so that block is smaller!
     */
    
    static int NON_CJK_OFFSET = 0x110000;
        
    static int swapCJK(int i) {
        
        if (i >= CJK_BASE) {
            if (i < CJK_LIMIT)              return i - CJK_BASE;
            
            if (i < CJK_COMPAT_USED_BASE)   return i + NON_CJK_OFFSET;
            
            if (i < CJK_COMPAT_USED_LIMIT)  return i - CJK_COMPAT_USED_BASE
                                                + (CJK_LIMIT - CJK_BASE);
            if (i < CJK_B_BASE)             return i + NON_CJK_OFFSET;
            
            if (i < CJK_B_LIMIT)            return i; // non-BMP-CJK
            
            return i + NON_CJK_OFFSET;  // non-CJK
        }
        if (i < CJK_A_BASE)                 return i + NON_CJK_OFFSET;
        
        if (i < CJK_A_LIMIT)                return i - CJK_A_BASE
                                                + (CJK_LIMIT - CJK_BASE) 
                                                + (CJK_COMPAT_USED_LIMIT - CJK_COMPAT_USED_BASE);
        return i + NON_CJK_OFFSET; // non-CJK
    }
    

    /**
     * @return Minimal trail value
     */
    public int getMinTrail() {
        return minTrail;
    }

    /**
     * @return Maximal trail value
     */
    public int getMaxTrail() {
        return maxTrail;
    }
    
    public int getCodePointFromRaw(int i) {
        i--;
        int result = 0;
        if(i >= NON_CJK_OFFSET) {
            result = i - NON_CJK_OFFSET;
        } else if(i >= CJK_B_BASE) {
            result = i;
        } else if(i < CJK_A_LIMIT + (CJK_LIMIT - CJK_BASE) + (CJK_COMPAT_USED_LIMIT - CJK_COMPAT_USED_BASE)) { 
            // rest of CJKs, compacted
            if(i < CJK_LIMIT - CJK_BASE) {
                result = i + CJK_BASE;
            } else if(i < (CJK_LIMIT - CJK_BASE) + (CJK_COMPAT_USED_LIMIT - CJK_COMPAT_USED_BASE)) {
                result = i + CJK_COMPAT_USED_BASE - (CJK_LIMIT - CJK_BASE);
            } else {
                result = i + CJK_A_BASE - (CJK_LIMIT - CJK_BASE) - (CJK_COMPAT_USED_LIMIT - CJK_COMPAT_USED_BASE);
            }
        } else {
            result = -1;
        }
        return result;
    }

    public int getRawFromCodePoint(int i) {
        return swapCJK(i)+1;
    }
}
