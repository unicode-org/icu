/*
 *******************************************************************************
 * Copyright (C) 2000-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.VersionInfo;

import java.text.CharacterIterator;
import com.ibm.icu.impl.Utility;

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
    
    //-------------------------------------------------------------------------
    // Private data
    //-------------------------------------------------------------------------  
    private char[] buffer = new char[100];
    private int bufferStart = 0;
    private int bufferPos   = 0;
    private int bufferLimit = 0;
    
    // This tells us what the bits in the "mode" object mean.
    private static final int COMPAT_BIT = 1;
    private static final int DECOMP_BIT = 2;
    private static final int COMPOSE_BIT = 4;
    
    // The input text and our position in it
    private UCharacterIterator  text;
    private Mode                mode = NFC;
    private int                 options = 0;
    private int                 currentIndex;
    private int                 nextIndex;
    
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
        //private int modeValue;
        private Mode(int value) {
            //modeValue = value;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.6
         */
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit, 
                                UnicodeSet nx) {
            int srcLen = (srcLimit - srcStart);
            int destLen = (destLimit - destStart);
            if( srcLen > destLen ) {
                return srcLen;
            }
            System.arraycopy(src,srcStart,dest,destStart,srcLen);
            return srcLen;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.6
         */
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit,
                                int options) {
            return normalize(   src, srcStart, srcLimit,
                                dest,destStart,destLimit,
                                NormalizerImpl.getNX(options)
                                );
        }
        
        /**
         * This method is used for method dispatch
         * @stable ICU 2.6
         */
        protected String normalize(String src, int options) {
            return src;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.8
         */
        protected int getMinC() {
            return -1;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.8
         */
        protected int getMask() {
            return -1;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.8
         */
        protected IsPrevBoundary getPrevBoundary() {
            return null;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.8
         */
        protected IsNextBoundary getNextBoundary() {
            return null;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.6
         */
        protected QuickCheckResult quickCheck(char[] src,int start, int limit, 
                                              boolean allowMaybe,UnicodeSet nx) {
            if(allowMaybe) {
                return MAYBE;
            }
            return NO;
        }

        /**
         * This method is used for method dispatch
         * @stable ICU 2.8
         */
        protected boolean isNFSkippable(int c) {
            return true;
        }
    }
    
    /** 
     * No decomposition/composition.  
     * @stable ICU 2.8
     */
    public static final Mode NONE = new Mode(COMPAT_BIT);

    /** 
     * Canonical decomposition.  
     * @stable ICU 2.8
     */
    public static final Mode NFD = new NFDMode(DECOMP_BIT);
    
    private static final class NFDMode extends Mode {
        private NFDMode(int value) {
            super(value);
        }

        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit, 
                                UnicodeSet nx) {
            int[] trailCC = new int[1];
            return NormalizerImpl.decompose(src,  srcStart,srcLimit,
                                            dest, destStart,destLimit,
                                            false, trailCC,nx);
        }
        
        protected String normalize( String src, int options) {
            return decompose(src,false);
        }

        protected int getMinC() {
            return NormalizerImpl.MIN_WITH_LEAD_CC;
        }

        protected IsPrevBoundary getPrevBoundary() {
            return new IsPrevNFDSafe();
        }

        protected IsNextBoundary getNextBoundary() {
            return new IsNextNFDSafe();
        }

        protected int getMask() {
            return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFD);
        }

        protected QuickCheckResult quickCheck(char[] src,int start, 
                                              int limit,boolean allowMaybe,
                                              UnicodeSet nx) {
            return NormalizerImpl.quickCheck(
                                             src, start,limit,
                                             NormalizerImpl.getFromIndexesArr(
                                                                              NormalizerImpl.INDEX_MIN_NFD_NO_MAYBE
                                                                              ),
                                             NormalizerImpl.QC_NFD,
                                             0,
                                             allowMaybe,
                                             nx
                                             );
        }

        protected boolean isNFSkippable(int c) {
            return NormalizerImpl.isNFSkippable(c,this,
                                                (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFD)
                                                );
        }           
    }
                                         
    /** 
     * Compatibility decomposition.  
     * @stable ICU 2.8
     */
    public static final Mode NFKD = new NFKDMode(3);
    
    private static final class NFKDMode extends Mode {
        private NFKDMode(int value) {
            super(value);
        }

        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit, 
                                UnicodeSet nx) {
            int[] trailCC = new int[1];
            return NormalizerImpl.decompose(src,  srcStart,srcLimit,
                                            dest, destStart,destLimit,
                                            true, trailCC, nx);
        }

        protected String normalize( String src, int options) {
            return decompose(src,true);
        }

        protected int getMinC() {
            return NormalizerImpl.MIN_WITH_LEAD_CC;
        }

        protected IsPrevBoundary getPrevBoundary() {
            return new IsPrevNFDSafe();
        }

        protected IsNextBoundary getNextBoundary() {
            return new IsNextNFDSafe();
        }

        protected int getMask() {
            return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFKD);
        }

        protected QuickCheckResult quickCheck(char[] src,int start, 
                                              int limit,boolean allowMaybe,
                                              UnicodeSet nx) {
            return NormalizerImpl.quickCheck(
                                             src,start,limit,
                                             NormalizerImpl.getFromIndexesArr(
                                                                              NormalizerImpl.INDEX_MIN_NFKD_NO_MAYBE
                                                                              ),
                                             NormalizerImpl.QC_NFKD,
                                             NormalizerImpl.OPTIONS_COMPAT,
                                             allowMaybe,
                                             nx
                                             );
        }

        protected boolean isNFSkippable(int c) {
            return NormalizerImpl.isNFSkippable(c, this,
                                                (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFKD)
                                                );
        }                                         
    }
                                         
    /** 
     * Canonical decomposition followed by canonical composition.  
     * @stable ICU 2.8
     */
    public static final Mode NFC = new NFCMode(COMPOSE_BIT);
    
    private static final class NFCMode extends Mode{
        private NFCMode(int value) {
            super(value);
        }
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit,
                                UnicodeSet nx) {
            return NormalizerImpl.compose( src, srcStart, srcLimit,
                                           dest,destStart,destLimit,
                                           0, nx);
        }
  
        protected String normalize( String src, int options) {
            return compose(src, false, options);
        }
       
        protected int getMinC() {
            return NormalizerImpl.getFromIndexesArr(
                                                    NormalizerImpl.INDEX_MIN_NFC_NO_MAYBE
                                                    );
        }
        protected IsPrevBoundary getPrevBoundary() {
            return new IsPrevTrueStarter();
        }
        protected IsNextBoundary getNextBoundary() {
            return new IsNextTrueStarter();
        }
        protected int getMask() {
            return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFC);
        }
        protected QuickCheckResult quickCheck(char[] src,int start, 
                                              int limit,boolean allowMaybe,
                                              UnicodeSet nx) {
            return NormalizerImpl.quickCheck(
                                             src,start,limit,
                                             NormalizerImpl.getFromIndexesArr(
                                                                              NormalizerImpl.INDEX_MIN_NFC_NO_MAYBE
                                                                              ),
                                             NormalizerImpl.QC_NFC,
                                             0,
                                             allowMaybe,
                                             nx
                                             );
        }
        protected boolean isNFSkippable(int c) {
            return NormalizerImpl.isNFSkippable(c,this,
                                                ( NormalizerImpl.CC_MASK|NormalizerImpl.COMBINES_ANY|
                                                  (NormalizerImpl.QC_NFC & NormalizerImpl.QC_ANY_NO)
                                                  )
                                                );
        } 
    }
                                         
    /** 
     * Default normalization.  
     * @stable ICU 2.8
     */
    public static final Mode DEFAULT = NFC; 
    
    /** 
     * Compatibility decomposition followed by canonical composition. 
     * @stable ICU 2.8
     */
    public static final Mode NFKC =new NFKCMode(5);
    
    private static final class NFKCMode extends Mode{
        private NFKCMode(int value) {
            super(value);
        }
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit, 
                                UnicodeSet nx) {
            return NormalizerImpl.compose(src,  srcStart,srcLimit,
                                          dest, destStart,destLimit,
                                          NormalizerImpl.OPTIONS_COMPAT, nx);
        }

        protected String normalize( String src, int options) {
            return compose(src, true, options);
        }
        protected int getMinC() {
            return NormalizerImpl.getFromIndexesArr(
                                                    NormalizerImpl.INDEX_MIN_NFKC_NO_MAYBE
                                                    );
        }
        protected IsPrevBoundary getPrevBoundary() {
            return new IsPrevTrueStarter();
        }
        protected IsNextBoundary getNextBoundary() {
            return new IsNextTrueStarter();
        }
        protected int getMask() {
            return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFKC);
        }
        protected QuickCheckResult quickCheck(char[] src,int start, 
                                              int limit,boolean allowMaybe,
                                              UnicodeSet nx) {
            return NormalizerImpl.quickCheck(
                                             src,start,limit,
                                             NormalizerImpl.getFromIndexesArr(
                                                                              NormalizerImpl.INDEX_MIN_NFKC_NO_MAYBE
                                                                              ),
                                             NormalizerImpl.QC_NFKC,
                                             NormalizerImpl.OPTIONS_COMPAT,
                                             allowMaybe,
                                             nx
                                             );
        }
        protected boolean isNFSkippable(int c) {
            return NormalizerImpl.isNFSkippable(c, this,
                                                ( NormalizerImpl.CC_MASK|NormalizerImpl.COMBINES_ANY|
                                                  (NormalizerImpl.QC_NFKC & NormalizerImpl.QC_ANY_NO)
                                                  )
                                                );
        } 
    }
                                        
    /** 
     * "Fast C or D" form. 
     * @stable ICU 2.8 
     */
    public static final Mode FCD = new FCDMode(6);
    
    private static final class FCDMode extends Mode{
        private FCDMode(int value) {
            super(value);
        }
        protected int normalize(char[] src, int srcStart, int srcLimit,
                                char[] dest,int destStart,int destLimit, 
                                UnicodeSet nx) {
            return NormalizerImpl.makeFCD(src, srcStart,srcLimit,
                                          dest, destStart,destLimit, nx);
        }
        protected String normalize( String src, int options) {
            return makeFCD(src, options);
        }
        protected int getMinC() {
            return NormalizerImpl.MIN_WITH_LEAD_CC;
        }
        protected IsPrevBoundary getPrevBoundary() {
            return new IsPrevNFDSafe();
        }
        protected IsNextBoundary getNextBoundary() {
            return new IsNextNFDSafe();
        }
        protected int getMask() {
            return NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFD;
        }
        protected QuickCheckResult quickCheck(char[] src,int start, 
                                              int limit,boolean allowMaybe,
                                              UnicodeSet nx) {
            return NormalizerImpl.checkFCD(src,start,limit,nx) ? YES : NO;
        }
        protected boolean isNFSkippable(int c) {
            /* FCD: skippable if lead cc==0 and trail cc<=1 */
            return (NormalizerImpl.getFCD16(c)>1);
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
    // Constructors
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
        this.text = UCharacterIterator.getInstance(
                                                   (CharacterIterator)iter.clone()
                                                   );
        this.mode = mode;
        this.options = opt;
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
            //clone the internal buffer
            if (buffer != null) {
                copy.buffer = new char[buffer.length];
                System.arraycopy(buffer,0,copy.buffer,0,buffer.length);
            }
            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e.toString());
        }
    }
    
    //--------------------------------------------------------------------------
    // Static Utility methods
    //--------------------------------------------------------------------------
    
    /**
     * Compose a string.
     * The string will be composed to according the the specified mode.
     * @param str        The string to compose.
     * @param compat     If true the string will be composed accoding to 
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
     * The string will be composed to according the the specified mode.
     * @param str        The string to compose.
     * @param compat     If true the string will be composed accoding to 
     *                    NFKC rules and if false will be composed according to 
     *                    NFC rules.
     * @param options    The only recognized option is UNICODE_3_2
     * @return String    The composed string   
     * @stable ICU 2.6
     */            
    public static String compose(String str, boolean compat, int options) {
           
        char[] dest = new char[str.length()*MAX_BUF_SIZE_COMPOSE];
        int destSize=0;
        char[] src = str.toCharArray();
        UnicodeSet nx = NormalizerImpl.getNX(options);

        /* reset options bits that should only be set here or inside compose() */
        options&=~(NormalizerImpl.OPTIONS_SETS_MASK|NormalizerImpl.OPTIONS_COMPAT|NormalizerImpl.OPTIONS_COMPOSE_CONTIGUOUS);

        if(compat) {
            options|=NormalizerImpl.OPTIONS_COMPAT;
        }

        for(;;) {
            destSize=NormalizerImpl.compose(src,0,src.length,
                                            dest,0,dest.length,options,
                                            nx);
            if(destSize<=dest.length) {
                return new String(dest,0,destSize);  
            } else {
                dest = new char[destSize];
            }
        }                   
    }
    
    /**
     * Compose a string.
     * The string will be composed to according the the specified mode.
     * @param source The char array to compose.
     * @param target A char buffer to receive the normalized text.
     * @param compat If true the char array will be composed accoding to 
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
        UnicodeSet nx = NormalizerImpl.getNX(options);

        /* reset options bits that should only be set here or inside compose() */
        options&=~(NormalizerImpl.OPTIONS_SETS_MASK|NormalizerImpl.OPTIONS_COMPAT|NormalizerImpl.OPTIONS_COMPOSE_CONTIGUOUS);

        if(compat) {
            options|=NormalizerImpl.OPTIONS_COMPAT;
        }

        int length = NormalizerImpl.compose(source,0,source.length,
                                            target,0,target.length,
                                            options,nx);
        if(length<=target.length) {
            return length;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    /**
     * Compose a string.
     * The string will be composed to according the the specified mode.
     * @param src       The char array to compose.
     * @param srcStart  Start index of the source
     * @param srcLimit  Limit index of the source
     * @param dest      The char buffer to fill in
     * @param destStart Start index of the destination buffer  
     * @param destLimit End index of the destination buffer
     * @param compat If true the char array will be composed accoding to 
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
        UnicodeSet nx = NormalizerImpl.getNX(options);

        /* reset options bits that should only be set here or inside compose() */
        options&=~(NormalizerImpl.OPTIONS_SETS_MASK|NormalizerImpl.OPTIONS_COMPAT|NormalizerImpl.OPTIONS_COMPOSE_CONTIGUOUS);

        if(compat) {
            options|=NormalizerImpl.OPTIONS_COMPAT;
        }

        int length = NormalizerImpl.compose(src,srcStart,srcLimit,
                                            dest,destStart,destLimit,
                                            options, nx);
        if(length<=(destLimit-destStart)) {
            return length;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    private static final int MAX_BUF_SIZE_COMPOSE = 2;
    private static final int MAX_BUF_SIZE_DECOMPOSE = 3;
    
    /**
     * Decompose a string.
     * The string will be decomposed to according the the specified mode.
     * @param str       The string to decompose.
     * @param compat    If true the string will be decomposed accoding to NFKD 
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
     * The string will be decomposed to according the the specified mode.
     * @param str     The string to decompose.
     * @param compat  If true the string will be decomposed accoding to NFKD 
     *                 rules and if false will be decomposed according to NFD 
     *                 rules.
     * @param options The normalization options, ORed together (0 for no options).
     * @return String The decomposed string 
     * @stable ICU 2.6
     */         
    public static String decompose(String str, boolean compat, int options) {
        
        char[] dest = new char[str.length()*MAX_BUF_SIZE_DECOMPOSE];
        int[] trailCC = new int[1];
        int destSize=0;
        UnicodeSet nx = NormalizerImpl.getNX(options);
        for(;;) {
            destSize=NormalizerImpl.decompose(str.toCharArray(),0,str.length(),
                                              dest,0,dest.length,
                                              compat,trailCC, nx);
            if(destSize<=dest.length) {
                return new String(dest,0,destSize); 
            } else {
                dest = new char[destSize];
            }
        } 
                
    }
    
    /**
     * Decompose a string.
     * The string will be decomposed to according the the specified mode.
     * @param source The char array to decompose.
     * @param target A char buffer to receive the normalized text.
     * @param compat If true the char array will be decomposed accoding to NFKD 
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
        int[] trailCC = new int[1];
        UnicodeSet nx = NormalizerImpl.getNX(options);
        int length = NormalizerImpl.decompose(source,0,source.length,
                                              target,0,target.length,
                                              compat,trailCC,nx);
        if(length<=target.length) {
            return length;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    /**
     * Decompose a string.
     * The string will be decomposed to according the the specified mode.
     * @param src       The char array to compose.
     * @param srcStart  Start index of the source
     * @param srcLimit  Limit index of the source
     * @param dest      The char buffer to fill in
     * @param destStart Start index of the destination buffer  
     * @param destLimit End index of the destination buffer
     * @param compat If true the char array will be decomposed accoding to NFKD 
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
        int[] trailCC = new int[1];
        UnicodeSet nx = NormalizerImpl.getNX(options);
        int length = NormalizerImpl.decompose(src,srcStart,srcLimit,
                                              dest,destStart,destLimit,
                                              compat,trailCC,nx);
        if(length<=(destLimit-destStart)) {
            return length;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
        
    private static String makeFCD(String src,int options) {
        int srcLen = src.length();
        char[] dest = new char[MAX_BUF_SIZE_DECOMPOSE*srcLen];
        int length = 0;
        UnicodeSet nx = NormalizerImpl.getNX(options);
        for(;;) {
            length = NormalizerImpl.makeFCD(src.toCharArray(),0,srcLen,
                                            dest,0,dest.length,nx);
            if(length <= dest.length) {
                return new String(dest,0,length);
            } else {
                dest = new char[length];
            }
        }
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
        return mode.normalize(str,options);
    }
    
    /**
     * Normalize a string.
     * The string will be normalized according the the specified normalization 
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
     * The string will be normalized according the the specified normalization 
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
        int length = normalize(source,0,source.length,target,0,target.length,mode, options);
        if(length<=target.length) {
            return length;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    /**
     * Normalize a string.
     * The string will be normalized according the the specified normalization
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
        int length = mode.normalize(src,srcStart,srcLimit,dest,destStart,destLimit, options);
       
        if(length<=(destLimit-destStart)) {
            return length;
        } else {
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    /**
     * Normalize a codepoint accoding to the given mode
     * @param char32    The input string to be normalized.
     * @param mode      The normalization mode
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2
     * @return String   The normalized string
     * @stable ICU 2.6
     * @see #UNICODE_3_2
     */
    // TODO: actually do the optimization when the guts of Normalizer are 
    // upgraded --has just dumb implementation for now
    public static String normalize(int char32, Mode mode, int options) {
        return normalize(UTF16.valueOf(char32), mode, options);
    }

    /**
     * Conveinience method to normalize a codepoint accoding to the given mode
     * @param char32    The input string to be normalized.
     * @param mode      The normalization mode
     * @return String   The normalized string
     * @see #UNICODE_3_2                
     * @stable ICU 2.6
     */
    // TODO: actually do the optimization when the guts of Normalizer are 
    // upgraded --has just dumb implementation for now
    public static String normalize(int char32, Mode mode) {
        return normalize(UTF16.valueOf(char32), mode, 0);
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
    public static QuickCheckResult quickCheck( String source, Mode mode) {
        return mode.quickCheck(source.toCharArray(),0,source.length(),true,null);
    }
    
    /**
     * Convenience method.
     *
     * @param source   string for determining if it is in a normalized format
     * @param mode     normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                  Normalizer.NFKC,Normalizer.NFKD)
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2     
     * @return         Return code to specify if the text is normalized or not 
     *                     (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     * @stable ICU 2.6
     */
    public static QuickCheckResult quickCheck( String source, Mode mode, int options) {
        return mode.quickCheck(source.toCharArray(),0,source.length(),true,NormalizerImpl.getNX(options));
    }
    
    /**
     * Convenience method.
     *
     * @param source Array of characters for determining if it is in a 
     *                normalized format
     * @param mode   normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                Normalizer.NFKC,Normalizer.NFKD)
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2
     * @return       Return code to specify if the text is normalized or not 
     *                (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     * @stable ICU 2.6
     */
    public static QuickCheckResult quickCheck(char[] source, Mode mode, int options) {
        return mode.quickCheck(source,0,source.length,true, NormalizerImpl.getNX(options));
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
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2    
     * @return          Return code to specify if the text is normalized or not 
     *                   (Normalizer.YES, Normalizer.NO or
     *                   Normalizer.MAYBE)
     * @stable ICU 2.6
     */

    public static QuickCheckResult quickCheck(char[] source,int start, 
                                              int limit, Mode mode,int options) {       
        return mode.quickCheck(source,start,limit,true,NormalizerImpl.getNX(options));
    }
    
    //-------------------------------------------------------------------------
    // Internal methods (for now)
    //-------------------------------------------------------------------------

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
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2    
     * @return Boolean value indicating whether the source string is in the
     *         "mode" normalization form
     * @stable ICU 2.6
     */
    public static boolean isNormalized(char[] src,int start,
                                       int limit, Mode mode, 
                                       int options) {
        return (mode.quickCheck(src,start,limit,false,NormalizerImpl.getNX(options))==YES);
    }
    
    /**
     * Convenience Method
     * @param str       the input string to be checked to see if it is 
     *                   normalized
     * @param mode      the normalization mode
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2   
     * @see #isNormalized
     * @stable ICU 2.6
     */
    public static boolean isNormalized(String str, Mode mode, int options) {
        return (mode.quickCheck(str.toCharArray(),0,str.length(),false,NormalizerImpl.getNX(options))==YES);
    }
    
    /**
     * Convenience Method
     * @param char32    the input code point to be checked to see if it is 
     *                   normalized
     * @param mode      the normalization mode
     * @param options   Options for use with exclusion set an tailored Normalization
     *                                   The only option that is currently recognized is UNICODE_3_2    
     *
     * @see #isNormalized
     * @stable ICU 2.6
     */
    // TODO: actually do the optimization when the guts of Normalizer are 
    // upgraded --has just dumb implementation for now
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
        return internalCompare(s1, s1Start, s1Limit, 
                               s2, s2Start, s2Limit, 
                               options);
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
    public static int compare(String s1, String s2, int options) {
         
        return compare(s1.toCharArray(),0,s1.length(),
                       s2.toCharArray(),0,s2.length(),
                       options);
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
        return compare(s1,0,s1.length,s2,0,s2.length,options);
    } 
        
    /**
     * Convenience method that can have faster implementation
     * by not allocating buffers.
     * @param char32a    the first code point to be checked against the
     * @param char32b    the second code point
     * @param options    A bit set of options
     * @stable ICU 2.8
     */
    // TODO: actually do the optimization when the guts of Normalizer are 
    // upgraded --has just dumb implementation for now
    public static int compare(int char32a, int char32b,int options) {
        return compare(UTF16.valueOf(char32a), UTF16.valueOf(char32b), options);
    }
    
    /**
     * Convenience method that can have faster implementation
     * by not allocating buffers.
     * @param char32a   the first code point to be checked against
     * @param str2      the second string
     * @param options   A bit set of options
     * @stable ICU 2.8
     */
    // TODO: actually do the optimization when the guts of Normalizer are 
    // upgraded --has just dumb implementation for now
    public static int compare(int char32a, String str2, int options) {
        return compare(UTF16.valueOf(char32a), str2, options);
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
    /* Concatenation of normalized strings --------------------------------- */
    
    public static int concatenate(char[] left,  int leftStart,  int leftLimit,
                                  char[] right, int rightStart, int rightLimit, 
                                  char[] dest,  int destStart,  int destLimit,
                                  Normalizer.Mode mode, int options) {
                               
    
        UCharacterIterator iter;
        
        int leftBoundary, rightBoundary, destLength;
    
        if(dest == null) {
            throw new IllegalArgumentException();
        }
    
        /* check for overlapping right and destination */
        if (right == dest && rightStart < destLimit && destStart < rightLimit) {
            throw new IllegalArgumentException("overlapping right and dst ranges");
        }
    
        /* allow left==dest */
    
        /*
         * Input: left[0..leftLength[ + right[0..rightLength[
         *
         * Find normalization-safe boundaries leftBoundary and rightBoundary
         * and copy the end parts together:
         * buffer=left[leftBoundary..leftLength[ + right[0..rightBoundary[
         *
         * dest=left[0..leftBoundary[ +
         *      normalize(buffer) +
         *      right[rightBoundary..rightLength[
         */
    
        /*
         * find a normalization boundary at the end of the left string
         * and copy the end part into the buffer
         */

        iter = UCharacterIterator.getInstance(left, leftStart, leftLimit);
                                             
        iter.setIndex(iter.getLength()); /* end of left string */
        char[] buffer=new char[100];
        int bufferLength;
        bufferLength=previous(iter, buffer,0,buffer.length,mode,false,null,options);
        
        leftBoundary=iter.getIndex();
        
        if(bufferLength>buffer.length) {
            char[] newBuf = new char[buffer.length*2];
            buffer = newBuf;
            newBuf = null; // null the reference for GC
            /* just copy from the left string: we know the boundary already */
            System.arraycopy(left,leftBoundary,buffer,0,bufferLength);
        }
    
        /*
         * find a normalization boundary at the beginning of the right string
         * and concatenate the beginning part to the buffer
         */

        iter = UCharacterIterator.getInstance(right, rightStart, rightLimit);
        
        rightBoundary=next(iter,buffer,bufferLength, buffer.length-bufferLength,
                           mode, false,null, options);
                           
        if(bufferLength>buffer.length) {
            char[] newBuf = new char[buffer.length*2];
            buffer = newBuf;
            newBuf = null; // null the reference for GC
            /* just copy from the right string: we know the boundary already */
            System.arraycopy(right,rightBoundary,buffer,
                             bufferLength,rightBoundary);
        }

        bufferLength+=rightBoundary;
    
        /* copy left[0..leftBoundary[ to dest */
        if(left!=dest && leftBoundary>0 && (destLimit)>0) {
            System.arraycopy(left,0,dest,0, Math.min(leftBoundary,destLimit)); 
        }
        destLength=leftBoundary;
    
        /* concatenate the normalization of the buffer to dest */
        if(destLimit>destLength) {
            destLength+=Normalizer.normalize(buffer,0,bufferLength,dest,
                                             destLength,destLimit,mode,options);
            
        } else {
            destLength+=Normalizer.normalize(buffer, 0, bufferLength,null,0,0,mode,options);
        }
    
        /* concatenate right[rightBoundary..rightLength[ to dest */
        rightStart+=rightBoundary;
        int rightLength=(rightLimit-rightStart);
        if(rightLength>0 && destLimit>destLength) {
            System.arraycopy(right,rightStart,dest,destLength,
                             Math.min(rightLength,destLength)
                             );
        }
        destLength+=rightLength;
        
        if(destLength<=(destLimit-destStart)) {
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
        char[] result = new char[(left.length+right.length)* MAX_BUF_SIZE_DECOMPOSE];
        for(;;) {
               
            int length = concatenate(left,  0, left.length,
                                     right, 0, right.length,
                                     result,0, result.length,
                                     mode, options);
            if(length<=result.length) {
                return new String(result,0,length);
            } else {
                result = new char[length];
            }
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
    public static String concatenate(String left, String right,Mode mode, int options) {
        char[] result = new char[(left.length()+right.length())* MAX_BUF_SIZE_DECOMPOSE];
        for(;;) {
               
            int length = concatenate(left.toCharArray(), 0, left.length(),
                                     right.toCharArray(),0, right.length(),
                                     result,             0, result.length,
                                     mode, options);
            if(length<=result.length) {
                return new String(result,0,length);
            } else {
                result = new char[length];
            }
        }            
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
     * Return the current character in the normalized text->
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int current() {
        if(bufferPos<bufferLimit || nextNormalize()) {
            return getCodePointAt(bufferPos);
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
        if(bufferPos<bufferLimit ||  nextNormalize()) {
            int c=getCodePointAt(bufferPos);
            bufferPos+=(c>0xFFFF) ? 2 : 1;
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
            int c=getCodePointAt(bufferPos-1);
            bufferPos-=(c>0xFFFF) ? 2 : 1;
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
        text.setIndex(0);
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
        text.setIndex(index);
        currentIndex=nextIndex=index; // validates index
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
     * @param index the desired index in the input text->
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
     * Return the first character in the normalized text->  This resets
     * the <tt>Normalizer's</tt> position to the beginning of the text->
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int first() {
        reset();
        return next();
    }
        
    /**
     * Return the last character in the normalized text->  This resets
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
        if(bufferPos<bufferLimit) {
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
     * Retrieve the index of the end of the input text->  This is the end index
     * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @return The current iteration position
     * @stable ICU 2.8
     */
    public int endIndex() {
        return text.getLength();
    }
    
    //-------------------------------------------------------------------------
    // Property access methods
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
     * The iteration position is set to the beginning of the input text->
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
     * The iteration position is set to the beginning of the input text->
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
     * The iteration position is set to the beginning of the input text->
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
     * The iteration position is set to the beginning of the input text->
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
    
    //-------------------------------------------------------------------------
    // Private utility methods
    //-------------------------------------------------------------------------
    

    /* backward iteration --------------------------------------------------- */
               
    /*
     * read backwards and get norm32
     * return 0 if the character is <minC
     * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first 
     * surrogate but read second!)
     */

    private static  long getPrevNorm32(UCharacterIterator src, 
                                       int/*unsigned*/ minC, 
                                       int/*unsigned*/ mask, 
                                       char[] chars) {
        long norm32;
        int ch=0;
        /* need src.hasPrevious() */
        if((ch=src.previous()) == UCharacterIterator.DONE) {
            return 0;
        }
        chars[0]=(char)ch;
        chars[1]=0;
    
        /* check for a surrogate before getting norm32 to see if we need to 
         * predecrement further */
        if(chars[0]<minC) {
            return 0;
        } else if(!UTF16.isSurrogate(chars[0])) {
            return NormalizerImpl.getNorm32(chars[0]);
        } else if(UTF16.isLeadSurrogate(chars[0]) || (src.getIndex()==0)) {
            /* unpaired surrogate */
            chars[1]=(char)src.current();
            return 0;
        } else if(UTF16.isLeadSurrogate(chars[1]=(char)src.previous())) {
            norm32=NormalizerImpl.getNorm32(chars[1]);
            if((norm32&mask)==0) {
                /* all surrogate pairs with this lead surrogate have irrelevant 
                 * data */
                return 0;
            } else {
                /* norm32 must be a surrogate special */
                return NormalizerImpl.getNorm32FromSurrogatePair(norm32,chars[0]);
            }
        } else {
            /* unpaired second surrogate, undo the c2=src.previous() movement */
            src.moveIndex( 1);
            return 0;
        }
    }
 
    private interface IsPrevBoundary{
        public boolean isPrevBoundary(UCharacterIterator src,
                                      int/*unsigned*/ minC, 
                                      int/*unsigned*/ mask, 
                                      char[] chars);
    }
    private static final class IsPrevNFDSafe implements IsPrevBoundary{
        /*
         * for NF*D:
         * read backwards and check if the lead combining class is 0
         * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first 
         * surrogate but read second!)
         */
        public boolean isPrevBoundary(UCharacterIterator src,
                                      int/*unsigned*/ minC, 
                                      int/*unsigned*/ ccOrQCMask, 
                                      char[] chars) {
    
            return NormalizerImpl.isNFDSafe(getPrevNorm32(src, minC, 
                                                          ccOrQCMask, chars), 
                                            ccOrQCMask, 
                                            ccOrQCMask& NormalizerImpl.QC_MASK);
        }
    }
    
    private static final class IsPrevTrueStarter implements IsPrevBoundary{
        /*
         * read backwards and check if the character is (or its decomposition 
         * begins with) a "true starter" (cc==0 and NF*C_YES)
         * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first 
         * surrogate but read second!)
         */
        public boolean isPrevBoundary(UCharacterIterator src, 
                                      int/*unsigned*/ minC,
                                      int/*unsigned*/ ccOrQCMask,
                                      char[] chars) {
            long norm32; 
            int/*unsigned*/ decompQCMask;
            
            decompQCMask=(ccOrQCMask<<2)&0xf; /*decomposition quick check mask*/
            norm32=getPrevNorm32(src, minC, ccOrQCMask|decompQCMask, chars);
            return NormalizerImpl.isTrueStarter(norm32,ccOrQCMask,decompQCMask);
        }
    }
    
    private static int findPreviousIterationBoundary(UCharacterIterator src,
                                                     IsPrevBoundary obj, 
                                                     int/*unsigned*/ minC,
                                                     int/*mask*/ mask,
                                                     char[] buffer, 
                                                     int[] startIndex) {
        char[] chars=new char[2];
        boolean isBoundary;
    
        /* fill the buffer from the end backwards */
        startIndex[0] = buffer.length;
        chars[0]=0;
        while(src.getIndex()>0 && chars[0]!=UCharacterIterator.DONE) {
            isBoundary=obj.isPrevBoundary(src, minC, mask, chars);
    
            /* always write this character to the front of the buffer */
            /* make sure there is enough space in the buffer */
            if(startIndex[0] < (chars[1]==0 ? 1 : 2)) {

                // grow the buffer
                char[] newBuf = new char[buffer.length*2];
                /* move the current buffer contents up */
                System.arraycopy(buffer,startIndex[0],newBuf,
                                 newBuf.length-(buffer.length-startIndex[0]),
                                 buffer.length-startIndex[0]);
                //adjust the startIndex
                startIndex[0]+=newBuf.length-buffer.length;
                
                buffer=newBuf;
                newBuf=null;                
                
            }
    
            buffer[--startIndex[0]]=chars[0];
            if(chars[1]!=0) {
                buffer[--startIndex[0]]=chars[1];
            }
    
            /* stop if this just-copied character is a boundary */
            if(isBoundary) {
                break;
            }
        }
    
        /* return the length of the buffer contents */
        return buffer.length-startIndex[0];
    }
    
    private static int previous(UCharacterIterator src,
                                char[] dest, int destStart, int destLimit, 
                                Mode mode, 
                                boolean doNormalize, 
                                boolean[] pNeededToNormalize,
                                int options) {

        IsPrevBoundary isPreviousBoundary;
        int destLength, bufferLength;
        int/*unsigned*/ mask;
        
        int c,c2;
        
        char minC;
        int destCapacity = destLimit-destStart;
        destLength=0;
        
        
        if(pNeededToNormalize!=null) {
            pNeededToNormalize[0]=false;
        }
        minC = (char)mode.getMinC();
        mask = mode.getMask();
        isPreviousBoundary = mode.getPrevBoundary();

        if(isPreviousBoundary==null) {
            destLength=0;
            if((c=src.previous())>=0) {
                destLength=1;
                if(UTF16.isTrailSurrogate((char)c)) {
                    c2= src.previous();
                    if(c2!= UCharacterIterator.DONE) {
                        if(UTF16.isLeadSurrogate((char)c2)) {
                            if(destCapacity>=2) {
                                dest[1]=(char)c; // trail surrogate 
                                destLength=2;
                            }
                            // lead surrogate to be written below 
                            c=c2; 
                        } else {
                            src.moveIndex(1);
                        }
                    }
                }
    
                if(destCapacity>0) {
                    dest[0]=(char)c;
                }
            }
            return destLength;
        }
        
        char[] buffer = new char[100];
        int[] startIndex= new int[1];
        bufferLength=findPreviousIterationBoundary(src,
                                                   isPreviousBoundary, 
                                                   minC, mask,buffer, 
                                                   startIndex);
        if(bufferLength>0) {
            if(doNormalize) {
                destLength=Normalizer.normalize(buffer,startIndex[0],
                                                startIndex[0]+bufferLength,
                                                dest, destStart,destLimit,
                                                mode, options);
                
                if(pNeededToNormalize!=null) {
                    pNeededToNormalize[0]=(destLength!=bufferLength ||
                                                Utility.arrayRegionMatches(
                                                                           buffer,0,dest,
                                                                           destStart,destLimit
                                                                           ));
                }
            } else {
                /* just copy the source characters */
                if(destCapacity>0) {
                    System.arraycopy(buffer,startIndex[0],dest,0,
                                     (bufferLength<destCapacity) ? 
                                     bufferLength : destCapacity);
                }
            }
        } 

    
        return destLength;
    }

 
    
    /* forward iteration ---------------------------------------------------- */
    /*
     * read forward and check if the character is a next-iteration boundary
     * if c2!=0 then (c, c2) is a surrogate pair
     */
    private interface IsNextBoundary{
        boolean isNextBoundary(UCharacterIterator src, 
                               int/*unsigned*/ minC, 
                               int/*unsigned*/ mask, 
                               int[] chars);
    }   
    /*
     * read forward and get norm32
     * return 0 if the character is <minC
     * if c2!=0 then (c2, c) is a surrogate pair
     * always reads complete characters
     */
    private static long /*unsigned*/ getNextNorm32(UCharacterIterator src, 
                                                   int/*unsigned*/ minC, 
                                                   int/*unsigned*/ mask, 
                                                   int[] chars) {
        long norm32;
    
        /* need src.hasNext() to be true */
        chars[0]=src.next();
        chars[1]=0;
    
        if(chars[0]<minC) {
            return 0;
        }
    
        norm32=NormalizerImpl.getNorm32((char)chars[0]);
        if(UTF16.isLeadSurrogate((char)chars[0])) {
            if(src.current()!=UCharacterIterator.DONE &&
               UTF16.isTrailSurrogate((char)(chars[1]=src.current()))) {
                src.moveIndex(1); /* skip the c2 surrogate */
                if((norm32&mask)==0) {
                    /* irrelevant data */
                    return 0;
                } else {
                    /* norm32 must be a surrogate special */
                    return NormalizerImpl.getNorm32FromSurrogatePair(norm32,(char)chars[1]);
                }
            } else {
                /* unmatched surrogate */
                return 0;
            }
        }
        return norm32;
    }


    /*
     * for NF*D:
     * read forward and check if the lead combining class is 0
     * if c2!=0 then (c, c2) is a surrogate pair
     */
    private static final class IsNextNFDSafe implements IsNextBoundary{
        public boolean isNextBoundary(UCharacterIterator src, 
                                      int/*unsigned*/ minC, 
                                      int/*unsigned*/ ccOrQCMask, 
                                      int[] chars) {
            return NormalizerImpl.isNFDSafe(getNextNorm32(src,minC,ccOrQCMask,chars), 
                                            ccOrQCMask, ccOrQCMask&NormalizerImpl.QC_MASK);
        }
    }
    
    /*
     * for NF*C:
     * read forward and check if the character is (or its decomposition begins 
     * with) a "true starter" (cc==0 and NF*C_YES)
     * if c2!=0 then (c, c2) is a surrogate pair
     */
    private static final class IsNextTrueStarter implements IsNextBoundary{
        public boolean isNextBoundary(UCharacterIterator src, 
                                      int/*unsigned*/ minC, 
                                      int/*unsigned*/ ccOrQCMask, 
                                      int[] chars) {
            long norm32;
            int/*unsigned*/ decompQCMask;
            
            decompQCMask=(ccOrQCMask<<2)&0xf; /*decomposition quick check mask*/
            norm32=getNextNorm32(src, minC, ccOrQCMask|decompQCMask, chars);
            return NormalizerImpl.isTrueStarter(norm32, ccOrQCMask, decompQCMask);
        }
    }
    
    private static int findNextIterationBoundary(UCharacterIterator src,
                                                 IsNextBoundary obj, 
                                                 int/*unsigned*/ minC, 
                                                 int/*unsigned*/ mask,
                                                 char[] buffer) {
        int[] chars = new int[2];
        int bufferIndex =0;
        
        if(src.current()==UCharacterIterator.DONE) {
            return 0;
        }
        /* get one character and ignore its properties */
        chars[0]=src.next();
        buffer[0]=(char)chars[0];
        bufferIndex=1;
        
        if(UTF16.isLeadSurrogate((char)chars[0])&& 
           src.current()!=UCharacterIterator.DONE) {
            if(UTF16.isTrailSurrogate((char)(chars[1]=src.next()))) {
                buffer[bufferIndex++]=(char)chars[1];
            } else {
                src.moveIndex(-1); /* back out the non-trail-surrogate */
            }
        }
    
        /* get all following characters until we see a boundary */
        /* checking hasNext() instead of c!=DONE on the off-chance that U+ffff 
         * is part of the string */
        while( src.current()!=UCharacterIterator.DONE) {
            if(obj.isNextBoundary(src, minC, mask, chars)) {
                /* back out the latest movement to stop at the boundary */
                src.moveIndex(chars[1]==0 ? -1 : -2);
                break;
            } else {
                if(bufferIndex+(chars[1]==0 ? 1 : 2)<=buffer.length) {
                    buffer[bufferIndex++]=(char)chars[0];
                    if(chars[1]!=0) {
                        buffer[bufferIndex++]=(char)chars[1];
                    }
                } else {
                    char[] newBuf = new char[buffer.length    *2];
                    System.arraycopy(buffer,0,newBuf,0,bufferIndex);
                    buffer = newBuf;
                    buffer[bufferIndex++]=(char)chars[0];
                    if(chars[1]!=0) {
                        buffer[bufferIndex++]=(char)chars[1];
                    }
                }
            }
        }
    
        /* return the length of the buffer contents */
        return bufferIndex;
    }
    
    private static int next(UCharacterIterator src,
                            char[] dest, int destStart, int destLimit,
                            Normalizer.Mode mode,
                            boolean doNormalize, 
                            boolean[] pNeededToNormalize,
                            int options) {
                                
        IsNextBoundary isNextBoundary;
        int /*unsigned*/ mask;
        int /*unsigned*/ bufferLength;
        int c,c2;
        char minC;
        int destCapacity = destLimit - destStart;
        int destLength = 0;
        
        if(pNeededToNormalize!=null) {
            pNeededToNormalize[0]=false;
        }

        minC = (char)mode.getMinC();
        mask = mode.getMask();
        isNextBoundary = mode.getNextBoundary();
        
        if(isNextBoundary==null) {
            destLength=0;
            c=src.next();
            if(c!=UCharacterIterator.DONE) {
                destLength=1;
                if(UTF16.isLeadSurrogate((char)c)) {
                    c2= src.next();
                    if(c2!= UCharacterIterator.DONE) {
                        if(UTF16.isTrailSurrogate((char)c2)) {
                            if(destCapacity>=2) {
                                dest[1]=(char)c2; // trail surrogate 
                                destLength=2;
                            }
                            // lead surrogate to be written below 
                        } else {
                            src.moveIndex(-1);
                        }
                    }
                }
    
                if(destCapacity>0) {
                    dest[0]=(char)c;
                }
            }
            return destLength;
        }

        char[] buffer=new char[100];
        int[] startIndex = new int[1];
        
        bufferLength=findNextIterationBoundary(src,isNextBoundary, minC, mask,
                                               buffer);
        if(bufferLength>0) {
            if(doNormalize) {
                destLength=mode.normalize(buffer,startIndex[0],bufferLength,
                                          dest,destStart,destLimit, options);
                
                if(pNeededToNormalize!=null) {
                    pNeededToNormalize[0]=(destLength!=bufferLength ||
                                                Utility.arrayRegionMatches(buffer,startIndex[0],
                                                                           dest,destStart,
                                                                           destLength));
                }
            } else {
                /* just copy the source characters */
                if(destCapacity>0) {
                    System.arraycopy(buffer,0,dest,destStart,
                                     Math.min(bufferLength,destCapacity));
                }
            }
        }
        return destLength;
    } 

    private void clearBuffer() {
        bufferLimit=bufferStart=bufferPos=0;
    }
        
    private boolean nextNormalize() {
        
        clearBuffer();
        currentIndex=nextIndex;
        text.setIndex(nextIndex);
                
        bufferLimit=next(text,buffer,bufferStart,buffer.length,mode,true,null,options);
                        
        nextIndex=text.getIndex();
        return (bufferLimit>0);
    }
        
    private boolean     previousNormalize() {

        clearBuffer();
        nextIndex=currentIndex;
        text.setIndex(currentIndex);
        bufferLimit=previous(text,buffer,bufferStart,buffer.length,mode,true,null,options);
                
        currentIndex=text.getIndex();
        bufferPos = bufferLimit;
        return bufferLimit>0;
    }
    
    private int getCodePointAt(int index) {
        if( UTF16.isSurrogate(buffer[index])) {
            if(UTF16.isLeadSurrogate(buffer[index])) {
                if((index+1)<bufferLimit &&
                   UTF16.isTrailSurrogate(buffer[index+1])) {
                    return UCharacterProperty.getRawSupplementary(
                                                                  buffer[index], 
                                                                  buffer[index+1]
                                                                  );
                }
            }else if(UTF16.isTrailSurrogate(buffer[index])) {
                if(index>0 && UTF16.isLeadSurrogate(buffer[index-1])) {
                    return UCharacterProperty.getRawSupplementary(
                                                                  buffer[index-1],
                                                                  buffer[index]
                                                                  );
                }
            }   
        }
        return buffer[index];
        
    }
    
    /**
     * Internal API
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static boolean isNFSkippable(int c, Mode mode) {
        return mode.isNFSkippable(c);
    }    

        
    private static int internalCompare(char[] s1, int s1Start,int s1Limit,
                                       char[] s2, int s2Start,int s2Limit,
                                       int options) {
                                  
        char[] fcd1  = new char[300];
        char[] fcd2  = new char[300];
        
        Normalizer.Mode mode;
        int result;
        
        if(    s1==null || s1Start<0 || s1Limit<0 || 
               s2==null || s2Start<0 || s2Limit<0 ||
               s1Limit<s1Start || s2Limit<s2Start
               ) {
                
            throw new IllegalArgumentException();
        }

        UnicodeSet nx=NormalizerImpl.getNX(options>>Normalizer.COMPARE_NORM_OPTIONS_SHIFT);
        options|= NormalizerImpl.COMPARE_EQUIV;
        result=0;

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
        if((options& Normalizer.FOLD_CASE_EXCLUDE_SPECIAL_I) >0 ) {
            mode=Normalizer.NFD;
            options&=~ Normalizer.INPUT_IS_FCD;
        } else {
            mode=Normalizer.FCD;
        }
        if((options& Normalizer.INPUT_IS_FCD)==0) {
            char[] dest;
            int fcdLen1, fcdLen2;
            boolean isFCD1, isFCD2;
        
            // check if s1 and/or s2 fulfill the FCD conditions
            isFCD1= Normalizer.YES==mode.quickCheck(s1, s1Start, s1Limit, true, nx);
            isFCD2= Normalizer.YES==mode.quickCheck(s2, s2Start, s2Limit, true, nx);
            /*
             * ICU 2.4 had a further optimization:
             * If both strings were not in FCD, then they were both NFD'ed,
             * and the COMPARE_EQUIV option was turned off.
             * It is not entirely clear that this is valid with the current
             * definition of the canonical caseless match.
             * Therefore, ICU 2.6 removes that optimization.
             */

            if(!isFCD1) {
                fcdLen1=mode.normalize(s1, 0, s1.length,
                                       fcd1, 0, fcd1.length,
                                       nx);
                                       
                if(fcdLen1>fcd1.length) {
                    dest=new char[fcdLen1];
                    fcdLen1=mode.normalize( s1, 0, s1.length,
                                            dest, 0, dest.length,
                                            nx);
                    s1=dest;
                } else {
                    s1=fcd1;
                }
                s1Limit=fcdLen1;
                s1Start=0;
            }

            if(!isFCD2) {
                fcdLen2=mode.normalize(s2,s2Start,s2Limit,
                                       fcd2,0,fcd2.length,
                                       nx);
                
                if(fcdLen2>fcd2.length) {
                    dest=new char[fcdLen2];
                    fcdLen2=mode.normalize( s2,s2Start,s2Limit,
                                            dest,0,dest.length,
                                            nx);
                    s2=dest;
                } else {
                    s2=fcd2;
                }
                s2Limit=fcdLen2;
                s2Start=0;
            }
                
        }
        
        result=NormalizerImpl.cmpEquivFold(s1, s1Start, s1Limit, 
                                           s2, s2Start, s2Limit, options);
        return result;
    }    
    
    /**
     * Fetches the Unicode version burned into the Normalization data file
     * @return VersionInfo version information of the normalizer
     * @internal
     * @deprecated This API is ICU internal only.
     */
    static VersionInfo getUnicodeVersion() {
        return NormalizerImpl.getUnicodeVersion();
    }
}
