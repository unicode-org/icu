/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/lang/UCharacter.java,v $ 
* $Date: 2001/08/22 22:38:30 $ 
* $Revision: 1.11 $
*
*******************************************************************************
*/


package com.ibm.text;

import java.util.Locale;

/**
* <p>
* The UCharacter class provides extensions to the 
* <a href=http://java.sun.com/j2se/1.3/docs/api/java/lang/Character.html>
* java.lang.Character</a> class. These extensions provide support for 
* Unicode 3.1 properties and together with the <a href=UTF16.html>UTF16</a> 
* class, provide support for supplementary characters (those with code 
* points above U+FFFF).
* </p>
* <p>
* Code points are represented in these API using ints. While it would be 
* more convenient in Java to have a separate primitive datatype for them, 
* ints suffice in the meantime.
* </p>
* <p>
* To use this class please add the jar file name ucharacter.jar to your 
* class path, since it contains data files which supply the information used 
* by this file.<br>
* E.g. In Windows <br>
* <code>set CLASSPATH=%CLASSPATH%;$JAR_FILE_PATH/ucharacter.jar</code>.
* </p>
* <p>
* For more information about the data file format, please refer to 
* <a href=http://oss.software.ibm.com/icu4j/doc/com/ibm/text/ReadMe.html>
* Read Me</a>.
* </p>
* <p>
* Aside from the additions for UTF-16 support, and the updated Unicode 3.1
* properties, the main differences between UCharacter and Character are:
* <ul>
* <li> UCharacter is not designed to be a char wrapper and does not have 
*      APIs to which involves management of that single char.<br>
*      These include: 
*      <ul>
*        <li> char charValue(), 
*        <li> int compareTo(java.lang.Character, java.lang.Character), etc.
*      </ul>
* <li> UCharacter does not include Character APIs that are deprecated, not 
*      does it include the Java-specific character information, such as 
*      boolean isJavaIdentifierPart(char ch).
* <li> Character maps characters 'A' - 'Z' and 'a' - 'z' to the numeric 
*      values '10' - '35'. UCharacter does not treat the above code points 
*      as having numeric values
* <li> For consistency with ICU4C's data, control code points below have their 
*      Unicode general category reset to the types below.
*      <ul>
*      <li> TAB 0x9 : U_SPACE_SEPARATOR 
*      <li> VT 0xb : U_SPACE_SEPARATOR 
*      <li> LF 0xa : U_PARAGRAPH_SEPARATOR 
*      <li> FF 0xc : U_LINE_SEPARATOR 
*      <li> CR 0xd : U_PARAGRAPH_SEPARATOR 
*      <li> FS 0x1c : U_PARAGRAPH_SEPARATOR 
*      <li> GS 0x1d : U_PARAGRAPH_SEPARATOR 
*      <li> RS 0x1e : U_PARAGRAPH_SEPARATOR 
*      <li> US 0x1f : U_SPACE_SEPARATOR 
*      <li> NL 0x85 : U_PARAGRAPH_SEPARATOR
*      </ul>
* <p>
* Further detail differences can be determined from the program 
*        <a href = http://oss.software.ibm.com/developerworks/opensource/cvs/icu4j/~checkout~/icu4j/src/com/ibm/icu/test/text/UCharacterCompare.java>
*        com.ibm.icu.test.text.UCharacterCompare</a>
* </p>
* @author Syn Wee Quek
* @since oct 06 2000
* @see com.ibm.text.UCharacterCategory
* @see com.ibm.text.UCharacterDirection
*/

public final class UCharacter
{ 
    // public variables ==============================================
  
    /** 
    * The lowest Unicode code point value. Code points are non-ne N_VALUE
    */
    public static final int MIN_VALUE = 0;

    /**
    * The highest Unicode code point value (scalar value) according to the 
    * Unicode Standard.<br> 
    * This is a 21-bit value (21 bits, rounded up).<br>
    * Up-to-date Unicode implementation of java.lang.Character.MIN_VALUE
    */
    public static final int MAX_VALUE = 0x10ffff;
      
    /**
    * The minimum value for Supplementary code points
    */
    public static final int SUPPLEMENTARY_MIN_VALUE = 0x10000;
      
    /**
    * Unicode value used when translating into Unicode encoding form and there 
    * is no existing character.
    */
	public static final int REPLACEMENT_CHAR = '\uFFFD';
    	
	// protected variables ===================================
    	
	/**
    * Shift and mask value for surrogates
    */
	protected static final int LEAD_SURROGATE_SHIFT_ = 10;
	protected static final int TRAIL_SURROGATE_MASK_ = 0x3FF;
                              
    // private variables =====================================
    	
    /**
    * Database storing the sets of character property
    */
    private static final UCharacterPropertyDB PROPERTY_DB_;
    
    /**
    * Initialization of the UCharacterPropertyDB instance. 
    * RuntimeException thrown when data is missing or data has been corrupted.
    */
    static
    {
        try
        {
            PROPERTY_DB_ = new UCharacterPropertyDB();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    /** 
    * Offset to add to combined surrogate pair to avoid msking.
    */
    private static final int SURROGATE_OFFSET_ = 
        SUPPLEMENTARY_MIN_VALUE - (0xD800 << LEAD_SURROGATE_SHIFT_) - 0xDC00;
      
    /**
    * Surrogate code point values
    */
    private static final int SURROGATE_MIN_VALUE_ = 0xD800;
    private static final int SURROGATE_MAX_VALUE_ = 0xDFFF;
      
    /**
    * To get the last character out from a data type
    */
    private static final int LAST_CHAR_MASK_ = 0xFFFF;
      
    /**
    * To get the last byte out from a data type
    */
    private static final int LAST_BYTE_MASK_ = 0xFF;
      
    /**
    * Shift 16 bits
    */
    private static final int SHIFT_16_ = 16;
      
    /**
    * Shift 24 bits
    */
    private static final int SHIFT_24_ = 24;
      
    /**
    * Minimum suffix value that indicates if a character is non character.
    * Unicode 3.0 non characters
    */
    private static final int NON_CHARACTER_SUFFIX_MIN_3_0_ = 0xFFFE;
    
    /**
    * New minimum non character in Unicode 3.1
    */
    private static final int NON_CHARACTER_MIN_3_1_ = 0xFDD0;
    
    /**
    * New non character range in Unicode 3.1
    */
    private static final int NON_CHARACTER_RANGE_3_1_ = 
                                             0xFDEF - NON_CHARACTER_MIN_3_1_;
      
    /**
    * Decimal radix
    */
    private static final int DECIMAL_RADIX_ = 10;
      
    /**
    * No break space code point
    */
    private static final int NO_BREAK_SPACE_ = 0xA0;
      
    /**
    * Narrow no break space code point
    */
    private static final int NARROW_NO_BREAK_SPACE_ = 0x202F;
      
    /**
    * Zero width no break space code point
    */
    private static final int ZERO_WIDTH_NO_BREAK_SPACE_ = 0xFEFF;
      
    /**
    * Ideographic number zero code point
    */
    private static final int IDEOGRAPHIC_NUMBER_ZERO_ = 0x3007;
            
    /**
    * CJK Ideograph, First code point
    */
    private static final int CJK_IDEOGRAPH_FIRST_ = 0x4e00;
      
    /**
    * CJK Ideograph, Second code point
    */
    private static final int CJK_IDEOGRAPH_SECOND_ = 0x4e8c;
            
    /**
    * CJK Ideograph, Third code point
    */
    private static final int CJK_IDEOGRAPH_THIRD_ = 0x4e09;
      
    /**
    * CJK Ideograph, Fourth code point
    */
    private static final int CJK_IDEOGRAPH_FOURTH_ = 0x56d8;
      
    /**
    * CJK Ideograph, FIFTH code point
    */
    private static final int CJK_IDEOGRAPH_FIFTH_ = 0x4e94;
      
    /**
    * CJK Ideograph, Sixth code point
    */
    private static final int CJK_IDEOGRAPH_SIXTH_ = 0x516d;
            
    /**
    * CJK Ideograph, Seventh code point
    */
    private static final int CJK_IDEOGRAPH_SEVENTH_ = 0x4e03;
      
    /**
    * CJK Ideograph, Eighth code point
    */
    private static final int CJK_IDEOGRAPH_EIGHTH_ = 0x516b;
      
    /**
    * CJK Ideograph, Nineth code point
    */
    private static final int CJK_IDEOGRAPH_NINETH_ = 0x4e5d;
      
    /**
    * Application Program command code point
    */
    private static final int APPLICATION_PROGRAM_COMMAND_ = 0x009F;
      
    /**
    * Unit seperator code point
    */
    private static final int UNIT_SEPERATOR_ = 0x001F;
      
    /**
    * Delete code point
    */
    private static final int DELETE_ = 0x007F;
      
    /**
    * Turkish ISO 639 2 character code
    */
    private static final String TURKISH_ = "tr";
      
    /**
    * Azerbaijani ISO 639 2 character code
    */
    private static final String AZERBAIJANI_ = "az";
      
    /**
    * Lithuanian ISO 639 2 character code
    */
    private static final String LITHUANIAN_ = "lt";
      
    /**
    * Latin owercase i
    */
    private static final char LATIN_SMALL_LETTER_I_ = 0x69;
      
    /**
    * Latin uppercase I
    */
    private static final char LATIN_CAPITAL_LETTER_I_ = 0x49;
      
    /**
    * Latin capital letter i with dot above
    */ 
    private static final char LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_ = 0x130;
      
    /**
    * Latin small letter i with dot above
    */ 
    private static final char LATIN_SMALL_LETTER_DOTLESS_I_ = 0x131;
      
    /**
    * Combining dot above
    */
    private static final char COMBINING_DOT_ABOVE_ = 0x307;
      
    /**
    * Greek capital letter sigma
    */
    private static final char GREEK_CAPITAL_LETTER_SIGMA_ = 0x3a3;
      
    /**
    * Greek small letter sigma
    */
    private static final char GREEK_SMALL_LETTER_SIGMA_ = 0x3c3;
      
    /**
    * Greek small letter rho
    */
    private static final char GREEK_SMALL_LETTER_RHO_ = 0x3c2;
      
    /**
    * ISO control character first range upper limit 0x0 - 0x1F
    */
    private static final int ISO_CONTROL_FIRST_RANGE_MAX_ = 0x1F;
      
    // constructor ====================================================
      
    /**
    * Private constructor to prevent instantiation
    */
    private UCharacter()
    {
    }
      
    // public methods ===================================================
      
    /**
    * Retrieves the decimal numeric value of a digit code point.<br>
    * A code point is a valid digit if the following is true: 
    * <ul>
    * <li> The method isDigit(ch) is true and the Unicode decimal digit value of 
    *      ch is less than the specified radix. 
    * </ul>
    * Note this method, unlike java.lang.Character.digit() does not regard the 
    * ascii characters 'A' - 'Z' and 'a' - 'z' as digits. 
    * @param ch the code point whose numeric value is to be determined
    * @param radix the radix which the digit is to be converted to
    * @return the numeric value of the code point ch in the argument radix,
    *         this method returns -1 if ch is not a valid digit code point or 
    *         if its digit value exceeds the radix.
    */
    public static int digit(int ch, int radix)
    {
        int props = getProps(ch);
        int result = -1;
        // if props == 0, it will just fall through and return -1
        if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
            // not contained in exception data
            if (UCharacterPropertyDB.getPropType(props) == 
                UCharacterCategory.DECIMAL_DIGIT_NUMBER) {
                result = UCharacterPropertyDB.getSignedValue(props);
            }
        }
        else {
            // contained in exception data
            int index = UCharacterPropertyDB.getExceptionIndex(props);
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_DIGIT_VALUE_)) {
                result  = PROPERTY_DB_.getException(index, 
                                        UCharacterPropertyDB.EXC_DIGIT_VALUE_) & 
                                        LAST_CHAR_MASK_; 
            }
            else {
                if (!PROPERTY_DB_.hasExceptionValue(index, 
                                  UCharacterPropertyDB.EXC_DENOMINATOR_VALUE_) 
                    && PROPERTY_DB_.hasExceptionValue(index, 
                                  UCharacterPropertyDB.EXC_NUMERIC_VALUE_)) {
                        result  = PROPERTY_DB_.getException(index, 
                                     UCharacterPropertyDB.EXC_NUMERIC_VALUE_);
                }
            }
        }
        
        if (result < 0) {
            result = getHanDigit(ch);
        }
        
        if (result < 0 || result >= radix) {
            return -1;
        }
        return result;
    }
      
    /**
    * Retrieves the decimal numeric value of a digit code point in radix 10<br>
    * Note this method, unlike java.lang.Character.digit() does not regard the 
    * ascii characters 'A' - 'Z' and 'a' - 'z' as digits. 
    * @param ch the code point whose numeric value is to be determined
    * @return the numeric value of the code point ch, this method returns -1 if 
    *         ch is not a valid digit code point
    */
    public static int digit(int ch)
    {
        return digit(ch, DECIMAL_RADIX_);
    }
                              
    /**
    * Returns the Unicode numeric value of the code point as a nonnegative 
    * integer. <br>
    * If the code point does not have a numeric value, then -1 is returned. <br>
    * If the code point has a numeric value that cannot be represented as a 
    * nonnegative integer (for example, a fractional value), then -2 is returned.
    * <br>
    * Note this method, unlike java.lang.Character.digit() does not regard the 
    * ascii characters 'A' - 'Z' and 'a' - 'z' as numbers. 
    * @param ch Unicode code point
    * @return numeric value of the code point as a nonnegative integer
    */
    public static int getNumericValue(int ch)
    {
        int props = getProps(ch);
        int type = UCharacterPropertyDB.getPropType(props);
        
        // if props == 0, it will just fall through and return -1
        if (type != UCharacterCategory.DECIMAL_DIGIT_NUMBER &&
            type != UCharacterCategory.LETTER_NUMBER &&
            type != UCharacterCategory.OTHER_NUMBER) {
            return -1;
        }
          
        int result = -1;
        if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
            // not contained in exception data
            result = UCharacterPropertyDB.getSignedValue(props);
        }
        else {
            // contained in exception data
            int index = UCharacterPropertyDB.getExceptionIndex(props);
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_DIGIT_VALUE_)) {
                result  = PROPERTY_DB_.getException(index, 
                                        UCharacterPropertyDB.EXC_DIGIT_VALUE_); 
            }
            else {
                if (!PROPERTY_DB_.hasExceptionValue(index, 
                                 UCharacterPropertyDB.EXC_DENOMINATOR_VALUE_)
                    && PROPERTY_DB_.hasExceptionValue(index, 
                                     UCharacterPropertyDB.EXC_NUMERIC_VALUE_)) {
                result  = PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_NUMERIC_VALUE_); 
                }
            }
        }
        
        if (result < 0) {
          result = getHanDigit(ch);
        }
        
        if (result < 0) {
            return -2;
        }
        return result;
    }
      
    /**
    * Returns a value indicating a code point's Unicode category.<br>
    * Up-to-date Unicode implementation of java.lang.Character.getType() except 
    * for the above mentioned code points that had their category changed.<br>
    * Return results are constants from the interface 
    * <a href=UCharacterCategory.html>UCharacterCategory</a>
    * @param ch code point whose type is to be determined
    * @return category which is a value of UCharacterCategory
    */
    public static int getType(int ch)
    {
        return UCharacterPropertyDB.getPropType(getProps(ch));
    }
       
    /**
    * Determines if a code point has a defined meaning in the up-to-date Unicode
    * standard.<br>
    * E.g. supplementary code points though allocated space are not defined in 
    * Unicode yet.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isDefined()
    * @param ch code point to be determined if it is defined in the most current 
    *        version of Unicode
    * @return true if this code point is defined in unicode
    */
    public static boolean isDefined(int ch)
    {
        return getProps(ch) != 0;
    }
                                    
    /**
    * Determines if a code point is a digit.<br>
    * Note this method, unlike java.lang.Character.isDigit() does not regard the 
    * ascii characters 'A' - 'Z' and 'a' - 'z' as digits.<br>
    * @param ch code point to determine if it is a digit
    * @return true if this code point is a digit
    */
    public static boolean isDigit(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.DECIMAL_DIGIT_NUMBER || 
            cat == UCharacterCategory.OTHER_NUMBER ||
            cat == UCharacterCategory.LETTER_NUMBER;
    }
                                    
    /**
    * Determines if the specified code point is an ISO control character.<br>
    * A code point is considered to be an ISO control character if it is in the 
    * range &#92u0000 through &#92u001F or in the range &#92u007F through 
    * &#92u009F.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isISOControl()
    * @param ch code point to determine if it is an ISO control character
    * @return true if code point is a ISO control character
    */
    public static boolean isISOControl(int ch)
    {
        return ch >= 0 && ch <= APPLICATION_PROGRAM_COMMAND_ && 
            ((ch <= UNIT_SEPERATOR_) || (ch >= DELETE_));
    }
                                    
    /**
    * Determines if the specified code point is a letter.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isLetter()
    * @param ch code point to determine if it is a letter
    * @return true if code point is a letter
    */
    public static boolean isLetter(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.UPPERCASE_LETTER || 
            cat == UCharacterCategory.LOWERCASE_LETTER || 
            cat == UCharacterCategory.TITLECASE_LETTER || 
            cat == UCharacterCategory.MODIFIER_LETTER ||
            cat == UCharacterCategory.OTHER_LETTER;
    }
                
    /**
    * Determines if the specified code point is a letter or digit.<br>
    * Note this method, unlike java.lang.Character does not regard the ascii 
    * characters 'A' - 'Z' and 'a' - 'z' as digits.
    * @param ch code point to determine if it is a letter or a digit
    * @return true if code point is a letter or a digit
    */
    public static boolean isLetterOrDigit(int ch)
    {
        return isDigit(ch) || isLetter(ch);
    }
        
    /**
    * Determines if the specified code point is a lowercase character.<br>
    * UnicodeData only contains case mappings for code points where they are 
    * one-to-one mappings; it also omits information about context-sensitive 
    * case mappings.<br> For more information about Unicode case mapping please 
    * refer to the <a href=http://www.unicode.org/unicode/reports/tr21/>
    * Technical report #21</a>.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isLowerCase()
    * @param ch code point to determine if it is in lowercase
    * @return true if code point is a lowercase character
    */
    public static boolean isLowerCase(int ch)
    {
        // if props == 0, it will just fall through and return false
        return getType(ch) == UCharacterCategory.LOWERCASE_LETTER;
    }
       
    /**
    * Determines if the specified code point is a white space character.<br>
    * A code point is considered to be an whitespace character if and only
    * if it satisfies one of the following criteria:
    * <ul>
    * <li> It is a Unicode space separator (category "Zs"), but is not
    *      a no-break space (&#92u00A0 or &#92u202F or &#92uFEFF).
    * <li> It is a Unicode line separator (category "Zl").
    * <li> It is a Unicode paragraph separator (category "Zp").
    * </ul>
    * Up-to-date Unicode implementation of java.lang.Character.isWhitespace().
    * @param ch code point to determine if it is a white space
    * @return true if the specified code point is a white space character
    */
    public static boolean isWhitespace(int ch)
    {
        int cat = getType(ch);
        // exclude no-break spaces
        // if props == 0, it will just fall through and return false
        return (cat == UCharacterCategory.SPACE_SEPARATOR || 
                cat == UCharacterCategory.LINE_SEPARATOR ||
                cat == UCharacterCategory.PARAGRAPH_SEPARATOR) && 
                (ch != NO_BREAK_SPACE_) && (ch != NARROW_NO_BREAK_SPACE_) && 
                (ch != ZERO_WIDTH_NO_BREAK_SPACE_);
    }
       
    /**
    * Determines if the specified code point is a Unicode specified space 
    * character, ie if code point is in the category Zs, Zl and Zp.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isSpaceChar().
    * @param ch code point to determine if it is a space
    * @return true if the specified code point is a space character
    */
    public static boolean isSpaceChar(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.SPACE_SEPARATOR || 
            cat == UCharacterCategory.LINE_SEPARATOR ||
            cat == UCharacterCategory.PARAGRAPH_SEPARATOR;
    }
                                    
    /**
    * Determines if the specified code point is a titlecase character.<br>
    * UnicodeData only contains case mappings for code points where they are 
    * one-to-one mappings; it also omits information about context-sensitive 
    * case mappings.<br>
    * For more information about Unicode case mapping please refer to the 
    * <a href=http://www.unicode.org/unicode/reports/tr21/>
    * Technical report #21</a>.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isTitleCase().
    * @param ch code point to determine if it is in title case
    * @return true if the specified code point is a titlecase character
    */
    public static boolean isTitleCase(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.TITLECASE_LETTER;
    }
       
    /**
    * Determines if the specified code point may be any part of a Unicode 
    * identifier other than the starting character.<br> 
    * A code point may be part of a Unicode identifier if and only if it is one 
    * of the following: 
    * <ul>
    * <li> Lu Uppercase letter
    * <li> Ll Lowercase letter
    * <li> Lt Titlecase letter
    * <li> Lm Modifier letter
    * <li> Lo Other letter
    * <li> Nl Letter number
    * <li> Pc Connecting punctuation character 
    * <li> Nd decimal number
    * <li> Mc Spacing combining mark 
    * <li> Mn Non-spacing mark 
    * <li> Cf formatting code
    * </ul>
    * Up-to-date Unicode implementation of 
    * java.lang.Character.isUnicodeIdentifierPart().<br>
    * See <a href=http://www.unicode.org/unicode/reports/tr8/>UTR #8</a>.
    * @param ch code point to determine if is can be part of a Unicode identifier
    * @return true if code point is any character belonging a unicode identifier
    *         suffix after the first character
    */
    public static boolean isUnicodeIdentifierPart(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.UPPERCASE_LETTER || 
            cat == UCharacterCategory.LOWERCASE_LETTER || 
            cat == UCharacterCategory.TITLECASE_LETTER || 
            cat == UCharacterCategory.MODIFIER_LETTER ||
            cat == UCharacterCategory.OTHER_LETTER || 
            cat == UCharacterCategory.LETTER_NUMBER ||
            cat == UCharacterCategory.CONNECTOR_PUNCTUATION ||
            cat == UCharacterCategory.DECIMAL_DIGIT_NUMBER ||
            cat == UCharacterCategory.COMBINING_SPACING_MARK || 
            cat == UCharacterCategory.NON_SPACING_MARK || 
            cat == UCharacterCategory.FORMAT;
    }
                       
    /**
    * Determines if the specified code point is permissible as the first 
    * character in a Unicode identifier.<br> 
    * A code point may start a Unicode identifier if it is of type either 
    * <ul> 
    * <li> Lu Uppercase letter
    * <li> Ll Lowercase letter
    * <li> Lt Titlecase letter
    * <li> Lm Modifier letter
    * <li> Lo Other letter
    * <li> Nl Letter number
    * </ul>
    * Up-to-date Unicode implementation of 
    * java.lang.Character.isUnicodeIdentifierStart().<br>
    * See <a href=http://www.unicode.org/unicode/reports/tr8/>UTR #8</a>.
    * @param ch code point to determine if it can start a Unicode identifier
    * @return true if code point is the first character belonging a unicode 
    *              identifier
    */
    public static boolean isUnicodeIdentifierStart(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.UPPERCASE_LETTER || 
            cat == UCharacterCategory.LOWERCASE_LETTER || 
            cat == UCharacterCategory.TITLECASE_LETTER || 
            cat == UCharacterCategory.MODIFIER_LETTER ||
            cat == UCharacterCategory.OTHER_LETTER || 
            cat == UCharacterCategory.LETTER_NUMBER;
    }

    /**
    * Determines if the specified code point should be regarded as an ignorable
    * character in a Unicode identifier.<br>
    * A character is ignorable in the Unicode standard if it is of the type Cf, 
    * Formatting code.<br>
    * Up-to-date Unicode implementation of 
    * java.lang.Character.isIdentifierIgnorable().<br>
    * See <a href=http://www.unicode.org/unicode/reports/tr8/>UTR #8</a>.
    * @param ch code point to be determined if it can be ignored in a Unicode 
    *        identifier.
    * @return true if the code point is ignorable
    */
    public static boolean isIdentifierIgnorable(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.FORMAT;
    }
                      
    /**
    * Determines if the specified code point is an uppercase character.<br>
    * UnicodeData only contains case mappings for code point where they are 
    * one-to-one mappings; it also omits information about context-sensitive 
    * case mappings.<br> 
    * For language specific case conversion behavior, use 
    * toUpperCase(locale, str). <br>
    * For example, the case conversion for dot-less i and dotted I in Turkish,
    * or for final sigma in Greek.
    * For more information about Unicode case mapping please refer to the 
    * <a href=http://www.unicode.org/unicode/reports/tr21/>
    * Technical report #21</a>.<br>
    * Up-to-date Unicode implementation of java.lang.Character.isUpperCase().
    * @param ch code point to determine if it is in uppercase
    * @return true if the code point is an uppercase character
    */
    public static boolean isUpperCase(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.UPPERCASE_LETTER;
    }
                       
    /**
    * The given code point is mapped to its lowercase equivalent; if the code 
    * point has no lowercase equivalent, the code point itself is returned.<br>
    * UnicodeData only contains case mappings for code point where they are 
    * one-to-one mappings; it also omits information about context-sensitive 
    * case mappings.<br> 
    * For language specific case conversion behavior, use 
    * toLowerCase(locale, str). <br>
    * For example, the case conversion for dot-less i and dotted I in Turkish,
    * or for final sigma in Greek.
    * For more information about Unicode case mapping please refer to the 
    * <a href=http://www.unicode.org/unicode/reports/tr21/>
    * Technical report #21</a>.<br>
    * Up-to-date Unicode implementation of java.lang.Character.toLowerCase()
    * @param ch code point whose lowercase equivalent is to be retrieved
    * @return the lowercase equivalent code point
    */
    public static int toLowerCase(int ch)
    {
        int props = getProps(ch);
        // if props == 0, it will just fall through and return itself
        if(!UCharacterPropertyDB.isExceptionIndicator(props)) {
            int cat = UCharacterPropertyDB.getPropType(props);
            if (cat == UCharacterCategory.UPPERCASE_LETTER || 
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterPropertyDB.getSignedValue(props);
            }
        } 
        else 
        {
            int index = UCharacterPropertyDB.getExceptionIndex(props);
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_LOWERCASE_)) {
                return PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_); 
            }
        }
        return ch;
    }

    /**
    * Converts argument code point and returns a String object representing the 
    * code point's value in UTF16 format.<br>
    * The result is a string whose length is 1 for non-supplementary code points, 
    * 2 otherwise.<br>
    * com.ibm.ibm.icu.UTF16 can be used to parse Strings generated by this 
    * function.<br>
    * Up-to-date Unicode implementation of java.lang.Character.toString()
    * @param ch code point
    * @return string representation of the code point, null if code point is not
    *         defined in unicode
    */
    public static String toString(int ch)
    {
        if (ch < MIN_VALUE || ch > MAX_VALUE) {
            return null;
        }
        
        if (ch < UCharacter.SUPPLEMENTARY_MIN_VALUE) {
            return String.valueOf((char)ch);
        }
        
        char result[] = new char[2];
        result[0] = (char)UTF16.getLeadSurrogate(ch);
        result[1] = (char)UTF16.getTrailSurrogate(ch);
        return new String(result);
    }
                                    
    /**
    * Converts the code point argument to titlecase.<br>
    * UnicodeData only contains case mappings for code points where they are 
    * one-to-one mappings; it also omits information about context-sensitive 
    * case mappings.<br> 
    * There are only four Unicode characters that are truly titlecase forms
    * that are distinct from uppercase forms.
    * For more information about Unicode case mapping please refer
    * to the <a href=http://www.unicode.org/unicode/reports/tr21/>
    * Technical report #21</a>.<br>
    * If no titlecase is available, the uppercase is returned. If no uppercase 
    * is available, the code point itself is returned.<br>
    * Up-to-date Unicode implementation of java.lang.Character.toTitleCase()
    * @param ch code point  whose title case is to be retrieved
    * @return titlecase code point
    */
    public static int toTitleCase(int ch)
    {
        int props = getProps(ch);
        // if props == 0, it will just fall through and return itself
        if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
            if (UCharacterPropertyDB.getPropType(props) == 
                UCharacterCategory.LOWERCASE_LETTER) {
                // here, titlecase is same as uppercase
                return ch - UCharacterPropertyDB.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterPropertyDB.getExceptionIndex(props);
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                         UCharacterPropertyDB.EXC_TITLECASE_)) {
                return PROPERTY_DB_.getException(index,
                                         UCharacterPropertyDB.EXC_TITLECASE_);
            }
            else {
                // here, titlecase is same as uppercase
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_UPPERCASE_)) {
                    return PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_); 
                }
            }
        }
        return ch; // no mapping - return c itself
    }
       
    /**
    * Converts the character argument to uppercase.<br>
    * UnicodeData only contains case mappings for characters where they are 
    * one-to-one mappings; it also omits information about context-sensitive 
    * case mappings.<br> 
    * For more information about Unicode case mapping please refer
    * to the <a href=http://www.unicode.org/unicode/reports/tr21/>
    * Technical report #21</a>.<br>
    * If no uppercase is available, the character itself is returned.<br>
    * Up-to-date Unicode implementation of java.lang.Character.toUpperCase()
    * @param ch code point whose uppercase is to be retrieved
    * @return uppercase code point
    */
    public static int toUpperCase(int ch)
    {
        int props = getProps(ch);
        // if props == 0, it will just fall through and return itself
        if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
            if (UCharacterPropertyDB.getPropType(props) == 
                UCharacterCategory.LOWERCASE_LETTER) {
                // here, titlecase is same as uppercase */
                return ch - UCharacterPropertyDB.getSignedValue(props);
            }
        }
        else 
        {
            int index = UCharacterPropertyDB.getExceptionIndex(props);
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_)) {
                return PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_); 
            }
        }
        return ch; // no mapping - return c itself
    }
       
    // extra methods not in java.lang.Character ===========================
       
    /**
    * Determines if the code point is a supplementary character.<br>
    * A code point is a supplementary character if and only if it is greater than
    * <a href=#SUPPLEMENTARY_MIN_VALUE>SUPPLEMENTARY_MIN_VALUE</a>
    * @param ch code point to be determined if it is in the supplementary plane
    * @return true if code point is a supplementary character
    */
    public static boolean isSupplementary(int ch)
    {
        return ch >= UCharacter.SUPPLEMENTARY_MIN_VALUE && 
            ch <= UCharacter.MAX_VALUE;
    }
      
    /**
    * Determines if the code point is in the BMP plane.<br>
    * @param ch code point to be determined if it is not a supplementary 
    *        character
    * @return true if code point is not a supplementary character
    */
    public static boolean isBMP(int ch) 
    {
        return (ch >= 0 && ch < LAST_CHAR_MASK_);
    }

    /**
    * Determines whether the specified code point is a printable character 
    * according to the Unicode standard.
    * @param ch code point to be determined if it is printable
    * @return true if the code point is a printable character
    */
    public static boolean isPrintable(int ch)
    {
        if (isISOControl(ch)) {
            return false;
        }
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return (cat != UCharacterCategory.UNASSIGNED && 
            cat != UCharacterCategory.CONTROL && 
            cat != UCharacterCategory.FORMAT &&
            cat != UCharacterCategory.PRIVATE_USE &&
            cat != UCharacterCategory.SURROGATE &&
            cat != UCharacterCategory.GENERAL_OTHER_TYPES);
    }

    /**
    * Determines whether the specified code point is of base form.<br>
    * A code point of base form does not graphically combine with preceding 
    * characters, and is neither a control nor a format character.
    * @param ch code point to be determined if it is of base form
    * @return true if the code point is of base form
    */
    public static boolean isBaseForm(int ch)
    {
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.DECIMAL_DIGIT_NUMBER || 
            cat == UCharacterCategory.OTHER_NUMBER || 
            cat == UCharacterCategory.LETTER_NUMBER || 
            cat == UCharacterCategory.UPPERCASE_LETTER || 
            cat == UCharacterCategory.LOWERCASE_LETTER || 
            cat == UCharacterCategory.TITLECASE_LETTER ||
            cat == UCharacterCategory.MODIFIER_LETTER || 
            cat == UCharacterCategory.OTHER_LETTER || 
            cat == UCharacterCategory.NON_SPACING_MARK || 
            cat == UCharacterCategory.ENCLOSING_MARK ||
            cat == UCharacterCategory.COMBINING_SPACING_MARK;
    }

    /**
    * Returns the Bidirection property of a code point.<br>
    * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional 
    * property.<br>
    * Result returned belongs to the interface 
    * <a href=UCharacterDirection.html>UCharacterDirection</a>
    * @param ch the code point to be determined its direction
    * @return direction constant from UCharacterDirection. Otherwise is 
    *         character is not defined, UCharacterDirection.BOUNDARY_NEUTRAL
    *         will be returned.
    */
    public static int getDirection(int ch)
    {
        int props = getProps(ch);
        if (props != 0) {
            return UCharacterPropertyDB.getDirection(props);
        }
        return UCharacterDirection.LEFT_TO_RIGHT;
    }

    /**
    * Determines whether the code point has the "mirrored" property.<br>
    * This property is set for characters that are commonly used in
    * Right-To-Left contexts and need to be displayed with a "mirrored"
    * glyph.
    * @param ch code point whose mirror is to be determined
    * @return true if the code point has the "mirrored" property
    */
    public static boolean isMirrored(int ch)
    {
        int props = getProps(ch);
        // if props == 0, it will just fall through and return false
        return UCharacterPropertyDB.isMirrored(props);
    }

    /**
    * Maps the specified code point to a "mirror-image" code point.<br>
    * For code points with the "mirrored" property, implementations sometimes 
    * need a "poor man's" mapping to another code point such that the default 
    * glyph may serve as the mirror-image of the default glyph of the specified
    * code point.<br> 
    * This is useful for text conversion to and from codepages with visual 
    * order, and for displays without glyph selection capabilities.
    * @param ch code point whose mirror is to be retrieved
    * @return another code point that may serve as a mirror-image substitute, or 
    *         ch itself if there is no such mapping or ch does not have the 
    *         "mirrored" property
    */
    public static int getMirror(int ch)
    {
        int props = getProps(ch);
        // mirrored - the value is a mirror offset
        // if props == 0, it will just fall through and return false
        if (UCharacterPropertyDB.isMirrored(props)) {
            if(!UCharacterPropertyDB.isExceptionIndicator(props)) {
                return ch + UCharacterPropertyDB.getSignedValue(props);
            }
            else 
            {
                int index = UCharacterPropertyDB.getExceptionIndex(props);
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                    UCharacterPropertyDB.EXC_MIRROR_MAPPING_)) 
                return PROPERTY_DB_.getException(index, 
                                     UCharacterPropertyDB.EXC_MIRROR_MAPPING_);   
            }
        }
        return ch;
    }
      
    /**
    * Gets the combining class of the argument codepoint
    * @param ch code point whose combining is to be retrieved
    * @return the combining class of the codepoint
    */
    public static byte getCombiningClass(int ch)
    {
        int props = getProps(ch);
        if(!UCharacterPropertyDB.isExceptionIndicator(props)) {
        if (UCharacterPropertyDB.getPropType(props) == 
                                        UCharacterCategory.NON_SPACING_MARK) {
            return (byte)(PROPERTY_DB_.getUnsignedValue(props));
        }
        else {
            return 0;
        }
        }
        else {
        // the combining class is in bits 23..16 of the first exception value
        return (byte)(
             (PROPERTY_DB_.getException(PROPERTY_DB_.getExceptionIndex(props), 
                                    UCharacterPropertyDB.EXC_COMBINING_CLASS_)
                                    >> SHIFT_16_) & LAST_BYTE_MASK_);
        }
    }
      
    /**
    * A code point is illegal if and only if
    * <ul>
    * <li> Out of bounds, less than 0 or greater than UCharacter.MAX_VALUE
    * <li> A surrogate value, 0xD800 to 0xDFFF
    * <li> Not-a-character, having the form 0x xxFFFF or 0x xxFFFE
    * </ul>
    * Note: legal does not mean that it is assigned in this version of Unicode.
    * @param ch code point to determine if it is a legal code point by itself
    * @return true if and only if legal. 
    */
    public static boolean isLegal(int ch) 
    {
        if (ch < MIN_VALUE) {
            return false;
        }
        if (ch < SURROGATE_MIN_VALUE_) {
            return true;
        }
        if (ch <= SURROGATE_MAX_VALUE_) {
            return false;
        }
        if (isNonCharacter(ch)) {
            return false;
        }
        return (ch <= MAX_VALUE);
    }
      
    /**
    * A string is legal iff all its code points are legal.
    * A code point is illegal if and only if
    * <ul>
    * <li> Out of bounds, less than 0 or greater than UCharacter.MAX_VALUE
    * <li> A surrogate value, 0xD800 to 0xDFFF
    * <li> Not-a-character, having the form 0x xxFFFF or 0x xxFFFE
    * </ul>
    * Note: legal does not mean that it is assigned in this version of Unicode.
    * @param ch code point to determine if it is a legal code point by itself
    * @return true if and only if legal. 
    */
    public static boolean isLegal(String str) 
    {
        int size = str.length();
        int codepoint;
        for (int i = 0; i < size; i ++)
        {
            codepoint = UTF16.charAt(str, i);
            if (!isLegal(codepoint)) {
                return false;
            }
            if (isSupplementary(codepoint)) {
                i ++;
            }
        }
        return true;
    }

    /**
    * Gets the version of Unicode data used. 
    * @return the unicode version number used
    */
    public static String getUnicodeVersion()
    {
        return PROPERTY_DB_.m_unicodeversion_;
    }
      
    /**
    * Retrieve the most current Unicode name of the argument code point, or 
    * null if the character is unassigned or outside the range 
    * UCharacter.MIN_VALUE and UCharacter.MAX_VALUE.<br>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param ch the code point for which to get the name
    * @return most current Unicode name
    */
    public static String getName(int ch)
    {
        return UCharacterName.getName(ch, 
                                    UCharacterNameChoice.U_UNICODE_CHAR_NAME);
    }
      
    /**
    * Retrieve the earlier version 1.0 Unicode name of the argument code point,
    * or null if the character is unassigned or outside the range 
    * UCharacter.MIN_VALUE and UCharacter.MAX_VALUE.<br>
    * <br>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param ch the code point for which to get the name
    * @return version 1.0 Unicode name
    */
    public static String getName1_0(int ch)
    {
        return UCharacterName.getName(ch, 
                                UCharacterNameChoice.U_UNICODE_10_CHAR_NAME);
    }
      
    /**
    * Find a Unicode code point by its most current Unicode name and return its 
    * code point value.<br>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param name most current Unicode character name whose code point is to be 
    *        returned
    * @return code point or -1 if name is not found
    */
    public static int getCharFromName(String name)
    {
        return UCharacterName.getCharFromName(
                            UCharacterNameChoice.U_UNICODE_CHAR_NAME, name);
    }
      
    /**
    * Find a Unicode character by its version 1.0 Unicode name and return its 
    * code point value.<br>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param name Unicode 1.0 code point name whose code point is to 
    *             returned
    * @return code point or -1 if name is not found
    */
    public static int getCharFromName1_0(String name)
    {
        return UCharacterName.getCharFromName(
                            UCharacterNameChoice.U_UNICODE_10_CHAR_NAME, name);
    }
      
    /**
    * Returns a code pointcorresponding to the two UTF16 characters.<br>
    * If the argument lead is not a high surrogate character or trail is not a 
    * low surrogate character, UCharacter.REPLACEMENT_CHAR is returned.
    * @param lead the lead char
    * @param trail the trail char
    * @return code point or UCharacter.REPLACEMENT_CHAR if surrogate characters 
    *         are invalid.
    */
    public static int getCodePoint(char lead, char trail) 
    {
        if (UTF16.isLeadSurrogate(lead) && UTF16.isTrailSurrogate(trail)) {
            return getRawSupplementary(lead, trail);
        }
        return UCharacter.REPLACEMENT_CHAR;
    }
      
    /**
    * Returns the code point corresponding to the UTF16 character.<br>
    * If argument char16 is a surrogate character, UCharacter.REPLACEMENT_CHAR 
    * is returned
    * @param char16 the UTF16 character
    * @return code point or UCharacter.REPLACEMENT_CHAR if argument is not a 
    *         invalid character.
    * @exception IllegalArgumentException thrown when char16 is not a valid
    *            codepoint
    */
    public static int getCodePoint(char char16) 
    {
        if (UCharacter.isLegal(char16)) {
            return char16;
        }
        throw new IllegalArgumentException("Illegal codepoint");
    }
      
    /**
    * Gets uppercase version of the argument string. 
    * Casing is dependent on the default locale and context-sensitive.
    * @param str source string to be performed on
    * @return uppercase version of the argument string
    */
    public static String toUpperCase(String str)
    {
        return toUpperCase(Locale.getDefault(), str);
    }
      
    /**
    * Gets lowercase version of the argument string. 
    * Casing is dependent on the default locale and context-sensitive
    * @param str source string to be performed on
    * @return lowercase version of the argument string
    */
    public static String toLowerCase(String str)
    {
        return toLowerCase(Locale.getDefault(), str);
    }
      
    /**
    * Gets uppercase version of the argument string. 
    * Casing is dependent on the argument locale and context-sensitive.
    * @param locale which string is to be converted in
    * @param str source string to be performed on
    * @return uppercase version of the argument string
    */
    public static String toUpperCase(Locale locale, String str)
    {
        int size = UTF16.countCodePoint(str);
        StringBuffer result = new StringBuffer(size << 1); // initial buffer
        int props;
        int ch;
        int index;
        String lang = locale.getLanguage();
        boolean tr_az = lang.equals(TURKISH_) || lang.equals(AZERBAIJANI_);
        boolean lt = lang.equals(LITHUANIAN_);
        
        for (int i = 0; i < size; i ++)
        {
            ch = UTF16.charAtCodePointOffset(str, i);
            props = PROPERTY_DB_.getProperty(ch);
            if (!UCharacterPropertyDB.isExceptionIndicator(props)) 
            {
                if (UCharacterPropertyDB.getPropType(props) == 
                    UCharacterCategory.LOWERCASE_LETTER) {
                ch -= UCharacterPropertyDB.getSignedValue(props);
                }
                UTF16.append(result, ch);
            }
            else 
            {
                index = UCharacterPropertyDB.getExceptionIndex(props);
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                  UCharacterPropertyDB.EXC_SPECIAL_CASING_)) {
                    getSpecialUpperCase(ch, index, result, str, i, tr_az, lt);          
                }
                else {
                    if (PROPERTY_DB_.hasExceptionValue(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_)) {
                        UTF16.append(result, PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_));
                    }
                }
            }
        }
        return result.toString();
    }
      
    /**
    * Gets lowercase version of the argument string. 
    * Casing is dependent on the argument locale and context-sensitive
    * @param locale which string is to be converted in
    * @param str source string to be performed on
    * @return lowercase version of the argument string
    */
    public static String toLowerCase(Locale locale, String str)
    {
        int size = UTF16.countCodePoint(str);
        StringBuffer result = new StringBuffer(size << 1); // initial buffer
        int props;
        int ch;
        int index;
        String lang = locale.getLanguage();
        boolean tr_az = lang.equals(TURKISH_) || lang.equals(AZERBAIJANI_);
        boolean lt = lang.equals(LITHUANIAN_);
        int type;
        
        for (int i = 0; i < size; i ++)
        {
            ch = UTF16.charAtCodePointOffset(str, i);
            props = PROPERTY_DB_.getProperty(ch);
            if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
                type = UCharacterPropertyDB.getPropType(props);
                if (type == UCharacterCategory.UPPERCASE_LETTER ||
                    type == UCharacterCategory.TITLECASE_LETTER) {
                ch += UCharacterPropertyDB.getSignedValue(props);
                }
                UTF16.append(result, ch);
            }
            else {
                index = UCharacterPropertyDB.getExceptionIndex(props);
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                    UCharacterPropertyDB.EXC_SPECIAL_CASING_)) {
                    getSpecialLowerCase(ch, index, result, str, i, tr_az, lt);          
                }
                else {
                    if (PROPERTY_DB_.hasExceptionValue(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_)) {
                        UTF16.append(result, PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_));
                    }
                }
            }
        }
        return result.toString();
    }
      
    /**
    * The given character is mapped to its case folding equivalent according to
    * UnicodeData.txt and CaseFolding.txt; if the character has no case folding 
    * equivalent, the character itself is returned.
    * Only "simple", single-code point case folding mappings are used.
    * For "full", multiple-code point mappings use the API 
    * foldCase(String str, boolean defaultmapping).
    * @param ch             the character to be converted
    * @param defaultmapping Indicates if all mappings defined in CaseFolding.txt 
    *                       is to be used, otherwise the mappings for dotted I 
    *                       and dotless i marked with 'I' in CaseFolding.txt will 
    *                       be skipped.
    * @return               the case folding equivalent of the character, if any;
    *                       otherwise the character itself.
    * @see                  #foldCase(String, boolean)
    */
    public static int foldCase(int ch, boolean defaultmapping)
    {
        int props = PROPERTY_DB_.getProperty(ch);
        if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
            int type = UCharacterPropertyDB.getPropType(props);
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterPropertyDB.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterPropertyDB.getExceptionIndex(props);
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_CASE_FOLDING_)) {
                int exception = PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_CASE_FOLDING_);
                if (exception != 0) {
                    int foldedcasech = 
                         PROPERTY_DB_.getFoldCase(exception & LAST_CHAR_MASK_);
                    if (foldedcasech != 0){
                        return foldedcasech;
                    }
                }
                else {
                    // special case folding mappings, hardcoded
                    if (defaultmapping && 
                                 (ch == LATIN_SMALL_LETTER_DOTLESS_I_ || 
                                  ch == LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_)) {
                        // map dotted I and dotless i to U+0069 small i
                        return LATIN_SMALL_LETTER_I_;
                    }
                    // return ch itself because it is excluded from case folding
                    return ch;
                }                                  
            }
            if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_LOWERCASE_)) {  
                // not else! - allow to fall through from above
                return PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_);
            }
        }
            
        return ch; // no mapping - return the character itself
    }

    /**
    * The given string is mapped to its case folding equivalent according to
    * UnicodeData.txt and CaseFolding.txt; if any character has no case folding 
    * equivalent, the character itself is returned.
    * "Full", multiple-code point case folding mappings are returned here.
    * For "simple" single-code point mappings use the API 
    * foldCase(int ch, boolean defaultmapping).
    * @param str            the String to be converted
    * @param defaultmapping Indicates if all mappings defined in CaseFolding.txt 
    *                       is to be used, otherwise the mappings for dotted I 
    *                       and dotless i marked with 'I' in CaseFolding.txt will 
    *                       be skipped.
    * @return               the case folding equivalent of the character, if any;
    *                       otherwise the character itself.
    * @see                  #foldCase(int, boolean)
    */
    public static String foldCase(String str, boolean defaultmapping)
    {
        StringBuffer result = new StringBuffer(str.length() << 1);
        int          count  = 0;
        int          ch;
        int          size   = UTF16.countCodePoint(str);

        // case mapping loop
        while (count < size) {
            ch = UTF16.charAtCodePointOffset(str, count);
            count ++;
            int props = PROPERTY_DB_.getProperty(ch);
            if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
                int type = UCharacterPropertyDB.getPropType(props);
                if (type == UCharacterCategory.UPPERCASE_LETTER ||
                    type == UCharacterCategory.TITLECASE_LETTER) {
                    ch += UCharacterPropertyDB.getSignedValue(props);
                }
            }  
            else {
                int index = UCharacterPropertyDB.getExceptionIndex(props);
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                    UCharacterPropertyDB.EXC_CASE_FOLDING_)) {
                    int exception = PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_CASE_FOLDING_);                             
                    if (exception != 0) {
                        PROPERTY_DB_.getFoldCase(exception & LAST_CHAR_MASK_, 
                                             exception >> SHIFT_24_, result);
                    } 
                    else {
                        // special case folding mappings, hardcoded
                        if (defaultmapping && 
                                (ch == LATIN_SMALL_LETTER_DOTLESS_I_ || 
                                ch == LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_)) {
                            // map dotted I and dotless i to U+0069 small i
                            result.append(LATIN_SMALL_LETTER_I_);
                        } 
                        else {
                            // output c itself because it is excluded from 
                            // case folding
                            UTF16.append(result, ch);
                        }
                    }
                    // do not fall through to the output of c
                    continue;
                } 
                else {
                    if (PROPERTY_DB_.hasExceptionValue(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_)) {
                        ch = PROPERTY_DB_.getException(index, 
                                          UCharacterPropertyDB.EXC_LOWERCASE_);
                    }
                }
            }

            // handle 1:1 code point mappings from UnicodeData.txt
            UTF16.append(result, ch);
        }
        
        return result.toString();
    }
      
    // protected methods ====================================================
      
    /**
    * Forms a supplementary code point from the argument character<br>
    * Note this is for internal use hence no checks for the validity of the
    * surrogate characters are done
    * @param lead lead surrogate character
    * @param trail trailing surrogate character
    * @return code point of the supplementary character
    */
    protected static int getRawSupplementary(char lead, char trail)
    {
        return (lead << LEAD_SURROGATE_SHIFT_) + trail + SURROGATE_OFFSET_;
    }
      
    // private methods ==============================================
      
    /**
    * Gets the correct property information from UCharacterPropertyDB
    * @param ch character whose information is to be retrieved
    * @return a 32 bit information, returns 0 if no data is found.
    */
    private static int getProps(int ch)
    {
        if (ch >= UCharacter.MIN_VALUE & ch <= UCharacter.MAX_VALUE) {
            return PROPERTY_DB_.getProperty(ch);
        }
        return 0;
    }
      
    /**
    * Getting Han character digit values
    * @param ch code point to test if it is a Han character
    * @return Han digit value if ch is a Han digit character
    */
    private static int getHanDigit(int ch)
    {
        switch(ch)
        {
        case IDEOGRAPHIC_NUMBER_ZERO_ :
            return 0; // Han Zero
        case CJK_IDEOGRAPH_FIRST_ :
            return 1; // Han One
        case CJK_IDEOGRAPH_SECOND_ :
            return 2; // Han Two
        case CJK_IDEOGRAPH_THIRD_ :
            return 3; // Han Three
        case CJK_IDEOGRAPH_FOURTH_ :
            return 4; // Han Four
        case CJK_IDEOGRAPH_FIFTH_ :
            return 5; // Han Five
        case CJK_IDEOGRAPH_SIXTH_ :
            return 6; // Han Six
        case CJK_IDEOGRAPH_SEVENTH_ :
            return 7; // Han Seven
        case CJK_IDEOGRAPH_EIGHTH_ : 
            return 8; // Han Eight
        case CJK_IDEOGRAPH_NINETH_ :
            return 9; // Han Nine
        }
        return -1; // no value
    }
      
    /**
    * Special casing uppercase management
    * @param ch code point to convert
    * @param index of exception containing special case information
    * @param buffer to add uppercase
    * @param str original string
    * @param chindex index of ch in str
    * @param tr_az if uppercase is to be made with TURKISH or AZERBAIJANI 
    *        in mind
    * @param lt if uppercase is to be made with LITHUANIAN in mind
    */
    private static void getSpecialUpperCase(int ch, int index, 
                                            StringBuffer buffer, String str, 
                                            int chindex, boolean tr_az, 
                                            boolean lt)
    {
        int exception = PROPERTY_DB_.getException(index, 
                                    UCharacterPropertyDB.EXC_SPECIAL_CASING_);
        if (exception < 0) {
        // use hardcoded conditions and mappings
            if (ch == LATIN_SMALL_LETTER_I_) {
                if (tr_az) {
                    // turkish and azerbaijani : i maps to dotted I
                    buffer.append(LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_);
                }
                else {
                    // other languages: i maps to I
                    buffer.append(LATIN_CAPITAL_LETTER_I_);
                }
            } 
            else {
                if (ch == COMBINING_DOT_ABOVE_ && lt) {
                    // lithuanian: remove DOT ABOVE after U+0069 "i" with 
                    // upper or titlecase
                    for (int j = chindex; j > 0; j ++) {
                        ch = UTF16.charAtCodePointOffset(str, j);
                        if (getType(ch) != 
                            UCharacterCategory.NON_SPACING_MARK) {
                            break;
                        }
                    }
                            
                    // if the base letter is not an 'i' (U+0069)? keep the dot
                    if (ch != LATIN_SMALL_LETTER_I_) {
                        buffer.append(COMBINING_DOT_ABOVE_);
                    }
                } 
                else { 
                    // no known conditional special case mapping, output the 
                    // code point itself
                    UTF16.append(buffer, ch);
                }
            }
        } 
        else {
            // get the special case mapping string from the data file
            index = exception & LAST_CHAR_MASK_;
            PROPERTY_DB_.getUpperCase(index, buffer);
        }
    }
      
    /**
    * Special casing lowercase management
    * @param ch code point to convert
    * @param index of exception containing special case information
    * @param buffer to add lowercase
    * @param str original string
    * @param chindex index of ch in str
    * @param tr_az if uppercase is to be made with TURKISH or AZERBAIJANI 
    *        in mind
    * @param lt if uppercase is to be made with LITHUANIAN in mind
    */
    private static void getSpecialLowerCase(int ch, int index, 
                                            StringBuffer buffer, String str, 
                                            int chindex, boolean tr_az, 
                                            boolean lt)
    {
        int exception = PROPERTY_DB_.getException(index, 
                                    UCharacterPropertyDB.EXC_SPECIAL_CASING_);
        if (exception < 0) {
            // use hardcoded conditions and mappings
            if (ch == LATIN_CAPITAL_LETTER_I_) {
                if (tr_az) {
                    // turkish and azerbaijani : I maps to dotless i
                    buffer.append(LATIN_SMALL_LETTER_DOTLESS_I_);
                }
                else {
                    // other languages: I maps to i
                    buffer.append(LATIN_SMALL_LETTER_I_);
                }
            } 
            else {
                if (ch == GREEK_CAPITAL_LETTER_SIGMA_) {
                    // greek capital sigma maps depending on whether the 
                    // following character is a letter
                    chindex ++;
                    if (chindex != str.length() && 
                        isLetter(UTF16.charAtCodePointOffset(str, chindex))) {
                        buffer.append(GREEK_SMALL_LETTER_SIGMA_);
                    }
                    else {
                        buffer.append(GREEK_SMALL_LETTER_RHO_);
                    }
                } 
                else {
                    // no known conditional special case mapping, output the 
                    // code point itself
                    UTF16.append(buffer, ch);
                }
            }
        } 
        else 
        {
            // get the special case mapping string from the data file
            index = exception & LAST_CHAR_MASK_;
            PROPERTY_DB_.getLowerCase(index, buffer);
        }
    }
      
    /**
    * Determines if codepoint is a non character
    * @param ch codepoint
    * @return true if codepoint is a non character false otherwise
    */
    private static boolean isNonCharacter(int ch) 
    {
        if ((ch & LAST_CHAR_MASK_) >= NON_CHARACTER_SUFFIX_MIN_3_0_) {
            return true;
        }
        
        int difference = ch - NON_CHARACTER_MIN_3_1_;
        return difference >= 0 && difference <=  NON_CHARACTER_RANGE_3_1_;
    }
}

