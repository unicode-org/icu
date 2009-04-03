//##header
/*
 *******************************************************************************
 * Copyright (C) 2009, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.text.ParsePosition;
import java.util.regex.Pattern;

import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.sun.jdi.InternalException;

/**
 * Contains utilities to supplement the JDK Regex, since it doesn't handle
 * Unicode well.
 * 
 * @author markdavis
 */
public class UnicodeRegex implements Cloneable, Freezable, StringTransform {
    // Note: we don't currently have any state, but intend to in the future,
    // particularly for the regex style supported.
    
    /**
     * Adds full Unicode property support, with the latest version of Unicode,
     * to Java Regex, bringing it up to Level 1 (see
     * http://www.unicode.org/reports/tr18/). It does this by preprocessing the
     * regex pattern string and interpreting the character classes (\p{...},
     * \P{...}, [...]) according to their syntax and meaning in UnicodeSet. With
     * this utility, Java regex expressions can be updated to work with the
     * latest version of Unicode, and with all Unicode properties. Note that the
     * UnicodeSet syntax has not yet, however, been updated to be completely
     * consistent with Java regex, so be careful of the differences.
     * <p>Not thread-safe; create a separate copy for different threads.
     * <p>In the future, we may extend this to support other regex packages.
     * 
     * @regex A modified Java regex pattern, as in the input to
     *        Pattern.compile(), except that all "character classes" are
     *        processed as if they were UnicodeSet patterns. Example:
     *        "abc[:bc=N:]. See UnicodeSet for the differences in syntax.
     * @return A processed Java regex pattern, suitable for input to
     *         Pattern.compile().
     */
    public String transform(String regex) {
        StringBuffer result = new StringBuffer();
        UnicodeSet temp = new UnicodeSet();
        ParsePosition pos = new ParsePosition(0);
        int state = 0; // 1 = after \

        // We add each character unmodified to the output, unless we have a
        // UnicodeSet. Note that we don't worry about supplementary characters,
        // since none of the syntax uses them.

        for (int i = 0; i < regex.length(); ++i) {
            // look for UnicodeSets, allowing for quoting with \ and \Q
            char ch = regex.charAt(i);
            switch (state) {
            case 0: // we only care about \, and '['.
                if (ch == '\\') {
                    if (UnicodeSet.resemblesPattern(regex, i)) {
                        // should only happen with \p
                        i = processSet(regex, i, result, temp, pos);
                        continue;
                    }
                    state = 1;
                } else if (ch == '[') {
                    // if we have what looks like a UnicodeSet
                    if (UnicodeSet.resemblesPattern(regex, i)) {
                        i = processSet(regex, i, result, temp, pos);
                        continue;
                    }
                }
                break;

            case 1: // we are after a \
                if (ch == 'Q') {
                    state = 1;
                } else {
                    state = 0;
                }
                break;

            case 2: // we are in a \Q...
                if (ch == '\\') {
                    state = 3;
                }
                break;

            case 3: // we are in at \Q...\
                if (ch == 'E') {
                    state = 0;
                }
                state = 2;
                break;
            }
            result.append(ch);
        }
        return result.toString();
    }

    /**
     * Convenience static function, using standard parameters.
     * @param regex as in process()
     * @return processed regex pattern, as in process()
     */
    public static String fix(String regex) {
        return STANDARD.transform(regex);
    }
    
    /**
     * Compile a regex string, after processing by fix(...).
     * 
     * @param regex
     *            Raw regex pattern, as in fix(...).
     * @return Pattern
     */
    public static Pattern compile(String regex) {
        return Pattern.compile(STANDARD.transform(regex));
    }

    /**
     * Compile a regex string, after processing by fix(...).
     * 
     * @param regex
     *            Raw regex pattern, as in fix(...).
     * @return Pattern
     */
    public static Pattern compile(String regex, int options) {
        return Pattern.compile(STANDARD.transform(regex), options);
    }
    
    /* (non-Javadoc)
     * @see com.ibm.icu.util.Freezable#cloneAsThawed()
     */
    public Object cloneAsThawed() {
        // TODO Auto-generated method stub
        try {
            return this.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(); // should never happen
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.Freezable#freeze()
     */
    public Object freeze() {
        // no action needed now.
        return this;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.Freezable#isFrozen()
     */
    public boolean isFrozen() {
        // at this point, always true
        return true;
    }

    // ===== PRIVATES =====
    
    private int processSet(String regex, int i, StringBuffer result, UnicodeSet temp, ParsePosition pos) {
        pos.setIndex(i);
        UnicodeSet x = temp.clear().applyPattern(regex, pos, null, 0);
        x.complement().complement(); // hack to fix toPattern
        result.append(x.toPattern(false));
        i = pos.getIndex() - 1; // allow for the loop increment
        return i;
    }

    private static UnicodeRegex STANDARD = new UnicodeRegex();

}
