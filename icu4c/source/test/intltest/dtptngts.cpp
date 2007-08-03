/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2007, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#if !UCONFIG_NO_FORMATTING

#include <stdio.h>
#include <stdlib.h>
#include "dtptngts.h" 

#include "unicode/calendar.h"
#include "unicode/smpdtfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/dtptngen.h"
#include "unicode/utypes.h"
#include "loctest.h"

static const UnicodeString patternData[] = {
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
    UnicodeString(""),
 };
 
#define MAX_LOCALE   4  
static const char* testLocale[MAX_LOCALE][3] = {
    {"en", "US","\0"},
    {"zh", "Hans", "CN"},
    {"de","DE", "\0"},
    {"fi","\0", "\0"},
 };
 


static const UnicodeString patternResults[] = {
    UnicodeString("1/1999"),  // en_US
    UnicodeString("Jan 1999"),
    UnicodeString("1/13/1999"),
    UnicodeString("Jan/13/1999"),
    UnicodeString("1/13"),
    UnicodeString("Jan 13"),
    UnicodeString("Q1 1999"),
    UnicodeString("11:58 PM"),
    UnicodeString("23:58"),
    UnicodeString("58:59"),
    UnicodeString("1999-1"),  // zh_Hans_CN
    UnicodeString("1999 1"),
    UnicodeString("1999113"),
    UnicodeString("1999113"),
    // TODO: These are diff from CLDR 1.4 to CLDR 1.5. will verify the result soon.
    //CharsToUnicodeString("1999\\u5E741\\u670813\\u65E5"),
    //CharsToUnicodeString("1999\\u5E741\\u670813\\u65E5"),
    UnicodeString("1-13"),
    UnicodeString("1 13"),
    CharsToUnicodeString("1999 Q1"),
    CharsToUnicodeString("\\u4E0B\\u534811:58"),
    CharsToUnicodeString("23:58"),
    UnicodeString("58:59"),
    UnicodeString("1.1999"),  // de_DE
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
    UnicodeString("13. tammita 1999"),
    UnicodeString("13.1."),
    UnicodeString("13. tammita"),
    UnicodeString("1. nelj./1999"),
    UnicodeString("23.58"),
    UnicodeString("23.58"),
    UnicodeString("58.59"),
    UnicodeString(""),
    
};



// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void IntlTestDateTimePatternGeneratorAPI::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite DateTimePatternGeneratorAPI");
    switch (index) {
        case 0: name = "DateTimePatternGenerator API test"; 
                if (exec) {
                    logln("DateTimePatternGenerator API test---"); logln("");
                    UErrorCode status = U_ZERO_ERROR;
                    Locale saveLocale;
                    Locale::setDefault(Locale::getEnglish(), status);
                    if(U_FAILURE(status)) {
                        errln("ERROR: Could not set default locale, test may not give correct results");
                    }
                    testAPI(/*par*/);
                    Locale::setDefault(saveLocale, status);
                }
                break;

        default: name = ""; break;
    }
}

/**
 * Test various generic API methods of DateTimePatternGenerator for API coverage.
 */
void IntlTestDateTimePatternGeneratorAPI::testAPI(/*char *par*/)
{
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
        dataerrln("ERROR: inconsistency is found in cloned object- exitting");
        return;
    }
    if (U_FAILURE(status)) {
        delete instFromLocale;
        dataerrln("ERROR: Could not create DateTimePatternGenerator (Locale::getFrench()) - exitting");
        return;
    }
    else {
           delete instFromLocale;
           delete cloneDTPatternGen;
     }
   
    // ======= Test simple use cases    
    logln("Testing simple use cases");
    status = U_ZERO_ERROR;
    Locale deLocale=Locale::getGermany();
    UDate sampleDate=LocaleTest::date(99, 9, 13, 23, 58, 59);
    DateTimePatternGenerator *gen = DateTimePatternGenerator::createInstance(deLocale, status);
    UnicodeString findPattern = gen->getBestPattern(UnicodeString("MMMddHmm"), status);
    SimpleDateFormat *format = new SimpleDateFormat(findPattern, deLocale, status);
    //TimeZone *zone = TimeZone::createTimeZone(UnicodeString("Europe/Paris"));
    TimeZone *zone = TimeZone::createTimeZone(UnicodeString("ECT"));
    format->setTimeZone(*zone);
    UnicodeString dateReturned, expectedResult;
    dateReturned="";
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("8:58 14. Okt");
    if ( dateReturned != expectedResult ) {
        if ( format != NULL )  delete format;
        if ( zone != NULL )  delete zone;
        if ( gen != NULL )  delete gen;
        dataerrln("ERROR: Simple test in  Locale::getGermany()) - exitting");
        return;
    }
    // add new pattern
    conflictingStatus = gen->addPattern(UnicodeString("d'. von' MMMM"), true, conflictingPattern, status); 
    status = U_ZERO_ERROR;
    UnicodeString testPattern=gen->getBestPattern(UnicodeString("MMMMdd"), status);
    testPattern=gen->getBestPattern(UnicodeString("MMMddHmm"), status);
    format->applyPattern(gen->getBestPattern(UnicodeString("MMMMddHmm"), status));
    dateReturned="";
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("8:58 14. von Oktober");
    if ( dateReturned != expectedResult ) {
        if ( format != NULL )  delete format;
        if ( zone != NULL )  delete zone;
        if ( gen != NULL )  delete gen;
        dataerrln("ERROR: Simple test add pattern d\'. von\' MMMM   - exitting");
        return;
    }
    if ( format != NULL )  delete format;
    
    // get a pattern and modify it
    format = (SimpleDateFormat *)DateFormat::createDateTimeInstance(DateFormat::kFull, DateFormat::kFull, 
                                                                  deLocale);
    format->setTimeZone(*zone);
    UnicodeString pattern;
    pattern = format->toPattern(pattern);
    dateReturned="";
    dateReturned = format->format(sampleDate, dateReturned, status);
    //expectedResult=UnicodeString("Donnerstag, 14. Oktober 1999 08:58:59 Frankreich");
    //The mismatch is caused by the setup of Timezone. The output pattern is same as in Java.
    expectedResult=UnicodeString("Donnerstag, 14. Oktober 1999 08:58:59 GMT+02:00");
    if ( dateReturned != expectedResult ) {
        if ( format != NULL )  delete format;
        if ( zone != NULL )  delete zone;
        if ( gen != NULL )  delete gen;
        dataerrln("ERROR: Simple test uses full date format.- exitting");
        return;
    }
     
    // modify it to change the zone.  
    UnicodeString newPattern = gen->replaceFieldTypes(pattern, UnicodeString("vvvv"), status);
    format->applyPattern(newPattern);
    dateReturned="";
    dateReturned = format->format(sampleDate, dateReturned, status);
    expectedResult=UnicodeString("Donnerstag, 14. Oktober 1999 08:58:59 GMT+02:00");
    // expectedResult=UnicodeString("Donnerstag, 14. Oktober 1999 08:58:59 Frankreich:);
    // The mismatch is caused by the setup of Timezone. The output pattern is same as in Java.
    if ( dateReturned != expectedResult ) {
        if ( format != NULL )  delete format;
        if ( zone != NULL )  delete zone;
        if ( gen != NULL )  delete gen;
        dataerrln("ERROR: Simple test modify the timezone - exitting");
        return;
    }
    /*
    printf("\n replace pattern:");
    for (int32_t i=0; i<pattern.length(); ++i) {
        printf("%c", pattern.charAt(i));
    }  
    printf(" with pattern:");
    for (int32_t i=0; i<newPattern.length(); ++i) {
        printf("%c", newPattern.charAt(i));
    }
    printf(" returnedDate:");
    for (int32_t i=0; i<dateReturned.length(); ++i) {
         printf("%c", dateReturned.charAt(i));
    }
    */
    if ( format != NULL ) delete format;
    if ( zone != NULL )  delete zone;
    
    // ======== Test getSkeletons and getBaseSkeletons
    UnicodeString patterns[40];
    UnicodeString skeletons[40];
    UnicodeString baseSkeletons[40];
    int32_t cntSkeletons=0;
    int32_t cntBaseSktns=0;
    
    StringEnumeration* ptrSkeletonEnum = gen->getSkeletons(status);
    if(U_FAILURE(status)) {
         errln("ERROR: Fail to get skeletons !\n");
    }
    UnicodeString returnPattern, *ptrSkeleton;
    ptrSkeletonEnum->reset(status);
    int32_t count=ptrSkeletonEnum->count(status);
    for (int32_t i=0; i<count; ++i) {
        ptrSkeleton = (UnicodeString *)ptrSkeletonEnum->snext(status);
        returnPattern = gen->getPatternForSkeleton(*ptrSkeleton);
    }
    StringEnumeration* ptrBaseSkeletonEnum = gen->getBaseSkeletons(status);
    if(U_FAILURE(status)) {
         errln("ERROR: Fail to get base skeletons !\n");
     }   
    count=ptrBaseSkeletonEnum->count(status);
    for (int32_t i=0; i<count; ++i) {
        ptrSkeleton = (UnicodeString *)ptrBaseSkeletonEnum->snext(status);
    }
    
    if ( gen != NULL )  delete gen;
    
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
        //printf("\n\n Locale: %s_%s_%s", testLocale[localeIndex][0], testLocale[localeIndex][1], testLocale[localeIndex][2]);
        //printf("\n    Status:%d", status);
        DateTimePatternGenerator *patGen=DateTimePatternGenerator::createInstance(loc, status);
        if(U_FAILURE(status)) {
            errln("ERROR: Could not create DateTimePatternGenerator with locale index:%d .\n", localeIndex);
        }
        while (patternData[dataIndex].length() > 0) {
            bestPattern = patGen->getBestPattern(patternData[dataIndex++], status);
            
            SimpleDateFormat* sdf = new SimpleDateFormat(bestPattern, loc, status);
            resultDate = "";
            resultDate = sdf->format(testDate, resultDate);
            if ( resultDate != patternResults[resultIndex] ) {
                errln("\nERROR: Test various skeletons[%d] .", dataIndex-1);
                // TODO Remove printf once ICU pick up CLDR 1.5
                /*
                printf("\nUnmatched result!\n TestPattern:");
                for (int32_t i=0; i < patternData[dataIndex-1].length(); ++i) {
                     printf("%c", patternData[dataIndex-1].charAt(i));
                }   
                printf("  BestPattern:");
                for (int32_t i=0; i < bestPattern.length(); ++i) {
                   printf("%c", bestPattern.charAt(i));
                } 

                printf("  expected result:");
                for (int32_t i=0; i < patternResults[resultIndex].length(); ++i) {
                   printf("%c", patternResults[resultIndex].charAt(i));
                }
                printf("\n  expected result in hex:");
                for (int32_t i=0; i < patternResults[resultIndex].length(); ++i) {
                   printf("0x%x ", patternResults[resultIndex].charAt(i));
                }
                printf("\n  running result:");
                for (int32_t i=0; i < resultDate.length(); ++i) {
                     printf("%c", resultDate.charAt(i));
                }
                printf("  running result in hex:");
                for (int32_t i=0; i < resultDate.length(); ++i) {
                     printf("0x%x ", resultDate.charAt(i));
                }
                */
            }
            
            resultIndex++;
            delete sdf;
        }
        delete patGen;
        localeIndex++;
    }



    // ======= Test random skeleton 
    const char randomChars[80] = {
     '1','2','3','4','5','6','7','8','9','0','!','@','#','$','%','^','&','*','(',')',
     '`',' ','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r',
     's','t','u','v','w','x','y','z','A','B','C','D','F','G','H','I','J','K','L','M',
     'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',':',';','<','.','?',';','\\'};
    DateTimePatternGenerator *randDTGen= DateTimePatternGenerator::createInstance(status);
    if (U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateTimePatternGenerator (Locale::getFrench()) - exitting");
        return;
    }

    for (int32_t i=0; i<10; ++i) {
        UnicodeString randomSkeleton="";
        int32_t len = rand() % 20;
        for (int32_t j=0; j<len; ++j ) {
           randomSkeleton += rand()%80;
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
    if (test!=NULL) {
        delete test;
        test=NULL;
    }
    
    
    // ====== Test createEmptyInstance()
    
    logln("Testing createEmptyInstance()");
    status = U_ZERO_ERROR;
    
    test = DateTimePatternGenerator::createEmptyInstance(status);
    if(U_FAILURE(status)) {
         errln("ERROR: Fail to create an empty instance !\n");
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
    
    if (test!=NULL) {
        delete test;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
