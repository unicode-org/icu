/*
**********************************************************************
*   Copyright (C) 1999-2002 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*/

#include "unicode/utypes.h"
#include "rbbidata.h"
#include "utrie.h"
#include "udatamem.h"
#include "cmemory.h"

#include "uassert.h"
#include <stdio.h>


U_NAMESPACE_BEGIN

const char RBBIDataWrapper::fgClassID=0;

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

    char *debugEnv = getenv("U_RBBIDEBUG");      // TODO:  make conditional on some compile time setting
    if (debugEnv && strstr(debugEnv, "data")) {this->printData();}

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
;
};



//-----------------------------------------------------------------------------
//
//    Reference Counting.   A single RBBIDataWrapper object is shared among
//                          however many RulesBasedBreakIterator instances are
//                          referencing the same data.
//
//-----------------------------------------------------------------------------
void RBBIDataWrapper::removeReference() {
    if (--fRefCount <= 0) {            // TODO   needs synchronization
        delete this;
    }
};


RBBIDataWrapper *RBBIDataWrapper::addReference() {
   ++fRefCount;                         // TODO:  needs synchronization
   return this;
};



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
    uint32_t c, s;

    printf("RBBI Data at %p\n", (void *)fHeader);
    printf("   Version = %d\n", fHeader->fVersion);
    printf("   total length of data  = %d\n", fHeader->fLength);
    printf("   number of character categories = %d\n\n", fHeader->fCatCount);

    printf("   Forward State Transition Table\n");
    printf("State |  Acc  LA   Tag");
    for (c=0; c<fHeader->fCatCount; c++) {printf("%3d ", c);};
    printf("\n------|---------------"); for (c=0;c<fHeader->fCatCount; c++) {printf("----");}
    printf("\n");

    for (s=0; s<fForwardTable->fNumStates; s++) {
        RBBIStateTableRow *row = (RBBIStateTableRow *)
                                  (fForwardTable->fTableData + (fForwardTable->fRowLen * s));
        printf("%4d  |  %3d %3d %3d ", s, row->fAccepting, row->fLookAhead, row->fTag);
        for (c=0; c<fHeader->fCatCount; c++)  {
            printf("%3d ", row->fNextState[c]);
        };
        printf("\n");
    }

    printf("\nOrignal Rules source:\n");
    c = 0;
    for (;;) {
        if (fRuleSource[c] == 0)
            break;
        putchar(fRuleSource[c]);
        c++;
    }
    printf("\n\n");
}








U_NAMESPACE_END
