package com.ibm.text.UCA;

import com.ibm.text.utility.Utility;

/**
 * For generation of Implicit CEs
 * @author Davis
 *
 * Cleaned up so that changes can be made more easily.
 * Old values:
# First Implicit: E26A792D
# Last Implicit: E3DC70C0
# First CJK: E0030300
# Last CJK: E0A9DD00
# First CJK_A: E0A9DF00
# Last CJK_A: E0DE3100

 */
public class Implicit {
    
    /**
     * constants
     */
    static final boolean DEBUG = false;
    
    static final long topByte = 0xFF000000L;
    static final long bottomByte = 0xFFL;
    static final long fourBytes = 0xFFFFFFFFL;
    
    static final int MAX_INPUT = 0x21FFFF;
    
    /**
     * Testing function
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.println("Start");
        try {
            Implicit foo = new Implicit(0xE0, 0xE4);
            int gap4 = foo.getGap4();
            int gap3 = foo.getGap3();
            int minTrail = foo.getMinTrail();
            int maxTrail = foo.getMaxTrail();
            long last = 0;
            long current;
            for (int i = 0; i <= MAX_INPUT; ++i) {
                current = foo.getImplicit(i) & fourBytes;
                long lastBottom = last & bottomByte;
                long currentBottom = current & bottomByte;
                long lastTop = last & topByte;
                long currentTop = current & topByte;
                
                // do some consistency checks
                long gap = current - last;               
                if (currentBottom != 0) { // if we are a 4-byte
                    // gap has to be at least gap4
                    // and gap from minTrail, maxTrail has to be at least gap4
                    if (gap <= gap4) foo.throwError("Failed gap4 between", i);
                    if (currentBottom < minTrail + gap4) foo.throwError("Failed gap4 before", i);
                    if (currentBottom > maxTrail - gap4) foo.throwError("Failed gap4 after", i);
                } else { // we are a three-byte
                    gap = gap >> 8; // move gap down for comparison.
                    long current3Bottom = (current >> 8) & bottomByte;
                    if (gap <= gap3) foo.throwError("Failed gap3 between ", i);
                    if (current3Bottom < minTrail + gap3) foo.throwError("Failed gap3 before", i);
                    if (current3Bottom > maxTrail - gap3) foo.throwError("Failed gap3 after", i);
                }
                // print out some values for spot-checking
                if (lastTop != currentTop || i == 0x10000 || i == 0x110000) {
                    foo.show(i-3);
                    foo.show(i-2);
                    foo.show(i-1);
                    if (i == 0) {
                        // do nothing
                    } else if (lastBottom == 0 && currentBottom != 0) {
                        System.out.println("+ primary boundary, 4-byte CE's below");
                    } else if (lastTop != currentTop) {
                        System.out.println("+ primary boundary");
                    }
                    foo.show(i);
                    foo.show(i+1);
                    foo.show(i+2);
                    System.out.println("...");
                }
                last = current;
            }
            foo.show(MAX_INPUT-2);
            foo.show(MAX_INPUT-1);
            foo.show(MAX_INPUT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("End");
        }
    }
    
    private void throwError(String title, int i) {
        throw new IllegalArgumentException(title + "\t" + Utility.hex(i) + "\t" + Utility.hex(getImplicit(i) & fourBytes));
    }

    private void show(int i) {
        if (i >= 0 && i <= MAX_INPUT) {
            System.out.println(Utility.hex(i) + "\t" + Utility.hex(getImplicit(i) & fourBytes));
        } 
    }
    
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
    int min4Boundary;
    
    public int getGap4() {
        return final4Multiplier - 1;
    }
    
    public int getGap3() {
        return final3Multiplier - 1;
    }
    
    // old comment
    // we must skip all 00, 01, 02 bytes, so most bytes have 253 values
    // we must leave a gap of 01 between all values of the last byte, so the last byte has 126 values (3 byte case)
    // we shift so that HAN all has the same first primary, for compression.
    // for the 4 byte case, we make the gap as large as we can fit.
    // Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
    // Four byte forms (most supplementaries) are EF xx xx xx (with a gap of LAST2_MULTIPLIER == 14)

    /**
     * Supply parameters for generating implicit CEs
     */
    public Implicit(int minPrimary, int maxPrimary) {
        // 13 is the largest 4-byte gap we can use without getting 2 four-byte forms.
        this(minPrimary, maxPrimary, 0x04, 0xFE, 1, 15);
    }
    
    /**
     * Set up to generate implicits.
     * @param minPrimary
     * @param maxPrimary
     * @param minTrail final byte
     * @param maxTrail final byte
     * @param gap3 the gap we leave for tailoring for 3-byte forms
     * @param gap4 the gap we leave for tailoring for 4-byte forms
     */
    public Implicit(int minPrimary, int maxPrimary, int minTrail, int maxTrail, int gap3, int gap4) {
        // some simple parameter checks
        if (minPrimary < 0 || minPrimary >= maxPrimary || maxPrimary > 0xFF) throw new IllegalArgumentException("bad lead bytes");
        if (minTrail < 0 || minTrail >= maxTrail || maxTrail > 0xFF) throw new IllegalArgumentException("bad trail bytes");
        if (gap3 < 1 || gap4 < 1) throw new IllegalArgumentException("must have larger gaps");
        
        this.minTrail = minTrail;
        this.maxTrail = maxTrail;
        
        final3Multiplier = gap3 + 1;
        final4Multiplier = gap4 + 1;
        min3Primary = minPrimary;
        max4Primary = maxPrimary;
        // compute constants for use later.
        // number of values we can use in trailing bytes
        // leave room for empty values below, between, AND above, so
        // gap = 2:
        // range 3..7 => (3,4) 5 (6,7): so 1 value
        // range 3..8 => (3,4) 5 (6,7,8): so 1 value
        // range 3..9 => (3,4) 5 (6,7,8,9): so 1 value
        // range 3..10 => (3,4) 5 (6,7) 8 (9, 10): so 2 values
        final3Count = 1 + (maxTrail - minTrail - 1) / final3Multiplier;
        final4Count = 1 + (maxTrail - minTrail - 1) / final4Multiplier;
        // medials can use full range
        medialCount = (maxTrail - minTrail + 1);
        // find out how many values fit in each form
        int fourByteCount = medialCount * medialCount * final4Count;
        int threeByteCount = medialCount * final3Count;
        // now determine where the 3/4 boundary is.
        // we use 3 bytes below the boundary, and 4 above
        int primariesAvailable = maxPrimary - minPrimary + 1;
        int min4BytesNeeded = divideAndRoundUp(MAX_INPUT, fourByteCount);
        int min3BytesNeeded = primariesAvailable - min4BytesNeeded;
        if (min3BytesNeeded < 1) throw new IllegalArgumentException("Too few 3-byte implicits available.");
        int min3ByteCoverage = min3BytesNeeded * threeByteCount;
        min4Primary = minPrimary + min3BytesNeeded;
        min4Boundary = min3ByteCoverage;
        // Now expand out the multiplier for the 4 bytes, and redo.
        int totalNeeded = MAX_INPUT - min4Boundary;
        int neededPerPrimaryByte = divideAndRoundUp(totalNeeded, min4BytesNeeded);
        if (DEBUG) System.out.println("neededPerPrimaryByte: " + neededPerPrimaryByte);
        int neededPerFinalByte = divideAndRoundUp(neededPerPrimaryByte, medialCount * medialCount);
        if (DEBUG) System.out.println("neededPerFinalByte: " + neededPerFinalByte);
        int expandedGap = (maxTrail - minTrail - 1) / (neededPerFinalByte + 1) - 1;
        if (DEBUG) System.out.println("expandedGap: " + expandedGap);
        if (expandedGap < gap4) throw new IllegalArgumentException("must have larger gaps");
        final4Multiplier = expandedGap + 1;
        final4Count = neededPerFinalByte;
        if (DEBUG) {
            System.out.println("final4Count: " + final4Count);
            for (int counter = 0; counter <= final4Count; ++counter) {
                int value = minTrail + (1 + counter)*final4Multiplier;
                System.out.println(counter + "\t" + value + "\t" + Utility.hex(value));
            }
        }
    }
    
    static public int divideAndRoundUp(int a, int b) {
        return 1 + (a-1)/b;
    }
    
    /**
     * Generate the implicit CE, left shifted to put the first byte at the top of an int.
     * @param cp code point
     * @return
     */
    public int getImplicit(int cp) {
        if (cp < 0 || cp > MAX_INPUT) {
            throw new IllegalArgumentException("Code point out of range " + Utility.hex(cp));
        }
        int last0 = cp - min4Boundary;
        if (last0 < 0) {
            int last1 = cp / final3Count;
            last0 = cp % final3Count;
                        
            int last2 = last1 / medialCount;
            last1 %= medialCount;
            
            last0 = minTrail + (last0 + 1)*final3Multiplier - 1; // spread out, leaving gap at start
            last1 = minTrail + last1; // offset
            last2 = min3Primary + last2; // offset
            
            if (last2 >= min4Primary) {
                throw new IllegalArgumentException("4-byte out of range: " + Utility.hex(cp) + ", " + Utility.hex(last2));
            } 
            
            return (last2 << 24) + (last1 << 16) + (last0 << 8);
        } else {
            int last1 = last0 / final4Count;
            last0 %= final4Count;
            
            int last2 = last1 / medialCount;
            last1 %= medialCount;
            
            int last3 = last2 / medialCount;
            last2 %= medialCount;
            
            last0 = minTrail + (last0 + 1)*final4Multiplier - 1; // spread out, leaving gap at start           
            last1 = minTrail + last1; // offset
            last2 = minTrail + last2; // offset
            last3 = min4Primary + last3; // offset
            
            if (last3 > max4Primary) {
                throw new IllegalArgumentException("4-byte out of range: " + Utility.hex(cp) + ", " + Utility.hex(last3));
            } 
            
            return (last3 << 24) + (last2 << 16) + (last1 << 8) + last0;
        }
    }
    /**
     * @return
     */
    public int getMinTrail() {
        return minTrail;
    }

    /**
     * @return
     */
    public int getMaxTrail() {
        return maxTrail;
    }

}