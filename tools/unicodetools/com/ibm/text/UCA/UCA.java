/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/UCA.java,v $ 
* $Date: 2002/07/03 02:15:47 $ 
* $Revision: 1.17 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;

import java.util.*;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.text.MessageFormat;
import java.io.IOException;
import com.ibm.text.UCD.Normalizer;
import com.ibm.text.UCD.UCD;
import com.ibm.text.utility.*;
import com.ibm.text.UCD.UnifiedBinaryProperty;
import com.ibm.text.UCD.UnicodeProperty;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

//import com.ibm.text.CollationData.*;

/**
* Collator is a working version of UTR#10 Unicode Collation Algorithm,
* as described on http://www.unicode.org/unicode/reports/tr10/
* @author Mark Davis

It is not optimized, although it does use some techniques that are required for
a real optimization, such as squeezing all the weights into 32 bits.<p>

Invariants relied upon by the algorithm:

UCA Data:
1. While it contains secondaries greater than 0xFF, 
these can be folded down by subtracting 0xC0--without collision--to be less than 0xFF
2. Tertiary values are less than 0x80
3. Contracting characters must be "completed": if "abcd" is a contracting character, 
then "abc" is also.
4. Variables (marked with *), have a distinct, closed range of primaries. 
That is, there are no variable CEs X, Z and non-ignorable CE Y such that X[1] <= Y[1] <= Z[1]
5. It needs to be fixed when reading: only non-zero weights (levels 1-3) are really variable!

#4 saves a bit in each CE.

Limits
1. There is a limit on the number of expanding characters. If N is the number of expanding
characters, then their total lengths must be less than 65536-N. This should never pose a
problem in practice.
2. If any of the weight limits are reached (FFFF for primary, FF for secondary, tertiary),
expanding characters can be used to achieve the right results, as discussed in UTR#10.

Remarks:
Neither the old 14651 nor the old UCA algorithms for backwards really worked.
This is because of shared
characters between scripts with different directions, like French with Arabic or Greek.
*/

final public class UCA implements Comparator, UCA_Types {
    public static final String copyright = 
      "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";
      
    public int compare(Object a, Object b) {
        return getSortKey((String) a).compareTo(getSortKey((String) b));
    }

    /**
     * Version of the UCA tables to use
     */
    //private static final String VERSION = "-3.0.1d3"; // ""; // "-2.1.9d7"; 
    public static final String UCA_BASE = "3.1.1"; // ""; // "-2.1.9d7"; 
    public static final String VERSION = "-" + UCA_BASE + "d6"; // ""; // "-2.1.9d7"; 
    public static final String ALLFILES = "allkeys"; // null if not there
    
    /**
     * Records the codeversion
     */
    private static final String codeVersion = "7";

    // base directory will change depending on the installation
    public static final String BASE_DIR = "c:\\DATA\\";
    
    
// =============================================================
// Test Settings
// =============================================================
    static final boolean DEBUG = false;
    static final boolean DEBUG_SHOW_LINE = false;
    
    static final boolean SHOW_STATS = true;
    
    static final boolean SHOW_CE = false;
    static final boolean CHECK_UNIQUE = false;
    static final boolean CHECK_UNIQUE_EXPANSIONS = false; // only effective if CHECK_UNIQUE
    static final boolean CHECK_UNIQUE_VARIABLES = false; // only effective if CHECK_UNIQUE
    static final boolean TEST_BACKWARDS = false;
    static final boolean RECORDING_DATA = false;
    static final boolean RECORDING_CHARS = true;
    
    private UCD ucd;
    private UCA_Data ucaData;
    
// =============================================================
// Main Methods
// =============================================================

    /**
     * Initializes the collation from a stream of rules in the normal formal.
     * If the source is null, uses the normal Unicode data files, which
     * need to be in BASE_DIR.
     */
    public UCA(BufferedReader source, String unicodeVersion) throws java.io.IOException {
        fullData = source == null;
        
        // load the normalizer
        if (toD == null) {
            toD = new Normalizer(Normalizer.NFD, unicodeVersion);
        }
        
        ucd = UCD.make(unicodeVersion);
        ucdVersion = ucd.getVersion();
        
        ucaData = new UCA_Data(toD, ucd);
        
        // either get the full sources, or just a demo set
        if (fullData) {
            for (int i = 0; i < KEYS.length; ++i) {
                BufferedReader in = new BufferedReader(
                    new FileReader(KEYS[i]), BUFFER_SIZE);
                addCollationElements(in);
                in.close();
            }
        } else {
            addCollationElements(source);
        }
        cleanup();
    }
    
    /**
     * Constructs a sort key for a string of input Unicode characters. Uses
     * default values for alternate and decomposition.
     * @param sourceString string to make a sort key for.
     * @return Result is a String not of really of Unicodes, but of weights.
     * String is just a handy way of returning them in Java, since there are no
     * unsigned shorts.
     */
    public String getSortKey(String sourceString) {
        return getSortKey(sourceString, defaultAlternate, defaultDecomposition);
    }
    /**
     * Constructs a sort key for a string of input Unicode characters. Uses
     * default value decomposition.
     * @param sourceString string to make a sort key for.
     * @param alternate choice of different 4th level weight construction
     * @return Result is a String not of really of Unicodes, but of weights.
     * String is just a handy way of returning them in Java, since there are no
     * unsigned shorts.
     */

    public String getSortKey(String sourceString, byte alternate) {
        return getSortKey(sourceString, alternate, defaultDecomposition);
    }
    
    /**
     * Constructs a sort key for a string of input Unicode characters.
     * @param sourceString string to make a sort key for.
     * @param alternate choice of different 4th level weight construction
     * @param decomposition true for UCA, false where the text is guaranteed to be
     * normalization form C with no combining marks of class 0.
     * @return Result is a String not of really of Unicodes, but of weights.
     * String is just a handy way of returning them in Java, since there are no
     * unsigned shorts.
     */
    public String getSortKey(String sourceString, byte alternate, boolean decomposition) {
        decompositionBuffer.setLength(0);
        if (decomposition) {
            toD.normalize(sourceString, decompositionBuffer);
        } else {
            decompositionBuffer.append(sourceString);
        }
        storedDecomposition = decomposition;    // record the setting for other methods
        index = 0;                              // position in source string

        // Weight strings - not chars, weights.
        primaries.setLength(0);             // clear out
        secondaries.setLength(0);           // clear out
        tertiaries.setLength(0);            // clear out
        quaternaries.setLength(0);          // clear out
        if (SHOW_CE) debugList.setLength(0); // clear out
        
        rearrangeBuffer = EMPTY;            // clear the rearrange buffer (thai)
        hangulBufferPosition = 0;           // clear hangul buffer
        hangulBuffer.setLength(0);           // clear hangul buffer
        
        char weight4 = '\u0000'; // DEFAULT FOR NON_IGNORABLE
        boolean lastWasVariable = false;

        // process CEs, building weight strings
        while (true) {
            //fixQuaternatiesPosition = quaternaries.length();
            int ce = getCE();
            if (ce == TERMINATOR) break;
            if (ce == 0) continue;
            
            switch (alternate) {
              case ZEROED:
                if (isVariable(ce)) {
                    ce = 0;
                }
                break;
              case SHIFTED_TRIMMED:
              case SHIFTED:
                if (ce == 0) {
                    weight4 = 0;
                } else if (isVariable(ce)) { // variables
                    weight4 = getPrimary(ce);
                    lastWasVariable = true;
                    ce = 0;
                } else if (lastWasVariable && getPrimary(ce) == 0) { // zap trailing ignorables
                    ce = 0;
                    weight4 = 0;
                } else { // above variables
                    lastWasVariable = false;
                    weight4 = '\uFFFF';
                }
                break;
              // case NON_IGNORABLE: // doesn't ever change!
            }
            if (SHOW_CE) {
                if (debugList.length() != 0) debugList.append("/");
                debugList.append(CEList.toString(ce));
            }
            
            // add weights
            char w = getPrimary(ce);
            if (DEBUG) System.out.println("\tCE: " + Utility.hex(ce));
            if (w != 0) {
                primaries.append(w);
            }
            
            w = getSecondary(ce);
            if (w != 0) {
                if (!useBackwards) {
                    secondaries.append(w);
                } else {
                    secondaries.insert(0, w);
                }
            }
            
            w = getTertiary(ce);
            if (w != 0) {
                tertiaries.append(w);
            }
   
            if (weight4 != 0) {
                quaternaries.append(weight4);
            }
        }
        
        // Produce weight strings
        // For simplicity, we use the strength setting here.
        // To optimize, we wouldn't actually generate the weights in the first place.
        
        StringBuffer result = primaries;
        if (strength >= 2) {
            result.append(LEVEL_SEPARATOR);    // separator
            result.append(secondaries);
            if (strength >= 3) {
                result.append(LEVEL_SEPARATOR);    // separator
                result.append(tertiaries);
                if (strength >= 4) {
                    result.append(LEVEL_SEPARATOR);    // separator
                    if (alternate == SHIFTED_TRIMMED) {
                        int q;
                        for (q = quaternaries.length()-1; q >= 0; --q) {
                            if (quaternaries.charAt(q) != '\uFFFF') {
                                break;
                            }
                        }
                        quaternaries.setLength(q+1);
                    }
                    result.append(quaternaries);
                    //appendInCodePointOrder(decompositionBuffer, result);
                }
            }
        }
        return result.toString();
    }
    
    // 0 ==
    // 2, -2 quarternary
    // 3, -3 tertiary
    // 4, -4 secondary
    // 5, -5 primary
    
    public static int strengthDifference(String sortKey1, String sortKey2) {
        int len1 = sortKey1.length();
        int len2 = sortKey2.length();
        int minLen = len1 < len2 ? len1 : len2;
        int strength = 5;
        for (int i = 0; i < minLen; ++i) {
            char c1 = sortKey1.charAt(i);
            char c2 = sortKey2.charAt(i);
            if (c1 < c2) return -strength;
            if (c1 > c2) return strength;
            if (c1 == LEVEL_SEPARATOR) --strength; // Separator!
        }
        if (len1 < len2) return -strength;
        if (len1 > len2) return strength;
        return 0;
    }
    
    /**
     * Turns backwards (e.g. for French) on globally for all secondaries
     */
    public void setBackwards(boolean backwards) {
        useBackwards = backwards;
    }

    /**
     * Retrieves value applied by set.
     */
    public boolean isBackwards() {
        return useBackwards;
    }

    /**
     * Causes variables (those with *) to be set to all zero weights (level 1-3).
     */
    public void setDecompositionState(boolean state) {
        defaultDecomposition = state;
    }

    /**
     * Retrieves value applied by set.
     */
    public boolean isDecomposed() {
        return defaultDecomposition;
    }

    /**
     * Causes variables (those with *) to be set to all zero weights (level 1-3).
     */
    public void setAlternate(byte status) {
        defaultAlternate = status;
    }

    /**
     * Retrieves value applied by set.
     */
    public byte getAlternate() {
        return defaultAlternate;
    }

    /**
     * Sets the maximum strength level to be included in the string. 
     * E.g. with 3, only weights of 1, 2, and 3 are included: level 4 weights are discarded.
     */
    public void setStrength(int inStrength) {
        strength = inStrength;
    }

    /**
     * Retrieves value applied by set.
     */
    public int getStrength() {
        return strength;
    }
    
    /**
     * Retrieves version
     */
    public String getCodeVersion() {
        return codeVersion;
    }

    /**
     * Retrieves versions
     */
    public String getDataVersion() {
        return dataVersion;
    }
    
    /**
     * Retrieves versions
     */
    public String getUCDVersion() {
        return ucdVersion;
    }
    
    public static String codePointOrder(String s) {
        return appendInCodePointOrder(s, new StringBuffer()).toString();
    }

    /**
     * Appends UTF-16 string
     * with the values swapped around so that they compare in
     * code-point order. Replace 0000 and 0001 by 0001 0001/2
     * @param source Normal UTF-16 (Java) string
     * @return sort key (as string)
     * @author Markus Scherer (cast into Java by MD)
     * NOTE: changed to be longer, but handle isolated surrogates
     */
    public static StringBuffer appendInCodePointOrder(String source, StringBuffer target) {
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            target.append((char)((cp >> 15) | 0x8000));
            target.append((char)(cp | 0x8000));
            /*
            if (ch <= 1) { // hack to avoid nulls
                target.append('\u0001');
                target.append((char)(ch+1));
            }
            target.append((char)(ch + utf16CodePointOrder[ch>>11]));
            */
        }
        return target;
    }
    
    /**
     * Returns a list of CEs for a unicode character at a position.
     * @param sourceString string to make a sort key for.
     * @param offset position in string
     * @param decomposition true for UCA, false where the text is guaranteed to be
     * normalization form C with no combining marks of class 0.
     * @param output array for output. Must be large enough on entry. When done, is terminated with TERMINATOR.
     */
    public void getCEs(String sourceString, boolean decomposition, IntStack output) {
        decompositionBuffer.setLength(0);
        if (decomposition) {
            toD.normalize(sourceString, decompositionBuffer);
        } else {
            decompositionBuffer.append(sourceString);
        }
        rearrangeBuffer = EMPTY;            // clear the rearrange buffer (thai)
        index = 0;

        // process CEs, building weight strings
        while (true) {
            //fixQuaternatiesPosition = quaternaries.length();
            int ce = getCE();
            if (ce == 0) continue;
            if (ce == TERMINATOR) break;
            output.push(ce);
        }
    }
    
    
    /**
     * Returns a list of CEs for a unicode character at a position.
     * @param sourceString string to make a sort key for.
     * @param offset position in string
     * @param decomposition true for UCA, false where the text is guaranteed to be
     * normalization form C with no combining marks of class 0.
     * @param output array for output. Must be large enough on entry. When done, is terminated with TERMINATOR.
     * @return count of CEs
     */
    public int getCEs(String sourceString, boolean decomposition, int[] output) {
        decompositionBuffer.setLength(0);
        if (decomposition) {
            toD.normalize(sourceString, decompositionBuffer);
        } else {
            decompositionBuffer.append(sourceString);
        }
        rearrangeBuffer = EMPTY;            // clear the rearrange buffer (thai)
        index = 0;
        int outpos = 0;
        output[0] = 0; // just in case!!

        // process CEs, building weight strings
        while (true) {
            //fixQuaternatiesPosition = quaternaries.length();
            int ce = getCE();
            if (ce == 0) continue;
            if (ce == TERMINATOR) break;
            output[outpos++] = ce;
        }
        return outpos;
    }
    
    /**
     * Returns a CEList for a unicode character at a position.
     * @param sourceString string to make a sort key for.
     * @param offset position in string
     * @param decomposition true for UCA, false where the text is guaranteed to be
     * normalization form C with no combining marks of class 0.
     * @param output array for output. Must be large enough on entry. When done, is terminated with TERMINATOR.
     * @return count of CEs
     */
    
    public CEList getCEList(String sourceString, boolean decomposition) {
        int len;
        while (true) {
            try {
                len = getCEs(sourceString, decomposition, ceListBuffer);
                break;
            } catch (ArrayIndexOutOfBoundsException e) {
                ceListBuffer = new int[ceListBuffer.length * 2];
            }
        }
        return new CEList(ceListBuffer, 0, len);
    }
    
    int[] ceListBuffer = new int[30]; // temporary storage, to avoid multiple creation
    
    
    /**
     * Get Usage
     */
    public BitSet getWeightUsage(int strength) {
        return strength == 1 ? primarySet : strength == 2 ? secondarySet : tertiarySet;
    }
     
    /**
     * Returns the char associated with a FIXED value
     */
    /*public char charFromFixed(int ce) {
        return getPrimary(ce);
    }
    */
    
    /**
     * Return the type of the CE
     */
    public byte getCEType(int ch) {
        return ucaData.getCEType(ch);
    }

    /**
     * Utility, used to get the primary weight from a 32-bit CE
     * The primary is 16 bits, stored in b31..b16
     */
    public static char getPrimary(int ce) {
        return (char)(ce >>> 16);
    }

    /**
     * Utility, used to get the secondary weight from a 32-bit CE
     * The secondary is 8 bits, stored in b15..b8
     */
    public static char getSecondary(int ce) {
        return (char)((ce >>> 7) & 0x1FF);
    }

    /**
     * Utility, used to get the tertiary weight from a 32-bit CE
     * The tertiary is 6 bits, stored in b6..b0
     */
    public static char getTertiary(int ce) {
        return (char)(ce & 0x7F);
    }

    /**
     * Utility, used to determine whether a CE is variable or not.
     */
     
    public boolean isVariable(int ce) {
        return (variableLowCE <= ce && ce <= variableHighCE);
    }
    
    /**
     * Utility, used to determine whether a CE is variable or not.
     */
     
    public int getVariableLow() {
        return variableLowCE;
    }
    
    /**
     * Utility, used to determine whether a CE is variable or not.
     */
     
    public int getVariableHigh() {
        return variableHighCE;
    }
    
    /**
     * Utility, used to make a CE from the pieces. They must already
     * be in the right range of values.
     */
    public static int makeKey(int primary, int secondary, int tertiary) {
        return (primary << 16) | (secondary << 7) | tertiary;
    }
    
// =============================================================
// Utility methods
// =============================================================

    /**
     * Produces a human-readable string for a sort key.
     * The 0000 separator is replaced by a '|'
     */
    static public String toString(String sortKey) {
        StringBuffer result = new StringBuffer();
        boolean needSep = false;
        result.append("[");
        for (int i = 0; i < sortKey.length(); ++i) {
            char ch = sortKey.charAt(i);
            if (needSep) result.append(" ");
            if (ch == 0) {
                result.append("|");
                needSep = true;
            } else {
                result.append(Utility.hex(ch));
                needSep = true;
            }
        }
        result.append("]");
        return result.toString();
    }
    
    /**
     * Produces a human-readable string for a collation element.
     * value is terminated by -1!
     */
     /*
    static public String ceToString(int[] ces, int len) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < len; ++i) {
            result.append(ceToString(ces[i]));
        }
        return result.toString();
    }
    &/
    
    /**
     * Produces a human-readable string for a collation element.
     * value is terminated by -1!
     */
     /*
    static public String ceToString(int[] ces) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; ; ++i) {
            if (ces[i] == TERMINATOR) break;
            result.append(ceToString(ces[i]));
        }
        return result.toString();
    }
    */
    
    static boolean isImplicitLeadCE(int ce) {
    	return isImplicitLeadPrimary(getPrimary(ce));
    }
    
    static boolean isImplicitLeadPrimary(int primary) {
    	return primary >= UNSUPPORTED_BASE && primary < UNSUPPORTED_LIMIT;
    }
    
/*
The formula from the UCA:

BASE:

FB40 CJK Ideograph 
FB80 CJK Ideograph Extension A/B 
FBC0 Any other code point 

AAAA = BASE + (CP >> 15);
BBBB = (CP & 0x7FFF) | 0x8000;The mapping given to CP is then given by:

CP => [.AAAA.0020.0002.][.BBBB.0000.0000.]
*/		
    
    /**
     * Returns implicit value
     */
    
    void CodepointToImplicit(int cp, int[] output) {
		int base = UNSUPPORTED_OTHER_BASE;
        if (ucd.isCJK_BASE(cp)) base = UNSUPPORTED_CJK_BASE;
        else if (ucd.isCJK_AB(cp)) base = UNSUPPORTED_CJK_AB_BASE;
        output[0] = base + (cp >>> 15);
        output[1] = (cp & 0x7FFF) | 0x8000;
    }
    
    /**
     * Takes implicit value
     */
    
    static int ImplicitToCodePoint(int leadImplicit, int trailImplicit) {
    	// could probably optimize all this, but it is not worth it.
    	if (leadImplicit < UNSUPPORTED_BASE || leadImplicit >= UNSUPPORTED_LIMIT) {
    		throw new IllegalArgumentException("Lead implicit out of bounds: " + Utility.hex(leadImplicit));
    	}
    	if ((trailImplicit & 0x8000) == 0) {
    		throw new IllegalArgumentException("Trail implicit out of bounds: " + Utility.hex(trailImplicit));
    	}
    	int base;
    	if (leadImplicit >= UNSUPPORTED_OTHER_BASE) base = UNSUPPORTED_OTHER_BASE;
    	else if (leadImplicit >= UNSUPPORTED_CJK_AB_BASE) base = UNSUPPORTED_CJK_AB_BASE;
    	else base = UNSUPPORTED_CJK_BASE;
    	
    	int result = ((leadImplicit - base) << 15) | (trailImplicit & 0x7FFF);
    	
    	if (result > 0x10FFFF) {
    		throw new IllegalArgumentException("Resulting character out of  bounds: "
    			+ Utility.hex(leadImplicit) + ", " + Utility.hex(trailImplicit) 
    			+ " => " + result);
    	}
    	return result;
    }
    
    /**
     * Supplies a zero-padded hex representation of an integer (without 0x)
     */
    /*
    static public String hex(int i) {
        String result = Long.toString(i & 0xFFFFFFFFL, 16).toUpperCase();
        return "00000000".substring(result.length(),8) + result;
    }
    */
    /**
     * Supplies a zero-padded hex representation of a Unicode character (without 0x, \\u)
     */
    /*
    static public String hex(char i) {
        String result = Integer.toString(i, 16).toUpperCase();
        return "0000".substring(result.length(),4) + result;
    }
    */
    /**
     * Supplies a zero-padded hex representation of a Unicode character (without 0x, \\u)
     */
     /*
    static public String hex(byte b) {
        int i = b & 0xFF;
        String result = Integer.toString(i, 16).toUpperCase();
        return "00".substring(result.length(),2) + result;
    }
    */
    /**
     * Supplies a zero-padded hex representation of a Unicode String (without 0x, \\u)
     *@param sep can be used to give a sequence, e.g. hex("ab", ",") gives "0061,0062"
     */
     /*
    static public String hex(String s, String sep) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(sep);
            result.append(hex(s.charAt(i)));
        }
        return result.toString();
    }
    */
    /**
     * Supplies a zero-padded hex representation of a Unicode String (without 0x, \\u)
     *@param sep can be used to give a sequence, e.g. hex("ab", ",") gives "0061,0062"
     */
     /*
    static public String hex(StringBuffer s, String sep) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(sep);
            result.append(hex(s.charAt(i)));
        }
        return result.toString();
    }
    */
    
// =============================================================
// Privates
// =============================================================
    
    
    IntStack expandingStack = new IntStack(10);
    
    /**
     * Array used to reorder surrogates to top of 16-bit range, and others down.
     * Adds 2000 to D800..DFFF, making them F800..FFFF
     * Subtracts 800 from E000..FFFF, making them D800..F7FF
     */
    private static final int[] utf16CodePointOrder = {
        0, 0, 0, 0,                        // 00, 08, 10, 18
        0, 0, 0, 0,                        // 20, 28, 30, 38
        0, 0, 0, 0,                        // 40, 48, 50, 58
        0, 0, 0, 0,                        // 60, 68, 70, 78
        0, 0, 0, 0,                        // 80, 88, 90, 98
        0, 0, 0, 0,                        // A0, A8, B0, B8
        0, 0, 0, 0x2000,                   // C0, C8, D0, D8
        -0x800, -0x800, -0x800, -0x800     // E0, E8, F0, F8
    };

    /**
     * NFD required
     */
    private static Normalizer toD;

    /**
     * Records the dataversion
     */
    private String dataVersion = "3.1d1";

    /**
     * Records the dataversion
     */
    private String ucdVersion = "?";

    /**
     * Turns backwards (e.g. for French) on globally for all secondaries
     */
    private boolean useBackwards = false;
    
    /**
     * Choice of how to handle variables (those with *)
     */
    private byte defaultAlternate = SHIFTED;
    
    /**
     * For testing
     */
    private boolean defaultDecomposition = true;
    
    /**
     * Sets the maximum strength level to be included in the string. 
     * E.g. with 3, only weights of 1, 2, and 3 are included: level 4 weights are discarded.
     */
    private int strength = 4;
    
    /**
     * Position in decompositionBuffer used when constructing sort key
     */
    private int index;

    /**
     * List of files to use for constructing the CE data, used by build()
     */
    private static final String[] KEYS = {
        //"D:\\UnicodeData\\testkeys.txt",
        BASE_DIR + "Collation\\allkeys" + VERSION + ".txt",
        /*
        BASE_DIR + "UnicodeData\\Collation\\basekeys" + VERSION + ".txt",
        BASE_DIR + "UnicodeData\\Collation\\compkeys" + VERSION + ".txt",
        BASE_DIR + "UnicodeData\\Collation\\ctrckeys" + VERSION + ".txt",
        */
    };
 
    /**
     * File buffer size, used to make reads faster.
     */
    private static final int BUFFER_SIZE = 64*1024;
    
// =============================================================
// Collation Element Memory Data Table Formats
// =============================================================

    /**
     * Temporary buffer used in getSortKey for the decomposed string
     */
    private StringBuffer decompositionBuffer = new StringBuffer();
    
    // was 0xFFC20101;
    
    /**
     * We take advantage of the variables being in a closed range to save a bit per CE.
     * The low and high values are initially set to be at the opposite ends of the range,
     * as the table is built from the UCA data, they are narrowed in.
     * The first three values are used in building; the last two in testing.
    */
    private int variableLow = '\uFFFF';
    private int nonVariableLow = '\uFFFF'; // HACK '\u089A';
    private int variableHigh = '\u0000';
    
    private int variableLowCE;  // used for testing against
    private int variableHighCE; // used for testing against
    
    /*
    
    private void fixSurrogateContraction(char ch) {
        //if (DEBUGCHAR) System.out.println(Utility.hex(ch) + ": " + line.substring(0, position[0]) + "|" + line.substring(position[0]));            
        if (ch == NOT_A_CHAR || !UTF16.isLeadSurrogate(ch)) return;
        String chs = String.valueOf(ch);
        Object probe = contractingTable.get(chs);
        if (probe != null) return;
        contractingTable.put(chs, new Integer(UNSUPPORTED));
    }
    
    */
    
    /**
     * Marks whether we are using the full data set, or an abbreviated version for
     * an applet.
     */
     
    private boolean fullData;
    
// =============================================================
// Temporaries used in getCE. 
// Made part of the object to avoid reallocating each time.
// =============================================================

    /**
     * Temporary buffers used in getSortKey to store weights
     * these are NOT strings of Unicode characters--they are
     * lists of weights. But this is a convenient way to store them,
     * since Java doesn't have unsigned shorts.
     */
    private StringBuffer primaries = new StringBuffer(100);
    private StringBuffer secondaries = new StringBuffer(100);
    private StringBuffer tertiaries = new StringBuffer(100);
    private StringBuffer quaternaries = new StringBuffer(100);
    
    /**
     * Temporary buffer used to collect progress data for debugging
     */
    StringBuffer debugList = new StringBuffer(100);
    
    /**
     * Temporary with requested decomposition
     */
    boolean storedDecomposition;
    
    /**
     * Used for supporting Thai rearrangement
     */
    static final char EMPTY = '\uFFFF';
    char rearrangeBuffer = EMPTY;
    UnicodeSet rearrangeList = new UnicodeSet();
    int hangulBufferPosition = 0;
    StringBuffer hangulBuffer = new StringBuffer();

// =============================================================
// getCE: Get the next Collation Element
// Main Routine
// =============================================================

    /**
     * Gets the next Collation Element from the decomposition buffer.
     * May take one or more characters.
     * Resets index to point at the next position to get characters from.
     *@param quaternary the collection of 4th level weights, synthesized from the
     * (normalized) character code.
     */
    private int getCE() {
        if (!expandingStack.isEmpty()) return expandingStack.popFront();
        char ch;
        
        // Fetch next character. Handle rearrangement for Thai, etc.
        if (rearrangeBuffer != EMPTY) {
            ch = rearrangeBuffer;
            rearrangeBuffer = EMPTY;
        } else if (hangulBufferPosition < hangulBuffer.length()) {
            ch = hangulBuffer.charAt(hangulBufferPosition++);
            if (hangulBufferPosition == hangulBuffer.length()) {
                hangulBuffer.setLength(0);
                hangulBufferPosition = 0;
            }
        } else {
            if (index >= decompositionBuffer.length()) return TERMINATOR;
            ch = decompositionBuffer.charAt(index++); // get next
            if (rearrangeList.contains(ch) && index < decompositionBuffer.length()) {// if in list
                rearrangeBuffer = ch;   // store for later
                ch = decompositionBuffer.charAt(index++);   // never rearrange twice!!
            }
        }
        
        index = ucaData.get(ch, decompositionBuffer, index, expandingStack);
        int ce = expandingStack.popFront(); // pop first (guaranteed to exist!)
        if (ce == UNSUPPORTED_FLAG) {
            return handleUnsupported(ch);
        }
        return ce;
    }
    
    private int handleUnsupported(char ch) {
        int bigChar = ch;
            
        // Special check for Hangul
        if (ucd.isHangulSyllable(bigChar)) {
            // MUST DECOMPOSE!!
            hangulBuffer = new StringBuffer();
            decomposeHangul(bigChar, hangulBuffer);
            return getCE();
            // RECURSIVE!!!
        }
        
        // special check and fix for unsupported surrogate pair, 20 1/8 bits
        if (0xD800 <= bigChar && bigChar <= 0xDFFF) {
            // ignore unmatched surrogates (e.g. return zero)
            if (bigChar >= 0xDC00 || index >= decompositionBuffer.length()) return 0; // unmatched
            int ch2 = decompositionBuffer.charAt(index);
            if (ch2 < 0xDC00 || 0xDFFF < ch2) return 0;  // unmatched
            index++; // skip next char
            bigChar = 0x10000 + ((ch - 0xD800) << 10) + (ch2 - 0xDC00); // extract value
        }

                        
        if (ucd.isNoncharacter(bigChar)) { // illegal code value, ignore!!
            return 0;
        }
            
		// find the implicit values; returned in 0 and 1
		int[] implicit = new int[2];
		CodepointToImplicit(bigChar, implicit);
			
        // Now compose the two keys
            
        // push BBBB
                        
        expandingStack.push(makeKey(implicit[1], 0, 0));
        
        // return AAAA
            
        return makeKey(implicit[0], NEUTRAL_SECONDARY, NEUTRAL_TERTIARY);
        

    }
    
    /**
     * Constants for Hangul
     */
    static final int // constants
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount,   // 11172
        LastInitial = LBase + LCount-1, // last initial jamo
        LastPrimary = SBase + (LCount-1) * VCount * TCount; // last corresponding primary
   
    public static StringBuffer decomposeHangul(int s, StringBuffer result) {
        int SIndex = s - SBase;
        if (0 > SIndex || SIndex >= SCount) {
            throw new IllegalArgumentException("Non-Hangul Syllable");
        }
        int L = LBase + SIndex / NCount;
        int V = VBase + (SIndex % NCount) / TCount;
        int T = TBase + SIndex % TCount;
        result.append((char)L);
        result.append((char)V);
        if (T != TBase) result.append((char)T);
        return result;
    }
   
    /**
     * Fix for Hangul, since the tables are not set up right.
     * The fix for Hangul is to give different values to the combining initial 
     * Jamo to put them up into the AC00 range, as follows. Each one is put
     * after the first syllable it begins.
     *
    private int fixJamo(char ch, int jamoCe) {
        
        int result = jamoCe - hangulHackBottom + 0xAC000000; // put into right range
        if (DEBUG) System.out.println("\tChanging " + hex(ch) + " " + hex(jamoCe) + " => " + hex(result));
        return result;
        /*
        int newPrimary;
        int LIndex = jamo - LBase;
        if (LIndex < LCount) {
            newPrimary = SBase + (LIndex + 1) * VCount * TCount; // multiply to match syllables
        } else {
            newPrimary = LastPrimary + (jamo - LastInitial); // just shift up
        }
        return makeKey(newPrimary, 0x21, 0x2);  // make secondary difference!
        * /
    }
    */
    
// =============================================================
// Building Collation Element Tables
// =============================================================

    /**
     * Value for returning int as well as function return,
     * since Java doesn't have output parameters
     */
    private int[] position = new int[1]; 
    
    /**
     * For recording statistics
     */
    private int count1 = 0, count2 = 0, count3 = 0, max2 = 0, max3 = 0;
    private int oldKey1 = -1, oldKey2 = -1, oldKey3 = -1;
    UnicodeSet found = new UnicodeSet();
    
    /*public Hashtable getContracting() {
        return new Hashtable(multiTable);
    }
    */
    
            
    public UCAContents getContents(byte ceLimit, Normalizer skipDecomps) {
        return new UCAContents(ceLimit, skipDecomps, ucdVersion);
    }
    
    static boolean haveUnspecified = false;
    static UnicodeSet unspecified = new UnicodeSet();
        
    public class UCAContents {
        int current = -1;
        Normalizer skipDecomps;
        Normalizer nfd;
        Normalizer nfkd;
        Iterator enum = null;
        byte ceLimit;
        int currentRange = SAMPLE_RANGES.length; // set to ZERO to enable
        int startOfRange = SAMPLE_RANGES[0][0];
        int endOfRange = startOfRange;
        int itemInRange = startOfRange;
        int skip = 1;
        boolean doSamples = false;
        UnicodeSetIterator usi = new UnicodeSetIterator();
        
        /**
         * use FIXED_CE as the limit
         */
        UCAContents(byte ceLimit, Normalizer skipDecomps, String unicodeVersion) {
            this.ceLimit = ceLimit;
            this.nfd = new Normalizer(Normalizer.NFD, unicodeVersion);
            this.nfkd = new Normalizer(Normalizer.NFKD, unicodeVersion);
            this.skipDecomps = skipDecomps;
            currentRange = 0;
            usi.reset(unspecified);
            usi.setAbbreviated(true);
            
            // FIX SAMPLES
            if (SAMPLE_RANGES[0][0] == 0) {
                for (int i = 0; ; ++i) { // add first unallocated character
                    if (!ucd.isAssigned(i)) {
                        SAMPLE_RANGES[0][0] = i;
                        break;
                    }
                }
            }
        }
        
        /**
         * use FIXED_CE as the limit
         */
        public void enableSamples() {
            doSamples = true;
        }
        
        /**
         * returns a string
         */
        public String next() {
            String result = null; // null if done
            
            // normal case
            while (current++ < 0x10FFFF) {
                if (DEBUG && current == 0xdbff) {
                    System.out.println("DEBUG");
                }
                //char ch = (char)current;
                byte type = getCEType(current);
                if (type >= ceLimit || type == CONTRACTING_CE) continue;
                
                //if (nfd.isNormalized(current) || type == HANGUL_CE) {
                //}
                
                if (skipDecomps != null && !skipDecomps.isNormalized(current)) continue; // CHECK THIS
                
                result = UTF16.valueOf(current);
                if (!haveUnspecified) unspecified.add(current);
                return result;
            }
            
            // contractions
            if (enum == null) enum = ucaData.getContractions();
            while (enum.hasNext()) {
                result = (String)enum.next();
                if (result.length() == 1 && UTF16.isLeadSurrogate(result.charAt(0))) {
                    //System.out.println("Skipping " + ucd.getCodeAndName(result));
                    continue; // try again
                }
                if (!haveUnspecified) {
                    if (UTF16.countCodePoint(result) == 1) {
                        unspecified.add(result);
                    }
                }
                return result;
            }
            
            if (!haveUnspecified) {
                if (DEBUG) System.out.println("Specified = " + unspecified.toPattern(true));
                UnicodeSet temp = new UnicodeSet();
                for (int i = 0; i < 0x10ffff; ++i) {
                    if (!ucd.isAllocated(i)) continue;
                    if (!unspecified.contains(i)) {
                        temp.add(i);
                    }
                    
                    // add the following so that if a CJK is in a decomposition, we add it
                    if (!nfkd.isNormalized(i)) {
                        String decomp = nfkd.normalize(i);
                        int cp2;
                        for (int j = 0; j < decomp.length(); j += UTF16.getCharCount(cp2)) {
                            cp2 = UTF16.charAt(decomp, j);
                            if (!unspecified.contains(cp2)) {
                                temp.add(cp2);
                            }
                        }
                    }
                }
                unspecified = temp;
                usi.reset(unspecified);
                usi.setAbbreviated(true);
                if (DEBUG) System.out.println("Unspecified = " + unspecified.toPattern(true));
                haveUnspecified = true;
             }
            
            if (!doSamples) return null;
            
            if (usi.next()) {
                if (usi.codepoint == usi.IS_STRING) result = usi.string;
                else result = UTF16.valueOf(usi.codepoint);
                if (DEBUG) System.out.println("Unspecified: " + ucd.getCodeAndName(result));
                return result;
            }
            
            // extra samples
            if (currentRange < SAMPLE_RANGES.length) {
                try {
                    result = UTF16.valueOf(itemInRange);
                } catch (RuntimeException e) {
                    System.out.println(Utility.hex(itemInRange));
                    throw e;
                }
                ++itemInRange;
                if (itemInRange > endOfRange) {
                    ++currentRange;
                    if (currentRange < SAMPLE_RANGES.length) {
                        startOfRange = itemInRange = SAMPLE_RANGES[currentRange][0];
                        endOfRange = SAMPLE_RANGES[currentRange].length > 1
                            ? SAMPLE_RANGES[currentRange][1]
                            : startOfRange;
                        //skip = ((endOfRange - startOfRange) / 3);
                    }
                } else if (itemInRange > startOfRange + 5 && itemInRange < endOfRange - 5 /* - skip*/) {
                    //itemInRange += skip;
                    itemInRange = endOfRange - 5;
                }
            }
            
            return result;
        }
        
        /**
         * returns a string and its ces
         */
        public String next(int[] ces, int[] len) {

            String result = next(); // null if done
            if (result != null) {
                len[0] = getCEs(result, true, ces);
            }
            return result;
        }
        
        int[] lengthBuffer = new int[1];
        
        /**
         * returns a string and its ces
         */
        public boolean next(Pair result) {
            String s = next(ceListBuffer, lengthBuffer);
            if (s == null) return false;
            result.first = new CEList(ceListBuffer, 0, lengthBuffer[0]);
            result.second = s;
            return true;
        }
        
    }
    
    static final int[][] SAMPLE_RANGES = {
                {0}, // LEAVE EMPTY--Turns into first unassigned character
                {0xFFF0}, 
                {0xD800},
                {0xDFFF},
                {0xFFFE},
                {0xFFFF},
                {0x10000},
                {0xC0000},
                {0xD0000},
                {0x10FFFF},
                {0x10FFFE},
                {0x10FFFF},
                {0x3400, 0x4DB5},
                {0x4E00, 0x9FA5},
                {0xAC00, 0xD7A3},
                {0xA000, 0xA48C},
                {0xE000, 0xF8FF},
                {0x20000, 0x2A6D6},
                {0xE0000, 0xE007E},
                {0xF0000, 0xF00FD},
                {0xFFF00, 0xFFFFD},
                {0x100000, 0x1000FD},
                {0x10FF00, 0x10FFFD},
    };
	                
    /**
     * Adds the collation elements from a file (or other stream) in the UCA format.
     * Values will override any previous mappings.
     */
    private void addCollationElements(BufferedReader in) throws java.io.IOException {
        IntStack tempStack = new IntStack(100);
        StringBuffer multiChars = new StringBuffer(); // used for contracting chars
        String inputLine = "";
        boolean[] wasImplicitLeadPrimary = new boolean[1];
            
        while (true) try {
            inputLine = in.readLine();
            if (inputLine == null) break;       // means file is done
            String line = cleanLine(inputLine); // remove comments, extra whitespace
            if (line.length() == 0) continue;   // skip empty lines
            
            if (DEBUG_SHOW_LINE) {
                System.out.println("Processing: " + inputLine);
            } 

            position[0] = 0;                    // start at front of line
            if (line.startsWith("@")) {
                if (line.startsWith("@version")) {
                    dataVersion = line.substring("@version".length()+1).trim();
                    continue;
                }
                
                if (line.startsWith("@rearrange")) {
                    line = line.substring("@rearrange".length()+1).trim();
                    String[] list = Utility.split(line, ',');
                    for (int i = 0; i < list.length; ++i) {
                        rearrangeList.add(Integer.parseInt(list[i].trim(), 16));
                    }
                    continue;
                }
                
                throw new IllegalArgumentException("Illegal @ command: " + line);
            }
            
            // collect characters
            multiChars.setLength(0);            // clear buffer
            
            char value = getChar(line, position);
            multiChars.append(value);
            
            //fixSurrogateContraction(value);
            char value2 = getChar(line, position);
            // append until we get terminator
            while (value2 != NOT_A_CHAR) {
                multiChars.append(value2);
                value2 = getChar(line, position);
            }

            if (RECORDING_CHARS) {
                found.addAll(multiChars.toString());
            }
            if (!fullData && RECORDING_DATA) {
                if (value == 0 || value == '\t' || value == '\n' || value == '\r'
                  || (0x20 <= value && value <= 0x7F)
                  || (0x80 <= value && value <= 0xFF)
                  || (0x300 <= value && value <= 0x3FF)
                  ) {
                    System.out.println("    + \"" + inputLine + "\\n\"");
                }
            }
            // for recording information
            boolean record = true;
            /* if (multiChars.length() > 0) record = false;
            else */
            if (!toD.isNormalized(value)) record = false;
            
            // collect CEs
            if (false && value == 0x2F00) {
            	System.out.println("debug");
            }
            
            wasImplicitLeadPrimary[0] = false;
            
            int ce = getCEFromLine(value, line, position, record, wasImplicitLeadPrimary);
            int ce2 = getCEFromLine(value, line, position, record, wasImplicitLeadPrimary);
            if (CHECK_UNIQUE && (ce2 == TERMINATOR || CHECK_UNIQUE_EXPANSIONS)) {
                if (!CHECK_UNIQUE_VARIABLES) {
                    checkUnique(value, ce, 0, inputLine); // only need to check first value
                } else {
                    int key1 = ce >>> 16;
                    if (isVariable(ce)) {
                        checkUnique(value, 0, key1, inputLine); // only need to check first value
                    }
                }
            }
            
            tempStack.clear();
            tempStack.push(ce);
            
            while (ce2 != TERMINATOR) {
                tempStack.push(ce2);
                ce2 = getCEFromLine(value, line, position, record, wasImplicitLeadPrimary);
                if (ce2 == TERMINATOR) break;
            } 
            
            ucaData.add(multiChars, tempStack);
            
        } catch (RuntimeException e) {
            System.out.println("Error on line: " + inputLine);
            throw e;
        }
    }
    
    /*
    private void concat(int[] ces1, int[] ces2) {
        
    }
    */
    
    /**
     * Checks the internal tables corresponding to the UCA data.
     */
    private void cleanup() {
        
        UnicodeProperty ubp = UnifiedBinaryProperty.make(
            UCD.BINARY_PROPERTIES + UCD.Logical_Order_Exception, ucd);
        UnicodeSet desiredSet = ubp.getSet();
        
        if (!rearrangeList.equals(desiredSet)) {
            throw new IllegalArgumentException("Rearrangement should be " + desiredSet.toPattern(true)
                + ", but is " + rearrangeList.toPattern(true));
        }
        
        ucaData.checkConsistency();

        Map missingStrings = new HashMap();
        Map tempMap = new HashMap();
        
        Iterator enum = ucaData.getContractions();
        while (enum.hasNext()) {
            String sequence = (String)enum.next();
            //System.out.println("Contraction: " + Utility.hex(sequence));
            for (int i = sequence.length()-1; i > 0; --i) {
                String shorter = sequence.substring(0,i);
                if (!ucaData.contractionTableContains(shorter)) {
                    IntStack tempStack = new IntStack(1);
                    getCEs(shorter, true, tempStack);
                    if (false) System.out.println("WARNING: CLOSING: " + ucd.getCodeAndName(shorter)
                        + " => " + CEList.toString(tempStack));
                    tempMap.put(shorter, tempStack);
                    // missingStrings.put(shorter,"");
                    // collationElements[sequence.charAt(0)] = UNSUPPORTED; // nuke all bad values
                }
            }
        }
        
        // now add them. We couldn't before because we were iterating over it.
        
        enum = tempMap.keySet().iterator();
        while (enum.hasNext()) {
            String shorter = (String) enum.next();
            IntStack tempStack = (IntStack) tempMap.get(shorter);
            ucaData.add(shorter, tempStack);
        }
        
        
        enum = missingStrings.keySet().iterator();
        if (missingStrings.size() != 0) {
            /**
            while (enum.hasMoreElements()) {
                String sequence = (String)enum.nextElement();
                getCE(sequence);
                FIX LATER;
            }
            */
            String errorMessage = "";
            while (enum.hasNext()) {
                String missing = (String)enum.next();
                if (errorMessage.length() != 0) errorMessage += ", ";
                errorMessage += "\"" + missing + "\"";
            }
            throw new IllegalArgumentException("Contracting table not closed! Missing " + errorMessage);
        }

        //fixlater;
        variableLowCE = variableLow << 16;
        variableHighCE = (variableHigh << 16) | 0xFFFF; // turn on bottom bits
        
        //int hangulHackBottom;
        //int hangulHackTop;
        
        //hangulHackBottom = collationElements[0x1100] & 0xFFFF0000; // remove secondaries & tertiaries
        //hangulHackTop = collationElements[0x11F9] | 0xFFFF; // bump up secondaries and tertiaries
        //if (SHOW_STATS) System.out.println("\tHangul Hack: " + Utility.hex(hangulHackBottom) + ", " + Utility.hex(hangulHackTop));
        
        // show some statistics
        if (SHOW_STATS) System.out.println("\tcount1: " + count1);
        if (SHOW_STATS) System.out.println("\tcount2: " + max2);
        if (SHOW_STATS) System.out.println("\tcount3: " + max3);
        if (SHOW_STATS) System.out.println("\tcontractions: " + ucaData.getContractionCount());
        
        if (SHOW_STATS) System.out.println("\tMIN1/MAX1: " + Utility.hex(MIN1) + "/" + Utility.hex(MAX1));
        if (SHOW_STATS) System.out.println("\tMIN2/MAX2: " + Utility.hex(MIN2) + "/" + Utility.hex(MAX2));
        if (SHOW_STATS) System.out.println("\tMIN3/MAX3: " + Utility.hex(MIN3) + "/" + Utility.hex(MAX3));
        
        if (SHOW_STATS) System.out.println("\tVar Min/Max: " + Utility.hex(variableLow) + "/" + Utility.hex(variableHigh));
        if (SHOW_STATS) System.out.println("\tNon-Var Min: " + Utility.hex(nonVariableLow));
        
        if (SHOW_STATS) System.out.println("\trenumberedVariable: " + renumberedVariable);
    }
    
    /**
     * Remove comments, extra whitespace
     */
    private String cleanLine(String line) {
        int commentPosition = line.indexOf('#');
        if (commentPosition >= 0) line = line.substring(0,commentPosition);
        commentPosition = line.indexOf('%');
        if (commentPosition >= 0) line = line.substring(0,commentPosition);
        return line.trim();
    }
    
    /**
     * Get a char from a line, of form: (<space> | <comma>)* <hex>*
     *@param position on input, the place to start at. 
     * On output, updated to point to the next place to search.
     *@return the character, or NOT_A_CHAR when done
     */
    
    // NOTE in case of surrogates, we buffer up the second character!!
    char charBuffer = 0;
    
    private char getChar(String line, int[] position) {
        char ch;
        if (charBuffer != 0) {
            ch = charBuffer;
            charBuffer = 0;
            return ch;
        }
        int start = position[0];
        while (true) { // trim whitespace
            if (start >= line.length()) return NOT_A_CHAR;
            ch = line.charAt(start);
            if (ch != ' ' && ch != ',') break;
            start++;
        }
        // from above, we have at least one char
        int hexLimit = start;
        while ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) {
            hexLimit++;
            ch = line.charAt(hexLimit);
        }
        if (hexLimit >= start + 4) {
            position[0] = hexLimit;
            int cp = Integer.parseInt(line.substring(start,hexLimit),16);
            if (cp <= 0xFFFF) return (char)cp;
            //DEBUGCHAR = true;
            charBuffer = UTF16.getTrailSurrogate(cp);
            return UTF16.getLeadSurrogate(cp);
        }
        
        return NOT_A_CHAR; 
    }
    
    boolean DEBUGCHAR = false;    
    
    BitSet primarySet = new BitSet();
    BitSet secondarySet = new BitSet();
    BitSet tertiarySet = new BitSet();
    
    public int writeUsedWeights(PrintWriter p, int strength, MessageFormat mf) {
        BitSet weights = strength == 1 ? primarySet : strength == 2 ? secondarySet : tertiarySet;
        int first = -1;
        int count = 0;
        for (int i = 0; i <= weights.length(); ++i) {
            if (strength > 1) {
                if (weights.get(i)) {
                    count++;
                    p.println(mf.format(new Object[] {Utility.hex((char)i), new Integer(stCounts[strength][i])}));
                }
                continue;
            }
            if (weights.get(i)) {
                if (first == -1) first = i;
            } else if (first != -1) {
                int last = i-1;
                int diff = last - first + 1;
                count += diff;
                String lastStr = last == first ? "" : Utility.hex((char)last);
                p.println(mf.format(new Object[] {Utility.hex((char)first),lastStr,new Integer(diff), new Integer(count)}));
                first = -1;
            }
        }
        return count;  
    }
    
    int[] secondaryCount = new int[0x200];
    int[] tertiaryCount = new int[0x80];
    int[][] stCounts = {null, null, secondaryCount, tertiaryCount};
    
    /**
     * Gets a CE from a UCA format line
     *@param value the first character for the line. Just used for statistics.
     *@param line a string of form "[.0000.0000.0000.0000]..."
     *@param position on input, the place to start at. 
     * On output, updated to point to the next place to search.
     */
    
    boolean haveVariableWarning = false;
    boolean haveZeroVariableWarning = false;
    
    private int getCEFromLine(char value, String line, int[] position, boolean record, boolean[] lastWasImplicitLead) {
        int start = line.indexOf('[', position[0]);
        if (start == -1) return TERMINATOR;
        boolean variable = line.charAt(start+1) == '*';
        int key1 = Integer.parseInt(line.substring(start+2,start+6),16);
        if (key1 == 0x1299) {
            System.out.println("\t1299");
        }
        int key2 = Integer.parseInt(line.substring(start+7,start+11),16);
        int key3 = Integer.parseInt(line.substring(start+12,start+16),16);
        if (record) {
        	if (lastWasImplicitLead[0]) {
        		lastWasImplicitLead[0] = false;
            } else if (isImplicitLeadPrimary(key1)) {
            	lastWasImplicitLead[0] = true;
            } else {
            	primarySet.set(key1);
            }
            secondarySet.set(key2);
            secondaryCount[key2]++;
            tertiarySet.set(key3);
            tertiaryCount[key3]++;
        }
        if (key1 == 0 && variable) {
            if (!haveZeroVariableWarning) {
                System.out.println("\tBAD DATA: Zero L1s cannot be variable!!: " + line);
                haveZeroVariableWarning = true;
            }
            variable = false; // FIX DATA FILE
        }
        if (key2 > 0x1FF) {
            throw new IllegalArgumentException("Weight2 doesn't fit: " + Utility.hex(key2) + "," + line);
        }
        if (key3 > 0x7F) {
            throw new IllegalArgumentException("Weight3 doesn't fit: " + Utility.hex(key3) + "," + line);
        }
        // adjust variable bounds, if needed
        if (variable) {
            if (key1 > nonVariableLow) {
                if (!haveVariableWarning) {
                    System.out.println("\tBAD DATA: Variable overlap, nonvariable low: "
                    + Utility.hex(nonVariableLow) + ", line: \"" + line + "\"");
                    haveVariableWarning = true;
                }
            } else {
                if (key1 < variableLow) variableLow = key1;
                if (key1 > variableHigh) variableHigh = key1;
            }
        } else if (key1 != 0) { // not variable, not zero
            if (key1 < variableHigh) {
                if (!haveVariableWarning) {
                    System.out.println("\tBAD DATA: Variable overlap, variable high: "
                    + Utility.hex(variableHigh) + ", line: \"" + line + "\"");
                    haveVariableWarning = true;
                }
            } else {
                if (key1 < nonVariableLow) nonVariableLow = key1;
            }
        }
            
        // statistics
        count1++;
        if (key1 != oldKey1) {
            oldKey1 = key1;
            if (count2 > max2) max2 = count2;
            if (count3 > max3) max3 = count3;
            count2 = count3 = 1;
        } else {
            count2++;
            if (key2 != oldKey2) {
                oldKey2 = key2;
                if (count3 > max3) max3 = count3;
                count3 = 1;
            } else {
                count3++;
            }
        }
        position[0] = start + 17;
        /*
        if (VARIABLE && variable) {
            key1 = key2 = key3 = 0;
            if (CHECK_UNIQUE) {
                if (key1 != lastUniqueVariable) renumberedVariable++;
                result = renumberedVariable;     // push primary down
                lastUniqueVariable = key1;
                key3 = key1;
                key1 = key2 = 0;
            }
        }
        */
        // gather some statistics
        if (key1 != 0 && key1 < MIN1) MIN1 = (char)key1;
        if (key2 != 0 && key2 < MIN2) MIN2 = (char)key2;
        if (key3 != 0 && key3 < MIN3) MIN3 = (char)key3;
        if (key1 > MAX1) MAX1 = (char)key1;
        if (key2 > MAX2) MAX2 = (char)key2;
        if (key3 > MAX3) MAX3 = (char)key3;
        return makeKey(key1, key2, key3);
    }
    
    /**
     * Just for statistics
     */
    int lastUniqueVariable = 0;
    int renumberedVariable = 50;
    char MIN1 = '\uFFFF'; // start large; will be reset as table is built
    char MIN2 = '\uFFFF'; // start large; will be reset as table is built
    char MIN3 = '\uFFFF'; // start large; will be reset as table is built
    char MAX1 = '\u0000'; // start small; will be reset as table is built
    char MAX2 = '\u0000'; // start small; will be reset as table is built
    char MAX3 = '\u0000'; // start small; will be reset as table is built
    
    /**
     * Used for checking data file integrity
     */
    private Map uniqueTable = new HashMap();
    
    /**
     * Used for checking data file integrity
     */
    private void checkUnique(char value, int result, int fourth, String line) {
        if (!toD.isNormalized(value)) return; // don't check decomposables.
        Object ceObj = new Long(((long)result << 16) | fourth);
        Object probe = uniqueTable.get(ceObj);
        if (probe != null) {
            System.out.println("\tCE(" + Utility.hex(value) 
              + ")=CE(" + Utility.hex(((Character)probe).charValue()) + "); " + line);
              
        } else {
            uniqueTable.put(ceObj, new Character(value));
        }
    }
}
