#ifndef UCOL_TOKENS_H
#define UCOL_TOKENS_H

#include "ucol_imp.h"

#define UCOL_TOK_POLARITY_NEGATIVE 0
#define UCOL_TOK_POLARITY_POSITIVE 1

#define UCOL_RESET_TOP_VALUE 0x9F000303
#define UCOL_NEXT_TOP_VALUE  0xD0000303

/* this is space for the extra strings that need to be unquoted */
/* during the parsing of the rules */
#define UCOL_TOK_EXTRA_RULE_SPACE_SIZE 1024
typedef struct UColToken UColToken;

typedef struct  {
  UColToken* first[2];
  UColToken* last[2];
/*
  UColToken* firstPositive;
  UColToken* lastPositive;
  UColToken* firstNegative;
  UColToken* lastNegative;
*/
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
  UCATableHeader *image;
  uint32_t resultLen;
  UColTokListHeader *lh;
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

int32_t uhash_hashTokens(const void *k);
UBool uhash_compareTokens(const void *key1, const void *key2);
void ucol_tok_initTokenList(UColTokenParser *src, UErrorCode *status);
uint32_t ucol_uprv_tok_assembleTokenList(UColTokenParser *src, UErrorCode *status);

#endif



