package com.ibm.text;
import java.util.*;

/**
 * A transliterator that converts from Unicode characters to 
 * hexadecimal Unicode escape sequences.  It outputs a
 * prefix specified in the constructor and optionally converts the hex
 * digits to uppercase.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: UnicodeToHexTransliterator.java,v $ $Revision: 1.1 $ $Date: 1999/12/20 18:29:21 $
 */
public class UnicodeToHexTransliterator extends Transliterator {

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Unicode-Hex";

    private String prefix;

    private boolean uppercase;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Constructs a transliterator.
     * @param prefix the string that will precede the four hex
     * digits for UNICODE_HEX transliterators.  Ignored
     * if direction is HEX_UNICODE.
     * @param uppercase if true, the four hex digits will be
     * converted to uppercase; otherwise they will be lowercase.
     * Ignored if direction is HEX_UNICODE.
     */
    public UnicodeToHexTransliterator(String prefix, boolean uppercase,
                                      UnicodeFilter filter) {
        super(_ID, filter);
        this.prefix = prefix;
        this.uppercase = uppercase;
    }

    /**
     * Constructs a transliterator with the default prefix "&#092;u"
     * that outputs uppercase hex digits.
     */
    public UnicodeToHexTransliterator() {
        this("\\u", true, null);
    }

    /**
     * Returns the string that precedes the four hex digits.
     * @return prefix string
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the string that precedes the four hex digits.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The prefix should not be changed by one
     * thread while another thread may be transliterating.
     * @param prefix prefix string
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns true if this transliterator outputs uppercase hex digits.
     */
    public boolean isUppercase() {
        return uppercase;
    }

    /**
     * Sets if this transliterator outputs uppercase hex digits.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The uppercase mode should not be changed by
     * one thread while another thread may be transliterating.
     * @param outputUppercase if true, then this transliterator
     * outputs uppercase hex digits.
     */
    public void setUppercase(boolean outputUppercase) {
        uppercase = outputUppercase;
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
         * Performs transliteration changing all characters to
         * Unicode hexadecimal escapes.  For example, '@' -> "U+0040",
         * assuming the prefix is "U+". 
         */
        int cursor = offsets[CURSOR];
        int limit = offsets[LIMIT];

        UnicodeFilter filter = getFilter();

    loop:
        while (cursor < limit) {
            char c = text.charAt(cursor);
            if (filter != null && !filter.isIn(c)) {
                ++cursor;
                continue;
            }
            String hex = hex(c);
            text.replace(cursor, cursor+1, hex);
            int len = hex.length();
            cursor += len; // Advance cursor by 1 and adjust for new text
            --len;
            limit += len;
        }

        offsets[LIMIT] = limit;
        offsets[CURSOR] = cursor;
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

    /**
     * Form escape sequence.
     */
    private final String hex(char c) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefix);
        if (c < 0x1000) {
            buf.append('0');
            if (c < 0x100) {
                buf.append('0');
                if (c < 0x10) {
                    buf.append('0');
                }
            }
        } 
        String h = Integer.toHexString(c);
        buf.append(uppercase ? h.toUpperCase() : h);
        return buf.toString();
    }
}
