#include "ucoltok.h"
#include "uhash.h"

static UHashtable *uchars2tokens;
static ListHeader ListList[256];

void deleteElement(void *element) {
/*
    UCAElements *el = (UCAElements *)element;

    int32_t i = 0;
    for(i = 0; i < el->noOfCEs; i++) {
        free(el->primary[i]);
        free(el->secondary[i]);
        free(el->tertiary[i]);
    }
    free(el);
*/
}

void ucol_tok_initTokenList(UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
  uchars2tokens = uhash_open(uhash_hashLong, uhash_compareLong, status);
  uhash_setValueDeleter(uchars2tokens, deleteElement);
}

UColToken *ucol_tok_open() {
  return NULL;
}

UColToken *ucol_tok_parse_next_token(UColTokenParser *src) {
  return 0;
}

ListHeader* ucol_tok_assembleTokenList(const UChar *rules, const int32_t rulesLength, int32_t *resultLen, UErrorCode *status) {
  UColToken *lastToken = NULL;
  UColToken *newToken = NULL;
  UColTokenParser src;

  src.source = rules;
  src.current = rules;
  src.end = rules+rulesLength;

  ucol_tok_initTokenList(status);
  resultLen = 0;

  while((newToken = ucol_tok_parse_next_token(&src)) != NULL) {
  }

/*
Processing Description
  1 Build a ListList. Each list has a header, which contains two lists (positive 
  and negative), a reset token, a baseCE, nextCE, and previousCE. The lists and 
  reset may be null. 
  2 As you process, you keep a LAST pointer that points to the last token you 
  handled. 
  3 Consider each item: relation, source, and expansion: e.g. ...< x / y ... 
    First convert all expansions into normal form. Examples: 
      If "xy" doesn't occur earlier in the list or in the UCA, convert &xy * c * 
      d * ... into &x * c/y * d * ... 
      Note: reset values can never have expansions, although they can cause the 
      very next item to have one. They may be contractions, if they are found 
      earlier in the list. 
  4 Lookup each [source,  expansion] in the CharsToToken map, and find a 
  sourceToken 
  5 If the relation is a reset: 
    If sourceToken is null 
      Create new list, create new sourceToken, make the baseCE from source, put 
      the sourceToken in ListHeader of the new list 
  6 Otherwise (when relation != reset) 
    If sourceToken is null, create new one, otherwise remove sourceToken from 
    where it was. 
    If LAST is a reset 
      insert sourceToken at the head of either the positive list or the negative 
      list, depending on the polarity of relation. 
      set the polarity of sourceToken to be the same as the list you put it in. 
    Otherwise (when LAST is not a reset) 
      if polarity (LAST) == polarity(relation), insert sourceToken after LAST, 
      otherwise insert before. 
      when inserting after or before, search to the next position with the same 
      strength in that direction. (This is called postpone insertion). 
  7 After all this, set LAST to point to sourceToken, and goto step 3. 
*/  

  return NULL;
}

