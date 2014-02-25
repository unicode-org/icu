/*
*******************************************************************************
*
*   Copyright (C) 1998-2014, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* Private implementation header for C collation
*   file name:  ucol_imp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000dec11
*   created by: Vladimir Weinstein
*
* Modification history
* Date        Name      Comments
* 02/16/2001  synwee    Added UCOL_GETPREVCE for the use in ucoleitr
* 02/27/2001  synwee    Added getMaxExpansion data structure in UCollator
* 03/02/2001  synwee    Added UCOL_IMPLICIT_CE
* 03/12/2001  synwee    Added pointer start to collIterate.
*/

#ifndef UCOL_IMP_H
#define UCOL_IMP_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/ucol.h"

/** Check whether two collators are equal. Collators are considered equal if they
 *  will sort strings the same. This means that both the current attributes and the
 *  rules must be equivalent.
 *  @param source first collator
 *  @param target second collator
 *  @return TRUE or FALSE
 *  @internal ICU 3.0
 */
U_INTERNAL UBool U_EXPORT2
ucol_equals(const UCollator *source, const UCollator *target);

/**
 * Convenience string denoting the Collation data tree
 */
#define U_ICUDATA_COLL U_ICUDATA_NAME U_TREE_SEPARATOR_STRING "coll"

#ifdef __cplusplus

U_NAMESPACE_BEGIN

struct CollationTailoring;

class Locale;
class UnicodeString;

/** Implemented in ucol_res.cpp. */
class CollationLoader {
public:
    static void appendRootRules(UnicodeString &s);
    static UnicodeString *loadRules(const char *localeID, const char *collationType,
                                    UErrorCode &errorCode);
    static const CollationTailoring *loadTailoring(const Locale &locale, Locale &validLocale,
                                                   UErrorCode &errorCode);

private:
    CollationLoader();  // not implemented, all methods are static
    static void loadRootRules(UErrorCode &errorCode);
};

U_NAMESPACE_END

#endif  /* __cplusplus */

#endif /* #if !UCONFIG_NO_COLLATION */

#endif
