#ifndef UCOL_TOKENS_H
#define UCOL_TOKENS_H

#include "ucolimp.h"

typedef struct UColToken UColToken;

struct UColToken {
  UColToken* previous;
  UColToken* next;
  uint32_t source;
  uint32_t expansion;
  UColAttributeValue strength;
  int32_t polarity;
};

typedef struct ListHeader ListHeader;

struct  ListHeader {
  UColToken* firstPositive;
  UColToken* lastPositive;
  UColToken* firstNegative;
  UColToken* lastNegative;
  UColToken* reset;
  uint32_t baseCE;
  uint32_t nextCE;
  uint32_t previousCE;
  UColAttributeValue strongestP;
  UColAttributeValue strongestN;
};

typedef struct UColTokenParser UColTokenParser;

struct UColTokenParser {
  const UChar *source;
  const UChar *end;
  const UChar *current;
};


U_CFUNC UColToken *ucol_tok_parse_next_token(UColTokenParser *src);
U_CFUNC UColToken *ucol_tok_open();
U_CFUNC ListHeader* ucol_tok_assembleTokenList(const UChar *rules, const int32_t rulesLength, int32_t *resultLen, UErrorCode *status);
#endif
