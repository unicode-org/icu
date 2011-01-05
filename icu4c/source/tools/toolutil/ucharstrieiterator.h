/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ucharstrieiterator.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010nov15
*   created by: Markus W. Scherer
*/

#ifndef __UCHARSTRIEITERATOR_H__
#define __UCHARSTRIEITERATOR_H__

/**
 * \file
 * \brief C++ API: UCharsTrie iterator for all of its (string, value) pairs.
 */

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "ucharstrie.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

/**
 * Iterator for all of the (string, value) pairs in a UCharsTrie.
 */
class U_TOOLUTIL_API UCharsTrieIterator : public UMemory {
public:
    /**
     * Iterates from the root of a UChar-serialized UCharsTrie.
     * @param trieUChars The trie UChars.
     * @param maxStringLength If 0, the iterator returns full strings.
     *                        Otherwise, the iterator returns strings with this maximum length.
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     */
    UCharsTrieIterator(const UChar *trieUChars, int32_t maxStringLength, UErrorCode &errorCode);

    /**
     * Iterates from the current state of the specified UCharsTrie.
     * @param trie The trie whose state will be copied for iteration.
     * @param maxStringLength If 0, the iterator returns full strings.
     *                        Otherwise, the iterator returns strings with this maximum length.
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     */
    UCharsTrieIterator(const UCharsTrie &trie, int32_t maxStringLength, UErrorCode &errorCode);

    /**
     * Resets this iterator to its initial state.
     */
    UCharsTrieIterator &reset();

    /**
     * Finds the next (string, value) pair if there is one.
     *
     * If the string is truncated to the maximum length and does not
     * have a real value, then the value is set to -1.
     * In this case, this "not a real value" is indistinguishable from
     * a real value of -1.
     * @return TRUE if there is another element.
     */
    UBool next(UErrorCode &errorCode);

    /**
     * @return TRUE if there are more elements.
     */
    UBool hasNext() const { return pos_!=NULL || !stack_.isEmpty(); }

    /**
     * @return the NUL-terminated string for the last successful next()
     */
    const UnicodeString &getString() const { return str_; }
    /**
     * @return the value for the last successful next()
     */
    int32_t getValue() const { return value_; }

private:
    UBool truncateAndStop() {
        pos_=NULL;
        value_=-1;  // no real value for str
        return TRUE;
    }

    const UChar *branchNext(const UChar *pos, int32_t length, UErrorCode &errorCode);

    const UChar *uchars_;
    const UChar *pos_;
    const UChar *initialPos_;
    int32_t remainingMatchLength_;
    int32_t initialRemainingMatchLength_;
    UBool skipValue_;  // Skip intermediate value which was already delivered.

    UnicodeString str_;
    int32_t maxLength_;
    int32_t value_;

    // The stack stores pairs of integers for backtracking to another
    // outbound edge of a branch node.
    // The first integer is an offset from ByteTrie.bytes.
    // The second integer has the str.length() from before the node in bits 15..0,
    // and the remaining branch length in bits 31..16.
    // (We could store the remaining branch length minus 1 in bits 30..16 and not use the sign bit,
    // but the code looks more confusing that way.)
    UVector32 stack_;
};

U_NAMESPACE_END

#endif  // __UCHARSTRIEITERATOR_H__
