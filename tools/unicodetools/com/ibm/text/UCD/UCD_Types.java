/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/UCD_Types.java,v $
* $Date: 2002/07/30 09:56:40 $
* $Revision: 1.14 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

public interface UCD_Types {
    
    public static final int dVersion = 9; // change to fix the generated file D version. If less than zero, no "d"
    
    public static final String BASE_DIR = "C:\\DATA\\";
    public static final String UCD_DIR = BASE_DIR + "UCD\\";
    public static final String BIN_DIR = BASE_DIR + "BIN\\";
    public static final String GEN_DIR = BASE_DIR + "GEN\\";
    
    public static final int 
    	CJK_BASE = 0x4E00,
    	CJK_LIMIT = 0x9FFF+1,
    	CJK_COMPAT_USED_BASE = 0xFA0E,
    	CJK_COMPAT_USED_LIMIT = 0xFA2F+1,
    	CJK_A_BASE = 0x3400,
    	CJK_A_LIMIT = 0x4DBF+1,
    	CJK_B_BASE = 0x20000,
    	CJK_B_LIMIT = 0x2A6DF+1;
    
    static final byte BINARY_FORMAT = 6; // bumped if binary format of UCD changes
    
    // Unicode Property Types
    static final byte 
        NOT_DERIVED = 1, 
        DERIVED_CORE = 2, 
        DERIVED_NORMALIZATION = 4, 
        DERIVED_ALL = 6, 
        ALL = (byte)-1;
        
     static final byte
        NON_ENUMERATED = -1,
        ENUMERATED = 0,
        BINARY = 1,
        FLATTENED_BINARY = 2;
    
    /*
  0	Code value in 4-digit hexadecimal format.
  1	Unicode 2.1 Character Name. These names match exactly the
  2	General Category. This is a useful breakdown into various "character
  3	Canonical Combining Classes. The classes used for the
  4	Bidirectional Category. See the list below for an explanation of the
  5	Character Decomposition. In the Unicode Standard, not all of
  6	Decimal digit value. This is a numeric field. If the character
  7	Digit value. This is a numeric field. If the character represents a
  8	Numeric value. This is a numeric field. If the character has the
  9	If the characters has been identified as a "mirrored" character in
 10	Unicode 1.0 Name. This is the old name as published in Unicode 1.0.
 11	10646 Comment field. This field is informative.
 12	Upper case equivalent mapping. If a character is part of an
 13	Lower case equivalent mapping. Similar to 12. This field is informative.
 14	Title case equivalent mapping. Similar to 12. This field is informative.
    */
    
    
    // for IDs
    static final byte NUMBER = -2, SHORT = -1, NORMAL = 0, LONG = 1, BOTH = 2;

    // Binary ENUM Grouping
    public static final int
        CATEGORY = 0,
        COMBINING_CLASS = 0x100,
        BIDI_CLASS = 0x200,
        DECOMPOSITION_TYPE = 0x300,
        NUMERIC_TYPE = 0x400,
        EAST_ASIAN_WIDTH = 0x500,
        LINE_BREAK = 0x600,
        JOINING_TYPE = 0x700,
        JOINING_GROUP = 0x800,
        BINARY_PROPERTIES = 0x900,
        SCRIPT = 0xA00,
        AGE = 0xB00,
        DERIVED = 0xC00,
        NEXT_ENUM = 0x100,
        LIMIT_ENUM = DERIVED + 0x100;

    public static final int LIMIT_COMBINING_CLASS = 256;

    // getCategory
    public static final byte
	UNASSIGNED		= 0,
	UPPERCASE_LETTER	= 1,
	LOWERCASE_LETTER	= 2,
	TITLECASE_LETTER	= 3,
	MODIFIER_LETTER		= 4,
	OTHER_LETTER		= 5,
	NON_SPACING_MARK	= 6,
	ENCLOSING_MARK		= 7,
	COMBINING_SPACING_MARK	= 8,
	DECIMAL_DIGIT_NUMBER	= 9,
	LETTER_NUMBER		= 10,
	OTHER_NUMBER		= 11,
	SPACE_SEPARATOR		= 12,
	LINE_SEPARATOR		= 13,
	PARAGRAPH_SEPARATOR	= 14,
	CONTROL			= 15,
	FORMAT			= 16,
	UNUSED_CATEGORY			= 17,
	PRIVATE_USE		= 18,
	SURROGATE		= 19,
	DASH_PUNCTUATION	= 20,
	START_PUNCTUATION	= 21,
	END_PUNCTUATION		= 22,
	CONNECTOR_PUNCTUATION	= 23,
	OTHER_PUNCTUATION	= 24,
	MATH_SYMBOL		= 25,
	CURRENCY_SYMBOL		= 26,
	MODIFIER_SYMBOL		= 27,
	OTHER_SYMBOL		= 28,
	INITIAL_PUNCTUATION	= 29,
	FINAL_PUNCTUATION		= 30,
	LIMIT_CATEGORY = FINAL_PUNCTUATION+1,

	// Unicode abbreviations
	Lu = UPPERCASE_LETTER,
	Ll = LOWERCASE_LETTER,
	Lt = TITLECASE_LETTER,
    Lm = MODIFIER_LETTER,
	Lo = OTHER_LETTER,
	Mn = NON_SPACING_MARK,
	Me = ENCLOSING_MARK,
	Mc = COMBINING_SPACING_MARK,
	Nd = DECIMAL_DIGIT_NUMBER,
	Nl = LETTER_NUMBER,
	No = OTHER_NUMBER,
	Zs = SPACE_SEPARATOR,
	Zl = LINE_SEPARATOR,
	Zp = PARAGRAPH_SEPARATOR,
	Cc = CONTROL,
	Cf = FORMAT,
	Cs = SURROGATE,
	Co = PRIVATE_USE,
	Cn = UNASSIGNED,
	Pc = CONNECTOR_PUNCTUATION,
	Pd = DASH_PUNCTUATION,
	Ps = START_PUNCTUATION,
	Pe = END_PUNCTUATION,
	Po = OTHER_PUNCTUATION,
	Pi = INITIAL_PUNCTUATION,
	Pf = FINAL_PUNCTUATION,
	Sm = MATH_SYMBOL,
	Sc = CURRENCY_SYMBOL,
	Sk = MODIFIER_SYMBOL,
	So = OTHER_SYMBOL;

    static final int
        LETTER_MASK = (1<<Lu) | (1<<Ll) | (1<<Lt) | (1<<Lm) | (1 << Lo),
        CASED_LETTER_MASK = (1<<Lu) | (1<<Ll) | (1<<Lt),
        MARK_MASK = (1<<Mn) | (1<<Me) | (1<<Mc),
        NUMBER_MASK = (1<<Nd) | (1<<Nl) | (1<<No),
        SEPARATOR_MASK = (1<<Zs) | (1<<Zl) | (1<<Zp),
        CONTROL_MASK = (1<<Cc) | (1<<Cf) | (1<<Cs) | (1<<Co),
        PUNCTUATION_MASK = (1<<Pc) | (1<<Pd) | (1<<Ps) | (1<<Pe) | (1<<Po) | (1<<Pi) | (1<<Pf),
        SYMBOL_MASK = (1<<Sm) | (1<<Sc) | (1<<Sk) | (1<<So),
        UNASSIGNED_MASK = (1<<Cn);

	// Binary Properties

	public static final byte
	    BidiMirrored = 0,
	    CompositionExclusion = 1,
        White_space = 2,
        Non_break = 3,
	    Bidi_Control = 4,
        Join_Control = 5,
        Dash = 6,
        Hyphen = 7,
        Quotation_Mark = 8,
        Terminal_Punctuation = 9,
        Math_Property = 10,
        Hex_Digit = 11,
        ASCII_Hex_Digit = 12,
	    Alphabetic = 13,
        Ideographic = 14,
        Diacritic = 15,
        Extender = 16,
        Other_Lowercase = 17,
        Other_Uppercase = 18,
        Noncharacter_Code_Point = 19,
        CaseFoldTurkishI = 20,
        Other_GraphemeExtend = 21,
        GraphemeLink = 22,
        IDS_BinaryOperator = 23,
        IDS_TrinaryOperator = 24,
        Radical = 25,
        UnifiedIdeograph = 26,
        Other_Default_Ignorable_Code_Point = 27,
        Deprecated = 28,
        Soft_Dotted = 29,
        Logical_Order_Exception = 30,
	    LIMIT_BINARY_PROPERTIES = 31;

	/*
    static final int
	    BidiMirroredMask = 1<<BidiMirrored,
	    CompositionExclusionMask = 1<<CompositionExclusion,
	    AlphabeticMask = 1<<Alphabetic,
	    Bidi_ControlMask = 1<<Bidi_Control,
        DashMask = 1<<Dash,
        DiacriticMask = 1<<Diacritic,
        ExtenderMask = 1<<Extender,
        Hex_DigitMask = 1<<Hex_Digit,
        HyphenMask = 1<<Hyphen,
        IdeographicMask = 1<<Ideographic,
        Join_ControlMask = 1<<Join_Control,
        Math_PropertyMask = 1<<Math_Property,
        Non_breakMask = 1<<Non_break,
        Noncharacter_Code_PointMask = 1<<Noncharacter_Code_Point,
        Other_LowercaseMask = 1<<Other_Lowercase,
        Other_UppercaseMask = 1<<Other_Uppercase,
        Quotation_MarkMask = 1<<Quotation_Mark,
        Terminal_PunctuationMask = 1<<Terminal_Punctuation,
        White_spaceMask = 1<<White_space;
    */

    // line break
    public static final byte
        LB_XX = 0, LB_OP = 1, LB_CL = 2, LB_QU = 3, LB_GL = 4, LB_NS = 5, LB_EX = 6, LB_SY = 7,
        LB_IS = 8, LB_PR = 9, LB_PO = 10, LB_NU = 11, LB_AL = 12, LB_ID = 13, LB_IN = 14, LB_HY = 15,
        LB_CM = 16, LB_BB = 17, LB_BA = 18, LB_SP = 19, LB_BK = 20, LB_CR = 21, LB_LF = 22, LB_CB = 23,
        LB_SA = 24, LB_AI = 25, LB_B2 = 26, LB_SG = 27, LB_ZW = 28,
        LIMIT_LINE_BREAK = 29,
        LB_LIMIT = LIMIT_LINE_BREAK;

    // east asian width
    public static final byte
         EAN = 0, EAA = 1, EAH = 2, EAW = 3, EAF = 4, EANa = 5,
         LIMIT_EAST_ASIAN_WIDTH = 6;

	// bidi class
	static final byte
	    BIDI_L = 0,     // Left-Right; Most alphabetic, syllabic, and logographic characters (e.g., CJK ideographs)
	    BIDI_R = 1,     // Right-Left; Arabic, Hebrew, and punctuation specific to those scripts
	    BIDI_EN = 2,    // European Number
	    BIDI_ES = 3,    // European Number Separator
	    BIDI_ET = 4,    // European Number Terminator
	    BIDI_AN = 5,    // Arabic Number
	    BIDI_CS = 6,    // Common Number Separator
	    BIDI_B = 7,     // Block Separator
	    BIDI_S = 8,     // Segment Separator
	    BIDI_WS = 9,    // Whitespace
	    BIDI_ON = 10,   // Other Neutrals ; All other characters: punctuation, symbols
	    LIMIT_BIDI_2 = 11,
	    BIDI_UNUSED = 11,
	    BIDI_BN = 12,
	    BIDI_NSM = 13,
	    BIDI_AL = 14,
	    BIDI_LRO = 15,
	    BIDI_RLO = 16,
	    BIDI_LRE = 17,
	    BIDI_RLE = 18,
	    BIDI_PDF = 19,
	    LIMIT_BIDI_CLASS = 20;

	// decompositionType
    static final byte NONE = 0,
        CANONICAL = 1,
        COMPATIBILITY = 2,
        COMPAT_UNSPECIFIED = 2,	// Otherwise unspecified compatibility character.
        COMPAT_FONT = 3,		// A font variant (e.g. a blackletter form).
        COMPAT_NOBREAK = 4,	// A no-break version of a space or hyphen.
        COMPAT_INITIAL = 5,	// // An initial presentation form (Arabic).
        COMPAT_MEDIAL = 6,	// // A medial presentation form (Arabic).
        COMPAT_FINAL = 7,	// // 	A final presentation form (Arabic).
        COMPAT_ISOLATED = 8,	// An isolated presentation form (Arabic).
        COMPAT_CIRCLE = 9,	// An encircled form.
        COMPAT_SUPER = 10,	// 	A superscript form.
        COMPAT_SUB = 11,	// 	A subscript form.
        COMPAT_VERTICAL = 12,	// A vertical layout presentation form.
        COMPAT_WIDE = 13,	// 	A wide (or zenkaku) compatibility character.
        COMPAT_NARROW = 14,	// A narrow (or hankaku) compatibility character.
        COMPAT_SMALL = 15,	// 	A small variant form (CNS compatibility).
        COMPAT_SQUARE = 16,	// A CJK squared font variant.
        COMPAT_FRACTION = 17,	// A vulgar fraction form.
        LIMIT_DECOMPOSITION_TYPE = 18;

    // mirrored type
    static final byte NO = 0, YES = 1, MIRRORED_LIMIT = 2;

    // for QuickCheck
    static final byte QNO = 0, QMAYBE = 1, QYES = 2;

    // case type
    static final byte LOWER = 0, TITLE = 1, UPPER = 2, UNCASED = 3, FOLD = 3, CASE_LIMIT = 4;
    static final byte SIMPLE = 0, FULL = 8;

    // normalization type
    static final byte UNNORMALIZED = 0, C = 1, KC = 2, D = 3, KD = 4, FORM_LIMIT = 5;

    // numericType
    static final byte NUMERIC_NONE = 0, NUMERIC = 1, DIGIT = 2, DECIMAL = 3,
        LIMIT_NUMERIC_TYPE = 4;

    public static final byte // SCRIPT CODE
        COMMON_SCRIPT = 0,
        LATIN_SCRIPT = 1,
        GREEK_SCRIPT = 2,
        CYRILLIC_SCRIPT = 3,
        ARMENIAN_SCRIPT = 4,
        HEBREW_SCRIPT = 5,
        ARABIC_SCRIPT = 6,
        SYRIAC_SCRIPT = 7,
        THAANA_SCRIPT = 8,
        DEVANAGARI_SCRIPT = 9,
        BENGALI_SCRIPT = 10,
        GURMUKHI_SCRIPT = 11,
        GUJARATI_SCRIPT = 12,
        ORIYA_SCRIPT = 13,
        TAMIL_SCRIPT = 14,
        TELUGU_SCRIPT = 15,
        KANNADA_SCRIPT = 16,
        MALAYALAM_SCRIPT = 17,
        SINHALA_SCRIPT = 18,
        THAI_SCRIPT = 19,
        LAO_SCRIPT = 20,
        TIBETAN_SCRIPT = 21,
        MYANMAR_SCRIPT = 22,
        GEORGIAN_SCRIPT = 23,
        UNUSED_SCRIPT = 24,
        HANGUL_SCRIPT = 25,
        ETHIOPIC_SCRIPT = 26,
        CHEROKEE_SCRIPT = 27,
        ABORIGINAL_SCRIPT = 28,
        OGHAM_SCRIPT = 29,
        RUNIC_SCRIPT = 30,
        KHMER_SCRIPT = 31,
        MONGOLIAN_SCRIPT = 32,
        HIRAGANA_SCRIPT = 33,
        KATAKANA_SCRIPT = 34,
        BOPOMOFO_SCRIPT = 35,
        HAN_SCRIPT = 36,
        YI_SCRIPT = 37,
        OLD_ITALIC_SCRIPT = 38,
        GOTHIC_SCRIPT = 39,
        DESERET_SCRIPT = 40,
        INHERITED_SCRIPT = 41,
        TAGALOG_SCRIPT = 42, 
        HANUNOO_SCRIPT = 43,
        BUHID_SCRIPT = 44,
        TAGBANWA_SCRIPT = 45,
        LIMIT_SCRIPT = 46;

  static final int
    UNKNOWN = 0,
    AGE10 = 1,
    AGE20 = 2,
    AGE21 = 3,
    AGE30 = 4,
    AGE31 = 5,
    LIMIT_AGE = 6;



public static byte
    JT_C = 0,
    JT_D = 1,
    JT_R = 2,
    JT_U = 3,
    JT_L = 4,
    JT_T = 5,
    LIMIT_JOINING_TYPE = 6;

public static byte
    NO_SHAPING = 0,
    AIN = 1,
    ALAPH = 2,
    ALEF = 3,
    BEH = 4,
    BETH = 5,
    DAL = 6,
    DALATH_RISH = 7,
    E = 8,
    FEH = 9,
    FINAL_SEMKATH = 10,
    GAF = 11,
    GAMAL = 12,
    HAH = 13,
    HAMZA_ON_HEH_GOAL = 14,
    HE = 15,
    HEH = 16,
    HEH_GOAL = 17,
    HETH = 18,
    KAF = 19,
    KAPH = 20,
    KNOTTED_HEH = 21,
    LAM = 22,
    LAMADH = 23,
    MEEM = 24,
    MIM = 25,
    NOON = 26,
    NUN = 27,
    PE = 28,
    QAF = 29,
    QAPH = 30,
    REH = 31,
    REVERSED_PE = 32,
    SAD = 33,
    SADHE = 34,
    SEEN = 35,
    SEMKATH = 36,
    SHIN = 37,
    SWASH_KAF = 38,
    TAH = 39,
    TAW = 40,
    TEH_MARBUTA = 41,
    TETH = 42,
    WAW = 43,
    SYRIAC_WAW = 44,
    YEH = 45,
    YEH_BARREE = 46,
    YEH_WITH_TAIL = 47,
    YUDH = 48,
    YUDH_HE = 49,
    ZAIN = 50,
    LIMIT_JOINING_GROUP = 51;
    
    static final byte NFD = 0, NFC = 1, NFKD = 2, NFKC = 3;    
    public static final int
        NF_COMPATIBILITY_MASK = 2,
        NF_COMPOSITION_MASK = 1;
    
    // DERIVED PROPERTY
    
    static final int
        PropMath = 0,
        PropAlphabetic = 1,
        PropLowercase = 2,
        PropUppercase = 3,

        ID_Start = 4,
        ID_Continue_NO_Cf = 5,

        Mod_ID_Start = 6,
        Mod_ID_Continue_NO_Cf = 7,

        Missing_Uppercase = 8,
        Missing_Lowercase = 9,
        Missing_Mixedcase = 10,

        FC_NFKC_Closure = 11,

        FullCompExclusion = 12,
        FullCompInclusion = 13,

        QuickNFD = 14,
        QuickNFC = 15,
        QuickNFKD = 16,
        QuickNFKC = 17,

        ExpandsOnNFD = 18,
        ExpandsOnNFC = 19,
        ExpandsOnNFKD = 20,
        ExpandsOnNFKC = 21,

        GenNFD = 22,
        GenNFC = 23,
        GenNFKD = 24,
        GenNFKC = 25,
        
        DefaultIgnorable = 26,
        GraphemeExtend = 27,
        GraphemeBase = 28,
        
        FC_NFC_Closure = 29,
        
        Other_Case_Ignorable = 30,
        Case_Ignorable = 31,
        Type_i = 32,
        
        NFC_Leading = 33,
        NFC_TrailingNonZero = 34,
        NFC_TrailingZero = 35,
        NFC_Resulting = 36,
        
        NFD_UnsafeStart = 37,
        NFC_UnsafeStart = 38,
        NFKD_UnsafeStart = 39,
        NFKC_UnsafeStart = 40,

        NFD_Skippable = 41,
        NFC_Skippable = 42,
        NFKD_Skippable = 43,
        NFKC_Skippable = 44,

        DERIVED_PROPERTY_LIMIT = 41;
    
}