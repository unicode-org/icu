/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CALLCOLL.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Ported for C API
*********************************************************************************
*/
/**
 * CollationDummyTest is a third level test class.  This tests creation of 
 * a customized collator object.  For example, number 1 to be sorted 
 * equlivalent to word 'one'.
 */
#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "callcoll.h"
#include "unicode/ustring.h"
#include <string.h>

static UCollator *myCollation;
static const UChar DEFAULTRULEARRAY[] =
{
          0x3d, 0x27, (UChar)0x200B, 0x27, 0x3d, (UChar)0x200C, 0x3d, (UChar)0x200D, 0x3d, (UChar)0x200E, 0x3d, (UChar)0x200F
        , 0x3d, (UChar)0x0001, 0x3d, (UChar)0x0002, 0x3d, (UChar)0x0003, 0x3d, (UChar)0x0004
        , 0x3d, (UChar)0x0005, 0x3d, (UChar)0x0006, 0x3d, (UChar)0x0007, 0x3d, (UChar)0x0008, 0x3d, 0x27, (UChar)0x0009, 0x27
        , 0x3d, 0x27, (UChar)0x000b, 0x27,  0x3d, (UChar)0x000e       /* vt,, so */
        , 0x3d, (UChar)0x000f, 0x3d, 0x27, (UChar)0x0010, 0x27,  0x3d, (UChar)0x0011, 0x3d, (UChar)0x0012, 0x3d, (UChar)0x0013 /* si, dle, dc1, dc2, dc3 */
        , 0x3d, (UChar)0x0014, 0x3d, (UChar)0x0015, 0x3d, (UChar)0x0016, 0x3d, (UChar)0x0017, 0x3d, (UChar)0x0018 /* dc4, nak, syn, etb, can */
        , 0x3d, (UChar)0x0019, 0x3d, (UChar)0x001a, 0x3d, (UChar)0x001b, 0x3d, (UChar)0x001c, 0x3d, (UChar)0x001d /* em, sub, esc, fs, gs */
        , 0x3d, (UChar)0x001e, 0x3d, (UChar)0x001f, 0x3d, (UChar)0x007f                   /* rs, us, del */
        /* ....then the C1 Latin 1 reserved control codes */
        , 0x3d, (UChar)0x0080, 0x3d, (UChar)0x0081, 0x3d, (UChar)0x0082, 0x3d, (UChar)0x0083, 0x3d, (UChar)0x0084, 0x3d, (UChar)0x0085
        , 0x3d, (UChar)0x0086, 0x3d, (UChar)0x0087, 0x3d, (UChar)0x0088, 0x3d, (UChar)0x0089, 0x3d, (UChar)0x008a, 0x3d, (UChar)0x008b
        , 0x3d, (UChar)0x008c, 0x3d, (UChar)0x008d, 0x3d, (UChar)0x008e, 0x3d, (UChar)0x008f, 0x3d, (UChar)0x0090, 0x3d, (UChar)0x0091
        , 0x3d, (UChar)0x0092, 0x3d, (UChar)0x0093, 0x3d, (UChar)0x0094, 0x3d, (UChar)0x0095, 0x3d, (UChar)0x0096, 0x3d, (UChar)0x0097
        , 0x3d, (UChar)0x0098, 0x3d, (UChar)0x0099, 0x3d, (UChar)0x009a, 0x3d, (UChar)0x009b, 0x3d, (UChar)0x009c, 0x3d, (UChar)0x009d
        , 0x3d, (UChar)0x009e, 0x3d, (UChar)0x009f
        /*  IGNORE except for secondary, tertiary difference */
        /*  Spaces */
        , 0x3b, 0x27, (UChar)0x0020, 0x27, 0x3b, 0x27, (UChar)0x00A0, 0x27                   /*  spaces */
        , 0x3b, 0x27, (UChar)0x2000, 0x27, 0x3b, 0x27, (UChar)0x2001, 0x27, 0x3b, 0x27, (UChar)0x2002, 0x27, 0x3b, 0x27, (UChar)0x2003, 0x27, 0x3b, 0x27, (UChar)0x2004, 0x27   /*  spaces */
        , 0x3b, 0x27, (UChar)0x2005, 0x27, 0x3b, 0x27, (UChar)0x2006, 0x27, 0x3b, 0x27, (UChar)0x2007, 0x27, 0x3b, 0x27, (UChar)0x2008, 0x27, 0x3b, 0x27, (UChar)0x2009, 0x27   /*  spaces */
        , 0x3b, 0x27, (UChar)0x200A, 0x27, 0x3b, 0x27, (UChar)0x3000, 0x27, 0x3b, 0x27, (UChar)0xFEFF, 0x27                 /*  spaces */
        , 0x3b, 0x27, 0xd, 0x27,  0x3b, 0x27, 9, 0x27,  0x3b, 0x27, 0xa, 0x27, 0x3b, 0x27, 0xc, 0x27, 0x3b, 0x27, (UChar)0x000b, 0x27   /*  whitespace */

        /*  Non-spacing accents */

        , 0x3b, (UChar)0x0301          /*  non-spacing acute accent */
        , 0x3b, (UChar)0x0300          /*  non-spacing grave accent */
        , 0x3b, (UChar)0x0306          /*  non-spacing breve accent */
        , 0x3b, (UChar)0x0302          /*  non-spacing circumflex accent */
        , 0x3b, (UChar)0x030c          /*  non-spacing caron/hacek accent */
        , 0x3b, (UChar)0x030a          /*  non-spacing ring above accent */
        , 0x3b, (UChar)0x030d          /*  non-spacing vertical line above */
        , 0x3b, (UChar)0x0308          /*  non-spacing diaeresis accent */
        , 0x3b, (UChar)0x030b          /*  non-spacing double acute accent */
        , 0x3b, (UChar)0x0303          /*  non-spacing tilde accent */
        , 0x3b, (UChar)0x0307          /*  non-spacing dot above/overdot accent */
        , 0x3b, (UChar)0x0304          /*  non-spacing macron accent */
        , 0x3b, (UChar)0x0337          /*  non-spacing short slash overlay (overstruck diacritic) */
        , 0x3b, (UChar)0x0327          /*  non-spacing cedilla accent */
        , 0x3b, (UChar)0x0328          /*  non-spacing ogonek accent */
        , 0x3b, (UChar)0x0323          /*  non-spacing dot-below/underdot accent */
        , 0x3b, (UChar)0x0332          /*  non-spacing underscore/underline accent */
        /*  with the rest of the general diacritical marks in binary order */
        , 0x3b, (UChar)0x0305          /*  non-spacing overscore/overline */
        , 0x3b, (UChar)0x0309          /*  non-spacing hook above */
        , 0x3b, (UChar)0x030e          /*  non-spacing double vertical line above */
        , 0x3b, (UChar)0x030f          /*  non-spacing double grave */
        , 0x3b, (UChar)0x0310          /*  non-spacing chandrabindu */
        , 0x3b, (UChar)0x0311          /*  non-spacing inverted breve */
        , 0x3b, (UChar)0x0312          /*  non-spacing turned comma above/cedilla above */
        , 0x3b, (UChar)0x0313          /*  non-spacing comma above */
        , 0x3b, (UChar)0x0314          /*  non-spacing reversed comma above */
        , 0x3b, (UChar)0x0315          /*  non-spacing comma above right */
        , 0x3b, (UChar)0x0316          /*  non-spacing grave below */
        , 0x3b, (UChar)0x0317          /*  non-spacing acute below */
        , 0x3b, (UChar)0x0318          /*  non-spacing left tack below */
        , 0x3b, (UChar)0x0319          /*  non-spacing tack below */
        , 0x3b, (UChar)0x031a          /*  non-spacing left angle above */
        , 0x3b, (UChar)0x031b          /*  non-spacing horn */
        , 0x3b, (UChar)0x031c          /*  non-spacing left half ring below */
        , 0x3b, (UChar)0x031d          /*  non-spacing up tack below */
        , 0x3b, (UChar)0x031e          /*  non-spacing down tack below */
        , 0x3b, (UChar)0x031f          /*  non-spacing plus sign below */
        , 0x3b, (UChar)0x0320          /*  non-spacing minus sign below */
        , 0x3b, (UChar)0x0321          /*  non-spacing palatalized hook below */
        , 0x3b, (UChar)0x0322          /*  non-spacing retroflex hook below */
        , 0x3b, (UChar)0x0324          /*  non-spacing double dot below */
        , 0x3b, (UChar)0x0325          /*  non-spacing ring below */
        , 0x3b, (UChar)0x0326          /*  non-spacing comma below */
        , 0x3b, (UChar)0x0329          /*  non-spacing vertical line below */
        , 0x3b, (UChar)0x032a          /*  non-spacing bridge below */
        , 0x3b, (UChar)0x032b          /*  non-spacing inverted double arch below */
        , 0x3b, (UChar)0x032c          /*  non-spacing hacek below */
        , 0x3b, (UChar)0x032d          /*  non-spacing circumflex below */
        , 0x3b, (UChar)0x032e          /*  non-spacing breve below */
        , 0x3b, (UChar)0x032f          /*  non-spacing inverted breve below */
        , 0x3b, (UChar)0x0330          /*  non-spacing tilde below */
        , 0x3b, (UChar)0x0331          /*  non-spacing macron below */
        , 0x3b, (UChar)0x0333          /*  non-spacing double underscore */
        , 0x3b, (UChar)0x0334          /*  non-spacing tilde overlay */
        , 0x3b, (UChar)0x0335          /*  non-spacing short bar overlay */
        , 0x3b, (UChar)0x0336          /*  non-spacing long bar overlay */
        , 0x3b, (UChar)0x0338          /*  non-spacing long slash overlay */
        , 0x3b, (UChar)0x0339          /*  non-spacing right half ring below */
        , 0x3b, (UChar)0x033a          /*  non-spacing inverted bridge below */
        , 0x3b, (UChar)0x033b          /*  non-spacing square below */
        , 0x3b, (UChar)0x033c          /*  non-spacing seagull below */
        , 0x3b, (UChar)0x033d          /*  non-spacing x above */
        , 0x3b, (UChar)0x033e          /*  non-spacing vertical tilde */
        , 0x3b, (UChar)0x033f          /*  non-spacing double overscore */
        , 0x3b, (UChar)0x0340          /*  non-spacing grave tone mark */
        , 0x3b, (UChar)0x0341          /*  non-spacing acute tone mark */
        , 0x3b, (UChar)0x0342, 0x3b, (UChar)0x0343, 0x3b, (UChar)0x0344, 0x3b, (UChar)0x0345, 0x3b, (UChar)0x0360, 0x3b, (UChar)0x0361    /*  newer */
        , 0x3b, (UChar)0x0483, 0x3b, (UChar)0x0484, 0x3b, (UChar)0x0485, 0x3b, (UChar)0x0486    /*  Cyrillic accents */

        , 0x3b, (UChar)0x20D0, 0x3b, (UChar)0x20D1, 0x3b, (UChar)0x20D2           /*  symbol accents */
        , 0x3b, (UChar)0x20D3, 0x3b, (UChar)0x20D4, 0x3b, (UChar)0x20D5           /*  symbol accents */
        , 0x3b, (UChar)0x20D6, 0x3b, (UChar)0x20D7, 0x3b, (UChar)0x20D8           /*  symbol accents */
        , 0x3b, (UChar)0x20D9, 0x3b, (UChar)0x20DA, 0x3b, (UChar)0x20DB           /*  symbol accents */
        , 0x3b, (UChar)0x20DC, 0x3b, (UChar)0x20DD, 0x3b, (UChar)0x20DE           /*  symbol accents */
        , 0x3b, (UChar)0x20DF, 0x3b, (UChar)0x20E0, 0x3b, (UChar)0x20E1           /*  symbol accents */

        , 0x2c, 0x27, (UChar)0x002D, 0x27, 0x3b, (UChar)0x00AD                     /*  dashes */
        , 0x3b, (UChar)0x2010, 0x3b, (UChar)0x2011, 0x3b, (UChar)0x2012           /*  dashes */
        , 0x3b, (UChar)0x2013, 0x3b, (UChar)0x2014, 0x3b, (UChar)0x2015           /*  dashes */
        , 0x3b, (UChar)0x2212                                                       /*  dashes */

        /*  other punctuation */

        , 0x3c, 0x27, (UChar)0x005f, 0x27 /*  underline/underscore (spacing) */
        , 0x3c, (UChar)0x00af          /*  overline or macron (spacing) */
/*         , 0x3c, (UChar)0x00ad          /* syllable hyphen (SHY) or soft hyphen */
        , 0x3c, 0x27, (UChar)0x002c, 0x27           /*  comma (spacing) */
        , 0x3c, 0x27, (UChar)0x003b, 0x27           /*  semicolon */
        , 0x3c, 0x27, (UChar)0x003a, 0x27           /*  colon */
        , 0x3c, 0x27, (UChar)0x0021, 0x27           /*  exclamation point */
        , 0x3c, (UChar)0x00a1                       /*  inverted exclamation point */
        , 0x3c, 0x27, (UChar)0x003f, 0x27           /*  question mark */
        , 0x3c, (UChar)0x00bf                       /*  inverted question mark */
        , 0x3c, 0x27, (UChar)0x002f, 0x27           /*  slash */
        , 0x3c, 0x27, (UChar)0x002e, 0x27           /*  period/full stop */
        , 0x3c, (UChar)0x00b4                       /*  acute accent (spacing) */
        , 0x3c, 0x27, (UChar)0x0060, 0x27           /*  grave accent (spacing) */
        , 0x3c, 0x27, (UChar)0x005e, 0x27           /*  circumflex accent (spacing) */
        , 0x3c, (UChar)0x00a8                       /*  diaresis/umlaut accent (spacing) */
        , 0x3c, 0x27, (UChar)0x007e, 0x27           /*  tilde accent (spacing) */
        , 0x3c, (UChar)0x00b7                       /*  middle dot (spacing) */
        , 0x3c, (UChar)0x00b8                       /*  cedilla accent (spacing) */
        , 0x3c, 0x27, (UChar)0x0027, 0x27           /*  apostrophe */
        , 0x3c, 0x27, 0x22, 0x27                       /*  quotation marks */
        , 0x3c, (UChar)0x00ab                       /*  left angle quotes */
        , 0x3c, (UChar)0x00bb                       /*  right angle quotes */
        , 0x3c, 0x27, (UChar)0x0028, 0x27           /*  left parenthesis */
        , 0x3c, 0x27, (UChar)0x0029, 0x27           /*  right parenthesis */
        , 0x3c, 0x27, (UChar)0x005b, 0x27           /*  left bracket */
        , 0x3c, 0x27, (UChar)0x005d, 0x27           /*  right bracket */
        , 0x3c, 0x27, (UChar)0x007b, 0x27           /*  left brace */
        , 0x3c, 0x27, (UChar)0x007d, 0x27           /*  right brace */
        , 0x3c, (UChar)0x00a7                       /*  section symbol */
        , 0x3c, (UChar)0x00b6                       /*  paragraph symbol */
        , 0x3c, (UChar)0x00a9                       /*  copyright symbol */
        , 0x3c, (UChar)0x00ae                       /*  registered trademark symbol */
        , 0x3c, 0x27, (UChar)0x0040, 0x27           /*  at sign */
        , 0x3c, (UChar)0x00a4                       /*  international currency symbol */
        , 0x3c, (UChar)0x00a2                       /*  cent sign */
        , 0x3c, 0x27, (UChar)0x0024, 0x27           /*  dollar sign */
        , 0x3c, (UChar)0x00a3                       /*  pound-sterling sign */
        , 0x3c, (UChar)0x00a5                       /*  yen sign */
        , 0x3c, 0x27, (UChar)0x002a, 0x27           /*  asterisk */
        , 0x3c, 0x27, (UChar)0x005c, 0x27           /*  backslash */
        , 0x3c, 0x27, (UChar)0x0026, 0x27           /*  ampersand */
        , 0x3c, 0x27, (UChar)0x0023, 0x27           /*  number sign */
        , 0x3c, 0x27, (UChar)0x0025, 0x27           /*  percent sign */
        , 0x3c, 0x27, (UChar)0x002b, 0x27           /*  plus sign */
/*         , 0x3c, (UChar)0x002d                    */ /* hyphen or minus sign */
        , 0x3c, (UChar)0x00b1                       /*  plus-or-minus sign */
        , 0x3c, (UChar)0x00f7                       /*  divide sign */
        , 0x3c, (UChar)0x00d7                       /*  multiply sign */
        , 0x3c, 0x27, (UChar)0x003c, 0x27           /*  less-than sign */
        , 0x3c, 0x27, (UChar)0x003d, 0x27           /*  equal sign */
        , 0x3c, 0x27, (UChar)0x003e, 0x27           /*  greater-than sign */
        , 0x3c, (UChar)0x00ac                       /*  end of line symbol/logical NOT symbol */
        , 0x3c, 0x27, (UChar)0x007c, 0x27           /*  vertical line/logical OR symbol */
        , 0x3c, (UChar)0x00a6                       /*  broken vertical line */
        , 0x3c, (UChar)0x00b0                       /*  degree symbol */
        , 0x3c, (UChar)0x00b5                       /*  micro symbol */

        /*  NUMERICS */

        , 0x3c, 0x30, 0x3c, 0x31, 0x3c, 0x32, 0x3c, 0x33, 0x3c, 0x34, 0x3c, 0x35, 0x3c, 0x36, 0x3c, 0x37, 0x3c, 0x38, 0x3c, 0x39 
        , 0x3c, (UChar)0x00bc, 0x3c, (UChar)0x00bd, 0x3c, (UChar)0x00be    /*  1/4,1/2,3/4 fractions */

        /*  NON-IGNORABLES */
        , 0x3c, 0x61, 0x2c, 0x41
        , 0x3c, 0x62, 0x2c, 0x42
        , 0x3c, 0x63, 0x2c, 0x43
        , 0x3c, 0x64, 0x2c, 0x44
        , 0x3c, (UChar)0x00F0, 0x2c, (UChar)0x00D0              /*  eth */
        , 0x3c, 0x65, 0x2c, 0x45
        , 0x3c, 0x66, 0x2c, 0x46
        , 0x3c, 0x67, 0x2c, 0x47
        , 0x3c, 0x68, 0x2c, 0x48
        , 0x3c, 0x69, 0x2c, 0x49
        , 0x3c, 0x6a, 0x2c, 0x4a
        , 0x3c, 0x6b, 0x2c, 0x4b
        , 0x3c, 0x6c, 0x2c, 0x4c
        , 0x3c, 0x6d, 0x2c, 0x4d
        , 0x3c, 0x6e, 0x2c, 0x4e
        , 0x3c, 0x6f, 0x2c, 0x4f
        , 0x3c, 0x70, 0x2c, 0x50
        , 0x3c, 0x71, 0x2c, 0x51
        , 0x3c, 0x72, 0x2c, 0x52
        , 0x3c, 0x73, 0x2c, 0x53, 0x26, 0x53, 0x53, 0x2c, (UChar)0x00DF /*  s-zet */
        , 0x3c, 0x74, 0x2c, 0x54
        , 0x26, 0x54, 0x48, 0x2c, 0x00FE, 0x26, 0x54, 0x48, 0x2c, (UChar)0x00DE  /*  thorn */
        , 0x3c, 0x75, 0x2c, 0x55
        , 0x3c, 0x76, 0x2c, 0x56
        , 0x3c, 0x77, 0x2c, 0x57
        , 0x3c, 0x78, 0x2c, 0x58
        , 0x3c, 0x79, 0x2c, 0x59
        , 0x3c, 0x7a, 0x2c, 0x5a
        , 0x26, 0x41, 0x45, 0x2c, (UChar)0x00C6                    /*  ae & AE ligature */
        , 0x26, 0x41, 0x45, 0x2c, (UChar)0x00E6
        , 0x26, 0x4f, 0x45, 0x2c, (UChar)0x0152                    /*  oe & OE ligature */
        , 0x26, 0x4f, 0x45, 0x2c, (UChar)0x0153
        , (UChar)0x0000
};

const UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {0x61, 0x62, 0x27, 0x63, 0},
    {0x63, 0x6f, 0x2d, 0x6f, 0x70, 0},
    {0x61, 0x62, 0},
    {0x61, 0x6d, 0x70, 0x65, 0x72, 0x73, 0x61, 0x64, 0},
    {0x61, 0x6c, 0x6c, 0},
    {0x66, 0x6f, 0x75, 0x72, 0},
    {0x66, 0x69, 0x76, 0x65, 0},
    {0x31, 0},
    {0x31, 0},
    {0x31, 0},                                            /*  10 */
    {0x32, 0},
    {0x32, 0},
    {0x48, 0x65, 0x6c, 0x6c, 0x6f, 0},
    {0x61, 0x3c, 0x62, 0},
    {0x61, 0x3c, 0x62, 0},
    {0x61, 0x63, 0x63, 0},
    {0x61, 0x63, 0x48, 0x63, 0},  /*  simple test */
    {0x70, 0x00EA, 0x63, 0x68, 0x65, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x62, 0x63, 0},                                  /*  20 */
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x00E6, 0x63, 0},
    {0x61, 0x63, 0x48, 0x63, 0},  /*  primary test */
    {0x62, 0x6c, 0x61, 0x63, 0x6b, 0},
    {0x66, 0x6f, 0x75, 0x72, 0},
    {0x66, 0x69, 0x76, 0x65, 0},
    {0x31, 0},
    {0x61, 0x62, 0x63, 0},                                        /*  30 */
    {0x61, 0x62, 0x63, 0},                                  
    {0x61, 0x62, 0x63, 0x48, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x63, 0x48, 0x63, 0},                              /*  34 */
    {0x61, 0x63, 0x65, 0x30},
    {0x31, 0x30},
    {0x70, 0x00EA,0x30}                                    /* 37     */
};

const UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {0x61, 0x62, 0x63, 0x27, 0},
    {0x43, 0x4f, 0x4f, 0x50, 0},
    {0x61, 0x62, 0x63, 0},
    {0x26, 0},
    {0x26, 0},
    {0x34, 0},
    {0x35, 0},
    {0x6f, 0x6e, 0x65, 0},
    {0x6e, 0x6e, 0x65, 0},
    {0x70, 0x6e, 0x65, 0},                                  /*  10 */
    {0x74, 0x77, 0x6f, 0},
    {0x75, 0x77, 0x6f, 0},
    {0x68, 0x65, 0x6c, 0x6c, 0x4f, 0},
    {0x61, 0x3c, 0x3d, 0x62, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x43, 0x48, 0x63, 0},
    {0x61, 0x43, 0x48, 0x63, 0},  /*  simple test */
    {0x70, (UChar)0x00E9, 0x63, 0x68, 0x00E9, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x42, 0x43, 0},                                  /*  20 */
    {0x61, 0x62, 0x63, 0x68, 0},
    {0x61, 0x62, 0x64, 0},
    {(UChar)0x00E4, 0x62, 0x63, 0},
    {0x61, (UChar)0x00C6, 0x63, 0},
    {0x61, 0x43, 0x48, 0x63, 0},  /*  primary test */
    {0x62, 0x6c, 0x61, 0x63, 0x6b, 0x2d, 0x62, 0x69, 0x72, 0x64, 0},
    {0x34, 0},
    {0x35, 0},
    {0x6f, 0x6e, 0x65, 0},
    {0x61, 0x62, 0x63, 0},
    {0x61, 0x42, 0x63, 0},                                  /*  30 */
    {0x61, 0x62, 0x63, 0x68, 0},
    {0x61, 0x62, 0x64, 0},
    {0x61, 0x43, 0x48, 0x63, 0},                                /*  34 */
    {0x61, 0x63, 0x65, 0x30},
    {0x31, 0x30},
    {0x70, (UChar)0x00EB,0x30}                                    /* 37 */
};

const UCollationResult results[] = {
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_GREATER,
    UCOL_LESS,                                     /*  10 */
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    /*  test primary > 17 */
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_EQUAL,                                    /*  20 */
    UCOL_LESS,
    UCOL_LESS,
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_LESS,
    /*  test secondary > 26 */
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_EQUAL,                                    /*  30 */
    UCOL_EQUAL,
    UCOL_LESS,
    UCOL_EQUAL,                                     /*  34 */
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_LESS                                        /* 37 */
};

const UChar testCases[][MAX_TOKEN_LEN] =
{
    {0x61, 0},
    {0x41, 0},
    {0x61, 0x65, 0},
    {0x61, 0x45, 0},
    {0x41, 0x65, 0},
    {0x41, 0x45, 0},
    {(UChar)0x00e6, 0},
    {(UChar)0x00c6, 0},
    {0x62, 0},
    {0x63, 0},
    {0x7a, 0}
};


void addAllCollTest(TestNode** root)
{
    
    
    addTest(root, &TestPrimary, "tscoll/callcoll/TestPrimary");
    addTest(root, &TestSecondary, "tscoll/callcoll/TestSecondary");
    addTest(root, &TestTertiary, "tscoll/callcoll/TestTertiary");
    addTest(root, &TestIdentical, "tscoll/callcoll/TestIdentical");
    addTest(root, &TestExtra, "tscoll/callcoll/TestExtra");
        

}

void doTest(UCollator* myCollation, const UChar source[], const UChar target[], UCollationResult result)
{
    int32_t sortklen;
    UCollationResult compareResult = 0, keyResult = 0;
    uint8_t *sortKey1 = 0, *sortKey2 = 0;
    int res;
    compareResult = ucol_strcoll(myCollation, source, u_strlen(source), target, u_strlen(target));
    
    sortklen=ucol_getSortKey(myCollation, source, u_strlen(source),  NULL, 0);
    sortKey1=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    ucol_getSortKey(myCollation, source, u_strlen(source), sortKey1, sortklen+1);
    
    sortklen=ucol_getSortKey(myCollation, target, u_strlen(target),  NULL, 0);
    sortKey2=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    ucol_getSortKey(myCollation, target, u_strlen(target), sortKey2, sortklen+1);
    
    res = memcmp(sortKey1, sortKey2, sortklen) ;
    if (res < 0) keyResult = -1;
    else if (res > 0) keyResult = 1;
    else keyResult = 0;
    
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, result );
}

void TestTertiary()
{
    int32_t len,i;
    UChar *rules, *newRules;
    UErrorCode status=U_ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    newRules=(UChar*)malloc(sizeof(UChar*) * (strlen(str)+1));
    u_uastrcpy(newRules, str);
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator :%s\n", myErrorName(status));
    }
   
   /* ucol_setNormalization(myCollation, UCOL_DEFAULT_NORMALIZATION); */
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 17 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    free(newRules);
    free(rules);
    ucol_close(myCollation);
    myCollation = 0;
}

void TestPrimary( )
{
    int32_t len,i;
    UChar *rules, *newRules;
    UErrorCode status=U_ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    
    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
    
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator :%s\n", myErrorName(status));
    }
    ucol_setNormalization(myCollation, UCOL_DEFAULT_NORMALIZATION);
    ucol_setStrength(myCollation, UCOL_PRIMARY);
    
    for (i = 17; i < 26 ; i++)
    {
        
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    free(newRules);
    free(rules);
    ucol_close(myCollation);
    myCollation = 0;
}

void TestSecondary()
{
    int32_t i;
    int32_t len;
    UChar *rules, *newRules;
    UErrorCode status=U_ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    
    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
    
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator :%s\n", myErrorName(status));
    }
    ucol_setStrength(myCollation, UCOL_SECONDARY);
    for (i = 26; i < 34 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    free(newRules);
    free(rules);
    ucol_close(myCollation);
    myCollation = 0;
}
void TestIdentical()
{
    int32_t i;
    int32_t len;
    UChar *rules = 0, *newRules = 0;
    UErrorCode status=U_ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";

    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
   
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_IDENTICAL, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator :%s\n", myErrorName(status));
    }
    for(i= 34; i<37; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    free(newRules);
    free(rules);
    ucol_close(myCollation);
    myCollation = 0;
}
void TestExtra()
{
    int32_t i, j;
    int32_t len;
    UChar *rules, *newRules;
    UErrorCode status = U_ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
    log_verbose("Testing extra\n");
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    

    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator :%s\n", myErrorName(status));
    }
    ucol_setNormalization(myCollation, UCOL_DEFAULT_NORMALIZATION); 
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 10 ; i++)
    {
        for (j = i + 1; j < 11; j += 1)
        {
        
            doTest(myCollation, testCases[i], testCases[j], UCOL_LESS);
        }
    }
    free(newRules);
    free(rules);
    ucol_close(myCollation);
    myCollation = 0;
}
