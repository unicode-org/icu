/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu/source/test/intltest/canittst.h,v $ 
 * $Date: 2002/03/19 07:18:06 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 * @author Mark E. Davis
 * @author Vladimir Weinstein
 */

/**
 * Test Canonical Iterator
 */

#ifndef _CANITTST
#define _CANITTST

#include "unicode/normlzr.h"
#include "unicode/translit.h"
#include "intltest.h"
#include "hash.h"

class CanonicalIteratorTest : public IntlTest {
public:
    CanonicalIteratorTest();
    virtual ~CanonicalIteratorTest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    void TestCanonicalIterator(void);
    void TestExhaustive(void);
    void TestBasic();
    UnicodeString collectionToString(Hashtable *col);
    //static UnicodeString collectionToString(Collection col);
private:
    void expectEqual(const UnicodeString &message, const UnicodeString &item, const UnicodeString &a, const UnicodeString &b);

    Transliterator *name;
    Transliterator *hex;
        
    UnicodeString getReadable(const UnicodeString &obj);
};

#endif // _CANITTST
