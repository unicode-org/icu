package com.ibm.text;
import java.util.*;

/**
 * A transliterator that converts Hangul to Jamo
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Mark Davis
 * @version $RCSfile: HangulJamoTransliterator.java,v $ $Revision: 1.2 $ $Date: 2000/01/18 17:51:09 $
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
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
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
