/*
*******************************************************************************
*   Copyright (C) 1996-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
* Modification History:
*
*   Date        Name        Description
*   06/24/99    helena      Integrated Alan's NF enhancements and Java2 bug fixes
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unum.h"

#include "unicode/uloc.h"
#include "unicode/numfmt.h"
#include "unicode/decimfmt.h"
#include "unicode/rbnf.h"
#include "unicode/ustring.h"
#include "unicode/fmtable.h"
#include "unicode/dcfmtsym.h"
#include "cpputils.h"
U_NAMESPACE_USE
/*
U_CAPI UNumberFormat*
unum_open(    UNumberFormatStyle    style,
        const   char*        locale,
        UErrorCode*        status)
{
  if(U_FAILURE(*status))
    return 0;
  UNumberFormat *retVal = 0;
  
  switch(style) {
  case UNUM_DECIMAL:
    if(locale == 0)
      retVal = (UNumberFormat*)NumberFormat::createInstance(*status);
    else
      retVal = (UNumberFormat*)NumberFormat::createInstance(Locale(locale),
                                *status);
    break;

  case UNUM_CURRENCY:
    if(locale == 0)
      retVal = (UNumberFormat*)NumberFormat::createCurrencyInstance(*status);
    else
      retVal = (UNumberFormat*)NumberFormat::createCurrencyInstance(Locale(locale),
                                    *status);
    break;

  case UNUM_PERCENT:
    if(locale == 0)
      retVal = (UNumberFormat*)NumberFormat::createPercentInstance(*status);
    else
      retVal = (UNumberFormat*)NumberFormat::createPercentInstance(Locale(locale),
                                *status);
    break;

  case UNUM_SPELLOUT:
    // Todo: TBD: Add spellout support
    //retVal = (UNumberFormat*)new NumberSpelloutFormat();
    //break;
    *status = U_UNSUPPORTED_ERROR;
    return 0;

  default:
    *status = U_UNSUPPORTED_ERROR;
    return 0;
  }

  if(retVal == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return retVal;
}

U_CAPI UNumberFormat*
unum_openPattern(    const    UChar*            pattern,
            int32_t            patternLength,
            const    char*        locale,
            UErrorCode*        status)
{
    UParseError parseError;
    return unum_openPatternWithError( pattern,patternLength,locale,&parseError,status);
}*/


U_CAPI UNumberFormat* U_EXPORT2
unum_open(  UNumberFormatStyle    style,  
            const    UChar*    pattern,
            int32_t            patternLength,
            const    char*     locale,
            UParseError*       parseErr,
            UErrorCode*        status)
{

  if(U_FAILURE(*status))
  { 
      return 0;
  }
  if(style!=UNUM_IGNORE){
       UNumberFormat *retVal = 0;
  
      switch(style) {
      case UNUM_DECIMAL:
        if(locale == 0)
          retVal = (UNumberFormat*)NumberFormat::createInstance(*status);
        else
          retVal = (UNumberFormat*)NumberFormat::createInstance(Locale(locale),
                                    *status);
        break;

      case UNUM_CURRENCY:
        if(locale == 0)
          retVal = (UNumberFormat*)NumberFormat::createCurrencyInstance(*status);
        else
          retVal = (UNumberFormat*)NumberFormat::createCurrencyInstance(Locale(locale),
                                        *status);
        break;

      case UNUM_PERCENT:
        if(locale == 0)
          retVal = (UNumberFormat*)NumberFormat::createPercentInstance(*status);
        else
          retVal = (UNumberFormat*)NumberFormat::createPercentInstance(Locale(locale),
                                    *status);
        break;

      case UNUM_SCIENTIFIC:
        if(locale == 0)
          retVal = (UNumberFormat*)NumberFormat::createScientificInstance(*status);
        else
          retVal = (UNumberFormat*)NumberFormat::createScientificInstance(Locale(locale),
                                    *status);
        break;

      case UNUM_SPELLOUT:
#if U_HAVE_RBNF
        return (UNumberFormat*)new RuleBasedNumberFormat(URBNF_SPELLOUT, Locale(locale), *status);
#else
        // fall through
#endif
      default:
        *status = U_UNSUPPORTED_ERROR;
        return 0;
      }

      if(retVal == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
      }

      return retVal;
  }else{
      /* we don't support RBNF patterns yet */
      UParseError tErr;
      int32_t len = (patternLength == -1 ? u_strlen(pattern) : patternLength);
      const UnicodeString pat((UChar*)pattern, len, len);
      DecimalFormatSymbols *syms = 0;
      
      if(parseErr==NULL){
          parseErr = &tErr;
      }
      
      if(locale == 0)
        syms = new DecimalFormatSymbols(*status);
      else
        syms = new DecimalFormatSymbols(Locale(locale),
                        *status);
  
      if(syms == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
      }

      DecimalFormat *fmt = 0;
      fmt = new DecimalFormat(pat, syms, *parseErr, *status);
      if(fmt == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        delete syms;
        return 0;
      }

      return (UNumberFormat*) fmt;
  }
}

U_CAPI void U_EXPORT2
unum_close(UNumberFormat* fmt)
{
 
  delete (NumberFormat*) fmt;
}

U_CAPI UNumberFormat* U_EXPORT2
unum_clone(const UNumberFormat *fmt,
       UErrorCode *status)
{
 
  if(U_FAILURE(*status)) return 0;

  Format *res = ((DecimalFormat*)fmt)->clone();
  
  if(res == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UNumberFormat*) res;
}

U_CAPI int32_t U_EXPORT2
unum_format(    const    UNumberFormat*    fmt,
        int32_t            number,
        UChar*            result,
        int32_t            resultLength,
        UFieldPosition    *pos,
        UErrorCode*        status)
{
 
  if(U_FAILURE(*status)) return -1;

  UnicodeString res;
  if(!(result==NULL && resultLength==0)) {
    // NULL destination for pure preflighting: empty dummy string
    // otherwise, alias the destination buffer
    res.setTo(result, 0, resultLength);
  }

  FieldPosition fp;
  
  if(pos != 0)
    fp.setField(pos->field);
  
  ((NumberFormat*)fmt)->format(number, res, fp);
  
  if(pos != 0) {
    pos->beginIndex = fp.getBeginIndex();
    pos->endIndex = fp.getEndIndex();
  }
  
  return res.extract(result, resultLength, *status);
}

U_CAPI int32_t U_EXPORT2
unum_formatDouble(    const    UNumberFormat*  fmt,
            double          number,
            UChar*          result,
            int32_t         resultLength,
            UFieldPosition  *pos, /* 0 if ignore */
            UErrorCode*     status)
{
 
  if(U_FAILURE(*status)) return -1;

  UnicodeString res;
  if(!(result==NULL && resultLength==0)) {
    // NULL destination for pure preflighting: empty dummy string
    // otherwise, alias the destination buffer
    res.setTo(result, 0, resultLength);
  }

  FieldPosition fp;
  
  if(pos != 0)
    fp.setField(pos->field);
  
  ((NumberFormat*)fmt)->format(number, res, fp);
  
  if(pos != 0) {
    pos->beginIndex = fp.getBeginIndex();
    pos->endIndex = fp.getEndIndex();
  }
  
  return res.extract(result, resultLength, *status);
}

U_CAPI int32_t U_EXPORT2
unum_parse(    const   UNumberFormat*  fmt,
        const   UChar*          text,
        int32_t         textLength,
        int32_t         *parsePos /* 0 = start */,
        UErrorCode      *status)
{
 
  if(U_FAILURE(*status)) return 0;

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

U_CAPI double U_EXPORT2
unum_parseDouble(    const   UNumberFormat*  fmt,
            const   UChar*          text,
            int32_t         textLength,
            int32_t         *parsePos /* 0 = start */,
            UErrorCode      *status)
{
 
  if(U_FAILURE(*status))
      return 0;

  int32_t len = (textLength < 0 ? u_strlen(text) : textLength);
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

U_CAPI const char* U_EXPORT2
unum_getAvailable(int32_t index)
{
 
  return uloc_getAvailable(index);
}

U_CAPI int32_t U_EXPORT2
unum_countAvailable()
{
 
  return uloc_countAvailable();
}

U_CAPI int32_t U_EXPORT2
unum_getAttribute(const UNumberFormat*          fmt,
          UNumberFormatAttribute  attr)
{
 
  switch(attr) {
  case UNUM_PARSE_INT_ONLY:
    return ((NumberFormat*)fmt)->isParseIntegerOnly();
    
  case UNUM_GROUPING_USED:
    return ((NumberFormat*)fmt)->isGroupingUsed();
    
  case UNUM_DECIMAL_ALWAYS_SHOWN:
    return ((DecimalFormat*)fmt)->isDecimalSeparatorAlwaysShown();    
    
  case UNUM_MAX_INTEGER_DIGITS:
    return ((NumberFormat*)fmt)->getMaximumIntegerDigits();
    
  case UNUM_MIN_INTEGER_DIGITS:
    return ((NumberFormat*)fmt)->getMinimumIntegerDigits();
    
  case UNUM_INTEGER_DIGITS:
    // TBD: what should this return?
    return ((NumberFormat*)fmt)->getMinimumIntegerDigits();
    
  case UNUM_MAX_FRACTION_DIGITS:
    return ((NumberFormat*)fmt)->getMaximumFractionDigits();
    
  case UNUM_MIN_FRACTION_DIGITS:
    return ((NumberFormat*)fmt)->getMinimumFractionDigits();
    
  case UNUM_FRACTION_DIGITS:
    // TBD: what should this return?
    return ((NumberFormat*)fmt)->getMinimumFractionDigits();

  case UNUM_MULTIPLIER:
    return ((DecimalFormat*)fmt)->getMultiplier();    
    
  case UNUM_GROUPING_SIZE:
    return ((DecimalFormat*)fmt)->getGroupingSize();    

  case UNUM_ROUNDING_MODE:
    return ((DecimalFormat*)fmt)->getRoundingMode();

  case UNUM_FORMAT_WIDTH:
    return ((DecimalFormat*)fmt)->getFormatWidth();

  /** The position at which padding will take place. */
  case UNUM_PADDING_POSITION:
    return ((DecimalFormat*)fmt)->getPadPosition();

  default:
    break;
  }
  return -1;
}

U_CAPI void U_EXPORT2
unum_setAttribute(    UNumberFormat*          fmt,
            UNumberFormatAttribute  attr,
            int32_t                 newValue)
{
 
  switch(attr) {
  case UNUM_PARSE_INT_ONLY:
    ((NumberFormat*)fmt)->setParseIntegerOnly((UBool)newValue);
    break;

  case UNUM_GROUPING_USED:
    ((NumberFormat*)fmt)->setGroupingUsed((UBool)newValue);
    break;

  case UNUM_DECIMAL_ALWAYS_SHOWN:
    ((DecimalFormat*)fmt)->setDecimalSeparatorAlwaysShown((UBool)newValue);
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

  case UNUM_SECONDARY_GROUPING_SIZE:
      ((DecimalFormat*)fmt)->setSecondaryGroupingSize(newValue);
    break;

  default:
    /* Shouldn't get here anyway */
    break;
  }
}

U_CAPI double U_EXPORT2
unum_getDoubleAttribute(const UNumberFormat*          fmt,
          UNumberFormatAttribute  attr)
{
 
  if (attr == UNUM_ROUNDING_INCREMENT) {
    return ((DecimalFormat*)fmt)->getRoundingIncrement();
  } else {
    return -1.0;
  }
}

U_CAPI void U_EXPORT2
unum_setDoubleAttribute(    UNumberFormat*          fmt,
            UNumberFormatAttribute  attr,
            double                 newValue)
{
 
  if (attr == UNUM_ROUNDING_INCREMENT) {   
    ((DecimalFormat*)fmt)->setRoundingIncrement(newValue);
  }
}

U_CAPI int32_t U_EXPORT2
unum_getTextAttribute(const UNumberFormat*  fmt,
            UNumberFormatTextAttribute      tag,
            UChar*                          result,
            int32_t                         resultLength,
            UErrorCode*                     status)
{
 
  if(U_FAILURE(*status))
      return -1;

  UnicodeString res;
  if(!(result==NULL && resultLength==0)) {
    // NULL destination for pure preflighting: empty dummy string
    // otherwise, alias the destination buffer
    res.setTo(result, 0, resultLength);
  }

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
    res = ((DecimalFormat*)fmt)->getPadCharacterString();
    break;

  case UNUM_CURRENCY_CODE:
    res = UnicodeString(((DecimalFormat*)fmt)->getCurrency());
    break;

  default:
    *status = U_UNSUPPORTED_ERROR;
    return -1;
  }

  return res.extract(result, resultLength, *status);
}

U_CAPI void U_EXPORT2
unum_setTextAttribute(    UNumberFormat*                    fmt,
            UNumberFormatTextAttribute      tag,
            const    UChar*                            newValue,
            int32_t                            newValueLength,
            UErrorCode                        *status)
{
 
  if(U_FAILURE(*status)) return;

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

  case UNUM_CURRENCY_CODE:
      ((DecimalFormat*)fmt)->setCurrency(newValue);
      break;

  default:
    *status = U_UNSUPPORTED_ERROR;
    break;
  }
}

U_CAPI int32_t U_EXPORT2
unum_toPattern(    const    UNumberFormat*          fmt,
        UBool                  isPatternLocalized,
        UChar*                  result,
        int32_t                 resultLength,
        UErrorCode*             status)
{
 
  if(U_FAILURE(*status)) return -1;

  UnicodeString pat;
  if(!(result==NULL && resultLength==0)) {
    // NULL destination for pure preflighting: empty dummy string
    // otherwise, alias the destination buffer
    pat.setTo(result, 0, resultLength);
  }

  if(isPatternLocalized)
    ((DecimalFormat*)fmt)->toLocalizedPattern(pat);
  else
    ((DecimalFormat*)fmt)->toPattern(pat);

  return pat.extract(result, resultLength, *status);
}

U_CAPI int32_t U_EXPORT2
unum_getSymbol(UNumberFormat *fmt,
               UNumberFormatSymbol symbol,
               UChar *buffer,
               int32_t size,
               UErrorCode *status) {
 
  if(status==NULL || U_FAILURE(*status)) {
    return 0;
  }

  if(fmt==NULL || (uint16_t)symbol>=UNUM_FORMAT_SYMBOL_COUNT) {
    *status=U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  return
    ((const DecimalFormat *)fmt)->
      getDecimalFormatSymbols()->
        getConstSymbol((DecimalFormatSymbols::ENumberFormatSymbol)symbol).
          extract(buffer, size, *status);
}

U_CAPI void U_EXPORT2
unum_setSymbol(UNumberFormat *fmt,
               UNumberFormatSymbol symbol,
               const UChar *value,
               int32_t length,
               UErrorCode *status) {
 
  if(status==NULL || U_FAILURE(*status)) {
    return;
  }

  if(fmt==NULL || (uint16_t)symbol>=UNUM_FORMAT_SYMBOL_COUNT || value==NULL || length<-1) {
    *status=U_ILLEGAL_ARGUMENT_ERROR;
    return;
  }

  DecimalFormatSymbols symbols(*((DecimalFormat *)fmt)->getDecimalFormatSymbols());
  symbols.setSymbol((DecimalFormatSymbols::ENumberFormatSymbol)symbol,
        length>=0 ? UnicodeString(value, length) : UnicodeString(value));
  ((DecimalFormat *)fmt)->setDecimalFormatSymbols(symbols);
}

/*
U_CAPI void
unum_applyPattern(            UNumberFormat     *format,
                    UBool          localized,
                    const   UChar           *pattern,
                    int32_t         patternLength
                    )
{
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    unum_applyPatternWithError(format,localized,pattern,patternLength,&parseError,&status);
}
*/

U_CAPI void U_EXPORT2
unum_applyPattern(  UNumberFormat  *format,
                    UBool          localized,
                    const UChar    *pattern,
                    int32_t        patternLength,
                    UParseError    *parseError,
                    UErrorCode*    status)
{
 
  UErrorCode tStatus = U_ZERO_ERROR;
  UParseError tParseError;
  
  if(parseError == NULL){
      parseError = &tParseError;
  }
  
  if(status==NULL){
      status = &tStatus;
  }

  int32_t len = (patternLength == -1 ? u_strlen(pattern) : patternLength);
  const UnicodeString pat((UChar*)pattern, len, len);

  // Verify if the object passed is a DecimalFormat object
  if(((NumberFormat*)format)->getDynamicClassID()!= DecimalFormat::getStaticClassID()){
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return;
  }
  
  if(localized)
    ((DecimalFormat*)format)->applyLocalizedPattern(pat,*parseError, *status);
  else
    ((DecimalFormat*)format)->applyPattern(pat,*parseError, *status);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
