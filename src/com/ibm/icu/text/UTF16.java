/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UTF16.java,v $ 
* $Date: 2002/04/03 22:48:10 $ 
* $Revision: 1.19 $
*
*******************************************************************************
*/

package com.ibm.icu.text;

import com.ibm.icu.impl.UCharacterProperty;
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
* for (int i = s.length() -1; i >= 0; --i) {
*   char ch = s.charAt(i);
*   doSomethingWith(ch);
* }
*  
* // iteration backwards: Changes for UTF-32
* int ch;
* for (int i = s.length() -1; i > 0; i-=UTF16.getCharCount(ch)) {
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
    // public variables ---------------------------------------------------
      
    /**
    * Value returned in <code><a href="#bounds(java.lang.String, int)">
    * bounds()</a></code>.
    * These values are chosen specifically so that it actually represents  
    * the position of the character 
    * [offset16 - (value >> 2), offset16 + (value & 3)]
    */
    public static final int SINGLE_CHAR_BOUNDARY = 1, 
                            LEAD_SURROGATE_BOUNDARY = 2, 
                            TRAIL_SURROGATE_BOUNDARY = 5;
    /** 
    * The lowest Unicode code point value.
    */
    public static final int CODEPOINT_MIN_VALUE = 0;
    /**
    * The highest Unicode code point value (scalar value) according to the 
    * Unicode Standard.
    */
    public static final int CODEPOINT_MAX_VALUE = 0x10ffff; 
    /**
    * The minimum value for Supplementary code points
    */
    public static final int SUPPLEMENTARY_MIN_VALUE  = 0x10000;  
    /**
     * Lead surrogate minimum value
     */
    public static final int LEAD_SURROGATE_MIN_VALUE = 0xD800;
	/**
     * Trail surrogate minimum value
     */
    public static final int TRAIL_SURROGATE_MIN_VALUE = 0xDC00; 
    /**
     * Lead surrogate maximum value
     */
    public static final int LEAD_SURROGATE_MAX_VALUE = 0xDBFF;
	/**
     * Trail surrogate maximum value
     */
    public static final int TRAIL_SURROGATE_MAX_VALUE = 0xDFFF;
    /**
     * Surrogate minimum value
     */
    public static final int SURROGATE_MIN_VALUE = LEAD_SURROGATE_MIN_VALUE;
    /**
     * Maximum surrogate value
     */
    public static final int SURROGATE_MAX_VALUE = TRAIL_SURROGATE_MAX_VALUE; 
                              
    // constructor --------------------------------------------------------
      
    /**
    * Prevent instance from being created.
    */
    private UTF16() 
    {
    }

    // public method ------------------------------------------------------
      
    /**
    * Extract a single UTF-32 value from a string.
    * Used when iterating forwards or backwards (with 
    * <code>UTF16.getCharCount()</code>, as well as random access. If a 
    * validity check is required, use 
    * <code><a href="../UCharacter.html#isLegal(char)">
    * UCharacter.isLegal()</a></code> on the return value.
    * If the char retrieved is part of a surrogate pair, its supplementary 
    * character will be returned. If a complete supplementary character is 
    * not found the incomplete character will be returned
    * @param source array of UTF-16 chars
    * @param offset16 UTF-16 offset to the start of the character.
    * @return UTF-32 value for the UTF-32 value that contains the char at 
    *         offset16. The boundaries of that codepoint are the same as in 
    *         <code>bounds32()</code>. 
    * @exception IndexOutOfBoundsException thrown if offset16 is out of 
    *            bounds.
    */
    public static int charAt(String source, int offset16) 
    {             
        if (offset16 < 0 || offset16 >= source.length()) {
            throw new StringIndexOutOfBoundsException(offset16);
        }
          
        char single = source.charAt(offset16);
        if (single < LEAD_SURROGATE_MIN_VALUE || 
            single > TRAIL_SURROGATE_MAX_VALUE) {
            return single;
        }

        // Convert the UTF-16 surrogate pair if necessary.
        // For simplicity in usage, and because the frequency of pairs is 
        // low, look both directions.
                
	    if (single <= LEAD_SURROGATE_MAX_VALUE) {
	        ++ offset16;
	        if (source.length() != offset16) {
    	        char trail = source.charAt(offset16);
	            if (trail >= TRAIL_SURROGATE_MIN_VALUE &&
	                trail <= TRAIL_SURROGATE_MAX_VALUE) {
	                return UCharacterProperty.getRawSupplementary(single, 
	                                                              trail);
	            }
	        }
	    } 
	    else 
	    { 
	        -- offset16;
	        if (offset16 >= 0) {
	        	// single is a trail surrogate so
	        	char lead = source.charAt(offset16);
	        	if (lead >= LEAD_SURROGATE_MIN_VALUE &&
	        	    lead <= LEAD_SURROGATE_MAX_VALUE) {
	         	   return UCharacterProperty.getRawSupplementary(lead, 
	         	                                                 single);
	        	}
	        }
	    } 
	    return single; // return unmatched surrogate
    }
      
    /**
    * Extract a single UTF-32 value from a string.
    * Used when iterating forwards or backwards (with
    * <code>UTF16.getCharCount()</code>, as well as random access. If a 
    * validity check is required, use 
    * <code><a href="../UCharacter.html#isLegal(char)">UCharacter.isLegal()
    * </a></code> on the return value.
    * If the char retrieved is part of a surrogate pair, its supplementary
    * character will be returned. If a complete supplementary character is 
    * not found the incomplete character will be returned
    * @param source UTF-16 chars string buffer
    * @param offset16 UTF-16 offset to the start of the character.
    * @return UTF-32 value for the UTF-32 value that contains the char at
    *         offset16. The boundaries of that codepoint are the same as in
    *         <code>bounds32()</code>.
    * @exception IndexOutOfBoundsException thrown if offset16 is out of 
    *            bounds.
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
        // For simplicity in usage, and because the frequency of pairs is 
        // low, look both directions.
                
	    if (single <= LEAD_SURROGATE_MAX_VALUE) 
	    {
	        ++ offset16;
	        if (source.length() != offset16)
	        {
	        char trail = source.charAt(offset16);
	        if (isTrailSurrogate(trail))
	            return UCharacterProperty.getRawSupplementary(single, trail);
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
	            return UCharacterProperty.getRawSupplementary(lead, single);
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
    * character will be returned. If a complete supplementary character is 
    * not found the incomplete character will be returned
    * @param source array of UTF-16 chars
    * @param start offset to substring in the source array for analyzing
    * @param limit offset to substring in the source array for analyzing
    * @param offset16 UTF-16 offset relative to start
    * @return UTF-32 value for the UTF-32 value that contains the char at
    *         offset16. The boundaries of that codepoint are the same as in
    *         <code>bounds32()</code>.
    * @exception IndexOutOfBoundsException thrown if offset16 is not within 
    *            the range of start and limit.
    */
    public static int charAt(char source[], int start, int limit, 
                             int offset16)
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
        // For simplicity in usage, and because the frequency of pairs is 
        // low, look both directions.      
	    if (single <= LEAD_SURROGATE_MAX_VALUE) {
	        offset16 ++;
	        if (offset16 >= limit) {
	            return single;
	        }
	        char trail = source[offset16];
	        if (isTrailSurrogate(trail)) {
	            return UCharacterProperty.getRawSupplementary(single, trail);
	        }
        } 
        else { // isTrailSurrogate(single), so
            if (offset16 == start) {
                return single;
            }
            offset16 --;
	        char lead = source[offset16];
	        if (isLeadSurrogate(lead))
	            return UCharacterProperty.getRawSupplementary(lead, single);
        }
        return single; // return unmatched surrogate
    }
      
    /**
    * Extract a single UTF-32 value from a string.
    * Used when iterating forwards or backwards (with
    * <code>UTF16.getCharCount()</code>, as well as random access. If a 
    * validity check is required, use 
    * <code><a href="../UCharacter.html#isLegal(char)">UCharacter.isLegal()
    * </a></code> on the return value.
    * If the char retrieved is part of a surrogate pair, its supplementary
    * character will be returned. If a complete supplementary character is 
    * not found the incomplete character will be returned
    * @param source UTF-16 chars string buffer
    * @param offset16 UTF-16 offset to the start of the character.
    * @return UTF-32 value for the UTF-32 value that contains the char at
    *         offset16. The boundaries of that codepoint are the same as in
    *         <code>bounds32()</code>.
    * @exception IndexOutOfBoundsException thrown if offset16 is out of 
    *            bounds.
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
        // For simplicity in usage, and because the frequency of pairs is 
        // low, look both directions.
                
	    if (single <= LEAD_SURROGATE_MAX_VALUE) 
	    {
	        ++ offset16;
	        if (source.length() != offset16)
	        {
	        char trail = source.charAt(offset16);
	        if (isTrailSurrogate(trail))
	            return UCharacterProperty.getRawSupplementary(single, trail);
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
	            return UCharacterProperty.getRawSupplementary(lead, single);
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
    * character will be returned. If a complete supplementary character is 
    * not found the incomplete character will be returned
    * @return UTF-32 value for the UTF-32 value that contains the char at 
    *         offset16. The boundaries of that codepoint are the same as in 
    *         <code>bounds32()</code>. 
    * @param source array of UTF-16 chars
    * @param offset32 UTF-32 offset to the start of the character.
    * @return a single UTF32 value
    * @exception IndexOutOfBoundsException if offset16 is out of bounds.
    * @deprecated to be removed after the year 2002, replaced by 
    *      UTF16.charAt(source, UTF16.findOffsetFromCodePoint(source, 
    *                   offset32));
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
    * @param ch the input codepoint.
    * @return 2 if is in supplementary space, otherwise 1. 
    */
    public static int getCharCount(int char32) 
    {
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
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
    *         For bit-twiddlers, the return values for these are chosen so 
    *         that the boundaries can be gotten by:
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
    * Returns the type of the boundaries around the char at offset16. Used 
    * for random access.
    * @param source string buffer to analyse
    * @param offset16 UTF16 offset
    * @return
    *     <ul>
    *     <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are
    *                                               [offset16, offset16 + 1]
    *     <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at 
    *                                    offset16; the bounds are 
    *                                    [offset16, offset16 + 2]
    *     <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at 
    *                                     offset16 - 1; the bounds are 
    *                                     [offset16 - 1, offset16 + 1]
    *     </ul>
    * For bit-twiddlers, the return values for these are chosen so that the 
    * boundaries can be gotten by: 
    *                    [offset16 - (value >> 2), offset16 + (value & 3)].
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
                if (offset16 >= 0 && 
                    isLeadSurrogate(source.charAt(offset16))) {
                    return TRAIL_SURROGATE_BOUNDARY;
                }
            }
        }
        return SINGLE_CHAR_BOUNDARY;
    }

    /**
    * Returns the type of the boundaries around the char at offset16. Used 
    * for random access. Note that the boundaries are determined with respect 
    * to the subarray, hence the char array {0xD800, 0xDC00} has the result 
    * SINGLE_CHAR_BOUNDARY for start = offset16 = 0 and limit = 1.
    * @param source char array to analyse
    * @param start offset to substring in the source array for analyzing
    * @param limit offset to substring in the source array for analyzing
    * @param offset16 UTF16 offset relative to start
    * @return
    *     <ul>
    *         <li> SINGLE_CHAR_BOUNDARY : a single char; the bounds are
    *         <li> LEAD_SURROGATE_BOUNDARY : a surrogate pair starting at
    *                       offset16; the bounds are [offset16, offset16 + 2]
    *         <li> TRAIL_SURROGATE_BOUNDARY : a surrogate pair starting at
    *               offset16 - 1; the bounds are [offset16 - 1, offset16 + 1]
    *     </ul>
    * For bit-twiddlers, the boundary values for these are chosen so that the 
    * boundaries can be gotten by: [offset16 - (boundvalue >> 2), offset16 
    *                                                    + (boundvalue & 3)].
    * @exception IndexOutOfBoundsException if offset16 is not within the 
    *                                      range of start and limit.
    */
    public static int bounds(char source[], int start, int limit, 
                             int offset16)
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
    * Returns the type of the boundaries around the char at offset32. Used 
    * for random access.
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
    *  UTF16.bounds(source, UTF16.findOffsetFromCodePoint(source, offset32));
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
        return LEAD_SURROGATE_MIN_VALUE <= char16 && 
            char16 <= TRAIL_SURROGATE_MAX_VALUE;
    }
        
    /**
    * Determines whether the character is a trail surrogate.
    * @param char16 the input character.
    * @return true iff the input character is a trail surrogate.
    */
    public static boolean isTrailSurrogate(char char16) 
    {
        return (TRAIL_SURROGATE_MIN_VALUE <= char16 && 
                char16 <= TRAIL_SURROGATE_MAX_VALUE);
    }
        
    /**
    * Determines whether the character is a lead surrogate.
    * @param char16 the input character.
    * @return true iff the input character is a lead surrogate
    */
    public static boolean isLeadSurrogate(char char16) 
    {
        return LEAD_SURROGATE_MIN_VALUE <= char16 && 
            char16 <= LEAD_SURROGATE_MAX_VALUE;
    }
            
    /**
    * Returns the lead surrogate.
    * If a validity check is required, use 
    * <code><a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> 
    * on char32 before calling.
    * @param char32 the input character.
    * @return lead surrogate if the getCharCount(ch) is 2; <br>
    *         and 0 otherwise (note: 0 is not a valid lead surrogate).
    */
    public static char getLeadSurrogate(int char32) 
    {
        if (char32 >= SUPPLEMENTARY_MIN_VALUE) {
        	return (char)(LEAD_SURROGATE_OFFSET_ + 
                      (char32 >> LEAD_SURROGATE_SHIFT_));
        }
            
        return 0;
    }
        
    /**
    * Returns the trail surrogate.
    * If a validity check is required, use 
    * <code><a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> 
    * on char32 before calling.
    * @param char32 the input character.
    * @return the trail surrogate if the getCharCount(ch) is 2; <br>otherwise 
    *         the character itself
    */
    public static char getTrailSurrogate(int char32) 
    {
        if (char32 >= SUPPLEMENTARY_MIN_VALUE) {
        	return (char)(TRAIL_SURROGATE_MIN_VALUE + 
                      (char32 & TRAIL_SURROGATE_MASK_));       
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
        if (char32 < CODEPOINT_MIN_VALUE || char32 > CODEPOINT_MAX_VALUE) {
        	throw new IllegalArgumentException("Illegal codepoint");
        }
        return toString(char32);
    }
      
    /**
    * Convenience method corresponding to String.valueOf(codepoint at 
    * offset16). 
    * Returns a one or two char string containing the UTF-32 value in UTF16 
    * format. If offset16 indexes a surrogate character, the whole 
    * supplementary codepoint will be returned.
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
    * format. If offset16 indexes a surrogate character, the whole 
    * supplementary codepoint will be returned.
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
    * format. If offset16 indexes a surrogate character, the whole 
    * supplementary codepoint will be returned, except when either the 
    * leading or trailing surrogate character lies out of the specified 
    * subarray. In the latter case, only the surrogate character within 
    * bounds will be returned.
    * If a validity check is required, use 
    * <a href="../UCharacter.html#isLegal(char)">isLegal()</a></code> on the 
    * codepoint at offset16 before calling.
    * The result returned will be a newly created String containing the 
    * relevant characters.
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
    * Used for random access. See the <a name="_top_">class description</a> 
    * for notes on roundtripping.
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
    public static int findOffsetFromCodePoint(StringBuffer source, 
                                              int offset32)
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
    * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at 
    * or after the given UTF-16 offset. Used for random access. See the 
    * <a name="_top_">class description</a> for notes on roundtripping.<br>
    * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, 
    * then the UTF-32 offset of the <strong>lead</strong> of the pair is 
    * returned.
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
    * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, 
    * then the UTF-32 offset of the <strong>lead</strong> of the pair is 
    * returned.
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
        if (hadLeadSurrogate && (isTrailSurrogate(source.charAt(offset16)))) 
        {
            result --;
        }
          
        return result;
    }

    /**
    * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at
    * the given UTF-16 offset. Used for random access. See the
    * <a name="_top_">class description</a> for notes on roundtripping.<br>
    * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, 
    * then the UTF-32 offset of the <strong>lead</strong> of the pair is 
    * returned.
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
    * @exception IndexOutOfBoundsException if offset16 is not within the 
    *            range of start and limit.
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
            	hadLeadSurrogate = false; // count valid trail as zero
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
    * @param char32 value to append.
    * @return the updated StringBuffer
    * @exception IllegalArgumentException thrown when char32 does not lie within
    *            the range of the Unicode codepoints
    */
    public static StringBuffer append(StringBuffer target, int char32)
    {
        // Check for irregular values
        if (char32 < CODEPOINT_MIN_VALUE || char32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException("Illegal codepoint");
        }
            
        // Write the UTF-16 values
        if (char32 >= SUPPLEMENTARY_MIN_VALUE) 
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
    * @exception IllegalArgumentException thrown if there is not enough 
    *            space for the append, or when char32 does not lie within
    *            the range of the Unicode codepoints.
    */
    public static int append(char[] target, int limit, int char32)
    {
        // Check for irregular values
        if (char32 < CODEPOINT_MIN_VALUE || char32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException("Illegal codepoint");
        }
        // Write the UTF-16 values
        if (char32 >= SUPPLEMENTARY_MIN_VALUE) 
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
        return findCodePointOffset(source, start, limit, limit - start);
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
    public static void setCharAt(StringBuffer target, int offset16, 
                                 int char32)
    {
        int count = 1;
        char single = target.charAt(offset16);
        
        if (isSurrogate(single)) 
        {
            // pairs of the surrogate with offset16 at the lead char found
            if (isLeadSurrogate(single) && (target.length() > offset16 + 1) 
                && isTrailSurrogate(target.charAt(offset16 + 1))) {
	            count ++;
	        }
	        else {
	            // pairs of the surrogate with offset16 at the trail char 
	            // found
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
	            // pairs of the surrogate with offset16 at the trail char 
	            // found
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
	        // this is not exact match in space, we'll have to do some 
	        // shifting
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
	            // char32 is a non-supplementary character trying to fill 
	            // into a supplementary space
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
    * @exception IndexOutOfBoundsException if the new offset16 is out of 
    *                                      bounds.
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
    * @exception IndexOutOfBoundsException if the new offset16 is out of 
    *                                      bounds.
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
    * @exception IndexOutOfBoundsException if the new offset16 is out of 
    *            bounds with respect to the subarray.
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
    * If the offset16 is in the middle of a supplementary codepoint, char32 
    * will be inserted after the supplementary codepoint.
    * The length of target increases by one if codepoint is non-supplementary, 
    * 2 otherwise. 
    * <p>
    * The overall effect is exactly as if the argument were converted to a 
    * string by the method valueOf(char) and the characters in that string 
    * were then inserted into target at the position indicated by offset16. 
    * </p>
    * <p>
    * The offset argument must be greater than or equal to 0, and less than 
    * or equal to the length of source.
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
    * If the offset16 is in the middle of a supplementary codepoint, char32 
    * will be inserted after the supplementary codepoint.
    * Limit increases by one if codepoint is non-supplementary, 2 otherwise. 
    * <p>
    * The overall effect is exactly as if the argument were converted to a 
    * string by the method valueOf(char) and the characters in that string 
    * were then inserted into target at the position indicated by offset16. 
    * </p>
    * <p>
    * The offset argument must be greater than or equal to 0, and less than 
    * or equal to the limit.
    * @param target char array to insert to
    * @param limit end index of the char array, limit <= target.length
    * @param offset16 offset which char32 will be inserted in
    * @param char32 codepoint to be inserted
    * @return new limit size
    * @exception IndexOutOfBoundsException thrown if offset16 is invalid.
    */
    public static int insert(char target[], int limit, int offset16, 
                             int char32)
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
    * Removes the codepoint at the specified position in this target 
    * (shortening target by 1 character if the codepoint is a 
    * non-supplementary, 2 otherwise).
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
    * Removes the codepoint at the specified position in this target 
    * (shortening target by 1 character if the codepoint is a 
    * non-supplementary, 2 otherwise).
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
    * Returns the index within the argument UTF16 format Unicode string of 
    * the first occurrence of the argument codepoint. I.e., the smallest 
    * index <code>i</code> such that <code>UTF16.charAt(source, i) == char32</code> is true. 
    * <p>If no such character occurs in this string, then -1 is returned.</p> 
    * <p>
    * Examples:<br>
    * UTF16.indexOf("abc", 'a') returns 0<br>
    * UTF16.indexOf("abc\ud800\udc00", 0x10000) returns 3<br>
    * UTF16.indexOf("abc\ud800\udc00", 0xd800) returns -1<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param char32 codepoint to search for 
    * @return the index of the first occurrence of the codepoint in the 
    *         argument Unicode string, or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int indexOf(String source, int char32)  
    {
        if (char32 < CODEPOINT_MIN_VALUE || 
            char32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException(
                            "Argument char32 is not a valid codepoint");
        }
        // non-surrogate bmp
        if (char32 < LEAD_SURROGATE_MIN_VALUE ||
            (char32 > TRAIL_SURROGATE_MAX_VALUE && 
             char32 < SUPPLEMENTARY_MIN_VALUE)) {
            return source.indexOf((char)char32);
        }
        // surrogate
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
            int result = source.indexOf((char)char32);
            if (result >= 0) {
                if (isLeadSurrogate((char)char32) && 
                    (result < source.length() - 1) && 
                    isTrailSurrogate(source.charAt(result + 1))) { 
                    return indexOf(source, char32, result + 1);
                }
                // trail surrogate
                if (result > 0 && 
                    isLeadSurrogate(source.charAt(result - 1))) {
                    return indexOf(source, char32, result + 1);                                              
                }
            }
            return result;
        }
        // supplementary
        String char32str = toString(char32);
        return source.indexOf(char32str);
    }
     
    /**
    * Returns the index within the argument UTF16 format Unicode string of 
    * the first occurrence of the argument string str. This method is 
    * implemented based on codepoints, hence a "lead surrogate character +
    * trail surrogate character" is treated as one entity.e
    * Hence if the str starts with trail surrogate character at index 0, a 
    * source with a leading a surrogate character before str found at in 
    * source will not have a valid match. Vice versa for lead surrogates 
    * that ends str.
    * See example below.
    * <p>If no such string str occurs in this source, then -1 is returned.
    * </p> <p>
    * Examples:<br>
    * UTF16.indexOf("abc", "ab") returns 0<br>
    * UTF16.indexOf("abc\ud800\udc00", "\ud800\udc00") returns 3<br>
    * UTF16.indexOf("abc\ud800\udc00", "\ud800") returns -1<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param str UTF16 format Unicode string to search for
    * @return the index of the first occurrence of the codepoint in the 
    *         argument Unicode string, or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int indexOf(String source, String str)  
    {
        int strLength = str.length();
        // non-surrogate ends
        if (!isTrailSurrogate(str.charAt(0)) && 
            !isLeadSurrogate(str.charAt(strLength - 1))) {
            return source.indexOf(str);
        }
        
        int result    = source.indexOf(str);
        int resultEnd = result + strLength;
        if (result >= 0) {
            // check last character
            if (isLeadSurrogate(str.charAt(strLength - 1)) && 
                (result < source.length() - 1) && 
                isTrailSurrogate(source.charAt(resultEnd + 1))) { 
                return indexOf(source, str, resultEnd + 1);
            }
            // check first character which is a trail surrogate
            if (isTrailSurrogate(str.charAt(0)) && result > 0 && 
                isLeadSurrogate(source.charAt(result - 1))) {
                return indexOf(source, str, resultEnd + 1);                                              
            }
        }
        return result;
    }
    
    /**
    * Returns the index within the argument UTF16 format Unicode string of 
    * the first occurrence of the argument codepoint. I.e., the smallest 
    * index i such that: <br>
    * (UTF16.charAt(source, i) == char32 && i >= fromIndex) is true. 
    * <p>If no such character occurs in this string, then -1 is returned.</p> 
    * <p>
    * Examples:<br>
    * UTF16.indexOf("abc", 'a', 1) returns -1<br>
    * UTF16.indexOf("abc\ud800\udc00", 0x10000, 1) returns 3<br>
    * UTF16.indexOf("abc\ud800\udc00", 0xd800, 1) returns -1<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param char32 codepoint to search for 
    * @param fromIndex the index to start the search from. 
    * @return the index of the first occurrence of the codepoint in the 
    *         argument Unicode string at or after fromIndex, or -1 if the 
    *         codepoint does not occur.
    * @draft since release 2.1
    */
    public static int indexOf(String source, int char32, int fromIndex) 
    {
        if (char32 < CODEPOINT_MIN_VALUE || char32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException(
                            "Argument char32 is not a valid codepoint");
        }
        // non-surrogate bmp
        if (char32 < LEAD_SURROGATE_MIN_VALUE ||
            (char32 > TRAIL_SURROGATE_MAX_VALUE && 
             char32 < SUPPLEMENTARY_MIN_VALUE)) {
            return source.indexOf((char)char32, fromIndex);
        }
        // surrogate
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
            int result = source.indexOf((char)char32, fromIndex);
            if (result >= 0) {
                if (isLeadSurrogate((char)char32) && 
                    (result < source.length() - 1) && 
                    isTrailSurrogate(source.charAt(result + 1))) { 
                    return indexOf(source, char32, result + 1);
                }
                // trail surrogate
                if (result > 0 && 
                    isLeadSurrogate(source.charAt(result - 1))) {
                    return indexOf(source, char32, result + 1);                                              
                }
            }
            return result;
        }
        // supplementary
        String char32str = toString(char32);
        return source.indexOf(char32str, fromIndex);
    }

    /**
    * Returns the index within the argument UTF16 format Unicode string of 
    * the first occurrence of the argument string str. This method is 
    * implemented based on codepoints, hence a "lead surrogate character +
    * trail surrogate character" is treated as one entity.e
    * Hence if the str starts with trail surrogate character at index 0, a 
    * source with a leading a surrogate character before str found at in 
    * source will not have a valid match. Vice versa for lead surrogates 
    * that ends str.
    * See example below.
    * <p>If no such string str occurs in this source, then -1 is returned.
    * </p> <p>
    * Examples:<br>
    * UTF16.indexOf("abc", "ab", 0) returns 0<br>
    * UTF16.indexOf("abc\ud800\udc00", "\ud800\udc00", 0) returns 3<br>
    * UTF16.indexOf("abc\ud800\udc00", "\ud800\udc00", 2) returns 3<br>
    * UTF16.indexOf("abc\ud800\udc00", "\ud800", 0) returns -1<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param str UTF16 format Unicode string to search for
    * @param fromIndex the index to start the search from. 
    * @return the index of the first occurrence of the codepoint in the 
    *         argument Unicode string, or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int indexOf(String source, String str, int fromIndex)  
    {
        int strLength = str.length();
        // non-surrogate ends
        if (!isTrailSurrogate(str.charAt(0)) && 
            !isLeadSurrogate(str.charAt(strLength - 1))) {
            return source.indexOf(str, fromIndex);
        }
        
        int result    = source.indexOf(str, fromIndex);
        int resultEnd = result + strLength;
        if (result >= 0) {
            // check last character
            if (isLeadSurrogate(str.charAt(strLength - 1)) && 
                (result < source.length() - 1) && 
                isTrailSurrogate(source.charAt(resultEnd))) { 
                return indexOf(source, str, resultEnd + 1);
            }
            // check first character which is a trail surrogate
            if (isTrailSurrogate(str.charAt(0)) && result > 0 && 
                isLeadSurrogate(source.charAt(result - 1))) {
                return indexOf(source, str, resultEnd + 1);                                              
            }
        }
        return result;
    }
    
    /**
    * Returns the index within the argument UTF16 format Unicode string of 
    * the last occurrence of the argument codepoint. I.e., the index returned 
    * is the largest value i such that: UTF16.charAt(source, i) == char32
    * is true. 
    * <p>
    * Examples:<br>
    * UTF16.lastIndexOf("abc", 'a') returns 0<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", 0x10000) returns 3<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", 0xd800) returns -1<br>
    * </p>
    * <p>source is searched backwards starting at the last character.</p> 
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param char32 codepoint to search for 
    * @return the index of the last occurrence of the codepoint in source, 
    *         or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int lastIndexOf(String source, int char32)  
    {
        if (char32 < CODEPOINT_MIN_VALUE || char32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException(
                            "Argument char32 is not a valid codepoint");
        }
        // non-surrogate bmp
        if (char32 < LEAD_SURROGATE_MIN_VALUE ||
            (char32 > TRAIL_SURROGATE_MAX_VALUE && 
             char32 < SUPPLEMENTARY_MIN_VALUE)) {
            return source.lastIndexOf((char)char32);
        }
        // surrogate
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
            int result = source.lastIndexOf((char)char32);
            if (result >= 0) {
                if (isLeadSurrogate((char)char32) && 
                    (result < source.length() - 1) && 
                    isTrailSurrogate(source.charAt(result + 1))) { 
                    return lastIndexOf(source, char32, result - 1);
                }
                // trail surrogate
                if (result > 0 && 
                    isLeadSurrogate(source.charAt(result - 1))) {
                    return lastIndexOf(source, char32, result - 1);                                              
                }
            }
            return result;
        }
        // supplementary
        String char32str = toString(char32);
        return source.lastIndexOf(char32str);
    }
    
    /**
    * Returns the index within the argument UTF16 format Unicode string of 
    * the last occurrence of the argument string str. This method is 
    * implemented based on codepoints, hence a "lead surrogate character +
    * trail surrogate character" is treated as one entity.e
    * Hence if the str starts with trail surrogate character at index 0, a 
    * source with a leading a surrogate character before str found at in 
    * source will not have a valid match. Vice versa for lead surrogates 
    * that ends str.
    * See example below.
    * <p>
    * Examples:<br>
    * UTF16.lastIndexOf("abc", "a") returns 0<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", "\ud800\udc00") returns 3<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", "\ud800") returns -1<br>
    * </p>
    * <p>source is searched backwards starting at the last character.</p> 
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param str UTF16 format Unicode string to search for 
    * @return the index of the last occurrence of the codepoint in source, 
    *         or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int lastIndexOf(String source, String str)  
    {
        int strLength = str.length();
        // non-surrogate ends
        if (!isTrailSurrogate(str.charAt(0)) && 
            !isLeadSurrogate(str.charAt(strLength - 1))) {
            return source.lastIndexOf(str);
        }
        
        int result    = source.lastIndexOf(str);
        if (result >= 0) {
            // check last character
            if (isLeadSurrogate(str.charAt(strLength - 1)) && 
                (result < source.length() - 1) && 
                isTrailSurrogate(source.charAt(result + strLength + 1))) { 
                return lastIndexOf(source, str, result - 1);
            }
            // check first character which is a trail surrogate
            if (isTrailSurrogate(str.charAt(0)) && result > 0 && 
                isLeadSurrogate(source.charAt(result - 1))) {
                return lastIndexOf(source, str, result - 1);                                              
            }
        }
        return result;
    }
    
    /**
    * <p>Returns the index within the argument UTF16 format Unicode string of 
    * the last occurrence of the argument codepoint, where the result is less
    * than or equals to fromIndex.</p> 
    * <p>This method is implemented based on codepoints, hence a single 
    * surrogate character will not match a supplementary character.</p>
    * <p>source is searched backwards starting at the last character starting 
    * at the specified index.</p>
    * <p>
    * Examples:<br>
    * UTF16.lastIndexOf("abc", 'c', 2) returns 2<br>
    * UTF16.lastIndexOf("abc", 'c', 1) returns -1<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", 0x10000, 5) returns 3<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", 0x10000, 3) returns 3<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", 0xd800) returns -1<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param char32 codepoint to search for 
    * @param fromIndex the index to start the search from. There is no 
    *                  restriction on the value of fromIndex. If it is 
    *                  greater than or equal to the length of this string, 
    *                  it has the same effect as if it were equal to one 
    *                  less than the length of this string: this entire 
    *                  string may be searched. If it is negative, it has 
    *                  the same effect as if it were -1: -1 is returned. 
    * @return the index of the last occurrence of the codepoint in source, 
    *         or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int lastIndexOf(String source, int char32, int fromIndex)
    {
        if (char32 < CODEPOINT_MIN_VALUE || char32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException(
                            "Argument char32 is not a valid codepoint");
        }
        // non-surrogate bmp
        if (char32 < LEAD_SURROGATE_MIN_VALUE ||
            (char32 > TRAIL_SURROGATE_MAX_VALUE && 
             char32 < SUPPLEMENTARY_MIN_VALUE)) {
            return source.lastIndexOf((char)char32, fromIndex);
        }
        // surrogate
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
            int result = source.lastIndexOf((char)char32, fromIndex);
            if (result >= 0) {
                if (isLeadSurrogate((char)char32) && 
                    (result < source.length() - 1) && 
                    isTrailSurrogate(source.charAt(result + 1))) { 
                    return lastIndexOf(source, char32, result - 1);
                }
                // trail surrogate
                if (result > 0 && 
                    isLeadSurrogate(source.charAt(result - 1))) {
                    return lastIndexOf(source, char32, result - 1);                                              
                }
            }
            return result;
        }
        // supplementary
        String char32str = toString(char32);
        return source.lastIndexOf(char32str, fromIndex);
    }
    
    /**
    * <p>Returns the index within the argument UTF16 format Unicode string of 
    * the last occurrence of the argument string str, where the result is less
    * than or equals to fromIndex.</p> 
    * <p>This method is implemented based on codepoints, hence a 
    * "lead surrogate character + trail surrogate character" is treated as one 
    * entity.
    * Hence if the str starts with trail surrogate character at index 0, a 
    * source with a leading a surrogate character before str found at in 
    * source will not have a valid match. Vice versa for lead surrogates 
    * that ends str.
    * </p>
    * See example below.
    * <p>
    * Examples:<br>
    * UTF16.lastIndexOf("abc", "c", 2) returns 2<br>
    * UTF16.lastIndexOf("abc", "c", 1) returns -1<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", "\ud800\udc00", 5) returns 3<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", "\ud800\udc00", 3) returns 3<br>
    * UTF16.lastIndexOf("abc\ud800\udc00", "\ud800", 4) returns -1<br>
    * </p>
    * <p>source is searched backwards starting at the last character.</p> 
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string that will be searched
    * @param str UTF16 format Unicode string to search for 
    * @param fromIndex the index to start the search from. There is no 
    *                  restriction on the value of fromIndex. If it is 
    *                  greater than or equal to the length of this string, 
    *                  it has the same effect as if it were equal to one 
    *                  less than the length of this string: this entire 
    *                  string may be searched. If it is negative, it has 
    *                  the same effect as if it were -1: -1 is returned. 
    * @return the index of the last occurrence of the codepoint in source, 
    *         or -1 if the codepoint does not occur.
    * @draft since release 2.1
    */
    public static int lastIndexOf(String source, String str, int fromIndex)  
    {
        int strLength = str.length();
        // non-surrogate ends
        if (!isTrailSurrogate(str.charAt(0)) && 
            !isLeadSurrogate(str.charAt(strLength - 1))) {
            return source.lastIndexOf(str, fromIndex);
        }
        
        int result    = source.lastIndexOf(str, fromIndex);
        if (result >= 0) {
            // check last character
            if (isLeadSurrogate(str.charAt(strLength - 1)) && 
                (result < source.length() - 1) && 
                isTrailSurrogate(source.charAt(result + strLength))) { 
                return lastIndexOf(source, str, result - 1);
            }
            // check first character which is a trail surrogate
            if (isTrailSurrogate(str.charAt(0)) && result > 0 && 
                isLeadSurrogate(source.charAt(result - 1))) {
                return lastIndexOf(source, str, result - 1);                                              
            }
        }
        return result;
    }

    /**
    * Returns a new UTF16 format Unicode string resulting from replacing all 
    * occurrences of oldChar32 in source with newChar32. 
    * If the character oldChar32 does not occur in the UTF16 format Unicode
    * string source, then source will be returned. Otherwise, a new String 
    * object is created that represents a codepoint sequence identical to the 
    * codepoint sequence represented by source, except that every occurrence 
    * of oldChar32 is replaced by an occurrence of newChar32. 
    * <p>
    * Examples: <br>
    * UTF16.replace("mesquite in your cellar", 'e', 'o');<br>
    *        returns "mosquito in your collar"<br>
    * UTF16.replace("JonL", 'q', 'x');<br>
    *        returns "JonL" (no change)<br>
    * UTF16.replace("Supplementary character \ud800\udc00", 0x10000, '!');
    * <br>   returns "Supplementary character !"<br>
    * UTF16.replace("Supplementary character \ud800\udc00", 0xd800, '!');
    * <br>   returns "Supplementary character \ud800\udc00"<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string which the codepoint 
    *               replacements will be based on.
    * @param oldChar32 non-zero old codepoint to be replaced.
    * @param newChar32 the new codepoint to replace oldChar32
    * @return new String derived from source by replacing every occurrence 
    *         of oldChar32 with newChar32, unless when no oldChar32 is found
    *         in source then source will be returned.
    * @draft since release 2.1
    */
    public static String replace(String source, int oldChar32, 
                                 int newChar32)  
    {
        if (oldChar32 <= 0 || oldChar32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException(
                            "Argument oldChar32 is not a valid codepoint");
        }
        if (newChar32 <= 0 || newChar32 > CODEPOINT_MAX_VALUE) {
            throw new IllegalArgumentException(
                            "Argument newChar32 is not a valid codepoint");
        }
        
        int index     = indexOf(source, oldChar32);
        if (index == -1) {
            return source;
        }
        String       newChar32Str    = toString(newChar32);
        int          oldChar32Size   = 1;
        int          newChar32Size   = newChar32Str.length();
        StringBuffer result = new StringBuffer(source);
        int          resultIndex     = index;
        
        if (oldChar32 >= SUPPLEMENTARY_MIN_VALUE) {
            oldChar32Size = 2;
        }
        
        while (index != -1) {
            int endResultIndex  = resultIndex + oldChar32Size;
            result.replace(resultIndex, endResultIndex, newChar32Str);
            int lastEndIndex    = index + oldChar32Size;
            index       = indexOf(source, oldChar32, lastEndIndex);
            resultIndex += newChar32Size + index - lastEndIndex;
        }
        return result.toString();
    }
    
    /**
    * Returns a new UTF16 format Unicode string resulting from replacing all 
    * occurrences of oldStr in source with newStr. 
    * If the character oldStr does not occur in the UTF16 format Unicode
    * string source, then source will be returned. Otherwise, a new String 
    * object is created that represents a codepoint sequence identical to the 
    * codepoint sequence represented by source, except that every occurrence 
    * of oldStr is replaced by an occurrence of newStr. 
    * <p>
    * Examples: <br>
    * UTF16.replace("mesquite in your cellar", "e", "o");<br>
    *        returns "mosquito in your collar"<br>
    * UTF16.replace("mesquite in your cellar", "mesquite", "cat");<br>
    *        returns "cat in your collar"<br>
    * UTF16.replace("JonL", "q", "x");<br>
    *        returns "JonL" (no change)<br>
    * UTF16.replace("Supplementary character \ud800\udc00", "\ud800\udc00",
    *               '!');
    * <br>   returns "Supplementary character !"<br>
    * UTF16.replace("Supplementary character \ud800\udc00", "\ud800", '!');
    * <br>   returns "Supplementary character \ud800\udc00"<br>
    * </p>
    * Note this method is provided as support to jdk 1.3, which does not 
    * support supplementary characters to its fullest.
    * @param source UTF16 format Unicode string which the codepoint 
    *               replacements will be based on.
    * @param oldChar32 non-zero old codepoint to be replaced.
    * @param newChar32 the new codepoint to replace oldChar32
    * @return new String derived from source by replacing every occurrence 
    *         of oldChar32 with newChar32, unless when no oldChar32 is found
    *         in source then source will be returned.
    * @draft since release 2.1
    */
    public static String replace(String source, String oldStr, 
                                 String newStr)  
    {
        int index     = indexOf(source, oldStr);
        if (index == -1) {
            return source;
        }
        int          oldStrSize   = oldStr.length();
        int          newStrSize   = newStr.length();
        StringBuffer result       = new StringBuffer(source);
        int          resultIndex     = index;
        
        while (index != -1) {
            int endResultIndex  = resultIndex + oldStrSize;
            result.replace(resultIndex, endResultIndex, newStr);
            int lastEndIndex    = index + oldStrSize;
            index       = indexOf(source, oldStr, lastEndIndex);
            resultIndex += newStrSize + index - lastEndIndex;
        }
        return result.toString();
    }
    
    /** 
    * Reverses a UTF16 format Unicode string and replaces source's content 
    * with it.
    * This method will reverse surrogate characters correctly, instead of 
    * blindly reversing every character.
    * <p>
    * Examples:<br>
    * UTF16.reverse(new StringBuffer(
    *             "Supplementary characters \ud800\udc00\ud801\udc01"))<br>
    * returns "\ud801\udc01\ud800\udc00 sretcarahc yratnemelppuS".
    * @param source the source StringBuffer that contains UTF16 format 
    *        Unicode string to be reversed
    * @return a modified source with reversed UTF16 format Unicode string.
    * @draft since release 2.1
    */
    public static StringBuffer reverse(StringBuffer source)      
    {
        StringBuffer result = source.reverse();
        int resultLength  = result.length();
        int maxLeadLength = resultLength - 2;
        int i = 0;
        while (i < resultLength) {
            if (i <= maxLeadLength && isTrailSurrogate(result.charAt(i)) &&
                isLeadSurrogate(result.charAt(i + 1))) {
                char trail = result.charAt(i);
                result.deleteCharAt(i);
                result.insert(i + 1, trail);
                i += 2;
            }
            else {
                i ++;
            }
        }
        return result;
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
            if (ca >= LEAD_SURROGATE_MIN_VALUE) {
            ca += (ca <= TRAIL_SURROGATE_MAX_VALUE) ? 0x2000 : -0x800;
            }
            if (cb >= LEAD_SURROGATE_MIN_VALUE) {
            cb += (cb <= TRAIL_SURROGATE_MAX_VALUE) ? 0x2000 : -0x800;
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
    
    // private data members -------------------------------------------------
                             
    /**
    * Shift value for lead surrogate to form a supplementary character.
    */
	private static final int LEAD_SURROGATE_SHIFT_ = 10;
	/**
    * Mask to retrieve the significant value from a trail surrogate.
    */
	private static final int TRAIL_SURROGATE_MASK_     = 0x3FF;   
    /**
     * Value that all lead surrogate starts with
     */
    private static final int LEAD_SURROGATE_OFFSET_ = 
	                                    LEAD_SURROGATE_MIN_VALUE - 
	                                   (SUPPLEMENTARY_MIN_VALUE 
	                                    >> LEAD_SURROGATE_SHIFT_); 	                  
    
    // private methods ------------------------------------------------------
    
    /**
    * <p>Converts argument code point and returns a String object representing 
    * the code point's value in UTF16 format.</p>
    * <p>This method does not check for the validity of the codepoint, the
    * results are not guaranteed if a invalid codepoint is passed as 
    * argument.</p>
    * <p>The result is a string whose length is 1 for non-supplementary code 
    * points, 2 otherwise.</p>
    * @param ch code point
    * @return string representation of the code point
    */
    public static String toString(int ch)
    {   
        if (ch < SUPPLEMENTARY_MIN_VALUE) {
            return String.valueOf((char)ch);
        }
        
        StringBuffer result = new StringBuffer();
        result.append(getLeadSurrogate(ch));
        result.append(getTrailSurrogate(ch));
        return result.toString();
    }
}
