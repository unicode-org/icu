/*
**********************************************************************
* Copyright (C) 1999, International Business Machines Corporation and
* others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.  Ported from java.  Modified to
*                           match current UnicodeString API.  Forced
*                           to use name "handleReplaceBetween" because
*                           of existing methods in UnicodeString.
**********************************************************************
*/

#ifndef REP_H
#define REP_H

#include "unicode/utypes.h"

class UnicodeString;

/**
 * <code>Replaceable</code> is an abstract base class representing a
 * string of characters that supports the replacement of a range of
 * itself with a new string of characters.  It is used by APIs that
 * change a piece of text while retaining style attributes.  In other
 * words, an implicit aspect of the <code>Replaceable</code> API is
 * that during a replace operation, new characters take on the
 * attributes, if any, of the old characters.  For example, if the
 * string "the <b>bold</b> font" has range (4, 8) replaced with
 * "strong", then it becomes "the <b>strong</b> font".
 *
 * <p><code>Replaceable</code> specifies ranges using an initial
 * offset and a limit offset.  The range of characters thus specified
 * includes the characters at offset initial..limit-1.  That is, the
 * start offset is inclusive, and the limit offset is exclusive.
 *
 * <p><code>Replaceable</code> also includes API to access characters
 * in the string: <code>length()</code>, <code>charAt()</code>, and
 * <code>extractBetween()</code>.
 *
 * @author Alan Liu
 * @draft
 */
class U_COMMON_API Replaceable {

public:

    /**
     * Destructor.
     * @draft
     */
    virtual ~Replaceable();

    /**
     * Return the number of characters in the text.
     * @return number of characters in text
     * @draft
     */ 
    virtual int32_t length() const = 0;

    /**
     * Return the character at the given offset into the text.
     * @param offset an integer between 0 and <code>length()</code>-1
     * inclusive
     * @return character of text at given offset
     * @draft
     */
    virtual UChar charAt(UTextOffset offset) const = 0;

    /**
     * Copy characters from this object into the destination character
     * array.  The first character to be copied is at index
     * <code>srcStart</code>; the last character to be copied is at
     * index <code>srcLimit-1</code> (thus the total number of
     * characters to be copied is <code>srcLimit-srcStart</code>). The
     * characters are copied into the subarray of <code>dst</code>
     * starting at index <code>dstStart</code> and ending at index
     * <code>dstStart + (srcLimit-srcStart) - 1</code>.
     *
     * @param srcStart the beginning index to copy, inclusive; <code>0
     * <= srcStart <= srcLimit</code>.
     * @param srcLimit the ending index to copy, exclusive;
     * <code>srcStart <= srcLimit <= length()</code>.
     * @param dst the destination array.
     * @param dstStart the start offset in the destination array.  
     * @draft
     */
    virtual void extractBetween(UTextOffset srcStart,
                                UTextOffset srcLimit,
                                UChar* dst,
                                UTextOffset dstStart = 0) const = 0;

    /**
     * Replace a substring of this object with the given text.  If the
     * characters being replaced have attributes, the new characters
     * that replace them should be given the same attributes.
     *
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= length()</code>.
     * @param text the text to replace characters <code>start</code>
     * to <code>limit - 1</code> 
     * @draft
     */
    virtual void handleReplaceBetween(UTextOffset start,
                                      UTextOffset limit,
                                      const UnicodeString& text) = 0;
    // Note: All other methods in this class take the names of
    // existing UnicodeString methods.  This method is the exception.
    // It is named differently because all replace methods of
    // UnicodeString return a UnicodeString&.  The 'between' is
    // required in order to conform to the UnicodeString naming
    // convention; API taking start/length are named <operation>, and
    // those taking start/limit are named <operationBetween>.  The
    // 'handle' is added because 'replaceBetween' and
    // 'doReplaceBetween' are already taken.

protected:

    /**
     * Default constructor.
     */
    Replaceable();
};

inline Replaceable::Replaceable() {}

inline Replaceable::~Replaceable() {}

#endif
