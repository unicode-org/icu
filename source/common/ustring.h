/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File ustring.h
*
* Modification History:
*
*   Date        Name        Description
*   12/07/98    bertrand    Creation.
*******************************************************************************
*/

#ifndef USTRING_H
#define USTRING_H
#include "utypes.h"

/** 
 * Determine the length of an array of UChar
 * @param s The array of UChars, NULL (U+0000) terminated.
 * @return The number of UChars in <TT>chars</TT>, minus the terminator.
 */
CAPI int32_t U_EXPORT2
u_strlen(const UChar *s);

/**
 * Concatenate two ustrings.  Appends a copy of <TT>src</TT>,
 * including the null terminator, to <TT>dst</TT>. The initial copied
 * character from <TT>src</TT> overwrites the null terminator in <TT>dst</TT>.
 * @param dst The destination string.
 * @param src The source string.
 * @return A pointer to <TT>dst</TT>.
 */

CAPI UChar* U_EXPORT2
u_strcat(UChar     *dst, 
    const UChar     *src);

/**
 * Concatenate two ustrings.  
 * Appends at most <TT>n</TT> characters from <TT>src</TT> to <TT>dst</TT>.
 * Adds a null terminator.
 * @param dst The destination string.
 * @param src The source string.
 * @param n The maximum number of characters to compare.
 * @return A pointer to <TT>dst</TT>.
 */
CAPI UChar* U_EXPORT2
u_strncat(UChar     *dst, 
     const UChar     *src, 
     int32_t     n);

/**
 * Find the first occurrence of a specified character in a ustring.
 * @param s The string to search.
 * @param c The character to find.
 * @return A pointer to the first occurrence of <TT>c</TT> in <TT>s</TT>,
 * or a null pointer if <TT>s</TT> does not contain <TT>c</TT>.
 */
CAPI UChar*  U_EXPORT2
u_strchr(const UChar     *s, 
    UChar     c);

/**
 * Compare two ustrings for bitwise equality.
 * @param s1 A string to compare.
 * @param s2 A string to compare.
 * @return 0 if <TT>s1</TT> and <TT>s2</TT> are bitwise equal; a negative
 * value if <TT>s1</TT> is bitwise less than <TT>s2,/TT>; a positive
 * value if <TT>s1</TT> is bitwise greater than <TT>s2,/TT>.
 */
CAPI int32_t  U_EXPORT2
u_strcmp(const UChar     *s1, 
    const UChar     *s2);

/**
 * Compare two ustrings for bitwise equality. 
 * Compares at most <TT>n</TT> characters.
 * @param s1 A string to compare.
 * @param s2 A string to compare.
 * @param n The maximum number of characters to compare.
 * @return 0 if <TT>s1</TT> and <TT>s2</TT> are bitwise equal; a negative
 * value if <TT>s1</TT> is bitwise less than <TT>s2,/TT>; a positive
 * value if <TT>s1</TT> is bitwise greater than <TT>s2,/TT>.
 */
CAPI int32_t U_EXPORT2
u_strncmp(const UChar     *ucs1, 
     const UChar     *ucs2, 
     int32_t     n);

/**
 * Copy a ustring.
 * Adds a null terminator.
 * @param dst The destination string.
 * @param src The source string.
 * @return A pointer to <TT>dst</TT>.
 */
CAPI UChar* U_EXPORT2
u_strcpy(UChar     *dst, 
    const UChar     *src);

/**
 * Copy a ustring.
 * Copies at most <TT>n</TT> characters.  The result will be null terminated
 * if the length of <TT>src</TT> is less than <TT>n</TT>.
 * @param dst The destination string.
 * @param src The source string.
 * @param n The maximum number of characters to copy.
 * @return A pointer to <TT>dst</TT>.
 */
CAPI UChar* U_EXPORT2
u_strncpy(UChar     *dst, 
     const UChar     *src, 
     int32_t     n);

/**
 * Copy a byte string encoded in the default codepage to a ustring.
 * Adds a null terminator.
 * performs a host byte to UChar conversion
 * @param dst The destination string.
 * @param src The source string.
 * @return A pointer to <TT>dst</TT>.
 */
CAPI UChar* U_EXPORT2 u_uastrcpy(UChar *ucs1,
               const char *s2 );

/**
 * Copy a byte string encoded in the default codepage to a ustring.
 * Copies at most <TT>n</TT> characters.  The result will be null terminated
 * if the length of <TT>src</TT> is less than <TT>n</TT>.
 * performs a host byte to UChar conversion
 * @param dst The destination string.
 * @param src The source string.
 * @param n The maximum number of characters to copy.
 * @return A pointer to <TT>dst</TT>.
 */
CAPI UChar* U_EXPORT2 u_uastrncpy(UChar *ucs1,
            const char *s2,
            int32_t n);

/**
 * Copy ustring to a byte string encoded in the default codepage.
 * Adds a null terminator.
 * performs a UChar to host byte conversion
 * @param dst The destination string.
 * @param src The source string.
 * @return A pointer to <TT>dst</TT>.
 */
CAPI char* U_EXPORT2 u_austrcpy(char *s1,
            const UChar *us2 );
#endif





