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

/*
 * @author Alan Liu
 * @version $RCSfile: NormalizationTransliterator.java,v $ $Revision: 1.1 $ $Date: 2001/06/12 23:01:55 $
 */
public class NormalizationTransliterator extends Transliterator {

    /**
     * The normalization mode of this transliterator.
     */
    private Normalizer.Mode mode;

    /**
     * Normalization options for this transliterator.
     */
    private int options;

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory("NFC", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.COMPOSE);
            }
        });
        Transliterator.registerFactory("NFD", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.DECOMP);
            }
        });
        Transliterator.registerFactory("NFKC", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.COMPOSE_COMPAT);
            }
        });
        Transliterator.registerFactory("NFKD", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.DECOMP_COMPAT);
            }
        });
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
            while (limit > start &&
                   UCharacter.getCombiningClass(text.charAt(limit)) != 0) {
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
            offsets.start = limit;
        }
    }
}
