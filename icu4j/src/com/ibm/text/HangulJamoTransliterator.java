package com.ibm.text;
import java.util.*;

/**
 * A transliterator that converts Hangul to Jamo
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Mark Davis
 * @version $RCSfile: HangulJamoTransliterator.java,v $ $Revision: 1.1 $ $Date: 2000/01/18 02:30:49 $
 */
public class HangulJamoTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Hangul-Jamo";

    /**
     * Constructs a transliterator.
     */
    public HangulJamoTransliterator() {
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
        int cursor = offsets[CURSOR];
        int limit = offsets[LIMIT];

        StringBuffer replacement = new StringBuffer();
        while (cursor < limit) {
            char c = filteredCharAt(text, cursor);
            if (decomposeHangul(c, replacement)) {
                text.replace(cursor, cursor+1, replacement.toString());
                cursor += replacement.length(); // skip over replacement
                limit += replacement.length() - 1; // fix up limit
            } else {
                ++cursor;
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
    

    static final int 
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172
    
    public static boolean decomposeHangul(char s, StringBuffer result) {
        int SIndex = s - SBase;
        if (0 > SIndex || SIndex >= SCount) {
            return false;
        }
        int L = LBase + SIndex / NCount;
        int V = VBase + (SIndex % NCount) / TCount;
        int T = TBase + SIndex % TCount;
        result.setLength(0);
        result.append((char)L);
        result.append((char)V);
        if (T != TBase) result.append((char)T);
        return true;
    }
    
}
