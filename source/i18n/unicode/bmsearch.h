/*
 ******************************************************************************
 *   Copyright (C) 1996-2008, International Business Machines                 *
 *   Corporation and others.  All Rights Reserved.                            *
 ******************************************************************************
 */

/**
 * \file 
 * \brief C++ API: Boyer-Moore StringSearch prototype
 */
 
#ifndef B_M_SEARCH_H
#define B_M_SEARCH_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/uobject.h"
#include "unicode/ucol.h"

#include "colldata.h"

U_NAMESPACE_BEGIN

class BadCharacterTable;
class GoodSuffixTable;
class Target;

class U_I18N_API BoyerMooreSearch : public UObject
{
public:
    BoyerMooreSearch(CollData *theData, const UnicodeString &patternString, const UnicodeString *targetString);
    ~BoyerMooreSearch();

    UBool empty();

    UBool search(int32_t offset, int32_t &start, int32_t &end);

    void setTargetString(const UnicodeString *targetString);

    // **** no longer need these? ****
    CollData *getData();
    CEList   *getPatternCEs();
    BadCharacterTable *getBadCharacterTable();
    GoodSuffixTable   *getGoodSuffixTable();

    virtual UClassID getDynamicClassID() const;
    static UClassID getStaticClassID();
    
private:
    UBool ownData;
    CollData *data;
    CEList *patCEs;
    BadCharacterTable *badCharacterTable;
    GoodSuffixTable   *goodSuffixTable;
    Target *target;
};

U_NAMESPACE_END

#endif // #if !UCONFIG_NO_COLLATION
#endif // #ifndef B_M_SEARCH_H
