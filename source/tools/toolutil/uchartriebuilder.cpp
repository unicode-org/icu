/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  uchartriebuilder.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010nov14
*   created by: Markus W. Scherer
*
* Builder class for UCharTrie dictionary trie.
*/

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "uarrsort.h"
#include "uchartrie.h"
#include "uchartriebuilder.h"

U_NAMESPACE_BEGIN

/*
 * Note: This builder implementation stores (string, value) pairs with full copies
 * of the 16-bit-unit sequences, until the UCharTrie is built.
 * It might(!) take less memory if we collected the data in a temporary, dynamic trie.
 */

class UCharTrieElement : public UMemory {
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

    int32_t compareStringTo(const UCharTrieElement &o, const UnicodeString &strings) const;

private:
    // The first strings unit contains the string length.
    // (Compared with a stringLength field here, this saves 2 bytes per string.)
    int32_t stringOffset;
    int32_t value;
};

void
UCharTrieElement::setTo(const UnicodeString &s, int32_t val,
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
UCharTrieElement::compareStringTo(const UCharTrieElement &other, const UnicodeString &strings) const {
    return getString(strings).compare(other.getString(strings));
}

UCharTrieBuilder::~UCharTrieBuilder() {
    delete[] elements;
    uprv_free(uchars);
}

UCharTrieBuilder &
UCharTrieBuilder::add(const UnicodeString &s, int32_t value, UErrorCode &errorCode) {
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
        UCharTrieElement *newElements=new UCharTrieElement[newCapacity];
        if(newElements==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
        }
        if(elementsLength>0) {
            uprv_memcpy(newElements, elements, elementsLength*sizeof(UCharTrieElement));
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
    const UCharTrieElement *leftElement=reinterpret_cast<const UCharTrieElement *>(left);
    const UCharTrieElement *rightElement=reinterpret_cast<const UCharTrieElement *>(right);
    return leftElement->compareStringTo(*rightElement, *strings);
}

U_CDECL_END

UnicodeString &
UCharTrieBuilder::build(UDictTrieBuildOption buildOption, UnicodeString &result, UErrorCode &errorCode) {
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
    uprv_sortArray(elements, elementsLength, (int32_t)sizeof(UCharTrieElement),
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
    if(uchars==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    } else {
        result.setTo(FALSE, uchars+(ucharsCapacity-ucharsLength), ucharsLength);
    }
    return result;
}

// Requires start<limit,
// and all strings of the [start..limit[ elements must be sorted and
// have a common prefix of length unitIndex.
void
UCharTrieBuilder::writeNode(int32_t start, int32_t limit, int32_t unitIndex) {
    UBool hasValue=FALSE;
    int32_t value=0;
    int32_t type;
    if(unitIndex==elements[start].getStringLength(strings)) {
        // An intermediate or final value.
        value=elements[start++].getValue();
        if(start==limit) {
            writeValueAndFinal(value, TRUE);  // final-value node
            return;
        }
        hasValue=TRUE;
    }
    // Now all [start..limit[ strings are longer than unitIndex.
    const UCharTrieElement &minElement=elements[start];
    const UCharTrieElement &maxElement=elements[limit-1];
    int32_t minUnit=minElement.charAt(unitIndex, strings);
    int32_t maxUnit=maxElement.charAt(unitIndex, strings);
    if(minUnit==maxUnit) {
        // Linear-match node: All strings have the same character at unitIndex.
        int32_t minStringLength=minElement.getStringLength(strings);
        int32_t lastUnitIndex=unitIndex;
        while(++lastUnitIndex<minStringLength &&
                minElement.charAt(lastUnitIndex, strings)==
                maxElement.charAt(lastUnitIndex, strings)) {}
        writeNode(start, limit, lastUnitIndex);
        // Break the linear-match sequence into chunks of at most kMaxLinearMatchLength.
        const UChar *s=minElement.getString(strings).getBuffer();
        int32_t length=lastUnitIndex-unitIndex;
        while(length>UCharTrie::kMaxLinearMatchLength) {
            lastUnitIndex-=UCharTrie::kMaxLinearMatchLength;
            length-=UCharTrie::kMaxLinearMatchLength;
            write(s+lastUnitIndex, UCharTrie::kMaxLinearMatchLength);
            write(UCharTrie::kMinLinearMatch+UCharTrie::kMaxLinearMatchLength-1);
        }
        write(s+unitIndex, length);
        type=UCharTrie::kMinLinearMatch+length-1;
    } else {
        // Branch node.
        int32_t length=0;  // Number of different units at unitIndex.
        int32_t i=start;
        do {
            UChar unit=elements[i++].charAt(unitIndex, strings);
            while(i<limit && unit==elements[i].charAt(unitIndex, strings)) {
                ++i;
            }
            ++length;
        } while(i<limit);
        // length>=2 because minUnit!=maxUnit.
        writeBranchSubNode(start, limit, unitIndex, length);
        if(--length<UCharTrie::kMinLinearMatch) {
            type=length;
        } else {
            write(length);
            type=0;
        }
    }
    writeValueAndType(hasValue, value, type);
}

// start<limit && all strings longer than unitIndex &&
// length different units at unitIndex
void
UCharTrieBuilder::writeBranchSubNode(int32_t start, int32_t limit, int32_t unitIndex, int32_t length) {
    UChar middleUnits[16];
    int32_t lessThan[16];
    int32_t ltLength=0;
    while(length>UCharTrie::kMaxBranchLinearSubNodeLength) {
        // Branch on the middle unit.
        // First, find the middle unit.
        int32_t count=length/2;
        int32_t i=start;
        UChar unit;
        do {
            unit=elements[i++].charAt(unitIndex, strings);
            while(unit==elements[i].charAt(unitIndex, strings)) {
                ++i;
            }
        } while(--count>0);
        // Encode the less-than branch first.
        unit=middleUnits[ltLength]=elements[i].charAt(unitIndex, strings);  // middle unit
        writeBranchSubNode(start, i, unitIndex, length/2);
        lessThan[ltLength]=ucharsLength;
        ++ltLength;
        // Continue for the greater-or-equal branch.
        start=i;
        length=length-length/2;
    }
    // For each unit, find its elements array start and whether it has a final value.
    int32_t starts[UCharTrie::kMaxBranchLinearSubNodeLength];
    UBool final[UCharTrie::kMaxBranchLinearSubNodeLength-1];
    int32_t unitNumber=0;
    do {
        int32_t i=starts[unitNumber]=start;
        UChar unit=elements[i++].charAt(unitIndex, strings);
        while(unit==elements[i].charAt(unitIndex, strings)) {
            ++i;
        }
        final[unitNumber]= start==i-1 && unitIndex+1==elements[start].getStringLength(strings);
        start=i;
    } while(++unitNumber<length-1);
    // unitNumber==length-1, and the maxUnit elements range is [start..limit[
    starts[unitNumber]=start;

    // Write the sub-nodes in reverse order: The jump lengths are deltas from
    // after their own positions, so if we wrote the minUnit sub-node first,
    // then its jump delta would be larger.
    // Instead we write the minUnit sub-node last, for a shorter delta.
    int32_t jumpTargets[UCharTrie::kMaxBranchLinearSubNodeLength-1];
    do {
        --unitNumber;
        if(!final[unitNumber]) {
            writeNode(starts[unitNumber], starts[unitNumber+1], unitIndex+1);
            jumpTargets[unitNumber]=ucharsLength;
        }
    } while(unitNumber>0);
    // The maxUnit sub-node is written as the very last one because we do
    // not jump for it at all.
    unitNumber=length-1;
    writeNode(start, limit, unitIndex+1);
    write(elements[start].charAt(unitIndex, strings));
    // Write the rest of this node's unit-value pairs.
    while(--unitNumber>=0) {
        start=starts[unitNumber];
        int32_t value;
        if(final[unitNumber]) {
            // Write the final value for the one string ending with this unit.
            value=elements[start].getValue();
        } else {
            // Write the delta to the start position of the sub-node.
            value=ucharsLength-jumpTargets[unitNumber];
        }
        writeValueAndFinal(value, final[unitNumber]);
        write(elements[start].charAt(unitIndex, strings));
    }
    // Write the split-branch nodes.
    while(ltLength>0) {
        --ltLength;
        writeDelta(ucharsLength-lessThan[ltLength]);  // less-than
        write(middleUnits[ltLength]);
    }
}

// Requires start<limit,
// and all strings of the [start..limit[ elements must be sorted and
// have a common prefix of length unitIndex.
DictTrieBuilder::Node *
UCharTrieBuilder::makeNode(int32_t start, int32_t limit, int32_t unitIndex, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    UBool hasValue=FALSE;
    int32_t value=0;
    if(unitIndex==elements[start].getStringLength(strings)) {
        // An intermediate or final value.
        value=elements[start++].getValue();
        if(start==limit) {
            return registerFinalValue(value, errorCode);
        }
        hasValue=TRUE;
    }
    ValueNode *node;
    // Now all [start..limit[ strings are longer than unitIndex.
    const UCharTrieElement &minElement=elements[start];
    const UCharTrieElement &maxElement=elements[limit-1];
    int32_t minUnit=minElement.charAt(unitIndex, strings);
    int32_t maxUnit=maxElement.charAt(unitIndex, strings);
    if(minUnit==maxUnit) {
        // Linear-match node: All strings have the same character at unitIndex.
        int32_t minStringLength=minElement.getStringLength(strings);
        int32_t lastUnitIndex=unitIndex;
        while(++lastUnitIndex<minStringLength &&
                minElement.charAt(lastUnitIndex, strings)==
                maxElement.charAt(lastUnitIndex, strings)) {}
        Node *nextNode=makeNode(start, limit, lastUnitIndex, errorCode);
        // Break the linear-match sequence into chunks of at most kMaxLinearMatchLength.
        const UChar *s=minElement.getString(strings).getBuffer();
        int32_t length=lastUnitIndex-unitIndex;
        while(length>UCharTrie::kMaxLinearMatchLength) {
            lastUnitIndex-=UCharTrie::kMaxLinearMatchLength;
            length-=UCharTrie::kMaxLinearMatchLength;
            node=new UCTLinearMatchNode(
                s+lastUnitIndex,
                UCharTrie::kMaxLinearMatchLength,
                nextNode);
            node=(ValueNode *)registerNode(node, errorCode);
            nextNode=node;
        }
        node=new UCTLinearMatchNode(s+unitIndex, length, nextNode);
    } else {
        // Branch node.
        int32_t length=0;  // Number of different units at unitIndex.
        int32_t i=start;
        do {
            UChar unit=elements[i++].charAt(unitIndex, strings);
            while(i<limit && unit==elements[i].charAt(unitIndex, strings)) {
                ++i;
            }
            ++length;
        } while(i<limit);
        // length>=2 because minUnit!=maxUnit.
        Node *subNode=makeBranchSubNode(start, limit, unitIndex, length, errorCode);
        node=new UCTBranchHeadNode(length, subNode);
    }
    if(hasValue && node!=NULL) {
        node->setValue(value);
    }
    return registerNode(node, errorCode);
}

// start<limit && all strings longer than unitIndex &&
// length different units at unitIndex
DictTrieBuilder::Node *
UCharTrieBuilder::makeBranchSubNode(int32_t start, int32_t limit, int32_t unitIndex,
                                    int32_t length, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    UChar middleUnits[16];
    Node *lessThan[16];
    int32_t ltLength=0;
    while(length>UCharTrie::kMaxBranchLinearSubNodeLength) {
        // Branch on the middle unit.
        // First, find the middle unit.
        int32_t count=length/2;
        int32_t i=start;
        UChar unit;
        do {
            unit=elements[i++].charAt(unitIndex, strings);
            while(unit==elements[i].charAt(unitIndex, strings)) {
                ++i;
            }
        } while(--count>0);
        // Create the less-than branch.
        unit=middleUnits[ltLength]=elements[i].charAt(unitIndex, strings);  // middle unit
        lessThan[ltLength]=makeBranchSubNode(start, i, unitIndex, length/2, errorCode);
        ++ltLength;
        // Continue for the greater-or-equal branch.
        start=i;
        length=length-length/2;
    }
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    UCTListBranchNode *listNode=new UCTListBranchNode();
    if(listNode==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    // For each unit, find its elements array start and whether it has a final value.
    int32_t unitNumber=0;
    do {
        int32_t i=start;
        UChar unit=elements[i++].charAt(unitIndex, strings);
        while(unit==elements[i].charAt(unitIndex, strings)) {
            ++i;
        }
        if(start==i-1 && unitIndex+1==elements[start].getStringLength(strings)) {
            listNode->add(unit, elements[start].getValue());
        } else {
            listNode->add(unit, makeNode(start, i, unitIndex+1, errorCode));
        }
        start=i;
    } while(++unitNumber<length-1);
    // unitNumber==length-1, and the maxUnit elements range is [start..limit[
    UChar unit=elements[start].charAt(unitIndex, strings);
    if(start==limit-1 && unitIndex+1==elements[start].getStringLength(strings)) {
        listNode->add(unit, elements[start].getValue());
    } else {
        listNode->add(unit, makeNode(start, limit, unitIndex+1, errorCode));
    }
    Node *node=registerNode(listNode, errorCode);
    // Create the split-branch nodes.
    while(ltLength>0) {
        --ltLength;
        node=registerNode(
            new UCTSplitBranchNode(middleUnits[ltLength], lessThan[ltLength], node), errorCode);
    }
    return node;
}

void
UCharTrieBuilder::UCTFinalValueNode::write(DictTrieBuilder &builder) {
    UCharTrieBuilder &b=(UCharTrieBuilder &)builder;
    offset=b.writeValueAndFinal(value, TRUE);
}

UCharTrieBuilder::UCTLinearMatchNode::UCTLinearMatchNode(const UChar *units, int32_t len, Node *nextNode)
        : LinearMatchNode(len, nextNode), s(units) {
    hash=hash*37+uhash_hashUCharsN(units, len);
}

UBool
UCharTrieBuilder::UCTLinearMatchNode::operator==(const Node &other) const {
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
UCharTrieBuilder::UCTLinearMatchNode::write(DictTrieBuilder &builder) {
    UCharTrieBuilder &b=(UCharTrieBuilder &)builder;
    next->write(builder);
    b.write(s, length);
    offset=b.writeValueAndType(hasValue, value, UCharTrie::kMinLinearMatch+length-1);
}

void
UCharTrieBuilder::UCTListBranchNode::write(DictTrieBuilder &builder) {
    UCharTrieBuilder &b=(UCharTrieBuilder &)builder;
    // Write the sub-nodes in reverse order: The jump lengths are deltas from
    // after their own positions, so if we wrote the minUnit sub-node first,
    // then its jump delta would be larger.
    // Instead we write the minUnit sub-node last, for a shorter delta.
    int32_t unitNumber=length-1;
    Node *rightEdge=equal[unitNumber];
    int32_t rightEdgeNumber= rightEdge==NULL ? firstEdgeNumber : rightEdge->getOffset();
    do {
        --unitNumber;
        if(equal[unitNumber]!=NULL) {
            equal[unitNumber]->writeUnlessInsideRightEdge(firstEdgeNumber, rightEdgeNumber, builder);
        }
    } while(unitNumber>0);
    // The maxUnit sub-node is written as the very last one because we do
    // not jump for it at all.
    unitNumber=length-1;
    if(rightEdge==NULL) {
        b.writeValueAndFinal(values[unitNumber], TRUE);
    } else {
        rightEdge->write(builder);
    }
    b.write(units[unitNumber]);
    // Write the rest of this node's unit-value pairs.
    while(--unitNumber>=0) {
        int32_t value;
        UBool isFinal;
        if(equal[unitNumber]==NULL) {
            // Write the final value for the one string ending with this unit.
            value=values[unitNumber];
            isFinal=TRUE;
        } else {
            // Write the delta to the start position of the sub-node.
            U_ASSERT(equal[unitNumber]->getOffset()>0);
            value=b.ucharsLength-equal[unitNumber]->getOffset();
            isFinal=FALSE;
        }
        b.writeValueAndFinal(value, isFinal);
        offset=b.write(units[unitNumber]);
    }
}

void
UCharTrieBuilder::UCTSplitBranchNode::write(DictTrieBuilder &builder) {
    UCharTrieBuilder &b=(UCharTrieBuilder &)builder;
    // Encode the less-than branch first.
    lessThan->writeUnlessInsideRightEdge(firstEdgeNumber, greaterOrEqual->getOffset(), builder);
    // Encode the greater-or-equal branch last because we do not jump for it at all.
    greaterOrEqual->write(builder);
    // Write this node.
    U_ASSERT(lessThan->getOffset()>0);
    b.writeDelta(b.ucharsLength-lessThan->getOffset());  // less-than
    offset=b.write(unit);
}

void
UCharTrieBuilder::UCTBranchHeadNode::write(DictTrieBuilder &builder) {
    UCharTrieBuilder &b=(UCharTrieBuilder &)builder;
    next->write(builder);
    if(length<=UCharTrie::kMinLinearMatch) {
        offset=b.writeValueAndType(hasValue, value, length-1);
    } else {
        b.write(length-1);
        offset=b.writeValueAndType(hasValue, value, 0);
    }
}

UBool
UCharTrieBuilder::ensureCapacity(int32_t length) {
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
UCharTrieBuilder::write(int32_t unit) {
    int32_t newLength=ucharsLength+1;
    if(ensureCapacity(newLength)) {
        ucharsLength=newLength;
        uchars[ucharsCapacity-ucharsLength]=(UChar)unit;
    }
    return ucharsLength;
}

int32_t
UCharTrieBuilder::write(const UChar *s, int32_t length) {
    int32_t newLength=ucharsLength+length;
    if(ensureCapacity(newLength)) {
        ucharsLength=newLength;
        u_memcpy(uchars+(ucharsCapacity-ucharsLength), s, length);
    }
    return ucharsLength;
}

int32_t
UCharTrieBuilder::writeValueAndFinal(int32_t i, UBool final) {
    UChar intUnits[3];
    int32_t length;
    if(i<0 || i>UCharTrie::kMaxTwoUnitValue) {
        intUnits[0]=(UChar)(UCharTrie::kThreeUnitValueLead);
        intUnits[1]=(UChar)(i>>16);
        intUnits[2]=(UChar)i;
        length=3;
    } else if(i<=UCharTrie::kMaxOneUnitValue) {
        intUnits[0]=(UChar)(i);
        length=1;
    } else {
        intUnits[0]=(UChar)(UCharTrie::kMinTwoUnitValueLead+(i>>16));
        intUnits[1]=(UChar)i;
        length=2;
    }
    intUnits[0]=(UChar)(intUnits[0]|(final<<15));
    return write(intUnits, length);
}

int32_t
UCharTrieBuilder::writeValueAndType(UBool hasValue, int32_t value, int32_t node) {
    if(!hasValue) {
        return write(node);
    }
    UChar intUnits[3];
    int32_t length;
    if(value<0 || value>UCharTrie::kMaxTwoUnitNodeValue) {
        intUnits[0]=(UChar)(UCharTrie::kThreeUnitNodeValueLead);
        intUnits[1]=(UChar)(value>>16);
        intUnits[2]=(UChar)value;
        length=3;
    } else if(value<=UCharTrie::kMaxOneUnitNodeValue) {
        intUnits[0]=(UChar)((value+1)<<6);
        length=1;
    } else {
        intUnits[0]=(UChar)(UCharTrie::kMinTwoUnitNodeValueLead+((value>>10)&0x7fc0));
        intUnits[1]=(UChar)value;
        length=2;
    }
    intUnits[0]|=(UChar)node;
    return write(intUnits, length);
}

int32_t
UCharTrieBuilder::writeDelta(int32_t i) {
    UChar intUnits[3];
    int32_t length;
    U_ASSERT(i>=0);
    if(i<=UCharTrie::kMaxOneUnitDelta) {
        length=0;
    } else if(i<=UCharTrie::kMaxTwoUnitDelta) {
        intUnits[0]=(UChar)(UCharTrie::kMinTwoUnitDeltaLead+(i>>16));
        length=1;
    } else {
        intUnits[0]=(UChar)(UCharTrie::kThreeUnitDeltaLead);
        intUnits[1]=(UChar)(i>>16);
        length=2;
    }
    intUnits[length++]=(UChar)i;
    return write(intUnits, length);
}

U_NAMESPACE_END
