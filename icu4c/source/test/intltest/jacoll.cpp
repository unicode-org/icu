/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
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

#ifndef _JACOLL
#include "jacoll.h"
#endif

#include "sfwdchit.h"

CollationKanaTest::CollationKanaTest()
: myCollation(0)
{
    UErrorCode status = U_ZERO_ERROR;
    myCollation = Collator::createInstance(Locale::JAPAN, status);
    if(!myCollation || U_FAILURE(status)) {
      errln(__FILE__ "failed to create! err " + UnicodeString(u_errorName(status)));
	/* if it wasn't already: */
	delete myCollation;
	myCollation = NULL;
	return;
    }

    myCollation->setDecomposition(Normalizer::DECOMP);
}

CollationKanaTest::~CollationKanaTest()
{
    delete myCollation;
}

const UChar CollationKanaTest::testSourceCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
    {0xff9E, 0x0000},
    {0x3042, 0x0000},
    {0x30A2, 0x0000},
    {0x3042, 0x3042, 0x0000},
    {0x30A2, 0x30FC, 0x0000},
    {0x30A2, 0x30FC, 0x30C8, 0x0000}                               /*  6 */
};

const UChar CollationKanaTest::testTargetCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
    {0xFF9F, 0x0000},
    {0x30A2, 0x0000},
    {0x3042, 0x3042, 0x0000},
    {0x30A2, 0x30FC, 0x0000},
    {0x30A2, 0x30FC, 0x30C8, 0x0000},
    {0x3042, 0x3042, 0x3068, 0x0000}                              /*  6 */
};

const Collator::EComparisonResult CollationKanaTest::results[] = {
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::LESS,
    Collator::GREATER 
};

const UChar CollationKanaTest::testBaseCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
  {0x30AB, 0x0000},
  {0x30AB, 0x30AD, 0x0000},
  {0x30AD, 0x0000},
  {0x30AD, 0x30AD, 0x0000}
};

const UChar CollationKanaTest::testPlainDakutenHandakutenCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
  {0x30CF, 0x30AB, 0x0000},
  {0x30D0, 0x30AB, 0x0000},
  {0x30CF, 0x30AD, 0x0000},
  {0x30D0, 0x30AD, 0x0000}
};

const UChar CollationKanaTest::testSmallLargeCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
  {0x30C3, 0x30CF, 0x0000},
  {0x30C4, 0x30CF, 0x0000},
  {0x30C3, 0x30D0, 0x0000},
  {0x30C4, 0x30D0, 0x0000}
};

const UChar CollationKanaTest::testKatakanaHiraganaCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
  {0x3042, 0x30C3, 0x0000},
  {0x30A2, 0x30C3, 0x0000},
  {0x3042, 0x30C4, 0x0000},
  {0x30A2, 0x30C4, 0x0000}
};

const UChar CollationKanaTest::testChooonKigooCases[][CollationKanaTest::MAX_TOKEN_LEN] = {
  /*0*/ {0x30AB, 0x30FC, 0x3042, 0x0000},
  /*1*/ {0x30AB, 0x30FC, 0x30A2, 0x0000},
  /*2*/ {0x30AB, 0x30A4, 0x3042, 0x0000},
  /*3*/ {0x30AB, 0x30A4, 0x30A2, 0x0000},
  /*4*/ {0x30AD, 0x30A4, 0x3042, 0x0000},
  /*5*/ {0x30AD, 0x30A4, 0x30A2, 0x0000},
  /*6*/ {0x30AD, 0x30FC, 0x3042, 0x0000},
  /*7*/ {0x30AD, 0x30FC, 0x30A2, 0x0000}
};

void CollationKanaTest::doTest( UnicodeString source, UnicodeString target, Collator::EComparisonResult result)
{
    Collator::EComparisonResult compareResult = myCollation->compare(source, target);
    SimpleFwdCharIterator src(source);
    SimpleFwdCharIterator trg(target);
    Collator::EComparisonResult incResult = myCollation->compare(src, trg);
    CollationKey sortKey1, sortKey2;
    UErrorCode key1status = U_ZERO_ERROR, key2status = U_ZERO_ERROR; //nos
    myCollation->getCollationKey(source, /*nos*/ sortKey1, key1status );
    myCollation->getCollationKey(target, /*nos*/ sortKey2, key2status );
    if (U_FAILURE(key1status) || U_FAILURE(key2status)) {
        errln("SortKey generation Failed.\n");
        return;
    }
    Collator::EComparisonResult keyResult = sortKey1.compareTo(sortKey2);
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, incResult, result );
}

void CollationKanaTest::TestTertiary(/* char* par */)
{
    int32_t i = 0;
    UErrorCode status = U_ZERO_ERROR;
    myCollation->setStrength(Collator::TERTIARY);
    /* for one case, strcollinc fails, since it doesn't have good handling of contractions*/
    /* normalization is turned off to stop strcollinc from executing */
    myCollation->setAttribute(UCOL_NORMALIZATION_MODE, UCOL_ON, status);
    myCollation->setAttribute(UCOL_CASE_LEVEL, UCOL_ON, status);
    for (i = 0; i < 6; i++) {
        doTest(testSourceCases[i], testTargetCases[i], results[i]);
    }
}

/* Testing base letters */
void CollationKanaTest::TestBase()
{
  int32_t i;
  UErrorCode status = U_ZERO_ERROR;
  myCollation->setStrength(Collator::PRIMARY);
  for (i = 0; i < 3 ; i++)
    doTest(testBaseCases[i], testBaseCases[i + 1], Collator::LESS);
}

/* Testing plain, Daku-ten, Handaku-ten letters */
void CollationKanaTest::TestPlainDakutenHandakuten(void)
{
  int32_t i;
  UErrorCode status = U_ZERO_ERROR;
  myCollation->setStrength(Collator::SECONDARY);
  for (i = 0; i < 3 ; i++)
    doTest(testPlainDakutenHandakutenCases[i], testPlainDakutenHandakutenCases[i + 1], 
           Collator::LESS);
}

/* 
* Test Small, Large letters
*/
void CollationKanaTest::TestSmallLarge(void)
{
  int32_t i;
  UErrorCode status = U_ZERO_ERROR;
  myCollation->setStrength(Collator::TERTIARY);
  myCollation->setAttribute(UCOL_CASE_LEVEL, UCOL_ON, status);
  for (i = 0; i < 3 ; i++)
    doTest(testSmallLargeCases[i], testSmallLargeCases[i + 1], Collator::LESS);
}

/*
* Test Katakana, Hiragana letters
*/
void CollationKanaTest::TestKatakanaHiragana(void)
{
  int32_t i;
  UErrorCode status = U_ZERO_ERROR;
  myCollation->setStrength(Collator::QUATERNARY);
  myCollation->setAttribute(UCOL_CASE_LEVEL, UCOL_ON, status);
  for (i = 0; i < 3 ; i++) {
    doTest(testKatakanaHiraganaCases[i], testKatakanaHiraganaCases[i + 1], 
      Collator::LESS);
  }
}

/*
* Test Choo-on kigoo
*/
void CollationKanaTest::TestChooonKigoo(void)
{
  int32_t i;
  UErrorCode status = U_ZERO_ERROR;
  myCollation->setAttribute(UCOL_CASE_LEVEL, UCOL_ON, status);
  for (i = 0; i < 7 ; i++) {
    doTest(testChooonKigooCases[i], testChooonKigooCases[i + 1], Collator::LESS);
  }
}


void CollationKanaTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite CollationKanaTest: ");
    switch (index) {
        case 0: name = "TestTertiary";  if (exec)   TestTertiary(/* par */); break;
        case 1: name = "TestBase";  if (exec)   TestBase(/* par */); break;
        case 2: name = "TestSmallLarge";  if (exec)   TestSmallLarge(/* par */); break;
        case 3: name = "TestTestPlainDakutenHandakuten";  if (exec)   TestPlainDakutenHandakuten(/* par */); break;
        case 4: name = "TestKatakanaHiragana";  if (exec)   TestKatakanaHiragana(/* par */); break;
        case 5: name = "TestChooonKigoo";  if (exec)   TestChooonKigoo(/* par */); break;
        default: name = ""; break;
    }
}


