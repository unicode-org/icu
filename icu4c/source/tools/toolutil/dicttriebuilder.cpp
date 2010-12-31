/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  dicttriebuilder.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010dec24
*   created by: Markus W. Scherer
*
* Base class for dictionary-trie builder classes.
*/

#include <typeinfo>  // for 'typeid' to work
#include "unicode/utypes.h"
#include "dicttriebuilder.h"
#include "uassert.h"
#include "uhash.h"

U_CDECL_BEGIN

static int32_t U_CALLCONV
hashDictTrieNode(const UHashTok key) {
    return U_NAMESPACE_QUALIFIER DictTrieBuilder::hashNode(key.pointer);
}

static UBool U_CALLCONV
equalDictTrieNodes(const UHashTok key1, const UHashTok key2) {
    return U_NAMESPACE_QUALIFIER DictTrieBuilder::equalNodes(key1.pointer, key2.pointer);
}

U_CDECL_END

U_NAMESPACE_BEGIN

DictTrieBuilder::DictTrieBuilder() : nodes(NULL) {}

DictTrieBuilder::~DictTrieBuilder() {
    deleteCompactBuilder();
}

void
DictTrieBuilder::createCompactBuilder(int32_t sizeGuess, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return;
    }
    nodes=uhash_openSize(hashDictTrieNode, equalDictTrieNodes, NULL,
                         sizeGuess, &errorCode);
    if(U_SUCCESS(errorCode) && nodes==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    if(U_SUCCESS(errorCode)) {
        uhash_setKeyDeleter(nodes, uhash_deleteUObject);
    }
}

void
DictTrieBuilder::deleteCompactBuilder() {
    uhash_close(nodes);
    nodes=NULL;
}

DictTrieBuilder::Node *
DictTrieBuilder::registerNode(Node *newNode, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        delete newNode;
        return NULL;
    }
    if(newNode==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    const UHashElement *old=uhash_find(nodes, newNode);
    if(old!=NULL) {
        delete newNode;
        return (Node *)old->key.pointer;
    }
    // If uhash_puti() returns a non-zero value from an equivalent, previously
    // registered node, then uhash_find() failed to find that and we will leak newNode.
#if !U_RELEASE
    int32_t oldValue=  // Only in debug mode to avoid a compiler warning about unused oldValue.
#endif
    uhash_puti(nodes, newNode, 1, &errorCode);
    U_ASSERT(oldValue==0);
    if(U_FAILURE(errorCode)) {
        delete newNode;
        return NULL;
    }
    return newNode;
}

DictTrieBuilder::Node *
DictTrieBuilder::registerFinalValue(int32_t value, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) {
        return NULL;
    }
    FinalValueNode key(value);
    const UHashElement *old=uhash_find(nodes, &key);
    if(old!=NULL) {
        return (Node *)old->key.pointer;
    }
    Node *newNode=createFinalValueNode(value);
    if(newNode==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    // If uhash_puti() returns a non-zero value from an equivalent, previously
    // registered node, then uhash_find() failed to find that and we will leak newNode.
#if !U_RELEASE
    int32_t oldValue=  // Only in debug mode to avoid a compiler warning about unused oldValue.
#endif
    uhash_puti(nodes, newNode, 1, &errorCode);
    U_ASSERT(oldValue==0);
    if(U_FAILURE(errorCode)) {
        delete newNode;
        return NULL;
    }
    return newNode;
}

UBool DictTrieBuilder::hashNode(const void *node) {
    return ((const Node *)node)->hashCode();
}

UBool DictTrieBuilder::equalNodes(const void *left, const void *right) {
    return *(const Node *)left==*(const Node *)right;
}

UBool DictTrieBuilder::Node::operator==(const Node &other) const {
    return this==&other || (typeid(*this)==typeid(other) && hash==other.hash);
}

int32_t DictTrieBuilder::Node::markRightEdgesFirst(int32_t edgeNumber) {
    if(offset==0) {
        offset=edgeNumber;
    }
    return edgeNumber;
}

UOBJECT_DEFINE_NO_RTTI_IMPLEMENTATION(DictTrieBuilder::Node)

UBool DictTrieBuilder::FinalValueNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    // Not:
    //   if(!Node::operator==(other)) {
    //       return FALSE;
    //   }
    // because registerFinalValue() compares a stack-allocated FinalValueNode
    // (stack-allocated so that we don't unnecessarily create lots of duplicate nodes)
    // with the specific builder's subclass of FinalValueNode,
    // and !Node::operator==(other) will always be false for that because it
    // compares the typeid's.
    // This workaround assumes that the subclass does not add fields that need to be compared.
    if(hash!=other.hashCode()) {
        return FALSE;
    }
    const FinalValueNode *o=dynamic_cast<const FinalValueNode *>(&other);
    return o!=NULL && value==o->value;
}

UBool DictTrieBuilder::ValueNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!Node::operator==(other)) {
        return FALSE;
    }
    const ValueNode &o=(const ValueNode &)other;
    return hasValue==o.hasValue && (!hasValue || value==o.value);
}

UBool DictTrieBuilder::LinearMatchNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!ValueNode::operator==(other)) {
        return FALSE;
    }
    const LinearMatchNode &o=(const LinearMatchNode &)other;
    return length==o.length && next==o.next;
}

int32_t DictTrieBuilder::LinearMatchNode::markRightEdgesFirst(int32_t edgeNumber) {
    if(offset==0) {
        offset=edgeNumber=next->markRightEdgesFirst(edgeNumber);
    }
    return edgeNumber;
}

UBool DictTrieBuilder::ListBranchNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!Node::operator==(other)) {
        return FALSE;
    }
    const ListBranchNode &o=(const ListBranchNode &)other;
    for(int32_t i=0; i<length; ++i) {
        if(units[i]!=o.units[i] || values[i]!=o.values[i] || equal[i]!=o.equal[i]) {
            return FALSE;
        }
    }
    return TRUE;
}

int32_t DictTrieBuilder::ListBranchNode::markRightEdgesFirst(int32_t edgeNumber) {
    if(offset==0) {
        firstEdgeNumber=edgeNumber;
        int32_t step=0;
        int32_t i=length;
        do {
            Node *edge=equal[--i];
            if(edge!=NULL) {
                edgeNumber=edge->markRightEdgesFirst(edgeNumber-step);
            }
            // For all but the rightmost edge, decrement the edge number.
            step=1;
        } while(i>0);
        offset=edgeNumber;
    }
    return edgeNumber;
}

UBool DictTrieBuilder::SplitBranchNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!Node::operator==(other)) {
        return FALSE;
    }
    const SplitBranchNode &o=(const SplitBranchNode &)other;
    return unit==o.unit && lessThan==o.lessThan && greaterOrEqual==o.greaterOrEqual;
}

int32_t DictTrieBuilder::SplitBranchNode::markRightEdgesFirst(int32_t edgeNumber) {
    if(offset==0) {
        firstEdgeNumber=edgeNumber;
        edgeNumber=greaterOrEqual->markRightEdgesFirst(edgeNumber);
        offset=edgeNumber=lessThan->markRightEdgesFirst(edgeNumber-1);
    }
    return edgeNumber;
}

UBool DictTrieBuilder::BranchHeadNode::operator==(const Node &other) const {
    if(this==&other) {
        return TRUE;
    }
    if(!ValueNode::operator==(other)) {
        return FALSE;
    }
    const BranchHeadNode &o=(const BranchHeadNode &)other;
    return length==o.length && next==o.next;
}

int32_t DictTrieBuilder::BranchHeadNode::markRightEdgesFirst(int32_t edgeNumber) {
    if(offset==0) {
        offset=edgeNumber=next->markRightEdgesFirst(edgeNumber);
    }
    return edgeNumber;
}

U_NAMESPACE_END
