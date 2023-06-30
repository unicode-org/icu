// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/numberformatter.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

/*
using Binding         = MessageFormatDataModel::Binding;
using Bindings        = MessageFormatDataModel::Bindings;
using Expression      = MessageFormatDataModel::Expression;
using ExpressionList  = MessageFormatDataModel::ExpressionList;
using Key             = MessageFormatDataModel::Key;
using KeyList         = MessageFormatDataModel::KeyList;
template<typename T>
using List            = MessageFormatDataModel::List<T>;
using Literal         = MessageFormatDataModel::Literal;
using OptionMap       = MessageFormatDataModel::OptionMap;
using Operand         = MessageFormatDataModel::Operand;
using Operator        = MessageFormatDataModel::Operator;
using Pattern         = MessageFormatDataModel::Pattern;
using PatternPart     = MessageFormatDataModel::PatternPart;
using Reserved        = MessageFormatDataModel::Reserved;
using SelectorKeys    = MessageFormatDataModel::SelectorKeys;
using VariantMap      = MessageFormatDataModel::VariantMap;
*/

// Function registry implementation

FunctionRegistry* FunctionRegistry::Builder::build(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    U_ASSERT(formatters.isValid() && selectors.isValid());
    LocalPointer<FunctionRegistry> result(new FunctionRegistry(formatters.orphan(), selectors.orphan()));
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

/* static */ FunctionRegistry::Builder* FunctionRegistry::builder(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<FunctionRegistry::Builder> result(new FunctionRegistry::Builder());
    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

FunctionRegistry::Builder& FunctionRegistry::Builder::setSelector(const UnicodeString& selectorName, SelectorFactory* selectorFactory, UErrorCode& errorCode) {
    THIS_ON_ERROR(errorCode);
    if (selectorFactory == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return *this;
    }

    selectors->put(selectorName, selectorFactory, errorCode);
    return *this;
}

const FormatterFactory* FunctionRegistry::getFormatter(const UnicodeString& formatterName) const {
    // Caller must check for null
    return ((FormatterFactory*) formatters->get(formatterName));
}

// Specific formatter implementations

// --------- Number

static void strToInt(const UnicodeString& s, int64_t& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    numberFormat->setParseIntegerOnly(true);
    Formattable asNumber;
    numberFormat->parse(s, asNumber, errorCode);
    CHECK_ERROR(errorCode);
    result = asNumber.getInt64(errorCode);
}

static void strToDouble(const UnicodeString& s, double& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<NumberFormat> numberFormat(NumberFormat::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    Formattable asNumber;
    numberFormat->parse(s, asNumber, errorCode);
    CHECK_ERROR(errorCode);
    result = asNumber.getDouble(errorCode);
}

number::LocalizedNumberFormatter* formatterForOptions(Locale locale, const Hashtable& fixedOptions, UErrorCode& status) {
    NULL_ON_ERROR(status);

    number::UnlocalizedNumberFormatter nf;
    UnicodeString* skeleton = (UnicodeString*) (fixedOptions.get(UnicodeString("skeleton")));
    if (skeleton != nullptr) {
        nf = number::NumberFormatter::forSkeleton(*skeleton, status);
    } else {
        nf = number::NumberFormatter::with();
        UnicodeString* minFractionDigits = (UnicodeString*) fixedOptions.get(UnicodeString("minimumFractionDigits"));
        if (minFractionDigits != nullptr) {
            int64_t minFractionDigitsInt;
            strToInt(*minFractionDigits, minFractionDigitsInt, status);
            nf = nf.precision(number::Precision::minFraction(minFractionDigitsInt));
        }
    }
    NULL_ON_ERROR(status);
    LocalPointer<number::LocalizedNumberFormatter> result(new number::LocalizedNumberFormatter(nf.locale(locale)));
    if (!result.isValid()) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return result.orphan();
}

void addAll(const Hashtable& source, Hashtable& dest, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    int32_t pos = UHASH_FIRST;
    while (true) {
        const UHashElement* element = source.nextElement(pos);
        if (element == nullptr) {
            break;
        }
        UnicodeString *key = static_cast<UnicodeString *>(element->key.pointer);
        UnicodeString* value = static_cast<UnicodeString*>(element->value.pointer);
        U_ASSERT(key != nullptr && value != nullptr);
        dest.put(*key, value, errorCode);
    }
}

// variableOptions = map of *resolved* options (strings)
void StandardFunctions::Number::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<number::LocalizedNumberFormatter> realFormatter;
    if (variableOptions.count() == 0) {
        realFormatter.adoptInstead(new number::LocalizedNumberFormatter(icuFormatter));
        if (!realFormatter.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    } else {
        // Create a new map and add both the arguments and variable options into it
        LocalPointer<Hashtable> mergedOptions(new Hashtable(compareVariableName, nullptr, errorCode));
        CHECK_ERROR(errorCode);
        addAll(fixedOptions, *mergedOptions, errorCode);
        addAll(variableOptions, *mergedOptions, errorCode);
        CHECK_ERROR(errorCode);
        realFormatter.adoptInstead(formatterForOptions(locale, *mergedOptions, errorCode));
    }
    CHECK_ERROR(errorCode);

    // Offset is determined by the variable options if it's defined,
    // then by the fixed options if they exist and define `offset`,
    // and finally defaults to 0
    UnicodeString* offsetStr = static_cast<UnicodeString*>(variableOptions.get(UnicodeString("offset")));
    bool hasOffset = offsetStr != nullptr;
    int64_t offset = 0;
    if (!hasOffset) {
        offsetStr = static_cast<UnicodeString*>(fixedOptions.get(UnicodeString("offset")));
        if (offsetStr != nullptr) {
            strToInt(*offsetStr, offset, errorCode);
        }
    } else if (!hasOffset) {
        offset = 0;
    } else {
        strToInt(*offsetStr, offset, errorCode);
    }
    CHECK_ERROR(errorCode);

    // TODO: NumberFormatterFactory.java dispatches on the type of `toFormat`,
    // but here the formatters only take strings.
    // Try to parse the string as a number, as in the `else` case there
    double numberValue;
    strToDouble(toFormat, numberValue, errorCode);
    if (U_FAILURE(errorCode)) {
        errorCode = U_ZERO_ERROR;
        result.setTo(UnicodeString("NaN"));
    }
    number::FormattedNumber numberResult = realFormatter->formatDouble(numberValue - offset, errorCode);
    CHECK_ERROR(errorCode);
    result.setTo(numberResult.toString(errorCode));
}

// --------- PluralFactory

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

