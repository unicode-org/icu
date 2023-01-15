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

#include "strengthprobe.h"

StrengthProbe::StrengthProbe(CompareFn comparer, GetSortKeyFn getter, UChar SE, 
                             UChar B0, UChar B1, UChar B2, UChar B3) :
SE(SE),
B0(B0), B1(B1), B2(B2), B3(B3),
utilFirstP(&utilFirst), utilSecondP(&utilSecond),
frenchSecondary(false),
comparer(comparer), skgetter(getter)
{
}

int
StrengthProbe::setProbeChars(UChar B0, UChar B1, UChar B2, UChar B3)
{
  this->B0 = B0;
  this->B1 = B1;
  this->B2 = B2;
  this->
B3 = B3;
  return checkSanity();
}

int
StrengthProbe::checkSanity() 
{
  int sanityRes;
  utilFirst.setTo(B0);
  utilSecond.setTo(B3);
  if((sanityRes = comparer(&utilFirstP, &utilSecondP)) >= 0) {
    return sanityRes*10 + 3;
  }
  utilSecond.setTo(B2);
  if((sanityRes = comparer(&utilFirstP, &utilSecondP)) >= 0) {
    return sanityRes*10 + 2;
  }
  utilSecond.setTo(B1);
  if((sanityRes = comparer(&utilFirstP, &utilSecondP)) >= 0) {
    return sanityRes*10 + 1;
  }
  utilFirst.setTo(B3);
  utilSecond.setTo(B2);
  if((sanityRes = comparer(&utilFirstP, &utilSecondP)) >= 0) {
    return sanityRes*10 + 5;
  }
  utilSecond.setTo(B1);
  if((sanityRes = comparer(&utilFirstP, &utilSecondP)) >= 0) {
    return sanityRes*10 + 4;
  }
  utilFirst.setTo(B2);
  if((sanityRes = comparer(&utilFirstP, &utilSecondP)) >= 0) {
    return sanityRes*10 + 6;
  }
  utilFirst.setTo(B0);
  if(distanceFromEmptyString(utilFirst) > UCOL_PRIMARY) {
    return 1000;
  }
  utilFirst.setTo(B1);
  if(distanceFromEmptyString(utilFirst) > UCOL_PRIMARY) {
    return 1001;
  }
  utilFirst.setTo(B2);
  if(distanceFromEmptyString(utilFirst) > UCOL_PRIMARY) {
    return 1002;
  }
  utilFirst.setTo(B3);
  if(distanceFromEmptyString(utilFirst) > UCOL_PRIMARY) {
    return 1003;
  }
  return 0;
}

UBool 
StrengthProbe::probePrefix(const Line &x, const Line &y, UChar first, UChar second) {
  utilFirst.name[0] = first;
  utilFirst.name[1] = SE;
  u_strcpy(utilFirst.name+2, x.name);
  utilFirst.name[x.len+2] = 0;
  utilFirst.len = x.len+2;

  utilSecond.name[0] = second;
  utilSecond.name[1] = SE;
  u_strcpy(utilSecond.name+2, y.name);
  utilSecond.name[y.len+2] = 0;
  utilSecond.len = y.len+2;

  if(comparer(&utilFirstP, &utilSecondP) < 0) {
    return true;
  } else {
    return false;
  }
}

UBool 
StrengthProbe::probeSuffix(const Line &x, const Line &y, UChar first, UChar second) {
  u_strcpy(utilFirst.name, x.name);
  utilFirst.name[x.len] = SE;
  utilFirst.name[x.len+1] = first;
  utilFirst.name[x.len+2] = 0;
  utilFirst.len = x.len + 2;
  u_strcpy(utilSecond.name, y.name);
  utilSecond.name[y.len] = SE;
  utilSecond.name[y.len+1] = second;
  utilSecond.name[y.len+2] = 0;
  utilSecond.len = y.len + 2;

  if(comparer(&utilFirstP, &utilSecondP) < 0) {
    return true;
  } else {
    return false;
  }
}

UBool 
StrengthProbe::probePrefixNoSep(const Line &x, const Line &y, UChar first, UChar second) {
  utilFirst.name[0] = first;
  u_strcpy(utilFirst.name+1, x.name);
  utilFirst.name[x.len+1] = 0;
  utilFirst.len = x.len + 1;

  utilSecond.name[0] = second;
  u_strcpy(utilSecond.name+1, y.name);
  utilSecond.name[y.len+1] = 0;
  utilSecond.len = y.len + 1;

  if(comparer(&utilFirstP, &utilSecondP) < 0) {
    return true;
  } else {
    return false;
  }
}

UBool 
StrengthProbe::probeSuffixNoSep(const Line &x, const Line &y, UChar first, UChar second) {
  u_strcpy(utilFirst.name, x.name);
  utilFirst.name[x.len] = first;
  utilFirst.name[x.len+1] = 0;
  utilFirst.len = x.len + 1;
  u_strcpy(utilSecond.name, y.name);
  utilSecond.name[y.len] = second;
  utilSecond.name[y.len+1] = 0;
  utilSecond.len = y.len + 1;

  if(comparer(&utilFirstP, &utilSecondP) < 0) {
    return true;
  } else {
    return false;
  }
}

UColAttributeValue 
StrengthProbe::getStrength(const Line &x, const Line &y) {
  const Line *xp = &x;
  const Line *yp = &y;

  Line empty;
  Line *emptyP = &empty;
  if(comparer(&emptyP, &xp) == 0) {
    return distanceFromEmptyString(y);
  }

  int32_t result = comparer(&xp, &yp);

  if(result == 0) {
    return UCOL_IDENTICAL;
  } else if(result > 0) {
    return UCOL_OFF; // bad situation
  } else { // we need to probe strength
    if(probeSuffix(x, y, B1, B0)) {
    //if(probePrefix(x, y, B2, B0)) { // swamps secondary difference
      return UCOL_PRIMARY;
    } else if(probePrefix(x, y, B3, B0)) { // swamps tertiary difference
      return UCOL_SECONDARY;
    } else if(probeSuffix(x, y, B3, B0)) { // swamped by tertiary difference
      return UCOL_TERTIARY;
    } else if(!probePrefix(x, y, B3, B0)) {
      return UCOL_QUATERNARY;
    }
    /*
    //if(probeSuffix(x, y, B1, B0)) {
    if(probePrefix(x, y, B2, B0)) { // swamps secondary difference
      return UCOL_PRIMARY;
    } else if(probePrefix(x, y, B3, B0)) { // swamps tertiary difference
      return UCOL_SECONDARY;
    } else if(probeSuffix(x, y, B3, B0)) { // swamped by tertiary difference
      return UCOL_TERTIARY;
    } else if(!probePrefix(x, y, B3, B0)) {
      return UCOL_QUATERNARY;
    }
    */
  }
  return UCOL_OFF; // bad
}

UColAttributeValue 
StrengthProbe::getStrength(const UnicodeString &sx, const UnicodeString &sy) {
  Line x(sx);
  Line y(sy);
  return getStrength(x, y);
}

int32_t 
StrengthProbe::compare(const UnicodeString &sx, const UnicodeString &sy) {
  Line x(sx);
  Line y(sy);
  const Line *xp = &x;
  const Line *yp = &y;
  return comparer(&xp, &yp);
}

int32_t 
StrengthProbe::compare(const Line &x, const Line &y) {
  const Line *xp = &x;
  const Line *yp = &y;
  return comparer(&xp, &yp);
}

UColAttributeValue 
StrengthProbe::distanceFromEmptyString(const Line &x) {
  if(x.name[0] == 0x30D) {
    int32_t putBreakPointHere = 0;
  }
  Line empty;
  Line *emptyP = &empty;
  uint8_t buff[256];
  getSortKey(empty.name, empty.len, buff, 256);
  Line B0Line(B0);
  Line *B0LineP = &B0Line;
  const Line *xp = &x;
  int32_t result = comparer(&emptyP, &xp);
  if(result == 0) {
    return UCOL_IDENTICAL;
  } else if(result > 0) {
    return UCOL_OFF;
  }
  result = comparer(&B0LineP, &xp);
  if(result <= 0) {
    return UCOL_PRIMARY;
  }
  Line sexb0(SE);
  sexb0.append(x.name, x.len);
  sexb0.append(B0);

  Line seb0(SE);
  seb0.append(B0);
  uint8_t seb0K[256];
  uint8_t sexb0K[256];
  uint8_t seb2K[256];
  uint8_t seb3K[256];
  memset(seb0K, 0, 256);
  memset(sexb0K, 0, 256);
  memset(seb2K, 0, 256);
  memset(seb3K, 0, 256);

  getSortKey(seb0, seb0K, 256);
  getSortKey(sexb0, sexb0K, 256);

  if(compare(seb0, sexb0) <= 0) {
    Line seb2(SE);
    seb2.append(B2);
    getSortKey(seb2, seb2K, 256);
    result = compare(seb2, sexb0);
    if((result <= 0 && !frenchSecondary) || (result >= 0 && frenchSecondary)) { // swamps tertiary difference
      return UCOL_SECONDARY;
    }
    Line seb3(SE);
    seb3.append(B3);
    getSortKey(seb3, seb3K, 256);
    if(compare(seb3, sexb0) < 0) {
      return UCOL_TERTIARY;
    }
    return UCOL_QUATERNARY;
  } else {
    // if this was UCA, we would have a primary difference.
    // however, this might not be so, since not everybody 
    // makes well formed CEs.
    // in cs_CZ on linux, space is tertiary ignorable, but
    // its quaternary level strength is lower than quad 
    // strengths for non-ignorables. oh well, more testing
    // required
    // I think that we can only have quaternary difference
    // here (in addition to primary difference).
    //if(!probePrefix(x, empty, B3, B0)) {
      //return UCOL_QUATERNARY;
    //} else {
      return UCOL_PRIMARY;
    //}
  }
}

UColAttributeValue 
StrengthProbe::distanceFromEmptyString(const UnicodeString &x) {
  const Line xp(x);
  return distanceFromEmptyString(xp);
}


UColAttributeValue 
StrengthProbe::getPrefixedStrength(const Line &prefix, const Line &x, const Line &y) {
  contractionUtilFirst.setToConcat(&prefix, &x);
  contractionUtilSecond.setToConcat(&prefix, &y);
  return getStrength(contractionUtilFirst, contractionUtilSecond);
}


StrengthProbe::StrengthProbe(const StrengthProbe &that) {
  *this = that;
}

StrengthProbe &
StrengthProbe::operator=(const StrengthProbe &that) {
  if(this != &that) {
    B0 = that.B0;
    B1 = that.B1;
    B2 = that.B2;
    B3 = that.B3;
    SE = that.SE;
    frenchSecondary = that.frenchSecondary;
    comparer = that.comparer;
    skgetter = that.skgetter;

    utilFirstP = &utilFirst;
    utilSecondP = &utilSecond;
  }

  return *this;
}

UBool
StrengthProbe::isFrenchSecondary(UErrorCode &status) {
  utilFirst.setTo(B0);
  utilFirst.append(SE);
  utilFirst.append(B2);
  utilSecond.setTo(B2);
  utilSecond.append(SE);
  utilSecond.append(B0);

  int32_t result = compare(utilFirst, utilSecond);

  if(result < 0) {
    return false;
  } else if(result > 0) {
    frenchSecondary = true;
    return true;
  } else {
    status = U_INTERNAL_PROGRAM_ERROR;
    return false;
  }
}

UBool
StrengthProbe::isUpperFirst(UErrorCode &status) {
  UChar i = 0;
  int32_t result = 0;
  int32_t upper = 0, lower = 0, equal = 0;
  for(i = 0x41; i < 0x5B; i++) {
    utilFirst.setTo(i);
    utilSecond.setTo(i+0x20);
    result = compare(utilFirst, utilSecond);
    if(result < 0) {
      upper++;
    } else if(result > 0) {
      lower++;
    } else {
      equal++;
    }
  }
  
  if(lower == 0 && equal == 0) {
    return true;
  }
  if(upper == 0 && equal == 0) {
    return false;
  }
  status = U_INTERNAL_PROGRAM_ERROR;
  return false;
}

