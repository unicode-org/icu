/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
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

U_NAMESPACE_USE

// for unistrm.h
/**
 * Write the contents of a UnicodeString to an ostream. This functions writes
 * the characters in a UnicodeString to an ostream. The UChars in the
 * UnicodeString are truncated to char, leading to undefined results with
 * anything not in the Latin1 character set.
 */
#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>
U_USTDIO_API std::ostream &operator<<(std::ostream& stream, const UnicodeString& s);

U_USTDIO_API std::istream &operator>>(std::istream& stream, UnicodeString& s);
#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>
U_USTDIO_API ostream &operator<<(ostream& stream, const UnicodeString& s);

U_USTDIO_API istream &operator>>(istream& stream, UnicodeString& s);
#endif

/* TODO: We should add the operator<< and the operator>> for UDate. */
/* No operator for UChar because it can conflict with wchar_t  */

#endif
