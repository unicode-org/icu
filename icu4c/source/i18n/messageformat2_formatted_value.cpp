// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_formatted_value.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

UnicodeString FormattedPlaceholder::toString(Locale locale, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return UnicodeString();
    }

    switch (getType()) {
        case FormattedPlaceholder::Type::STRING: {
            return getString();
        }
        case FormattedPlaceholder::Type::NUMBER: {
            const number::FormattedNumber& num = getNumber();
            return num.toString(status);
        }
        default: {
            const Formattable& input = getInput();
            LocalPointer<FormattedPlaceholder> formatted(FormattedPlaceholder::format(locale, input, status));
            if (U_FAILURE(status)) {
                return UnicodeString();
            }
            return formatted->toString(locale, status);
        }
    }
}

FormattedPlaceholder::~FormattedPlaceholder() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
