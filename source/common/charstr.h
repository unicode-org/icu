/*
**********************************************************************
*   Copyright (c) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/19/2001  aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "cmemory.h"

//--------------------------------------------------------------------
// class CharString
//
// This is a tiny wrapper class that is used internally to make a
// UnicodeString look like a const char*.  It can be allocated on the
// stack.  It only creates a heap buffer if it needs to.
//--------------------------------------------------------------------

U_NAMESPACE_BEGIN

class U_COMMON_API CharString : public UMemory {
public:
    // Constructor
    //     @param  str    The unicode string to be converted to char *
    //     @param  codepage   The char * code page.  ""   for invariant conversion.
    //                                               NULL for default code page.
    inline CharString(const UnicodeString& str, const char *codepage = "");
    inline ~CharString();
    inline operator const char*() const { return ptr; }

private:
    char buf[128];
    char* ptr;

    CharString(const CharString &other); // forbid copying of this class
    CharString &operator=(const CharString &other); // forbid copying of this class
};

inline CharString::CharString(const UnicodeString& str, const char *codepage) {
    int32_t    len;
    ptr = buf;
    len = str.extract(0, 0x7FFFFFFF, buf ,sizeof(buf)-1, codepage);
    buf[sizeof(buf)-1] = 0;  // extract does not add null if it thinks there is no space for it.
    if (len >= (int32_t)(sizeof(buf)-1)) {
        ptr = (char *)uprv_malloc(len+1);
        str.extract(0, 0x7FFFFFFF, ptr, len+1, codepage);
    }
}

inline CharString::~CharString() {
    if (ptr != buf) {
        uprv_free(ptr);
    }
}

U_NAMESPACE_END

//eof
