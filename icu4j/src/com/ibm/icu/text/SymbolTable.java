/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/SymbolTable.java,v $ 
 * $Date: 2000/04/21 22:16:29 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.text;
import java.text.ParsePosition;

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
public interface SymbolTable {

    /**
     * The character preceding a symbol reference name.
     */
    final char SYMBOL_REF = '$';

    /**
     * Lookup the object associated with this string and return it.
     * Return <tt>null</tt> if no such name exists.
     */
    Object lookup(String s);

    /**
     * Parse a symbol reference name from the given string, starting
     * at the given position.  If no valid symbol reference name is
     * found, throw an exception.
     * @param text the text to parse for the name
     * @param pos on entry, the index of the first character to parse.
     * This is the character following the SYMBOL_REF character.  On
     * exit, the index after the last parsed character.
     * @param limit the index after the last character to be parsed.
     * @return the parsed name.
     * @exception IllegalArgumentException if no valid name is found.
     */
    String parseReference(String text, ParsePosition pos, int limit);
}
