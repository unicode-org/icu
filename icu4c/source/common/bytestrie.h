/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  bytestrie.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010sep25
*   created by: Markus W. Scherer
*/

#ifndef __BYTESTRIE_H__
#define __BYTESTRIE_H__

/**
 * \file
 * \brief C++ API: Trie for mapping byte sequences to integer values.
 */

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "uassert.h"
#include "ustringtrie.h"

U_NAMESPACE_BEGIN

class ByteSink;
class BytesTrieBuilder;
class BytesTrieIterator;

/**
 * Light-weight, non-const reader class for a BytesTrie.
 * Traverses a byte-serialized data structure with minimal state,
 * for mapping byte sequences to non-negative integer values.
 */
class U_COMMON_API BytesTrie : public UMemory {
public:
    BytesTrie(const void *trieBytes)
            : bytes_(reinterpret_cast<const uint8_t *>(trieBytes)),
              pos_(bytes_), remainingMatchLength_(-1) {}

    /**
     * Resets this trie to its initial state.
     */
    BytesTrie &reset() {
        pos_=bytes_;
        remainingMatchLength_=-1;
        return *this;
    }

    /**
     * BytesTrie state object, for saving a trie's current state
     * and resetting the trie back to this state later.
     */
    class State : public UMemory {
    public:
        State() { bytes=NULL; }
    private:
        friend class BytesTrie;

        const uint8_t *bytes;
        const uint8_t *pos;
        int32_t remainingMatchLength;
    };

    /**
     * Saves the state of this trie.
     * @see resetToState
     */
    const BytesTrie &saveState(State &state) const {
        state.bytes=bytes_;
        state.pos=pos_;
        state.remainingMatchLength=remainingMatchLength_;
        return *this;
    }

    /**
     * Resets this trie to the saved state.
     * If the state object contains no state, or the state of a different trie,
     * then this trie remains unchanged.
     * @see saveState
     * @see reset
     */
    BytesTrie &resetToState(const State &state) {
        if(bytes_==state.bytes && bytes_!=NULL) {
            pos_=state.pos;
            remainingMatchLength_=state.remainingMatchLength;
        }
        return *this;
    }

    /**
     * Determines whether the byte sequence so far matches, whether it has a value,
     * and whether another input byte can continue a matching byte sequence.
     * @return The match/value Result.
     */
    UStringTrieResult current() const;

    /**
     * Traverses the trie from the initial state for this input byte.
     * Equivalent to reset().next(inByte).
     * @return The match/value Result.
     */
    inline UStringTrieResult first(int32_t inByte) {
        remainingMatchLength_=-1;
        return nextImpl(bytes_, inByte);
    }

    /**
     * Traverses the trie from the current state for this input byte.
     * @return The match/value Result.
     */
    UStringTrieResult next(int32_t inByte);

    /**
     * Traverses the trie from the current state for this byte sequence.
     * Equivalent to
     * \code
     * Result result=current();
     * for(each c in s)
     *   if(!USTRINGTRIE_HAS_NEXT(result)) return USTRINGTRIE_NO_MATCH;
     *   result=next(c);
     * return result;
     * \endcode
     * @return The match/value Result.
     */
    UStringTrieResult next(const char *s, int32_t length);

    /**
     * Returns a matching byte sequence's value if called immediately after
     * current()/first()/next() returned USTRINGTRIE_INTERMEDIATE_VALUE or USTRINGTRIE_FINAL_VALUE.
     * getValue() can be called multiple times.
     *
     * Do not call getValue() after USTRINGTRIE_NO_MATCH or USTRINGTRIE_NO_VALUE!
     */
    inline int32_t getValue() const {
        const uint8_t *pos=pos_;
        int32_t leadByte=*pos++;
        U_ASSERT(leadByte>=kMinValueLead);
        return readValue(pos, leadByte>>1);
    }

    /**
     * Determines whether all byte sequences reachable from the current state
     * map to the same value.
     * @param uniqueValue Receives the unique value, if this function returns TRUE.
     *                    (output-only)
     * @return TRUE if all byte sequences reachable from the current state
     *         map to the same value.
     */
    inline UBool hasUniqueValue(int32_t &uniqueValue) const {
        const uint8_t *pos=pos_;
        // Skip the rest of a pending linear-match node.
        return pos!=NULL && findUniqueValue(pos+remainingMatchLength_+1, FALSE, uniqueValue);
    }

    /**
     * Finds each byte which continues the byte sequence from the current state.
     * That is, each byte b for which it would be next(b)!=USTRINGTRIE_NO_MATCH now.
     * @param out Each next byte is appended to this object.
     *            (Only uses the out.Append(s, length) method.)
     * @return the number of bytes which continue the byte sequence from here
     */
    int32_t getNextBytes(ByteSink &out) const;

private:
    friend class BytesTrieBuilder;
    friend class BytesTrieIterator;

    inline void stop() {
        pos_=NULL;
    }

    // Reads a compact 32-bit integer.
    // pos is already after the leadByte, and the lead byte is already shifted right by 1.
    static int32_t readValue(const uint8_t *pos, int32_t leadByte);
    static inline const uint8_t *skipValue(const uint8_t *pos, int32_t leadByte) {
        U_ASSERT(leadByte>=kMinValueLead);
        if(leadByte>=(kMinTwoByteValueLead<<1)) {
            if(leadByte<(kMinThreeByteValueLead<<1)) {
                ++pos;
            } else if(leadByte<(kFourByteValueLead<<1)) {
                pos+=2;
            } else {
                pos+=3+((leadByte>>1)&1);
            }
        }
        return pos;
    }
    static inline const uint8_t *skipValue(const uint8_t *pos) {
        int32_t leadByte=*pos++;
        return skipValue(pos, leadByte);
    }

    // Reads a jump delta and jumps.
    static const uint8_t *jumpByDelta(const uint8_t *pos);

    static inline const uint8_t *skipDelta(const uint8_t *pos) {
        int32_t delta=*pos++;
        if(delta>=kMinTwoByteDeltaLead) {
            if(delta<kMinThreeByteDeltaLead) {
                ++pos;
            } else if(delta<kFourByteDeltaLead) {
                pos+=2;
            } else {
                pos+=3+(delta&1);
            }
        }
        return pos;
    }

    static inline UStringTrieResult valueResult(int32_t node) {
        return (UStringTrieResult)(USTRINGTRIE_INTERMEDIATE_VALUE-(node&kValueIsFinal));
    }

    // Handles a branch node for both next(byte) and next(string).
    UStringTrieResult branchNext(const uint8_t *pos, int32_t length, int32_t inByte);

    // Requires remainingLength_<0.
    UStringTrieResult nextImpl(const uint8_t *pos, int32_t inByte);

    // Helper functions for hasUniqueValue().
    // Recursively finds a unique value (or whether there is not a unique one)
    // from a branch.
    static const uint8_t *findUniqueValueFromBranch(const uint8_t *pos, int32_t length,
                                                    UBool haveUniqueValue, int32_t &uniqueValue);
    // Recursively finds a unique value (or whether there is not a unique one)
    // starting from a position on a node lead byte.
    static UBool findUniqueValue(const uint8_t *pos, UBool haveUniqueValue, int32_t &uniqueValue);

    // Helper functions for getNextBytes().
    // getNextBytes() when pos is on a branch node.
    static void getNextBranchBytes(const uint8_t *pos, int32_t length, ByteSink &out);
    static void append(ByteSink &out, int c);

    // BytesTrie data structure
    //
    // The trie consists of a series of byte-serialized nodes for incremental
    // string/byte sequence matching. The root node is at the beginning of the trie data.
    //
    // Types of nodes are distinguished by their node lead byte ranges.
    // After each node, except a final-value node, another node follows to
    // encode match values or continue matching further bytes.
    //
    // Node types:
    //  - Value node: Stores a 32-bit integer in a compact, variable-length format.
    //    The value is for the string/byte sequence so far.
    //    One node bit indicates whether the value is final or whether
    //    matching continues with the next node.
    //  - Linear-match node: Matches a number of bytes.
    //  - Branch node: Branches to other nodes according to the current input byte.
    //    The node byte is the length of the branch (number of bytes to select from)
    //    minus 1. It is followed by a sub-node:
    //    - If the length is at most kMaxBranchLinearSubNodeLength, then
    //      there are length-1 (key, value) pairs and then one more comparison byte.
    //      If one of the key bytes matches, then the value is either a final value for
    //      the string/byte sequence so far, or a "jump" delta to the next node.
    //      If the last byte matches, then matching continues with the next node.
    //      (Values have the same encoding as value nodes.)
    //    - If the length is greater than kMaxBranchLinearSubNodeLength, then
    //      there is one byte and one "jump" delta.
    //      If the input byte is less than the sub-node byte, then "jump" by delta to
    //      the next sub-node which will have a length of length/2.
    //      (The delta has its own compact encoding.)
    //      Otherwise, skip the "jump" delta to the next sub-node
    //      which will have a length of length-length/2.

    // Node lead byte values.

    // 00..0f: Branch node. If node!=0 then the length is node+1, otherwise
    // the length is one more than the next byte.

    // For a branch sub-node with at most this many entries, we drop down
    // to a linear search.
    static const int32_t kMaxBranchLinearSubNodeLength=5;

    // 10..1f: Linear-match node, match 1..16 bytes and continue reading the next node.
    static const int32_t kMinLinearMatch=0x10;
    static const int32_t kMaxLinearMatchLength=0x10;

    // 20..ff: Variable-length value node.
    // If odd, the value is final. (Otherwise, intermediate value or jump delta.)
    // Then shift-right by 1 bit.
    // The remaining lead byte value indicates the number of following bytes (0..4)
    // and contains the value's top bits.
    static const int32_t kMinValueLead=kMinLinearMatch+kMaxLinearMatchLength;  // 0x20
    // It is a final value if bit 0 is set.
    static const int32_t kValueIsFinal=1;

    // Compact value: After testing bit 0, shift right by 1 and then use the following thresholds.
    static const int32_t kMinOneByteValueLead=kMinValueLead/2;  // 0x10
    static const int32_t kMaxOneByteValue=0x40;  // At least 6 bits in the first byte.

    static const int32_t kMinTwoByteValueLead=kMinOneByteValueLead+kMaxOneByteValue+1;  // 0x51
    static const int32_t kMaxTwoByteValue=0x1aff;

    static const int32_t kMinThreeByteValueLead=kMinTwoByteValueLead+(kMaxTwoByteValue>>8)+1;  // 0x6c
    static const int32_t kFourByteValueLead=0x7e;

    // A little more than Unicode code points. (0x11ffff)
    static const int32_t kMaxThreeByteValue=((kFourByteValueLead-kMinThreeByteValueLead)<<16)-1;

    static const int32_t kFiveByteValueLead=0x7f;

    // Compact delta integers.
    static const int32_t kMaxOneByteDelta=0xbf;
    static const int32_t kMinTwoByteDeltaLead=kMaxOneByteDelta+1;  // 0xc0
    static const int32_t kMinThreeByteDeltaLead=0xf0;
    static const int32_t kFourByteDeltaLead=0xfe;
    static const int32_t kFiveByteDeltaLead=0xff;

    static const int32_t kMaxTwoByteDelta=((kMinThreeByteDeltaLead-kMinTwoByteDeltaLead)<<8)-1;  // 0x2fff
    static const int32_t kMaxThreeByteDelta=((kFourByteDeltaLead-kMinThreeByteDeltaLead)<<16)-1;  // 0xdffff

    // Fixed value referencing the BytesTrie bytes.
    const uint8_t *bytes_;

    // Iterator variables.

    // Pointer to next trie byte to read. NULL if no more matches.
    const uint8_t *pos_;
    // Remaining length of a linear-match node, minus 1. Negative if not in such a node.
    int32_t remainingMatchLength_;
};

U_NAMESPACE_END

#endif  // __BYTESTRIE_H__
