package com.ibm.text;

/**
 * <code>UnicodeFilter</code> defines a protocol for selecting a
 * subset of the full range (U+0000 to U+FFFF) of Unicode characters.
 * Currently, filters are used in conjunction with classes like {@link
 * Transliterator} to only process selected characters through a
 * transformation.
 *
 * {@link UnicodeFilterLogic}
 */

public interface UnicodeFilter {

    /**
     * Returns <tt>true</tt> for characters that are in the selected
     * subset.  In other words, if a character is <b>to be
     * filtered</b>, then <tt>isIn()</tt> returns
     * <b><tt>false</tt></b>.
     */
    public boolean isIn(char c);
}
