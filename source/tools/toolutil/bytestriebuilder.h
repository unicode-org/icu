/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  bytestriebuilder.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010sep25
*   created by: Markus W. Scherer
*/

#ifndef __BYTESTRIEBUILDER_H__
#define __BYTESTRIEBUILDER_H__

#include "unicode/utypes.h"
#include "unicode/stringpiece.h"
#include "bytestrie.h"
#include "charstr.h"
#include "stringtriebuilder.h"

U_NAMESPACE_BEGIN

class BytesTrieElement;

/**
 * Builder class for BytesTrie.
 */
class U_TOOLUTIL_API BytesTrieBuilder : public StringTrieBuilder {
public:
    BytesTrieBuilder()
            : elements(NULL), elementsCapacity(0), elementsLength(0),
              bytes(NULL), bytesCapacity(0), bytesLength(0) {}
    virtual ~BytesTrieBuilder();

    BytesTrieBuilder &add(const StringPiece &s, int32_t value, UErrorCode &errorCode);

    StringPiece build(UStringTrieBuildOption buildOption, UErrorCode &errorCode);

    BytesTrieBuilder &clear() {
        strings.clear();
        elementsLength=0;
        bytesLength=0;
        return *this;
    }

private:
    virtual int32_t getElementStringLength(int32_t i) const;
    virtual UChar getElementUnit(int32_t i, int32_t byteIndex) const;
    virtual int32_t getElementValue(int32_t i) const;

    virtual int32_t getLimitOfLinearMatch(int32_t first, int32_t last, int32_t byteIndex) const;

    virtual int32_t countElementUnits(int32_t start, int32_t limit, int32_t byteIndex) const;
    virtual int32_t skipElementsBySomeUnits(int32_t i, int32_t byteIndex, int32_t count) const;
    virtual int32_t indexOfElementWithNextUnit(int32_t i, int32_t byteIndex, UChar byte) const;

    virtual UBool matchNodesCanHaveValues() const { return FALSE; }

    virtual int32_t getMaxBranchLinearSubNodeLength() const { return BytesTrie::kMaxBranchLinearSubNodeLength; }
    virtual int32_t getMinLinearMatch() const { return BytesTrie::kMinLinearMatch; }
    virtual int32_t getMaxLinearMatchLength() const { return BytesTrie::kMaxLinearMatchLength; }

    class BTLinearMatchNode : public LinearMatchNode {
    public:
        BTLinearMatchNode(const char *units, int32_t len, Node *nextNode);
        virtual UBool operator==(const Node &other) const;
        virtual void write(StringTrieBuilder &builder);
    private:
        const char *s;
    };

    virtual Node *createLinearMatchNode(int32_t i, int32_t byteIndex, int32_t length,
                                        Node *nextNode) const;

    UBool ensureCapacity(int32_t length);
    virtual int32_t write(int32_t byte);
    int32_t write(const char *b, int32_t length);
    virtual int32_t writeElementUnits(int32_t i, int32_t byteIndex, int32_t length);
    virtual int32_t writeValueAndFinal(int32_t i, UBool isFinal);
    virtual int32_t writeValueAndType(UBool hasValue, int32_t value, int32_t node);
    virtual int32_t writeDeltaTo(int32_t jumpTarget);

    CharString strings;
    BytesTrieElement *elements;
    int32_t elementsCapacity;
    int32_t elementsLength;

    // Byte serialization of the trie.
    // Grows from the back: bytesLength measures from the end of the buffer!
    char *bytes;
    int32_t bytesCapacity;
    int32_t bytesLength;
};

U_NAMESPACE_END

#endif  // __BYTESTRIEBUILDER_H__
