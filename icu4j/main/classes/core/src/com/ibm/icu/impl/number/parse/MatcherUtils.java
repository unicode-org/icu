// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

/**
 * @author sffc
 *
 */
public class MatcherUtils {
    public static boolean isValidCodePoint(int cp) {
        return Character.isValidCodePoint(cp)
                && (Character.isSupplementaryCodePoint(cp) || !Character.isSurrogate((char) cp));
    }

}
