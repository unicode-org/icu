// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __NUMBER_UTILS_H__
#define __NUMBER_UTILS_H__

#include "unicode/numberformatter.h"
#include "number_types.h"
#include "number_decimalquantity.h"
#include "number_scientific.h"
#include "number_patternstring.h"
#include "number_modifiers.h"
#include "number_multiplier.h"
#include "number_roundingutils.h"
#include "decNumber.h"
#include "charstr.h"

U_NAMESPACE_BEGIN

#define DECNUM_INITIAL_CAPACITY 34

// Export an explicit template instantiation of the MaybeStackHeaderAndArray that is used as a data member of DecNum.
// When building DLLs for Windows this is required even though no direct access to the MaybeStackHeaderAndArray leaks out of the i18n library.
// (See digitlst.h, pluralaffix.h, datefmt.h, and others for similar examples.)
#if U_PF_WINDOWS <= U_PLATFORM && U_PLATFORM <= U_PF_CYGWIN
template class U_I18N_API MaybeStackHeaderAndArray<decNumber, char, DECNUM_INITIAL_CAPACITY>;
#endif

namespace number {
namespace impl {

struct MicroProps : public MicroPropsGenerator {

    // NOTE: All of these fields are properly initialized in NumberFormatterImpl.
    RoundingImpl rounder;
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


/**
 * A wrapper around LocalizedNumberFormatter implementing the Format interface, enabling improved
 * compatibility with other APIs.
 *
 * @draft ICU 62
 * @see NumberFormatter
 */
class U_I18N_API LocalizedNumberFormatterAsFormat : public Format {
  public:
    LocalizedNumberFormatterAsFormat(const LocalizedNumberFormatter& formatter, const Locale& locale);

    /**
     * Destructor.
     */
    ~LocalizedNumberFormatterAsFormat() U_OVERRIDE;

    /**
     * Equals operator.
     */
    UBool operator==(const Format& other) const U_OVERRIDE;

    /**
     * Creates a copy of this object.
     */
    Format* clone() const U_OVERRIDE;

    /**
     * Formats a Number using the wrapped LocalizedNumberFormatter. The provided formattable must be a
     * number type.
     */
    UnicodeString& format(const Formattable& obj, UnicodeString& appendTo, FieldPosition& pos,
                          UErrorCode& status) const U_OVERRIDE;

    /**
     * Formats a Number using the wrapped LocalizedNumberFormatter. The provided formattable must be a
     * number type.
     */
    UnicodeString& format(const Formattable& obj, UnicodeString& appendTo, FieldPositionIterator* posIter,
                          UErrorCode& status) const U_OVERRIDE;

    /**
     * Not supported: sets an error index and returns.
     */
    void parseObject(const UnicodeString& source, Formattable& result,
                     ParsePosition& parse_pos) const U_OVERRIDE;

    /**
     * Gets the LocalizedNumberFormatter that this wrapper class uses to format numbers.
     *
     * For maximum efficiency, this function returns by const reference. You must copy the return value
     * into a local variable if you want to use it beyond the lifetime of the current object:
     *
     * <pre>
     * LocalizedNumberFormatter localFormatter = fmt->getNumberFormatter();
     * </pre>
     *
     * You can however use the return value directly when chaining:
     *
     * <pre>
     * FormattedNumber result = fmt->getNumberFormatter().formatDouble(514.23, status);
     * </pre>
     *
     * @return The unwrapped LocalizedNumberFormatter.
     */
    const LocalizedNumberFormatter& getNumberFormatter() const;

  private:
    LocalizedNumberFormatter fFormatter;

    // Even though the locale is inside the LocalizedNumberFormatter, we have to keep it here, too, because
    // LocalizedNumberFormatter doesn't have a getLocale() method, and ICU-TC didn't want to add one.
    Locale fLocale;
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
// Exported as U_I18N_API for tests
class U_I18N_API DecNum : public UMemory {
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
    static constexpr int32_t kDefaultDigits = DECNUM_INITIAL_CAPACITY;
    MaybeStackHeaderAndArray<decNumber, char, kDefaultDigits> fData;
    decContext fContext;

    void _setTo(const char* str, int32_t maxDigits, UErrorCode& status);
};


} // namespace impl
} // namespace number
U_NAMESPACE_END

#endif //__NUMBER_UTILS_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
