/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM Corp. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99    rgillam     Complete port from Java.
*   01/13/2000 helena      Added UErrorCode to ctors.
**********************************************************************
*/

#ifndef DBBI_TBL_H
#define DBBI_TBL_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/udata.h"
#include "brkdict.h"

U_NAMESPACE_BEGIN

/* forward declaration */
class DictionaryBasedBreakIterator;

//
//   DictionaryBasedBreakIteratorTables
//
//        This class sits between instances of DictionaryBasedBreakIterator
//        and the dictionary data itself,  which is of type BreakDictionary.
//        It provides reference counting, allowing multiple copies of a
//        DictionaryBasedBreakIterator to share a single instance of
//        BreakDictionary.
//
//        TODO:  it'd probably be cleaner to add the reference counting to
//        BreakDictionary and get rid of this class, but doing it this way
//        was a convenient transition from earlier code, and time is short...
//
class DictionaryBasedBreakIteratorTables : public UMemory {

private:
    int32_t      fRefCount;


public:
    //=======================================================================
    // constructor
    //=======================================================================
    /* @param dictionaryFilename The name of the dictionary file
     * @param status The error code
     * @return the newly created DictionaryBasedBreakIteratorTables
     **/
    DictionaryBasedBreakIteratorTables(const char*       dictionaryFilename,
                                             UErrorCode& status);

    BreakDictionary    *fDictionary;
    void addReference();
    void removeReference();
    /**
     * Destructor.  Should not be used directly.  Use removeReference() istead.
     *              (Not private to avoid compiler warnings.)
     */
    virtual ~DictionaryBasedBreakIteratorTables();

private:
    /**
     * The copy constructor is declared private and not implemented.
     * THIS CLASS MAY NOT BE COPIED.
     * @param that The DictionaryBasedBreakIteratorTables to be copied.
     * @return the newly constructed DictionaryBasedBreakIteratorTables.
     */
    DictionaryBasedBreakIteratorTables(const DictionaryBasedBreakIteratorTables& that);

    //=======================================================================
    // boilerplate
    //=======================================================================


    /**
     * The assignment operator is declared private and not implemented.
     * THIS CLASS MAY NOT BE COPIED.
     * Call addReference() and share an existing copy instead.
     * @that The object to be copied
     * @return the newly created  DictionaryBasedBreakIteratorTables.
     */
    DictionaryBasedBreakIteratorTables& operator=(
            const DictionaryBasedBreakIteratorTables& that);
};

U_NAMESPACE_END

#endif
