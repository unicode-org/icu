// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __NUMBER_LONGNAMES_H__
#define __NUMBER_LONGNAMES_H__

#include "cmemory.h"
#include "unicode/uversion.h"
#include "number_utils.h"
#include "number_modifiers.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {

class LongNameHandler : public MicroPropsGenerator, public ModifierStore, public UMemory {
  public:
    static UnicodeString getUnitDisplayName(
        const Locale& loc,
        const MeasureUnit& unit,
        UNumberUnitWidth width,
        UErrorCode& status);

    static UnicodeString getUnitPattern(
        const Locale& loc,
        const MeasureUnit& unit,
        UNumberUnitWidth width,
        StandardPlural::Form pluralForm,
        UErrorCode& status);

    static LongNameHandler*
    forCurrencyLongNames(const Locale &loc, const CurrencyUnit &currency, const PluralRules *rules,
                         const MicroPropsGenerator *parent, UErrorCode &status);

    static void forMeasureUnit(const Locale &loc, const MeasureUnit &unit, const MeasureUnit &perUnit,
                               const UNumberUnitWidth &width, const PluralRules *rules,
                               const MicroPropsGenerator *parent, LongNameHandler *fillIn,
                               UErrorCode &status);

    void
    processQuantity(DecimalQuantity &quantity, MicroProps &micros, UErrorCode &status) const U_OVERRIDE;

    const Modifier* getModifier(Signum signum, StandardPlural::Form plural) const U_OVERRIDE;

  private:
    SimpleModifier fModifiers[StandardPlural::Form::COUNT];
    // Not owned
    const PluralRules *rules;
    // Not owned
    const MicroPropsGenerator *parent;

    LongNameHandler(const PluralRules *rules, const MicroPropsGenerator *parent)
        : rules(rules), parent(parent) {
    }

    LongNameHandler() : rules(nullptr), parent(nullptr) {
    }

    friend class MemoryPool<LongNameHandler>; // To enable emplaceBack();
    friend class NumberFormatterImpl;

    static void forCompoundUnit(const Locale &loc, const MeasureUnit &unit, const MeasureUnit &perUnit,
                                const UNumberUnitWidth &width, const PluralRules *rules,
                                const MicroPropsGenerator *parent, LongNameHandler *fillIn,
                                UErrorCode &status);

    void simpleFormatsToModifiers(const UnicodeString *simpleFormats, Field field, UErrorCode &status);
    void multiSimpleFormatsToModifiers(const UnicodeString *leadFormats, UnicodeString trailFormat,
                                       Field field, UErrorCode &status);
};

const int MAX_PREFS_COUNT = 10;

/**
 * A MicroPropsGenerator that multiplexes between different LongNameHandlers,
 * depending on the outputUnit (micros.helpers.outputUnit should be set earlier
 * in the chain).
 */
class LongNameMultiplexer : public MicroPropsGenerator, public UMemory {
  public:
    // FIXME: docstring?
    static LongNameMultiplexer *forMeasureUnits(const Locale &loc,
                                                const MaybeStackVector<MeasureUnit> &units,
                                                const UNumberUnitWidth &width, const PluralRules *rules,
                                                const MicroPropsGenerator *parent, UErrorCode &status);

    void processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                         UErrorCode &status) const U_OVERRIDE;

  private:
    /**
     * Because we only know which LongNameHandler we wish to call after calling
     * earlier MicroPropsGenerators in the chain, LongNameMultiplexer keeps the
     * parent link, while the LongNameHandlers are given no parents.
     */
    MaybeStackVector<LongNameHandler> fLongNameHandlers;
    LocalArray<MeasureUnit> fMeasureUnits;
    const MicroPropsGenerator *fParent;

    LongNameMultiplexer(const MicroPropsGenerator *parent) : fParent(parent) {
    }
};

}  // namespace impl
}  // namespace number
U_NAMESPACE_END

#endif //__NUMBER_LONGNAMES_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
