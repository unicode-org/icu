/*
 *******************************************************************************
 * Copyright (C) 2009, Google, International Business Machines Corporation     *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;

import com.ibm.icu.impl.UCaseProps;

/**
 * A transliterator that performs locale-sensitive toLower()
 * case mapping.
 */
class CaseFoldTransliterator extends Transliterator{

    /**
     * Package accessible ID.
     */
    static final String _ID = "Any-CaseFold";
    
    // TODO: Add variants for tr, az, lt, default = default locale

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return new CaseFoldTransliterator();
            }
        });

        Transliterator.registerSpecialInverse("CaseFold", "Upper", false);
    }

    private UCaseProps csp;
    private ReplaceableContextIterator iter;
    private StringBuffer result;

    /**
     * Constructs a transliterator.
     */

    public CaseFoldTransliterator() {
        super(_ID, null);
        try {
            csp=UCaseProps.getSingleton();
        } catch (IOException e) {
            csp=null;
        }
        iter=new ReplaceableContextIterator();
        result = new StringBuffer();
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        if(csp==null) {
            return;
        }

        if(offsets.start >= offsets.limit) {
            return;
        } 

        iter.setText(text);
        result.setLength(0);
        int c, delta;

        // Walk through original string
        // If there is a case change, modify corresponding position in replaceable

        iter.setIndex(offsets.start);
        iter.setLimit(offsets.limit);
        iter.setContextLimits(offsets.contextStart, offsets.contextLimit);
        while((c=iter.nextCaseMapCP())>=0) {
            c=csp.toFullFolding(c, result, 0); // toFullFolding(int c, StringBuffer out, int options)

            if(iter.didReachLimit() && isIncremental) {
                // the case mapping function tried to look beyond the context limit
                // wait for more input
                offsets.start=iter.getCaseMapCPStart();
                return;
            }

            /* decode the result */
            if(c<0) {
                /* c mapped to itself, no change */
                continue;
            } else if(c<=UCaseProps.MAX_STRING_LENGTH) {
                /* replace by the mapping string */
                delta=iter.replace(result.toString());
                result.setLength(0);
            } else {
                /* replace by single-code point mapping */
                delta=iter.replace(UTF16.valueOf(c));
            }

            if(delta!=0) {
                offsets.limit += delta;
                offsets.contextLimit += delta;
            }
        }
        offsets.start = offsets.limit;
    }
}
