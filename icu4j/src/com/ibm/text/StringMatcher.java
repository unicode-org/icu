/*
 *******************************************************************************
 * Copyright (C) 2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/StringMatcher.java,v $ 
 * $Date: 2001/10/04 18:24:15 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

class StringMatcher implements UnicodeMatcher {

    private String pattern;

    private boolean isSegment;

    private final RuleBasedTransliterator.Data data;

    public StringMatcher(String theString,
                         int start,
                         int limit,
                         boolean isSeg,
                         RuleBasedTransliterator.Data theData) {
        data = theData;
        isSegment = isSeg;
        pattern = theString.substring(start, limit);
    }

    /**
     * Implement UnicodeMatcher
     */
    public int matches(Replaceable text,
                       int[] offset,
                       int limit,
                       boolean incremental) {
        int i;
        int[] cursor = new int[] { offset[0] };
        if (limit < cursor[0]) {
            for (i=pattern.length()-1; i>=0; --i) {
                char keyChar = pattern.charAt(i);
                UnicodeMatcher subm = data.lookup(keyChar);
                if (subm == null) {
                    if (cursor[0] >= limit &&
                        keyChar == text.charAt(cursor[0])) {
                        --cursor[0];
                    } else {
                        return U_MISMATCH;
                    }
                } else {
                    int m =
                        subm.matches(text, cursor, limit, incremental);
                    if (m != U_MATCH) {
                        return m;
                    }
                }
            }
        } else {
            for (i=0; i<pattern.length(); ++i) {
                if (incremental && cursor[0] == limit) {
                    // We've reached the context limit without a mismatch and
                    // without completing our match.
                    return U_PARTIAL_MATCH;
                }
                char keyChar = pattern.charAt(i);
                UnicodeMatcher subm = data.lookup(keyChar);
                if (subm == null) {
                    // Don't need the cursor < limit check if
                    // incremental is true (because it's done above); do need
                    // it otherwise.
                    if (cursor[0] < limit &&
                        keyChar == text.charAt(cursor[0])) {
                        ++cursor[0];
                    } else {
                        return U_MISMATCH;
                    }
                } else {
                    int m =
                        subm.matches(text, cursor, limit, incremental);
                    if (m != U_MATCH) {
                        return m;
                    }
                }
            }
        }

        offset[0] = cursor[0];
        return U_MATCH;
    }

    /**
     * Implement UnicodeMatcher
     */
    public String toPattern(boolean escapeUnprintable) {
        StringBuffer result = new StringBuffer();
        StringBuffer quoteBuf = new StringBuffer();
        if (isSegment) {
            result.append('(');
        }
        for (int i=0; i<pattern.length(); ++i) {
            char keyChar = pattern.charAt(i);
            UnicodeMatcher m = data.lookup(keyChar);
            if (m == null) {
                TransliterationRule.appendToRule(result, keyChar, false, escapeUnprintable, quoteBuf);
            } else {
                TransliterationRule.appendToRule(result, m.toPattern(escapeUnprintable),
                                                 true, escapeUnprintable, quoteBuf);
            }
        }
        if (isSegment) {
            result.append(')');
        }
        // Flush quoteBuf out to result
        TransliterationRule.appendToRule(result, (isSegment?')':-1),
                                         true, escapeUnprintable, quoteBuf);
        return result.toString();
    }

    /**
     * Implement UnicodeMatcher
     */
    public boolean matchesIndexValue(byte v) {
        if (pattern.length() == 0) {
            return true;
        }
        int c = UTF16.charAt(pattern, 0);
        UnicodeMatcher m = data.lookup(c);
        return (m == null) ? ((c & 0xFF) == v) : m.matchesIndexValue(v);
    }
}

//eof
