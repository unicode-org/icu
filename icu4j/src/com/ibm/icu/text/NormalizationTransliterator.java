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
 * @version $RCSfile: NormalizationTransliterator.java,v $ $Revision: 1.4 $ $Date: 2001/10/04 20:10:30 $
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
        Transliterator.registerFactory("Any-NFC", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.COMPOSE);
            }
        });
        Transliterator.registerFactory("Any-NFD", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.DECOMP);
            }
        });
        Transliterator.registerFactory("Any-NFKC", new Transliterator.Factory() {
            public Transliterator getInstance() {
                return NormalizationTransliterator.
                    getInstance(Normalizer.COMPOSE_COMPAT);
            }
        });
        Transliterator.registerFactory("Any-NFKD", new Transliterator.Factory() {
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
            // Wrinkle: Jamo has a combining class of zero, but we
            // don't want to normalize individual Jamo one at a time
            // if we're composing incrementally.  If we are composing
            // in incremental mode then we collect up trailing jamo
            // and save them for next time.
            boolean doStandardBackup = true;
            if (mode.compose()) {
                // As a minor optimization, if there are three or more
                // trailing jamo, we let the first three through --
                // these should be handled correctly.
                char c;
                while (limit > offsets.start &&
                       (c=text.charAt(limit-1)) >= 0x1100 &&
                       c < 0x1200) {
                    --limit;
                }
                // Characters in [limit, offsets.limit) are jamo.
                // If we have at least 3 jamo, then allow them
                // to be transliterated.  If we have zero jamo,
                // then proceed as usual.
                if (limit < offsets.limit) {
                    if ((offsets.limit - limit) >= 3) {
                        limit += 3;
                    }
                    doStandardBackup = false;
                }
            }

            if (doStandardBackup) {
                --limit;
                while (limit > start &&
                       UCharacter.getCombiningClass(text.charAt(limit)) != 0) {
                    --limit;
                }
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
