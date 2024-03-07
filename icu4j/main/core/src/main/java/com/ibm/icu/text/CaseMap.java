// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import java.util.Locale;

import com.ibm.icu.impl.CaseMapImpl;
import com.ibm.icu.impl.UCaseProps;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;

/**
 * Low-level case mapping options and methods. Immutable.
 * "Setters" return instances with the union of the current and new options set.
 *
 * This class is not intended for public subclassing.
 *
 * @stable ICU 59
 */
public abstract class CaseMap {
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected int internalOptions;

    private CaseMap(int opt) { internalOptions = opt; }

    private static int getCaseLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return UCaseProps.getCaseLocale(locale);
    }

    /**
     * @return Lowercasing object with default options.
     * @stable ICU 59
     */
    public static Lower toLower() { return Lower.DEFAULT; }
    /**
     * @return Uppercasing object with default options.
     * @stable ICU 59
     */
    public static Upper toUpper() { return Upper.DEFAULT; }
    /**
     * @return Titlecasing object with default options.
     * @stable ICU 59
     */
    public static Title toTitle() { return Title.DEFAULT; }
    /**
     * @return Case folding object with default options.
     * @stable ICU 59
     */
    public static Fold fold() { return Fold.DEFAULT; }

    /**
     * Returns an instance that behaves like this one but
     * omits unchanged text when case-mapping with {@link Edits}.
     *
     * @return an options object with this option.
     * @stable ICU 59
     */
    public abstract CaseMap omitUnchangedText();

    /**
     * Lowercasing options and methods. Immutable.
     *
     * @see #toLower()
     * @stable ICU 59
     */
    public static final class Lower extends CaseMap {
        private static final Lower DEFAULT = new Lower(0);
        private static final Lower OMIT_UNCHANGED = new Lower(CaseMapImpl.OMIT_UNCHANGED_TEXT);
        private Lower(int opt) { super(opt); }

        /**
         * {@inheritDoc}
         * @stable ICU 59
         */
        @Override
        public Lower omitUnchangedText() {
            return OMIT_UNCHANGED;
        }

        /**
         * Lowercases a string.
         * Casing is locale-dependent and context-sensitive.
         * The result may be longer or shorter than the original.
         *
         * @param locale    The locale ID. Can be null for {@link Locale#getDefault}.
         *                  (See {@link ULocale#toLocale}.)
         * @param src       The original string.
         * @return the result string.
         *
         * @see UCharacter#toLowerCase(Locale, String)
         * @stable ICU 60
         */
        public String apply(Locale locale, CharSequence src) {
            return CaseMapImpl.toLower(getCaseLocale(locale), internalOptions, src);
        }

        /**
         * Lowercases a string and optionally records edits (see {@link #omitUnchangedText}).
         * Casing is locale-dependent and context-sensitive.
         * The result may be longer or shorter than the original.
         *
         * @param locale    The locale ID. Can be null for {@link Locale#getDefault}.
         *                  (See {@link ULocale#toLocale}.)
         * @param src       The original string.
         * @param dest      A buffer for the result string. Must not be null.
         * @param edits     Records edits for index mapping, working with styled text,
         *                  and getting only changes (if any).
         *                  This function calls edits.reset() first. edits can be null.
         * @return dest with the result string (or only changes) appended.
         *
         * @see UCharacter#toLowerCase(Locale, String)
         * @stable ICU 59
         */
         public <A extends Appendable> A apply(
                 Locale locale, CharSequence src, A dest, Edits edits) {
             return CaseMapImpl.toLower(getCaseLocale(locale), internalOptions, src, dest, edits);
         }
    }

    /**
     * Uppercasing options and methods. Immutable.
     *
     * @see #toUpper()
     * @stable ICU 59
     */
    public static final class Upper extends CaseMap {
        private static final Upper DEFAULT = new Upper(0);
        private static final Upper OMIT_UNCHANGED = new Upper(CaseMapImpl.OMIT_UNCHANGED_TEXT);
        private Upper(int opt) { super(opt); }

        /**
         * {@inheritDoc}
         * @stable ICU 59
         */
        @Override
        public Upper omitUnchangedText() {
            return OMIT_UNCHANGED;
        }

        /**
         * Uppercases a string.
         * Casing is locale-dependent and context-sensitive.
         * The result may be longer or shorter than the original.
         *
         * @param locale    The locale ID. Can be null for {@link Locale#getDefault}.
         *                  (See {@link ULocale#toLocale}.)
         * @param src       The original string.
         * @return the result string.
         *
         * @see UCharacter#toUpperCase(Locale, String)
         * @stable ICU 60
         */
        public String apply(Locale locale, CharSequence src) {
            return CaseMapImpl.toUpper(getCaseLocale(locale), internalOptions, src);
        }

        /**
         * Uppercases a string and optionally records edits (see {@link #omitUnchangedText}).
         * Casing is locale-dependent and context-sensitive.
         * The result may be longer or shorter than the original.
         *
         * @param locale    The locale ID. Can be null for {@link Locale#getDefault}.
         *                  (See {@link ULocale#toLocale}.)
         * @param src       The original string.
         * @param dest      A buffer for the result string. Must not be null.
         * @param edits     Records edits for index mapping, working with styled text,
         *                  and getting only changes (if any).
         *                  This function calls edits.reset() first. edits can be null.
         * @return dest with the result string (or only changes) appended.
         *
         * @see UCharacter#toUpperCase(Locale, String)
         * @stable ICU 59
         */
         public <A extends Appendable> A apply(
                 Locale locale, CharSequence src, A dest, Edits edits) {
             return CaseMapImpl.toUpper(getCaseLocale(locale), internalOptions, src, dest, edits);
         }
    }

    /**
     * Titlecasing options and methods. Immutable.
     *
     * @see #toTitle()
     * @stable ICU 59
     */
    public static final class Title extends CaseMap {
        private static final Title DEFAULT = new Title(0);
        private static final Title OMIT_UNCHANGED = new Title(CaseMapImpl.OMIT_UNCHANGED_TEXT);
        private Title(int opt) { super(opt); }

        /**
         * Returns an instance that behaves like this one but
         * titlecases the string as a whole rather than each word.
         * (Titlecases only the character at index 0, possibly adjusted.)
         *
         * <p>It is an error to specify multiple titlecasing iterator options together,
         * including both an option and an explicit BreakIterator.
         *
         * @return an options object with this option.
         * @see #adjustToCased()
         * @stable ICU 60
         */
        public Title wholeString() {
            return new Title(CaseMapImpl.addTitleIteratorOption(
                    internalOptions, CaseMapImpl.TITLECASE_WHOLE_STRING));
        }

        /**
         * Returns an instance that behaves like this one but
         * titlecases sentences rather than words.
         * (Titlecases only the first character of each sentence, possibly adjusted.)
         *
         * <p>It is an error to specify multiple titlecasing iterator options together,
         * including both an option and an explicit BreakIterator.
         *
         * @return an options object with this option.
         * @see #adjustToCased()
         * @stable ICU 60
         */
        public Title sentences() {
            return new Title(CaseMapImpl.addTitleIteratorOption(
                    internalOptions, CaseMapImpl.TITLECASE_SENTENCES));
        }

        /**
         * {@inheritDoc}
         * @stable ICU 59
         */
        @Override
        public Title omitUnchangedText() {
            if (internalOptions == 0 || internalOptions == CaseMapImpl.OMIT_UNCHANGED_TEXT) {
                return OMIT_UNCHANGED;
            }
            return new Title(internalOptions | CaseMapImpl.OMIT_UNCHANGED_TEXT);
        }

        /**
         * Returns an instance that behaves like this one but
         * does not lowercase non-initial parts of words when titlecasing.
         *
         * <p>By default, titlecasing will titlecase the character at each
         * (possibly adjusted) BreakIterator index and
         * lowercase all other characters up to the next iterator index.
         * With this option, the other characters will not be modified.
         *
         * @return an options object with this option.
         * @see UCharacter#TITLECASE_NO_LOWERCASE
         * @see #adjustToCased()
         * @stable ICU 59
         */
        public Title noLowercase() {
            return new Title(internalOptions | UCharacter.TITLECASE_NO_LOWERCASE);
        }

        /**
         * Returns an instance that behaves like this one but
         * does not adjust the titlecasing BreakIterator indexes;
         * titlecases exactly the characters at breaks from the iterator.
         *
         * <p>By default, titlecasing will take each break iterator index,
         * adjust it to the next relevant character (see {@link #adjustToCased()}),
         * and titlecase that one.
         *
         * <p>Other characters are lowercased.
         *
         * @return an options object with this option.
         * @see UCharacter#TITLECASE_NO_BREAK_ADJUSTMENT
         * @stable ICU 59
         */
        public Title noBreakAdjustment() {
            return new Title(CaseMapImpl.addTitleAdjustmentOption(
                    internalOptions, UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT));
        }

        /**
         * Returns an instance that behaves like this one but
         * adjusts each titlecasing BreakIterator index to the next cased character.
         * (See the Unicode Standard, chapter 3, Default Case Conversion, R3 toTitlecase(X).)
         *
         * <p>This used to be the default index adjustment in ICU.
         * Since ICU 60, the default index adjustment is to the next character that is
         * a letter, number, symbol, or private use code point.
         * (Uncased modifier letters are skipped.)
         * The difference in behavior is small for word titlecasing,
         * but the new adjustment is much better for whole-string and sentence titlecasing:
         * It yields "49ers" and "«丰(abc)»" instead of "49Ers" and "«丰(Abc)»".
         *
         * <p>It is an error to specify multiple titlecasing adjustment options together.
         *
         * @return an options object with this option.
         * @see #noBreakAdjustment()
         * @stable ICU 60
         */
        public Title adjustToCased() {
            return new Title(CaseMapImpl.addTitleAdjustmentOption(
                    internalOptions, CaseMapImpl.TITLECASE_ADJUST_TO_CASED));
        }

        /**
         * Titlecases a string.
         * Casing is locale-dependent and context-sensitive.
         * The result may be longer or shorter than the original.
         *
         * <p>Titlecasing uses a break iterator to find the first characters of words
         * that are to be titlecased. It titlecases those characters and lowercases
         * all others. (This can be modified with options bits.)
         *
         * @param locale    The locale ID. Can be null for {@link Locale#getDefault}.
         *                  (See {@link ULocale#toLocale}.)
         * @param iter      A break iterator to find the first characters of words that are to be titlecased.
         *                  It is set to the source string (setText())
         *                  and used one or more times for iteration (first() and next()).
         *                  If null, then a word break iterator for the locale is used
         *                  (or something equivalent).
         * @param src       The original string.
         * @return the result string.
         *
         * @see UCharacter#toUpperCase(Locale, String)
         * @stable ICU 60
         */
        public String apply(Locale locale, BreakIterator iter, CharSequence src) {
            if (iter == null && locale == null) {
                locale = Locale.getDefault();
            }
            iter = CaseMapImpl.getTitleBreakIterator(locale, internalOptions, iter);
            iter.setText(src);
            return CaseMapImpl.toTitle(getCaseLocale(locale), internalOptions, iter, src);
        }

        /**
         * Titlecases a string and optionally records edits (see {@link #omitUnchangedText}).
         * Casing is locale-dependent and context-sensitive.
         * The result may be longer or shorter than the original.
         *
         * <p>Titlecasing uses a break iterator to find the first characters of words
         * that are to be titlecased. It titlecases those characters and lowercases
         * all others. (This can be modified with options bits.)
         *
         * @param locale    The locale ID. Can be null for {@link Locale#getDefault}.
         *                  (See {@link ULocale#toLocale}.)
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
         * @stable ICU 59
         */
         public <A extends Appendable> A apply(
                 Locale locale, BreakIterator iter, CharSequence src, A dest, Edits edits) {
             if (iter == null && locale == null) {
                 locale = Locale.getDefault();
             }
             iter = CaseMapImpl.getTitleBreakIterator(locale, internalOptions, iter);
             iter.setText(src);
             return CaseMapImpl.toTitle(
                     getCaseLocale(locale), internalOptions, iter, src, dest, edits);
         }
    }

    /**
     * Case folding options and methods. Immutable.
     *
     * @see #fold()
     * @stable ICU 59
     */
    public static final class Fold extends CaseMap {
        private static final Fold DEFAULT = new Fold(0);
        private static final Fold TURKIC = new Fold(UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I);
        private static final Fold OMIT_UNCHANGED = new Fold(CaseMapImpl.OMIT_UNCHANGED_TEXT);
        private static final Fold TURKIC_OMIT_UNCHANGED = new Fold(
                UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I | CaseMapImpl.OMIT_UNCHANGED_TEXT);
        private Fold(int opt) { super(opt); }

        /**
         * {@inheritDoc}
         * @stable ICU 59
         */
        @Override
        public Fold omitUnchangedText() {
            return (internalOptions & UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I) == 0 ?
                    OMIT_UNCHANGED : TURKIC_OMIT_UNCHANGED;
        }

        /**
         * Returns an instance that behaves like this one but
         * handles dotted I and dotless i appropriately for Turkic languages (tr, az).
         *
         * <p>Uses the Unicode CaseFolding.txt mappings marked with 'T' that
         * are to be excluded for default mappings and
         * included for the Turkic-specific mappings.
         *
         * @return an options object with this option.
         * @see UCharacter#FOLD_CASE_EXCLUDE_SPECIAL_I
         * @stable ICU 59
         */
        public Fold turkic() {
            return (internalOptions & CaseMapImpl.OMIT_UNCHANGED_TEXT) == 0 ?
                    TURKIC : TURKIC_OMIT_UNCHANGED;
        }

        /**
         * Case-folds a string.
         * The result may be longer or shorter than the original.
         *
         * <p>Case-folding is locale-independent and not context-sensitive,
         * but there is an option for whether to include or exclude mappings for dotted I
         * and dotless i that are marked with 'T' in CaseFolding.txt.
         *
         * @param src       The original string.
         * @return the result string.
         *
         * @see UCharacter#foldCase(String, int)
         * @stable ICU 60
         */
        public String apply(CharSequence src) {
            return CaseMapImpl.fold(internalOptions, src);
        }

        /**
         * Case-folds a string and optionally records edits (see {@link #omitUnchangedText}).
         * The result may be longer or shorter than the original.
         *
         * <p>Case-folding is locale-independent and not context-sensitive,
         * but there is an option for whether to include or exclude mappings for dotted I
         * and dotless i that are marked with 'T' in CaseFolding.txt.
         *
         * @param src       The original string.
         * @param dest      A buffer for the result string. Must not be null.
         * @param edits     Records edits for index mapping, working with styled text,
         *                  and getting only changes (if any).
         *                  This function calls edits.reset() first. edits can be null.
         * @return dest with the result string (or only changes) appended.
         *
         * @see UCharacter#foldCase(String, int)
         * @stable ICU 59
         */
         public <A extends Appendable> A apply(CharSequence src, A dest, Edits edits) {
             return CaseMapImpl.fold(internalOptions, src, dest, edits);
         }
    }
}
