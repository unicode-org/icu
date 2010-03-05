/*
 * Copyright (C) 1996-2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 */
package com.ibm.icu.text;

import java.io.IOException;

import com.ibm.icu.impl.UCaseProps;
import com.ibm.icu.util.ULocale;

/**
 * A transliterator that converts all letters (as defined by
 * <code>UCharacter.isLetter()</code>) to lower case, except for those
 * letters preceded by non-letters.  The latter are converted to title
 * case using <code>UCharacter.toTitleCase()</code>.
 * @author Alan Liu
 */
class TitlecaseTransliterator extends Transliterator {

    static final String _ID = "Any-Title";

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return new TitlecaseTransliterator(ULocale.US);
            }
        });

        registerSpecialInverse("Title", "Lower", false);
    }

    private ULocale locale;

    private UCaseProps csp;
    private ReplaceableContextIterator iter;
    private StringBuffer result;
    private int[] locCache;

   /**
     * Constructs a transliterator.
     */
    public TitlecaseTransliterator(ULocale loc) {
        super(_ID, null);
        locale = loc;
        // Need to look back 2 characters in the case of "can't"
        setMaximumContextLength(2);
        try {
            csp=UCaseProps.getSingleton();
        } catch (IOException e) {
            csp=null;
        }
        iter=new ReplaceableContextIterator();
        result = new StringBuffer();
        locCache = new int[1];
        locCache[0]=0;
    }
     
    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        // TODO reimplement, see ustrcase.c
        // using a real word break iterator
        //   instead of just looking for a transition between cased and uncased characters
        // call CaseMapTransliterator::handleTransliterate() for lowercasing? (set fMap)
        // needs to take isIncremental into account because case mappings are context-sensitive
        //   also detect when lowercasing function did not finish because of context

        if (offsets.start >= offsets.limit) {
            return;
        }

        // case type: >0 cased (UCaseProps.LOWER etc.)  ==0 uncased  <0 case-ignorable
        int type;

        // Our mode; we are either converting letter toTitle or
        // toLower.
        boolean doTitle = true;

        // Determine if there is a preceding context of cased case-ignorable*,
        // in which case we want to start in toLower mode.  If the
        // prior context is anything else (including empty) then start
        // in toTitle mode.
        int c, start;
        for (start = offsets.start - 1; start >= offsets.contextStart; start -= UTF16.getCharCount(c)) {
            c = text.char32At(start);
            type=csp.getTypeOrIgnorable(c);
            if(type>0) { // cased
                doTitle=false;
                break;
            } else if(type==0) { // uncased but not ignorable
                break;
            }
            // else (type<0) case-ignorable: continue
        }

        // Convert things after a cased character toLower; things
        // after a uncased, non-case-ignorable character toTitle.  Case-ignorable
        // characters are copied directly and do not change the mode.

        iter.setText(text);
        iter.setIndex(offsets.start);
        iter.setLimit(offsets.limit);
        iter.setContextLimits(offsets.contextStart, offsets.contextLimit);

        result.setLength(0);

        // Walk through original string
        // If there is a case change, modify corresponding position in replaceable
        int delta;

        while((c=iter.nextCaseMapCP())>=0) {
            type=csp.getTypeOrIgnorable(c);
            if(type>=0) { // not case-ignorable
                if(doTitle) {
                    c=csp.toFullTitle(c, iter, result, locale, locCache);
                } else {
                    c=csp.toFullLower(c, iter, result, locale, locCache);
                }
                doTitle = type==0; // doTitle=isUncased

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
        }
        offsets.start = offsets.limit;
    }
}
