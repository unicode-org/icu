// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.text.UTF16;
import java.util.Iterator;
import java.util.Set;

/**
 * UnicodeSetIterator iterates over the contents of a UnicodeSet. It iterates over either code
 * points or code point ranges. After all code points or ranges have been returned, it returns the
 * multicharacter strings of the UnicodeSet, if any.
 *
 * <p>To iterate over code points, use a loop like this:
 *
 * <pre>
 * UnicodeSetIterator it = new UnicodeSetIterator(set);
 * while (set.next()) {
 *   if (set.codepoint != UnicodeSetIterator.IS_STRING) {
 *     processCodepoint(set.codepoint);
 *   } else {
 *     processString(set.string);
 *   }
 * }
 * </pre>
 *
 * <p>To iterate over code point ranges, use a loop like this:
 *
 * <pre>
 * UnicodeSetIterator it = new UnicodeSetIterator(set);
 * while (set.nextRange()) {
 *   if (set.codepoint != UnicodeSetIterator.IS_STRING) {
 *     processCodepointRange(set.codepoint, set.codepointEnd);
 *   } else {
 *     processString(set.string);
 *   }
 * }
 * </pre>
 *
 * @author M. Davis
 * @internal CLDR
 */
public class UnicodeMapIterator<T> {

    /**
     * Value of {@code codepoint} if the iterator points to a string. If {@code codepoint ==
     * IS_STRING}, then examine {@code string} for the current iteration result.
     */
    public static int IS_STRING = -1;

    /**
     * Current code point, or the special value {@code IS_STRING}, if the iterator points to a
     * string.
     */
    public int codepoint;

    /**
     * When iterating over ranges using {@code nextRange()}, {@code codepointEnd} contains the
     * inclusive end of the iteration range, if {@code codepoint != IS_STRING}. If iterating over
     * code points using {@code next()}, or if {@code codepoint == IS_STRING}, then the value of
     * {@code codepointEnd} is undefined.
     */
    public int codepointEnd;

    /**
     * If {@code codepoint == IS_STRING}, then {@code string} points to the current string. If
     * {@code codepoint != IS_STRING}, the value of {@code string} is undefined.
     */
    public String string;

    /** The value associated with this element or range. */
    public T value;

    /**
     * Create an iterator over the given set.
     *
     * @param set set to iterate over
     */
    public UnicodeMapIterator(UnicodeMap set) {
        reset(set);
    }

    /**
     * Create an iterator over nothing. {@code next()} and {@code nextRange()} return false. This is
     * a convenience constructor allowing the target to be set later.
     */
    public UnicodeMapIterator() {
        reset(new UnicodeMap());
    }

    /**
     * Returns the next element in the set, either a single code point or a string. If there are no
     * more elements in the set, return false. If {@code codepoint == IS_STRING}, the value is a
     * string in the {@code string} field. Otherwise the value is a single code point in the {@code
     * codepoint} field.
     *
     * <p>The order of iteration is all code points in sorted order, followed by all strings sorted
     * order. {@code codepointEnd} is undefined after calling this method. {@code string} is
     * undefined unless {@code codepoint == IS_STRING}. Do not mix calls to {@code next()} and
     * {@code nextRange()} without calling {@code reset()} between them. The results of doing so are
     * undefined.
     *
     * @return true if there was another element in the set and this object contains the element.
     */
    public boolean next() {
        if (nextElement <= endElement) {
            codepoint = codepointEnd = nextElement++;
            return true;
        }
        while (range < endRange) {
            if (loadRange(++range) == null) {
                continue;
            }
            codepoint = codepointEnd = nextElement++;
            return true;
        }

        // stringIterator == null iff there are no string elements remaining

        if (stringIterator == null) return false;
        codepoint = IS_STRING; // signal that value is actually a string
        string = stringIterator.next();
        if (!stringIterator.hasNext()) stringIterator = null;
        return true;
    }

    /**
     * Returns the next element in the set, either a code point range or a string. If there are no
     * more elements in the set, return false. If {@code codepoint == IS_STRING}, the value is a
     * string in the {@code string} field. Otherwise the value is a range of one or more code points
     * from {@code codepoint} to {@code codepointeEnd} inclusive.
     *
     * <p>The order of iteration is all code points ranges in sorted order, followed by all strings
     * sorted order. Ranges are disjoint and non-contiguous. {@code string} is undefined unless
     * {@code codepoint == IS_STRING}. Do not mix calls to {@code next()} and {@code nextRange()}
     * without calling {@code reset()} between them. The results of doing so are undefined.
     *
     * @return true if there was another element in the set and this object contains the element.
     */
    public boolean nextRange() {
        if (nextElement <= endElement) {
            codepointEnd = endElement;
            codepoint = nextElement;
            nextElement = endElement + 1;
            return true;
        }
        while (range < endRange) {
            if (loadRange(++range) == null) {
                continue;
            }
            codepointEnd = endElement;
            codepoint = nextElement;
            nextElement = endElement + 1;
            return true;
        }

        // stringIterator == null iff there are no string elements remaining

        if (stringIterator == null) return false;
        codepoint = IS_STRING; // signal that value is actually a string
        string = stringIterator.next();
        if (!stringIterator.hasNext()) stringIterator = null;
        return true;
    }

    /**
     * Sets this iterator to visit the elements of the given set and resets it to the start of that
     * set. The iterator is valid only so long as {@code set} is valid.
     *
     * @param set the set to iterate over.
     */
    public void reset(UnicodeMap set) {
        this.map = set;
        reset();
    }

    /**
     * Resets this iterator to the start of the set.
     *
     * @return
     */
    public UnicodeMapIterator<T> reset() {
        endRange = map.getRangeCount() - 1;
        // both next*() methods will test: if (nextElement <= endElement)
        // we set them to fail this test, which will cause them to load the first range
        nextElement = 0;
        endElement = -1;
        range = -1;

        stringIterator = null;
        Set<String> strings = map.getNonRangeStrings();
        if (strings != null) {
            stringIterator = strings.iterator();
            if (!stringIterator.hasNext()) stringIterator = null;
        }
        value = null;
        return this;
    }

    /**
     * Gets the current string from the iterator. Only use after calling next(), not nextRange().
     */
    public String getString() {
        if (codepoint != IS_STRING) {
            return UTF16.valueOf(codepoint);
        }
        return string;
    }

    // ======================= PRIVATES ===========================

    private UnicodeMap<T> map;
    private int endRange = 0;
    private int range = 0;
    private Iterator<String> stringIterator = null;
    protected int endElement;
    protected int nextElement;

    /*
     * Invariant: stringIterator is null when there are no (more) strings remaining
     */

    protected T loadRange(int range) {
        nextElement = map.getRangeStart(range);
        endElement = map.getRangeEnd(range);
        value = map.getRangeValue(range);
        return value;
    }
}
