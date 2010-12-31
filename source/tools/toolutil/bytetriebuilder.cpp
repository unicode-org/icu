/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  bytetriebuilder.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010sep25
*   created by: Markus W. Scherer
*
* Builder class for ByteTrie dictionary trie.
*/

#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "bytetrie.h"
#include "bytetriebuilder.h"
#include "charstr.h"
#include "cmemory.h"
#include "uarrsort.h"

U_NAMESPACE_BEGIN

/*
 * Note: This builder implementation stores (bytes, value) pairs with full copies
 * of the byte sequences, until the ByteTrie is built.
 * It might(!) take less memory if we collected the data in a temporary, dynamic trie.
 */

class ByteTrieElement : public UMemory {
public:
    // Use compiler's default constructor, initializes nothing.

    void setTo(const StringPiece &s, int32_t val, CharString &strings, UErrorCode &errorCode);

    StringPiece getString(const CharString &strings) const {
        int32_t offset=stringOffset;
        int32_t length;
        if(offset>=0) {
            length=(uint8_t)strings[offset++];
        } else {
            offset=~offset;
            length=((int32_t)(uint8_t)strings[offset]<<8)|(uint8_t)strings[offset+1];
            offset+=2;
        }
        return StringPiece(strings.data()+offset, length);
    }
    int32_t getStringLength(const CharString &strings) const {
        int32_t offset=stringOffset;
        if(offset>=0) {
            return (uint8_t)strings[offset];
        } else {
            offset=~offset;
            return ((int32_t)(uint8_t)strings[offset]<<8)|(uint8_t)strings[offset+1];
        }
    }

    char charAt(int32_t index, const CharString &strings) const { return data(strings)[index]; }

    int32_t getValue() const { return value; }

    int32_t compareStringTo(const ByteTrieElement &o, const CharString &strings) const;

private:
    const char *data(const CharString &strings) const {
        int32_t offset=stringOffset;
        if(offset>=0) {
            ++offset;
        } else {
            offset=~offset+2;
        }
        return strings.data()+offset;
    }

    // If the stringOffset is non-negative, then the first strings byte contains
    // the string length.
    // If the stringOffset is negative, then the first two strings bytes contain
    // the string length (big-endian), and the offset needs to be bit-inverted.
    // (Compared with a stringLength field here, this saves 3 bytes per string for most strings.)
    int32_t stringOffset;
    int32_t value;
};

void
ByteTrieElement::setTo(const StringPiece &s, int32_t val,
                       CharString &strings, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return;
    }
    int32_t length=s.length();
    if(length>0xffff) {
        // Too long: We store the length in 1 or 2 bytes.
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }
    int32_t offset=strings.length();
    if(length>0xff) {
        offset=~offset;
        strings.append((char)(length>>8), errorCode);
    }
    strings.append((char)length, errorCode);
    stringOffset=offset;
    value=val;
    strings.append(s, errorCode);
}

int32_t
ByteTrieElement::compareStringTo(const ByteTrieElement &other, const CharString &strings) const {
    // TODO: add StringPiece::compare(), see ticket #8187
    StringPiece thisString=getString(strings);
    StringPiece otherString=other.getString(strings);
    int32_t lengthDiff=thisString.length()-otherString.length();
    int32_t commonLength;
    if(lengthDiff<=0) {
        commonLength=thisString.length();
    } else {
        commonLength=otherString.length();
    }
    int32_t diff=uprv_memcmp(thisString.data(), otherString.data(), commonLength);
    return diff!=0 ? diff : lengthDiff;
}

ByteTrieBuilder::~ByteTrieBuilder() {
    delete[] elements;
    uprv_free(bytes);
}

ByteTrieBuilder &
ByteTrieBuilder::add(const StringPiece &s, int32_t value, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return *this;
    }
    if(bytesLength>0) {
        // Cannot add elements after building.
        errorCode=U_NO_WRITE_PERMISSION;
        return *this;
    }
    bytesCapacity+=s.length()+1;  // Crude bytes preallocation estimate.
    if(elementsLength==elementsCapacity) {
        int32_t newCapacity;
        if(elementsCapacity==0) {
            newCapacity=1024;
        } else {
            newCapacity=4*elementsCapacity;
        }
        ByteTrieElement *newElements=new ByteTrieElement[newCapacity];
        if(newElements==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
        }
        if(elementsLength>0) {
            uprv_memcpy(newElements, elements, elementsLength*sizeof(ByteTrieElement));
        }
        delete[] elements;
        elements=newElements;
        elementsCapacity=newCapacity;
    }
    elements[elementsLength++].setTo(s, value, strings, errorCode);
    return *this;
}

U_CDECL_BEGIN

static int32_t U_CALLCONV
compareElementStrings(const void *context, const void *left, const void *right) {
    const CharString *strings=reinterpret_cast<const CharString *>(context);
    const ByteTrieElement *leftElement=reinterpret_cast<const ByteTrieElement *>(left);
    const ByteTrieElement *rightElement=reinterpret_cast<const ByteTrieElement *>(right);
    return leftElement->compareStringTo(*rightElement, *strings);
}

U_CDECL_END

StringPiece
ByteTrieBuilder::build(UDictTrieBuildOption buildOption, UErrorCode &errorCode) {
    StringPiece result;
    if(U_FAILURE(errorCode)) {
        return result;
    }
    if(bytesLength>0) {
        // Already built.
        result.set(bytes+(bytesCapacity-bytesLength), bytesLength);
        return result;
    }
    if(elementsLength==0) {
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return result;
    }
    uprv_sortArray(elements, elementsLength, (int32_t)sizeof(ByteTrieElement),
                   compareElementStrings, &strings,
                   FALSE,  // need not be a stable sort
                   &errorCode);
    if(U_FAILURE(errorCode)) {
        return result;
    }
    // Duplicate strings are not allowed.
    StringPiece prev=elements[0].getString(strings);
    for(int32_t i=1; i<elementsLength; ++i) {
        StringPiece current=elements[i].getString(strings);
        if(prev==current) {
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return result;
        }
        prev=current;
    }
    // Create and byte-serialize the trie for the elements.
    if(bytesCapacity<1024) {
        bytesCapacity=1024;
    }
    bytes=reinterpret_cast<char *>(uprv_malloc(bytesCapacity));
    if(bytes==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return result;
    }
    if(buildOption==UDICTTRIE_BUILD_FAST) {
        writeNode(0, elementsLength, 0);
    } else /* UDICTTRIE_BUILD_SMALL */ {
        createCompactBuilder(2*elementsLength, errorCode);
        Node *root=makeNode(0, elementsLength, 0, errorCode);
        if(U_SUCCESS(errorCode)) {
            root->markRightEdgesFirst(-1);
            root->write(*this);
        }
        deleteCompactBuilder();
    }
    if(bytes==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    } else {
        result.set(bytes+(bytesCapacity-bytesLength), bytesLength);
    }
    return result;
}

// Requires start<limit,
// and all strings of the [start..limit[ elements must be sorted and
// have a common prefix of length byteIndex.
void
ByteTrieBuilder::writeNode(int32_t start, int32_t limit, int32_t byteIndex) {
    UBool hasValue=FALSE;
    int32_t value=0;
    if(byteIndex==elements[start].getStringLength(strings)) {
        // An intermediate or final value.
        value=elements[start++].getValue();
        if(start==limit) {
            writeValueAndFinal(value, TRUE);  // final-value node
            return;
        }
        hasValue=TRUE;
    }
    // Now all [start..limit[ strings are longer than byteIndex.
    const ByteTrieElement &minElement=elements[start];
    const ByteTrieElement &maxElement=elements[limit-1];
    int32_t minByte=(uint8_t)minElement.charAt(byteIndex, strings);
    int32_t maxByte=(uint8_t)maxElement.charAt(byteIndex, strings);
    if(minByte==maxByte) {
        // Linear-match node: All strings have the same character at byteIndex.
        int32_t minStringLength=minElement.getStringLength(strings);
        int32_t lastByteIndex=byteIndex;
        while(++lastByteIndex<minStringLength &&
                minElement.charAt(lastByteIndex, strings)==
                maxElement.charAt(lastByteIndex, strings)) {}
        writeNode(start, limit, lastByteIndex);
        // Break the linear-match sequence into chunks of at most kMaxLinearMatchLength.
        const char *s=minElement.getString(strings).data();
        int32_t length=lastByteIndex-byteIndex;
        while(length>ByteTrie::kMaxLinearMatchLength) {
            lastByteIndex-=ByteTrie::kMaxLinearMatchLength;
            length-=ByteTrie::kMaxLinearMatchLength;
            write(s+lastByteIndex, ByteTrie::kMaxLinearMatchLength);
            write(ByteTrie::kMinLinearMatch+ByteTrie::kMaxLinearMatchLength-1);
        }
        write(s+byteIndex, length);
        write(ByteTrie::kMinLinearMatch+length-1);
    } else {
        // Branch node.
        int32_t length=0;  // Number of different bytes at byteIndex.
        int32_t i=start;
        do {
            char byte=elements[i++].charAt(byteIndex, strings);
            while(i<limit && byte==elements[i].charAt(byteIndex, strings)) {
                ++i;
            }
            ++length;
        } while(i<limit);
        // length>=2 because minByte!=maxByte.
        writeBranchSubNode(start, limit, byteIndex, length);
        write(--length);
        if(length>=ByteTrie::kMinLinearMatch) {
            write(0);
        }
    }
    if(hasValue) {
        writeValueAndFinal(value, FALSE);
    }
}

// start<limit && all strings longer than byteIndex &&
// length different bytes at byteIndex
void
ByteTrieBuilder::writeBranchSubNode(int32_t start, int32_t limit, int32_t byteIndex, int32_t length) {
    char middleBytes[16];
    int32_t lessThan[16];
    int32_t ltLength=0;
    while(length>ByteTrie::kMaxBranchLinearSubNodeLength) {
        // Branch on the middle byte.
        // First, find the middle byte.
        int32_t count=length/2;
        int32_t i=start;
        char byte;
        do {
            byte=elements[i++].charAt(byteIndex, strings);
            while(byte==elements[i].charAt(byteIndex, strings)) {
                ++i;
            }
        } while(--count>0);
        // Encode the less-than branch first.
        byte=middleBytes[ltLength]=elements[i].charAt(byteIndex, strings);  // middle byte
        writeBranchSubNode(start, i, byteIndex, length/2);
        lessThan[ltLength]=bytesLength;
        ++ltLength;
        // Continue for the greater-or-equal branch.
        start=i;
        length=length-length/2;
    }
    // For each byte, find its elements array start and whether it has a final value.
    int32_t starts[ByteTrie::kMaxBranchLinearSubNodeLength];
    UBool final[ByteTrie::kMaxBranchLinearSubNodeLength-1];
    int32_t byteNumber=0;
    do {
        int32_t i=starts[byteNumber]=start;
        char byte=elements[i++].charAt(byteIndex, strings);
        while(byte==elements[i].charAt(byteIndex, strings)) {
            ++i;
        }
        final[byteNumber]= start==i-1 && byteIndex+1==elements[start].getStringLength(strings);
        start=i;
    } while(++byteNumber<length-1);
    // byteNumber==length-1, and the maxByte elements range is [start..limit[
    starts[byteNumber]=start;

    // Write the sub-nodes in reverse order: The jump lengths are deltas from
    // after their own positions, so if we wrote the minByte sub-node first,
    // then its jump delta would be larger.
    // Instead we write the minByte sub-node last, for a shorter delta.
    int32_t jumpTargets[ByteTrie::kMaxBranchLinearSubNodeLength-1];
    do {
        --byteNumber;
        if(!final[byteNumber]) {
            writeNode(starts[byteNumber], starts[byteNumber+1], byteIndex+1);
            jumpTargets[byteNumber]=bytesLength;
        }
    } while(byteNumber>0);
    // The maxByte sub-node is written as the very last one because we do
    // not jump for it at all.
    byteNumber=length-1;
    writeNode(start, limit, byteIndex+1);
    write((uint8_t)elements[start].charAt(byteIndex, strings));
    // Write the rest of this node's byte-value pairs.
    while(--byteNumber>=0) {
        start=starts[byteNumber];
        int32_t value;
        if(final[byteNumber]) {
            // Write the final value for the one string ending with this byte.
            value=elements[start].getValue();
        } else {
            // Write the delta to the start position of the sub-node.
            value=bytesLength-jumpTargets[byteNumber];
        }
        writeValueAndFinal(value, final[byteNumber]);
        write((uint8_t)elements[start].charAt(byteIndex, strings));
    }
    // Write the split-branch nodes.
    while(ltLength>0) {
        --ltLength;
        writeDelta(bytesLength-lessThan[ltLength]);  // less-than
        write((uint8_t)middleBytes[ltLength]);
    }
}

// Requires start<limit,
// and all strings of the [start..limit[ elements must be sorted and
// have a common prefix of length byteIndex.
DictTrieBuilder::Node *
ByteTrieBuilder::makeNode(int32_t start, int32_t limit, int32_t byteIndex, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    UBool hasValue=FALSE;
    int32_t value=0;
    if(byteIndex==elements[start].getStringLength(strings)) {
        // An intermediate or final value.
        value=elements[start++].getValue();
        if(start==limit) {
            return registerFinalValue(value, errorCode);
        }
        hasValue=TRUE;
    }
    Node *node;
    // Now all [start..limit[ strings are longer than byteIndex.
    const ByteTrieElement &minElement=elements[start];
    const ByteTrieElement &maxElement=elements[limit-1];
    int32_t minByte=(uint8_t)minElement.charAt(byteIndex, strings);
    int32_t maxByte=(uint8_t)maxElement.charAt(byteIndex, strings);
    if(minByte==maxByte) {
        // Linear-match node: All strings have the same character at byteIndex.
        int32_t minStringLength=minElement.getStringLength(strings);
        int32_t lastByteIndex=byteIndex;
        while(++lastByteIndex<minStringLength &&
                minElement.charAt(lastByteIndex, strings)==
                maxElement.charAt(lastByteIndex, strings)) {}
        Node *nextNode=makeNode(start, limit, lastByteIndex, errorCode);
        // Break the linear-match sequence into chunks of at most kMaxLinearMatchLength.
        const char *s=minElement.getString(strings).data();
        int32_t length=lastByteIndex-byteIndex;
        while(length>ByteTrie::kMaxLinearMatchLength) {
            lastByteIndex-=ByteTrie::kMaxLinearMatchLength;
            length-=ByteTrie::kMaxLinearMatchLength;
            node=new BTLinearMatchNode(
                s+lastByteIndex,
                ByteTrie::kMaxLinearMatchLength,
                nextNode);
            node=registerNode(node, errorCode);
            nextNode=node;
        }
        node=new BTLinearMatchNode(s+byteIndex, length, nextNode);
    } else {
        // Branch node.
        int32_t length=0;  // Number of different bytes at byteIndex.
        int32_t i=start;
        do {
            char byte=elements[i++].charAt(byteIndex, strings);
            while(i<limit && byte==elements[i].charAt(byteIndex, strings)) {
                ++i;
            }
            ++length;
        } while(i<limit);
        // length>=2 because minByte!=maxByte.
        Node *subNode=makeBranchSubNode(start, limit, byteIndex, length, errorCode);
        node=new BTBranchHeadNode(length, subNode);
    }
    node=registerNode(node, errorCode);
    if(hasValue) {
        node=registerNode(new BTValueNode(value, node), errorCode);
    }
    return node;
}

// start<limit && all strings longer than byteIndex &&
// length different bytes at byteIndex
DictTrieBuilder::Node *
ByteTrieBuilder::makeBranchSubNode(int32_t start, int32_t limit, int32_t byteIndex,
                                   int32_t length, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    char middleBytes[16];
    Node *lessThan[16];
    int32_t ltLength=0;
    while(length>ByteTrie::kMaxBranchLinearSubNodeLength) {
        // Branch on the middle byte.
        // First, find the middle byte.
        int32_t count=length/2;
        int32_t i=start;
        char byte;
        do {
            byte=elements[i++].charAt(byteIndex, strings);
            while(byte==elements[i].charAt(byteIndex, strings)) {
                ++i;
            }
        } while(--count>0);
        // Encode the less-than branch first.
        byte=middleBytes[ltLength]=elements[i].charAt(byteIndex, strings);  // middle byte
        lessThan[ltLength]=makeBranchSubNode(start, i, byteIndex, length/2, errorCode);
        ++ltLength;
        // Continue for the greater-or-equal branch.
        start=i;
        length=length-length/2;
    }
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    BTListBranchNode *listNode=new BTListBranchNode();
    if(listNode==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    // For each byte, find its elements array start and whether it has a final value.
    int32_t byteNumber=0;
    do {
        int32_t i=start;
        char byte=elements[i++].charAt(byteIndex, strings);
        while(byte==elements[i].charAt(byteIndex, strings)) {
            ++i;
        }
        if(start==i-1 && byteIndex+1==elements[start].getStringLength(strings)) {
            listNode->add((uint8_t)byte, elements[start].getValue());
        } else {
            listNode->add((uint8_t)byte, makeNode(start, i, byteIndex+1, errorCode));
        }
        start=i;
    } while(++byteNumber<length-1);
    // byteNumber==length-1, and the maxByte elements range is [start..limit[
    char byte=elements[start].charAt(byteIndex, strings);
    if(start==limit-1 && byteIndex+1==elements[start].getStringLength(strings)) {
        listNode->add((uint8_t)byte, elements[start].getValue());
    } else {
        listNode->add((uint8_t)byte, makeNode(start, limit, byteIndex+1, errorCode));
    }
    Node *node=registerNode(listNode, errorCode);
    // Create the split-branch nodes.
    while(ltLength>0) {
        --ltLength;
        node=registerNode(
            new BTSplitBranchNode(middleBytes[ltLength], lessThan[ltLength], node), errorCode);
    }
    return node;
}

void
ByteTrieBuilder::BTFinalValueNode::write(DictTrieBuilder &builder) {
    ByteTrieBuilder &b=(ByteTrieBuilder &)builder;
    offset=b.writeValueAndFinal(value, TRUE);
}

UBool
ByteTrieBuilder::BTValueNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!ValueNode::operator==(other)) {
        return FALSE;
    }
    const BTValueNode &o=(const BTValueNode &)other;
    return next==o.next;
}

int32_t
ByteTrieBuilder::BTValueNode::markRightEdgesFirst(int32_t edgeNumber) {
    if(offset==0) {
        offset=edgeNumber=next->markRightEdgesFirst(edgeNumber);
    }
    return edgeNumber;
}

void
ByteTrieBuilder::BTValueNode::write(DictTrieBuilder &builder) {
    ByteTrieBuilder &b=(ByteTrieBuilder &)builder;
    next->write(builder);
    offset=b.writeValueAndFinal(value, FALSE);
}

ByteTrieBuilder::BTLinearMatchNode::BTLinearMatchNode(const char *bytes, int32_t len, Node *nextNode)
        : LinearMatchNode(len, nextNode), s(bytes) {
    hash=hash*37+uhash_hashCharsN(bytes, len);
}

UBool
ByteTrieBuilder::BTLinearMatchNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!LinearMatchNode::operator==(other)) {
        return FALSE;
    }
    const BTLinearMatchNode &o=(const BTLinearMatchNode &)other;
    return 0==uprv_memcmp(s, o.s, length);
}

void
ByteTrieBuilder::BTLinearMatchNode::write(DictTrieBuilder &builder) {
    ByteTrieBuilder &b=(ByteTrieBuilder &)builder;
    next->write(builder);
    b.write(s, length);
    offset=b.write(minLinearMatch()+length-1);
}

void
ByteTrieBuilder::BTListBranchNode::write(DictTrieBuilder &builder) {
    ByteTrieBuilder &b=(ByteTrieBuilder &)builder;
    // Write the sub-nodes in reverse order: The jump lengths are deltas from
    // after their own positions, so if we wrote the minByte sub-node first,
    // then its jump delta would be larger.
    // Instead we write the minByte sub-node last, for a shorter delta.
    int32_t byteNumber=length-1;
    Node *rightEdge=equal[byteNumber];
    int32_t rightEdgeNumber= rightEdge==NULL ? firstEdgeNumber : rightEdge->getOffset();
    do {
        --byteNumber;
        if(equal[byteNumber]!=NULL) {
            equal[byteNumber]->writeUnlessInsideRightEdge(firstEdgeNumber, rightEdgeNumber, builder);
        }
    } while(byteNumber>0);
    // The maxByte sub-node is written as the very last one because we do
    // not jump for it at all.
    byteNumber=length-1;
    if(rightEdge==NULL) {
        b.writeValueAndFinal(values[byteNumber], TRUE);
    } else {
        rightEdge->write(builder);
    }
    b.write(units[byteNumber]);
    // Write the rest of this node's byte-value pairs.
    while(--byteNumber>=0) {
        int32_t value;
        UBool isFinal;
        if(equal[byteNumber]==NULL) {
            // Write the final value for the one string ending with this byte.
            value=values[byteNumber];
            isFinal=TRUE;
        } else {
            // Write the delta to the start position of the sub-node.
            U_ASSERT(equal[byteNumber]->getOffset()>0);
            value=b.bytesLength-equal[byteNumber]->getOffset();
            isFinal=FALSE;
        }
        b.writeValueAndFinal(value, isFinal);
        offset=b.write(units[byteNumber]);
    }
}

void
ByteTrieBuilder::BTSplitBranchNode::write(DictTrieBuilder &builder) {
    ByteTrieBuilder &b=(ByteTrieBuilder &)builder;
    // Encode the less-than branch first.
    lessThan->writeUnlessInsideRightEdge(firstEdgeNumber, greaterOrEqual->getOffset(), builder);
    // Encode the greater-or-equal branch last because we do not jump for it at all.
    greaterOrEqual->write(builder);
    // Write this node.
    U_ASSERT(lessThan->getOffset()>0);
    b.writeDelta(b.bytesLength-lessThan->getOffset());  // less-than
    offset=b.write(unit);
}

void
ByteTrieBuilder::BTBranchHeadNode::write(DictTrieBuilder &builder) {
    ByteTrieBuilder &b=(ByteTrieBuilder &)builder;
    next->write(builder);
    offset=b.write((length-1));
    if(length>minLinearMatch()) {
        offset=b.write(0);
    }
}

UBool
ByteTrieBuilder::ensureCapacity(int32_t length) {
    if(bytes==NULL) {
        return FALSE;  // previous memory allocation had failed
    }
    if(length>bytesCapacity) {
        int32_t newCapacity=bytesCapacity;
        do {
            newCapacity*=2;
        } while(newCapacity<=length);
        char *newBytes=reinterpret_cast<char *>(uprv_malloc(newCapacity));
        if(newBytes==NULL) {
            // unable to allocate memory
            uprv_free(bytes);
            bytes=NULL;
            return FALSE;
        }
        uprv_memcpy(newBytes+(newCapacity-bytesLength),
                    bytes+(bytesCapacity-bytesLength), bytesLength);
        uprv_free(bytes);
        bytes=newBytes;
        bytesCapacity=newCapacity;
    }
    return TRUE;
}

int32_t
ByteTrieBuilder::write(int32_t byte) {
    int32_t newLength=bytesLength+1;
    if(ensureCapacity(newLength)) {
        bytesLength=newLength;
        bytes[bytesCapacity-bytesLength]=(char)byte;
    }
    return bytesLength;
}

int32_t
ByteTrieBuilder::write(const char *b, int32_t length) {
    int32_t newLength=bytesLength+length;
    if(ensureCapacity(newLength)) {
        bytesLength=newLength;
        uprv_memcpy(bytes+(bytesCapacity-bytesLength), b, length);
    }
    return bytesLength;
}

int32_t
ByteTrieBuilder::writeValueAndFinal(int32_t i, UBool final) {
    char intBytes[5];
    int32_t length=1;
    if(i<0 || i>0xffffff) {
        intBytes[0]=(char)ByteTrie::kFiveByteValueLead;
        intBytes[1]=(char)(i>>24);
        intBytes[2]=(char)(i>>16);
        intBytes[3]=(char)(i>>8);
        intBytes[4]=(char)i;
        length=5;
    } else if(i<=ByteTrie::kMaxOneByteValue) {
        intBytes[0]=(char)(ByteTrie::kMinOneByteValueLead+i);
    } else {
        if(i<=ByteTrie::kMaxTwoByteValue) {
            intBytes[0]=(char)(ByteTrie::kMinTwoByteValueLead+(i>>8));
        } else {
            if(i<=ByteTrie::kMaxThreeByteValue) {
                intBytes[0]=(char)(ByteTrie::kMinThreeByteValueLead+(i>>16));
            } else {
                intBytes[0]=(char)ByteTrie::kFourByteValueLead;
                intBytes[1]=(char)(i>>16);
                length=2;
            }
            intBytes[length++]=(char)(i>>8);
        }
        intBytes[length++]=(char)i;
    }
    intBytes[0]=(char)((intBytes[0]<<1)|final);
    return write(intBytes, length);
}

int32_t
ByteTrieBuilder::writeDelta(int32_t i) {
    char intBytes[5];
    int32_t length;
    U_ASSERT(i>=0);
    if(i<=ByteTrie::kMaxOneByteDelta) {
        length=0;
    } else if(i<=ByteTrie::kMaxTwoByteDelta) {
        intBytes[0]=(char)(ByteTrie::kMinTwoByteDeltaLead+(i>>8));
        length=1;
    } else {
        if(i<=ByteTrie::kMaxThreeByteDelta) {
            intBytes[0]=(char)(ByteTrie::kMinThreeByteDeltaLead+(i>>16));
            length=2;
        } else {
            if(i<=0xffffff) {
                intBytes[0]=(char)ByteTrie::kFourByteDeltaLead;
                length=3;
            } else {
                intBytes[0]=(char)ByteTrie::kFiveByteDeltaLead;
                intBytes[1]=(char)(i>>24);
                length=4;
            }
            intBytes[1]=(char)(i>>16);
        }
        intBytes[1]=(char)(i>>8);
    }
    intBytes[length++]=(char)i;
    return write(intBytes, length);
}

U_NAMESPACE_END
