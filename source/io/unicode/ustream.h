/*
**********************************************************************
*   Copyright (C) 2001-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*  FILE NAME : ustream.h
*
*   Modification History:
*
*   Date        Name        Description
*   06/25/2001  grhoten     Move iostream from unistr.h
******************************************************************************
*/
   
#ifndef USTREAM_H
#define USTREAM_H

#include "unicode/unistr.h"


/**
 * Write the contents of a UnicodeString to an ostream. This functions writes
 * the characters in a UnicodeString to an ostream. The UChars in the
 * UnicodeString are truncated to char, leading to undefined results with
 * anything not in the Latin1 character set.
 */
#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>

U_NAMESPACE_BEGIN
U_IO_API std::ostream & U_EXPORT2 operator<<(std::ostream& stream, const UnicodeString& s);

U_IO_API std::istream & U_EXPORT2 operator>>(std::istream& stream, UnicodeString& s);
U_NAMESPACE_END

#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>

U_NAMESPACE_BEGIN
U_IO_API ostream & U_EXPORT2 operator<<(ostream& stream, const UnicodeString& s);

U_IO_API istream & U_EXPORT2 operator>>(istream& stream, UnicodeString& s);
U_NAMESPACE_END

#endif

/* No operator for UChar because it can conflict with wchar_t  */

#endif
