
/*
************************************************************************
* Copyright (c) 2007-2010, International Business Machines
* Corporation and others.  All Rights Reserved.
************************************************************************
*/

/** C++ Utilities to aid in debugging **/

#ifndef _DBGUTIL_H
#define _DBGUTIL_H

#include "unicode/utypes.h"
#include "udbgutil.h"
#include "unicode/unistr.h"

#if !UCONFIG_NO_FORMATTING

//class UnicodeString;

U_CAPI const UnicodeString&  U_EXPORT2 udbg_enumString(UDebugEnumType type, int32_t field);

/**
 * @return enum offset, or UDBG_INVALID_ENUM on error
 */ 
U_CAPI int32_t  U_EXPORT2 udbg_enumByString(UDebugEnumType type, const UnicodeString& string);

/**
 * Convert a UnicodeString (with ascii digits) into a number.
 * @param s string
 * @return numerical value, or 0 on error
 */
U_CAPI int32_t U_EXPORT2 udbg_stoi(const UnicodeString &s);

U_CAPI double U_EXPORT2 udbg_stod(const UnicodeString &s);

U_CAPI UnicodeString *udbg_escape(const UnicodeString &s, UnicodeString *dst);

#endif

#endif
