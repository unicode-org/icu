/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1996-1999                   *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*
*  FILE NAME : unistrm.h
*
*   Modification History:
*
*   Date        Name        Description
*   02/05/97    aliu        Added UnicodeString streamIn and streamOut methods.
*   03/26/97    aliu        Added indexOf(UChar,).
*   04/24/97    aliu        Numerous changes per code review.
*   05/06/97    helena      Added isBogus().
*****************************************************************************************
*/         
#ifndef UNISTRM_H
#define UNISTRM_H

#include <iostream.h>


#include "filestrm.h"
#include "unistr.h"


class U_COMMON_API UnicodeStringStreamer
{
public:
    static void streamIn(UnicodeString* string, FileStream* is);
    static void streamOut(const UnicodeString* string, FileStream* os);
};

/**
 * Write the contents of a UnicodeString to an ostream. This functions writes
 * the characters in a UnicodeString to an ostream. The UChars in the
 * UnicodeString are truncated to char, leading to undefined results with
 * anything not in the Latin1 character set.
 */
U_COMMON_API ostream& operator<<(ostream&              stream,
                              const UnicodeString&  string);

#endif



