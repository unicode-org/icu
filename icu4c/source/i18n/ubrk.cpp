/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1998-1999               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

#include "ubrk.h"

#include "brkiter.h"
#include "uloc.h"
#include "ustring.h"
#include "uchriter.h"

CAPI UBreakIterator*
ubrk_open(UBreakIteratorType type,
      const char *locale,
      const UChar *text,
      int32_t textLength,
      UErrorCode *status)
{
  if(FAILURE(*status)) return 0;
  
  BreakIterator *result = 0;
  
  switch(type) {

  case UBRK_CHARACTER:
    result = BreakIterator::createCharacterInstance(Locale().init(locale));
    break;

  case UBRK_WORD:
    result = BreakIterator::createWordInstance(Locale().init(locale));
    break;

  case UBRK_LINE:
    result = BreakIterator::createLineInstance(Locale().init(locale));
    break;

  case UBRK_SENTENCE:
    result = BreakIterator::createSentenceInstance(Locale().init(locale));
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

CAPI UBreakIterator*
ubrk_openRules(const UChar *rules,
           int32_t rulesLength,
           const UChar *text,
           int32_t textLength,
           UErrorCode *status)
{
  if(FAILURE(*status)) return 0;

  return 0;
}

CAPI void
ubrk_close(UBreakIterator *bi)
{
  delete (BreakIterator*) bi;
}

CAPI UTextOffset
ubrk_current(const UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->current();
}

CAPI UTextOffset
ubrk_next(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->next();
}

CAPI UTextOffset
ubrk_previous(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->previous();
}

CAPI UTextOffset
ubrk_first(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->first();
}

CAPI UTextOffset
ubrk_last(UBreakIterator *bi)
{
  return ((BreakIterator*)bi)->last();
}

CAPI UTextOffset
ubrk_preceding(UBreakIterator *bi,
           UTextOffset offset)
{
  return ((BreakIterator*)bi)->preceding(offset);
}

CAPI UTextOffset
ubrk_following(UBreakIterator *bi,
           UTextOffset offset)
{
  return ((BreakIterator*)bi)->following(offset);
}

CAPI const char*
ubrk_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

CAPI int32_t
ubrk_countAvailable()
{
  return uloc_countAvailable();
}
