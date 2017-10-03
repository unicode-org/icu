// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File line.cpp
*
* Modification History:
*
*   Date        Name        Description
*   03/18/2003  weiv        Creation.
*******************************************************************************
*/

#include "line.h"
#include <stdio.h>

UnicodeSet * Line::needsQuoting = NULL;

void
Line::init()
{
        len = 0;
        expLen = 0;
        strength = UCOL_OFF;
        strengthFromEmpty = UCOL_OFF;
        cumulativeStrength = UCOL_OFF;
        expStrength = UCOL_OFF;
        previous = NULL;
        next = NULL;
        left = NULL;
        right = NULL;
        isContraction = FALSE;
        isExpansion = FALSE;
        isRemoved = FALSE;
        isReset = FALSE;
        expIndex = 0;
        firstCC = 0;
        lastCC = 0;
        sortKey = NULL;
}

Line::Line()
{
  init();
  memset(name, 0, 25*sizeof(UChar));
  memset(expansionString, 0, 25*sizeof(UChar));
}

Line::Line(const UChar* name, int32_t len)
{
  init();
  this->len = len;
  u_memcpy(this->name, name, len);
  memset(expansionString, 0, 25*sizeof(UChar));
  UChar32 c;
  U16_GET(name, 0, 0, len, c);
  firstCC = u_getCombiningClass(c);
  U16_GET(name, 0, len-1, len, c);
  lastCC = u_getCombiningClass(c);
}

Line::Line(const UChar name)
{
    init();
    len = 1;
    this->name[0] = name;
    this->name[1] = 0;
  memset(expansionString, 0, 25*sizeof(UChar));
  firstCC = u_getCombiningClass(name);
  lastCC = firstCC;
}

Line::Line(const UnicodeString &string)
{
  init();
  setTo(string);
}

Line::Line(const char *buff, int32_t buffLen, UErrorCode &status) :
previous(NULL),
next(NULL),
left(NULL),
right(NULL)
{
  initFromString(buff, buffLen, status);
}

Line::Line(const Line &other) :
  previous(NULL),
  next(NULL),
left(NULL),
right(NULL)
{
  *this = other;
}
         
Line &
Line::operator=(const Line &other) {
  len = other.len;
  expLen = other.expLen;
  strength = other.strength;
  strengthFromEmpty = other.strengthFromEmpty;
  cumulativeStrength = other.cumulativeStrength;
  expStrength = other.expStrength;
  isContraction = other.isContraction;
  isExpansion = other.isExpansion;
  isRemoved = other.isRemoved;
  isReset = other.isReset;
  expIndex = other.expIndex;
  firstCC = other.firstCC;
  lastCC = other.lastCC;
  u_strcpy(name, other.name);
  u_strcpy(expansionString, other.expansionString);
  sortKey = other.sortKey;
  left = other.left;
  right = other.right;
  return *this;
}

UBool 
Line::operator==(const Line &other) const {
  if(this == &other) {
    return TRUE;
  }
  if(len != other.len) {
    return FALSE;
  }
  if(u_strcmp(name, other.name) != 0) {
    return FALSE;
  }
  return TRUE;
}

UBool 
Line::equals(const Line &other) const {
  if(this == &other) {
    return TRUE;
  }
  if(len != other.len) {
    return FALSE;
  }
  if(u_strcmp(name, other.name) != 0) {
    return FALSE;
  }
  if(strength != other.strength) {
    return FALSE;
  }
  if(expLen != other.expLen) {
    return FALSE;
  }
  if(u_strcmp(expansionString, other.expansionString)) {
    return FALSE;
  }
  return TRUE;
}

UBool
Line::operator!=(const Line &other) const {
  return !(*this == other);
}


Line::~Line() {
}

void
Line::copyArray(Line *dest, const Line *src, int32_t size) {
  int32_t i = 0;
  for(i = 0; i < size; i++) {
    dest[i] = src[i];
  }
}

void
Line::setName(const UChar* name, int32_t len) {
  this->len = len;
  u_memcpy(this->name, name, len);
  UChar32 c;
  U16_GET(name, 0, 0, len, c);
  firstCC = u_getCombiningClass(c);
  U16_GET(name, 0, len-1, len, c);
  lastCC = u_getCombiningClass(c);
}

void 
Line::setToConcat(const Line *first, const Line *second) {
  u_strcpy(name, first->name);
  u_strcat(name, second->name);
  len = first->len + second->len;
  firstCC = first->firstCC;
  lastCC = second->lastCC;
}

UnicodeString
Line::stringToName(UChar *string, int32_t len) {
  UErrorCode status = U_ZERO_ERROR;
  UnicodeString result;
  char buffer[256];
  int32_t i = 0;
  UChar32 c; 
  while(i < len) {
    U16_NEXT(string, i, len, c);
    if(c < 0x10000) {
      sprintf(buffer, "%04X ", c);
    } else {
      sprintf(buffer, "%06X ", c);
    }
    result.append(buffer);
  }
  i = 0;
  while(i < len) {
    U16_NEXT(string, i, len, c);
    u_charName(c, U_EXTENDED_CHAR_NAME, buffer, 256, &status);
    result.append("{");
    result.append(buffer);
    result.append("} ");
  }
/*
  for(i = 0; i < len; i++) {
    sprintf(buffer, "%04X ", string[i]);
    result.append(buffer);
  }
  for(i = 0; i < len; i++) {
    u_charName(string[i], U_EXTENDED_CHAR_NAME, buffer, 256, &status);
    result.append("{");
    result.append(buffer);
    result.append("} ");
  }
*/  
  return result;
}

UnicodeString
Line::toBundleString() 
{

  UnicodeString result;
  UErrorCode status = U_ZERO_ERROR;
  if(!needsQuoting) {
    needsQuoting = new UnicodeSet("[[:whitespace:][:c:][:z:][[:ascii:]-[a-zA-Z0-9]]]", status);
  }
  UChar NFC[50];
  int32_t NFCLen = unorm_normalize(name, len, UNORM_NFC, 0, NFC, 50, &status);
  result.append("\"");
  if(isReset) {
    result.append("&");
  } else {
    result.append(strengthToString(strength, FALSE, FALSE));
  }
  UBool quote = needsQuoting->containsSome(name) || needsQuoting->containsSome(NFC);
  if(quote) {
    result.append("'");
  }
  if(NFC[0] == 0x22) {
    result.append("\\u0022");
  } else {
    result.append(NFC, NFCLen);
  }
  if(quote && NFC[0] != 0x0027) {
    result.append("'");
  }
  if(expLen && !isReset) {
    quote = needsQuoting->containsSome(expansionString);
    result.append(" / ");
    if(quote) {
      result.append("'");
    }
    result.append(expansionString);
    if(quote) {
      result.append("'");
    }
  }
  result.append("\" //");

  result.append(stringToName(NFC, NFCLen));
  if(expLen && !isReset) {
    result.append(" / ");
    result.append(stringToName(expansionString, expLen));
  }
  result.append("\n");
  return result;
}

UnicodeString
Line::toHTMLString() 
{
  UnicodeString result;
  UErrorCode status = U_ZERO_ERROR;
  UChar NFC[50];
  int32_t NFCLen = unorm_normalize(name, len, UNORM_NFC, 0, NFC, 50, &status);
  result.append("<span title=\"");
  result.append(stringToName(NFC, NFCLen));
  if(expLen && !isReset) {
    result.append(" / ");
    result.append(stringToName(expansionString, expLen));
  }
  result.append("\">");
  if(isReset) {
    result.append("&amp;");
  } else {
    result.append(strengthToString(strength, FALSE, TRUE));
  }
  result.append(NFC, NFCLen);
  if(expLen && !isReset) {
    result.append("&nbsp;/&nbsp;");
    result.append(expansionString);
  }
  result.append("</span><br>\n");
  return result;
}

UnicodeString
Line::toString(UBool pretty) {
  UnicodeString result;
  if(!pretty) {
    result.setTo(name);
    if(expLen) {
      result.append("/");
      result.append(expansionString);
    }
  } else {
    UErrorCode status = U_ZERO_ERROR;
    UChar NFC[50];
    int32_t NFCLen = unorm_normalize(name, len, UNORM_NFC, 0, NFC, 50, &status);
    result.setTo(NFC, NFCLen);
    if(expLen) {
      result.append("/");
      result.append(expansionString);
    }
    /*
    if(NFCLen != len || u_strncmp(name, NFC, len) != 0) {
      result.append("(NFC: ");
      result.append(NFC, NFCLen);
      result.append(stringToName(NFC, NFCLen));
      result.append(")");
    }
    */
    result.append("    # ");
    result.append(stringToName(NFC, NFCLen));
    if(expLen) {
      result.append("/ ");
      result.append(stringToName(expansionString, expLen));
    }
  }
  return result;
}


void
Line::setTo(const UnicodeString &string) {
  int32_t len = string.length();
  u_strncpy(name, string.getBuffer(), len);
  name[len] = 0;
  this->len = len;
  UChar32 c;
  U16_GET(name, 0, 0, len, c);
  firstCC = u_getCombiningClass(c);
  U16_GET(name, 0, len-1, len, c);
  lastCC = u_getCombiningClass(c);
}

void 
Line::setTo(const UChar32 n) {
  UBool isError = FALSE;
  len = 0; // we are setting the line to char, not appending
  U16_APPEND(name, len, 25, n, isError);
  name[len] = 0;
  firstCC = u_getCombiningClass(n);
  lastCC = firstCC;
}


UnicodeString
Line::strengthIndent(UColAttributeValue strength, int indentSize, UnicodeString &result) 
{
  int i;
  int numIndents = strength+1;
  if(strength > UCOL_IDENTICAL) {
    return result;
  } else if(strength == UCOL_IDENTICAL) {
    numIndents = 5;
  }
  for(i = 0; i < numIndents*indentSize; i++) {
    result.append(" ");
  }
  return result;
}

UnicodeString 
Line::strengthToString(UColAttributeValue strength, UBool pretty, UBool html) {
  UnicodeString result;
  if(html) {
    switch(strength) {
    case UCOL_IDENTICAL:
      result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;=&nbsp;");
      break;
    case UCOL_QUATERNARY:
      result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;&lt;&lt;&lt;&nbsp;");
      break;
    case UCOL_TERTIARY:
      result.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;&lt;&lt;&nbsp;");
      break;
    case UCOL_SECONDARY:
      result.append("&nbsp;&nbsp;&nbsp;&nbsp;&lt;&lt;&nbsp;");
      break;
    case UCOL_PRIMARY:
      result.append("&nbsp;&nbsp;&lt;&nbsp;");
      break;
    case UCOL_OFF:
      result.append("&nbsp;&gt;?&nbsp;");
      break;
    default:
      result.append("&nbsp;?!&nbsp;");
      break;
    }
  } else {
    switch(strength) {
    case UCOL_IDENTICAL:
      if(pretty) {
        result.append("        ");
      }
      result.append(" = ");
      break;
    case UCOL_QUATERNARY:
      if(pretty) {
        result.append("        ");
      }
      result.append(" <<<< ");
      break;
    case UCOL_TERTIARY:
      //u_fprintf(file, "<3");
      if(pretty) {
        result.append("      ");
      }
      result.append(" <<< ");
      break;
    case UCOL_SECONDARY:
      //u_fprintf(file, "<2");
      if(pretty) {
        result.append("    ");
      }
      result.append(" << ");
      break;
    case UCOL_PRIMARY:
      //u_fprintf(file, "<1");
      if(pretty) {
        result.append("  ");
      }
      result.append(" < ");
      break;
    case UCOL_OFF:
      result.append(" >? ");
      break;
    default:
      result.append(" ?! ");
      break;
    }
  }
  return result;
}

Line *
Line::nextInteresting() {
  Line *result = this->next;
  while(result && result->strength != UCOL_IDENTICAL) {
    result = result->next;
  }
  return result;
}

void
Line::append(const UChar* n, int32_t length) 
{
  u_strncat(name, n, length);
  name[len+length] = 0;
  len += length;
  UChar32 end;
  U16_GET(n, 0, length-1, length, end);
  lastCC = u_getCombiningClass(end);
}

void
Line::append(const UChar n) 
{
  name[len] = n;
  name[len+1] = 0;
  len++;
  lastCC = u_getCombiningClass(n);
}

void
Line::append(const Line &l)
{
  append(l.name, l.len);
  lastCC = l.lastCC;
}

void
Line::clear()
{
  name[0] = 0;
  len = 0;
}

int32_t
Line::write(char *buff, int32_t, UErrorCode &) 
{
  /*
    UChar     name[25];
    int32_t   len;
    UChar     expansionString[25];
    int32_t   expLen;

    UColAttributeValue strength;
    UColAttributeValue strengthFromEmpty;
    UColAttributeValue cumulativeStrength;
    UColAttributeValue expStrength;

    Line *previous;
    Line *next;

    UBool   isContraction;
    UBool   isExpansion;
    UBool   isRemoved;
    UBool   isReset;

    int32_t expIndex;
    uint8_t firstCC;
    uint8_t lastCC;
*/
  int32_t resLen = 0;
  int32_t i = 0;
  sprintf(buff+resLen, "%04X", name[0]);
  resLen += 4;
  for(i = 1; i < len; i++) {
    sprintf(buff+resLen, " %04X", name[i]);
    resLen += 5;
  }
  sprintf(buff+resLen, "/");
  resLen += 1;

  i = 0;
  if(expLen) {
    sprintf(buff+resLen, "%04X", expansionString[0]);
    resLen += 4;
    for(i = 1; i < expLen; i++) {
      sprintf(buff+resLen, " %04X", expansionString[i]);
      resLen += 5;
    }
  }
  sprintf(buff+resLen, "; ");
  resLen += 2;

  sprintf(buff+resLen, "%02i ", strength);
  resLen += 3;
  sprintf(buff+resLen, "%02i", strengthFromEmpty);
  resLen += 2;
  sprintf(buff+resLen, "%02i", cumulativeStrength);
  resLen += 2;
  sprintf(buff+resLen, "%02i", expStrength);
  resLen += 2;

  // Various flags. The only interesting ones are isReset and isRemoved. We will not output removed lines
  //sprintf(buff+resLen, "%1i%1i%1i%1i ", isContraction, isExpansion, isRemoved, isReset);
  //resLen += 5;
  sprintf(buff+resLen, "%1i%1i ", isRemoved, isReset);
  resLen += 3;

  // first and last CC
  // can be calculated on reading
  //sprintf(buff+resLen, "%03i %03i ", firstCC, lastCC);
  //resLen += 8;

  sprintf(buff+resLen, "%08X", expIndex);
  resLen += 8;

  buff[resLen] = 0;

  return resLen;
}

void
Line::initFromString(const char *buff, int32_t, UErrorCode &)
{
  int32_t bufIndex = 0;
  int32_t i = 0;

  sscanf(buff+bufIndex, "%04X", &name[i]);
  i++;
  bufIndex += 4;
  while(buff[bufIndex] != '/') {
    sscanf(buff+bufIndex, " %04X", &name[i]);
    i++;
    bufIndex += 5;
  }
  len = i;
  name[len] = 0;
  bufIndex++;

  if(i > 1) {
    isContraction = TRUE;
  } else {
    isContraction = FALSE;
  }

  if(buff[bufIndex] == ';') {
    isExpansion = FALSE;
    bufIndex += 2;
    expansionString[0] = 0;
    expLen = 0;
  } else {
    i = 0;
    sscanf(buff+bufIndex, "%04X", &expansionString[i]);
    i++;
    bufIndex += 4;
    while(buff[bufIndex] != ';') {
      sscanf(buff+bufIndex, " %04X", &expansionString[i]);
      i++;
      bufIndex += 5;
    }
    expLen = i;
    expansionString[expLen] = 0;
    bufIndex += 2;
  }
  sscanf(buff+bufIndex, "%02i ", &strength);
  bufIndex += 3;
  sscanf(buff+bufIndex, "%02i", &strengthFromEmpty);
  bufIndex += 2;
  sscanf(buff+bufIndex, "%02i", &cumulativeStrength);
  bufIndex += 2;
  sscanf(buff+bufIndex, "%02i", &expStrength);
  bufIndex += 2;

  sscanf(buff+bufIndex, "%1i%1i ", &isRemoved, &isReset);
  bufIndex += 3;

  sscanf(buff+bufIndex, "%08X", &expIndex);
  bufIndex += 8;

  // calculate first and last CC
  UChar32 c;
  U16_GET(name, 0, 0, len, c);
  firstCC = u_getCombiningClass(c);
  U16_GET(name, 0, len-1, len, c);
  lastCC = u_getCombiningClass(c);
}

void
Line::swapCase(UChar *string, int32_t &sLen)
{
  UChar32 c = 0;
  int32_t i = 0, j = 0;
  UChar buff[256];
  UBool isError = FALSE;
  while(i < sLen) {
    U16_NEXT(string, i, sLen, c);
    if(u_isUUppercase(c)) {
      c = u_tolower(c);
    } else if(u_isULowercase(c)) {
      c = u_toupper(c);
    }
    U16_APPEND(buff, j, 256, c, isError);
  }
  buff[j] = 0;
  u_strcpy(string, buff);
  sLen = j;
}


void
Line::swapCase()
{
  swapCase(name, len);
  swapCase(expansionString, expLen);
}

UnicodeString
Line::dumpSortkey() 
{

  char buffer[256];
  char *buff = buffer;
  *buff = 0;
  uint8_t *key = sortKey;
  if(sortKey) {
    while(*key) {
      sprintf(buff, "%02X ", *key);
      key++;
      buff += 3;
      if(buff - buffer > 252) {
        break;
      }
    }
  }
  return UnicodeString(buffer);
}

