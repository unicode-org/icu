/*
**********************************************************************
*   Copyright (C) 1999-2002 IBM Corp. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99    rgillam     Complete port from Java.
*   01/13/2000 helena      Added UErrorCode to ctors.
*   06/14/2002 andy        Gutted for new RBBI impl.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "dbbi_tbl.h"
#include "unicode/dbbi.h"
#include "umutex.h"

U_NAMESPACE_BEGIN

//=======================================================================
// constructor
//=======================================================================

DictionaryBasedBreakIteratorTables::DictionaryBasedBreakIteratorTables(
                                 const char* dictionaryFilename, 
                                 UErrorCode &status) {
    fDictionary = new BreakDictionary(dictionaryFilename, status);
    fRefCount = 1;
}


void DictionaryBasedBreakIteratorTables::addReference() {
    umtx_atomic_inc(&fRefCount);
}


void DictionaryBasedBreakIteratorTables::removeReference() {
    if (umtx_atomic_dec(&fRefCount) == 0) {
        delete this;
    }
}


/**
 * Destructor
 */
DictionaryBasedBreakIteratorTables::~DictionaryBasedBreakIteratorTables() {
    delete fDictionary;
    fDictionary = NULL;
}


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

/* eof */
