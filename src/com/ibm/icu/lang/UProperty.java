/**
*******************************************************************************
* Copyright (C) 1996-2004, international Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.lang;

/**
 * <p>Selection constants for Unicode properties. </p>
 * <p>These constants are used in functions like 
 * UCharacter.hasBinaryProperty(int) to select one of the Unicode properties. 
 * </p>
 * <p>The properties APIs are intended to reflect Unicode properties as 
 * defined in the Unicode Character Database (UCD) and Unicode Technical 
 * Reports (UTR).</p>
 * <p>For details about the properties see <a href=http://www.unicode.org>
 * http://www.unicode.org</a>.</p> 
 * <p>For names of Unicode properties see the UCD file PropertyAliases.txt.
 * </p> 
 * <p>Important: If ICU is built with UCD files from Unicode versions below 
 * 3.2, then properties marked with "new" are not or not fully 
 * available. Check UCharacter.getUnicodeVersion() to be sure.</p>
 * @author Syn Wee Quek
 * @stable ICU 2.6
 * @see com.ibm.icu.lang.UCharacter
 */
public interface UProperty
{
    // public data member --------------------------------------------------
  
    /** 
     * <p>Binary property Alphabetic. </p>
     * <p>Property for UCharacter.isUAlphabetic(), different from the property 
     * in UCharacter.isalpha().</p>
     * <p>Lu + Ll + Lt + Lm + Lo + Nl + Other_Alphabetic.</p>
     * @stable ICU 2.6
     */ 
    public static final int ALPHABETIC = 0; 

    /** 
     * First constant for binary Unicode properties. 
     * @stable ICU 2.6
     */ 
    public static final int BINARY_START = ALPHABETIC;

    /** 
     * Binary property ASCII_Hex_Digit (0-9 A-F a-f).
     * @stable ICU 2.6
     */ 
    public static final int ASCII_HEX_DIGIT = 1; 

    /** 
     * <p>Binary property Bidi_Control.</p>
     * <p>Format controls which have specific functions in the Bidi Algorithm.
     * </p> 
     * @stable ICU 2.6
     */ 
    public static final int BIDI_CONTROL = 2; 

    /** 
     * <p>Binary property Bidi_Mirrored.</p> 
     * <p>Characters that may change display in RTL text.</p> 
     * <p>Property for UCharacter.isMirrored().</p> 
     * <p>See Bidi Algorithm; UTR 9.</p> 
     * @stable ICU 2.6
     */ 
    public static final int BIDI_MIRRORED = 3; 

    /** 
     * <p>Binary property Dash.</p> 
     * <p>Variations of dashes.</p> 
     * @stable ICU 2.6
     */ 
    public static final int DASH = 4; 

    /** 
     * <p>Binary property Default_Ignorable_Code_Point (new).
     * </p> 
     * <p>Property that indicates codepoint is ignorable in most processing.
     * </p>
     * <p>Codepoints (2060..206F, FFF0..FFFB, E0000..E0FFF) + 
     * Other_Default_Ignorable_Code_Point + (Cf + Cc + Cs - White_Space)</p> 
     * @stable ICU 2.6
     */ 
    public static final int DEFAULT_IGNORABLE_CODE_POINT = 5; 

    /** 
     * <p>Binary property Deprecated (new).</p> 
     * <p>The usage of deprecated characters is strongly discouraged.</p> 
     * @stable ICU 2.6
     */ 
    public static final int DEPRECATED = 6; 

    /** 
     * <p>Binary property Diacritic.</p> 
     * <p>Characters that linguistically modify the meaning of another 
     * character to which they apply.</p> 
     * @stable ICU 2.6
     */ 
    public static final int DIACRITIC = 7; 

    /** 
     * <p>Binary property Extender.</p> 
     * <p>Extend the value or shape of a preceding alphabetic character, e.g.
     * length and iteration marks.</p> 
     * @stable ICU 2.6
     */ 
    public static final int EXTENDER = 8; 

    /** 
     * <p>Binary property Full_Composition_Exclusion.</p> 
     * <p>CompositionExclusions.txt + Singleton Decompositions + 
     * Non-Starter Decompositions.</p> 
     * @stable ICU 2.6
     */ 
    public static final int FULL_COMPOSITION_EXCLUSION = 9; 

    /** 
     * <p>Binary property Grapheme_Base (new).</p>
     * <p>For programmatic determination of grapheme cluster boundaries. 
     * [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Grapheme_Link-Grapheme_Extend-CGJ</p> 
     * @stable ICU 2.6
     */ 
    public static final int GRAPHEME_BASE = 10; 

    /** 
     * <p>Binary property Grapheme_Extend (new).</p> 
     * <p>For programmatic determination of grapheme cluster boundaries.</p> 
     * <p>Me+Mn+Mc+Other_Grapheme_Extend-Grapheme_Link-CGJ</p> 
     * @stable ICU 2.6
     */ 
    public static final int GRAPHEME_EXTEND = 11; 

    /** 
     * <p>Binary property Grapheme_Link (new).</p> 
     * <p>For programmatic determination of grapheme cluster boundaries.</p> 
     * @stable ICU 2.6
     */ 
    public static final int GRAPHEME_LINK = 12; 

    /** 
     * <p>Binary property Hex_Digit.</p> 
     * <p>Characters commonly used for hexadecimal numbers.</p> 
     * @stable ICU 2.6
     */ 
    public static final int HEX_DIGIT = 13; 

    /** 
     * <p>Binary property Hyphen.</p> 
     * <p>Dashes used to mark connections between pieces of words, plus the 
     * Katakana middle dot.</p> 
     * @stable ICU 2.6
     */ 
    public static final int HYPHEN = 14; 

    /** 
     * <p>Binary property ID_Continue.</p>
     * <p>Characters that can continue an identifier.</p> 
     * <p>ID_Start+Mn+Mc+Nd+Pc</p> 
     * @stable ICU 2.6
     */ 
    public static final int ID_CONTINUE = 15; 

    /** 
     * <p>Binary property ID_Start.</p> 
     * <p>Characters that can start an identifier.</p> 
     * <p>Lu+Ll+Lt+Lm+Lo+Nl</p> 
     * @stable ICU 2.6
     */ 
    public static final int ID_START = 16; 

    /** 
     * <p>Binary property Ideographic.</p> 
     * <p>CJKV ideographs.</p> 
     * @stable ICU 2.6
     */ 
    public static final int IDEOGRAPHIC = 17; 

    /** 
     * <p>Binary property IDS_Binary_Operator (new).</p> 
     * <p>For programmatic determination of Ideographic Description Sequences.
     * </p> 
     * @stable ICU 2.6
     */ 
    public static final int IDS_BINARY_OPERATOR = 18; 

    /** 
     * <p>Binary property IDS_Trinary_Operator (new).</p> 
     * <p?For programmatic determination of Ideographic Description 
     * Sequences.</p> 
     * @stable ICU 2.6
     */ 
    public static final int IDS_TRINARY_OPERATOR = 19; 

    /** 
     * <p>Binary property Join_Control.</p> 
     * <p>Format controls for cursive joining and ligation.</p> 
     * @stable ICU 2.6
     */ 
    public static final int JOIN_CONTROL = 20; 

    /** 
     * <p>Binary property Logical_Order_Exception (new).</p> 
     * <p>Characters that do not use logical order and require special 
     * handling in most processing.</p> 
     * @stable ICU 2.6
     */ 
    public static final int LOGICAL_ORDER_EXCEPTION = 21; 

    /** 
     * <p>Binary property Lowercase.</p> 
     * <p>Same as UCharacter.isULowercase(), different from 
     * UCharacter.islower().</p> 
     * <p>Ll+Other_Lowercase</p> 
     * @stable ICU 2.6
     */ 
    public static final int LOWERCASE = 22; 

    /** <p>Binary property Math.</p> 
     * <p>Sm+Other_Math</p> 
     * @stable ICU 2.6
     */ 
    public static final int MATH = 23; 

    /** 
     * <p>Binary property Noncharacter_Code_Point.</p> 
     * <p>Code points that are explicitly defined as illegal for the encoding 
     * of characters.</p> 
     * @stable ICU 2.6
     */ 
    public static final int NONCHARACTER_CODE_POINT = 24; 

    /** 
     * <p>Binary property Quotation_Mark.</p> 
     * @stable ICU 2.6
     */ 
    public static final int QUOTATION_MARK = 25; 

    /** 
     * <p>Binary property Radical (new).</p> 
     * <p>For programmatic determination of Ideographic Description 
     * Sequences.</p> 
     * @stable ICU 2.6
     */ 
    public static final int RADICAL = 26; 

    /** 
     * <p>Binary property Soft_Dotted (new).</p> 
     * <p>Characters with a "soft dot", like i or j.</p>
     * <p>An accent placed on these characters causes the dot to disappear.</p> 
     * @stable ICU 2.6
     */ 
    public static final int SOFT_DOTTED = 27; 

    /** 
     * <p>Binary property Terminal_Punctuation.</p> 
     * <p>Punctuation characters that generally mark the end of textual 
     * units.</p> 
     * @stable ICU 2.6
     */ 
    public static final int TERMINAL_PUNCTUATION = 28; 

    /** 
     * <p>Binary property Unified_Ideograph (new).</p> 
     * <p>For programmatic determination of Ideographic Description 
     * Sequences.</p> 
     * @stable ICU 2.6
     */ 
    public static final int UNIFIED_IDEOGRAPH = 29; 

    /** 
     * <p>Binary property Uppercase.</p> 
     * <p>Same as UCharacter.isUUppercase(), different from 
     * UCharacter.isUpperCase().</p> 
     * <p>Lu+Other_Uppercase</p> 
     * @stable ICU 2.6
     */ 
    public static final int UPPERCASE = 30; 

    /** 
     * <p>Binary property White_Space.</p> 
     * <p>Same as UCharacter.isUWhiteSpace(), different from 
     * UCharacter.isSpace() and UCharacter.isWhitespace().</p> 
     * Space characters+TAB+CR+LF-ZWSP-ZWNBSP</p> 
     * @stable ICU 2.6
     */ 
    public static final int WHITE_SPACE = 31; 

    /** 
     * <p>Binary property XID_Continue.</p> 
     * <p>ID_Continue modified to allow closure under normalization forms 
     * NFKC and NFKD.</p> 
     * @stable ICU 2.6
     */ 
    public static final int XID_CONTINUE = 32; 

    /** 
     * <p>Binary property XID_Start.</p> 
     * <p>ID_Start modified to allow closure under normalization forms NFKC 
     * and NFKD.</p> 
     * @stable ICU 2.6
     */ 
    public static final int XID_START = 33; 

    /**
     * <p>Binary property Case_Sensitive.</p>
     * <p>Either the source of a case
     * mapping or _in_ the target of a case mapping. Not the same as
     * the general category Cased_Letter.</p>
     * @stable ICU 2.6
     */
    public static final int CASE_SENSITIVE = 34;

    /**
     * Binary property STerm (new in Unicode 4.0.1).
     * Sentence Terminal. Used in UAX #29: Text Boundaries
     * (http://www.unicode.org/reports/tr29/)
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int S_TERM = 35;

    /**
     * Binary property Variation_Selector (new in Unicode 4.0.1).
     * Indicates all those characters that qualify as Variation Selectors.
     * For details on the behavior of these characters,
     * see StandardizedVariants.html and 15.6 Variation Selectors.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int VARIATION_SELECTOR = 36;

    /** 
     * Binary property NFD_Inert.
     * ICU-specific property for characters that are inert under NFD,
     * i.e., they do not interact with adjacent characters.
     * Used for example in normalizing transforms in incremental mode
     * to find the boundary of safely normalizable text despite possible
     * text additions.
     *
     * There is one such property per normalization form.
     * These properties are computed as follows - an inert character is:
     * a) unassigned, or ALL of the following:
     * b) of combining class 0.
     * c) not decomposed by this normalization form.
     * AND if NFC or NFKC,
     * d) can never compose with a previous character.
     * e) can never compose with a following character.
     * f) can never change if another character is added.
     * Example: a-breve might satisfy all but f, but if you
     * add an ogonek it changes to a-ogonek + breve
     *
     * See also com.ibm.text.UCD.NFSkippable in the ICU4J repository,
     * and icu/source/common/unormimp.h .
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFD_INERT = 37;

    /** 
     * Binary property NFKD_Inert.
     * ICU-specific property for characters that are inert under NFKD,
     * i.e., they do not interact with adjacent characters.
     * Used for example in normalizing transforms in incremental mode
     * to find the boundary of safely normalizable text despite possible
     * text additions.
     * @see #NFD_INERT
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFKD_INERT = 38;

    /** 
     * Binary property NFC_Inert.
     * ICU-specific property for characters that are inert under NFC,
     * i.e., they do not interact with adjacent characters.
     * Used for example in normalizing transforms in incremental mode
     * to find the boundary of safely normalizable text despite possible
     * text additions.
     * @see #NFD_INERT
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFC_INERT = 39;

    /** 
     * Binary property NFKC_Inert.
     * ICU-specific property for characters that are inert under NFKC,
     * i.e., they do not interact with adjacent characters.
     * Used for example in normalizing transforms in incremental mode
     * to find the boundary of safely normalizable text despite possible
     * text additions.
     * @see #NFD_INERT
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFKC_INERT = 40;

    /**
     * Binary Property Segment_Starter.
     * ICU-specific property for characters that are starters in terms of
     * Unicode normalization and combining character sequences.
     * They have ccc=0 and do not occur in non-initial position of the
     * canonical decomposition of any character
     * (like " in NFD(a-umlaut) and a Jamo T in an NFD(Hangul LVT)).
     * ICU uses this property for segmenting a string for generating a set of
     * canonically equivalent strings, e.g. for canonical closure while
     * processing collation tailoring rules.
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int SEGMENT_STARTER = 41;

    /** 
     * <p>One more than the last constant for binary Unicode properties.</p> 
     * @stable ICU 2.6
     */
    public static final int BINARY_LIMIT = 42;
    
    /** 
     * Enumerated property Bidi_Class.
     * Same as UCharacter.getDirection(int), returns UCharacterDirection values. 
     * @stable ICU 2.4 
     */
    public static final int BIDI_CLASS = 0x1000;

    /** 
     * First constant for enumerated/integer Unicode properties. 
     * @stable ICU 2.4
     */
    public static final int INT_START = BIDI_CLASS;

    /** 
     * Enumerated property Block.
     * Same as UCharacter.UnicodeBlock.of(int), returns UCharacter.UnicodeBlock 
     * values. 
     * @stable ICU 2.4 
     */
    public static final int BLOCK = 0x1001;

    /** 
     * Enumerated property Canonical_Combining_Class.
     * Same as UCharacter.getCombiningClass(int), returns 8-bit numeric values. 
     * @stable ICU 2.4 
     */
    public static final int CANONICAL_COMBINING_CLASS = 0x1002;

    /** 
     * Enumerated property Decomposition_Type.
     * Returns UCharacter.DecompositionType values. 
     * @stable ICU 2.4
     */
    public static final int DECOMPOSITION_TYPE = 0x1003;

    /** 
     * Enumerated property East_Asian_Width.
     * See http://www.unicode.org/reports/tr11/
     * Returns UCharacter.EastAsianWidth values. 
     * @stable ICU 2.4 
     */
    public static final int EAST_ASIAN_WIDTH = 0x1004;

    /** 
     * Enumerated property General_Category.
     * Same as UCharacter.getType(int), returns UCharacterCategory values. 
     * @stable ICU 2.4 
     */
    public static final int GENERAL_CATEGORY = 0x1005;

    /** 
     * Enumerated property Joining_Group.
     * Returns UCharacter.JoiningGroup values. 
     * @stable ICU 2.4 
     */
    public static final int JOINING_GROUP = 0x1006;

    /** 
     * Enumerated property Joining_Type.
     * Returns UCharacter.JoiningType values. 
     * @stable ICU 2.4 
     */
    public static final int JOINING_TYPE = 0x1007;

    /** 
     * Enumerated property Line_Break.
     * Returns UCharacter.LineBreak values. 
     * @stable ICU 2.4 
     */
    public static final int LINE_BREAK = 0x1008;

    /** 
     * Enumerated property Numeric_Type.
     * Returns UCharacter.NumericType values. 
     * @stable ICU 2.4 
     */
    public static final int NUMERIC_TYPE = 0x1009;

    /** 
     * Enumerated property Script.
     * Same as UScript.getScript(int), returns UScript values. 
     * @stable ICU 2.4 
     */
    public static final int SCRIPT = 0x100A;
    
    /** 
     * Enumerated property Hangul_Syllable_Type, new in Unicode 4.
     * Returns HangulSyllableType values. 
     * @stable ICU 2.6 
     */
    public static final int HANGUL_SYLLABLE_TYPE = 0x100B;

    /**
     * Enumerated property NFD_Quick_Check.
     * Returns numeric values compatible with Normalizer.QuickCheckResult.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFD_QUICK_CHECK = 0x100C;

    /**
     * Enumerated property NFKD_Quick_Check.
     * Returns numeric values compatible with Normalizer.QuickCheckResult.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFKD_QUICK_CHECK = 0x100D;

    /**
     * Enumerated property NFC_Quick_Check.
     * Returns numeric values compatible with Normalizer.QuickCheckResult.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFC_QUICK_CHECK = 0x100E;

    /**
     * Enumerated property NFKC_Quick_Check.
     * Returns numeric values compatible with Normalizer.QuickCheckResult.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int NFKC_QUICK_CHECK = 0x100F;

    /**
     * Enumerated property Lead_Canonical_Combining_Class.
     * ICU-specific property for the ccc of the first code point
     * of the decomposition, or lccc(c)=ccc(NFD(c)[0]).
     * Useful for checking for canonically ordered text;
     * see Normalizer.FCD and http://www.unicode.org/notes/tn5/#FCD .
     * Returns 8-bit numeric values like CANONICAL_COMBINING_CLASS.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int LEAD_CANONICAL_COMBINING_CLASS = 0x1010;

    /**
     * Enumerated property Trail_Canonical_Combining_Class.
     * ICU-specific property for the ccc of the last code point
     * of the decomposition, or lccc(c)=ccc(NFD(c)[last]).
     * Useful for checking for canonically ordered text;
     * see Normalizer.FCD and http://www.unicode.org/notes/tn5/#FCD .
     * Returns 8-bit numeric values like CANONICAL_COMBINING_CLASS.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int TRAIL_CANONICAL_COMBINING_CLASS = 0x1011;

    /** 
     * One more than the last constant for enumerated/integer Unicode 
     * properties. 
     * @stable ICU 2.4 
     */
    public static final int INT_LIMIT = 0x1012;

    /** 
     * Bitmask property General_Category_Mask.
     * This is the General_Category property returned as a bit mask.
     * When used in UCharacter.getIntPropertyValue(c),
     * returns bit masks for UCharacterCategory values where exactly one bit is set. 
     * When used with UCharacter.getPropertyValueName() and UCharacter.getPropertyValueEnum(), 
     * a multi-bit mask is used for sets of categories like "Letters". 
     * @stable ICU 2.4
     */ 
    public static final int GENERAL_CATEGORY_MASK = 0x2000; 

    /** 
     * First constant for bit-mask Unicode properties. 
     * @stable ICU 2.4
     */
    public static final int MASK_START = GENERAL_CATEGORY_MASK;

    /** 
     * One more than the last constant for bit-mask Unicode properties. 
     * @stable ICU 2.4
     */ 
    public static final int MASK_LIMIT = 0x2001; 
    
    /**
     * Double property Numeric_Value.
     * Corresponds to UCharacter.getUnicodeNumericValue(int).
     * @stable ICU 2.4
     */
    public static final int NUMERIC_VALUE = 0x3000;

    /**
     * First constant for double Unicode properties.
     * @stable ICU 2.4
     */
    public static final int DOUBLE_START = NUMERIC_VALUE;

    /**
     * One more than the last constant for double Unicode properties.
     * @stable ICU 2.4
     */
    public static final int DOUBLE_LIMIT = 0x3001;

    /**
     * String property Age.
     * Corresponds to UCharacter.getAge(int).
     * @stable ICU 2.4
     */
    public static final int AGE = 0x4000;

    /**
     * First constant for string Unicode properties.
     * @stable ICU 2.4
     */
    public static final int STRING_START = AGE;

    /**
     * String property Bidi_Mirroring_Glyph.
     * Corresponds to UCharacter.getMirror(int).
     * @stable ICU 2.4
     */
    public static final int BIDI_MIRRORING_GLYPH = 0x4001;

    /**
     * String property Case_Folding.
     * Corresponds to UCharacter.foldCase(String, boolean).
     * @stable ICU 2.4
     */
    public static final int CASE_FOLDING = 0x4002;

    /**
     * String property ISO_Comment.
     * Corresponds to UCharacter.getISOComment(int).
     * @stable ICU 2.4
     */
    public static final int ISO_COMMENT = 0x4003;

    /**
     * String property Lowercase_Mapping.
     * Corresponds to UCharacter.toLowerCase(String).
     * @stable ICU 2.4
     */
    public static final int LOWERCASE_MAPPING = 0x4004;

    /**
     * String property Name.
     * Corresponds to UCharacter.getName(int).
     * @stable ICU 2.4
     */
    public static final int NAME = 0x4005;

    /**
     * String property Simple_Case_Folding.
     * Corresponds to UCharacter.foldCase(int, boolean).
     * @stable ICU 2.4
     */
    public static final int SIMPLE_CASE_FOLDING = 0x4006;

    /**
     * String property Simple_Lowercase_Mapping.
     * Corresponds to UCharacter.toLowerCase(int).
     * @stable ICU 2.4
     */
    public static final int SIMPLE_LOWERCASE_MAPPING = 0x4007;

    /**
     * String property Simple_Titlecase_Mapping.
     * Corresponds to UCharacter.toTitleCase(int).
     * @stable ICU 2.4
     */
    public static final int SIMPLE_TITLECASE_MAPPING = 0x4008;

    /**
     * String property Simple_Uppercase_Mapping.
     * Corresponds to UCharacter.toUpperCase(int).
     * @stable ICU 2.4
     */
    public static final int SIMPLE_UPPERCASE_MAPPING = 0x4009;

    /**
     * String property Titlecase_Mapping.
     * Corresponds to UCharacter.toTitleCase(String).
     * @stable ICU 2.4
     */
    public static final int TITLECASE_MAPPING = 0x400A;

    /**
     * String property Unicode_1_Name.
     * Corresponds to UCharacter.getName1_0(int).
     * @stable ICU 2.4
     */
    public static final int UNICODE_1_NAME = 0x400B;

    /**
     * String property Uppercase_Mapping.
     * Corresponds to UCharacter.toUpperCase(String).
     * @stable ICU 2.4
     */
    public static final int UPPERCASE_MAPPING = 0x400C;

    /**
     * One more than the last constant for string Unicode properties.
     * @stable ICU 2.4
     */
    public static final int STRING_LIMIT = 0x400D;

    /**
     * Selector constants for UCharacter.getPropertyName() and
     * UCharacter.getPropertyValueName().  These selectors are used to
     * choose which name is returned for a given property or value.
     * All properties and values have a long name.  Most have a short
     * name, but some do not.  Unicode allows for additional names,
     * beyond the long and short name, which would be indicated by
     * LONG + i, where i=1, 2,...
     *
     * @see UCharacter#getPropertyName
     * @see UCharacter#getPropertyValueName
     * @stable ICU 2.4
     */
    public interface NameChoice {
        /**
         * Selector for the abbreviated name of a property or value.
         * Most properties and values have a short name; those that do
         * not return null.
         * @stable ICU 2.4
         */
        static final int SHORT = 0;

        /**
         * Selector for the long name of a property or value.  All
         * properties and values have a long name.
         * @stable ICU 2.4
         */
        static final int LONG = 1;

        /**
         * The number of predefined property name choices.  Individual
         * properties or values may have more than COUNT aliases.
         * @stable ICU 2.4
         */
        static final int COUNT = 2;
    }
}
