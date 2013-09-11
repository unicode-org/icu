/*
*****************************************************************************************
* Copyright (C) 2013, International Business Machines
* Corporation and others. All Rights Reserved.
*****************************************************************************************
*/

#ifndef UNUMSYS_H
#define UNUMSYS_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/uenum.h"
#include "unicode/localpointer.h"

/**
 * \file
 * \brief C API: UNumberingSystem, information about numbering systems
 *
 * Defines numbering systems. A numbering system describes the scheme by which 
 * numbers are to be presented to the end user. In its simplest form, a numbering
 * system describes the set of digit characters that are to be used to display
 * numbers, such as Western digits, Thai digits, Arabic-Indic digits, etc. 
 * More complicated numbering systems are algorithmic in nature, and require use
 * of an RBNF formatter (rule based number formatter), in order to calculate
 * the characters to be displayed for a given number. Examples of algorithmic
 * numbering systems include Roman numerals, Chinese numerals, and Hebrew numerals.
 * Formatting rules for many commonly used numbering systems are included in
 * the ICU package, based on the numbering system rules defined in CLDR.
 * Alternate numbering systems can be specified to a locale by using the
 * numbers locale keyword.
 */

#ifndef U_HIDE_DRAFT_API

/**
 * Opaque UNumberingSystem object for use in C programs.
 * @draft ICU 52
 */
struct UNumberingSystem;
typedef struct UNumberingSystem UNumberingSystem;  /**< C typedef for struct UNumberingSystem. @draft ICU 52 */

/**
 * Opens a UNumberingSystem object using the default numbering system for the specified locale.
 * @param locale The locale for which the default numbering system should be opened.
 * @param status A pointer to a UErrorCode to receive any errors. For example, this may be U_UNSUPPORTED_ERROR
 *        for a locale such as "en@numbers=xyz" that specifies a numbering system unknown to ICU.
 * @return A UNumberingSystem for the specified locale, or NULL if an error occurred.
 * @draft ICU 52
 */
U_DRAFT UNumberingSystem * U_EXPORT2
unumsys_open(const char *locale, UErrorCode *status);

/**
 * Opens a UNumberingSystem object using the name of one of the predefined numbering systems defined by CLDR
 * and known to ICU, such as "latn", "arabext", or "hanidec"; the full list is returned by unumsys_openAvailableNames.
 * Note that some of the numbering systems names listed at
 * http://unicode.org/repos/cldr/tags/latest/common/bcp47/number.xml
 * do not identify specific numbering systems, but rather types that may only be used as part of a locale which
 * defines how they are mapped to a specific numbering system such as "latn" or "hant".
 *
 * @param name The name of the numbering system for which a UNumberingSystem object should be opened.
 * @param status A pointer to a UErrorCode to receive any errors. For example, this may be U_UNSUPPORTED_ERROR
 *        for a numbering system such as "xyz" that is unknown to ICU.
 * @return A UNumberingSystem for the specified name, or NULL if an error occurred.
 * @draft ICU 52
 */
U_DRAFT UNumberingSystem * U_EXPORT2
unumsys_openByName(const char *name, UErrorCode *status);

/**
 * Close a UNumberingSystem object. Once closed it may no longer be used.
 * @param unumsys The UNumberingSystem object to close.
 * @draft ICU 52
 */
U_DRAFT void U_EXPORT2
unumsys_close(UNumberingSystem *unumsys);

#if U_SHOW_CPLUSPLUS_API
U_NAMESPACE_BEGIN

/**
 * \class LocalUNumberingSystemPointer
 * "Smart pointer" class, closes a UNumberingSystem via unumsys_close().
 * For most methods see the LocalPointerBase base class.
 * @see LocalPointerBase
 * @see LocalPointer
 * @draft ICU 52
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUNumberingSystemPointer, UNumberingSystem, unumsys_close);

U_NAMESPACE_END
#endif

/**
 * Returns an enumeration over the names of all of the predefined numbering systems known to ICU.
 * @param status A pointer to a UErrorCode to receive any errors.
 * @return A pointer to a UEnumeration that must be closed with uenum_close(), or NULL if an error occurred.
 * @draft ICU 52
 */
U_DRAFT UEnumeration * U_EXPORT2
unumsys_openAvailableNames(UErrorCode *status);

/**
 * Returns the name of the specified UNumberingSystem object (if it is one of the predefined names known to ICU).
 * @param unumsys The UNumberingSystem whose name is desired.
 * @return A pointer to the name of the specified UNumberingSystem object, or NULL if the name is not one of the ICU predefined names.
 * @draft ICU 52
 */
U_DRAFT const char * U_EXPORT2
unumsys_getName(UNumberingSystem *unumsys);

/**
 * Returns the radix of the specified UNumberingSystem object.
 * @param unumsys The UNumberingSystem whose radix is desired.
 * @return The radix of the specified UNumberingSystem object.
 * @draft ICU 52
 */
U_DRAFT int32_t U_EXPORT2
unumsys_getRadix(UNumberingSystem *unumsys);

/**
 * Returns whether the given UNumberingSystem object is for an algorithmic (not purely decimal) system.
 * @param unumsys The UNumberingSystem whose algorithmic status is desired.
 * @return TRUE if the specified UNumberingSystem object is for an algorithmic system.
 * @draft ICU 52
 */
U_DRAFT UBool U_EXPORT2
unumsys_isAlgorithmic(UNumberingSystem *unumsys);

#endif  /* U_HIDE_DRAFT_API */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
