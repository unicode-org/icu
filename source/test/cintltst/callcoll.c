/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
*
* File CALLCOLL.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Ported for CAPI
*********************************************************************************
*/
/**
 * CollationDummyTest is a third level test class.  This tests creation of 
 * a customized collator object.  For example, number 1 to be sorted 
 * equlivalent to word 'one'.
 */
#include "utypes.h"
#include "ucol.h"
#include "uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "callcoll.h"
#include "ustring.h"
#include <string.h>
#include <memory.h>
static UCollator *myCollation;
static const UChar DEFAULTRULEARRAY[] =
{
          '=', '\'', (UChar)0x200B, '\'', '=', (UChar)0x200C, '=', (UChar)0x200D, '=', (UChar)0x200E, '=', (UChar)0x200F
        , '=', (UChar)0x0001, '=', (UChar)0x0002, '=', (UChar)0x0003, '=', (UChar)0x0004
        , '=', (UChar)0x0005, '=', (UChar)0x0006, '=', (UChar)0x0007, '=', (UChar)0x0008, '=', '\'', (UChar)0x0009, '\''
        , '=', '\'', (UChar)0x000b, '\'',  '=', (UChar)0x000e       /* vt,, so */
        , '=', (UChar)0x000f, '=', '\'', (UChar)0x0010, '\'',  '=', (UChar)0x0011, '=', (UChar)0x0012, '=', (UChar)0x0013 /* si, dle, dc1, dc2, dc3 */
        , '=', (UChar)0x0014, '=', (UChar)0x0015, '=', (UChar)0x0016, '=', (UChar)0x0017, '=', (UChar)0x0018 /* dc4, nak, syn, etb, can */
        , '=', (UChar)0x0019, '=', (UChar)0x001a, '=', (UChar)0x001b, '=', (UChar)0x001c, '=', (UChar)0x001d /* em, sub, esc, fs, gs */
        , '=', (UChar)0x001e, '=', (UChar)0x001f, '=', (UChar)0x007f                   /* rs, us, del */
        /* ....then the C1 Latin 1 reserved control codes */
        , '=', (UChar)0x0080, '=', (UChar)0x0081, '=', (UChar)0x0082, '=', (UChar)0x0083, '=', (UChar)0x0084, '=', (UChar)0x0085
        , '=', (UChar)0x0086, '=', (UChar)0x0087, '=', (UChar)0x0088, '=', (UChar)0x0089, '=', (UChar)0x008a, '=', (UChar)0x008b
        , '=', (UChar)0x008c, '=', (UChar)0x008d, '=', (UChar)0x008e, '=', (UChar)0x008f, '=', (UChar)0x0090, '=', (UChar)0x0091
        , '=', (UChar)0x0092, '=', (UChar)0x0093, '=', (UChar)0x0094, '=', (UChar)0x0095, '=', (UChar)0x0096, '=', (UChar)0x0097
        , '=', (UChar)0x0098, '=', (UChar)0x0099, '=', (UChar)0x009a, '=', (UChar)0x009b, '=', (UChar)0x009c, '=', (UChar)0x009d
        , '=', (UChar)0x009e, '=', (UChar)0x009f
        /*  IGNORE except for secondary, tertiary difference */
        /*  Spaces */
        , ';', '\'', (UChar)0x0020, '\'', ';', '\'', (UChar)0x00A0, '\''                   /*  spaces */
        , ';', '\'', (UChar)0x2000, '\'', ';', '\'', (UChar)0x2001, '\'', ';', '\'', (UChar)0x2002, '\'', ';', '\'', (UChar)0x2003, '\'', ';', '\'', (UChar)0x2004, '\''   /*  spaces */
        , ';', '\'', (UChar)0x2005, '\'', ';', '\'', (UChar)0x2006, '\'', ';', '\'', (UChar)0x2007, '\'', ';', '\'', (UChar)0x2008, '\'', ';', '\'', (UChar)0x2009, '\''   /*  spaces */
        , ';', '\'', (UChar)0x200A, '\'', ';', '\'', (UChar)0x3000, '\'', ';', '\'', (UChar)0xFEFF, '\''                 /*  spaces */
        , ';', '\'', '\r', '\'',  ';', '\'', '\t', '\'',  ';', '\'', '\n', '\'', ';', '\'', '\f', '\'', ';', '\'', (UChar)0x000b, '\''   /*  whitespace */

        /*  Non-spacing accents */

        , ';', (UChar)0x0301          /*  non-spacing acute accent */
        , ';', (UChar)0x0300          /*  non-spacing grave accent */
        , ';', (UChar)0x0306          /*  non-spacing breve accent */
        , ';', (UChar)0x0302          /*  non-spacing circumflex accent */
        , ';', (UChar)0x030c          /*  non-spacing caron/hacek accent */
        , ';', (UChar)0x030a          /*  non-spacing ring above accent */
        , ';', (UChar)0x030d          /*  non-spacing vertical line above */
        , ';', (UChar)0x0308          /*  non-spacing diaeresis accent */
        , ';', (UChar)0x030b          /*  non-spacing double acute accent */
        , ';', (UChar)0x0303          /*  non-spacing tilde accent */
        , ';', (UChar)0x0307          /*  non-spacing dot above/overdot accent */
        , ';', (UChar)0x0304          /*  non-spacing macron accent */
        , ';', (UChar)0x0337          /*  non-spacing short slash overlay (overstruck diacritic) */
        , ';', (UChar)0x0327          /*  non-spacing cedilla accent */
        , ';', (UChar)0x0328          /*  non-spacing ogonek accent */
        , ';', (UChar)0x0323          /*  non-spacing dot-below/underdot accent */
        , ';', (UChar)0x0332          /*  non-spacing underscore/underline accent */
        /*  with the rest of the general diacritical marks in binary order */
        , ';', (UChar)0x0305          /*  non-spacing overscore/overline */
        , ';', (UChar)0x0309          /*  non-spacing hook above */
        , ';', (UChar)0x030e          /*  non-spacing double vertical line above */
        , ';', (UChar)0x030f          /*  non-spacing double grave */
        , ';', (UChar)0x0310          /*  non-spacing chandrabindu */
        , ';', (UChar)0x0311          /*  non-spacing inverted breve */
        , ';', (UChar)0x0312          /*  non-spacing turned comma above/cedilla above */
        , ';', (UChar)0x0313          /*  non-spacing comma above */
        , ';', (UChar)0x0314          /*  non-spacing reversed comma above */
        , ';', (UChar)0x0315          /*  non-spacing comma above right */
        , ';', (UChar)0x0316          /*  non-spacing grave below */
        , ';', (UChar)0x0317          /*  non-spacing acute below */
        , ';', (UChar)0x0318          /*  non-spacing left tack below */
        , ';', (UChar)0x0319          /*  non-spacing tack below */
        , ';', (UChar)0x031a          /*  non-spacing left angle above */
        , ';', (UChar)0x031b          /*  non-spacing horn */
        , ';', (UChar)0x031c          /*  non-spacing left half ring below */
        , ';', (UChar)0x031d          /*  non-spacing up tack below */
        , ';', (UChar)0x031e          /*  non-spacing down tack below */
        , ';', (UChar)0x031f          /*  non-spacing plus sign below */
        , ';', (UChar)0x0320          /*  non-spacing minus sign below */
        , ';', (UChar)0x0321          /*  non-spacing palatalized hook below */
        , ';', (UChar)0x0322          /*  non-spacing retroflex hook below */
        , ';', (UChar)0x0324          /*  non-spacing double dot below */
        , ';', (UChar)0x0325          /*  non-spacing ring below */
        , ';', (UChar)0x0326          /*  non-spacing comma below */
        , ';', (UChar)0x0329          /*  non-spacing vertical line below */
        , ';', (UChar)0x032a          /*  non-spacing bridge below */
        , ';', (UChar)0x032b          /*  non-spacing inverted double arch below */
        , ';', (UChar)0x032c          /*  non-spacing hacek below */
        , ';', (UChar)0x032d          /*  non-spacing circumflex below */
        , ';', (UChar)0x032e          /*  non-spacing breve below */
        , ';', (UChar)0x032f          /*  non-spacing inverted breve below */
        , ';', (UChar)0x0330          /*  non-spacing tilde below */
        , ';', (UChar)0x0331          /*  non-spacing macron below */
        , ';', (UChar)0x0333          /*  non-spacing double underscore */
        , ';', (UChar)0x0334          /*  non-spacing tilde overlay */
        , ';', (UChar)0x0335          /*  non-spacing short bar overlay */
        , ';', (UChar)0x0336          /*  non-spacing long bar overlay */
        , ';', (UChar)0x0338          /*  non-spacing long slash overlay */
        , ';', (UChar)0x0339          /*  non-spacing right half ring below */
        , ';', (UChar)0x033a          /*  non-spacing inverted bridge below */
        , ';', (UChar)0x033b          /*  non-spacing square below */
        , ';', (UChar)0x033c          /*  non-spacing seagull below */
        , ';', (UChar)0x033d          /*  non-spacing x above */
        , ';', (UChar)0x033e          /*  non-spacing vertical tilde */
        , ';', (UChar)0x033f          /*  non-spacing double overscore */
        , ';', (UChar)0x0340          /*  non-spacing grave tone mark */
        , ';', (UChar)0x0341          /*  non-spacing acute tone mark */
        , ';', (UChar)0x0342, ';', (UChar)0x0343, ';', (UChar)0x0344, ';', (UChar)0x0345, ';', (UChar)0x0360, ';', (UChar)0x0361    /*  newer */
        , ';', (UChar)0x0483, ';', (UChar)0x0484, ';', (UChar)0x0485, ';', (UChar)0x0486    /*  Cyrillic accents */

        , ';', (UChar)0x20D0, ';', (UChar)0x20D1, ';', (UChar)0x20D2           /*  symbol accents */
        , ';', (UChar)0x20D3, ';', (UChar)0x20D4, ';', (UChar)0x20D5           /*  symbol accents */
        , ';', (UChar)0x20D6, ';', (UChar)0x20D7, ';', (UChar)0x20D8           /*  symbol accents */
        , ';', (UChar)0x20D9, ';', (UChar)0x20DA, ';', (UChar)0x20DB           /*  symbol accents */
        , ';', (UChar)0x20DC, ';', (UChar)0x20DD, ';', (UChar)0x20DE           /*  symbol accents */
        , ';', (UChar)0x20DF, ';', (UChar)0x20E0, ';', (UChar)0x20E1           /*  symbol accents */

        , ',', '\'', (UChar)0x002D, '\'', ';', (UChar)0x00AD                     /*  dashes */
        , ';', (UChar)0x2010, ';', (UChar)0x2011, ';', (UChar)0x2012           /*  dashes */
        , ';', (UChar)0x2013, ';', (UChar)0x2014, ';', (UChar)0x2015           /*  dashes */
        , ';', (UChar)0x2212                                                       /*  dashes */

        /*  other punctuation */

        , '<', '\'', (UChar)0x005f, '\'' /*  underline/underscore (spacing) */
        , '<', (UChar)0x00af          /*  overline or macron (spacing) */
/*         , '<', (UChar)0x00ad          /* syllable hyphen (SHY) or soft hyphen */
        , '<', '\'', (UChar)0x002c, '\''           /*  comma (spacing) */
        , '<', '\'', (UChar)0x003b, '\''           /*  semicolon */
        , '<', '\'', (UChar)0x003a, '\''           /*  colon */
        , '<', '\'', (UChar)0x0021, '\''           /*  exclamation point */
        , '<', (UChar)0x00a1                       /*  inverted exclamation point */
        , '<', '\'', (UChar)0x003f, '\''           /*  question mark */
        , '<', (UChar)0x00bf                       /*  inverted question mark */
        , '<', '\'', (UChar)0x002f, '\''           /*  slash */
        , '<', '\'', (UChar)0x002e, '\''           /*  period/full stop */
        , '<', (UChar)0x00b4                       /*  acute accent (spacing) */
        , '<', '\'', (UChar)0x0060, '\''           /*  grave accent (spacing) */
        , '<', '\'', (UChar)0x005e, '\''           /*  circumflex accent (spacing) */
        , '<', (UChar)0x00a8                       /*  diaresis/umlaut accent (spacing) */
        , '<', '\'', (UChar)0x007e, '\''           /*  tilde accent (spacing) */
        , '<', (UChar)0x00b7                       /*  middle dot (spacing) */
        , '<', (UChar)0x00b8                       /*  cedilla accent (spacing) */
        , '<', '\'', (UChar)0x0027, '\''           /*  apostrophe */
        , '<', '\'', '"', '\''                       /*  quotation marks */
        , '<', (UChar)0x00ab                       /*  left angle quotes */
        , '<', (UChar)0x00bb                       /*  right angle quotes */
        , '<', '\'', (UChar)0x0028, '\''           /*  left parenthesis */
        , '<', '\'', (UChar)0x0029, '\''           /*  right parenthesis */
        , '<', '\'', (UChar)0x005b, '\''           /*  left bracket */
        , '<', '\'', (UChar)0x005d, '\''           /*  right bracket */
        , '<', '\'', (UChar)0x007b, '\''           /*  left brace */
        , '<', '\'', (UChar)0x007d, '\''           /*  right brace */
        , '<', (UChar)0x00a7                       /*  section symbol */
        , '<', (UChar)0x00b6                       /*  paragraph symbol */
        , '<', (UChar)0x00a9                       /*  copyright symbol */
        , '<', (UChar)0x00ae                       /*  registered trademark symbol */
        , '<', '\'', (UChar)0x0040, '\''           /*  at sign */
        , '<', (UChar)0x00a4                       /*  international currency symbol */
        , '<', (UChar)0x00a2                       /*  cent sign */
        , '<', '\'', (UChar)0x0024, '\''           /*  dollar sign */
        , '<', (UChar)0x00a3                       /*  pound-sterling sign */
        , '<', (UChar)0x00a5                       /*  yen sign */
        , '<', '\'', (UChar)0x002a, '\''           /*  asterisk */
        , '<', '\'', (UChar)0x005c, '\''           /*  backslash */
        , '<', '\'', (UChar)0x0026, '\''           /*  ampersand */
        , '<', '\'', (UChar)0x0023, '\''           /*  number sign */
        , '<', '\'', (UChar)0x0025, '\''           /*  percent sign */
        , '<', '\'', (UChar)0x002b, '\''           /*  plus sign */
/*         , '<', (UChar)0x002d                    */ /* hyphen or minus sign */
        , '<', (UChar)0x00b1                       /*  plus-or-minus sign */
        , '<', (UChar)0x00f7                       /*  divide sign */
        , '<', (UChar)0x00d7                       /*  multiply sign */
        , '<', '\'', (UChar)0x003c, '\''           /*  less-than sign */
        , '<', '\'', (UChar)0x003d, '\''           /*  equal sign */
        , '<', '\'', (UChar)0x003e, '\''           /*  greater-than sign */
        , '<', (UChar)0x00ac                       /*  end of line symbol/logical NOT symbol */
        , '<', '\'', (UChar)0x007c, '\''           /*  vertical line/logical OR symbol */
        , '<', (UChar)0x00a6                       /*  broken vertical line */
        , '<', (UChar)0x00b0                       /*  degree symbol */
        , '<', (UChar)0x00b5                       /*  micro symbol */

        /*  NUMERICS */

        , '<', '0', '<', '1', '<', '2', '<', '3', '<', '4', '<', '5', '<', '6', '<', '7', '<', '8', '<', '9' 
        , '<', (UChar)0x00bc, '<', (UChar)0x00bd, '<', (UChar)0x00be    /*  1/4,1/2,3/4 fractions */

        /*  NON-IGNORABLES */
        , '<', 'a', ',', 'A'
        , '<', 'b', ',', 'B'
        , '<', 'c', ',', 'C'
        , '<', 'd', ',', 'D'
        , '<', (UChar)0x00F0, ',', (UChar)0x00D0              /*  eth */
        , '<', 'e', ',', 'E'
        , '<', 'f', ',', 'F'
        , '<', 'g', ',', 'G'
        , '<', 'h', ',', 'H'
        , '<', 'i', ',', 'I'
        , '<', 'j', ',', 'J'
        , '<', 'k', ',', 'K'
        , '<', 'l', ',', 'L'
        , '<', 'm', ',', 'M'
        , '<', 'n', ',', 'N'
        , '<', 'o', ',', 'O'
        , '<', 'p', ',', 'P'
        , '<', 'q', ',', 'Q'
        , '<', 'r', ',', 'R'
        , '<', 's', ',', 'S', '&', 'S', 'S', ',', (UChar)0x00DF /*  s-zet */
        , '<', 't', ',', 'T'
        , '&', 'T', 'H', ',', 0x00FE, '&', 'T', 'H', ',', (UChar)0x00DE  /*  thorn */
        , '<', 'u', ',', 'U'
        , '<', 'v', ',', 'V'
        , '<', 'w', ',', 'W'
        , '<', 'x', ',', 'X'
        , '<', 'y', ',', 'Y'
        , '<', 'z', ',', 'Z'
        , '&', 'A', 'E', ',', (UChar)0x00C6                    /*  ae & AE ligature */
        , '&', 'A', 'E', ',', (UChar)0x00E6
        , '&', 'O', 'E', ',', (UChar)0x0152                    /*  oe & OE ligature */
        , '&', 'O', 'E', ',', (UChar)0x0153
        , (UChar)0x0000
};

const UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {'a', 'b', '\'', 'c', 0},
    {'c', 'o', '-', 'o', 'p', 0},
    {'a', 'b', 0},
    {'a', 'm', 'p', 'e', 'r', 's', 'a', 'd', 0},
    {'a', 'l', 'l', 0},
    {'f', 'o', 'u', 'r', 0},
    {'f', 'i', 'v', 'e', 0},
    {'1', 0},
    {'1', 0},
    {'1', 0},                                            /*  10 */
    {'2', 0},
    {'2', 0},
    {'H', 'e', 'l', 'l', 'o', 0},
    {'a', '<', 'b', 0},
    {'a', '<', 'b', 0},
    {'a', 'c', 'c', 0},
    {'a', 'c', 'H', 'c', 0},  /*  simple test */
    {'p', 0x00EA, 'c', 'h', 'e', 0},
    {'a', 'b', 'c', 0},
    {'a', 'b', 'c', 0},                                  /*  20 */
    {'a', 'b', 'c', 0},
    {'a', 'b', 'c', 0},
    {'a', 'b', 'c', 0},
    {'a', 0x00E6, 'c', 0},
    {'a', 'c', 'H', 'c', 0},  /*  primary test */
    {'b', 'l', 'a', 'c', 'k', 0},
    {'f', 'o', 'u', 'r', 0},
    {'f', 'i', 'v', 'e', 0},
    {'1', 0},
    {'a', 'b', 'c', 0},                                        /*  30 */
    {'a', 'b', 'c', 0},                                  
    {'a', 'b', 'c', 'H', 0},
    {'a', 'b', 'c', 0},
    {'a', 'c', 'H', 'c', 0},                              /*  34 */
    {'a', 'c', 'e', '0'},
    {'1', '0'},
    {'p', 0x00EA,'0'}                                    /* 37     */
};

const UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {'a', 'b', 'c', '\'', 0},
    {'C', 'O', 'O', 'P', 0},
    {'a', 'b', 'c', 0},
    {'&', 0},
    {'&', 0},
    {'4', 0},
    {'5', 0},
    {'o', 'n', 'e', 0},
    {'n', 'n', 'e', 0},
    {'p', 'n', 'e', 0},                                  /*  10 */
    {'t', 'w', 'o', 0},
    {'u', 'w', 'o', 0},
    {'h', 'e', 'l', 'l', 'O', 0},
    {'a', '<', '=', 'b', 0},
    {'a', 'b', 'c', 0},
    {'a', 'C', 'H', 'c', 0},
    {'a', 'C', 'H', 'c', 0},  /*  simple test */
    {'p', (UChar)0x00E9, 'c', 'h', 0x00E9, 0},
    {'a', 'b', 'c', 0},
    {'a', 'B', 'C', 0},                                  /*  20 */
    {'a', 'b', 'c', 'h', 0},
    {'a', 'b', 'd', 0},
    {(UChar)0x00E4, 'b', 'c', 0},
    {'a', (UChar)0x00C6, 'c', 0},
    {'a', 'C', 'H', 'c', 0},  /*  primary test */
    {'b', 'l', 'a', 'c', 'k', '-', 'b', 'i', 'r', 'd', 0},
    {'4', 0},
    {'5', 0},
    {'o', 'n', 'e', 0},
    {'a', 'b', 'c', 0},
    {'a', 'B', 'c', 0},                                  /*  30 */
    {'a', 'b', 'c', 'h', 0},
    {'a', 'b', 'd', 0},
    {'a', 'C', 'H', 'c', 0},                                /*  34 */
    {'a', 'c', 'e', '0'},
    {'1', '0'},
    {'p', (UChar)0x00EB,'0'}                                    /* 37 */
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
    {'a', 0},
    {'A', 0},
    {'a', 'e', 0},
    {'a', 'E', 0},
    {'A', 'e', 0},
    {'A', 'E', 0},
    {(UChar)0x00e6, 0},
    {(UChar)0x00c6, 0},
    {'b', 0},
    {'c', 0},
    {'z', 0}
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
    UErrorCode status=ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    newRules=(UChar*)malloc(sizeof(UChar*) * (strlen(str)+1));
    u_uastrcpy(newRules, str);
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(FAILURE(status)){
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
    UErrorCode status=ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    
    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
    
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(FAILURE(status)){
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
    UErrorCode status=ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    
    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
    
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(FAILURE(status)){
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
    UErrorCode status=ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";

    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
   
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    
    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_IDENTICAL, &status);
    if(FAILURE(status)){
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
    UErrorCode status = ZERO_ERROR;
    const char* str="& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ";
    newRules=(UChar*)malloc(sizeof(UChar*) * strlen(str));
    u_uastrcpy(newRules, str);
    log_verbose("Testing extra\n");
    len=u_strlen(DEFAULTRULEARRAY) + u_strlen(newRules);
    rules=(UChar*)malloc(sizeof(UChar) * (len+1));
    u_strcpy(rules, DEFAULTRULEARRAY);
    u_strcat(rules, newRules);
    

    myCollation=ucol_openRules(rules, u_strlen(rules), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if(FAILURE(status)){
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
