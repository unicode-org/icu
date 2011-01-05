/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   created on: 2010nov23
*   created by: Markus W. Scherer
*   ported from ICU4C bytestrie.h/.cpp
*/
package com.ibm.icu.impl;

import java.io.IOException;

/**
 * Light-weight, non-const reader class for a BytesTrie.
 * Traverses a byte-serialized data structure with minimal state,
 * for mapping byte sequences to non-negative integer values.
 *
 * @author Markus W. Scherer
 */
public final class BytesTrie implements Cloneable {
    public BytesTrie(byte[] trieBytes, int offset) {
        bytes_=trieBytes;
        pos_=root_=offset;
        remainingMatchLength_=-1;
    }

    /**
     * Clones this trie reader object and its state,
     * but not the byte array which will be shared.
     * @return A shallow clone of this trie.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();  // A shallow copy is just what we need.
    }

    /**
     * Resets this trie to its initial state.
     */
    public BytesTrie reset() {
        pos_=root_;
        remainingMatchLength_=-1;
        return this;
    }

    /**
     * BytesTrie state object, for saving a trie's current state
     * and resetting the trie back to this state later.
     */
    public static final class State {
        public State() {}
        private byte[] bytes;
        private int pos;
        private int remainingMatchLength;
    };

    /**
     * Saves the state of this trie.
     * @see #resetToState
     */
    public BytesTrie saveState(State state) /*const*/ {
        state.bytes=bytes_;
        state.pos=pos_;
        state.remainingMatchLength=remainingMatchLength_;
        return this;
    }

    /**
     * Resets this trie to the saved state.
     * @throws IllegalArgumentException if the state object contains no state,
     *         or the state of a different trie
     * @see #saveState
     * @see #reset
     */
    public BytesTrie resetToState(State state) {
        if(bytes_==state.bytes && bytes_!=null) {
            pos_=state.pos;
            remainingMatchLength_=state.remainingMatchLength;
        } else {
            throw new IllegalArgumentException("incompatible trie state");
        }
        return this;
    }

    /**
     * Return values for BytesTrie.next(), UCharsTrie.next() and similar methods.
     */
    public enum Result {
        /**
         * The input unit(s) did not continue a matching string.
         */
        NO_MATCH,
        /**
         * The input unit(s) continued a matching string
         * but there is no value for the string so far.
         * (It is a prefix of a longer string.)
         */
        NO_VALUE,
        /**
         * The input unit(s) continued a matching string
         * and there is a value for the string so far.
         * This value will be returned by getValue().
         * No further input byte/unit can continue a matching string.
         */
        FINAL_VALUE,
        /**
         * The input unit(s) continued a matching string
         * and there is a value for the string so far.
         * This value will be returned by getValue().
         * Another input byte/unit can continue a matching string.
         */
        INTERMEDIATE_VALUE;

        /**
         * Same as (result!=NO_MATCH).
         * @return true if the input bytes/units so far are part of a matching string/byte sequence.
         */
        public boolean matches() { return ordinal()!=0; }

        /**
         * Equivalent to (result==INTERMEDIATE_VALUE || result==FINAL_VALUE).
         * @return true if there is a value for the input bytes/units so far.
         * @see #getValue
         */
        public boolean hasValue() { return ordinal()>=2; }

        /**
         * Equivalent to (result==NO_VALUE || result==INTERMEDIATE_VALUE).
         * @return true if another input byte/unit can continue a matching string.
         */
        public boolean hasNext() { return (ordinal()&1)!=0; }
    }

    /**
     * Determines whether the byte sequence so far matches, whether it has a value,
     * and whether another input byte can continue a matching byte sequence.
     * @return The match/value Result.
     */
    public Result current() /*const*/ {
        int pos=pos_;
        if(pos<0) {
            return Result.NO_MATCH;
        } else {
            int node;
            return (remainingMatchLength_<0 && (node=bytes_[pos]&0xff)>=kMinValueLead) ?
                    valueResults_[node&kValueIsFinal] : Result.NO_VALUE;
        }
    }

    /**
     * Traverses the trie from the initial state for this input byte.
     * Equivalent to reset().next(inByte).
     * @return The match/value Result.
     */
    public Result first(int inByte) {
        remainingMatchLength_=-1;
        return nextImpl(root_, inByte);
    }

    /**
     * Traverses the trie from the current state for this input byte.
     * @return The match/value Result.
     */
    public Result next(int inByte) {
        int pos=pos_;
        if(pos<0) {
            return Result.NO_MATCH;
        }
        int length=remainingMatchLength_;  // Actual remaining match length minus 1.
        if(length>=0) {
            // Remaining part of a linear-match node.
            if(inByte==(bytes_[pos++]&0xff)) {
                remainingMatchLength_=--length;
                pos_=pos;
                int node;
                return (length<0 && (node=bytes_[pos]&0xff)>=kMinValueLead) ?
                        valueResults_[node&kValueIsFinal] : Result.NO_VALUE;
            } else {
                stop();
                return Result.NO_MATCH;
            }
        }
        return nextImpl(pos, inByte);
    }

    /**
     * Traverses the trie from the current state for this byte sequence.
     * Equivalent to
     * <pre>
     * Result result=current();
     * for(each c in s)
     *   if(!result.hasNext()) return Result.NO_MATCH;
     *   result=next(c);
     * return result;
     * </pre>
     * @return The match/value Result.
     */
    // public Result next(const char *s, int length);

    /**
     * Returns a matching byte sequence's value if called immediately after
     * current()/first()/next() returned Result.INTERMEDIATE_VALUE or Result.FINAL_VALUE.
     * getValue() can be called multiple times.
     *
     * Do not call getValue() after Result.NO_MATCH or Result.NO_VALUE!
     */
    public int getValue() /*const*/ {
        int pos=pos_;
        int leadByte=bytes_[pos++]&0xff;
        assert(leadByte>=kMinValueLead);
        return readValue(bytes_, pos, leadByte>>1);
    }

    /**
     * Determines whether all byte sequences reachable from the current state
     * map to the same value, and if so, returns that value.
     * @return the unique value in bits 32..1 with bit 0 set,
     *         if all byte sequences reachable from the current state
     *         map to the same value; otherwise returns 0.
     */
    public long getUniqueValue() /*const*/ {
        int pos=pos_;
        if(pos<0) {
            return 0;
        }
        // Skip the rest of a pending linear-match node.
        long uniqueValue=findUniqueValue(bytes_, pos+remainingMatchLength_+1, 0);
        // Ignore internally used bits 63..33; extend the actual value's sign bit from bit 32.
        return (uniqueValue<<31)>>31;
    }

    /**
     * Finds each byte which continues the byte sequence from the current state.
     * That is, each byte b for which it would be next(b)!=Result.NO_MATCH now.
     * @param out Each next byte is 0-extended to a char and appended to this object.
     *            (Only uses the out.append(c) method.)
     * @return the number of bytes which continue the byte sequence from here
     */
    public int getNextBytes(Appendable out) /*const*/ {
        int pos=pos_;
        if(pos<0) {
            return 0;
        }
        if(remainingMatchLength_>=0) {
            append(out, bytes_[pos]&0xff);  // Next byte of a pending linear-match node.
            return 1;
        }
        int node=bytes_[pos++]&0xff;
        if(node>=kMinValueLead) {
            if((node&kValueIsFinal)!=0) {
                return 0;
            } else {
                pos=skipValue(pos, node);
                node=bytes_[pos++]&0xff;
                assert(node<kMinValueLead);
            }
        }
        if(node<kMinLinearMatch) {
            if(node==0) {
                node=bytes_[pos++]&0xff;
            }
            getNextBranchBytes(bytes_, pos, ++node, out);
            return node;
        } else {
            // First byte of the linear-match node.
            append(out, bytes_[pos]&0xff);
            return 1;
        }
    }

    private void stop() {
        pos_=-1;
    }

    // Reads a compact 32-bit integer.
    // pos is already after the leadByte, and the lead byte is already shifted right by 1.
    private static int readValue(byte[] bytes, int pos, int leadByte) {
        int value;
        if(leadByte<kMinTwoByteValueLead) {
            value=leadByte-kMinOneByteValueLead;
        } else if(leadByte<kMinThreeByteValueLead) {
            value=((leadByte-kMinTwoByteValueLead)<<8)|(bytes[pos]&0xff);
        } else if(leadByte<kFourByteValueLead) {
            value=((leadByte-kMinThreeByteValueLead)<<16)|((bytes[pos]&0xff)<<8)|(bytes[pos+1]&0xff);
        } else if(leadByte==kFourByteValueLead) {
            value=((bytes[pos]&0xff)<<16)|((bytes[pos+1]&0xff)<<8)|(bytes[pos+2]&0xff);
        } else {
            value=(bytes[pos]<<24)|((bytes[pos+1]&0xff)<<16)|((bytes[pos+2]&0xff)<<8)|(bytes[pos+3]&0xff);
        }
        return value;
    }
    private static int skipValue(int pos, int leadByte) {
        assert(leadByte>=kMinValueLead);
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
    private static int skipValue(byte[] bytes, int pos) {
        int leadByte=bytes[pos++]&0xff;
        return skipValue(pos, leadByte);
    }

    // Reads a jump delta and jumps.
    private static int jumpByDelta(byte[] bytes, int pos) {
        int delta=bytes[pos++]&0xff;
        if(delta<kMinTwoByteDeltaLead) {
            // nothing to do
        } else if(delta<kMinThreeByteDeltaLead) {
            delta=((delta-kMinTwoByteDeltaLead)<<8)|(bytes[pos++]&0xff);
        } else if(delta<kFourByteDeltaLead) {
            delta=((delta-kMinThreeByteDeltaLead)<<16)|((bytes[pos]&0xff)<<8)|(bytes[pos+1]&0xff);
            pos+=2;
        } else if(delta==kFourByteDeltaLead) {
            delta=((bytes[pos]&0xff)<<16)|((bytes[pos+1]&0xff)<<8)|(bytes[pos+2]&0xff);
            pos+=3;
        } else {
            delta=(bytes[pos]<<24)|((bytes[pos+1]&0xff)<<16)|((bytes[pos+2]&0xff)<<8)|(bytes[pos+3]&0xff);
            pos+=4;
        }
        return pos+delta;
    }

    private static int skipDelta(byte[] bytes, int pos) {
        int delta=bytes[pos++]&0xff;
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

    private static Result[] valueResults_={ Result.INTERMEDIATE_VALUE, Result.FINAL_VALUE };

    // Handles a branch node for both next(byte) and next(string).
    private Result branchNext(int pos, int length, int inByte) {
        // Branch according to the current byte.
        if(length==0) {
            length=bytes_[pos++]&0xff;
        }
        ++length;
        // The length of the branch is the number of bytes to select from.
        // The data structure encodes a binary search.
        while(length>kMaxBranchLinearSubNodeLength) {
            if(inByte<(bytes_[pos++]&0xff)) {
                length>>=1;
                pos=jumpByDelta(bytes_, pos);
            } else {
                length=length-(length>>1);
                pos=skipDelta(bytes_, pos);
            }
        }
        // Drop down to linear search for the last few bytes.
        // length>=2 because the loop body above sees length>kMaxBranchLinearSubNodeLength>=3
        // and divides length by 2.
        do {
            if(inByte==(bytes_[pos++]&0xff)) {
                Result result;
                int node=bytes_[pos]&0xff;
                assert(node>=kMinValueLead);
                if((node&kValueIsFinal)!=0) {
                    // Leave the final value for getValue() to read.
                    result=Result.FINAL_VALUE;
                } else {
                    // Use the non-final value as the jump delta.
                    ++pos;
                    // int delta=readValue(pos, node>>1);
                    node>>=1;
                    int delta;
                    if(node<kMinTwoByteValueLead) {
                        delta=node-kMinOneByteValueLead;
                    } else if(node<kMinThreeByteValueLead) {
                        delta=((node-kMinTwoByteValueLead)<<8)|(bytes_[pos++]&0xff);
                    } else if(node<kFourByteValueLead) {
                        delta=((node-kMinThreeByteValueLead)<<16)|((bytes_[pos]&0xff)<<8)|(bytes_[pos+1]&0xff);
                        pos+=2;
                    } else if(node==kFourByteValueLead) {
                        delta=((bytes_[pos]&0xff)<<16)|((bytes_[pos+1]&0xff)<<8)|(bytes_[pos+2]&0xff);
                        pos+=3;
                    } else {
                        delta=(bytes_[pos]<<24)|((bytes_[pos+1]&0xff)<<16)|((bytes_[pos+2]&0xff)<<8)|(bytes_[pos+3]&0xff);
                        pos+=4;
                    }
                    // end readValue()
                    pos+=delta;
                    node=bytes_[pos]&0xff;
                    result= node>=kMinValueLead ? valueResults_[node&kValueIsFinal] : Result.NO_VALUE;
                }
                pos_=pos;
                return result;
            }
            --length;
            pos=skipValue(bytes_, pos);
        } while(length>1);
        if(inByte==(bytes_[pos++]&0xff)) {
            pos_=pos;
            int node=bytes_[pos]&0xff;
            return node>=kMinValueLead ? valueResults_[node&kValueIsFinal] : Result.NO_VALUE;
        } else {
            stop();
            return Result.NO_MATCH;
        }
    }

    // Requires remainingLength_<0.
    private Result nextImpl(int pos, int inByte) {
        for(;;) {
            int node=bytes_[pos++]&0xff;
            if(node<kMinLinearMatch) {
                return branchNext(pos, node, inByte);
            } else if(node<kMinValueLead) {
                // Match the first of length+1 bytes.
                int length=node-kMinLinearMatch;  // Actual match length minus 1.
                if(inByte==(bytes_[pos++]&0xff)) {
                    remainingMatchLength_=--length;
                    pos_=pos;
                    return (length<0 && (node=bytes_[pos]&0xff)>=kMinValueLead) ?
                            valueResults_[node&kValueIsFinal] : Result.NO_VALUE;
                } else {
                    // No match.
                    break;
                }
            } else if((node&kValueIsFinal)!=0) {
                // No further matching bytes.
                break;
            } else {
                // Skip intermediate value.
                pos=skipValue(pos, node);
                // The next node must not also be a value node.
                assert((bytes_[pos]&0xff)<kMinValueLead);
            }
        }
        stop();
        return Result.NO_MATCH;
    }

    // Helper functions for hasUniqueValue().
    // Recursively finds a unique value (or whether there is not a unique one)
    // from a branch.
    // uniqueValue: On input, same as for getUniqueValue()/findUniqueValue().
    // On return, if not 0, then bits 63..33 contain the updated non-negative pos.
    private static long findUniqueValueFromBranch(byte[] bytes, int pos, int length,
                                                  long uniqueValue) {
        while(length>kMaxBranchLinearSubNodeLength) {
            ++pos;  // ignore the comparison byte
            uniqueValue=findUniqueValueFromBranch(bytes, jumpByDelta(bytes, pos), length>>1, uniqueValue);
            if(uniqueValue==0) {
                return 0;
            }
            length=length-(length>>1);
            pos=skipDelta(bytes, pos);
        }
        do {
            ++pos;  // ignore a comparison byte
            // handle its value
            int node=bytes[pos++]&0xff;
            boolean isFinal=(node&kValueIsFinal)!=0;
            int value=readValue(bytes, pos, node>>1);
            pos=skipValue(pos, node);
            if(isFinal) {
                if(uniqueValue!=0) {
                    if(value!=(int)(uniqueValue>>1)) {
                        return 0;
                    }
                } else {
                    uniqueValue=((long)value<<1)|1;
                }
            } else {
                uniqueValue=findUniqueValue(bytes, pos+value, uniqueValue);
                if(uniqueValue==0) {
                    return 0;
                }
            }
        } while(--length>1);
        // ignore the last comparison byte
        return ((long)(pos+1)<<33)|(uniqueValue&0x1ffffffffL);
    }
    // Recursively finds a unique value (or whether there is not a unique one)
    // starting from a position on a node lead byte.
    // uniqueValue: If there is one, then bits 32..1 contain the value and bit 0 is set.
    // Otherwise, uniqueValue is 0. Bits 63..33 are ignored.
    private static long findUniqueValue(byte[] bytes, int pos, long uniqueValue) {
        for(;;) {
            int node=bytes[pos++]&0xff;
            if(node<kMinLinearMatch) {
                if(node==0) {
                    node=bytes[pos++]&0xff;
                }
                uniqueValue=findUniqueValueFromBranch(bytes, pos, node+1, uniqueValue);
                if(uniqueValue==0) {
                    return 0;
                }
                pos=(int)(uniqueValue>>>33);
            } else if(node<kMinValueLead) {
                // linear-match node
                pos+=node-kMinLinearMatch+1;  // Ignore the match bytes.
            } else {
                boolean isFinal=(node&kValueIsFinal)!=0;
                int value=readValue(bytes, pos, node>>1);
                if(uniqueValue!=0) {
                    if(value!=(int)(uniqueValue>>1)) {
                        return 0;
                    }
                } else {
                    uniqueValue=((long)value<<1)|1;
                }
                if(isFinal) {
                    return uniqueValue;
                }
                pos=skipValue(pos, node);
            }
        }
    }

    // Helper functions for getNextBytes().
    // getNextBytes() when pos is on a branch node.
    private static void getNextBranchBytes(byte[] bytes, int pos, int length, Appendable out) {
        while(length>kMaxBranchLinearSubNodeLength) {
            ++pos;  // ignore the comparison byte
            getNextBranchBytes(bytes, jumpByDelta(bytes, pos), length>>1, out);
            length=length-(length>>1);
            pos=skipDelta(bytes, pos);
        }
        do {
            append(out, bytes[pos++]&0xff);
            pos=skipValue(bytes, pos);
        } while(--length>1);
        append(out, bytes[pos]&0xff);
    }
    private static void append(Appendable out, int c) {
        try {
            out.append((char)c);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

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
    private static final int kMaxBranchLinearSubNodeLength=5;

    // 10..1f: Linear-match node, match 1..16 bytes and continue reading the next node.
    private static final int kMinLinearMatch=0x10;
    private static final int kMaxLinearMatchLength=0x10;

    // 20..ff: Variable-length value node.
    // If odd, the value is final. (Otherwise, intermediate value or jump delta.)
    // Then shift-right by 1 bit.
    // The remaining lead byte value indicates the number of following bytes (0..4)
    // and contains the value's top bits.
    private static final int kMinValueLead=kMinLinearMatch+kMaxLinearMatchLength;  // 0x20
    // It is a final value if bit 0 is set.
    private static final int kValueIsFinal=1;

    // Compact value: After testing bit 0, shift right by 1 and then use the following thresholds.
    private static final int kMinOneByteValueLead=kMinValueLead/2;  // 0x10
    private static final int kMaxOneByteValue=0x40;  // At least 6 bits in the first byte.

    private static final int kMinTwoByteValueLead=kMinOneByteValueLead+kMaxOneByteValue+1;  // 0x51
    private static final int kMaxTwoByteValue=0x1aff;

    private static final int kMinThreeByteValueLead=kMinTwoByteValueLead+(kMaxTwoByteValue>>8)+1;  // 0x6c
    private static final int kFourByteValueLead=0x7e;

    // A little more than Unicode code points. (0x11ffff)
    /*package*/ static final int kMaxThreeByteValue=((kFourByteValueLead-kMinThreeByteValueLead)<<16)-1;

    /*package*/ static final int kFiveByteValueLead=0x7f;

    // Compact delta integers.
    private static final int kMaxOneByteDelta=0xbf;
    private static final int kMinTwoByteDeltaLead=kMaxOneByteDelta+1;  // 0xc0
    private static final int kMinThreeByteDeltaLead=0xf0;
    private static final int kFourByteDeltaLead=0xfe;
    /*package*/ static final int kFiveByteDeltaLead=0xff;

    /*package*/ static final int kMaxTwoByteDelta=((kMinThreeByteDeltaLead-kMinTwoByteDeltaLead)<<8)-1;  // 0x2fff
    /*package*/ static final int kMaxThreeByteDelta=((kFourByteDeltaLead-kMinThreeByteDeltaLead)<<16)-1;  // 0xdffff

    // Fixed value referencing the BytesTrie bytes.
    private byte[] bytes_;
    private int root_;

    // Iterator variables.

    // Index of next trie byte to read. Negative if no more matches.
    private int pos_;
    // Remaining length of a linear-match node, minus 1. Negative if not in such a node.
    private int remainingMatchLength_;
};
