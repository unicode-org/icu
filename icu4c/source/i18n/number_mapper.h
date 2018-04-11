// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMBER_MAPPER_H__
#define __NUMBER_MAPPER_H__

#include "number_types.h"
#include "unicode/currpinf.h"
#include "standardplural.h"
#include "number_patternstring.h"
#include "number_currencysymbols.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


class PropertiesAffixPatternProvider : public AffixPatternProvider, public UMemory {
  public:
    bool isBogus() const {
        return fBogus;
    }

    void setToBogus() {
        fBogus = true;
    }

    void setTo(const DecimalFormatProperties& properties, UErrorCode& status);

    PropertiesAffixPatternProvider() = default; // puts instance in valid but undefined state

    PropertiesAffixPatternProvider(const DecimalFormatProperties& properties, UErrorCode& status) {
        setTo(properties, status);
    }

    // AffixPatternProvider Methods:

    char16_t charAt(int32_t flags, int32_t i) const U_OVERRIDE;

    int32_t length(int32_t flags) const U_OVERRIDE;

    UnicodeString getString(int32_t flags) const U_OVERRIDE;

    bool hasCurrencySign() const U_OVERRIDE;

    bool positiveHasPlusSign() const U_OVERRIDE;

    bool hasNegativeSubpattern() const U_OVERRIDE;

    bool negativeHasMinusSign() const U_OVERRIDE;

    bool containsSymbolType(AffixPatternType, UErrorCode&) const U_OVERRIDE;

    bool hasBody() const U_OVERRIDE;

  private:
    UnicodeString posPrefix;
    UnicodeString posSuffix;
    UnicodeString negPrefix;
    UnicodeString negSuffix;

    const UnicodeString& getStringInternal(int32_t flags) const;

    bool fBogus{true};
};


class CurrencyPluralInfoAffixProvider : public AffixPatternProvider, public UMemory {
  public:
    bool isBogus() const {
        return fBogus;
    }

    void setToBogus() {
        fBogus = true;
    }

    void setTo(const CurrencyPluralInfo& cpi, const DecimalFormatProperties& properties,
               UErrorCode& status);

    // AffixPatternProvider Methods:

    char16_t charAt(int32_t flags, int32_t i) const U_OVERRIDE;

    int32_t length(int32_t flags) const U_OVERRIDE;

    UnicodeString getString(int32_t flags) const U_OVERRIDE;

    bool hasCurrencySign() const U_OVERRIDE;

    bool positiveHasPlusSign() const U_OVERRIDE;

    bool hasNegativeSubpattern() const U_OVERRIDE;

    bool negativeHasMinusSign() const U_OVERRIDE;

    bool containsSymbolType(AffixPatternType, UErrorCode&) const U_OVERRIDE;

    bool hasBody() const U_OVERRIDE;

  private:
    PropertiesAffixPatternProvider affixesByPlural[StandardPlural::COUNT];

    bool fBogus{true};
};


/**
 * A struct for ownership of a few objects needed for formatting.
 */
struct DecimalFormatWarehouse {
    PropertiesAffixPatternProvider propertiesAPP;
    CurrencyPluralInfoAffixProvider currencyPluralInfoAPP;
    CurrencySymbols currencySymbols;
};


/**
 * Utilities for converting between a DecimalFormatProperties and a MacroProps.
 */
class NumberPropertyMapper {
  public:
    /** Convenience method to create a NumberFormatter directly from Properties. */
    static UnlocalizedNumberFormatter create(const DecimalFormatProperties& properties,
                                             const DecimalFormatSymbols& symbols,
                                             DecimalFormatWarehouse& warehouse, UErrorCode& status);

    /** Convenience method to create a NumberFormatter directly from Properties. */
    static UnlocalizedNumberFormatter create(const DecimalFormatProperties& properties,
                                             const DecimalFormatSymbols& symbols,
                                             DecimalFormatWarehouse& warehouse,
                                             DecimalFormatProperties& exportedProperties,
                                             UErrorCode& status);

    /**
     * Creates a new {@link MacroProps} object based on the content of a {@link DecimalFormatProperties}
     * object. In other words, maps Properties to MacroProps. This function is used by the
     * JDK-compatibility API to call into the ICU 60 fluent number formatting pipeline.
     *
     * @param properties
     *            The property bag to be mapped.
     * @param symbols
     *            The symbols associated with the property bag.
     * @param exportedProperties
     *            A property bag in which to store validated properties. Used by some DecimalFormat
     *            getters.
     * @return A new MacroProps containing all of the information in the Properties.
     */
    static MacroProps oldToNew(const DecimalFormatProperties& properties,
                               const DecimalFormatSymbols& symbols, DecimalFormatWarehouse& warehouse,
                               DecimalFormatProperties* exportedProperties, UErrorCode& status);
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMBER_MAPPER_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
