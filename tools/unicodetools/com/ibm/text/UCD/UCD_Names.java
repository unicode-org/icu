package com.ibm.text.UCD;

import com.ibm.text.utility.*;


final class UCD_Names implements UCD_Types {
    
    static final String[] UNIFIED_PROPERTIES = {
        "General Category (listing UnicodeData.txt, field 2: see UnicodeData.html)",
        "Combining Class (listing UnicodeData.txt, field 3: see UnicodeData.html)",
        "Bidi Class (listing UnicodeData.txt, field 4: see UnicodeData.html)",
        "Decomposition Type (from UnicodeData.txt, field 5: see UnicodeData.html)",
        "Numeric Type (from UnicodeData.txt, field 6/7/8: see UnicodeData.html)", 
        "East Asian Width (listing EastAsianWidth.txt, field 1)",
        "Line Break (listing LineBreak.txt, field 1)",
        "Joining Type (listing ArabicShaping.txt, field 1).\r\n"
            + "#\tType T is derived from Mn + Cf - ZWNJ - ZWJ\r\n"
            + "#\tAll other code points have the type U",
        "Joining Group (listing ArabicShaping.txt, field 2)",
        "BidiMirrored (listing UnicodeData.txt, field 9: see UnicodeData.html)",
        "Script",
        "Age (from a comparison of UCD versions 1.1 [minus Hangul], 2.0, 2.1, 3.0, 3.1)"
    };
    
    static final String[] SHORT_UNIFIED_PROPERTIES = {
        "GeneralCategory",
        "CombiningClass",
        "BidiClass",
        "DecompositionType",
        "NumericType", 
        "EastAsianWidth",
        "LineBreak",
        "JoiningType",
        "JoiningGroup",
        "Value",
        "Script",
        "Age"
    };
    
    static final String[] ABB_UNIFIED_PROPERTIES = {
        "gc",
        "cc",
        "bc",
        "dt",
        "nt", 
        "ea",
        "lb",
        "jt",
        "jg",
        "va",
        "sc",
        "Ag"
    };
    
    
    static final String[] BP = {
	    "BidiMirrored",
	    "CompositionExclusion", 
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
        "CaseFoldTurkishI",
        "Other_GraphemeExtend",
        "GraphemeLink",
        "IDS_BinaryOperator",
        "IDS_TrinaryOperator",
        "Radical",
        "UnifiedIdeograph",
        "Reserved_Cf_Code_Point",
        "Deprecated",
    };
    
    static final String[] SHORT_BP = {
	    "BidiM",
	    "CExc", 
        "WhSp",
        "NBrk",
	    "BdCon",
        "JCon",
        "Dash",
        "Hyph",
        "QMark",
        "TPunc",
        "OMath",
        "HexD",
        "AHexD",
	    "OAlph",
        "Ideo",
        "Diac",
        "Ext",
        "OLoc",
        "OUpc",
        "NChar",
        "TurkI",
        "OGrX",
        "GrLink",
        "IDSB",
        "IDST",
        "Radical",
        "UCJK",
        "RCf",
        "Dep",
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
    
    static final String[] YN_TABLE = {"N", "Y"};
    
    static String[] EA = {
        "N", "A", "H", "W", "F", "Na"
    };        
        
    static String[] SHORT_EA = {
        "Neutral", "Ambiguous", "Halfwidth", "Wide", "Fullwidth", "Narrow"
    };        
        
    static final String[] LB = {
        "XX", "OP", "CL", "QU", "GL", "NS", "EX", "SY",
        "IS", "PR", "PO", "NU", "AL", "ID", "IN", "HY",
        "CM", "BB", "BA", "SP", "BK", "CR", "LF", "CB",
        "SA", "AI", "B2", "SG", "ZW"
    };

    static final String[] LONG_LB = {
        "Unknown", "OpenPunctuation", "ClosePunctuation", "Quotation", 
        "Glue", "Nonstarter", "Exclamation", "BreakSymbols",
        "InfixNumeric", "PrefixNumeric", "PostfixNumeric", 
        "Numeric", "Alphabetic", "Ideographic", "Inseperable", "Hyphen",
        "CombiningMark", "BreakBefore", "BreakAfter", "Space", 
        "MandatoryBreak", "CarriageReturn", "LineFeed", "ContingentBreak",
        "ComplexContext", "Ambiguous", "BreakBeforeAndAfter", "Surrogate", "ZWSpace"
    };

	public static final String[] SCRIPT = {
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
    "<unused>", // 	JAMO -- NOT SEPARATED FROM HANGUL IN 15924
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
    "OLD-ITALIC",
    "GOTHIC",
    "DESERET",
    "INHERITED",
  };
  
	public static final String[] ABB_SCRIPT = {
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
    "<unused>", // 	JAMO -- NOT SEPARATED FROM HANGUL IN 15924
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
  };
  
  
  
  static final String[] AGE = {
    "UNSPECIFIED",
    "1.1",
    "2.0", "2.1",
    "3.0", "3.1"
  };
    
    
    static final String[] GC = {
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
        "<unused>", // missing
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
    
    static final String[] LONG_GC = {
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
        "<unused>", // missing
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

    

    static String[] BC = {
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
        "<unused>", "BN", "NSM", "AL", "LRO", "RLO", "LRE", "RLE", "PDF"
    };
        
    static String[] LONG_BC = {
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
        "<unused>", 
        "BoundaryNeutral", "NonspacingMark", "ArabicLetter", 
        "LeftToRightOverride", 
        "RightToLeftOverride", "LeftToRightEmbedding", 
        "RightToLeftEmbedding", "PopDirectionalFormat"
    };
        
    private static String[] CASE_TABLE = {
        "LOWER", "TITLE", "UPPER", "UNCASED"
    };

    static String[] DT = {
        "", // NONE
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
    
    static String[] SHORT_DT = {
        "", // NONE
        "ca", // CANONICAL
        "co",	// Otherwise unspecified compatibility character.
        "fo",		// A font variant (e.g. a blackletter form).
        "nb",	// A no-break version of a space or hyphen.
        "in",	// // An initial presentation form (Arabic).
        "me",	// // A medial presentation form (Arabic).
        "fi",	// // 	A final presentation form (Arabic).
        "is",	// An isolated presentation form (Arabic).
        "ci",	// An encircled form.
        "sp",	// 	A superscript form.
        "sb",	// 	A subscript form.
        "ve",	// A vertical layout presentation form.
        "wi",	// 	A wide (or zenkaku) compatibility character.
        "na",	// A narrow (or hankaku) compatibility character.
        "sm",	// 	A small variant form (CNS compatibility).
        "sq",	// A CJK squared font variant.
        "fr",	// A vulgar fraction form.
    };
    
    static private String[] MIRRORED_TABLE = {
        "N",
        "Y"
    };

    static String[] NT = {
        "",
        "numeric",
        "digit",
        "decimal",
    };
    
    static String[] SHORT_NT = {
        "",
        "nu",
        "di",
        "de",
    };
    
    static {
        if (LIMIT_CATEGORY != GC.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: category");
        }
        if (LIMIT_BIDI_CLASS != BC.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: bidi");
        }
        if (LIMIT_LINE_BREAK != LB.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: linebreak");
        }
        if (LIMIT_DECOMPOSITION_TYPE != DT.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: compat type");
        }
        if (MIRRORED_LIMIT != MIRRORED_TABLE.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: compat type");
        }
        if (MIRRORED_LIMIT != MIRRORED_TABLE.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: compat type");
        }
        if (CASE_LIMIT != CASE_TABLE.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: case");
        }
        if (LIMIT_NUMERIC_TYPE != NT.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: case");
        }
        if (LIMIT_EAST_ASIAN_WIDTH != EA.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: east Asian Width");
        }
        if (LIMIT_BINARY_PROPERTIES != BP.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: binary properties");
        }
        if (LIMIT_SCRIPT != SCRIPT.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: script");
        }
        if (LIMIT_AGE != AGE.length) {
            System.err.println("!! ERROR !! UnicodeTypes and UInfo out of sync: age");
        }
    }
    
    public static byte ON = Utility.lookup("ON", BC);
    
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
        "YEH",
        "YEH_BARREE",
        "YEH_WITH_TAIL",
        "YUDH",
        "YUDH_HE",
        "ZAIN",
    };
    
    public static String[] OLD_JOINING_GROUP = {
        "<no shaping>",
        "AIN",
        "ALAPH",
        "ALEF",
        "BEH",
        "BETH",
        "DAL",
        "DALATH RISH",
        "E",
        "FEH",
        "FINAL SEMKATH",
        "GAF",
        "GAMAL",
        "HAH",
        "HAMZA ON HEH GOAL",
        "HE",
        "HEH",
        "HEH GOAL",
        "HETH",
        "KAF",
        "KAPH",
        "KNOTTED HEH",
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
        "REVERSED PE",
        "SAD",
        "SADHE",
        "SEEN",
        "SEMKATH",
        "SHIN",
        "SWASH KAF",
        "TAH",
        "TAW",
        "TEH MARBUTA",
        "TETH",
        "WAW",
        "YEH",
        "YEH BARREE",
        "YEH WITH TAIL",
        "YUDH",
        "YUDH HE",
        "ZAIN",
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