//  file:  rbbidata.h
//
//**********************************************************************
//   Copyright (C) 1999 IBM Corp. All rights reserved.
//**********************************************************************
//
//   RBBI data formats  Includes
//
//                          Structs that describes the format of the Binary RBBI data,
//                          as it is stored in ICU's data file.
//
//      RBBIDataWrapper  -  Instances of this class sit between the
//                          raw data structs and the RulesBasedBreakIterator objects
//                          that are created by applications.  The wrapper class
//                          provides reference counting for the underlying data,
//                          and direct pointers to data that would not otherwise
//                          be accessible without ugly pointer arithmetic.  The
//                          wrapper does not attempt to provide any higher level
//                          abstractions for the data itself.
//
//                          There will be only one instance of RBBIDataWrapper for any
//                          set of RBBI run time data being shared by instances
//                          (clones) of RulesBasedBreakIterator.
//

#ifndef __RBBIDATA_H__
#define __RBBIDATA_H__

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/udata.h"
#include "utrie.h"


U_NAMESPACE_BEGIN

//
//  The following structs map exactly onto the raw data from ICU common data file.
//
struct RBBIDataHeader {
    uint32_t         fMagic;       // == 0xbla0
    uint32_t         fVersion;     // == 1
    uint32_t         fLength;      // Total length in bytes of this RBBI Data,
                                   //     including all sections, not just the header.
    uint32_t         fCatCount;    // Number of character categories.

    //
    // Offsets and sizes of each of the subsections within the RBBI data.
    // All offsets are bytes from the start of the RBBIDataHeader.
    // All sizes are in bytes.
    //
    uint32_t         fFTable;      // forward state transition table.
    uint32_t         fFTableLen;
    uint32_t         fRTable;      // Offset to the reverse state transition table.
    uint32_t         fRTableLen;
    uint32_t         fTrie;        // Offset to Trie data for character categories
    uint32_t         fTrieLen;
    uint32_t         fRuleSource;  // Offset to the source for for the break
    uint32_t         fRuleSourceLen;  //   rules.  Stored UChar *.

    uint32_t         fReserved[8]; // Reserved for expansion

};



struct  RBBIStateTableRow {
    int16_t          fAccepting;    // Non-zero if this row is for an accepting state.
                                    // Value is the {nnn} value to return to calling
                                    //    application.
    int16_t          fLookAhead;    // Non-zero if this row is for a state that
                                    //   corresponds to a '/' in the rule source.
                                    //   Value is the same as the fAccepting
                                    //     value for the rule (which will appear
                                    //     in a different state.
    int16_t          fTag;          // Non-zero if this row covers a {tagged} position
                                    //    from a rule.  value is the tag number.
    int16_t          fReserved;
    uint16_t         fNextState[2]; // Next State, indexed by char category.
                                    //   Array Size is fNumCols from the
                                    //   state table header.
                                    //   CAUTION:  see RBBITableBuilder::getTableSize()
                                    //             before changing anything here.
};


struct RBBIStateTable {
    uint32_t         fNumStates;    // Number of states.
    uint32_t         fRowLen;       // Length of a state table row, in bytes.
    char             fTableData[4]; // First RBBIStateTableRow begins here.
                                    //   (making it char[] simplifies ugly address
                                    //    arithmetic for indexing variable length rows.)
};


//
//  The reference counting wrapper class
//
class RBBIDataWrapper : public UMemory {
public:
    RBBIDataWrapper(const RBBIDataHeader *data, UErrorCode &status);
    RBBIDataWrapper(UDataMemory* udm, UErrorCode &status);
    ~RBBIDataWrapper();

    void                  init(const RBBIDataHeader *data, UErrorCode &status);
    RBBIDataWrapper      *addReference();
    void                  removeReference();
    UBool                 operator ==(const RBBIDataWrapper &other) const;
    int32_t               hashCode();
    const UnicodeString  &getRuleSourceString();
    void                  printData();

    //
    //  Pointers to items within the data
    //
    const RBBIDataHeader     *fHeader;
    const RBBIStateTable     *fForwardTable;
    const RBBIStateTable     *fReverseTable;
    const UChar              *fRuleSource;

    UTrie               fTrie;

private:
    int32_t             fRefCount;
    UDataMemory        *fUDataMem;
    UnicodeString       fRuleString;

    RBBIDataWrapper(const RBBIDataWrapper &other); // forbid copying of this class
    RBBIDataWrapper &operator=(const RBBIDataWrapper &other); // forbid copying of this class
};

U_NAMESPACE_END

#endif

