#ifndef UCOL_TOKENS_H
#define UCOL_TOKENS_H

#include "ucolimp.h"

#define UCOL_TOK_POLARITY_NEGATIVE 0
#define UCOL_TOK_POLARITY_POSITIVE 1

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
  uint32_t nextCE;
  uint32_t previousCE;
  uint32_t strongest[2];
/*
  UColAttributeValue strongestP;
  UColAttributeValue strongestN;
*/
} UColTokListHeader;

struct UColToken {
  UChar debugSource;
  UChar debugExpansion;
  uint32_t source;
  uint32_t expansion;
  uint32_t strength;
  uint32_t polarity; /* 1 for <, <<, <<<, , ; and -1 for >, >>, >>> */
  UColTokListHeader *listHeader;
  UColToken* previous;
  UColToken* next;
};

typedef struct {
  const UChar *source;
  const UChar *end;
  const UChar *current;
  const InverseTableHeader *invUCA;
  const UCollator *UCA;
  uint32_t resultLen;
  UColTokListHeader *lh;
} UColTokenParser;


#define ucol_tok_isSpecialChar(ch)              \
     (((((ch) <= 0x002F) && ((ch) >= 0x0020))|| \
      (((ch) <= 0x003F) && ((ch) >= 0x003A)) || \
      (((ch) <= 0x0060) && ((ch) >= 0x005B)) || \
      (((ch) <= 0x007E) && ((ch) >= 0x007B))))


U_CFUNC UColToken *ucol_tok_parse_next_token(UColTokenParser *src, UErrorCode *status);
U_CFUNC UColToken *ucol_tok_open();
U_CFUNC uint32_t ucol_tok_assembleTokenList(UColTokenParser *src, UErrorCode *status);
#endif
