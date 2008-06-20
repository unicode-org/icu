/**
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.CollectionUtilities.MultiComparator;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/** Provides more flexible formatting of UnicodeSet patterns.
 */
public class PrettyPrinter {
    private static final UnicodeSet patternWhitespace = (UnicodeSet) new UnicodeSet("[[:Cn:][:Default_Ignorable_Code_Point:][:patternwhitespace:]]").freeze();
    private static final UnicodeSet sortAtEnd = (UnicodeSet) new UnicodeSet("[[:Cn:][:Cs:][:Co:][:Ideographic:]]").freeze();
    
    private boolean first = true;
    private StringBuffer target = new StringBuffer();
    private int firstCodePoint = -2;
    private int lastCodePoint = -2;
    private boolean compressRanges = true;
    private String lastString = "";
    private UnicodeSet toQuote = new UnicodeSet(patternWhitespace);
    private Transliterator quoter = null;
    
    private Comparator ordering;
    private Comparator spaceComp = Collator.getInstance(ULocale.ROOT);
    {
        setOrdering(Collator.getInstance(ULocale.ROOT));
        ((RuleBasedCollator)spaceComp).setStrength(RuleBasedCollator.PRIMARY);
    }
    
    public Transliterator getQuoter() {
        return quoter;
    }

    public PrettyPrinter setQuoter(Transliterator quoter) {
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
    
    public Comparator getOrdering() {
        return ordering;
    }
    
    /**
     * @param ordering the resulting  ordering of the list of characters in the pattern
     * @return
     */
    public PrettyPrinter setOrdering(Comparator ordering) {
        this.ordering = new MultiComparator(new Comparator[] {ordering, new UTF16.StringComparator(true,false,0)});
        return this;
    }
    
    public Comparator getSpaceComparator() {
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
        toQuote = (UnicodeSet)toQuote.clone();
        toQuote.addAll(patternWhitespace);
        this.toQuote = toQuote;
        return this;
    }
        
    /**
     * Get the pattern for a particular set.
     * @param uset
     * @return formatted UnicodeSet
     */
    public String toPattern(UnicodeSet uset) {
        first = true;
        UnicodeSet putAtEnd = new UnicodeSet(uset).retainAll(sortAtEnd); // remove all the unassigned gorp for now
        // make sure that comparison separates all strings, even canonically equivalent ones
        Set orderedStrings = new TreeSet(ordering);
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
        for (Iterator it = orderedStrings.iterator(); it.hasNext();) {
            appendUnicodeSetItem((String) it.next());
        }
        for (UnicodeSetIterator it = new UnicodeSetIterator(putAtEnd); it.next();) { // add back the unassigned gorp
            appendUnicodeSetItem(it.codepoint);
        }
        flushLast();
        target.append("]");
        String sresult = target.toString();
        
        // double check the results. This can be removed once we have more tests.
//        try {
//            UnicodeSet  doubleCheck = new UnicodeSet(sresult);
//            if (!uset.equals(doubleCheck)) {
//                throw new IllegalStateException("Failure to round-trip in pretty-print " + uset + " => " + sresult + "\r\n source-result: " + new UnicodeSet(uset).removeAll(doubleCheck) +  "\r\n result-source: " + new UnicodeSet(doubleCheck).removeAll(uset));
//            }
//        } catch (RuntimeException e) {
//            throw (RuntimeException) new IllegalStateException("Failure to round-trip in pretty-print " + uset).initCause(e);
//        }
        return sresult;
    }
    
    private PrettyPrinter appendUnicodeSetItem(String s) {
        int cp;
        if (UTF16.hasMoreCodePointsThan(s, 1)) {
            flushLast();
            addSpace(s);
            target.append("{");
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                appendQuoted(cp = UTF16.charAt(s, i));
            }
            target.append("}");
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
    private void addSpace(String s) {
        if (first) {
            first = false;
        } else if (spaceComp.compare(s, lastString) != 0) {
            target.append(' ');
        } else {
            int cp = UTF16.charAt(s,0);
            int type = UCharacter.getType(cp);
            if (type == UCharacter.NON_SPACING_MARK || type == UCharacter.ENCLOSING_MARK) {
                target.append(' ');
            } else if (type == UCharacter.SURROGATE && cp >= UTF16.TRAIL_SURROGATE_MIN_VALUE) {
                target.append(' '); // make sure we don't accidentally merge two surrogates
            }
        }
    }
    
    private void flushLast() {
        if (lastCodePoint >= 0) {
            addSpace(UTF16.valueOf(firstCodePoint));
            if (firstCodePoint != lastCodePoint) {
                appendQuoted(firstCodePoint);
                target.append(firstCodePoint + 1 == lastCodePoint ? ' ' : '-');
            }
            appendQuoted(lastCodePoint);
            lastString = UTF16.valueOf(lastCodePoint);
            firstCodePoint = lastCodePoint = -2;
        }
    }
    PrettyPrinter appendQuoted(int codePoint) {
        if (toQuote.contains(codePoint)) {
            if (quoter != null) {
                target.append(quoter.transliterate(UTF16.valueOf(codePoint)));
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
            if (patternWhitespace.contains(codePoint)) {
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
}
