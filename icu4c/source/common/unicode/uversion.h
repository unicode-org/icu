/*
*******************************************************************************
*   Copyright (C) 2000-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  uversion.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   Created by: Vladimir Weinstein
*
*  Gets included by utypes.h and Windows .rc files
*/

/**
 * \file
 * \brief C API: API for accessing ICU version numbers. 
 */
/*===========================================================================*/
/* Main ICU version information                                              */
/*===========================================================================*/

#ifndef UVERSION_H
#define UVERSION_H

/**
 * IMPORTANT: When updating version, the following things need to be done:
 * source/common/unicode/uversion.h - this file: update major, minor,
 *        patchlevel, suffix, version, short version constants, namespace,
 *                                                             and copyright
 * source/common/common.vcproj - update 'Output file name' on the link tab so
 *                   that it contains the new major/minor combination
 * source/i18n/i18n.vcproj - same as for the common.vcproj
 * source/layout/layout.vcproj - same as for the common.vcproj
 * source/layoutex/layoutex.vcproj - same
 * source/stubdata/stubdata.vcproj - same as for the common.vcproj
 * source/io/io.vcproj - same as for the common.vcproj
 * source/data/makedata.mak - change U_ICUDATA_NAME so that it contains
 *                            the new major/minor combination
 * source/tools/genren/genren.pl - use this script according to the README
 *                    in that folder                                         
 */

#include "unicode/umachine.h"

/* Actual version info lives in uverdefs.h */
#include "unicode/uverdefs.h"

/** Maximum length of the copyright string.
 *  @stable ICU 2.4
 */
#define U_COPYRIGHT_STRING_LENGTH  128

/** An ICU version consists of up to 4 numbers from 0..255.
 *  @stable ICU 2.4
 */
#define U_MAX_VERSION_LENGTH 4

/** In a string, ICU version fields are delimited by dots.
 *  @stable ICU 2.4
 */
#define U_VERSION_DELIMITER '.'

/** The maximum length of an ICU version string.
 *  @stable ICU 2.4
 */
#define U_MAX_VERSION_STRING_LENGTH 20

/** The binary form of a version on ICU APIs is an array of 4 uint8_t.
 *  @stable ICU 2.4
 */
typedef uint8_t UVersionInfo[U_MAX_VERSION_LENGTH];



/*===========================================================================*/
/* General version helper functions. Definitions in putil.c                  */
/*===========================================================================*/

/**
 * Parse a string with dotted-decimal version information and
 * fill in a UVersionInfo structure with the result.
 * Definition of this function lives in putil.c
 *
 * @param versionArray The destination structure for the version information.
 * @param versionString A string with dotted-decimal version information,
 *                      with up to four non-negative number fields with
 *                      values of up to 255 each.
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
u_versionFromString(UVersionInfo versionArray, const char *versionString);

/**
 * Parse a Unicode string with dotted-decimal version information and
 * fill in a UVersionInfo structure with the result.
 * Definition of this function lives in putil.c
 *
 * @param versionArray The destination structure for the version information.
 * @param versionString A Unicode string with dotted-decimal version
 *                      information, with up to four non-negative number
 *                      fields with values of up to 255 each.
 * @draft ICU 4.2
 */
U_STABLE void U_EXPORT2
u_versionFromUString(UVersionInfo versionArray, const UChar *versionString);


/**
 * Write a string with dotted-decimal version information according
 * to the input UVersionInfo.
 * Definition of this function lives in putil.c
 *
 * @param versionArray The version information to be written as a string.
 * @param versionString A string buffer that will be filled in with
 *                      a string corresponding to the numeric version
 *                      information in versionArray.
 *                      The buffer size must be at least U_MAX_VERSION_STRING_LENGTH.
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
u_versionToString(UVersionInfo versionArray, char *versionString);

/**
 * Gets the ICU release version.  The version array stores the version information
 * for ICU.  For example, release "1.3.31.2" is then represented as 0x01031F02.
 * Definition of this function lives in putil.c
 *
 * @param versionArray the version # information, the result will be filled in
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2
u_getVersion(UVersionInfo versionArray);

/**
 * Compare two versions of UVersionInfo type to determine their equality.
 *
 * @param version1 The destination structure for the version information.
 * @param version2 A Unicode string with dotted-decimal version
 *                      information, with up to four non-negative number
 *                      fields with values of up to 255 each.
 *
 * @return 0 if equal, negative value if version1 is less than version2, positive value if version1 is greater than version2
 *
 * @draft ICU 4.4
 */
U_DRAFT int32_t U_EXPORT2
u_versionCompare(UVersionInfo version1, UVersionInfo version2);

/**
 * Copy the information in versionSrc to versionDest.
 *
 * @param versionDest The destination structure for the version information.
 * @param versionSrc The source UVersionInfo to copy
 * @draft ICU 4.4
 */
U_DRAFT void U_EXPORT2
u_versionCopy(UVersionInfo versionDest, UVersionInfo versionSrc);

#endif
