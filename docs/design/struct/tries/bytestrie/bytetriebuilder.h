// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  bytetriebuilder.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010sep25
*   created by: Markus W. Scherer
*
* Builder class for ByteTrie dictionary trie.
*/

#ifndef __BYTETRIEBUILDER_H__
#define __BYTETRIEBUILDER_H__

#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "bytetrie.h"
#include "charstr.h"
#include "cmemory.h"
#include "uarrsort.h"

U_NAMESPACE_BEGIN

class ByteTrieElement;

class /*U_TOOLUTIL_API*/ ByteTrieBuilder : public UMemory {
public:
    ByteTrieBuilder()
            : elements(NULL), elementsCapacity(0), elementsLength(0),
              bytes(NULL), bytesCapacity(0), bytesLength(0) {}
    ~ByteTrieBuilder();

    ByteTrieBuilder &add(const StringPiece &s, int32_t value, UErrorCode &errorCode);

    StringPiece build(UErrorCode &errorCode);

    ByteTrieBuilder &clear() {
        strings.clear();
        elementsLength=0;
        bytesLength=0;
        return *this;
    }

private:
    void makeNode(int32_t start, int32_t limit, int32_t byteIndex);
    void makeListBranchNode(int32_t start, int32_t limit, int32_t byteIndex, int32_t length);
    void makeThreeWayBranchNode(int32_t start, int32_t limit, int32_t byteIndex, int32_t length);

    UBool ensureCapacity(int32_t length);
    void write(int32_t byte);
    void write(const char *b, int32_t length);
    void writeCompactInt(int32_t i, UBool final);
    int32_t writeFixedInt(int32_t i);  // Returns number of bytes.

    CharString strings;
    ByteTrieElement *elements;
    int32_t elementsCapacity;
    int32_t elementsLength;

    // Byte serialization of the trie.
    // Grows from the back: bytesLength measures from the end of the buffer!
    char *bytes;
    int32_t bytesCapacity;
    int32_t bytesLength;
};

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
    // TODO: add StringPiece.compareTo()
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
ByteTrieBuilder::build(UErrorCode &errorCode) {
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
    makeNode(0, elementsLength, 0);
    if(bytes==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    } else {
        result.set(bytes+(bytesCapacity-bytesLength), bytesLength);
    }
    return result;
}

// Requires start<limit,
// and all strings of the [start..limit[ elements must be sorted and
// have a common prefix of length firstByteIndex.
void
ByteTrieBuilder::makeNode(int32_t start, int32_t limit, int32_t byteIndex) {
    if(byteIndex==elements[start].getStringLength(strings)) {
        // An intermediate or final value.
        int32_t value=elements[start++].getValue();
        UBool final= start==limit;
        if(!final) {
            makeNode(start, limit, byteIndex);
        }
        writeCompactInt(value, final);
        return;
    }
    // Now all [start..limit[ strings are longer than byteIndex.
    int32_t minByte=(uint8_t)elements[start].charAt(byteIndex, strings);
    int32_t maxByte=(uint8_t)elements[limit-1].charAt(byteIndex, strings);
    if(minByte==maxByte) {
        // Linear-match node: All strings have the same character at byteIndex.
        int32_t lastByteIndex=byteIndex;
        int32_t length=0;
        do {
            ++lastByteIndex;
            ++length;
        } while(length<ByteTrie::kMaxLinearMatchLength &&
                elements[start].getStringLength(strings)>lastByteIndex &&
                elements[start].charAt(lastByteIndex, strings)==
                    elements[limit-1].charAt(lastByteIndex, strings));
        makeNode(start, limit, lastByteIndex);
        write(elements[start].getString(strings).data()+byteIndex, length);
        write(ByteTrie::kMinLinearMatch+length-1);
        return;
    }
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
    if(length<=ByteTrie::kMaxListBranchLength) {
        makeListBranchNode(start, limit, byteIndex, length);
    } else {
        makeThreeWayBranchNode(start, limit, byteIndex, length);
    }
}

// start<limit && all strings longer than byteIndex &&
// 2..kMaxListBranchLength different bytes at byteIndex
void
ByteTrieBuilder::makeListBranchNode(int32_t start, int32_t limit, int32_t byteIndex, int32_t length) {
    // List of byte-value pairs where values are either final values
    // or jumps to other parts of the trie.
    int32_t starts[ByteTrie::kMaxListBranchLength-1];
    UBool final[ByteTrie::kMaxListBranchLength-1];
    // For each byte except the last one, find its elements array start and its value if final.
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

    // Write the sub-nodes in reverse order: The jump lengths are deltas from
    // after their own positions, so if we wrote the minByte sub-node first,
    // then its jump delta would be larger.
    // Instead we write the minByte sub-node last, for a shorter delta.
    int32_t jumpTargets[ByteTrie::kMaxListBranchLength-1];
    byteNumber-=2;
    do {
        if(!final[byteNumber]) {
            makeNode(starts[byteNumber], starts[byteNumber+1], byteIndex+1);
            jumpTargets[byteNumber]=bytesLength;
        }
    } while(--byteNumber>=0);
    // The maxByte sub-node is written as the very last one because we do
    // not jump for it at all.
    byteNumber=length-1;
    makeNode(start, limit, byteIndex+1);
    write(elements[start].charAt(byteIndex, strings));
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
        writeCompactInt(value, final[byteNumber]);
        write(elements[start].charAt(byteIndex, strings));
    }
    // Write the node lead byte.
    write(ByteTrie::kMinListBranch+length-2);
}

// start<limit && all strings longer than byteIndex &&
// at least three different bytes at byteIndex
void
ByteTrieBuilder::makeThreeWayBranchNode(int32_t start, int32_t limit, int32_t byteIndex, int32_t length) {
    // Three-way branch on the middle byte.
    // Find the middle byte.
    length/=2;  // >=1
    int32_t i=start;
    do {
        char byte=elements[i++].charAt(byteIndex, strings);
        while(byte==elements[i].charAt(byteIndex, strings)) {
            ++i;
        }
    } while(--length>0);
    // Encode the less-than branch first.
    // Unlike in the list-branch node (see comments above) where
    // all jumps are encoded in compact integers, in this node type the
    // less-than jump is more efficient
    // (because it is only ever a jump, with a known number of bytes)
    // than the equals jump (where a jump needs to be distinguished from a final value).
    makeNode(start, i, byteIndex);
    int32_t leftNode=bytesLength;
    // Find the elements range for the middle byte.
    start=i;
    char byte=elements[i++].charAt(byteIndex, strings);
    while(byte==elements[i].charAt(byteIndex, strings)) {
        ++i;
    }
    // Encode the equals branch.
    int32_t value;
    UBool final;
    if(start==i-1 && byteIndex+1==elements[start].getStringLength(strings)) {
        // Store the final value for the one string ending with this byte.
        value=elements[start].getValue();
        final=TRUE;
    } else {
        // Store the start position of the sub-node.
        makeNode(start, i, byteIndex+1);
        value=bytesLength;
        final=FALSE;
    }
    // Encode the greater-than branch last because we do not jump for it at all.
    makeNode(i, limit, byteIndex);
    // Write this node.
    if(!final) {
        value=bytesLength-value;
    }
    writeCompactInt(value, final);  // equals
    int32_t bytesForJump=writeFixedInt(bytesLength-leftNode);  // less-than
    write(byte);
    write(bytesForJump-1);
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

void
ByteTrieBuilder::write(int32_t byte) {
    int32_t newLength=bytesLength+1;
    if(ensureCapacity(newLength)) {
        bytesLength=newLength;
        bytes[bytesCapacity-bytesLength]=(char)byte;
    }
}

void
ByteTrieBuilder::write(const char *b, int32_t length) {
    int32_t newLength=bytesLength+length;
    if(ensureCapacity(newLength)) {
        bytesLength=newLength;
        uprv_memcpy(bytes+(bytesCapacity-bytesLength), b, length);
    }
}

void
ByteTrieBuilder::writeCompactInt(int32_t i, UBool final) {
    char intBytes[5];
    int32_t length=1;
    if(i<0 || i>0xffffff) {
        intBytes[0]=(char)(ByteTrie::kFiveByteLead);
        intBytes[1]=(char)(i>>24);
        intBytes[2]=(char)(i>>16);
        intBytes[3]=(char)(i>>8);
        intBytes[4]=(char)(i);
        length=5;
    } else if(i<=ByteTrie::kMaxOneByteValue) {
        intBytes[0]=(char)(ByteTrie::kMinOneByteLead+i);
    } else {
        if(i<=ByteTrie::kMaxTwoByteValue) {
            intBytes[0]=(char)(ByteTrie::kMinTwoByteLead+(i>>8));
        } else {
            if(i<=ByteTrie::kMaxThreeByteValue) {
                intBytes[0]=(char)(ByteTrie::kMinThreeByteLead+(i>>16));
            } else {
                intBytes[0]=(char)(ByteTrie::kFourByteLead);
                intBytes[1]=(char)(i>>16);
                length=2;
            }
            intBytes[length++]=(char)(i>>8);
        }
        intBytes[length++]=(char)(i);
    }
    intBytes[0]=(char)((intBytes[0]<<1)|final);
    write(intBytes, length);
}

int32_t
ByteTrieBuilder::writeFixedInt(int32_t i) {
    char intBytes[4];
    int32_t length;
    if(i<0 || i>0xffffff) {
        intBytes[0]=(char)(i>>24);
        intBytes[1]=(char)(i>>16);
        intBytes[2]=(char)(i>>8);
        length=3;  // last byte below
    } else {
        if(i<=0xffff) {
            length=0;
        } else {
            intBytes[0]=(char)(i>>16);
            length=1;
        }
        if(i>0xff) {
            intBytes[length++]=(char)(i>>8);
        }
    }
    intBytes[length++]=(char)(i);
    write(intBytes, length);
    return length;
}

U_NAMESPACE_END

#endif  // __BYTETRIEBUILDER_H__
