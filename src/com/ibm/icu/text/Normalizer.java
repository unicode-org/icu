/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Normalizer.java,v $ 
 * $Date: 2002/07/16 00:20:35 $ 
 * $Revision: 1.19 $
 *
 *******************************************************************************
 */
package com.ibm.icu.text;
import com.ibm.icu.impl.*;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.UCharacterProperty;
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
 * <p>
 *      00C1    LATIN CAPITAL LETTER A WITH ACUTE
 * </p>
 *
 * or as two separate characters (the "decomposed" form):
 *
 * <p>
 *      0041    LATIN CAPITAL LETTER A
 *      0301    COMBINING ACUTE ACCENT
 * </p>
 *
 * To a user of your program, however, both of these sequences should be
 * treated as the same "user-level" character "A with acute accent".  When you are searching or
 * comparing text, you must ensure that these two sequences are treated 
 * equivalently.  In addition, you must handle characters with more than one
 * accent.  Sometimes the order of a character's combining accents is
 * significant, while in other cases accent sequences in different orders are
 * really equivalent.
 *
 * Similarly, the string "ffi" can be encoded as three separate letters:
 *
 * <p>
 *      0066    LATIN SMALL LETTER F
 *      0066    LATIN SMALL LETTER F
 *      0069    LATIN SMALL LETTER I
 * <\p>
 *
 * or as the single character
 *
 * <p>
 *      FB03    LATIN SMALL LIGATURE FFI
 * <\p>
 *
 * The ffi ligature is not a distinct semantic character, and strictly speaking
 * it shouldn't be in Unicode at all, but it was included for compatibility
 * with existing character sets that already provided it.  The Unicode standard
 * identifies such characters by giving them "compatibility" decompositions
 * into the corresponding semantic characters.  When sorting and searching, you
 * will often want to use these mappings.
 *
 * <code>normalize</code> helps solve these problems by transforming text into the
 * canonical composed and decomposed forms as shown in the first example above.  
 * In addition, you can have it perform compatibility decompositions so that 
 * you can treat compatibility characters the same as their equivalents.
 * Finally, <code>normalize</code> rearranges accents into the proper canonical
 * order, so that you do not have to worry about accent rearrangement on your
 * own.
 *
 * Form FCD, "Fast C or D", is also designed for collation.
 * It allows to work on strings that are not necessarily normalized
 * with an algorithm (like in collation) that works under "canonical closure", i.e., it treats precomposed
 * characters and their decomposed equivalents the same.
 *
 * It is not a normalization form because it does not provide for uniqueness of representation. Multiple strings
 * may be canonically equivalent (their NFDs are identical) and may all conform to FCD without being identical
 * themselves.
 *
 * The form is defined such that the "raw decomposition", the recursive canonical decomposition of each character,
 * results in a string that is canonically ordered. This means that precomposed characters are allowed for as long
 * as their decompositions do not need canonical reordering.
 *
 * Its advantage for a process like collation is that all NFD and most NFC texts - and many unnormalized texts -
 * already conform to FCD and do not need to be normalized (NFD) for such a process. The FCD quick check will
 * return YES for most strings in practice.
 *
 * normalize(FCD) may be implemented with NFD.
 *
 * For more details on FCD see the collation design document:
 * http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/collation/ICU_collation_design.htm
 *
 * ICU collation performs either NFD or FCD normalization automatically if normalization
 * is turned on for the collator object.
 * Beyond collation and string search, normalized strings may be useful for string equivalence comparisons,
 * transliteration/transcription, unique representations, etc.
 *
 * The W3C generally recommends to exchange texts in NFC.
 * Note also that most legacy character encodings use only precomposed forms and often do not
 * encode any combining marks by themselves. For conversion to such character encodings the
 * Unicode text needs to be normalized to NFC.
 * For more usage examples, see the Unicode Standard Annex.
 */

public final class Normalizer implements Cloneable{
    
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
     * Constant indicating that the end of the iteration has been reached.
     * This is guaranteed to have the same value as {@link UCharacterIterator#DONE}.
     * 
     */
    public static final int DONE = UCharacterIterator.DONE;

    /**
     * Constants for normalization modes.
     */
    public static class Mode {
		private int modeValue;
		private Mode(int value){
		    modeValue = value;
		}
        protected int dispatch(char[] src, int srcStart, int srcLimit,
                     char[] dest, int destStart, int destLimit){
            int srcLen = (srcLimit - srcStart);
            int destLen = (destLimit - destStart);
            if( srcLen < destLen ){
                return srcLen;
            }
            System.arraycopy(src,srcStart,dest,destStart,srcLen);
            return srcLen;
        }
        
        protected String dispatch(String src){
            return src;
        }
        
        protected int getMinC(){
            return -1;
        }
        protected int getMask(){
            return -1;
        }
        protected IsPrevBoundary getPrevBoundary(){
            return null;
        }
        protected IsNextBoundary getNextBoundary(){
            return null;
        }
        protected QuickCheckResult quickCheck(char[] src,int start, int limit, boolean allowMaybe){
            if(allowMaybe){
                return MAYBE;
            }
            return NO;
        }
        
    }
    
    /** No decomposition/composition.  */
    public static Mode NONE = new Mode(1);

    /** Canonical decomposition.  */
    public static Mode NFD = new Mode(2){
            protected int dispatch( char[] src, int srcStart, int srcLimit,
                          char[] dest,int destStart,int destLimit){
              return decompose(src,  srcStart,srcLimit,
                               dest, destStart,destLimit,
                               false);
            }
            
            protected String dispatch( String src){
                return decompose(src,false);
            }
            protected int getMinC(){
                return NormalizerImpl.MIN_WITH_LEAD_CC;
            }
            protected IsPrevBoundary getPrevBoundary(){
                return new IsPrevNFDSafe();
            }
            protected IsNextBoundary getNextBoundary(){
                return new IsNextNFDSafe();
            }
            protected int getMask(){
                return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFD);
            }
            protected QuickCheckResult quickCheck(char[] src,int start, 
                                                  int limit,boolean allowMaybe){
                return NormalizerImpl.quickCheck(
                                      src, start,limit,
                                      NormalizerImpl.getFromIndexesArr(
                                           NormalizerImpl.INDEX_MIN_NFD_NO_MAYBE
                                      ),
                                      NormalizerImpl.QC_NFD,
                                      allowMaybe
                                 );
            }
           
         };
                                         
    /** Compatibility decomposition.  */
    public static Mode NFKD = new Mode(3){
            protected int dispatch( char[] src, int srcStart, int srcLimit,
                           char[] dest,int destStart,int destLimit){
              return decompose(src,  srcStart,srcLimit,
                               dest, destStart,destLimit,
                               true);
            }
            protected String dispatch( String src){
                return decompose(src,true);
            }
            protected int getMinC(){
                return NormalizerImpl.MIN_WITH_LEAD_CC;
            }
            protected IsPrevBoundary getPrevBoundary(){
                return new IsPrevNFDSafe();
            }
            protected IsNextBoundary getNextBoundary(){
                return new IsNextNFDSafe();
            }
            protected int getMask(){
                return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFKD);
            }
            protected QuickCheckResult quickCheck(char[] src,int start, 
                                                  int limit,boolean allowMaybe){
                return NormalizerImpl.quickCheck(
                                      src,start,limit,
                                      NormalizerImpl.getFromIndexesArr(
                                          NormalizerImpl.INDEX_MIN_NFKD_NO_MAYBE
                                      ),
                                      NormalizerImpl.QC_NFKD,
                                      allowMaybe
                                );
            }                                        
         };
                                         
    /** Canonical decomposition followed by canonical composition.  */
    public static Mode NFC = new Mode(4){
            protected int dispatch( char[] src, int srcStart, int srcLimit,
                          char[] dest,int destStart,int destLimit){
              return compose(src,  srcStart,srcLimit,
                             dest, destStart,destLimit,
                             false);
            }
            
            protected String dispatch( String src){
                return compose(src,false);
            }
           
            protected int getMinC(){
                return NormalizerImpl.getFromIndexesArr(
                                        NormalizerImpl.INDEX_MIN_NFC_NO_MAYBE
                                    );
            }
            protected IsPrevBoundary getPrevBoundary(){
                return new IsPrevTrueStarter();
            }
            protected IsNextBoundary getNextBoundary(){
                return new IsNextTrueStarter();
            }
            protected int getMask(){
                return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFC);
            }
            protected QuickCheckResult quickCheck(char[] src,int start, 
                                                  int limit,boolean allowMaybe){
                return NormalizerImpl.quickCheck(
                                       src,start,limit,
                                       NormalizerImpl.getFromIndexesArr(
                                           NormalizerImpl.INDEX_MIN_NFC_NO_MAYBE
                                       ),
                                       NormalizerImpl.QC_NFC,
                                       allowMaybe
                                   );
            }
         };
                                         
    /** Default normalization.  */
    public static Mode DEFAULT = NFC; 
    
    /** Compatibility decomposition followed by canonical composition.  */
    public static Mode NFKC =new Mode(5){
            protected int dispatch( char[] src, int srcStart, int srcLimit,
                          char[] dest,int destStart,int destLimit){
              return compose(src,  srcStart,srcLimit,
                             dest, destStart,destLimit,
                             true);
            }
            protected String dispatch( String src){
                return compose(src,true);
            }
            protected int getMinC(){
                return NormalizerImpl.getFromIndexesArr(
                                        NormalizerImpl.INDEX_MIN_NFKC_NO_MAYBE
                                    );
            }
            protected IsPrevBoundary getPrevBoundary(){
                return new IsPrevTrueStarter();
            }
            protected IsNextBoundary getNextBoundary(){
                return new IsNextTrueStarter();
            }
            protected int getMask(){
                return (NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFKC);
            }
            protected QuickCheckResult quickCheck(char[] src,int start, 
                                                  int limit,boolean allowMaybe){
                return NormalizerImpl.quickCheck(
                                       src,start,limit,
                                       NormalizerImpl.getFromIndexesArr(
                                          NormalizerImpl.INDEX_MIN_NFKC_NO_MAYBE
                                       ),
                                       NormalizerImpl.QC_NFKC,
                                       allowMaybe
                                     );
            }
         };
                                        
    /** "Fast C or D" form. @since ICU 2.1 */
    public static Mode FCD = new Mode(6){
            protected int dispatch( char[] src, int srcStart, int srcLimit,
                          char[] dest,int destStart,int destLimit){
              return NormalizerImpl.makeFCD(src, srcStart,srcLimit,
                                            dest, destStart,destLimit);
            }
            protected String dispatch( String src){
                return makeFCD(src);
            }
            protected int getMinC(){
                return NormalizerImpl.MIN_WITH_LEAD_CC;
            }
            protected IsPrevBoundary getPrevBoundary(){
                return new IsPrevNFDSafe();
            }
            protected IsNextBoundary getNextBoundary(){
                return new IsNextNFDSafe();
            }
            protected int getMask(){
                return NormalizerImpl.CC_MASK|NormalizerImpl.QC_NFD;
            }
            protected QuickCheckResult quickCheck(char[] src,int start, 
                                                  int limit,boolean allowMaybe){
                return NormalizerImpl.checkFCD(src,start,limit) ? YES : NO;
            }  
         };

    
    /**
     * Null operation for use with the {@link #Normalizer constructors}
     * and the static {@link #normalize normalize} method.  This value tells
     * the <tt>Normalizer</tt> to do nothing but return unprocessed characters
     * from the underlying String or CharacterIterator.  If you have code which
     * requires raw text at some times and normalized text at others, you can
     * use <tt>NO_OP</tt> for the cases where you want raw text, rather
     * than having a separate code path that bypasses <tt>Normalizer</tt>
     * altogether.
     * <p>
     * @see #setMode
     * @deprecated
     */
    public static final Mode NO_OP = NONE;

    /**
     * Canonical decomposition followed by canonical composition.  Used with the
     * {@link #Normalizer constructors} and the static {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>C</b>.
     * <p>
     * @see #setMode
     * @deprecated
     */
    public static final Mode COMPOSE = new Mode(COMPOSE_BIT);

    /**
     * Compatibility decomposition followed by canonical composition.
     * Used with the {@link #Normalizer constructors} and the static
     * {@link #normalize normalize} method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>KC</b>.
     * <p>
     * @see #setMode
     * @deprecated
     */
    public static final Mode COMPOSE_COMPAT = new Mode(COMPOSE_BIT | COMPAT_BIT);

    /**
     * Canonical decomposition.  This value is passed to the
     * {@link #Normalizer constructors} and the static {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>D</b>.
     * <p>
     * @see #setMode
     * @deprecated
     */
    public static final Mode DECOMP = new Mode(DECOMP_BIT);

    /**
     * Compatibility decomposition.  This value is passed to the
     * {@link #Normalizer constructors} and the static {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical Form</a>
     * <b>KD</b>.
     * <p>
     * @see #setMode
     * @deprecated
     */
    public static final Mode DECOMP_COMPAT = new Mode(DECOMP_BIT | COMPAT_BIT);

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
     * @deprecated
     */
    public static final int IGNORE_HANGUL = 0x0001;
          
    /**
     * Result values for quickCheck().
     * For details see Unicode Technical Report 15.
     * 
     */
    public static final class QuickCheckResult{
		private int resultValue;
		private QuickCheckResult(int value){
		    resultValue=value;
		}
    }
    /** 
     * Indicates that string is not in the normalized format
     */
    public static QuickCheckResult NO = new QuickCheckResult(0);
	
    /** 
     * Indicates that string is in the normalized format
     */
    public static QuickCheckResult YES = new QuickCheckResult(1);

    /** 
     * Indicates it cannot be determined if string is in the normalized 
     * format without further thorough checks.
     */
    public static QuickCheckResult MAYBE = new QuickCheckResult(2);
    
    /**
     * Option bit for compare:
     * Both input strings are assumed to fulfill FCD conditions.
     * @since ICU 2.2
     */
    public static final int INPUT_IS_FCD    =      0x20000;
	
    /**
     * Option bit for compare:
     * Perform case-insensitive comparison.
     * @since ICU 2.2
     */
    public static final int COMPARE_IGNORE_CASE  =     0x10000;
	
    /**
     * Option bit for compare:
     * Compare strings in code point order instead of code unit order.
     * @since ICU 2.2
     */
    public static final int COMPARE_CODE_POINT_ORDER = 0x8000;
    
    /** Option value for case folding: exclude the mappings for dotted I 
     * and dotless i marked with 'I' in CaseFolding.txt. 
     * @since ICU 2.2
     */
    public static final int FOLD_CASE_EXCLUDE_SPECIAL_I = 0x0001;
	
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new <tt>Normalizer</tt> object for iterating over the
     * normalized form of a given string.
     * <p>
     * @param str   The string to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     */
    public Normalizer(String str, Mode mode) {
        this( UCharacterIterator.getInstance(str), mode);
    }

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
     *            Currently the only available option is {@link #IGNORE_HANGUL}.
     *            If you want the default behavior corresponding to one of the
     *            standard Unicode Normalization Forms, use 0 for this argument.
     * @deprecated
     */
    public Normalizer(String str, Mode mode, int opt) {
        this( UCharacterIterator.getInstance(str), mode );
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
     */
    public Normalizer(CharacterIterator iter, Mode mode) {
        this( UCharacterIterator.getInstance(iter), mode);
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
     *            Currently the only available option is {@link #IGNORE_HANGUL}.
     *            If you want the default behavior corresponding to one of the
     *            standard Unicode Normalization Forms, use 0 for this argument.
     * @deprecated
     */
    public Normalizer(CharacterIterator iter, Mode mode, int opt) {
        this( UCharacterIterator.getInstance(iter), mode);
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
     */
    public Normalizer(UCharacterIterator iter, Mode mode){
        this.text     = iter;
        this.mode     = mode;
    }

    /**
     * Clones this <tt>Normalizer</tt> object.  All properties of this
     * object are duplicated in the new object, including the cloning of any
     * {@link CharacterIterator} that was passed in to the constructor
     * or to {@link #setText(CharacterIterator) setText}.
     * However, the text storage underlying
     * the <tt>CharacterIterator</tt> is not duplicated unless the
     * iterator's <tt>clone</tt> method does so.
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
            throw new InternalError(e.toString());
        }
    }
    
    //--------------------------------------------------------------------------
    // Static Utility methods
    //--------------------------------------------------------------------------

    /**
     * Normalizes a <tt>String</tt> using the given normalization operation.
     * <p>
     * The <tt>options</tt> parameter specifies which optional
     * <tt>Normalizer</tt> features are to be enabled for this operation.
     * Currently the only available option is {@link #IGNORE_HANGUL}.
     * If you want the default behavior corresponding to one of the standard
     * Unicode Normalization Forms, use 0 for this argument.
     * <p>
     * @param str       the input string to be normalized.
     *
     * @param aMode     the normalization mode
     *
     * @param options   the optional features to be enabled.
     * @deprecated
     */
    public static String normalize(String str, Mode mode, int options){
        return normalize(str,mode);
    }
    
    /**
     * Compose a string.
     * The string will be composed to according the the specified mode.
     * @param str        The string to compose.
     * @param compat     If true the char array will be composed accoding to 
     *                    NFKC rules and if false will be composed according to 
     *                    NFC rules.
     * @return String    The composed string   
     */            
    public static String compose(String str, boolean compat){
        char[] dest = new char[str.length()*MAX_BUF_SIZE];
        int destSize=0;
        char[] src = str.toCharArray();
        for(;;){
            destSize=NormalizerImpl.compose(src,0,src.length,
                                            dest,0,dest.length,compat);
            if(destSize<=dest.length){
		        return new String(dest,0,destSize);  
            }else{
                dest = new char[destSize];
            }
        }                     
    }
    
    /**
     *  Compose a string.
     * The string will be composed to according the the specified mode.
     * @param str        The string to compose.
     * @param compat     If true the char array will be composed accoding to 
     *                    NFKC rules and if false will be composed according to 
     *                    NFC rules.
     * @param options    The only recognized option is IGNORE_HANGUL
     * @return String    The composed string   
     * @deprecated
     */            
    public static String compose(String str, boolean compat, int options){
        return compose(str,compat);                     
    }
    
    /**
     * Compose a string.
     * The string will be composed to according the the specified mode.
     * @param source The char array to compose.
     * @param target A char buffer to receive the normalized text.
     * @param compat If true the char array will be composed accoding to 
     *                NFKC rules and if false will be composed according to 
     *                NFC rules.
     * @return int   The total buffer size needed;if greater than length of 
     *                result, the output was truncated.
     * @exception IndexOutOfBoundsException if target.length is less than the 
     *             required length  
     */         
    public static int compose(char[] source,char[] target, boolean compat){
        int length = NormalizerImpl.compose(source,0,source.length,
                                            target,0,target.length,
                                            compat);
		if(length<=target.length){
		    return length;
		}else{
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
     * @return int   The total buffer size needed;if greater than length of 
     *                result, the output was truncated.
     * @exception IndexOutOfBoundsException if target.length is less than the 
     *             required length  
     */         
    public static int compose(char[] src,int srcStart, int srcLimit,
                              char[] dest,int destStart, int destLimit,
                              boolean compat){
        int length = NormalizerImpl.compose(src,srcStart,srcLimit,
                                            dest,destStart,destLimit,
                                            compat);
        if(length<=(destLimit-destStart)){
            return length;
        }else{
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    private static final int MAX_BUF_SIZE = 20;
    
    /**
     * Decompose a string.
     * The string will be decomposed to according the the specified mode.
     * @param str       The string to decompose.
     * @param compat    If true the char array will be decomposed accoding to NFKD rules
     *                   and if false will be decomposed according to NFD rules.
     * @return String   The decomposed string   
     */         
    public static String decompose(String str, boolean compat){
        char[] dest = new char[str.length()*MAX_BUF_SIZE];
        int[] trailCC = new int[1];
        int destSize=0;
        for(;;){
            destSize=NormalizerImpl.decompose(str.toCharArray(),0,str.length(),
                                              dest,0,dest.length,
                                              compat,trailCC);
            if(destSize<=dest.length){
		        return new String(dest,0,destSize); 
            }else{
                dest = new char[destSize];
            }
        } 
	                     
    }
    
    /**
     * Decompose a string.
     * The string will be decomposed to according the the specified mode.
     * @param str     The string to decompose.
     * @param compat  If true the char array will be decomposed accoding to NFKD rules
     *                 and if false will be decomposed according to NFD rules.
     * @return String The decomposed string 
     * @deprecated  
     */         
    public static String decompose(String str, boolean compat, int options){
        return decompose(str,compat);                 
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
     * @exception IndexOutOfBoundsException if the target capacity is less than
     *             the required length   
     */
    public static int decompose(char[] source,char[] target, boolean compat){
        int[] trailCC = new int[1];
        int length = NormalizerImpl.decompose(source,0,source.length,
                                              target,0,target.length,
                                              compat,trailCC);
		if(length<=target.length){
		    return length;
		}else{
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
     * @return int   The total buffer size needed;if greater than length of 
     *                result,the output was truncated.
     * @exception IndexOutOfBoundsException if the target capacity is less than
     *             the required length   
     */
    public static int decompose(char[] src,int srcStart, int srcLimit,
                                char[] dest,int destStart, int destLimit,
                                boolean compat){
        int[] trailCC = new int[1];
        int length = NormalizerImpl.decompose(src,srcStart,srcLimit,
                                              dest,destStart,destLimit,
                                              compat,trailCC);
        if(length<=(destLimit-destStart)){
            return length;
        }else{
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }
    
    /**
     * Normalize a string.
     * The string will be normalized according the the specified normalization mode
     * and options.
     * @param source     The string to normalize.
     * @param mode       The normalization mode; one of Normalizer.NONE, 
     *                    Normalizer.NFD, Normalizer.NFC, Normalizer.NFKC, 
     *                    Normalizer.NFKD, Normalizer.DEFAULT
     * @return String    The normalized string
     *   
     */
    public static String normalize( String src,Mode mode){
        return mode.dispatch(src);    
    }
    
    private static String makeFCD(String src){
        int srcLen = src.length();
        char[] dest = new char[MAX_BUF_SIZE*srcLen];
        int length = 0;
        for(;;){
            length = NormalizerImpl.makeFCD(src.toCharArray(),0,srcLen,
                                            dest,0,dest.length);
            if(length <= dest.length){
                return new String(dest,0,length);
            }else{
                dest = new char[length];
            }
        }
    }
    
    /**
     * Normalize a string.
     * The string will be normalized according the the specified normalization mode
     * and options.
     * @param source The char array to normalize.
     * @param target A char buffer to receive the normalized text.
     * @param mode   The normalization mode; one of Normalizer.NONE, 
     *                Normalizer.NFD, Normalizer.NFC, Normalizer.NFKC, 
     *                Normalizer.NFKD, Normalizer.DEFAULT
     * @return int   The total buffer size needed;if greater than length of result,
     *                the output was truncated.
     * @exception    IndexOutOfBoundsException if the target capacity is less than
     *                the required length     
     */
    public static int normalize(char[] source,char[] target, Mode  mode){
		int length = normalize(source,0,source.length,target,0,target.length,mode);
		if(length<=target.length){
		    return length;
		}else{
		    throw new IndexOutOfBoundsException(Integer.toString(length));
		} 
    }
    
    /**
     * Normalize a string.
     * The string will be normalized according the the specified normalization mode
     * and options.
     * @param src       The char array to compose.
     * @param srcStart  Start index of the source
     * @param srcLimit  Limit index of the source
     * @param dest      The char buffer to fill in
     * @param destStart Start index of the destination buffer  
     * @param destLimit End index of the destination buffer
     * @param mode   The normalization mode; one of Normalizer.NONE, 
     *               Normalizer.NFD, Normalizer.NFC, Normalizer.NFKC, 
     *               Normalizer.NFKD, Normalizer.DEFAULT
     * @return int   The total buffer size needed;if greater than length of result,
     *               the output was truncated.
     * @exception IndexOutOfBoundsException if the target capacity is less than
     *             the required length     
     */       
    public static int normalize(char[] src,int srcStart, int srcLimit, 
                                char[] dest,int destStart, int destLimit,
                                Mode  mode){
        int length =mode.dispatch(src,srcStart,srcLimit,dest,destStart,destLimit);
       
        if(length<=(destLimit-destStart)){
            return length;
        }else{
            throw new IndexOutOfBoundsException(Integer.toString(length));
        } 
    }

    /**
     * Conveinience method.
     *
     * @param source       string for determining if it is in a normalized format
     * @param mode         normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                     Normalizer.NFKC,Normalizer.NFKD)
     * @return             Return code to specify if the text is normalized or not 
     *                     (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     */
    public static QuickCheckResult quickCheck( String source, Mode mode){
	    return mode.quickCheck(source.toCharArray(),0,source.length(),true);
    }
    
    /**
     * Conveinience method.
     *
     * @param source Array of characters for determining if it is in a normalized format
     * @param mode   normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                Normalizer.NFKC,Normalizer.NFKD)
     * @return       Return code to specify if the text is normalized or not 
     *                (Normalizer.YES, Normalizer.NO or Normalizer.MAYBE)
     */
    public static QuickCheckResult quickCheck(char[] source, Mode mode){
        return mode.quickCheck(source,0,source.length,true);
    }
    
    /**
     * Performing quick check on a string, to quickly determine if the string is 
     * in a particular normalization format.
     * Three types of result can be returned Normalizer.YES, Normalizer.NO or
     * Normalizer.MAYBE. Result Normalizer.YES indicates that the argument
     * string is in the desired normalized format, Normalizer.NO determines that
     * argument string is not in the desired normalized format. A Normalizer.MAYBE
     * result indicates that a more thorough check is required, the user may have to
     * put the string in its normalized form and compare the results.
     *
     * @param source       string for determining if it is in a normalized format
     * @param start        the start index of the source
     * @param limit        the limit index of the source it is equal to the length
     * @param mode         normalization format (Normalizer.NFC,Normalizer.NFD,  
     *                     Normalizer.NFKC,Normalizer.NFKD)
     * @return             Return code to specify if the text is normalized or not 
     *                     (Normalizer.YES, Normalizer.NO or
     *                     Normalizer.MAYBE)
     */

    public static QuickCheckResult quickCheck(char[] source,int start, 
                                              int limit, Mode mode){    	
	    return mode.quickCheck(source,start,limit,true);
    }
    
    //-------------------------------------------------------------------------
    // Internal methods (for now)
    //-------------------------------------------------------------------------

    /**
     * Normalize a codepoint accoding to the given mode
     * @param char32    The input string to be normalized.
     * @param aMode     The normalization mode
     * @return String   The normalized string
     */
    // TODO: actually do the optimization when the guts of Normalizer are upgraded
    // --has just dumb implementation for now
    public static String normalize(int char32, Mode mode) {
        return normalize(UTF16.valueOf(char32), mode);
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
     * @param aMode     the normalization mode
     * @return Boolean value indicating whether the source string is in the
     *         "mode" normalization form
     */
    // TODO: actually do the optimization when the guts of Normalizer are upgraded
    // --has just dumb implementation for now
    public static boolean isNormalized(char[] src,int start,int limit, Mode mode) {
        return (mode.quickCheck(src,start,limit,false)==YES);
    }
    
    /**
     * Convenience Method
     * @param str       the input string to be checked to see if it is normalized
     *
     * @param aMode     the normalization mode
     * @see #isNormalized
     */
    // TODO: actually do the optimization when the guts of Normalizer are upgraded
    // --has just dumb implementation for now
    public static boolean isNormalized(String str, Mode mode) {
        return (mode.quickCheck(str.toCharArray(),0,str.length(),false)==YES);
    }
    
    /**
     * Convenience Method
     * @param char32    the input code point to be checked to see if it is normalized
     *
     * @param aMode     the normalization mode
     * @see #isNormalized
     */
    // TODO: actually do the optimization when the guts of Normalizer are upgraded
    // --has just dumb implementation for now
    public static boolean isNormalized(int char32, Mode mode) {
        return isNormalized(UTF16.valueOf(char32), mode);
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
     * Bulk normalization is only necessary if the strings do not fulfill the FCD
     * conditions. Only in this case, and only if the strings are relatively long,
     * is memory allocated temporarily.
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
     *     Set if the caller knows that both s1 and s2 fulfill the FCD conditions.
     *     If not set, the function will quickCheck for FCD
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
     */
     public static int compare(char[] s1, int s1Start, int s1Limit,
                               char[] s2, int s2Start, int s2Limit,
                               int options){
         return NormalizerImpl.compare(s1, s1Start, s1Limit, 
                                       s2, s2Start, s2Limit, options);
     } 
       
    /**
     * Compare two strings for canonical equivalence.
     * Further options include case-insensitive comparison and
     * code point order (as opposed to code unit order).
     * Conveinience method.
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
     *     Set if the caller knows that both s1 and s2 fulfill the FCD conditions.
     *     If not set, the function will quickCheck for FCD
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
     */
     public static int compare(String s1, String s2, int options){
         
         return compare(s1.toCharArray(),0,s1.length(),
                                       s2.toCharArray(),0,s2.length(),
                                       options);
     }
     
    /**
     * Compare two strings for canonical equivalence.
     * Further options include case-insensitive comparison and
     * code point order (as opposed to code unit order).
     * Conveinience method.
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
     *     Set if the caller knows that both s1 and s2 fulfill the FCD conditions.
     *     If not set, the function will quickCheck for FCD
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
     */
     public static int compare(char[] s1, char[] s2, int options){
         
         return compare(s1,0,s1.length,s2,0,s2.length,options);
     } 
        
    /**
     * Convenience method that can have faster implementation
     * by not allocating buffers.
     * @param char32a    the first code point to be checked against the
     * @param char32b    the second code point
     *
     * @param aMode     the normalization mode
     */
    // TODO: actually do the optimization when the guts of Normalizer are upgraded
    // --has just dumb implementation for now
    public static int compare(int char32a, int char32b,int options) {
        return compare(UTF16.valueOf(char32a), UTF16.valueOf(char32b), options);
    }
    
    
    /**
     * Convenience method that can have faster implementation
     * by not allocating buffers.
     * @internal
     * @param char32a    the first code point to be checked against the
     * @param str2    the second string
     *
     * @param aMode     the normalization mode
     *
     */
    // TODO: actually do the optimization when the guts of Normalizer are upgraded
    // --has just dumb implementation for now
    public static int compare(int charA, String str2, int options) {
        return compare(UTF16.valueOf(charA), str2, options);
    }
   
    /**
     * Concatenate normalized strings, making sure that the result is normalized
     * as well.
     *
     * If both the left and the right strings are in
     * the normalization form according to "mode",
     * then the result will be
     *
     * \code
     *     dest=normalize(left+right, mode)
     * \endcode
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
     * @param leftStart start index of the left array.
     * @param leftLimit end index of the left array (==length)
     * @param right Right source array.
     * @param rightStart start index of the right array.
     * @param leftLimit end index of the right array (==length)
     * @param dest The output buffer; can be null if destStart==destLimit==0 
     *              for pure preflighting.
     * @param destStart start index of the destination array
     * @param mode The normalization mode.
     * @return Length of output (number of chars) when successful or 
     *          IndexOutOfBoundsException
     * @exception IndexOutOfBoundsException whose message has the string 
     *             representation of destination capacity required. 
     * @see #normalize
     * @see #next
     * @see #previous
     * @exception IndexOutOfBoundsException if target capacity is less than the
     *             required length
     */
     /* Concatenation of normalized strings ---------------------------------- */
    
    public static int concatenate(char[] left,  int leftStart,  int leftLimit,
                                  char[] right, int rightStart, int rightLimit, 
                                  char[] dest,  int destStart,  int destLimit,
                                  Normalizer.Mode mode) {
                               
        char[] buffer=new char[100];
        int bufferLength;
    
        UCharacterIterator iter;
        
        int leftBoundary, rightBoundary, destLength;
    
        if(dest == null){
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
    
        bufferLength=previous(iter, buffer,0,buffer.length,mode,false,null);
        
        leftBoundary=iter.getIndex();
        
        if(bufferLength>buffer.length) {
            char[] newBuf = new char[buffer.length*2];
            // TODO: this may need to be commented ???
            //System.arraycopy(newBuf,0,buffer,0,bufferLength);
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
                           mode, false,null);
                           
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
                                                     destLength,destLimit,mode);
            
        } else {
            destLength+=Normalizer.normalize(buffer, 0, bufferLength,null,0,0,mode);
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
        
        if(destLength<=(destLimit-destStart)){
            return destLength;
        }else{
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
     * @return result
     *
     * @see #concatenate
     * @see #normalize
     * @see #next
     * @see #previous
     * @see #concatenate
     */
    public static String concatenate(char[] left, char[] right,Mode mode){
        char[] result = new char[(left.length+right.length)* MAX_BUF_SIZE];
        for(;;){
               
            int length = concatenate(left,  0, left.length,
                                     right, 0, right.length,
                                     result,0, result.length,
                                     mode);
            if(length<=result.length){
                return new String(result,0,length);
            }else{
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
     * @return result
     *
     * @see #concatenate
     * @see #normalize
     * @see #next
     * @see #previous
     * @see #concatenate
     */
    public static String concatenate(String left, String right,Mode mode){
        char[] result = new char[(left.length()+right.length())* MAX_BUF_SIZE];
        for(;;){
               
            int length = concatenate(left.toCharArray(), 0, left.length(),
                         right.toCharArray(),0, right.length(),
                         result,             0, result.length,
                         mode);
            if(length<=result.length){
                return new String(result,0,length);
            }else{
                result = new char[length];
            }
        }            
    }
    
    //-------------------------------------------------------------------------
    // Iteration API
    //-------------------------------------------------------------------------
	
    /**
     * Return the current character in the normalized text->
     * @return The codepoint as an int
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
     * @return      the first normalized character that is the result of iterating
     *              forward starting at the given index.
     *
     * @throws IllegalArgumentException if the given index is less than
     *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
     * @return The codepoint as an int
     */
    public int setIndex(int index) {
		setIndexOnly(index);
		return current();
    }
 
    /**
     * Retrieve the index of the start of the input text.  This is the begin index
     * of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @deprecated
     * @return The codepoint as an int
     */
    public int getBeginIndex() {
        return 0;
    }

    /**
     * Retrieve the index of the end of the input text.  This is the end index
     * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @deprecated
     * @return The codepoint as an int
     */
    public int getEndIndex() {
        return text.getLength()-1;
    }
    /**
     * Return the first character in the normalized text->  This resets
     * the <tt>Normalizer's</tt> position to the beginning of the text->
     * @return The codepoint as an int
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
     *@return The current iteration position
     */
    public int getIndex(){
		if(bufferPos<bufferLimit) {
		    return currentIndex;
		} else {
		    return nextIndex;
		}
    }
	
    /**
     * Retrieve the index of the start of the input text->  This is the begin index
     * of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @return The current iteration position
     */
    public int startIndex(){
		return 0;
    }
	
    /**
     * Retrieve the index of the end of the input text->  This is the end index
     * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
     * over which this <tt>Normalizer</tt> is iterating
     * @return The current iteration position
     */
    public int endIndex(){
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
     *  <li>{@link #COMPOSE}        - Unicode canonical decompositiion
     *                                  followed by canonical composition.
     *  <li>{@link #COMPOSE_COMPAT} - Unicode compatibility decompositiion
     *                                  follwed by canonical composition.
     *  <li>{@link #DECOMP}         - Unicode canonical decomposition
     *  <li>{@link #DECOMP_COMPAT}  - Unicode compatibility decomposition.
     *  <li>{@link #NO_OP}          - Do nothing but return characters
     *                                  from the underlying input text.
     * </ul>
     *
     * @see #getMode
     */
    public void setMode(Mode newMode){
		mode = newMode;
    }
	/**
     * Return the basic operation performed by this <tt>Normalizer</tt>
     *
     * @see #setMode
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
     *   <li>{@link #IGNORE_HANGUL} - Do not decompose Hangul syllables into the Jamo alphabet
     *          and vice-versa.  This option is off by default (<i>i.e.</i> Hangul processing
     *          is enabled) since the Unicode standard specifies that Hangul to Jamo
     *          is a canonical decomposition.  For any of the standard Unicode Normalization
     *          Forms, you should leave this option off.
     * </ul>
     * <p>
     * @param   option  the option whose value is to be set.
     * @param   value   the new setting for the option.  Use <tt>true</tt> to
     *                  turn the option on and <tt>false</tt> to turn it off.
     *
     * @see #getOption
     * @deprecated
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
     * @deprecated
     */
    public int getOption(int option){
	    if((options & option)!=0){
            return 1 ;
        }else{
            return 0;
        }
    }
    
    /**
     * Gets the underlying text storage
     * @param fillIn the char buffer to fill the UTF-16 units.
     *         The length of the buffer should be equal to the length of the
     *         underlying text storage
     * @throws IndexOutOfBoundsException
     * @see   #getLength
     */
    public int getText(char[] fillIn){
        return text.getText(fillIn);
    }
    
    /**
     * Gets the length of underlying text storage
     * @return the length
     */ 
    public int getLength(){
        return text.getLength();
    }
    
    /**
     * Returns the text under iteration as a string
     * @param result a copy of the text under iteration.
     */
    public String getText(){
        return text.getText();
    }
    
    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text->
     * @param newText   The new string to be normalized.
     */
    public void setText(StringBuffer newText){
        
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
                throw new InternalError("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }
	
    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text->
     * @param newText   The new string to be normalized.
     */
    public void setText(char[] newText){
        
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
                throw new InternalError("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }
    
    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text->
     * @param newText   The new string to be normalized.
     */
    public void setText(String newText){
	    
		UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
		if (newIter == null) {
	            throw new InternalError("Could not create a new UCharacterIterator");
		}  
		text = newIter;
		reset();
    }
    
    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the input text->
     * @param newText   The new string to be normalized.
     */
    public void setText(CharacterIterator newText){
        
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new InternalError("Could not create a new UCharacterIterator");
        }  
        text = newIter;
        reset();
    }
    
    /**
     * Set the input text over which this <tt>Normalizer</tt> will iterate.
     * The iteration position is set to the beginning of the string.
     * @param newText   The new string to be normalized.
     */
    public void setText(UCharacterIterator newText){ 
        try{
	        UCharacterIterator newIter = (UCharacterIterator)newText.clone();
		    if (newIter == null) {
			    throw new InternalError("Could not create a new UCharacterIterator");
		    }
		    text = newIter;
		    reset();
        }catch(CloneNotSupportedException e){
            throw new InternalError("Could not clone the UCharacterIterator");
        }
    }
    
    //-------------------------------------------------------------------------
    // Private utility methods
    //-------------------------------------------------------------------------
    

    /* backward iteration ------------------------------------------------------- */
               
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
        if((ch=src.previous()) == UCharacterIterator.DONE){
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
                return NormalizerImpl.getNorm32FromSurrogatePair(norm32, chars[0]);
            }
        } else {
            /* unpaired second surrogate, undo the c2=src.previous() movement */
            src.moveIndex( 1);
            return 0;
        }
    }
 
     public interface IsPrevBoundary{
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
    
            return NormalizerImpl.isNFDSafe(getPrevNorm32(src, minC, ccOrQCMask, chars), ccOrQCMask, ccOrQCMask& NormalizerImpl.QC_MASK);
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
            return NormalizerImpl.isTrueStarter(norm32, ccOrQCMask, decompQCMask);
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
                   /*int options,*/
                   boolean doNormalize, 
                   boolean[] pNeededToNormalize) {

        IsPrevBoundary isPreviousBoundary;
        int destLength, bufferLength;
        int/*unsigned*/ mask;
        int[] startIndex= new int[1];
        char[] chars= new char[2];
        //int32_t c, c2;
        char minC;
        int destCapacity = destLimit-destStart;
        destLength=0;
        char[] buffer = new char[100];
        
        if(pNeededToNormalize!=null) {
            pNeededToNormalize[0]=false;
        }
        minC = (char)mode.getMinC();
        mask = mode.getMask();
        isPreviousBoundary = mode.getPrevBoundary();

        if(isPreviousBoundary==null){
            destLength=0;
            if((chars[0]=(char)src.previous())>=0) {
                destLength=1;
                if(UTF16.isTrailSurrogate(chars[0])){
                    chars[1]=(char)src.previous();
                    if((int)chars[1]!= UCharacterIterator.DONE){
                        if(UTF16.isLeadSurrogate(chars[1])) {
                            if(destCapacity>=2) {
                                dest[1]=chars[0]; // trail surrogate 
                                destLength=2;
                            }
                            // lead surrogate to be written below 
                            chars[0]=chars[1]; 
                        } else {
                            src.moveIndex(1);
                        }
                    }
                }
    
                if(destCapacity>0) {
                    dest[0]=(char)chars[0];
                }
            }
            return destLength;
         }
    
        bufferLength=findPreviousIterationBoundary(src,
                                                   isPreviousBoundary, 
                                                   minC, mask,buffer, 
                                                   startIndex);
        if(bufferLength>0) {
            if(doNormalize) {
                destLength=Normalizer.normalize(buffer,startIndex[0],
                                     startIndex[0]+bufferLength,
                                     dest, destStart,destLimit,mode);
                
                if(pNeededToNormalize!=null) {
                    pNeededToNormalize[0]=(boolean)(destLength!=bufferLength ||
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
                                                    bufferLength : destCapacity
                                    );
                }
            }
        } 

    
        return destLength;
    }

 
    
    /* forward iteration -------------------------------------------------------- */
    /*
     * read forward and check if the character is a next-iteration boundary
     * if c2!=0 then (c, c2) is a surrogate pair
     */
    public interface IsNextBoundary{
        boolean isNextBoundary(UCharacterIterator src, 
                               int/*unsigned*/ minC, 
                               int/*unsigned*/ mask, 
                               char[] chars);
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
                                                  char[] chars) {
        long norm32;
    
        /* need src.hasNext() to be true */
        chars[0]=(char)src.next();
        chars[1]=0;
    
        if(chars[0]<minC) {
            return 0;
        }
    
        norm32=NormalizerImpl.getNorm32(chars[0]);
        if(UTF16.isLeadSurrogate(chars[0])) {
            if(src.current()!=UCharacterIterator.DONE &&
                        UTF16.isTrailSurrogate(chars[1]=(char)src.current())) {
                src.moveIndex(1); /* skip the c2 surrogate */
                if((norm32&mask)==0) {
                    /* irrelevant data */
                    return 0;
                } else {
                    /* norm32 must be a surrogate special */
                    return NormalizerImpl.getNorm32FromSurrogatePair(norm32, chars[1]);
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
                               char[] chars) {
            return NormalizerImpl.isNFDSafe(getNextNorm32(src, minC, ccOrQCMask, chars), 
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
                               char[] chars) {
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
        char[] chars = new char[2];
        int bufferIndex =0;
        
        if(src.current()==UCharacterIterator.DONE){
            return 0;
        }
        /* get one character and ignore its properties */
        chars[0]=(char)src.next();
        buffer[0]=chars[0];
        bufferIndex=1;
        
        if(UTF16.isLeadSurrogate(chars[0])&& 
                                        src.current()!=UCharacterIterator.DONE){
            if(UTF16.isTrailSurrogate(chars[1]=(char)src.next())) {
                buffer[bufferIndex++]=chars[1];
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
                    buffer[bufferIndex++]=chars[0];
                    if(chars[1]!=0) {
                        buffer[bufferIndex++]=chars[1];
                    }
                }else{
                    char[] newBuf = new char[buffer.length    *2];
                    System.arraycopy(buffer,0,newBuf,0,bufferIndex);
                    buffer = newBuf;
                    buffer[bufferIndex++]=chars[0];
                    if(chars[1]!=0) {
                        buffer[bufferIndex++]=chars[1];
                    }
                }
            }
        }
    
        /* return the length of the buffer contents */
        return bufferIndex;
    }
    
    private static int next(UCharacterIterator src,
                           char[] dest, int destStart, int destLimit,
                           Normalizer.Mode mode, /*int options,*/
                           boolean doNormalize, boolean[] pNeededToNormalize){
        char[] buffer=new char[100];
        IsNextBoundary isNextBoundary;
        int /*unsigned*/ mask;
        int /*unsigned*/ bufferLength;
        char[] chars = new char[2];
        char minC;
        int destCapacity = destLimit - destStart;
        int destLength = 0;
        int[] startIndex = new int[1];
        if(pNeededToNormalize!=null) {
            pNeededToNormalize[0]=false;
        }

        minC = (char)mode.getMinC();
        mask = mode.getMask();
        isNextBoundary = mode.getNextBoundary();
        
        if(isNextBoundary==null){
            destLength=0;
            chars[0]=(char)src.next();
            if((int)chars[0]!=UCharacterIterator.DONE) {
                destLength=1;
                if(UTF16.isLeadSurrogate(chars[0])){
                    chars[1]= (char)src.next();
                    if((int)chars[1]!= UCharacterIterator.DONE) {
                        if(UTF16.isTrailSurrogate(chars[1])) {
                            if(destCapacity>=2) {
                                dest[1]=chars[1]; // trail surrogate 
                                destLength=2;
                            }
                            // lead surrogate to be written below 
                        } else {
                            src.moveIndex(-1);
                        }
                    }
                }
    
                if(destCapacity>0) {
                    dest[0]=chars[0];
                }
            }
            return destLength;
        }
        
        bufferLength=findNextIterationBoundary(src,isNextBoundary, minC, mask,
                                               buffer);
        if(bufferLength>0) {
            if(doNormalize) {
                destLength=mode.dispatch(buffer,startIndex[0],bufferLength,
                                                   dest,destStart,destLimit);
                
                if(pNeededToNormalize!=null) {
                    pNeededToNormalize[0]=(boolean)(destLength!=bufferLength ||
                                Utility.arrayRegionMatches(buffer,startIndex[0],
                                                           dest,destStart,
                                                           destLength));
                }
            } else {
                /* just copy the source characters */
                if(destCapacity>0) {
                    System.arraycopy(buffer,0,dest,destStart,
                                        Math.min(bufferLength,destCapacity)
                                     );
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
	        
		bufferLimit=next(text,buffer,bufferStart,buffer.length,mode,true,null);
	                
		nextIndex=text.getIndex();
		return (bufferLimit>0);
    }
	
    private boolean	previousNormalize() {

		clearBuffer();
		nextIndex=currentIndex;
		text.setIndex(currentIndex);
		bufferLimit=previous(text,buffer,bufferStart,buffer.length,mode,true,null);
		
		currentIndex=text.getIndex();
	    bufferPos = bufferLimit;
		return bufferLimit>0;
    }
    
    private int getCodePointAt(int index){
        if( UTF16.isSurrogate(buffer[index])){
            if(UTF16.isLeadSurrogate(buffer[index])){
                if((index+1)<bufferLimit &&
                                    UTF16.isTrailSurrogate(buffer[index+1])){
		               return UCharacterProperty.getRawSupplementary(
				        	          buffer[index], 
                                      buffer[index+1]
                                  );
                }
            }else if(UTF16.isTrailSurrogate(buffer[index])){
                if(index>0 && UTF16.isLeadSurrogate(buffer[index-1])){
                    return UCharacterProperty.getRawSupplementary(
								     buffer[index-1],
								     buffer[index]
								  );
                }
            }   
        }
        return buffer[index];
        
    }
                  
}
