/**
*******************************************************************************
* Copyright (C) 1996-2001, international Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*      /usr/cvs/icu4j/icu4j/src/com/ibm/icu/text/UCharacterCategory.java $ 
* $Date: 2002/10/03 23:42:02 $ 
* $Revision: 1.4 $
*
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
 * @since March 8 2002
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
  	 */ 
    public static final int ALPHABETIC = 0; 
    /** 
     * First constant for binary Unicode properties. 
     */ 
    public static final int BINARY_START = ALPHABETIC;
    /** 
     * Binary property ASCII_Hex_Digit (0-9 A-F a-f).
     */ 
    public static final int ASCII_HEX_DIGIT = 1; 
    /** 
     * <p>Binary property Bidi_Control.</p>
     * <p>Format controls which have specific functions in the Bidi Algorithm.
     * </p> 
     */ 
   	public static final int BIDI_CONTROL = 2; 
    /** 
     * <p>Binary property Bidi_Mirrored.</p> 
     * <p>Characters that may change display in RTL text.</p> 
     * <p>Property for UCharacter.isMirrored().</p> 
     * <p>See Bidi Algorithm; UTR 9.</p> 
     */ 
    public static final int BIDI_MIRRORED = 3; 
    /** 
     * <p>Binary property Dash.</p> 
     * <p>Variations of dashes.</p> 
     */ 
    public static final int DASH = 4; 
    /** 
     * <p>Binary property Default_Ignorable_Code_Point (new).
     * </p> 
     * <p>Property that indicates codepoint is ignorable in most processing.
     * </p>
     * <p>Codepoints (2060..206F, FFF0..FFFB, E0000..E0FFF) + 
     * Other_Default_Ignorable_Code_Point + (Cf + Cc + Cs - White_Space)</p> 
     */ 
    public static final int DEFAULT_IGNORABLE_CODE_POINT = 5; 
    /** 
     * <p>Binary property Deprecated (new).</p> 
     * <p>The usage of deprecated characters is strongly discouraged.</p> 
     */ 
    public static final int DEPRECATED = 6; 
    /** 
     * <p>Binary property Diacritic.</p> 
     * <p>Characters that linguistically modify the meaning of another 
     * character to which they apply.</p> 
     */ 
    public static final int DIACRITIC = 7; 
    /** 
     * <p>Binary property Extender.</p> 
     * <p>Extend the value or shape of a preceding alphabetic character, e.g.
     * length and iteration marks.</p> 
     */ 
    public static final int EXTENDER = 8; 
    /** 
     * <p>Binary property Full_Composition_Exclusion.</p> 
     * <p>CompositionExclusions.txt + Singleton Decompositions + 
     * Non-Starter Decompositions.</p> 
     */ 
    public static final int FULL_COMPOSITION_EXCLUSION = 9; 
    /** 
     * <p>Binary property Grapheme_Base (new).</p>
     * <p>For programmatic determination of grapheme cluster boundaries. 
     * [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Grapheme_Link-Grapheme_Extend-CGJ</p> 
     */ 
    public static final int GRAPHEME_BASE = 10; 
    /** 
     * <p>Binary property Grapheme_Extend (new).</p> 
     * <p>For programmatic determination of grapheme cluster boundaries.</p> 
     * <p>Me+Mn+Mc+Other_Grapheme_Extend-Grapheme_Link-CGJ</p> 
     */ 
    public static final int GRAPHEME_EXTEND = 11; 
    /** 
     * <p>Binary property Grapheme_Link (new).</p> 
     * <p>For programmatic determination of grapheme cluster boundaries.</p> 
     */ 
    public static final int GRAPHEME_LINK = 12; 
    /** 
     * <p>Binary property Hex_Digit.</p> 
     * <p>Characters commonly used for hexadecimal numbers.</p> 
     */ 
    public static final int HEX_DIGIT = 13; 
    /** 
     * <p>Binary property Hyphen.</p> 
     * <p>Dashes used to mark connections between pieces of words, plus the 
     * Katakana middle dot.</p> 
     */ 
    public static final int HYPHEN = 14; 
    /** 
     * <p>Binary property ID_Continue.</p>
     * <p>Characters that can continue an identifier.</p> 
     * <p>ID_Start+Mn+Mc+Nd+Pc</p> 
     */ 
    public static final int ID_CONTINUE = 15; 
    /** 
     * <p>Binary property ID_Start.</p> 
     * <p>Characters that can start an identifier.</p> 
     * <p>Lu+Ll+Lt+Lm+Lo+Nl</p> 
     */ 
    public static final int ID_START = 16; 
    /** 
     * <p>Binary property Ideographic.</p> 
     * <p>CJKV ideographs.</p> 
     */ 
    public static final int IDEOGRAPHIC = 17; 
    /** 
     * <p>Binary property IDS_Binary_Operator (new).</p> 
     * <p>For programmatic determination of Ideographic Description Sequences.
     * </p> 
     */ 
    public static final int IDS_BINARY_OPERATOR = 18; 
    /** 
     * <p>Binary property IDS_Trinary_Operator (new).</p> 
     * <p?For programmatic determination of Ideographic Description 
     * Sequences.</p> 
     */ 
    public static final int IDS_TRINARY_OPERATOR = 19; 
    /** 
     * <p>Binary property Join_Control.</p> 
     * <p>Format controls for cursive joining and ligation.</p> 
     */ 
    public static final int JOIN_CONTROL = 20; 
    /** 
     * <p>Binary property Logical_Order_Exception (new).</p> 
     * <p>Characters that do not use logical order and require special 
     * handling in most processing.</p> 
     */ 
    public static final int LOGICAL_ORDER_EXCEPTION = 21; 
    /** 
     * <p>Binary property Lowercase.</p> 
     * <p>Same as UCharacter.isULowercase(), different from 
     * UCharacter.islower().</p> 
     * <p>Ll+Other_Lowercase</p> 
     */ 
    public static final int LOWERCASE = 22; 
    /** <p>Binary property Math.</p> 
     * <p>Sm+Other_Math</p> 
     */ 
    public static final int MATH = 23; 
    /** 
     * <p>Binary property Noncharacter_Code_Point.</p> 
     * <p>Code points that are explicitly defined as illegal for the encoding 
     * of characters.</p> 
     */ 
    public static final int NONCHARACTER_CODE_POINT = 24; 
    /** 
     * <p>Binary property Quotation_Mark.</p> 
     */ 
    public static final int QUOTATION_MARK = 25; 
    /** 
     * <p>Binary property Radical (new).</p> 
     * <p>For programmatic determination of Ideographic Description 
     * Sequences.</p> 
     */ 
    public static final int RADICAL = 26; 
    /** 
     * <p>Binary property Soft_Dotted (new).</p> 
     * <p>Characters with a "soft dot", like i or j.</p>
     * <p>An accent placed on these characters causes the dot to disappear.</p> 
     */ 
    public static final int SOFT_DOTTED = 27; 
    /** 
     * <p>Binary property Terminal_Punctuation.</p> 
     * <p>Punctuation characters that generally mark the end of textual 
     * units.</p> 
     */ 
    public static final int TERMINAL_PUNCTUATION = 28; 
    /** 
     * <p>Binary property Unified_Ideograph (new).</p> 
     * <p>For programmatic determination of Ideographic Description 
     * Sequences.</p> 
     */ 
    public static final int UNIFIED_IDEOGRAPH = 29; 
    /** 
     * <p>Binary property Uppercase.</p> 
     * <p>Same as UCharacter.isUUppercase(), different from 
     * UCharacter.isUpperCase().</p> 
     * <p>Lu+Other_Uppercase</p> 
     */ 
    public static final int UPPERCASE = 30; 
    /** 
     * <p>Binary property White_Space.</p> 
     * <p>Same as UCharacter.isUWhiteSpace(), different from 
     * UCharacter.isSpace() and UCharacter.isWhitespace().</p> 
     * Space characters+TAB+CR+LF-ZWSP-ZWNBSP</p> 
     */ 
    public static final int WHITE_SPACE = 31; 
    /** 
     * <p>Binary property XID_Continue.</p> 
     * <p>ID_Continue modified to allow closure under normalization forms 
     * NFKC and NFKD.</p> 
     */ 
    public static final int XID_CONTINUE = 32; 
    /** 
     * <p>Binary property XID_Start.</p> 
     * <p>ID_Start modified to allow closure under normalization forms NFKC 
     * and NFKD.</p> 
     */ 
    public static final int XID_START = 33; 
    /** 
     * <p>One more than the last constant for binary Unicode properties.</p> 
     */ 
    public static final int BINARY_LIMIT = 34;
    /** 
     * Enumerated property Bidi_Class.
     * Same as UCharacter.getDirection(int), returns UCharacterDirection values. 
     * @draft ICU 2.4 
     */
    public static final int BIDI_CLASS = 0x1000;
    /** 
     * First constant for enumerated/integer Unicode properties. 
     * @draft ICU 2.4
     */
    public static final int INT_START = BIDI_CLASS;
    /** 
     * Enumerated property Block.
     * Same as UCharacter.UnicodeBlock.of(int), returns UCharacter.UnicodeBlock 
     * values. 
     * @draft ICU 2.4 
     */
    public static final int BLOCK = 0x1001;
    /** 
     * Enumerated property Canonical_Combining_Class.
     * Same as UCharacter.getCombiningClass(int), returns 8-bit numeric values. 
     * @draft ICU 2.4 
     */
    public static final int CANONICAL_COMBINING_CLASS = 0x1002;
    /** 
     * Enumerated property Decomposition_Type.
     * Returns UCharacter.DecompositionType values. 
     * @draft ICU 2.4
     */
    public static final int DECOMPOSITION_TYPE = 0x1003;
    /** 
     * Enumerated property East_Asian_Width.
     * See http://www.unicode.org/reports/tr11/
     * Returns UCharacter.EastAsianWidth values. 
     * @draft ICU 2.4 
     */
    public static final int EAST_ASIAN_WIDTH = 0x1004;
    /** 
     * Enumerated property General_Category.
     * Same as UCharacter.getType(int), returns UCharacterCategory values. 
     * @draft ICU 2.4 
     */
    public static final int GENERAL_CATEGORY = 0x1005;
    /** 
     * Enumerated property Joining_Group.
     * Returns UCharacter.JoiningGroup values. 
     * @draft ICU 2.4 
     */
    public static final int JOINING_GROUP = 0x1006;
    /** 
     * Enumerated property Joining_Type.
     * Returns UCharacter.JoiningType values. 
     * @draft ICU 2.4 
     */
    public static final int JOINING_TYPE = 0x1007;
    /** 
     * Enumerated property Line_Break.
     * Returns UCharacter.LineBreak values. 
     * @draft ICU 2.4 
     */
    public static final int LINE_BREAK = 0x1008;
    /** 
     * Enumerated property Numeric_Type.
     * Returns UCharacter.NumericType values. 
     * @draft ICU 2.4 
     */
    public static final int NUMERIC_TYPE = 0x1009;
    /** 
     * Enumerated property Script.
     * Same as UScript.getScript(int), returns UScript values. 
     * @draft ICU 2.4 
     */
    public static final int SCRIPT = 0x100A;
    /** 
     * One more than the last constant for enumerated/integer Unicode 
     * properties. 
     * @draft ICU 2.4 
     */
    public static final int INT_LIMIT = 0x100B;
}
