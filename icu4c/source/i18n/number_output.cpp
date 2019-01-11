// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/numberformatter.h"
#include "number_utypes.h"
#include "util.h"
#include "number_decimalquantity.h"

U_NAMESPACE_BEGIN
namespace number {


FormattedNumber::FormattedNumber(FormattedNumber&& src) U_NOEXCEPT
        : fResults(src.fResults), fErrorCode(src.fErrorCode) {
    // Disown src.fResults to prevent double-deletion
    src.fResults = nullptr;
    src.fErrorCode = U_INVALID_STATE_ERROR;
}

FormattedNumber& FormattedNumber::operator=(FormattedNumber&& src) U_NOEXCEPT {
    delete fResults;
    fResults = src.fResults;
    fErrorCode = src.fErrorCode;
    // Disown src.fResults to prevent double-deletion
    src.fResults = nullptr;
    src.fErrorCode = U_INVALID_STATE_ERROR;
    return *this;
}

UnicodeString FormattedNumber::toString(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return ICU_Utility::makeBogusString();
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return ICU_Utility::makeBogusString();
    }
    return fResults->string.toUnicodeString();
}

UnicodeString FormattedNumber::toTempString(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return ICU_Utility::makeBogusString();
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return ICU_Utility::makeBogusString();
    }
    return fResults->string.toTempUnicodeString();
}

Appendable& FormattedNumber::appendTo(Appendable& appendable, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return appendable;
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return appendable;
    }
    appendable.appendString(fResults->string.chars(), fResults->string.length());
    return appendable;
}

UBool FormattedNumber::nextPosition(ConstrainedFieldPosition& cfpos, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return FALSE;
    }
    // NOTE: MSVC sometimes complains when implicitly converting between bool and UBool
    return fResults->string.nextPosition(cfpos, status) ? TRUE : FALSE;
}

UBool FormattedNumber::nextFieldPosition(FieldPosition& fieldPosition, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return FALSE;
    }
    // NOTE: MSVC sometimes complains when implicitly converting between bool and UBool
    return fResults->string.nextFieldPosition(fieldPosition, status) ? TRUE : FALSE;
}

void FormattedNumber::getAllFieldPositions(FieldPositionIterator& iterator, UErrorCode& status) const {
    FieldPositionIteratorHandler fpih(&iterator, status);
    getAllFieldPositionsImpl(fpih, status);
}

void FormattedNumber::getAllFieldPositionsImpl(FieldPositionIteratorHandler& fpih,
                                               UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return;
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return;
    }
    fResults->string.getAllFieldPositions(fpih, status);
}

void FormattedNumber::getDecimalQuantity(impl::DecimalQuantity& output, UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return;
    }
    if (fResults == nullptr) {
        status = fErrorCode;
        return;
    }
    output = fResults->quantity;
}

FormattedNumber::~FormattedNumber() {
    delete fResults;
}


} // namespace number
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
