// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __FORMVAL_IMPL_H__
#define __FORMVAL_IMPL_H__

#include "unicode/utypes.h"
#if !UCONFIG_NO_FORMATTING

// This file contains compliant implementations of FormattedValue which can be
// leveraged by ICU formatters.
//
// Each implementation is defined in its own cpp file in order to split
// dependencies more modularly.

#include "unicode/formattedvalue.h"
#include "fphdlimp.h"
#include "uvectr32.h"
#include "util.h"

U_NAMESPACE_BEGIN


/** Implementation using FieldPositionHandler to accept fields. */
class FormattedValueFieldPositionIteratorImpl : public UMemory, public FormattedValue {
public:

    /** @param initialFieldCapacity Initially allocate space for this many fields. */
    FormattedValueFieldPositionIteratorImpl(int32_t initialFieldCapacity, UErrorCode& status);

    virtual ~FormattedValueFieldPositionIteratorImpl();

    // Implementation of FormattedValue (const):

    UnicodeString toString(UErrorCode& status) const U_OVERRIDE;
    UnicodeString toTempString(UErrorCode& status) const U_OVERRIDE;
    Appendable& appendTo(Appendable& appendable, UErrorCode& status) const U_OVERRIDE;
    UBool nextPosition(ConstrainedFieldPosition& cfpos, UErrorCode& status) const U_OVERRIDE;

    // Additional methods used during construction phase only (non-const):

    FieldPositionIteratorHandler getHandler(UErrorCode& status);
    void appendString(UnicodeString string, UErrorCode& status);

private:
    // Final data:
    UnicodeString fString;
    UVector32 fFields;
};


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
#endif // __FORMVAL_IMPL_H__
