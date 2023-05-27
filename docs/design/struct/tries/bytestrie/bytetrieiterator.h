// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  bytetrieiterator.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010nov03
*   created by: Markus W. Scherer
*/

#ifndef __BYTETRIEITERATOR_H__
#define __BYTETRIEITERATOR_H__

/**
 * \file
 * \brief C++ API: ByteTrie iterator for all of its (byte sequence, value) pairs.
 */

// Needed if and when we change the .dat package index to a ByteTrie,
// so that icupkg can work with an input package.

#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "bytetrie.h"
#include "charstr.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

/**
 * Iterator for all of the (byte sequence, value) pairs in a a ByteTrie.
 */
class /*U_TOOLUTIL_API*/ ByteTrieIterator : public UMemory {
public:
    ByteTrieIterator(const void *trieBytes, UErrorCode &errorCode)
            : trie(trieBytes), value(0), stack(errorCode) {}

    /**
     * Finds the next (byte sequence, value) pair if there is one.
     * @return TRUE if there is another element.
     */
    UBool next(UErrorCode &errorCode);

    /**
     * @return TRUE if there are more elements.
     */
    UBool hasNext() const { return trie.pos!=NULL || !stack.isEmpty(); }

    /**
     * @return the NUL-terminated byte sequence for the last successful next()
     */
    const StringPiece &getString() const { return sp; }
    /**
     * @return the value for the last successful next()
     */
    const int32_t getValue() const { return value; }

private:
    // The stack stores pairs of integers for backtracking to another
    // outbound edge of a branch node.
    // The first integer is an offset from ByteTrie.bytes.
    // The second integer has the str.length() from before the node in bits 27..0,
    // and the state in bits 31..28.
    // Except for the following values for a three-way-branch node,
    // the lower values indicate how many branches of a list-branch node
    // are left to be visited.
    static const int32_t kThreeWayBranchEquals=0xe;
    static const int32_t kThreeWayBranchGreaterThan=0xf;

    ByteTrie trie;

    CharString str;
    StringPiece sp;
    int32_t value;

    UVector32 stack;
};

UBool
ByteTrieIterator::next(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return FALSE;
    }
    if(trie.pos==NULL) {
        if(stack.isEmpty()) {
            return FALSE;
        }
        // Read the top of the stack and continue with the next outbound edge of
        // the branch node.
        // The last outbound edge causes the branch node to be popped off the stack
        // and the iteration to continue from the trie.pos there.
        int32_t stackSize=stack.size();
        int32_t state=stack.elementAti(stackSize-1);
        trie.pos=trie.bytes+stack.elementAti(stackSize-2);
        str.truncate(state&0xfffffff);
        state=(state>>28)&0xf;
        if(state==kThreeWayBranchEquals) {
            int32_t node=*trie.pos;  // Known to be a three-way-branch node.
            uint8_t trieByte=trie.pos[1];
            trie.pos+=node+3;  // Skip node, trie byte and fixed-width integer.
            UBool isFinal=trie.readCompactInt();
            // Rewrite the top of the stack for the greater-than branch.
            stack.setElementAt((int32_t)(trie.pos-trie.bytes), stackSize-2);
            stack.setElementAt((kThreeWayBranchGreaterThan<<28)|str.length(), stackSize-1);
            str.append((char)trieByte, errorCode);
            if(isFinal) {
                value=trie.value;
                trie.stop();
                sp.set(str.data(), str.length());
                return TRUE;
            } else {
                trie.pos+=trie.value;
            }
        } else if(state==kThreeWayBranchGreaterThan) {
            // Pop the state.
            stack.setSize(stackSize-2);
        } else {
            // Remainder of a list-branch node.
            // Read the next key byte.
            str.append((char)*trie.pos++, errorCode);
            if(state>0) {
                UBool isFinal=trie.readCompactInt();
                // Rewrite the top of the stack for the next branch.
                stack.setElementAt((int32_t)(trie.pos-trie.bytes), stackSize-2);
                stack.setElementAt(((state-1)<<28)|(str.length()-1), stackSize-1);
                if(isFinal) {
                    value=trie.value;
                    trie.stop();
                    sp.set(str.data(), str.length());
                    return TRUE;
                } else {
                    trie.pos+=trie.value;
                }
            } else {
                // Pop the state.
                stack.setSize(stackSize-2);
            }
        }
    }
    for(;;) {
        int32_t node=*trie.pos++;
        if(node>=ByteTrie::kMinValueLead) {
            // Deliver value for the byte sequence so far.
            if(trie.readCompactInt(node)) {
                value=trie.value;
                trie.stop();
            }
            sp.set(str.data(), str.length());
            return TRUE;
        } else if(node<ByteTrie::kMinLinearMatch) {
            // Branch node, needs to take the first outbound edge and push state for the rest.
            if(node<ByteTrie::kMinListBranch) {
                // Branching on a byte value,
                // with a jump delta for less-than, a compact int for equals,
                // and continuing for greater-than.
                stack.addElement((int32_t)(trie.pos-1-trie.bytes), errorCode);
                stack.addElement((kThreeWayBranchEquals<<28)|str.length(), errorCode);
                // For the less-than branch, ignore the trie byte.
                ++trie.pos;
                // Jump.
                int32_t delta=trie.readFixedInt(node);
                trie.pos+=delta;
            } else {
                // Branch node with a list of key-value pairs where
                // values are compact integers: either final values or jump deltas.
                int32_t length=node-ByteTrie::kMinListBranch;  // Actual list length minus 2.
                // Read the first (key, value) pair.
                uint8_t trieByte=*trie.pos++;
                UBool isFinal=trie.readCompactInt();
                stack.addElement((int32_t)(trie.pos-trie.bytes), errorCode);
                stack.addElement((length<<28)|str.length(), errorCode);
                str.append((char)trieByte, errorCode);
                if(isFinal) {
                    value=trie.value;
                    trie.stop();
                    sp.set(str.data(), str.length());
                    return TRUE;
                } else {
                    trie.pos+=trie.value;
                }
            }
        } else {
            // Linear-match node, append length bytes to str.
            int32_t length=node-ByteTrie::kMinLinearMatch+1;
            str.append(reinterpret_cast<const char *>(trie.pos), length, errorCode);
            trie.pos+=length;
        }
    }
}

U_NAMESPACE_END

#endif  // __BYTETRIEITERATOR_H__
