/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
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
    inline CharString(const UnicodeString& str);
    inline ~CharString();
    inline operator const char*() const { return ptr; }

private:
    char buf[128];
    char* ptr;

    CharString(const CharString &other); // forbid copying of this class
    CharString &operator=(const CharString &other); // forbid copying of this class
};

inline CharString::CharString(const UnicodeString& str) {
    // Invariant converter should create str.length() chars
    if (str.length() >= (int32_t)sizeof(buf)) {
        ptr = (char *)uprv_malloc(str.length() + 8);
    } else {
        ptr = buf;
    }
    str.extract(0, 0x7FFFFFFF, ptr, "");
}

inline CharString::~CharString() {
    if (ptr != buf) {
        uprv_free(ptr);
    }
}

U_NAMESPACE_END

//eof
