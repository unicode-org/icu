/*
**********************************************************************
*   Copyright (C) 2002 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*/

//
//  File:  rbbinode.cpp
//
//         Implementation of class RBBINode, which represents a node in the
//         tree generated when parsing the Rules Based Break Iterator rules.
//
//         This "Class" is actually closer to a struct.
//         Code using it is expected to directly access fields much of the time.
//

#include "unicode/unistr.h"
#include "unicode/uniset.h"
#include "unicode/uchar.h"
#include "unicode/parsepos.h"
#include "uvector.h"

#include "rbbirb.h"
#include "rbbinode.h"

#include "uassert.h"

#include <stdio.h>     // TODO - getrid of this.


U_NAMESPACE_BEGIN

const char RBBINode::fgClassID=0;

int  RBBINode::gLastSerial = 0;



//-------------------------------------------------------------------------
//
//    Constructor.   Just set the fields to reasonable default values.
//
//-------------------------------------------------------------------------
RBBINode::RBBINode(NodeType t) : UObject() {
    fSerialNum    = ++gLastSerial;
    fType         = t;
    fParent       = NULL;
    fLeftChild    = NULL;
    fRightChild   = NULL;
    fInputSet     = NULL;
    fFirstPos     = 0;
    fLastPos      = 0;
    fNullable     = FALSE;
    fLookAheadEnd = FALSE;
    fVal          = 0;

    UErrorCode     status = U_ZERO_ERROR;
    fFirstPosSet  = new UVector(status);  // TODO - get a real status from somewhere
    fLastPosSet   = new UVector(status);
    fFollowPos    = new UVector(status);
    if      (t==opCat) {fPrecedence = precOpCat;}
    else if (t==opOr)  {fPrecedence = precOpOr;}
    else if (t==opStart) {fPrecedence = precStart;}
    else if (t= opLParen) {fPrecedence = precLParen;}

};


RBBINode::RBBINode(const RBBINode &other) : UObject(other) {
    fSerialNum   = ++gLastSerial;
    fType        = other.fType;
    fParent      = NULL;
    fLeftChild   = NULL;
    fRightChild  = NULL;
    fInputSet    = other.fInputSet;
    fPrecedence  = other.fPrecedence;
    fText        = other.fText;
    fFirstPos    = other.fFirstPos;
    fLastPos     = other.fLastPos;
    fNullable    = other.fNullable;
    fVal         = other.fVal;
    UErrorCode     status = U_ZERO_ERROR;
    fFirstPosSet = new UVector(status);   // TODO - get a real status from somewhere
    fLastPosSet  = new UVector(status);
    fFollowPos   = new UVector(status);
};


//-------------------------------------------------------------------------
//
//    Destructor.   Deletes both this node AND any child nodes,
//                  except in the case of variable reference nodes.  For
//                  these, the l. child points back to the definition, which
//                  is common for all references to the variable, meaning
//                  it can't be deleted here.
//
//-------------------------------------------------------------------------
RBBINode::~RBBINode() {
    // printf("deleting node %8x   serial %4d\n", this, this->fSerialNum);
    delete fInputSet;
    fInputSet = NULL;

    switch (this->fType) {
    case varRef:
    case setRef:
        // for these node types, multiple instances point to the same "children"
        // Storage ownership of children handled elsewhere.  Don't delete here.
        break;

    case uset:
        delete fLeftChild;
        // For usets, don't delete the right child; it's used to form a linked list of usets.
        break;

    default:
        delete        fLeftChild;
        fLeftChild =   NULL;
        delete        fRightChild;
        fRightChild = NULL;
    }


    delete fFirstPosSet;
    delete fLastPosSet;
    delete fFollowPos;

}


//-------------------------------------------------------------------------
//
//    cloneTree     Make a copy of the subtree rooted at this node.
//                  Discard any variable references encountered along the way,
//                  and replace with copies of the variable's definitions.
//                  Used to replicate the expression underneath variable
//                  references in preparation for generating the DFA tables.
//
//-------------------------------------------------------------------------
RBBINode *RBBINode::cloneTree() {
    RBBINode    *n;

    if (fType == RBBINode::varRef) {
        // If the current node is a variable reference, skip over it
        //   and clone the definition of the variable instead.
        n = fLeftChild->cloneTree();
    } else if (fType == RBBINode::uset) {
        n = this;
    } else {
        n = new RBBINode(*this);
        if (fLeftChild != NULL) {
            n->fLeftChild          = fLeftChild->cloneTree();
            n->fLeftChild->fParent = n;
        }
        if (fRightChild != NULL) {
            n->fRightChild          = fRightChild->cloneTree();
            n->fRightChild->fParent = n;
        }
    }
    return n;
};



//-------------------------------------------------------------------------
//
//   flattenVariables   Walk a parse tree, replacing any variable
//                      references with a copy of the variable's definition.
//                      Aside from variables, the tree is not changed.
//
//                      This function works by recursively walking the tree
//                      without doing anything until a variable reference is
//                      found, then calling cloneTree() at that point.  Any
//                      nested references are handled by cloneTree(), not here.
//
//-------------------------------------------------------------------------
void RBBINode::flattenVariables() {
    U_ASSERT(fType != varRef);

    if (fLeftChild != NULL) {
        if (fLeftChild->fType==varRef) {
            RBBINode *oldChild   = fLeftChild;
            fLeftChild           = oldChild->cloneTree();
            fLeftChild->fParent  = this;
            delete oldChild;
        } else {
            fLeftChild->flattenVariables();
        }
    }

    if (fRightChild != NULL) {
        if (fRightChild->fType==varRef) {
            RBBINode *oldChild   = fRightChild;
            fRightChild          = oldChild->cloneTree();
            fRightChild->fParent = this;
            delete oldChild;
        } else {
            fRightChild->flattenVariables();
        }
    }
}



//-------------------------------------------------------------------------
//
//  flattenSets    Walk the parse tree, replacing any nodes of type setRef
//                 with a copy of the expression tree for the set.  A set's
//                 equivalent expression tree is precomputed and saved as
//                 the left child of the uset node.
//
//-------------------------------------------------------------------------
void RBBINode::flattenSets() {
    U_ASSERT(fType != setRef);

    if (fLeftChild != NULL) {
        if (fLeftChild->fType==setRef) {
            RBBINode *setRefNode = fLeftChild;
            RBBINode *usetNode   = setRefNode->fLeftChild;
            RBBINode *replTree   = usetNode->fLeftChild;
            fLeftChild           = replTree->cloneTree();
            fLeftChild->fParent  = this;
            delete setRefNode;
        } else {
            fLeftChild->flattenSets();
        }
    }

    if (fRightChild != NULL) {
        if (fRightChild->fType==setRef) {
            RBBINode *setRefNode = fRightChild;
            RBBINode *usetNode   = setRefNode->fLeftChild;
            RBBINode *replTree   = usetNode->fLeftChild;
            fRightChild           = replTree->cloneTree();
            fRightChild->fParent  = this;
            delete setRefNode;
        } else {
            fRightChild->flattenSets();
        }
    }
}



//-------------------------------------------------------------------------
//
//   findNodes()     Locate all the nodes of the specified type, starting
//                   at the specified root.
//
//-------------------------------------------------------------------------
void   RBBINode::findNodes(UVector *dest, RBBINode::NodeType kind, UErrorCode &status) {
    /* test for buffer overflows */
    if (U_FAILURE(status)) {
        return;
    }
    if (fType == kind) {
        dest->addElement(this, status);
    }
    if (fLeftChild != NULL) {
        fLeftChild->findNodes(dest, kind, status);
    }
    if (fRightChild !=NULL && fType != RBBINode::uset) {
        fRightChild->findNodes(dest, kind, status);
    }
}


//-------------------------------------------------------------------------
//
//    print.         Print out a single node, for debugging.
//
//-------------------------------------------------------------------------
static const char *nodeTypeNames[] = {
            "setRef",
            "uset",
            "varRef",
            "leafChar",
            "lookAhead",
            "tag",
            "endMark",
            "opStart",
            "opCat",
            "opOr",
            "opStar",
            "opPlus",
            "opQuestion",
            "opBreak",
            "opReverse",
            "opLParen"
};

void RBBINode::print() {
    printf("%10x  %12s  %10x  %10x  %10x      %4d     %6d   %d ",
        this, nodeTypeNames[fType], fParent, fLeftChild, fRightChild,
        fSerialNum, fFirstPos, fVal);
    if (fType == varRef) {
        printUnicodeString(fText);
    }
    putc('\n', stdout);
}


void RBBINode::printUnicodeString(const UnicodeString &s, int minWidth)
{
    int i;
    for (i=0; i<s.length(); i++) {
        putc(s.charAt(i), stdout);
    }
    for (i=s.length(); i<minWidth; i++) {
        putc(' ', stdout);
    }
}


//-------------------------------------------------------------------------
//
//    print.         Print out the tree of nodes rooted at "this"
//
//-------------------------------------------------------------------------
void RBBINode::printTree(UBool printHeading, UBool doVars) {
    if (printHeading) {
        printf( "-------------------------------------------------------------------\n"
                "    Address       type         Parent   LeftChild  RightChild    serial  position value\n"
              );
    }
    this->print();
    // Only dump the definition under a variable reference if asked to.
    // Unconditinally dump children of all other node types.
    if (fType != varRef || doVars) {
        if (fLeftChild != NULL) {
            fLeftChild->printTree(FALSE);
        }

        // Note:  The right child field of uset nodes is borrowed to link them into a list
        //        They are actually a leaf node as far as the tree is concerned.
        if (fRightChild != NULL  && this->fType != RBBINode::uset) {
            fRightChild->printTree(FALSE);
        }
    }
}



U_NAMESPACE_END


