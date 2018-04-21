// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMBER_UTILS_H__
#define __NUMBER_UTILS_H__

#include "unicode/numberformatter.h"
#include "number_types.h"
#include "number_decimalquantity.h"
#include "number_scientific.h"
#include "number_patternstring.h"
#include "number_modifiers.h"
#include "number_multiplier.h"
#include "decNumber.h"
#include "charstr.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {

struct MicroProps : public MicroPropsGenerator {

    // NOTE: All of these fields are properly initialized in NumberFormatterImpl.
    Rounder rounding;
    Grouper grouping;
    Padder padding;
    IntegerWidth integerWidth;
    UNumberSignDisplay sign;
    UNumberDecimalSeparatorDisplay decimal;
    bool useCurrency;

    // Note: This struct has no direct ownership of the following pointers.
    const DecimalFormatSymbols* symbols;
    const Modifier* modOuter;
    const Modifier* modMiddle;
    const Modifier* modInner;

    // The following "helper" fields may optionally be used during the MicroPropsGenerator.
    // They live here to retain memory.
    struct {
        ScientificModifier scientificModifier;
        EmptyModifier emptyWeakModifier{false};
        EmptyModifier emptyStrongModifier{true};
        MultiplierFormatHandler multiplier;
    } helpers;


    MicroProps() = default;

    MicroProps(const MicroProps& other) = default;

    MicroProps& operator=(const MicroProps& other) = default;

    void processQuantity(DecimalQuantity&, MicroProps& micros, UErrorCode& status) const U_OVERRIDE {
        (void) status;
        if (this == &micros) {
            // Unsafe path: no need to perform a copy.
            U_ASSERT(!exhausted);
            micros.exhausted = true;
            U_ASSERT(exhausted);
        } else {
            // Safe path: copy self into the output micros.
            micros = *this;
        }
    }

  private:
    // Internal fields:
    bool exhausted = false;
};

enum CldrPatternStyle {
    CLDR_PATTERN_STYLE_DECIMAL,
    CLDR_PATTERN_STYLE_CURRENCY,
    CLDR_PATTERN_STYLE_ACCOUNTING,
    CLDR_PATTERN_STYLE_PERCENT,
    CLDR_PATTERN_STYLE_SCIENTIFIC,
    CLDR_PATTERN_STYLE_COUNT,
};

// Namespace for naked functions
namespace utils {

inline int32_t insertDigitFromSymbols(NumberStringBuilder& output, int32_t index, int8_t digit,
                                      const DecimalFormatSymbols& symbols, Field field,
                                      UErrorCode& status) {
    if (symbols.getCodePointZero() != -1) {
        return output.insertCodePoint(index, symbols.getCodePointZero() + digit, field, status);
    }
    return output.insert(index, symbols.getConstDigitSymbol(digit), field, status);
}

inline bool unitIsCurrency(const MeasureUnit& unit) {
    return uprv_strcmp("currency", unit.getType()) == 0;
}

inline bool unitIsNoUnit(const MeasureUnit& unit) {
    return uprv_strcmp("none", unit.getType()) == 0;
}

inline bool unitIsPercent(const MeasureUnit& unit) {
    return uprv_strcmp("percent", unit.getSubtype()) == 0;
}

inline bool unitIsPermille(const MeasureUnit& unit) {
    return uprv_strcmp("permille", unit.getSubtype()) == 0;
}

// NOTE: In Java, this method is in NumberFormat.java
const char16_t*
getPatternForStyle(const Locale& locale, const char* nsName, CldrPatternStyle style, UErrorCode& status);

} // namespace utils


/** A very thin C++ wrapper around decNumber.h */
class DecNum : public UMemory {
  public:
    DecNum();  // leaves object in valid but undefined state

    // Copy-like constructor; use the default move operators.
    DecNum(const DecNum& other, UErrorCode& status);

    /** Sets the decNumber to the StringPiece. */
    void setTo(StringPiece str, UErrorCode& status);

    /** Sets the decNumber to the NUL-terminated char string. */
    void setTo(const char* str, UErrorCode& status);

    /** Uses double_conversion to set this decNumber to the given double. */
    void setTo(double d, UErrorCode& status);

    /** Sets the decNumber to the BCD representation. */
    void setTo(const uint8_t* bcd, int32_t length, int32_t scale, bool isNegative, UErrorCode& status);

    void normalize();

    void multiplyBy(const DecNum& rhs, UErrorCode& status);

    void divideBy(const DecNum& rhs, UErrorCode& status);

    bool isNegative() const;

    bool isZero() const;

    inline const decNumber* getRawDecNumber() const {
        return fData.getAlias();
    }

  private:
    static constexpr int32_t kDefaultDigits = 34;
    MaybeStackHeaderAndArray<decNumber, char, kDefaultDigits> fData;
    decContext fContext;

    void _setTo(const char* str, int32_t maxDigits, UErrorCode& status);
};


} // namespace impl
} // namespace number
U_NAMESPACE_END

#endif //__NUMBER_UTILS_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
