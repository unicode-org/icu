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

#include "unicode/utypes.h"
#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

class ParsePosition;
class UnicodeFunctor;
class UnicodeSet;
class UnicodeString;

/**
 * An interface that maps strings to objects.  This interface defines
 * both lookup protocol and parsing.  This allows different components
 * to share a symbol table and to handle name parsing uniformly.  It
 * is expected that client parse code look for the SYMBOL_REF
 * character and, when seen, attempt to parse the characters after it
 * using parseReference().
 *
 * <p>Currently, RuleBasedTransliterator and UnicodeSet use this
 * interface to share variable definitions.
 */
class SymbolTable /* not : public UObject because this is an interface/mixin class */ {
public:

    /**
     * The character preceding a symbol reference name.
     */
    enum { SYMBOL_REF = 0x0024 /*$*/ };

    /**
     * Destructor.
     */
    virtual inline ~SymbolTable() {};

    /**
     * Lookup the characters associated with this string and return it.
     * Return <tt>NULL</tt> if no such name exists.  The resultant
     * string may have length zero.
     */
    virtual const UnicodeString* lookup(const UnicodeString& s) const = 0;

    /**
     * Lookup the UnicodeMatcher associated with the given character, and
     * return it.  Return <tt>null</tt> if not found.
     */
    virtual const UnicodeFunctor* lookupMatcher(UChar32 ch) const = 0;

    /**
     * Parse a symbol reference name from the given string, starting
     * at the given position.  If no valid symbol reference name is
     * found, return an empty string.
     * @param text the text to parse for the name
     * @param pos on entry, the index of the first character to parse.
     * This is the character following the SYMBOL_REF character.  On
     * exit, the index after the last parsed character.
     * @param limit the index after the last character to be parsed.
     * @return the parsed name or an empty string.
     */
    virtual UnicodeString parseReference(const UnicodeString& text,
                                         ParsePosition& pos, int32_t limit) const = 0;
};
U_NAMESPACE_END
 

#endif
