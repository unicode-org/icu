package com.ibm.icu.dev.test.normalizer;

/**
 * Utility class for supplementary code point 
 * support. This one is written purely for updating
 * Normalization sample from the unicode.org site.
 * If you want the real thing, use UTF16 class
 * from ICU4J
 * @author Vladimir Weinstein, Markus Scherer
 */
public class UTF16Util {
    static final int suppOffset = (0xd800 << 10) + 0xdc00 - 0x10000;

	/**
	 * Method nextCodePoint. Returns the next code point
     * in a string. 
	 * @param s String in question
	 * @param i index from which we want a code point
	 * @return int codepoint at index i
	 */
    static final public int nextCodePoint(String s, int i) {
        int ch = s.charAt(i);
        if (0xd800 <= ch && ch <= 0xdbff && ++i < s.length()) {
            int ch2 = s.charAt(i);
            if (0xdc00 <= ch2 && ch2 <= 0xdfff) {
                ch = (ch << 10) + ch2 - suppOffset;
            }
        }
        return ch;
    }

	/**
	 * Method prevCodePoint. Gets the code point preceding
     * index i (predecrement). 
	 * @param s String in question
	 * @param i index in string
	 * @return int codepoint at index --i
	 */
    static final public int prevCodePoint(String s, int i) {
        int ch = s.charAt(--i);
        if (0xdc00 <= ch && ch <= 0xdfff && --i >= 0) {
            int ch2 = s.charAt(i);
            if (0xd800 <= ch2 && ch2 <= 0xdbff) {
                ch = (ch2 << 10) + ch - suppOffset;
            }
        }
        return ch;
    }

	/**
     * Method nextCodePoint. Returns the next code point
     * in a string. 
     * @param s StringBuffer in question
     * @param i index from which we want a code point
     * @return int codepoint at index i
	 */
    static final public int nextCodePoint(StringBuffer s, int i) {
        int ch = s.charAt(i);
        if (0xd800 <= ch && ch <= 0xdbff && ++i < s.length()) {
            int ch2 = s.charAt(i);
            if (0xdc00 <= ch2 && ch2 <= 0xdfff) {
                ch = (ch << 10) + ch2 - suppOffset;
            }
        }
        return ch;
    }

    /**
     * Method prevCodePoint. Gets the code point preceding
     * index i (predecrement). 
     * @param s StringBuffer in question
     * @param i index in string
     * @return int codepoint at index --i
     */
    static final public int prevCodePoint(StringBuffer s, int i) {
        int ch = s.charAt(--i);
        if (0xdc00 <= ch && ch <= 0xdfff && --i >= 0) {
            int ch2 = s.charAt(i);
            if (0xd800 <= ch2 && ch2 <= 0xdbff) {
                ch = (ch2 << 10) + ch - suppOffset;
            }
        }
        return ch;
    }

	/**
	 * Method codePointLength. Returns the length 
     * in UTF-16 code units of a given code point
	 * @param c code point in question
	 * @return int length in UTF-16 code units. Can be 1 or 2
	 */
    static final public int codePointLength(int c) {
        return c <= 0xffff ? 1 : 2;
    }

	/**
	 * Method appendCodePoint. Appends a code point
     * to a StringBuffer
	 * @param buffer StringBuffer in question
	 * @param ch code point to append
	 */
    static final public void appendCodePoint(StringBuffer buffer, int ch) {
        if (ch <= 0xffff) {
            buffer.append((char)ch);
        } else {
            buffer.append((char)(0xd7c0 + (ch >> 10)));
            buffer.append((char)(0xdc00 + (ch & 0x3ff))); 
        }
    }

	/**
	 * Method insertCodePoint. Inserts a code point in
     * a StringBuffer
	 * @param buffer StringBuffer in question
	 * @param i index at which we want code point to be inserted
	 * @param ch code point to be inserted
	 */
    static final public void insertCodePoint(StringBuffer buffer, int i, int ch) {
        if (ch <= 0xffff) {
            buffer.insert(i, (char)ch);
        } else {
            buffer.insert(i, (char)(0xd7c0 + (ch >> 10))).insert(i + 1, (char)(0xdc00 + (ch & 0x3ff))); 
        }
    }
    
	/**
	 * Method setCodePointAt. Changes a code point at a
     * given index. Can change the length of the string.
	 * @param buffer StringBuffer in question
	 * @param i index at which we want to change the contents
	 * @param ch replacement code point
	 * @return int difference in resulting StringBuffer length
	 */
    static final public int setCodePointAt(StringBuffer buffer, int i, int ch) {
        int cp = nextCodePoint(buffer, i);
        
        if (ch <= 0xffff && cp <= 0xffff) { // Both BMP
            buffer.setCharAt(i, (char)ch);
            return 0;
        } else if (ch > 0xffff && cp > 0xffff) { // Both supplementary
            buffer.setCharAt(i, (char)(0xd7c0 + (ch >> 10)));
            buffer.setCharAt(i+1, (char)(0xdc00 + (ch & 0x3ff)));
            return 0;
        } else if (ch <= 0xffff && cp > 0xffff) { // putting BMP instead of supplementary, buffer shrinks
            buffer.setCharAt(i, (char)ch);
            buffer.deleteCharAt(i+1);
            return -1;
        } else { //if (ch > 0xffff && cp <= 0xffff) { // putting supplementary instead of BMP, buffer grows
            buffer.setCharAt(i, (char)(0xd7c0 + (ch >> 10)));
            buffer.insert(i+1, (char)(0xdc00 + (ch & 0x3ff))); 
            return 1;           
        }
    }

	/**
	 * Method countCodePoint. Counts the UTF-32 code points
     * in a UTF-16 encoded string.
	 * @param source String in question.
	 * @return int number of code points in this string
	 */
    static final public int countCodePoint(String source) 
    {         
        int result = 0;
        char ch;
        boolean hadLeadSurrogate = false;
        
        for (int i = 0; i < source.length(); ++ i) 
        {
            ch = source.charAt(i);
            if (hadLeadSurrogate && 0xdc00 <= ch && ch <= 0xdfff) {
                hadLeadSurrogate = false;           // count valid trail as zero
            }
            else
            {
                hadLeadSurrogate = (0xd800 <= ch && ch <= 0xdbff);
                ++ result;                          // count others as 1
            }
        }
        
        return result;
    }
    
    /**
     * Method countCodePoint. Counts the UTF-32 code points
     * in a UTF-16 encoded string.
     * @param source StringBuffer in question.
     * @return int number of code points in this string
     */
    static final public int countCodePoint(StringBuffer source) 
    {         
        int result = 0;
        char ch;
        boolean hadLeadSurrogate = false;
        
        for (int i = 0; i < source.length(); ++ i) 
        {
            ch = source.charAt(i);
            if (hadLeadSurrogate && 0xdc00 <= ch && ch <= 0xdfff) {
                hadLeadSurrogate = false;           // count valid trail as zero
            }
            else
            {
                hadLeadSurrogate = (0xd800 <= ch && ch <= 0xdbff);
                ++ result;                          // count others as 1
            }
        }
        
        return result;
    }

}
