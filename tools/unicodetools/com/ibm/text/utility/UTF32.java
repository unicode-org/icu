package com.ibm.text.utility;

/**
* Utility class for demonstrating UTF16 character conversions and indexing conversions.
* Ideally, these methods would be on existing classes in Java, but they can also be used
* in a stand-alone utility class like this one.
* <p>Code that uses strings alone rarely need modification. 
* By design, UTF-16 does not allow overlap, so searching for strings is a safe operation.
* Similarly, concatenation is always safe. Substringing is safe if the start and end are both
* on UTF32 boundaries. In normal code, the values for start and end are on those boundaries,
* since they arose from operations like searching.
* If not, the nearest UTF-32 boundaries can be determined using <code>bounds32()</code>.
* <p>Here is a summary of the methods:
* <ul><li>
* <code>char32At()</code>, <code>count32()</code>, and <code>append32()</code>
* are most important methods for most programs.
* They are used for iteration, filtering and copying. See the examples below.
* </li><li>
* <code>bounds32()</code> is useful for finding the nearest UTF-32 boundaries.
* However, in most circumstances it is better to use 
* <a <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/text/BreakIterator.html#getCharacterInstance(java.util.Locale)">
* BreakIterator.getCharacterInstance(Locale)</a> to find character boundaries
* that are closer to end-user expectations.
* </li><li>
* <code>valueOf32()</code> is occasionally convenient for producing a string containing a UTF-32 value. 
* </li><li>
* <code>findOffset16()</code> and <code>findOffset32()</code> are generally not needed, 
* except when interfacing to specifications that use UTF-32 indices (such as XSL).
* </li><li>
* <code>isLegal()</code> can be used to test whether UTF-16 or UTF-32 values are valid.
* </li><li>
* <code>isLeadSurrogate()</code>, <code>isSurrogate()</code>, and <code>isTrailSurrogate()</code>
* test the type of a char. They are useful for lower-level code.
* </li><li>
* <code>getChar32()</code>, <code>getLead()</code>, and <code>getTrail()</code> 
* are sometimes useful for putting together and taking apart UTF-32 values.
* </li></ul>
* <strong>Examples:</strong>
* <p>The following examples illustrate use of some of these methods. 
<pre>
// iteration forwards: Original
for (int i = 0; i < s.length(); ++i) {
    char ch = s.charAt(i);
    doSomethingWith(ch);
}

// iteration forwards: Changes for UTF-32
int ch;
for (int i = 0; i < s.length(); i+=UTF32.count16(ch)) {
    ch = UTF32.char32At(s,i);
    doSomethingWith(ch);
}

// iteration backwards: Original
for (int i = s.length()-1; i >= 0; --i) {
    char ch = s.charAt(i);
    doSomethingWith(ch);
}

// iteration backwards: Changes for UTF-32
int ch;
for (int i = s.length()-1; i > 0; i-=UTF32.count16(ch)) {
    ch = UTF32.char32At(s,i);
    doSomethingWith(ch);
}

* </pre>
* <strong>Notes:</strong>
* <ul><li>
* <strong>Naming:</strong> For clarity, High and Low surrogates are called <code>Lead</code> and <code>Trail</code> in the API,
* which gives a better sense of their ordering in a string. <code>offset16</code> and <code>offset32</code> are used to distinguish
* offsets to UTF-16 boundaries vs offsets to UTF-32 boundaries. 
* <code>int char32</code> is used to contain UTF-32 characters, as opposed to <code>char</code>, which is a UTF-16 code unit.
* </li><li>
* <strong>Roundtripping Offsets:</strong> You can always roundtrip
* from a UTF-32 offset to a UTF-16 offset and back.
* Because of the difference in structure, you can roundtrip
* from a UTF-16 offset to a UTF-32 offset and back if and only if <code>bounds(string, offset16) != TRAIL</code>.
* </li><li>
* <strong>Exceptions:</strong> The error checking will throw an exception if indices are out of bounds.
* Other than than that, all methods will behave reasonably, 
* even if unmatched surrogates or out-of-bounds UTF-32 values are present.
* <code>isLegal()</code> can be used to check for validity if desired.
* </li><li>
* <strong>Unmatched Surrogates:</strong> If the string contains unmatched surrogates, then these are
* counted as one UTF-32 value. This matches their iteration behavior, which is vital.
* It also matches common display practice as
* missing glyphs (see the Unicode Standard Section 5.4, 5.5).
* </li><li>
* <strong>Out-of-bounds UTF-32 values:</strong> If a <code>char32</code> contains an out-of-bounds UTF-32 value, 
* then it is treated as REPLACEMENT_CHAR for consistency across the API.
* </li><li>
* <strong>Optimization:</strong> The method implementations may need optimization if the compiler doesn't fold static final methods.
* Since surrogate pairs will form an exceeding small percentage of all the text in the world,
* the singleton case should always be optimized for.
* </li></ul>
* @author Mark Davis, with help from Markus Scherer
*/
public final class UTF32 {
    
    // =========================================================
    // UTILITIES
    // =========================================================
    
    /**
     * Unicode value used when translating into Unicode encoding form
     * and there is no existing character.
     */
	public static final char REPLACEMENT_CHAR = '\uFFFD';
	    
    /**
     * Value returned in <code><a href="#bounds32(java.lang.String, int)">bounds32()</a></code>.
     */
    public static final int SINGLE = 1, LEAD = 2, TRAIL = 5;

    /**
    * Determines how many chars this char32 requires.
    * If a validity check is required, use <code><a href="#isLegal(char)">isLegal()</a></code>
    * on char32 before calling.
     * <p><i>If this were integrated into the Java API, it could be a static method of either Character or String.</i>
    * @return 2 if is in surrogate space, otherwise 1. 
    * @param ch the input character.
    */
    public static int count16(int char32) {
        if (char32 < MIN_SUPPLEMENTARY) return 1;
        return 2;
    }
    
   /**
     * Extract a single UTF-32 value from a string.
     * Used when iterating forwards or backwards (with <code>count16()</code>, as well as random access.
     * If a validity check is required, use <code><a href="#isLegal(char)">isLegal()</a></code> on the return value.
     * <p><i>If this were integrated into the Java API, it could be a method of String, StringBuffer and possibly CharacterIterator.</i>
     * @return UTF-32 value for the UTF-32 value that contains the char at offset16.
     * The boundaries of that codepoint are the same as in <code>bounds32()</code>. 
     * @param source array of UTF-16 chars
     * @param offset16 UTF-16 offset to the start of the character.
     */
    public static int char32At(String source, int offset16) {
        char single = source.charAt(offset16);
        if (!isSurrogate(single)) return single;

        try { // use exception to catch out-of-bounds
        
            // Convert the UTF-16 surrogate pair if necessary.
            // For simplicity in usage, and because the frequency of pairs is low,
            // look both directions.
            
	        if (isLeadSurrogate(single)) {
	            char trail = source.charAt(++offset16);
	            if (isTrailSurrogate(trail)) {
	                return ((int)single << SURROGATE_SHIFT) + trail + SURROGATE_OFFSET;
	            }
            } else { // isTrailSurrogate(single), so
	            char lead = source.charAt(--offset16);
	            if (isLeadSurrogate(lead)) {
	                return ((int)lead << SURROGATE_SHIFT) + single + SURROGATE_OFFSET;
	            }
            }
        } catch (StringIndexOutOfBoundsException e) {}
        return single; // return unmatched surrogate
    }

    public static int char32At(StringBuffer source, int offset16) {
        char single = source.charAt(offset16);
        if (!isSurrogate(single)) return single;

        try { // use exception to catch out-of-bounds
        
            // Convert the UTF-16 surrogate pair if necessary.
            // For simplicity in usage, and because the frequency of pairs is low,
            // look both directions.
            
	        if (isLeadSurrogate(single)) {
	            char trail = source.charAt(++offset16);
	            if (isTrailSurrogate(trail)) {
	                return ((int)single << SURROGATE_SHIFT) + trail + SURROGATE_OFFSET;
	            }
            } else { // isTrailSurrogate(single), so
	            char lead = source.charAt(--offset16);
	            if (isLeadSurrogate(lead)) {
	                return ((int)lead << SURROGATE_SHIFT) + single + SURROGATE_OFFSET;
	            }
            }
        } catch (StringIndexOutOfBoundsException e) {}
        return single; // return unmatched surrogate
    }
    
    public static int char32At(char[] source, int start16, int end16, int offset16) {
        if (offset16 < start16 || offset16 >= end16) {
            throw new ArrayIndexOutOfBoundsException(offset16);
        }
        
        char single = source[offset16];
        if (!isSurrogate(single)) return single;

        try { // use exception to catch out-of-bounds
        
            // Convert the UTF-16 surrogate pair if necessary.
            // For simplicity in usage, and because the frequency of pairs is low,
            // look both directions.
            
	        if (isLeadSurrogate(single)) {
	            ++offset16;
	            if (offset16 >= end16) return single;
	            char trail = source[offset16];
	            if (isTrailSurrogate(trail)) {
	                return ((int)single << SURROGATE_SHIFT) + trail + SURROGATE_OFFSET;
	            }
            } else { // isTrailSurrogate(single), so
	            char lead = source[--offset16];
	            if (isLeadSurrogate(lead)) {
	                return ((int)lead << SURROGATE_SHIFT) + single + SURROGATE_OFFSET;
	            }
            }
        } catch (ArrayIndexOutOfBoundsException e) {}
        return single; // return unmatched surrogate
    }
    
    
    // moral equivalent of valueOf32(charAt32(x)), but no memory alloc
    public static String getCodePointSubstring(String s, int offset16) {
        switch(bounds32(s,offset16)) {
          default: return s.substring(offset16,offset16+1);
          case LEAD: return s.substring(offset16,offset16+2);
          case TRAIL: return s.substring(offset16-1,offset16+1);
        }
    }

    // moral equivalent of valueOf32(charAt32(x)), but no memory alloc
    public static String getCodePointSubstring(StringBuffer s, int offset16) {
        switch(bounds32(s,offset16)) {
          default: return s.substring(offset16,offset16+1);
          case LEAD: return s.substring(offset16,offset16+2);
          case TRAIL: return s.substring(offset16-1,offset16+1);
        }
    }

    public static int append32(char[] output, int oPosition, int oEnd, int cp) {
        if (oPosition >= oEnd) throw new ArrayIndexOutOfBoundsException(oPosition);
        output[oPosition++] = UTF32.getLead(cp);
        if (UTF32.count16(cp) != 1) {
            if (oPosition >= oEnd) throw new ArrayIndexOutOfBoundsException(oPosition);
            output[oPosition++] = UTF32.getTrail(cp);
        }
        return oPosition;
    }

    public static void setChar32At(StringBuffer b, int position, int codePoint) {
        int type = bounds32(b, position);
        // handle simple cases: #chars at position match #chars in codePoint
        int end = position;
        switch (type) {
          case SINGLE:
            if (isSupplementary(codePoint)) break;
            b.setCharAt(position, (char)codePoint);
            return;
          case LEAD:
            if (!isSupplementary(codePoint)) {
                ++end;
                break;
            }
            b.setCharAt(position++, (char)getLead(codePoint));
            b.setCharAt(position, (char)getTrail(codePoint));
            return;
          case TRAIL:
            if (!isSupplementary(codePoint)) {
                --position;
                break;
            }
            b.setCharAt(position++, (char)getLead(codePoint));
            b.setCharAt(position, (char)getTrail(codePoint));
            return;
        }
        // mismatch, just use long form
        b.replace(position, end+1, valueOf32(codePoint));
    }
        
    /**
     * See if a char value is legal. It can't be:
     * <ul><li>Not-a-character (either \\uFFFF or\\uFFFE).
     * The datatype char itself prevents out of bounds errors.
     * </li></ul>
     * Note: legal does not mean that it is assigned in this version of Unicode. 
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
     * @param UTF-32 value to test
     * @return true iff legal. 
     */
    public static boolean isLegal(char char16) {
        return (char16 < 0xFFFE);
    }

    /**
     * See if a UTF32 value is legal. It can't be:
     * <ul>
     * <li>Out of bounds (less than 0 or greater than MAX_UNICODE)</li>
     * <li>A surrogate value (00D800 to 00DCFF)</li>
     * <li>Not-a-character (of the form xxFFFF or xxFFFE)</li>
     * </ul>
     * Note: legal does not mean that it is assigned in this version of Unicode.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
     * @param char32 UTF-32 value to test
     * @return true iff legal. 
     */
    public static boolean isLegal(int char32) {
        if (char32 < 0) return false;
        //if (char32 < SURROGATE_BASE) return true;
        //if (char32 < SURROGATE_LIMIT) return false;
        if ((char32 & PLANE_MASK) >= NON_CHARACTER_BASE) return false;
        return (char32 <= MAX_UNICODE);
    }

   /**
    * Determines whether the code unit OR code point is a surrogate.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return true iff the input character is a surrogate.
    * @param ch the input character.
    */
    public static boolean isSurrogate(int char32) {
        return (SURROGATE_BASE <= char32 && char32 < SURROGATE_LIMIT);
    }
    
   /**
    * Determines whether the code point is a supplementary.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return true iff the input character is a surrogate.
    * @param ch the input character.
    */
    public static boolean isSupplementary(int char32) {
        return (char32 >= MIN_SUPPLEMENTARY && char32 <= MAX_UNICODE);
    }
    
   /**
    * Determines whether the code point is a supplementary.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return true iff the input character is a surrogate.
    * @param ch the input character.
    */
    public static boolean isBasic(int char32) {
        return (char32 >= 0 && char32 < MIN_SUPPLEMENTARY);
    }
    
   /**
    * Determines whether the character is a trail surrogate.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return true iff the input character is a trail surrogate.
    * @param ch the input character.
    */
    public static boolean isTrailSurrogate(char ch) {
        return (TRAIL_BASE <= ch && ch < TRAIL_LIMIT);
    }
    
   /**
    * Determines whether the character is a lead surrogate.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return true iff the input character is a lead surrogate.
    * @param ch the input character.
    */
    public static boolean isLeadSurrogate(char ch) {
        return (LEAD_BASE <= ch && ch < LEAD_LIMIT);
    }
        
   /**
    * Returns the lead surrogate.
    * If a validity check is required, use <code><a href="#isLegal(char)">isLegal()</a></code> on char32 before calling.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return lead surrogate if the count16(ch) is 2;
    * <br>otherwise the character itself
    * @param char32 the input character.
    */
    public static char getLead(int char32) {
        if (char32 >= MIN_SUPPLEMENTARY) {
            return (char)(LEAD_BASE_OFFSET + (char32 >> SURROGATE_SHIFT));
        }
        return (char)char32;
    }
    
   /**
    * Returns the trail surrogate.
    * If a validity check is required, use <code><a href="#isLegal(char)">isLegal()</a></code> on char32 before calling.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return the trail surrogate if the count16(ch) is 2;
    * <br>and 0 otherwise (note: 0 is not a valid lead surrogate).
    * @param char32 the input character.
    */
    public static char getTrail(int char32) {
        if (char32 >= MIN_SUPPLEMENTARY) {
            return (char)(TRAIL_BASE + (char32 & TRAIL_MASK));       
        }
        return '\u0000';
    }
    
   /**
    * Convenience method corresponding to String.valueOf(char). It returns a one or two char string containing
    * the UTF-32 value. If the input value can't be converted, it substitutes REPLACEMENT_CHAR.
    * If a validity check is required, use <code><a href="#isLegal(char)">isLegal()</a></code> before calling.
     * <p><i>If this were integrated into the Java API, it could be a static method of String.</i>
    * @return string value of char32
    * @param ch the input character.
    */
    public static String valueOf32(int char32) {
        if (char32 < 0 || MAX_UNICODE < char32) return String.valueOf(REPLACEMENT_CHAR);
        if (char32 < MIN_SUPPLEMENTARY) return String.valueOf((char)char32);
        synchronized (buf2) {   // saves allocations
            buf2[0] = (char)(LEAD_BASE_OFFSET + (char32 >> SURROGATE_SHIFT));
            buf2[1] = (char)(TRAIL_BASE + (char32 & TRAIL_MASK));
            return String.valueOf(buf2);
        }
    }
    private static char[] buf2 = new char[2]; // used to avoid allocations
    
   /**
    * Returns the UTF-32 character corresponding to the two chars.
    * If a validity check is required, check the arguments with 
    * <code>isLeadSurrogate()</code> and <code>isTrailSurrogate()</code>, respectively before calling.
     * <p><i>If this were integrated into the Java API, it could be a static method of String or Character.</i>
    * @return the UTF-32 character, or REPLACEMENT_CHAR if invalid.
    * @param lead the lead char
    * @param lead the trail char
    */
    public static int getChar32(char lead, char trail) {
        if (isLeadSurrogate(lead) && isTrailSurrogate(trail)) {
            return (lead <<= SURROGATE_SHIFT) + trail + SURROGATE_OFFSET;
        }
        return REPLACEMENT_CHAR;
    }
        
    /**
    * Returns the type of the UTF32 boundaries around the char at offset16.
    * Used for random access.
     * <p><i>If this were integrated into the Java API, it could be a method of String, StringBuffer and possibly CharacterIterator.</i>
    * @return SINGLE, FIRST, or SECOND:
    * <ul><li>
    * SINGLE: a single char; the bounds are [offset16, offset16+1]
    * </li><li>
    * LEAD: a surrogate pair starting at offset16; the bounds are [offset16, offset16+2]
    * </li><li>
    * TRAIL: a surrogate pair starting at offset16-1; the bounds are [offset16-1, offset16+1]
    * </ul>
    * For bit-twiddlers, the return values for these are chosen so that the boundaries can be gotten by:
    * [offset16 - (value>>2), offset16 + (value&3)].
    * @param source text to analyse
    * @param offset16 UTF-16 offset
    * @exception StringIndexOutOfBoundsException if offset16 is out of bounds.
    */
    public static int bounds32(String source, int offset16) {
        char ch = source.charAt(offset16);
        if (isSurrogate(ch)) {
            if (isLeadSurrogate(ch)) {
                if (++offset16 < source.length()
                  && isTrailSurrogate(source.charAt(offset16))) return LEAD;
            } else { // isTrailSurrogate(ch), so
                if (--offset16 >= 0
                  && isLeadSurrogate(source.charAt(offset16))) return TRAIL;
            }
        }
        return SINGLE;
    }

    public static int bounds32(StringBuffer source, int offset16) {
        char ch = source.charAt(offset16);
        if (isSurrogate(ch)) {
            if (isLeadSurrogate(ch)) {
                if (++offset16 < source.length()
                  && isTrailSurrogate(source.charAt(offset16))) return LEAD;
            } else { // isTrailSurrogate(ch), so
                if (--offset16 >= 0
                  && isLeadSurrogate(source.charAt(offset16))) return TRAIL;
            }
        }
        return SINGLE;
    }
    
    // should be renamed bounds

    public static int bounds32(char[] source, int oStart, int oEnd, int offset16) {
        if (offset16 < oStart || offset16 >= oEnd) {
            throw new ArrayIndexOutOfBoundsException(offset16);
        }
        char ch = source[offset16];
        if (isSurrogate(ch)) {
            if (isLeadSurrogate(ch)) {
                if (++offset16 < oEnd
                  && isTrailSurrogate(source[offset16])) return LEAD;
            } else { // isTrailSurrogate(ch), so
                if (--offset16 >= oStart
                  && isLeadSurrogate(source[offset16])) return TRAIL;
            }
        }
        return SINGLE;
    }



    /**
    * Returns the UTF-16 offset that corresponds to a UTF-32 offset. 
    * Used for random access. See the <a name="_top_">class description</a>
    * for notes on roundtripping.
     * <p><i>If this were integrated into the Java API, it could be a method of String, StringBuffer and possibly CharacterIterator.</i>
    * @return UTF-16 offset
    * @param offset32 UTF-32 offset
    * @param source text to analyse
    * @exception StringIndexOutOfBoundsException if offset32 is out of bounds.
    */
    public static int findOffset16(String source, int offset32) {
        int remaining = offset32; // for decrementing
        boolean hadLeadSurrogate = false;
        int i;
        
        for (i = 0; remaining > 0 && i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (hadLeadSurrogate && isTrailSurrogate(ch)) {
                hadLeadSurrogate = false;           // count valid trail as zero
            } else {
                hadLeadSurrogate = isLeadSurrogate(ch);
                --remaining;                        // count others as 1
            }
        }
        
        // if we didn't use up all of remaining (or if we started < 0)
        // then it is beyond the bounds
        
        if (remaining != 0) throw new StringIndexOutOfBoundsException(offset32);
        
        // special check for last surrogate if needed, for consistency with
        // other situations
        
        if (hadLeadSurrogate && i < source.length() && isTrailSurrogate(source.charAt(i))) {
            ++i;                                // grab extra unicode
        }
        return i;
    }

    /**
    * Returns the UTF-32 offset corresponding to the first UTF-32 boundary at or after the given UTF-16 offset.
    * Used for random access. See the <a name="_top_">class description</a>
    * for notes on roundtripping.
    * <i>Note: If the UTF-16 offset is into the middle of a surrogate pair, then
    * the UTF-32 offset of the <strong>end</strong> of the pair is returned.</i>
    * <p>To find the UTF-32 length of a string, use:
    * <pre>
    * len32 = getOffset32(source, source.length());
    * </pre>
     * <p><i>If this were integrated into the Java API, it could be a methods of String, StringBuffer and possibly CharacterIterator.</i>
    * @return UTF-32 offset
    * @param source text to analyse
    * @param offset16 UTF-16 offset
    * @exception StringIndexOutOfBoundsException if offset16 is out of bounds.
    */
    public static int findOffset32(String source, int offset16) {
        int result = 0;
        boolean hadLeadSurrogate = false;
        for (int i = 0; i < offset16; ++i) {
            char ch = source.charAt(i);
            if (hadLeadSurrogate && isTrailSurrogate(ch)) {
                hadLeadSurrogate = false;           // count valid trail as zero
            } else {
                hadLeadSurrogate = isLeadSurrogate(ch);
                ++result;                           // count others as 1
            }
        }
        return result;
    }

    public static int length32(String source) {
        return findOffset32(source, source.length());
    }

    /**
     * Append a single UTF-32 value to the end of a StringBuffer.
    * If a validity check is required, use <code><a href="#isLegal(char)">isLegal()</a></code> on char32 before calling.
     * <p><i>If this were integrated into the Java API, it could be a method of StringBuffer.</i>
     * @param char32 value to append. If out of bounds, substitutes REPLACEMENT_CHAR.
     * @param target string to add to
     */
    public static void append32(StringBuffer target, int char32) {
        
        // Check for irregular values
            
        if (char32 < 0 || char32 > MAX_UNICODE) char32 = REPLACEMENT_CHAR;
        
        // Write the UTF-16 values
        
	    if (char32 >= MIN_SUPPLEMENTARY) {
	        target.append((char)(LEAD_BASE_OFFSET + (char32 >> SURROGATE_SHIFT)));
	        target.append((char)(TRAIL_BASE + (char32 & TRAIL_MASK)));
	    } else {
	        target.append((char)char32);
	    }
    }
    
    /**
     * Compare strings using Unicode code point order, instead of UTF-16 code unit order.
     */
    public static final class StringComparator implements java.util.Comparator {
        /**
         * Standard String compare. Only one small section is different, marked in the code.
         */
        public int compare(Object a, Object b) {
	        if (a == b) {
	            return 0;
	        }
            if (a == null) {
                return -1;
            } else if (b == null) {
                return 1;
            }
            String sa = (String) a;
            String sb = (String) b;
            int lena = sa.length();
            int lenb = sb.length();
            int len = lena;
            if (len > lenb) len = lenb;
            for (int i = 0; i < len; ++i) {
                char ca = sa.charAt(i);
                char cb = sb.charAt(i);
                if (ca == cb) continue; // skip remap if equal
                
                // start of only different section
                if (ca >= 0xD800) {  // reshuffle to get right codepoint order
                    ca += (ca < 0xE000) ? 0x2000 : -0x800;
                }
                if (cb >= 0xD800) {  // reshuffle to get right codepoint order
                    cb += (cb < 0xE000) ? 0x2000 : -0x800;
                }
                // end of only different section
                
                if (ca < cb) return -1;
                return 1; // wasn't equal, so return 1
            }
            if (lena < lenb) return -1;
            if (lena > lenb) return 1;
            return 0;
        }
    }
                        
    // ===========================================================
    // PRIVATES
    // ===========================================================
    
    /**
     * Prevent instance from being created.
     */
    private UTF32() {}
    
   /**
     * Maximum code point values for UTF-32.
     */
    private static final int MAX_UNICODE = 0x10FFFF;
    
   /**
     * Maximum values for Basic code points (BMP).
     */
    private static final int MAX_BASIC = 0xFFFF;
    
   /**
     * Minimum value for Supplementary code points (SMP).
     */
    private static final int MIN_SUPPLEMENTARY = 0x10000;
    
    /**
     * Used to mask off single plane in checking for NON_CHARACTER
     */
    private static final int PLANE_MASK = 0xFFFF;
    
    /**
     * Range of non-characters in each plane
     */
    private static final int 
        NON_CHARACTER_BASE = 0xFFFE, 
        NON_CHARACTER_END = 0xFFFF;

    // useful statics and tables for fast lookup
    
	/**
	 * Values for surrogate detection. X is a surrogate iff X & SURROGATE_MASK == SURROGATE_MASK.
	 */
    static final int SURROGATE_MASK = 0xD800;
    
    /**
     * Bottom 10 bits for use in surrogates.
     */
	private static final int TRAIL_MASK = 0x3FF;
	
    /**
     * Shift value for surrogates.
     */
	private static final int SURROGATE_SHIFT = 10;
	
	/** 
	 * Lead surrogates go from LEAD_BASE up to LEAD_LIMIT-1.
	 */
	private static final int LEAD_BASE = 0xD800, LEAD_LIMIT = 0xDC00;
	
	/** 
	 * Trail surrogates go from TRAIL_BASE up to TRAIL_LIMIT-1.
	 */
	private static final int TRAIL_BASE = 0xDC00, TRAIL_LIMIT = 0xE000;
	
	/** 
	 * Surrogates go from SURROGATE_BASE up to SURROGATE_LIMIT-1.
	 */
	private static final int SURROGATE_BASE = 0xD800, SURROGATE_LIMIT = 0xE000;
    
    /**
     * Any codepoint at or greater than SURROGATE_SPACE_BASE requires 2 16-bit code units.
     */
	//private static final int SURROGATE_SPACE_BASE = 0x10000;

    /**
     * Offset to add to combined surrogate pair to avoid masking.
     */
	private static final int SURROGATE_OFFSET = MIN_SUPPLEMENTARY
	    - (LEAD_BASE << SURROGATE_SHIFT) - TRAIL_BASE;
	    
	private static final int LEAD_BASE_OFFSET = LEAD_BASE - (MIN_SUPPLEMENTARY >> SURROGATE_SHIFT);
	
};
