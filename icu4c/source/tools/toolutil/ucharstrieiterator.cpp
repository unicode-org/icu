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

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "ucharstrie.h"
#include "ucharstrieiterator.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

UCharsTrieIterator::UCharsTrieIterator(const UChar *trieUChars, int32_t maxStringLength,
                                       UErrorCode &errorCode)
        : uchars_(trieUChars),
          pos_(uchars_), initialPos_(uchars_),
          remainingMatchLength_(-1), initialRemainingMatchLength_(-1),
          skipValue_(FALSE),
          maxLength_(maxStringLength), value_(0), stack_(errorCode) {}

UCharsTrieIterator::UCharsTrieIterator(const UCharsTrie &trie, int32_t maxStringLength,
                                       UErrorCode &errorCode)
        : uchars_(trie.uchars_), pos_(trie.pos_), initialPos_(trie.pos_),
          remainingMatchLength_(trie.remainingMatchLength_),
          initialRemainingMatchLength_(trie.remainingMatchLength_),
          skipValue_(FALSE),
          maxLength_(maxStringLength), value_(0), stack_(errorCode) {
    int32_t length=remainingMatchLength_;  // Actual remaining match length minus 1.
    if(length>=0) {
        // Pending linear-match node, append remaining UChars to str.
        ++length;
        if(maxLength_>0 && length>maxLength_) {
            length=maxLength_;  // This will leave remainingMatchLength>=0 as a signal.
        }
        str_.append(pos_, length);
        pos_+=length;
        remainingMatchLength_-=length;
    }
}

UCharsTrieIterator &UCharsTrieIterator::reset() {
    pos_=initialPos_;
    remainingMatchLength_=initialRemainingMatchLength_;
    skipValue_=FALSE;
    int32_t length=remainingMatchLength_+1;  // Remaining match length.
    if(maxLength_>0 && length>maxLength_) {
        length=maxLength_;
    }
    str_.truncate(length);
    pos_+=length;
    remainingMatchLength_-=length;
    stack_.setSize(0);
    return *this;
}

UBool
UCharsTrieIterator::next(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return FALSE;
    }
    const UChar *pos=pos_;
    if(pos==NULL) {
        if(stack_.isEmpty()) {
            return FALSE;
        }
        // Pop the state off the stack and continue with the next outbound edge of
        // the branch node.
        int32_t stackSize=stack_.size();
        int32_t length=stack_.elementAti(stackSize-1);
        pos=uchars_+stack_.elementAti(stackSize-2);
        stack_.setSize(stackSize-2);
        str_.truncate(length&0xffff);
        length=(int32_t)((uint32_t)length>>16);
        if(length>1) {
            pos=branchNext(pos, length, errorCode);
            if(pos==NULL) {
                return TRUE;  // Reached a final value.
            }
        } else {
            str_.append(*pos++);
        }
    }
    if(remainingMatchLength_>=0) {
        // We only get here if we started in a pending linear-match node
        // with more than maxLength remaining units.
        return truncateAndStop();
    }
    for(;;) {
        int32_t node=*pos++;
        if(node>=UCharsTrie::kMinValueLead) {
            if(skipValue_) {
                pos=UCharsTrie::skipNodeValue(pos, node);
                node&=UCharsTrie::kNodeTypeMask;
                skipValue_=FALSE;
            } else {
                // Deliver value for the string so far.
                UBool isFinal=(UBool)(node>>15);
                if(isFinal) {
                    value_=UCharsTrie::readValue(pos, node&0x7fff);
                } else {
                    value_=UCharsTrie::readNodeValue(pos, node);
                }
                if(isFinal || (maxLength_>0 && str_.length()==maxLength_)) {
                    pos_=NULL;
                } else {
                    // We cannot skip the value right here because it shares its
                    // lead unit with a match node which we have to evaluate
                    // next time.
                    // Instead, keep pos_ on the node lead unit itself.
                    pos_=pos-1;
                    skipValue_=TRUE;
                }
                return TRUE;
            }
        }
        if(maxLength_>0 && str_.length()==maxLength_) {
            return truncateAndStop();
        }
        if(node<UCharsTrie::kMinLinearMatch) {
            if(node==0) {
                node=*pos++;
            }
            pos=branchNext(pos, node+1, errorCode);
            if(pos==NULL) {
                return TRUE;  // Reached a final value.
            }
        } else {
            // Linear-match node, append length units to str_.
            int32_t length=node-UCharsTrie::kMinLinearMatch+1;
            if(maxLength_>0 && str_.length()+length>maxLength_) {
                str_.append(pos, maxLength_-str_.length());
                return truncateAndStop();
            }
            str_.append(pos, length);
            pos+=length;
        }
    }
}

// Branch node, needs to take the first outbound edge and push state for the rest.
const UChar *
UCharsTrieIterator::branchNext(const UChar *pos, int32_t length, UErrorCode &errorCode) {
    while(length>UCharsTrie::kMaxBranchLinearSubNodeLength) {
        ++pos;  // ignore the comparison unit
        // Push state for the greater-or-equal edge.
        stack_.addElement((int32_t)(UCharsTrie::skipDelta(pos)-uchars_), errorCode);
        stack_.addElement(((length-(length>>1))<<16)|str_.length(), errorCode);
        // Follow the less-than edge.
        length>>=1;
        pos=UCharsTrie::jumpByDelta(pos);
    }
    // List of key-value pairs where values are either final values or jump deltas.
    // Read the first (key, value) pair.
    UChar trieUnit=*pos++;
    int32_t node=*pos++;
    UBool isFinal=(UBool)(node>>15);
    int32_t value=UCharsTrie::readValue(pos, node&=0x7fff);
    pos=UCharsTrie::skipValue(pos, node);
    stack_.addElement((int32_t)(pos-uchars_), errorCode);
    stack_.addElement(((length-1)<<16)|str_.length(), errorCode);
    str_.append(trieUnit);
    if(isFinal) {
        pos_=NULL;
        value_=value;
        return NULL;
    } else {
        return pos+value;
    }
}

U_NAMESPACE_END
