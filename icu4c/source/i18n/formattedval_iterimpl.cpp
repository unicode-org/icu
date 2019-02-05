// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

// This file contains one implementation of FormattedValue.
// Other independent implementations should go into their own cpp file for
// better dependency modularization.

#include "formattedval_impl.h"

U_NAMESPACE_BEGIN


FormattedValueFieldPositionIteratorImpl::FormattedValueFieldPositionIteratorImpl(
        int32_t initialFieldCapacity,
        UErrorCode& status)
        : fFields(initialFieldCapacity * 4, status) {
}

FormattedValueFieldPositionIteratorImpl::~FormattedValueFieldPositionIteratorImpl() = default;

UnicodeString FormattedValueFieldPositionIteratorImpl::toString(
        UErrorCode&) const {
    return fString;
}

UnicodeString FormattedValueFieldPositionIteratorImpl::toTempString(
        UErrorCode&) const {
    // The alias must point to memory owned by this object;
    // fastCopyFrom doesn't do this when using a stack buffer.
    return UnicodeString(TRUE, fString.getBuffer(), fString.length());
}

Appendable& FormattedValueFieldPositionIteratorImpl::appendTo(
        Appendable& appendable,
        UErrorCode&) const {
    appendable.appendString(fString.getBuffer(), fString.length());
    return appendable;
}

UBool FormattedValueFieldPositionIteratorImpl::nextPosition(
        ConstrainedFieldPosition& cfpos,
        UErrorCode&) const {
    U_ASSERT(fFields.size() % 4 == 0);
    int32_t numFields = fFields.size() / 4;
    int32_t i = cfpos.getInt64IterationContext();
    for (; i < numFields; i++) {
        UFieldCategory category = static_cast<UFieldCategory>(fFields.elementAti(i * 4));
        int32_t field = fFields.elementAti(i * 4 + 1);
        if (cfpos.matchesField(category, field)) {
            int32_t start = fFields.elementAti(i * 4 + 2);
            int32_t limit = fFields.elementAti(i * 4 + 3);
            cfpos.setState(category, field, start, limit);
            break;
        }
    }
    cfpos.setInt64IterationContext(i == numFields ? i : i + 1);
    return i < numFields;
}


FieldPositionIteratorHandler FormattedValueFieldPositionIteratorImpl::getHandler(
        UErrorCode& status) {
    return FieldPositionIteratorHandler(&fFields, status);
}

void FormattedValueFieldPositionIteratorImpl::appendString(
        UnicodeString string,
        UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    fString.append(string);
    // Make the string NUL-terminated
    if (fString.getTerminatedBuffer() == nullptr) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
}


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
