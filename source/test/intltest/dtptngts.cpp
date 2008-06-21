/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2008, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <stdio.h>
#include <stdlib.h>
#include "dtptngts.h" 

#include "unicode/calendar.h"
#include "unicode/smpdtfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/dtptngen.h"
#include "loctest.h"


// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void IntlTestDateTimePatternGeneratorAPI::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite DateTimePatternGeneratorAPI");
    switch (index) {
        TESTCASE(0, testAPI);
        default: name = ""; break;
    }
}

#define MAX_LOCALE   4  

/**
 * Test various generic API methods of DateTimePatternGenerator for API coverage.
 */
void IntlTestDateTimePatternGeneratorAPI::testAPI(/*char *par*/)
{
    UnicodeString patternData[] = {
        UnicodeString("yM"),
        UnicodeString("yMMM"),
        UnicodeString("yMd"),
        UnicodeString("yMMMd"),
        UnicodeString("Md"),
        UnicodeString("MMMd"),
        UnicodeString("yQQQ"),
        UnicodeString("hhmm"),
        UnicodeString("HHmm"),
        UnicodeString("mmss"),
        UnicodeString(),
     };
     
    const char* testLocale[MAX_LOCALE][3] = {
        {"en", "US", ""},
        {"zh", "Hans", "CN"},
        {"de", "DE", ""},
        {"fi", "", ""},
     };
     
    UnicodeString patternResults[] = {
        UnicodeString("1/1999"),  // en_US
        UnicodeString("Jan 1999"),
        UnicodeString("1/13/1999"),
        UnicodeString("Jan 13, 1999"),
        UnicodeString("1/13"),
        UnicodeString("Jan 13"),
        UnicodeString("Q1 1999"),
        UnicodeString("11:58 PM"),
        UnicodeString("23:58"),
        UnicodeString("58:59"),
        UnicodeString("1999-1"),  // zh_Hans_CN
        UnicodeString("1999-01"),
        CharsToUnicodeString("1999\\u5E741\\u670813\\u65E5"),
        CharsToUnicodeString("1999\\u5E7401\\u670813\\u65E5"),
        UnicodeString("1-13"),
        UnicodeString("01-13"),
        CharsToUnicodeString("1999\\u5E741\\u5B63"),
        CharsToUnicodeString("\\u4E0B\\u534811:58"),
        CharsToUnicodeString("23:58"),
        UnicodeString("58:59"),
        UnicodeString("1999-1"),  // de_DE
        UnicodeString("Jan 1999"),
        UnicodeString("13.1.1999"),
        UnicodeString("13. Jan 1999"),
        UnicodeString("13.1."),
        UnicodeString("13. Jan"),
        UnicodeString("Q1 1999"),
        UnicodeString("23:58"),
        UnicodeString("23:58"),
        UnicodeString("58:59"),
        UnicodeString("1/1999"),  // fi
        UnicodeString("tammi 1999"),
        UnicodeString("13.1.1999"),
        UnicodeString("13. tammi 1999"),
        UnicodeString("13.1."),
        UnicodeString("13. tammi"),
        UnicodeString("1. nelj. 1999"),
        UnicodeString("23.58"),
        UnicodeString("23.58"),
        UnicodeString("58.59"),
        UnicodeString(),
    };

    UnicodeString patternTests2[] = {
        UnicodeString("yyyyMMMdd"),
        UnicodeString("EyyyyMMMdd"),
        UnicodeString("yyyyMMdd"),
        UnicodeString("yyyyMMM"),
        UnicodeString("yyyyMM"),
        UnicodeString("yyMM"),
        UnicodeString("yMMMMMd"),
        UnicodeString("EEEEEMMMMMd"),
        UnicodeString("MMMd"),
        UnicodeString("MMMdhmm"),
        UnicodeString("EMMMdhmms"),
        UnicodeString("MMdhmm"),
        UnicodeString("EEEEMMMdhmms"),
        UnicodeString("yyyyMMMddhhmmss"),
        UnicodeString("EyyyyMMMddhhmmss"),
        UnicodeString("hmm"),
        UnicodeString("hhmm"),
        UnicodeString(""),
    };
    UnicodeString patternResults2[] = {
        UnicodeString("Oct 14, 1999"),
        UnicodeString("Thu, Oct 14, 1999"),
        UnicodeString("10/14/1999"),
        UnicodeString("Oct 1999"),
        UnicodeString("10/1999"),
        UnicodeString("10/99"),
        UnicodeString("O/14/1999"),
        UnicodeString("T, O 14"),
        UnicodeString("Oct 14"),
        UnicodeString("Oct 14 6:58 AM"),
        UnicodeString("Thu, Oct 14 6:58:59 AM"),
        UnicodeString("10/14 6:58 AM"),
        UnicodeString("Thursday, Oct 14 6:58:59 AM"),
        UnicodeString("Oct 14, 1999 06:58:59 AM"),
        UnicodeString("Thu, Oct 14, 1999 06:58:59 AM"),
        UnicodeString("6:58 AM"),
        UnicodeString("06:58 AM"),
        UnicodeString(""),
    };
    
    // results for getSkeletons() and getPatternForSkeleton()
    const UnicodeString testSkeletonsResults[] = { 
        UnicodeString("HH:mm"), 
        UnicodeString("MMMMd"), 
        UnicodeString("MMMMMd"), 
    };
          
    const UnicodeString testBaseSkeletonsResults[] = {        
        UnicodeString("Hm"),  
        UnicodeString("MMMd"), 
        UnicodeString("MMMd"),
    };

    UnicodeString newDecimal(" "); // space
    UnicodeString newAppendItemName("hrs.");
    UnicodeString newAppendItemFormat("{1} {0}");
    UnicodeString newDateTimeFormat("{1} {0}");
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString conflictingPattern;
    UDateTimePatternConflict conflictingStatus;

    // ======= Test CreateInstance with default locale
    logln("Testing DateTimePatternGenerator createInstance from default locale");
    
    DateTimePatternGenerator *instFromDefaultLocale=DateTimePatternGenerator::createInstance(status);
    if (U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateTimePatternGenerator (default) - exitting");
        return;
    }
    else {
        delete instFromDefaultLocale;
    }

    // ======= Test CreateInstance with given locale    
    logln("Testing DateTimePatternGenerator createInstance from French locale");
    status = U_ZERO_ERROR;
    DateTimePatternGenerator *instFromLocale=DateTimePatternGenerator::createInstance(Locale::getFrench(), status);
    if (U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateTimePatternGenerator (Locale::getFrench()) - exitting");
        return;
    }

    // ======= Test clone DateTimePatternGenerator    
    logln("Testing DateTimePatternGenerator::clone()");
    status = U_ZERO_ERROR;
    

    UnicodeString decimalSymbol = instFromLocale->getDecimal();
    UnicodeString newDecimalSymbol = UnicodeString("*");
    decimalSymbol = instFromLocale->getDecimal();
    instFromLocale->setDecimal(newDecimalSymbol);
    DateTimePatternGenerator *cloneDTPatternGen=instFromLocale->clone();
    decimalSymbol = cloneDTPatternGen->getDecimal();
    if (decimalSymbol != newDecimalSymbol) {
        errln("ERROR: inconsistency is found in cloned object.");
    }
    if ( !(*cloneDTPatternGen == *instFromLocale) ) {
        errln("ERROR: inconsistency is found in cloned object.");
    }
    
    if ( *cloneDTPatternGen != *instFromLocale ) {
        errln("ERROR: inconsistency is found in cloned object.");
    }
    
    delete instFromLocale;
    delete cloneDTPatternGen;
    
    // ======= Test simple use cases    
    logln("Testing simple use cases");
    status = U_ZERO_ERROR;
    Locale deLocale=Locale::getGermany();
    UDate sampleDate=LocaleTest::date(99, 9, 13, 23, 58, 59);
    DateTimePatternGenerator *gen = DateTimePatternGenerator::createInstance(deLocale, status);
    if (U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateTimePatternGenerator (Locale::getGermany()) - exitting");
        return;
    }
    UnicodeString findPattern = gen->getBestPattern(UnicodeString("MMMddHmm"), status);
    SimpleDateFormat *format = new SimpleDateFormat(findPattern, deLocale, status);
    if (U_FAILURE(status)) {
        dataerrln("ERROR: Could not create SimpleDateFormat (Locale::getGermany())");
        delete gen;
        return;
    }
    TimeZone *zone = TimeZone::createTimeZone(UnicodeString("ECT"));
    if (zone==NULL) {
        dataerrln("ERROR: Could not create TimeZone ECT");
        delete gen;
        delete format;
        return;
    }
    format->setTimeZone(*zone);
    UnicodeString dateReturned, expectedResult;
    dateReturned.remove();
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("14. Okt 8:58");
    if ( dateReturned != expectedResult ) {
        errln("ERROR: Simple test in getBestPattern with Locale::getGermany()).");
    }
    // add new pattern
    status = U_ZERO_ERROR;
    conflictingStatus = gen->addPattern(UnicodeString("d'. von' MMMM"), true, conflictingPattern, status); 
    if (U_FAILURE(status)) {
        errln("ERROR: Could not addPattern - d\'. von\' MMMM");
    }
    status = U_ZERO_ERROR;
    UnicodeString testPattern=gen->getBestPattern(UnicodeString("MMMMdd"), status);
    testPattern=gen->getBestPattern(UnicodeString("MMMddHmm"), status);
    format->applyPattern(gen->getBestPattern(UnicodeString("MMMMddHmm"), status));
    dateReturned.remove();
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("14. von Oktober 8:58");
    if ( dateReturned != expectedResult ) {
        errln("ERROR: Simple test addPattern failed!: d\'. von\' MMMM  ");
    }
    delete format;
    
    // get a pattern and modify it
    format = (SimpleDateFormat *)DateFormat::createDateTimeInstance(DateFormat::kFull, DateFormat::kFull, 
                                                                  deLocale);
    format->setTimeZone(*zone);
    UnicodeString pattern;
    pattern = format->toPattern(pattern);
    dateReturned.remove();
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("Donnerstag, 14. Oktober 1999 08:58:59 Frankreich");
    if ( dateReturned != expectedResult ) {
        errln("ERROR: Simple test uses full date format.");
        errln(UnicodeString(" Got: ") + dateReturned + UnicodeString(" Expected: ") + expectedResult);
    }
     
    // modify it to change the zone.  
    UnicodeString newPattern = gen->replaceFieldTypes(pattern, UnicodeString("vvvv"), status);
    format->applyPattern(newPattern);
    dateReturned.remove();
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("Donnerstag, 14. Oktober 1999 08:58:59 Frankreich");
    if ( dateReturned != expectedResult ) {
        errln("ERROR: Simple test modify the timezone!");
        errln(UnicodeString(" Got: ")+ dateReturned + UnicodeString(" Expected: ") + expectedResult);
    }
    
    // setDeciaml(), getDeciaml()
    gen->setDecimal(newDecimal);
    if (newDecimal != gen->getDecimal()) {
        errln("ERROR: unexpected result from setDecimal() and getDecimal()!.\n");
    }
    
    // setAppenItemName() , getAppendItemName()
    gen->setAppendItemName(UDATPG_HOUR_FIELD, newAppendItemName);
    if (newAppendItemName != gen->getAppendItemName(UDATPG_HOUR_FIELD)) {
        errln("ERROR: unexpected result from setAppendItemName() and getAppendItemName()!.\n");
    }
    
    // setAppenItemFormat() , getAppendItemFormat()
    gen->setAppendItemFormat(UDATPG_HOUR_FIELD, newAppendItemFormat);
    if (newAppendItemFormat != gen->getAppendItemFormat(UDATPG_HOUR_FIELD)) {
        errln("ERROR: unexpected result from setAppendItemFormat() and getAppendItemFormat()!.\n");
    }
    
    // setDateTimeFormat() , getDateTimeFormat()
    gen->setDateTimeFormat(newDateTimeFormat);
    if (newDateTimeFormat != gen->getDateTimeFormat()) {
        errln("ERROR: unexpected result from setDateTimeFormat() and getDateTimeFormat()!.\n");
    }
    
    // ======== Test getSkeleton and getBaseSkeleton
    status = U_ZERO_ERROR;
    pattern = UnicodeString("dd-MMM");
    UnicodeString expectedSkeleton = UnicodeString("MMMdd");
    UnicodeString expectedBaseSkeleton = UnicodeString("MMMd");
    UnicodeString retSkeleton = gen->getSkeleton(pattern, status);
    if(U_FAILURE(status) || retSkeleton != expectedSkeleton ) {
         errln("ERROR: Unexpected result from getSkeleton().\n");
         errln(UnicodeString(" Got: ") + retSkeleton + UnicodeString(" Expected: ") + expectedSkeleton );
    }
    retSkeleton = gen->getBaseSkeleton(pattern, status);
    if(U_FAILURE(status) || retSkeleton !=  expectedBaseSkeleton) {
         errln("ERROR: Unexpected result from getBaseSkeleton().\n");
         errln(UnicodeString(" Got: ") + retSkeleton + UnicodeString(" Expected:")+ expectedBaseSkeleton);
    }

    pattern = UnicodeString("dd/MMMM/yy");
    expectedSkeleton = UnicodeString("yyMMMMdd");
    expectedBaseSkeleton = UnicodeString("yMMMd");
    retSkeleton = gen->getSkeleton(pattern, status);
    if(U_FAILURE(status) || retSkeleton != expectedSkeleton ) {
         errln("ERROR: Unexpected result from getSkeleton().\n");
         errln(UnicodeString(" Got: ") + retSkeleton + UnicodeString(" Expected: ") + expectedSkeleton );
    }
    retSkeleton = gen->getBaseSkeleton(pattern, status);
    if(U_FAILURE(status) || retSkeleton !=  expectedBaseSkeleton) {
         errln("ERROR: Unexpected result from getBaseSkeleton().\n");
         errln(UnicodeString(" Got: ") + retSkeleton + UnicodeString(" Expected:")+ expectedBaseSkeleton);
    }
    delete format;
    delete zone;
    delete gen;
    
    {
        // Trac# 6104
        status = U_ZERO_ERROR;
        pattern = UnicodeString("YYYYMMM");
        UnicodeString expR = CharsToUnicodeString("1999/01");
        Locale loc("ja");
        UDate testDate1= LocaleTest::date(99, 0, 13, 23, 58, 59);
        DateTimePatternGenerator *patGen=DateTimePatternGenerator::createInstance(loc, status);
        if(U_FAILURE(status)) {
            dataerrln("ERROR: Could not create DateTimePatternGenerator");
            return;
        }
        UnicodeString bPattern = patGen->getBestPattern(pattern, status);
        UnicodeString rDate;
        SimpleDateFormat sdf(bPattern, loc, status);
        rDate.remove();
        rDate = sdf.format(testDate1, rDate);

        logln(UnicodeString(" ja locale with skeleton: YYYYMMM  Best Pattern:") + bPattern);
        logln(UnicodeString("  Formatted date:") + rDate);

        if ( expR!= rDate ) {
            errln(UnicodeString("\nERROR: Test Japanese month hack Got: ") + rDate + 
                  UnicodeString(" Expected: ") + expR );
        }
        
        delete patGen;
    }
    {   // Trac# 6104
        Locale loc("zh");
        UnicodeString expR = UnicodeString("1999-01");
        UDate testDate1= LocaleTest::date(99, 0, 13, 23, 58, 59);
        DateTimePatternGenerator *patGen=DateTimePatternGenerator::createInstance(loc, status);
        if(U_FAILURE(status)) {
            dataerrln("ERROR: Could not create DateTimePatternGenerator");
            return;
        }
        UnicodeString bPattern = patGen->getBestPattern(pattern, status);
        UnicodeString rDate;
        SimpleDateFormat sdf(bPattern, loc, status);
        rDate.remove();
        rDate = sdf.format(testDate1, rDate);

        logln(UnicodeString(" zh locale with skeleton: YYYYMMM  Best Pattern:") + bPattern);
        logln(UnicodeString("  Formatted date:") + rDate);
        if ( expR!= rDate ) {
            errln(UnicodeString("\nERROR: Test Chinese month hack Got: ") + rDate + 
                  UnicodeString(" Expected: ") + expR );
        }
        delete patGen;   
    }

    {
         // Trac# 6172 duplicate time pattern
         status = U_ZERO_ERROR;
         pattern = UnicodeString("hmv");
         UnicodeString expR = UnicodeString("h:m a v");
         Locale loc("en");
         DateTimePatternGenerator *patGen=DateTimePatternGenerator::createInstance(loc, status);
         if(U_FAILURE(status)) {
             dataerrln("ERROR: Could not create DateTimePatternGenerator");
             return;
         }
         UnicodeString bPattern = patGen->getBestPattern(pattern, status);
         logln(UnicodeString(" en locale with skeleton: hmv  Best Pattern:") + bPattern);

         if ( expR!= bPattern ) {
             errln(UnicodeString("\nERROR: Test EN time format Got: ") + bPattern + 
                   UnicodeString(" Expected: ") + expR );
         }
         
         delete patGen;
     }
     
    
    // ======= Test various skeletons.
    logln("Testing DateTimePatternGenerator with various skeleton");
   
    status = U_ZERO_ERROR;
    int32_t localeIndex=0;
    int32_t resultIndex=0;
    UnicodeString resultDate;
    UDate testDate= LocaleTest::date(99, 0, 13, 23, 58, 59);
    while (localeIndex < MAX_LOCALE )
    {       
        int32_t dataIndex=0;
        UnicodeString bestPattern;
        
        Locale loc(testLocale[localeIndex][0], testLocale[localeIndex][1], testLocale[localeIndex][2], "");
        logln("\n\n Locale: %s_%s_%s", testLocale[localeIndex][0], testLocale[localeIndex][1], testLocale[localeIndex][2]);
        DateTimePatternGenerator *patGen=DateTimePatternGenerator::createInstance(loc, status);
        if(U_FAILURE(status)) {
            dataerrln("ERROR: Could not create DateTimePatternGenerator with locale index:%d . - exitting\n", localeIndex);
            return;
        }
        while (patternData[dataIndex].length() > 0) {
            log(patternData[dataIndex]);
            bestPattern = patGen->getBestPattern(patternData[dataIndex++], status);
            logln(UnicodeString(" -> ") + bestPattern);
            
            SimpleDateFormat sdf(bestPattern, loc, status);
            resultDate.remove();
            resultDate = sdf.format(testDate, resultDate);
            if ( resultDate != patternResults[resultIndex] ) {
                errln(UnicodeString("\nERROR: Test various skeletons[") + (dataIndex-1)
                    + UnicodeString("]. Got: ") + resultDate + UnicodeString(" Expected: ") + patternResults[resultIndex] );
            }
            
            resultIndex++;
        }
        delete patGen;
        localeIndex++;
    }
    
    // ======= More tests ticket#6110
    logln("Testing DateTimePatternGenerator with various skeleton");
   
    status = U_ZERO_ERROR;
    localeIndex=0;
    resultIndex=0;
    testDate= LocaleTest::date(99, 9, 13, 23, 58, 59);
    {       
        int32_t dataIndex=0;
        UnicodeString bestPattern;
        logln("\n\n Test various skeletons for English locale...");
        DateTimePatternGenerator *patGen=DateTimePatternGenerator::createInstance(Locale::getEnglish(), status);
        if(U_FAILURE(status)) {
            dataerrln("ERROR: Could not create DateTimePatternGenerator with locale English . - exitting\n");
            return;
        }
        TimeZone *enZone = TimeZone::createTimeZone(UnicodeString("ECT/GMT"));
        if (enZone==NULL) {
            dataerrln("ERROR: Could not create TimeZone ECT");
            delete patGen;
            return;
        }
        SimpleDateFormat *enFormat = (SimpleDateFormat *)DateFormat::createDateTimeInstance(DateFormat::kFull, 
                         DateFormat::kFull, Locale::getEnglish());
        enFormat->setTimeZone(*enZone);
        while (patternTests2[dataIndex].length() > 0) {
            logln(patternTests2[dataIndex]);
            bestPattern = patGen->getBestPattern(patternTests2[dataIndex], status);
            logln(UnicodeString(" -> ") + bestPattern);
            enFormat->applyPattern(bestPattern);
            resultDate.remove();
            resultDate = enFormat->format(testDate, resultDate);
            if ( resultDate != patternResults2[resultIndex] ) {
                errln(UnicodeString("\nERROR: Test various skeletons[") + dataIndex
                    + UnicodeString("]. Got: ") + resultDate + UnicodeString(" Expected: ") + 
                    patternResults2[resultIndex] );
            }
            dataIndex++;
            resultIndex++;
        }
        delete patGen;
        delete enZone;
        delete enFormat;
    }



    // ======= Test random skeleton 
    DateTimePatternGenerator *randDTGen= DateTimePatternGenerator::createInstance(status);
    if (U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateTimePatternGenerator (Locale::getFrench()) - exitting");
        return;
    }
    UChar newChar;
    int32_t i;
    for (i=0; i<10; ++i) {
        UnicodeString randomSkeleton;
        int32_t len = rand() % 20;
        for (int32_t j=0; j<len; ++j ) {
            while ((newChar = (UChar)(rand()%0x7f))>=(UChar)0x20) {
                randomSkeleton += newChar;
            }
        }
        UnicodeString bestPattern = randDTGen->getBestPattern(randomSkeleton, status);
    }
    delete randDTGen;
    
    // UnicodeString randomString=Unicode
    // ======= Test getStaticClassID()

    logln("Testing getStaticClassID()");
    status = U_ZERO_ERROR;
    DateTimePatternGenerator *test= DateTimePatternGenerator::createInstance(status);
    
    if(test->getDynamicClassID() != DateTimePatternGenerator::getStaticClassID()) {
        errln("ERROR: getDynamicClassID() didn't return the expected value");
    }
    delete test;
    
    // ====== Test createEmptyInstance()
    
    logln("Testing createEmptyInstance()");
    status = U_ZERO_ERROR;
    
    test = DateTimePatternGenerator::createEmptyInstance(status);
    if(U_FAILURE(status)) {
         errln("ERROR: Fail to create an empty instance ! - exitting.\n");
         delete test;
         return;
    }
    
    conflictingStatus = test->addPattern(UnicodeString("MMMMd"), true, conflictingPattern, status); 
    status = U_ZERO_ERROR;
    testPattern=test->getBestPattern(UnicodeString("MMMMdd"), status);
    conflictingStatus = test->addPattern(UnicodeString("HH:mm"), true, conflictingPattern, status); 
    conflictingStatus = test->addPattern(UnicodeString("MMMMMd"), true, conflictingPattern, status); //duplicate pattern
    StringEnumeration *output=NULL;
    output = test->getRedundants(status);
    expectedResult=UnicodeString("MMMMd");
    if (output != NULL) {
        output->reset(status);
        const UnicodeString *dupPattern=output->snext(status);
        if ( (dupPattern==NULL) || (*dupPattern != expectedResult) ) {
            errln("ERROR: Fail in getRedundants !\n");
        }
    }
    
    // ======== Test getSkeletons and getBaseSkeletons
    StringEnumeration* ptrSkeletonEnum = test->getSkeletons(status);
    if(U_FAILURE(status)) {
        errln("ERROR: Fail to get skeletons !\n");
    }
    UnicodeString returnPattern, *ptrSkeleton;
    ptrSkeletonEnum->reset(status);
    int32_t count=ptrSkeletonEnum->count(status);
    for (i=0; i<count; ++i) {
        ptrSkeleton = (UnicodeString *)ptrSkeletonEnum->snext(status);
        returnPattern = test->getPatternForSkeleton(*ptrSkeleton);
        if ( returnPattern != testSkeletonsResults[i] ) {
            errln(UnicodeString("ERROR: Unexpected result from getSkeletons and getPatternForSkeleton\nGot: ") + returnPattern
               + UnicodeString("\nExpected: ") + testSkeletonsResults[i]
               + UnicodeString("\n"));
        }
    }
    StringEnumeration* ptrBaseSkeletonEnum = test->getBaseSkeletons(status);
    if(U_FAILURE(status)) {
        errln("ERROR: Fail to get base skeletons !\n");
    }   
    count=ptrBaseSkeletonEnum->count(status);
    for (i=0; i<count; ++i) {
        ptrSkeleton = (UnicodeString *)ptrBaseSkeletonEnum->snext(status);
        if ( *ptrSkeleton != testBaseSkeletonsResults[i] ) {
            errln("ERROR: Unexpected result from getBaseSkeletons() !\n");
        }
    }

    // ========= DateTimePatternGenerator sample code in Userguide
    // set up the generator
    Locale locale = Locale::getFrench();
    status = U_ZERO_ERROR;
    DateTimePatternGenerator *generator = DateTimePatternGenerator::createInstance( locale, status);
        
    // get a pattern for an abbreviated month and day
    pattern = generator->getBestPattern(UnicodeString("MMMd"), status); 
    SimpleDateFormat formatter(pattern, locale, status); 

    zone = TimeZone::createTimeZone(UnicodeString("GMT"));
    formatter.setTimeZone(*zone);
    // use it to format (or parse)
    UnicodeString formatted;
    formatted = formatter.format(Calendar::getNow(), formatted, status); 
    // for French, the result is "13 sept."
    formatted.remove();
    // cannot use the result from getNow() because the value change evreyday.
    testDate= LocaleTest::date(99, 0, 13, 23, 58, 59);
    formatted = formatter.format(testDate, formatted, status);
    expectedResult=UnicodeString("14 janv.");
    if ( formatted != expectedResult ) {
        errln("ERROR: Userguide sample code result!");
        errln(UnicodeString(" Got: ")+ formatted + UnicodeString(" Expected: ") + expectedResult);
    }

    delete zone;
    delete output;
    delete ptrSkeletonEnum;
    delete ptrBaseSkeletonEnum;
    delete test;
    delete generator;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
