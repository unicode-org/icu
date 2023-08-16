// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_formatted_value.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {
   
UnicodeString FormattedPlaceholder::toString(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return UnicodeString();
    }

    if (isFormattedNumber()) {
        U_ASSERT(num.isValid());
        return num->toString(status);
    }
    U_ASSERT(value.isValid());
    U_ASSERT(value->getType() == Formattable::Type::kString);
    return value->getString();
}
                      
} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
