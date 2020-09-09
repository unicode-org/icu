// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef _CURRENCYDISPLAYNAMES_H_
#define _CURRENCYDISPLAYNAMES_H_

#include "unicode/uobject.h"
#include "unicode/ucurr.h"
#include "unicode/locid.h"
#include "unicode/ures.h"

U_NAMESPACE_BEGIN

struct CurrencyDisplayNamesDataSink;

class CurrencyDisplayNames : public UMemory{
    friend struct CurrencyDisplayNamesDataSink;
    friend struct FormattingData;
    friend struct VariantSymbol;
public:

#ifndef U_HIDE_DRAFT_API

    /**
     * Return an instance of CurrencyDisplayNames that provides information
     * localized for display in the provided locale.  If there is no data
     * for the provided locale, this falls back to the current default
     * locale; if there is no data for that either, it falls back to the root
     * locale. Substitute values are returned from APIs when there is no data
     * for the requested ISO code.
     *
     * @param locale    The locale to get symbols for.
     * @param status    Input/output parameter, set to success or
     *                  failure code upon return.
     * @draft ICU 6X
     */
    static const CurrencyDisplayNames *getInstance(Locale* locale, UErrorCode& status);

    /**
     * Return an instance of CurrencyDisplayNames that provides information
     * localized for display in the provided locale. If noSubstitute is
     * false, this behaves like getInstance(ULocale). Otherwise, 1) if
     * there is no supporting data for the locale at all, there is no
     * fallback through the default locale or root, and null is returned,
     * and 2) if there is data for the locale, but not data for the
     * requested ISO code, null is returned from those APIs instead of a
     * substitute value.
     *
     * @param locale        The locale to get symbols for.
     * @param noSubstitute  If true, do not return substitute values
     * @param status        Input/output parameter, set to success or
     *                      failure code upon return.
     * @draft ICU 6X
     */
    static const CurrencyDisplayNames *getInstance(Locale* locale, UBool noSubstitute, UErrorCode& status);

    /**
     * Returns the name or symbol for the given currency corresponding to nameStyle.
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param nameStyle selector for the type of name to return
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * @draft ICU 6X
     */
    const UChar *getName(const UChar* isoCode,
        UCurrNameStyle nameStyle,
        UErrorCode& status) const;

    /**
     * Returns the 'long name' for the currency with the provided ISO code.
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * @draft ICU 6X
     */
    const UChar *getName(const UChar *isoCode, UErrorCode &status) const;

    /**
     * Returns the symbol for the currency with the provided ISO code.
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * @draft ICU 6X
     */
    const UChar *getSymbol(const UChar *isoCode, UErrorCode &status) const;

    /**
     * Returns the narrow symbol for the currency with the provided ISO code.
     * The narrow currency symbol is similar to the regular currency symbol,
     * but it always takes the shortest form;
     * for example, "$" instead of "US$" for USD in en-CA.
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * @draft ICU 6X
     */
    const UChar *getNarrowSymbol(const UChar *isoCode, UErrorCode &status) const;

    /**
     * Returns the formal symbol for the currency with the provided ISO code.
     * The formal currency symbol is similar to the regular currency symbol,
     * but it always takes the form used in formal settings such as banking;
     * for example, "NT$" instead of "$" for TWD in zh-TW.
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * @draft ICU 6X
     */
    const UChar *getFormalSymbol(const UChar *isoCode, UErrorCode &status) const;

    /**
     * Returns the variant symbol for the currency with the provided ISO code.
     * The variant symbol for a currency is an alternative symbol that is not
     * necessarily as widely used as the regular symbol.
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * @draft ICU 6X
     */
    const UChar *getVariantSymbol(const UChar *isoCode, UErrorCode &status) const;

    /**
     * Returns the plural name for the currency with the provided ISO code.
     * For example, the plural name for the USD currency object in the
     * en_US locale is "US dollar" or "US dollars".
     * @param currency null-terminated 3-letter ISO 4217 code
     * @param status error code
     * @return UChar. If there is no data for the ISO code, substitutes
     * isoCode, or returns null if noSubstitute was set when getting the instance.
     * If there is data for the ISO code but no data for the plural key,
     * substitutes the 'other' value (and failing that the isoCode) or returns null.
     * @draft ICU 6X
     */
    const UChar *getPluralName(const UChar *isoCode,
        const char* pluralCount, UErrorCode &status) const;

    /**
     * Returns the locale used to determine how to translate the currency names.
     * This is not necessarily the same locale passed to getInstance.
     * @return the display locale
     * @draft ICU 6X
     */
    Locale *getLocale();



private:
    CurrencyDisplayNames(Locale* locale, UBool noSubstitute);
    ~CurrencyDisplayNames();
    Locale* locale;
    const UBool noSubstitute;
    //FormattingData formattingDataCache;
    //VariantSymbol variantSymbolCache;

    FormattingData* fetchFormattingData(const UChar* isoCode, UErrorCode& errorCode) const;
    VariantSymbol* fetchVariantSymbol(const UChar* isoCode, const char* variant, UErrorCode& errorCode) const;
};


#endif  // U_HIDE_DRAFT_API
U_NAMESPACE_END

#endif //_CURRENCYDISPLAYNAMES_H_
