/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UTF16.java,v $ 
* $Date: 2001/11/06 00:13:23 $ 
* $Revision: 1.10 $
*
*******************************************************************************
*/

package com.ibm.text;

import com.ibm.text.UCharacter;

/**
* Standalone utility class providing UTF16 character conversions and indexing 
* conversions.
* <p>Code that uses strings alone rarely need modification. 
* By design, UTF-16 does not allow overlap, so searching for strings is a safe 
* operation. Similarly, concatenation is always safe. Substringing is safe if 
* the start and end are both on UTF-32 boundaries. In normal code, the values 
* for start and end are on those boundaries, since they arose from operations 
* like searching. If not, the nearest UTF-32 boundaries can be determined 
* using <code>bounds()</code>.
* <strong>Examples:</strong>
* <p>The following examples illustrate use of some of these methods. 
* <pre>
* // iteration forwards: Original
* for (int i = 0; i < s.length(); ++i) {
*   char ch = s.charAt(i);
*   doSomethingWith(ch);
* }
*
* // iteration forwards: Changes for UTF-32
* int ch;
* for (int i = 0; i < s.length(); i+=UTF16.getCharCount(ch)) {
*   ch = UTF16.charAt(s,i);
*   doSomethingWith(ch);
* }
*
* // iteration backwards: Original
* for (int i = s.length()-1; i >= 0; --i) {
*   char ch = s.charAt(i);
*   doSomethingWith(ch);
* }
*  
* // iteration backwards: Changes for UTF-32
* int ch;
* for (int i = s.length()-1; i > 0; i-=UTF16.getCharCount(ch)) {
*   ch = UTF16.charAt(s,i);
*   doSomethingWith(ch);
* }
* </pre>
* <strong>Notes:</strong>
* <ul>
*   <li>
*   <strong>Naming:</strong> For clarity, High and Low surrogates are called 
*   <code>Lead</code> and <code>Trail</code> in the API, which gives a better 
*   sense of their ordering in a string. <code>offset16</code> and 
*   <code>offset32</code> are used to distinguish offsets to UTF-16 
*   boundaries vs offsets to UTF-32 boundaries. <code>int char32</code> is 
*   used to contain UTF-32 characters, as opposed to <code>char16</code>, 
*   which is a UTF-16 code unit.
*   </li>
*   <li>
*   <strong>Roundtripping Offsets:</strong> You can always roundtrip from a 
*   UTF-32 offset to a UTF-16 offset and back. Because of the difference in 
*   structure, you can roundtrip from a UTF-16 offset to a UTF-32 offset and 
*   back if and only if <code>bounds(string, offset16) != TRAIL</code>.
*   </li>
*   <li>
*   <strong>Exceptions:</strong> The error checking will throw an exception 
*   if indices are out of bounds. Other than than that, all methods will 
*   behave reasonably, even if unmatched surrogates or out-of-bounds UTF-32 
*   values are present. <code>UCharacter.isLegal()</code> can be used to check 
*   for validity if desired.
*   </li>
*   <li>
*   <strong>Unmatched Surrogates:</strong> If the string contains unmatched 
*   surrogates, then these are counted as one UTF-32 value. This matches 
*   their iteration behavior, which is vital. It also matches common display 
*   practice as missing glyphs (see the Unicode Standard Section 5.4, 5.5).
*   </li>
*   <li>
*     <strong>Optimization:</strong> The method implementations may need 
*     optimization if the compiler doesn't fold static final methods. Since 
*     surrogate pairs will form an exceeding small percentage of all the text 
*     in the world, the singleton case should always be optimized for.
*   </li>
* </ul>
* @author Mark Davis, with help from Markus Scherer
* @since Nov2400
*/

public final class UTF16
{
  // public variables =============================================
  
  /**
  * Value returned in <code><a href="#bounds(java.lang.String, int)">
  * bounds()</a></code>.
  * These values are chosen specifically so that it actually represents the 
  * position of the character 
  * [offset16 - (value >> 2), offset16 + (value & 3)]
  */
  public static final int SINGLE_CHAR_BOUNDARY = 1, 
                          LEAD_SURROGATE_BOUNDARY = 2, 
                          TRAIL_SURROGATE_BOUNDARY = 5;
                          
  // private variables ============================================
	
  /** 
	* Lead surrogates values from LEAD_SURROGATE_MIN_VALUE_ to LEAD_SURROGATE_MAX_VALUE_
	*/
	private static final int LEAD_SURROGATE_MIN_VALUE_ = 0xD800;
	private static final int LEAD_SURROGATE_MAX_VALUE_ = 0xDBFF;
	
	/**
	* Surrogate lead offset, to be used when breaking up UTF32 into surrogate
	* pair
	*/
	private static final int LEAD_SURROGATE_OFFSET_ = LEAD_SURROGATE_MIN_VALUE_ - 
	    (UCharacter.SUPPLEMENTARY_MIN_VALUE >> UCharacter.LEAD_SURROGATE_SHIFT_);
	
	/** 
	* Trail surrogates values from TRAIL_SURROGATE_MIN_VALUE_ to 
	* TRAIL_SURROGATE_MAX_VALUE_
	*/
	private static final int TRAIL_SURROGATE_MIN_VALUE_ = 0xDC00; 
	private static final int TRAIL_SURROGATE_MAX_VALUE_ = 0xDFFF;
                          
  // constructor ==================================================
  
  /**
  * Prevent instance from being created.
  */
  private UTF16() 
  {
  }

  // public method ================================================
  
  /**
  * Extract a single UTF-32 value from a string.
  * Used when iterating forwards or backwards (with 
  * <code>UTF16.getCharCount()</code>, as well as random access. If a validity 
  * check is required, use <code><a href="../UCharacter.html#isLegal(char)">
  * UCharacter.isLegal()</a></code> on the return value.
  * If the char retrieved is part of a surrogate pair, its supplementary 
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @param source array of UTF-16 chars
  * @param offset16 UTF-16 offset to the start of the character.
  * @return UTF-32 value for the UTF-32 value that contains the char at 
  *         offset16. The boundaries of that codepoint are the same as in 
  *         <code>bounds32()</code>. 
  * @exception IndexOutOfBoundsException thrown if offset16 is out of bounds.
  */
  public static int charAt(String source, int offset16) 
  {             
      if (offset16 < 0 || offset16 >= source.length()) {
        throw new StringIndexOutOfBoundsException(offset16);
      }
      
      char single = source.charAt(offset16);
      if (!isSurrogate(single)) {
        return single;
      }

      // Convert the UTF-16 surrogate pair if necessary.
      // For simplicity in usage, and because the frequency of pairs is low,
      // look both directions.
            
	  if (isLeadSurrogate(single)) 
	  {
	    ++ offset16;
	    if (source.length() != offset16)
	    {
	      char trail = source.charAt(offset16);
	      if (isTrailSurrogate(trail))
	        return UCharacter.getRawSupplementary(single, trail);
	    }
	  } 
	  else 
	  { 
	    -- offset16;
	    if (offset16 >= 0)
	    {
	      // single is a trail surrogate so
	      char lead = source.charAt(offset16);
	      if (isLeadSurrogate(lead))
	        return UCharacter.getRawSupplementary(lead, single);
	    }
	  } 
	  return single; // return unmatched surrogate
  }
  
  /**
  * Extract a single UTF-32 value from a string.
  * Used when iterating forwards or backwards (with
  * <code>UTF16.getCharCount()</code>, as well as random access. If a validity 
  * check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">UCharacter.isLegal()
  * </a></code> on the return value.
  * If the char retrieved is part of a surrogate pair, its supplementary
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @param source UTF-16 chars string buffer
  * @param offset16 UTF-16 offset to the start of the character.
  * @return UTF-32 value for the UTF-32 value that contains the char at
  *         offset16. The boundaries of that codepoint are the same as in
  *         <code>bounds32()</code>.
  * @exception IndexOutOfBoundsException thrown if offset16 is out of bounds.
  */
  public static int charAt(StringBuffer source, int offset16)
  {
      if (offset16 < 0 || offset16 >= source.length()) {
        throw new StringIndexOutOfBoundsException(offset16);
      }
      
      char single = source.charAt(offset16);
      if (!isSurrogate(single)) {
        return single;
      }

      // Convert the UTF-16 surrogate pair if necessary.
      // For simplicity in usage, and because the frequency of pairs is low,
      // look both directions.
            
	  if (isLeadSurrogate(single)) 
	  {
	    ++ offset16;
	    if (source.length() != offset16)
	    {
	      char trail = source.charAt(offset16);
	      if (isTrailSurrogate(trail))
	        return UCharacter.getRawSupplementary(single, trail);
	    }
	  } 
	  else 
	  { 
	    -- offset16;
	    if (offset16 >= 0)
	    {
	      // single is a trail surrogate so
	      char lead = source.charAt(offset16);
	      if (isLeadSurrogate(lead)) {
	          return UCharacter.getRawSupplementary(lead, single);
	      }
	    }
	  } 
	  return single; // return unmatched surrogate
  }
  
  /**
  * Extract a single UTF-32 value from a substring.
  * Used when iterating forwards or backwards (with
  * <code>UTF16.getCharCount()</code>, as well as random access. If a
  * validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">UCharacter.isLegal()
  * </a></code> on the return value.
  * If the char retrieved is part of a surrogate pair, its supplementary
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @param source array of UTF-16 chars
  * @param start offset to substring in the source array for analyzing
  * @param limit offset to substring in the source array for analyzing
  * @param offset16 UTF-16 offset relative to start
  * @return UTF-32 value for the UTF-32 value that contains the char at
  *         offset16. The boundaries of that codepoint are the same as in
  *         <code>bounds32()</code>.
  * @exception IndexOutOfBoundsException thrown if offset16 is not within the 
  *            range of start and limit.
  */
  public static int charAt(char source[], int start, int limit, int offset16)
  {
      offset16 += start;
      if (offset16 < start || offset16 >= limit) {
          throw new ArrayIndexOutOfBoundsException(offset16);
      }
        
      char single = source[offset16];
      if (!isSurrogate(single)) {
          return single;
      }

      // Convert the UTF-16 surrogate pair if necessary.
      // For simplicity in usage, and because the frequency of pairs is low,
      // look both directions.      
	  if (isLeadSurrogate(single)) {
	      offset16 ++;
	      if (offset16 >= limit) {
	          return single;
	      }
	      char trail = source[offset16];
	      if (isTrailSurrogate(trail)) {
	          return UCharacter.getRawSupplementary(single, trail);
	      }
      } 
      else { // isTrailSurrogate(single), so
          if (offset16 == start) {
              return single;
          }
          offset16 --;
	      char lead = source[offset16];
	      if (isLeadSurrogate(lead))
	        return UCharacter.getRawSupplementary(lead, single);
      }
      return single; // return unmatched surrogate
  }
  
  /**
  * Extract a single UTF-32 value from a string.
  * Used when iterating forwards or backwards (with
  * <code>UTF16.getCharCount()</code>, as well as random access. If a validity 
  * check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">UCharacter.isLegal()
  * </a></code> on the return value.
  * If the char retrieved is part of a surrogate pair, its supplementary
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @param source UTF-16 chars string buffer
  * @param offset16 UTF-16 offset to the start of the character.
  * @return UTF-32 value for the UTF-32 value that contains the char at
  *         offset16. The boundaries of that codepoint are the same as in
  *         <code>bounds32()</code>.
  * @exception IndexOutOfBoundsException thrown if offset16 is out of bounds.
  */
  public static int charAt(Replaceable source, int offset16)
  {
      if (offset16 < 0 || offset16 >= source.length()) {
        throw new StringIndexOutOfBoundsException(offset16);
      }
      
      char single = source.charAt(offset16);
      if (!isSurrogate(single)) {
        return single;
      }

      // Convert the UTF-16 surrogate pair if necessary.
      // For simplicity in usage, and because the frequency of pairs is low,
      // look both directions.
            
	  if (isLeadSurrogate(single)) 
	  {
	    ++ offset16;
	    if (source.length() != offset16)
	    {
	      char trail = source.charAt(offset16);
	      if (isTrailSurrogate(trail))
	        return UCharacter.getRawSupplementary(single, trail);
	    }
	  } 
	  else 
	  { 
	    -- offset16;
	    if (offset16 >= 0)
	    {
	      // single is a trail surrogate so
	      char lead = source.charAt(offset16);
	      if (isLeadSurrogate(lead)) {
	          return UCharacter.getRawSupplementary(lead, single);
	      }
	    }
	  } 
	  return single; // return unmatched surrogate
  }
  
  /**
  * Extract a single UTF-32 value from a string.
  * If a validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">
  * UCharacter.isLegal()</a></code> on the return value.
  * If tbe char retrieved is part of a surrogate pair, its supplementary 
  * character will be returned. If a complete supplementary character is not
  * found the incomplete character will be returned
  * @return UTF-32 value for the UTF-32 value that contains the char at 
  *         offset16. The boundaries of that codepoint are the same as in 
  *         <code>bounds32()</code>. 
  * @param source array of UTF-16 chars
  * @param offset32 UTF-32 offset to the start of the character.
  * @return a single UTF32 value
  * @exception IndexOutOfBoundsException if offset16 is out of bounds.
  * @deprecated to be removed after the year 2002, replaced by 
  *      TF16.charAt(source, UTF16.findOffsetFromCodePoint(source, offset32));
  */
  public static int charAtCodePointOffset(String source, int offset32) 
  {
    return charAt(source, findOffsetFromCodePoint(source, offset32));
  }
  
  /**
  * Determines how many chars this char32 requires.
  * If a validity check is required, use <code>
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param ch the input character.
  * @return 2 if is in surrogate space, otherwise 1. 
  */
  public static int getCharCount(int char32) 
  {
    if (char32 < UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return 1;
    }
    return 2;
  }
  
  /**
  * Returns the type of the boundaries around the char at offset16.
  * Used for random access.
  * @param source text to analyse
  * @param offset16 UTF-16 offset
  * @return <ul>
  *           <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are 
  *                                       [offset16, offset16+1]
  *           <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                                          offset16; 
  *                                          the bounds are 
  *                                          [offset16, offset16 + 2]
  *           <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                                           offset16 - 1; the bounds are 
  *                                           [offset16 - 1, offset16 + 1]
  *         </ul>
  *         For bit-twiddlers, the return values for these are chosen so that 
  *         the boundaries can be gotten by:
  *         [offset16 - (value >> 2), offset16 + (value & 3)].
  * @exception IndexOutOfBoundsException if offset16 is out of bounds.
  */
  public static int bounds(String source, int offset16) 
  {
      char ch = source.charAt(offset16);
      if (isSurrogate(ch)) {
          if (isLeadSurrogate(ch)) 
          {
              if (++ offset16 < source.length() && 
                  isTrailSurrogate(source.charAt(offset16))) {
                  return LEAD_SURROGATE_BOUNDARY;
              }
          } 
          else {
              // isTrailSurrogate(ch), so
              -- offset16;
              if (offset16 >= 0 && isLeadSurrogate(source.charAt(offset16))) {
                  return TRAIL_SURROGATE_BOUNDARY;
              }
          }
      }
      return SINGLE_CHAR_BOUNDARY;
  }
  
  /**
  * Returns the type of the boundaries around the char at offset16. Used for
  * random access.
  * @param source string buffer to analyse
  * @param offset16 UTF16 offset
  * @return
  *     <ul>
  *     <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are
  *                                                  [offset16, offset16 + 1]
  *     <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at offset16; 
  *                                   the bounds are [offset16, offset16 + 2]
  *     <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at 
  *                  offset16 - 1; the bounds are [offset16 - 1, offset16 + 1]
  *     </ul>
  * For bit-twiddlers, the return values for these are chosen so that the 
  * boundaries can be gotten by: 
  *                        [offset16 - (value >> 2), offset16 + (value & 3)].
  * @exception IndexOutOfBoundsException if offset16 is out of bounds.
  */
  public static int bounds(StringBuffer source, int offset16)
  {
      char ch = source.charAt(offset16);
      if (isSurrogate(ch)) {
          if (isLeadSurrogate(ch)) 
          {
              if (++ offset16 < source.length() && 
                  isTrailSurrogate(source.charAt(offset16))) {
                  return LEAD_SURROGATE_BOUNDARY;
              }
          } 
          else {
              // isTrailSurrogate(ch), so
              -- offset16;
              if (offset16 >= 0 && isLeadSurrogate(source.charAt(offset16))) {
                  return TRAIL_SURROGATE_BOUNDARY;
              }
          }
      }
      return SINGLE_CHAR_BOUNDARY;
  }

  /**
  * Returns the type of the boundaries around the char at offset16. Used for
  * random access. Note that the boundaries are determined with respect to 
  * the subarray, hence the char array {0xD800, 0xDC00} has the result 
  * SINGLE_CHAR_BOUNDARY for start = offset16 = 0 and limit = 1.
  * @param source char array to analyse
  * @param start offset to substring in the source array for analyzing
  * @param limit offset to substring in the source array for analyzing
  * @param offset16 UTF16 offset relative to start
  * @return
  *     <ul>
  *         <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are
  *         <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at
  *                          offset16; the bounds are [offset16, offset16 + 2]
  *         <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at
  *                  offset16 - 1; the bounds are [offset16 - 1, offset16 + 1]
  *     </ul>
  * For bit-twiddlers, the boundary values for these are chosen so that the 
  * boundaries can be gotten by: [offset16 - (boundvalue >> 2), offset16 
  *                                                       + (boundvalue & 3)].
  * @exception IndexOutOfBoundsException if offset16 is not within the range 
  *            of start and limit.
  */
  public static int bounds(char source[], int start, int limit, int offset16)
  {
      offset16 += start;
      if (offset16 < start || offset16 >= limit) {
          throw new ArrayIndexOutOfBoundsException(offset16);
      }
      char ch = source[offset16];
      if (isSurrogate(ch)) {
          if (isLeadSurrogate(ch)) {
              ++ offset16;
              if (offset16 < limit && isTrailSurrogate(source[offset16])) {
                  return LEAD_SURROGATE_BOUNDARY;
              }
          } 
          else { // isTrailSurrogate(ch), so
              -- offset16;
              if (offset16 >= start && isLeadSurrogate(source[offset16])) {
                  return TRAIL_SURROGATE_BOUNDARY;
              }
          }
      }
      return SINGLE_CHAR_BOUNDARY;    
  }

  /**
  * Returns the type of the boundaries around the char at offset32. Used for
  * random access.
  * @param source string to analyse
  * @param offset32 UTF32 offset
  * @return
  *     <ul>
  *         <li> SINGLE_CHAR_BOUNDARY : a single char
  *         <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at
  *                                        offset32
  *     </ul>
  * For bit-twiddlers, see <a href=#bounds(java.lang.String, int)>
  * bounds(java.lang.String, int)</a> for information on the choice of the 
  * boundary values.
  * @exception IndexOutOfBoundsException if offset16 is out of bounds.
  * @deprecated will be removed after end of year 2002, replaced by
  *     UTF16.bounds(source, UTF16.findOffsetFromCodePoint(source, offset32));
  */
  public static int boundsAtCodePointOffset(String source, int offset32) 
  {
    return bounds(source, findOffsetFromCodePoint(source, offset32));
  }

  /**
  * Determines whether the <b>code value is a surrogate.
  * @param ch the input character.
  * @return true iff the input character is a surrogate.
  */
  public static boolean isSurrogate(char char16) 
  {
    return LEAD_SURROGATE_MIN_VALUE_ <= char16 && 
           char16 <= TRAIL_SURROGATE_MAX_VALUE_;
  }
    
  /**
  * Determines whether the character is a trail surrogate.
  * @param char16 the input character.
  * @return true iff the input character is a trail surrogate.
  */
  public static boolean isTrailSurrogate(char char16) 
  {
    return (TRAIL_SURROGATE_MIN_VALUE_ <= char16 && 
            char16 <= TRAIL_SURROGATE_MAX_VALUE_);
  }
    
  /**
  * Determines whether the character is a lead surrogate.
  * @param char16 the input character.
  * @return true iff the input character is a lead surrogate
  */
  public static boolean isLeadSurrogate(char char16) 
  {
    return LEAD_SURROGATE_MIN_VALUE_ <= char16 && 
           char16 <= LEAD_SURROGATE_MAX_VALUE_;
  }
        
  /**
  * Returns the lead surrogate.
  * If a validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 the input character.
  * @return lead surrogate if the getCharCount(ch) is 2; <br>
  *         and 0 otherwise (note: 0 is not a valid lead surrogate).
  */
  public static char getLeadSurrogate(int char32) 
  {
    if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return (char)(LEAD_SURROGATE_OFFSET_ + 
                   (char32 >> UCharacter.LEAD_SURROGATE_SHIFT_));
    }
        
    return 0;
  }
    
  /**
  * Returns the trail surrogate.
  * If a validity check is required, use 
  * <code><a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 the input character.
  * @return the trail surrogate if the getCharCount(ch) is 2; <br>otherwise the 
  *         character itself
  */
  public static char getTrailSurrogate(int char32) 
  {
    if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return (char)(TRAIL_SURROGATE_MIN_VALUE_ + (char32 & 
                    UCharacter.TRAIL_SURROGATE_MASK_));       
    }
      
    return (char)char32;
  }
    
  /**
  * Convenience method corresponding to String.valueOf(char). Returns a one 
  * or two char string containing the UTF-32 value in UTF16 format. If a 
  * validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 the input character.
  * @return string value of char32 in UTF16 format
  * @exception IllegalArgumentException thrown if char32 is a invalid 
  *            codepoint.
  */
  public static String valueOf(int char32)
  {
    if (char32 < UCharacter.MIN_VALUE || char32 > UCharacter.MAX_VALUE) {
      throw new IllegalArgumentException("Illegal codepoint");
    }
    if (char32 < UCharacter.SUPPLEMENTARY_MIN_VALUE) {
      return String.valueOf((char)char32);
    }
    char str[] = new char[2];   
    str[0] = (char)(LEAD_SURROGATE_OFFSET_ + 
                    (char32 >> UCharacter.LEAD_SURROGATE_SHIFT_));
    str[1] = (char)(TRAIL_SURROGATE_MIN_VALUE_ + 
                    (char32 & UCharacter.TRAIL_SURROGATE_MASK_));
    return String.valueOf(str);
  }
  
  /**
  * Convenience method corresponding to String.valueOf(codepoint at offset16). 
  * Returns a one or two char string containing the UTF-32 value in UTF16 
  * format. If offset16 indexes a surrogate character, the whole supplementary
  * codepoint will be returned.
  * If a validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on the 
  * codepoint at offset16 before calling.
  * The result returned will be a newly created String obtained by calling 
  * source.substring(..) with the appropriate indexes.
  * @param source the input string.
  * @param offset16 the UTF16 index to the codepoint in source
  * @return string value of char32 in UTF16 format
  */
  public static String valueOf(String source, int offset16)
  {
      switch (bounds(source, offset16)) {
          case LEAD_SURROGATE_BOUNDARY: 
                              return source.substring(offset16, offset16 + 2);
          case TRAIL_SURROGATE_BOUNDARY: 
                          return source.substring(offset16 - 1, offset16 + 1);
          default: return source.substring(offset16, offset16 + 1);
      }
  }
  
  /**
  * Convenience method corresponding to 
  * StringBuffer.valueOf(codepoint at offset16). 
  * Returns a one or two char string containing the UTF-32 value in UTF16 
  * format. If offset16 indexes a surrogate character, the whole supplementary
  * codepoint will be returned.
  * If a validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on the 
  * codepoint at offset16 before calling.
  * The result returned will be a newly created String obtained by calling 
  * source.substring(..) with the appropriate indexes.
  * @param source the input string buffer.
  * @param offset16 the UTF16 index to the codepoint in source
  * @return string value of char32 in UTF16 format
  */
  public static String valueOf(StringBuffer source, int offset16)
  {
      switch (bounds(source, offset16)) {
          case LEAD_SURROGATE_BOUNDARY: 
                              return source.substring(offset16, offset16 + 2);
          case TRAIL_SURROGATE_BOUNDARY: 
                          return source.substring(offset16 - 1, offset16 + 1);
          default: return source.substring(offset16, offset16 + 1);
      }
  }
  
  /**
  * Convenience method. 
  * Returns a one or two char string containing the UTF-32 value in UTF16 
  * format. If offset16 indexes a surrogate character, the whole supplementary
  * codepoint will be returned, except when either the leading or trailing 
  * surrogate character lies out of the specified subarray. In the latter case,
  * only the surrogate character within bounds will be returned.
  * If a validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on the 
  * codepoint at offset16 before calling.
  * The result returned will be a newly created String containing the relevant
  * characters.
  * @param source the input char array.
  * @param start start index of the subarray
  * @param limit end index of the subarray
  * @param offset16 the UTF16 index to the codepoint in source relative to 
  *        start
  * @return string value of char32 in UTF16 format
  */
  public static String valueOf(char source[], int start, int limit,
                               int offset16)
  {
      switch (bounds(source, start, limit, offset16)) {
          case LEAD_SURROGATE_BOUNDARY: 
               return new String(source, start + offset16, 2);
          case TRAIL_SURROGATE_BOUNDARY: 
               return new String(source, start + offset16 - 1, 2);
      }
      return new String(source, start + offset16, 1);
  }
  
  /**
  * Returns the UTF-16 offset that corresponds to a UTF-32 offset. 
  * Used for random access. See the <a name="_top_">class description</a> for 
  * notes on roundtripping.
  * @param source the UTF-16 string
  * @param offset32 UTF-32 offset
  * @return UTF-16 offset 
  * @exception IndexOutOfBoundsException if offset32 is out of bounds.
  */
  public static int findOffsetFromCodePoint(String source, int offset32) 
  {
    char ch;
    int size = source.length(),
        result = 0,
        count = offset32;
    if (offset32 < 0 || offset32 > size) {
      throw new StringIndexOutOfBoundsException(offset32);
    }
    while (result < size && count > 0)
    {
      ch = source.charAt(result);
      if (isLeadSurrogate(ch) && ((result + 1) < size) && 
          isTrailSurrogate(source.charAt(result + 1))) {
        result ++;
      }
        
      count --;
      result ++;
    }
    if (count != 0) {
      throw new StringIndexOutOfBoundsException(offset32);
    }
    return result;
  }
  
  /**
  * Returns the UTF-16 offset that corresponds to a UTF-32 offset.
  * Used for random access. See the <a name="_top_">class description</a>
  * for notes on roundtripping.
  * @param source the UTF-16 string buffer
  * @param offset32 UTF-32 offset
  * @return UTF-16 offset
  * @exception IndexOutOfBoundsException if offset32 is out of bounds.
  */
  public static int findOffsetFromCodePoint(StringBuffer source, int offset32)
  {
    char ch;
    int size = source.length(),
        result = 0,
        count = offset32;
    if (offset32 < 0 || offset32 > size) {
      throw new StringIndexOutOfBoundsException(offset32);
    }
    while (result < size && count > 0)
    {
      ch = source.charAt(result);
      if (isLeadSurrogate(ch) && ((result + 1) < size) && 
          isTrailSurrogate(source.charAt(result + 1))) {
        result ++;
      }
        
      count --;
      result ++;
    }
    if (count != 0) {
      throw new StringIndexOutOfBoundsException(offset32);
    }
    return result;
  }

  /**
  * Returns the UTF-16 offset that corresponds to a UTF-32 offset.
  * Used for random access. See the <a name="_top_">class description</a>
  * for notes on roundtripping.
  * @param source the UTF-16 char array whose substring is to be analysed
  * @param start offset of the substring to be analysed
  * @param limit offset of the substring to be analysed
  * @param offset32 UTF-32 offset relative to start
  * @return UTF-16 offset relative to start
  * @exception IndexOutOfBoundsException if offset32 is out of bounds.
  */
  public static int findOffsetFromCodePoint(char source[], int start, 
                                            int limit, int offset32)
  {
    char ch;
    int result = start,
        count = offset32;
    if (offset32 > limit - start) {
      throw new ArrayIndexOutOfBoundsException(offset32);
    }
    while (result < limit && count > 0)
    {
      ch = source[result];
      if (isLeadSurrogate(ch) && ((result + 1) < limit) && 
          isTrailSurrogate(source[result + 1])) {
        result ++;
      }
        
      count --;
      result ++;
    }
    if (count != 0) {
      throw new ArrayIndexOutOfBoundsException(offset32);
    }
    return result - start;
  }
  
  /**
  * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at or 
  * after the given UTF-16 offset. Used for random access. See the 
  * <a name="_top_">class description</a> for notes on roundtripping.<br>
  * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, then
  * the UTF-32 offset of the <strong>lead</strong> of the pair is returned.
  * </i>
  * <p>
  * To find the UTF-32 length of a string, use:
  *   <pre>
  *     len32 = countCodePoint(source, source.length());
  *   </pre>
  * </p>
  * <p>
  * @param source text to analyse
  * @param offset16 UTF-16 offset < source text length.
  * @return UTF-32 offset
  * @exception IndexOutOfBoundsException if offset16 is out of bounds.
  */
  public static int findCodePointOffset(String source, int offset16) 
  {
    if (offset16 < 0 || offset16 > source.length()) {
      throw new StringIndexOutOfBoundsException(offset16);
    }
     
    int result = 0;
    char ch;
    boolean hadLeadSurrogate = false;
    
    for (int i = 0; i < offset16; ++ i) 
    {
      ch = source.charAt(i);
      if (hadLeadSurrogate && isTrailSurrogate(ch)) {
        hadLeadSurrogate = false;           // count valid trail as zero
      }
      else
      {
        hadLeadSurrogate = isLeadSurrogate(ch);
        ++ result;                          // count others as 1
      }
    }
    
    if (offset16 == source.length()) {
        return result;
    }
    
    // end of source being the less significant surrogate character
    // shift result back to the start of the supplementary character
    if (hadLeadSurrogate && (isTrailSurrogate(source.charAt(offset16)))) {
      result --;
    }
      
    return result;
  }
  
  /**
  * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at
  * the given UTF-16 offset. Used for random access. See the
  * <a name="_top_">class description</a> for notes on roundtripping.<br>
  * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, then
  * the UTF-32 offset of the <strong>lead</strong> of the pair is returned.
  * </i>
  * <p>
  * To find the UTF-32 length of a string, use:
  *   <pre>
  *     len32 = countCodePoint(source);
  *   </pre>
  * </p>
  * <p>
  * @param source text to analyse
  * @param offset16 UTF-16 offset < source text length.
  * @return UTF-32 offset
  * @exception IndexOutOfBoundsException if offset16 is out of bounds.
  */
  public static int findCodePointOffset(StringBuffer source, int offset16)
  {
    if (offset16 < 0 || offset16 > source.length()) {
      throw new StringIndexOutOfBoundsException(offset16);
    }
     
    int result = 0;
    char ch;
    boolean hadLeadSurrogate = false;
    
    for (int i = 0; i < offset16; ++ i) 
    {
      ch = source.charAt(i);
      if (hadLeadSurrogate && isTrailSurrogate(ch)) {
        hadLeadSurrogate = false;           // count valid trail as zero
      }
      else
      {
        hadLeadSurrogate = isLeadSurrogate(ch);
        ++ result;                          // count others as 1
      }
    }
    
    if (offset16 == source.length()) {
        return result;
    }
    
    // end of source being the less significant surrogate character
    // shift result back to the start of the supplementary character
    if (hadLeadSurrogate && (isTrailSurrogate(source.charAt(offset16)))) {
      result --;
    }
      
    return result;
  }

  /**
  * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at
  * the given UTF-16 offset. Used for random access. See the
  * <a name="_top_">class description</a> for notes on roundtripping.<br>
  * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, then
  * the UTF-32 offset of the <strong>lead</strong> of the pair is returned.
  * </i>
  * <p>
  * To find the UTF-32 length of a substring, use:
  *   <pre>
  *     len32 = countCodePoint(source, start, limit);
  *   </pre>
  * </p>
  * <p>
  * @param source text to analyse
  * @param start offset of the substring
  * @param limit offset of the substring
  * @param offset16 UTF-16 relative to start
  * @return UTF-32 offset relative to start
  * @exception IndexOutOfBoundsException if offset16 is not within the range 
  *            of start and limit.
  */
  public static int findCodePointOffset(char source[], int start, int limit,
                                        int offset16)
  {
    offset16 += start;
    if (offset16 > limit) {
      throw new StringIndexOutOfBoundsException(offset16);
    }
     
    int result = 0;
    char ch;
    boolean hadLeadSurrogate = false;
    
    for (int i = start; i < offset16; ++ i) 
    {
      ch = source[i];
      if (hadLeadSurrogate && isTrailSurrogate(ch)) {
        hadLeadSurrogate = false;           // count valid trail as zero
      }
      else
      {
        hadLeadSurrogate = isLeadSurrogate(ch);
        ++ result;                          // count others as 1
      }
    }
    
    if (offset16 == limit) {
        return result;
    }
    
    // end of source being the less significant surrogate character
    // shift result back to the start of the supplementary character
    if (hadLeadSurrogate && (isTrailSurrogate(source[offset16]))) {
      result --;
    }
      
    return result;
  }

  /**
  * Append a single UTF-32 value to the end of a StringBuffer.
  * If a validity check is required, use 
  * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on 
  * char32 before calling.
  * @param char32 value to append. If out of bounds, substitutes 
  *               UTF32.REPLACEMENT_CHAR.
  * @return the updated StringBuffer
  */
  public static StringBuffer append(StringBuffer target, int char32)
  {
      // Check for irregular values
      if (char32 < UCharacter.MIN_VALUE || char32 > UCharacter.MAX_VALUE) {
          throw new IllegalArgumentException("Illegal codepoint");
      }
        
      // Write the UTF-16 values
      if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) 
      {
          target.append(getLeadSurrogate(char32));
	      target.append(getTrailSurrogate(char32));
      } 
	  else {
	      target.append((char)char32);
	  }
	  return target;
  }
  
  /**
  * Adds a codepoint to offset16 position of the argument char array.
  * @param target char array to be append with the new code point
  * @param limit UTF16 offset which the codepoint will be appended.
  * @param char32 code point to be appended
  * @return offset after char32 in the array.
  * @exception IndexOutOfBoundsException thrown if there is not enough space 
  *            for the append.
  */
  public static int append(char[] target, int limit, int char32)
  {
      // Check for irregular values
      if (char32 < UCharacter.MIN_VALUE || char32 > UCharacter.MAX_VALUE) {
          throw new IllegalArgumentException("Illegal codepoint");
      }
      // Write the UTF-16 values
      if (char32 >= UCharacter.SUPPLEMENTARY_MIN_VALUE) 
      {
          target[limit ++] = getLeadSurrogate(char32);
          target[limit ++] = getTrailSurrogate(char32);
      }
      else {
          target[limit ++] = (char)char32;
      }
      return limit;
  }
    
  /**
  * Number of codepoints in a UTF16 String
  * @param source UTF16 string
  * @return number of codepoint in string
  */
  public static int countCodePoint(String source)
  {
    if (source == null || source.length() == 0) {
        return 0;
    }
    return findCodePointOffset(source, source.length());
  }
  
  /**
  * Number of codepoints in a UTF16 String buffer
  * @param source UTF16 string buffer
  * @return number of codepoint in string
  */
  public static int countCodePoint(StringBuffer source)
  {
      if (source == null || source.length() == 0) {
          return 0;
      }
      return findCodePointOffset(source, source.length());
  }

  /**
  * Number of codepoints in a UTF16 char array substring
  * @param source UTF16 char array
  * @param start offset of the substring
  * @param limit offset of the substring
  * @return number of codepoint in the substring
  * @exception IndexOutOfBoundsException if start and limit are not valid.
  */
  public static int countCodePoint(char source[], int start, int limit)
  {
      if (source == null || source.length == 0) {
          return 0;
      }
      return findCodePointOffset(source, start, limit, limit) -
             findCodePointOffset(source, start, limit, start);
  }
  
  /**
  * Sets a code point into a UTF32 position.
  * Adjusts target according if we are replacing a non-supplementary 
  * codepoint with a supplementary and vice versa.
  * @param target stringbuffer
  * @param offset32 UTF32 position to insert into
  * @exception IndexOutOfBoundsException if offset32 is out of bounds.
  * @param char32 code point
  * @deprecated to be removed after the year 2002,
  * UTF16.setCharAt(target, 
  *                 findOffsetFromCodePoint(target.toString(), offset32), 
  *                                         char32);
  */
  public static void setCharAtCodePointOffset(StringBuffer target, 
                                              int offset32, int char32)
  {
    int offset16 = findOffsetFromCodePoint(target.toString(), offset32);
    setCharAt(target, offset16, char32);
  }

  /**
  * Set a code point into a UTF16 position. 
  * Adjusts target according if we are replacing a non-supplementary 
  * codepoint with a supplementary and vice versa.
  * @param target stringbuffer
  * @param offset16 UTF16 position to insert into
  * @param char32 code point
  */
  public static void setCharAt(StringBuffer target, int offset16, int char32)
  {
      int count = 1;
      char single = target.charAt(offset16);
    
      if (isSurrogate(single)) 
      {
          // pairs of the surrogate with offset16 at the lead char found
          if (isLeadSurrogate(single) && (target.length() > offset16 + 1) &&
              isTrailSurrogate(target.charAt(offset16 + 1))) {
	          count ++;
	      }
	      else {
	          // pairs of the surrogate with offset16 at the trail char found
	          if (isTrailSurrogate(single) && (offset16 > 0) &&
	              isLeadSurrogate(target.charAt(offset16 -1)))
	          {
	              offset16 --;
	              count ++;
	          }
	      }
	  }
	  target.replace(offset16, offset16 + count, valueOf(char32));
  }
	
  /**
  * Set a code point into a UTF16 position in a char array.
  * Adjusts target according if we are replacing a non-supplementary 
  * codepoint with a supplementary and vice versa.
  * @param target char array
  * @param limit numbers of valid chars in target, different from 
  *        target.length. limit counts the number of chars in target 
  *        that represents a string, not the size of array target.
  * @param offset16 UTF16 position to insert into
  * @param char32 code point
  * @return new number of chars in target that represents a string
  * @exception IndexOutOfBoundsException if offset16 is out of range
  */
  public static int setCharAt(char target[], int limit, 
                              int offset16, int char32)
  {
      if (offset16 >= limit) {
          throw new ArrayIndexOutOfBoundsException(offset16);
      }
      int count = 1;
      char single = target[offset16];
    
      if (isSurrogate(single)) 
      {
          // pairs of the surrogate with offset16 at the lead char found
          if (isLeadSurrogate(single) && (target.length > offset16 + 1) &&
              isTrailSurrogate(target[offset16 + 1])) {
	          count ++;
	      }
	      else {
	          // pairs of the surrogate with offset16 at the trail char found
	          if (isTrailSurrogate(single) && (offset16 > 0) &&
	              isLeadSurrogate(target[offset16 -1]))
	          {
	              offset16 --;
	              count ++;
	          }
	      }
	  }
	  
	  String str = valueOf(char32);
	  int result = limit;
	  int strlength = str.length();
	  target[offset16] = str.charAt(0);
	  if (count == strlength) {
	      if (count == 2) {
	          target[offset16 + 1] = str.charAt(1);
	      }
	  }
	  else {
	      // this is not exact match in space, we'll have to do some shifting
	      System.arraycopy(target, offset16 + count, target, 
	                       offset16 + strlength, limit - (offset16 + count));
	      if (count < strlength) {
	          // char32 is a supplementary character trying to squeeze into
	          // a non-supplementary space
	          target[offset16 + 1] = str.charAt(1);
	          result ++;
	          if (result < target.length) {
	              target[result] = 0;
	          }
	      }
	      else {
	          // char32 is a non-supplementary character trying to fill into
	          // a supplementary space
	          result --;
	          target[result] = 0;
	      }
	  }
	  return result;
  }
  
  /**
  * Shifts offset16 by the argument number of codepoints
  * @param source string
  * @param offset16 UTF16 position to shift
  * @param shift32 number of codepoints to shift
  * @return new shifted offset16 
  * @exception IndexOutOfBoundsException if the new offset16 is out of bounds.
  */
  public static int moveCodePointOffset(String source, int offset16, 
                                                       int shift32)
  {
      int size = source.length();
      if (offset16 < 0 || shift32 + offset16 > size) {
          throw new StringIndexOutOfBoundsException(offset16);
      }
      char ch;
      int result = offset16;
      int count = shift32;
      while (result < size && count > 0)
      {
          ch = source.charAt(result);
          if (isLeadSurrogate(ch) && ((result + 1) < size) && 
              isTrailSurrogate(source.charAt(result + 1))) {
              result ++;
          }
            
          count --;
          result ++;
      }
      if (count != 0) {
          throw new StringIndexOutOfBoundsException(shift32);
      }
      return result;
  }

  /**
  * Shifts offset16 by the argument number of codepoints
  * @param target string buffer
  * @param offset16 UTF16 position to shift
  * @param shift32 number of codepoints to shift
  * @return new shifted offset16 
  * @exception IndexOutOfBoundsException if the new offset16 is out of bounds.
  */
  public static int moveCodePointOffset(StringBuffer source, int offset16, 
                                                             int shift32)
  {
      int size = source.length();
      if (offset16 < 0 || shift32 + offset16 > size) {
          throw new StringIndexOutOfBoundsException(offset16);
      }
      char ch;
      int result = offset16;
      int count = shift32;
      while (result < size && count > 0)
      {
          ch = source.charAt(result);
          if (isLeadSurrogate(ch) && ((result + 1) < size) && 
              isTrailSurrogate(source.charAt(result + 1))) {
              result ++;
          }
            
          count --;
          result ++;
      }
      if (count != 0) {
          throw new StringIndexOutOfBoundsException(shift32);
      }
      return result;
  }

  /**
  * Shifts offset16 by the argument number of codepoints within a subarray.
  * @param target char array
  * @param start position of the subarray to be performed on
  * @param limit position of the subarray to be performed on
  * @param offset16 UTF16 position to shift relative to start
  * @param shift32 number of codepoints to shift
  * @return new shifted offset16 relative to start
  * @exception IndexOutOfBoundsException if the new offset16 is out of bounds 
  *            with respect to the subarray.
  */
  public static int moveCodePointOffset(char source[], int start, int limit, 
                                        int offset16, int shift32)
  {
      offset16 += start;
      if (shift32 + offset16 > limit) {
          throw new StringIndexOutOfBoundsException(offset16);
      }
      char ch;
      int result = offset16;
      int count = shift32;
      while (result < limit && count > 0)
      {
          ch = source[result];
          if (isLeadSurrogate(ch) && ((result + 1) < limit) && 
              isTrailSurrogate(source[result + 1])) {
              result ++;
          }
            
          count --;
          result ++;
      }
      if (count != 0) {
          throw new StringIndexOutOfBoundsException(shift32);
      }
      return result - start;
  }
  
  /**
  * Inserts char32 codepoint into target at the argument offset16. 
  * If the offset16 is in the middle of a supplementary codepoint, char32 will 
  * be inserted after the supplementary codepoint.
  * The length of target increases by one if codepoint is non-supplementary, 
  * 2 otherwise. 
  * <p>
  * The overall effect is exactly as if the argument were converted to a 
  * string by the method valueOf(char) and the characters in that string were 
  * then inserted into target at the position indicated by offset16. 
  * </p>
  * <p>
  * The offset argument must be greater than or equal to 0, and less than or 
  * equal to the length of source.
  * @param target string buffer to insert to
  * @param offset16 offset which char32 will be inserted in
  * @param char32 codepoint to be inserted
  * @return a reference to target
  * @exception IndexOutOfBoundsException thrown if offset16 is invalid.
  */
  public static StringBuffer insert(StringBuffer target, int offset16, 
                                    int char32)
  {
      String str = valueOf(char32);
      if (offset16 != target.length() && 
          bounds(target, offset16) == TRAIL_SURROGATE_BOUNDARY) {
          offset16 ++;
      }
      target.insert(offset16, str);
      return target;
  }

  /**
  * Inserts char32 codepoint into target at the argument offset16. 
  * If the offset16 is in the middle of a supplementary codepoint, char32 will 
  * be inserted after the supplementary codepoint.
  * Limit increases by one if codepoint is non-supplementary, 2 otherwise. 
  * <p>
  * The overall effect is exactly as if the argument were converted to a 
  * string by the method valueOf(char) and the characters in that string were 
  * then inserted into target at the position indicated by offset16. 
  * </p>
  * <p>
  * The offset argument must be greater than or equal to 0, and less than or 
  * equal to the limit.
  * @param target char array to insert to
  * @param limit end index of the char array, limit <= target.length
  * @param offset16 offset which char32 will be inserted in
  * @param char32 codepoint to be inserted
  * @return new limit size
  * @exception IndexOutOfBoundsException thrown if offset16 is invalid.
  */
  public static int insert(char target[], int limit, int offset16, int char32)
  {
      String str = valueOf(char32);
      if (offset16 != limit &&
          bounds(target, 0, limit, offset16) == TRAIL_SURROGATE_BOUNDARY) {
          offset16 ++;
      }
      int size = str.length();
      if (limit + size > target.length) {
          throw new ArrayIndexOutOfBoundsException(offset16 + size);
      }
      System.arraycopy(target, offset16, target, offset16 + size, 
                       limit - offset16);
      target[offset16] = str.charAt(0);
      if (size == 2) {
          target[offset16 + 1] = str.charAt(1);
      }
      return limit + size;
  }

  /**
  * Removes the codepoint at the specified position in this target (shortening 
  * target by 1 character if the codepoint is a non-supplementary, 2 
  * otherwise).
  * @param target string buffer to remove codepoint from
  * @param offset16 offset which the codepoint will be removed
  * @return a reference to target
  * @exception IndexOutOfBoundsException thrown if offset16 is invalid.
  */
  public static StringBuffer delete(StringBuffer target, int offset16)
  {
      int count = 1;
      switch (bounds(target, offset16)) {
          case LEAD_SURROGATE_BOUNDARY: 
                              count ++;
                              break;
          case TRAIL_SURROGATE_BOUNDARY: 
                              count ++;
                              offset16 --;
                              break;
      }
      target.delete(offset16, offset16 + count);
      return target;
  }

  /**
  * Removes the codepoint at the specified position in this target (shortening 
  * target by 1 character if the codepoint is a non-supplementary, 2 
  * otherwise).
  * @param target string buffer to remove codepoint from
  * @param limit end index of the char array, limit <= target.length
  * @param offset16 offset which the codepoint will be removed
  * @return a new limit size
  * @exception IndexOutOfBoundsException thrown if offset16 is invalid.
  */
  public static int delete(char target[], int limit, int offset16)
  {
      int count = 1;
      switch (bounds(target, 0, limit, offset16)) {
          case LEAD_SURROGATE_BOUNDARY: 
                              count ++;
                              break;
          case TRAIL_SURROGATE_BOUNDARY: 
                              count ++;
                              offset16 --;
                              break;
      }
      System.arraycopy(target, offset16 + count, target, offset16, 
                       limit - (offset16 + count));
      target[limit - count] = 0;
      return limit - count;
  }

  /**
  * Compare strings using Unicode code point order, instead of UTF-16 code 
  * unit order.
  */
  public static final class StringComparator implements java.util.Comparator 
  {
    /**
    * Standard String compare. Only one small section is different, marked in 
    * the code.
    */
    public int compare(Object a, Object b) 
    {
	    if (a == b) {
	      return 0;
	    }
      if (a == null) {
        return -1;
      }
      if (b == null) {
        return 1;
      }
          
      String sa = (String) a;
      String sb = (String) b;
      int lena = sa.length();
      int lenb = sb.length();
      int len = lena;
      if (len > lenb) {
        len = lenb;
      }
        
      for (int i = 0; i < len; ++i) 
      {
        char ca = sa.charAt(i);
        char cb = sb.charAt(i);
        if (ca == cb) {
          continue; // skip remap if equal
        }
                
        // start of only different section
        // what this part does is to rearrange the characters 0xE000 to 0xFFFF
        // to the region starting from 0xD800
        // and shift the surrogate characters to above this region
        if (ca >= LEAD_SURROGATE_MIN_VALUE_) {
          ca += (ca <= TRAIL_SURROGATE_MAX_VALUE_) ? 0x2000 : -0x800;
        }
        if (cb >= LEAD_SURROGATE_MIN_VALUE_) {
          cb += (cb <= TRAIL_SURROGATE_MAX_VALUE_) ? 0x2000 : -0x800;
        }
        // end of only different section
                
        if (ca < cb) {
          return -1;
        }
          
        return 1; // wasn't equal, so return 1
      }
      
      if (lena < lenb) {
        return -1;
      }
        
      if (lena > lenb) {
        return 1;
      }
            
      return 0;
    }
  }
}
