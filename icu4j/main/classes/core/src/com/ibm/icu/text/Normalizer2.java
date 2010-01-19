/*
*******************************************************************************
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.InputStream;
import java.io.IOException;

import com.ibm.icu.text.Normalizer;

/**
 * Unicode normalization functionality for standard Unicode normalization or
 * for using custom mapping tables.
 * All instances of this class are unmodifiable/immutable.
 * <p>
 * The primary functions are to produce a normalized string and to detect whether
 * a string is already normalized.
 * The most commonly used normalization forms are those defined in
 * http://www.unicode.org/unicode/reports/tr15/
 * However, this API supports additional normalization forms for specialized purposes.
 * For example, NFKC_Casefold is provided via getInstance("nfkc_cf", COMPOSE)
 * and can be used in implementations of UTS #46.
 * <p>
 * Not only are the standard compose and decompose modes supplied,
 * but additional modes are provided as documented in the Mode enum.
 * <p>
 * Some of the functions in this class identify normalization boundaries.
 * At a normalization boundary, the portions of the string
 * before it and starting from it do not interact and can be handled independently.
 * <p>
 * The spanQuickCheckYes() stops at a normalization boundary.
 * When the goal is a normalized string, then the text before the boundary
 * can be copied, and the remainder can be processed with normalizeSecondAndAppend().
 * <p>
 * The hasBoundaryBefore(), hasBoundaryAfter() and isInert() functions test whether
 * a character is guaranteed to be at a normalization boundary,
 * regardless of context.
 * This is used for moving from one normalization boundary to the next
 * or preceding boundary, and for performing iterative normalization.
 * <p>
 * Iterative normalization is useful when only a small portion of a
 * longer string needs to be processed.
 * For example, in ICU, iterative normalization is used by the NormalizationTransliterator
 * (to avoid replacing already-normalized text) and ucol_nextSortKeyPart()
 * (to process only the substring for which sort key bytes are computed).
 * <p>
 * The set of normalization boundaries returned by these functions may not be
 * complete: There may be more boundaries that could be returned.
 * Different functions may return different boundaries.
 * @draft ICU 4.4
 * @provisional This API might change or be removed in a future release.
 * @author Markus W. Scherer
 */
public abstract class Normalizer2 {
    /**
     * Constants for normalization modes.
     * For details about standard Unicode normalization forms
     * and about the algorithms which are also used with custom mapping tables
     * see http://www.unicode.org/unicode/reports/tr15/
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    enum Mode {
        /**
         * Decomposition followed by composition.
         * Same as standard NFC when using an "nfc" instance.
         * Same as standard NFKC when using an "nfkc" instance.
         * For details about standard Unicode normalization forms
         * see http://www.unicode.org/unicode/reports/tr15/
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        COMPOSE,
        /**
         * Map, and reorder canonically.
         * Same as standard NFD when using an "nfc" instance.
         * Same as standard NFKD when using an "nfkc" instance.
         * For details about standard Unicode normalization forms
         * see http://www.unicode.org/unicode/reports/tr15/
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        DECOMPOSE,
        /**
         * "Fast C or D" form.
         * If a string is in this form, then further decomposition <i>without reordering</i>
         * would yield the same form as DECOMPOSE.
         * Text in "Fast C or D" form can be processed efficiently with data tables
         * that are "canonically closed", that is, that provide equivalent data for
         * equivalent text, without having to be fully normalized.<br>
         * Not a standard Unicode normalization form.<br>
         * Not a unique form: Different FCD strings can be canonically equivalent.<br>
         * For details see http://www.unicode.org/notes/tn5/#FCD
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        FCD,
        /**
         * Compose only contiguously.
         * Also known as "FCC" or "Fast C Contiguous".
         * The result will often but not always be in NFC.
         * The result will conform to FCD which is useful for processing.<br>
         * Not a standard Unicode normalization form.<br>
         * For details see http://www.unicode.org/notes/tn5/#FCC
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        COMPOSE_CONTIGUOUS
    };

    /**
     * Returns a Normalizer2 instance which uses the specified data file
     * (an ICU data file if data=null, or else custom binary data)
     * and which composes or decomposes text according to the specified mode.
     * Returns an unmodifiable singleton instance.
     * <ul>
     * <li>Use data=null for data files that are part of ICU's own data.
     * <li>Use name="nfc" and COMPOSE/DECOMPOSE for Unicode standard NFC/NFD.
     * <li>Use name="nfkc" and COMPOSE/DECOMPOSE for Unicode standard NFKC/NFKD.
     * <li>Use name="nfkc_cf" and COMPOSE for Unicode standard NFKC_CF=NFKC_Casefold.
     * </ul>
     * If data!=null, then the binary data is read once and cached using the provided
     * name as the key.
     * @param data the binary, big-endian normalization (.nrm file) data, or null for ICU data
     * @param name "nfc" or "nfkc" or "nfkc_cf" or name of custom data file
     * @param mode normalization mode (compose or decompose etc.)
     * @return the requested Normalizer2, if successful
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static Normalizer2 getInstance(InputStream data, String name, Mode mode) throws IOException {
        return null;
    }

    /**
     * Returns the normalized form of the source string.
     * @param src source string
     * @return normalized src
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String normalize(CharSequence src) {
        return normalize(src, new StringBuilder()).toString();
    }
    /**
     * Writes the normalized form of the source string to the destination string
     * (replacing its contents) and returns the destination string.
     * The source and destination strings must be different objects.
     * @param src source string
     * @param dest destination string; its contents is replaced with normalized src
     * @return dest
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract StringBuilder normalize(CharSequence src, StringBuilder dest);
    /**
     * Appends the normalized form of the second string to the first string
     * (merging them at the boundary) and returns the first string.
     * The result is normalized if the first string was normalized.
     * The first and second strings must be different objects.
     * @param first string, should be normalized
     * @param second string, will be normalized
     * @return first
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract StringBuilder normalizeSecondAndAppend(
            StringBuilder first, CharSequence second);
    /**
     * Appends the second string to the first string
     * (merging them at the boundary) and returns the first string.
     * The result is normalized if both the strings were normalized.
     * The first and second strings must be different objects.
     * @param first string, should be normalized
     * @param second string, should be normalized
     * @return first
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract StringBuilder append(StringBuilder first, CharSequence second);

    /**
     * Tests if the string is normalized.
     * Internally, in cases where the quickCheck() method would return "maybe"
     * (which is only possible for the two COMPOSE modes) this method
     * resolves to "yes" or "no" to provide a definitive result,
     * at the cost of doing more work in those cases.
     * @param s input string
     * @return true if s is normalized
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract boolean isNormalized(CharSequence s);

    /**
     * Tests if the string is normalized.
     * For the two COMPOSE modes, the result could be "maybe" in cases that
     * would take a little more work to resolve definitively.
     * Use spanQuickCheckYes() and normalizeSecondAndAppend() for a faster
     * combination of quick check + normalization, to avoid
     * re-checking the "yes" prefix.
     * @param s input string
     * @return the quick check result
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Normalizer.QuickCheckResult quickCheck(CharSequence s);

    /**
     * Returns the end of the normalized substring of the input string.
     * In other words, with <code>end=spanQuickCheckYes(s);</code>
     * the substring <code>s.subSequence(0, end)</code>
     * will pass the quick check with a "yes" result.
     * <p>
     * The returned end index is usually one or more characters before the
     * "no" or "maybe" character: The end index is at a normalization boundary.
     * (See the class documentation for more about normalization boundaries.)
     * <p>
     * When the goal is a normalized string and most input strings are expected
     * to be normalized already, then call this method,
     * and if it returns a prefix shorter than the input string,
     * copy that prefix and use normalizeSecondAndAppend() for the remainder.
     * @param s input string
     * @return "yes" span end index
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract int spanQuickCheckYes(CharSequence s);

    /**
     * Tests if the character always has a normalization boundary before it,
     * regardless of context.
     * If true, then the character does not normalization-interact with
     * preceding characters.
     * In other words, a string containing this character can be normalized
     * by processing portions before this character and starting from this
     * character independently.
     * This is used for iterative normalization. See the class documentation for details.
     * @param c character to test
     * @return true if c has a normalization boundary before it
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract boolean hasBoundaryBefore(int c);

    /**
     * Tests if the character always has a normalization boundary after it,
     * regardless of context.
     * If true, then the character does not normalization-interact with
     * following characters.
     * In other words, a string containing this character can be normalized
     * by processing portions up to this character and after this
     * character independently.
     * This is used for iterative normalization. See the class documentation for details.
     * <p>
     * Note that this operation may be significantly slower than hasBoundaryBefore().
     * @param c character to test
     * @return true if c has a normalization boundary after it
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract boolean hasBoundaryAfter(int c);

    /**
     * Tests if the character is normalization-inert.
     * If true, then the character does not change, nor normalization-interact with
     * preceding or following characters.
     * In other words, a string containing this character can be normalized
     * by processing portions before this character and after this
     * character independently.
     * This is used for iterative normalization. See the class documentation for details.
     * <p>
     * Note that this operation may be significantly slower than hasBoundaryBefore().
     * @param c character to test
     * @return true if c is normalization-inert
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract boolean isInert(int c);
}
