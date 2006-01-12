/*
********************************************************************************
*   Copyright (C) 2005-2006, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINCOLL.CPP
*
********************************************************************************
*/

#include "unicode/utypes.h"

#ifdef U_WINDOWS

#if !UCONFIG_NO_COLLATION

#include "wincoll.h"

#include "unicode/coll.h"
#include "unicode/locid.h"
#include "unicode/ustring.h"

// NOTE: a PRIVATE interface!
#include "locmap.h"

#   define WIN32_LEAN_AND_MEAN
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(Win32Collator)

Win32Collator::Win32Collator(const Locale &locale, UErrorCode &status)
  : Collator(), fStrength(TERTIARY)
{
    if (!U_FAILURE(status)) {
        fLCID = locale.getLCID();
    }
}

Win32Collator::Win32Collator(const Locale &locale, ECollationStrength strength, UErrorCode &status)
  : Collator(), fStrength(strength)
{
    if (!U_FAILURE(status)) {
        fLCID = locale.getLCID();
    }
}

Win32Collator::Win32Collator(const Win32Collator &other)
  : Collator(other)
{
    *this = other;
}

Win32Collator::~Win32Collator()
{
    // anything?
}

Win32Collator &Win32Collator::operator=(const Win32Collator &other)
{
//    Collator::operator=(other);

    this->fLCID     = other.fLCID;
    this->fStrength = other.fStrength;

    return *this;
}

Collator *Win32Collator::clone(void) const
{
    return new Win32Collator(*this);
}

UCollationResult Win32Collator::compare(const UnicodeString& source,
                                        const UnicodeString& target,
                                        UErrorCode &status) const
{
    return compare(source.getBuffer(), source.length(),
                   target.getBuffer(), target.length(),
                   status);
}

UCollationResult Win32Collator::compare(const UnicodeString& source,
                                    const UnicodeString& target,
                                    int32_t length,
                                    UErrorCode &status) const
{
    return compare(source.getBuffer(), length, target.getBuffer(), length, status);
}

UCollationResult Win32Collator::compare(const UChar* source, int32_t sourceLength,
                                        const UChar* target, int32_t targetLength,
                                        UErrorCode &status) const
{
    DWORD dwCmpFlags = fStrength > Collator::SECONDARY? 0 : NORM_IGNORECASE;
    int32_t result = CompareStringW(fLCID, dwCmpFlags, source, sourceLength, target, targetLength);

    return (UCollationResult) (result - 2);
}

CollationKey &Win32Collator::getCollationKey(const UnicodeString&  source,
                                             CollationKey& key,
                                             UErrorCode& status) const
{
    return getCollationKey(source.getBuffer(), source.length(), key, status);
}

// TODO: This ends up pre-flighting twice, becuase it calls getSortKey() twice,
// and that always pre-flights. Might be OK for a deprecated API...
CollationKey &Win32Collator::getCollationKey(const UChar*source,
                                             int32_t sourceLength,
                                             CollationKey& key,
                                             UErrorCode& status) const
{
#ifdef FRIEND_OF_COLLATION_KEY
    if (U_FAILURE(status)) {
        return key.setToBogus();
    }

    if ((!source) || (sourceLen == 0)) {
        return key.reset();
    }

    int32_t resultLen = getSortKey(source, sourceLength, NULL, 0);
    uint8_t *result = new uint8_t[resultLen];

    getSortKey(source, sourceLength, result, resutLen);
    key.adopt(result, resultLen);
#else
    status = U_UNSUPPORTED_ERROR;
#endif

    return key;
}

// TODO: is this good enough? (Do we care?)
int32_t Win32Collator::hashCode(void) const
{
    return fLCID ^ fStrength;
}

// TODO: Good enough?
const Locale Win32Collator::getLocale(ULocDataLocaleType type, UErrorCode& status) const
{
    Locale loc(uprv_convertToPosix(fLCID, &status));

    return loc;
}

Collator::ECollationStrength Win32Collator::getStrength(void) const
{
    return fStrength;
}

void Win32Collator::setStrength(Collator::ECollationStrength newStrength)
{
    fStrength = newStrength;
}

// TODO: Need to include Windows version?
void Win32Collator::getVersion(UVersionInfo info) const
{
    info[0] = info[1] = info[2] = info[3] = 0;
}

void Win32Collator::setAttribute(UColAttribute attr, UColAttributeValue value, UErrorCode &status)
{
    if (attr == UCOL_STRENGTH) {
        fStrength = getECollationStrength(value);
    } else {
        status = U_UNSUPPORTED_ERROR;
    }
}

UColAttributeValue Win32Collator::getAttribute(UColAttribute attr, UErrorCode &status)
{
    if (attr == UCOL_STRENGTH) {
        return getUCollationStrength(fStrength);
    }

    status = U_UNSUPPORTED_ERROR;
    return UCOL_DEFAULT;
}

uint32_t Win32Collator::setVariableTop(const UChar *varTop, int32_t len, UErrorCode &status)
{
    status = U_UNSUPPORTED_ERROR;
    return 0;
}

uint32_t Win32Collator::setVariableTop(const UnicodeString varTop, UErrorCode &status)
{
    status = U_UNSUPPORTED_ERROR;
    return 0;
}

void Win32Collator::setVariableTop(const uint32_t varTop, UErrorCode &status)
{
    status = U_UNSUPPORTED_ERROR;
}

uint32_t Win32Collator::getVariableTop(UErrorCode &status) const
{
    status = U_UNSUPPORTED_ERROR;
    return 0;
}

Collator* Win32Collator::safeClone(void)
{
    return clone();
}

int32_t Win32Collator::getSortKey(const UnicodeString &source, uint8_t *result, int32_t resultLength) const
{
    return getSortKey(source.getBuffer(), source.length(), result, resultLength);
}

// TODO: Is there a way to do this w/o pre-flighting?
// TODO: We could avoid the tests of resultLength if we always copy from our buffer into result
int32_t Win32Collator::getSortKey(const UChar *source, int32_t sourceLength, uint8_t *result, int32_t resultLength) const
{
    int32_t requiredLength = LCMapStringW(fLCID, LCMAP_SORTKEY, source, sourceLength, NULL, 0);

    if (result != NULL) {
        uint8_t *res   = result;
        int32_t resLen = resultLength;

        if (requiredLength > resultLength) {
            res = new uint8_t[requiredLength];
            resLen = requiredLength;
        }
        
        // We don't need to keep the return value here because it will be requiredLength.
        LCMapStringW(fLCID, LCMAP_SORTKEY, source, sourceLength, (LPWSTR) res, resLen);

        if (requiredLength > resultLength) {
            for (int32_t i = 0; i < resultLength; i += 1) {
                result[i] = res[i];
            }

            delete[] res;
        }
    }

    return requiredLength;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_COLLATION */

#endif // #ifdef U_WINDOWS
