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
 * @version $RCSfile: NormalizationTransliterator.java,v $ $Revision: 1.10 $ $Date: 2001/11/21 20:57:08 $
 */
class NormalizationTransliterator extends Transliterator {

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
    static final UnicodeSet UNSAFE_START = new UnicodeSet("[\u09BE\u09D7\u0B3E\u0B56\u0B57\u0BBE\u0BD7\u0CC2\u0CD5-\u0CD6\u0D3E\u0D57\u0DCF\u0DDF\u0F73\u0F75\u0F81\u102E\u1161-\u1175\u11A8-\u11C2\u3133\u3135-\u3136\u313A-\u313F\u314F-\u3163\uFF9E-\uFF9F\uFFA3\uFFA5-\uFFA6\uFFAA-\uFFAF\uFFC2-\uFFC7\uFFCA-\uFFCF\uFFD2-\uFFD7\uFFDA-\uFFDC]", false);

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
        if (m.compat()) {
            id.append('K');
        }
        id.append(m.compose() ? 'C' : 'D');
        return new NormalizationTransliterator(id.toString(), m, opt);
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
    private NormalizationTransliterator(String id, Normalizer.Mode m,
                                        int opt) {
        super(id, null);
        mode = m;
        options = opt;
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        int start = offsets.start;
        int limit = offsets.limit;

        // For the non-incremental case normalize right up to
        // offsets.limit.  In the incremental case, find the last base
        // character b, and pass everything from the start up to the
        // character before b to normalizer.
        if (isIncremental) {
            --limit;
            char c;
            while (limit > start &&
                   (UCharacter.getCombiningClass(c=text.charAt(limit)) != 0 ||
                    UNSAFE_START.contains(c))) {
                --limit;
            }
        }
        
        if (limit > start) {
            char chars[] = new char[limit - start];
            text.getChars(start, limit, chars, 0);
            String input = new String(chars);
            String output = Normalizer.normalize(input, mode, options);
            text.replace(start, limit, output);

            int delta = output.length() - input.length();
            offsets.contextLimit += delta;
            offsets.limit += delta;
            offsets.start = limit + delta;
        }
    }
}
