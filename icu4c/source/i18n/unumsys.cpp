/*
*****************************************************************************************
* Copyright (C) 2013, International Business Machines Corporation and others.
* All Rights Reserved.
*****************************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unumsys.h"
#include "unicode/numsys.h"
#include "unicode/uenum.h"

U_NAMESPACE_USE


U_CAPI UNumberingSystem* U_EXPORT2
unumsys_open(const char *locale, UErrorCode *status)
{
    return (UNumberingSystem*)NumberingSystem::createInstance(Locale(locale), *status);
}


U_CAPI UNumberingSystem* U_EXPORT2
unumsys_openByName(const char *name, UErrorCode *status)
{
    return (UNumberingSystem*)NumberingSystem::createInstanceByName(name, *status);
}


U_CAPI void U_EXPORT2
unumsys_close(UNumberingSystem *unumsys)
{
    delete ((NumberingSystem*)unumsys);
}


U_CAPI UEnumeration* U_EXPORT2
unumsys_openAvailableNames(UErrorCode *status)
{
    return uenum_openFromStringEnumeration(NumberingSystem::getAvailableNames(*status), status);
}


U_CAPI const char * U_EXPORT2
unumsys_getName(UNumberingSystem *unumsys)
{
    return ((NumberingSystem*)unumsys)->getName();
}


U_CAPI int32_t U_EXPORT2
unumsys_getRadix(UNumberingSystem *unumsys)
{
    return ((NumberingSystem*)unumsys)->getRadix();
}


U_CAPI UBool U_EXPORT2
unumsys_isAlgorithmic(UNumberingSystem *unumsys)
{
    return ((NumberingSystem*)unumsys)->isAlgorithmic();
}


#endif /* #if !UCONFIG_NO_FORMATTING */
