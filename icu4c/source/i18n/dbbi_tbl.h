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

#include "rbbi_tbl.h"
#include "brkdict.h"

/**
 * This subclass of RuleBasedBreakIteratorTables contains the additional
 * static data that is used by DictionaryBasedBreakIterator.  This comprises
 * the dictionary itself and an array of flags that indicate which characters
 * are in the dictionary.
 *
 * @author Richard Gillam
 */
class DictionaryBasedBreakIteratorTables : public RuleBasedBreakIteratorTables {

private:
    /**
     * a list of known words that is used to divide up contiguous ranges of letters,
     * stored in a compressed, indexed, format that offers fast access
     */
    BreakDictionary dictionary;

    /**
     * a list of flags indicating which character categories are contained in
     * the dictionary file (this is used to determine which ranges of characters
     * to apply the dictionary to)
     */
    int8_t* categoryFlags;

    //=======================================================================
    // constructor
    //=======================================================================

    DictionaryBasedBreakIteratorTables(const void* tablesImage,
                                       char* dictionaryFilename,
                                       UErrorCode& status);
                                 
    /**
     * The copy constructor is declared private and not implemented.
     * THIS CLASS MAY NOT BE COPIED.
     */
    DictionaryBasedBreakIteratorTables(const DictionaryBasedBreakIteratorTables& that);

    //=======================================================================
    // boilerplate
    //=======================================================================

    /**
     * Destructor
     */
    virtual ~DictionaryBasedBreakIteratorTables();

    /**
     * The assignment operator is declared private and not implemented.
     * THIS CLASS MAY NOT BE COPIED.
     */
    DictionaryBasedBreakIteratorTables& operator=(
            const DictionaryBasedBreakIteratorTables& that);

protected:
    /**
     * Looks up a character's category (i.e., its category for breaking purposes,
     * not its Unicode category)
     */
    virtual int32_t lookupCategory(UChar c, BreakIterator* bi) const;

    friend class DictionaryBasedBreakIterator;
};

#endif
