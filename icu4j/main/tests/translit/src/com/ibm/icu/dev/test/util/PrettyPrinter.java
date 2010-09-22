/**
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */

package com.ibm.icu.dev.test.util;

import java.io.IOException;
import java.text.FieldPosition;
import java.util.Comparator;
import java.util.TreeSet;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/** Provides more flexible formatting of UnicodeSet patterns.
 */
public class PrettyPrinter {
    private static final StringComparator CODEPOINT_ORDER = new UTF16.StringComparator(true,false,0);
    private static final UnicodeSet PATTERN_WHITESPACE = (UnicodeSet) new UnicodeSet("[[:Cn:][:Default_Ignorable_Code_Point:][:patternwhitespace:]]").freeze();
    private static final UnicodeSet SORT_AT_END = (UnicodeSet) new UnicodeSet("[[:Cn:][:Cs:][:Co:][:Ideographic:]]").freeze();
    private static final UnicodeSet QUOTED_SYNTAX = (UnicodeSet) new UnicodeSet("[\\[\\]\\-\\^\\&\\\\\\{\\}\\$\\:]").addAll(PATTERN_WHITESPACE).freeze();

    private boolean first = true;
    private StringBuffer target = new StringBuffer();
    private int firstCodePoint = -2;
    private int lastCodePoint = -2;
    private boolean compressRanges = true;
    private String lastString = "";
    private UnicodeSet toQuote = new UnicodeSet(PATTERN_WHITESPACE);
    private StringTransform quoter = null;

    private Comparator<String> ordering;
    private Comparator<String> spaceComp;

    public PrettyPrinter() {
    }

    public StringTransform getQuoter() {
        return quoter;
    }

    public PrettyPrinter setQuoter(StringTransform quoter) {
        this.quoter = quoter;
        return this; // for chaining
    }

    public boolean isCompressRanges() {
        return compressRanges;
    }

    /**
     * @param compressRanges if you want abcde instead of a-e, make this false
     * @return
     */
    public PrettyPrinter setCompressRanges(boolean compressRanges) {
        this.compressRanges = compressRanges;
        return this;
    }

    public Comparator<String> getOrdering() {
        return ordering;
    }

    /**
     * @param ordering the resulting  ordering of the list of characters in the pattern
     * @return
     */
    public PrettyPrinter setOrdering(Comparator ordering) {
        this.ordering = ordering == null ? CODEPOINT_ORDER : new com.ibm.icu.impl.MultiComparator<String>(ordering, CODEPOINT_ORDER);
        return this;
    }

    public Comparator<String> getSpaceComparator() {
        return spaceComp;
    }

    /**
     * @param spaceComp if the comparison returns non-zero, then a space will be inserted between characters
     * @return this, for chaining
     */
    public PrettyPrinter setSpaceComparator(Comparator spaceComp) {
        this.spaceComp = spaceComp;
        return this;
    }

    public UnicodeSet getToQuote() {
        return toQuote;
    }

    /**
     * a UnicodeSet of extra characters to quote with \\uXXXX-style escaping (will automatically quote pattern whitespace)
     * @param toQuote
     */
    public PrettyPrinter setToQuote(UnicodeSet toQuote) {
        if (toQuote != null) {
            toQuote = (UnicodeSet)toQuote.cloneAsThawed();
            toQuote.addAll(PATTERN_WHITESPACE);
            this.toQuote = toQuote;
        }
        return this;
    }


    /**
     * Get the pattern for a particular set.
     * @param uset
     * @return formatted UnicodeSet
     */
    public String format(UnicodeSet uset) {
        first = true;
        UnicodeSet putAtEnd = new UnicodeSet(uset).retainAll(SORT_AT_END); // remove all the unassigned gorp for now
        // make sure that comparison separates all strings, even canonically equivalent ones
        TreeSet<String> orderedStrings = new TreeSet<String>(ordering);
        for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.nextRange();) {
            if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                orderedStrings.add(it.string);
            } else {
                for (int i = it.codepoint; i <= it.codepointEnd; ++i) {
                    if (!putAtEnd.contains(i)) {
                        orderedStrings.add(UTF16.valueOf(i));
                    }
                }
            }
        }
        target.setLength(0);
        target.append("[");
        for (String item : orderedStrings) {
            appendUnicodeSetItem(item);
        }
        for (UnicodeSetIterator it = new UnicodeSetIterator(putAtEnd); it.next();) { // add back the unassigned gorp
            appendUnicodeSetItem(it.codepoint); // we know that these are only codepoints, not strings, so this is safe
        }
        flushLast();
        target.append("]");
        String sresult = target.toString();

        // double check the results. This can be removed once we have more tests.
        //        try {
        //            UnicodeSet  doubleCheck = new UnicodeSet(sresult);
        //            if (!uset.equals(doubleCheck)) {
        //                throw new IllegalStateException("Failure to round-trip in pretty-print " + uset + " => " + sresult + Utility.LINE_SEPARATOR + " source-result: " + new UnicodeSet(uset).removeAll(doubleCheck) +  Utility.LINE_SEPARATOR + " result-source: " + new UnicodeSet(doubleCheck).removeAll(uset));
        //            }
        //        } catch (RuntimeException e) {
        //            throw (RuntimeException) new IllegalStateException("Failure to round-trip in pretty-print " + uset).initCause(e);
        //        }
        return sresult;
    }

    private PrettyPrinter appendUnicodeSetItem(String s) {
        if (UTF16.hasMoreCodePointsThan(s, 1)) {
            flushLast();
            addSpaceAsNeededBefore(s);
            appendQuoted(s);
            lastString = s;
        } else {
            appendUnicodeSetItem(UTF16.charAt(s, 0));
        }
        return this;
    }

    private void appendUnicodeSetItem(int cp) {
        if (!compressRanges)
            flushLast();
        if (cp == lastCodePoint + 1) {
            lastCodePoint = cp; // continue range
        } else { // start range
            flushLast();
            firstCodePoint = lastCodePoint = cp;
        }
    }
    /**
     * 
     */
    private void addSpaceAsNeededBefore(String s) {
        if (first) {
            first = false;
        } else if (spaceComp != null && spaceComp.compare(s, lastString) != 0) {
            target.append(' ');
        } else {
            int cp = UTF16.charAt(s,0);
            if (!toQuote.contains(cp) && !QUOTED_SYNTAX.contains(cp)) {
                int type = UCharacter.getType(cp);
                if (type == UCharacter.NON_SPACING_MARK || type == UCharacter.ENCLOSING_MARK) {
                    target.append(' ');
                } else if (type == UCharacter.SURROGATE && cp >= UTF16.TRAIL_SURROGATE_MIN_VALUE) {
                    target.append(' '); // make sure we don't accidentally merge two surrogates
                }
            }
        }
    }

    private void addSpaceAsNeededBefore(int codepoint) {
        addSpaceAsNeededBefore(UTF16.valueOf(codepoint));
    }

    private void flushLast() {
        if (lastCodePoint >= 0) {
            addSpaceAsNeededBefore(firstCodePoint);
            if (firstCodePoint != lastCodePoint) {
                appendQuoted(firstCodePoint);
                if (firstCodePoint + 1 != lastCodePoint) {
                    target.append('-');
                } else {
                    addSpaceAsNeededBefore(lastCodePoint);
                }
            }
            appendQuoted(lastCodePoint);
            lastString = UTF16.valueOf(lastCodePoint);
            firstCodePoint = lastCodePoint = -2;
        }
    }


    private void appendQuoted(String s) {
        if (toQuote.containsSome(s) && quoter != null) {
            target.append(quoter.transform(s));
        } else {
            int cp;
            target.append("{");
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                appendQuoted(cp = UTF16.charAt(s, i));
            }
            target.append("}");
        }
    }

    PrettyPrinter appendQuoted(int codePoint) {
        if (toQuote.contains(codePoint)) {
            if (quoter != null) {
                target.append(quoter.transform(UTF16.valueOf(codePoint)));
                return this;
            }
            if (codePoint > 0xFFFF) {
                target.append("\\U");
                target.append(Utility.hex(codePoint,8));
            } else {
                target.append("\\u");
                target.append(Utility.hex(codePoint,4));                    
            }
            return this;
        }
        switch (codePoint) {
        case '[': // SET_OPEN:
        case ']': // SET_CLOSE:
        case '-': // HYPHEN:
        case '^': // COMPLEMENT:
        case '&': // INTERSECTION:
        case '\\': //BACKSLASH:
        case '{':
        case '}':
        case '$':
        case ':':
            target.append('\\');
            break;
        default:
            // Escape whitespace
            if (PATTERN_WHITESPACE.contains(codePoint)) {
                target.append('\\');
            }
            break;
        }
        UTF16.append(target, codePoint);
        return this;
    }        
    //  Appender append(String s) {
    //  target.append(s);
    //  return this;
    //  }
    //  public String toString() {
    //  return target.toString();
    //  }

    public Appendable format(UnicodeSet obj, Appendable toAppendTo, FieldPosition pos) {
        try {
            return toAppendTo.append(format(obj));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
