package com.ibm.text;

/**
 * An interface that maps strings to objects.
 */
public interface SymbolTable {

    /**
     * Lookup the object associated with this string and return it.
     * Return <tt>null</tt> if no such name exists.
     */
    Object lookup(String s);
}
