package com.ibm.text;
import java.util.*;

/**
 * A transliterator that converts Jamo to Hangul
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Mark Davis
 */
public class JamoHangulTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Jamo-Hangul";

    /**
     * Constructs a transliterator.
     */
    public JamoHangulTransliterator() {
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
         * Performs transliteration changing Jamo to Hangul 
         */
        int cursor = offsets[CURSOR];
        int limit = offsets[LIMIT];
        if (cursor >= limit) return;
        
        // get last character
        char last = filteredCharAt(text, cursor++);
        // testing
        if (limit - cursor > 2) {
          last = (char)(last + 0);
        }

    loop:
        while (cursor < limit) {
            char c = filteredCharAt(text, cursor);
            char replacement = composeHangul(last, c);
            if (replacement != 0) {
              text.replace(cursor-1, cursor+1, String.valueOf(replacement));
              last = replacement;
              // leave cursor where it is
              --limit; // fix up limit
            } else {
              ++cursor;
            }
        }

        offsets[LIMIT] = limit + 1;
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
        return 3;
    }
    

    static final int 
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172
   
   /**
    * Return composed character (if it composes)
    * 0 otherwise
    */
   public static char composeHangul(char last, char ch) {
      // check to see if two current characters are L and V
      int LIndex = last - LBase;
      if (0 <= LIndex && LIndex < LCount) {
          int VIndex = ch - VBase;
          if (0 <= VIndex && VIndex < VCount) {
              // make syllable of form LV
              return (char)(SBase + (LIndex * VCount + VIndex) * TCount);
          }
      }
      // check to see if two current characters are LV and T
      int SIndex = last - SBase;
      if (0 <= SIndex && SIndex < SCount && (SIndex % TCount) == 0) {
          int TIndex = ch - TBase;
          if (0 <= TIndex && TIndex <= TCount) {
              // make syllable of form LVT
              return (char)(last + TIndex);
          }
      }
      // if neither case was true, skip
      return '\u0000';
    }    
}
