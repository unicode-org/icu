/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File error.h
*
* Modification History:
*
*   Date        Name        Description
*   05/28/99    stephen     Creation.
*******************************************************************************
*/

#ifndef ERROR_H
#define ERROR_H 1

#define U_APPEND_CHAR32(c,target,len) {                         \
    if (c <= 0xffff)                                            \
    {                                                           \
        *(target)++ = (UChar) c;                                \
        len=1;                                                  \
    }                                                           \
    else                                                        \
    {                                                           \
        c -= 0x0010000;                                         \
        *(target)++ = (UChar) (0xd800 + (UChar) (c >> 10));     \
        *(target)++ = (UChar) (0xdc00 + (UChar) (c & 0x3ff));   \
        len=2;                                                  \
    }                                                           \
}

extern const char *gCurrentFileName;

void error   (uint32_t linenumber, const char *msg, ...);
void warning (uint32_t linenumber, const char *msg, ...);

#endif
