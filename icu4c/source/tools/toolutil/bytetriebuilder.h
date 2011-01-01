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
    void writeNode(int32_t start, int32_t limit, int32_t byteIndex);
    void writeBranchSubNode(int32_t start, int32_t limit, int32_t byteIndex, int32_t length);

    Node *makeNode(int32_t start, int32_t limit, int32_t byteIndex, UErrorCode &errorCode);
    Node *makeBranchSubNode(int32_t start, int32_t limit, int32_t byteIndex,
                            int32_t length, UErrorCode &errorCode);

    UBool ensureCapacity(int32_t length);
    int32_t write(int32_t byte);
    int32_t write(const char *b, int32_t length);
    int32_t writeValueAndFinal(int32_t i, UBool final);
    int32_t writeDelta(int32_t i);

    // Compacting builder.

    // Indirect "friend" access.
    // Nested classes cannot be friends of ByteTrie unless the whole header is included,
    // at least with AIX xlC_r,
    // so this Builder class, which is a friend, provides the necessary value.
    static int32_t minLinearMatch() { return ByteTrie::kMinLinearMatch; }

    class BTFinalValueNode : public FinalValueNode {
    public:
        BTFinalValueNode(int32_t v) : FinalValueNode(v) {}
        virtual void write(DictTrieBuilder &builder);
    };

    class BTValueNode : public ValueNode {
    public:
        BTValueNode(int32_t v, Node *nextNode)
                : ValueNode(0x222222*37+hashCode(nextNode)), next(nextNode) { setValue(v); }
        virtual UBool operator==(const Node &other) const;
        virtual int32_t markRightEdgesFirst(int32_t edgeNumber);
        virtual void write(DictTrieBuilder &builder);
    private:
        Node *next;
    };

    class BTLinearMatchNode : public LinearMatchNode {
    public:
        BTLinearMatchNode(const char *units, int32_t len, Node *nextNode);
        virtual UBool operator==(const Node &other) const;
        virtual void write(DictTrieBuilder &builder);
    private:
        const char *s;
    };

    class BTListBranchNode : public ListBranchNode {
    public:
        BTListBranchNode() : ListBranchNode() {}
        virtual void write(DictTrieBuilder &builder);
    };

    class BTSplitBranchNode : public SplitBranchNode {
    public:
        BTSplitBranchNode(char middleUnit, Node *lessThanNode, Node *greaterOrEqualNode)
                : SplitBranchNode((uint8_t)middleUnit, lessThanNode, greaterOrEqualNode) {}
        virtual void write(DictTrieBuilder &builder);
    };

    class BTBranchHeadNode : public BranchHeadNode {
    public:
        BTBranchHeadNode(int32_t len, Node *subNode) : BranchHeadNode(len, subNode) {}
        virtual void write(DictTrieBuilder &builder);
    };

    virtual Node *createFinalValueNode(int32_t value) const { return new BTFinalValueNode(value); }

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
