/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
// Requires Java2
package com.ibm.richtext.textlayout.attributes;

/**
 * A Map is a collection of key-value pairs (or entries), where each
 * key in the Map is unique.  This interface is a subset of the
 * JDK 1.2 Map interface.  It is used by JDK 1.1-compatible code.
 */
public interface Map {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
     * Return the number of entries in this Map.
     * @return the number of entries in this Map
     */
    public int size();

    /**
     * Return true if this Map has no entries.
     * @return true if this Map has no entries
     */
    public boolean isEmpty();

    /**
     * Return the value of the given key.
     * @return the value of the given key.  If the key does not have
     * a value in this Map, null is returned.
     */
    public Object get(Object key);

    /**
     * Return true if this Map contains the given key.
     * @return true if this Map contains the given key
     */
    public boolean containsKey(Object key);
}