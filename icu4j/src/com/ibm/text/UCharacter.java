/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UCharacter.java,v $ 
* $Date: 2001/11/12 21:51:02 $ 
* $Revision: 1.17 $
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
* To use this class please add the jar file name icu4j.jar to the 
* class path, since it contains data files which supply the information used 
* by this file.<br>
* E.g. In Windows <br>
* <code>set CLASSPATH=%CLASSPATH%;$JAR_FILE_PATH/ucharacter.jar</code>.<br>
* Otherwise, another method would be to copy the files uprops.dat and 
* unames.dat from the icu4j source subdirectory 
* <i>$ICU4J_SRC/src/com/ibm/text/resources</i> to your class directory 
* <i>$ICU4J_CLASS/com/ibm/text/resources</i>.
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
*      values '10' - '35'. UCharacter also does this in digit and
*      getNumericValue, to adhere to the java semantics of these
*      methods.  New methods unicodeDigit, and
*      getUnicodeNumericValue do not treat the above code points 
*      as having numeric values.  This is a semantic change from ICU4J 1.3.1.
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
    	
	
    // constructor ====================================================
      
    /**
    * Private constructor to prevent instantiation
    */
    private UCharacter()
    {
    }
      
    // public methods ===================================================
      
    /**
    * Retrieves the numeric value of a decimal digit code point.
    * <br>This method observes the semantics of
    * <code>java.lang.Character.digit()</code>.  Note that this
    * will return positive values for code points for which isDigit
    * returns false, just like java.lang.Character.
    * <br><em>Semantic Change:</em> In release 1.3.1 and
    * prior, this did not treat the European letters as having a
    * digit value, and also treated numeric letters and other numbers as 
    * digits.  
    * This has been changed to conform to the java semantics.
    * <br>A code point is a valid digit if and only if:
    * <ul>
    *   <li>ch is a decimal digit or one of the european letters, and
    *   <li>the value of ch is less than the specified radix.
    * </ul>
    * @param ch the code point to query
    * @param radix the radix
    * @return the numeric value represented by the code point in the
    * specified radix, or -1 if the code point is not a decimal digit
    * or if its value is too large for the radix
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
        
        if (result < 0 && radix > 10) {
            result = getEuropeanDigit(ch);
        }
        
        if (result < 0 || result >= radix) {
            return -1;
        }
        return result;
    }
    
    private static boolean isEuropeanDigit(int ch) {
        return (ch <= 0x7a && ((ch >= 0x41 && ch <= 0x5a) || ch >= 0x61)) ||
            (ch >= 0xff21 && (ch <= 0xff3a || (ch >= 0xff41 && ch <= 0xff5a)));
    }

    private static int getEuropeanDigit(int ch) {
        if (ch <= 0x7a) {
            if (ch >= 0x41 && ch <= 0x5a) {
                return ch + 10 - 0x41;
            } else if (ch >= 0x61) {
                return ch + 10 - 0x61;
            }
        } else if (ch >= 0xff21) {
            if (ch <= 0xff3a) {
                return ch + 10 - 0xff21;
            } else if (ch >= 0xff41 && ch <= 0xff5a) {
                return ch + 10 - 0xff41;
            }
        }
        return -1;
    }

    /**
    * Retrieves the numeric value of a decimal digit code point.
    * <br>This is a convenience overload of <code>digit(int, int)</code> 
    * that provides a decimal radix.
    * <br><em>Semantic Change:</em> In release 1.3.1 and prior, this
    * treated numeric letters and other numbers as digits.  This has
    * been changed to conform to the java semantics.
    * @param ch the code point to query
    * @return the numeric value represented by the code point,
    * or -1 if the code point is not a decimal digit or if its
    * value is too large for a decimal radix 
    */
    public static int digit(int ch)
    {
        return digit(ch, DECIMAL_RADIX_);
    }

   /**
    * Returns the Unicode numeric value of the code point as a nonnegative 
    * integer.
    * <br>If the code point does not have a numeric value, then -1 is returned. 
    * <br>
    * If the code point has a numeric value that cannot be represented as a 
    * nonnegative integer (for example, a fractional value), then -2 is 
    * returned.
    * <br><em>Semantic Change:</em> In release 1.3.1 and
    * prior, this returned -1 for ASCII letters and their
    * fullwidth counterparts.  This has been changed to
    * conform to the java semantics.
    * @param ch the code point to query
    * @return the numeric value of the code point, or -1 if it has no numeric 
    * value, or -2 if it has a numeric value that cannot be represented as a 
    * nonnegative integer
    */
    public static int getNumericValue(int ch)
    {
        return getNumericValueInternal(ch, true);
    }

   /**
    * Returns the Unicode numeric value of the code point as a nonnegative 
    * integer.
    * <br>If the code point does not have a numeric value, then -1 is returned. <br>
    * If the code point has a numeric value that cannot be represented as a 
    * nonnegative integer (for example, a fractional value), then -2 is 
    * returned.
    * This returns values other than -1 for all and only those code points 
    * whose type is a numeric type.
    * @param ch the code point to query
    * @return the numeric value of the code point, or -1 if it has no numeric 
    * value, or -2 if it has a numeric value that cannot be represented as a 
    * nonnegative integer
    */
    public static int getUnicodeNumericValue(int ch)
    {
        return getNumericValueInternal(ch, false);
    }

    private static int getNumericValueInternal(int ch, boolean useEuropean)
    {
        int props = getProps(ch);
        int type = UCharacterPropertyDB.getPropType(props);
        
        // if props == 0, it will just fall through and return -1
        if (type != UCharacterCategory.DECIMAL_DIGIT_NUMBER &&
            type != UCharacterCategory.LETTER_NUMBER &&
            type != UCharacterCategory.OTHER_NUMBER) {

            return useEuropean ? getEuropeanDigit(ch) : -1;
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
                                        UCharacterPropertyDB.EXC_DIGIT_VALUE_) &
                    LAST_CHAR_MASK_; 
            }
            else {
                if (PROPERTY_DB_.hasExceptionValue(index, 
                               UCharacterPropertyDB.EXC_DENOMINATOR_VALUE_)) {
                    return -2;
                }
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                   UCharacterPropertyDB.EXC_NUMERIC_VALUE_)) {
                    result = PROPERTY_DB_.getException(index, 
                                      UCharacterPropertyDB.EXC_NUMERIC_VALUE_); 
                }
            }
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
    * Determines if a code point is a Java digit.
    * <br>This method observes the semantics of
    * <code>java.lang.Character.isDigit()</code>.  It returns true for
    * decimal digits only.
    * <br><em>Semantic Change:</em> In release 1.3.1 and prior, this
    * treated numeric letters and other numbers as digits.  This has
    * been changed to conform to the java semantics.
    * @param ch code point to query
    * @return true if this code point is a digit */
    public static boolean isDigit(int ch)
    {
        return getType(ch) == UCharacterCategory.DECIMAL_DIGIT_NUMBER;
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
            // cat == UCharacterCategory.FORMAT;
            isIdentifierIgnorable(ch);
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
        /*
        int cat = getType(ch);
        // if props == 0, it will just fall through and return false
        return cat == UCharacterCategory.FORMAT;
        */
        // see java.lang.Character.isIdentifierIgnorable() on range of 
        // ignorable characters.
        return ch <= 8 || (ch >= 0xe && ch <= 0x1b) ||
               (ch >= 0x7f && ch <= 0x9f) ||
               getType(ch) == UCharacterCategory.FORMAT;
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
    public static int getCombiningClass(int ch)
    {
        int props = getProps(ch);
        if(!UCharacterPropertyDB.isExceptionIndicator(props)) {
        if (UCharacterPropertyDB.getPropType(props) == 
                                        UCharacterCategory.NON_SPACING_MARK) {
            return PROPERTY_DB_.getUnsignedValue(props);
        }
        else {
            return 0;
        }
        }
        else {
        // the combining class is in bits 23..16 of the first exception value
        return (PROPERTY_DB_.getException(
                                    PROPERTY_DB_.getExceptionIndex(props), 
                                    UCharacterPropertyDB.EXC_COMBINING_CLASS_)
                                    >> SHIFT_16_) & LAST_BYTE_MASK_;
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
        int size = str.length();
        StringBuffer result = new StringBuffer(size); // initial buffer
        int offset = 0;
        
        while (offset < size)
        {
            int ch = UTF16.charAt(str, offset);
            int props = PROPERTY_DB_.getProperty(ch);
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
                int index = UCharacterPropertyDB.getExceptionIndex(props);
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                  UCharacterPropertyDB.EXC_SPECIAL_CASING_)) {
                    getSpecialUpperCase(ch, index, result, str, offset, 
                                        locale);          
                }
                else {
                    if (PROPERTY_DB_.hasExceptionValue(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_)) {
                        UTF16.append(result, PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_UPPERCASE_));
                    }
                }
            }
            offset += UTF16.getCharCount(ch);
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
        // case mapping loop
        int offset = 0;
        int length = str.length();
        StringBuffer result = new StringBuffer(length);
        while (offset < length) {
            int ch = UTF16.charAt(str, offset);
            int props = PROPERTY_DB_.getProperty(ch);
            if (!UCharacterPropertyDB.isExceptionIndicator(props)) {
                int type = UCharacterPropertyDB.getPropType(props);
                if (type == UCharacterCategory.UPPERCASE_LETTER ||
                    type == UCharacterCategory.TITLECASE_LETTER) {
                    ch += UCharacterPropertyDB.getSignedValue(props);
                }
                UTF16.append(result, ch);
            }
            else {
                int index = UCharacterPropertyDB.getExceptionIndex(props);
                if (PROPERTY_DB_.hasExceptionValue(index, 
                                  UCharacterPropertyDB.EXC_SPECIAL_CASING_)) {
                    getSpecialLowerCase(ch, index, result, str, offset, 
                                        locale);          
                }
                else {
                    if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_LOWERCASE_)) {
                        UTF16.append(result, PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_));
                    }
                }
            }
            offset += UTF16.getCharCount(ch);
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
        int          size   = str.length();
        StringBuffer result = new StringBuffer(size);
        int          offset  = 0;
        int          ch;

        // case mapping loop
        while (offset < size) {
            ch = UTF16.charAt(str, offset);
            offset += UTF16.getCharCount(ch);
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
    
    /**
    * Return numeric value of Han code points.
    * <br> This returns the value of Han 'numeric' code points,
    * including those for zero, ten, hundred, thousand, ten thousand,
    * and hundred million.  Unicode does not consider these to be
    * numeric. This includes both the standard and 'checkwriting'
    * characters, the 'big circle' zero character, and the standard
    * zero character.
    * @draft
    * @param ch code point to query
    * @return value if it is a Han 'numeric character,' otherwise return -1.  
    */
    public static int getHanNumericValue(int ch)
    {
        switch(ch)
        {
        case IDEOGRAPHIC_NUMBER_ZERO_ :
        case CJK_IDEOGRAPH_COMPLEX_ZERO_ :
            return 0; // Han Zero
        case CJK_IDEOGRAPH_FIRST_ :
        case CJK_IDEOGRAPH_COMPLEX_ONE_ :
            return 1; // Han One
        case CJK_IDEOGRAPH_SECOND_ :
        case CJK_IDEOGRAPH_COMPLEX_TWO_ :
            return 2; // Han Two
        case CJK_IDEOGRAPH_THIRD_ :
        case CJK_IDEOGRAPH_COMPLEX_THREE_ :
            return 3; // Han Three
        case CJK_IDEOGRAPH_FOURTH_ :
        case CJK_IDEOGRAPH_COMPLEX_FOUR_ :
            return 4; // Han Four
        case CJK_IDEOGRAPH_FIFTH_ :
        case CJK_IDEOGRAPH_COMPLEX_FIVE_ :
            return 5; // Han Five
        case CJK_IDEOGRAPH_SIXTH_ :
        case CJK_IDEOGRAPH_COMPLEX_SIX_ :
            return 6; // Han Six
        case CJK_IDEOGRAPH_SEVENTH_ :
        case CJK_IDEOGRAPH_COMPLEX_SEVEN_ :
            return 7; // Han Seven
        case CJK_IDEOGRAPH_EIGHTH_ : 
        case CJK_IDEOGRAPH_COMPLEX_EIGHT_ :
            return 8; // Han Eight
        case CJK_IDEOGRAPH_NINETH_ :
        case CJK_IDEOGRAPH_COMPLEX_NINE_ :
            return 9; // Han Nine
        case CJK_IDEOGRAPH_TEN_ :
        case CJK_IDEOGRAPH_COMPLEX_TEN_ :
            return 10;
        case CJK_IDEOGRAPH_HUNDRED_ :
        case CJK_IDEOGRAPH_COMPLEX_HUNDRED_ :
            return 100;
        case CJK_IDEOGRAPH_THOUSAND_ :
        case CJK_IDEOGRAPH_COMPLEX_THOUSAND_ :
            return 1000;
        case CJK_IDEOGRAPH_TEN_THOUSAND_ :
            return 10000;
        case CJK_IDEOGRAPH_HUNDRED_MILLION_ :
            return 100000000;
        }
        return -1; // no value
    }
    
    // protected variables ===================================
    	
	/**
    * Shift and mask value for surrogates
    */
	protected static final int LEAD_SURROGATE_SHIFT_ = 10;
	protected static final int TRAIL_SURROGATE_MASK_ = 0x3FF;
                              
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
    
    /**
    * Han digit characters
    */
    private static final int CJK_IDEOGRAPH_COMPLEX_ZERO_     = 0x96f6;    
    private static final int CJK_IDEOGRAPH_COMPLEX_ONE_      = 0x58f9;    
    private static final int CJK_IDEOGRAPH_COMPLEX_TWO_      = 0x8cb3;    
    private static final int CJK_IDEOGRAPH_COMPLEX_THREE_    = 0x53c3;    
    private static final int CJK_IDEOGRAPH_COMPLEX_FOUR_     = 0x8086;    
    private static final int CJK_IDEOGRAPH_COMPLEX_FIVE_     = 0x4f0d;    
    private static final int CJK_IDEOGRAPH_COMPLEX_SIX_      = 0x9678;    
    private static final int CJK_IDEOGRAPH_COMPLEX_SEVEN_    = 0x67d2;    
    private static final int CJK_IDEOGRAPH_COMPLEX_EIGHT_    = 0x634c;    
    private static final int CJK_IDEOGRAPH_COMPLEX_NINE_     = 0x7396;    
    private static final int CJK_IDEOGRAPH_TEN_              = 0x5341;    
    private static final int CJK_IDEOGRAPH_COMPLEX_TEN_      = 0x62fe;    
    private static final int CJK_IDEOGRAPH_HUNDRED_          = 0x767e;    
    private static final int CJK_IDEOGRAPH_COMPLEX_HUNDRED_  = 0x4f70;    
    private static final int CJK_IDEOGRAPH_THOUSAND_         = 0x5343;    
    private static final int CJK_IDEOGRAPH_COMPLEX_THOUSAND_ = 0x4edf;    
    private static final int CJK_IDEOGRAPH_TEN_THOUSAND_     = 0x824c;    
    private static final int CJK_IDEOGRAPH_HUNDRED_MILLION_  = 0x5104;
    
    /**
    * Hyphens
    */
    private static final int HYPHEN_      = 0x2010;
    private static final int SOFT_HYPHEN_ = 0xAD;
    
    /**
    * LATIN SMALL LETTER J
    */
    private static final int LATIN_SMALL_LETTER_J_ = 0x6a;
    
    /**
    * LATIN SMALL LETTER I WITH OGONEK
    */
    private static final int LATIN_SMALL_LETTER_I_WITH_OGONEK_ = 0x12f;
    
    /**
    * LATIN SMALL LETTER I WITH TILDE BELOW
    */
    private static final int LATIN_SMALL_LETTER_I_WITH_TILDE_BELOW_ = 0x1e2d;
    
    /**
    * LATIN SMALL LETTER I WITH DOT BELOW
    */
    private static final int LATIN_SMALL_LETTER_I_WITH_DOT_BELOW_ = 0x1ecb;
    
    /**
    * Combining class for combining mark above
    */
    private static final int COMBINING_MARK_ABOVE_CLASS_ = 230;
    
    /**
    * LATIN CAPITAL LETTER J
    */
    private static final int LATIN_CAPITAL_LETTER_J_ = 0x4a;
    
    /**
    * LATIN CAPITAL LETTER I WITH OGONEK
    */
    private static final int LATIN_CAPITAL_I_WITH_OGONEK_ = 0x12e;
    
    /**
    * LATIN CAPITAL LETTER I WITH TILDE
    */
    private static final int LATIN_CAPITAL_I_WITH_TILDE_ = 0x128;
    
    /**
    * LATIN CAPITAL LETTER I WITH GRAVE
    */
    private static final int LATIN_CAPITAL_I_WITH_GRAVE_ = 0xcc;
    
    /**
    * LATIN CAPITAL LETTER I WITH ACUTE
    */
    private static final int LATIN_CAPITAL_I_WITH_ACUTE_ = 0xcd;
    
    /**
    * COMBINING GRAVE ACCENT
    */
    private static final int COMBINING_GRAVE_ACCENT_ = 0x300;
    
    /**
    * COMBINING ACUTE ACCENT
    */
    private static final int COMBINING_ACUTE_ACCENT_ = 0x301;
    
    /**
    * COMBINING TILDE
    */
    private static final int COMBINING_TILDE_ = 0x303;
      
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
    * Getting the locales used for case mapping
    * @param locale to work with
    * @return locale which the actual case mapping works with
    */
    private static Locale getCaseLocale(Locale locale) 
    {
        String language = locale.getLanguage();
        
        // the locale can have no language
        if (language.length() != 2) {
            return locale;
        }

        if (language.equals(TURKISH_) || language.equals(AZERBAIJANI_)) {
            return new Locale("tr", "TR");
        } 
        if (language.equals(LITHUANIAN_)) {
            return new Locale("lt", "LT");
        }
        return locale;
    }
    
    /**  
    * In Unicode 3.1.1, an ignorable sequence is a sequence of *zero* or more 
    * characters from the set {HYPHEN, SOFT HYPHEN, general category = Mn}.
    * (Expected to change!) 
    * @param ch codepoint
    * @param cat category of the argument codepoint
    * @return true if ch is case ignorable.
    */
    private static boolean isIgnorable(int ch, int cat) 
    {
        return cat == UCharacterCategory.NON_SPACING_MARK || ch == HYPHEN_ || 
                      ch == SOFT_HYPHEN_;
    }

    /** 
    * Determines if offset is not followed by a sequence consisting of
    * an ignorable sequence and then a cased letter {Ll, Lu, Lt}.
    * @param str string to determine
    * @param offset offset in string to check
    * @return false if any character after index in src is a cased letter
    * @see SpecialCasing.txt
    */
    private static boolean isCFINAL(String str, int offset) 
    {
        int length = str.length();
        offset += UTF16.getCharCount(UTF16.charAt(str, offset));
        while (offset < length) {
            int ch = UTF16.charAt(str, offset);
            int cat = getType(ch);
            if (cat == UCharacterCategory.LOWERCASE_LETTER || 
                cat == UCharacterCategory.UPPERCASE_LETTER ||
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return false; // followed by cased letter
            }
            if (!isIgnorable(ch, cat)) {
                return true; // not ignorable
            }
            offset += UTF16.getCharCount(ch);
        }

        return true;
    }

    /**
    * Determines if offset is not preceded by a sequence consisting of a cased 
    * letter {Ll, Lu, Lt} and an ignorable sequence. 
    * @param str string to determine
    * @param offset offset in string to check
    * @return true if any character before index in src is a cased letter
    * @see SpecialCasing.txt
    */
    private static boolean isNotCINITIAL(String str, int offset) 
    {
        offset --;
        while (offset >= 0) {
            int ch = UTF16.charAt(str, offset);
            int cat = getType(ch);
            if (cat == UCharacterCategory.LOWERCASE_LETTER || 
                cat == UCharacterCategory.UPPERCASE_LETTER ||
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return true; // preceded by cased letter
            }
            if (!isIgnorable(ch, cat)) {
                return false; // not ignorable
            }
            offset -= UTF16.getCharCount(ch);
        }

        return false; 
    }

    /**
    * Determines if a string at offset is preceded by any base characters 
    * { 'i', 'j', U+012f, U+1e2d, U+1ecb } with no intervening character with
    * combining class = 230
    * @param str string to be determined
    * @param offset offset in string to check
    * @return true if some characters preceding the offset index belongs to
    *         the set { 'i', 'j', U+012f, U+1e2d, U+1ecb }
    * @see SpecialCasing.txt
    */
    private static boolean isAFTER_i(String str, int offset) 
    {
        offset --;
        while (offset >= 0) {
            int ch = UTF16.charAt(str, offset);
            if (ch == LATIN_SMALL_LETTER_I_ || ch == LATIN_SMALL_LETTER_J_ || 
                ch == LATIN_SMALL_LETTER_I_WITH_OGONEK_ ||
                ch == LATIN_SMALL_LETTER_I_WITH_TILDE_BELOW_ || 
                ch == LATIN_SMALL_LETTER_I_WITH_DOT_BELOW_) {
                return true; // preceded by TYPE_i
            }
    
            int cc = getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                // preceded by different base character not TYPE_i), or 
                // intervening cc == 230
                return false; 
            }
            offset -= UTF16.getCharCount(ch);
        }

        return false; // not preceded by TYPE_i
    }

    /**
    * Determines if a string at offset is preceded by base characters 'I' with 
    * no intervening combining class = 230
    * @param str string to be determined
    * @param offset offset in string to check
    * @return true if some characters preceding the offset index is the
    *         character 'I' with no intervening combining class = 230
    * @see SpecialCasing.txt
    */
    private static boolean isAFTER_I(String str, int offset) 
    {
        offset --;
        while (offset >= 0) {
            int ch = UTF16.charAt(str, offset);
            if (ch == LATIN_CAPITAL_LETTER_I_) {
                return true; // preceded by I
            }

            int cc = getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                // preceded by different base character (not I), or 
                // intervening cc == 230
                return false; 
            }
            offset -= UTF16.getCharCount(ch);
        }

        return false; // not preceded by I
    }

    /** 
    * Determines if a string at offset is followed by one or more characters 
    * of combining class = 230.
    * @param str string to be determined
    * @param offset offset in string to check
    * @return true if a string at offset is followed by one or more characters 
    *         of combining class = 230.
    * @see SpecialCasing.txt
    */
    private static boolean isFollowedByMOREABOVE(String str, int offset) 
    {
        int length = str.length();
        offset += UTF16.getCharCount(UTF16.charAt(str, 0));
        while (offset < length) {
            int ch = UTF16.charAt(str, offset);
            int cc = getCombiningClass(ch);
            if (cc == COMBINING_MARK_ABOVE_CLASS_) {
                return true; // at least one cc==230 following 
            }
            if (cc == 0) {
                return false; // next base character, no more cc==230 following
            }
            offset += UTF16.getCharCount(ch);
        }

        return false; // no more cc == 230 following
    }

    /** 
    * Determines if a string at offset is followed by a dot above 
    * with no characters of combining class == 230 in between 
    * @param str string to be determined
    * @param offset offset in string to check
    * @return true if a string at offset is followed by oa dot above 
    *         with no characters of combining class == 230 in between
    * @see SpecialCasing.txt
    */
    private static boolean isFollowedByDotAbove(String str, int offset) 
    {
        int length = str.length();
        offset += UTF16.getCharCount(UTF16.charAt(str, 0));
        while (offset < length) {
            int ch = UTF16.charAt(str, offset);
            if (ch == COMBINING_DOT_ABOVE_) {
                return true;
            }
            int cc = getCombiningClass(ch);
            if (cc == 0 || cc == COMBINING_MARK_ABOVE_CLASS_) {
                return false; // next base character or cc==230 in between
            }
            offset += UTF16.getCharCount(ch);
        }

        return false; // no dot above following
    }
  
    /**
    * Special casing uppercase management
    * @param ch code point to convert
    * @param index of exception containing special case information
    * @param buffer to add uppercase
    * @param str original string
    * @param offset index of ch in str
    * @param tr_az if uppercase is to be made with TURKISH or AZERBAIJANI 
    *        in mind
    * @param lt if uppercase is to be made with LITHUANIAN in mind
    */
    private static void getSpecialUpperCase(int ch, int index, 
                                            StringBuffer buffer, String str, 
                                            int offset, Locale locale)
    {
        int exception = PROPERTY_DB_.getException(index, 
                                    UCharacterPropertyDB.EXC_SPECIAL_CASING_);
        if (exception < 0) {
            String language = locale.getLanguage();
            // use hardcoded conditions and mappings
            if ((language.equals(TURKISH_) || language.equals(AZERBAIJANI_))
                && ch == LATIN_SMALL_LETTER_I_) {
                // turkish: i maps to dotted I
                buffer.append(LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_);
            } 
            else {
                if (language.equals(LITHUANIAN_) && ch == COMBINING_DOT_ABOVE_ 
                    && isAFTER_i(str, offset)) {
                    // lithuanian: remove DOT ABOVE after U+0069 "i" with 
                    // upper or titlecase
                    return; // remove the dot (continue without output)
                } 
                else {
                    // no known conditional special case mapping, use a normal 
                    // mapping
                    if (PROPERTY_DB_.hasExceptionValue(index, 
                        UCharacterPropertyDB.EXC_UPPERCASE_)) {
                        UTF16.append(buffer, PROPERTY_DB_.getException(index, 
                                        UCharacterPropertyDB.EXC_UPPERCASE_)); 
                    }
                    else {
                        UTF16.append(buffer, ch);
                    }
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
    * @param offset index of ch in str
    * @param locale current locale
    */
    private static void getSpecialLowerCase(int ch, int index, 
                                            StringBuffer buffer, String str, 
                                            int offset, Locale locale)
    {
        int exception = PROPERTY_DB_.getException(index, 
                                    UCharacterPropertyDB.EXC_SPECIAL_CASING_);
        if (exception < 0) {
            // fill u and i with the case mapping result string
            // use hardcoded conditions and mappings
            if (locale.getLanguage().equals(LITHUANIAN_) &&
                // base characters, find accents above
                (((ch == LATIN_CAPITAL_LETTER_I_ || 
                   ch == LATIN_CAPITAL_LETTER_J_ ||
                   ch == LATIN_CAPITAL_I_WITH_OGONEK_) &&
                  isFollowedByMOREABOVE(str, offset)) ||
                  // precomposed with accent above, no need to find one
                  (ch == LATIN_CAPITAL_I_WITH_GRAVE_ || 
                   ch == LATIN_CAPITAL_I_WITH_ACUTE_ || 
                   ch == LATIN_CAPITAL_I_WITH_TILDE_))) {
                   // lithuanian: add a dot above if there are more accents 
                   // above (to always have the dot)
                   switch(ch) {
                   case LATIN_CAPITAL_LETTER_I_: 
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        break;
                   case LATIN_CAPITAL_LETTER_J_: 
                        buffer.append((char)LATIN_SMALL_LETTER_J_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        break;
                   case LATIN_CAPITAL_I_WITH_OGONEK_:
                        buffer.append((char)LATIN_SMALL_LETTER_I_WITH_OGONEK_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        break;
                   case LATIN_CAPITAL_I_WITH_GRAVE_: 
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        buffer.append((char)COMBINING_GRAVE_ACCENT_);
                        break;
                   case LATIN_CAPITAL_I_WITH_ACUTE_: 
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        buffer.append((char)COMBINING_ACUTE_ACCENT_);
                        break;
                   case LATIN_CAPITAL_I_WITH_TILDE_:
                        buffer.append((char)LATIN_SMALL_LETTER_I_);
                        buffer.append((char)COMBINING_DOT_ABOVE_);
                        buffer.append((char)COMBINING_TILDE_);
                        break;
                   }
                   /*
                   Note: This handling of I and of dot above differs from 
                   Unicode 3.1.1's SpecialCasing-5.txt because the AFTER_i 
                   condition there does not work for decomposed I+dot above.
                   This fix is being proposed to the UTC.
                   */
            } 
            else {
                String language = locale.getLanguage();
                if ((language.equals(TURKISH_) || 
                     language.equals(AZERBAIJANI_)) && 
                    ch == LATIN_CAPITAL_LETTER_I_ && 
                    !isFollowedByDotAbove(str, offset)) {
                    // turkish: I maps to dotless i
                    // other languages or turkish with decomposed I+dot above: 
                    // I maps to i
                    buffer.append(LATIN_SMALL_LETTER_DOTLESS_I_);
                } 
                else {
                    if (ch == COMBINING_DOT_ABOVE_ && 
                        isAFTER_I(str, offset) && 
                        !isFollowedByMOREABOVE(str, offset)) {
                        // decomposed I+dot above becomes i (see handling of 
                        // U+0049 for turkish) and removes the dot above
                        return; // remove the dot (continue without output)
                    } 
                    else {
                        if (ch == GREEK_CAPITAL_LETTER_SIGMA_ &&
                            isCFINAL(str, offset) &&
                            isNotCINITIAL(str, offset)) {
                            // greek capital sigma maps depending on 
                            // surrounding cased letters
                            buffer.append(GREEK_SMALL_LETTER_RHO_);
                        } 
                        else {
                            // no known conditional special case mapping, use 
                            // a normal mapping
                            if (PROPERTY_DB_.hasExceptionValue(index, 
                                       UCharacterPropertyDB.EXC_LOWERCASE_)) {
                                UTF16.append(buffer, 
                                             PROPERTY_DB_.getException(index, 
                                         UCharacterPropertyDB.EXC_LOWERCASE_)); 
                            }
                            else {
                                UTF16.append(buffer, ch);
                            }
                        }
                    } 
                }
            }
        }
        else {
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

