// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.text;

import java.util.Locale;

import com.ibm.icu.impl.UCaseProps;
import com.ibm.icu.lang.UCharacter;

// TODO: issues/questions
// - optimizing strategies for unstyled text: stop after number of changes or length of replacement?

/**
 * Low-level case mapping functions.
 *
 * @draft ICU 59
 * @provisional This API might change or be removed in a future release.
 */
public final class CaseMap {
    /**
     * Omit unchanged text when case-mapping with Edits.
     *
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
    public static final int OMIT_UNCHANGED_TEXT = 0x4000;

    /**
     * Lowercases a string and optionally records edits.
     * Casing is locale-dependent and context-sensitive.
     * The result may be longer or shorter than the original.
     *
     * @param locale    The locale ID.
     * @param options   Options bit set, usually 0. See {@link #OMIT_UNCHANGED_TEXT}.
     * @param src       The original string.
     * @param dest      A buffer for the result string. Must not be null.
     * @param edits     Records edits for index mapping, working with styled text,
     *                  and getting only changes (if any).
     *                  This function calls edits.reset() first. edits can be null.
     * @return dest with the result string (or only changes) appended.
     *
     * @see UCharacter#toLowerCase(Locale, String)
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
     public static <A extends Appendable> A toLower(
             Locale locale, int options, CharSequence src, A dest, Edits edits) {
         if (locale == null) {
             locale = Locale.getDefault();
         }
         int caseLocale = UCaseProps.getCaseLocale(locale);
         // TODO: remove package path
         return com.ibm.icu.impl.CaseMap.toLower(caseLocale, options, src, dest, edits);
     }

    /**
     * Uppercases a string and optionally records edits.
     * Casing is locale-dependent and context-sensitive.
     * The result may be longer or shorter than the original.
     *
     * @param locale    The locale ID.
     * @param options   Options bit set, usually 0. See {@link #OMIT_UNCHANGED_TEXT}.
     * @param src       The original string.
     * @param dest      A buffer for the result string. Must not be null.
     * @param edits     Records edits for index mapping, working with styled text,
     *                  and getting only changes (if any).
     *                  This function calls edits.reset() first. edits can be null.
     * @return dest with the result string (or only changes) appended.
     *
     * @see UCharacter#toUpperCase(Locale, String)
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
     public static <A extends Appendable> A toUpper(
             Locale locale, int options, CharSequence src, A dest, Edits edits) {
         return null;
     }

    /**
     * Titlecases a string and optionally records edits.
     * Casing is locale-dependent and context-sensitive.
     * The result may be longer or shorter than the original.
     *
     * Titlecasing uses a break iterator to find the first characters of words
     * that are to be titlecased. It titlecases those characters and lowercases
     * all others. (This can be modified with options bits.)
     *
     * @param locale    The locale ID.
     * @param options   Options bit set, usually 0. See {@link #OMIT_UNCHANGED_TEXT},
     *                  {@link UCharacter#TITLECASE_NO_LOWERCASE},
     *                  {@link UCharacter#TITLECASE_NO_BREAK_ADJUSTMENT}.
     * @param iter      A break iterator to find the first characters of words that are to be titlecased.
     *                  It is set to the source string (setText())
     *                  and used one or more times for iteration (first() and next()).
     *                  If null, then a word break iterator for the locale is used
     *                  (or something equivalent).
     * @param src       The original string.
     * @param dest      A buffer for the result string. Must not be null.
     * @param edits     Records edits for index mapping, working with styled text,
     *                  and getting only changes (if any).
     *                  This function calls edits.reset() first. edits can be null.
     * @return dest with the result string (or only changes) appended.
     *
     * @see UCharacter#toTitleCase(Locale, String, BreakIterator, int)
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
     public static <A extends Appendable> A toTitle(
             Locale locale, int options, BreakIterator iter,
             CharSequence src, A dest, Edits edits) {
         return null;
     }

    /**
     * Case-folds a string and optionally records edits.
     *
     * Case-folding is locale-independent and not context-sensitive,
     * but there is an option for whether to include or exclude mappings for dotted I
     * and dotless i that are marked with 'T' in CaseFolding.txt.
     *
     * The result may be longer or shorter than the original.
     *
     * @param options   Options bit set, usually 0. See {@link #OMIT_UNCHANGED_TEXT},
     *                  {@link UCharacter#FOLD_CASE_DEFAULT},
     *                  {@link UCharacter#FOLD_CASE_EXCLUDE_SPECIAL_I}.
     * @param src       The original string.
     * @param dest      A buffer for the result string. Must not be null.
     * @param edits     Records edits for index mapping, working with styled text,
     *                  and getting only changes (if any).
     *                  This function calls edits.reset() first. edits can be null.
     * @return dest with the result string (or only changes) appended.
     *
     * @see UCharacter#foldCase(String, int)
     * @draft ICU 59
     * @provisional This API might change or be removed in a future release.
     */
     public static <A extends Appendable> A foldCase(
             int options, CharSequence src, A dest, Edits edits) {
         return null;
     }
}
