/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CDANTST.C
*
* Modification History:
*        Name                      Description            
*     Madhu Katragadda              Ported for C API
*********************************************************************************/
/**
 * CollationDanishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include <stdlib.h>
#include <string.h>

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "cdantst.h"
#include "callcoll.h"
#include "unicode/ustring.h"

static UCollator *myCollation;
const static UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {(UChar)0x004C /* 'L' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00FC, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00E4, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00F6, (UChar)0x0077 /* 'w' */, (UChar)0x0077 /* 'w' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00E4, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00FC, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */}
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {(UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00FC, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0079 /* 'y' */, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00F6, (UChar)0x0077 /* 'w' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006D /* 'm' */, (UChar)0x0061 /* 'a' */, (UChar)0x0073 /* 's' */, (UChar)0x0074 /* 't' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, (UChar)0x0077 /* 'w' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00F6, (UChar)0x0077 /* 'w' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, (UChar)0x0079 /* 'y' */, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */}
};

const static UCollationResult results[] = {
    UCOL_LESS,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_LESS,
    /* test primary > 5*/
    UCOL_EQUAL,
    UCOL_LESS,
    UCOL_EQUAL
};

const static UChar testBugs[][MAX_TOKEN_LEN] = {
    {(UChar)0x0041 /* 'A' */, (UChar)0x002F /* '/' */, (UChar)0x0053 /* 'S' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0041 /* 'A' */, (UChar)0x004E /* 'N' */, (UChar)0x0044 /* 'D' */, (UChar)0x0052 /* 'R' */, (UChar)0x0045 /* 'E' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0041 /* 'A' */, (UChar)0x004E /* 'N' */, (UChar)0x0044 /* 'D' */, (UChar)0x0052 /* 'R' */, 0x00C9, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0041 /* 'A' */, (UChar)0x004E /* 'N' */, (UChar)0x0044 /* 'D' */, (UChar)0x0052 /* 'R' */, (UChar)0x0045 /* 'E' */, (UChar)0x0041 /* 'A' */, (UChar)0x0053 /* 'S' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0041 /* 'A' */, (UChar)0x0053 /* 'S' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0043 /* 'C' */, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {0x00C7, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0043 /* 'C' */, (UChar)0x0042 /* 'B' */, (UChar)0x0000 /* '\0' */},
    {0x00C7, (UChar)0x0043 /* 'C' */,(UChar)0x0000 /* '\0' */},
    {(UChar)0x0044 /* 'D' */, (UChar)0x002E /* '.' */, (UChar)0x0053 /* 'S' */, (UChar)0x002E /* '.' */, (UChar)0x0042 /* 'B' */, (UChar)0x002E /* '.' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0044 /* 'D' */, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},                                                                           
    {(UChar)0x0044 /* 'D' */, (UChar)0x0042 /* 'B' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0044 /* 'D' */, (UChar)0x0053 /* 'S' */, (UChar)0x0042 /* 'B' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0044 /* 'D' */, (UChar)0x0053 /* 'S' */, (UChar)0x0043 /* 'C' */, (UChar)0x0000 /* '\0' */},
    {0x00D0, /*0x0110,*/ (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {0x00D0, /*0x0110,*/ (UChar)0x0043 /* 'C' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0045 /* 'E' */, (UChar)0x004B /* 'K' */, (UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x0052 /* 'R' */, (UChar)0x0041 /* 'A' */, (UChar)0x005F /* '_' */, (UChar)0x0041 /* 'A' */, (UChar)0x0052 /* 'R' */, (UChar)0x0042 /* 'B' */, (UChar)0x0045 /* 'E' */, (UChar)0x004A /* 'J' */, (UChar)0x0044 /* 'D' */, (UChar)0x0045 /* 'E' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0045 /* 'E' */, (UChar)0x004B /* 'K' */, (UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x0052 /* 'R' */, (UChar)0x0041 /* 'A' */, (UChar)0x0042 /* 'B' */, (UChar)0x0055 /* 'U' */, (UChar)0x0044 /* 'D' */, 0},
    {(UChar)0x0048 /* 'H' */, 0x00D8, (UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x0000 /* '\0' */},  
    {(UChar)0x0048 /* 'H' */, (UChar)0x0041 /* 'A' */, (UChar)0x0041 /* 'A' */, (UChar)0x0047 /* 'G' */, (UChar)0x0000 /* '\0' */},                                                                 
    {(UChar)0x0048 /* 'H' */, 0x00C5, (UChar)0x004E /* 'N' */, (UChar)0x0044 /* 'D' */, (UChar)0x0042 /* 'B' */, (UChar)0x004F /* 'O' */, (UChar)0x0047 /* 'G' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0048 /* 'H' */, (UChar)0x0041 /* 'A' */, (UChar)0x0041 /* 'A' */, (UChar)0x004E /* 'N' */, (UChar)0x0044 /* 'D' */, (UChar)0x0056 /* 'V' */, 0x00C6, (UChar)0x0052 /* 'R' */, (UChar)0x004B /* 'K' */, (UChar)0x0053 /* 'S' */, (UChar)0x0042 /* 'B' */, (UChar)0x0041 /* 'A' */, (UChar)0x004E /* 'N' */, (UChar)0x004B /* 'K' */, (UChar)0x0045 /* 'E' */, (UChar)0x004E /* 'N' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006B /* 'k' */, (UChar)0x0061 /* 'a' */, (UChar)0x0072 /* 'r' */, (UChar)0x006C /* 'l' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004B /* 'K' */, (UChar)0x0061 /* 'a' */, (UChar)0x0072 /* 'r' */, (UChar)0x006C /* 'l' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004E /* 'N' */, (UChar)0x0049 /* 'I' */, (UChar)0x0045 /* 'E' */, (UChar)0x004C /* 'L' */, (UChar)0x0053 /* 'S' */, (UChar)0x0020 /* ' ' */, (UChar)0x004A /* 'J' */, 0x00D8, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, (UChar)0x0045 /* 'E' */, (UChar)0x004E /* 'N' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004E /* 'N' */, (UChar)0x0049 /* 'I' */, (UChar)0x0045 /* 'E' */, (UChar)0x004C /* 'L' */, (UChar)0x0053 /* 'S' */, (UChar)0x002D /* '-' */, (UChar)0x004A /* 'J' */, 0x00D8, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, (UChar)0x0045 /* 'E' */, (UChar)0x004E /* 'N' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004E /* 'N' */, (UChar)0x0049 /* 'I' */, (UChar)0x0045 /* 'E' */, (UChar)0x004C /* 'L' */, (UChar)0x0053 /* 'S' */, (UChar)0x0045 /* 'E' */, (UChar)0x004E /* 'N' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0052 /* 'R' */, 0x00C9, (UChar)0x0045 /* 'E' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0052 /* 'R' */, (UChar)0x0045 /* 'E' */, (UChar)0x0045 /* 'E' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0042 /* 'B' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0052 /* 'R' */, 0x00C9, (UChar)0x0045 /* 'E' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x004C /* 'L' */, (UChar)0x0000 /* '\0' */},                                                    
    {(UChar)0x0052 /* 'R' */, (UChar)0x0045 /* 'E' */, (UChar)0x0045 /* 'E' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0056 /* 'V' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0043 /* 'C' */, (UChar)0x0048 /* 'H' */, (UChar)0x0059 /* 'Y' */, (UChar)0x0054 /* 'T' */, (UChar)0x0054 /* 'T' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0042 /* 'B' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0043 /* 'C' */, (UChar)0x0048 /* 'H' */, (UChar)0x0059 /* 'Y' */, (UChar)0x0054 /* 'T' */, (UChar)0x0054 /* 'T' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0048 /* 'H' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0043 /* 'C' */, (UChar)0x0048 /* 'H' */, 0x00DC, (UChar)0x0054 /* 'T' */, (UChar)0x0054 /* 'T' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0048 /* 'H' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0043 /* 'C' */, (UChar)0x0048 /* 'H' */, (UChar)0x0059 /* 'Y' */, (UChar)0x0054 /* 'T' */, (UChar)0x0054 /* 'T' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x004C /* 'L' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0043 /* 'C' */, (UChar)0x0048 /* 'H' */, 0x00DC, (UChar)0x0054 /* 'T' */, (UChar)0x0054 /* 'T' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x004D /* 'M' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0053 /* 'S' */, (UChar)0x0000 /* '\0' */},
    {0x00DF, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0053 /* 'S' */, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x0045 /* 'E' */, (UChar)0x0020 /* ' ' */, (UChar)0x0056 /* 'V' */, (UChar)0x0049 /* 'I' */, (UChar)0x004C /* 'L' */, (UChar)0x0044 /* 'D' */, (UChar)0x004D /* 'M' */, (UChar)0x004F /* 'O' */, (UChar)0x0053 /* 'S' */, (UChar)0x0045 /* 'E' */, (UChar)0x0000 /* '\0' */},               
    {(UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x0045 /* 'E' */, (UChar)0x004B /* 'K' */, 0x00C6, (UChar)0x0052 /* 'R' */, 0},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x004D /* 'M' */, (UChar)0x0020 /* ' ' */, (UChar)0x0050 /* 'P' */, (UChar)0x0045 /* 'E' */, (UChar)0x0054 /* 'T' */, (UChar)0x0045 /* 'E' */, (UChar)0x0052 /* 'R' */, (UChar)0x0053 /* 'S' */, (UChar)0x0045 /* 'E' */, (UChar)0x004E /* 'N' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x004D /* 'M' */, (UChar)0x004C /* 'L' */, (UChar)0x0059 /* 'Y' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0054 /* 'T' */, (UChar)0x0048 /* 'H' */, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x0056 /* 'V' */, (UChar)0x0041 /* 'A' */, (UChar)0x004C /* 'L' */, (UChar)0x0044 /* 'D' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0054 /* 'T' */, (UChar)0x0048 /* 'H' */, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x0056 /* 'V' */, (UChar)0x0041 /* 'A' */, (UChar)0x0052 /* 'R' */, (UChar)0x0044 /* 'D' */, (UChar)0x0055 /* 'U' */, (UChar)0x0052 /* 'R' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0054 /* 'T' */, (UChar)0x0048 /* 'H' */, (UChar)0x0059 /* 'Y' */, (UChar)0x0047 /* 'G' */, (UChar)0x0045 /* 'E' */, (UChar)0x0053 /* 'S' */, (UChar)0x0045 /* 'E' */, (UChar)0x004E /* 'N' */, (UChar)0x0000 /* '\0' */},
    {0x00FE, (UChar)0x004F /* 'O' */, (UChar)0x0052 /* 'R' */, (UChar)0x0056 /* 'V' */, (UChar)0x0041 /* 'A' */, (UChar)0x0052 /* 'R' */, 0x00D0, /*0x0110,*/ (UChar)0x0055 /* 'U' */, (UChar)0x0052 /* 'R' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0056 /* 'V' */, (UChar)0x0045 /* 'E' */, (UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x0045 /* 'E' */, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, 0x00C5, (UChar)0x0052 /* 'R' */, (UChar)0x0044 /* 'D' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0056 /* 'V' */, (UChar)0x0045 /* 'E' */, (UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x0045 /* 'E' */, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, (UChar)0x0041 /* 'A' */, (UChar)0x0041 /* 'A' */, (UChar)0x0052 /* 'R' */, (UChar)0x0044 /* 'D' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0041 /* 'A' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0056 /* 'V' */, (UChar)0x0045 /* 'E' */, (UChar)0x0053 /* 'S' */, (UChar)0x0054 /* 'T' */, (UChar)0x0045 /* 'E' */, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, 0x00C5, (UChar)0x0052 /* 'R' */, (UChar)0x0044 /* 'D' */, (UChar)0x002C /* ',' */, (UChar)0x0020 /* ' ' */, (UChar)0x0042 /* 'B' */, (UChar)0x0000 /* '\0' */},                 
    {0x00C6, (UChar)0x0042 /* 'B' */, (UChar)0x004C /* 'L' */, (UChar)0x0045 /* 'E' */, (UChar)0x0000 /* '\0' */},
    {0x00C4, (UChar)0x0042 /* 'B' */, (UChar)0x004C /* 'L' */, (UChar)0x0045 /* 'E' */, (UChar)0x0000 /* '\0' */},
    {0x00D8, (UChar)0x0042 /* 'B' */, (UChar)0x0045 /* 'E' */, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, (UChar)0x0000 /* '\0' */},
    {0x00D6, (UChar)0x0042 /* 'B' */, (UChar)0x0045 /* 'E' */, (UChar)0x0052 /* 'R' */, (UChar)0x0047 /* 'G' */, (UChar)0x0000 /* '\0' */}
};

const static UChar testNTList[][MAX_TOKEN_LEN] = {
    {(UChar)0x0061 /* 'a' */, (UChar)0x006E /* 'n' */, (UChar)0x0064 /* 'd' */, (UChar)0x0065 /* 'e' */, (UChar)0x0072 /* 'r' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0061 /* 'a' */, (UChar)0x0071 /* 'q' */, (UChar)0x0075 /* 'u' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0065 /* 'e' */, (UChar)0x006D /* 'm' */, (UChar)0x0069 /* 'i' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0063 /* 'c' */, (UChar)0x006F /* 'o' */, (UChar)0x0074 /* 't' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0063 /* 'c' */, (UChar)0x006F /* 'o' */, (UChar)0x0074 /* 't' */, 0x00e9, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0063 /* 'c' */, 0x00f4, (UChar)0x0074 /* 't' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0063 /* 'c' */, 0x00f4, (UChar)0x0074 /* 't' */, 0x00e9, (UChar)0x0000 /* '\0' */},
    {0x010d, (UChar)0x0075 /* 'u' */, 0x010d, 0x0113, (UChar)0x0074 /* 't' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0043 /* 'C' */, (UChar)0x007A /* 'z' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0068 /* 'h' */, (UChar)0x0069 /* 'i' */, 0x0161, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0069 /* 'i' */, (UChar)0x0072 /* 'r' */, (UChar)0x0064 /* 'd' */, (UChar)0x0069 /* 'i' */, (UChar)0x0073 /* 's' */, (UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0069 /* 'i' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0069 /* 'i' */, (UChar)0x0072 /* 'r' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x006C /* 'l' */, (UChar)0x0061 /* 'a' */, (UChar)0x006D /* 'm' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, 0x00f5, (UChar)0x0075 /* 'u' */, (UChar)0x0067 /* 'g' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, 0x00f2, (UChar)0x007A /* 'z' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, 0x010d, (UChar)0x0000 /* '\0' */},                                
    {(UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00fc, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0079 /* 'y' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},                               
    {(UChar)0x006C /* 'l' */, 0x00e4, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00f6, (UChar)0x0077 /* 'w' */, (UChar)0x0065 /* 'e' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006D /* 'm' */, 0x00e0, 0x0161, (UChar)0x0074 /* 't' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006D /* 'm' */, 0x00ee, (UChar)0x0072 /* 'r' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006D /* 'm' */, (UChar)0x0079 /* 'y' */, (UChar)0x006E /* 'n' */, (UChar)0x0064 /* 'd' */, (UChar)0x0069 /* 'i' */, (UChar)0x0067 /* 'g' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004D /* 'M' */, 0x00e4, (UChar)0x006E /* 'n' */, (UChar)0x006E /* 'n' */, (UChar)0x0065 /* 'e' */, (UChar)0x0072 /* 'r' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006D /* 'm' */, 0x00f6, (UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0074 /* 't' */, (UChar)0x0065 /* 'e' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0070 /* 'p' */, (UChar)0x0069 /* 'i' */, 0x00f1, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0070 /* 'p' */, (UChar)0x0069 /* 'i' */, (UChar)0x006E /* 'n' */, (UChar)0x0074 /* 't' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0070 /* 'p' */, (UChar)0x0079 /* 'y' */, (UChar)0x006C /* 'l' */, (UChar)0x006F /* 'o' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {0x0161, 0x00e0, (UChar)0x0072 /* 'r' */, (UChar)0x0061 /* 'a' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0073 /* 's' */, (UChar)0x0061 /* 'a' */, (UChar)0x0076 /* 'v' */, (UChar)0x006F /* 'o' */, (UChar)0x0069 /* 'i' */, (UChar)0x0072 /* 'r' */, (UChar)0x0000 /* '\0' */},
    {0x0160, (UChar)0x0065 /* 'e' */, (UChar)0x0072 /* 'r' */, (UChar)0x0062 /* 'b' */, 0x016b, (UChar)0x0072 /* 'r' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0053 /* 'S' */, (UChar)0x0069 /* 'i' */, (UChar)0x0065 /* 'e' */, (UChar)0x0074 /* 't' */, (UChar)0x006C /* 'l' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {0x015b, (UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, (UChar)0x0062 /* 'b' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0073 /* 's' */, (UChar)0x0075 /* 'u' */, (UChar)0x0062 /* 'b' */, (UChar)0x0074 /* 't' */, (UChar)0x006C /* 'l' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0073 /* 's' */, (UChar)0x0079 /* 'y' */, (UChar)0x006D /* 'm' */, (UChar)0x0062 /* 'b' */, (UChar)0x006F /* 'o' */, (UChar)0x006C /* 'l' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0073 /* 's' */, 0x00e4, (UChar)0x006D /* 'm' */, (UChar)0x0074 /* 't' */, (UChar)0x006C /* 'l' */, (UChar)0x0069 /* 'i' */, (UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0077 /* 'w' */, (UChar)0x0061 /* 'a' */, (UChar)0x0066 /* 'f' */, (UChar)0x0066 /* 'f' */, (UChar)0x006C /* 'l' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0076 /* 'v' */, (UChar)0x0065 /* 'e' */, (UChar)0x0072 /* 'r' */, (UChar)0x006B /* 'k' */, (UChar)0x0065 /* 'e' */, (UChar)0x0068 /* 'h' */, (UChar)0x0072 /* 'r' */, (UChar)0x0074 /* 't' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0077 /* 'w' */, (UChar)0x006F /* 'o' */, (UChar)0x006F /* 'o' */, (UChar)0x0064 /* 'd' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0076 /* 'v' */, (UChar)0x006F /* 'o' */, (UChar)0x0078 /* 'x' */, (UChar)0x0000 /* '\0' */},                                 
    {(UChar)0x0076 /* 'v' */, 0x00e4, (UChar)0x0067 /* 'g' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0079 /* 'y' */, (UChar)0x0065 /* 'e' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0079 /* 'y' */, (UChar)0x0075 /* 'u' */, (UChar)0x0061 /* 'a' */, (UChar)0x006E /* 'n' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x0079 /* 'y' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x0063 /* 'c' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {0x017e, (UChar)0x0061 /* 'a' */, (UChar)0x006C /* 'l' */, (UChar)0x0000 /* '\0' */},
    {0x017e, (UChar)0x0065 /* 'e' */, (UChar)0x006E /* 'n' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {0x017d, (UChar)0x0065 /* 'e' */, (UChar)0x006E /* 'n' */, 0x0113, (UChar)0x0076 /* 'v' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x007A /* 'z' */, (UChar)0x006F /* 'o' */, (UChar)0x006F /* 'o' */, 0},
    {(UChar)0x005A /* 'Z' */, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0065 /* 'e' */, (UChar)0x0064 /* 'd' */, (UChar)0x0072 /* 'r' */, (UChar)0x0069 /* 'i' */, (UChar)0x006A /* 'j' */, (UChar)0x0061 /* 'a' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x005A /* 'Z' */, 0x00fc, (UChar)0x0072 /* 'r' */, (UChar)0x0069 /* 'i' */, (UChar)0x0063 /* 'c' */, (UChar)0x0068 /* 'h' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x007A /* 'z' */, (UChar)0x0079 /* 'y' */, (UChar)0x0073 /* 's' */, (UChar)0x006B /* 'k' */, 0},             
    {0x00e4, (UChar)0x006E /* 'n' */, (UChar)0x0064 /* 'd' */, (UChar)0x0065 /* 'e' */, (UChar)0x0072 /* 'r' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */}                  
};


void addDanishCollTest(TestNode** root)
{
    
    
    /*addTest(root, &TestTertiary, "tscoll/cdantst/TestTertiary");*/
    /*addTest(root, &TestPrimary, "tscoll/cdantst/TestPrimary");*/

}

    

static void TestTertiary( )
{
    
    int32_t i,j;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("da_DK", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
        return;
    }
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 5 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    j = 0;
    log_verbose("Test internet data list : ");
    for (i = 0; i < 53; i++) {
        for (j = i+1; j < 54; j++) {
            doTest(myCollation, testBugs[i], testBugs[j], UCOL_LESS);
        }
    }
    log_verbose("Test NT data list : ");
    for (i = 0; i < 52; i++) {
        for (j = i+1; j < 53; j++) {
            doTest(myCollation, testNTList[i], testNTList[j], UCOL_LESS);
        }
    }
    ucol_close(myCollation);
}

static void TestPrimary()
{
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("da_DK", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: %s: in creation of rule based collator: %s\n", __FILE__, myErrorName(status));
        return;
    }
    ucol_setStrength(myCollation, UCOL_PRIMARY);
    for (i = 5; i < 8 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);  
}

#endif /* #if !UCONFIG_NO_COLLATION */
