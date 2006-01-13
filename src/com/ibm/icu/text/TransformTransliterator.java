/*
 * Copyright (C) 1996-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 */
package com.ibm.icu.text;
//import java.util.*;

abstract class TransformTransliterator {
    // Currently unused
}

///**
// * An abstract class for transliterators based on a transform
// * operation.  To create a transliterator that implements a
// * transformation, create a subclass of this class and implement the
// * abstract <code>transform()</code> and <code>hasTransform()</code>
// * methods.
// * @author Alan Liu
// */
//abstract class TransformTransliterator extends Transliterator {
//
//    /**
//     * Constructs a transliterator.  For use by subclasses.
//     */
//    protected TransformTransliterator(String id, UnicodeFilter f) {
//        super(id, f);
//    }
//
//    /**
//     * Implements {@link Transliterator#handleTransliterate}.
//     */
//    protected void handleTransliterate(Replaceable text,
//                                       Position offsets, boolean incremental) {
//
//        int start;
//        for (start = offsets.start; start < offsets.limit; ++start) {
//            // Scan for the first character that is != its transform.
//            // If there are none, we fall out without doing anything.
//            char c = text.charAt(start);
//            if (hasTransform(c)) {
//                // There is a transforming character at start.  Break
//                // up the remaining string, from start to
//                // offsets.limit, into segments of unfiltered and
//                // filtered characters.  Only transform the unfiltered
//                // characters.  As always, minimize the number of
//                // calls to Replaceable.replace().
//
//                int len = offsets.limit - start;
//                // assert(len >= 1);
//                
//                char[] buf = new char[len];
//                text.getChars(start, offsets.limit, buf, 0);
//
//                int segStart = 0;
//                int segLimit;
//                UnicodeFilter filt = getFilter();
//
//                // lenDelta is the accumulated length difference for
//                // all transformed segments.  It is new length - old
//                // length.
//                int lenDelta = 0;
//
//                // Set segStart, segLimit to the unfiltered segment
//                // starting with start.  If the filter is null, then
//                // segStart/Limit will be set to the whole string,
//                // that is, 0/len.
//                do {
//                    // Set segLimit to the first filtered char at or
//                    // after segStart.
//                    segLimit = len;
//                    if (filt != null) {
//                        segLimit = segStart;
//                        while (segLimit < len && filt.contains(buf[segLimit])) {
//                             ++segLimit;
//                        }
//                    }
//
//                    // Transform the unfiltered chars between segStart
//                    // and segLimit.
//                    int segLen = segLimit - segStart;
//                    if (segLen != 0) {
//                        String newStr = transform(
//                            new String(buf, segStart, segLen));
//                        text.replace(start, start + segLen, newStr);
//                        start += newStr.length();
//                        lenDelta += newStr.length() - segLen;
//                    }
//
//                    // Set segStart to the first unfiltered char at or
//                    // after segLimit.
//                    segStart = segLimit;
//                    if (filt != null) {
//                        while (segStart < len && !filt.contains(buf[segStart])) {
//                            ++segStart;
//                        }
//                    }
//                    start += segStart - segLimit;
//
//                } while (segStart < len);
//                
//                offsets.limit += lenDelta;
//                offsets.contextLimit += lenDelta;
//                offsets.start = offsets.limit;
//                return;
//            }
//        }
//        // assert(start == offsets.limit);
//        offsets.start = start;
//    }
//
//    /**
//     * Subclasses must implement this method to determine whether a
//     * given character has a transform that is not equal to itself.
//     * This is approximately equivalent to <code>c !=
//     * transform(String.valueOf(c))</code>, where
//     * <code>String.valueOf(c)</code> returns a String containing the
//     * single character (not integer) <code>c</code>.  Subclasses that
//     * transform all their input can simply return <code>true</code>.
//     */
//    protected abstract boolean hasTransform(int c);
//
//    /**
//     * Subclasses must implement this method to transform a string.
//     */
//    protected abstract String transform(String s);
//}
