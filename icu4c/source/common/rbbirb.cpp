//
//  file:  rbbirb.cpp
//
//  Copyright (C) 2002-2003, International Business Machines Corporation and others.
//  All Rights Reserved.
//
//  This file contains the RBBIRuleBuilder class implementation.  This is the main class for
//    building (compiling) break rules into the tables required by the runtime
//    RBBI engine.
//

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/brkiter.h"
#include "unicode/rbbi.h"
#include "unicode/ubrk.h"
#include "unicode/unistr.h"
#include "unicode/uniset.h"
#include "unicode/uchar.h"
#include "unicode/uchriter.h"
#include "unicode/parsepos.h"
#include "unicode/parseerr.h"
#include "cmemory.h"
#include "cstring.h"

#include "rbbirb.h"
#include "rbbinode.h"

#include "rbbiscan.h"
#include "rbbisetb.h"
#include "rbbitblb.h"
#include "rbbidata.h"


U_NAMESPACE_BEGIN


//----------------------------------------------------------------------------------------
//
//  Constructor.
//
//----------------------------------------------------------------------------------------
RBBIRuleBuilder::RBBIRuleBuilder(const UnicodeString   &rules,
                                       UParseError     &parseErr,
                                       UErrorCode      &status)
 : fRules(rules)
{
    fStatus     = &status;
    fParseError = &parseErr;
    fDebugEnv   = NULL;
#ifdef RBBI_DEBUG
    fDebugEnv   = getenv("U_RBBIDEBUG");
#endif


    fForwardTree        = NULL;
    fReverseTree        = NULL;
    fForwardTables      = NULL;
    fReverseTables      = NULL;
    fUSetNodes          = new UVector(status);
    fScanner            = new RBBIRuleScanner(this);
    fSetBuilder         = new RBBISetBuilder(this);
    if(fSetBuilder == 0 || fScanner == 0 || fUSetNodes == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
}



//----------------------------------------------------------------------------------------
//
//  Destructor
//
//----------------------------------------------------------------------------------------
RBBIRuleBuilder::~RBBIRuleBuilder() {

    int        i;
    for (i=0; ; i++) {
        RBBINode *n = (RBBINode *)fUSetNodes->elementAt(i);
        if (n==NULL) {
            break;
        }
        delete n;
    }

    delete fUSetNodes;
    delete fSetBuilder;
    delete fForwardTables;
    delete fReverseTables;
    delete fForwardTree;
    delete fReverseTree;
    delete fScanner;
}





//----------------------------------------------------------------------------------------
//
//   flattenData() -  Collect up the compiled RBBI rule data and put it into
//                    the format for saving in ICU data files,
//                    which is also the format needed by the RBBI runtime engine.
//
//----------------------------------------------------------------------------------------
static int32_t align8(int32_t i) {return (i+7) & 0xfffffff8;}

RBBIDataHeader *RBBIRuleBuilder::flattenData() {
    if (U_FAILURE(*fStatus)) {
        return NULL;
    }

    // Remove comments and whitespace from the rules to make it smaller.
    UnicodeString strippedRules(RBBIRuleScanner::stripRules(fRules));

    // Calculate the size of each section in the data.
    //   Sizes here are padded up to a multiple of 8 for better memory alignment.
    //   Sections sizes actually stored in the header are for the actual data
    //     without the padding.
    //
    int32_t headerSize        = align8(sizeof(RBBIDataHeader));
    int32_t forwardTableSize  = align8(fForwardTables->getTableSize());
    int32_t reverseTableSize  = align8(fReverseTables->getTableSize());
    int32_t trieSize          = align8(fSetBuilder->getTrieSize());
    int32_t rulesSize         = align8((strippedRules.length()+1) * sizeof(UChar));

    int32_t         totalSize = headerSize + forwardTableSize + reverseTableSize
                                + trieSize + rulesSize;
    RBBIDataHeader  *data     = (RBBIDataHeader *)uprv_malloc(totalSize);
    if (data == NULL) {
        *fStatus = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    uprv_memset(data, 0, totalSize);


    data->fMagic         = 0xb1a0;
    data->fVersion       = 1;
    data->fLength        = totalSize;
    data->fCatCount      = fSetBuilder->getNumCharCategories();

    data->fFTable        = headerSize;
    data->fFTableLen     = forwardTableSize;
    data->fRTable        = data->fFTable + forwardTableSize;
    data->fRTableLen     = reverseTableSize;
    data->fTrie          = data->fRTable + reverseTableSize;
    data->fTrieLen       = fSetBuilder->getTrieSize();
    data->fRuleSource    = data->fTrie   + trieSize;
    data->fRuleSourceLen = strippedRules.length() * sizeof(UChar);

    uprv_memset(data->fReserved, 0, sizeof(data->fReserved));

    fForwardTables->exportTable((uint8_t *)data + data->fFTable);
    fReverseTables->exportTable((uint8_t *)data + data->fRTable);
    fSetBuilder->serializeTrie ((uint8_t *)data + data->fTrie);
    strippedRules.extract((UChar *)((uint8_t *)data+data->fRuleSource), rulesSize/2+1, *fStatus);

    return data;
}






//----------------------------------------------------------------------------------------
//
//  createRuleBasedBreakIterator    construct from source rules that are passed in
//                                  in a UnicodeString
//
//----------------------------------------------------------------------------------------
BreakIterator *
RBBIRuleBuilder::createRuleBasedBreakIterator( const UnicodeString    &rules,
                                    UParseError      &parseError,
                                    UErrorCode       &status)
{
    if (U_FAILURE(status)) {
        return NULL;
    }

    //
    // Read the input rules, generate a parse tree, symbol table,
    // and list of all Unicode Sets referenced by the rules.
    //
    RBBIRuleBuilder  builder(rules, parseError, status);
    builder.fScanner->parse();
    if (U_FAILURE(status)) {
        return NULL;
    }

    //
    // UnicodeSet processing.
    //    Munge the Unicode Sets to create a set of character categories.
    //    Generate the mapping tables (TRIE) from input 32-bit characters to
    //    the character categories.
    //
    builder.fSetBuilder->build();


    //
    //   Generate the DFA state transition table.
    //
    builder.fForwardTables = new RBBITableBuilder(&builder, &builder.fForwardTree);
    builder.fReverseTables = new RBBITableBuilder(&builder, &builder.fReverseTree);
    if(builder.fForwardTables == NULL || builder.fReverseTables == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    builder.fForwardTables->build();
    builder.fReverseTables->build();
    if (U_FAILURE(status)) {
        return NULL;
    }


    //
    //   Package up the compiled data into a memory image
    //      in the run-time format.
    //
    RBBIDataHeader   *data;
    data = builder.flattenData();


    //
    //  Clean up the compiler related stuff
    //


    //
    //  Create a break iterator from the compiled rules.
    //     (Identical to creation from stored pre-compiled rules)
    //
    RuleBasedBreakIterator *This = new RuleBasedBreakIterator(data, status);
    /* test for NULL */
    if(This == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    if (U_FAILURE(status)) {
        delete This;
        This = NULL;
    }
    return This;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
