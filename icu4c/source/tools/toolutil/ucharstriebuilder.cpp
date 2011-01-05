/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ucharstriebuilder.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010nov14
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "uarrsort.h"
#include "ucharstrie.h"
#include "ucharstriebuilder.h"

U_NAMESPACE_BEGIN

/*
 * Note: This builder implementation stores (string, value) pairs with full copies
 * of the 16-bit-unit sequences, until the UCharsTrie is built.
 * It might(!) take less memory if we collected the data in a temporary, dynamic trie.
 */

class UCharsTrieElement : public UMemory {
public:
    // Use compiler's default constructor, initializes nothing.

    void setTo(const UnicodeString &s, int32_t val, UnicodeString &strings, UErrorCode &errorCode);

    UnicodeString getString(const UnicodeString &strings) const {
        int32_t length=strings[stringOffset];
        return strings.tempSubString(stringOffset+1, length);
    }
    int32_t getStringLength(const UnicodeString &strings) const {
        return strings[stringOffset];
    }

    UChar charAt(int32_t index, const UnicodeString &strings) const {
        return strings[stringOffset+1+index];
    }

    int32_t getValue() const { return value; }

    int32_t compareStringTo(const UCharsTrieElement &o, const UnicodeString &strings) const;

private:
    // The first strings unit contains the string length.
    // (Compared with a stringLength field here, this saves 2 bytes per string.)
    int32_t stringOffset;
    int32_t value;
};

void
UCharsTrieElement::setTo(const UnicodeString &s, int32_t val,
                         UnicodeString &strings, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return;
    }
    int32_t length=s.length();
    if(length>0xffff) {
        // Too long: We store the length in 1 unit.
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }
    stringOffset=strings.length();
    strings.append((UChar)length);
    value=val;
    strings.append(s);
}

int32_t
UCharsTrieElement::compareStringTo(const UCharsTrieElement &other, const UnicodeString &strings) const {
    return getString(strings).compare(other.getString(strings));
}

UCharsTrieBuilder::~UCharsTrieBuilder() {
    delete[] elements;
    uprv_free(uchars);
}

UCharsTrieBuilder &
UCharsTrieBuilder::add(const UnicodeString &s, int32_t value, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return *this;
    }
    if(ucharsLength>0) {
        // Cannot add elements after building.
        errorCode=U_NO_WRITE_PERMISSION;
        return *this;
    }
    ucharsCapacity+=s.length()+1;  // Crude uchars preallocation estimate.
    if(elementsLength==elementsCapacity) {
        int32_t newCapacity;
        if(elementsCapacity==0) {
            newCapacity=1024;
        } else {
            newCapacity=4*elementsCapacity;
        }
        UCharsTrieElement *newElements=new UCharsTrieElement[newCapacity];
        if(newElements==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
        }
        if(elementsLength>0) {
            uprv_memcpy(newElements, elements, elementsLength*sizeof(UCharsTrieElement));
        }
        delete[] elements;
        elements=newElements;
        elementsCapacity=newCapacity;
    }
    elements[elementsLength++].setTo(s, value, strings, errorCode);
    if(U_SUCCESS(errorCode) && strings.isBogus()) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return *this;
}

U_CDECL_BEGIN

static int32_t U_CALLCONV
compareElementStrings(const void *context, const void *left, const void *right) {
    const UnicodeString *strings=reinterpret_cast<const UnicodeString *>(context);
    const UCharsTrieElement *leftElement=reinterpret_cast<const UCharsTrieElement *>(left);
    const UCharsTrieElement *rightElement=reinterpret_cast<const UCharsTrieElement *>(right);
    return leftElement->compareStringTo(*rightElement, *strings);
}

U_CDECL_END

UnicodeString &
UCharsTrieBuilder::build(UStringTrieBuildOption buildOption, UnicodeString &result, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return result;
    }
    if(ucharsLength>0) {
        // Already built.
        result.setTo(FALSE, uchars+(ucharsCapacity-ucharsLength), ucharsLength);
        return result;
    }
    if(elementsLength==0) {
        errorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return result;
    }
    if(strings.isBogus()) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return result;
    }
    uprv_sortArray(elements, elementsLength, (int32_t)sizeof(UCharsTrieElement),
                   compareElementStrings, &strings,
                   FALSE,  // need not be a stable sort
                   &errorCode);
    if(U_FAILURE(errorCode)) {
        return result;
    }
    // Duplicate strings are not allowed.
    UnicodeString prev=elements[0].getString(strings);
    for(int32_t i=1; i<elementsLength; ++i) {
        UnicodeString current=elements[i].getString(strings);
        if(prev==current) {
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return result;
        }
        prev.fastCopyFrom(current);
    }
    // Create and UChar-serialize the trie for the elements.
    if(ucharsCapacity<1024) {
        ucharsCapacity=1024;
    }
    uchars=reinterpret_cast<UChar *>(uprv_malloc(ucharsCapacity*2));
    if(uchars==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return result;
    }
    StringTrieBuilder::build(buildOption, elementsLength, errorCode);
    if(uchars==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    } else {
        result.setTo(FALSE, uchars+(ucharsCapacity-ucharsLength), ucharsLength);
    }
    return result;
}

int32_t
UCharsTrieBuilder::getElementStringLength(int32_t i) const {
    return elements[i].getStringLength(strings);
}

UChar
UCharsTrieBuilder::getElementUnit(int32_t i, int32_t unitIndex) const {
    return elements[i].charAt(unitIndex, strings);
}

int32_t
UCharsTrieBuilder::getElementValue(int32_t i) const {
    return elements[i].getValue();
}

int32_t
UCharsTrieBuilder::getLimitOfLinearMatch(int32_t first, int32_t last, int32_t unitIndex) const {
    const UCharsTrieElement &firstElement=elements[first];
    const UCharsTrieElement &lastElement=elements[last];
    int32_t minStringLength=firstElement.getStringLength(strings);
    while(++unitIndex<minStringLength &&
            firstElement.charAt(unitIndex, strings)==
            lastElement.charAt(unitIndex, strings)) {}
    return unitIndex;
}

int32_t
UCharsTrieBuilder::countElementUnits(int32_t start, int32_t limit, int32_t unitIndex) const {
    int32_t length=0;  // Number of different units at unitIndex.
    int32_t i=start;
    do {
        UChar unit=elements[i++].charAt(unitIndex, strings);
        while(i<limit && unit==elements[i].charAt(unitIndex, strings)) {
            ++i;
        }
        ++length;
    } while(i<limit);
    return length;
}

int32_t
UCharsTrieBuilder::skipElementsBySomeUnits(int32_t i, int32_t unitIndex, int32_t count) const {
    do {
        UChar unit=elements[i++].charAt(unitIndex, strings);
        while(unit==elements[i].charAt(unitIndex, strings)) {
            ++i;
        }
    } while(--count>0);
    return i;
}

int32_t
UCharsTrieBuilder::indexOfElementWithNextUnit(int32_t i, int32_t unitIndex, UChar unit) const {
    while(unit==elements[i].charAt(unitIndex, strings)) {
        ++i;
    }
    return i;
}

UCharsTrieBuilder::UCTLinearMatchNode::UCTLinearMatchNode(const UChar *units, int32_t len, Node *nextNode)
        : LinearMatchNode(len, nextNode), s(units) {
    hash=hash*37+uhash_hashUCharsN(units, len);
}

UBool
UCharsTrieBuilder::UCTLinearMatchNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!LinearMatchNode::operator==(other)) {
        return FALSE;
    }
    const UCTLinearMatchNode &o=(const UCTLinearMatchNode &)other;
    return 0==u_memcmp(s, o.s, length);
}

void
UCharsTrieBuilder::UCTLinearMatchNode::write(StringTrieBuilder &builder) {
    UCharsTrieBuilder &b=(UCharsTrieBuilder &)builder;
    next->write(builder);
    b.write(s, length);
    offset=b.writeValueAndType(hasValue, value, b.getMinLinearMatch()+length-1);
}

StringTrieBuilder::Node *
UCharsTrieBuilder::createLinearMatchNode(int32_t i, int32_t unitIndex, int32_t length,
                                         Node *nextNode) const {
    return new UCTLinearMatchNode(
            elements[i].getString(strings).getBuffer()+unitIndex,
            length,
            nextNode);
}

UBool
UCharsTrieBuilder::ensureCapacity(int32_t length) {
    if(uchars==NULL) {
        return FALSE;  // previous memory allocation had failed
    }
    if(length>ucharsCapacity) {
        int32_t newCapacity=ucharsCapacity;
        do {
            newCapacity*=2;
        } while(newCapacity<=length);
        UChar *newUChars=reinterpret_cast<UChar *>(uprv_malloc(newCapacity*2));
        if(newUChars==NULL) {
            // unable to allocate memory
            uprv_free(uchars);
            uchars=NULL;
            return FALSE;
        }
        u_memcpy(newUChars+(newCapacity-ucharsLength),
                 uchars+(ucharsCapacity-ucharsLength), ucharsLength);
        uprv_free(uchars);
        uchars=newUChars;
        ucharsCapacity=newCapacity;
    }
    return TRUE;
}

int32_t
UCharsTrieBuilder::write(int32_t unit) {
    int32_t newLength=ucharsLength+1;
    if(ensureCapacity(newLength)) {
        ucharsLength=newLength;
        uchars[ucharsCapacity-ucharsLength]=(UChar)unit;
    }
    return ucharsLength;
}

int32_t
UCharsTrieBuilder::write(const UChar *s, int32_t length) {
    int32_t newLength=ucharsLength+length;
    if(ensureCapacity(newLength)) {
        ucharsLength=newLength;
        u_memcpy(uchars+(ucharsCapacity-ucharsLength), s, length);
    }
    return ucharsLength;
}

int32_t
UCharsTrieBuilder::writeElementUnits(int32_t i, int32_t unitIndex, int32_t length) {
    return write(elements[i].getString(strings).getBuffer()+unitIndex, length);
}

int32_t
UCharsTrieBuilder::writeValueAndFinal(int32_t i, UBool final) {
    UChar intUnits[3];
    int32_t length;
    if(i<0 || i>UCharsTrie::kMaxTwoUnitValue) {
        intUnits[0]=(UChar)(UCharsTrie::kThreeUnitValueLead);
        intUnits[1]=(UChar)(i>>16);
        intUnits[2]=(UChar)i;
        length=3;
    } else if(i<=UCharsTrie::kMaxOneUnitValue) {
        intUnits[0]=(UChar)(i);
        length=1;
    } else {
        intUnits[0]=(UChar)(UCharsTrie::kMinTwoUnitValueLead+(i>>16));
        intUnits[1]=(UChar)i;
        length=2;
    }
    intUnits[0]=(UChar)(intUnits[0]|(final<<15));
    return write(intUnits, length);
}

int32_t
UCharsTrieBuilder::writeValueAndType(UBool hasValue, int32_t value, int32_t node) {
    if(!hasValue) {
        return write(node);
    }
    UChar intUnits[3];
    int32_t length;
    if(value<0 || value>UCharsTrie::kMaxTwoUnitNodeValue) {
        intUnits[0]=(UChar)(UCharsTrie::kThreeUnitNodeValueLead);
        intUnits[1]=(UChar)(value>>16);
        intUnits[2]=(UChar)value;
        length=3;
    } else if(value<=UCharsTrie::kMaxOneUnitNodeValue) {
        intUnits[0]=(UChar)((value+1)<<6);
        length=1;
    } else {
        intUnits[0]=(UChar)(UCharsTrie::kMinTwoUnitNodeValueLead+((value>>10)&0x7fc0));
        intUnits[1]=(UChar)value;
        length=2;
    }
    intUnits[0]|=(UChar)node;
    return write(intUnits, length);
}

int32_t
UCharsTrieBuilder::writeDeltaTo(int32_t jumpTarget) {
    int32_t i=ucharsLength-jumpTarget;
    UChar intUnits[3];
    int32_t length;
    U_ASSERT(i>=0);
    if(i<=UCharsTrie::kMaxOneUnitDelta) {
        length=0;
    } else if(i<=UCharsTrie::kMaxTwoUnitDelta) {
        intUnits[0]=(UChar)(UCharsTrie::kMinTwoUnitDeltaLead+(i>>16));
        length=1;
    } else {
        intUnits[0]=(UChar)(UCharsTrie::kThreeUnitDeltaLead);
        intUnits[1]=(UChar)(i>>16);
        length=2;
    }
    intUnits[length++]=(UChar)i;
    return write(intUnits, length);
}

U_NAMESPACE_END
