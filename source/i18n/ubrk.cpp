/*
*****************************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*/

#include "unicode/ubrk.h"

#include "unicode/brkiter.h"
#include "unicode/uloc.h"
#include "unicode/ustring.h"
#include "unicode/uchriter.h"

U_CAPI UBreakIterator*
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
    result = BreakIterator::createCharacterInstance(Locale().init(locale), *status);
    break;

  case UBRK_WORD:
    result = BreakIterator::createWordInstance(Locale().init(locale), *status);
    break;

  case UBRK_LINE:
    result = BreakIterator::createLineInstance(Locale().init(locale), *status);
    break;

  case UBRK_SENTENCE:
    result = BreakIterator::createSentenceInstance(Locale().init(locale), *status);
    break;
  }

  // check for allocation error
  if(result == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  int32_t textLen = (textLength == -1 ? u_strlen(text) : textLength);
  UCharCharacterIterator *iter = 0;
  iter = new UCharCharacterIterator(text, textLen);
  if(iter == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    delete result;
    return 0;
  }
  result->adoptText(iter);

  return (UBreakIterator*)result;
}

U_CAPI UBreakIterator*
ubrk_openRules(const UChar *rules,
           int32_t rulesLength,
           const UChar *text,
           int32_t textLength,
           UErrorCode *status)
{
  if(U_FAILURE(*status)) return 0;
  *status = U_UNSUPPORTED_ERROR;
  return 0;
}

U_CAPI void
ubrk_close(UBreakIterator *bi)
{
  delete (BreakIterator*) bi;
}

U_CAPI void
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

U_CAPI UTextOffset
ubrk_current(const UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->current();
}

U_CAPI UTextOffset
ubrk_next(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->next();
}

U_CAPI UTextOffset
ubrk_previous(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->previous();
}

U_CAPI UTextOffset
ubrk_first(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->first();
}

U_CAPI UTextOffset
ubrk_last(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->last();
}

U_CAPI UTextOffset
ubrk_preceding(UBreakIterator *bi,
           UTextOffset offset)
{
  return ((BreakIterator*)bi)->preceding(offset);
}

U_CAPI UTextOffset
ubrk_following(UBreakIterator *bi,
           UTextOffset offset)
{
  return ((BreakIterator*)bi)->following(offset);
}

U_CAPI const char*
ubrk_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

U_CAPI int32_t
ubrk_countAvailable()
{
  return uloc_countAvailable();
}
