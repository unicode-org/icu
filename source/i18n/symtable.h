/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   02/04/00    aliu        Creation.
**********************************************************************
*/
#ifndef SYMTABLE_H
#define SYMTABLE_H

/**
 * An abstract class that maps strings to objects.
 */
class SymbolTable {
public:

    /**
     * Lookup the object associated with this string and return it.
     * Return U_ILLEGAL_ARGUMENT_ERROR status if the name does not
     * exist.  Return a non-NULL set if the name is mapped to a set;
     * otherwise return a NULL set.
     */
    virtual void lookup(const UnicodeString& name, UChar& c, UnicodeSet*& set,
                        UErrorCode& status) const = 0;
};

#endif
