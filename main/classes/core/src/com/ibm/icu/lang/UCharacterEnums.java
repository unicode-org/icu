/**
 *******************************************************************************
 * Copyright (C) 2004-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.lang;

/**
 * A container for the different 'enumerated types' used by UCharacter.
 * @stable ICU 3.0
 */
public class UCharacterEnums {

    /** This is just a namespace, it is not instantiatable. */
    ///CLOVER:OFF
    private UCharacterEnums() {}

    /**
     * 'Enum' for the CharacterCategory constants.  These constants are 
     * compatible in name <b>but not in value</b> with those defined in
     * <code>java.lang.Character</code>.
     * @see UCharacterCategory
     * @stable ICU 3.0
     */
    public static interface ECharacterCategory {
        /**
         * Unassigned character type
         * @stable ICU 2.1
         */
        public static final byte UNASSIGNED              = 0; 

        /**
         * Character type Cn
         * Not Assigned (no characters in [UnicodeData.txt] have this property) 
         * @stable ICU 2.6
         */
        public static final byte GENERAL_OTHER_TYPES     = 0;

        /**
         * Character type Lu
         * @stable ICU 2.1
         */
        public static final byte UPPERCASE_LETTER        = 1;

        /**
         * Character type Ll
         * @stable ICU 2.1
         */
        public static final byte LOWERCASE_LETTER        = 2;

        /**
         * Character type Lt
         * @stable ICU 2.1
         */

        public static final byte TITLECASE_LETTER        = 3;

        /**
         * Character type Lm
         * @stable ICU 2.1
         */
        public static final byte MODIFIER_LETTER         = 4;

        /**
         * Character type Lo
         * @stable ICU 2.1
         */
        public static final byte OTHER_LETTER            = 5;

        /**
         * Character type Mn
         * @stable ICU 2.1
         */
        public static final byte NON_SPACING_MARK        = 6;

        /**
         * Character type Me
         * @stable ICU 2.1
         */
        public static final byte ENCLOSING_MARK          = 7;

        /**
         * Character type Mc
         * @stable ICU 2.1
         */
        public static final byte COMBINING_SPACING_MARK  = 8;

        /**
         * Character type Nd
         * @stable ICU 2.1      
         */
        public static final byte DECIMAL_DIGIT_NUMBER    = 9;

        /**
         * Character type Nl
         * @stable ICU 2.1
         */
        public static final byte LETTER_NUMBER           = 10;
        
        /**
         * Character type No
         * @stable ICU 2.1
         */
        public static final byte OTHER_NUMBER            = 11;

        /**
         * Character type Zs
         * @stable ICU 2.1
         */
        public static final byte SPACE_SEPARATOR         = 12;

        /**
         * Character type Zl
         * @stable ICU 2.1
         */
        public static final byte LINE_SEPARATOR          = 13;

        /**
         * Character type Zp
         * @stable ICU 2.1
         */
        public static final byte PARAGRAPH_SEPARATOR     = 14;

        /**
         * Character type Cc
         * @stable ICU 2.1
         */
        public static final byte CONTROL                 = 15;

        /**
         * Character type Cf
         * @stable ICU 2.1
         */
        public static final byte FORMAT                  = 16;

        /**
         * Character type Co
         * @stable ICU 2.1
         */
        public static final byte PRIVATE_USE             = 17;

        /**
         * Character type Cs
         * @stable ICU 2.1
         */
        public static final byte SURROGATE               = 18;

        /**
         * Character type Pd
         * @stable ICU 2.1
         */
        public static final byte DASH_PUNCTUATION        = 19;

        /**
         * Character type Ps
         * @stable ICU 2.1
         */
        public static final byte START_PUNCTUATION       = 20;
        
        /**
         * Character type Pe
         * @stable ICU 2.1
         */
        public static final byte END_PUNCTUATION         = 21;

        /**
         * Character type Pc
         * @stable ICU 2.1
         */
        public static final byte CONNECTOR_PUNCTUATION   = 22;

        /**
         * Character type Po
         * @stable ICU 2.1
         */
        public static final byte OTHER_PUNCTUATION       = 23;

        /**
         * Character type Sm
         * @stable ICU 2.1
         */
        public static final byte MATH_SYMBOL             = 24;

        /**
         * Character type Sc
         * @stable ICU 2.1
         */
        public static final byte CURRENCY_SYMBOL         = 25;
        
        /**
         * Character type Sk
         * @stable ICU 2.1
         */
        public static final byte MODIFIER_SYMBOL         = 26;
        
        /**
         * Character type So
         * @stable ICU 2.1
         */
        public static final byte OTHER_SYMBOL            = 27;
        
        /**
         * Character type Pi
         * @see #INITIAL_QUOTE_PUNCTUATION
         * @stable ICU 2.1
         */
        public static final byte INITIAL_PUNCTUATION     = 28;

        /**
         * Character type Pi
         * This name is compatible with java.lang.Character's name for this type.
         * @see #INITIAL_PUNCTUATION
         * @stable ICU 2.8
         */
        public static final byte INITIAL_QUOTE_PUNCTUATION = 28;

        /**
         * Character type Pf
         * @see #FINAL_QUOTE_PUNCTUATION
         * @stable ICU 2.1
         */
        public static final byte FINAL_PUNCTUATION       = 29;

        /**
         * Character type Pf
         * This name is compatible with java.lang.Character's name for this type.
         * @see #FINAL_PUNCTUATION
         * @stable ICU 2.8
         */
        public static final byte FINAL_QUOTE_PUNCTUATION   = 29;
        
        /**
         * Character type count
         * @stable ICU 2.1
         */
        public static final byte CHAR_CATEGORY_COUNT     = 30;
    }

    /**
     * 'Enum' for the CharacterDirection constants.  There are two sets
     * of names, those used in ICU, and those used in the JDK.  The
     * JDK constants are compatible in name <b>but not in value</b> 
     * with those defined in <code>java.lang.Character</code>.
     * @see UCharacterDirection
     * @stable ICU 3.0
     */
    public static interface ECharacterDirection {
        /**
         * Directional type L
         * @stable ICU 2.1
         */
        public static final int LEFT_TO_RIGHT              = 0;

        /**
         * JDK-compatible synonym for LEFT_TO_RIGHT.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = (byte)LEFT_TO_RIGHT;

        /**
         * Directional type R
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT              = 1;

        /**
         * JDK-compatible synonym for RIGHT_TO_LEFT.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = (byte)RIGHT_TO_LEFT;

        /**
         * Directional type EN
         * @stable ICU 2.1
         */
        public static final int EUROPEAN_NUMBER            = 2;

        /**
         * JDK-compatible synonym for EUROPEAN_NUMBER.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = (byte)EUROPEAN_NUMBER;

        /**
         * Directional type ES
         * @stable ICU 2.1
         */
        public static final int EUROPEAN_NUMBER_SEPARATOR  = 3;

        /**
         * JDK-compatible synonym for EUROPEAN_NUMBER_SEPARATOR.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = (byte)EUROPEAN_NUMBER_SEPARATOR;

        /**
         * Directional type ET
         * @stable ICU 2.1
         */
        public static final int EUROPEAN_NUMBER_TERMINATOR = 4;

        /**
         * JDK-compatible synonym for EUROPEAN_NUMBER_TERMINATOR.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = (byte)EUROPEAN_NUMBER_TERMINATOR;

        /**
         * Directional type AN
         * @stable ICU 2.1
         */                                                    
        public static final int ARABIC_NUMBER              = 5;

        /**
         * JDK-compatible synonym for ARABIC_NUMBER.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_ARABIC_NUMBER = (byte)ARABIC_NUMBER;

        /**
         * Directional type CS
         * @stable ICU 2.1
         */
        public static final int COMMON_NUMBER_SEPARATOR    = 6;

        /**
         * JDK-compatible synonym for COMMON_NUMBER_SEPARATOR.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = (byte)COMMON_NUMBER_SEPARATOR;

        /**
         * Directional type B
         * @stable ICU 2.1
         */
        public static final int BLOCK_SEPARATOR            = 7;

        /**
         * JDK-compatible synonym for BLOCK_SEPARATOR.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = (byte)BLOCK_SEPARATOR;

        /**
         * Directional type S
         * @stable ICU 2.1
         */      
        public static final int SEGMENT_SEPARATOR          = 8;

        /**
         * JDK-compatible synonym for SEGMENT_SEPARATOR.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = (byte)SEGMENT_SEPARATOR;

        /**
         * Directional type WS
         * @stable ICU 2.1
         */
        public static final int WHITE_SPACE_NEUTRAL        = 9;

        /**
         * JDK-compatible synonym for WHITE_SPACE_NEUTRAL.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_WHITESPACE = (byte)WHITE_SPACE_NEUTRAL;

        /**
         * Directional type ON
         * @stable ICU 2.1
         */
        public static final int OTHER_NEUTRAL              = 10;

        /**
         * JDK-compatible synonym for OTHER_NEUTRAL.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_OTHER_NEUTRALS = (byte)OTHER_NEUTRAL;

        /**
         * Directional type LRE
         * @stable ICU 2.1
         */
        public static final int LEFT_TO_RIGHT_EMBEDDING    = 11;

        /**
         * JDK-compatible synonym for LEFT_TO_RIGHT_EMBEDDING.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = (byte)LEFT_TO_RIGHT_EMBEDDING;

        /**
         * Directional type LRO
         * @stable ICU 2.1
         */
        public static final int LEFT_TO_RIGHT_OVERRIDE     = 12;  

        /**
         * JDK-compatible synonym for LEFT_TO_RIGHT_OVERRIDE.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = (byte)LEFT_TO_RIGHT_OVERRIDE;

        /**
         * Directional type AL
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_ARABIC       = 13;

        /**
         * JDK-compatible synonym for RIGHT_TO_LEFT_ARABIC.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = (byte)RIGHT_TO_LEFT_ARABIC;

        /**
         * Directional type RLE
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_EMBEDDING    = 14;

        /**
         * JDK-compatible synonym for RIGHT_TO_LEFT_EMBEDDING.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = (byte)RIGHT_TO_LEFT_EMBEDDING;

        /**
         * Directional type RLO
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_OVERRIDE     = 15;

        /**
         * JDK-compatible synonym for RIGHT_TO_LEFT_OVERRIDE.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = (byte)RIGHT_TO_LEFT_OVERRIDE;

        /**
         * Directional type PDF
         * @stable ICU 2.1
         */
        public static final int POP_DIRECTIONAL_FORMAT     = 16;

        /**
         * JDK-compatible synonym for POP_DIRECTIONAL_FORMAT.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = (byte)POP_DIRECTIONAL_FORMAT;

        /**
         * Directional type NSM
         * @stable ICU 2.1
         */
        public static final int DIR_NON_SPACING_MARK       = 17;

        /**
         * JDK-compatible synonym for DIR_NON_SPACING_MARK.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_NONSPACING_MARK = (byte)DIR_NON_SPACING_MARK;

        /**
         * Directional type BN
         * @stable ICU 2.1
         */
        public static final int BOUNDARY_NEUTRAL           = 18;

        /**
         * JDK-compatible synonym for BOUNDARY_NEUTRAL.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = (byte)BOUNDARY_NEUTRAL;

        /**
         * Number of directional types
         * @stable ICU 2.1
         */
        public static final int CHAR_DIRECTION_COUNT       = 19;

        /**
         * Undefined bidirectional character type. Undefined <code>char</code>
         * values have undefined directionality in the Unicode specification.
         * @stable ICU 3.0
         */
        public static final byte DIRECTIONALITY_UNDEFINED = -1;
    }
}
