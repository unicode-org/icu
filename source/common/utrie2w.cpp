/*
******************************************************************************
*
*   Copyright (C) 2001-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  utrie2.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2008aug16 (starting from a copy of utrie.c)
*   created by: Markus W. Scherer
*
*   mostly moved back into C 2011 jan 13  by srl
*
*   This is a common implementation of a Unicode trie.
*   It is a kind of compressed, serializable table of 16- or 32-bit values associated with
*   Unicode code points (0..0x10ffff).
*   This is the second common version of a Unicode trie (hence the name UTrie2).
*   See utrie2.h for a comparison.
*
*   This file contains only the runtime and enumeration code, for read-only access.
*   See utrie2_builder.c for the builder code.
*/
#ifdef UTRIE2_DEBUG
#   include <stdio.h>
#endif

#include "unicode/utypes.h"
#include "cmemory.h"
#include "utrie2.h"
#include "utrie2_impl.h"

/* C++ convenience wrappers ------------------------------------------------- */

U_NAMESPACE_BEGIN

uint16_t BackwardUTrie2StringIterator::previous16() {
    codePointLimit=codePointStart;
    if(start>=codePointStart) {
        codePoint=U_SENTINEL;
        return 0;
    }
    uint16_t result;
    UTRIE2_U16_PREV16(trie, start, codePointStart, codePoint, result);
    return result;
}

uint16_t ForwardUTrie2StringIterator::next16() {
    codePointStart=codePointLimit;
    if(codePointLimit==limit) {
        codePoint=U_SENTINEL;
        return 0;
    }
    uint16_t result;
    UTRIE2_U16_NEXT16(trie, codePointLimit, limit, codePoint, result);
    return result;
}

UTrie2 *UTrie2Singleton::getInstance(InstantiatorFn *instantiator, const void *context,
                                     UErrorCode &errorCode) {
    void *duplicate;
    UTrie2 *instance=(UTrie2 *)singleton.getInstance(instantiator, context, duplicate, errorCode);
    utrie2_close((UTrie2 *)duplicate);
    return instance;
}

U_NAMESPACE_END
