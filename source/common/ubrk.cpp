/*
*****************************************************************************************
*   Copyright (C) 1996-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/ubrk.h"

#include "unicode/brkiter.h"
#include "unicode/uloc.h"
#include "unicode/ustring.h"
#include "unicode/uchriter.h"
#include "unicode/rbbi.h"
#include "rbbirb.h"

U_NAMESPACE_USE

//----------------------------------------------------------------------------------------
//
//    ubrk_open      Create a canned type of break iterator based on type (word, line, etc.)
//                   and locale.
//
//----------------------------------------------------------------------------------------
U_CAPI UBreakIterator* U_EXPORT2
ubrk_open(UBreakIteratorType type,
      const char *locale,
      const UChar *text,
      int32_t textLength,
      UErrorCode *status)
{

  if(U_FAILURE(*status)) return 0;

  BreakIterator *result = 0;

  switch(type) {

  case UBRK_CHARACTER:
    result = BreakIterator::createCharacterInstance(Locale(locale), *status);
    break;

  case UBRK_WORD:
    result = BreakIterator::createWordInstance(Locale(locale), *status);
    break;

  case UBRK_LINE:
    result = BreakIterator::createLineInstance(Locale(locale), *status);
    break;

  case UBRK_SENTENCE:
    result = BreakIterator::createSentenceInstance(Locale(locale), *status);
    break;

  case UBRK_TITLE:
    result = BreakIterator::createTitleInstance(Locale(locale), *status);
    break;
  }

  // check for allocation error
  if (U_FAILURE(*status)) {
     return 0;
  }
  if(result == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  UCharCharacterIterator *iter = 0;
  iter = new UCharCharacterIterator(text, textLength);
  if(iter == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    delete result;
    return 0;
  }
  result->adoptText(iter);

  return (UBreakIterator*)result;
}



//----------------------------------------------------------------------------------------
//
//   ubrk_openRules      open a break iterator from a set of break rules.
//                       Invokes the rule builder.
//
//----------------------------------------------------------------------------------------
U_CAPI UBreakIterator* U_EXPORT2
ubrk_openRules(  const UChar        *rules,
                       int32_t       rulesLength,
                 const UChar        *text,
                       int32_t       textLength,
                       UParseError  *parseErr,
                       UErrorCode   *status)  {

    if (status == NULL || U_FAILURE(*status)){
        return 0;
    }

    BreakIterator *result = 0;
    UnicodeString ruleString(rules, rulesLength);
    result = RBBIRuleBuilder::createRuleBasedBreakIterator(ruleString, *parseErr, *status);
    if(U_FAILURE(*status)) {
        return 0;
    }

    if (text != NULL) {
        UCharCharacterIterator *iter = 0;
        iter = new UCharCharacterIterator(text, textLength);
        if(iter == 0) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            delete result;
            return 0;
        }
        result->adoptText(iter);
    }
    return (UBreakIterator *)result;
}





U_CAPI UBreakIterator * U_EXPORT2
ubrk_safeClone(
          const UBreakIterator *bi,
          void *stackBuffer,
          int32_t *pBufferSize,
          UErrorCode *status)
{
    if (status == NULL || U_FAILURE(*status)){
        return 0;
    }
    if (!pBufferSize || !bi){
       *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    return (UBreakIterator *)(((BreakIterator*)bi)->
        createBufferClone(stackBuffer, *pBufferSize, *status));
}



U_CAPI void U_EXPORT2
ubrk_close(UBreakIterator *bi)
{
    BreakIterator *ubi = (BreakIterator*) bi;
    if (ubi) {
        if (ubi->isBufferClone()) {
            ubi->~BreakIterator();
            *(uint32_t *)ubi = 0xdeadbeef;
        } else {
            delete ubi;
        }
    }
}

U_CAPI void U_EXPORT2
ubrk_setText(UBreakIterator* bi,
             const UChar*    text,
             int32_t         textLength,
             UErrorCode*     status)
{

  if (U_FAILURE(*status)) return;

  const CharacterIterator& biText = ((BreakIterator*)bi)->getText();

  int32_t textLen = (textLength == -1 ? u_strlen(text) : textLength);
  if (biText.getDynamicClassID() == UCharCharacterIterator::getStaticClassID()) {
      ((UCharCharacterIterator&)biText).setText(text, textLen);
  }
  else {
      UCharCharacterIterator *iter = 0;
      iter = new UCharCharacterIterator(text, textLen);
      if(iter == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return;
      }
      ((BreakIterator*)bi)->adoptText(iter);
  }
}

U_CAPI int32_t U_EXPORT2
ubrk_current(const UBreakIterator *bi)
{

  return ((BreakIterator*)bi)->current();
}

U_CAPI int32_t U_EXPORT2
ubrk_next(UBreakIterator *bi)
{

  return ((BreakIterator*)bi)->next();
}

U_CAPI int32_t U_EXPORT2
ubrk_previous(UBreakIterator *bi)
{

  return ((BreakIterator*)bi)->previous();
}

U_CAPI int32_t U_EXPORT2
ubrk_first(UBreakIterator *bi)
{

  return ((BreakIterator*)bi)->first();
}

U_CAPI int32_t U_EXPORT2
ubrk_last(UBreakIterator *bi)
{

  return ((BreakIterator*)bi)->last();
}

U_CAPI int32_t U_EXPORT2
ubrk_preceding(UBreakIterator *bi,
           int32_t offset)
{

  return ((BreakIterator*)bi)->preceding(offset);
}

U_CAPI int32_t U_EXPORT2
ubrk_following(UBreakIterator *bi,
           int32_t offset)
{

  return ((BreakIterator*)bi)->following(offset);
}

U_CAPI const char* U_EXPORT2
ubrk_getAvailable(int32_t index)
{

  return uloc_getAvailable(index);
}

U_CAPI int32_t U_EXPORT2
ubrk_countAvailable()
{

  return uloc_countAvailable();
}


U_CAPI  UBool U_EXPORT2
ubrk_isBoundary(UBreakIterator *bi, int32_t offset)
{
    return ((BreakIterator *)bi)->isBoundary(offset);
}


U_CAPI  int32_t U_EXPORT2
ubrk_getRuleStatus(UBreakIterator *bi)
{
    return ((RuleBasedBreakIterator *)bi)->getRuleStatus();
}

U_CAPI  int32_t U_EXPORT2
ubrk_getRuleStatusVec(UBreakIterator *bi, int32_t *fillInVec, int32_t capacity, UErrorCode *status)
{
    return ((RuleBasedBreakIterator *)bi)->getRuleStatusVec(fillInVec, capacity, *status);
}


U_CAPI const char* U_EXPORT2
ubrk_getLocaleByType(const UBreakIterator *bi, 
                     ULocDataLocaleType type, 
                     UErrorCode* status)
{
    if (bi == NULL) {
        if (U_SUCCESS(*status)) {
            *status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        return NULL;
    }
    return ((BreakIterator*)bi)->getLocaleID(type, *status);
}


#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
