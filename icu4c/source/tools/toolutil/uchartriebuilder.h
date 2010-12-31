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

#ifndef __UCHARTRIEBUILDER_H__
#define __UCHARTRIEBUILDER_H__

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "dicttriebuilder.h"

U_NAMESPACE_BEGIN

class UCharTrieElement;

class U_TOOLUTIL_API UCharTrieBuilder : public DictTrieBuilder {
public:
    UCharTrieBuilder()
            : elements(NULL), elementsCapacity(0), elementsLength(0),
              uchars(NULL), ucharsCapacity(0), ucharsLength(0) {}
    ~UCharTrieBuilder();

    UCharTrieBuilder &add(const UnicodeString &s, int32_t value, UErrorCode &errorCode);

    UnicodeString &build(UDictTrieBuildOption buildOption, UnicodeString &result, UErrorCode &errorCode);

    UCharTrieBuilder &clear() {
        strings.remove();
        elementsLength=0;
        ucharsLength=0;
        return *this;
    }

private:
    void writeNode(int32_t start, int32_t limit, int32_t unitIndex);
    void writeBranchSubNode(int32_t start, int32_t limit, int32_t unitIndex, int32_t length);

    Node *makeNode(int32_t start, int32_t limit, int32_t unitIndex, UErrorCode &errorCode);
    Node *makeBranchSubNode(int32_t start, int32_t limit, int32_t unitIndex,
                            int32_t length, UErrorCode &errorCode);

    UBool ensureCapacity(int32_t length);
    int32_t write(int32_t unit);
    int32_t write(const UChar *s, int32_t length);
    int32_t writeValueAndFinal(int32_t i, UBool final);
    int32_t writeValueAndType(UBool hasValue, int32_t value, int32_t node);
    int32_t writeDelta(int32_t i);

    // Compacting builder.
    class UCTFinalValueNode : public FinalValueNode {
    public:
        UCTFinalValueNode(int32_t v) : FinalValueNode(v) {}
        virtual void write(DictTrieBuilder &builder);
    };

    class UCTLinearMatchNode : public LinearMatchNode {
    public:
        UCTLinearMatchNode(const UChar *units, int32_t len, Node *nextNode);
        virtual UBool operator==(const Node &other) const;
        virtual void write(DictTrieBuilder &builder);
    private:
        const UChar *s;
    };

    class UCTListBranchNode : public ListBranchNode {
    public:
        UCTListBranchNode() : ListBranchNode() {}
        virtual void write(DictTrieBuilder &builder);
    };

    class UCTSplitBranchNode : public SplitBranchNode {
    public:
        UCTSplitBranchNode(UChar middleUnit, Node *lessThanNode, Node *greaterOrEqualNode)
                : SplitBranchNode(middleUnit, lessThanNode, greaterOrEqualNode) {}
        virtual void write(DictTrieBuilder &builder);
    };

    class UCTBranchHeadNode : public BranchHeadNode {
    public:
        UCTBranchHeadNode(int32_t len, Node *subNode) : BranchHeadNode(len, subNode) {}
        virtual void write(DictTrieBuilder &builder);
    };

    virtual Node *createFinalValueNode(int32_t value) const { return new UCTFinalValueNode(value); }

    UnicodeString strings;
    UCharTrieElement *elements;
    int32_t elementsCapacity;
    int32_t elementsLength;

    // UChar serialization of the trie.
    // Grows from the back: ucharsLength measures from the end of the buffer!
    UChar *uchars;
    int32_t ucharsCapacity;
    int32_t ucharsLength;
};

U_NAMESPACE_END

#endif  // __UCHARTRIEBUILDER_H__
