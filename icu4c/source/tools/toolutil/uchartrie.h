/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  uchartrie.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010nov14
*   created by: Markus W. Scherer
*/

#ifndef __UCHARTRIE_H__
#define __UCHARTRIE_H__

/**
 * \file
 * \brief C++ API: Dictionary trie for mapping Unicode strings (or 16-bit-unit sequences)
 *                 to integer values.
 */

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "uassert.h"
#include "udicttrie.h"

U_NAMESPACE_BEGIN

class UCharTrieBuilder;
class UCharTrieIterator;

/**
 * Base class for objects to which Unicode characters and strings can be appended.
 * Combines elements of Java Appendable and ICU4C ByteSink.
 * TODO: Should live in separate files, could be public API.
 */
class U_TOOLUTIL_API Appendable : public UObject {
public:
    /**
     * Appends a 16-bit code unit.
     * @param c code unit
     * @return *this
     */
    virtual Appendable &append(UChar c) = 0;
    /**
     * Appends a code point; has a default implementation.
     * @param c code point
     * @return *this
     */
    virtual Appendable &appendCodePoint(UChar32 c);
    /**
     * Appends a string; has a default implementation.
     * @param s string
     * @param length string length, or -1 if NUL-terminated
     * @return *this
     */
    virtual Appendable &append(const UChar *s, int32_t length);

    // TODO: getAppendBuffer(), see ByteSink
    // TODO: flush() (?) see ByteSink

private:
    // No ICU "poor man's RTTI" for this class nor its subclasses.
    virtual UClassID getDynamicClassID() const;
};

/**
 * Light-weight, non-const reader class for a UCharTrie.
 * Traverses a UChar-serialized data structure with minimal state,
 * for mapping strings (16-bit-unit sequences) to non-negative integer values.
 */
class U_TOOLUTIL_API UCharTrie : public UMemory {
public:
    UCharTrie(const UChar *trieUChars)
            : uchars_(trieUChars),
              pos_(uchars_), remainingMatchLength_(-1) {}

    /**
     * Resets this trie to its initial state.
     */
    UCharTrie &reset() {
        pos_=uchars_;
        remainingMatchLength_=-1;
        return *this;
    }

    /**
     * UCharTrie state object, for saving a trie's current state
     * and resetting the trie back to this state later.
     */
    class State : public UMemory {
    public:
        State() { uchars=NULL; }
    private:
        friend class UCharTrie;

        const UChar *uchars;
        const UChar *pos;
        int32_t remainingMatchLength;
    };

    /**
     * Saves the state of this trie.
     * @see resetToState
     */
    const UCharTrie &saveState(State &state) const {
        state.uchars=uchars_;
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
    UCharTrie &resetToState(const State &state) {
        if(uchars_==state.uchars && uchars_!=NULL) {
            pos_=state.pos;
            remainingMatchLength_=state.remainingMatchLength;
        }
        return *this;
    }

    /**
     * Determines whether the string so far matches, whether it has a value,
     * and whether another input UChar can continue a matching string.
     * @return The match/value Result.
     */
    UDictTrieResult current() const;

    /**
     * Traverses the trie from the initial state for this input UChar.
     * Equivalent to reset().next(uchar).
     * @return The match/value Result.
     */
    inline UDictTrieResult first(int32_t uchar) {
        remainingMatchLength_=-1;
        return nextImpl(uchars_, uchar);
    }

    /**
     * Traverses the trie from the initial state for the
     * one or two UTF-16 code units for this input code point.
     * Equivalent to reset().nextForCodePoint(cp).
     * @return The match/value Result.
     */
    inline UDictTrieResult firstForCodePoint(UChar32 cp) {
        return cp<=0xffff ?
            first(cp) :
            (first(U16_LEAD(cp))!=UDICTTRIE_NO_MATCH ?
                next(U16_TRAIL(cp)) :
                UDICTTRIE_NO_MATCH);
    }

    /**
     * Traverses the trie from the current state for this input UChar.
     * @return The match/value Result.
     */
    UDictTrieResult next(int32_t uchar);

    /**
     * Traverses the trie from the current state for the
     * one or two UTF-16 code units for this input code point.
     * @return The match/value Result.
     */
    inline UDictTrieResult nextForCodePoint(UChar32 cp) {
        return cp<=0xffff ?
            next(cp) :
            (next(U16_LEAD(cp))!=UDICTTRIE_NO_MATCH ?
                next(U16_TRAIL(cp)) :
                UDICTTRIE_NO_MATCH);
    }

    /**
     * Traverses the trie from the current state for this string.
     * Equivalent to
     * \code
     * Result result=current();
     * for(each c in s)
     *   if((result=next(c))==UDICTTRIE_NO_MATCH) return UDICTTRIE_NO_MATCH;
     * return result;
     * \endcode
     * @return The match/value Result.
     */
    UDictTrieResult next(const UChar *s, int32_t length);

    /**
     * Returns a matching string's value if called immediately after
     * current()/first()/next() returned UDICTTRIE_HAS_VALUE or UDICTTRIE_HAS_FINAL_VALUE.
     * getValue() can be called multiple times.
     *
     * Do not call getValue() after UDICTTRIE_NO_MATCH or UDICTTRIE_NO_VALUE!
     */
    inline int32_t getValue() const {
        const UChar *pos=pos_;
        int32_t leadUnit=*pos++;
        U_ASSERT(leadUnit>=kMinValueLead);
        return leadUnit&kValueIsFinal ?
            readValue(pos, leadUnit&0x7fff) : readNodeValue(pos, leadUnit);
    }

    /**
     * Determines whether all strings reachable from the current state
     * map to the same value.
     * @param uniqueValue Receives the unique value, if this function returns TRUE.
     *                    (output-only)
     * @return TRUE if all strings reachable from the current state
     *         map to the same value.
     */
    inline UBool hasUniqueValue(int32_t &uniqueValue) const {
        const UChar *pos=pos_;
        // Skip the rest of a pending linear-match node.
        return pos!=NULL && findUniqueValue(pos+remainingMatchLength_+1, FALSE, uniqueValue);
    }

    /**
     * Finds each UChar which continues the string from the current state.
     * That is, each UChar c for which it would be next(c)!=UDICTTRIE_NO_MATCH now.
     * @param out Each next UChar is appended to this object.
     *            (Only uses the out.append(c) method.)
     * @return the number of UChars which continue the string from here
     */
    int32_t getNextUChars(Appendable &out) const;

private:
    friend class UCharTrieBuilder;
    friend class UCharTrieIterator;

    inline void stop() {
        pos_=NULL;
    }

    // Reads a compact 32-bit integer.
    // pos is already after the leadUnit, and the lead unit has bit 15 reset.
    static inline int32_t readValue(const UChar *pos, int32_t leadUnit) {
        int32_t value;
        if(leadUnit<kMinTwoUnitValueLead) {
            value=leadUnit;
        } else if(leadUnit<kThreeUnitValueLead) {
            value=((leadUnit-kMinTwoUnitValueLead)<<16)|*pos;
        } else {
            value=(pos[0]<<16)|pos[1];
        }
        return value;
    }
    static inline const UChar *skipValue(const UChar *pos, int32_t leadUnit) {
        if(leadUnit>=kMinTwoUnitValueLead) {
            if(leadUnit<kThreeUnitValueLead) {
                ++pos;
            } else {
                pos+=2;
            }
        }
        return pos;
    }
    static inline const UChar *skipValue(const UChar *pos) {
        int32_t leadUnit=*pos++;
        return skipValue(pos, leadUnit&0x7fff);
    }

    static inline int32_t readNodeValue(const UChar *pos, int32_t leadUnit) {
        U_ASSERT(kMinValueLead<=leadUnit && leadUnit<kValueIsFinal);
        int32_t value;
        if(leadUnit<kMinTwoUnitNodeValueLead) {
            value=(leadUnit>>6)-1;
        } else if(leadUnit<kThreeUnitNodeValueLead) {
            value=(((leadUnit&0x7fc0)-kMinTwoUnitNodeValueLead)<<10)|*pos;
        } else {
            value=(pos[0]<<16)|pos[1];
        }
        return value;
    }
    static inline const UChar *skipNodeValue(const UChar *pos, int32_t leadUnit) {
        U_ASSERT(kMinValueLead<=leadUnit && leadUnit<kValueIsFinal);
        if(leadUnit>=kMinTwoUnitNodeValueLead) {
            if(leadUnit<kThreeUnitNodeValueLead) {
                ++pos;
            } else {
                pos+=2;
            }
        }
        return pos;
    }

    static inline const UChar *jumpByDelta(const UChar *pos) {
        int32_t delta=*pos++;
        if(delta>=kMinTwoUnitDeltaLead) {
            if(delta==kThreeUnitDeltaLead) {
                delta=(pos[0]<<16)|pos[1];
                pos+=2;
            } else {
                delta=((delta-kMinTwoUnitDeltaLead)<<16)|*pos++;
            }
        }
        return pos+delta;
    }

    static const UChar *skipDelta(const UChar *pos) {
        int32_t delta=*pos++;
        if(delta>=kMinTwoUnitDeltaLead) {
            if(delta==kThreeUnitDeltaLead) {
                pos+=2;
            } else {
                ++pos;
            }
        }
        return pos;
    }

    static inline UDictTrieResult valueResult(int32_t node) {
        return (UDictTrieResult)(UDICTTRIE_HAS_VALUE-(node>>15));
    }

    // Handles a branch node for both next(uchar) and next(string).
    UDictTrieResult branchNext(const UChar *pos, int32_t length, int32_t uchar);

    // Requires remainingLength_<0.
    UDictTrieResult nextImpl(const UChar *pos, int32_t uchar);

    // Helper functions for hasUniqueValue().
    // Recursively finds a unique value (or whether there is not a unique one)
    // from a branch.
    static const UChar *findUniqueValueFromBranch(const UChar *pos, int32_t length,
                                                  UBool haveUniqueValue, int32_t &uniqueValue);
    // Recursively finds a unique value (or whether there is not a unique one)
    // starting from a position on a node lead unit.
    static UBool findUniqueValue(const UChar *pos, UBool haveUniqueValue, int32_t &uniqueValue);

    // Helper functions for getNextUChars().
    // getNextUChars() when pos is on a branch node.
    static void getNextBranchUChars(const UChar *pos, int32_t length, Appendable &out);

    // UCharTrie data structure
    //
    // The trie consists of a series of UChar-serialized nodes for incremental
    // Unicode string/UChar sequence matching. (UChar=16-bit unsigned integer)
    // The root node is at the beginning of the trie data.
    //
    // Types of nodes are distinguished by their node lead unit ranges.
    // After each node, except a final-value node, another node follows to
    // encode match values or continue matching further units.
    //
    // Node types:
    //  - Final-value node: Stores a 32-bit integer in a compact, variable-length format.
    //    The value is for the string/UChar sequence so far.
    //  - Match node, optionally with an intermediate value in a different compact format.
    //    The value, if present, is for the string/UChar sequence so far.
    //
    //  Aside from the value, which uses the node lead unit's high bits:
    //
    //  - Linear-match node: Matches a number of units.
    //  - Branch node: Branches to other nodes according to the current input unit.
    //    The node unit is the length of the branch (number of units to select from)
    //    minus 1. It is followed by a sub-node:
    //    - If the length is at most kMaxBranchLinearSubNodeLength, then
    //      there are length-1 (key, value) pairs and then one more comparison unit.
    //      If one of the key units matches, then the value is either a final value for
    //      the string so far, or a "jump" delta to the next node.
    //      If the last unit matches, then matching continues with the next node.
    //      (Values have the same encoding as final-value nodes.)
    //    - If the length is greater than kMaxBranchLinearSubNodeLength, then
    //      there is one unit and one "jump" delta.
    //      If the input unit is less than the sub-node unit, then "jump" by delta to
    //      the next sub-node which will have a length of length/2.
    //      (The delta has its own compact encoding.)
    //      Otherwise, skip the "jump" delta to the next sub-node
    //      which will have a length of length-length/2.

    // Match-node lead unit values, after masking off intermediate-value bits:

    // 0000..002f: Branch node. If node!=0 then the length is node+1, otherwise
    // the length is one more than the next unit.

    // For a branch sub-node with at most this many entries, we drop down
    // to a linear search.
    static const int32_t kMaxBranchLinearSubNodeLength=5;

    // 0030..003f: Linear-match node, match 1..16 units and continue reading the next node.
    static const int32_t kMinLinearMatch=0x30;
    static const int32_t kMaxLinearMatchLength=0x10;

    // Match-node lead unit bits 14..6 for the optional intermediate value.
    // If these bits are 0, then there is no intermediate value.
    // Otherwise, see the *NodeValue* constants below.
    static const int32_t kMinValueLead=kMinLinearMatch+kMaxLinearMatchLength;  // 0x0040
    static const int32_t kNodeTypeMask=kMinValueLead-1;  // 0x003f

    // A final-value node has bit 15 set.
    static const int32_t kValueIsFinal=0x8000;

    // Compact value: After testing and masking off bit 15, use the following thresholds.
    static const int32_t kMaxOneUnitValue=0x3fff;

    static const int32_t kMinTwoUnitValueLead=kMaxOneUnitValue+1;  // 0x4000
    static const int32_t kThreeUnitValueLead=0x7fff;

    static const int32_t kMaxTwoUnitValue=((kThreeUnitValueLead-kMinTwoUnitValueLead)<<16)-1;  // 0x3ffeffff

    // Compact intermediate-value integer, lead unit shared with a branch or linear-match node.
    static const int32_t kMaxOneUnitNodeValue=0xff;
    static const int32_t kMinTwoUnitNodeValueLead=kMinValueLead+((kMaxOneUnitNodeValue+1)<<6);  // 0x4040
    static const int32_t kThreeUnitNodeValueLead=0x7fc0;

    static const int32_t kMaxTwoUnitNodeValue=
        ((kThreeUnitNodeValueLead-kMinTwoUnitNodeValueLead)<<10)-1;  // 0xfdffff

    // Compact delta integers.
    static const int32_t kMaxOneUnitDelta=0xfbff;
    static const int32_t kMinTwoUnitDeltaLead=kMaxOneUnitDelta+1;  // 0xfc00
    static const int32_t kThreeUnitDeltaLead=0xffff;

    static const int32_t kMaxTwoUnitDelta=((kThreeUnitDeltaLead-kMinTwoUnitDeltaLead)<<16)-1;  // 0x03feffff

    // Fixed value referencing the UCharTrie words.
    const UChar *uchars_;

    // Iterator variables.

    // Pointer to next trie unit to read. NULL if no more matches.
    const UChar *pos_;
    // Remaining length of a linear-match node, minus 1. Negative if not in such a node.
    int32_t remainingMatchLength_;
};

U_NAMESPACE_END

#endif  // __UCHARTRIE_H__
