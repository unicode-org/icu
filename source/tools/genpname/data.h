/**
 * Copyright (C) 2002-2005, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * MACHINE GENERATED FILE.  !!! Do not edit manually !!!
 *
 * Generated from
 *   uchar.h
 *   uscript.h
 *   Blocks.txt
 *   PropertyAliases.txt
 *   PropertyValueAliases.txt
 *
 * Date: Fri Mar 11 14:55:46 2005
 * Unicode version: 4.1.0
 * Script: preparse.pl
 */

/* Unicode version 4.1.0 */
const uint8_t VERSION_0 = 4;
const uint8_t VERSION_1 = 1;
const uint8_t VERSION_2 = 0;
const uint8_t VERSION_3 = 0;

const int32_t STRING_COUNT = 718;

/* to be sorted */
const AliasName STRING_TABLE[] = {
    AliasName("", 0),
    AliasName("A", 1),
    AliasName("AHex", 2),
    AliasName("AI", 3),
    AliasName("AL", 4),
    AliasName("ALetter", 5),
    AliasName("AN", 6),
    AliasName("AR", 7),
    AliasName("ASCII_Hex_Digit", 8),
    AliasName("ATAR", 9),
    AliasName("ATB", 10),
    AliasName("ATBL", 11),
    AliasName("ATerm", 12),
    AliasName("Above", 13),
    AliasName("Above_Left", 14),
    AliasName("Above_Right", 15),
    AliasName("Aegean_Numbers", 16),
    AliasName("Age", 17),
    AliasName("Ain", 18),
    AliasName("Alaph", 19),
    AliasName("Alef", 20),
    AliasName("Alpha", 21),
    AliasName("Alphabetic", 22),
    AliasName("Alphabetic_Presentation_Forms", 23),
    AliasName("Ambiguous", 24),
    AliasName("Ancient_Greek_Musical_Notation", 25),
    AliasName("Ancient_Greek_Numbers", 26),
    AliasName("Arab", 27),
    AliasName("Arabic", 28),
    AliasName("Arabic_Letter", 29),
    AliasName("Arabic_Number", 30),
    AliasName("Arabic_Presentation_Forms-A", 31),
    AliasName("Arabic_Presentation_Forms-B", 32),
    AliasName("Arabic_Supplement", 33),
    AliasName("Armenian", 34),
    AliasName("Armn", 35),
    AliasName("Arrows", 36),
    AliasName("Attached_Above_Right", 37),
    AliasName("Attached_Below", 38),
    AliasName("Attached_Below_Left", 39),
    AliasName("B", 40),
    AliasName("B2", 41),
    AliasName("BA", 42),
    AliasName("BB", 43),
    AliasName("BK", 44),
    AliasName("BL", 45),
    AliasName("BN", 46),
    AliasName("BR", 47),
    AliasName("Basic_Latin", 48),
    AliasName("Beh", 49),
    AliasName("Below", 50),
    AliasName("Below_Left", 51),
    AliasName("Below_Right", 52),
    AliasName("Beng", 53),
    AliasName("Bengali", 54),
    AliasName("Beth", 55),
    AliasName("Bidi_C", 56),
    AliasName("Bidi_Class", 57),
    AliasName("Bidi_Control", 58),
    AliasName("Bidi_M", 59),
    AliasName("Bidi_Mirrored", 60),
    AliasName("Bidi_Mirroring_Glyph", 61),
    AliasName("Block", 62),
    AliasName("Block_Elements", 63),
    AliasName("Bopo", 64),
    AliasName("Bopomofo", 65),
    AliasName("Bopomofo_Extended", 66),
    AliasName("Boundary_Neutral", 67),
    AliasName("Box_Drawing", 68),
    AliasName("Brai", 69),
    AliasName("Braille", 70),
    AliasName("Braille_Patterns", 71),
    AliasName("Break_After", 72),
    AliasName("Break_Before", 73),
    AliasName("Break_Both", 74),
    AliasName("Break_Symbols", 75),
    AliasName("Bugi", 76),
    AliasName("Buginese", 77),
    AliasName("Buhd", 78),
    AliasName("Buhid", 79),
    AliasName("Byzantine_Musical_Symbols", 80),
    AliasName("C", 81),
    AliasName("CB", 82),
    AliasName("CJK_Compatibility", 83),
    AliasName("CJK_Compatibility_Forms", 84),
    AliasName("CJK_Compatibility_Ideographs", 85),
    AliasName("CJK_Compatibility_Ideographs_Supplement", 86),
    AliasName("CJK_Radicals_Supplement", 87),
    AliasName("CJK_Strokes", 88),
    AliasName("CJK_Symbols_and_Punctuation", 89),
    AliasName("CJK_Unified_Ideographs", 90),
    AliasName("CJK_Unified_Ideographs_Extension_A", 91),
    AliasName("CJK_Unified_Ideographs_Extension_B", 92),
    AliasName("CL", 93),
    AliasName("CM", 94),
    AliasName("CR", 95),
    AliasName("CS", 96),
    AliasName("Canadian_Aboriginal", 97),
    AliasName("Canonical", 98),
    AliasName("Canonical_Combining_Class", 99),
    AliasName("Cans", 100),
    AliasName("Carriage_Return", 101),
    AliasName("Case_Folding", 102),
    AliasName("Case_Sensitive", 103),
    AliasName("Cased_Letter", 104),
    AliasName("Cc", 105),
    AliasName("Cf", 106),
    AliasName("Cher", 107),
    AliasName("Cherokee", 108),
    AliasName("Circle", 109),
    AliasName("Close", 110),
    AliasName("Close_Punctuation", 111),
    AliasName("Cn", 112),
    AliasName("Co", 113),
    AliasName("Combining_Diacritical_Marks", 114),
    AliasName("Combining_Diacritical_Marks_Supplement", 115),
    AliasName("Combining_Diacritical_Marks_for_Symbols", 116),
    AliasName("Combining_Half_Marks", 117),
    AliasName("Combining_Mark", 118),
    AliasName("Common", 119),
    AliasName("Common_Separator", 120),
    AliasName("Comp_Ex", 121),
    AliasName("Compat", 122),
    AliasName("Complex_Context", 123),
    AliasName("Connector_Punctuation", 124),
    AliasName("Contingent_Break", 125),
    AliasName("Control", 126),
    AliasName("Control_Pictures", 127),
    AliasName("Copt", 128),
    AliasName("Coptic", 129),
    AliasName("Cprt", 130),
    AliasName("Cs", 131),
    AliasName("Currency_Symbol", 132),
    AliasName("Currency_Symbols", 133),
    AliasName("Cypriot", 134),
    AliasName("Cypriot_Syllabary", 135),
    AliasName("Cyrillic", 136),
    AliasName("Cyrillic_Supplement", 137),
    AliasName("Cyrillic_Supplementary", 138),
    AliasName("Cyrl", 139),
    AliasName("D", 140),
    AliasName("DA", 141),
    AliasName("DB", 142),
    AliasName("DI", 143),
    AliasName("Dal", 144),
    AliasName("Dalath_Rish", 145),
    AliasName("Dash", 146),
    AliasName("Dash_Punctuation", 147),
    AliasName("De", 148),
    AliasName("Decimal", 149),
    AliasName("Decimal_Number", 150),
    AliasName("Decomposition_Type", 151),
    AliasName("Default_Ignorable_Code_Point", 152),
    AliasName("Dep", 153),
    AliasName("Deprecated", 154),
    AliasName("Deseret", 155),
    AliasName("Deva", 156),
    AliasName("Devanagari", 157),
    AliasName("Di", 158),
    AliasName("Dia", 159),
    AliasName("Diacritic", 160),
    AliasName("Digit", 161),
    AliasName("Dingbats", 162),
    AliasName("Double_Above", 163),
    AliasName("Double_Below", 164),
    AliasName("Dsrt", 165),
    AliasName("Dual_Joining", 166),
    AliasName("E", 167),
    AliasName("EN", 168),
    AliasName("ES", 169),
    AliasName("ET", 170),
    AliasName("EX", 171),
    AliasName("East_Asian_Width", 172),
    AliasName("Enclosed_Alphanumerics", 173),
    AliasName("Enclosed_CJK_Letters_and_Months", 174),
    AliasName("Enclosing_Mark", 175),
    AliasName("Ethi", 176),
    AliasName("Ethiopic", 177),
    AliasName("Ethiopic_Extended", 178),
    AliasName("Ethiopic_Supplement", 179),
    AliasName("European_Number", 180),
    AliasName("European_Separator", 181),
    AliasName("European_Terminator", 182),
    AliasName("Exclamation", 183),
    AliasName("Ext", 184),
    AliasName("Extend", 185),
    AliasName("Extender", 186),
    AliasName("F", 187),
    AliasName("False", 188),
    AliasName("Fe", 189),
    AliasName("Feh", 190),
    AliasName("Final", 191),
    AliasName("Final_Punctuation", 192),
    AliasName("Final_Semkath", 193),
    AliasName("Font", 194),
    AliasName("Format", 195),
    AliasName("Fraction", 196),
    AliasName("Full_Composition_Exclusion", 197),
    AliasName("Fullwidth", 198),
    AliasName("GCB", 199),
    AliasName("GL", 200),
    AliasName("Gaf", 201),
    AliasName("Gamal", 202),
    AliasName("General_Category", 203),
    AliasName("General_Category_Mask", 204),
    AliasName("General_Punctuation", 205),
    AliasName("Geometric_Shapes", 206),
    AliasName("Geor", 207),
    AliasName("Georgian", 208),
    AliasName("Georgian_Supplement", 209),
    AliasName("Glag", 210),
    AliasName("Glagolitic", 211),
    AliasName("Glue", 212),
    AliasName("Goth", 213),
    AliasName("Gothic", 214),
    AliasName("Gr_Base", 215),
    AliasName("Gr_Ext", 216),
    AliasName("Gr_Link", 217),
    AliasName("Grapheme_Base", 218),
    AliasName("Grapheme_Cluster_Break", 219),
    AliasName("Grapheme_Extend", 220),
    AliasName("Grapheme_Link", 221),
    AliasName("Greek", 222),
    AliasName("Greek_Extended", 223),
    AliasName("Greek_and_Coptic", 224),
    AliasName("Grek", 225),
    AliasName("Gujarati", 226),
    AliasName("Gujr", 227),
    AliasName("Gurmukhi", 228),
    AliasName("Guru", 229),
    AliasName("H", 230),
    AliasName("H2", 231),
    AliasName("H3", 232),
    AliasName("HY", 233),
    AliasName("Hah", 234),
    AliasName("Halfwidth", 235),
    AliasName("Halfwidth_and_Fullwidth_Forms", 236),
    AliasName("Hamza_On_Heh_Goal", 237),
    AliasName("Han", 238),
    AliasName("Hang", 239),
    AliasName("Hangul", 240),
    AliasName("Hangul_Compatibility_Jamo", 241),
    AliasName("Hangul_Jamo", 242),
    AliasName("Hangul_Syllable_Type", 243),
    AliasName("Hangul_Syllables", 244),
    AliasName("Hani", 245),
    AliasName("Hano", 246),
    AliasName("Hanunoo", 247),
    AliasName("He", 248),
    AliasName("Hebr", 249),
    AliasName("Hebrew", 250),
    AliasName("Heh", 251),
    AliasName("Heh_Goal", 252),
    AliasName("Heth", 253),
    AliasName("Hex", 254),
    AliasName("Hex_Digit", 255),
    AliasName("High_Private_Use_Surrogates", 256),
    AliasName("High_Surrogates", 257),
    AliasName("Hira", 258),
    AliasName("Hiragana", 259),
    AliasName("Hrkt", 260),
    AliasName("Hyphen", 261),
    AliasName("ID", 262),
    AliasName("IDC", 263),
    AliasName("IDS", 264),
    AliasName("IDSB", 265),
    AliasName("IDST", 266),
    AliasName("IDS_Binary_Operator", 267),
    AliasName("IDS_Trinary_Operator", 268),
    AliasName("ID_Continue", 269),
    AliasName("ID_Start", 270),
    AliasName("IN", 271),
    AliasName("IPA_Extensions", 272),
    AliasName("IS", 273),
    AliasName("ISO_Comment", 274),
    AliasName("Ideo", 275),
    AliasName("Ideographic", 276),
    AliasName("Ideographic_Description_Characters", 277),
    AliasName("Infix_Numeric", 278),
    AliasName("Inherited", 279),
    AliasName("Initial", 280),
    AliasName("Initial_Punctuation", 281),
    AliasName("Inseparable", 282),
    AliasName("Inseperable", 283),
    AliasName("Iota_Subscript", 284),
    AliasName("Isolated", 285),
    AliasName("Ital", 286),
    AliasName("JL", 287),
    AliasName("JT", 288),
    AliasName("JV", 289),
    AliasName("Join_C", 290),
    AliasName("Join_Causing", 291),
    AliasName("Join_Control", 292),
    AliasName("Joining_Group", 293),
    AliasName("Joining_Type", 294),
    AliasName("KV", 295),
    AliasName("Kaf", 296),
    AliasName("Kana", 297),
    AliasName("Kana_Voicing", 298),
    AliasName("Kanbun", 299),
    AliasName("Kangxi_Radicals", 300),
    AliasName("Kannada", 301),
    AliasName("Kaph", 302),
    AliasName("Katakana", 303),
    AliasName("Katakana_Or_Hiragana", 304),
    AliasName("Katakana_Phonetic_Extensions", 305),
    AliasName("Khaph", 306),
    AliasName("Khar", 307),
    AliasName("Kharoshthi", 308),
    AliasName("Khmer", 309),
    AliasName("Khmer_Symbols", 310),
    AliasName("Khmr", 311),
    AliasName("Knda", 312),
    AliasName("Knotted_Heh", 313),
    AliasName("L", 314),
    AliasName("LC", 315),
    AliasName("LF", 316),
    AliasName("LOE", 317),
    AliasName("LRE", 318),
    AliasName("LRO", 319),
    AliasName("LV", 320),
    AliasName("LVT", 321),
    AliasName("LVT_Syllable", 322),
    AliasName("LV_Syllable", 323),
    AliasName("Lam", 324),
    AliasName("Lamadh", 325),
    AliasName("Lao", 326),
    AliasName("Laoo", 327),
    AliasName("Latin", 328),
    AliasName("Latin-1_Supplement", 329),
    AliasName("Latin_Extended-A", 330),
    AliasName("Latin_Extended-B", 331),
    AliasName("Latin_Extended_Additional", 332),
    AliasName("Latn", 333),
    AliasName("Lead_Canonical_Combining_Class", 334),
    AliasName("Leading_Jamo", 335),
    AliasName("Left", 336),
    AliasName("Left_Joining", 337),
    AliasName("Left_To_Right", 338),
    AliasName("Left_To_Right_Embedding", 339),
    AliasName("Left_To_Right_Override", 340),
    AliasName("Letter", 341),
    AliasName("Letter_Number", 342),
    AliasName("Letterlike_Symbols", 343),
    AliasName("Limb", 344),
    AliasName("Limbu", 345),
    AliasName("Linb", 346),
    AliasName("Line_Break", 347),
    AliasName("Line_Feed", 348),
    AliasName("Line_Separator", 349),
    AliasName("Linear_B", 350),
    AliasName("Linear_B_Ideograms", 351),
    AliasName("Linear_B_Syllabary", 352),
    AliasName("Ll", 353),
    AliasName("Lm", 354),
    AliasName("Lo", 355),
    AliasName("Logical_Order_Exception", 356),
    AliasName("Low_Surrogates", 357),
    AliasName("Lower", 358),
    AliasName("Lowercase", 359),
    AliasName("Lowercase_Letter", 360),
    AliasName("Lowercase_Mapping", 361),
    AliasName("Lt", 362),
    AliasName("Lu", 363),
    AliasName("M", 364),
    AliasName("Malayalam", 365),
    AliasName("Mandatory_Break", 366),
    AliasName("Mark", 367),
    AliasName("Math", 368),
    AliasName("Math_Symbol", 369),
    AliasName("Mathematical_Alphanumeric_Symbols", 370),
    AliasName("Mathematical_Operators", 371),
    AliasName("Maybe", 372),
    AliasName("Mc", 373),
    AliasName("Me", 374),
    AliasName("Medial", 375),
    AliasName("Meem", 376),
    AliasName("MidLetter", 377),
    AliasName("MidNum", 378),
    AliasName("Mim", 379),
    AliasName("Miscellaneous_Mathematical_Symbols-A", 380),
    AliasName("Miscellaneous_Mathematical_Symbols-B", 381),
    AliasName("Miscellaneous_Symbols", 382),
    AliasName("Miscellaneous_Symbols_and_Arrows", 383),
    AliasName("Miscellaneous_Technical", 384),
    AliasName("Mlym", 385),
    AliasName("Mn", 386),
    AliasName("Modifier_Letter", 387),
    AliasName("Modifier_Symbol", 388),
    AliasName("Modifier_Tone_Letters", 389),
    AliasName("Mong", 390),
    AliasName("Mongolian", 391),
    AliasName("Musical_Symbols", 392),
    AliasName("Myanmar", 393),
    AliasName("Mymr", 394),
    AliasName("N", 395),
    AliasName("NA", 396),
    AliasName("NChar", 397),
    AliasName("NFC_Inert", 398),
    AliasName("NFC_QC", 399),
    AliasName("NFC_Quick_Check", 400),
    AliasName("NFD_Inert", 401),
    AliasName("NFD_QC", 402),
    AliasName("NFD_Quick_Check", 403),
    AliasName("NFKC_Inert", 404),
    AliasName("NFKC_QC", 405),
    AliasName("NFKC_Quick_Check", 406),
    AliasName("NFKD_Inert", 407),
    AliasName("NFKD_QC", 408),
    AliasName("NFKD_Quick_Check", 409),
    AliasName("NK", 410),
    AliasName("NL", 411),
    AliasName("NR", 412),
    AliasName("NS", 413),
    AliasName("NSM", 414),
    AliasName("NU", 415),
    AliasName("Na", 416),
    AliasName("Name", 417),
    AliasName("Narrow", 418),
    AliasName("Nd", 419),
    AliasName("Neutral", 420),
    AliasName("New_Tai_Lue", 421),
    AliasName("Next_Line", 422),
    AliasName("Nl", 423),
    AliasName("No", 424),
    AliasName("No_Block", 425),
    AliasName("No_Joining_Group", 426),
    AliasName("Nobreak", 427),
    AliasName("Non_Joining", 428),
    AliasName("Noncharacter_Code_Point", 429),
    AliasName("None", 430),
    AliasName("Nonspacing_Mark", 431),
    AliasName("Nonstarter", 432),
    AliasName("Noon", 433),
    AliasName("Not_Applicable", 434),
    AliasName("Not_Reordered", 435),
    AliasName("Nu", 436),
    AliasName("Nukta", 437),
    AliasName("Number", 438),
    AliasName("Number_Forms", 439),
    AliasName("Numeric", 440),
    AliasName("Numeric_Type", 441),
    AliasName("Numeric_Value", 442),
    AliasName("Nun", 443),
    AliasName("OLetter", 444),
    AliasName("ON", 445),
    AliasName("OP", 446),
    AliasName("OV", 447),
    AliasName("Ogam", 448),
    AliasName("Ogham", 449),
    AliasName("Old_Italic", 450),
    AliasName("Old_Persian", 451),
    AliasName("Open_Punctuation", 452),
    AliasName("Optical_Character_Recognition", 453),
    AliasName("Oriya", 454),
    AliasName("Orya", 455),
    AliasName("Osma", 456),
    AliasName("Osmanya", 457),
    AliasName("Other", 458),
    AliasName("Other_Letter", 459),
    AliasName("Other_Neutral", 460),
    AliasName("Other_Number", 461),
    AliasName("Other_Punctuation", 462),
    AliasName("Other_Symbol", 463),
    AliasName("Overlay", 464),
    AliasName("P", 465),
    AliasName("PDF", 466),
    AliasName("PO", 467),
    AliasName("PR", 468),
    AliasName("Paragraph_Separator", 469),
    AliasName("Pat_Syn", 470),
    AliasName("Pat_WS", 471),
    AliasName("Pattern_Syntax", 472),
    AliasName("Pattern_White_Space", 473),
    AliasName("Pc", 474),
    AliasName("Pd", 475),
    AliasName("Pe", 476),
    AliasName("Pf", 477),
    AliasName("Phonetic_Extensions", 478),
    AliasName("Phonetic_Extensions_Supplement", 479),
    AliasName("Pi", 480),
    AliasName("Po", 481),
    AliasName("Pop_Directional_Format", 482),
    AliasName("Postfix_Numeric", 483),
    AliasName("Prefix_Numeric", 484),
    AliasName("Private_Use", 485),
    AliasName("Private_Use_Area", 486),
    AliasName("Ps", 487),
    AliasName("Punctuation", 488),
    AliasName("QMark", 489),
    AliasName("QU", 490),
    AliasName("Qaac", 491),
    AliasName("Qaai", 492),
    AliasName("Qaf", 493),
    AliasName("Qaph", 494),
    AliasName("Quotation", 495),
    AliasName("Quotation_Mark", 496),
    AliasName("R", 497),
    AliasName("RLE", 498),
    AliasName("RLO", 499),
    AliasName("Radical", 500),
    AliasName("Reh", 501),
    AliasName("Reversed_Pe", 502),
    AliasName("Right", 503),
    AliasName("Right_Joining", 504),
    AliasName("Right_To_Left", 505),
    AliasName("Right_To_Left_Embedding", 506),
    AliasName("Right_To_Left_Override", 507),
    AliasName("Runic", 508),
    AliasName("Runr", 509),
    AliasName("S", 510),
    AliasName("SA", 511),
    AliasName("SB", 512),
    AliasName("SD", 513),
    AliasName("SG", 514),
    AliasName("SP", 515),
    AliasName("STerm", 516),
    AliasName("SY", 517),
    AliasName("Sad", 518),
    AliasName("Sadhe", 519),
    AliasName("Sc", 520),
    AliasName("Script", 521),
    AliasName("Seen", 522),
    AliasName("Segment_Separator", 523),
    AliasName("Segment_Starter", 524),
    AliasName("Semkath", 525),
    AliasName("Sensitive", 526),
    AliasName("Sentence_Break", 527),
    AliasName("Sep", 528),
    AliasName("Separator", 529),
    AliasName("Shavian", 530),
    AliasName("Shaw", 531),
    AliasName("Shin", 532),
    AliasName("Simple_Case_Folding", 533),
    AliasName("Simple_Lowercase_Mapping", 534),
    AliasName("Simple_Titlecase_Mapping", 535),
    AliasName("Simple_Uppercase_Mapping", 536),
    AliasName("Sinh", 537),
    AliasName("Sinhala", 538),
    AliasName("Sk", 539),
    AliasName("Sm", 540),
    AliasName("Small", 541),
    AliasName("Small_Form_Variants", 542),
    AliasName("So", 543),
    AliasName("Soft_Dotted", 544),
    AliasName("Sp", 545),
    AliasName("Space", 546),
    AliasName("Space_Separator", 547),
    AliasName("Spacing_Mark", 548),
    AliasName("Spacing_Modifier_Letters", 549),
    AliasName("Specials", 550),
    AliasName("Square", 551),
    AliasName("Sub", 552),
    AliasName("Super", 553),
    AliasName("Superscripts_and_Subscripts", 554),
    AliasName("Supplemental_Arrows-A", 555),
    AliasName("Supplemental_Arrows-B", 556),
    AliasName("Supplemental_Mathematical_Operators", 557),
    AliasName("Supplemental_Punctuation", 558),
    AliasName("Supplementary_Private_Use_Area-A", 559),
    AliasName("Supplementary_Private_Use_Area-B", 560),
    AliasName("Surrogate", 561),
    AliasName("Swash_Kaf", 562),
    AliasName("Sylo", 563),
    AliasName("Syloti_Nagri", 564),
    AliasName("Symbol", 565),
    AliasName("Syrc", 566),
    AliasName("Syriac", 567),
    AliasName("Syriac_Waw", 568),
    AliasName("T", 569),
    AliasName("Tagalog", 570),
    AliasName("Tagb", 571),
    AliasName("Tagbanwa", 572),
    AliasName("Tags", 573),
    AliasName("Tah", 574),
    AliasName("Tai_Le", 575),
    AliasName("Tai_Xuan_Jing_Symbols", 576),
    AliasName("Tale", 577),
    AliasName("Talu", 578),
    AliasName("Tamil", 579),
    AliasName("Taml", 580),
    AliasName("Taw", 581),
    AliasName("Teh_Marbuta", 582),
    AliasName("Telu", 583),
    AliasName("Telugu", 584),
    AliasName("Term", 585),
    AliasName("Terminal_Punctuation", 586),
    AliasName("Teth", 587),
    AliasName("Tfng", 588),
    AliasName("Tglg", 589),
    AliasName("Thaa", 590),
    AliasName("Thaana", 591),
    AliasName("Thai", 592),
    AliasName("Tibetan", 593),
    AliasName("Tibt", 594),
    AliasName("Tifinagh", 595),
    AliasName("Titlecase_Letter", 596),
    AliasName("Titlecase_Mapping", 597),
    AliasName("Trail_Canonical_Combining_Class", 598),
    AliasName("Trailing_Jamo", 599),
    AliasName("Transparent", 600),
    AliasName("True", 601),
    AliasName("U", 602),
    AliasName("UIdeo", 603),
    AliasName("Ugar", 604),
    AliasName("Ugaritic", 605),
    AliasName("Unassigned", 606),
    AliasName("Unicode_1_Name", 607),
    AliasName("Unified_Canadian_Aboriginal_Syllabics", 608),
    AliasName("Unified_Ideograph", 609),
    AliasName("Unknown", 610),
    AliasName("Upper", 611),
    AliasName("Uppercase", 612),
    AliasName("Uppercase_Letter", 613),
    AliasName("Uppercase_Mapping", 614),
    AliasName("V", 615),
    AliasName("VR", 616),
    AliasName("VS", 617),
    AliasName("Variation_Selector", 618),
    AliasName("Variation_Selectors", 619),
    AliasName("Variation_Selectors_Supplement", 620),
    AliasName("Vertical", 621),
    AliasName("Vertical_Forms", 622),
    AliasName("Virama", 623),
    AliasName("Vowel_Jamo", 624),
    AliasName("W", 625),
    AliasName("WB", 626),
    AliasName("WJ", 627),
    AliasName("WS", 628),
    AliasName("WSpace", 629),
    AliasName("Waw", 630),
    AliasName("White_Space", 631),
    AliasName("Wide", 632),
    AliasName("Word_Break", 633),
    AliasName("Word_Joiner", 634),
    AliasName("XIDC", 635),
    AliasName("XIDS", 636),
    AliasName("XID_Continue", 637),
    AliasName("XID_Start", 638),
    AliasName("XX", 639),
    AliasName("Xpeo", 640),
    AliasName("Y", 641),
    AliasName("Yeh", 642),
    AliasName("Yeh_Barree", 643),
    AliasName("Yeh_With_Tail", 644),
    AliasName("Yes", 645),
    AliasName("Yi", 646),
    AliasName("Yi_Radicals", 647),
    AliasName("Yi_Syllables", 648),
    AliasName("Yiii", 649),
    AliasName("Yijing_Hexagram_Symbols", 650),
    AliasName("Yudh", 651),
    AliasName("Yudh_He", 652),
    AliasName("Z", 653),
    AliasName("ZW", 654),
    AliasName("ZWSpace", 655),
    AliasName("Zain", 656),
    AliasName("Zhain", 657),
    AliasName("Zl", 658),
    AliasName("Zp", 659),
    AliasName("Zs", 660),
    AliasName("Zyyy", 661),
    AliasName("age", 662),
    AliasName("bc", 663),
    AliasName("blk", 664),
    AliasName("bmg", 665),
    AliasName("can", 666),
    AliasName("ccc", 667),
    AliasName("cf", 668),
    AliasName("cntrl", 669),
    AliasName("com", 670),
    AliasName("digit", 671),
    AliasName("dt", 672),
    AliasName("ea", 673),
    AliasName("enc", 674),
    AliasName("fin", 675),
    AliasName("font", 676),
    AliasName("fra", 677),
    AliasName("gc", 678),
    AliasName("gcm", 679),
    AliasName("hst", 680),
    AliasName("init", 681),
    AliasName("isc", 682),
    AliasName("iso", 683),
    AliasName("jg", 684),
    AliasName("jt", 685),
    AliasName("lb", 686),
    AliasName("lc", 687),
    AliasName("lccc", 688),
    AliasName("med", 689),
    AliasName("na", 690),
    AliasName("na1", 691),
    AliasName("nar", 692),
    AliasName("nb", 693),
    AliasName("nfcinert", 694),
    AliasName("nfdinert", 695),
    AliasName("nfkcinert", 696),
    AliasName("nfkdinert", 697),
    AliasName("none", 698),
    AliasName("nt", 699),
    AliasName("nv", 700),
    AliasName("punct", 701),
    AliasName("sc", 702),
    AliasName("segstart", 703),
    AliasName("sfc", 704),
    AliasName("slc", 705),
    AliasName("sml", 706),
    AliasName("space", 707),
    AliasName("sqr", 708),
    AliasName("stc", 709),
    AliasName("sub", 710),
    AliasName("suc", 711),
    AliasName("sup", 712),
    AliasName("tc", 713),
    AliasName("tccc", 714),
    AliasName("uc", 715),
    AliasName("vert", 716),
    AliasName("wide", 717),
};

/* to be filled in */
int32_t REMAP[718];

const int32_t NAME_GROUP_COUNT = 1051;

int32_t NAME_GROUP[] = {
    126, -126,          /*   0: "Control", "Control" */
    95, -95,            /*   2: "CR", "CR" */
    185, -185,          /*   4: "Extend", "Extend" */
    314, -314,          /*   6: "L", "L" */
    316, -316,          /*   8: "LF", "LF" */
    320, -320,          /*  10: "LV", "LV" */
    321, -321,          /*  12: "LVT", "LVT" */
    458, -458,          /*  14: "Other", "Other" */
    569, -569,          /*  16: "T", "T" */
    615, -615,          /*  18: "V", "V" */
    364, -372,          /*  20: "M", "Maybe" */
    395, -424,          /*  22: "N", "No" */
    641, -645,          /*  24: "Y", "Yes" */
    12, -12,            /*  26: "ATerm", "ATerm" */
    110, -110,          /*  28: "Close", "Close" */
    195, -195,          /*  30: "Format", "Format" */
    358, -358,          /*  32: "Lower", "Lower" */
    440, -440,          /*  34: "Numeric", "Numeric" */
    444, -444,          /*  36: "OLetter", "OLetter" */
    528, -528,          /*  38: "Sep", "Sep" */
    545, -545,          /*  40: "Sp", "Sp" */
    516, -516,          /*  42: "STerm", "STerm" */
    611, -611,          /*  44: "Upper", "Upper" */
    5, -5,              /*  46: "ALetter", "ALetter" */
    303, -303,          /*  48: "Katakana", "Katakana" */
    377, -377,          /*  50: "MidLetter", "MidLetter" */
    378, -378,          /*  52: "MidNum", "MidNum" */
    21, -22,            /*  54: "Alpha", "Alphabetic" */
    2, -8,              /*  56: "AHex", "ASCII_Hex_Digit" */
    56, -58,            /*  58: "Bidi_C", "Bidi_Control" */
    59, -60,            /*  60: "Bidi_M", "Bidi_Mirrored" */
    526, -103,          /*  62: "Sensitive", "Case_Sensitive" */
    146, -146,          /*  64: "Dash", "Dash" */
    143, -152,          /*  66: "DI", "Default_Ignorable_Code_Point" */
    153, -154,          /*  68: "Dep", "Deprecated" */
    159, -160,          /*  70: "Dia", "Diacritic" */
    184, -186,          /*  72: "Ext", "Extender" */
    121, -197,          /*  74: "Comp_Ex", "Full_Composition_Exclusion" */
    215, -218,          /*  76: "Gr_Base", "Grapheme_Base" */
    216, -220,          /*  78: "Gr_Ext", "Grapheme_Extend" */
    217, -221,          /*  80: "Gr_Link", "Grapheme_Link" */
    254, -255,          /*  82: "Hex", "Hex_Digit" */
    261, -261,          /*  84: "Hyphen", "Hyphen" */
    275, -276,          /*  86: "Ideo", "Ideographic" */
    265, -267,          /*  88: "IDSB", "IDS_Binary_Operator" */
    266, -268,          /*  90: "IDST", "IDS_Trinary_Operator" */
    263, -269,          /*  92: "IDC", "ID_Continue" */
    264, -270,          /*  94: "IDS", "ID_Start" */
    290, -292,          /*  96: "Join_C", "Join_Control" */
    317, -356,          /*  98: "LOE", "Logical_Order_Exception" */
    358, -359,          /* 100: "Lower", "Lowercase" */
    368, -368,          /* 102: "Math", "Math" */
    694, -398,          /* 104: "nfcinert", "NFC_Inert" */
    695, -401,          /* 106: "nfdinert", "NFD_Inert" */
    696, -404,          /* 108: "nfkcinert", "NFKC_Inert" */
    697, -407,          /* 110: "nfkdinert", "NFKD_Inert" */
    397, -429,          /* 112: "NChar", "Noncharacter_Code_Point" */
    470, -472,          /* 114: "Pat_Syn", "Pattern_Syntax" */
    471, -473,          /* 116: "Pat_WS", "Pattern_White_Space" */
    489, -496,          /* 118: "QMark", "Quotation_Mark" */
    500, -500,          /* 120: "Radical", "Radical" */
    703, -524,          /* 122: "segstart", "Segment_Starter" */
    513, -544,          /* 124: "SD", "Soft_Dotted" */
    585, -586,          /* 126: "Term", "Terminal_Punctuation" */
    603, -609,          /* 128: "UIdeo", "Unified_Ideograph" */
    611, -612,          /* 130: "Upper", "Uppercase" */
    617, -618,          /* 132: "VS", "Variation_Selector" */
    629, 631, -707,     /* 134: "WSpace", "White_Space", "space" */
    635, -637,          /* 137: "XIDC", "XID_Continue" */
    636, -638,          /* 139: "XIDS", "XID_Start" */
    700, -442,          /* 141: "nv", "Numeric_Value" */
    663, -57,           /* 143: "bc", "Bidi_Class" */
    664, -62,           /* 145: "blk", "Block" */
    667, -99,           /* 147: "ccc", "Canonical_Combining_Class" */
    672, -151,          /* 149: "dt", "Decomposition_Type" */
    673, -172,          /* 151: "ea", "East_Asian_Width" */
    678, -203,          /* 153: "gc", "General_Category" */
    199, -219,          /* 155: "GCB", "Grapheme_Cluster_Break" */
    680, -243,          /* 157: "hst", "Hangul_Syllable_Type" */
    684, -293,          /* 159: "jg", "Joining_Group" */
    685, -294,          /* 161: "jt", "Joining_Type" */
    688, -334,          /* 163: "lccc", "Lead_Canonical_Combining_Class" */
    686, -347,          /* 165: "lb", "Line_Break" */
    399, -400,          /* 167: "NFC_QC", "NFC_Quick_Check" */
    402, -403,          /* 169: "NFD_QC", "NFD_Quick_Check" */
    405, -406,          /* 171: "NFKC_QC", "NFKC_Quick_Check" */
    408, -409,          /* 173: "NFKD_QC", "NFKD_Quick_Check" */
    699, -441,          /* 175: "nt", "Numeric_Type" */
    702, -521,          /* 177: "sc", "Script" */
    512, -527,          /* 179: "SB", "Sentence_Break" */
    714, -598,          /* 181: "tccc", "Trail_Canonical_Combining_Class" */
    626, -633,          /* 183: "WB", "Word_Break" */
    679, -204,          /* 185: "gcm", "General_Category_Mask" */
    662, -17,           /* 187: "age", "Age" */
    665, -61,           /* 189: "bmg", "Bidi_Mirroring_Glyph" */
    668, -102,          /* 191: "cf", "Case_Folding" */
    682, -274,          /* 193: "isc", "ISO_Comment" */
    687, -361,          /* 195: "lc", "Lowercase_Mapping" */
    690, -417,          /* 197: "na", "Name" */
    704, -533,          /* 199: "sfc", "Simple_Case_Folding" */
    705, -534,          /* 201: "slc", "Simple_Lowercase_Mapping" */
    709, -535,          /* 203: "stc", "Simple_Titlecase_Mapping" */
    711, -536,          /* 205: "suc", "Simple_Uppercase_Mapping" */
    713, -597,          /* 207: "tc", "Titlecase_Mapping" */
    691, -607,          /* 209: "na1", "Unicode_1_Name" */
    715, -614,          /* 211: "uc", "Uppercase_Mapping" */
    6, -30,             /* 213: "AN", "Arabic_Number" */
    40, -469,           /* 215: "B", "Paragraph_Separator" */
    46, -67,            /* 217: "BN", "Boundary_Neutral" */
    96, -120,           /* 219: "CS", "Common_Separator" */
    414, -431,          /* 221: "NSM", "Nonspacing_Mark" */
    168, -180,          /* 223: "EN", "European_Number" */
    169, -181,          /* 225: "ES", "European_Separator" */
    170, -182,          /* 227: "ET", "European_Terminator" */
    314, -338,          /* 229: "L", "Left_To_Right" */
    318, -339,          /* 231: "LRE", "Left_To_Right_Embedding" */
    319, -340,          /* 233: "LRO", "Left_To_Right_Override" */
    445, -460,          /* 235: "ON", "Other_Neutral" */
    466, -482,          /* 237: "PDF", "Pop_Directional_Format" */
    497, -505,          /* 239: "R", "Right_To_Left" */
    4, -29,             /* 241: "AL", "Arabic_Letter" */
    498, -506,          /* 243: "RLE", "Right_To_Left_Embedding" */
    499, -507,          /* 245: "RLO", "Right_To_Left_Override" */
    510, -523,          /* 247: "S", "Segment_Separator" */
    628, -631,          /* 249: "WS", "White_Space" */
    187, -188,          /* 251: "F", "False" */
    569, -601,          /* 253: "T", "True" */
    0, -16,             /* 255: "", "Aegean_Numbers" */
    0, -23,             /* 257: "", "Alphabetic_Presentation_Forms" */
    0, -25,             /* 259: "", "Ancient_Greek_Musical_Notation" */
    0, -26,             /* 261: "", "Ancient_Greek_Numbers" */
    0, -28,             /* 263: "", "Arabic" */
    0, -31,             /* 265: "", "Arabic_Presentation_Forms-A" */
    0, -32,             /* 267: "", "Arabic_Presentation_Forms-B" */
    0, -33,             /* 269: "", "Arabic_Supplement" */
    0, -34,             /* 271: "", "Armenian" */
    0, -36,             /* 273: "", "Arrows" */
    0, -48,             /* 275: "", "Basic_Latin" */
    0, -54,             /* 277: "", "Bengali" */
    0, -63,             /* 279: "", "Block_Elements" */
    0, -65,             /* 281: "", "Bopomofo" */
    0, -66,             /* 283: "", "Bopomofo_Extended" */
    0, -68,             /* 285: "", "Box_Drawing" */
    0, -71,             /* 287: "", "Braille_Patterns" */
    0, -77,             /* 289: "", "Buginese" */
    0, -79,             /* 291: "", "Buhid" */
    0, -80,             /* 293: "", "Byzantine_Musical_Symbols" */
    0, -108,            /* 295: "", "Cherokee" */
    0, -83,             /* 297: "", "CJK_Compatibility" */
    0, -84,             /* 299: "", "CJK_Compatibility_Forms" */
    0, -85,             /* 301: "", "CJK_Compatibility_Ideographs" */
    0, -86,             /* 303: "", "CJK_Compatibility_Ideographs_Supplement" */
    0, -87,             /* 305: "", "CJK_Radicals_Supplement" */
    0, -88,             /* 307: "", "CJK_Strokes" */
    0, -89,             /* 309: "", "CJK_Symbols_and_Punctuation" */
    0, -90,             /* 311: "", "CJK_Unified_Ideographs" */
    0, -91,             /* 313: "", "CJK_Unified_Ideographs_Extension_A" */
    0, -92,             /* 315: "", "CJK_Unified_Ideographs_Extension_B" */
    0, -114,            /* 317: "", "Combining_Diacritical_Marks" */
    0, -115,            /* 319: "", "Combining_Diacritical_Marks_Supplement" */
    0, -117,            /* 321: "", "Combining_Half_Marks" */
    0, -116,            /* 323: "", "Combining_Diacritical_Marks_for_Symbols" */
    0, -127,            /* 325: "", "Control_Pictures" */
    0, -129,            /* 327: "", "Coptic" */
    0, -133,            /* 329: "", "Currency_Symbols" */
    0, -135,            /* 331: "", "Cypriot_Syllabary" */
    0, -136,            /* 333: "", "Cyrillic" */
    0, 137, -138,       /* 335: "", "Cyrillic_Supplement", "Cyrillic_Supplementary" */
    0, -155,            /* 338: "", "Deseret" */
    0, -157,            /* 340: "", "Devanagari" */
    0, -162,            /* 342: "", "Dingbats" */
    0, -173,            /* 344: "", "Enclosed_Alphanumerics" */
    0, -174,            /* 346: "", "Enclosed_CJK_Letters_and_Months" */
    0, -177,            /* 348: "", "Ethiopic" */
    0, -178,            /* 350: "", "Ethiopic_Extended" */
    0, -179,            /* 352: "", "Ethiopic_Supplement" */
    0, -205,            /* 354: "", "General_Punctuation" */
    0, -206,            /* 356: "", "Geometric_Shapes" */
    0, -208,            /* 358: "", "Georgian" */
    0, -209,            /* 360: "", "Georgian_Supplement" */
    0, -211,            /* 362: "", "Glagolitic" */
    0, -214,            /* 364: "", "Gothic" */
    0, -224,            /* 366: "", "Greek_and_Coptic" */
    0, -223,            /* 368: "", "Greek_Extended" */
    0, -226,            /* 370: "", "Gujarati" */
    0, -228,            /* 372: "", "Gurmukhi" */
    0, -236,            /* 374: "", "Halfwidth_and_Fullwidth_Forms" */
    0, -241,            /* 376: "", "Hangul_Compatibility_Jamo" */
    0, -242,            /* 378: "", "Hangul_Jamo" */
    0, -244,            /* 380: "", "Hangul_Syllables" */
    0, -247,            /* 382: "", "Hanunoo" */
    0, -250,            /* 384: "", "Hebrew" */
    0, -256,            /* 386: "", "High_Private_Use_Surrogates" */
    0, -257,            /* 388: "", "High_Surrogates" */
    0, -259,            /* 390: "", "Hiragana" */
    0, -277,            /* 392: "", "Ideographic_Description_Characters" */
    0, -272,            /* 394: "", "IPA_Extensions" */
    0, -299,            /* 396: "", "Kanbun" */
    0, -300,            /* 398: "", "Kangxi_Radicals" */
    0, -301,            /* 400: "", "Kannada" */
    0, -303,            /* 402: "", "Katakana" */
    0, -305,            /* 404: "", "Katakana_Phonetic_Extensions" */
    0, -308,            /* 406: "", "Kharoshthi" */
    0, -309,            /* 408: "", "Khmer" */
    0, -310,            /* 410: "", "Khmer_Symbols" */
    0, -326,            /* 412: "", "Lao" */
    0, -329,            /* 414: "", "Latin-1_Supplement" */
    0, -330,            /* 416: "", "Latin_Extended-A" */
    0, -332,            /* 418: "", "Latin_Extended_Additional" */
    0, -331,            /* 420: "", "Latin_Extended-B" */
    0, -343,            /* 422: "", "Letterlike_Symbols" */
    0, -345,            /* 424: "", "Limbu" */
    0, -351,            /* 426: "", "Linear_B_Ideograms" */
    0, -352,            /* 428: "", "Linear_B_Syllabary" */
    0, -357,            /* 430: "", "Low_Surrogates" */
    0, -365,            /* 432: "", "Malayalam" */
    0, -370,            /* 434: "", "Mathematical_Alphanumeric_Symbols" */
    0, -371,            /* 436: "", "Mathematical_Operators" */
    0, -380,            /* 438: "", "Miscellaneous_Mathematical_Symbols-A" */
    0, -381,            /* 440: "", "Miscellaneous_Mathematical_Symbols-B" */
    0, -382,            /* 442: "", "Miscellaneous_Symbols" */
    0, -383,            /* 444: "", "Miscellaneous_Symbols_and_Arrows" */
    0, -384,            /* 446: "", "Miscellaneous_Technical" */
    0, -389,            /* 448: "", "Modifier_Tone_Letters" */
    0, -391,            /* 450: "", "Mongolian" */
    0, -392,            /* 452: "", "Musical_Symbols" */
    0, -393,            /* 454: "", "Myanmar" */
    0, -421,            /* 456: "", "New_Tai_Lue" */
    0, -425,            /* 458: "", "No_Block" */
    0, -439,            /* 460: "", "Number_Forms" */
    0, -449,            /* 462: "", "Ogham" */
    0, -450,            /* 464: "", "Old_Italic" */
    0, -451,            /* 466: "", "Old_Persian" */
    0, -453,            /* 468: "", "Optical_Character_Recognition" */
    0, -454,            /* 470: "", "Oriya" */
    0, -457,            /* 472: "", "Osmanya" */
    0, -478,            /* 474: "", "Phonetic_Extensions" */
    0, -479,            /* 476: "", "Phonetic_Extensions_Supplement" */
    0, -486,            /* 478: "", "Private_Use_Area" */
    0, -508,            /* 480: "", "Runic" */
    0, -530,            /* 482: "", "Shavian" */
    0, -538,            /* 484: "", "Sinhala" */
    0, -542,            /* 486: "", "Small_Form_Variants" */
    0, -549,            /* 488: "", "Spacing_Modifier_Letters" */
    0, -550,            /* 490: "", "Specials" */
    0, -554,            /* 492: "", "Superscripts_and_Subscripts" */
    0, -555,            /* 494: "", "Supplemental_Arrows-A" */
    0, -556,            /* 496: "", "Supplemental_Arrows-B" */
    0, -557,            /* 498: "", "Supplemental_Mathematical_Operators" */
    0, -558,            /* 500: "", "Supplemental_Punctuation" */
    0, -559,            /* 502: "", "Supplementary_Private_Use_Area-A" */
    0, -560,            /* 504: "", "Supplementary_Private_Use_Area-B" */
    0, -564,            /* 506: "", "Syloti_Nagri" */
    0, -567,            /* 508: "", "Syriac" */
    0, -570,            /* 510: "", "Tagalog" */
    0, -572,            /* 512: "", "Tagbanwa" */
    0, -573,            /* 514: "", "Tags" */
    0, -575,            /* 516: "", "Tai_Le" */
    0, -576,            /* 518: "", "Tai_Xuan_Jing_Symbols" */
    0, -579,            /* 520: "", "Tamil" */
    0, -584,            /* 522: "", "Telugu" */
    0, -591,            /* 524: "", "Thaana" */
    0, -592,            /* 526: "", "Thai" */
    0, -593,            /* 528: "", "Tibetan" */
    0, -595,            /* 530: "", "Tifinagh" */
    0, -605,            /* 532: "", "Ugaritic" */
    0, -608,            /* 534: "", "Unified_Canadian_Aboriginal_Syllabics" */
    0, -619,            /* 536: "", "Variation_Selectors" */
    0, -620,            /* 538: "", "Variation_Selectors_Supplement" */
    0, -622,            /* 540: "", "Vertical_Forms" */
    0, -650,            /* 542: "", "Yijing_Hexagram_Symbols" */
    0, -647,            /* 544: "", "Yi_Radicals" */
    0, -648,            /* 546: "", "Yi_Syllables" */
    412, -435,          /* 548: "NR", "Not_Reordered" */
    447, -464,          /* 550: "OV", "Overlay" */
    11, -39,            /* 552: "ATBL", "Attached_Below_Left" */
    10, -38,            /* 554: "ATB", "Attached_Below" */
    9, -37,             /* 556: "ATAR", "Attached_Above_Right" */
    45, -51,            /* 558: "BL", "Below_Left" */
    40, -50,            /* 560: "B", "Below" */
    47, -52,            /* 562: "BR", "Below_Right" */
    314, -336,          /* 564: "L", "Left" */
    497, -503,          /* 566: "R", "Right" */
    4, -14,             /* 568: "AL", "Above_Left" */
    1, -13,             /* 570: "A", "Above" */
    7, -15,             /* 572: "AR", "Above_Right" */
    142, -164,          /* 574: "DB", "Double_Below" */
    141, -163,          /* 576: "DA", "Double_Above" */
    273, -284,          /* 578: "IS", "Iota_Subscript" */
    410, -437,          /* 580: "NK", "Nukta" */
    295, -298,          /* 582: "KV", "Kana_Voicing" */
    616, -623,          /* 584: "VR", "Virama" */
    666, -98,           /* 586: "can", "Canonical" */
    674, -109,          /* 588: "enc", "Circle" */
    670, -122,          /* 590: "com", "Compat" */
    675, -191,          /* 592: "fin", "Final" */
    676, -194,          /* 594: "font", "Font" */
    677, -196,          /* 596: "fra", "Fraction" */
    681, -280,          /* 598: "init", "Initial" */
    683, -285,          /* 600: "iso", "Isolated" */
    689, -375,          /* 602: "med", "Medial" */
    692, -418,          /* 604: "nar", "Narrow" */
    693, -427,          /* 606: "nb", "Nobreak" */
    698, -430,          /* 608: "none", "None" */
    706, -541,          /* 610: "sml", "Small" */
    708, -551,          /* 612: "sqr", "Square" */
    710, -552,          /* 614: "sub", "Sub" */
    712, -553,          /* 616: "sup", "Super" */
    716, -621,          /* 618: "vert", "Vertical" */
    717, -632,          /* 620: "wide", "Wide" */
    1, -24,             /* 622: "A", "Ambiguous" */
    187, -198,          /* 624: "F", "Fullwidth" */
    230, -235,          /* 626: "H", "Halfwidth" */
    416, -418,          /* 628: "Na", "Narrow" */
    395, -420,          /* 630: "N", "Neutral" */
    625, -632,          /* 632: "W", "Wide" */
    373, -548,          /* 634: "Mc", "Spacing_Mark" */
    474, -124,          /* 636: "Pc", "Connector_Punctuation" */
    105, 126, -669,     /* 638: "Cc", "Control", "cntrl" */
    520, -132,          /* 641: "Sc", "Currency_Symbol" */
    475, -147,          /* 643: "Pd", "Dash_Punctuation" */
    419, 150, -671,     /* 645: "Nd", "Decimal_Number", "digit" */
    374, -175,          /* 648: "Me", "Enclosing_Mark" */
    476, -111,          /* 650: "Pe", "Close_Punctuation" */
    477, -192,          /* 652: "Pf", "Final_Punctuation" */
    106, -195,          /* 654: "Cf", "Format" */
    112, -606,          /* 656: "Cn", "Unassigned" */
    480, -281,          /* 658: "Pi", "Initial_Punctuation" */
    423, -342,          /* 660: "Nl", "Letter_Number" */
    658, -349,          /* 662: "Zl", "Line_Separator" */
    353, -360,          /* 664: "Ll", "Lowercase_Letter" */
    540, -369,          /* 666: "Sm", "Math_Symbol" */
    354, -387,          /* 668: "Lm", "Modifier_Letter" */
    539, -388,          /* 670: "Sk", "Modifier_Symbol" */
    386, -431,          /* 672: "Mn", "Nonspacing_Mark" */
    355, -459,          /* 674: "Lo", "Other_Letter" */
    424, -461,          /* 676: "No", "Other_Number" */
    481, -462,          /* 678: "Po", "Other_Punctuation" */
    543, -463,          /* 680: "So", "Other_Symbol" */
    659, -469,          /* 682: "Zp", "Paragraph_Separator" */
    113, -485,          /* 684: "Co", "Private_Use" */
    660, -547,          /* 686: "Zs", "Space_Separator" */
    487, -452,          /* 688: "Ps", "Open_Punctuation" */
    131, -561,          /* 690: "Cs", "Surrogate" */
    362, -596,          /* 692: "Lt", "Titlecase_Letter" */
    363, -613,          /* 694: "Lu", "Uppercase_Letter" */
    81, -458,           /* 696: "C", "Other" */
    315, -104,          /* 698: "LC", "Cased_Letter" */
    314, -341,          /* 700: "L", "Letter" */
    364, -367,          /* 702: "M", "Mark" */
    395, -438,          /* 704: "N", "Number" */
    465, 488, -701,     /* 706: "P", "Punctuation", "punct" */
    510, -565,          /* 709: "S", "Symbol" */
    653, -529,          /* 711: "Z", "Separator" */
    314, -335,          /* 713: "L", "Leading_Jamo" */
    321, -322,          /* 715: "LVT", "LVT_Syllable" */
    320, -323,          /* 717: "LV", "LV_Syllable" */
    396, -434,          /* 719: "NA", "Not_Applicable" */
    569, -599,          /* 721: "T", "Trailing_Jamo" */
    615, -624,          /* 723: "V", "Vowel_Jamo" */
    0, -18,             /* 725: "", "Ain" */
    0, -19,             /* 727: "", "Alaph" */
    0, -20,             /* 729: "", "Alef" */
    0, -49,             /* 731: "", "Beh" */
    0, -55,             /* 733: "", "Beth" */
    0, -144,            /* 735: "", "Dal" */
    0, -145,            /* 737: "", "Dalath_Rish" */
    0, -167,            /* 739: "", "E" */
    0, -189,            /* 741: "", "Fe" */
    0, -190,            /* 743: "", "Feh" */
    0, -193,            /* 745: "", "Final_Semkath" */
    0, -201,            /* 747: "", "Gaf" */
    0, -202,            /* 749: "", "Gamal" */
    0, -234,            /* 751: "", "Hah" */
    0, -237,            /* 753: "", "Hamza_On_Heh_Goal" */
    0, -248,            /* 755: "", "He" */
    0, -251,            /* 757: "", "Heh" */
    0, -252,            /* 759: "", "Heh_Goal" */
    0, -253,            /* 761: "", "Heth" */
    0, -296,            /* 763: "", "Kaf" */
    0, -302,            /* 765: "", "Kaph" */
    0, -306,            /* 767: "", "Khaph" */
    0, -313,            /* 769: "", "Knotted_Heh" */
    0, -324,            /* 771: "", "Lam" */
    0, -325,            /* 773: "", "Lamadh" */
    0, -376,            /* 775: "", "Meem" */
    0, -379,            /* 777: "", "Mim" */
    0, -433,            /* 779: "", "Noon" */
    0, -426,            /* 781: "", "No_Joining_Group" */
    0, -443,            /* 783: "", "Nun" */
    0, -476,            /* 785: "", "Pe" */
    0, -493,            /* 787: "", "Qaf" */
    0, -494,            /* 789: "", "Qaph" */
    0, -501,            /* 791: "", "Reh" */
    0, -502,            /* 793: "", "Reversed_Pe" */
    0, -518,            /* 795: "", "Sad" */
    0, -519,            /* 797: "", "Sadhe" */
    0, -522,            /* 799: "", "Seen" */
    0, -525,            /* 801: "", "Semkath" */
    0, -532,            /* 803: "", "Shin" */
    0, -562,            /* 805: "", "Swash_Kaf" */
    0, -568,            /* 807: "", "Syriac_Waw" */
    0, -574,            /* 809: "", "Tah" */
    0, -581,            /* 811: "", "Taw" */
    0, -582,            /* 813: "", "Teh_Marbuta" */
    0, -587,            /* 815: "", "Teth" */
    0, -630,            /* 817: "", "Waw" */
    0, -642,            /* 819: "", "Yeh" */
    0, -643,            /* 821: "", "Yeh_Barree" */
    0, -644,            /* 823: "", "Yeh_With_Tail" */
    0, -651,            /* 825: "", "Yudh" */
    0, -652,            /* 827: "", "Yudh_He" */
    0, -656,            /* 829: "", "Zain" */
    0, -657,            /* 831: "", "Zhain" */
    140, -166,          /* 833: "D", "Dual_Joining" */
    81, -291,           /* 835: "C", "Join_Causing" */
    314, -337,          /* 837: "L", "Left_Joining" */
    602, -428,          /* 839: "U", "Non_Joining" */
    497, -504,          /* 841: "R", "Right_Joining" */
    569, -600,          /* 843: "T", "Transparent" */
    4, -22,             /* 845: "AL", "Alphabetic" */
    3, -24,             /* 847: "AI", "Ambiguous" */
    42, -72,            /* 849: "BA", "Break_After" */
    43, -73,            /* 851: "BB", "Break_Before" */
    41, -74,            /* 853: "B2", "Break_Both" */
    517, -75,           /* 855: "SY", "Break_Symbols" */
    95, -101,           /* 857: "CR", "Carriage_Return" */
    93, -111,           /* 859: "CL", "Close_Punctuation" */
    94, -118,           /* 861: "CM", "Combining_Mark" */
    511, -123,          /* 863: "SA", "Complex_Context" */
    82, -125,           /* 865: "CB", "Contingent_Break" */
    171, -183,          /* 867: "EX", "Exclamation" */
    200, -212,          /* 869: "GL", "Glue" */
    231, -231,          /* 871: "H2", "H2" */
    232, -232,          /* 873: "H3", "H3" */
    233, -261,          /* 875: "HY", "Hyphen" */
    262, -276,          /* 877: "ID", "Ideographic" */
    273, -278,          /* 879: "IS", "Infix_Numeric" */
    271, 282, -283,     /* 881: "IN", "Inseparable", "Inseperable" */
    287, -287,          /* 884: "JL", "JL" */
    288, -288,          /* 886: "JT", "JT" */
    289, -289,          /* 888: "JV", "JV" */
    316, -348,          /* 890: "LF", "Line_Feed" */
    44, -366,           /* 892: "BK", "Mandatory_Break" */
    411, -422,          /* 894: "NL", "Next_Line" */
    413, -432,          /* 896: "NS", "Nonstarter" */
    415, -440,          /* 898: "NU", "Numeric" */
    446, -452,          /* 900: "OP", "Open_Punctuation" */
    467, -483,          /* 902: "PO", "Postfix_Numeric" */
    468, -484,          /* 904: "PR", "Prefix_Numeric" */
    490, -495,          /* 906: "QU", "Quotation" */
    515, -546,          /* 908: "SP", "Space" */
    514, -561,          /* 910: "SG", "Surrogate" */
    639, -610,          /* 912: "XX", "Unknown" */
    627, -634,          /* 914: "WJ", "Word_Joiner" */
    654, -655,          /* 916: "ZW", "ZWSpace" */
    148, -149,          /* 918: "De", "Decimal" */
    158, -161,          /* 920: "Di", "Digit" */
    430, -430,          /* 922: "None", "None" */
    436, -440,          /* 924: "Nu", "Numeric" */
    27, -28,            /* 926: "Arab", "Arabic" */
    35, -34,            /* 928: "Armn", "Armenian" */
    53, -54,            /* 930: "Beng", "Bengali" */
    64, -65,            /* 932: "Bopo", "Bopomofo" */
    69, -70,            /* 934: "Brai", "Braille" */
    76, -77,            /* 936: "Bugi", "Buginese" */
    78, -79,            /* 938: "Buhd", "Buhid" */
    100, -97,           /* 940: "Cans", "Canadian_Aboriginal" */
    107, -108,          /* 942: "Cher", "Cherokee" */
    661, -119,          /* 944: "Zyyy", "Common" */
    128, 129, -491,     /* 946: "Copt", "Coptic", "Qaac" */
    130, -134,          /* 949: "Cprt", "Cypriot" */
    139, -136,          /* 951: "Cyrl", "Cyrillic" */
    165, -155,          /* 953: "Dsrt", "Deseret" */
    156, -157,          /* 955: "Deva", "Devanagari" */
    176, -177,          /* 957: "Ethi", "Ethiopic" */
    207, -208,          /* 959: "Geor", "Georgian" */
    210, -211,          /* 961: "Glag", "Glagolitic" */
    213, -214,          /* 963: "Goth", "Gothic" */
    225, -222,          /* 965: "Grek", "Greek" */
    227, -226,          /* 967: "Gujr", "Gujarati" */
    229, -228,          /* 969: "Guru", "Gurmukhi" */
    245, -238,          /* 971: "Hani", "Han" */
    239, -240,          /* 973: "Hang", "Hangul" */
    246, -247,          /* 975: "Hano", "Hanunoo" */
    249, -250,          /* 977: "Hebr", "Hebrew" */
    258, -259,          /* 979: "Hira", "Hiragana" */
    492, -279,          /* 981: "Qaai", "Inherited" */
    312, -301,          /* 983: "Knda", "Kannada" */
    297, -303,          /* 985: "Kana", "Katakana" */
    260, -304,          /* 987: "Hrkt", "Katakana_Or_Hiragana" */
    307, -308,          /* 989: "Khar", "Kharoshthi" */
    311, -309,          /* 991: "Khmr", "Khmer" */
    327, -326,          /* 993: "Laoo", "Lao" */
    333, -328,          /* 995: "Latn", "Latin" */
    344, -345,          /* 997: "Limb", "Limbu" */
    346, -350,          /* 999: "Linb", "Linear_B" */
    385, -365,          /* 1001: "Mlym", "Malayalam" */
    390, -391,          /* 1003: "Mong", "Mongolian" */
    394, -393,          /* 1005: "Mymr", "Myanmar" */
    578, -421,          /* 1007: "Talu", "New_Tai_Lue" */
    448, -449,          /* 1009: "Ogam", "Ogham" */
    286, -450,          /* 1011: "Ital", "Old_Italic" */
    640, -451,          /* 1013: "Xpeo", "Old_Persian" */
    455, -454,          /* 1015: "Orya", "Oriya" */
    456, -457,          /* 1017: "Osma", "Osmanya" */
    509, -508,          /* 1019: "Runr", "Runic" */
    531, -530,          /* 1021: "Shaw", "Shavian" */
    537, -538,          /* 1023: "Sinh", "Sinhala" */
    563, -564,          /* 1025: "Sylo", "Syloti_Nagri" */
    566, -567,          /* 1027: "Syrc", "Syriac" */
    589, -570,          /* 1029: "Tglg", "Tagalog" */
    571, -572,          /* 1031: "Tagb", "Tagbanwa" */
    577, -575,          /* 1033: "Tale", "Tai_Le" */
    580, -579,          /* 1035: "Taml", "Tamil" */
    583, -584,          /* 1037: "Telu", "Telugu" */
    590, -591,          /* 1039: "Thaa", "Thaana" */
    592, -592,          /* 1041: "Thai", "Thai" */
    594, -593,          /* 1043: "Tibt", "Tibetan" */
    588, -595,          /* 1045: "Tfng", "Tifinagh" */
    604, -605,          /* 1047: "Ugar", "Ugaritic" */
    649, -646,          /* 1049: "Yiii", "Yi" */
};

#define MAX_NAMES_PER_GROUP 3

const int32_t VALUES_GCB_COUNT = 10;

const Alias VALUES_GCB[] = {
    Alias((int32_t) U_GCB_CONTROL, 0),
    Alias((int32_t) U_GCB_CR, 2),
    Alias((int32_t) U_GCB_EXTEND, 4),
    Alias((int32_t) U_GCB_L, 6),
    Alias((int32_t) U_GCB_LF, 8),
    Alias((int32_t) U_GCB_LV, 10),
    Alias((int32_t) U_GCB_LVT, 12),
    Alias((int32_t) U_GCB_OTHER, 14),
    Alias((int32_t) U_GCB_T, 16),
    Alias((int32_t) U_GCB_V, 18),
};

const int32_t VALUES_NFC_QC_COUNT = 3;

const Alias VALUES_NFC_QC[] = {
    Alias((int32_t) UNORM_MAYBE, 20),
    Alias((int32_t) UNORM_NO, 22),
    Alias((int32_t) UNORM_YES, 24),
};

const int32_t VALUES_NFD_QC_COUNT = 2;

const Alias VALUES_NFD_QC[] = {
    Alias((int32_t) UNORM_NO, 22),
    Alias((int32_t) UNORM_YES, 24),
};

const int32_t VALUES_NFKC_QC_COUNT = 3;

const Alias VALUES_NFKC_QC[] = {
    Alias((int32_t) UNORM_MAYBE, 20),
    Alias((int32_t) UNORM_NO, 22),
    Alias((int32_t) UNORM_YES, 24),
};

const int32_t VALUES_NFKD_QC_COUNT = 2;

const Alias VALUES_NFKD_QC[] = {
    Alias((int32_t) UNORM_NO, 22),
    Alias((int32_t) UNORM_YES, 24),
};

const int32_t VALUES_SB_COUNT = 11;

const Alias VALUES_SB[] = {
    Alias((int32_t) U_SB_ATERM, 26),
    Alias((int32_t) U_SB_CLOSE, 28),
    Alias((int32_t) U_SB_FORMAT, 30),
    Alias((int32_t) U_SB_LOWER, 32),
    Alias((int32_t) U_SB_NUMERIC, 34),
    Alias((int32_t) U_SB_OLETTER, 36),
    Alias((int32_t) U_SB_OTHER, 14),
    Alias((int32_t) U_SB_SEP, 38),
    Alias((int32_t) U_SB_SP, 40),
    Alias((int32_t) U_SB_STERM, 42),
    Alias((int32_t) U_SB_UPPER, 44),
};

const int32_t VALUES_WB_COUNT = 7;

const Alias VALUES_WB[] = {
    Alias((int32_t) U_WB_ALETTER, 46),
    Alias((int32_t) U_WB_FORMAT, 30),
    Alias((int32_t) U_WB_KATAKANA, 48),
    Alias((int32_t) U_WB_MIDLETTER, 50),
    Alias((int32_t) U_WB_MIDNUM, 52),
    Alias((int32_t) U_WB_NUMERIC, 34),
    Alias((int32_t) U_WB_OTHER, 14),
};

const int32_t VALUES_bc_COUNT = 19;

const Alias VALUES_bc[] = {
    Alias((int32_t) U_ARABIC_NUMBER, 213),
    Alias((int32_t) U_BLOCK_SEPARATOR, 215),
    Alias((int32_t) U_BOUNDARY_NEUTRAL, 217),
    Alias((int32_t) U_COMMON_NUMBER_SEPARATOR, 219),
    Alias((int32_t) U_DIR_NON_SPACING_MARK, 221),
    Alias((int32_t) U_EUROPEAN_NUMBER, 223),
    Alias((int32_t) U_EUROPEAN_NUMBER_SEPARATOR, 225),
    Alias((int32_t) U_EUROPEAN_NUMBER_TERMINATOR, 227),
    Alias((int32_t) U_LEFT_TO_RIGHT, 229),
    Alias((int32_t) U_LEFT_TO_RIGHT_EMBEDDING, 231),
    Alias((int32_t) U_LEFT_TO_RIGHT_OVERRIDE, 233),
    Alias((int32_t) U_OTHER_NEUTRAL, 235),
    Alias((int32_t) U_POP_DIRECTIONAL_FORMAT, 237),
    Alias((int32_t) U_RIGHT_TO_LEFT, 239),
    Alias((int32_t) U_RIGHT_TO_LEFT_ARABIC, 241),
    Alias((int32_t) U_RIGHT_TO_LEFT_EMBEDDING, 243),
    Alias((int32_t) U_RIGHT_TO_LEFT_OVERRIDE, 245),
    Alias((int32_t) U_SEGMENT_SEPARATOR, 247),
    Alias((int32_t) U_WHITE_SPACE_NEUTRAL, 249),
};

const int32_t VALUES_binprop_COUNT = 2;

const Alias VALUES_binprop[] = {
    Alias((int32_t) 0, 251),
    Alias((int32_t) 1, 253),
};

const int32_t VALUES_blk_COUNT = 146;

const Alias VALUES_blk[] = {
    Alias((int32_t) UBLOCK_AEGEAN_NUMBERS, 255),
    Alias((int32_t) UBLOCK_ALPHABETIC_PRESENTATION_FORMS, 257),
    Alias((int32_t) UBLOCK_ANCIENT_GREEK_MUSICAL_NOTATION, 259),
    Alias((int32_t) UBLOCK_ANCIENT_GREEK_NUMBERS, 261),
    Alias((int32_t) UBLOCK_ARABIC, 263),
    Alias((int32_t) UBLOCK_ARABIC_PRESENTATION_FORMS_A, 265),
    Alias((int32_t) UBLOCK_ARABIC_PRESENTATION_FORMS_B, 267),
    Alias((int32_t) UBLOCK_ARABIC_SUPPLEMENT, 269),
    Alias((int32_t) UBLOCK_ARMENIAN, 271),
    Alias((int32_t) UBLOCK_ARROWS, 273),
    Alias((int32_t) UBLOCK_BASIC_LATIN, 275),
    Alias((int32_t) UBLOCK_BENGALI, 277),
    Alias((int32_t) UBLOCK_BLOCK_ELEMENTS, 279),
    Alias((int32_t) UBLOCK_BOPOMOFO, 281),
    Alias((int32_t) UBLOCK_BOPOMOFO_EXTENDED, 283),
    Alias((int32_t) UBLOCK_BOX_DRAWING, 285),
    Alias((int32_t) UBLOCK_BRAILLE_PATTERNS, 287),
    Alias((int32_t) UBLOCK_BUGINESE, 289),
    Alias((int32_t) UBLOCK_BUHID, 291),
    Alias((int32_t) UBLOCK_BYZANTINE_MUSICAL_SYMBOLS, 293),
    Alias((int32_t) UBLOCK_CHEROKEE, 295),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY, 297),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY_FORMS, 299),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY_IDEOGRAPHS, 301),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT, 303),
    Alias((int32_t) UBLOCK_CJK_RADICALS_SUPPLEMENT, 305),
    Alias((int32_t) UBLOCK_CJK_STROKES, 307),
    Alias((int32_t) UBLOCK_CJK_SYMBOLS_AND_PUNCTUATION, 309),
    Alias((int32_t) UBLOCK_CJK_UNIFIED_IDEOGRAPHS, 311),
    Alias((int32_t) UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A, 313),
    Alias((int32_t) UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B, 315),
    Alias((int32_t) UBLOCK_COMBINING_DIACRITICAL_MARKS, 317),
    Alias((int32_t) UBLOCK_COMBINING_DIACRITICAL_MARKS_SUPPLEMENT, 319),
    Alias((int32_t) UBLOCK_COMBINING_HALF_MARKS, 321),
    Alias((int32_t) UBLOCK_COMBINING_MARKS_FOR_SYMBOLS, 323),
    Alias((int32_t) UBLOCK_CONTROL_PICTURES, 325),
    Alias((int32_t) UBLOCK_COPTIC, 327),
    Alias((int32_t) UBLOCK_CURRENCY_SYMBOLS, 329),
    Alias((int32_t) UBLOCK_CYPRIOT_SYLLABARY, 331),
    Alias((int32_t) UBLOCK_CYRILLIC, 333),
    Alias((int32_t) UBLOCK_CYRILLIC_SUPPLEMENT, 335),
    Alias((int32_t) UBLOCK_DESERET, 338),
    Alias((int32_t) UBLOCK_DEVANAGARI, 340),
    Alias((int32_t) UBLOCK_DINGBATS, 342),
    Alias((int32_t) UBLOCK_ENCLOSED_ALPHANUMERICS, 344),
    Alias((int32_t) UBLOCK_ENCLOSED_CJK_LETTERS_AND_MONTHS, 346),
    Alias((int32_t) UBLOCK_ETHIOPIC, 348),
    Alias((int32_t) UBLOCK_ETHIOPIC_EXTENDED, 350),
    Alias((int32_t) UBLOCK_ETHIOPIC_SUPPLEMENT, 352),
    Alias((int32_t) UBLOCK_GENERAL_PUNCTUATION, 354),
    Alias((int32_t) UBLOCK_GEOMETRIC_SHAPES, 356),
    Alias((int32_t) UBLOCK_GEORGIAN, 358),
    Alias((int32_t) UBLOCK_GEORGIAN_SUPPLEMENT, 360),
    Alias((int32_t) UBLOCK_GLAGOLITIC, 362),
    Alias((int32_t) UBLOCK_GOTHIC, 364),
    Alias((int32_t) UBLOCK_GREEK, 366),
    Alias((int32_t) UBLOCK_GREEK_EXTENDED, 368),
    Alias((int32_t) UBLOCK_GUJARATI, 370),
    Alias((int32_t) UBLOCK_GURMUKHI, 372),
    Alias((int32_t) UBLOCK_HALFWIDTH_AND_FULLWIDTH_FORMS, 374),
    Alias((int32_t) UBLOCK_HANGUL_COMPATIBILITY_JAMO, 376),
    Alias((int32_t) UBLOCK_HANGUL_JAMO, 378),
    Alias((int32_t) UBLOCK_HANGUL_SYLLABLES, 380),
    Alias((int32_t) UBLOCK_HANUNOO, 382),
    Alias((int32_t) UBLOCK_HEBREW, 384),
    Alias((int32_t) UBLOCK_HIGH_PRIVATE_USE_SURROGATES, 386),
    Alias((int32_t) UBLOCK_HIGH_SURROGATES, 388),
    Alias((int32_t) UBLOCK_HIRAGANA, 390),
    Alias((int32_t) UBLOCK_IDEOGRAPHIC_DESCRIPTION_CHARACTERS, 392),
    Alias((int32_t) UBLOCK_IPA_EXTENSIONS, 394),
    Alias((int32_t) UBLOCK_KANBUN, 396),
    Alias((int32_t) UBLOCK_KANGXI_RADICALS, 398),
    Alias((int32_t) UBLOCK_KANNADA, 400),
    Alias((int32_t) UBLOCK_KATAKANA, 402),
    Alias((int32_t) UBLOCK_KATAKANA_PHONETIC_EXTENSIONS, 404),
    Alias((int32_t) UBLOCK_KHAROSHTHI, 406),
    Alias((int32_t) UBLOCK_KHMER, 408),
    Alias((int32_t) UBLOCK_KHMER_SYMBOLS, 410),
    Alias((int32_t) UBLOCK_LAO, 412),
    Alias((int32_t) UBLOCK_LATIN_1_SUPPLEMENT, 414),
    Alias((int32_t) UBLOCK_LATIN_EXTENDED_A, 416),
    Alias((int32_t) UBLOCK_LATIN_EXTENDED_ADDITIONAL, 418),
    Alias((int32_t) UBLOCK_LATIN_EXTENDED_B, 420),
    Alias((int32_t) UBLOCK_LETTERLIKE_SYMBOLS, 422),
    Alias((int32_t) UBLOCK_LIMBU, 424),
    Alias((int32_t) UBLOCK_LINEAR_B_IDEOGRAMS, 426),
    Alias((int32_t) UBLOCK_LINEAR_B_SYLLABARY, 428),
    Alias((int32_t) UBLOCK_LOW_SURROGATES, 430),
    Alias((int32_t) UBLOCK_MALAYALAM, 432),
    Alias((int32_t) UBLOCK_MATHEMATICAL_ALPHANUMERIC_SYMBOLS, 434),
    Alias((int32_t) UBLOCK_MATHEMATICAL_OPERATORS, 436),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A, 438),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B, 440),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_SYMBOLS, 442),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_SYMBOLS_AND_ARROWS, 444),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_TECHNICAL, 446),
    Alias((int32_t) UBLOCK_MODIFIER_TONE_LETTERS, 448),
    Alias((int32_t) UBLOCK_MONGOLIAN, 450),
    Alias((int32_t) UBLOCK_MUSICAL_SYMBOLS, 452),
    Alias((int32_t) UBLOCK_MYANMAR, 454),
    Alias((int32_t) UBLOCK_NEW_TAI_LUE, 456),
    Alias((int32_t) UBLOCK_NO_BLOCK, 458),
    Alias((int32_t) UBLOCK_NUMBER_FORMS, 460),
    Alias((int32_t) UBLOCK_OGHAM, 462),
    Alias((int32_t) UBLOCK_OLD_ITALIC, 464),
    Alias((int32_t) UBLOCK_OLD_PERSIAN, 466),
    Alias((int32_t) UBLOCK_OPTICAL_CHARACTER_RECOGNITION, 468),
    Alias((int32_t) UBLOCK_ORIYA, 470),
    Alias((int32_t) UBLOCK_OSMANYA, 472),
    Alias((int32_t) UBLOCK_PHONETIC_EXTENSIONS, 474),
    Alias((int32_t) UBLOCK_PHONETIC_EXTENSIONS_SUPPLEMENT, 476),
    Alias((int32_t) UBLOCK_PRIVATE_USE_AREA, 478),
    Alias((int32_t) UBLOCK_RUNIC, 480),
    Alias((int32_t) UBLOCK_SHAVIAN, 482),
    Alias((int32_t) UBLOCK_SINHALA, 484),
    Alias((int32_t) UBLOCK_SMALL_FORM_VARIANTS, 486),
    Alias((int32_t) UBLOCK_SPACING_MODIFIER_LETTERS, 488),
    Alias((int32_t) UBLOCK_SPECIALS, 490),
    Alias((int32_t) UBLOCK_SUPERSCRIPTS_AND_SUBSCRIPTS, 492),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_ARROWS_A, 494),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_ARROWS_B, 496),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_MATHEMATICAL_OPERATORS, 498),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_PUNCTUATION, 500),
    Alias((int32_t) UBLOCK_SUPPLEMENTARY_PRIVATE_USE_AREA_A, 502),
    Alias((int32_t) UBLOCK_SUPPLEMENTARY_PRIVATE_USE_AREA_B, 504),
    Alias((int32_t) UBLOCK_SYLOTI_NAGRI, 506),
    Alias((int32_t) UBLOCK_SYRIAC, 508),
    Alias((int32_t) UBLOCK_TAGALOG, 510),
    Alias((int32_t) UBLOCK_TAGBANWA, 512),
    Alias((int32_t) UBLOCK_TAGS, 514),
    Alias((int32_t) UBLOCK_TAI_LE, 516),
    Alias((int32_t) UBLOCK_TAI_XUAN_JING_SYMBOLS, 518),
    Alias((int32_t) UBLOCK_TAMIL, 520),
    Alias((int32_t) UBLOCK_TELUGU, 522),
    Alias((int32_t) UBLOCK_THAANA, 524),
    Alias((int32_t) UBLOCK_THAI, 526),
    Alias((int32_t) UBLOCK_TIBETAN, 528),
    Alias((int32_t) UBLOCK_TIFINAGH, 530),
    Alias((int32_t) UBLOCK_UGARITIC, 532),
    Alias((int32_t) UBLOCK_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS, 534),
    Alias((int32_t) UBLOCK_VARIATION_SELECTORS, 536),
    Alias((int32_t) UBLOCK_VARIATION_SELECTORS_SUPPLEMENT, 538),
    Alias((int32_t) UBLOCK_VERTICAL_FORMS, 540),
    Alias((int32_t) UBLOCK_YIJING_HEXAGRAM_SYMBOLS, 542),
    Alias((int32_t) UBLOCK_YI_RADICALS, 544),
    Alias((int32_t) UBLOCK_YI_SYLLABLES, 546),
};

const int32_t VALUES_ccc_COUNT = 19;

const Alias VALUES_ccc[] = {
    Alias((int32_t) 0, 548),
    Alias((int32_t) 1, 550),
    Alias((int32_t) 200, 552),
    Alias((int32_t) 202, 554),
    Alias((int32_t) 216, 556),
    Alias((int32_t) 218, 558),
    Alias((int32_t) 220, 560),
    Alias((int32_t) 222, 562),
    Alias((int32_t) 224, 564),
    Alias((int32_t) 226, 566),
    Alias((int32_t) 228, 568),
    Alias((int32_t) 230, 570),
    Alias((int32_t) 232, 572),
    Alias((int32_t) 233, 574),
    Alias((int32_t) 234, 576),
    Alias((int32_t) 240, 578),
    Alias((int32_t) 7, 580),
    Alias((int32_t) 8, 582),
    Alias((int32_t) 9, 584),
};

const int32_t VALUES_dt_COUNT = 18;

const Alias VALUES_dt[] = {
    Alias((int32_t) U_DT_CANONICAL, 586),
    Alias((int32_t) U_DT_CIRCLE, 588),
    Alias((int32_t) U_DT_COMPAT, 590),
    Alias((int32_t) U_DT_FINAL, 592),
    Alias((int32_t) U_DT_FONT, 594),
    Alias((int32_t) U_DT_FRACTION, 596),
    Alias((int32_t) U_DT_INITIAL, 598),
    Alias((int32_t) U_DT_ISOLATED, 600),
    Alias((int32_t) U_DT_MEDIAL, 602),
    Alias((int32_t) U_DT_NARROW, 604),
    Alias((int32_t) U_DT_NOBREAK, 606),
    Alias((int32_t) U_DT_NONE, 608),
    Alias((int32_t) U_DT_SMALL, 610),
    Alias((int32_t) U_DT_SQUARE, 612),
    Alias((int32_t) U_DT_SUB, 614),
    Alias((int32_t) U_DT_SUPER, 616),
    Alias((int32_t) U_DT_VERTICAL, 618),
    Alias((int32_t) U_DT_WIDE, 620),
};

const int32_t VALUES_ea_COUNT = 6;

const Alias VALUES_ea[] = {
    Alias((int32_t) U_EA_AMBIGUOUS, 622),
    Alias((int32_t) U_EA_FULLWIDTH, 624),
    Alias((int32_t) U_EA_HALFWIDTH, 626),
    Alias((int32_t) U_EA_NARROW, 628),
    Alias((int32_t) U_EA_NEUTRAL, 630),
    Alias((int32_t) U_EA_WIDE, 632),
};

const int32_t VALUES_gc_COUNT = 30;

const Alias VALUES_gc[] = {
    Alias((int32_t) U_COMBINING_SPACING_MARK, 634),
    Alias((int32_t) U_CONNECTOR_PUNCTUATION, 636),
    Alias((int32_t) U_CONTROL_CHAR, 638),
    Alias((int32_t) U_CURRENCY_SYMBOL, 641),
    Alias((int32_t) U_DASH_PUNCTUATION, 643),
    Alias((int32_t) U_DECIMAL_DIGIT_NUMBER, 645),
    Alias((int32_t) U_ENCLOSING_MARK, 648),
    Alias((int32_t) U_END_PUNCTUATION, 650),
    Alias((int32_t) U_FINAL_PUNCTUATION, 652),
    Alias((int32_t) U_FORMAT_CHAR, 654),
    Alias((int32_t) U_GENERAL_OTHER_TYPES, 656),
    Alias((int32_t) U_INITIAL_PUNCTUATION, 658),
    Alias((int32_t) U_LETTER_NUMBER, 660),
    Alias((int32_t) U_LINE_SEPARATOR, 662),
    Alias((int32_t) U_LOWERCASE_LETTER, 664),
    Alias((int32_t) U_MATH_SYMBOL, 666),
    Alias((int32_t) U_MODIFIER_LETTER, 668),
    Alias((int32_t) U_MODIFIER_SYMBOL, 670),
    Alias((int32_t) U_NON_SPACING_MARK, 672),
    Alias((int32_t) U_OTHER_LETTER, 674),
    Alias((int32_t) U_OTHER_NUMBER, 676),
    Alias((int32_t) U_OTHER_PUNCTUATION, 678),
    Alias((int32_t) U_OTHER_SYMBOL, 680),
    Alias((int32_t) U_PARAGRAPH_SEPARATOR, 682),
    Alias((int32_t) U_PRIVATE_USE_CHAR, 684),
    Alias((int32_t) U_SPACE_SEPARATOR, 686),
    Alias((int32_t) U_START_PUNCTUATION, 688),
    Alias((int32_t) U_SURROGATE, 690),
    Alias((int32_t) U_TITLECASE_LETTER, 692),
    Alias((int32_t) U_UPPERCASE_LETTER, 694),
};

const int32_t VALUES_gcm_COUNT = 38;

const Alias VALUES_gcm[] = {
    Alias((int32_t) U_GC_CC_MASK, 638),
    Alias((int32_t) U_GC_CF_MASK, 654),
    Alias((int32_t) U_GC_CN_MASK, 656),
    Alias((int32_t) U_GC_CO_MASK, 684),
    Alias((int32_t) U_GC_CS_MASK, 690),
    Alias((int32_t) U_GC_C_MASK, 696),
    Alias((int32_t) U_GC_LC_MASK, 698),
    Alias((int32_t) U_GC_LL_MASK, 664),
    Alias((int32_t) U_GC_LM_MASK, 668),
    Alias((int32_t) U_GC_LO_MASK, 674),
    Alias((int32_t) U_GC_LT_MASK, 692),
    Alias((int32_t) U_GC_LU_MASK, 694),
    Alias((int32_t) U_GC_L_MASK, 700),
    Alias((int32_t) U_GC_MC_MASK, 634),
    Alias((int32_t) U_GC_ME_MASK, 648),
    Alias((int32_t) U_GC_MN_MASK, 672),
    Alias((int32_t) U_GC_M_MASK, 702),
    Alias((int32_t) U_GC_ND_MASK, 645),
    Alias((int32_t) U_GC_NL_MASK, 660),
    Alias((int32_t) U_GC_NO_MASK, 676),
    Alias((int32_t) U_GC_N_MASK, 704),
    Alias((int32_t) U_GC_PC_MASK, 636),
    Alias((int32_t) U_GC_PD_MASK, 643),
    Alias((int32_t) U_GC_PE_MASK, 650),
    Alias((int32_t) U_GC_PF_MASK, 652),
    Alias((int32_t) U_GC_PI_MASK, 658),
    Alias((int32_t) U_GC_PO_MASK, 678),
    Alias((int32_t) U_GC_PS_MASK, 688),
    Alias((int32_t) U_GC_P_MASK, 706),
    Alias((int32_t) U_GC_SC_MASK, 641),
    Alias((int32_t) U_GC_SK_MASK, 670),
    Alias((int32_t) U_GC_SM_MASK, 666),
    Alias((int32_t) U_GC_SO_MASK, 680),
    Alias((int32_t) U_GC_S_MASK, 709),
    Alias((int32_t) U_GC_ZL_MASK, 662),
    Alias((int32_t) U_GC_ZP_MASK, 682),
    Alias((int32_t) U_GC_ZS_MASK, 686),
    Alias((int32_t) U_GC_Z_MASK, 711),
};

const int32_t VALUES_hst_COUNT = 6;

const Alias VALUES_hst[] = {
    Alias((int32_t) U_HST_LEADING_JAMO, 713),
    Alias((int32_t) U_HST_LVT_SYLLABLE, 715),
    Alias((int32_t) U_HST_LV_SYLLABLE, 717),
    Alias((int32_t) U_HST_NOT_APPLICABLE, 719),
    Alias((int32_t) U_HST_TRAILING_JAMO, 721),
    Alias((int32_t) U_HST_VOWEL_JAMO, 723),
};

const int32_t VALUES_jg_COUNT = 54;

const Alias VALUES_jg[] = {
    Alias((int32_t) U_JG_AIN, 725),
    Alias((int32_t) U_JG_ALAPH, 727),
    Alias((int32_t) U_JG_ALEF, 729),
    Alias((int32_t) U_JG_BEH, 731),
    Alias((int32_t) U_JG_BETH, 733),
    Alias((int32_t) U_JG_DAL, 735),
    Alias((int32_t) U_JG_DALATH_RISH, 737),
    Alias((int32_t) U_JG_E, 739),
    Alias((int32_t) U_JG_FE, 741),
    Alias((int32_t) U_JG_FEH, 743),
    Alias((int32_t) U_JG_FINAL_SEMKATH, 745),
    Alias((int32_t) U_JG_GAF, 747),
    Alias((int32_t) U_JG_GAMAL, 749),
    Alias((int32_t) U_JG_HAH, 751),
    Alias((int32_t) U_JG_HAMZA_ON_HEH_GOAL, 753),
    Alias((int32_t) U_JG_HE, 755),
    Alias((int32_t) U_JG_HEH, 757),
    Alias((int32_t) U_JG_HEH_GOAL, 759),
    Alias((int32_t) U_JG_HETH, 761),
    Alias((int32_t) U_JG_KAF, 763),
    Alias((int32_t) U_JG_KAPH, 765),
    Alias((int32_t) U_JG_KHAPH, 767),
    Alias((int32_t) U_JG_KNOTTED_HEH, 769),
    Alias((int32_t) U_JG_LAM, 771),
    Alias((int32_t) U_JG_LAMADH, 773),
    Alias((int32_t) U_JG_MEEM, 775),
    Alias((int32_t) U_JG_MIM, 777),
    Alias((int32_t) U_JG_NOON, 779),
    Alias((int32_t) U_JG_NO_JOINING_GROUP, 781),
    Alias((int32_t) U_JG_NUN, 783),
    Alias((int32_t) U_JG_PE, 785),
    Alias((int32_t) U_JG_QAF, 787),
    Alias((int32_t) U_JG_QAPH, 789),
    Alias((int32_t) U_JG_REH, 791),
    Alias((int32_t) U_JG_REVERSED_PE, 793),
    Alias((int32_t) U_JG_SAD, 795),
    Alias((int32_t) U_JG_SADHE, 797),
    Alias((int32_t) U_JG_SEEN, 799),
    Alias((int32_t) U_JG_SEMKATH, 801),
    Alias((int32_t) U_JG_SHIN, 803),
    Alias((int32_t) U_JG_SWASH_KAF, 805),
    Alias((int32_t) U_JG_SYRIAC_WAW, 807),
    Alias((int32_t) U_JG_TAH, 809),
    Alias((int32_t) U_JG_TAW, 811),
    Alias((int32_t) U_JG_TEH_MARBUTA, 813),
    Alias((int32_t) U_JG_TETH, 815),
    Alias((int32_t) U_JG_WAW, 817),
    Alias((int32_t) U_JG_YEH, 819),
    Alias((int32_t) U_JG_YEH_BARREE, 821),
    Alias((int32_t) U_JG_YEH_WITH_TAIL, 823),
    Alias((int32_t) U_JG_YUDH, 825),
    Alias((int32_t) U_JG_YUDH_HE, 827),
    Alias((int32_t) U_JG_ZAIN, 829),
    Alias((int32_t) U_JG_ZHAIN, 831),
};

const int32_t VALUES_jt_COUNT = 6;

const Alias VALUES_jt[] = {
    Alias((int32_t) U_JT_DUAL_JOINING, 833),
    Alias((int32_t) U_JT_JOIN_CAUSING, 835),
    Alias((int32_t) U_JT_LEFT_JOINING, 837),
    Alias((int32_t) U_JT_NON_JOINING, 839),
    Alias((int32_t) U_JT_RIGHT_JOINING, 841),
    Alias((int32_t) U_JT_TRANSPARENT, 843),
};

const int32_t VALUES_lb_COUNT = 36;

const Alias VALUES_lb[] = {
    Alias((int32_t) U_LB_ALPHABETIC, 845),
    Alias((int32_t) U_LB_AMBIGUOUS, 847),
    Alias((int32_t) U_LB_BREAK_AFTER, 849),
    Alias((int32_t) U_LB_BREAK_BEFORE, 851),
    Alias((int32_t) U_LB_BREAK_BOTH, 853),
    Alias((int32_t) U_LB_BREAK_SYMBOLS, 855),
    Alias((int32_t) U_LB_CARRIAGE_RETURN, 857),
    Alias((int32_t) U_LB_CLOSE_PUNCTUATION, 859),
    Alias((int32_t) U_LB_COMBINING_MARK, 861),
    Alias((int32_t) U_LB_COMPLEX_CONTEXT, 863),
    Alias((int32_t) U_LB_CONTINGENT_BREAK, 865),
    Alias((int32_t) U_LB_EXCLAMATION, 867),
    Alias((int32_t) U_LB_GLUE, 869),
    Alias((int32_t) U_LB_H2, 871),
    Alias((int32_t) U_LB_H3, 873),
    Alias((int32_t) U_LB_HYPHEN, 875),
    Alias((int32_t) U_LB_IDEOGRAPHIC, 877),
    Alias((int32_t) U_LB_INFIX_NUMERIC, 879),
    Alias((int32_t) U_LB_INSEPARABLE, 881),
    Alias((int32_t) U_LB_JL, 884),
    Alias((int32_t) U_LB_JT, 886),
    Alias((int32_t) U_LB_JV, 888),
    Alias((int32_t) U_LB_LINE_FEED, 890),
    Alias((int32_t) U_LB_MANDATORY_BREAK, 892),
    Alias((int32_t) U_LB_NEXT_LINE, 894),
    Alias((int32_t) U_LB_NONSTARTER, 896),
    Alias((int32_t) U_LB_NUMERIC, 898),
    Alias((int32_t) U_LB_OPEN_PUNCTUATION, 900),
    Alias((int32_t) U_LB_POSTFIX_NUMERIC, 902),
    Alias((int32_t) U_LB_PREFIX_NUMERIC, 904),
    Alias((int32_t) U_LB_QUOTATION, 906),
    Alias((int32_t) U_LB_SPACE, 908),
    Alias((int32_t) U_LB_SURROGATE, 910),
    Alias((int32_t) U_LB_UNKNOWN, 912),
    Alias((int32_t) U_LB_WORD_JOINER, 914),
    Alias((int32_t) U_LB_ZWSPACE, 916),
};

const int32_t VALUES_lccc_COUNT = 19;

const Alias VALUES_lccc[] = {
    Alias((int32_t) 0, 548),
    Alias((int32_t) 1, 550),
    Alias((int32_t) 200, 552),
    Alias((int32_t) 202, 554),
    Alias((int32_t) 216, 556),
    Alias((int32_t) 218, 558),
    Alias((int32_t) 220, 560),
    Alias((int32_t) 222, 562),
    Alias((int32_t) 224, 564),
    Alias((int32_t) 226, 566),
    Alias((int32_t) 228, 568),
    Alias((int32_t) 230, 570),
    Alias((int32_t) 232, 572),
    Alias((int32_t) 233, 574),
    Alias((int32_t) 234, 576),
    Alias((int32_t) 240, 578),
    Alias((int32_t) 7, 580),
    Alias((int32_t) 8, 582),
    Alias((int32_t) 9, 584),
};

const int32_t VALUES_nt_COUNT = 4;

const Alias VALUES_nt[] = {
    Alias((int32_t) U_NT_DECIMAL, 918),
    Alias((int32_t) U_NT_DIGIT, 920),
    Alias((int32_t) U_NT_NONE, 922),
    Alias((int32_t) U_NT_NUMERIC, 924),
};

const int32_t VALUES_sc_COUNT = 62;

const Alias VALUES_sc[] = {
    Alias((int32_t) USCRIPT_ARABIC, 926),
    Alias((int32_t) USCRIPT_ARMENIAN, 928),
    Alias((int32_t) USCRIPT_BENGALI, 930),
    Alias((int32_t) USCRIPT_BOPOMOFO, 932),
    Alias((int32_t) USCRIPT_BRAILLE, 934),
    Alias((int32_t) USCRIPT_BUGINESE, 936),
    Alias((int32_t) USCRIPT_BUHID, 938),
    Alias((int32_t) USCRIPT_CANADIAN_ABORIGINAL, 940),
    Alias((int32_t) USCRIPT_CHEROKEE, 942),
    Alias((int32_t) USCRIPT_COMMON, 944),
    Alias((int32_t) USCRIPT_COPTIC, 946),
    Alias((int32_t) USCRIPT_CYPRIOT, 949),
    Alias((int32_t) USCRIPT_CYRILLIC, 951),
    Alias((int32_t) USCRIPT_DESERET, 953),
    Alias((int32_t) USCRIPT_DEVANAGARI, 955),
    Alias((int32_t) USCRIPT_ETHIOPIC, 957),
    Alias((int32_t) USCRIPT_GEORGIAN, 959),
    Alias((int32_t) USCRIPT_GLAGOLITIC, 961),
    Alias((int32_t) USCRIPT_GOTHIC, 963),
    Alias((int32_t) USCRIPT_GREEK, 965),
    Alias((int32_t) USCRIPT_GUJARATI, 967),
    Alias((int32_t) USCRIPT_GURMUKHI, 969),
    Alias((int32_t) USCRIPT_HAN, 971),
    Alias((int32_t) USCRIPT_HANGUL, 973),
    Alias((int32_t) USCRIPT_HANUNOO, 975),
    Alias((int32_t) USCRIPT_HEBREW, 977),
    Alias((int32_t) USCRIPT_HIRAGANA, 979),
    Alias((int32_t) USCRIPT_INHERITED, 981),
    Alias((int32_t) USCRIPT_KANNADA, 983),
    Alias((int32_t) USCRIPT_KATAKANA, 985),
    Alias((int32_t) USCRIPT_KATAKANA_OR_HIRAGANA, 987),
    Alias((int32_t) USCRIPT_KHAROSHTHI, 989),
    Alias((int32_t) USCRIPT_KHMER, 991),
    Alias((int32_t) USCRIPT_LAO, 993),
    Alias((int32_t) USCRIPT_LATIN, 995),
    Alias((int32_t) USCRIPT_LIMBU, 997),
    Alias((int32_t) USCRIPT_LINEAR_B, 999),
    Alias((int32_t) USCRIPT_MALAYALAM, 1001),
    Alias((int32_t) USCRIPT_MONGOLIAN, 1003),
    Alias((int32_t) USCRIPT_MYANMAR, 1005),
    Alias((int32_t) USCRIPT_NEW_TAI_LUE, 1007),
    Alias((int32_t) USCRIPT_OGHAM, 1009),
    Alias((int32_t) USCRIPT_OLD_ITALIC, 1011),
    Alias((int32_t) USCRIPT_OLD_PERSIAN, 1013),
    Alias((int32_t) USCRIPT_ORIYA, 1015),
    Alias((int32_t) USCRIPT_OSMANYA, 1017),
    Alias((int32_t) USCRIPT_RUNIC, 1019),
    Alias((int32_t) USCRIPT_SHAVIAN, 1021),
    Alias((int32_t) USCRIPT_SINHALA, 1023),
    Alias((int32_t) USCRIPT_SYLOTI_NAGRI, 1025),
    Alias((int32_t) USCRIPT_SYRIAC, 1027),
    Alias((int32_t) USCRIPT_TAGALOG, 1029),
    Alias((int32_t) USCRIPT_TAGBANWA, 1031),
    Alias((int32_t) USCRIPT_TAI_LE, 1033),
    Alias((int32_t) USCRIPT_TAMIL, 1035),
    Alias((int32_t) USCRIPT_TELUGU, 1037),
    Alias((int32_t) USCRIPT_THAANA, 1039),
    Alias((int32_t) USCRIPT_THAI, 1041),
    Alias((int32_t) USCRIPT_TIBETAN, 1043),
    Alias((int32_t) USCRIPT_TIFINAGH, 1045),
    Alias((int32_t) USCRIPT_UGARITIC, 1047),
    Alias((int32_t) USCRIPT_YI, 1049),
};

const int32_t VALUES_tccc_COUNT = 19;

const Alias VALUES_tccc[] = {
    Alias((int32_t) 0, 548),
    Alias((int32_t) 1, 550),
    Alias((int32_t) 200, 552),
    Alias((int32_t) 202, 554),
    Alias((int32_t) 216, 556),
    Alias((int32_t) 218, 558),
    Alias((int32_t) 220, 560),
    Alias((int32_t) 222, 562),
    Alias((int32_t) 224, 564),
    Alias((int32_t) 226, 566),
    Alias((int32_t) 228, 568),
    Alias((int32_t) 230, 570),
    Alias((int32_t) 232, 572),
    Alias((int32_t) 233, 574),
    Alias((int32_t) 234, 576),
    Alias((int32_t) 240, 578),
    Alias((int32_t) 7, 580),
    Alias((int32_t) 8, 582),
    Alias((int32_t) 9, 584),
};

const int32_t PROPERTY_COUNT = 80;

const Property PROPERTY[] = {
    Property((int32_t) UCHAR_ALPHABETIC, 54, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_ASCII_HEX_DIGIT, 56, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_BIDI_CONTROL, 58, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_BIDI_MIRRORED, 60, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_CASE_SENSITIVE, 62, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DASH, 64, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DEFAULT_IGNORABLE_CODE_POINT, 66, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DEPRECATED, 68, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DIACRITIC, 70, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_EXTENDER, 72, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_FULL_COMPOSITION_EXCLUSION, 74, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_GRAPHEME_BASE, 76, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_GRAPHEME_EXTEND, 78, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_GRAPHEME_LINK, 80, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_HEX_DIGIT, 82, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_HYPHEN, 84, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_IDEOGRAPHIC, 86, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_IDS_BINARY_OPERATOR, 88, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_IDS_TRINARY_OPERATOR, 90, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_ID_CONTINUE, 92, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_ID_START, 94, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_JOIN_CONTROL, 96, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_LOGICAL_ORDER_EXCEPTION, 98, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_LOWERCASE, 100, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_MATH, 102, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_NFC_INERT, 104, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_NFD_INERT, 106, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_NFKC_INERT, 108, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_NFKD_INERT, 110, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_NONCHARACTER_CODE_POINT, 112, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_PATTERN_SYNTAX, 114, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_PATTERN_WHITE_SPACE, 116, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_QUOTATION_MARK, 118, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_RADICAL, 120, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_SEGMENT_STARTER, 122, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_SOFT_DOTTED, 124, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_S_TERM, 42, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_TERMINAL_PUNCTUATION, 126, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_UNIFIED_IDEOGRAPH, 128, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_UPPERCASE, 130, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_VARIATION_SELECTOR, 132, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_WHITE_SPACE, 134, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_XID_CONTINUE, 137, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_XID_START, 139, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_BIDI_CLASS, 143, VALUES_bc_COUNT, VALUES_bc),
    Property((int32_t) UCHAR_BLOCK, 145, VALUES_blk_COUNT, VALUES_blk),
    Property((int32_t) UCHAR_CANONICAL_COMBINING_CLASS, 147, VALUES_ccc_COUNT, VALUES_ccc),
    Property((int32_t) UCHAR_DECOMPOSITION_TYPE, 149, VALUES_dt_COUNT, VALUES_dt),
    Property((int32_t) UCHAR_EAST_ASIAN_WIDTH, 151, VALUES_ea_COUNT, VALUES_ea),
    Property((int32_t) UCHAR_GENERAL_CATEGORY, 153, VALUES_gc_COUNT, VALUES_gc),
    Property((int32_t) UCHAR_GRAPHEME_CLUSTER_BREAK, 155, VALUES_GCB_COUNT, VALUES_GCB),
    Property((int32_t) UCHAR_HANGUL_SYLLABLE_TYPE, 157, VALUES_hst_COUNT, VALUES_hst),
    Property((int32_t) UCHAR_JOINING_GROUP, 159, VALUES_jg_COUNT, VALUES_jg),
    Property((int32_t) UCHAR_JOINING_TYPE, 161, VALUES_jt_COUNT, VALUES_jt),
    Property((int32_t) UCHAR_LEAD_CANONICAL_COMBINING_CLASS, 163, VALUES_lccc_COUNT, VALUES_lccc),
    Property((int32_t) UCHAR_LINE_BREAK, 165, VALUES_lb_COUNT, VALUES_lb),
    Property((int32_t) UCHAR_NFC_QUICK_CHECK, 167, VALUES_NFC_QC_COUNT, VALUES_NFC_QC),
    Property((int32_t) UCHAR_NFD_QUICK_CHECK, 169, VALUES_NFD_QC_COUNT, VALUES_NFD_QC),
    Property((int32_t) UCHAR_NFKC_QUICK_CHECK, 171, VALUES_NFKC_QC_COUNT, VALUES_NFKC_QC),
    Property((int32_t) UCHAR_NFKD_QUICK_CHECK, 173, VALUES_NFKD_QC_COUNT, VALUES_NFKD_QC),
    Property((int32_t) UCHAR_NUMERIC_TYPE, 175, VALUES_nt_COUNT, VALUES_nt),
    Property((int32_t) UCHAR_SCRIPT, 177, VALUES_sc_COUNT, VALUES_sc),
    Property((int32_t) UCHAR_SENTENCE_BREAK, 179, VALUES_SB_COUNT, VALUES_SB),
    Property((int32_t) UCHAR_TRAIL_CANONICAL_COMBINING_CLASS, 181, VALUES_tccc_COUNT, VALUES_tccc),
    Property((int32_t) UCHAR_WORD_BREAK, 183, VALUES_WB_COUNT, VALUES_WB),
    Property((int32_t) UCHAR_AGE, 187, 0, NULL),
    Property((int32_t) UCHAR_BIDI_MIRRORING_GLYPH, 189, 0, NULL),
    Property((int32_t) UCHAR_CASE_FOLDING, 191, 0, NULL),
    Property((int32_t) UCHAR_ISO_COMMENT, 193, 0, NULL),
    Property((int32_t) UCHAR_LOWERCASE_MAPPING, 195, 0, NULL),
    Property((int32_t) UCHAR_NAME, 197, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_CASE_FOLDING, 199, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_LOWERCASE_MAPPING, 201, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_TITLECASE_MAPPING, 203, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_UPPERCASE_MAPPING, 205, 0, NULL),
    Property((int32_t) UCHAR_TITLECASE_MAPPING, 207, 0, NULL),
    Property((int32_t) UCHAR_UNICODE_1_NAME, 209, 0, NULL),
    Property((int32_t) UCHAR_UPPERCASE_MAPPING, 211, 0, NULL),
    Property((int32_t) UCHAR_NUMERIC_VALUE, 141, 0, NULL),
    Property((int32_t) UCHAR_GENERAL_CATEGORY_MASK, 185, VALUES_gcm_COUNT, VALUES_gcm),
};

/*eof*/
