/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/UCharacter.java,v $ 
* $Date: 2000/12/26 22:41:10 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/


package com.ibm.icu.text;

/**
* A static class designed to be a generic code point information source that 
* handles surrogate pairs.<br>
* Data for code point information originates from Unicode 3.0 data files, 
* UnicodeData.txt and Mirror.txt, downloadable from the Unicode Consortium site 
* ftp://ftp.unicode.org/Public/<br>
* ICU's gennames and genprops programs are used to compact the information from 
* the above mentioned files before being used by this package. The binary 
* result files are named unames.dat and uprops.dat. <br>
* Both are jared with the package for release, hence to use this class please
* add the jar file name <code>ucharacter.jar</code> to your class path.<br>
* E.g. In Windows <code>set CLASSPATH=%CLASSPATH%;$JAR_FILE_PATH/ucharacter.jar
* </code><br>
* For more information about the data file format, please refer to 
* <a href=http://oss.software.ibm.com/icu4j/icu4jhtml/com/ibm/icu/text/ReadMe.html>
* Read Me</a>.<br>
* Each code point used here in in terms of a 32 bit int. This is so as to 
* handle supplementary code points which has 21 bit in size.<br>
* APIs provide up-to-date Unicode implementation of java.lang.Character, <br>
* hence
* <ul>
*   <li> Deprecated APIs are not defined here
*   <li> UCharacter is not designed to be a char wrapper and does not have APIs 
*        to which involves management of that single char. e.g. char 
*        charValue(), int compareTo(java.lang.Character, java.lang.Character) 
*        etc.
*	  <li> To handle surrogates, int parameters APIs are provided
*   <li> Java specific character information is not defined e.g. boolean 
*        isJavaIdentifierPart(char ch)
*   <li> Has extra methods to fully utilize up-to-date Unicode data information
*   <li> Provides methods to gets code points from a UTF-16 char or surrogate 
*        pairs
* </ul>
* <p>
* Difference between UCharacter and java.lang.Character
* <ul>
*   <li> UCharacter supports Unicode 3.0 Data while java.lang.Character 
*        supports only Unicode 2.0. <br>
*        Note : UCharacter will support Unicode 3.1 when it officially releases
*   <li> UCharacter provides the support for supplementary code points.<br>
*        Hence the code point type used is an int (support for 21 bits) where
*        else java.lang.Character uses a char (16 bit)
*   <li> The below control code points had their type is overwritten by ICU to 
*        the type shown
*        <ul>
*          <li> TAB 0x9 : U_SPACE_SEPARATOR
*          <li> VT 0xb : U_SPACE_SEPARATOR
*          <li> LF 0xa : U_PARAGRAPH_SEPARATOR
*          <li> FF 0xc : U_LINE_SEPARATOR
*          <li> CR 0xd : U_PARAGRAPH_SEPARATOR
*          <li> FS 0x1c : U_PARAGRAPH_SEPARATOR
*          <li> GS 0x1d : U_PARAGRAPH_SEPARATOR
*          <li> RS 0x1e : U_PARAGRAPH_SEPARATOR
*          <li> US 0x1f : U_SPACE_SEPARATOR
*          <li> NL 0x85 : U_PARAGRAPH_SEPARATOR
*        </ul>
*        Because of these type overwrites, some methods might be affected.
*   <li> java.lang.Character maps characters 'A' - 'Z' and 'a' - 'z' to the 
*        numeric values '10' - '35'. UCharacter does not treat the above 
*        code points as having numeric values
*   <li> Further detail differences can be determined from the program 
*        <a href = ../test/text/UCharacterCompare.html>
*        com.ibm.icu.test.text.UCharacterCompare</a>
* </ul>
* </p> 
* Examples on using this class is located at the test program 
* <a href = ../test/text/UCharacterCompare.html>
*        com.ibm.icu.test.text.UCharacterTest</a>
* @author Syn Wee Quek
* @since oct 06 2000
* @see com.ibm.icu.text.UCharacterCategoryEnum
* @see com.ibm.icu.text.UCharacterDirectionEnum
* @see com.ibm.icu.test.text.UCharacterCompare
* @see com.ibm.icu.test.text.UCharacterTest
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
  * For isLegal() use, checks if code point is not a character
  */
  private static final int LAST_CHAR_MASK_ = 0xFFFF;
  private static final int NOT_A_CHAR_ = 0xFFFE;
  
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
    if (!UCharacterPropertyDB.isExceptionIndicator(props))   
    {
      // not contained in exception data
      if (UCharacterPropertyDB.getPropType(props) == 
          UCharacterCategoryEnum.DECIMAL_DIGIT_NUMBER)
        result = UCharacterPropertyDB.getSignedValue(props);
    }
    else 
    {
      // contained in exception data
      int index = UCharacterPropertyDB.getExceptionIndex(props);
      if (PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_DIGIT_VALUE_)) 
        result  = PROPERTY_DB_.getException(index, 
                                        UCharacterPropertyDB.EXC_DIGIT_VALUE_); 
      else 
        if (!PROPERTY_DB_.hasExceptionValue(index, 
                                   UCharacterPropertyDB.EXC_DENOMINATOR_VALUE_)
            && PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_NUMERIC_VALUE_))
        result  = PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_NUMERIC_VALUE_); 
    }
    
    if (result < 0)
      result = getHanDigit(ch);
    
    if (result < 0 || result >= radix)
      return -1;
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
    if (type != UCharacterCategoryEnum.DECIMAL_DIGIT_NUMBER &&
        type != UCharacterCategoryEnum.LETTER_NUMBER &&
        type != UCharacterCategoryEnum.OTHER_NUMBER)
      return -1;
      
    int result = -1;
    if (!UCharacterPropertyDB.isExceptionIndicator(props))   
      // not contained in exception data
      result = UCharacterPropertyDB.getSignedValue(props);
    else 
    {
      // contained in exception data
      int index = UCharacterPropertyDB.getExceptionIndex(props);
      if (PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_DIGIT_VALUE_)) 
        result  = PROPERTY_DB_.getException(index, 
                                        UCharacterPropertyDB.EXC_DIGIT_VALUE_); 
      else 
        if (!PROPERTY_DB_.hasExceptionValue(index, 
                                   UCharacterPropertyDB.EXC_DENOMINATOR_VALUE_)
            && PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_NUMERIC_VALUE_))
        result  = PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_NUMERIC_VALUE_); 
    }
    
    if (result < 0)
      result = getHanDigit(ch);
    
    if (result < 0)
      return -2;
    return result;
  }
  
  /**
  * Returns a value indicating a code point's Unicode category.<br>
  * Up-to-date Unicode implementation of java.lang.Character.getType() except 
  * for the above mentioned code points that had their category changed.<br>
  * Return results are constants from the interface 
  * <a href=UCharacterCategoryEnum.html>UCharacterCategoryEnum</a>
  * @param ch code point whose type is to be determined
  * @return category which is a value of UCharacterCategoryEnum
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
    return cat == UCharacterCategoryEnum.DECIMAL_DIGIT_NUMBER || 
           cat == UCharacterCategoryEnum.OTHER_NUMBER ||
           cat == UCharacterCategoryEnum.LETTER_NUMBER;
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
    return cat == UCharacterCategoryEnum.UPPERCASE_LETTER || 
           cat == UCharacterCategoryEnum.LOWERCASE_LETTER || 
           cat == UCharacterCategoryEnum.TITLECASE_LETTER || 
           cat == UCharacterCategoryEnum.MODIFIER_LETTER ||
           cat == UCharacterCategoryEnum.OTHER_LETTER;
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
    return getType(ch) == UCharacterCategoryEnum.LOWERCASE_LETTER;
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
    return (cat == UCharacterCategoryEnum.SPACE_SEPARATOR || 
            cat == UCharacterCategoryEnum.LINE_SEPARATOR ||
            cat == UCharacterCategoryEnum.PARAGRAPH_SEPARATOR) && 
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
    return cat == UCharacterCategoryEnum.SPACE_SEPARATOR || 
           cat == UCharacterCategoryEnum.LINE_SEPARATOR ||
           cat == UCharacterCategoryEnum.PARAGRAPH_SEPARATOR;
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
    return cat == UCharacterCategoryEnum.TITLECASE_LETTER;
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
    return cat == UCharacterCategoryEnum.UPPERCASE_LETTER || 
           cat == UCharacterCategoryEnum.LOWERCASE_LETTER || 
           cat == UCharacterCategoryEnum.TITLECASE_LETTER || 
           cat == UCharacterCategoryEnum.MODIFIER_LETTER ||
           cat == UCharacterCategoryEnum.OTHER_LETTER || 
           cat == UCharacterCategoryEnum.LETTER_NUMBER ||
           cat == UCharacterCategoryEnum.CONNECTOR_PUNCTUATION ||
           cat == UCharacterCategoryEnum.DECIMAL_DIGIT_NUMBER ||
           cat == UCharacterCategoryEnum.COMBINING_SPACING_MARK || 
           cat == UCharacterCategoryEnum.NON_SPACING_MARK || 
           cat == UCharacterCategoryEnum.FORMAT;
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
    return cat == UCharacterCategoryEnum.UPPERCASE_LETTER || 
           cat == UCharacterCategoryEnum.LOWERCASE_LETTER || 
           cat == UCharacterCategoryEnum.TITLECASE_LETTER || 
           cat == UCharacterCategoryEnum.MODIFIER_LETTER ||
           cat == UCharacterCategoryEnum.OTHER_LETTER || 
           cat == UCharacterCategoryEnum.LETTER_NUMBER;
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
    return cat == UCharacterCategoryEnum.FORMAT;
  }
                  
  /**
  * Determines if the specified code point is an uppercase character.<br>
  * UnicodeData only contains case mappings for code point where they are 
  * one-to-one mappings; it also omits information about context-sensitive 
  * case mappings.<br> 
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
    return cat == UCharacterCategoryEnum.UPPERCASE_LETTER;
  }
                   
  /**
  * The given code point is mapped to its lowercase equivalent; if the code 
  * point has no lowercase equivalent, the code point itself is returned.<br>
  * UnicodeData only contains case mappings for code point where they are 
  * one-to-one mappings; it also omits information about context-sensitive 
  * case mappings.<br> 
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
    if(!UCharacterPropertyDB.isExceptionIndicator(props)) 
    {
      int cat = UCharacterPropertyDB.getPropType(props);
      if (cat == UCharacterCategoryEnum.UPPERCASE_LETTER || 
          cat == UCharacterCategoryEnum.TITLECASE_LETTER) 
        return ch + UCharacterPropertyDB.getSignedValue(props);
    } 
    else 
    {
      int index = UCharacterPropertyDB.getExceptionIndex(props);
      if (PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_LOWERCASE_)) 
        return PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_LOWERCASE_); 
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
    if (ch < MIN_VALUE || ch > MAX_VALUE)
      return null;
    
    if (ch < UCharacter.SUPPLEMENTARY_MIN_VALUE) 
      return String.valueOf((char)ch);
    
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
  * For more information about Unicode case mapping please refer
  * to the <a href=http://www.unicode.org/unicode/reports/tr21/>
  * Technical report #21</a>.<br>
  * If no titlecase is available, the uppercase is returned. If no uppercase is
  * available, the code point itself is returned.<br>
  * Up-to-date Unicode implementation of java.lang.Character.toTitleCase()
  * @param ch code point  whose title case is to be retrieved
  * @return titlecase code point
  */
  public static int toTitleCase(int ch)
  {
    int props = getProps(ch);
    // if props == 0, it will just fall through and return itself
    if (!UCharacterPropertyDB.isExceptionIndicator(props)) 
    {
      if (UCharacterPropertyDB.getPropType(props) == 
          UCharacterCategoryEnum.LOWERCASE_LETTER) 
        // here, titlecase is same as uppercase
        return ch - UCharacterPropertyDB.getSignedValue(props);
    } 
    else 
    {
      int index = UCharacterPropertyDB.getExceptionIndex(props);
      if (PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_TITLECASE_)) 
        return PROPERTY_DB_.getException(index,
                                      UCharacterPropertyDB.EXC_TITLECASE_);
      else
        // here, titlecase is same as uppercase
        if (PROPERTY_DB_.hasExceptionValue(index, 
                                        UCharacterPropertyDB.EXC_UPPERCASE_)) 
          return PROPERTY_DB_.getException(index, 
                                       UCharacterPropertyDB.EXC_UPPERCASE_); 
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
    if (!UCharacterPropertyDB.isExceptionIndicator(props)) 
    {
      if (UCharacterPropertyDB.getPropType(props) == 
          UCharacterCategoryEnum.LOWERCASE_LETTER) 
        // here, titlecase is same as uppercase */
        return ch - UCharacterPropertyDB.getSignedValue(props);
    }
    else 
    {
      int index = UCharacterPropertyDB.getExceptionIndex(props);
      if (PROPERTY_DB_.hasExceptionValue(index, 
                                      UCharacterPropertyDB.EXC_UPPERCASE_)) 
        return PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_UPPERCASE_); 
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
    return (ch >= 0 && ch < 0xFFFF);
  }

  /**
  * Determines whether the specified code point is a control character or not.
  * <br>
  * A code point is considered a control character if it is of either type 
  * <ul>
  * <li> Cc control character
  * <li> Cf Format character
  * <li> Zl line seperator 
  * <li> Zp paragraph seperator
  * </ul>
  * @param ch the code point to be determined if it is a control character
  * @return true if the code point is a control character
  */
  public static boolean isControl(int ch)
  {
    int cat = getType(ch);
    // if props == 0, it will just fall through and return false
    return cat == UCharacterCategoryEnum.CONTROL || 
           cat == UCharacterCategoryEnum.FORMAT || 
           cat == UCharacterCategoryEnum.LINE_SEPARATOR || 
           cat == UCharacterCategoryEnum.PARAGRAPH_SEPARATOR;
  }

  /**
  * Determines whether the specified code point is a printable character 
  * according to the Unicode standard.
  * @param ch code point to be determined if it is printable
  * @return true if the code point is a printable character
  */
  public static boolean isPrintable(int ch)
  {
    int cat = getType(ch);
    // if props == 0, it will just fall through and return false
    return cat == UCharacterCategoryEnum.DECIMAL_DIGIT_NUMBER || 
           cat == UCharacterCategoryEnum.OTHER_NUMBER ||
           cat == UCharacterCategoryEnum.LETTER_NUMBER || 
           cat == UCharacterCategoryEnum.UPPERCASE_LETTER || 
           cat == UCharacterCategoryEnum.LOWERCASE_LETTER || 
           cat == UCharacterCategoryEnum.TITLECASE_LETTER ||
           cat == UCharacterCategoryEnum.MODIFIER_LETTER || 
           cat == UCharacterCategoryEnum.OTHER_LETTER ||
           cat == UCharacterCategoryEnum.NON_SPACING_MARK || 
           cat == UCharacterCategoryEnum.ENCLOSING_MARK ||
           cat == UCharacterCategoryEnum.COMBINING_SPACING_MARK || 
           cat == UCharacterCategoryEnum.SPACE_SEPARATOR ||
           cat == UCharacterCategoryEnum.LINE_SEPARATOR || 
           cat == UCharacterCategoryEnum.PARAGRAPH_SEPARATOR ||
           cat == UCharacterCategoryEnum.DASH_PUNCTUATION || 
           cat == UCharacterCategoryEnum.START_PUNCTUATION ||
           cat == UCharacterCategoryEnum.END_PUNCTUATION || 
           cat == UCharacterCategoryEnum.CONNECTOR_PUNCTUATION ||
           cat == UCharacterCategoryEnum.OTHER_PUNCTUATION || 
           cat == UCharacterCategoryEnum.MATH_SYMBOL ||
           cat == UCharacterCategoryEnum.CURRENCY_SYMBOL || 
           cat == UCharacterCategoryEnum.MODIFIER_SYMBOL ||
           cat == UCharacterCategoryEnum.OTHER_SYMBOL;
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
    return cat == UCharacterCategoryEnum.DECIMAL_DIGIT_NUMBER || 
           cat == UCharacterCategoryEnum.OTHER_NUMBER || 
           cat == UCharacterCategoryEnum.LETTER_NUMBER || 
           cat == UCharacterCategoryEnum.UPPERCASE_LETTER || 
           cat == UCharacterCategoryEnum.LOWERCASE_LETTER || 
           cat == UCharacterCategoryEnum.TITLECASE_LETTER ||
           cat == UCharacterCategoryEnum.MODIFIER_LETTER || 
           cat == UCharacterCategoryEnum.OTHER_LETTER || 
           cat == UCharacterCategoryEnum.NON_SPACING_MARK || 
           cat == UCharacterCategoryEnum.ENCLOSING_MARK ||
           cat == UCharacterCategoryEnum.COMBINING_SPACING_MARK;
  }

  /**
  * Returns the Bidirection property of a code point.<br>
  * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional 
  * property.<br>
  * Result returned belongs to the interface 
  * <a href=UCharacterDirectionEnum.html>UCharacterDirectionEnum</a>
  * @param ch the code point to be determined its direction
  * @return direction constant from UCharacterDirectionEnum. Otherwise is 
  *         character is not defined, UCharacterDirectionEnum.BOUNDARY_NEUTRAL
  *         will be returned.
  */
  public static int getDirection(int ch)
  {
    int props = getProps(ch);
    if (props != 0) 
      return UCharacterPropertyDB.getDirection(props);
    return UCharacterDirectionEnum.LEFT_TO_RIGHT;
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
    if (UCharacterPropertyDB.isMirrored(props))
      if(!UCharacterPropertyDB.isExceptionIndicator(props)) 
        return ch + UCharacterPropertyDB.getSignedValue(props);
      else 
      {
        int index = UCharacterPropertyDB.getExceptionIndex(props);
        if (PROPERTY_DB_.hasExceptionValue(index, 
                                     UCharacterPropertyDB.EXC_MIRROR_MAPPING_)) 
          return PROPERTY_DB_.getException(index, 
                                     UCharacterPropertyDB.EXC_MIRROR_MAPPING_);   
      }
    return ch;
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
    if (ch < MIN_VALUE) return false;
    if (ch < SURROGATE_MIN_VALUE_) return true;
    if (ch <= SURROGATE_MAX_VALUE_) return false;
    
    if ((ch & LAST_CHAR_MASK_) >= NOT_A_CHAR_) 
      return false;
    return (ch <= MAX_VALUE);
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
  * Retrieve the most current Unicode name of the argument code point.<br>
  * Note calling any methods related to code point names, e.g. get*Name*() 
  * incurs a one-time initialisation cost to construct the name tables.
  * @param ch the code point for which to get the name
  * @return most current Unicode name
  */
  public static String getName(int ch)
  {
    return UCharacterName.getName(ch, 
                                 UCharacterNameChoiceEnum.U_UNICODE_CHAR_NAME);
  }
  
  /**
  * Retrieve the earlier version 1.0 Unicode name of the argument code point.
  * <br>
  * Note calling any methods related to code point names, e.g. get*Name*() 
  * incurs a one-time initialisation cost to construct the name tables.
  * @param ch the code point for which to get the name
  * @return version 1.0 Unicode name
  */
  public static String getName1_0(int ch)
  {
    return UCharacterName.getName(ch, 
                              UCharacterNameChoiceEnum.U_UNICODE_10_CHAR_NAME);
  }
  
  /**
  * Find a Unicode code point by its most current Unicode name and return its 
  * code point value.<br>
  * Note calling any methods related to code point names, e.g. get*Name*() 
  * incurs a one-time initialisation cost to construct the name tables.
  * @param name most current Unicode character name whose code point is to be 
  *        returned
  * @return code point
  */
  public static int getCharFromName(String name)
  {
    return UCharacterName.getCharFromName(
                           UCharacterNameChoiceEnum.U_UNICODE_CHAR_NAME, name);
  }
  
  /**
  * Find a Unicode character by its version 1.0 Unicode name and return its 
  * code point value.<br>
  * Note calling any methods related to code point names, e.g. get*Name*() 
  * incurs a one-time initialisation cost to construct the name tables.
  * @param name Unicode 1.0 code point name whose code point is to 
  *             returned
  * @return code point
  */
  public static int getCharFromName1_0(String name)
  {
    return UCharacterName.getCharFromName(
                        UCharacterNameChoiceEnum.U_UNICODE_10_CHAR_NAME, name);
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
    if (UTF16.isLeadSurrogate(lead) && UTF16.isTrailSurrogate(trail))
      return getRawSupplementary(lead, trail);
    return UCharacter.REPLACEMENT_CHAR;
  }
  
  /**
  * Returns the code point corresponding to the UTF16 character.<br>
  * If argument char16 is a surrogate character, UCharacter.REPLACEMENT_CHAR 
  * is returned
  * @param char16 the UTF16 character
  * @return code point or UCharacter.REPLACEMENT_CHAR if argument is not a 
  *         invalid character.
  */
  public static int getCodePoint(char char16) 
  {
    if (UCharacter.isLegal(char16))
      return char16;
    return UCharacter.REPLACEMENT_CHAR;
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
  * Gets the correct property information from UCharacterPptyDB
  * @param ch character whose information is to be retrieved
  * @return a 32 bit information, returns 0 if no data is found.
  */
  private static int getProps(int ch)
  {
    if (ch >= UCharacter.MIN_VALUE & ch <= UCharacter.MAX_VALUE)
      return PROPERTY_DB_.getProperty(ch);
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
}

