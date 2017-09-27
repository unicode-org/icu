// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef NUMBERFORMAT_LONGNAMEHANDLER_H
#define NUMBERFORMAT_LONGNAMEHANDLER_H

#include "unicode/uversion.h"
#include "number_utils.h"
#include "number_modifiers.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {

class LongNameHandler : public MicroPropsGenerator, public UObject {
  public:
    static LongNameHandler
    forCurrencyLongNames(const Locale &loc, const CurrencyUnit &currency, const PluralRules *rules,
                         const MicroPropsGenerator *parent, UErrorCode &status);

    static LongNameHandler
    forMeasureUnit(const Locale &loc, const MeasureUnit &unit, const UNumberUnitWidth &width,
                   const PluralRules *rules, const MicroPropsGenerator *parent, UErrorCode &status);

    void
    processQuantity(DecimalQuantity &quantity, MicroProps &micros, UErrorCode &status) const override;

  private:
    SimpleModifier fModifiers[StandardPlural::Form::COUNT];
    const PluralRules *rules;
    const MicroPropsGenerator *parent;

    LongNameHandler(const PluralRules *rules, const MicroPropsGenerator *parent)
            : rules(rules), parent(parent) {}

    static void simpleFormatsToModifiers(const UnicodeString *simpleFormats, Field field,
                                         SimpleModifier *output, UErrorCode &status);
};

}  // namespace impl
}  // namespace number
U_NAMESPACE_END

#endif //NUMBERFORMAT_LONGNAMEHANDLER_H
