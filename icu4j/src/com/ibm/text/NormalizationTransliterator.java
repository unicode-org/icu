/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/08/01    aliu        Creation.
**********************************************************************
*/

package com.ibm.text;
import java.util.*;

/**
 * @author Alan Liu
 * @version $RCSfile: NormalizationTransliterator.java,v $ $Revision: 1.13 $ $Date: 2001/11/29 17:27:44 $
 */
final class NormalizationTransliterator extends Transliterator {

    /**
     * The normalization mode of this transliterator.
     */
    private Normalizer.Mode mode;

    /**
     * Normalization options for this transliterator.
     */
    private int options;

    /**
     * The set of "unsafe start" characters.  These are characters
     * with cc==0 but which may interact with previous characters.  We
     * effectively consider these to be cc!=0, for our purposes.
     *
     * From http://www.macchiato.com/utc/NFUnsafeStart-3.1.1dX.txt
     *
     * TODO Update this to 4 separate sets, one for each norm. form.
     */
     
    static final UnicodeSet[] UNSAFE_STARTS = new UnicodeSet[4];
    
    static final int
        D = 0, C = 1, KD= 2, KC = 3;
    
    // TODO: Set to exact values for different NFs for more accuracy
    static {
        UNSAFE_STARTS[D] = new UnicodeSet("[\u0F73\u0F75\u0F81]", false);
        UNSAFE_STARTS[C] = new UnicodeSet("[\u09BE\u09D7\u0B3E\u0B56-\u0B57\u0BBE\u0BD7\u0CC2\u0CD5-\u0CD6"
            + "\u0D3E\u0D57\u0DCF\u0DDF\u0F73\u0F75\u0F81\u102E\u1161-\u1175\u11A8-\u11C2]", false);
        UNSAFE_STARTS[KD] = new UnicodeSet("[\u0F73\u0F75\u0F81\uFF9E-\uFF9F]", false);
        UNSAFE_STARTS[KC] = new UnicodeSet("[\u09BE\u09D7\u0B3E\u0B56-\u0B57\u0BBE\u0BD7\u0CC2\u0CD5-\u0CD6"
            + "\u0D3E\u0D57\u0DCF\u0DDF\u0F73\u0F75\u0F81\u102E\u1161-\u1175\u11A8-\u11C2\u3133\u3135-\u3136"
            + "\u313A-\u313F\u314F-\u3163\uFF9E-\uFF9F\uFFA3\uFFA5-\uFFA6\uFFAA-\uFFAF\uFFC2-\uFFC7\uFFCA-\uFFCF"
            + "\uFFD2-\uFFD7\uFFDA-\uFFDC]", false);
    }
    
    // Instance data, simply pointer to one of the above
    final UnicodeSet UNSAFE_START;
    
    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory("Any-NFC", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.COMPOSE);
            }
        });
        Transliterator.registerFactory("Any-NFD", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.DECOMP);
            }
        });
        Transliterator.registerFactory("Any-NFKC", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.COMPOSE_COMPAT);
            }
        });
        Transliterator.registerFactory("Any-NFKD", new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return NormalizationTransliterator.
                    getInstance(Normalizer.DECOMP_COMPAT);
            }
        });
        Transliterator.registerSpecialInverse("NFC", "NFD", true);
        Transliterator.registerSpecialInverse("NFKC", "NFKD", true);
    }

    /**
     * Factory method.
     */
    public static NormalizationTransliterator getInstance(Normalizer.Mode m,
                                                          int opt) {
        StringBuffer id = new StringBuffer("NF");
        int choice = 0;
        if (m.compat()) {
            id.append('K');
            choice |= KD;
        }
        if (m.compose()) {
            id.append('C');
            choice |= C;
        } else {
            id.append('D');
        }
        return new NormalizationTransliterator(id.toString(), m, choice, opt);
    }

    /**
     * Factory method.
     */
    public static NormalizationTransliterator getInstance(Normalizer.Mode m) {
        return getInstance(m, 0);
    }

    /**
     * Constructs a transliterator.
     */
    private NormalizationTransliterator(String id, Normalizer.Mode m, int startChoice,
                                        int opt) {
        super(id, null);
        mode = m;
        options = opt;
        UNSAFE_START = UNSAFE_STARTS[startChoice];
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        int start = offsets.start;
        int limit = offsets.limit;
        if (start >= limit) return;

        int overallDelta = 0;
            
        // Walk through the string looking for safe characters.
        // Whenever you hit one normalize from the start of the last
        // safe character up to just before the next safe character
        // Also, if you hit the end and we are not in incremental mode,
        // do to end.
            
        // TODO: fix for surrogates
        // TODO: add QuickCheck, so we rarely convert OK stuff
            
        int lastSafe = start; // go back to start in any event
        int cp;
        for (int i = start+1; i < limit; i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(text, i);
            if (UCharacter.getCombiningClass(cp) == 0 && !UNSAFE_START.contains(cp)) {
                int delta = convert(text, lastSafe, i);
                i += delta;
                limit += delta;
                overallDelta += delta;
                lastSafe = i;
            }
        }
        if (!isIncremental) {
            int delta = convert(text, lastSafe, limit);
            overallDelta += delta;
            lastSafe = limit + delta;
        }
        offsets.contextLimit += overallDelta;
        offsets.limit += overallDelta;
        offsets.start = lastSafe;
    }
    
    int convert(Replaceable text, int lastSafe, int limit) {        
        //System.out.println("t: " + com.ibm.util.Utility.hex(text.toString()) + ", s: " + lastSafe + ", l: " + limit);

        int len = limit - lastSafe;
        if (buffer.length < len) {
            buffer = new char[len]; // rare, and we don't care if we grow too large
        }
        text.getChars(lastSafe, limit, buffer, 0);
        String input = new String(buffer, 0, len); // TODO: fix normalizer to take char[]
        String output = Normalizer.normalize(input, mode, options);
        if (output.equals(input)) {
            return 0;
        }
        text.replace(lastSafe, limit, output);
        return output.length() - len;
    }
    
    private char buffer[] = new char[30];
    
}
