package com.ibm.text;
import java.util.*;

/**
 * A transliterator that converts from hexadecimal Unicode
 * escape sequences to the characters they represent.  For example, "U+0040"
 * and '\u0040'.  It recognizes the
 * prefixes "U+", "u+", "&#92;U", and "&#92;u".  Hex values may be
 * upper- or lowercase.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: HexToUnicodeTransliterator.java,v $ $Revision: 1.1 $ $Date: 1999/12/20 18:29:21 $
 */
public class HexToUnicodeTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Hex-Unicode";

    /**
     * Constructs a transliterator.
     */
    public HexToUnicodeTransliterator() {
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
        int[] offsets = { start, limit, start };
        handleKeyboardTransliterate(text, offsets);
        return offsets[LIMIT];
    }

    /**
     * Implements {@link Transliterator#handleKeyboardTransliterate}.
     */
    protected void handleKeyboardTransliterate(Replaceable text,
                                               int[] offsets) {
        /**
         * Performs transliteration changing Unicode hexadecimal
         * escapes to characters.  For example, "U+0040" -> '@'.  A fixed
         * set of prefixes is recognized: "&#92;u", "&#92;U", "u+", "U+". 
         */
        int cursor = offsets[CURSOR];
        int limit = offsets[LIMIT];

        int maxCursor = limit - 6;
    loop:
        while (cursor <= maxCursor) {
            char c = filteredCharAt(text, cursor + 5);
            int digit0 = Character.digit(c, 16);
            if (digit0 < 0) {
                if (c == '\\') {
                    cursor += 5;
                } else if (c == 'U' || c == 'u' || c == '+') {
                    cursor += 4;
                } else {
                    cursor += 6;
                }
                continue;
            }

            int u = digit0;

            for (int i=4; i>=2; --i) {
                c = filteredCharAt(text, cursor + i);
                int digit = Character.digit(c, 16);
                if (digit < 0) {
                    if (c == 'U' || c == 'u' || c == '+') {
                        cursor += i-1;
                    } else {
                        cursor += 6;
                    }
                    continue loop;
                }
                u |= digit << (4 * (5-i));
            }

            c = filteredCharAt(text, cursor);
            char d = filteredCharAt(text, cursor + 1);
            if (((c == 'U' || c == 'u') && d == '+')
                || (c == '\\' && (d == 'U' || d == 'u'))) {
                
                // At this point, we have a match; replace cursor..cursor+5
                // with u.
                text.replace(cursor, cursor+6, String.valueOf((char) u));
                limit -= 5;
                maxCursor -= 5;

                ++cursor;
            } else {
                cursor += 6;
            }
        }

        offsets[LIMIT] = limit;
        offsets[CURSOR] = cursor;
    }
    
    private char filteredCharAt(Replaceable text, int i) {
        char c;
        UnicodeFilter filter = getFilter();
        return (filter == null) ? text.charAt(i) :
            (filter.isIn(c = text.charAt(i)) ? c : '\uFFFF');
    }

    /**
     * Return the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.
     * @param direction either <code>FORWARD</code> or <code>REVERSE</code>
     * @return maximum number of preceding context characters this
     * transliterator needs to examine
     */
    protected int getMaximumContextLength() {
        return 0;
    }
}
