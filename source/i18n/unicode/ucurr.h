/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#ifndef _UCURR_H_
#define _UCURR_H_

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

/**
 * @internal
 */
typedef const void* UCurrRegistryKey;

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
 * Finds a currency code for the given locale.
 * @param locale the locale for which to retrieve a currency code. 
 *               Currency can be specified by the "currency" keyword
 *               in which case it overrides the default currency code
 * @param buff   fill in buffer. Can be NULL for preflighting.
 * @param buffCapacity capacity of the fill in buffer. Can be 0 for
 *               preflighting. If it is non-zero, the buff parameter
 *               must not be NULL.
 * @param ec error code
 * @return length of the currency string. It should always be 3. If 0,
 *                currency couldn't be found or the input values are 
 *                invalid. 
 * @draft ICU 2.8
 */
U_DRAFT int32_t U_EXPORT2
ucurr_forLocale(const char* locale,
                UChar* buff,
                int32_t buffCapacity,
                UErrorCode* ec);

/**
 * Selector constants for ucurr_getName().
 *
 * @see ucurr_getName
 * @draft ICU 2.6
 */
typedef enum UCurrNameStyle {
    /**
     * Selector for ucurr_getName indicating a symbolic name for a
     * currency, such as "$" for USD.
     * @draft ICU 2.6
     */
    UCURR_SYMBOL_NAME,

    /**
     * Selector for ucurr_getName indicating the long name for a
     * currency, such as "US Dollar" for USD.
     * @draft ICU 2.6
     */
    UCURR_LONG_NAME
} UCurrNameStyle;

/**
 * Register an (existing) ISO 4217 currency code for the given locale.
 * Only the country code and the two variants EURO and PRE_EURO are
 * recognized.
 * @param isoCode the three-letter ISO 4217 currency code
 * @param locale  the locale for which to register this currency code
 * @param status the in/out status code
 * @return a registry key that can be used to unregister this currency code, or NULL
 * if there was an error.
 * @draft ICU 2.6
 */
U_DRAFT UCurrRegistryKey U_EXPORT2
    ucurr_register(const UChar* isoCode, 
                   const char* locale,  
                   UErrorCode* status);
/**
 * Unregister the previously-registered currency definitions using the
 * URegistryKey returned from ucurr_register.  Key becomes invalid after
 * a successful call and should not be used again.  Any currency 
 * that might have been hidden by the original ucurr_register call is 
 * restored.
 * @param key the registry key returned by a previous call to ucurr_register
 * @param status the in/out status code, no special meanings are assigned
 * @return TRUE if the currency for this key was successfully unregistered
 * @draft ICU 2.6
 */
U_DRAFT UBool U_EXPORT2
    ucurr_unregister(UCurrRegistryKey key, UErrorCode* status);

/**
 * Returns the display name for the given currency in the
 * given locale.  For example, the display name for the USD
 * currency object in the en_US locale is "$".
 * @param currency null-terminated 3-letter ISO 4217 code
 * @param locale locale in which to display currency
 * @param nameStyle selector for which kind of name to return
 * @param isChoiceFormat fill-in set to TRUE if the returned value
 * is a ChoiceFormat pattern; otherwise it is a static string
 * @param len fill-in parameter to receive length of result
 * @param ec error code
 * @return pointer to display string of 'len' UChars.  If the resource
 * data contains no entry for 'currency', then 'currency' itself is
 * returned.  If *isChoiceFormat is TRUE, then the result is a
 * ChoiceFormat pattern.  Otherwise it is a static string.
 * @draft ICU 2.6
 */
U_DRAFT const UChar* U_EXPORT2
ucurr_getName(const UChar* currency,
              const char* locale,
              UCurrNameStyle nameStyle,
              UBool* isChoiceFormat,
              int32_t* len,
              UErrorCode* ec);

/**
 * Returns the number of the number of fraction digits that should
 * be displayed for the given currency.
 * @param currency null-terminated 3-letter ISO 4217 code
 * @param ec input-output error code
 * @return a non-negative number of fraction digits to be
 * displayed, or 0 if there is an error
 * @draft ICU 3.0
 */
U_DRAFT int32_t U_EXPORT2
ucurr_getDefaultFractionDigits(const UChar* currency,
                               UErrorCode* ec);

/**
 * Returns the rounding increment for the given currency, or 0.0 if no
 * rounding is done by the currency.
 * @param currency null-terminated 3-letter ISO 4217 code
 * @param ec input-output error code
 * @return the non-negative rounding increment, or 0.0 if none,
 * or 0.0 if there is an error
 * @draft ICU 3.0
 */
U_DRAFT double U_EXPORT2
ucurr_getRoundingIncrement(const UChar* currency,
                           UErrorCode* ec);

#ifdef XP_CPLUSPLUS
#include "unicode/unistr.h"
#include "unicode/parsepos.h"
U_NAMESPACE_BEGIN

/**
 * Attempt to parse the given string as a currency, either as a
 * display name in the given locale, or as a 3-letter ISO 4217
 * code.  If multiple display names match, then the longest one is
 * selected.  If both a display name and a 3-letter ISO code
 * match, then the display name is preferred, unless it's length
 * is less than 3.
 *
 * @param locale the locale of the display names to match
 * @param text the text to parse
 * @param pos input-output position; on input, the position within
 * text to match; must have 0 <= pos.getIndex() < text.length();
 * on output, the position after the last matched character. If
 * the parse fails, the position in unchanged upon output.
 * @return the ISO 4217 code, as a string, of the best match, or
 * null if there is no match
 *
 * @internal
 */
void
uprv_parseCurrency(const char* locale,
                   const UnicodeString& text,
                   ParsePosition& pos,
                   UChar* result,
                   UErrorCode& ec);

U_NAMESPACE_END
#endif

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
