/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/Replaceable.java,v $ 
 * $Date: 2000/04/25 17:17:37 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

/**
 * <code>Replaceable</code> is an interface that supports the
 * operation of replacing a substring with another piece of text.
 * <code>Replaceable</code> is needed in order to change a piece of
 * text while retaining style attributes.  For example, if the string
 * "the <b>bold</b> font" has range (4, 8) replaced with "strong",
 * then it becomes "the <b>strong</b> font".
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: Replaceable.java,v $ $Revision: 1.3 $ $Date: 2000/04/25 17:17:37 $
 */
public interface Replaceable {
    /**
     * Return the number of characters in the text.
     * @return number of characters in text
     */ 
    int length();

    /**
     * Return the character at the given offset into the text.
     * @param offset an integer between 0 and <code>length()</code>-1
     * inclusive
     * @return character of text at given offset
     */
    char charAt(int offset);

    /**
     * Copies characters from this object into the destination
     * character array.  The first character to be copied is at index
     * <code>srcStart</code>; the last character to be copied is at
     * index <code>srcLimit-1</code> (thus the total number of
     * characters to be copied is <code>srcLimit-srcStart</code>). The
     * characters are copied into the subarray of <code>dst</code>
     * starting at index <code>dstStart</code> and ending at index
     * <code>dstStart + (srcLimit-srcStart) - 1</code>.
     *
     * @param srcStart the beginning index to copy, inclusive; <code>0
     * <= start <= limit</code>.
     * @param srcLimit the ending index to copy, exclusive;
     * <code>start <= limit <= length()</code>.
     * @param dst the destination array.
     * @param dstStart the start offset in the destination array.
     */
    void getChars(int srcStart, int srcLimit, char dst[], int dstStart);

    /**
     * Replace a substring of this object with the given text.
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= length()</code>.
     * @param text the text to replace characters <code>start</code>
     * to <code>limit - 1</code>
     */
    void replace(int start, int limit, String text);

    /**
     * Replace a substring of this object with the given text.
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= length()</code>.
     * @param chars the text to replace characters <code>start</code>
     * to <code>limit - 1</code>
     * @param charsStart the beginning index into <code>chars</code>,
     * inclusive; <code>0 <= start <= limit</code>.
     * @param charsLen the number of characters of <code>chars</code>.
     */
    void replace(int start, int limit, char[] chars,
                 int charsStart, int charsLen);
    // Note: We use length rather than limit to conform to StringBuffer
    // and System.arraycopy.

    /**
     * Copy a substring of this object, retaining attribute (out-of-band)
     * information.  This method is used to duplicate or reorder substrings.
     * The destination index must not overlap the source range.
     * Implementations that do not care about maintaining out-of-band
     * information during copying may use the naive implementation:
     *
     * <pre> char[] text = new char[limit - start];
     * getChars(start, limit, text, 0);
     * replace(dest, dest, text, 0, limit - start);</pre>
     * 
     * @param start the beginning index, inclusive; <code>0 <= start <=
     * limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit <=
     * length()</code>.
     * @param dest the destination index.  The characters from
     * <code>start..limit-1</code> will be copied to <code>dest</code>.
     * Implementations of this method may assume that <code>dest <= start ||
     * dest >= limit</code>.
     */
    void copy(int start, int limit, int dest);
}
