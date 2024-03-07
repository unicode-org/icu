// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File line.h
*
* Modification History:
*
*   Date        Name        Description
*   07/07/2003  weiv        Creation.
*******************************************************************************
*/

//
//   class Line
//
//      Each line from the source file (containing a name, presumably) gets
//      one of these structs.
//

#ifndef COLPROBE_STRENGTHPROBE_H
#define COLPROBE_STRENGTHPROBE_H

#include "colprobe.h"
#include "line.h"
#include "unicode/uniset.h"
#include "unicode/usetiter.h"

class StrengthProbe {
public:
  char16_t SE;
  char16_t B0;
  char16_t B1;
  char16_t B2;
  char16_t B3;
private:
  Line utilFirst;
  Line utilSecond;
  Line *utilFirstP;
  Line *utilSecondP;
  Line contractionUtilFirst;
  Line contractionUtilSecond;
  UBool probePrefix(const Line &x, const Line &y, char16_t first, char16_t second);
  UBool probeSuffix(const Line &x, const Line &y, char16_t first, char16_t second);
  UBool probePrefixNoSep(const Line &x, const Line &y, char16_t first, char16_t second);
  UBool probeSuffixNoSep(const Line &x, const Line &y, char16_t first, char16_t second);

  UBool frenchSecondary;

public:
  CompareFn comparer;
  GetSortKeyFn skgetter;
  
  StrengthProbe() {};
  StrengthProbe(CompareFn comparer, GetSortKeyFn getter, char16_t SE = 0x0030, char16_t B0 = 0x0061, char16_t B1 = 0x0062, char16_t B2 = 0x00E1, char16_t B3 = 0x0041); //, char16_t LB = 0x0039, char16_t UB = 0xfa29);
  int setProbeChars(char16_t B0, char16_t B1, char16_t B2, char16_t B3);
  int checkSanity();
  StrengthProbe(const StrengthProbe &that);
  StrengthProbe &operator=(const StrengthProbe &that);
  UColAttributeValue getStrength(const Line &x, const Line &y);
  UColAttributeValue getStrength(const UnicodeString &x, const UnicodeString &y);
  UColAttributeValue getPrefixedStrength(const Line &prefix, const Line &x, const Line &y);
  int32_t compare(const UnicodeString &x, const UnicodeString &y);
  int32_t compare(const Line &x, const Line &y);
  UColAttributeValue distanceFromEmptyString(const Line &x);
  UColAttributeValue distanceFromEmptyString(const UnicodeString &x);
  UBool isFrenchSecondary(UErrorCode &status);
  UBool isUpperFirst(UErrorCode &status);
  int getSortKey(const Line &l, uint8_t *buffer, int32_t buffCap) {
    return skgetter(l.name, l.len, buffer, buffCap);
  };

  int getSortKey(char16_t *string, int32_t sLen, uint8_t *buffer, int32_t buffCap) {
    return skgetter(string, sLen, buffer, buffCap);
  };
 
};


#endif //#ifndef COLPROBE_STRENGTHPROBE_H

