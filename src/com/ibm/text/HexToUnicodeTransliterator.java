/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/HexToUnicodeTransliterator.java,v $ 
 * $Date: 2000/03/10 04:07:20 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
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
 * @version $RCSfile: HexToUnicodeTransliterator.java,v $ $Revision: 1.4 $ $Date: 2000/03/10 04:07:20 $
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
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        /**
         * Performs transliteration changing Unicode hexadecimal
         * escapes to characters.  For example, "U+0040" -> '@'.  A fixed
         * set of prefixes is recognized: "&#92;u", "&#92;U", "u+", "U+". 
         */
        int cursor = offsets.cursor;
        int limit = offsets.limit;

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

        offsets.limit = limit;
        offsets.cursor = cursor;
    }
}
