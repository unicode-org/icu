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

//--------------------------------------------------------------------
// class CharString
//
// This is a tiny wrapper class that is used internally to make a
// UnicodeString look like a const char*.  It can be allocated on the
// stack.  It only creates a heap buffer if it needs to.
//--------------------------------------------------------------------

U_NAMESPACE_BEGIN

class U_COMMON_API CharString : public UObject {
 public:
    inline CharString(const UnicodeString& str);
    inline ~CharString();
    inline operator const char*() { return ptr; }

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

 private:
    char buf[128];
    char* ptr;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

inline CharString::CharString(const UnicodeString& str) {
    // TODO This isn't quite right -- we should probably do
    // preflighting here to determine the real length.
    if (str.length() >= (int32_t)sizeof(buf)) {
        ptr = new char[str.length() + 8];
    } else {
        ptr = buf;
    }
    str.extract(0, 0x7FFFFFFF, ptr, "");
}

inline CharString::~CharString() {
    if (ptr != buf) {
        delete[] ptr;
    }
}

U_NAMESPACE_END

//eof
