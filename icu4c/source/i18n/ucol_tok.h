/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_tok.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 02/22/2001
*   created by: Vladimir Weinstein
*
* This module reads a tailoring rule string and produces a list of 
* tokens that will be turned into collation elements
* 
*/

#ifndef UCOL_TOKENS_H
#define UCOL_TOKENS_H

#include "ucol_imp.h"
#include "uhash.h"

#define UCOL_TOK_UNSET 0xFFFFFFFF
#define UCOL_TOK_RESET 0xDEADBEEF

#define UCOL_TOK_POLARITY_NEGATIVE 0
#define UCOL_TOK_POLARITY_POSITIVE 1

#define UCOL_TOK_TOP 0x04
#define UCOL_TOK_VARIABLE_TOP 0x08
#define UCOL_TOK_BEFORE 0x03
#define UCOL_TOK_SUCCESS 0x10

/* this is space for the extra strings that need to be unquoted */
/* during the parsing of the rules */
#define UCOL_TOK_EXTRA_RULE_SPACE_SIZE 1024
typedef struct UColToken UColToken;

typedef struct  {
  UColToken* first;
  UColToken* last;
  UColToken* reset;
  uint32_t baseCE;
  uint32_t baseContCE;
  uint32_t nextCE;
  uint32_t nextContCE;
  uint32_t previousCE;
  uint32_t previousContCE;
  int32_t pos[UCOL_STRENGTH_LIMIT];
  uint32_t gapsLo[3*UCOL_CE_STRENGTH_LIMIT];
  uint32_t gapsHi[3*UCOL_CE_STRENGTH_LIMIT];
  uint32_t numStr[UCOL_CE_STRENGTH_LIMIT];
  UColToken* fStrToken[UCOL_CE_STRENGTH_LIMIT];
  UColToken* lStrToken[UCOL_CE_STRENGTH_LIMIT];
} UColTokListHeader;

struct UColToken {
  UChar debugSource;
  UChar debugExpansion;
  uint32_t CEs[128];
  uint32_t noOfCEs;
  uint32_t expCEs[128];
  uint32_t noOfExpCEs;
  uint32_t source;
  uint32_t expansion;
  uint32_t strength;
  uint32_t toInsert;
  uint32_t polarity; /* 1 for <, <<, <<<, , ; and -1 for >, >>, >>> */
  UColTokListHeader *listHeader;
  UColToken* previous;
  UColToken* next;
};

typedef struct {
  UChar *source;
  UChar *end;
  UChar *current;
  UChar *sourceCurrent;
  UChar *extraCurrent;
  UChar *extraEnd;
  const InverseTableHeader *invUCA;
  const UCollator *UCA;
  UHashtable *tailored;
  UColOptionSet *opts;
  uint32_t resultLen;
  UColTokListHeader *lh;
  UColToken *varTop;
} UColTokenParser;

typedef struct {
  const UChar *subName;
  int32_t subLen;
  UColAttributeValue attrVal;
} ucolTokSuboption;

typedef struct {
   const UChar *optionName;
   int32_t optionLen;
   ucolTokSuboption *subopts;
   int32_t subSize;
   UColAttribute attr;
} ucolTokOption;

#define ucol_tok_isSpecialChar(ch)              \
    (((((ch) <= 0x002F) && ((ch) >= 0x0020)) || \
      (((ch) <= 0x003F) && ((ch) >= 0x003A)) || \
      (((ch) <= 0x0060) && ((ch) >= 0x005B)) || \
      (((ch) <= 0x007E) && ((ch) >= 0x007B))))


U_CFUNC uint32_t ucol_tok_assembleTokenList(UColTokenParser *src, UErrorCode *status);
U_CFUNC void ucol_tok_closeTokenList(UColTokenParser *src);

void ucol_uprv_tok_setOptionInImage(UColOptionSet *opts, UColAttribute attrib, UColAttributeValue value);
UBool ucol_uprv_tok_readAndSetOption(UColOptionSet *opts, const UChar* start, const UChar *end, UBool *variableTop, UBool *top, UErrorCode *status);

void ucol_tok_initTokenList(UColTokenParser *src, const UChar *rules, const uint32_t rulesLength, UCollator *UCA, UErrorCode *status);
uint32_t ucol_uprv_tok_assembleTokenList(UColTokenParser *src, UErrorCode *status);
U_CAPI const UChar U_EXPORT2 *ucol_tok_parseNextToken(UColTokenParser *src, 
                        uint32_t *strength, 
                        uint32_t *chOffset, uint32_t *chLen, 
                        uint32_t *exOffset, uint32_t *exLen,
                        uint8_t *specs,
                        UBool startOfRules,
                        UErrorCode *status);


#endif





