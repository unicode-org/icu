/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  bytestrieiterator.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010nov03
*   created by: Markus W. Scherer
*/

#ifndef __BYTESTRIEITERATOR_H__
#define __BYTESTRIEITERATOR_H__

/**
 * \file
 * \brief C++ API: BytesTrie iterator for all of its (byte sequence, value) pairs.
 */

// Needed if and when we change the .dat package index to a BytesTrie,
// so that icupkg can work with an input package.

#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "bytestrie.h"
#include "charstr.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

/**
 * Iterator for all of the (byte sequence, value) pairs in a BytesTrie.
 */
class U_TOOLUTIL_API BytesTrieIterator : public UMemory {
public:
    /**
     * Iterates from the root of a byte-serialized BytesTrie.
     * @param trieBytes The trie bytes.
     * @param maxStringLength If 0, the iterator returns full strings/byte sequences.
     *                        Otherwise, the iterator returns strings with this maximum length.
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     */
    BytesTrieIterator(const void *trieBytes, int32_t maxStringLength, UErrorCode &errorCode);

    /**
     * Iterates from the current state of the specified BytesTrie.
     * @param trie The trie whose state will be copied for iteration.
     * @param maxStringLength If 0, the iterator returns full strings/byte sequences.
     *                        Otherwise, the iterator returns strings with this maximum length.
     * @param errorCode Standard ICU error code. Its input value must
     *                  pass the U_SUCCESS() test, or else the function returns
     *                  immediately. Check for U_FAILURE() on output or use with
     *                  function chaining. (See User Guide for details.)
     */
    BytesTrieIterator(const BytesTrie &trie, int32_t maxStringLength, UErrorCode &errorCode);

    /**
     * Resets this iterator to its initial state.
     */
    BytesTrieIterator &reset();

    /**
     * Finds the next (byte sequence, value) pair if there is one.
     *
     * If the byte sequence is truncated to the maximum length and does not
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
     * @return the NUL-terminated byte sequence for the last successful next()
     */
    const StringPiece &getString() const { return sp_; }
    /**
     * @return the value for the last successful next()
     */
    int32_t getValue() const { return value_; }

private:
    UBool truncateAndStop() {
        pos_=NULL;
        value_=-1;  // no real value for str
        sp_.set(str_.data(), str_.length());
        return TRUE;
    }

    const uint8_t *branchNext(const uint8_t *pos, int32_t length, UErrorCode &errorCode);

    const uint8_t *bytes_;
    const uint8_t *pos_;
    const uint8_t *initialPos_;
    int32_t remainingMatchLength_;
    int32_t initialRemainingMatchLength_;

    CharString str_;
    StringPiece sp_;
    int32_t maxLength_;
    int32_t value_;

    // The stack stores pairs of integers for backtracking to another
    // outbound edge of a branch node.
    // The first integer is an offset from BytesTrie.bytes.
    // The second integer has the str.length() from before the node in bits 15..0,
    // and the remaining branch length in bits 24..16. (Bits 31..25 are unused.)
    // (We could store the remaining branch length minus 1 in bits 23..16 and not use bits 31..24,
    // but the code looks more confusing that way.)
    UVector32 stack_;
};

U_NAMESPACE_END

#endif  // __BYTESTRIEITERATOR_H__
