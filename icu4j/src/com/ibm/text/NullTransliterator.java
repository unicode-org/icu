package com.ibm.text;
import java.util.*;

/**
 * A transliterator that leaves text unchanged.
 */
public class NullTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 2000. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Null";

    /**
     * Constructs a transliterator.
     */
    public NullTransliterator() {
        super(_ID, null);
    }

    /**
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return the new limit index
     */
    public int transliterate(Replaceable text, int start, int limit) {
        return limit;
    }

    /**
     * Implements {@link Transliterator#handleKeyboardTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       int[] offsets) {
        offsets[CURSOR] = offsets[LIMIT];
    }
}
