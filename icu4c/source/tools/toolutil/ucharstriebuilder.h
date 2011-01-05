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

#ifndef __UCHARSTRIEBUILDER_H__
#define __UCHARSTRIEBUILDER_H__

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "stringtriebuilder.h"
#include "ucharstrie.h"

U_NAMESPACE_BEGIN

class UCharsTrieElement;

/**
 * Builder class for UCharsTrie.
 */
class U_TOOLUTIL_API UCharsTrieBuilder : public StringTrieBuilder {
public:
    UCharsTrieBuilder()
            : elements(NULL), elementsCapacity(0), elementsLength(0),
              uchars(NULL), ucharsCapacity(0), ucharsLength(0) {}
    virtual ~UCharsTrieBuilder();

    UCharsTrieBuilder &add(const UnicodeString &s, int32_t value, UErrorCode &errorCode);

    UnicodeString &build(UStringTrieBuildOption buildOption, UnicodeString &result, UErrorCode &errorCode);

    UCharsTrieBuilder &clear() {
        strings.remove();
        elementsLength=0;
        ucharsLength=0;
        return *this;
    }

private:
    virtual int32_t getElementStringLength(int32_t i) const;
    virtual UChar getElementUnit(int32_t i, int32_t unitIndex) const;
    virtual int32_t getElementValue(int32_t i) const;

    virtual int32_t getLimitOfLinearMatch(int32_t first, int32_t last, int32_t unitIndex) const;

    virtual int32_t countElementUnits(int32_t start, int32_t limit, int32_t unitIndex) const;
    virtual int32_t skipElementsBySomeUnits(int32_t i, int32_t unitIndex, int32_t count) const;
    virtual int32_t indexOfElementWithNextUnit(int32_t i, int32_t unitIndex, UChar unit) const;

    virtual UBool matchNodesCanHaveValues() const { return TRUE; }

    virtual int32_t getMaxBranchLinearSubNodeLength() const { return UCharsTrie::kMaxBranchLinearSubNodeLength; }
    virtual int32_t getMinLinearMatch() const { return UCharsTrie::kMinLinearMatch; }
    virtual int32_t getMaxLinearMatchLength() const { return UCharsTrie::kMaxLinearMatchLength; }

    class UCTLinearMatchNode : public LinearMatchNode {
    public:
        UCTLinearMatchNode(const UChar *units, int32_t len, Node *nextNode);
        virtual UBool operator==(const Node &other) const;
        virtual void write(StringTrieBuilder &builder);
    private:
        const UChar *s;
    };

    virtual Node *createLinearMatchNode(int32_t i, int32_t unitIndex, int32_t length,
                                        Node *nextNode) const;

    UBool ensureCapacity(int32_t length);
    virtual int32_t write(int32_t unit);
    int32_t write(const UChar *s, int32_t length);
    virtual int32_t writeElementUnits(int32_t i, int32_t unitIndex, int32_t length);
    virtual int32_t writeValueAndFinal(int32_t i, UBool final);
    virtual int32_t writeValueAndType(UBool hasValue, int32_t value, int32_t node);
    virtual int32_t writeDeltaTo(int32_t jumpTarget);

    UnicodeString strings;
    UCharsTrieElement *elements;
    int32_t elementsCapacity;
    int32_t elementsLength;

    // UChar serialization of the trie.
    // Grows from the back: ucharsLength measures from the end of the buffer!
    UChar *uchars;
    int32_t ucharsCapacity;
    int32_t ucharsLength;
};

U_NAMESPACE_END

#endif  // __UCHARSTRIEBUILDER_H__
