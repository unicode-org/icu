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

#define U_APPEND_CHAR32(c,target){\
       if(c < 0xfffe){\
            *(target)++ = (UChar)c;\
       }\
       else if(c >0xffff){\
                c-=0x0010000;\
				*(target)++ = (UChar)(0xd800+(UChar)(c>>10));\
				*(target)++ = (UChar)(0xdc00+(UChar)(c&0x3ff));\
       }\
}
void setErrorText(const char *s);
const char* getErrorText(void);

#endif
