/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved. 
 ********************************************************************/
#ifndef _COLL
#include "unicode/coll.h"
#endif

#ifndef _TBLCOLL
#include "unicode/tblcoll.h"
#endif

#ifndef _UNISTR
#include "unicode/unistr.h"
#endif

#ifndef _SORTKEY
#include "unicode/sortkey.h"
#endif

#include "lcukocol.h"

#include "sfwdchit.h"

LotusCollationKoreanTest::LotusCollationKoreanTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance("ko_kr", status);
    myCollation->setDecomposition(Normalizer::DECOMP);
}

LotusCollationKoreanTest::~LotusCollationKoreanTest()
{
    delete myCollation;
}

const UChar LotusCollationKoreanTest::testSourceCases[][LotusCollationKoreanTest::MAX_TOKEN_LEN] = {
	{0xac00, 0}
    
};

const UChar LotusCollationKoreanTest::testTargetCases[][LotusCollationKoreanTest::MAX_TOKEN_LEN] = {
	{0xac01, 0}
};

const Collator::EComparisonResult LotusCollationKoreanTest::results[] = {
    Collator::LESS
};

void LotusCollationKoreanTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
{
    Collator::EComparisonResult compareResult = myCollation->compare(source, target);
    CollationKey sortKey1, sortKey2;
    UErrorCode key1status = U_ZERO_ERROR, key2status = U_ZERO_ERROR; //nos
    myCollation->getCollationKey(source, /*nos*/ sortKey1, key1status );
    myCollation->getCollationKey(target, /*nos*/ sortKey2, key2status );
    if (U_FAILURE(key1status) || U_FAILURE(key2status)) {
        errln("SortKey generation Failed.\n");
        return;
    }
    Collator::EComparisonResult keyResult = sortKey1.compareTo(sortKey2);
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, compareResult, result );
}

void LotusCollationKoreanTest::TestTertiary(/* char* par */)
{
    int32_t i = 0;
	myCollation->setStrength(Collator::TERTIARY);
	
    for (i = 0; i < 1; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
	
}

void LotusCollationKoreanTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite LotusCollationKoreanTest: ");
    switch (index) {
        case 0: name = "TestTertiary";  if (exec)   TestTertiary(/* par */); break;
        default: name = ""; break;
    }
}


