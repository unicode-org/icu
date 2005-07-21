/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/unistr.h"
#include "unicode/sortkey.h"
#include "dacoll.h"

#include "sfwdchit.h"

CollationDanishTest::CollationDanishTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale("da", "DK", ""),status);
    if(!myCollation || U_FAILURE(status)) {
      errln(__FILE__ "failed to create! err " + UnicodeString(u_errorName(status)));
    /* if it wasn't already: */
    delete myCollation;
    myCollation = NULL;
    }

}

CollationDanishTest::~CollationDanishTest()
{
    delete myCollation;
}

const UChar CollationDanishTest::testSourceCases[][CollationDanishTest::MAX_TOKEN_LEN] 
= {
    {(UChar)0x004C /* 'L' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00FC, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00E4, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00F6, (UChar)0x0077 /* 'w' */, (UChar)0x0077 /* 'w' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00E4, (UChar)0x0076 /* 'v' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00FC, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */}
};

const UChar CollationDanishTest::testTargetCases[][CollationDanishTest::MAX_TOKEN_LEN] 
= {
    {(UChar)0x006C /* 'l' */, (UChar)0x0075 /* 'u' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00FC, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006C /* 'l' */, (UChar)0x0079 /* 'y' */, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00F6, (UChar)0x0077 /* 'w' */, (UChar)0x0065 /* 'e' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x006D /* 'm' */, (UChar)0x0061 /* 'a' */, (UChar)0x0073 /* 's' */, (UChar)0x0074 /* 't' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, (UChar)0x0077 /* 'w' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, 0x00F6, (UChar)0x0077 /* 'w' */, (UChar)0x0069 /* 'i' */, (UChar)0x0000 /* '\0' */},
    {(UChar)0x004C /* 'L' */, (UChar)0x0079 /* 'y' */, (UChar)0x0062 /* 'b' */, (UChar)0x0065 /* 'e' */, (UChar)0x0063 /* 'c' */, (UChar)0x006B /* 'k' */, (UChar)0x0000 /* '\0' */}
};

const Collator::EComparisonResult CollationDanishTest::results[] = {
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER,
    Collator::LESS,
    Collator::LESS,
    /* test primary > 5*/
    Collator::EQUAL,
    Collator::LESS,
    Collator::EQUAL
};

const UChar CollationDanishTest::testBugs[][CollationDanishTest::MAX_TOKEN_LEN] = {
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

const UChar CollationDanishTest::testNTList[][CollationDanishTest::MAX_TOKEN_LEN] = {
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

void CollationDanishTest::TestTertiary(/* char* par */)
{
    int32_t i = 0;
    myCollation->setStrength(Collator::TERTIARY);
    UErrorCode status = U_ZERO_ERROR;
    /* problem in strcollinc for unfinshed contractions */
    if(U_FAILURE(status)){
      errln("ERROR: in setting normalization mode of the Danish collator\n");
      return;
    }
    for (i = 0; i < 5 ; i++) {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    int32_t j = 0;
    logln("Test internet data list : ");
    for (i = 0; i < 53; i++) {
        for (j = i+1; j < 54; j++) {
            doTest(myCollation, testBugs[i], testBugs[j], Collator::LESS);
        }
    }
    logln("Test NT data list : ");
    for (i = 0; i < 52; i++) {
        for (j = i+1; j < 53; j++) {
            doTest(myCollation, testNTList[i], testNTList[j], Collator::LESS);
        }
    }
}

void CollationDanishTest::TestPrimary(/* char* par */)
{
    int32_t i;
    myCollation->setStrength(Collator::PRIMARY);
    for (i = 5; i < 8; i++) {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
}

void CollationDanishTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite CollationDanishTest: ");

    if((!myCollation) && exec) {
      errln(__FILE__ " cannot test - failed to create collator.");
      name = "";
      return;
    }
    switch (index) {
        case 0: name = "TestPrimary";   if (exec)   TestPrimary(/* par */); break;
        case 1: name = "TestTertiary";  if (exec)   TestTertiary(/* par */); break;
        default: name = ""; break;
    }
}

#endif /* #if !UCONFIG_NO_COLLATION */
