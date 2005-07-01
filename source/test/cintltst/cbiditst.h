/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*   file name:  cbiditst.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep22
*   created by: Markus W. Scherer
*/

#ifndef CBIDITST_H
#define CBIDITST_H

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ubidi.h"

#ifdef XP_CPLUSPLUS
extern "C" {
#endif

#define MAX_STRING_LENGTH 200

/* length of an array */
#define ARRAY_LENGTH(array) (sizeof(array)/sizeof(array[0]))

/*  Comparing the description of the BiDi algorithm with this implementation
    is easier with the same names for the BiDi types in the code as there.
    See UCharDirection in uchar.h .
*/
enum { 
    L=  U_LEFT_TO_RIGHT,
    R=  U_RIGHT_TO_LEFT,
    EN= U_EUROPEAN_NUMBER,
    ES= U_EUROPEAN_NUMBER_SEPARATOR,
    ET= U_EUROPEAN_NUMBER_TERMINATOR,
    AN= U_ARABIC_NUMBER,
    CS= U_COMMON_NUMBER_SEPARATOR,
    B=  U_BLOCK_SEPARATOR,
    S=  U_SEGMENT_SEPARATOR,
    WS= U_WHITE_SPACE_NEUTRAL,
    ON= U_OTHER_NEUTRAL,
    LRE=U_LEFT_TO_RIGHT_EMBEDDING,
    LRO=U_LEFT_TO_RIGHT_OVERRIDE,
    AL= U_RIGHT_TO_LEFT_ARABIC,
    RLE=U_RIGHT_TO_LEFT_EMBEDDING,
    RLO=U_RIGHT_TO_LEFT_OVERRIDE,
    PDF=U_POP_DIRECTIONAL_FORMAT,
    NSM=U_DIR_NON_SPACING_MARK,
    BN= U_BOUNDARY_NEUTRAL,
    dirPropCount
};

extern const char *
dirPropNames[dirPropCount];

extern UChar
charFromDirProp[dirPropCount];

typedef struct {
    const uint8_t *text;
    int32_t length;
    UBiDiLevel paraLevel;
    int32_t lineStart, lineLimit;
    UBiDiDirection direction;
    UBiDiLevel resultLevel;
    const UBiDiLevel *levels;
    const uint8_t *visualMap;
} BiDiTestData;

extern BiDiTestData
tests[];

extern int
bidiTestCount;

#ifdef XP_CPLUSPLUS
}
#endif

#endif
