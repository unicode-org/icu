//
//  rbbitblb.cpp
//

/*
**********************************************************************
*   Copyright (c) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/unistr.h"
#include "rbbitblb.h"
#include "rbbirb.h"
#include "rbbisetb.h"
#include "rbbidata.h"
#include "cstring.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

RBBITableBuilder::RBBITableBuilder(RBBIRuleBuilder *rb, RBBINode **rootNode) :
 fTree(*rootNode) {
    fRB             = rb;
    fStatus         = fRB->fStatus;
    fDStates        = new UVector(*fStatus);
}



RBBITableBuilder::~RBBITableBuilder() {
    int i;
    for (i=0; i<fDStates->size(); i++) {
        delete (RBBIStateDescriptor *)fDStates->elementAt(i);
    }
    delete   fDStates;
}


//-----------------------------------------------------------------------------
//
//   RBBITableBuilder::build  -  This is the main function for building the DFA state transtion
//                               table from the RBBI rules parse tree.
//
//-----------------------------------------------------------------------------
void  RBBITableBuilder::build() {

    if (U_FAILURE(*fStatus)) {
        return;
    }

    // If there were no rules, just return.  This situation can easily arise
    //   for the reverse rules.
    if (fTree==NULL) {
        return;
    }

    //
    // Walk through the tree, replacing any references to $variables with a copy of the
    //   parse tree for the substition expression.
    //
    fTree = fTree->flattenVariables();
    if (fRB->fDebugEnv && uprv_strstr(fRB->fDebugEnv, "ftree")) {
        RBBIDebugPrintf("Parse tree after flattening variable references.\n");
        fTree->printTree(TRUE);
    }

    //
    // Add a unique right-end marker to the expression.
    //   Appears as a cat-node, left child being the original tree,
    //   right child being the end marker.
    //
    RBBINode *cn = new RBBINode(RBBINode::opCat);
    cn->fLeftChild = fTree;
    fTree->fParent = cn;
    cn->fRightChild = new RBBINode(RBBINode::endMark);
    cn->fRightChild->fParent = cn;
    fTree = cn;

    //
    //  Replace all references to UnicodeSets with the tree for the equivalent
    //      expression.
    //
    fTree->flattenSets();
    if (fRB->fDebugEnv && uprv_strstr(fRB->fDebugEnv, "stree")) {
        RBBIDebugPrintf("Parse tree after flattening Unicode Set references.\n");
        fTree->printTree(TRUE);
    }


    //
    // calculate the functions nullable, firstpos, lastpos and followpos on
    // nodes in the parse tree.
    //    See the alogrithm description in Aho.
    //    Understanding how this works by looking at the code alone will be
    //       nearly impossible.
    //
    calcNullable(fTree);
    calcFirstPos(fTree);
    calcLastPos(fTree);
    calcFollowPos(fTree);
    if (fRB->fDebugEnv && uprv_strstr(fRB->fDebugEnv, "pos")) {
        RBBIDebugPrintf("\n\n");
        printPosSets(fTree);
    }

    //
    // Build the DFA state transition tables.
    //
    buildStateTable();
    flagAcceptingStates();
    flagLookAheadStates();
    flagTaggedStates();
    if (fRB->fDebugEnv && uprv_strstr(fRB->fDebugEnv, "states")) {printStates();};

}



//-----------------------------------------------------------------------------
//
//   calcNullable.    Impossible to explain succinctly.  See Aho, section 3.9
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::calcNullable(RBBINode *n) {
    if (n == NULL) {
        return;
    }
    if (n->fType == RBBINode::setRef ||
        n->fType == RBBINode::endMark ) {
        // These are non-empty leaf node types.
        n->fNullable = FALSE;
        return;
    }

    if (n->fType == RBBINode::lookAhead || n->fType == RBBINode::tag) {
        // Lookahead marker node.  It's a leaf, so no recursion on children.
        // It's nullable because it does not match any literal text from the input stream.
        n->fNullable = TRUE;
        return;
    }


    // The node is not a leaf.
    //  Calculate nullable on its children.
    calcNullable(n->fLeftChild);
    calcNullable(n->fRightChild);

    // Apply functions from table 3.40 in Aho
    if (n->fType == RBBINode::opOr) {
        n->fNullable = n->fLeftChild->fNullable || n->fRightChild->fNullable;
    }
    else if (n->fType == RBBINode::opCat) {
        n->fNullable = n->fLeftChild->fNullable && n->fRightChild->fNullable;
    }
    else if (n->fType == RBBINode::opStar || n->fType == RBBINode::opQuestion) {
        n->fNullable = TRUE;
    }
    else {
        n->fNullable = FALSE;
    }
}




//-----------------------------------------------------------------------------
//
//   calcFirstPos.    Impossible to explain succinctly.  See Aho, section 3.9
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::calcFirstPos(RBBINode *n) {
    if (n == NULL) {
        return;
    }
    if (n->fType == RBBINode::leafChar  ||
        n->fType == RBBINode::endMark   ||
        n->fType == RBBINode::lookAhead ||
        n->fType == RBBINode::tag) {
        // These are non-empty leaf node types.
        n->fFirstPosSet->addElement(n, *fStatus);
        return;
    }

    // The node is not a leaf.
    //  Calculate firstPos on its children.
    calcFirstPos(n->fLeftChild);
    calcFirstPos(n->fRightChild);

    // Apply functions from table 3.40 in Aho
    if (n->fType == RBBINode::opOr) {
        setAdd(n->fFirstPosSet, n->fLeftChild->fFirstPosSet);
        setAdd(n->fFirstPosSet, n->fRightChild->fFirstPosSet);
    }
    else if (n->fType == RBBINode::opCat) {
        setAdd(n->fFirstPosSet, n->fLeftChild->fFirstPosSet);
        if (n->fLeftChild->fNullable) {
            setAdd(n->fFirstPosSet, n->fRightChild->fFirstPosSet);
        }
    }
    else if (n->fType == RBBINode::opStar ||
             n->fType == RBBINode::opQuestion ||
             n->fType == RBBINode::opPlus) {
        setAdd(n->fFirstPosSet, n->fLeftChild->fFirstPosSet);
    }
}



//-----------------------------------------------------------------------------
//
//   calcLastPos.    Impossible to explain succinctly.  See Aho, section 3.9
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::calcLastPos(RBBINode *n) {
    if (n == NULL) {
        return;
    }
    if (n->fType == RBBINode::leafChar  ||
        n->fType == RBBINode::endMark   ||
        n->fType == RBBINode::lookAhead ||
        n->fType == RBBINode::tag) {
        // These are non-empty leaf node types.
        n->fLastPosSet->addElement(n, *fStatus);
        return;
    }

    // The node is not a leaf.
    //  Calculate lastPos on its children.
    calcLastPos(n->fLeftChild);
    calcLastPos(n->fRightChild);

    // Apply functions from table 3.40 in Aho
    if (n->fType == RBBINode::opOr) {
        setAdd(n->fLastPosSet, n->fLeftChild->fLastPosSet);
        setAdd(n->fLastPosSet, n->fRightChild->fLastPosSet);
    }
    else if (n->fType == RBBINode::opCat) {
        setAdd(n->fLastPosSet, n->fRightChild->fLastPosSet);
        if (n->fRightChild->fNullable) {
            setAdd(n->fLastPosSet, n->fLeftChild->fLastPosSet);
        }
    }
    else if (n->fType == RBBINode::opStar     ||
             n->fType == RBBINode::opQuestion ||
             n->fType == RBBINode::opPlus) {
        setAdd(n->fLastPosSet, n->fLeftChild->fLastPosSet);
    }
}



//-----------------------------------------------------------------------------
//
//   calcFollowPos.    Impossible to explain succinctly.  See Aho, section 3.9
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::calcFollowPos(RBBINode *n) {
    if (n == NULL ||
        n->fType == RBBINode::leafChar ||
        n->fType == RBBINode::endMark) {
        return;
    }

    calcFollowPos(n->fLeftChild);
    calcFollowPos(n->fRightChild);

    // Aho rule #1
    if (n->fType == RBBINode::opCat) {
        RBBINode *i;   // is 'i' in Aho's description
        uint32_t     ix;

        UVector *LastPosOfLeftChild = n->fLeftChild->fLastPosSet;

        for (ix=0; ix<(uint32_t)LastPosOfLeftChild->size(); ix++) {
            i = (RBBINode *)LastPosOfLeftChild->elementAt(ix);
            setAdd(i->fFollowPos, n->fRightChild->fFirstPosSet);
        }
    }

    // Aho rule #2
    if (n->fType == RBBINode::opStar ||
        n->fType == RBBINode::opPlus) {
        RBBINode   *i;  // again, n and i are the names from Aho's description.
        uint32_t    ix;

        for (ix=0; ix<(uint32_t)n->fLastPosSet->size(); ix++) {
            i = (RBBINode *)n->fLastPosSet->elementAt(ix);
            setAdd(i->fFollowPos, n->fFirstPosSet);
        }
    }



}


//-----------------------------------------------------------------------------
//
//   buildStateTable()    Determine the set of runtime DFA states and the
//                        transition tables for these states, by the algorithm
//                        of fig. 3.44 in Aho.
//
//                        Most of the comments are quotes of Aho's psuedo-code.
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::buildStateTable() {
    //
    // Add a dummy state 0 - the stop state.  Not from Aho.
    int      lastInputSymbol = fRB->fSetBuilder->getNumCharCategories() - 1;
    RBBIStateDescriptor *failState = new RBBIStateDescriptor(lastInputSymbol, fStatus);
    failState->fPositions = new UVector(*fStatus);
    fDStates->addElement(failState, *fStatus);

    // initially, the only unmarked state in Dstates is firstpos(root),
    //       where toot is the root of the syntax tree for (r)#;
    RBBIStateDescriptor *initialState = new RBBIStateDescriptor(lastInputSymbol, fStatus);
    initialState->fPositions = new UVector(*fStatus);
    setAdd(initialState->fPositions, fTree->fFirstPosSet);
    fDStates->addElement(initialState, *fStatus);

    // while there is an unmarked state T in Dstates do begin
    for (;;) {
        RBBIStateDescriptor *T = NULL;
        int32_t              tx;
        for (tx=1; tx<fDStates->size(); tx++) {
            RBBIStateDescriptor *temp;
            temp = (RBBIStateDescriptor *)fDStates->elementAt(tx);
            if (temp->fMarked == FALSE) {
                T = temp;
                break;
            }
        }
        if (T == NULL) {
            break;
        }

        // mark T;
        T->fMarked = TRUE;

        // for each input symbol a do begin
        int32_t  a;
        for (a = 1; a<=lastInputSymbol; a++) {
            // let U be the set of positions that are in followpos(p)
            //    for some position p in T
            //    such that the symbol at position p is a;
            UVector    *U = NULL;
            RBBINode   *p;
            int32_t     px;
            for (px=0; px<T->fPositions->size(); px++) {
                p = (RBBINode *)T->fPositions->elementAt(px);
                if ((p->fType == RBBINode::leafChar) &&  (p->fVal == a)) {
                    if (U == NULL) {
                        U = new UVector(*fStatus);
                    }
                    setAdd(U, p->fFollowPos);
                }
            }

            // if U is not empty and not in DStates then
            int32_t  ux = 0;
            UBool    UinDstates = FALSE;
            if (U != NULL) {
                U_ASSERT(U->size() > 0);
                int  ix;
                for (ix=0; ix<fDStates->size(); ix++) {
                    RBBIStateDescriptor *temp2;
                    temp2 = (RBBIStateDescriptor *)fDStates->elementAt(ix);
                    if (setEquals(U, temp2->fPositions)) {
                        delete U;
                        U  = temp2->fPositions;
                        ux = ix;
                        UinDstates = TRUE;
                        break;
                    }
                }

                // Add U as an unmarked state to Dstates
                if (!UinDstates)
                {
                    RBBIStateDescriptor *newState = new RBBIStateDescriptor(lastInputSymbol, fStatus);
                    newState->fPositions = U;
                    fDStates->addElement(newState, *fStatus);
                    ux = fDStates->size()-1;
                }

                // Dtran[T, a] := U;
                T->fDtran->setElementAt(ux, a);
            }
        }
    }
}



//-----------------------------------------------------------------------------
//
//   flagAcceptingStates    Identify accepting states.
//                          First get a list of all of the end marker nodes.
//                          Then, for each state s,
//                              if s contains one of the end marker nodes in its list of tree positions then
//                                  s is an accepting state.
//
//-----------------------------------------------------------------------------
void     RBBITableBuilder::flagAcceptingStates() {
    UVector     endMarkerNodes(*fStatus);
    RBBINode    *endMarker;
    int32_t     i;
    int32_t     n;

    fTree->findNodes(&endMarkerNodes, RBBINode::endMark, *fStatus);

    for (i=0; i<endMarkerNodes.size(); i++) {
        endMarker = (RBBINode *)endMarkerNodes.elementAt(i);
        for (n=0; n<fDStates->size(); n++) {
            RBBIStateDescriptor *sd = (RBBIStateDescriptor *)fDStates->elementAt(n);
            if (sd->fPositions->indexOf(endMarker) >= 0) {
                // Any non-zero value for fAccepting means this is an accepting node.
                // The value is what will be returned to the user as the break status.
                // If no other value was specified, force it to -1.
                sd->fAccepting = endMarker->fVal;
                if (sd->fAccepting == 0) {
                    sd->fAccepting = -1;
                }

                // If the end marker node is from a look-ahead rule, set
                //   the fLookAhead field or this state also.
                if (endMarker->fLookAheadEnd) {
                    sd->fLookAhead = sd->fAccepting;
                }
            }
        }
    }
}


//-----------------------------------------------------------------------------
//
//    flagLookAheadStates   Very similar to flagAcceptingStates, above.
//
//-----------------------------------------------------------------------------
void     RBBITableBuilder::flagLookAheadStates() {
    UVector     lookAheadNodes(*fStatus);
    RBBINode    *lookAheadNode;
    int32_t     i;
    int32_t     n;

    fTree->findNodes(&lookAheadNodes, RBBINode::lookAhead, *fStatus);
    for (i=0; i<lookAheadNodes.size(); i++) {
        lookAheadNode = (RBBINode *)lookAheadNodes.elementAt(i);

        for (n=0; n<fDStates->size(); n++) {
            RBBIStateDescriptor *sd = (RBBIStateDescriptor *)fDStates->elementAt(n);
            if (sd->fPositions->indexOf(lookAheadNode) >= 0) {
                sd->fLookAhead = lookAheadNode->fVal;
            }
        }
    }
}




//-----------------------------------------------------------------------------
//
//    flagTaggedStates
//
//-----------------------------------------------------------------------------
void     RBBITableBuilder::flagTaggedStates() {
    UVector     tagNodes(*fStatus);
    RBBINode    *tagNode;
    int32_t     i;
    int32_t     n;

    fTree->findNodes(&tagNodes, RBBINode::tag, *fStatus);
    for (i=0; i<tagNodes.size(); i++) {                   // For each tag node t (all of 'em)
        tagNode = (RBBINode *)tagNodes.elementAt(i);

        for (n=0; n<fDStates->size(); n++) {              //    For each state  s (row in the state table)
            RBBIStateDescriptor *sd = (RBBIStateDescriptor *)fDStates->elementAt(n);
            if (sd->fPositions->indexOf(tagNode) >= 0) {  //       if  s include the tag node t
                if (sd->fTagVal < tagNode->fVal) {
                    // If more than one rule tag applies to this state, the larger
                    //   tag takes precedence.
                sd->fTagVal = tagNode->fVal;
            }
        }
    }
}
}



//-----------------------------------------------------------------------------
//
//  setAdd     Set operation on UVector
//             dest = dest union source
//             Elements may only appear once.   Order is unimportant.
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::setAdd(UVector *dest, UVector *source) {
    int destOriginalSize = dest->size();
    int sourceSize       = source->size();
    int32_t  si, di;

    for (si=0; si<sourceSize; si++) {
        void *elToAdd = source->elementAt(si);
        for (di=0; di<destOriginalSize; di++) {
            if (dest->elementAt(di) == elToAdd) {
                goto  elementAlreadyInDest;
            }
        }
        dest->addElement(elToAdd, *fStatus);
    elementAlreadyInDest: ;
    }
}


//-----------------------------------------------------------------------------
//
//  setEqual    Set operation on UVector.
//              Compare for equality.
//              Elements may appear only once.
//              Elements may appear in any order.
//
//-----------------------------------------------------------------------------
UBool RBBITableBuilder::setEquals(UVector *a, UVector *b) {
    int32_t    aSize = a->size();
    int32_t    bSize = b->size();

    if (aSize != bSize) {
        return FALSE;
    }

    int32_t  ax;
    int32_t  bx;
    int32_t  firstBx = 0;
    void     *aVal;
    void     *bVal = NULL;

    for (ax=0; ax<aSize; ax++) {
        aVal = a->elementAt(ax);
        for (bx=firstBx; bx<bSize; bx++) {
            bVal = b->elementAt(bx);
            if (aVal == bVal) {
                if (bx==firstBx) {
                    firstBx++;
                }
                break;
            }
        }
        if (aVal != bVal) {
            return FALSE;
        }
    }
    return TRUE;
}


//-----------------------------------------------------------------------------
//
//  printPosSets   Debug function.  Dump Nullable, firstpos, lastpos and followpos
//                 for each node in the tree.
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::printPosSets(RBBINode *n) {
#ifdef RBBI_DEBUG
    if (n==NULL) {
        return;
    }
    n->print();
    RBBIDebugPrintf("         Nullable:  %s\n", n->fNullable?"TRUE":"FALSE");

    RBBIDebugPrintf("         firstpos:  ");
    printSet(n->fFirstPosSet);

    RBBIDebugPrintf("         lastpos:   ");
    printSet(n->fLastPosSet);

    RBBIDebugPrintf("         followpos: ");
    printSet(n->fFollowPos);

    printPosSets(n->fLeftChild);
    printPosSets(n->fRightChild);
#endif
}



//-----------------------------------------------------------------------------
//
//   getTableSize()    Calculate the size of the runtime form of this
//                     state transition table.
//
//-----------------------------------------------------------------------------
int32_t  RBBITableBuilder::getTableSize() {
    int32_t    size = 0;
    int32_t    numRows;
    int32_t    numCols;
    int32_t    rowSize;

    if (fTree == NULL) {
        return 0;
    }

    size    = sizeof(RBBIStateTable) - 4;    // The header, with no rows to the table.

    numRows = fDStates->size();
    numCols = fRB->fSetBuilder->getNumCharCategories();

    //  Note  The declaration of RBBIStateTableRow is for a table of two columns.
    //        Therefore we subtract two from numCols when determining
    //        how much storage to add to a row for the total columns.
    rowSize = sizeof(RBBIStateTableRow) + sizeof(uint16_t)*(numCols-2);
    size   += numRows * rowSize;
    return size;
}



//-----------------------------------------------------------------------------
//
//   exportTable()    export the state transition table in the format required
//                    by the runtime engine.  getTableSize() bytes of memory
//                    must be available at the output address "where".
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::exportTable(void *where) {
    RBBIStateTable    *table = (RBBIStateTable *)where;
    uint32_t           state;
    int                col;

    if (U_FAILURE(*fStatus) || fTree == NULL) {
        return;
    }

    if (fRB->fSetBuilder->getNumCharCategories() > 0x7fff ||
        fDStates->size() > 0x7fff) {
        *fStatus = U_BRK_INTERNAL_ERROR;
        return;
    }

    table->fRowLen    = sizeof(RBBIStateTableRow) +
                            sizeof(uint16_t) * (fRB->fSetBuilder->getNumCharCategories() - 2);
    table->fNumStates = fDStates->size();

    for (state=0; state<table->fNumStates; state++) {
        RBBIStateDescriptor *sd = (RBBIStateDescriptor *)fDStates->elementAt(state);
        RBBIStateTableRow   *row = (RBBIStateTableRow *)(table->fTableData + state*table->fRowLen);
        U_ASSERT (-32768 < sd->fAccepting && sd->fAccepting <= 32767);
        U_ASSERT (-32768 < sd->fLookAhead && sd->fLookAhead <= 32767);
        row->fAccepting = (int16_t)sd->fAccepting;
        row->fLookAhead = (int16_t)sd->fLookAhead;
        row->fTag       = (int16_t)sd->fTagVal;
        for (col=0; col<fRB->fSetBuilder->getNumCharCategories(); col++) {
            row->fNextState[col] = (uint16_t)sd->fDtran->elementAti(col);
        }
    }
}



//-----------------------------------------------------------------------------
//
//   printSet    Debug function.   Print the contents of a UVector
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::printSet(UVector *s) {
#ifdef RBBI_DEBUG
    int32_t  i;
    for (i=0; i<s->size(); i++) {
        void *v = s->elementAt(i);
        RBBIDebugPrintf("%10p", v);
    }
    RBBIDebugPrintf("\n");
#endif
}


//-----------------------------------------------------------------------------
//
//   printStates    Debug Function.  Dump the fully constructed state transition table.
//
//-----------------------------------------------------------------------------
void RBBITableBuilder::printStates() {
#ifdef RBBI_DEBUG
    int     c;    // input "character"
    int     n;    // state number

    RBBIDebugPrintf("state |           i n p u t     s y m b o l s \n");
    RBBIDebugPrintf("      | Acc  LA    Tag");
    for (c=0; c<fRB->fSetBuilder->getNumCharCategories(); c++) {RBBIDebugPrintf(" %2d", c);};
    RBBIDebugPrintf("\n");
    RBBIDebugPrintf("      |---------------");
    for (c=0; c<fRB->fSetBuilder->getNumCharCategories(); c++) {RBBIDebugPrintf("---");};
    RBBIDebugPrintf("\n");

    for (n=0; n<fDStates->size(); n++) {
        RBBIStateDescriptor *sd = (RBBIStateDescriptor *)fDStates->elementAt(n);
        RBBIDebugPrintf("  %3d | " , n);
        RBBIDebugPrintf("%3d %3d %5d ", sd->fAccepting, sd->fLookAhead, sd->fTagVal);
        for (c=0; c<fRB->fSetBuilder->getNumCharCategories(); c++) {
            RBBIDebugPrintf(" %2d", sd->fDtran->elementAti(c));
        }
        RBBIDebugPrintf("\n");
    }
    RBBIDebugPrintf("\n\n");
#endif
}





//-----------------------------------------------------------------------------
//
//   RBBIStateDescriptor     Methods.  This is a very struct-like class
//                           Most access is directly to the fields.
//
//-----------------------------------------------------------------------------

RBBIStateDescriptor::RBBIStateDescriptor(int lastInputSymbol, UErrorCode *fStatus) {
    fMarked    = FALSE;
    fAccepting = 0;
    fLookAhead = 0;
    fTagVal    = 0;
    fPositions = NULL;
    fDtran     = NULL;
    if (U_FAILURE(*fStatus)) {
        return;
    }
    fDtran     = new UVector(lastInputSymbol+1, *fStatus);
    if (fDtran == NULL) {
        *fStatus = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    fDtran->setSize(lastInputSymbol+1);    // fDtran needs to be pre-sized.
                                           //   It is indexed by input symbols, and will
                                           //   hold  the next state number for each
                                           //   symbol.
}


RBBIStateDescriptor::~RBBIStateDescriptor() {
    delete       fPositions;
    delete       fDtran;
    fPositions = NULL;
    fDtran     = NULL;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
