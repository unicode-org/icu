/*
 *******************************************************************************
 * Copyright (C) 2000-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.Norm2AllModes;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.VersionInfo;

import java.nio.CharBuffer;
import java.text.CharacterIterator;

/**
 * Unicode Normalization 
 *
 * <h2>Unicode normalization API</h2>
 *
 * <code>normalize</code> transforms Unicode text into an equivalent composed or
 * decomposed form, allowing for easier sorting and searching of text.
 * <code>normalize</code> supports the standard normalization forms described in
 * <a href="http://www.unicode.org/unicode/reports/tr15/" target="unicode">
 * Unicode Standard Annex #15 &mdash; Unicode Normalization Forms</a>.
 *
 * Characters with accents or other adornments can be encoded in
 * several different ways in Unicode.  For example, take the character A-acute.
 * In Unicode, this can be encoded as a single character (the
 * "composed" form):
 *
 * <pre>
 *      00C1    LATIN CAPITAL LETTER A WITH ACUTE
 * </pre>
 *
 * or as two separate characters (the "decomposed" form):
 *
 * <pre>
 *      0041    LATIN CAPITAL LETTER A
 *      0301    COMBINING ACUTE ACCENT
 * </pre>
 *
 * To a user of your program, however, both of these sequences should be
 * treated as the same "user-level" character "A with acute accent".  When you 
 * are searching or comparing text, you must ensure that these two sequences are 
 * treated equivalently.  In addition, you must handle characters with more than
 * one accent.  Sometimes the order of a character's combining accents is
 * significant, while in other cases accent sequences in different orders are
 * really equivalent.
 *
 * Similarly, the string "ffi" can be encoded as three separate letters:
 *
 * <pre>
 *      0066    LATIN SMALL LETTER F
 *      0066    LATIN SMALL LETTER F
 *      0069    LATIN SMALL LETTER I
 * </pre>
 *
 * or as the single character
 *
 * <pre>
 *      FB03    LATIN SMALL LIGATURE FFI
 * </pre>
 *
 * The ffi ligature is not a distinct semantic character, and strictly speaking
 * it shouldn't be in Unicode at all, but it was included for compatibility
 * with existing character sets that already provided it.  The Unicode standard
 * identifies such characters by giving them "compatibility" decompositions
 * into the corresponding semantic characters.  When sorting and searching, you
 * will often want to use these mappings.
 *
 * <code>normalize</code> helps solve these problems by transforming text into 
 * the canonical composed and decomposed forms as shown in the first example 
 * above. In addition, you can have it perform compatibility decompositions so 
 * that you can treat compatibility characters the same as their equivalents.
 * Finally, <code>normalize</code> rearranges accents into the proper canonical
 * order, so that you do not have to worry about accent rearrangement on your
 * own.
 *
 * Form FCD, "Fast C or D", is also designed for collation.
 * It allows to work on strings that are not necessarily normalized
 * with an algorithm (like in collation) that works under "canonical closure", 
 * i.e., it treats precomposed characters and their decomposed equivalents the 
 * same.
 *
 * It is not a normalization form because it does not provide for uniqueness of 
 * representation. Multiple strings may be canonically equivalent (their NFDs 
 * are identical) and may all conform to FCD without being identical themselves.
 *
 * The form is defined such that the "raw decomposition", the recursive 
 * canonical decomposition of each character, results in a string that is 
 * canonically ordered. This means that precomposed characters are allowed for 
 * as long as their decompositions do not need canonical reordering.
 *
 * Its advantage for a process like collation is that all NFD and most NFC texts
 * - and many unnormalized texts - already conform to FCD and do not need to be 
 * normalized (NFD) for such a process. The FCD quick check will return YES for 
 * most strings in practice.
 *
 * normalize(FCD) may be implemented with NFD.
 *
 * For more details on FCD see the collation design document:
 * http://source.icu-project.org/repos/icu/icuhtml/trunk/design/collation/ICU_collation_design.htm
 *
 * ICU collation performs either NFD or FCD normalization automatically if 
 * normalization is turned on for the collator object. Beyond collation and 
 * string search, normalized strings may be useful for string equivalence 
 * comparisons, transliteration/transcription, unique representations, etc.
 *
 * The W3C generally recommends to exchange texts in NFC.
 * Note also that most legacy character encodings use only precomposed forms and
 * often do not encode any combining marks by themselves. For conversion to such
 * character encodings the Unicode text needs to be normalized to NFC.
 * For more usage examples, see the Unicode Standard Annex.
 * @stable ICU 2.8
 */
public final class Normalizer implements Cloneable {
    // The input text and our position in it
    private UCharacterIterator  text;
    private Normalizer2         norm2;
    private Mode                mode;
    private int                 options;

    // The normalization buffer is the result of normalization
    // of the source in [currentIndex..nextIndex[ .
    private int                 currentIndex;
    private int                 nextIndex;

    // A buffer for holding intermediate results
    private StringBuilder       buffer;
    private int                 bufferPos;

    /**
     * Options bit set value to select Unicode 3.2 normalization
     * (except NormalizationCorrections).
     * At most one Unicode version can be selected at a time.
     * @stable ICU 2.6
     */
    public static final int UNICODE_3_2=0x20;

    /**
     * Constant indicating that the end of the iteration has been reached.
     * This is guaranteed to have the same value as {@link UCharacterIterator#DONE}.
     * @stable ICU 2.8
     */
    public static final int DONE = UCharacterIterator.DONE;

    /**
     * Constants for normalization modes.
     * @stable ICU 2.8
     */
    public static class Mode {
        private Mode(Normalizer2 n2) {
            normalizer2 = n2;
            uni32Normalizer2 = new FilteredNormalizer2(n2, UNI32_SET);
        }
        private final Normalizer2 getNormalizer2(int options) {
            return (options&UNICODE_3_2) != 0 ? uni32Normalizer2 : normalizer2;
        }

        /**
         * Obsolete method.
         * @stable ICU 2.6
         */
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit, 
                                UnicodeSet nx) {
            // TODO: deprecate or remove this method
            int srcLen = (srcLimit - srcStart);
            int destLen = (destLimit - destStart);
            if( srcLen > destLen ) {
                return srcLen;
            }
            System.arraycopy(src,srcStart,dest,destStart,srcLen);
            return srcLen;
        }

        /**
         * Obsolete method.
         * @stable ICU 2.6
         */
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit,
                                int options) {
            // TODO: deprecate or remove this method
            return normalize(   src, srcStart, srcLimit,
                                dest,destStart,destLimit,
                                null);
        }
        
        /**
         * Obsolete method.
         * @stable ICU 2.8
         */
        protected int getMinC() {
            return -1;  // TODO: deprecate or remove this method
        }

        /**
         * Obsolete method.
         * @stable ICU 2.8
         */
        protected int getMask() {
            return -1;  // TODO: deprecate or remove this method
        }

        /**
         * Obsolete method.
         * @stable ICU 2.8
         */
        protected IsPrevBoundary getPrevBoundary() {
            return null;  // TODO: deprecate or remove this method
        }

        /**
         * Obsolete method.
         * @stable ICU 2.8
         */
        protected IsNextBoundary getNextBoundary() {
            return null;  // TODO: deprecate or remove this method
        }

        /**
         * Obsolete method.
         * @stable ICU 2.8
         */
        protected boolean isNFSkippable(int c) {
            return true;  // TODO: deprecate or remove this method
        }
        private final Normalizer2 normalizer2;
        private final FilteredNormalizer2 uni32Normalizer2;
        private static final UnicodeSet UNI32_SET = new UnicodeSet("[:age=3.2:]").freeze();
    }

    private interface IsPrevBoundary {}  // TODO: remove when Mode.getPrevBoundary() is removed
    private interface IsNextBoundary {}  // TODO: remove when Mode.getNextBoundary() is removed

    /** 
     * No decomposition/composition.  
     * @stable ICU 2.8
     */
    public static final Mode NONE = new Mode(Norm2AllModes.NOOP_NORMALIZER2);

    /** 
     * Canonical decomposition.  
     * @stable ICU 2.8
     */
    public static final Mode NFD = new Mode(Norm2AllModes.getNFCInstanceNoIOException().decomp);

    /** 
     * Compatibility decomposition.  
     * @stable ICU 2.8
     */
    public static final Mode NFKD = new Mode(Norm2AllModes.getNFKCInstanceNoIOException().decomp);

    /** 
     * Canonical decomposition followed by canonical composition.  
     * @stable ICU 2.8
     */
    public static final Mode NFC = new Mode(Norm2AllModes.getNFCInstanceNoIOException().comp);

    /** 
     * Default normalization.  
     * @stable ICU 2.8
     */
    public static final Mode DEFAULT = NFC; 

    /** 
     * Compatibility decomposition followed by canonical composition. 
     * @stable ICU 2.8
     */
    public static final Mode NFKC =new Mode(Norm2AllModes.getNFKCInstanceNoIOException().comp);

    /** 
     * "Fast C or D" form. 
     * @stable ICU 2.8 
     */
    public static final Mode FCD = new FCDMode();

    private static final class FCDMode extends Mode{
        private FCDMode() {
            super(Norm2AllModes.getNFCInstanceNoIOException().fcd);
            Norm2AllModes.getNFCInstanceNoIOException().impl.getFCDTrie();
        }
    }

    /**
     * Null operation for use with the {@link com.ibm.icu.text.Normalizer constructors}
     * and the static {@link #normalize normalize} method.  This value tells
     * the <tt>Normalizer</tt> to do nothing but return unprocessed characters
     * from the underlying String or CharacterIterator.  If you have code which
     * requires raw text at some times and normalized text at others, you can
     * use <tt>NO_OP</tt> for the cases where you want raw text, rather
     * than having a separate code path that bypasses <tt>Normalizer</tt>
     * altogether.
     * <p>
     * @see #setMode
     * @deprecated ICU 2.8. Use Nomalizer.NONE
     * @see #NONE
     */
    public static final Mode NO_OP = NONE;

    /**
     * Canonical decomposition followed by canonical composition.  Used with the
     * {@link com.ibm.icu.text.Normalizer constructors} and the static 
     * {@link #normalize normalize} method to determine the operation to be 
     * performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical 
     * Form</a>
     * <b>C</b>.
     * <p>
     * @see #setMode
     * @deprecated ICU 2.8. Use Normalier.NFC
     * @see #NFC
     */
    public static final Mode COMPOSE = NFC;

    /**
     * Compatibility decomposition followed by canonical composition.
     * Used with the {@link com.ibm.icu.text.Normalizer constructors} and the static
     * {@link #normalize normalize} method to determine the operation to be 
     * performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical 
     * Form</a>
     * <b>KC</b>.
     * <p>
     * @see #setMode
     * @deprecated ICU 2.8. Use Normalizer.NFKC
     * @see #NFKC
     */
    public static final Mode COMPOSE_COMPAT = NFKC;

    /**
     * Canonical decomposition.  This value is passed to the
     * {@link com.ibm.icu.text.Normalizer constructors} and the static
     * {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical 
     * Form</a>
     * <b>D</b>.
     * <p>
     * @see #setMode
     * @deprecated ICU 2.8. Use Normalizer.NFD
     * @see #NFD
     */
    public static final Mode DECOMP = NFD;

    /**
     * Compatibility decomposition.  This value is passed to the
     * {@link com.ibm.icu.text.Normalizer constructors} and the static 
     * {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical 
     * Form</a>
     * <b>KD</b>.
     * <p>
     * @see #setMode
     * @deprecated ICU 2.8. Use Normalizer.NFKD
     * @see #NFKD
     */
    public static final Mode DECOMP_COMPAT = NFKD;

    /**
     * Option to disable Hangul/Jamo composition and decomposition.
     * This option applies to Korean text,
     * which can be represented either in the Jamo alphabet or in Hangul
     * characters, which are really just two or three Jamo combined
     * into one visual glyph.  Since Jamo takes up more storage space than
     * Hangul, applications that process only Hangul text may wish to turn
     * this option on when decomposing text.
     * <p>
     * The Unicode standard treates Hangul to Jamo conversion as a
     * canonical decomposition, so this option must be turned <b>off</b> if you
     * wish to transform strings into one of the standard
     * <a href="http://www.unicode.org/unicode/reports/tr15/" target="unicode">
     * Unicode Normalization Forms</a>.
     * <p>
     * @see #setOption
     * @deprecated ICU 2.8. This option is no longer supported.
     */
    public static final int IGNORE_HANGUL = 0x0001;
          
    /**
     * Result values for quickCheck().
     * For details see Unicode Technical Report 15.
     * @stable ICU 2.8
     */
    public static final class QuickCheckResult{
        //private int resultValue;
        private QuickCheckResult(int value) {
            //resultValue=value;
        }
    }
    /** 
     * Indicates that string is not in the normalized format
     * @stable ICU 2.8
     */
    public static final QuickCheckResult NO = new QuickCheckResult(0);
        
    /** 
     * Indicates that string is in the normalized format
     * @stable ICU 2.8
     */
    public static final QuickCheckResult YES = new QuickCheckResult(1);

    /** 
     * Indicates it cannot be determined if string is in the normalized 
     * format without further thorough checks.
     * @stable ICU 2.8
     */
    public static final QuickCheckResult MAYBE = new QuickCheckResult(2);
    
    /**
     * Option bit for compare:
     * Case sensitively compare the strings
     * @stable ICU 2.8
     */
    public static final int FOLD_CASE_DEFAULT =  UCharacter.FOLD_CASE_DEFAULT;
    
    /**
     * Option bit for compare:
     * Both input strings are assumed to fulfill FCD conditions.
     * @stable ICU 2.8
     */
    public static final int INPUT_IS_FCD    =      0x20000;
        
    /**
     * Option bit for compare:
     * Perform case-insensitive comparison.
     * @stable ICU 2.8
     */
    public static final int COMPARE_IGNORE_CASE  =     0x10000;
        
    /**
     * Option bit for compare:
     * Compare strings in code point order instead of code unit order.
     * @stable ICU 2.8
     */
    public static final int COMPARE_CODE_POINT_ORDER = 0x8000;
    
    /** 
     * Option value for case folding: exclude the mappings for dotted I 
     * and dotless i marked with 'I' in CaseFolding.txt. 
     * @stable ICU 2.8
     */
    public static final int FOLD_CASE_EXCLUDE_SPECIAL_I = UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I;
    
    /**
     * Lowest-order bit number of compare() options bits corresponding to
     * normalization options bits.
     *
     * The options parameter for compare() uses most bits for
     * itself and for various comparison and folding flags.
     * The most significant bits, however, are shifted down and passed on
     * to the normalization implementation.
     * (That is, from compare(..., options, ...),
     * options>>COMPARE_NORM_OPTIONS_SHIFT will be passed on to the
     * internal normalization functions.)
     *
     * @see #compare
     * @stable ICU 2.6
     */
    public static final int COMPARE_NORM_OPTIONS_SHIFT  = 20;
        
    //-------------------------------------------------------------------------
    // Iterator constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of a given string.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this object.
     * <p>
     * @param str  The string to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode The normalization mode.
     *
     * @param opt Any optional features to be enabled.
     *            Currently the only available option is {@link #UNICODE_3_2}.
     *            If you want the default behavior corresponding to one of the
     *            standard Unicode Normalization Forms, use 0 for this argument.
     * @stable ICU 2.6
     */
    public Normalizer(String str, Mode mode, int opt) {
        this.text = UCharacterIterator.getInstance(str);
        this.mode = mode; 
        this.options=opt;
        norm2 = mode.getNormalizer2(opt);
        buffer = new StringBuilder();
    }

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of the given text.
     * <p>
     * @param iter  The input text to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     *
     * @param opt Any optional features to be enabled.
     *            Currently the only available option is {@link #UNICODE_3_2}.
     *            If you want the default behavior corresponding to one of the
     *            standard Unicode Normalization Forms, use 0 for this argument.
     * @stable ICU 2.6
     */
    public Normalizer(CharacterIterator iter, Mode mode, int opt) {
        this.text = UCharacterIterator.getInstance((CharacterIterator)iter.clone());
        this.mode = mode;
        this.options = opt;
        norm2 = mode.getNormalizer2(opt);
        buffer = new StringBuilder();
    }

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of the given text.
     * <p>
     * @param iter  The input text to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     * @param options The normalization options, ORed together (0 for no options).
     * @stable ICU 2.6
     */
    public Normalizer(UCharacterIterator iter, Mode mode, int options) {
        try {
            this.text     = (UCharacterIterator)iter.clone();
            this.mode     = mode;
            this.options  = options;
            norm2 = mode.getNormalizer2(options);
            buffer = new StringBuilder();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * Clones this <tt>Normalizer</tt> object.  All properties of this
     * object are duplicated in the new object, including the cloning of any
     * {@link CharacterIterator} that was passed in to the constructor
     * or to {@link #setText(CharacterIterator) setText}.
     * However, the text storage underlying
     * the <tt>CharacterIterator</tt> is not duplicated unless the
     * iterator's <tt>clone</tt> method does so.
     * @stable ICU 2.8
     */
    public Object clone() {
        try {
            Normalizer copy = (Normalizer) super.clone();
            copy.text = (UCharacterIterator) text.clone();
            copy.mode = mode;
            copy.options = options;
            copy.norm2 = norm2;
            copy.buffer = new StringBuilder(buffer);
            copy.bufferPos = bufferPos;
            copy.currentIndex = currentIndex;
            copy.nextIndex = nextIndex;
            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    //--------------------------------------------------------------------------
    // Static Utility methods
    //--------------------------------------------------------------------------

    private static final Mode getComposeMode(boolean compat) {
        return compat ? NFKC : NFC;
    }
    private static final Mode getDecomposeMode(boolean compat) {
        return compat ? NFKD : NFD;
    }

    /**
     * Compose a string.
     * The string will be composed to according to the specified mode.
     * @param str        The string to compose.
     * @param compat     If true the string will be composed according to 
     *                    NFKC rules and if false will be composed according to 
     *                    NFC rules.
     * @return String    The composed string   
     * @stable ICU 2.8
     */            
    public static String compose(String str, boolean compat) {
        return compose(str,compat,0);           
    }
    
    /**
     * Compose a string.
     * The string will be composed to according to the specified mode.
     * @param str        The string to compose.
     * @param compat     If true the string will be composed according to 
     *                    NFKC rules and if false will be composed according to 
     *                    NFC rules.
     * @param options    The only recognized option is UNICODE_3_2
     * @return String    The composed string   
     * @stable ICU 2.6
     */            
    public static String compose(String str, boolean compat, int options) {
        return getComposeMode(compat).getNormalizer2(options).normalize(str);
    }
    
    /**
     * Compose a string.
     * The string will be composed to according to the specified mode.
     * @param source The char array to compose.
     * @param target A char buffer to receive the normalized text.
     * @param compat If true the char array will be composed according to 
     *                NFKC rules and if false will be composed according to 
     *                NFC rules.
     * @param options The normalization options, ORed together (0 for no options).
     * @return int   The total buffer size needed;if greater than length of 
     *                result, the output was truncated.
     * @exception IndexOutOfBoundsException if target.length is less than the 
     *             required length
     * @stable ICU 2.6  
     */         
    public static int compose(char[] source,char[] target, boolean compat, int options) {
        return compose(source, 0, source.length, target, 0, target.length, compat, options);
    }
    
    /**
     * Compose a string.
     * The string will be composed to according to the specified mode.
     * @param src       The char array to compose.
     * @param srcStart  Start index of the source
     * @param srcLimit  Limit index of the source
     * @param dest      The char buffer to fill in
     * @param destStart Start index of the destination buffer  
     * @param destLimit End index of the destination buffer
     * @param compat If true the char array will be composed according to 
     *                NFKC rules and if false will be composed according to 
     *                NFC rules.
     * @param options The normalization options, ORed together (0 for no options).
     * @return int   The total buffer size needed;if greater than length of 
     *                result, the output was truncated.
     * @exception IndexOutOfBoundsException if target.length is less than the 
     *             required length 
     * @stable ICU 2.6 
     */         
    public static int compose(char[] src,int srcStart, int srcLimit,
                              char[] dest,int destStart, int destLimit,
                              boolean compat, int options) {
        CharBuffer srcBuffer = CharBuffer.wrap(src, srcStart, srcLimit - srcStart);
        CharsAppendable app = new CharsAppendable(dest, destStart, destLimit);
        getComposeMode(compat).getNormalizer2(options).normalize(srcBuffer, app);
        return app.length();
    }

    /**
     * Decompose a string.
     * The string will be decomposed to according to the specified mode.
     * @param str       The string to decompose.
     * @param compat    If true the string will be decomposed according to NFKD 
     *                   rules and if false will be decomposed according to NFD 
     *                   rules.
     * @return String   The decomposed string  
     * @stable ICU 2.8 
     */         
    public static String decompose(String str, boolean compat) {
        return decompose(str,compat,0);                  
    }
    
    /**
     * Decompose a string.
     * The string will be decomposed to according to the specified mode.
     * @param str     The string to decompose.
     * @param compat  If true the string will be decomposed according to NFKD 
     *                 rules and if false will be decomposed according to NFD 
     *                 rules.
     * @param options The normalization options, ORed together (0 for no options).
     * @return String The decomposed string 
     * @stable ICU 2.6
     */         
    public static String decompose(String str, boolean compat, int options) {
        return getDecomposeMode(compat).getNormalizer2(options).normalize(str);
    }

    /**
     * Decompose a string.
     * The string will be decomposed to according to the specified mode.
     * @param source The char array to decompose.
     * @param target A char buffer to receive the normalized text.
     * @param compat If true the char array will be decomposed according to NFKD 
     *                rules and if false will be decomposed according to 
     *                NFD rules.
     * @return int   The total buffer size needed;if greater than length of 
     *                result,the output was truncated.
     * @param options The normalization options, ORed together (0 for no options).
     * @exception IndexOutOfBoundsException if the target capacity is less than
     *             the required length   
     * @stable ICU 2.6
     */
    public static int decompose(char[] source,char[] target, boolean compat, int options) {
        return decompose(source, 0, source.length, target, 0, target.length, compat, options);
    }
    
    /**
     * Decompose a string.
     * The string will be decomposed to according to the specified mode.
     * @param src       The char array to compose.
     * @param srcStart  Start index of the source
     * @param srcLimit  Limit index of the source
     * @param dest      The char buffer to fill in
     * @param destStart Start index of the destination buffer  
     * @param destLimit End index of the destination buffer
     * @param compat If true the char array will be decomposed according to NFKD 
     *                rules and if false will be decomposed according to 
     *                NFD rules.
     * @param options The normalization options, ORed together (0 for no options).
     * @return int   The total buffer size needed;if greater than length of 
     *                result,the output was truncated.
     * @exception IndexOutOfBoundsException if the target capacity is less than
     *             the required length  
     * @stable ICU 2.6 
     */
    public static int decompose(char[] src,int srcStart, int srcLimit,
                                char[] dest,int destStart, int destLimit,
                                boolean compat, int options) {
        CharBuffer srcBuffer = CharBuffer.wrap(src, srcStart, srcLimit - srcStart);
        CharsAppendable app = new CharsAppendable(dest, destStart, destLimit);
        getDecomposeMode(compat).getNormalizer2(options).normalize(srcBuffer, app);
        return app.length();
    }

    /**
     * Normalizes a <tt>String</tt> using the given normalization operation.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this operation.
     * Currently the only available option is {@link #UNICODE_3_2}.
     * If you want the default behavior corresponding to one of the standard
     * Unicode Normalization Forms, use 0 for this argument.
     * <p>
     * @param str       the input string to be normalized.
     * @param mode      the normalization mode
     * @param options   the optional features to be enabled.
     * @return String   the normalized string
     * @stable ICU 2.6
     */
    public static String normalize(String str, Mode mode, int options) {
        return mode.getNormalizer2(options).normalize(str);
    }
    
    /**
     * Normalize a string.
     * The string will be normalized according to the specified normalization 
     * mode and options.
     * @param src        The string to normalize.
     * @param mode       The normalization mode; one of Normalizer.NONE, 
     *                    Normalizer.NFD, Normalizer.NFC, Normalizer.NFKC, 
     *                    Normalizer.NFKD, Normalizer.DEFAULT
     * @return the normalized string
     * @stable ICU 2.8
     *   
     */
    public static String normalize(String src,Mode mode) {
        return normalize(src, mode, 0);    
    }
    /**
     * Normalize a string.
     * The string will be normalized according to the specified normalization 
     * mode and options.
     * @param source The char array to normalize.
     * @param target A char buffer to receive the normalized text.
     * @param mode   The normalization mode; one of Normalizer.NONE, 
     *                Normalizer.NFD, Normalizer.NFC, Normalizer.NFKC, 
     *                Normalizer.NFKD, Normalizer.DEFAULT
     * @param options The normalization options, ORed together (0 for no options).
     * @return int   The total buffer size needed;if greater than length of 
     *                result, the output was truncated.
     * @exception    IndexOutOfBoundsException if the target capacity is less 
     *                than the required length
     * @stable ICU 2.6     
     */
    public static int normalize(char[] source,char[] target, Mode  mode, int options) {
        return normalize(source,0,source.length,target,0,target.length,mode, options);
    }

    /**
     * Normalize a string.
     * The string will be normalized according to the specified normalization
     * mode and options.
     * @param src       The char array to compose.
     * @param srcStart  Start index of the source
     * @param srcLimit  Limit index of the source
     * @param dest      The char buffer to fill in
     * @param destStart Start index of the destination buffer  
     * @param destLimit End index of the destination buffer
     * @param mode      The normalization mode; one of Normalizer.NONE, 
     *                   Normalizer.NFD, Normalizer.NFC, Normalizer.NFKC, 
     *                   Normalizer.NFKD, Normalizer.DEFAULT
     * @param options The normalization options, ORed together (0 for no options). 
     * @return int      The total buffer size needed;if greater than length of 
     *                   result, the output was truncated.
     * @exception       IndexOutOfBoundsException if the target capacity is 
     *                   less than the required length
     * @stable ICU 2.6    
     */       
    public static int normalize(char[] src,int srcStart, int srcLimit, 
                                char[] dest,int destStart, int destLimit,
                                Mode  mode, int options) {
        CharBuffer srcBuffer = CharBuffer.wrap(src, srcStart, srcLimit - srcStart);
        CharsAppendable app = new CharsAppendable(dest, destStart, destLimit);
        mode.getNormalizer2(options).normalize(srcBuffer, app);
        return app.length();
    }

    /**
     * Normalize a codepoint according to the given mode
     * @param char32    The input string to be normalized.
     * @param mode      The normalization mode
     * @param options   Options for use with exclusion set and tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2
     * @return String   The normalized string
     * @stable ICU 2.6
     * @see #UNICODE_3_2
     */
    public static String normalize(int char32, Mode mode, int options) {
        if(mode == NFD && options == 0) {
            String decomposition =
                Norm2AllModes.getNFCInstanceNoIOException().impl.getDecomposition(char32);
            if(decomposition == null) {
                decomposition = UTF16.valueOf(char32);
            }
            return decomposition;
        }
        return normalize(UTF16.valueOf(char32), mode, options);
    }

    /**
     * Convenience method to normalize a codepoint according to the given mode
     * @param char32    The input string to be normalized.
     * @param mode      The normalization mode
     * @return String   The normalized string
     * @stable ICU 2.6
     */
    public static String normalize(int char32, Mode mode) {
        return normalize(char32, mode, 0);
    }

    /**
     * Convenience method.
     *
     * @param source   string for determining if it is in a normalized format
     * @param mode     normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                  Normalizer.NFKC,Normalizer.NFKD)
     * @return         Return code to specify if the text is normalized or not 
     *                     (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     * @stable ICU 2.8
     */
    public static QuickCheckResult quickCheck(String source, Mode mode) {
        return quickCheck(source, mode, 0);
    }

    /**
     * Performing quick check on a string, to quickly determine if the string is 
     * in a particular normalization format.
     * Three types of result can be returned Normalizer.YES, Normalizer.NO or
     * Normalizer.MAYBE. Result Normalizer.YES indicates that the argument
     * string is in the desired normalized format, Normalizer.NO determines that
     * argument string is not in the desired normalized format. A 
     * Normalizer.MAYBE result indicates that a more thorough check is required, 
     * the user may have to put the string in its normalized form and compare 
     * the results.
     *
     * @param source   string for determining if it is in a normalized format
     * @param mode     normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                  Normalizer.NFKC,Normalizer.NFKD)
     * @param options   Options for use with exclusion set and tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2     
     * @return         Return code to specify if the text is normalized or not 
     *                     (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     * @stable ICU 2.6
     */
    public static QuickCheckResult quickCheck(String source, Mode mode, int options) {
        return mode.getNormalizer2(options).quickCheck(source);
    }

    /**
     * Convenience method.
     *
     * @param source Array of characters for determining if it is in a 
     *                normalized format
     * @param mode   normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                Normalizer.NFKC,Normalizer.NFKD)
     * @param options   Options for use with exclusion set and tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2
     * @return       Return code to specify if the text is normalized or not 
     *                (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     * @stable ICU 2.6
     */
    public static QuickCheckResult quickCheck(char[] source, Mode mode, int options) {
        return quickCheck(source, 0, source.length, mode, options);
    }

    /**
     * Performing quick check on a string, to quickly determine if the string is 
     * in a particular normalization format.
     * Three types of result can be returned Normalizer.YES, Normalizer.NO or
     * Normalizer.MAYBE. Result Normalizer.YES indicates that the argument
     * string is in the desired normalized format, Normalizer.NO determines that
     * argument string is not in the desired normalized format. A 
     * Normalizer.MAYBE result indicates that a more thorough check is required, 
     * the user may have to put the string in its normalized form and compare 
     * the results.
     *
     * @param source    string for determining if it is in a normalized format
     * @param start     the start index of the source
     * @param limit     the limit index of the source it is equal to the length
     * @param mode      normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                   Normalizer.NFKC,Normalizer.NFKD)
     * @param options   Options for use with exclusion set and tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2    
     * @return          Return code to specify if the text is normalized or not 
     *                   (Normalizer.YES, Normalizer.NO or
     *                   Normalizer.MAYBE)
     * @stable ICU 2.6
     */

    public static QuickCheckResult quickCheck(char[] source,int start, 
                                              int limit, Mode mode,int options) {       
        CharBuffer srcBuffer = CharBuffer.wrap(source, start, limit - start);
        return mode.getNormalizer2(options).quickCheck(srcBuffer);
    }

    /**
     * Test if a string is in a given normalization form.
     * This is semantically equivalent to source.equals(normalize(source, mode)).
     *
     * Unlike quickCheck(), this function returns a definitive result,
     * never a "maybe".
     * For NFD, NFKD, and FCD, both functions work exactly the same.
     * For NFC and NFKC where quickCheck may return "maybe", this function will
     * perform further tests to arrive at a true/false result.
     * @param src       The input array of characters to be checked to see if 
     *                   it is normalized
     * @param start     The strart index in the source
     * @param limit     The limit index in the source
     * @param mode      the normalization mode
     * @param options   Options for use with exclusion set and tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2    
     * @return Boolean value indicating whether the source string is in the
     *         "mode" normalization form
     * @stable ICU 2.6
     */
    public static boolean isNormalized(char[] src,int start,
                                       int limit, Mode mode, 
                                       int options) {
        CharBuffer srcBuffer = CharBuffer.wrap(src, start, limit - start);
        return mode.getNormalizer2(options).isNormalized(srcBuffer);
    }

    /**
     * Test if a string is in a given normalization form.
     * This is semantically equivalent to source.equals(normalize(source, mode)).
     *
     * Unlike quickCheck(), this function returns a definitive result,
     * never a "maybe".
     * For NFD, NFKD, and FCD, both functions work exactly the same.
     * For NFC and NFKC where quickCheck may return "maybe", this function will
     * perform further tests to arrive at a true/false result.
     * @param str       the input string to be checked to see if it is 
     *                   normalized
     * @param mode      the normalization mode
     * @param options   Options for use with exclusion set and tailored Normalization
     *                  The only option that is currently recognized is UNICODE_3_2   
     * @see #isNormalized
     * @stable ICU 2.6
     */
    public static boolean isNormalized(String str, Mode mode, int options) {
        return mode.getNormalizer2(options).isNormalized(str);
    }

    /**
     * Convenience Method
     * @param char32    the input code point to be checked to see if it is 
     *                   normalized
     * @param mode      the normalization mode
     * @param options   Options for use with exclusion set and tailored Normalization
     *                  The only option that is currently recognized is UNICODE_3_2    
     *
     * @see #isNormalized
     * @stable ICU 2.6
     */
    public static boolean isNormalized(int char32, Mode mode,int options) {
        return isNormalized(UTF16.valueOf(char32), mode, options);
    }

    /**
     * Compare two strings for canonical equivalence.
     * Further options include case-insensitive comparison and
     * code point order (as opposed to code unit order).
     *
     * Canonical equivalence between two strings is defined as their normalized
     * forms (NFD or NFC) being identical.
     * This function compares strings incrementally instead of normalizing
     * (and optionally case-folding) both strings entirely,
     * improving performance significantly.
     *
     * Bulk normalization is only necessary if the strings do not fulfill the 
     * FCD conditions. Only in this case, and only if the strings are relatively 
     * long, is memory allocated temporarily.
     * For FCD strings and short non-FCD strings there is no memory allocation.
     *
     * Semantically, this is equivalent to
     *   strcmp[CodePointOrder](foldCase(NFD(s1)), foldCase(NFD(s2)))
     * where code point order and foldCase are all optional.
     *
     * @param s1        First source character array.
     * @param s1Start   start index of source
     * @param s1Limit   limit of the source
     *
     * @param s2        Second source character array.
     * @param s2Start   start index of the source
     * @param s2Limit   limit of the source
     * 
     * @param options A bit set of options:
     *   - FOLD_CASE_DEFAULT or 0 is used for default options:
     *     Case-sensitive comparison in code unit order, and the input strings
     *     are quick-checked for FCD.
     *
     *   - INPUT_IS_FCD
     *     Set if the caller knows that both s1 and s2 fulfill the FCD 
     *     conditions.If not set, the function will quickCheck for FCD
     *     and normalize if necessary.
     *
     *   - COMPARE_CODE_POINT_ORDER
     *     Set to choose code point order instead of code unit order
     *
     *   - COMPARE_IGNORE_CASE
     *     Set to compare strings case-insensitively using case folding,
     *     instead of case-sensitively.
     *     If set, then the following case folding options are used.
     *
     *
     * @return <0 or 0 or >0 as usual for string comparisons
     *
     * @see #normalize
     * @see #FCD
     * @stable ICU 2.8
     */
    public static int compare(char[] s1, int s1Start, int s1Limit,
                              char[] s2, int s2Start, int s2Limit,
                              int options) {
        if( s1==null || s1Start<0 || s1Limit<0 || 
            s2==null || s2Start<0 || s2Limit<0 ||
            s1Limit<s1Start || s2Limit<s2Start
        ) {
            throw new IllegalArgumentException();
        }
        return internalCompare(CharBuffer.wrap(s1, s1Start, s1Limit-s1Start), 
                               CharBuffer.wrap(s2, s2Start, s2Limit-s2Start), 
                               options);
    } 

    /**
     * Compare two strings for canonical equivalence.
     * Further options include case-insensitive comparison and
     * code point order (as opposed to code unit order).
     *
     * Canonical equivalence between two strings is defined as their normalized
     * forms (NFD or NFC) being identical.
     * This function compares strings incrementally instead of normalizing
     * (and optionally case-folding) both strings entirely,
     * improving performance significantly.
     *
     * Bulk normalization is only necessary if the strings do not fulfill the 
     * FCD conditions. Only in this case, and only if the strings are relatively 
     * long, is memory allocated temporarily.
     * For FCD strings and short non-FCD strings there is no memory allocation.
     *
     * Semantically, this is equivalent to
     *   strcmp[CodePointOrder](foldCase(NFD(s1)), foldCase(NFD(s2)))
     * where code point order and foldCase are all optional.
     *
     * @param s1 First source string.
     * @param s2 Second source string.
     *
     * @param options A bit set of options:
     *   - FOLD_CASE_DEFAULT or 0 is used for default options:
     *     Case-sensitive comparison in code unit order, and the input strings
     *     are quick-checked for FCD.
     *
     *   - INPUT_IS_FCD
     *     Set if the caller knows that both s1 and s2 fulfill the FCD 
     *     conditions. If not set, the function will quickCheck for FCD
     *     and normalize if necessary.
     *
     *   - COMPARE_CODE_POINT_ORDER
     *     Set to choose code point order instead of code unit order
     *
     *   - COMPARE_IGNORE_CASE
     *     Set to compare strings case-insensitively using case folding,
     *     instead of case-sensitively.
     *     If set, then the following case folding options are used.
     *
     * @return <0 or 0 or >0 as usual for string comparisons
     *
     * @see #normalize
     * @see #FCD
     * @stable ICU 2.8
     */
    public static int compare(String s1, String s2, int options) {
        return internalCompare(s1, s2, options);
    }

    /**
     * Compare two strings for canonical equivalence.
     * Further options include case-insensitive comparison and
     * code point order (as opposed to code unit order).
     * Convenience method.
     *
     * @param s1 First source string.
     * @param s2 Second source string.
     *
     * @param options A bit set of options:
     *   - FOLD_CASE_DEFAULT or 0 is used for default options:
     *     Case-sensitive comparison in code unit order, and the input strings
     *     are quick-checked for FCD.
     *
     *   - INPUT_IS_FCD
     *     Set if the caller knows that both s1 and s2 fulfill the FCD 
     *     conditions. If not set, the function will quickCheck for FCD
     *     and normalize if necessary.
     *
     *   - COMPARE_CODE_POINT_ORDER
     *     Set to choose code point order instead of code unit order
     *
     *   - COMPARE_IGNORE_CASE
     *     Set to compare strings case-insensitively using case folding,
     *     instead of case-sensitively.
     *     If set, then the following case folding options are used.
     *
     * @return <0 or 0 or >0 as usual for string comparisons
     *
     * @see #normalize
     * @see #FCD
     * @stable ICU 2.8
     */
    public static int compare(char[] s1, char[] s2, int options) {
        return internalCompare(CharBuffer.wrap(s1), CharBuffer.wrap(s2), options);
    }

    /**
     * Convenience method that can have faster implementation
     * by not allocating buffers.
     * @param char32a    the first code point to be checked against the
     * @param char32b    the second code point
     * @param options    A bit set of options
     * @stable ICU 2.8
     */
    public static int compare(int char32a, int char32b, int options) {
        return internalCompare(UTF16.valueOf(char32a), UTF16.valueOf(char32b), options|INPUT_IS_FCD);
    }

    /**
     * Convenience method that can have faster implementation
     * by not allocating buffers.
     * @param char32a   the first code point to be checked against
     * @param str2      the second string
     * @param options   A bit set of options
     * @stable ICU 2.8
     */
    public static int compare(int char32a, String str2, int options) {
        return internalCompare(UTF16.valueOf(char32a), str2, options);
    }

    /* Concatenation of normalized strings --------------------------------- */
    /**
     * Concatenate normalized strings, making sure that the result is normalized
     * as well.
     *
     * If both the left and the right strings are in
     * the normalization form according to "mode",
     * then the result will be
     *
     * <code>
     *     dest=normalize(left+right, mode)
     * </code>
     *
     * With the input strings already being normalized,
     * this function will use next() and previous()
     * to find the adjacent end pieces of the input strings.
     * Only the concatenation of these end pieces will be normalized and
     * then concatenated with the remaining parts of the input strings.
     *
     * It is allowed to have dest==left to avoid copying the entire left string.
     *
     * @param left Left source array, may be same as dest.
     * @param leftStart start in the left array.
     * @param leftLimit limit in the left array (==length)
     * @param right Right source array.
     * @param rightStart start in the right array.
     * @param rightLimit limit in the right array (==length)
     * @param dest The output buffer; can be null if destStart==destLimit==0 
     *              for pure preflighting.
     * @param destStart start in the destination array
     * @param destLimit limit in the destination array (==length)
     * @param mode The normalization mode.
     * @param options The normalization options, ORed together (0 for no options).
     * @return Length of output (number of chars) when successful or 
     *          IndexOutOfBoundsException
     * @exception IndexOutOfBoundsException whose message has the string 
     *             representation of destination capacity required. 
     * @see #normalize
     * @see #next
     * @see #previous
     * @exception IndexOutOfBoundsException if target capacity is less than the
     *             required length
     * @stable ICU 2.8
     */
    public static int concatenate(char[] left,  int leftStart,  int leftLimit,
                                  char[] right, int rightStart, int rightLimit, 
                                  char[] dest,  int destStart,  int destLimit,
                                  Normalizer.Mode mode, int options) {
        if(dest == null) {
            throw new IllegalArgumentException();
        }
    
        /* check for overlapping right and destination */
        if (right == dest && rightStart < destLimit && destStart < rightLimit) {
            throw new IllegalArgumentException("overlapping right and dst ranges");
        }
    
        /* allow left==dest */
        StringBuilder destBuilder=new StringBuilder(leftLimit-leftStart+rightLimit-rightStart+16);
        destBuilder.append(left, leftStart, leftLimit-leftStart);
        CharBuffer rightBuffer=CharBuffer.wrap(right, rightStart, rightLimit-rightStart);
        mode.getNormalizer2(options).append(destBuilder, rightBuffer);
        int destLength=destBuilder.length();
        if(destLength<=(destLimit-destStart)) {
            destBuilder.getChars(0, destLength, dest, destStart);
            return destLength;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(destLength));
        }
    }

    /**
     * Concatenate normalized strings, making sure that the result is normalized
     * as well.
     *
     * If both the left and the right strings are in
     * the normalization form according to "mode",
     * then the result will be
     *
     * <code>
     *     dest=normalize(left+right, mode)
     * </code>
     *
     * For details see concatenate 
     *
     * @param left Left source string.
     * @param right Right source string.
     * @param mode The normalization mode.
     * @param options The normalization options, ORed together (0 for no options).
     * @return result
     *
     * @see #concatenate
     * @see #normalize
     * @see #next
     * @see #previous
     * @see #concatenate
     * @stable ICU 2.8
     */
    public static String concatenate(char[] left, char[] right,Mode mode, int options) {
        StringBuilder dest=new StringBuilder(left.length+right.length+16).append(left);
        return mode.getNormalizer2(options).append(dest, CharBuffer.wrap(right)).toString();
    }

    /**
     * Concatenate normalized strings, making sure that the result is normalized
     * as well.
     *
     * If both the left and the right strings are in
     * the normalization form according to "mode",
     * then the result will be
     *
     * <code>
     *     dest=normalize(left+right, mode)
     * </code>
     *
     * With the input strings already being normalized,
     * this function will use next() and previous()
     * to find the adjacent end pieces of the input strings.
     * Only the concatenation of these end pieces will be normalized and
     * then concatenated with the remaining parts of the input strings.
     *
     * @param left Left source string.
     * @param right Right source string.
     * @param mode The normalization mode.
     * @param options The normalization options, ORed together (0 for no options).
     * @return result
     *
     * @see #concatenate
     * @see #normalize
     * @see #next
     * @see #previous
     * @see #concatenate
     * @stable ICU 2.8
     */
    public static String concatenate(String left, String right, Mode mode, int options) {
        StringBuilder dest=new StringBuilder(left.length()+right.length()+16).append(left);
        return mode.getNormalizer2(options).append(dest, right).toString();
    }

    /**
     * Gets the FC_NFKC closure set from the normalization data
     * @param c The code point whose closure set is to be retrieved
     * @param dest The char array to receive the closure set
     * @stable ICU 3.8
     */
    public static int getFC_NFKC_Closure(int c,char[] dest) {
        return NormalizerImpl.getFC_NFKC_Closure(c,dest);
    }
    /**
     * Gets the FC_NFKC closure set from the normalization data
     * @param c The the code point whose closure set is to be retrieved
     * @return String representation of the closure set
     * @stable ICU 3.8
     */ 
    public static String getFC_NFKC_Closure(int c) {
        char[] dest = new char[10];
        for(;;) {
            int length = getFC_NFKC_Closure(c,dest);
            if(length<=dest.length) {
                return new String(dest,0,length);
            } else {
                dest = new char[length];
            }
        }
    }

    //-------------------------------------------------------------------------
    // Iteration API
    //-------------------------------------------------------------------------

    /**
     * Return the current character in the normalized text.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int current() {
        if(bufferPos<buffer.length() || nextNormalize()) {
            return buffer.codePointAt(bufferPos);
        } else {
            return DONE;
        }
    }

    /**
     * Return the next character in the normalized text and advance
     * the iteration position by one.  If the end
     * of the text has already been reached, {@link #DONE} is returned.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int next() {
        if(bufferPos<buffer.length() ||  nextNormalize()) {
            int c=buffer.codePointAt(bufferPos);
            bufferPos+=Character.charCount(c);
            return c;
        } else {
            return DONE;
        }
    }
        
        
    /**
     * Return the previous character in the normalized text and decrement
     * the iteration position by one.  If the beginning
     * of the text has already been reached, {@link #DONE} is returned.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int previous() {
        if(bufferPos>0 || previousNormalize()) {
            int c=buffer.codePointBefore(bufferPos);
            bufferPos-=Character.charCount(c);
            return c;
        } else {
            return DONE;
        }
    }
        
    /**
     * Reset the index to the beginning of the text.
     * This is equivalent to setIndexOnly(startIndex)).
     * @stable ICU 2.8
     */
    public void reset() {
        text.setToStart();
        currentIndex=nextIndex=0;
        clearBuffer();
    }
    
    /**
     * Set the iteration position in the input text that is being normalized,
     * without any immediate normalization.
     * After setIndexOnly(), getIndex() will return the same index that is
     * specified here.
     *
     * @param index the desired index in the input text.
     * @stable ICU 2.8
     */
    public void setIndexOnly(int index) {
        text.setIndex(index);  // validates index
        currentIndex=nextIndex=index;
        clearBuffer();
    }
        
    /**
     * Set the iteration position in the input text that is being normalized
     * and return the first normalized character at that position.
     * <p>
     * <b>Note:</b> This method sets the position in the <em>input</em> text,
     * while {@link #next} and {@link #previous} iterate through characters
     * in the normalized <em>output</em>.  This means that there is not
     * necessarily a one-to-one correspondence between characters returned
     * by <tt>next</tt> and <tt>previous</tt> and the indices passed to and
     * returned from <tt>setIndex</tt> and {@link #getIndex}.
     * <p>
     * @param index the desired index in the input text.
     *
     * @return   the first normalized character that is the result of iterating
     *            forward starting at the given index.
     *
     * @throws IllegalArgumentException if the given index is less than
     *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
     * @deprecated ICU 3.2
     * @obsolete ICU 3.2
     */
     ///CLOVER:OFF
     public int setIndex(int index) {
         setIndexOnly(index);
         return current();
     }
     ///CLOVER:ON
    /**
     * Retrieve the index of the start of the input text. This is the begin 
     * index of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the 
     * <tt>String</tt> over which this <tt>Normalizer</tt> is iterating
     * @deprecated ICU 2.2. Use startIndex() instead.
     * @return The codepoint as an int
     * @see #startIndex
     */
    public int getBeginIndex() {
        return 0;
    }

    /**
     * Retrieve the index of the end of the input text.  This is the end index
     * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @deprecated ICU 2.2. Use endIndex() instead.
     * @return The codepoint as an int
     * @see #endIndex
     */
    public int getEndIndex() {
        return endIndex();
    }
    /**
     * Return the first character in the normalized text.  This resets
     * the <tt>Normalizer's</tt> position to the beginning of the text.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int first() {
        reset();
        return next();
    }
        
    /**
     * Return the last character in the normalized text.  This resets
     * the <tt>Normalizer's</tt> position to be just before the
     * the input text corresponding to that normalized character.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int last() {
        text.setToLimit();
        currentIndex=nextIndex=text.getIndex();
        clearBuffer();
        return previous();
    }

    /**
     * Retrieve the current iteration position in the input text that is
     * being normalized.  This method is useful in applications such as
     * searching, where you need to be able to determine the position in
     * the input text that corresponds to a given normalized output character.
     * <p>
     * <b>Note:</b> This method sets the position in the <em>input</em>, while
     * {@link #next} and {@link #previous} iterate through characters in the
     * <em>output</em>.  This means that there is not necessarily a one-to-one
     * correspondence between characters returned by <tt>next</tt> and
     * <tt>previous</tt> and the indices passed to and returned from
     * <tt>setIndex</tt> and {@link #getIndex}.
     * @return The current iteration position
     * @stable ICU 2.8
     */
    public int getIndex() {
        if(bufferPos<buffer.length()) {
            return currentIndex;
        } else {
            return nextIndex;
        }
    }

    /**
     * Retrieve the index of the start of the input text. This is the begin 
     * index of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the 
     * <tt>String</tt> over which this <tt>Normalizer</tt> is iterating
     * @return The current iteration position
     * @stable ICU 2.8
     */
    public int startIndex() {
        return 0;
    }

    /**
     * Retrieve the index of the end of the input text.  This is the end index
     * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @return The current iteration position
     * @stable ICU 2.8
     */
    public int endIndex() {
        return text.getLength();
    }

    //-------------------------------------------------------------------------
    // Iterator attributes
    //-------------------------------------------------------------------------
    /**
     * Set the normalization mode for this object.
     * <p>
     * <b>Note:</b>If the normalization mode is changed while iterating
     * over a string, calls to {@link #next} and {@link #previous} may
     * return previously buffers characters in the old normalization mode
     * until the iteration is able to re-sync at the next base character.
     * It is safest to call {@link #setText setText()}, {@link #first},
     * {@link #last}, etc. after calling <tt>setMode</tt>.
     * <p>
     * @param newMode the new mode for this <tt>Normalizer</tt>.
     * The supported modes are:
     * <ul>
     *  <li>{@link #NFC}    - Unicode canonical decompositiion
     *                        followed by canonical composition.
     *  <li>{@link #NFKC}   - Unicode compatibility decompositiion
     *                        follwed by canonical composition.
     *  <li>{@link #NFD}    - Unicode canonical decomposition
     *  <li>{@link #NFKD}   - Unicode compatibility decomposition.
     *  <li>{@link #NONE}   - Do nothing but return characters
     *                        from the underlying input text.
     * </ul>
     *
     * @see #getMode
     * @stable ICU 2.8
     */
    public void setMode(Mode newMode) {
        mode = newMode;
        norm2 = mode.getNormalizer2(options);
    }
    /**
     * Return the basic operation performed by this <tt>Normalizer</tt>
     *
     * @see #setMode
     * @stable ICU 2.8
     */
    public Mode getMode() {
        return mode;
    }
    /**
     * Set options that affect this <tt>Normalizer</tt>'s operation.
     * Options do not change the basic composition or decomposition operation
     * that is being performed , but they control whether
     * certain optional portions of the operation are done.
     * Currently the only available option is:
     * <p>
     * <ul>
     *   <li>{@link #UNICODE_3_2} - Use Normalization conforming to Unicode version 3.2.
     * </ul>
     * <p>
     * @param   option  the option whose value is to be set.
     * @param   value   the new setting for the option.  Use <tt>true</tt> to
     *                  turn the option on and <tt>false</tt> to turn it off.
     *
     * @see #getOption
     * @stable ICU 2.6
     */
    public void setOption(int option,boolean value) {
        if (value) {
            options |= option;
        } else {
            options &= (~option);
        }
        norm2 = mode.getNormalizer2(options);
    }

    /**
     * Determine whether an option is turned on or off.
     * <p>
     * @see #setOption
     * @stable ICU 2.6
     */
    public int getOption(int option) {
        if((options & option)!=0) {
            return 1 ;
        } else {
            return 0;
        }
    }
    
    /**
     * Gets the underlying text storage
     * @param fillIn the char buffer to fill the UTF-16 units.
     *         The length of the buffer should be equal to the length of the
     *         underlying text storage
     * @throws IndexOutOfBoundsException If the index passed for the array is invalid.
     * @see   #getLength
     * @stable ICU 2.8
     */
    public int getText(char[] fillIn) {
        return text.getText(fillIn);
    }
    
    /**
     * Gets the length of underlying text storage
     * @return the length
     * @stable ICU 2.8
     */ 
    public int getLength() {
        return text.getLength();
    }
    
    /**
     * Returns the text under iteration as a string
     * @return a copy of the text under iteration.
     * @stable ICU 2.8
     */
    public String getText() {
        return text.getText();
    }
    
    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(StringBuffer newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }

    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(char[] newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }

    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(String newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }

    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(CharacterIterator newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }

    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the string.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(UCharacterIterator newText) { 
        try{
            UCharacterIterator newIter = (UCharacterIterator)newText.clone();
            if (newIter == null) {
                throw new IllegalStateException("Could not create a new UCharacterIterator");
            }
            text = newIter;
            reset();
        }catch(CloneNotSupportedException e) {
            throw new IllegalStateException("Could not clone the UCharacterIterator");
        }
    }

    private void clearBuffer() {
        buffer.setLength(0);
        bufferPos=0;
    }

    private boolean nextNormalize() {
        clearBuffer();
        currentIndex=nextIndex;
        text.setIndex(nextIndex);
        // Skip at least one character so we make progress.
        int c=text.nextCodePoint();
        if(c<0) {
            return false;
        }
        StringBuilder segment=new StringBuilder().appendCodePoint(c);
        while((c=text.nextCodePoint())>=0) {
            if(norm2.hasBoundaryBefore(c)) {
                text.moveCodePointIndex(-1);
                break;
            }
            segment.appendCodePoint(c);
        }
        nextIndex=text.getIndex();
        norm2.normalize(segment, buffer);
        return buffer.length()!=0;
    }

    private boolean previousNormalize() {
        clearBuffer();
        nextIndex=currentIndex;
        text.setIndex(currentIndex);
        StringBuilder segment=new StringBuilder();
        int c;
        while((c=text.previousCodePoint())>=0) {
            if(c<=0xffff) {
                segment.insert(0, (char)c);
            } else {
                segment.insert(0, Character.toChars(c));
            }
            if(norm2.hasBoundaryBefore(c)) {
                break;
            }
        }
        currentIndex=text.getIndex();
        norm2.normalize(segment, buffer);
        bufferPos=buffer.length();
        return buffer.length()!=0;
    }

    // TODO: Broaden the public compare(String, String, options) API like this. Ticket #7407
    private static int internalCompare(CharSequence s1, CharSequence s2, int options) {
        int normOptions=options>>>Normalizer.COMPARE_NORM_OPTIONS_SHIFT;
        options|= NormalizerImpl.COMPARE_EQUIV;

        /*
         * UAX #21 Case Mappings, as fixed for Unicode version 4
         * (see Jitterbug 2021), defines a canonical caseless match as
         *
         * A string X is a canonical caseless match
         * for a string Y if and only if
         * NFD(toCasefold(NFD(X))) = NFD(toCasefold(NFD(Y)))
         *
         * For better performance, we check for FCD (or let the caller tell us that
         * both strings are in FCD) for the inner normalization.
         * BasicNormalizerTest::FindFoldFCDExceptions() makes sure that
         * case-folding preserves the FCD-ness of a string.
         * The outer normalization is then only performed by NormalizerImpl.cmpEquivFold()
         * when there is a difference.
         *
         * Exception: When using the Turkic case-folding option, we do perform
         * full NFD first. This is because in the Turkic case precomposed characters
         * with 0049 capital I or 0069 small i fold differently whether they
         * are first decomposed or not, so an FCD check - a check only for
         * canonical order - is not sufficient.
         */
        if((options&INPUT_IS_FCD)==0 || (options&FOLD_CASE_EXCLUDE_SPECIAL_I)!=0) {
            Normalizer2 n2;
            if((options&FOLD_CASE_EXCLUDE_SPECIAL_I)!=0) {
                n2=NFD.getNormalizer2(normOptions);
            } else {
                n2=FCD.getNormalizer2(normOptions);
            }

            // check if s1 and/or s2 fulfill the FCD conditions
            int spanQCYes1=n2.spanQuickCheckYes(s1);
            int spanQCYes2=n2.spanQuickCheckYes(s2);

            /*
             * ICU 2.4 had a further optimization:
             * If both strings were not in FCD, then they were both NFD'ed,
             * and the COMPARE_EQUIV option was turned off.
             * It is not entirely clear that this is valid with the current
             * definition of the canonical caseless match.
             * Therefore, ICU 2.6 removes that optimization.
             */

            if(spanQCYes1<s1.length()) {
                StringBuilder fcd1=new StringBuilder(s1.length()+16).append(s1, 0, spanQCYes1);
                s1=n2.normalizeSecondAndAppend(fcd1, s1.subSequence(spanQCYes1, s1.length()));
            }
            if(spanQCYes2<s2.length()) {
                StringBuilder fcd2=new StringBuilder(s2.length()+16).append(s2, 0, spanQCYes2);
                s2=n2.normalizeSecondAndAppend(fcd2, s2.subSequence(spanQCYes2, s2.length()));
            }
        }

        // TODO: Temporarily hideously slow. Convert internals to work on CharSequence.
        int length1=s1.length();
        char[] s1Array=new char[length1];
        for(int i=0; i<length1; ++i) {
            s1Array[i]=s1.charAt(i);
        }
        int length2=s2.length();
        char[] s2Array=new char[length2];
        for(int i=0; i<length2; ++i) {
            s2Array[i]=s2.charAt(i);
        }
        return NormalizerImpl.cmpEquivFold(s1Array, 0, length1, s2Array, 0, length2, options);
    }    

    /**
     * Fetches the Unicode version burned into the Normalization data file
     * @return VersionInfo version information of the normalizer
     */
    static VersionInfo getUnicodeVersion() {
        return NormalizerImpl.getUnicodeVersion();
    }

    /**
     * An Appendable that writes into a char array with a capacity that may be
     * less than array.length.
     * (By contrast, CharBuffer will write beyond destLimit all the way up to array.length.)
     * <p>
     * An overflow is only reported at the end, for the old Normalizer API functions that write
     * to char arrays.
     */
    private static final class CharsAppendable implements Appendable {
        public CharsAppendable(char[] dest, int destStart, int destLimit) {
            chars=dest;
            start=offset=destStart;
            limit=destLimit;
        }
        public int length() {
            int len=offset-start;
            if(offset<=limit) {
                return len;
            } else {
                throw new IndexOutOfBoundsException(Integer.toString(len));
            }
        }
        public Appendable append(char c) {
            if(offset<limit) {
                chars[offset]=c;
            }
            ++offset;
            return this;
        }
        public Appendable append(CharSequence s) {
            return append(s, 0, s.length());
        }
        public Appendable append(CharSequence s, int sStart, int sLimit) {
            int len=sLimit-sStart;
            if(len<=(limit-offset)) {
                while(sStart<sLimit) {  // TODO: Is there a better way to copy the characters?
                    chars[offset++]=s.charAt(sStart++);
                }
            } else {
                offset+=len;
            }
            return this;
        }

        private final char[] chars;
        private final int start, limit;
        private int offset;
    }
}
