/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/UCA_Types.java,v $ 
* $Date: 2002/07/14 22:07:00 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;

public interface UCA_Types {
    public static final char LEVEL_SEPARATOR = '\u0000'; 
    /**
     * Expanding characters are marked with a exception bit combination
     * in the collationElement table.
     * This means that they map to more than one CE, which is looked up in
     * the expansionTable by index.
     */
    static final int EXPANDING_MASK = 0xFFFF0000; // marks expanding range start
    
    /**
     * This mask is used to get the index from an EXPANDING exception.
     * The contracting characters can also make use of this in a future optimization.
     */
    static final int EXCEPTION_INDEX_MASK = 0x0000FFFF;

    /**
     * Contracting characters are marked with a exception bit combination 
     * in the collationElement table.
     * This means that they are the first character of a contraction, and need
     * to be looked up (with following characters) in the contractingTable.<br>
     * This isn't a MASK since there is exactly one value.
     */
    static final int CONTRACTING = 0xFFFE0000;

    static final int UNSUPPORTED_FLAG = 0xFFFD0000;

    
    /**
     * Used to composed Hangul and Han characters
     */
     
    static final int NEUTRAL_SECONDARY = 0x20;
    static final int NEUTRAL_TERTIARY = 0x02;
    
    /** Enum for alternate handling */
    public static final byte SHIFTED = 0, ZEROED = 1, NON_IGNORABLE = 2, SHIFTED_TRIMMED = 3, LAST = 3;
    
    /**
     * Used to terminate a list of CEs
     */
    public static final int TERMINATOR = 0xFFFFFFFF;   // CE that marks end of string
          
    /**
     * Any unsupported characters (those not in the UCA data tables) 
     * are marked with a exception bit combination
     * so that they can be treated specially.<br>
     * There are at least 34 values, so that we can use a range for surrogates
     * However, we do add to the first weight if we have surrogate pairs!
     */
    static final int UNSUPPORTED_CJK_BASE = 0xFB40;
    static final int UNSUPPORTED_CJK_AB_BASE = 0xFB80;
    static final int UNSUPPORTED_OTHER_BASE = 0xFBC0;
    
    static final int UNSUPPORTED_BASE = UNSUPPORTED_CJK_BASE;
    static final int UNSUPPORTED_LIMIT = UNSUPPORTED_OTHER_BASE + 0x40;
    
 
    /**
     *  Special char value that means failed or terminated
     */
    static final char NOT_A_CHAR = '\uFFFF';
    
    /**
     * CEType
     */
    static final byte NORMAL_CE = 0, CONTRACTING_CE = 1, EXPANDING_CE = 2, 
        CJK_CE = 3, CJK_AB_CE = 4, HANGUL_CE = 5, UNSUPPORTED_CE = 7,
        FIXED_CE = 3;
        // SURROGATE_CE = 6, 
   
}