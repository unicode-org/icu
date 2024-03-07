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
*   03/18/2003  weiv        Creation.
*******************************************************************************
*/

//
//   class Line
//
//      Each line from the source file (containing a name, presumably) gets
//      one of these structs.
//

#ifndef COLPROBE_LINE_H
#define COLPROBE_LINE_H
#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/ustring.h"
#include "unicode/unistr.h"
#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "colprobe.h"

#include <stdlib.h>
#include <string.h>

static const int MAX_EXPANSION_PREFIXES = 10;

class  Line {
public:
  static void copyArray(Line *dest, const Line *src, int32_t size);
  Line();
  Line(const Line &other);
  Line(const char16_t* name, int32_t len);
  Line(const UnicodeString &string);
  Line(const char16_t name);
  Line(const char *buff, int32_t buffLen, UErrorCode &status);
  ~Line();
  Line & operator=(const Line &other);
  UBool operator==(const Line &other) const;
  UBool operator!=(const Line &other) const;
  void setToConcat(const Line *first, const Line *second);
  void setName(const char16_t* name, int32_t len);
  UnicodeString toString(UBool pretty = false);
  UnicodeString toBundleString();
  UnicodeString toHTMLString();
  int32_t write(char *buff, int32_t buffLen, UErrorCode &status);
  void initFromString(const char *buff, int32_t buffLen, UErrorCode &status);
  

  UnicodeString strengthIndent(UColAttributeValue strength, int indentSize, UnicodeString &result);
  UnicodeString strengthToString(UColAttributeValue strength, UBool pretty, UBool html = false);
  UnicodeString stringToName(char16_t *string, int32_t len);
  void setTo(const UnicodeString &string);
  void setTo(const UChar32 n);
  UBool equals(const Line &other) const;
  Line *nextInteresting();
  void append(const char16_t n);
  void append(const char16_t* n, int32_t length);
  void append(const Line &l);
  void clear();
  void swapCase();
  void swapCase(char16_t *string, int32_t &sLen);
  UnicodeString dumpSortkey();
  void init();

  
public:
    char16_t  name[25];
    int32_t   len;
    char16_t  expansionString[25];
    int32_t   expLen;

    UColAttributeValue strength;
    UColAttributeValue strengthFromEmpty;
    UColAttributeValue cumulativeStrength;
    UColAttributeValue expStrength;

    Line *previous;
    Line *next;

    // In case this element is a contraction
    // we keep a pointer at which lines were components
    Line *left;
    Line *right;

    UBool   isContraction;
    UBool   isExpansion;
    UBool   isRemoved;
    UBool   isReset;

    int32_t expIndex;
    uint8_t firstCC;
    uint8_t lastCC;

    uint8_t   *sortKey;
public:
  static UnicodeSet *needsQuoting;
};


#endif //COLPROBE_LINE_H
