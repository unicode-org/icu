/**
 * MACHINE GENERATED FILE.  !!! Do not edit manually !!!
 *
 * Generated from
 *   uchar.h
 *   uscript.h
 *   Blocks.txt
 *   PropertyAliases.txt
 *   PropertyValueAliases.txt
 *
 * Date: Wed Jan 22 10:08:55 2003
 * Unicode version: 3.2.0
 * Script: preparse.pl
 */

/* Unicode version 3.2.0 */
const uint8_t VERSION_0 = 3;
const uint8_t VERSION_1 = 2;
const uint8_t VERSION_2 = 0;
const uint8_t VERSION_3 = 0;

const int32_t STRING_COUNT = 582;

/* to be sorted */
const AliasName STRING_TABLE[] = {
    AliasName("", 0),
    AliasName("A", 1),
    AliasName("AHex", 2),
    AliasName("AI", 3),
    AliasName("AIN", 4),
    AliasName("AL", 5),
    AliasName("ALAPH", 6),
    AliasName("ALEF", 7),
    AliasName("AN", 8),
    AliasName("AR", 9),
    AliasName("ASCII_Hex_Digit", 10),
    AliasName("ATAR", 11),
    AliasName("ATBL", 12),
    AliasName("Above", 13),
    AliasName("Above_Left", 14),
    AliasName("Above_Right", 15),
    AliasName("Age", 16),
    AliasName("Alpha", 17),
    AliasName("Alphabetic", 18),
    AliasName("Alphabetic Presentation Forms", 19),
    AliasName("Ambiguous", 20),
    AliasName("Arab", 21),
    AliasName("Arabic", 22),
    AliasName("Arabic Presentation Forms-A", 23),
    AliasName("Arabic Presentation Forms-B", 24),
    AliasName("Arabic_Letter", 25),
    AliasName("Arabic_Number", 26),
    AliasName("Armenian", 27),
    AliasName("Armn", 28),
    AliasName("Arrows", 29),
    AliasName("Attached_Above_Right", 30),
    AliasName("Attached_Below_Left", 31),
    AliasName("B", 32),
    AliasName("B2", 33),
    AliasName("BA", 34),
    AliasName("BB", 35),
    AliasName("BEH", 36),
    AliasName("BETH", 37),
    AliasName("BK", 38),
    AliasName("BL", 39),
    AliasName("BN", 40),
    AliasName("BR", 41),
    AliasName("Basic Latin", 42),
    AliasName("Below", 43),
    AliasName("Below_Left", 44),
    AliasName("Below_Right", 45),
    AliasName("Beng", 46),
    AliasName("Bengali", 47),
    AliasName("Bidi_C", 48),
    AliasName("Bidi_Class", 49),
    AliasName("Bidi_Control", 50),
    AliasName("Bidi_M", 51),
    AliasName("Bidi_Mirrored", 52),
    AliasName("Bidi_Mirroring_Glyph", 53),
    AliasName("Block", 54),
    AliasName("Block Elements", 55),
    AliasName("Bopo", 56),
    AliasName("Bopomofo", 57),
    AliasName("Bopomofo Extended", 58),
    AliasName("Boundary_Neutral", 59),
    AliasName("Box Drawing", 60),
    AliasName("Braille Patterns", 61),
    AliasName("Break_After", 62),
    AliasName("Break_Before", 63),
    AliasName("Break_Both", 64),
    AliasName("Break_Symbols", 65),
    AliasName("Buhd", 66),
    AliasName("Buhid", 67),
    AliasName("Byzantine Musical Symbols", 68),
    AliasName("C", 69),
    AliasName("CB", 70),
    AliasName("CJK Compatibility", 71),
    AliasName("CJK Compatibility Forms", 72),
    AliasName("CJK Compatibility Ideographs", 73),
    AliasName("CJK Compatibility Ideographs Supplement", 74),
    AliasName("CJK Radicals Supplement", 75),
    AliasName("CJK Symbols and Punctuation", 76),
    AliasName("CJK Unified Ideographs", 77),
    AliasName("CJK Unified Ideographs Extension A", 78),
    AliasName("CJK Unified Ideographs Extension B", 79),
    AliasName("CL", 80),
    AliasName("CM", 81),
    AliasName("CR", 82),
    AliasName("CS", 83),
    AliasName("Canadian_Aboriginal", 84),
    AliasName("Canonical_Combining_Class", 85),
    AliasName("Cans", 86),
    AliasName("Carriage_Return", 87),
    AliasName("Case_Folding", 88),
    AliasName("Cased_Letter", 89),
    AliasName("Cc", 90),
    AliasName("Cf", 91),
    AliasName("Cher", 92),
    AliasName("Cherokee", 93),
    AliasName("Close_Punctuation", 94),
    AliasName("Cn", 95),
    AliasName("Co", 96),
    AliasName("Combining Diacritical Marks", 97),
    AliasName("Combining Diacritical Marks for Symbols", 98),
    AliasName("Combining Half Marks", 99),
    AliasName("Combining_Mark", 100),
    AliasName("Common", 101),
    AliasName("Common_Separator", 102),
    AliasName("Comp_Ex", 103),
    AliasName("Complex_Context", 104),
    AliasName("Connector_Punctuation", 105),
    AliasName("Contingent_Break", 106),
    AliasName("Control", 107),
    AliasName("Control Pictures", 108),
    AliasName("Coptic", 109),
    AliasName("Cs", 110),
    AliasName("Currency Symbols", 111),
    AliasName("Currency_Symbol", 112),
    AliasName("Cyrillic", 113),
    AliasName("Cyrillic Supplementary", 114),
    AliasName("Cyrl", 115),
    AliasName("D", 116),
    AliasName("DA", 117),
    AliasName("DAL", 118),
    AliasName("DALATH_RISH", 119),
    AliasName("DB", 120),
    AliasName("DI", 121),
    AliasName("Dash", 122),
    AliasName("Dash_Punctuation", 123),
    AliasName("Decimal", 124),
    AliasName("Decimal_Number", 125),
    AliasName("Decomposition_Type", 126),
    AliasName("Default_Ignorable_Code_Point", 127),
    AliasName("Dep", 128),
    AliasName("Deprecated", 129),
    AliasName("Deseret", 130),
    AliasName("Deva", 131),
    AliasName("Devanagari", 132),
    AliasName("Dia", 133),
    AliasName("Diacritic", 134),
    AliasName("Digit", 135),
    AliasName("Dingbats", 136),
    AliasName("Double_Above", 137),
    AliasName("Double_Below", 138),
    AliasName("Dsrt", 139),
    AliasName("Dual_Joining", 140),
    AliasName("E", 141),
    AliasName("EN", 142),
    AliasName("ES", 143),
    AliasName("ET", 144),
    AliasName("EX", 145),
    AliasName("East_Asian_Width", 146),
    AliasName("Enclosed Alphanumerics", 147),
    AliasName("Enclosed CJK Letters and Months", 148),
    AliasName("Enclosing_Mark", 149),
    AliasName("Ethi", 150),
    AliasName("Ethiopic", 151),
    AliasName("European_Number", 152),
    AliasName("European_Separator", 153),
    AliasName("European_Terminator", 154),
    AliasName("Exclamation", 155),
    AliasName("Ext", 156),
    AliasName("Extender", 157),
    AliasName("F", 158),
    AliasName("FEH", 159),
    AliasName("FINAL_SEMKATH", 160),
    AliasName("False", 161),
    AliasName("Final_Punctuation", 162),
    AliasName("Format", 163),
    AliasName("Full_Composition_Exclusion", 164),
    AliasName("Fullwidth", 165),
    AliasName("GAF", 166),
    AliasName("GAMAL", 167),
    AliasName("GL", 168),
    AliasName("General Punctuation", 169),
    AliasName("General_Category", 170),
    AliasName("General_Category_Mask", 171),
    AliasName("Geometric Shapes", 172),
    AliasName("Geor", 173),
    AliasName("Georgian", 174),
    AliasName("Glue", 175),
    AliasName("Goth", 176),
    AliasName("Gothic", 177),
    AliasName("Gr_Base", 178),
    AliasName("Gr_Ext", 179),
    AliasName("Gr_Link", 180),
    AliasName("Grapheme_Base", 181),
    AliasName("Grapheme_Extend", 182),
    AliasName("Grapheme_Link", 183),
    AliasName("Greek", 184),
    AliasName("Greek Extended", 185),
    AliasName("Greek and Coptic", 186),
    AliasName("Grek", 187),
    AliasName("Gujarati", 188),
    AliasName("Gujr", 189),
    AliasName("Gurmukhi", 190),
    AliasName("Guru", 191),
    AliasName("H", 192),
    AliasName("HAH", 193),
    AliasName("HAMZA_ON_HEH_GOAL", 194),
    AliasName("HE", 195),
    AliasName("HEH", 196),
    AliasName("HEH_GOAL", 197),
    AliasName("HETH", 198),
    AliasName("HY", 199),
    AliasName("Halfwidth", 200),
    AliasName("Halfwidth and Fullwidth Forms", 201),
    AliasName("Han", 202),
    AliasName("Hang", 203),
    AliasName("Hangul", 204),
    AliasName("Hangul Compatibility Jamo", 205),
    AliasName("Hangul Jamo", 206),
    AliasName("Hangul Syllables", 207),
    AliasName("Hani", 208),
    AliasName("Hano", 209),
    AliasName("Hanunoo", 210),
    AliasName("Hebr", 211),
    AliasName("Hebrew", 212),
    AliasName("Hex", 213),
    AliasName("Hex_Digit", 214),
    AliasName("High Private Use Surrogates", 215),
    AliasName("High Surrogates", 216),
    AliasName("Hira", 217),
    AliasName("Hiragana", 218),
    AliasName("Hyphen", 219),
    AliasName("ID", 220),
    AliasName("IDC", 221),
    AliasName("IDS", 222),
    AliasName("IDSB", 223),
    AliasName("IDST", 224),
    AliasName("IDS_Binary_Operator", 225),
    AliasName("IDS_Trinary_Operator", 226),
    AliasName("ID_Continue", 227),
    AliasName("ID_Start", 228),
    AliasName("IN", 229),
    AliasName("IPA Extensions", 230),
    AliasName("IS", 231),
    AliasName("ISO_Comment", 232),
    AliasName("Ideo", 233),
    AliasName("Ideographic", 234),
    AliasName("Ideographic Description Characters", 235),
    AliasName("Infix_Numeric", 236),
    AliasName("Inherited", 237),
    AliasName("Initial_Punctuation", 238),
    AliasName("Inseperable", 239),
    AliasName("Iota_Subscript", 240),
    AliasName("Ital", 241),
    AliasName("Join_C", 242),
    AliasName("Join_Causing", 243),
    AliasName("Join_Control", 244),
    AliasName("Joining_Group", 245),
    AliasName("Joining_Type", 246),
    AliasName("KAF", 247),
    AliasName("KAPH", 248),
    AliasName("KNOTTED_HEH", 249),
    AliasName("KV", 250),
    AliasName("Kana", 251),
    AliasName("Kana_Voicing", 252),
    AliasName("Kanbun", 253),
    AliasName("Kangxi Radicals", 254),
    AliasName("Kannada", 255),
    AliasName("Katakana", 256),
    AliasName("Katakana Phonetic Extensions", 257),
    AliasName("Khmer", 258),
    AliasName("Khmr", 259),
    AliasName("Knda", 260),
    AliasName("L", 261),
    AliasName("LAM", 262),
    AliasName("LAMADH", 263),
    AliasName("LC", 264),
    AliasName("LF", 265),
    AliasName("LOE", 266),
    AliasName("LRE", 267),
    AliasName("LRO", 268),
    AliasName("Lao", 269),
    AliasName("Laoo", 270),
    AliasName("Latin", 271),
    AliasName("Latin Extended Additional", 272),
    AliasName("Latin Extended-A", 273),
    AliasName("Latin Extended-B", 274),
    AliasName("Latin-1 Supplement", 275),
    AliasName("Latn", 276),
    AliasName("Left", 277),
    AliasName("Left_Joining", 278),
    AliasName("Left_To_Right", 279),
    AliasName("Left_To_Right_Embedding", 280),
    AliasName("Left_To_Right_Override", 281),
    AliasName("Letter", 282),
    AliasName("Letter_Number", 283),
    AliasName("Letterlike Symbols", 284),
    AliasName("Line_Break", 285),
    AliasName("Line_Feed", 286),
    AliasName("Line_Separator", 287),
    AliasName("Ll", 288),
    AliasName("Lm", 289),
    AliasName("Lo", 290),
    AliasName("Logical_Order_Exception", 291),
    AliasName("Low Surrogates", 292),
    AliasName("Lower", 293),
    AliasName("Lowercase", 294),
    AliasName("Lowercase_Letter", 295),
    AliasName("Lowercase_Mapping", 296),
    AliasName("Lt", 297),
    AliasName("Lu", 298),
    AliasName("M", 299),
    AliasName("MEEM", 300),
    AliasName("MIM", 301),
    AliasName("Malayalam", 302),
    AliasName("Mandatory_Break", 303),
    AliasName("Mark", 304),
    AliasName("Math", 305),
    AliasName("Math_Symbol", 306),
    AliasName("Mathematical Alphanumeric Symbols", 307),
    AliasName("Mathematical Operators", 308),
    AliasName("Mc", 309),
    AliasName("Me", 310),
    AliasName("Miscellaneous Mathematical Symbols-A", 311),
    AliasName("Miscellaneous Mathematical Symbols-B", 312),
    AliasName("Miscellaneous Symbols", 313),
    AliasName("Miscellaneous Technical", 314),
    AliasName("Mlym", 315),
    AliasName("Mn", 316),
    AliasName("Modifier_Letter", 317),
    AliasName("Modifier_Symbol", 318),
    AliasName("Mong", 319),
    AliasName("Mongolian", 320),
    AliasName("Musical Symbols", 321),
    AliasName("Myanmar", 322),
    AliasName("Mymr", 323),
    AliasName("N", 324),
    AliasName("NChar", 325),
    AliasName("NK", 326),
    AliasName("NOON", 327),
    AliasName("NO_JOINING_GROUP", 328),
    AliasName("NR", 329),
    AliasName("NS", 330),
    AliasName("NSM", 331),
    AliasName("NU", 332),
    AliasName("NUN", 333),
    AliasName("Na", 334),
    AliasName("Name", 335),
    AliasName("Narrow", 336),
    AliasName("Nd", 337),
    AliasName("Neutral", 338),
    AliasName("Nl", 339),
    AliasName("No", 340),
    AliasName("Non_Joining", 341),
    AliasName("Noncharacter_Code_Point", 342),
    AliasName("None", 343),
    AliasName("Nonspacing_Mark", 344),
    AliasName("Nonstarter", 345),
    AliasName("Not_Reordered", 346),
    AliasName("Nukta", 347),
    AliasName("Number", 348),
    AliasName("Number Forms", 349),
    AliasName("Numeric", 350),
    AliasName("Numeric_Type", 351),
    AliasName("Numeric_Value", 352),
    AliasName("ON", 353),
    AliasName("OP", 354),
    AliasName("OV", 355),
    AliasName("Ogam", 356),
    AliasName("Ogham", 357),
    AliasName("Old Italic", 358),
    AliasName("Old_Italic", 359),
    AliasName("Open_Punctuation", 360),
    AliasName("Optical Character Recognition", 361),
    AliasName("Oriya", 362),
    AliasName("Orya", 363),
    AliasName("Other", 364),
    AliasName("Other_Letter", 365),
    AliasName("Other_Neutral", 366),
    AliasName("Other_Number", 367),
    AliasName("Other_Punctuation", 368),
    AliasName("Other_Symbol", 369),
    AliasName("Overlay", 370),
    AliasName("P", 371),
    AliasName("PDF", 372),
    AliasName("PE", 373),
    AliasName("PO", 374),
    AliasName("PR", 375),
    AliasName("Paragraph_Separator", 376),
    AliasName("Pc", 377),
    AliasName("Pd", 378),
    AliasName("Pe", 379),
    AliasName("Pf", 380),
    AliasName("Pi", 381),
    AliasName("Po", 382),
    AliasName("Pop_Directional_Format", 383),
    AliasName("Postfix_Numeric", 384),
    AliasName("Prefix_Numeric", 385),
    AliasName("Private Use Area", 386),
    AliasName("Private_Use", 387),
    AliasName("Ps", 388),
    AliasName("Punctuation", 389),
    AliasName("QAF", 390),
    AliasName("QAPH", 391),
    AliasName("QMark", 392),
    AliasName("QU", 393),
    AliasName("Qaac", 394),
    AliasName("Qaai", 395),
    AliasName("Quotation", 396),
    AliasName("Quotation_Mark", 397),
    AliasName("R", 398),
    AliasName("REH", 399),
    AliasName("REVERSED_PE", 400),
    AliasName("RLE", 401),
    AliasName("RLO", 402),
    AliasName("Radical", 403),
    AliasName("Right", 404),
    AliasName("Right_Joining", 405),
    AliasName("Right_To_Left", 406),
    AliasName("Right_To_Left_Embedding", 407),
    AliasName("Right_To_Left_Override", 408),
    AliasName("Runic", 409),
    AliasName("Runr", 410),
    AliasName("S", 411),
    AliasName("SA", 412),
    AliasName("SAD", 413),
    AliasName("SADHE", 414),
    AliasName("SD", 415),
    AliasName("SEEN", 416),
    AliasName("SEMKATH", 417),
    AliasName("SG", 418),
    AliasName("SHIN", 419),
    AliasName("SP", 420),
    AliasName("SWASH_KAF", 421),
    AliasName("SY", 422),
    AliasName("SYRIAC_WAW", 423),
    AliasName("Sc", 424),
    AliasName("Script", 425),
    AliasName("Segment_Separator", 426),
    AliasName("Separator", 427),
    AliasName("Simple_Case_Folding", 428),
    AliasName("Simple_Lowercase_Mapping", 429),
    AliasName("Simple_Titlecase_Mapping", 430),
    AliasName("Simple_Uppercase_Mapping", 431),
    AliasName("Sinh", 432),
    AliasName("Sinhala", 433),
    AliasName("Sk", 434),
    AliasName("Sm", 435),
    AliasName("Small Form Variants", 436),
    AliasName("So", 437),
    AliasName("Soft_Dotted", 438),
    AliasName("Space", 439),
    AliasName("Space_Separator", 440),
    AliasName("Spacing Modifier Letters", 441),
    AliasName("Spacing_Mark", 442),
    AliasName("Specials", 443),
    AliasName("Superscripts and Subscripts", 444),
    AliasName("Supplemental Arrows-A", 445),
    AliasName("Supplemental Arrows-B", 446),
    AliasName("Supplemental Mathematical Operators", 447),
    AliasName("Supplementary Private Use Area-A", 448),
    AliasName("Supplementary Private Use Area-B", 449),
    AliasName("Surrogate", 450),
    AliasName("Symbol", 451),
    AliasName("Syrc", 452),
    AliasName("Syriac", 453),
    AliasName("T", 454),
    AliasName("TAH", 455),
    AliasName("TAW", 456),
    AliasName("TEH_MARBUTA", 457),
    AliasName("TETH", 458),
    AliasName("Tagalog", 459),
    AliasName("Tagb", 460),
    AliasName("Tagbanwa", 461),
    AliasName("Tags", 462),
    AliasName("Tamil", 463),
    AliasName("Taml", 464),
    AliasName("Telu", 465),
    AliasName("Telugu", 466),
    AliasName("Term", 467),
    AliasName("Terminal_Punctuation", 468),
    AliasName("Tglg", 469),
    AliasName("Thaa", 470),
    AliasName("Thaana", 471),
    AliasName("Thai", 472),
    AliasName("Tibetan", 473),
    AliasName("Tibt", 474),
    AliasName("Titlecase_Letter", 475),
    AliasName("Titlecase_Mapping", 476),
    AliasName("Transparent", 477),
    AliasName("True", 478),
    AliasName("U", 479),
    AliasName("UIdeo", 480),
    AliasName("Unassigned", 481),
    AliasName("Unicode_1_Name", 482),
    AliasName("Unified Canadian Aboriginal Syllabics", 483),
    AliasName("Unified_Ideograph", 484),
    AliasName("Unknown", 485),
    AliasName("Upper", 486),
    AliasName("Uppercase", 487),
    AliasName("Uppercase_Letter", 488),
    AliasName("Uppercase_Mapping", 489),
    AliasName("VR", 490),
    AliasName("Variation Selectors", 491),
    AliasName("Virama", 492),
    AliasName("W", 493),
    AliasName("WAW", 494),
    AliasName("WS", 495),
    AliasName("WSpace", 496),
    AliasName("White_Space", 497),
    AliasName("Wide", 498),
    AliasName("XIDC", 499),
    AliasName("XIDS", 500),
    AliasName("XID_Continue", 501),
    AliasName("XID_Start", 502),
    AliasName("XX", 503),
    AliasName("YEH", 504),
    AliasName("YEH_BARREE", 505),
    AliasName("YEH_WITH_TAIL", 506),
    AliasName("YUDH", 507),
    AliasName("YUDH_HE", 508),
    AliasName("Yi", 509),
    AliasName("Yi Radicals", 510),
    AliasName("Yi Syllables", 511),
    AliasName("Yiii", 512),
    AliasName("Z", 513),
    AliasName("ZAIN", 514),
    AliasName("ZW", 515),
    AliasName("ZWSpace", 516),
    AliasName("Zl", 517),
    AliasName("Zp", 518),
    AliasName("Zs", 519),
    AliasName("Zyyy", 520),
    AliasName("age", 521),
    AliasName("bc", 522),
    AliasName("blk", 523),
    AliasName("bmg", 524),
    AliasName("can", 525),
    AliasName("canonical", 526),
    AliasName("ccc", 527),
    AliasName("cf", 528),
    AliasName("circle", 529),
    AliasName("com", 530),
    AliasName("compat", 531),
    AliasName("de", 532),
    AliasName("di", 533),
    AliasName("dt", 534),
    AliasName("ea", 535),
    AliasName("enc", 536),
    AliasName("fin", 537),
    AliasName("final", 538),
    AliasName("font", 539),
    AliasName("fra", 540),
    AliasName("fraction", 541),
    AliasName("gc", 542),
    AliasName("gcm", 543),
    AliasName("init", 544),
    AliasName("initial", 545),
    AliasName("isc", 546),
    AliasName("iso", 547),
    AliasName("isolated", 548),
    AliasName("jg", 549),
    AliasName("jt", 550),
    AliasName("lb", 551),
    AliasName("lc", 552),
    AliasName("med", 553),
    AliasName("medial", 554),
    AliasName("na", 555),
    AliasName("na1", 556),
    AliasName("nar", 557),
    AliasName("narrow", 558),
    AliasName("nb", 559),
    AliasName("noBreak", 560),
    AliasName("none", 561),
    AliasName("nt", 562),
    AliasName("nu", 563),
    AliasName("nv", 564),
    AliasName("sc", 565),
    AliasName("sfc", 566),
    AliasName("slc", 567),
    AliasName("small", 568),
    AliasName("sml", 569),
    AliasName("sqr", 570),
    AliasName("square", 571),
    AliasName("stc", 572),
    AliasName("sub", 573),
    AliasName("suc", 574),
    AliasName("sup", 575),
    AliasName("super", 576),
    AliasName("tc", 577),
    AliasName("uc", 578),
    AliasName("vert", 579),
    AliasName("vertical", 580),
    AliasName("wide", 581),
};

/* to be filled in */
int32_t REMAP[582];

const int32_t NAME_GROUP_COUNT = 814;

int32_t NAME_GROUP[] = {
    17, -18,            /*   0: "Alpha", "Alphabetic" */
    2, -10,             /*   2: "AHex", "ASCII_Hex_Digit" */
    48, -50,            /*   4: "Bidi_C", "Bidi_Control" */
    51, -52,            /*   6: "Bidi_M", "Bidi_Mirrored" */
    122, -122,          /*   8: "Dash", "Dash" */
    121, -127,          /*  10: "DI", "Default_Ignorable_Code_Point" */
    128, -129,          /*  12: "Dep", "Deprecated" */
    133, -134,          /*  14: "Dia", "Diacritic" */
    156, -157,          /*  16: "Ext", "Extender" */
    103, -164,          /*  18: "Comp_Ex", "Full_Composition_Exclusion" */
    178, -181,          /*  20: "Gr_Base", "Grapheme_Base" */
    179, -182,          /*  22: "Gr_Ext", "Grapheme_Extend" */
    180, -183,          /*  24: "Gr_Link", "Grapheme_Link" */
    213, -214,          /*  26: "Hex", "Hex_Digit" */
    219, -219,          /*  28: "Hyphen", "Hyphen" */
    233, -234,          /*  30: "Ideo", "Ideographic" */
    223, -225,          /*  32: "IDSB", "IDS_Binary_Operator" */
    224, -226,          /*  34: "IDST", "IDS_Trinary_Operator" */
    221, -227,          /*  36: "IDC", "ID_Continue" */
    222, -228,          /*  38: "IDS", "ID_Start" */
    242, -244,          /*  40: "Join_C", "Join_Control" */
    266, -291,          /*  42: "LOE", "Logical_Order_Exception" */
    293, -294,          /*  44: "Lower", "Lowercase" */
    305, -305,          /*  46: "Math", "Math" */
    325, -342,          /*  48: "NChar", "Noncharacter_Code_Point" */
    392, -397,          /*  50: "QMark", "Quotation_Mark" */
    403, -403,          /*  52: "Radical", "Radical" */
    415, -438,          /*  54: "SD", "Soft_Dotted" */
    467, -468,          /*  56: "Term", "Terminal_Punctuation" */
    480, -484,          /*  58: "UIdeo", "Unified_Ideograph" */
    486, -487,          /*  60: "Upper", "Uppercase" */
    496, -497,          /*  62: "WSpace", "White_Space" */
    499, -501,          /*  64: "XIDC", "XID_Continue" */
    500, -502,          /*  66: "XIDS", "XID_Start" */
    564, -352,          /*  68: "nv", "Numeric_Value" */
    522, -49,           /*  70: "bc", "Bidi_Class" */
    523, -54,           /*  72: "blk", "Block" */
    527, -85,           /*  74: "ccc", "Canonical_Combining_Class" */
    534, -126,          /*  76: "dt", "Decomposition_Type" */
    535, -146,          /*  78: "ea", "East_Asian_Width" */
    542, -170,          /*  80: "gc", "General_Category" */
    549, -245,          /*  82: "jg", "Joining_Group" */
    550, -246,          /*  84: "jt", "Joining_Type" */
    551, -285,          /*  86: "lb", "Line_Break" */
    562, -351,          /*  88: "nt", "Numeric_Type" */
    565, -425,          /*  90: "sc", "Script" */
    543, -171,          /*  92: "gcm", "General_Category_Mask" */
    521, -16,           /*  94: "age", "Age" */
    524, -53,           /*  96: "bmg", "Bidi_Mirroring_Glyph" */
    528, -88,           /*  98: "cf", "Case_Folding" */
    546, -232,          /* 100: "isc", "ISO_Comment" */
    552, -296,          /* 102: "lc", "Lowercase_Mapping" */
    555, -335,          /* 104: "na", "Name" */
    566, -428,          /* 106: "sfc", "Simple_Case_Folding" */
    567, -429,          /* 108: "slc", "Simple_Lowercase_Mapping" */
    572, -430,          /* 110: "stc", "Simple_Titlecase_Mapping" */
    574, -431,          /* 112: "suc", "Simple_Uppercase_Mapping" */
    577, -476,          /* 114: "tc", "Titlecase_Mapping" */
    556, -482,          /* 116: "na1", "Unicode_1_Name" */
    578, -489,          /* 118: "uc", "Uppercase_Mapping" */
    8, -26,             /* 120: "AN", "Arabic_Number" */
    32, -376,           /* 122: "B", "Paragraph_Separator" */
    40, -59,            /* 124: "BN", "Boundary_Neutral" */
    83, -102,           /* 126: "CS", "Common_Separator" */
    331, -344,          /* 128: "NSM", "Nonspacing_Mark" */
    142, -152,          /* 130: "EN", "European_Number" */
    143, -153,          /* 132: "ES", "European_Separator" */
    144, -154,          /* 134: "ET", "European_Terminator" */
    261, -279,          /* 136: "L", "Left_To_Right" */
    267, -280,          /* 138: "LRE", "Left_To_Right_Embedding" */
    268, -281,          /* 140: "LRO", "Left_To_Right_Override" */
    353, -366,          /* 142: "ON", "Other_Neutral" */
    372, -383,          /* 144: "PDF", "Pop_Directional_Format" */
    398, -406,          /* 146: "R", "Right_To_Left" */
    5, -25,             /* 148: "AL", "Arabic_Letter" */
    401, -407,          /* 150: "RLE", "Right_To_Left_Embedding" */
    402, -408,          /* 152: "RLO", "Right_To_Left_Override" */
    411, -426,          /* 154: "S", "Segment_Separator" */
    495, -497,          /* 156: "WS", "White_Space" */
    158, -161,          /* 158: "F", "False" */
    454, -478,          /* 160: "T", "True" */
    0, -19,             /* 162: "", "Alphabetic Presentation Forms" */
    0, -22,             /* 164: "", "Arabic" */
    0, -23,             /* 166: "", "Arabic Presentation Forms-A" */
    0, -24,             /* 168: "", "Arabic Presentation Forms-B" */
    0, -27,             /* 170: "", "Armenian" */
    0, -29,             /* 172: "", "Arrows" */
    0, -42,             /* 174: "", "Basic Latin" */
    0, -47,             /* 176: "", "Bengali" */
    0, -55,             /* 178: "", "Block Elements" */
    0, -57,             /* 180: "", "Bopomofo" */
    0, -58,             /* 182: "", "Bopomofo Extended" */
    0, -60,             /* 184: "", "Box Drawing" */
    0, -61,             /* 186: "", "Braille Patterns" */
    0, -67,             /* 188: "", "Buhid" */
    0, -68,             /* 190: "", "Byzantine Musical Symbols" */
    0, -93,             /* 192: "", "Cherokee" */
    0, -71,             /* 194: "", "CJK Compatibility" */
    0, -72,             /* 196: "", "CJK Compatibility Forms" */
    0, -73,             /* 198: "", "CJK Compatibility Ideographs" */
    0, -74,             /* 200: "", "CJK Compatibility Ideographs Supplement" */
    0, -75,             /* 202: "", "CJK Radicals Supplement" */
    0, -76,             /* 204: "", "CJK Symbols and Punctuation" */
    0, -77,             /* 206: "", "CJK Unified Ideographs" */
    0, -78,             /* 208: "", "CJK Unified Ideographs Extension A" */
    0, -79,             /* 210: "", "CJK Unified Ideographs Extension B" */
    0, -97,             /* 212: "", "Combining Diacritical Marks" */
    0, -99,             /* 214: "", "Combining Half Marks" */
    0, -98,             /* 216: "", "Combining Diacritical Marks for Symbols" */
    0, -108,            /* 218: "", "Control Pictures" */
    0, -111,            /* 220: "", "Currency Symbols" */
    0, -113,            /* 222: "", "Cyrillic" */
    0, -114,            /* 224: "", "Cyrillic Supplementary" */
    0, -130,            /* 226: "", "Deseret" */
    0, -132,            /* 228: "", "Devanagari" */
    0, -136,            /* 230: "", "Dingbats" */
    0, -147,            /* 232: "", "Enclosed Alphanumerics" */
    0, -148,            /* 234: "", "Enclosed CJK Letters and Months" */
    0, -151,            /* 236: "", "Ethiopic" */
    0, -169,            /* 238: "", "General Punctuation" */
    0, -172,            /* 240: "", "Geometric Shapes" */
    0, -174,            /* 242: "", "Georgian" */
    0, -177,            /* 244: "", "Gothic" */
    0, -186,            /* 246: "", "Greek and Coptic" */
    0, -185,            /* 248: "", "Greek Extended" */
    0, -188,            /* 250: "", "Gujarati" */
    0, -190,            /* 252: "", "Gurmukhi" */
    0, -201,            /* 254: "", "Halfwidth and Fullwidth Forms" */
    0, -205,            /* 256: "", "Hangul Compatibility Jamo" */
    0, -206,            /* 258: "", "Hangul Jamo" */
    0, -207,            /* 260: "", "Hangul Syllables" */
    0, -210,            /* 262: "", "Hanunoo" */
    0, -212,            /* 264: "", "Hebrew" */
    0, -215,            /* 266: "", "High Private Use Surrogates" */
    0, -216,            /* 268: "", "High Surrogates" */
    0, -218,            /* 270: "", "Hiragana" */
    0, -235,            /* 272: "", "Ideographic Description Characters" */
    0, -230,            /* 274: "", "IPA Extensions" */
    0, -253,            /* 276: "", "Kanbun" */
    0, -254,            /* 278: "", "Kangxi Radicals" */
    0, -255,            /* 280: "", "Kannada" */
    0, -256,            /* 282: "", "Katakana" */
    0, -257,            /* 284: "", "Katakana Phonetic Extensions" */
    0, -258,            /* 286: "", "Khmer" */
    0, -269,            /* 288: "", "Lao" */
    0, -275,            /* 290: "", "Latin-1 Supplement" */
    0, -273,            /* 292: "", "Latin Extended-A" */
    0, -272,            /* 294: "", "Latin Extended Additional" */
    0, -274,            /* 296: "", "Latin Extended-B" */
    0, -284,            /* 298: "", "Letterlike Symbols" */
    0, -292,            /* 300: "", "Low Surrogates" */
    0, -302,            /* 302: "", "Malayalam" */
    0, -307,            /* 304: "", "Mathematical Alphanumeric Symbols" */
    0, -308,            /* 306: "", "Mathematical Operators" */
    0, -311,            /* 308: "", "Miscellaneous Mathematical Symbols-A" */
    0, -312,            /* 310: "", "Miscellaneous Mathematical Symbols-B" */
    0, -313,            /* 312: "", "Miscellaneous Symbols" */
    0, -314,            /* 314: "", "Miscellaneous Technical" */
    0, -320,            /* 316: "", "Mongolian" */
    0, -321,            /* 318: "", "Musical Symbols" */
    0, -322,            /* 320: "", "Myanmar" */
    0, -349,            /* 322: "", "Number Forms" */
    0, -357,            /* 324: "", "Ogham" */
    0, -358,            /* 326: "", "Old Italic" */
    0, -361,            /* 328: "", "Optical Character Recognition" */
    0, -362,            /* 330: "", "Oriya" */
    0, -386,            /* 332: "", "Private Use Area" */
    0, -409,            /* 334: "", "Runic" */
    0, -433,            /* 336: "", "Sinhala" */
    0, -436,            /* 338: "", "Small Form Variants" */
    0, -441,            /* 340: "", "Spacing Modifier Letters" */
    0, -443,            /* 342: "", "Specials" */
    0, -444,            /* 344: "", "Superscripts and Subscripts" */
    0, -445,            /* 346: "", "Supplemental Arrows-A" */
    0, -446,            /* 348: "", "Supplemental Arrows-B" */
    0, -447,            /* 350: "", "Supplemental Mathematical Operators" */
    0, -448,            /* 352: "", "Supplementary Private Use Area-A" */
    0, -449,            /* 354: "", "Supplementary Private Use Area-B" */
    0, -453,            /* 356: "", "Syriac" */
    0, -459,            /* 358: "", "Tagalog" */
    0, -461,            /* 360: "", "Tagbanwa" */
    0, -462,            /* 362: "", "Tags" */
    0, -463,            /* 364: "", "Tamil" */
    0, -466,            /* 366: "", "Telugu" */
    0, -471,            /* 368: "", "Thaana" */
    0, -472,            /* 370: "", "Thai" */
    0, -473,            /* 372: "", "Tibetan" */
    0, -483,            /* 374: "", "Unified Canadian Aboriginal Syllabics" */
    0, -491,            /* 376: "", "Variation Selectors" */
    0, -510,            /* 378: "", "Yi Radicals" */
    0, -511,            /* 380: "", "Yi Syllables" */
    329, -346,          /* 382: "NR", "Not_Reordered" */
    355, -370,          /* 384: "OV", "Overlay" */
    12, -31,            /* 386: "ATBL", "Attached_Below_Left" */
    11, -30,            /* 388: "ATAR", "Attached_Above_Right" */
    39, -44,            /* 390: "BL", "Below_Left" */
    32, -43,            /* 392: "B", "Below" */
    41, -45,            /* 394: "BR", "Below_Right" */
    261, -277,          /* 396: "L", "Left" */
    398, -404,          /* 398: "R", "Right" */
    5, -14,             /* 400: "AL", "Above_Left" */
    1, -13,             /* 402: "A", "Above" */
    9, -15,             /* 404: "AR", "Above_Right" */
    120, -138,          /* 406: "DB", "Double_Below" */
    117, -137,          /* 408: "DA", "Double_Above" */
    231, -240,          /* 410: "IS", "Iota_Subscript" */
    326, -347,          /* 412: "NK", "Nukta" */
    250, -252,          /* 414: "KV", "Kana_Voicing" */
    490, -492,          /* 416: "VR", "Virama" */
    525, -526,          /* 418: "can", "canonical" */
    536, -529,          /* 420: "enc", "circle" */
    530, -531,          /* 422: "com", "compat" */
    537, -538,          /* 424: "fin", "final" */
    539, -539,          /* 426: "font", "font" */
    540, -541,          /* 428: "fra", "fraction" */
    544, -545,          /* 430: "init", "initial" */
    547, -548,          /* 432: "iso", "isolated" */
    553, -554,          /* 434: "med", "medial" */
    557, -558,          /* 436: "nar", "narrow" */
    559, -560,          /* 438: "nb", "noBreak" */
    0, -561,            /* 440: "", "none" */
    569, -568,          /* 442: "sml", "small" */
    570, -571,          /* 444: "sqr", "square" */
    573, -573,          /* 446: "sub", "sub" */
    575, -576,          /* 448: "sup", "super" */
    579, -580,          /* 450: "vert", "vertical" */
    581, -581,          /* 452: "wide", "wide" */
    1, -20,             /* 454: "A", "Ambiguous" */
    158, -165,          /* 456: "F", "Fullwidth" */
    192, -200,          /* 458: "H", "Halfwidth" */
    334, -336,          /* 460: "Na", "Narrow" */
    324, -338,          /* 462: "N", "Neutral" */
    493, -498,          /* 464: "W", "Wide" */
    309, -442,          /* 466: "Mc", "Spacing_Mark" */
    377, -105,          /* 468: "Pc", "Connector_Punctuation" */
    90, -107,           /* 470: "Cc", "Control" */
    424, -112,          /* 472: "Sc", "Currency_Symbol" */
    378, -123,          /* 474: "Pd", "Dash_Punctuation" */
    337, -125,          /* 476: "Nd", "Decimal_Number" */
    310, -149,          /* 478: "Me", "Enclosing_Mark" */
    379, -94,           /* 480: "Pe", "Close_Punctuation" */
    380, -162,          /* 482: "Pf", "Final_Punctuation" */
    91, -163,           /* 484: "Cf", "Format" */
    95, -481,           /* 486: "Cn", "Unassigned" */
    381, -238,          /* 488: "Pi", "Initial_Punctuation" */
    339, -283,          /* 490: "Nl", "Letter_Number" */
    517, -287,          /* 492: "Zl", "Line_Separator" */
    288, -295,          /* 494: "Ll", "Lowercase_Letter" */
    435, -306,          /* 496: "Sm", "Math_Symbol" */
    289, -317,          /* 498: "Lm", "Modifier_Letter" */
    434, -318,          /* 500: "Sk", "Modifier_Symbol" */
    316, -344,          /* 502: "Mn", "Nonspacing_Mark" */
    290, -365,          /* 504: "Lo", "Other_Letter" */
    340, -367,          /* 506: "No", "Other_Number" */
    382, -368,          /* 508: "Po", "Other_Punctuation" */
    437, -369,          /* 510: "So", "Other_Symbol" */
    518, -376,          /* 512: "Zp", "Paragraph_Separator" */
    96, -387,           /* 514: "Co", "Private_Use" */
    519, -440,          /* 516: "Zs", "Space_Separator" */
    388, -360,          /* 518: "Ps", "Open_Punctuation" */
    110, -450,          /* 520: "Cs", "Surrogate" */
    297, -475,          /* 522: "Lt", "Titlecase_Letter" */
    298, -488,          /* 524: "Lu", "Uppercase_Letter" */
    69, -364,           /* 526: "C", "Other" */
    264, -89,           /* 528: "LC", "Cased_Letter" */
    261, -282,          /* 530: "L", "Letter" */
    299, -304,          /* 532: "M", "Mark" */
    324, -348,          /* 534: "N", "Number" */
    371, -389,          /* 536: "P", "Punctuation" */
    411, -451,          /* 538: "S", "Symbol" */
    513, -427,          /* 540: "Z", "Separator" */
    0, -4,              /* 542: "", "AIN" */
    0, -6,              /* 544: "", "ALAPH" */
    0, -7,              /* 546: "", "ALEF" */
    0, -36,             /* 548: "", "BEH" */
    0, -37,             /* 550: "", "BETH" */
    0, -118,            /* 552: "", "DAL" */
    0, -119,            /* 554: "", "DALATH_RISH" */
    0, -141,            /* 556: "", "E" */
    0, -159,            /* 558: "", "FEH" */
    0, -160,            /* 560: "", "FINAL_SEMKATH" */
    0, -166,            /* 562: "", "GAF" */
    0, -167,            /* 564: "", "GAMAL" */
    0, -193,            /* 566: "", "HAH" */
    0, -194,            /* 568: "", "HAMZA_ON_HEH_GOAL" */
    0, -195,            /* 570: "", "HE" */
    0, -196,            /* 572: "", "HEH" */
    0, -197,            /* 574: "", "HEH_GOAL" */
    0, -198,            /* 576: "", "HETH" */
    0, -247,            /* 578: "", "KAF" */
    0, -248,            /* 580: "", "KAPH" */
    0, -249,            /* 582: "", "KNOTTED_HEH" */
    0, -262,            /* 584: "", "LAM" */
    0, -263,            /* 586: "", "LAMADH" */
    0, -300,            /* 588: "", "MEEM" */
    0, -301,            /* 590: "", "MIM" */
    0, -327,            /* 592: "", "NOON" */
    0, -328,            /* 594: "", "NO_JOINING_GROUP" */
    0, -333,            /* 596: "", "NUN" */
    0, -373,            /* 598: "", "PE" */
    0, -390,            /* 600: "", "QAF" */
    0, -391,            /* 602: "", "QAPH" */
    0, -399,            /* 604: "", "REH" */
    0, -400,            /* 606: "", "REVERSED_PE" */
    0, -413,            /* 608: "", "SAD" */
    0, -414,            /* 610: "", "SADHE" */
    0, -416,            /* 612: "", "SEEN" */
    0, -417,            /* 614: "", "SEMKATH" */
    0, -419,            /* 616: "", "SHIN" */
    0, -421,            /* 618: "", "SWASH_KAF" */
    0, -423,            /* 620: "", "SYRIAC_WAW" */
    0, -455,            /* 622: "", "TAH" */
    0, -456,            /* 624: "", "TAW" */
    0, -457,            /* 626: "", "TEH_MARBUTA" */
    0, -458,            /* 628: "", "TETH" */
    0, -494,            /* 630: "", "WAW" */
    0, -504,            /* 632: "", "YEH" */
    0, -505,            /* 634: "", "YEH_BARREE" */
    0, -506,            /* 636: "", "YEH_WITH_TAIL" */
    0, -507,            /* 638: "", "YUDH" */
    0, -508,            /* 640: "", "YUDH_HE" */
    0, -514,            /* 642: "", "ZAIN" */
    116, -140,          /* 644: "D", "Dual_Joining" */
    69, -243,           /* 646: "C", "Join_Causing" */
    261, -278,          /* 648: "L", "Left_Joining" */
    479, -341,          /* 650: "U", "Non_Joining" */
    398, -405,          /* 652: "R", "Right_Joining" */
    454, -477,          /* 654: "T", "Transparent" */
    5, -18,             /* 656: "AL", "Alphabetic" */
    3, -20,             /* 658: "AI", "Ambiguous" */
    34, -62,            /* 660: "BA", "Break_After" */
    35, -63,            /* 662: "BB", "Break_Before" */
    33, -64,            /* 664: "B2", "Break_Both" */
    422, -65,           /* 666: "SY", "Break_Symbols" */
    82, -87,            /* 668: "CR", "Carriage_Return" */
    80, -94,            /* 670: "CL", "Close_Punctuation" */
    81, -100,           /* 672: "CM", "Combining_Mark" */
    412, -104,          /* 674: "SA", "Complex_Context" */
    70, -106,           /* 676: "CB", "Contingent_Break" */
    145, -155,          /* 678: "EX", "Exclamation" */
    168, -175,          /* 680: "GL", "Glue" */
    199, -219,          /* 682: "HY", "Hyphen" */
    220, -234,          /* 684: "ID", "Ideographic" */
    231, -236,          /* 686: "IS", "Infix_Numeric" */
    229, -239,          /* 688: "IN", "Inseperable" */
    265, -286,          /* 690: "LF", "Line_Feed" */
    38, -303,           /* 692: "BK", "Mandatory_Break" */
    330, -345,          /* 694: "NS", "Nonstarter" */
    332, -350,          /* 696: "NU", "Numeric" */
    354, -360,          /* 698: "OP", "Open_Punctuation" */
    374, -384,          /* 700: "PO", "Postfix_Numeric" */
    375, -385,          /* 702: "PR", "Prefix_Numeric" */
    393, -396,          /* 704: "QU", "Quotation" */
    420, -439,          /* 706: "SP", "Space" */
    418, -450,          /* 708: "SG", "Surrogate" */
    503, -485,          /* 710: "XX", "Unknown" */
    515, -516,          /* 712: "ZW", "ZWSpace" */
    532, -124,          /* 714: "de", "Decimal" */
    533, -135,          /* 716: "di", "Digit" */
    0, -343,            /* 718: "", "None" */
    563, -350,          /* 720: "nu", "Numeric" */
    21, -22,            /* 722: "Arab", "Arabic" */
    28, -27,            /* 724: "Armn", "Armenian" */
    46, -47,            /* 726: "Beng", "Bengali" */
    56, -57,            /* 728: "Bopo", "Bopomofo" */
    66, -67,            /* 730: "Buhd", "Buhid" */
    92, -93,            /* 732: "Cher", "Cherokee" */
    520, -101,          /* 734: "Zyyy", "Common" */
    394, -109,          /* 736: "Qaac", "Coptic" */
    115, -113,          /* 738: "Cyrl", "Cyrillic" */
    139, -130,          /* 740: "Dsrt", "Deseret" */
    131, -132,          /* 742: "Deva", "Devanagari" */
    150, -151,          /* 744: "Ethi", "Ethiopic" */
    173, -174,          /* 746: "Geor", "Georgian" */
    176, -177,          /* 748: "Goth", "Gothic" */
    187, -184,          /* 750: "Grek", "Greek" */
    189, -188,          /* 752: "Gujr", "Gujarati" */
    191, -190,          /* 754: "Guru", "Gurmukhi" */
    208, -202,          /* 756: "Hani", "Han" */
    203, -204,          /* 758: "Hang", "Hangul" */
    209, -210,          /* 760: "Hano", "Hanunoo" */
    211, -212,          /* 762: "Hebr", "Hebrew" */
    217, -218,          /* 764: "Hira", "Hiragana" */
    395, -237,          /* 766: "Qaai", "Inherited" */
    260, -255,          /* 768: "Knda", "Kannada" */
    251, -256,          /* 770: "Kana", "Katakana" */
    259, -258,          /* 772: "Khmr", "Khmer" */
    270, -269,          /* 774: "Laoo", "Lao" */
    276, -271,          /* 776: "Latn", "Latin" */
    315, -302,          /* 778: "Mlym", "Malayalam" */
    319, -320,          /* 780: "Mong", "Mongolian" */
    323, -322,          /* 782: "Mymr", "Myanmar" */
    356, -357,          /* 784: "Ogam", "Ogham" */
    241, -359,          /* 786: "Ital", "Old_Italic" */
    363, -362,          /* 788: "Orya", "Oriya" */
    410, -409,          /* 790: "Runr", "Runic" */
    432, -433,          /* 792: "Sinh", "Sinhala" */
    452, -453,          /* 794: "Syrc", "Syriac" */
    469, -459,          /* 796: "Tglg", "Tagalog" */
    460, -461,          /* 798: "Tagb", "Tagbanwa" */
    464, -463,          /* 800: "Taml", "Tamil" */
    465, -466,          /* 802: "Telu", "Telugu" */
    470, -471,          /* 804: "Thaa", "Thaana" */
    472, -472,          /* 806: "Thai", "Thai" */
    474, -473,          /* 808: "Tibt", "Tibetan" */
    86, -84,            /* 810: "Cans", "Canadian_Aboriginal" */
    512, -509,          /* 812: "Yiii", "Yi" */
};

#define MAX_NAMES_PER_GROUP 2

const int32_t VALUES_bc_COUNT = 19;

const Alias VALUES_bc[] = {
    Alias((int32_t) U_ARABIC_NUMBER, 120),
    Alias((int32_t) U_BLOCK_SEPARATOR, 122),
    Alias((int32_t) U_BOUNDARY_NEUTRAL, 124),
    Alias((int32_t) U_COMMON_NUMBER_SEPARATOR, 126),
    Alias((int32_t) U_DIR_NON_SPACING_MARK, 128),
    Alias((int32_t) U_EUROPEAN_NUMBER, 130),
    Alias((int32_t) U_EUROPEAN_NUMBER_SEPARATOR, 132),
    Alias((int32_t) U_EUROPEAN_NUMBER_TERMINATOR, 134),
    Alias((int32_t) U_LEFT_TO_RIGHT, 136),
    Alias((int32_t) U_LEFT_TO_RIGHT_EMBEDDING, 138),
    Alias((int32_t) U_LEFT_TO_RIGHT_OVERRIDE, 140),
    Alias((int32_t) U_OTHER_NEUTRAL, 142),
    Alias((int32_t) U_POP_DIRECTIONAL_FORMAT, 144),
    Alias((int32_t) U_RIGHT_TO_LEFT, 146),
    Alias((int32_t) U_RIGHT_TO_LEFT_ARABIC, 148),
    Alias((int32_t) U_RIGHT_TO_LEFT_EMBEDDING, 150),
    Alias((int32_t) U_RIGHT_TO_LEFT_OVERRIDE, 152),
    Alias((int32_t) U_SEGMENT_SEPARATOR, 154),
    Alias((int32_t) U_WHITE_SPACE_NEUTRAL, 156),
};

const int32_t VALUES_binprop_COUNT = 2;

const Alias VALUES_binprop[] = {
    Alias((int32_t) 0, 158),
    Alias((int32_t) 1, 160),
};

const int32_t VALUES_blk_COUNT = 110;

const Alias VALUES_blk[] = {
    Alias((int32_t) UBLOCK_ALPHABETIC_PRESENTATION_FORMS, 162),
    Alias((int32_t) UBLOCK_ARABIC, 164),
    Alias((int32_t) UBLOCK_ARABIC_PRESENTATION_FORMS_A, 166),
    Alias((int32_t) UBLOCK_ARABIC_PRESENTATION_FORMS_B, 168),
    Alias((int32_t) UBLOCK_ARMENIAN, 170),
    Alias((int32_t) UBLOCK_ARROWS, 172),
    Alias((int32_t) UBLOCK_BASIC_LATIN, 174),
    Alias((int32_t) UBLOCK_BENGALI, 176),
    Alias((int32_t) UBLOCK_BLOCK_ELEMENTS, 178),
    Alias((int32_t) UBLOCK_BOPOMOFO, 180),
    Alias((int32_t) UBLOCK_BOPOMOFO_EXTENDED, 182),
    Alias((int32_t) UBLOCK_BOX_DRAWING, 184),
    Alias((int32_t) UBLOCK_BRAILLE_PATTERNS, 186),
    Alias((int32_t) UBLOCK_BUHID, 188),
    Alias((int32_t) UBLOCK_BYZANTINE_MUSICAL_SYMBOLS, 190),
    Alias((int32_t) UBLOCK_CHEROKEE, 192),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY, 194),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY_FORMS, 196),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY_IDEOGRAPHS, 198),
    Alias((int32_t) UBLOCK_CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT, 200),
    Alias((int32_t) UBLOCK_CJK_RADICALS_SUPPLEMENT, 202),
    Alias((int32_t) UBLOCK_CJK_SYMBOLS_AND_PUNCTUATION, 204),
    Alias((int32_t) UBLOCK_CJK_UNIFIED_IDEOGRAPHS, 206),
    Alias((int32_t) UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A, 208),
    Alias((int32_t) UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B, 210),
    Alias((int32_t) UBLOCK_COMBINING_DIACRITICAL_MARKS, 212),
    Alias((int32_t) UBLOCK_COMBINING_HALF_MARKS, 214),
    Alias((int32_t) UBLOCK_COMBINING_MARKS_FOR_SYMBOLS, 216),
    Alias((int32_t) UBLOCK_CONTROL_PICTURES, 218),
    Alias((int32_t) UBLOCK_CURRENCY_SYMBOLS, 220),
    Alias((int32_t) UBLOCK_CYRILLIC, 222),
    Alias((int32_t) UBLOCK_CYRILLIC_SUPPLEMENTARY, 224),
    Alias((int32_t) UBLOCK_DESERET, 226),
    Alias((int32_t) UBLOCK_DEVANAGARI, 228),
    Alias((int32_t) UBLOCK_DINGBATS, 230),
    Alias((int32_t) UBLOCK_ENCLOSED_ALPHANUMERICS, 232),
    Alias((int32_t) UBLOCK_ENCLOSED_CJK_LETTERS_AND_MONTHS, 234),
    Alias((int32_t) UBLOCK_ETHIOPIC, 236),
    Alias((int32_t) UBLOCK_GENERAL_PUNCTUATION, 238),
    Alias((int32_t) UBLOCK_GEOMETRIC_SHAPES, 240),
    Alias((int32_t) UBLOCK_GEORGIAN, 242),
    Alias((int32_t) UBLOCK_GOTHIC, 244),
    Alias((int32_t) UBLOCK_GREEK, 246),
    Alias((int32_t) UBLOCK_GREEK_EXTENDED, 248),
    Alias((int32_t) UBLOCK_GUJARATI, 250),
    Alias((int32_t) UBLOCK_GURMUKHI, 252),
    Alias((int32_t) UBLOCK_HALFWIDTH_AND_FULLWIDTH_FORMS, 254),
    Alias((int32_t) UBLOCK_HANGUL_COMPATIBILITY_JAMO, 256),
    Alias((int32_t) UBLOCK_HANGUL_JAMO, 258),
    Alias((int32_t) UBLOCK_HANGUL_SYLLABLES, 260),
    Alias((int32_t) UBLOCK_HANUNOO, 262),
    Alias((int32_t) UBLOCK_HEBREW, 264),
    Alias((int32_t) UBLOCK_HIGH_PRIVATE_USE_SURROGATES, 266),
    Alias((int32_t) UBLOCK_HIGH_SURROGATES, 268),
    Alias((int32_t) UBLOCK_HIRAGANA, 270),
    Alias((int32_t) UBLOCK_IDEOGRAPHIC_DESCRIPTION_CHARACTERS, 272),
    Alias((int32_t) UBLOCK_IPA_EXTENSIONS, 274),
    Alias((int32_t) UBLOCK_KANBUN, 276),
    Alias((int32_t) UBLOCK_KANGXI_RADICALS, 278),
    Alias((int32_t) UBLOCK_KANNADA, 280),
    Alias((int32_t) UBLOCK_KATAKANA, 282),
    Alias((int32_t) UBLOCK_KATAKANA_PHONETIC_EXTENSIONS, 284),
    Alias((int32_t) UBLOCK_KHMER, 286),
    Alias((int32_t) UBLOCK_LAO, 288),
    Alias((int32_t) UBLOCK_LATIN_1_SUPPLEMENT, 290),
    Alias((int32_t) UBLOCK_LATIN_EXTENDED_A, 292),
    Alias((int32_t) UBLOCK_LATIN_EXTENDED_ADDITIONAL, 294),
    Alias((int32_t) UBLOCK_LATIN_EXTENDED_B, 296),
    Alias((int32_t) UBLOCK_LETTERLIKE_SYMBOLS, 298),
    Alias((int32_t) UBLOCK_LOW_SURROGATES, 300),
    Alias((int32_t) UBLOCK_MALAYALAM, 302),
    Alias((int32_t) UBLOCK_MATHEMATICAL_ALPHANUMERIC_SYMBOLS, 304),
    Alias((int32_t) UBLOCK_MATHEMATICAL_OPERATORS, 306),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A, 308),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B, 310),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_SYMBOLS, 312),
    Alias((int32_t) UBLOCK_MISCELLANEOUS_TECHNICAL, 314),
    Alias((int32_t) UBLOCK_MONGOLIAN, 316),
    Alias((int32_t) UBLOCK_MUSICAL_SYMBOLS, 318),
    Alias((int32_t) UBLOCK_MYANMAR, 320),
    Alias((int32_t) UBLOCK_NUMBER_FORMS, 322),
    Alias((int32_t) UBLOCK_OGHAM, 324),
    Alias((int32_t) UBLOCK_OLD_ITALIC, 326),
    Alias((int32_t) UBLOCK_OPTICAL_CHARACTER_RECOGNITION, 328),
    Alias((int32_t) UBLOCK_ORIYA, 330),
    Alias((int32_t) UBLOCK_PRIVATE_USE_AREA, 332),
    Alias((int32_t) UBLOCK_RUNIC, 334),
    Alias((int32_t) UBLOCK_SINHALA, 336),
    Alias((int32_t) UBLOCK_SMALL_FORM_VARIANTS, 338),
    Alias((int32_t) UBLOCK_SPACING_MODIFIER_LETTERS, 340),
    Alias((int32_t) UBLOCK_SPECIALS, 342),
    Alias((int32_t) UBLOCK_SUPERSCRIPTS_AND_SUBSCRIPTS, 344),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_ARROWS_A, 346),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_ARROWS_B, 348),
    Alias((int32_t) UBLOCK_SUPPLEMENTAL_MATHEMATICAL_OPERATORS, 350),
    Alias((int32_t) UBLOCK_SUPPLEMENTARY_PRIVATE_USE_AREA_A, 352),
    Alias((int32_t) UBLOCK_SUPPLEMENTARY_PRIVATE_USE_AREA_B, 354),
    Alias((int32_t) UBLOCK_SYRIAC, 356),
    Alias((int32_t) UBLOCK_TAGALOG, 358),
    Alias((int32_t) UBLOCK_TAGBANWA, 360),
    Alias((int32_t) UBLOCK_TAGS, 362),
    Alias((int32_t) UBLOCK_TAMIL, 364),
    Alias((int32_t) UBLOCK_TELUGU, 366),
    Alias((int32_t) UBLOCK_THAANA, 368),
    Alias((int32_t) UBLOCK_THAI, 370),
    Alias((int32_t) UBLOCK_TIBETAN, 372),
    Alias((int32_t) UBLOCK_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS, 374),
    Alias((int32_t) UBLOCK_VARIATION_SELECTORS, 376),
    Alias((int32_t) UBLOCK_YI_RADICALS, 378),
    Alias((int32_t) UBLOCK_YI_SYLLABLES, 380),
};

const int32_t VALUES_ccc_COUNT = 18;

const Alias VALUES_ccc[] = {
    Alias((int32_t) 0, 382),
    Alias((int32_t) 1, 384),
    Alias((int32_t) 202, 386),
    Alias((int32_t) 216, 388),
    Alias((int32_t) 218, 390),
    Alias((int32_t) 220, 392),
    Alias((int32_t) 222, 394),
    Alias((int32_t) 224, 396),
    Alias((int32_t) 226, 398),
    Alias((int32_t) 228, 400),
    Alias((int32_t) 230, 402),
    Alias((int32_t) 232, 404),
    Alias((int32_t) 233, 406),
    Alias((int32_t) 234, 408),
    Alias((int32_t) 240, 410),
    Alias((int32_t) 7, 412),
    Alias((int32_t) 8, 414),
    Alias((int32_t) 9, 416),
};

const int32_t VALUES_dt_COUNT = 18;

const Alias VALUES_dt[] = {
    Alias((int32_t) U_DT_CANONICAL, 418),
    Alias((int32_t) U_DT_CIRCLE, 420),
    Alias((int32_t) U_DT_COMPAT, 422),
    Alias((int32_t) U_DT_FINAL, 424),
    Alias((int32_t) U_DT_FONT, 426),
    Alias((int32_t) U_DT_FRACTION, 428),
    Alias((int32_t) U_DT_INITIAL, 430),
    Alias((int32_t) U_DT_ISOLATED, 432),
    Alias((int32_t) U_DT_MEDIAL, 434),
    Alias((int32_t) U_DT_NARROW, 436),
    Alias((int32_t) U_DT_NOBREAK, 438),
    Alias((int32_t) U_DT_NONE, 440),
    Alias((int32_t) U_DT_SMALL, 442),
    Alias((int32_t) U_DT_SQUARE, 444),
    Alias((int32_t) U_DT_SUB, 446),
    Alias((int32_t) U_DT_SUPER, 448),
    Alias((int32_t) U_DT_VERTICAL, 450),
    Alias((int32_t) U_DT_WIDE, 452),
};

const int32_t VALUES_ea_COUNT = 6;

const Alias VALUES_ea[] = {
    Alias((int32_t) U_EA_AMBIGUOUS, 454),
    Alias((int32_t) U_EA_FULLWIDTH, 456),
    Alias((int32_t) U_EA_HALFWIDTH, 458),
    Alias((int32_t) U_EA_NARROW, 460),
    Alias((int32_t) U_EA_NEUTRAL, 462),
    Alias((int32_t) U_EA_WIDE, 464),
};

const int32_t VALUES_gc_COUNT = 30;

const Alias VALUES_gc[] = {
    Alias((int32_t) U_COMBINING_SPACING_MARK, 466),
    Alias((int32_t) U_CONNECTOR_PUNCTUATION, 468),
    Alias((int32_t) U_CONTROL_CHAR, 470),
    Alias((int32_t) U_CURRENCY_SYMBOL, 472),
    Alias((int32_t) U_DASH_PUNCTUATION, 474),
    Alias((int32_t) U_DECIMAL_DIGIT_NUMBER, 476),
    Alias((int32_t) U_ENCLOSING_MARK, 478),
    Alias((int32_t) U_END_PUNCTUATION, 480),
    Alias((int32_t) U_FINAL_PUNCTUATION, 482),
    Alias((int32_t) U_FORMAT_CHAR, 484),
    Alias((int32_t) U_GENERAL_OTHER_TYPES, 486),
    Alias((int32_t) U_INITIAL_PUNCTUATION, 488),
    Alias((int32_t) U_LETTER_NUMBER, 490),
    Alias((int32_t) U_LINE_SEPARATOR, 492),
    Alias((int32_t) U_LOWERCASE_LETTER, 494),
    Alias((int32_t) U_MATH_SYMBOL, 496),
    Alias((int32_t) U_MODIFIER_LETTER, 498),
    Alias((int32_t) U_MODIFIER_SYMBOL, 500),
    Alias((int32_t) U_NON_SPACING_MARK, 502),
    Alias((int32_t) U_OTHER_LETTER, 504),
    Alias((int32_t) U_OTHER_NUMBER, 506),
    Alias((int32_t) U_OTHER_PUNCTUATION, 508),
    Alias((int32_t) U_OTHER_SYMBOL, 510),
    Alias((int32_t) U_PARAGRAPH_SEPARATOR, 512),
    Alias((int32_t) U_PRIVATE_USE_CHAR, 514),
    Alias((int32_t) U_SPACE_SEPARATOR, 516),
    Alias((int32_t) U_START_PUNCTUATION, 518),
    Alias((int32_t) U_SURROGATE, 520),
    Alias((int32_t) U_TITLECASE_LETTER, 522),
    Alias((int32_t) U_UPPERCASE_LETTER, 524),
};

const int32_t VALUES_gcm_COUNT = 38;

const Alias VALUES_gcm[] = {
    Alias((int32_t) U_GC_CC_MASK, 470),
    Alias((int32_t) U_GC_CF_MASK, 484),
    Alias((int32_t) U_GC_CN_MASK, 486),
    Alias((int32_t) U_GC_CO_MASK, 514),
    Alias((int32_t) U_GC_CS_MASK, 520),
    Alias((int32_t) U_GC_C_MASK, 526),
    Alias((int32_t) U_GC_LC_MASK, 528),
    Alias((int32_t) U_GC_LL_MASK, 494),
    Alias((int32_t) U_GC_LM_MASK, 498),
    Alias((int32_t) U_GC_LO_MASK, 504),
    Alias((int32_t) U_GC_LT_MASK, 522),
    Alias((int32_t) U_GC_LU_MASK, 524),
    Alias((int32_t) U_GC_L_MASK, 530),
    Alias((int32_t) U_GC_MC_MASK, 466),
    Alias((int32_t) U_GC_ME_MASK, 478),
    Alias((int32_t) U_GC_MN_MASK, 502),
    Alias((int32_t) U_GC_M_MASK, 532),
    Alias((int32_t) U_GC_ND_MASK, 476),
    Alias((int32_t) U_GC_NL_MASK, 490),
    Alias((int32_t) U_GC_NO_MASK, 506),
    Alias((int32_t) U_GC_N_MASK, 534),
    Alias((int32_t) U_GC_PC_MASK, 468),
    Alias((int32_t) U_GC_PD_MASK, 474),
    Alias((int32_t) U_GC_PE_MASK, 480),
    Alias((int32_t) U_GC_PF_MASK, 482),
    Alias((int32_t) U_GC_PI_MASK, 488),
    Alias((int32_t) U_GC_PO_MASK, 508),
    Alias((int32_t) U_GC_PS_MASK, 518),
    Alias((int32_t) U_GC_P_MASK, 536),
    Alias((int32_t) U_GC_SC_MASK, 472),
    Alias((int32_t) U_GC_SK_MASK, 500),
    Alias((int32_t) U_GC_SM_MASK, 496),
    Alias((int32_t) U_GC_SO_MASK, 510),
    Alias((int32_t) U_GC_S_MASK, 538),
    Alias((int32_t) U_GC_ZL_MASK, 492),
    Alias((int32_t) U_GC_ZP_MASK, 512),
    Alias((int32_t) U_GC_ZS_MASK, 516),
    Alias((int32_t) U_GC_Z_MASK, 540),
};

const int32_t VALUES_jg_COUNT = 51;

const Alias VALUES_jg[] = {
    Alias((int32_t) U_JG_AIN, 542),
    Alias((int32_t) U_JG_ALAPH, 544),
    Alias((int32_t) U_JG_ALEF, 546),
    Alias((int32_t) U_JG_BEH, 548),
    Alias((int32_t) U_JG_BETH, 550),
    Alias((int32_t) U_JG_DAL, 552),
    Alias((int32_t) U_JG_DALATH_RISH, 554),
    Alias((int32_t) U_JG_E, 556),
    Alias((int32_t) U_JG_FEH, 558),
    Alias((int32_t) U_JG_FINAL_SEMKATH, 560),
    Alias((int32_t) U_JG_GAF, 562),
    Alias((int32_t) U_JG_GAMAL, 564),
    Alias((int32_t) U_JG_HAH, 566),
    Alias((int32_t) U_JG_HAMZA_ON_HEH_GOAL, 568),
    Alias((int32_t) U_JG_HE, 570),
    Alias((int32_t) U_JG_HEH, 572),
    Alias((int32_t) U_JG_HEH_GOAL, 574),
    Alias((int32_t) U_JG_HETH, 576),
    Alias((int32_t) U_JG_KAF, 578),
    Alias((int32_t) U_JG_KAPH, 580),
    Alias((int32_t) U_JG_KNOTTED_HEH, 582),
    Alias((int32_t) U_JG_LAM, 584),
    Alias((int32_t) U_JG_LAMADH, 586),
    Alias((int32_t) U_JG_MEEM, 588),
    Alias((int32_t) U_JG_MIM, 590),
    Alias((int32_t) U_JG_NOON, 592),
    Alias((int32_t) U_JG_NO_JOINING_GROUP, 594),
    Alias((int32_t) U_JG_NUN, 596),
    Alias((int32_t) U_JG_PE, 598),
    Alias((int32_t) U_JG_QAF, 600),
    Alias((int32_t) U_JG_QAPH, 602),
    Alias((int32_t) U_JG_REH, 604),
    Alias((int32_t) U_JG_REVERSED_PE, 606),
    Alias((int32_t) U_JG_SAD, 608),
    Alias((int32_t) U_JG_SADHE, 610),
    Alias((int32_t) U_JG_SEEN, 612),
    Alias((int32_t) U_JG_SEMKATH, 614),
    Alias((int32_t) U_JG_SHIN, 616),
    Alias((int32_t) U_JG_SWASH_KAF, 618),
    Alias((int32_t) U_JG_SYRIAC_WAW, 620),
    Alias((int32_t) U_JG_TAH, 622),
    Alias((int32_t) U_JG_TAW, 624),
    Alias((int32_t) U_JG_TEH_MARBUTA, 626),
    Alias((int32_t) U_JG_TETH, 628),
    Alias((int32_t) U_JG_WAW, 630),
    Alias((int32_t) U_JG_YEH, 632),
    Alias((int32_t) U_JG_YEH_BARREE, 634),
    Alias((int32_t) U_JG_YEH_WITH_TAIL, 636),
    Alias((int32_t) U_JG_YUDH, 638),
    Alias((int32_t) U_JG_YUDH_HE, 640),
    Alias((int32_t) U_JG_ZAIN, 642),
};

const int32_t VALUES_jt_COUNT = 6;

const Alias VALUES_jt[] = {
    Alias((int32_t) U_JT_DUAL_JOINING, 644),
    Alias((int32_t) U_JT_JOIN_CAUSING, 646),
    Alias((int32_t) U_JT_LEFT_JOINING, 648),
    Alias((int32_t) U_JT_NON_JOINING, 650),
    Alias((int32_t) U_JT_RIGHT_JOINING, 652),
    Alias((int32_t) U_JT_TRANSPARENT, 654),
};

const int32_t VALUES_lb_COUNT = 29;

const Alias VALUES_lb[] = {
    Alias((int32_t) U_LB_ALPHABETIC, 656),
    Alias((int32_t) U_LB_AMBIGUOUS, 658),
    Alias((int32_t) U_LB_BREAK_AFTER, 660),
    Alias((int32_t) U_LB_BREAK_BEFORE, 662),
    Alias((int32_t) U_LB_BREAK_BOTH, 664),
    Alias((int32_t) U_LB_BREAK_SYMBOLS, 666),
    Alias((int32_t) U_LB_CARRIAGE_RETURN, 668),
    Alias((int32_t) U_LB_CLOSE_PUNCTUATION, 670),
    Alias((int32_t) U_LB_COMBINING_MARK, 672),
    Alias((int32_t) U_LB_COMPLEX_CONTEXT, 674),
    Alias((int32_t) U_LB_CONTINGENT_BREAK, 676),
    Alias((int32_t) U_LB_EXCLAMATION, 678),
    Alias((int32_t) U_LB_GLUE, 680),
    Alias((int32_t) U_LB_HYPHEN, 682),
    Alias((int32_t) U_LB_IDEOGRAPHIC, 684),
    Alias((int32_t) U_LB_INFIX_NUMERIC, 686),
    Alias((int32_t) U_LB_INSEPERABLE, 688),
    Alias((int32_t) U_LB_LINE_FEED, 690),
    Alias((int32_t) U_LB_MANDATORY_BREAK, 692),
    Alias((int32_t) U_LB_NONSTARTER, 694),
    Alias((int32_t) U_LB_NUMERIC, 696),
    Alias((int32_t) U_LB_OPEN_PUNCTUATION, 698),
    Alias((int32_t) U_LB_POSTFIX_NUMERIC, 700),
    Alias((int32_t) U_LB_PREFIX_NUMERIC, 702),
    Alias((int32_t) U_LB_QUOTATION, 704),
    Alias((int32_t) U_LB_SPACE, 706),
    Alias((int32_t) U_LB_SURROGATE, 708),
    Alias((int32_t) U_LB_UNKNOWN, 710),
    Alias((int32_t) U_LB_ZWSPACE, 712),
};

const int32_t VALUES_nt_COUNT = 4;

const Alias VALUES_nt[] = {
    Alias((int32_t) U_NT_DECIMAL, 714),
    Alias((int32_t) U_NT_DIGIT, 716),
    Alias((int32_t) U_NT_NONE, 718),
    Alias((int32_t) U_NT_NUMERIC, 720),
};

const int32_t VALUES_sc_COUNT = 46;

const Alias VALUES_sc[] = {
    Alias((int32_t) USCRIPT_ARABIC, 722),
    Alias((int32_t) USCRIPT_ARMENIAN, 724),
    Alias((int32_t) USCRIPT_BENGALI, 726),
    Alias((int32_t) USCRIPT_BOPOMOFO, 728),
    Alias((int32_t) USCRIPT_BUHID, 730),
    Alias((int32_t) USCRIPT_CHEROKEE, 732),
    Alias((int32_t) USCRIPT_COMMON, 734),
    Alias((int32_t) USCRIPT_COPTIC, 736),
    Alias((int32_t) USCRIPT_CYRILLIC, 738),
    Alias((int32_t) USCRIPT_DESERET, 740),
    Alias((int32_t) USCRIPT_DEVANAGARI, 742),
    Alias((int32_t) USCRIPT_ETHIOPIC, 744),
    Alias((int32_t) USCRIPT_GEORGIAN, 746),
    Alias((int32_t) USCRIPT_GOTHIC, 748),
    Alias((int32_t) USCRIPT_GREEK, 750),
    Alias((int32_t) USCRIPT_GUJARATI, 752),
    Alias((int32_t) USCRIPT_GURMUKHI, 754),
    Alias((int32_t) USCRIPT_HAN, 756),
    Alias((int32_t) USCRIPT_HANGUL, 758),
    Alias((int32_t) USCRIPT_HANUNOO, 760),
    Alias((int32_t) USCRIPT_HEBREW, 762),
    Alias((int32_t) USCRIPT_HIRAGANA, 764),
    Alias((int32_t) USCRIPT_INHERITED, 766),
    Alias((int32_t) USCRIPT_KANNADA, 768),
    Alias((int32_t) USCRIPT_KATAKANA, 770),
    Alias((int32_t) USCRIPT_KHMER, 772),
    Alias((int32_t) USCRIPT_LAO, 774),
    Alias((int32_t) USCRIPT_LATIN, 776),
    Alias((int32_t) USCRIPT_MALAYALAM, 778),
    Alias((int32_t) USCRIPT_MONGOLIAN, 780),
    Alias((int32_t) USCRIPT_MYANMAR, 782),
    Alias((int32_t) USCRIPT_OGHAM, 784),
    Alias((int32_t) USCRIPT_OLD_ITALIC, 786),
    Alias((int32_t) USCRIPT_ORIYA, 788),
    Alias((int32_t) USCRIPT_RUNIC, 790),
    Alias((int32_t) USCRIPT_SINHALA, 792),
    Alias((int32_t) USCRIPT_SYRIAC, 794),
    Alias((int32_t) USCRIPT_TAGALOG, 796),
    Alias((int32_t) USCRIPT_TAGBANWA, 798),
    Alias((int32_t) USCRIPT_TAMIL, 800),
    Alias((int32_t) USCRIPT_TELUGU, 802),
    Alias((int32_t) USCRIPT_THAANA, 804),
    Alias((int32_t) USCRIPT_THAI, 806),
    Alias((int32_t) USCRIPT_TIBETAN, 808),
    Alias((int32_t) USCRIPT_UCAS, 810),
    Alias((int32_t) USCRIPT_YI, 812),
};

const int32_t PROPERTY_COUNT = 60;

const Property PROPERTY[] = {
    Property((int32_t) UCHAR_ALPHABETIC, 0, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_ASCII_HEX_DIGIT, 2, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_BIDI_CONTROL, 4, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_BIDI_MIRRORED, 6, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DASH, 8, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DEFAULT_IGNORABLE_CODE_POINT, 10, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DEPRECATED, 12, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_DIACRITIC, 14, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_EXTENDER, 16, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_FULL_COMPOSITION_EXCLUSION, 18, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_GRAPHEME_BASE, 20, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_GRAPHEME_EXTEND, 22, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_GRAPHEME_LINK, 24, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_HEX_DIGIT, 26, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_HYPHEN, 28, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_IDEOGRAPHIC, 30, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_IDS_BINARY_OPERATOR, 32, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_IDS_TRINARY_OPERATOR, 34, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_ID_CONTINUE, 36, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_ID_START, 38, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_JOIN_CONTROL, 40, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_LOGICAL_ORDER_EXCEPTION, 42, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_LOWERCASE, 44, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_MATH, 46, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_NONCHARACTER_CODE_POINT, 48, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_QUOTATION_MARK, 50, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_RADICAL, 52, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_SOFT_DOTTED, 54, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_TERMINAL_PUNCTUATION, 56, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_UNIFIED_IDEOGRAPH, 58, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_UPPERCASE, 60, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_WHITE_SPACE, 62, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_XID_CONTINUE, 64, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_XID_START, 66, VALUES_binprop_COUNT, VALUES_binprop),
    Property((int32_t) UCHAR_BIDI_CLASS, 70, VALUES_bc_COUNT, VALUES_bc),
    Property((int32_t) UCHAR_BLOCK, 72, VALUES_blk_COUNT, VALUES_blk),
    Property((int32_t) UCHAR_CANONICAL_COMBINING_CLASS, 74, VALUES_ccc_COUNT, VALUES_ccc),
    Property((int32_t) UCHAR_DECOMPOSITION_TYPE, 76, VALUES_dt_COUNT, VALUES_dt),
    Property((int32_t) UCHAR_EAST_ASIAN_WIDTH, 78, VALUES_ea_COUNT, VALUES_ea),
    Property((int32_t) UCHAR_GENERAL_CATEGORY, 80, VALUES_gc_COUNT, VALUES_gc),
    Property((int32_t) UCHAR_JOINING_GROUP, 82, VALUES_jg_COUNT, VALUES_jg),
    Property((int32_t) UCHAR_JOINING_TYPE, 84, VALUES_jt_COUNT, VALUES_jt),
    Property((int32_t) UCHAR_LINE_BREAK, 86, VALUES_lb_COUNT, VALUES_lb),
    Property((int32_t) UCHAR_NUMERIC_TYPE, 88, VALUES_nt_COUNT, VALUES_nt),
    Property((int32_t) UCHAR_SCRIPT, 90, VALUES_sc_COUNT, VALUES_sc),
    Property((int32_t) UCHAR_AGE, 94, 0, NULL),
    Property((int32_t) UCHAR_BIDI_MIRRORING_GLYPH, 96, 0, NULL),
    Property((int32_t) UCHAR_CASE_FOLDING, 98, 0, NULL),
    Property((int32_t) UCHAR_ISO_COMMENT, 100, 0, NULL),
    Property((int32_t) UCHAR_LOWERCASE_MAPPING, 102, 0, NULL),
    Property((int32_t) UCHAR_NAME, 104, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_CASE_FOLDING, 106, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_LOWERCASE_MAPPING, 108, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_TITLECASE_MAPPING, 110, 0, NULL),
    Property((int32_t) UCHAR_SIMPLE_UPPERCASE_MAPPING, 112, 0, NULL),
    Property((int32_t) UCHAR_TITLECASE_MAPPING, 114, 0, NULL),
    Property((int32_t) UCHAR_UNICODE_1_NAME, 116, 0, NULL),
    Property((int32_t) UCHAR_UPPERCASE_MAPPING, 118, 0, NULL),
    Property((int32_t) UCHAR_NUMERIC_VALUE, 68, 0, NULL),
    Property((int32_t) UCHAR_GENERAL_CATEGORY_MASK, 92, VALUES_gcm_COUNT, VALUES_gcm),
};

/*eof*/
