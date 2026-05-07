// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.rbbi;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import java.text.ParsePosition;

/**
 * A segmentation rule expressed as in UAXes #14 and #29.
 *
 * <p>Rules are applied sequentially. Rules operate on a mutable remapped string (which the caller
 * should initially set to the string to be segmented), and can resolve positions in the original
 * string to either BREAK or NO_BREAK.
 */
public abstract class SegmentationRule {
    enum Resolution {
        BREAK,
        NO_BREAK,
    }

    static class BreakContext {
        BreakContext(int index) {
            indexInRemapped = index;
        }

        Integer indexInRemapped;
        SegmentationRule appliedRule = null;
    }
    ;

    SegmentationRule(String name) {
        name_ = name;
    }

    // Returns "\\uhhhh" for a BMP code point and "\\uDhhh\\uDhhh" (UTF-16) for other code points.
    private String javaUEscape(int codePoint) {
        if (codePoint <= 0xFFFF) {
            return "\\u" + Utility.hex(codePoint);
        } else {
            return "\\u"
                    + Utility.hex(UTF16.getLeadSurrogate(codePoint))
                    + "\\u"
                    + Utility.hex(UTF16.getTrailSurrogate(codePoint));
        }
    }

    protected String expandUnicodeSets(String regex) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        final boolean java8OrOlder = System.getProperty("java.version").startsWith("1.");
        while (i < regex.length()) {
            if (regex.charAt(i) == '[' || regex.charAt(i) == '\\') {
                ParsePosition pp = new ParsePosition(i);
                final UnicodeSet set = new UnicodeSet(regex, pp, null);
                // Regular expressions that match unpaired surrogates apparently behave
                // differently in Java 8.  Let’s not go there.
                if (java8OrOlder) {
                    set.removeAll(new UnicodeSet("[\\uD800-\\uDFFF]"));
                }
                // Escape everything.  We could use _generatePattern, but then we would have to
                // convert \U escapes to sequences of \‌u escapes, and to escape # ourselves.
                result.append('[');
                for (EntryRange range : set.ranges()) {
                    result.append(javaUEscape(range.codepoint));
                    if (range.codepointEnd != range.codepoint) {
                        result.append('-');
                        result.append(javaUEscape(range.codepointEnd));
                    }
                }
                result.append(']');
                i = pp.getIndex();
            } else {
                result.append(regex.charAt(i++));
            }
        }
        return result.toString();
    }

    abstract void apply(StringBuilder remapped, BreakContext[] resolved);

    abstract Resolution resolution();

    String name() {
        return name_;
    }

    private final String name_;
}
