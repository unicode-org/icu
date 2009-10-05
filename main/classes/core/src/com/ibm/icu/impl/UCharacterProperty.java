/**
*******************************************************************************
* Copyright (C) 1996-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.MissingResourceException;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.VersionInfo;

import com.ibm.icu.impl.NormalizerImpl;

/**
* <p>Internal class used for Unicode character property database.</p>
* <p>This classes store binary data read from uprops.icu.
* It does not have the capability to parse the data into more high-level
* information. It only returns bytes of information when required.</p>
* <p>Due to the form most commonly used for retrieval, array of char is used
* to store the binary data.</p>
* <p>UCharacterPropertyDB also contains information on accessing indexes to
* significant points in the binary data.</p>
* <p>Responsibility for molding the binary data into more meaning form lies on
* <a href=UCharacter.html>UCharacter</a>.</p>
* @author Syn Wee Quek
* @since release 2.1, february 1st 2002
*/

public final class UCharacterProperty
{
    // public data members -----------------------------------------------

    /**
    * Trie data
    */
    public CharTrie m_trie_;
    /**
     * Optimization
     * CharTrie index array
     */
    public char[] m_trieIndex_;
    /**
     * Optimization
     * CharTrie data array
     */
    public char[] m_trieData_;
    /**
     * Optimization
     * CharTrie data offset
     */
    public int m_trieInitialValue_;
    /**
    * Unicode version
    */
    public VersionInfo m_unicodeVersion_;
    /**
    * Latin capital letter i with dot above
    */
    public static final char LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_ = 0x130;
    /**
    * Latin small letter i with dot above
    */
    public static final char LATIN_SMALL_LETTER_DOTLESS_I_ = 0x131;
    /**
    * Latin lowercase i
    */
    public static final char LATIN_SMALL_LETTER_I_ = 0x69;
    /**
    * Character type mask
    */
    public static final int TYPE_MASK = 0x1F;

    // uprops.h enum UPropertySource --------------------------------------- ***

    /** No source, not a supported property. */
    public static final int SRC_NONE=0;
    /** From uchar.c/uprops.icu main trie */
    public static final int SRC_CHAR=1;
    /** From uchar.c/uprops.icu properties vectors trie */
    public static final int SRC_PROPSVEC=2;
    /** Hangul_Syllable_Type, from uchar.c/uprops.icu */
    public static final int SRC_HST=3;
    /** From unames.c/unames.icu */
    public static final int SRC_NAMES=4;
    /** From unorm.cpp/unorm.icu */
    public static final int SRC_NORM=5;
    /** From ucase.c/ucase.icu */
    public static final int SRC_CASE=6;
    /** From ubidi_props.c/ubidi.icu */
    public static final int SRC_BIDI=7;
    /** From uchar.c/uprops.icu main trie as well as properties vectors trie */
    public static final int SRC_CHAR_AND_PROPSVEC=8;
    /** One more than the highest UPropertySource (SRC_) constant. */
    public static final int SRC_COUNT=9;

    // public methods ----------------------------------------------------

    /**
     * Java friends implementation
     */
    public void setIndexData(CharTrie.FriendAgent friendagent)
    {
        m_trieIndex_ = friendagent.getPrivateIndex();
        m_trieData_ = friendagent.getPrivateData();
        m_trieInitialValue_ = friendagent.getPrivateInitialValue();
    }

    /**
    * Gets the property value at the index.
    * This is optimized.
    * Note this is a little different from CharTrie the index m_trieData_
    * is never negative.
    * @param ch code point whose property value is to be retrieved
    * @return property value of code point
    */
    public final int getProperty(int ch)
    {
        if (ch < UTF16.LEAD_SURROGATE_MIN_VALUE
            || (ch > UTF16.LEAD_SURROGATE_MAX_VALUE
                && ch < UTF16.SUPPLEMENTARY_MIN_VALUE)) {
            // BMP codepoint 0000..D7FF or DC00..FFFF
            // optimized
            try { // using try for ch < 0 is faster than using an if statement
                return m_trieData_[
                    (m_trieIndex_[ch >> Trie.INDEX_STAGE_1_SHIFT_]
                          << Trie.INDEX_STAGE_2_SHIFT_)
                    + (ch & Trie.INDEX_STAGE_3_MASK_)];
            } catch (ArrayIndexOutOfBoundsException e) {
                return m_trieInitialValue_;
            }
        }
        if (ch <= UTF16.LEAD_SURROGATE_MAX_VALUE) {
            // lead surrogate D800..DBFF
            return m_trieData_[
                    (m_trieIndex_[Trie.LEAD_INDEX_OFFSET_
                                  + (ch >> Trie.INDEX_STAGE_1_SHIFT_)]
                          << Trie.INDEX_STAGE_2_SHIFT_)
                    + (ch & Trie.INDEX_STAGE_3_MASK_)];
        }
        if (ch <= UTF16.CODEPOINT_MAX_VALUE) {
            // supplementary code point 10000..10FFFF
            // look at the construction of supplementary characters
            // trail forms the ends of it.
            return m_trie_.getSurrogateValue(
                                          UTF16.getLeadSurrogate(ch),
                                          (char)(ch & Trie.SURROGATE_MASK_));
        }
        // ch is out of bounds
        // return m_dataOffset_ if there is an error, in this case we return
        // the default value: m_initialValue_
        // we cannot assume that m_initialValue_ is at offset 0
        // this is for optimization.
        return m_trieInitialValue_;

        // this all is an inlined form of return m_trie_.getCodePointValue(ch);
    }

    /*
    * Getting the signed numeric value of a character embedded in the property
    * argument
    * @param prop the character
    * @return signed numberic value
    */
//    public static int getSignedValue(int prop)
//    {
//        return ((short)prop >> VALUE_SHIFT_);
//    }

    /**
    * Getting the unsigned numeric value of a character embedded in the property
    * argument
    * @param prop the character
    * @return unsigned numberic value
    */
    public static int getUnsignedValue(int prop)
    {
        return (prop >> VALUE_SHIFT_) & UNSIGNED_VALUE_MASK_AFTER_SHIFT_;
    }

    /* internal numeric pseudo-types for special encodings of numeric values */
    public static final int NT_FRACTION=4; /* ==UCharacter.NumericType.COUNT, must not change unless binary format version changes */
    public static final int NT_LARGE=5;
    public static final int NT_COUNT=6;

    /**
     * Gets the unicode additional properties.
     * C version getUnicodeProperties.
     * @param codepoint codepoint whose additional properties is to be
     *                  retrieved
     * @param column The column index.
     * @return unicode properties
     */
       public int getAdditional(int codepoint, int column) {
        if (column == -1) {
            return getProperty(codepoint);
        }
           if (column < 0 || column >= m_additionalColumnsCount_) {
           return 0;
       }
       return m_additionalVectors_[
                     m_additionalTrie_.getCodePointValue(codepoint) + column];
       }

    static final int MY_MASK = UCharacterProperty.TYPE_MASK
        & ((1<<UCharacterCategory.UPPERCASE_LETTER) |
            (1<<UCharacterCategory.LOWERCASE_LETTER) |
            (1<<UCharacterCategory.TITLECASE_LETTER) |
            (1<<UCharacterCategory.MODIFIER_LETTER) |
            (1<<UCharacterCategory.OTHER_LETTER));


       /**
     * <p>Get the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a
     * character.</p>
     * <p>This can be useful to avoid emitting code points to receiving
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * <p>This API does not check the validity of the codepoint.</p>
     * @param codepoint The code point.
     * @return the Unicode version number
     */
    public VersionInfo getAge(int codepoint)
    {
        int version = getAdditional(codepoint, 0) >> AGE_SHIFT_;
        return VersionInfo.getInstance(
                           (version >> FIRST_NIBBLE_SHIFT_) & LAST_NIBBLE_MASK_,
                           version & LAST_NIBBLE_MASK_, 0, 0);
    }

    private static final long UNSIGNED_INT_MASK = 0xffffffffL;

    private static final int GC_CN_MASK = getMask(UCharacter.UNASSIGNED);
    private static final int GC_CC_MASK = getMask(UCharacter.CONTROL);
    private static final int GC_CS_MASK = getMask(UCharacter.SURROGATE);
    private static final int GC_ZS_MASK = getMask(UCharacter.SPACE_SEPARATOR);
    private static final int GC_ZL_MASK = getMask(UCharacter.LINE_SEPARATOR);
    private static final int GC_ZP_MASK = getMask(UCharacter.PARAGRAPH_SEPARATOR);
    /** Mask constant for multiple UCharCategory bits (Z Separators). */
    private static final int GC_Z_MASK = GC_ZS_MASK|GC_ZL_MASK|GC_ZP_MASK;

    /**
     * Checks if c is in
     * [^\p{space}\p{gc=Control}\p{gc=Surrogate}\p{gc=Unassigned}]
     * with space=\p{Whitespace} and Control=Cc.
     * Implements UCHAR_POSIX_GRAPH.
     * @internal
     */
    private static final boolean isgraphPOSIX(int c) {
        /* \p{space}\p{gc=Control} == \p{gc=Z}\p{Control} */
        /* comparing ==0 returns FALSE for the categories mentioned */
        return (getMask(UCharacter.getType(c))&
                (GC_CC_MASK|GC_CS_MASK|GC_CN_MASK|GC_Z_MASK))
               ==0;
    }

    private static final class BinaryProperties{
       int column;
       long mask;
       public BinaryProperties(int column,long mask){
               this.column = column;
               this.mask  = mask;
       }
   }
   BinaryProperties[] binProps={
       /*
        * column and mask values for binary properties from u_getUnicodeProperties().
        * Must be in order of corresponding UProperty,
        * and there must be exacly one entry per binary UProperty.
        */
       new BinaryProperties(  1,                (  1 << ALPHABETIC_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << ASCII_HEX_DIGIT_PROPERTY_) ),
       new BinaryProperties( SRC_BIDI,   0 ),                                       /* UCHAR_BIDI_CONTROL */
       new BinaryProperties( SRC_BIDI,   0 ),                                       /* UCHAR_BIDI_MIRRORED */
       new BinaryProperties(  1,                (  1 << DASH_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << DEPRECATED_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << DIACRITIC_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << EXTENDER_PROPERTY_) ),
       new BinaryProperties( SRC_NORM,   0 ),                                       /* UCHAR_FULL_COMPOSITION_EXCLUSION */
       new BinaryProperties(  1,                (  1 << GRAPHEME_BASE_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << GRAPHEME_EXTEND_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << GRAPHEME_LINK_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << HEX_DIGIT_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << HYPHEN_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << ID_CONTINUE_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << ID_START_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << IDEOGRAPHIC_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << IDS_BINARY_OPERATOR_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << IDS_TRINARY_OPERATOR_PROPERTY_) ),
       new BinaryProperties( SRC_BIDI,   0 ),                                       /* UCHAR_JOIN_CONTROL */
       new BinaryProperties(  1,                (  1 << LOGICAL_ORDER_EXCEPTION_PROPERTY_) ),
       new BinaryProperties( SRC_CASE,   0 ),                                       /* UCHAR_LOWERCASE */
       new BinaryProperties(  1,                (  1 << MATH_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << NONCHARACTER_CODE_POINT_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << QUOTATION_MARK_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << RADICAL_PROPERTY_) ),
       new BinaryProperties( SRC_CASE,   0 ),                                       /* UCHAR_SOFT_DOTTED */
       new BinaryProperties(  1,                (  1 << TERMINAL_PUNCTUATION_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << UNIFIED_IDEOGRAPH_PROPERTY_) ),
       new BinaryProperties( SRC_CASE,   0 ),                                       /* UCHAR_UPPERCASE */
       new BinaryProperties(  1,                (  1 << WHITE_SPACE_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << XID_CONTINUE_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << XID_START_PROPERTY_) ),
       new BinaryProperties( SRC_CASE,   0 ),                                       /* UCHAR_CASE_SENSITIVE */
       new BinaryProperties(  1,                (  1 << S_TERM_PROPERTY_) ),
       new BinaryProperties(  1,                (  1 << VARIATION_SELECTOR_PROPERTY_) ),
       new BinaryProperties( SRC_NORM,   0 ),                                       /* UCHAR_NFD_INERT */
       new BinaryProperties( SRC_NORM,   0 ),                                       /* UCHAR_NFKD_INERT */
       new BinaryProperties( SRC_NORM,   0 ),                                       /* UCHAR_NFC_INERT */
       new BinaryProperties( SRC_NORM,   0 ),                                       /* UCHAR_NFKC_INERT */
       new BinaryProperties( SRC_NORM,   0 ),                                       /* UCHAR_SEGMENT_STARTER */
       new BinaryProperties(  1,                (  1 << PATTERN_SYNTAX) ),
       new BinaryProperties(  1,                (  1 << PATTERN_WHITE_SPACE) ),
       new BinaryProperties( SRC_CHAR_AND_PROPSVEC,  0 ),                           /* UCHAR_POSIX_ALNUM */
       new BinaryProperties( SRC_CHAR,  0 ),                                        /* UCHAR_POSIX_BLANK */
       new BinaryProperties( SRC_CHAR,  0 ),                                        /* UCHAR_POSIX_GRAPH */
       new BinaryProperties( SRC_CHAR,  0 ),                                        /* UCHAR_POSIX_PRINT */
       new BinaryProperties( SRC_CHAR,  0 )                                         /* UCHAR_POSIX_XDIGIT */
   };


    /**
     * <p>Check a binary Unicode property for a code point.</p>
     * <p>Unicode, especially in version 3.2, defines many more properties
     * than the original set in UnicodeData.txt.</p>
     * <p>This API is intended to reflect Unicode properties as defined in
     * the Unicode Character Database (UCD) and Unicode Technical Reports
     * (UTR).</p>
     * <p>For details about the properties see
     * <a href=http://www.unicode.org/>http://www.unicode.org/</a>.</p>
     * <p>For names of Unicode properties see the UCD file
     * PropertyAliases.txt.</p>
     * <p>This API does not check the validity of the codepoint.</p>
     * <p>Important: If ICU is built with UCD files from Unicode versions
     * below 3.2, then properties marked with "new" are not or
     * not fully available.</p>
     * @param codepoint Code point to test.
     * @param property selector constant from com.ibm.icu.lang.UProperty,
     *        identifies which binary property to check.
     * @return true or false according to the binary Unicode property value
     *         for ch. Also false if property is out of bounds or if the
     *         Unicode version does not have data for the property at all, or
     *         not for this code point.
     * @see com.ibm.icu.lang.UProperty
     */

    public boolean hasBinaryProperty(int codepoint, int property)
    {
         if(property <UProperty.BINARY_START || UProperty.BINARY_LIMIT<=property) {
            // not a known binary property
            return false;
        } else {
            long mask=binProps[property].mask;
            int column=binProps[property].column;
            if(mask!=0) {
                // systematic, directly stored properties
                return ((UNSIGNED_INT_MASK & getAdditional(codepoint, column)) & mask)!=0;
            } else {
                if(column==SRC_CASE) {
                    /* case mapping properties */
                    UCaseProps csp;
                    try {
                        csp = UCaseProps.getSingleton();
                    } catch (IOException e) {
                        return false;
                    }
                    switch(property) {
                    case UProperty.LOWERCASE:
                        return UCaseProps.LOWER==csp.getType(codepoint);
                    case UProperty.UPPERCASE:
                        return UCaseProps.UPPER==csp.getType(codepoint);
                    case UProperty.SOFT_DOTTED:
                        return csp.isSoftDotted(codepoint);
                    case UProperty.CASE_SENSITIVE:
                        return csp.isCaseSensitive(codepoint);
                    default:
                        break;
                    }
                } else if(column==SRC_NORM) {
                    /* normalization properties from unorm.icu */
                    switch(property) {
                    case UProperty.FULL_COMPOSITION_EXCLUSION:
                        return NormalizerImpl.isFullCompositionExclusion(codepoint);
                    case UProperty.NFD_INERT:
                        return Normalizer.isNFSkippable(codepoint, Normalizer.NFD);
                    case UProperty.NFKD_INERT:
                        return Normalizer.isNFSkippable(codepoint, Normalizer.NFKD);
                    case UProperty.NFC_INERT:
                        return Normalizer.isNFSkippable(codepoint, Normalizer.NFC);
                    case UProperty.NFKC_INERT:
                        return Normalizer.isNFSkippable(codepoint, Normalizer.NFKC);
                    case UProperty.SEGMENT_STARTER:
                        return NormalizerImpl.isCanonSafeStart(codepoint);
                    default:
                        break;
                    }
                } else if(column==SRC_BIDI) {
                    /* bidi/shaping properties */
                    UBiDiProps bdp;
                    try {
                        bdp = UBiDiProps.getSingleton();
                    } catch (IOException e) {
                        return false;
                    }
                    switch(property) {
                    case UProperty.BIDI_MIRRORED:
                        return bdp.isMirrored(codepoint);
                    case UProperty.BIDI_CONTROL:
                        return bdp.isBidiControl(codepoint);
                    case UProperty.JOIN_CONTROL:
                        return bdp.isJoinControl(codepoint);
                    default:
                        break;
                    }
                } else if(column==SRC_CHAR) {
                    switch(property) {
                    case UProperty.POSIX_BLANK:
                        // "horizontal space"
                        if(codepoint<=0x9f) {
                            return codepoint==9 || codepoint==0x20; /* TAB or SPACE */
                        } else {
                            /* Zs */
                            return UCharacter.getType(codepoint)==UCharacter.SPACE_SEPARATOR;
                        }
                    case UProperty.POSIX_GRAPH:
                        return isgraphPOSIX(codepoint);
                    case UProperty.POSIX_PRINT:
                        /*
                         * Checks if codepoint is in \p{graph}\p{blank} - \p{cntrl}.
                         *
                         * The only cntrl character in graph+blank is TAB (in blank).
                         * Here we implement (blank-TAB)=Zs instead of calling u_isblank().
                         */
                        return (UCharacter.getType(codepoint)==UCharacter.SPACE_SEPARATOR) || isgraphPOSIX(codepoint);
                    case UProperty.POSIX_XDIGIT:
                        /* check ASCII and Fullwidth ASCII a-fA-F */
                        if(
                            (codepoint<=0x66 && codepoint>=0x41 && (codepoint<=0x46 || codepoint>=0x61)) ||
                            (codepoint>=0xff21 && codepoint<=0xff46 && (codepoint<=0xff26 || codepoint>=0xff41))
                        ) {
                            return true;
                        }
    
                        return UCharacter.getType(codepoint)==UCharacter.DECIMAL_DIGIT_NUMBER;
                    default:
                        break;
                    }
                } else if(column==SRC_CHAR_AND_PROPSVEC) {
                    switch(property) {
                    case UProperty.POSIX_ALNUM:
                        return UCharacter.isUAlphabetic(codepoint) || UCharacter.isDigit(codepoint);
                    default:
                        break;
                    }
                }
            }
        }
        return false;
    }

    public final int getSource(int which) {
        if(which<UProperty.BINARY_START) {
            return SRC_NONE; /* undefined */
        } else if(which<UProperty.BINARY_LIMIT) {
            if(binProps[which].mask!=0) {
                return SRC_PROPSVEC;
            } else {
                return binProps[which].column;
            }
        } else if(which<UProperty.INT_START) {
            return SRC_NONE; /* undefined */
        } else if(which<UProperty.INT_LIMIT) {
            switch(which) {
            case UProperty.GENERAL_CATEGORY:
            case UProperty.NUMERIC_TYPE:
                return SRC_CHAR;

            case UProperty.HANGUL_SYLLABLE_TYPE:
                return SRC_HST;

            case UProperty.CANONICAL_COMBINING_CLASS:
            case UProperty.NFD_QUICK_CHECK:
            case UProperty.NFKD_QUICK_CHECK:
            case UProperty.NFC_QUICK_CHECK:
            case UProperty.NFKC_QUICK_CHECK:
            case UProperty.LEAD_CANONICAL_COMBINING_CLASS:
            case UProperty.TRAIL_CANONICAL_COMBINING_CLASS:
                return SRC_NORM;

            case UProperty.BIDI_CLASS:
            case UProperty.JOINING_GROUP:
            case UProperty.JOINING_TYPE:
                return SRC_BIDI;

            default:
                return SRC_PROPSVEC;
            }
        } else if(which<UProperty.STRING_START) {
            switch(which) {
            case UProperty.GENERAL_CATEGORY_MASK:
            case UProperty.NUMERIC_VALUE:
                return SRC_CHAR;

            default:
                return SRC_NONE;
            }
        } else if(which<UProperty.STRING_LIMIT) {
            switch(which) {
            case UProperty.AGE:
                return SRC_PROPSVEC;

            case UProperty.BIDI_MIRRORING_GLYPH:
                return SRC_BIDI;

            case UProperty.CASE_FOLDING:
            case UProperty.LOWERCASE_MAPPING:
            case UProperty.SIMPLE_CASE_FOLDING:
            case UProperty.SIMPLE_LOWERCASE_MAPPING:
            case UProperty.SIMPLE_TITLECASE_MAPPING:
            case UProperty.SIMPLE_UPPERCASE_MAPPING:
            case UProperty.TITLECASE_MAPPING:
            case UProperty.UPPERCASE_MAPPING:
                return SRC_CASE;

            case UProperty.ISO_COMMENT:
            case UProperty.NAME:
            case UProperty.UNICODE_1_NAME:
                return SRC_NAMES;

            default:
                return SRC_NONE;
            }
        } else {
            return SRC_NONE; /* undefined */
        }
    }

    /**
    * Forms a supplementary code point from the argument character<br>
    * Note this is for internal use hence no checks for the validity of the
    * surrogate characters are done
    * @param lead lead surrogate character
    * @param trail trailing surrogate character
    * @return code point of the supplementary character
    */
    public static int getRawSupplementary(char lead, char trail)
    {
        return (lead << LEAD_SURROGATE_SHIFT_) + trail + SURROGATE_OFFSET_;
    }

    /**
    * Loads the property data and initialize the UCharacterProperty instance.
    * @throws MissingResourceException when data is missing or data has been corrupted
    */
    public static UCharacterProperty getInstance()
    {
        if(INSTANCE_ == null) {
            try {
                INSTANCE_ = new UCharacterProperty();
            }
            catch (Exception e) {
                throw new MissingResourceException(e.getMessage(),"","");
            }
        }
        return INSTANCE_;
    }

    /**
     * <p>
     * Unicode property names and property value names are compared
     * "loosely". Property[Value]Aliases.txt say:
     * <quote>
     *   "With loose matching of property names, the case distinctions,
     *    whitespace, and '_' are ignored."
     * </quote>
     * </p>
     * <p>
     * This function does just that, for ASCII (char *) name strings.
     * It is almost identical to ucnv_compareNames() but also ignores
     * ASCII White_Space characters (U+0009..U+000d).
     * </p>
     * @param name1 name to compare
     * @param name2 name to compare
     * @return 0 if names are equal, < 0 if name1 is less than name2 and > 0
     *         if name1 is greater than name2.
     */
    /* to be implemented in 2.4
     * public static int comparePropertyNames(String name1, String name2)
    {
        int result = 0;
        int i1 = 0;
        int i2 = 0;
        while (true) {
            char ch1 = 0;
            char ch2 = 0;
            // Ignore delimiters '-', '_', and ASCII White_Space
            if (i1 < name1.length()) {
                ch1 = name1.charAt(i1 ++);
            }
            while (ch1 == '-' || ch1 == '_' || ch1 == ' ' || ch1 == '\t'
                   || ch1 == '\n' // synwee what is || ch1 == '\v'
                   || ch1 == '\f' || ch1=='\r') {
                if (i1 < name1.length()) {
                    ch1 = name1.charAt(i1 ++);
                }
                else {
                    ch1 = 0;
                }
            }
            if (i2 < name2.length()) {
                ch2 = name2.charAt(i2 ++);
            }
            while (ch2 == '-' || ch2 == '_' || ch2 == ' ' || ch2 == '\t'
                   || ch2 == '\n' // synwee what is || ch1 == '\v'
                   || ch2 == '\f' || ch2=='\r') {
                if (i2 < name2.length()) {
                    ch2 = name2.charAt(i2 ++);
                }
                else {
                    ch2 = 0;
                }
            }

            // If we reach the ends of both strings then they match
            if (ch1 == 0 && ch2 == 0) {
                return 0;
            }

            // Case-insensitive comparison
            if (ch1 != ch2) {
                result = Character.toLowerCase(ch1)
                                                - Character.toLowerCase(ch2);
                if (result != 0) {
                    return result;
                }
            }
        }
    }
    */

    /**
     * Checks if the argument c is to be treated as a white space in ICU
     * rules. Usually ICU rule white spaces are ignored unless quoted.
     * Equivalent to test for Pattern_White_Space Unicode property.
     * Stable set of characters, won't change.
     * See UAX #31 Identifier and Pattern Syntax: http://www.unicode.org/reports/tr31/
     * @param c codepoint to check
     * @return true if c is a ICU white space
     */
    public static boolean isRuleWhiteSpace(int c)
    {
        /* "white space" in the sense of ICU rule parsers
           This is a FIXED LIST that is NOT DEPENDENT ON UNICODE PROPERTIES.
           See UAX #31 Identifier and Pattern Syntax: http://www.unicode.org/reports/tr31/
           U+0009..U+000D, U+0020, U+0085, U+200E..U+200F, and U+2028..U+2029
           Equivalent to test for Pattern_White_Space Unicode property.
        */
        return (c >= 0x0009 && c <= 0x2029 &&
                (c <= 0x000D || c == 0x0020 || c == 0x0085 ||
                 c == 0x200E || c == 0x200F || c >= 0x2028));
    }

    /**
     * Get the the maximum values for some enum/int properties.
     * @return maximum values for the integer properties.
     */
    public int getMaxValues(int column)
    {
       // return m_maxBlockScriptValue_;

        switch(column) {
        case 0:
            return m_maxBlockScriptValue_;
        case 2:
            return m_maxJTGValue_;
        default:
            return 0;
        }
    }

    /**
     * Gets the type mask
     * @param type character type
     * @return mask
     */
    public static final int getMask(int type)
    {
        return 1 << type;
    }

    // protected variables -----------------------------------------------

    /**
     * Extra property trie
     */
    CharTrie m_additionalTrie_;
    /**
     * Extra property vectors, 1st column for age and second for binary
     * properties.
     */
    int m_additionalVectors_[];
    /**
     * Number of additional columns
     */
    int m_additionalColumnsCount_;
    /**
     * Maximum values for block, bits used as in vector word
     * 0
     */
    int m_maxBlockScriptValue_;
    /**
     * Maximum values for script, bits used as in vector word
     * 0
     */
     int m_maxJTGValue_;
    // private variables -------------------------------------------------

      /**
     * UnicodeData.txt property object
     */
    private static UCharacterProperty INSTANCE_ = null;

    /**
    * Default name of the datafile
    */
    private static final String DATA_FILE_NAME_ = ICUResourceBundle.ICU_BUNDLE+"/uprops.icu";

    /**
    * Default buffer size of datafile
    */
    private static final int DATA_BUFFER_SIZE_ = 25000;

    /**
    * Numeric value shift
    */
    private static final int VALUE_SHIFT_ = 8;

    /**
    * Mask to be applied after shifting to obtain an unsigned numeric value
    */
    private static final int UNSIGNED_VALUE_MASK_AFTER_SHIFT_ = 0xFF;

    /*
     *
     */
    //private static final int NUMERIC_TYPE_SHIFT = 5;

    /*
    * To get the last 5 bits out from a data type
    */
    //private static final int LAST_5_BIT_MASK_ = 0x1F;

    /**
    * Shift value for lead surrogate to form a supplementary character.
    */
    private static final int LEAD_SURROGATE_SHIFT_ = 10;
    /**
    * Offset to add to combined surrogate pair to avoid msking.
    */
    private static final int SURROGATE_OFFSET_ =
                           UTF16.SUPPLEMENTARY_MIN_VALUE -
                           (UTF16.SURROGATE_MIN_VALUE <<
                           LEAD_SURROGATE_SHIFT_) -
                           UTF16.TRAIL_SURROGATE_MIN_VALUE;


    // additional properties ----------------------------------------------

    /**
     * Additional properties used in internal trie data
     */
    /*
     * Properties in vector word 1
     * Each bit encodes one binary property.
     * The following constants represent the bit number, use 1<<UPROPS_XYZ.
     * UPROPS_BINARY_1_TOP<=32!
     *
     * Keep this list of property enums in sync with
     * propListNames[] in icu/source/tools/genprops/props2.c!
     *
     * ICU 2.6/uprops format version 3.2 stores full properties instead of "Other_".
     */
    private static final int WHITE_SPACE_PROPERTY_ = 0;
    private static final int DASH_PROPERTY_ = 1;
    private static final int HYPHEN_PROPERTY_ = 2;
    private static final int QUOTATION_MARK_PROPERTY_ = 3;
    private static final int TERMINAL_PUNCTUATION_PROPERTY_ = 4;
    private static final int MATH_PROPERTY_ = 5;
    private static final int HEX_DIGIT_PROPERTY_ = 6;
    private static final int ASCII_HEX_DIGIT_PROPERTY_ = 7;
    private static final int ALPHABETIC_PROPERTY_ = 8;
    private static final int IDEOGRAPHIC_PROPERTY_ = 9;
    private static final int DIACRITIC_PROPERTY_ = 10;
    private static final int EXTENDER_PROPERTY_ = 11;
    private static final int NONCHARACTER_CODE_POINT_PROPERTY_ = 12;
    private static final int GRAPHEME_EXTEND_PROPERTY_ = 13;
    private static final int GRAPHEME_LINK_PROPERTY_ = 14;
    private static final int IDS_BINARY_OPERATOR_PROPERTY_ = 15;
    private static final int IDS_TRINARY_OPERATOR_PROPERTY_ = 16;
    private static final int RADICAL_PROPERTY_ = 17;
    private static final int UNIFIED_IDEOGRAPH_PROPERTY_ = 18;
    private static final int DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_ = 19;
    private static final int DEPRECATED_PROPERTY_ = 20;
    private static final int LOGICAL_ORDER_EXCEPTION_PROPERTY_ = 21;
    private static final int XID_START_PROPERTY_ = 22;
    private static final int XID_CONTINUE_PROPERTY_ = 23;
    private static final int ID_START_PROPERTY_    = 24;
    private static final int ID_CONTINUE_PROPERTY_ = 25;
    private static final int GRAPHEME_BASE_PROPERTY_ = 26;
    private static final int S_TERM_PROPERTY_ = 27;
    private static final int VARIATION_SELECTOR_PROPERTY_ = 28;
    private static final int PATTERN_SYNTAX = 29;                   /* new in ICU 3.4 and Unicode 4.1 */
    private static final int PATTERN_WHITE_SPACE = 30;

    /**
     * First nibble shift
     */
    private static final int FIRST_NIBBLE_SHIFT_ = 0x4;
    /**
     * Second nibble mask
     */
    private static final int LAST_NIBBLE_MASK_ = 0xF;
    /**
     * Age value shift
     */
    private static final int AGE_SHIFT_ = 24;


    // private constructors --------------------------------------------------

    /**
    * Constructor
    * @exception IOException thrown when data reading fails or data corrupted
    */
    private UCharacterProperty() throws IOException
    {
        // jar access
        InputStream is = ICUData.getRequiredStream(DATA_FILE_NAME_);
        BufferedInputStream b = new BufferedInputStream(is, DATA_BUFFER_SIZE_);
        UCharacterPropertyReader reader = new UCharacterPropertyReader(b);
        reader.read(this);
        b.close();

        m_trie_.putIndexData(this);
    }

    // private methods -------------------------------------------------------

    /*
     * Compare additional properties to see if it has argument type
     * @param property 32 bit properties
     * @param type character type
     * @return true if property has type
     */
    /*private boolean compareAdditionalType(int property, int type)
    {
        return (property & (1 << type)) != 0;
    }*/

    // property starts for UnicodeSet -------------------------------------- ***

    private static final int TAB     = 0x0009;
    //private static final int LF      = 0x000a;
    //private static final int FF      = 0x000c;
    private static final int CR      = 0x000d;
    private static final int U_A     = 0x0041;
    private static final int U_F     = 0x0046;
    private static final int U_Z     = 0x005a;
    private static final int U_a     = 0x0061;
    private static final int U_f     = 0x0066;
    private static final int U_z     = 0x007a;
    private static final int DEL     = 0x007f;
    private static final int NL      = 0x0085;
    private static final int NBSP    = 0x00a0;
    private static final int CGJ     = 0x034f;
    private static final int FIGURESP= 0x2007;
    private static final int HAIRSP  = 0x200a;
    //private static final int ZWNJ    = 0x200c;
    //private static final int ZWJ     = 0x200d;
    private static final int RLM     = 0x200f;
    private static final int NNBSP   = 0x202f;
    private static final int WJ      = 0x2060;
    private static final int INHSWAP = 0x206a;
    private static final int NOMDIG  = 0x206f;
    private static final int U_FW_A  = 0xff21;
    private static final int U_FW_F  = 0xff26;
    private static final int U_FW_Z  = 0xff3a;
    private static final int U_FW_a  = 0xff41;
    private static final int U_FW_f  = 0xff46;
    private static final int U_FW_z  = 0xff5a;
    private static final int ZWNBSP  = 0xfeff;

    /* for Hangul_Syllable_Type */
    public void uhst_addPropertyStarts(UnicodeSet set) {
        /* add code points with hardcoded properties, plus the ones following them */

        /*
         * Add Jamo type boundaries for UCHAR_HANGUL_SYLLABLE_TYPE.
         * First, we add fixed boundaries for the blocks of Jamos.
         * Then we check in loops to see where the current Unicode version
         * actually stops assigning such Jamos. We start each loop
         * at the end of the per-Jamo-block assignments in Unicode 4 or earlier.
         * (These have not changed since Unicode 2.)
         */
        int c, value, value2;

        set.add(0x1100);
        value=UCharacter.HangulSyllableType.LEADING_JAMO;
        for(c=0x115a; c<=0x115f; ++c) {
            value2= UCharacter.getIntPropertyValue(c, UProperty.HANGUL_SYLLABLE_TYPE);
            if(value!=value2) {
                value=value2;
                set.add(c);
            }
        }

        set.add(0x1160);
        value=UCharacter.HangulSyllableType.VOWEL_JAMO;
        for(c=0x11a3; c<=0x11a7; ++c) {
            value2=UCharacter.getIntPropertyValue(c, UProperty.HANGUL_SYLLABLE_TYPE);
            if(value!=value2) {
                value=value2;
                set.add(c);
            }
        }

        set.add(0x11a8);
        value=UCharacter.HangulSyllableType.TRAILING_JAMO;
        for(c=0x11fa; c<=0x11ff; ++c) {
            value2=UCharacter.getIntPropertyValue(c, UProperty.HANGUL_SYLLABLE_TYPE);
            if(value!=value2) {
                value=value2;
                set.add(c);
            }
        }

        /* Add Hangul type boundaries for UCHAR_HANGUL_SYLLABLE_TYPE. */
        for(c=NormalizerImpl.HANGUL_BASE; c<(NormalizerImpl.HANGUL_BASE+NormalizerImpl.HANGUL_COUNT); c+=NormalizerImpl.JAMO_T_COUNT) {
            set.add(c);
            set.add(c+1);
        }
        set.add(c);
    }

    public UnicodeSet addPropertyStarts(UnicodeSet set) {
        /* add the start code point of each same-value range of the main trie */
        TrieIterator propsIter = new TrieIterator(m_trie_);
        RangeValueIterator.Element propsResult = new RangeValueIterator.Element();
          while(propsIter.next(propsResult)){
            set.add(propsResult.start);
        }

        /* add code points with hardcoded properties, plus the ones following them */

        /* add for u_isblank() */
        set.add(TAB);
        set.add(TAB+1);

        /* add for IS_THAT_CONTROL_SPACE() */
        set.add(CR+1); /* range TAB..CR */
        set.add(0x1c);
        set.add(0x1f+1);
        set.add(NL);
        set.add(NL+1);

        /* add for u_isIDIgnorable() what was not added above */
        set.add(DEL); /* range DEL..NBSP-1, NBSP added below */
        set.add(HAIRSP);
        set.add(RLM+1);
        set.add(INHSWAP);
        set.add(NOMDIG+1);
        set.add(ZWNBSP);
        set.add(ZWNBSP+1);

        /* add no-break spaces for u_isWhitespace() what was not added above */
        set.add(NBSP);
        set.add(NBSP+1);
        set.add(FIGURESP);
        set.add(FIGURESP+1);
        set.add(NNBSP);
        set.add(NNBSP+1);

        /* add for u_charDigitValue() */
        // TODO remove when UCharacter.getHanNumericValue() is changed to just return
        // Unicode numeric values 
        set.add(0x3007);
        set.add(0x3008);
        set.add(0x4e00);
        set.add(0x4e01);
        set.add(0x4e8c);
        set.add(0x4e8d);
        set.add(0x4e09);
        set.add(0x4e0a);
        set.add(0x56db);
        set.add(0x56dc);
        set.add(0x4e94);
        set.add(0x4e95);
        set.add(0x516d);
        set.add(0x516e);
        set.add(0x4e03);
        set.add(0x4e04);
        set.add(0x516b);
        set.add(0x516c);
        set.add(0x4e5d);
        set.add(0x4e5e);

        /* add for u_digit() */
        set.add(U_a);
        set.add(U_z+1);
        set.add(U_A);
        set.add(U_Z+1);
        set.add(U_FW_a);
        set.add(U_FW_z+1);
        set.add(U_FW_A);
        set.add(U_FW_Z+1);

        /* add for u_isxdigit() */
        set.add(U_f+1);
        set.add(U_F+1);
        set.add(U_FW_f+1);
        set.add(U_FW_F+1);

        /* add for UCHAR_DEFAULT_IGNORABLE_CODE_POINT what was not added above */
        set.add(WJ); /* range WJ..NOMDIG */
        set.add(0xfff0);
        set.add(0xfffb+1);
        set.add(0xe0000);
        set.add(0xe0fff+1);

        /* add for UCHAR_GRAPHEME_BASE and others */
        set.add(CGJ);
        set.add(CGJ+1);

        return set; // for chaining
    }

    public void upropsvec_addPropertyStarts(UnicodeSet set) {
        /* add the start code point of each same-value range of the properties vectors trie */
        if(m_additionalColumnsCount_>0) {
            /* if m_additionalColumnsCount_==0 then the properties vectors trie may not be there at all */
            TrieIterator propsVectorsIter = new TrieIterator(m_additionalTrie_);
            RangeValueIterator.Element propsVectorsResult = new RangeValueIterator.Element();
            while(propsVectorsIter.next(propsVectorsResult)){
                set.add(propsVectorsResult.start);
            }
        }
    }

/*----------------------------------------------------------------
 * Inclusions list
 *----------------------------------------------------------------*/

    /*
     * Return a set of characters for property enumeration.
     * The set implicitly contains 0x110000 as well, which is one more than the highest
     * Unicode code point.
     *
     * This set is used as an ordered list - its code points are ordered, and
     * consecutive code points (in Unicode code point order) in the set define a range.
     * For each two consecutive characters (start, limit) in the set,
     * all of the UCD/normalization and related properties for
     * all code points start..limit-1 are all the same,
     * except for character names and ISO comments.
     *
     * All Unicode code points U+0000..U+10ffff are covered by these ranges.
     * The ranges define a partition of the Unicode code space.
     * ICU uses the inclusions set to enumerate properties for generating
     * UnicodeSets containing all code points that have a certain property value.
     *
     * The Inclusion List is generated from the UCD. It is generated
     * by enumerating the data tries, and code points for hardcoded properties
     * are added as well.
     *
     * --------------------------------------------------------------------------
     *
     * The following are ideas for getting properties-unique code point ranges,
     * with possible optimizations beyond the current implementation.
     * These optimizations would require more code and be more fragile.
     * The current implementation generates one single list (set) for all properties.
     *
     * To enumerate properties efficiently, one needs to know ranges of
     * repetitive values, so that the value of only each start code point
     * can be applied to the whole range.
     * This information is in principle available in the uprops.icu/unorm.icu data.
     *
     * There are two obstacles:
     *
     * 1. Some properties are computed from multiple data structures,
     *    making it necessary to get repetitive ranges by intersecting
     *    ranges from multiple tries.
     *
     * 2. It is not economical to write code for getting repetitive ranges
     *    that are precise for each of some 50 properties.
     *
     * Compromise ideas:
     *
     * - Get ranges per trie, not per individual property.
     *   Each range contains the same values for a whole group of properties.
     *   This would generate currently five range sets, two for uprops.icu tries
     *   and three for unorm.icu tries.
     *
     * - Combine sets of ranges for multiple tries to get sufficient sets
     *   for properties, e.g., the uprops.icu main and auxiliary tries
     *   for all non-normalization properties.
     *
     * Ideas for representing ranges and combining them:
     *
     * - A UnicodeSet could hold just the start code points of ranges.
     *   Multiple sets are easily combined by or-ing them together.
     *
     * - Alternatively, a UnicodeSet could hold each even-numbered range.
     *   All ranges could be enumerated by using each start code point
     *   (for the even-numbered ranges) as well as each limit (end+1) code point
     *   (for the odd-numbered ranges).
     *   It should be possible to combine two such sets by xor-ing them,
     *   but no more than two.
     *
     * The second way to represent ranges may(?!) yield smaller UnicodeSet arrays,
     * but the first one is certainly simpler and applicable for combining more than
     * two range sets.
     *
     * It is possible to combine all range sets for all uprops/unorm tries into one
     * set that can be used for all properties.
     * As an optimization, there could be less-combined range sets for certain
     * groups of properties.
     * The relationship of which less-combined range set to use for which property
     * depends on the implementation of the properties and must be hardcoded
     * - somewhat error-prone and higher maintenance but can be tested easily
     * by building property sets "the simple way" in test code.
     *
     * ---
     *
     * Do not use a UnicodeSet pattern because that causes infinite recursion;
     * UnicodeSet depends on the inclusions set.
     *
     * ---
     *
     * getInclusions() is commented out starting 2005-feb-12 because
     * UnicodeSet now calls the uxyz_addPropertyStarts() directly,
     * and only for the relevant property source.
     */
    /*
    public UnicodeSet getInclusions() {
        UnicodeSet set = new UnicodeSet();
        NormalizerImpl.addPropertyStarts(set);
        addPropertyStarts(set);
        return set;
    }
    */
}
