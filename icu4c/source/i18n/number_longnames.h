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

// FIXME: describe me?
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

    static LongNameHandler*
    forMeasureUnit(const Locale &loc, const MeasureUnit &unit, const MeasureUnit &perUnit,
                   const UNumberUnitWidth &width, const PluralRules *rules,
                   const MicroPropsGenerator *parent, UErrorCode &status);

    void
    processQuantity(DecimalQuantity &quantity, MicroProps &micros, UErrorCode &status) const U_OVERRIDE;

    const Modifier* getModifier(Signum signum, StandardPlural::Form plural) const U_OVERRIDE;

  private:
    SimpleModifier fModifiers[StandardPlural::Form::COUNT];
    const PluralRules *rules;
    const MicroPropsGenerator *parent;

    LongNameHandler(const PluralRules *rules, const MicroPropsGenerator *parent)
            : rules(rules), parent(parent) {}

    static LongNameHandler*
    forCompoundUnit(const Locale &loc, const MeasureUnit &unit, const MeasureUnit &perUnit,
                    const UNumberUnitWidth &width, const PluralRules *rules,
                    const MicroPropsGenerator *parent, UErrorCode &status);

    // FIXME: describe me?
    void simpleFormatsToModifiers(const UnicodeString *simpleFormats, Field field, UErrorCode &status);
    void multiSimpleFormatsToModifiers(const UnicodeString *leadFormats, UnicodeString trailFormat,
                                       Field field, UErrorCode &status);
};

const int MAX_PREFS_COUNT = 10;

/// FIXME
/// And check if ModifierStore is the right pattern here.
class LongNameMultiplexer : public MicroPropsGenerator, public UMemory {
  public:
    static LongNameMultiplexer *forMeasureUnits(const Locale &loc,
                                                const MaybeStackVector<MeasureUnit> &units,
                                                const UNumberUnitWidth &width, const PluralRules *rules,
                                                const MicroPropsGenerator *parent, UErrorCode &status);

    void processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                         UErrorCode &status) const U_OVERRIDE;

  private:
    // LocalPointer<const LongNameHandler> fLongNameHandlers[MAX_PREFS_COUNT];
    MaybeStackVector<const LongNameHandler> fLongNameHandlers;
    LocalArray<MeasureUnit> fMeasureUnits;
    const MicroPropsGenerator *fParent;

    LongNameMultiplexer(const MicroPropsGenerator *parent) : fParent(parent) {
    //   // LocalPointer related:
    //   // U_ASSERT(fLongNameHandlers[0] == NULL);
    //   // U_ASSERT(fLongNameHandlers[MAX_PREFS_COUNT-1] == NULL);
    }
};

}  // namespace impl
}  // namespace number
U_NAMESPACE_END

#endif //__NUMBER_LONGNAMES_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
