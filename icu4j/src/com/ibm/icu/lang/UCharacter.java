/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/lang/UCharacter.java,v $ 
* $Date: 2002/03/15 22:48:07 $ 
* $Revision: 1.32 $
*
*******************************************************************************
*/

package com.ibm.icu.lang;

import java.util.Locale;
import com.ibm.icu.impl.UnicodeProperty;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.Utility; 
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.ValueIterator;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.impl.NormalizerImpl;

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
* <i>$ICU4J_SRC/src/com.ibm.icu.impl.data</i> to your class directory 
* <i>$ICU4J_CLASS/com.ibm.icu.impl.data</i>.
* </p>
* <p>
* For more information about the data file format, please refer to 
* <a href=http://oss.software.ibm.com/icu4j/doc/com/ibm/icu/lang/ReadMe.html>
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
* </ul>
* <p>
* Further detail differences can be determined from the program 
*        <a href = http://oss.software.ibm.com/developerworks/opensource/cvs/icu4j/~checkout~/icu4j/src/com/ibm/icu/dev/test/lang/UCharacterCompare.java>
*        com.ibm.icu.dev.test.lang.UCharacterCompare</a>
* </p>
* @author Syn Wee Quek
* @since oct 06 2000
* @see com.ibm.icu.lang.UCharacterCategory
* @see com.ibm.icu.lang.UCharacterDirection
*/

public final class UCharacter
{ 
    // public data members -----------------------------------------------
  
    /** 
    * The lowest Unicode code point value.
    */
    public static final int MIN_VALUE = UTF16.CODEPOINT_MIN_VALUE;

    /**
    * The highest Unicode code point value (scalar value) according to the 
    * Unicode Standard.<br> 
    * This is a 21-bit value (21 bits, rounded up).<br>
    * Up-to-date Unicode implementation of java.lang.Character.MIN_VALUE
    */
    public static final int MAX_VALUE = UTF16.CODEPOINT_MAX_VALUE; 
      
    /**
    * The minimum value for Supplementary code points
    */
    public static final int SUPPLEMENTARY_MIN_VALUE = 
                                          UTF16.SUPPLEMENTARY_MIN_VALUE;
      
    /**
    * Unicode value used when translating into Unicode encoding form and there 
    * is no existing character.
    */
	public static final int REPLACEMENT_CHAR = '\uFFFD';
    	
	
    // public methods ----------------------------------------------------
      
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
        int props       = getProps(ch);
        int numericType = UCharacterProperty.getNumericType(props);
        
        int result = -1;
        if (numericType == UCharacterProperty.GENERAL_NUMERIC_TYPE_) {
        	// if props == 0, it will just fall through and return -1
        	if (!UCharacterProperty.isExceptionIndicator(props)) {
            	// not contained in exception data
            	result = UCharacterProperty.getSignedValue(props);
            }
            else {
            	int index = UCharacterProperty.getExceptionIndex(props);
            	if (PROPERTY_.hasExceptionValue(index, 
                                   UCharacterProperty.EXC_NUMERIC_VALUE_)) {
                	return PROPERTY_.getException(index, 
                                      UCharacterProperty.EXC_NUMERIC_VALUE_); 
                }
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
        return UCharacterProperty.getPropType(getProps(ch));
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
                (ch != NO_BREAK_SPACE_) && 
                (ch != NARROW_NO_BREAK_SPACE_) && 
                (ch != ZERO_WIDTH_NO_BREAK_SPACE_) ||
                // TAB VT LF FF CR FS GS RS US NL are all control characters
                // that are white spaces.
                (ch >= 0x9 && ch <= 0xd) || (ch >= 0x1c && ch <= 0x1f) ||
                (ch == 0x85);
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
            cat == UCharacterCategory.PARAGRAPH_SEPARATOR ||
            // TAB VT LF FF CR FS GS RS US NL are all control characters
            // that are white spaces.
            (ch >= 0x9 && ch <= 0xd) || (ch >= 0x1c && ch <= 0x1f) ||
            (ch == 0x85);
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
        if(!UCharacterProperty.isExceptionIndicator(props)) {
            int cat = UCharacterProperty.getPropType(props);
            if (cat == UCharacterCategory.UPPERCASE_LETTER || 
                cat == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
        } 
        else 
        {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                                       UCharacterProperty.EXC_LOWERCASE_)) {
                return PROPERTY_.getException(index, 
                                         UCharacterProperty.EXC_LOWERCASE_); 
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
        
        if (ch < SUPPLEMENTARY_MIN_VALUE) {
            return String.valueOf((char)ch);
        }
        
        StringBuffer result = new StringBuffer();
        result.append(UTF16.getLeadSurrogate(ch));
        result.append(UTF16.getTrailSurrogate(ch));
        return result.toString();
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
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            if (UCharacterProperty.getPropType(props) == 
                UCharacterCategory.LOWERCASE_LETTER) {
                // here, titlecase is same as uppercase
                return ch - UCharacterProperty.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                                         UCharacterProperty.EXC_TITLECASE_)) {
                return PROPERTY_.getException(index,
                                         UCharacterProperty.EXC_TITLECASE_);
            }
            else {
                // here, titlecase is same as uppercase
                if (PROPERTY_.hasExceptionValue(index, 
                                       UCharacterProperty.EXC_UPPERCASE_)) {
                    return PROPERTY_.getException(index, 
                                         UCharacterProperty.EXC_UPPERCASE_); 
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
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            if (UCharacterProperty.getPropType(props) == 
                UCharacterCategory.LOWERCASE_LETTER) {
                // here, titlecase is same as uppercase */
                return ch - UCharacterProperty.getSignedValue(props);
            }
        }
        else 
        {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                                         UCharacterProperty.EXC_UPPERCASE_)) {
                return PROPERTY_.getException(index, 
                                         UCharacterProperty.EXC_UPPERCASE_); 
            }
        }
        return ch; // no mapping - return c itself
    }
       
    // extra methods not in java.lang.Character --------------------------
       
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
        return (ch >= 0 && ch <= LAST_CHAR_MASK_);
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
            return UCharacterProperty.getDirection(props);
        }
        return UCharacterDirection.BOUNDARY_NEUTRAL;
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
        return UCharacterProperty.isMirrored(props);
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
        if (UCharacterProperty.isMirrored(props)) {
            if(!UCharacterProperty.isExceptionIndicator(props)) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
            else 
            {
                int index = UCharacterProperty.getExceptionIndex(props);
                if (PROPERTY_.hasExceptionValue(index, 
                                    UCharacterProperty.EXC_MIRROR_MAPPING_)) 
                return PROPERTY_.getException(index, 
                                     UCharacterProperty.EXC_MIRROR_MAPPING_);   
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
    	if (ch < MIN_VALUE || ch > MAX_VALUE) {
    		throw new IllegalArgumentException("Codepoint out of bounds");
    	}
    	return NormalizerImpl.getCombiningClass(ch);
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
        if (ch < UTF16.SURROGATE_MIN_VALUE) {
            return true;
        }
        if (ch <= UTF16.SURROGATE_MAX_VALUE) {
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
    public static VersionInfo getUnicodeVersion()
    {
        return PROPERTY_.m_unicodeVersion_;
    }
      
    /**
    * Retrieve the most current Unicode name of the argument code point, or 
    * null if the character is unassigned or outside the range 
    * UCharacter.MIN_VALUE and UCharacter.MAX_VALUE or does not have a name.
    * <br>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param ch the code point for which to get the name
    * @return most current Unicode name
    */
    public static String getName(int ch)
    {
        return NAME_.getName(ch, UCharacterNameChoice.U_UNICODE_CHAR_NAME);
    }
      
    /**
    * Retrieve the earlier version 1.0 Unicode name of the argument code point,
    * or null if the character is unassigned or outside the range 
    * UCharacter.MIN_VALUE and UCharacter.MAX_VALUE or does not have a name.
    * <br>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param ch the code point for which to get the name
    * @return version 1.0 Unicode name
    */
    public static String getName1_0(int ch)
    {
        return NAME_.getName(ch, 
                             UCharacterNameChoice.U_UNICODE_10_CHAR_NAME);
    }
    
    /**
    * <p>Retrieves a name for a valid codepoint. Unlike, getName(int) and
    * getName1_0(int), this method will return a name even for codepoints that
    * are not assigned a name in UnicodeData.txt.
    * </p>
    * The names are returned in the following order.
    * <ul>
    * <li> Most current Unicode name if there is any
    * <li> Unicode 1.0 name if there is any
    * <li> Extended name in the form of "<codepoint_type-codepoint_hex_digits>". 
    *      E.g. <noncharacter-fffe>
    * </ul>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param ch the code point for which to get the name
    * @return a name for the argument codepoint
    * @draft 2.1
    */
    public static String getExtendedName(int ch) 
    {
        return NAME_.getName(ch, UCharacterNameChoice.U_EXTENDED_CHAR_NAME);
    }
      
    /**
    * <p>Find a Unicode code point by its most current Unicode name and 
    * return its code point value. All Unicode names are in uppercase.</p>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param name most current Unicode character name whose code point is to be 
    *        returned
    * @return code point or -1 if name is not found
    */
    public static int getCharFromName(String name)
    {
        return NAME_.getCharFromName(
                            UCharacterNameChoice.U_UNICODE_CHAR_NAME, name);
    }
      
    /**
    * <p>Find a Unicode character by its version 1.0 Unicode name and return 
    * its code point value. All Unicode names are in uppercase.</p>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param name Unicode 1.0 code point name whose code point is to 
    *             returned
    * @return code point or -1 if name is not found
    */
    public static int getCharFromName1_0(String name)
    {
        return NAME_.getCharFromName(
                         UCharacterNameChoice.U_UNICODE_10_CHAR_NAME, name);
    }
    
    /**
    * <p>Find a Unicode character by either its name and return its code 
    * point value. All Unicode names are in uppercase. 
    * Extended names are all lowercase except for numbers and are contained
    * within angle brackets.</p>
    * The names are searched in the following order
    * <ul>
    * <li> Most current Unicode name if there is any
    * <li> Unicode 1.0 name if there is any
    * <li> Extended name in the form of "<codepoint_type-codepoint_hex_digits>". 
    *      E.g. <noncharacter-FFFE>
    * </ul>
    * Note calling any methods related to code point names, e.g. get*Name*() 
    * incurs a one-time initialisation cost to construct the name tables.
    * @param name codepoint name
    * @return code point associated with the name or -1 if the name is not
    *         found.
    * @draft 2.1
    */
    public static int getCharFromExtendedName(String name)
    {
        return NAME_.getCharFromName(
                            UCharacterNameChoice.U_EXTENDED_CHAR_NAME, name);
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
        if (lead >= UTF16.LEAD_SURROGATE_MIN_VALUE && 
	        lead <= UTF16.LEAD_SURROGATE_MAX_VALUE &&
            trail >= UTF16.TRAIL_SURROGATE_MIN_VALUE && 
	        trail <= UTF16.TRAIL_SURROGATE_MAX_VALUE) {
            return UCharacterProperty.getRawSupplementary(lead, trail);
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
    * <p>Gets the titlecase version of the argument string.</p>
    * <p>Position for titlecasing is determined by the argument break 
    * iterator, hence the user can customized his break iterator for 
    * a specialized titlecasing. In this case only the forward iteration 
    * needs to be implemented.
    * If the break iterator passed in is null, the default Unicode algorithm
    * will be used to determine the titlecase positions.
    * </p>
    * <p>Only positions returned by the break iterator will be title cased,
    * character in between the positions will all be in lower case.</p>
    * <p>Casing is dependent on the default locale and context-sensitive</p>
    * @param str source string to be performed on
    * @param breakiter break iterator to determine the positions in which
    *        the character should be title cased.
    * @return lowercase version of the argument string
    * @draft 2.1
    */
    public static String toTitleCase(String str, BreakIterator breakiter)
    {
        return toTitleCase(Locale.getDefault(), str, breakiter);
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
    	if (locale == null) {
    		locale = Locale.getDefault();
    	}
        return PROPERTY_.toUpperCase(locale, str, 0, str.length());
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
    	int length = str.length();
    	StringBuffer result = new StringBuffer(length);
    	if (locale == null) {
    		locale = Locale.getDefault();
    	}
        PROPERTY_.toLowerCase(locale, str, 0, length, result);
        return result.toString();
    }
    
    /**
    * <p>Gets the titlecase version of the argument string.</p>
    * <p>Position for titlecasing is determined by the argument break 
    * iterator, hence the user can customized his break iterator for 
    * a specialized titlecasing. In this case only the forward iteration 
    * needs to be implemented.
    * If the break iterator passed in is null, the default Unicode algorithm
    * will be used to determine the titlecase positions.
    * </p>
    * <p>Only positions returned by the break iterator will be title cased,
    * character in between the positions will all be in lower case.</p>
    * <p>Casing is dependent on the argument locale and context-sensitive</p>
    * @param locale which string is to be converted in
    * @param str source string to be performed on
    * @param breakiter break iterator to determine the positions in which
    *        the character should be title cased.
    * @return lowercase version of the argument string
    * @draft 2.1
    */
    public static String toTitleCase(Locale locale, String str, 
                                     BreakIterator breakiter)
    {
        if (breakiter == null) {
        	if (locale == null) {
        		locale = Locale.getDefault();
        	}
            breakiter = BreakIterator.getWordInstance(locale);
        }
        return PROPERTY_.toTitleCase(locale, str, breakiter);
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
        int props = PROPERTY_.getProperty(ch);
        if (!UCharacterProperty.isExceptionIndicator(props)) {
            int type = UCharacterProperty.getPropType(props);
            if (type == UCharacterCategory.UPPERCASE_LETTER ||
                type == UCharacterCategory.TITLECASE_LETTER) {
                return ch + UCharacterProperty.getSignedValue(props);
            }
        } 
        else {
            int index = UCharacterProperty.getExceptionIndex(props);
            if (PROPERTY_.hasExceptionValue(index, 
                                      UCharacterProperty.EXC_CASE_FOLDING_)) {
                int exception = PROPERTY_.getException(index, 
                                      UCharacterProperty.EXC_CASE_FOLDING_);
                if (exception != 0) {
                    int foldedcasech = 
                         PROPERTY_.getFoldCase(exception & LAST_CHAR_MASK_);
                    if (foldedcasech != 0){
                        return foldedcasech;
                    }
                }
                else {
                    // special case folding mappings, hardcoded
                    if (defaultmapping && 
                        (ch == 
                           UCharacterProperty.LATIN_SMALL_LETTER_DOTLESS_I_ || 
                         ch == 
                    UCharacterProperty.LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_)) 
                    {
                        // map dotted I and dotless i to U+0069 small i
                        return UCharacterProperty.LATIN_SMALL_LETTER_I_;
                    }
                    // return ch itself because it is excluded from case folding
                    return ch;
                }                                  
            }
            if (PROPERTY_.hasExceptionValue(index, 
                                       UCharacterProperty.EXC_LOWERCASE_)) {  
                // not else! - allow to fall through from above
                return PROPERTY_.getException(index, 
                                         UCharacterProperty.EXC_LOWERCASE_);
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
            int props = PROPERTY_.getProperty(ch);
            if (!UCharacterProperty.isExceptionIndicator(props)) {
                int type = UCharacterProperty.getPropType(props);
                if (type == UCharacterCategory.UPPERCASE_LETTER ||
                    type == UCharacterCategory.TITLECASE_LETTER) {
                    ch += UCharacterProperty.getSignedValue(props);
                }
            }  
            else {
                int index = UCharacterProperty.getExceptionIndex(props);
                if (PROPERTY_.hasExceptionValue(index, 
                                    UCharacterProperty.EXC_CASE_FOLDING_)) {
                    int exception = PROPERTY_.getException(index, 
                                      UCharacterProperty.EXC_CASE_FOLDING_);                             
                    if (exception != 0) {
                        PROPERTY_.getFoldCase(exception & LAST_CHAR_MASK_, 
                                             exception >> SHIFT_24_, result);
                    } 
                    else {
                        // special case folding mappings, hardcoded
                        if (defaultmapping && 
                            (ch == 
                            UCharacterProperty.LATIN_SMALL_LETTER_DOTLESS_I_ || 
                             ch == 
                    UCharacterProperty.LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_)) 
                        {
                            // map dotted I and dotless i to U+0069 small i
                            result.append(
                                    UCharacterProperty.LATIN_SMALL_LETTER_I_);
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
                    if (PROPERTY_.hasExceptionValue(index, 
                                         UCharacterProperty.EXC_LOWERCASE_)) {
                        ch = PROPERTY_.getException(index, 
                                          UCharacterProperty.EXC_LOWERCASE_);
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
    
    /**
    * <p>Gets an iterator for character types, iterating over codepoints.</p>
    * Example of use:<br>
    * <pre>
    * RangeValueIterator iterator = UCharacter.getTypeIterator();
    * RangeValueIterator.Element element = new RangeValueIterator.Element();
    * while (iterator.next(element)) {
    *     System.out.println("Codepoint \\u" + 
    *                        Integer.toHexString(element.start) + 
    *                        " to codepoint \\u" +
    *                        Integer.toHexString(element.limit - 1) + 
    *                        " has the character type " + 
    *                        element.value);
    * }
    * </pre>
    * @return an iterator 
    * @draft 2.1
    */
    public static RangeValueIterator getTypeIterator()
    {
        return new UCharacterTypeIterator(PROPERTY_);
    }

	/**
    * <p>Gets an iterator for character names, iterating over codepoints.</p>
    * <p>This API only gets the iterator for the modern, most up-to-date 
    * Unicode names. For older 1.0 Unicode names use get1_0NameIterator() or
    * for extended names use getExtendedNameIterator().</p>
    * Example of use:<br>
    * <pre>
    * ValueIterator iterator = UCharacter.getNameIterator();
    * ValueIterator.Element element = new ValueIterator.Element();
    * while (iterator.next(element)) {
    *     System.out.println("Codepoint \\u" + 
    *                        Integer.toHexString(element.codepoint) +
    *                        " has the name " + (String)element.value);
    * }
    * </pre>
    * <p>The maximal range which the name iterator iterates is from 
    * UCharacter.MIN_VALUE to UCharacter.MAX_VALUE.</p>
    * @return an iterator 
    * @draft 2.1
    */
    public static ValueIterator getNameIterator()
    {
        return new UCharacterNameIterator(NAME_,
                                   UCharacterNameChoice.U_UNICODE_CHAR_NAME);
    }
    
    /**
    * <p>Gets an iterator for character names, iterating over codepoints.</p>
    * <p>This API only gets the iterator for the older 1.0 Unicode names. 
    * For modern, most up-to-date Unicode names use getNameIterator() or
    * for extended names use getExtendedNameIterator().</p>
    * Example of use:<br>
    * <pre>
    * ValueIterator iterator = UCharacter.get1_0NameIterator();
    * ValueIterator.Element element = new ValueIterator.Element();
    * while (iterator.next(element)) {
    *     System.out.println("Codepoint \\u" + 
    *                        Integer.toHexString(element.codepoint) +
    *                        " has the name " + (String)element.value);
    * }
    * </pre>
    * @return an iterator 
    * @draft 2.1
    */
    public static ValueIterator getName1_0Iterator()
    {
        return new UCharacterNameIterator(NAME_,
                                 UCharacterNameChoice.U_UNICODE_10_CHAR_NAME);
    }
    
    /**
    * <p>Gets an iterator for character names, iterating over codepoints.</p>
    * <p>This API only gets the iterator for the extended names. 
    * For modern, most up-to-date Unicode names use getNameIterator() or
    * for older 1.0 Unicode names use get1_0NameIterator().</p>
    * Example of use:<br>
    * <pre>
    * ValueIterator iterator = UCharacter.getExtendedNameIterator();
    * ValueIterator.Element element = new ValueIterator.Element();
    * while (iterator.next(element)) {
    *     System.out.println("Codepoint \\u" + 
    *                        Integer.toHexString(element.codepoint) +
    *                        " has the name " + (String)element.value);
    * }
    * </pre>
    * @return an iterator 
    * @draft 2.1
    */
    public static ValueIterator getExtendedNameIterator()
    {
        return new UCharacterNameIterator(NAME_,
                                 UCharacterNameChoice.U_EXTENDED_CHAR_NAME);
    }
    
    /**
     * <p>Get the "age" of the code point.</p>
     * <p>The "age" is the Unicode version when the code point was first
     * designated (as a non-character or for Private Use) or assigned a 
     * character.</p>
     * <p>This can be useful to avoid emitting code points to receiving 
     * processes that do not accept newer characters.</p>
     * <p>The data is from the UCD file DerivedAge.txt.</p>
     * @param ch The code point.
     * @return the Unicode version number
     * @draft ICU 2.1
     */
    public static VersionInfo getAge(int ch) 
    {
    	if (ch < MIN_VALUE || ch > MAX_VALUE) {
    		throw new IllegalArgumentException("Codepoint out of bounds");
    	}
    	return PROPERTY_.getAge(ch);
    }
    
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
	 * @draft ICU 2.1
	 */
	public static boolean hasBinaryProperty(int ch, int property) 
	{
		if (ch < MIN_VALUE || ch > MAX_VALUE) {
    		throw new IllegalArgumentException("Codepoint out of bounds");
    	}
    	return PROPERTY_.hasBinaryProperty(ch, property);
	}
	
	/**
	 * <p>Check if a code point has the Alphabetic Unicode property.</p> 
	 * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.ALPHABETIC).</p>
	 * <p>Different from UCharacter.isLetter(ch)!</p> 
	 * @draft ICU 2.1
	 * @param ch codepoint to be tested
	 */
	public static boolean isUAlphabetic(int ch)
	{
		return hasBinaryProperty(ch, UProperty.ALPHABETIC);
	}

	/**
	 * <p>Check if a code point has the Lowercase Unicode property.</p>
	 * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.LOWERCASE).</p>
	 * <p>This is different from UCharacter.isLowerCase(ch)!</p>
	 * @param ch codepoint to be tested
	 * @draft ICU 2.1
	 */
	public static boolean isULowercase(int ch) 
	{
		return hasBinaryProperty(ch, UProperty.LOWERCASE);
	}

	/**
	 * <p>Check if a code point has the Uppercase Unicode property.</p>
	 * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.UPPERCASE).</p>
	 * <p>This is different from UCharacter.isUpperCase(ch)!</p>
	 * @param ch codepoint to be tested
	 * @draft ICU 2.1
	 */
	public static boolean isUUppercase(int ch) 
	{
		return hasBinaryProperty(ch, UProperty.UPPERCASE);
	}

	/**
	 * <p>Check if a code point has the White_Space Unicode property.</p>
	 * <p>Same as UCharacter.hasBinaryProperty(ch, UProperty.WHITE_SPACE).</p>
	 * <p>This is different from both UCharacter.isSpace(ch) and 
	 * UCharacter.isWhiteSpace(ch)!</p>
	 * @param ch codepoint to be tested
	 * @draft ICU 2.1
	 */
	public static boolean isUWhiteSpace(int ch) 
	{
		return hasBinaryProperty(ch, UProperty.WHITE_SPACE);
	}

    // protected data members --------------------------------------------
    
    /**
    * Database storing the sets of character name
    */
    protected static final UCharacterName NAME_;
      
    // block to initialise name database and unicode 1.0 data 
    static
    {
        try
        {
            NAME_ = new UCharacterName();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    // protected methods -------------------------------------------------
      
    /**
    * Determines if codepoint is a non character
    * @param ch codepoint
    * @return true if codepoint is a non character false otherwise
    */
    static boolean isNonCharacter(int ch) 
    {
        if ((ch & NON_CHARACTER_SUFFIX_MIN_3_0_) == 
                                            NON_CHARACTER_SUFFIX_MIN_3_0_) {
            return true;
        }
        
        return ch >= NON_CHARACTER_MIN_3_1_ && ch <=  NON_CHARACTER_MAX_3_1_;
    }
        
    // private variables -------------------------------------------------
    
    /**
    * Database storing the sets of character property
    */
    private static final UCharacterProperty PROPERTY_;
                                                    
	// block to initialise character property database
    static
    {
        try
        {
            PROPERTY_ = UCharacterProperty.getInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }                                                    
   
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
    private static final int NON_CHARACTER_MAX_3_1_ = 0xFDEF;
      
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
                           
    // private constructor -----------------------------------------------
      
    /**
    * Private constructor to prevent instantiation
    */
    private UCharacter()
    {
    }
      
    // private methods ---------------------------------------------------
      
    /**
    * Gets the correct property information from UCharacterProperty
    * @param ch character whose information is to be retrieved
    * @return a 32 bit information, returns 0 if no data is found.
    */
    private static int getProps(int ch)
    {
        if (ch >= MIN_VALUE & ch <= MAX_VALUE) {
            return PROPERTY_.getProperty(ch);
        }
        return 0;
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
    
    private static int getNumericValueInternal(int ch, boolean useEuropean)
    {
        int props       = getProps(ch);
        int numericType = UCharacterProperty.getNumericType(props);
        
        int result = -1;
        if (numericType == UCharacterProperty.GENERAL_NUMERIC_TYPE_) {
        	// if props == 0, it will just fall through and return -1
        	if (!UCharacterProperty.isExceptionIndicator(props)) {
            	// not contained in exception data
            	result = UCharacterProperty.getSignedValue(props);
            }
            else {
            	int index = UCharacterProperty.getExceptionIndex(props);
            	if (PROPERTY_.hasExceptionValue(index, 
                               UCharacterProperty.EXC_DENOMINATOR_VALUE_)) {
                    return -2;
                }
            	if (PROPERTY_.hasExceptionValue(index, 
                                   UCharacterProperty.EXC_NUMERIC_VALUE_)) {
                	result = PROPERTY_.getException(index, 
                                      UCharacterProperty.EXC_NUMERIC_VALUE_); 
                }
            }
        }
        
        if (result < 0 && useEuropean) {
            result = getEuropeanDigit(ch);
        }
        
        return result;
    }
}

