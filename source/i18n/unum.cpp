/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1996                                       *
*   (C) Copyright International Business Machines Corporation,  1998-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
* Modification History:
*
*   Date        Name        Description
*   06/24/99    helena      Integrated Alan's NF enhancements and Java2 bug fixes
*******************************************************************************
*/

#include "unum.h"

#include "uloc.h"
#include "numfmt.h"
#include "decimfmt.h"
//#include "spellfmt.h"
#include "ustring.h"
#include "cpputils.h"
#include "fmtable.h"
#include "dcfmtsym.h"

CAPI UNumberFormat*
unum_open(    UNumberFormatStyle    style,
        const   char*        locale,
        UErrorCode*        status)
{
  if(FAILURE(*status)) return 0;
  UNumberFormat *retVal = 0;
  
  switch(style) {
  case UNUM_DECIMAL:
    if(locale == 0)
      retVal = (UNumberFormat*)NumberFormat::createInstance(*status);
    else
      retVal = (UNumberFormat*)NumberFormat::createInstance(Locale().init(locale),
                                *status);
    break;

  case UNUM_CURRENCY:
    if(locale == 0)
      retVal = (UNumberFormat*)NumberFormat::createCurrencyInstance(*status);
    else
      retVal = (UNumberFormat*)NumberFormat::createCurrencyInstance(Locale().init(locale),
                                    *status);
    break;

  case UNUM_PERCENT:
    if(locale == 0)
      retVal = (UNumberFormat*)NumberFormat::createPercentInstance(*status);
    else
      retVal = (UNumberFormat*)NumberFormat::createPercentInstance(Locale().init(locale),
                                *status);
    break;

  case UNUM_SPELLOUT:
    // TBD: Add spellout support
    //retVal = (UNumberFormat*)new NumberSpelloutFormat();
    *status = U_UNSUPPORTED_ERROR;
    return 0;
    break;
  }

  if(retVal == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return retVal;
}

CAPI UNumberFormat*
unum_openPattern(    const    UChar*            pattern,
            int32_t            patternLength,
            const    char*        locale,
            UErrorCode*        status)
{
  if(FAILURE(*status)) return 0;

  int32_t len = (patternLength == -1 ? u_strlen(pattern) : patternLength);
  const UnicodeString pat((UChar*)pattern, len, len);
  DecimalFormatSymbols *syms = 0;

  if(locale == 0)
    syms = new DecimalFormatSymbols(*status);
  else
    syms = new DecimalFormatSymbols(Locale().init(locale),
                    *status);
  
  if(syms == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  DecimalFormat *fmt = 0;
  fmt = new DecimalFormat(pat, syms, *status);
  if(fmt == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    delete syms;
    return 0;
  }

  return (UNumberFormat*) fmt;
}

CAPI void
unum_close(UNumberFormat* fmt)
{
  delete (NumberFormat*) fmt;
}

CAPI UNumberFormat*
unum_clone(const UNumberFormat *fmt,
       UErrorCode *status)
{
  if(FAILURE(*status)) return 0;

  Format *res = ((DecimalFormat*)fmt)->clone();
  
  if(res == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UNumberFormat*) res;
}

CAPI int32_t
unum_format(    const    UNumberFormat*    fmt,
        int32_t            number,
        UChar*            result,
        int32_t            resultLength,
        UFieldPosition    *pos,
        UErrorCode*        status)
{
  if(FAILURE(*status)) return -1;

  int32_t actSize;

  UnicodeString res(result, 0, resultLength);
  FieldPosition fp;
  
  if(pos != 0)
    fp.setField(pos->field);
  
  ((NumberFormat*)fmt)->format(number, res, fp);
  T_fillOutputParams(&res, result, resultLength, &actSize, status);
  
  if(pos != 0) {
    pos->beginIndex = fp.getBeginIndex();
    pos->endIndex = fp.getEndIndex();
  }
  
  return actSize;  
}

CAPI int32_t
unum_formatDouble(    const    UNumberFormat*  fmt,
            double          number,
            UChar*          result,
            int32_t         resultLength,
            UFieldPosition  *pos, /* 0 if ignore */
            UErrorCode*     status)
{
  if(FAILURE(*status)) return -1;

  int32_t actSize;

  UnicodeString res(result, 0, resultLength);
  FieldPosition fp;
  
  if(pos != 0)
    fp.setField(pos->field);
  
  ((NumberFormat*)fmt)->format(number, res, fp);
  T_fillOutputParams(&res, result, resultLength, &actSize, status);
  
  if(pos != 0) {
    pos->beginIndex = fp.getBeginIndex();
    pos->endIndex = fp.getEndIndex();
  }
  
  return actSize;  
}

CAPI int32_t
unum_parse(    const   UNumberFormat*  fmt,
        const   UChar*          text,
        int32_t         textLength,
        int32_t         *parsePos /* 0 = start */,
        UErrorCode      *status)
{
  if(FAILURE(*status)) return 0;

  int32_t len = (textLength == -1 ? u_strlen(text) : textLength);
  const UnicodeString src((UChar*)text, len, len);
  ParsePosition pp;
  Formattable res;

  if(parsePos != 0)
    pp.setIndex(*parsePos);

  ((NumberFormat*)fmt)->parse(src, res, pp);

  if(parsePos != 0) {
    if(pp.getErrorIndex() == -1)
      *parsePos = pp.getIndex();
    else {
      *parsePos = pp.getErrorIndex();
      *status = U_PARSE_ERROR;
    }
  }

  /* return the actual type of the result, cast to a long */
  return (res.getType() == Formattable::kLong) 
    ? res.getLong() 
    : (int32_t) res.getDouble();
}

CAPI double
unum_parseDouble(    const   UNumberFormat*  fmt,
            const   UChar*          text,
            int32_t         textLength,
            int32_t         *parsePos /* 0 = start */,
            UErrorCode      *status)
{
  if(FAILURE(*status)) return 0;

  int32_t len = (textLength == -1 ? u_strlen(text) : textLength);
  const UnicodeString src((UChar*)text, len, len);
  ParsePosition pp;
  Formattable res;

  if(parsePos != 0)
    pp.setIndex(*parsePos);

  ((NumberFormat*)fmt)->parse(src, res, pp);

  if(parsePos != 0) {
    if(pp.getErrorIndex() == -1)
      *parsePos = pp.getIndex();
    else {
      *parsePos = pp.getErrorIndex();
      *status = U_PARSE_ERROR;
    }
  }
  
  /* return the actual type of the result, cast to a double */
  return (res.getType() == Formattable::kDouble) 
    ? res.getDouble() 
    : (double) res.getLong();
}

CAPI const char*
unum_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

CAPI int32_t
unum_countAvailable()
{
  return uloc_countAvailable();
}

CAPI int32_t
unum_getAttribute(const UNumberFormat*          fmt,
          UNumberFormatAttribute  attr)
{
  switch(attr) {
  case UNUM_PARSE_INT_ONLY:
    return ((NumberFormat*)fmt)->isParseIntegerOnly();
    break;
    
  case UNUM_GROUPING_USED:
    return ((NumberFormat*)fmt)->isGroupingUsed();
    break;
    
  case UNUM_DECIMAL_ALWAYS_SHOWN:
    return ((DecimalFormat*)fmt)->isDecimalSeparatorAlwaysShown();    
    break;
    
  case UNUM_MAX_INTEGER_DIGITS:
    return ((NumberFormat*)fmt)->getMaximumIntegerDigits();
    break;
    
  case UNUM_MIN_INTEGER_DIGITS:
    return ((NumberFormat*)fmt)->getMinimumIntegerDigits();
    break;
    
  case UNUM_INTEGER_DIGITS:
    // TBD: what should this return?
    return ((NumberFormat*)fmt)->getMinimumIntegerDigits();
    break;
    
  case UNUM_MAX_FRACTION_DIGITS:
    return ((NumberFormat*)fmt)->getMaximumFractionDigits();
    break;
    
  case UNUM_MIN_FRACTION_DIGITS:
    return ((NumberFormat*)fmt)->getMinimumFractionDigits();
    break;
    
  case UNUM_FRACTION_DIGITS:
    // TBD: what should this return?
    return ((NumberFormat*)fmt)->getMinimumFractionDigits();
    break;

  case UNUM_MULTIPLIER:
    return ((DecimalFormat*)fmt)->getMultiplier();    
    break;
    
  case UNUM_GROUPING_SIZE:
    return ((DecimalFormat*)fmt)->getGroupingSize();    
    break;

  case UNUM_ROUNDING_MODE:
    return ((DecimalFormat*)fmt)->getRoundingMode();
    break;

  case UNUM_FORMAT_WIDTH:
    return ((DecimalFormat*)fmt)->getFormatWidth();
    break;

  /** The position at which padding will take place. */
  case UNUM_PADDING_POSITION:
    return ((DecimalFormat*)fmt)->getPadPosition();
    break;

  default:
    return -1;
    break;
  }
}

CAPI void
unum_setAttribute(    UNumberFormat*          fmt,
            UNumberFormatAttribute  attr,
            int32_t                 newValue)
{
  switch(attr) {
  case UNUM_PARSE_INT_ONLY:
    ((NumberFormat*)fmt)->setParseIntegerOnly((bool_t)newValue);
    break;
    
  case UNUM_GROUPING_USED:
    ((NumberFormat*)fmt)->setGroupingUsed((bool_t)newValue);
    break;
    
  case UNUM_DECIMAL_ALWAYS_SHOWN:
    ((DecimalFormat*)fmt)->setDecimalSeparatorAlwaysShown((bool_t)newValue);
    break;
    
  case UNUM_MAX_INTEGER_DIGITS:
    ((NumberFormat*)fmt)->setMaximumIntegerDigits(newValue);
    break;
    
  case UNUM_MIN_INTEGER_DIGITS:
    ((NumberFormat*)fmt)->setMinimumIntegerDigits(newValue);
    break;
    
  case UNUM_INTEGER_DIGITS:
    ((NumberFormat*)fmt)->setMinimumIntegerDigits(newValue);
    ((NumberFormat*)fmt)->setMaximumIntegerDigits(newValue);
    break;
    
  case UNUM_MAX_FRACTION_DIGITS:
    ((NumberFormat*)fmt)->setMaximumFractionDigits(newValue);
    break;
    
  case UNUM_MIN_FRACTION_DIGITS:
    ((NumberFormat*)fmt)->setMinimumFractionDigits(newValue);
    break;
    
  case UNUM_FRACTION_DIGITS:
    ((NumberFormat*)fmt)->setMinimumFractionDigits(newValue);
    ((NumberFormat*)fmt)->setMaximumFractionDigits(newValue);
    break;

  case UNUM_MULTIPLIER:
    ((DecimalFormat*)fmt)->setMultiplier(newValue);    
    break;
    
  case UNUM_GROUPING_SIZE:
    ((DecimalFormat*)fmt)->setGroupingSize(newValue);    
    break;

  case UNUM_ROUNDING_MODE:
      ((DecimalFormat*)fmt)->setRoundingMode((DecimalFormat::ERoundingMode)newValue);
    break;

  case UNUM_FORMAT_WIDTH:
    ((DecimalFormat*)fmt)->setFormatWidth(newValue);
    break;

  /** The position at which padding will take place. */
  case UNUM_PADDING_POSITION:
      ((DecimalFormat*)fmt)->setPadPosition((DecimalFormat::EPadPosition)newValue);
    break;
  
  }
}

CAPI double
unum_getDoubleAttribute(const UNumberFormat*          fmt,
          UNumberFormatAttribute  attr)
{
  switch(attr) {
  case UNUM_ROUNDING_INCREMENT:
    return ((DecimalFormat*)fmt)->getRoundingIncrement();
    break;

  default:
    return -1.0;
    break;
  }
}

CAPI void
unum_setDoubleAttribute(    UNumberFormat*          fmt,
            UNumberFormatAttribute  attr,
            double                 newValue)
{
  switch(attr) {   
  case UNUM_ROUNDING_INCREMENT:
    ((DecimalFormat*)fmt)->setRoundingIncrement(newValue);
    break;
  
  }
}

CAPI int32_t
unum_getTextAttribute(    const    UNumberFormat*                    fmt,
            UNumberFormatTextAttribute      tag,
            UChar*                            result,
            int32_t                            resultLength,
            UErrorCode*                        status)
{
  if(FAILURE(*status)) return -1;

  int32_t actSize = 0;

  UnicodeString res(result, 0, resultLength);

  switch(tag) {
  case UNUM_POSITIVE_PREFIX:
    ((DecimalFormat*)fmt)->getPositivePrefix(res);
    break;

  case UNUM_POSITIVE_SUFFIX:
    ((DecimalFormat*)fmt)->getPositiveSuffix(res);
    break;

  case UNUM_NEGATIVE_PREFIX:
    ((DecimalFormat*)fmt)->getNegativePrefix(res);
    break;

  case UNUM_NEGATIVE_SUFFIX:
    ((DecimalFormat*)fmt)->getNegativeSuffix(res);
    break;

  case UNUM_PADDING_CHARACTER:
    *result = ((DecimalFormat*)fmt)->getPadCharacter();
    actSize = 1;
    break;

  default:
    *status = U_UNSUPPORTED_ERROR;
    return -1;
    break;
  }

  if (actSize != 1)
    T_fillOutputParams(&res, result, resultLength, &actSize, status);
  return actSize;
}

CAPI void
unum_setTextAttribute(    UNumberFormat*                    fmt,
            UNumberFormatTextAttribute      tag,
            const    UChar*                            newValue,
            int32_t                            newValueLength,
            UErrorCode                        *status)
{
  if(FAILURE(*status)) return;

  int32_t len = (newValueLength == -1 ? u_strlen(newValue) : newValueLength);
  const UnicodeString val((UChar*)newValue, len, len);
  
  switch(tag) {
  case UNUM_POSITIVE_PREFIX:
    ((DecimalFormat*)fmt)->setPositivePrefix(val);
    break;

  case UNUM_POSITIVE_SUFFIX:
    ((DecimalFormat*)fmt)->setPositiveSuffix(val);
    break;

  case UNUM_NEGATIVE_PREFIX:
    ((DecimalFormat*)fmt)->setNegativePrefix(val);
    break;

  case UNUM_NEGATIVE_SUFFIX:
    ((DecimalFormat*)fmt)->setNegativeSuffix(val);
    break;

  case UNUM_PADDING_CHARACTER:
    ((DecimalFormat*)fmt)->setPadCharacter(*newValue);
    break;

  default:
    *status = U_UNSUPPORTED_ERROR;
    break;
  }
}

CAPI int32_t
unum_toPattern(    const    UNumberFormat*          fmt,
        bool_t                  isPatternLocalized,
        UChar*                  result,
        int32_t                 resultLength,
        UErrorCode*             status)
{
  if(FAILURE(*status)) return -1;

  int32_t actSize;

  UnicodeString pat(result, 0, resultLength);

  if(isPatternLocalized)
    ((DecimalFormat*)fmt)->toLocalizedPattern(pat);
  else
    ((DecimalFormat*)fmt)->toPattern(pat);

  T_fillOutputParams(&pat, result, resultLength, &actSize, status);
  return actSize;
}

CAPI void
unum_getSymbols(const UNumberFormat* fmt,
        UNumberFormatSymbols *syms)
{
  UnicodeString temp;
  int32_t len;
  const DecimalFormatSymbols *dfs = 
    ((DecimalFormat*)fmt)->getDecimalFormatSymbols();
  
  syms->decimalSeparator     = dfs->getDecimalSeparator();
  syms->groupingSeparator     = dfs->getGroupingSeparator();
  syms->patternSeparator     = dfs->getPatternSeparator();
  syms->percent         = dfs->getPercent();
  syms->zeroDigit         = dfs->getZeroDigit();
  syms->digit             = dfs->getDigit();
  syms->plusSign          = dfs->getPlusSign();
  syms->minusSign         = dfs->getMinusSign();
  
  dfs->getCurrencySymbol(temp);
  len = icu_min(temp.size(), UNFSYMBOLSMAXSIZE);
  u_strncpy(syms->currency, temp.getUChars(), len);
  syms->currency[len > 0 ? len + 1 : 0] = 0x0000;

  dfs->getInternationalCurrencySymbol(temp);
  len = icu_min(temp.size(), UNFSYMBOLSMAXSIZE);
  u_strncpy(syms->intlCurrency, temp.getUChars(), len);
  syms->intlCurrency[len > 0 ? len + 1 : 0] = 0x0000;
  
  syms->monetarySeparator    = dfs->getMonetaryDecimalSeparator();
  syms->exponential         = dfs->getExponentialSymbol();
  syms->perMill         = dfs->getPerMill();
  syms->padEscape       = dfs->getPadEscape();

  dfs->getInfinity(temp);
  len = icu_min(temp.size(), UNFSYMBOLSMAXSIZE);
  u_strncpy(syms->infinity, temp.getUChars(), len);
  syms->infinity[len > 0 ? len + 1 : 0] = 0x0000;

  dfs->getNaN(temp);
  len = icu_min(temp.size(), UNFSYMBOLSMAXSIZE);
  u_strncpy(syms->naN, temp.getUChars(), len);
  syms->naN[len > 0 ? len + 1 : 0] = 0x0000;
}

CAPI void
unum_setSymbols(            UNumberFormat*          fmt,
                    const   UNumberFormatSymbols*   symbolsToSet,
                    UErrorCode *status)
{
  if(FAILURE(*status)) return;

  DecimalFormatSymbols *syms = new DecimalFormatSymbols(*status);
  if(syms == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  syms->setDecimalSeparator(symbolsToSet->decimalSeparator);
  syms->setGroupingSeparator(symbolsToSet->groupingSeparator);
  syms->setPatternSeparator(symbolsToSet->patternSeparator);
  syms->setPercent(symbolsToSet->percent);
  syms->setZeroDigit(symbolsToSet->zeroDigit);
  syms->setDigit(symbolsToSet->digit);
  syms->setPlusSign(symbolsToSet->plusSign);
  syms->setMinusSign(symbolsToSet->minusSign);
  
  syms->setCurrencySymbol(symbolsToSet->currency);
  syms->setInternationalCurrencySymbol(symbolsToSet->intlCurrency);
  
  syms->setMonetaryDecimalSeparator(symbolsToSet->monetarySeparator);
  syms->setExponentialSymbol(symbolsToSet->exponential);
  syms->setPerMill(symbolsToSet->perMill);
  syms->setPadEscape(symbolsToSet->padEscape);

  syms->setInfinity(symbolsToSet->infinity);
  syms->setNaN(symbolsToSet->naN);

  ((DecimalFormat*)fmt)->adoptDecimalFormatSymbols(syms);
}
