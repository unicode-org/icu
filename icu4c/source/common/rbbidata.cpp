/*
***************************************************************************
*   Copyright (C) 1999-2003 International Business Machines Corporation   *
*   and others. All rights reserved.                                      *
***************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/utypes.h"
#include "rbbidata.h"
#include "rbbirb.h"
#include "utrie.h"
#include "udatamem.h"
#include "cmemory.h"
#include "cstring.h"
#include "umutex.h"

#include "uassert.h"


//-----------------------------------------------------------------------------------
//
//   Trie access folding function.  Copied as-is from properties code in uchar.c
//
//-----------------------------------------------------------------------------------
U_CDECL_BEGIN
static int32_t U_CALLCONV
getFoldingOffset(uint32_t data) {
    /* if bit 15 is set, then the folding offset is in bits 14..0 of the 16-bit trie result */
    if(data&0x8000) {
        return (int32_t)(data&0x7fff);
    } else {
        return 0;
    }
}
U_CDECL_END

U_NAMESPACE_BEGIN

//-----------------------------------------------------------------------------
//
//    Constructors.
//
//-----------------------------------------------------------------------------
RBBIDataWrapper::RBBIDataWrapper(const RBBIDataHeader *data, UErrorCode &status) {
    init(data, status);
}

RBBIDataWrapper::RBBIDataWrapper(UDataMemory* udm, UErrorCode &status) {
    const RBBIDataHeader *d = (const RBBIDataHeader *)
        ((char *)&(udm->pHeader->info) + udm->pHeader->info.size);
    init(d, status);
    fUDataMem = udm;
}

//-----------------------------------------------------------------------------
//
//    init().   Does most of the work of construction, shared between the
//              constructors.
//
//-----------------------------------------------------------------------------
void RBBIDataWrapper::init(const RBBIDataHeader *data, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }
    fHeader = data;
    if (fHeader->fMagic != 0xb1a0) {
        status = U_BRK_INTERNAL_ERROR;
        return;
    }

    fUDataMem     = NULL;
    fForwardTable = (RBBIStateTable *)((char *)data + fHeader->fFTable);
    fReverseTable = NULL;
    if (data->fRTableLen != 0) {
        fReverseTable = (RBBIStateTable *)((char *)data + fHeader->fRTable);
    }


    utrie_unserialize(&fTrie,
                       (uint8_t *)data + fHeader->fTrie,
                       fHeader->fTrieLen,
                       &status);
    if (U_FAILURE(status)) {
        return;
    }
    fTrie.getFoldingOffset=getFoldingOffset;


    fRuleSource   = (UChar *)((char *)data + fHeader->fRuleSource);
    fRuleString.setTo(TRUE, fRuleSource, -1);

    fRefCount = 1;

#ifdef RBBI_DEBUG
    char *debugEnv = getenv("U_RBBIDEBUG");
    if (debugEnv && uprv_strstr(debugEnv, "data")) {this->printData();}
#endif
}


//-----------------------------------------------------------------------------
//
//    Destructor.     Don't call this - use removeReferenc() instead.
//
//-----------------------------------------------------------------------------
RBBIDataWrapper::~RBBIDataWrapper() {
    U_ASSERT(fRefCount == 0);
    if (fUDataMem) {
        udata_close(fUDataMem);
    } else {
        uprv_free((void *)fHeader);
    }
}



//-----------------------------------------------------------------------------
//
//   Operator ==    Consider two RBBIDataWrappers to be equal if they
//                  refer to the same underlying data.  Although
//                  the data wrappers are normally shared between
//                  iterator instances, it's possible to independently
//                  open the same data twice, and get two instances, which
//                  should still be ==.
//
//-----------------------------------------------------------------------------
UBool RBBIDataWrapper::operator ==(const RBBIDataWrapper &other) const {
    if (fHeader == other.fHeader) {
        return TRUE;
    }
    if (fHeader->fLength != other.fHeader->fLength) {
        return FALSE;
    }
    if (uprv_memcmp(fHeader, other.fHeader, fHeader->fLength) == 0) {
        return TRUE;
    }
    return FALSE;
}

int32_t  RBBIDataWrapper::hashCode() {
    return fHeader->fFTableLen;
}



//-----------------------------------------------------------------------------
//
//    Reference Counting.   A single RBBIDataWrapper object is shared among
//                          however many RulesBasedBreakIterator instances are
//                          referencing the same data.
//
//-----------------------------------------------------------------------------
void RBBIDataWrapper::removeReference() {
    if (umtx_atomic_dec(&fRefCount) == 0) {
        delete this;
    }
}


RBBIDataWrapper *RBBIDataWrapper::addReference() {
   umtx_atomic_inc(&fRefCount);
   return this;
}



//-----------------------------------------------------------------------------
//
//  getRuleSourceString
//
//-----------------------------------------------------------------------------
const UnicodeString &RBBIDataWrapper::getRuleSourceString() {
    return fRuleString;
}


//-----------------------------------------------------------------------------
//
//  print   -  debugging function to dump the runtime data tables.
//
//-----------------------------------------------------------------------------
void  RBBIDataWrapper::printData() {
#ifdef RBBI_DEBUG
    uint32_t c, s;

    RBBIDebugPrintf("RBBI Data at %p\n", (void *)fHeader);
    RBBIDebugPrintf("   Version = %d\n", fHeader->fVersion);
    RBBIDebugPrintf("   total length of data  = %d\n", fHeader->fLength);
    RBBIDebugPrintf("   number of character categories = %d\n\n", fHeader->fCatCount);

    RBBIDebugPrintf("   Forward State Transition Table\n");
    RBBIDebugPrintf("State |  Acc  LA   Tag");
    for (c=0; c<fHeader->fCatCount; c++) {RBBIDebugPrintf("%3d ", c);}
    RBBIDebugPrintf("\n------|---------------"); for (c=0;c<fHeader->fCatCount; c++) {RBBIDebugPrintf("----");}
    RBBIDebugPrintf("\n");

    for (s=0; s<fForwardTable->fNumStates; s++) {
        RBBIStateTableRow *row = (RBBIStateTableRow *)
                                  (fForwardTable->fTableData + (fForwardTable->fRowLen * s));
        RBBIDebugPrintf("%4d  |  %3d %3d %3d ", s, row->fAccepting, row->fLookAhead, row->fTag);
        for (c=0; c<fHeader->fCatCount; c++)  {
            RBBIDebugPrintf("%3d ", row->fNextState[c]);
        }
        RBBIDebugPrintf("\n");
    }

    RBBIDebugPrintf("\nOrignal Rules source:\n");
    c = 0;
    for (;;) {
        if (fRuleSource[c] == 0)
            break;
        RBBIDebugPrintf("%c", fRuleSource[c]);
        c++;
    }
    RBBIDebugPrintf("\n\n");
#endif
}



U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
