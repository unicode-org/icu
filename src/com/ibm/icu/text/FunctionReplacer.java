/*
**********************************************************************
*   Copyright (c) 2002-2003, International Business Machines Corporation
*   and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/14/2002  aliu        Creation.
**********************************************************************
*/

package com.ibm.icu.text;

/**
 * A replacer that calls a transliterator to generate its output text.
 * The input text to the transliterator is the output of another
 * UnicodeReplacer object.  That is, this replacer wraps another
 * replacer with a transliterator.
 * @author Alan Liu
 */
class FunctionReplacer implements UnicodeReplacer {

    /**
     * The transliterator.  Must not be null.
     */
    private Transliterator translit;

    /**
     * The replacer object.  This generates text that is then
     * processed by 'translit'.  Must not be null.
     */
    private UnicodeReplacer replacer;

    /**
     * Construct a replacer that takes the output of the given
     * replacer, passes it through the given transliterator, and emits
     * the result as output.
     */
    public FunctionReplacer(Transliterator theTranslit,
                            UnicodeReplacer theReplacer) {
        translit = theTranslit;
        replacer = theReplacer;
    }

    /**
     * UnicodeReplacer API
     */
    public int replace(Replaceable text,
                       int start,
                       int limit,
                       int[] cursor) {

        // First delegate to subordinate replacer
        int len = replacer.replace(text, start, limit, cursor);
        limit = start + len;

        // Now transliterate
        limit = translit.transliterate(text, start, limit);

        return limit - start;
    }

    /**
     * UnicodeReplacer API
     */
    public String toReplacerPattern(boolean escapeUnprintable) {
        StringBuffer rule = new StringBuffer("&");
        rule.append(translit.getID());
        rule.append("( ");
        rule.append(replacer.toReplacerPattern(escapeUnprintable));
        rule.append(" )");
        return rule.toString();
    }

    /**
     * Union the set of all characters that may output by this object
     * into the given set.
     * @param toUnionTo the set into which to union the output characters
     */
    public void addReplacementSetTo(UnicodeSet toUnionTo) {
        toUnionTo.addAll(translit.getTargetSet());
    }
}

//eof
