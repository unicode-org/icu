/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/JamoHangulTransliterator.java,v $ 
 * $Date: 2000/06/28 20:49:54 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
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
        setMaximumContextLength(3);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        /**
         * Performs transliteration changing Jamo to Hangul 
         */
        int cursor = offsets.start;
        int limit = offsets.limit;
        if (cursor >= limit) return;
        
        int count[] = new int[1];

        char last = filteredCharAt(text, cursor++);
        while (cursor <= limit) {
            char next = 0xFFFF; // go over end of string, just in case
            if (cursor < limit) next = filteredCharAt(text, cursor);
            char replacement = composeHangul(last, next, count);
            if (replacement != last) {
                text.replace(cursor-1, cursor-1 + count[0], String.valueOf(replacement));
                limit = limit - count[0] + 1; // fix up limit 2 => -1, 1 => 0
                last = replacement;
                if (next == 0xFFFF) break;
                // don't change cursor, so we revisit char
            } else {
                ++cursor;
                last = next;
            }
        }
        
        ++limit;
        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
        offsets.start = cursor;
    }
    

    static final int 
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount,   // 11172
        LLimit = 0x1200;
   
   /**
    * Return composed character (if it is a modern jamo)
    * last otherwise.
    * If there is a replacement, returns count[0] = 2 if ch was used, 1 otherwise
    */
   public static char composeHangul(char last, char ch, int[] count) {
      count[0] = 2; // default is replace 2 chars
      // check to see if two current characters are L and V
      int LIndex = last - LBase;
      if (0 <= LIndex && LIndex < LCount) {
          int VIndex = ch - VBase;
          if (0 <= VIndex && VIndex < VCount) {
              // make syllable of form LV
              return (char)(SBase + (LIndex * VCount + VIndex) * TCount);
          } else {
            // it is isolated, so fix!
            count[0] = 1; // not using ch
            return (char)(SBase + (LIndex * VCount) * TCount);
          }
      }
      
      // if neither case was true, see if we have an isolated Jamo we need to fix
      if (LBase <= last && last < LLimit) {
        // need to fix: it is either medial or final!
        int VIndex = last - VBase;
        if (0 <= VIndex && VIndex < VCount) {
            LIndex = 0x110B - LBase; // use empty consonant
            // make syllable of form LV
            count[0] = 1; // not using ch
            return (char)(SBase + (LIndex * VCount + VIndex) * TCount);
        }
        // ok, see if final. Use null consonant + a + final
        int TIndex = last - TBase;
        if (0 <= TIndex && TIndex <= TCount) {  // need to fix!
            count[0] = 1; // not using ch
            return (char)(0xC544 + TIndex);
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
      
      return last;
    }    
}
