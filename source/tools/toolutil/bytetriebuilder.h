/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
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
#include "dicttriebuilder.h"

U_NAMESPACE_BEGIN

class ByteTrieElement;

class U_TOOLUTIL_API ByteTrieBuilder : public DictTrieBuilder {
public:
    ByteTrieBuilder()
            : elements(NULL), elementsCapacity(0), elementsLength(0),
              bytes(NULL), bytesCapacity(0), bytesLength(0) {}
    virtual ~ByteTrieBuilder();

    ByteTrieBuilder &add(const StringPiece &s, int32_t value, UErrorCode &errorCode);

    StringPiece build(UDictTrieBuildOption buildOption, UErrorCode &errorCode);

    ByteTrieBuilder &clear() {
        strings.clear();
        elementsLength=0;
        bytesLength=0;
        return *this;
    }

private:
    virtual int32_t getElementStringLength(int32_t i) const;
    virtual UChar getElementUnit(int32_t i, int32_t byteIndex) const;
    virtual int32_t getElementValue(int32_t i) const;

    virtual int32_t getLimitOfLinearMatch(int32_t first, int32_t last, int32_t unitIndex) const;

    virtual int32_t countElementUnits(int32_t start, int32_t limit, int32_t byteIndex) const;
    virtual int32_t skipElementsBySomeUnits(int32_t i, int32_t byteIndex, int32_t count) const;
    virtual int32_t indexOfElementWithNextUnit(int32_t i, int32_t byteIndex, UChar byte) const;

    virtual UBool matchNodesCanHaveValues() const { return FALSE; }

    virtual int32_t getMaxBranchLinearSubNodeLength() const { return ByteTrie::kMaxBranchLinearSubNodeLength; }
    virtual int32_t getMinLinearMatch() const { return ByteTrie::kMinLinearMatch; }
    virtual int32_t getMaxLinearMatchLength() const { return ByteTrie::kMaxLinearMatchLength; }

    class BTLinearMatchNode : public LinearMatchNode {
    public:
        BTLinearMatchNode(const char *units, int32_t len, Node *nextNode);
        virtual UBool operator==(const Node &other) const;
        virtual void write(DictTrieBuilder &builder);
    private:
        const char *s;
    };

    virtual Node *createLinearMatchNode(int32_t i, int32_t unitIndex, int32_t length,
                                        Node *nextNode) const;

    UBool ensureCapacity(int32_t length);
    virtual int32_t write(int32_t byte);
    int32_t write(const char *b, int32_t length);
    virtual int32_t writeElementUnits(int32_t i, int32_t byteIndex, int32_t length);
    virtual int32_t writeValueAndFinal(int32_t i, UBool final);
    virtual int32_t writeValueAndType(UBool hasValue, int32_t value, int32_t node);
    virtual int32_t writeDeltaTo(int32_t jumpTarget);

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

U_NAMESPACE_END

#endif  // __BYTETRIEBUILDER_H__
