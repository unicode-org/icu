/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/UCD_Names.java,v $
* $Date: 2006/04/05 22:12:44 $
* $Revision: 1.32 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.text.utility.*;


final class UCD_Names implements UCD_Types {
    
    public static String[][] NON_ENUMERATED_NAMES = {
        {"na", "Name"},
        {"dm", "Decomposition_Mapping"},
        {"nv", "Numeric_Value"},
        {"bmg", "Bidi_Mirroring_Glyph"},
        {"lc", "Lowercase_Mapping"},
        {"uc", "Uppercase_Mapping"},
        {"tc", "Titlecase_Mapping"},
        {"cf", "Case_Folding"},
        {"slc", "Simple_Lowercase_Mapping"},
        {"suc", "Simple_Uppercase_Mapping"},
        {"stc", "Simple_Titlecase_Mapping"},
        {"sfc", "Simple_Case_Folding"},
        {"scc", "Special_Case_Condition"},
        {"blk", "Block"},
        {"na1", "Unicode_1_Name"},
        {"isc", "ISO_Comment"},
        {"age", "Age"},
    };

    static final String[] UNIFIED_PROPERTY_HEADERS = {
        "General Category (listing UnicodeData.txt, field 2: see UCD.html)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   Cn.",
        "Combining Class (listing UnicodeData.txt, field 3: see UCD.html)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   0.",
        "Bidi Class (listing UnicodeData.txt, field 4: see UCD.html)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   L.",
        "Decomposition Type (from UnicodeData.txt, field 5: see UCD.html)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   None.",
        "Numeric Type (from UnicodeData.txt, field 6/7/8 plus Unihan.txt: see UCD.html)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   None.",
        "East Asian Width (listing EastAsianWidth.txt, field 1)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   N.",
        "Line Break (listing LineBreak.txt, field 1)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   XX.",
        "Joining Type (listing ArabicShaping.txt, field 2).\r\n"
            + "#\tType T is derived, as described in ArabicShaping.txt\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   U.",
        "Joining Group (listing ArabicShaping.txt, field 3)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   NO_JOINING_GROUP.",
        "BidiMirrored (listing UnicodeData.txt, field 9: see UCD.html)\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   N.",
        "Script\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   COMMON.",
        "Age (from a comparison of UCD versions 1.1 [minus Hangul], 2.0, 2.1, 3.0, 3.1)",
        "Hangul Syllable Type\r\n"
            + "#\tAll code points not explicitly listed in this file have the property\r\n"
            + "#\tvalue:   NA.",
        "Derived"
    };

    static final String[] UNIFIED_PROPERTIES = {
        "GeneralCategory",
        "CanonicalCombiningClass",
        "BidiClass",
        "DecompositionType",
        "NumericType",
        "EastAsianWidth",
        "LineBreak",
        "JoiningType",
        "JoiningGroup",
        "",
        "Script",
        "Age",
        "Hangul_Syllable_Type",
        ""
    };

    static final String[] SHORT_UNIFIED_PROPERTIES = {
        "gc",
        "ccc",
        "bc",
        "dt",
        "nt",
        "ea",
        "lb",
        "jt",
        "jg",
        "",
        "sc",
        "age",
        "hst",
        "",
    };


    static final String[] BP = {
	    "Bidi_Mirrored",
	    "Composition_Exclusion",
        "White_Space",
        "NonBreak",
	    "Bidi_Control",
        "Join_Control",
        "Dash",
        "Hyphen",
        "Quotation_Mark",
        "Terminal_Punctuation",
        "Other_Math",
        "Hex_Digit",
        "ASCII_Hex_Digit",
	    "Other_Alphabetic",
        "Ideographic",
        "Diacritic",
        "Extender",
        "Other_Lowercase",
        "Other_Uppercase",
        "Noncharacter_Code_Point",
        "Case_Fold_Turkish_I",
        "Other_Grapheme_Extend",
        "Grapheme_Link",
        "IDS_Binary_Operator",
        "IDS_Trinary_Operator",
        "Radical",
        "Unified_Ideograph",
        "Other_Default_Ignorable_Code_Point",
        "Deprecated",
        "Soft_Dotted",
        "Logical_Order_Exception",
        "Other_ID_Start",
        "STerm",
        "Variation_Selector",
        "Other_ID_Continue",
		"Pattern_White_Space",
		"Pattern_Syntax"
    };

    static final String[] SHORT_BP = {
	    "Bidi_M",
	    "CE",
        "WSpace",
        "NBrk",
	    "Bidi_C",
        "JoinC",
        "Dash",
        "Hyphen",
        "QMark",
        "Term",
        "OMath",
        "Hex",
        "AHex",
	    "OAlpha",
        "Ideo",
        "Dia",
        "Ext",
        "OLower",
        "OUpper",
        "NChar",
        "TurkI",
        "OGrExt",
        "Gr_Link",
        "IDSB",
        "IDST",
        "Radical",
        "UIdeo",
        "ODI",
        "Dep",
        "SD",
        "LOE",
        "OIDS",
        "STerm",
        "VS",
        "OIDC",
		"PatWS",
		"PatSyn"
    };

    /*
    static final String[] BP_OLD = {
	    "BidiMirrored",
	    "CompositionExclusion",
        "White_space",
        "Non_break",
	    "Bidi_Control",
        "Join_Control",
        "Dash",
        "Hyphen",
        "Quotation_Mark",
        "Terminal_Punctuation",
        "Math",
        "Hex_Digit",
	    "Other_Alphabetic",
        "Ideographic",
        "Diacritic",
        "Extender",
        "Other_Lowercase",
        "Other_Uppercase",
        "Noncharacter_Code_Point",
        "Other_GraphemeExtend",
        "GraphemeLink",
        "IDS_BinaryOperator",
        "IDS_TrinaryOperator",
        "Radical",
        "UnifiedIdeograph"
    };
    */

    static final String[] DeletedProperties = {
        "Private_Use",
        "Composite",
        "Format_Control",
        "High_Surrogate",
        "Identifier_Part_Not_Cf",
        "Low_Surrogate",
        "Other_Format_Control",
        "Private_Use_High_Surrogate",
        "Unassigned_Code_Point"
    };

	static final String[] YN_TABLE = {"F", "T"};
	static final String[] YN_TABLE_LONG = {"False", "True"};

    static String[] EAST_ASIAN_WIDTH = {
        "N", "A", "H", "W", "F", "Na"
    };

    static String[] LONG_EAST_ASIAN_WIDTH = {
        "Neutral", "Ambiguous", "Halfwidth", "Wide", "Fullwidth", "Narrow"
    };

    static final String[] LINE_BREAK = {
        "XX", "OP", "CL", "QU", "GL", "NS", "EX", "SY",
        "IS", "PR", "PO", "NU", "AL", "ID", "IN", "HY",
        "CM", "BB", "BA", "SP", "BK", "CR", "LF", "CB",
        "SA", "AI", "B2", "SG", "ZW",
        "NL",
        "WJ", "JL", "JV", "JT", "H2", "H3"
        //"JL",
        //"JV",
        //"JT",

    };

    static final String[] LONG_LINE_BREAK = {
        "Unknown", "OpenPunctuation", "ClosePunctuation", "Quotation",
        "Glue", "Nonstarter", "Exclamation", "BreakSymbols",
        "InfixNumeric", "PrefixNumeric", "PostfixNumeric",
        "Numeric", "Alphabetic", "Ideographic", "Inseparable", "Hyphen",
        "CombiningMark", "BreakBefore", "BreakAfter", "Space",
        "MandatoryBreak", "CarriageReturn", "LineFeed", "ContingentBreak",
        "ComplexContext", "Ambiguous", "BreakBoth", "Surrogate", "ZWSpace",
        "Next_Line",
        "Word_Joiner", "JL", "JV", "JT", "H2", "H3"
        //"Leading_Jamo",
        //"Vowel_Jamo",
        //"Trailing_Jamo",
    };

	public static final String[] LONG_SCRIPT = {
    "COMMON", // 	COMMON -- NOT A LETTER: NO EXACT CORRESPONDENCE IN 15924
    "LATIN", // 	LATIN
    "GREEK", // 	GREEK
    "CYRILLIC", // 	CYRILLIC
    "ARMENIAN", // 	ARMENIAN
    "HEBREW", // 	HEBREW
    "ARABIC", // 	ARABIC
    "SYRIAC", // 	SYRIAC
    "THAANA", // 	THAANA
    "DEVANAGARI", // 	DEVANAGARI
    "BENGALI", // 	BENGALI
    "GURMUKHI", // 	GURMUKHI
    "GUJARATI", // 	GUJARATI
    "ORIYA", // 	ORIYA
    "TAMIL", // 	TAMIL
    "TELUGU", // 	TELUGU
    "KANNADA", // 	KANNADA
    "MALAYALAM", // 	MALAYALAM
    "SINHALA", // 	SINHALA
    "THAI", // 	THAI
    "LAO", // 	LAO
    "TIBETAN", // 	TIBETAN
    "MYANMAR", // 	MYANMAR
    "GEORGIAN", // 	GEORGIAN
    UnicodeProperty.UNUSED, // 	JAMO -- NOT SEPARATED FROM HANGUL IN 15924
    "HANGUL", // 	HANGUL
    "ETHIOPIC", // 	ETHIOPIC
    "CHEROKEE", // 	CHEROKEE
    "CANADIAN-ABORIGINAL", // 	ABORIGINAL
    "OGHAM", // 	OGHAM
    "RUNIC", // 	RUNIC
    "KHMER", // 	KHMER
    "MONGOLIAN", // 	MONGOLIAN
    "HIRAGANA", // 	HIRAGANA
    "KATAKANA", // 	KATAKANA
    "BOPOMOFO", // 	BOPOMOFO
    "HAN", // 	HAN
    "YI", // 	YI
    "OLD_ITALIC",
    "GOTHIC",
    "DESERET",
    "INHERITED",    // nonspacing marks
    "TAGALOG",
    "HANUNOO",
    "BUHID",
    "TAGBANWA",
    "LIMBU",
    "TAI_LE",
    "LINEAR_B",
    "UGARITIC",
    "SHAVIAN",
    "OSMANYA",
    "CYPRIOT", 
    "BRAILLE",
    "KATAKANA_OR_HIRAGANA",
    "BUGINESE",
    "COPTIC",
    "NEW_TAI_LUE",
    "GLAGOLITIC",
    "TIFINAGH",
    "SYLOTI_NAGRI",
    "OLD_PERSIAN",
    "KHAROSHTHI",
    "Balinese",
    "Cuneiform",
    "Phoenician",
    "Phags-pa",
    "Nko",
    "Unknown"
  };

	public static final Map EXTRA_SCRIPT = new HashMap();
	static {
		EXTRA_SCRIPT.put("Coptic", "Qaac");
	}

	public static final String[] SCRIPT = {
    "Zyyy", // 	COMMON -- NOT A LETTER: NO EXACT CORRESPONDENCE IN 15924
    "Latn", // 	LATIN
    "Grek", // 	GREEK
    "Cyrl", // 	CYRILLIC
    "Armn", // 	ARMENIAN
    "Hebr", // 	HEBREW
    "Arab", // 	ARABIC
    "Syrc", // 	SYRIAC
    "Thaa", // 	THAANA
    "Deva", // 	DEVANAGARI
    "Beng", // 	BENGALI
    "Guru", // 	GURMUKHI
    "Gujr", // 	GUJARATI
    "Orya", // 	ORIYA
    "Taml", // 	TAMIL
    "Telu", // 	TELUGU
    "Knda", // 	KANNADA
    "Mlym", // 	MALAYALAM
    "Sinh", // 	SINHALA
    "Thai", // 	THAI
    "Laoo", // 	LAO
    "Tibt", // 	TIBETAN
    "Mymr", // 	MYANMAR
    "Geor", // 	GEORGIAN
    UnicodeProperty.UNUSED, // 	JAMO -- NOT SEPARATED FROM HANGUL IN 15924
    "Hang", // 	HANGUL
    "Ethi", // 	ETHIOPIC
    "Cher", // 	CHEROKEE
    "Cans", // 	ABORIGINAL
    "Ogam", // 	OGHAM
    "Runr", // 	RUNIC
    "Khmr", // 	KHMER
    "Mong", // 	MONGOLIAN
    "Hira", // 	HIRAGANA
    "Kana", // 	KATAKANA
    "Bopo", // 	BOPOMOFO
    "Hani", // 	HAN
    "Yiii", // 	YI
    "Ital",
    "Goth",
    "Dsrt",
    "Qaai",
    "Tglg",
    "Hano",
    "Buhd",
    "Tagb",
    /*
    "LIMBU",
    "TAI_LE",
    "LINEAR_B",
    "UGARITIC",
    "SHAVIAN",
    "OSMANYA",
    "CYPRIOT", 
    */
    "Limb",
    "Tale",
    "Linb",
    "Ugar",
    "Shaw",
    "Osma",
    "Cprt",
    "Brai",
    "Hrkt",
    "Bugi",
    "Copt",
    "Talu",
    "Glag",
    "Tfng",
    "Sylo",
    "Xpeo",
    "Khar",
    "Bali",
    "Xsux",
    "Phnx",
    "Phag",
    "Nkoo",
    "Zzzz"
  };

  static final String[] AGE = {
    "unassigned",
    "1.1",
    "2.0",
    "2.1",
    "3.0",
    "3.1",
    "3.2",
    "4.0",
	"4.1",
	"5.0",
  };

    static final String[] GENERAL_CATEGORY = {
        "Cn", // = Other, Not Assigned 0

        "Lu", // = Letter, Uppercase 1
        "Ll", // = Letter, Lowercase 2
        "Lt", // = Letter, Titlecase 3
        "Lm", // = Letter, Modifier 4
        "Lo", // = Letter, Other 5

        "Mn", // = Mark, Non-Spacing 6
        "Me", // = Mark, Enclosing 8
        "Mc", // = Mark, Spacing Combining 7

        "Nd", // = Number, Decimal Digit 9
        "Nl", // = Number, Letter 10
        "No", // = Number, Other 11

        "Zs", // = Separator, Space 12
        "Zl", // = Separator, Line 13
        "Zp", // = Separator, Paragraph 14

        "Cc", // = Other, Control 15
        "Cf", // = Other, Format 16
        UnicodeProperty.UNUSED, // missing
        "Co", // = Other, Private Use 18
        "Cs", // = Other, Surrogate 19


        "Pd", // = Punctuation, Dash 20
        "Ps", // = Punctuation, Open 21
        "Pe", // = Punctuation, Close 22
        "Pc", // = Punctuation, Connector 23
        "Po", // = Punctuation, Other 24

        "Sm", // = Symbol, Math 25
        "Sc", // = Symbol, Currency 26
        "Sk", // = Symbol, Modifier 27
        "So", // = Symbol, Other 28

        "Pi", // = Punctuation, Initial quote 29 (may behave like Ps or Pe depending on usage)
        "Pf" // = Punctuation, Final quote 30 (may behave like Ps or Pe dependingon usage)
    };
    
    static final String[] LONG_GENERAL_CATEGORY = {
        "Unassigned", // = Other, Not Assigned 0

        "UppercaseLetter", // = Letter, Uppercase 1
        "LowercaseLetter", // = Letter, Lowercase 2
        "TitlecaseLetter", // = Letter, Titlecase 3
        "ModifierLetter", // = Letter, Modifier 4
        "OtherLetter", // = Letter, Other 5

        "NonspacingMark", // = Mark, Non-Spacing 6
        "EnclosingMark", // = Mark, Enclosing 8
        "SpacingMark", // = Mark, Spacing Combining 7

        "DecimalNumber", // = Number, Decimal Digit 9
        "LetterNumber", // = Number, Letter 10
        "OtherNumber", // = Number, Other 11

        "SpaceSeparator", // = Separator, Space 12
        "LineSeparator", // = Separator, Line 13
        "ParagraphSeparator", // = Separator, Paragraph 14

        "Control", // = Other, Control 15
        "Format", // = Other, Format 16
        UnicodeProperty.UNUSED, // missing
        "PrivateUse", // = Other, Private Use 18
        "Surrogate", // = Other, Surrogate 19


        "DashPunctuation", // = Punctuation, Dash 20
        "OpenPunctuation", // = Punctuation, Open 21
        "ClosePunctuation", // = Punctuation, Close 22
        "ConnectorPunctuation", // = Punctuation, Connector 23
        "OtherPunctuation", // = Punctuation, Other 24

        "MathSymbol", // = Symbol, Math 25
        "CurrencySymbol", // = Symbol, Currency 26
        "ModifierSymbol", // = Symbol, Modifier 27
        "OtherSymbol", // = Symbol, Other 28

        "InitialPunctuation", // = Punctuation, Initial quote 29 (may behave like Ps or Pe depending on usage)
        "FinalPunctuation" // = Punctuation, Final quote 30 (may behave like Ps or Pe dependingon usage)
    };

    static final String[][] SUPER_CATEGORIES = {
        {"L", "Letter", null, "Ll | Lm | Lo | Lt | Lu"},
        {"M", "Mark", null, "Mc | Me | Mn"},
        {"N", "Number", null, "Nd | Nl | No"},
        {"Z", "Separator", null, "Zl | Zp | Zs"},
        {"C", "Other", "cntrl", "Cc | Cf | Cn | Co | Cs"},
        {"S", "Symbol", null, "Sc | Sk | Sm | So"},
        {"P", "Punctuation", "punct", "Pc | Pd | Pe | Pf | Pi | Po | Ps"},
        {"LC", "Cased Letter", null, "Ll | Lt | Lu"},
    };

	public static final Map EXTRA_GENERAL_CATEGORY = new HashMap();
	static {
		EXTRA_GENERAL_CATEGORY.put("Decimal_Number", "digit");
		EXTRA_GENERAL_CATEGORY.put("Control", "cntrl");
	}



    static final String[] BIDI_CLASS = {
        "L", //	Left-Right; Most alphabetic, syllabic, and logographic characters (e.g., CJK ideographs)
        "R", //	Right-Left; Arabic, Hebrew, and punctuation specific to those scripts
        "EN", //	European Number
        "ES", //	European Number Separator
        "ET", //	European Number Terminator
        "AN", //	Arabic Number
        "CS", //	Common Number Separator
        "B", //	Paragraph Separator
        "S", //	Segment Separator
        "WS", //	Whitespace
        "ON", //	Other Neutrals ; All other characters: punctuation, symbols
        UnicodeProperty.UNUSED, "BN", "NSM", "AL", "LRO", "RLO", "LRE", "RLE", "PDF"
    };

    static String[] LONG_BIDI_CLASS = {
        "LeftToRight", //	Left-Right; Most alphabetic, syllabic, and logographic characters (e.g., CJK ideographs)
        "RightToLeft", //	Right-Left; Arabic, Hebrew, and punctuation specific to those scripts
        "EuropeanNumber", //	European Number
        "EuropeanSeparator", //	European Number Separator
        "EuropeanTerminator", //	European Number Terminator
        "ArabicNumber", //	Arabic Number
        "CommonSeparator", //	Common Number Separator
        "ParagraphSeparator", //	Paragraph Separator
        "SegmentSeparator", //	Segment Separator
        "WhiteSpace", //	Whitespace
        "OtherNeutral", //	Other Neutrals ; All other characters: punctuation, symbols
        UnicodeProperty.UNUSED,
        "BoundaryNeutral", "NonspacingMark", "ArabicLetter",
        "LeftToRightOverride",
        "RightToLeftOverride", "LeftToRightEmbedding",
        "RightToLeftEmbedding", "PopDirectionalFormat"
    };

    private static String[] CASE_TABLE = {
        "LOWER", "TITLE", "UPPER", "UNCASED"
    };

    static String[] LONG_DECOMPOSITION_TYPE = {
        "none", // NONE
        "canonical", // CANONICAL
        "compat",	// Otherwise unspecified compatibility character.
        "font",		// A font variant (e.g. a blackletter form).
        "noBreak",	// A no-break version of a space or hyphen.
        "initial",	// // An initial presentation form (Arabic).
        "medial",	// // A medial presentation form (Arabic).
        "final",	// // 	A final presentation form (Arabic).
        "isolated",	// An isolated presentation form (Arabic).
        "circle",	// An encircled form.
        "super",	// 	A superscript form.
        "sub",	// 	A subscript form.
        "vertical",	// A vertical layout presentation form.
        "wide",	// 	A wide (or zenkaku) compatibility character.
        "narrow",	// A narrow (or hankaku) compatibility character.
        "small",	// 	A small variant form (CNS compatibility).
        "square",	// A CJK squared font variant.
        "fraction",	// A vulgar fraction form.
    };
    static String[] DECOMPOSITION_TYPE = {
        "none", // NONE
        "can", // CANONICAL
        "com",	// Otherwise unspecified compatibility character.
        "font",		// A font variant (e.g. a blackletter form).
        "nb",	// A no-break version of a space or hyphen.
        "init",	// // An initial presentation form (Arabic).
        "med",	// // A medial presentation form (Arabic).
        "fin",	// // 	A final presentation form (Arabic).
        "iso",	// An isolated presentation form (Arabic).
        "enc",	// An encircled form.
        "sup",	// 	A superscript form.
        "sub",	// 	A subscript form.
        "vert",	// A vertical layout presentation form.
        "wide",	// 	A wide (or zenkaku) compatibility character.
        "nar",	// A narrow (or hankaku) compatibility character.
        "sml",	// 	A small variant form (CNS compatibility).
        "sqr",	// A CJK squared font variant.
        "fra",	// A vulgar fraction form.
    };
    static {
        fixArray(LONG_DECOMPOSITION_TYPE);
        //fixArray(DECOMPOSITION_TYPE);
    }


    static private String[] MIRRORED_TABLE = {
        "N",
        "Y"
    };

    static String[] LONG_NUMERIC_TYPE = {
        "none",
        "numeric",
        "digit",
        "decimal",
        /*
        "Han_Primary",
        "Han_Accounting",
        "Han_Other"
        */
    };

    static String[] NUMERIC_TYPE = {
        "none",
        "nu",
        "di",
        "de",
        /*
        "hp",
        "ha",
        "ho"
        */
    };
    static {
        fixArray(LONG_NUMERIC_TYPE);
        fixArray(NUMERIC_TYPE);
    }

    static String[] COMBINING_CLASS = new String[256];
    static String[] LONG_COMBINING_CLASS = new String[256];
    // TODO clean this up, just a quick copy of code
    static {
        for (int style = SHORT; style <= LONG; ++style)
        for (int index = 0; index < 256; ++index) {
            String s = null;
            switch (index) {
                case 0: s = style < LONG ? "NR" : "NotReordered"; break;
                case 1: s = style < LONG ? "OV" :  "Overlay"; break;
                case 7: s = style < LONG ? "NK" :  "Nukta"; break;
                case 8: s = style < LONG ? "KV" :  "KanaVoicing"; break;
                case 9: s = style < LONG ? "VR" :  "Virama"; break;
                case 200: s = style < LONG ? "ATBL" :  "AttachedBelowLeft"; break;
                case 202: s = style < LONG ? "ATB" :  "AttachedBelow"; break;
                /*
                case 204: s = style < LONG ? "ATBR" :  "AttachedBelowRight"; break;
                case 208: s = style < LONG ? "ATL" :  "AttachedLeft"; break;
                case 210: s = style < LONG ? "ATR" :  "AttachedRight"; break;
                case 212: s = style < LONG ? "ATAL" :  "AttachedAboveLeft"; break;
                case 214: s = style < LONG ? "ATA" :  "AttachedAbove"; break;                
                */
                case 216: s = style < LONG ? "ATAR" :   "AttachedAboveRight"; break;
                case 218: s = style < LONG ? "BL" :   "BelowLeft"; break;
                case 220: s = style < LONG ? "B" :   "Below"; break;
                case 222: s = style < LONG ? "BR" :   "BelowRight"; break;
                case 224: s = style < LONG ? "L" :   "Left"; break;
                case 226: s = style < LONG ? "R" :   "Right"; break;
                case 228: s = style < LONG ? "AL" :   "AboveLeft"; break;
                case 230: s = style < LONG ? "A" :   "Above"; break;
                case 232: s = style < LONG ? "AR" :   "AboveRight"; break;
                case 233: s = style < LONG ? "DB" :   "DoubleBelow"; break;
                case 234: s = style < LONG ? "DA" :   "DoubleAbove"; break;
                case 240: s = style < LONG ? "IS" :   "IotaSubscript"; break;
                default: s = "" + index;
            }
            if (style < LONG) COMBINING_CLASS[index] = s;
            else LONG_COMBINING_CLASS[index] = s;
        }
        if (false) for (int i = 0; i < 256; ++i) {
            System.out.println(i 
                + "\t" + COMBINING_CLASS[i]
                 + "\t" + LONG_COMBINING_CLASS[i]);
        }
    }
    
    static {
        if (LIMIT_CATEGORY != GENERAL_CATEGORY.length || LIMIT_CATEGORY != LONG_GENERAL_CATEGORY.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: category");
        }
        if (LIMIT_BIDI_CLASS != BIDI_CLASS.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: bidi");
        }
        if (LIMIT_LINE_BREAK != LINE_BREAK.length || LIMIT_LINE_BREAK != LONG_LINE_BREAK.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: linebreak");
        }
        if (LIMIT_DECOMPOSITION_TYPE != LONG_DECOMPOSITION_TYPE.length || LIMIT_DECOMPOSITION_TYPE != DECOMPOSITION_TYPE.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: decomp type");
        }
        if (LIMIT_MIRRORED != MIRRORED_TABLE.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: compat type");
        }
        if (LIMIT_CASE != CASE_TABLE.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: case");
        }
        if (LIMIT_NUMERIC_TYPE != LONG_NUMERIC_TYPE.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: numeric type");
        }
        if (LIMIT_EAST_ASIAN_WIDTH != LONG_EAST_ASIAN_WIDTH.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: east Asian Width");
        }
        if (LIMIT_BINARY_PROPERTIES != BP.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: binary properties");
        }
        if (LIMIT_SCRIPT != LONG_SCRIPT.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: script");
        }
        if (LIMIT_AGE != AGE.length) {
            System.err.println("!! ERROR !! Enums and Names out of sync: age");
        }
    }

    public static byte ON = Utility.lookup("ON", BIDI_CLASS, true);

    public static String[] HANGUL_SYLLABLE_TYPE = {
        "NA",
        "L",
        "V",
        "T",
        "LV",
        "LVT",
    };

    public static String[] LONG_HANGUL_SYLLABLE_TYPE = {
        "Not_Applicable",
        "Leading_Jamo",
        "Vowel_Jamo",
        "Trailing_Jamo",
        "LV_Syllable",
        "LVT_Syllable",
    };

    public static String[] JOINING_TYPE = {
        "C",
        "D",
        "R",
        "U",
        "L",
        "T"
    };

    public static String[] LONG_JOINING_TYPE = {
        "JoinCausing",
        "DualJoining",
        "RightJoining",
        "NonJoining",
        "LeftJoining",
        "Transparent"
    };

    public static String[] JOINING_GROUP = {
        "NO_JOINING_GROUP",
        "AIN",
        "ALAPH",
        "ALEF",
        "BEH",
        "BETH",
        "DAL",
        "DALATH_RISH",
        "E",
        "FEH",
        "FINAL_SEMKATH",
        "GAF",
        "GAMAL",
        "HAH",
        "HAMZA_ON_HEH_GOAL",
        "HE",
        "HEH",
        "HEH_GOAL",
        "HETH",
        "KAF",
        "KAPH",
        "KNOTTED_HEH",
        "LAM",
        "LAMADH",
        "MEEM",
        "MIM",
        "NOON",
        "NUN",
        "PE",
        "QAF",
        "QAPH",
        "REH",
        "REVERSED_PE",
        "SAD",
        "SADHE",
        "SEEN",
        "SEMKATH",
        "SHIN",
        "SWASH_KAF",
        "TAH",
        "TAW",
        "TEH_MARBUTA",
        "TETH",
        "WAW",
        "SYRIAC WAW",
        "YEH",
        "YEH_BARREE",
        "YEH_WITH_TAIL",
        "YUDH",
        "YUDH_HE",
        "ZAIN",
        "ZHAIN",
        "KHAPH",
        "FE",
    };
    static {
        fixArray(JOINING_GROUP);
    }
    static void fixArray (String[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = Utility.getUnskeleton(
                array[i].toLowerCase(Locale.ENGLISH),
                true);
        }
    }

    static void titlecase (String[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = array[1].substring(0,1).toUpperCase()
                 + array[i].substring(1);
        }
    }

    public static String[] OLD_JOINING_GROUP = {
            "<no shaping>",
            "AIN",
            "ALAPH",
            "ALEF",
            "BAA",
            "BETH",
            "DAL",
            "DALATH_RISH",
            "E",
            "FA",
            "FINAL_SEMKATH",
            "GAF",
            "GAMAL",
            "HAA",
            "HAMZAH_ON_HA_GOAL",
            "HE",
            "HA",
            "HA_GOAL",
            "HETH",
            "CAF",
            "KAPH",
            "KNOTTED_HA",
            "LAM",
            "LAMADH",
            "MEEM",
            "MIM",
            "NOON",
            "NUN",
            "PE",
            "QAF",
            "QAPH",
            "RA",
            "REVERSED_PE",
            "SAD",
            "SADHE",
            "SEEN",
            "SEMKATH",
            "SHIN",
            "SWASH_CAF",
            "TAH",
            "TAW",
            "TAA_MARBUTAH",
            "TETH",
            "WAW",
            "SYRIAC WAW",
            "YA",
            "YA_BARREE",
            "ALEF_MAQSURAH",
            "YUDH",
            "YUDH_HE",
            "ZAIN",
            "ZHAIN",
            "KHAPH",
            "FE",
        };

    static String[] JAMO_L_TABLE = {
                // Value;  Short Name; Unicode Name
        "G",    // U+1100; G; HANGUL CHOSEONG KIYEOK
        "GG",   // U+1101; GG; HANGUL CHOSEONG SSANGKIYEOK
        "N",    // U+1102; N; HANGUL CHOSEONG NIEUN
        "D",    // U+1103; D; HANGUL CHOSEONG TIKEUT
        "DD",   // U+1104; DD; HANGUL CHOSEONG SSANGTIKEUT
        "R",    // U+1105; L; HANGUL CHOSEONG RIEUL
        "M",    // U+1106; M; HANGUL CHOSEONG MIEUM
        "B",    // U+1107; B; HANGUL CHOSEONG PIEUP
        "BB",   // U+1108; BB; HANGUL CHOSEONG SSANGPIEUP
        "S",    // U+1109; S; HANGUL CHOSEONG SIOS
        "SS",   // U+110A; SS; HANGUL CHOSEONG SSANGSIOS
        "",     // U+110B; ; HANGUL CHOSEONG IEUNG
        "J",    // U+110C; J; HANGUL CHOSEONG CIEUC
        "JJ",   // U+110D; JJ; HANGUL CHOSEONG SSANGCIEUC
        "C",    // U+110E; C; HANGUL CHOSEONG CHIEUCH
        "K",    // U+110F; K; HANGUL CHOSEONG KHIEUKH
        "T",    // U+1110; T; HANGUL CHOSEONG THIEUTH
        "P",    // U+1111; P; HANGUL CHOSEONG PHIEUPH
        "H"     // U+1112; H; HANGUL CHOSEONG HIEUH
    };

    static String[] JAMO_V_TABLE = {
                // Value;  Short Name; Unicode Name
        "A",    // U+1161; A; HANGUL JUNGSEONG A
        "AE",   // U+1162; AE; HANGUL JUNGSEONG AE
        "YA",   // U+1163; YA; HANGUL JUNGSEONG YA
        "YAE",  // U+1164; YAE; HANGUL JUNGSEONG YAE
        "EO",   // U+1165; EO; HANGUL JUNGSEONG EO
        "E",    // U+1166; E; HANGUL JUNGSEONG E
        "YEO",  // U+1167; YEO; HANGUL JUNGSEONG YEO
        "YE",   // U+1168; YE; HANGUL JUNGSEONG YE
        "O",    // U+1169; O; HANGUL JUNGSEONG O
        "WA",   // U+116A; WA; HANGUL JUNGSEONG WA
        "WAE",  // U+116B; WAE; HANGUL JUNGSEONG WAE
        "OE",   // U+116C; OE; HANGUL JUNGSEONG OE
        "YO",   // U+116D; YO; HANGUL JUNGSEONG YO
        "U",    // U+116E; U; HANGUL JUNGSEONG U
        "WEO",  // U+116F; WEO; HANGUL JUNGSEONG WEO
        "WE",   // U+1170; WE; HANGUL JUNGSEONG WE
        "WI",   // U+1171; WI; HANGUL JUNGSEONG WI
        "YU",   // U+1172; YU; HANGUL JUNGSEONG YU
        "EU",   // U+1173; EU; HANGUL JUNGSEONG EU
        "YI",   // U+1174; YI; HANGUL JUNGSEONG YI
        "I",    // U+1175; I; HANGUL JUNGSEONG I
    };

    static String[] JAMO_T_TABLE = {
                // Value;  Short Name; Unicode Name
        "",     // filler, for LV syllable
        "G",    // U+11A8; G; HANGUL JONGSEONG KIYEOK
        "GG",   // U+11A9; GG; HANGUL JONGSEONG SSANGKIYEOK
        "GS",   // U+11AA; GS; HANGUL JONGSEONG KIYEOK-SIOS
        "N",    // U+11AB; N; HANGUL JONGSEONG NIEUN
        "NJ",   // U+11AC; NJ; HANGUL JONGSEONG NIEUN-CIEUC
        "NH",   // U+11AD; NH; HANGUL JONGSEONG NIEUN-HIEUH
        "D",    // U+11AE; D; HANGUL JONGSEONG TIKEUT
        "L",    // U+11AF; L; HANGUL JONGSEONG RIEUL
        "LG",   // U+11B0; LG; HANGUL JONGSEONG RIEUL-KIYEOK
        "LM",   // U+11B1; LM; HANGUL JONGSEONG RIEUL-MIEUM
        "LB",   // U+11B2; LB; HANGUL JONGSEONG RIEUL-PIEUP
        "LS",   // U+11B3; LS; HANGUL JONGSEONG RIEUL-SIOS
        "LT",   // U+11B4; LT; HANGUL JONGSEONG RIEUL-THIEUTH
        "LP",   // U+11B5; LP; HANGUL JONGSEONG RIEUL-PHIEUPH
        "LH",   // U+11B6; LH; HANGUL JONGSEONG RIEUL-HIEUH
        "M",    // U+11B7; M; HANGUL JONGSEONG MIEUM
        "B",    // U+11B8; B; HANGUL JONGSEONG PIEUP
        "BS",   // U+11B9; BS; HANGUL JONGSEONG PIEUP-SIOS
        "S",    // U+11BA; S; HANGUL JONGSEONG SIOS
        "SS",   // U+11BB; SS; HANGUL JONGSEONG SSANGSIOS
        "NG",   // U+11BC; NG; HANGUL JONGSEONG IEUNG
        "J",    // U+11BD; J; HANGUL JONGSEONG CIEUC
        "C",    // U+11BE; C; HANGUL JONGSEONG CHIEUCH
        "K",    // U+11BF; K; HANGUL JONGSEONG KHIEUKH
        "T",    // U+11C0; T; HANGUL JONGSEONG THIEUTH
        "P",    // U+11C1; P; HANGUL JONGSEONG PHIEUPH
        "H",    // U+11C2; H; HANGUL JONGSEONG HIEUH
    };
    
    static final String[] NF_NAME = {"NFD", "NFC", "NFKD", "NFKC"};
    
    static final String[][] NAME_ABBREVIATIONS = {
        {"CJK UNIFIED IDEOGRAPH-", "CJK-"},
        {"CJK COMPATIBILITY IDEOGRAPH-", "CJKC-"},
        {"IDEOGRAPHIC TELEGRAPH SYMBOL FOR", "ITSF."},
        
        {"BRAILLE PATTERN DOTS-", "BPD-"},
        {"CANADIAN SYLLABICS WEST-", "CSW."},
        /*{"LATIN SMALL LETTER", "LSL."},
        {"LATIN CAPITAL LETTER", "LCL."},
        {"GREEK SMALL LETTER", "GSL."},
        {"GREEK CAPITAL LETTER", "GCL."},
        {"CYRILLIC SMALL LETTER", "GSL."},
        {"CYRILLIC CAPITAL LETTER", "GCL."},
        {"BYZANTINE MUSICAL SYMBOL", "BMS."},
        {"YI SYLLABLE", "YS."},
        {"ETHIOPIC SYLLABLE", "ES."},
        {"HANGUL SYLLABLE", "HS."},
        {"CANADIAN SYLLABICS", "CS."},
        {"ARABIC LETTER", "ALt."},
        {"ARABIC LIGATURE", "AL."},
        */
        
        {"MATHEMATICAL SANS-SERIF", "MSS."},
        {"MATHEMATICAL SERIF", "MS."},
        {"BOLD ITALIC", "BI."},
        {"ISOLATED FORM", "IF."},
        {"FINAL FORM", "FF."},
        {"INITIAL FORM", "IF."},
        {"VOWEL SIGN", "VS."},
        {"KANGXI RADICAL", "KR."},
        {"MUSICAL SYMBOL", "MS."},
        {"SMALL LETTER", "SL."},
        {"CAPITAL LETTER", "CL."},
        
        {"LIGATURE", "Lg."},
        {"SYLLABICS", "Ss."},
        {"MATHEMATICAL", "M."},
        {"LETTER", "L."},
        {"SYLLABLE", "S."},
        {"SYMBOL", "Sy."},
        {"WITH", "W."},
        {"CAPITAL", "C."},
        {"SMALL", "C."},
        {"COMBINING", "Cm."},
        {"HANGUL", "H."},
    };
    
    static final String[][] PROP_TYPE_NAMES = {
        {"Numeric", "AA"},
        {"String", "AB"},
        {"Miscellaneous", "AC"},
        {"Catalog", "AD"},
        {"Enumerated", "AE"},
        {"Binary", "ZX"},
        {"Flattened Binary", "ZY"},
        {"Unknown", "ZZ"}
    };
    

    /*
LETTER: 23598
MATHEMATICAL:   11976
SYLLABLE:       11872
CAPITAL:        8918
WITH:   8008
COMPATIBILITY:  7800
SMALL:  7740
IDEOGRAPH:      6165
SYLLABICS:      5670
ARABIC: 5646
CANADIAN:       5040
LATIN:  4840
SYMBOL: 4626
LIGATURE:       4048
MUSICAL:        3255
FORM:   3044
ETHIOPIC:       2760
RADICAL:        2695
HANGUL: 2670
ITALIC: 2526
YI:     2468
BOLD:   2256
BYZANTINE:      2214

COMPATIBILITY/IDEOGRAPH:        13800
YI/SYLLABLE:    12815
CANADIAN/SYLLABICS:     11340
CAPITAL/LETTER: 10948
SMALL/LETTER:   10692
CJK/COMPATIBILITY:      10200
ARABIC/LIGATURE:        7110
IDEOGRAPH/-:    6600
MUSICAL/SYMBOL: 6510
MATHEMATICAL/SANS:      5848
LATIN/SMALL:    5786
MATHEMATICAL/BOLD:      5678
ETHIOPIC/SYLLABLE:      5389
LATIN/CAPITAL:  5330
ARABIC/LETTER:  4992
BYZANTINE/MUSICAL:      4182
BRAILLE/PATTERN:        3825
ISOLATED/FORM:  3068
PATTERN/DOTS:   3060
KANGXI/RADICAL: 2996
SYLLABICS/CARRIER:      2975
-/SERIF:        2576
ITALIC/CAPITAL: 2520
BOLD/ITALIC:    2420
KATAKANA/LETTER:        2415
FINAL/FORM:     2400
SERIF/BOLD:     2300
SANS/-: 2208
ITALIC/SMALL:   2184
MONGOLIAN/LETTER:       2080
MATHEMATICAL/ITALIC:    2071
INITIAL/FORM:   2064
CYRILLIC/CAPITAL:       2032

CJK/COMPATIBILITY/IDEOGRAPH:    16200
COMPATIBILITY/IDEOGRAPH/-:      15000
LATIN/SMALL/LETTER:     9306
LATIN/CAPITAL/LETTER:   8160
MATHEMATICAL/SANS/-:    6536
BYZANTINE/MUSICAL/SYMBOL:       5904
BRAILLE/PATTERN/DOTS:   5100
CANADIAN/SYLLABICS/CARRIER:     4550
SANS/-/SERIF:   4416
PATTERN/DOTS/-: 3570
GREEK/SMALL/LETTER:     2934
CYRILLIC/CAPITAL/LETTER:        2852
-/SERIF/BOLD:   2760
MATHEMATICAL/BOLD/ITALIC:       2640
CYRILLIC/SMALL/LETTER:  2604
GREEK/CAPITAL/LETTER:   2580

CJK/COMPATIBILITY/IDEOGRAPH/-:  17400
MATHEMATICAL/SANS/-/SERIF:      8600
BRAILLE/PATTERN/DOTS/-: 5610
SANS/-/SERIF/BOLD:      3910
CANADIAN/SYLLABICS/WEST/-:      2200
IDEOGRAPHIC/TELEGRAPH/SYMBOL/FOR:       2176
-/SERIF/BOLD/ITALIC:    2090
    */
    

/*
    static {
        UNASSIGNED_INFO.code = '\uFFFF';
        UNASSIGNED_INFO.name = "<reserved>";
        UNASSIGNED_INFO.decomposition = "";
        UNASSIGNED_INFO.fullCanonicalDecomposition = "";
        UNASSIGNED_INFO.fullCompatibilityDecomposition = "";
        UNASSIGNED_INFO.name10 = "";
        UNASSIGNED_INFO.comment = "";

        UNASSIGNED_INFO.numericType = NONE;
        UNASSIGNED_INFO.decompositionType = NONE;

        UNASSIGNED_INFO.category = lookup("Cn",CATEGORY_TABLE, "PROXY");
        UNASSIGNED_INFO.canonical = 0;

        UNASSIGNED_INFO.uppercase = "";
        UNASSIGNED_INFO.lowercase = "";
        UNASSIGNED_INFO.titlecase = "";

        UNASSIGNED_INFO.bidi = ON;

        UNASSIGNED_INFO.mirrored = NO;
    }
        */
}