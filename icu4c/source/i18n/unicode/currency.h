/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/unicode/Attic/currency.h,v $ 
* $Revision: 1.4 $
**********************************************************************
*/
#ifndef UCURRENCY_H
#define UCURRENCY_H

#include "unicode/utypes.h"

/**
 * The ucurr API encapsulates information about a currency, as defined by
 * ISO 4217.  A currency is represented by a 3-character string
 * containing its ISO 4217 code.  This API can return various data
 * necessary the proper display of a currency:
 *
 * <ul><li>A display symbol, for a specific locale
 * <li>The number of fraction digits to display
 * <li>A rounding increment
 * </ul>
 *
 * The <tt>DecimalFormat</tt> class uses these data to display
 * currencies.
 * @author Alan Liu
 * @since ICU 2.2
 */

/**
 * Returns a currency code for the default currency in the given
 * locale.
 * @param locale the locale for which to retrieve a currency code
 * @param result pointer to a buffer of at least 4 chars
 * to receive a null-terminated 3-letter ISO 4217 code
 * @param ec error code
 */
U_CAPI void U_EXPORT2
ucurr_forLocale(const char* locale,
                char*       result,
                UErrorCode* ec);

/**
 * Returns the display string for the given currency in the
 * given locale.  For example, the display string for the USD
 * currency object in the en_US locale is "$".
 * @param currency null-terminated 3-letter ISO 4217 code
 * @param result pointer to result buffer, or NULL
 * @param resultCapacity size of result buffer
 * @param locale locale in which to display currency
 * @param ec error code
 * @return size of result, possibly larger than resultCapacity
 */
U_CAPI int32_t U_EXPORT2
ucurr_getSymbol(const char* currency,
                UChar*      result,
                int32_t     resultCapacity,
                const char* locale,
                UErrorCode* ec);

/**
 * Returns the number of the number of fraction digits that should
 * be displayed for the given currency.
 * @return a non-negative number of fraction digits to be
 * displayed
 */
U_CAPI int32_t U_EXPORT2
ucurr_getDefaultFractionDigits(const char* currency);

/**
 * Returns the rounding increment for the given currency, or 0.0 if no
 * rounding is done by the currency.
 * @return the non-negative rounding increment, or 0.0 if none
 */
U_CAPI double U_EXPORT2
ucurr_getRoundingIncrement(const char* currency);


/*------------------------------------------------------------------*/
/* Internal API.  Do not use.  Will change without notice.          */

#ifdef XP_CPLUSPLUS

  #include "unicode/unistr.h"

  U_NAMESPACE_BEGIN

  class Locale;

  /**
   * Do not use; for ICU internal use only.  This is a C++ function
   * even though it's named like a C function.
   *
   * @internal
   */
  UnicodeString ucurr_getSymbolAsUnicodeString(const char* currency,
                                               const Locale& locale);

  U_NAMESPACE_END

#endif

#endif
