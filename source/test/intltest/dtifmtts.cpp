
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2008, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING


//FIXME: how to define it in compiler time
#define DTIFMTTS_DEBUG 1


#ifdef DTIFMTTS_DEBUG 
#include <iostream>
#endif

#include "cstring.h"
#include "dtifmtts.h"
#include "unicode/dtintrv.h"
#include "unicode/dtitvinf.h"
#include "unicode/dtitvfmt.h"


#ifdef DTIFMTTS_DEBUG 
//#define PRINTMESG(msg) { std::cout << "(" << __FILE__ << ":" << __LINE__ << ") " << msg << "\n"; }
#define PRINTMESG(msg) { std::cout << msg; }
#endif

#define ARRAY_SIZE(array) (sizeof array / sizeof array[0])


// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void DateIntervalFormatTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ ) {
    if (exec) logln("TestSuite DateIntervalFormat");
    switch (index) {
        // TODO: uncomment. comment out temporarily
        //TESTCASE(0, testAPI);
        //TESTCASE(1, testFormat);
        default: name = ""; break;
    }
}

/**
 * Test various generic API methods of DateIntervalFormat for API coverage.
 */
void DateIntervalFormatTest::testAPI() {

    /* ====== Test create interval instance with default locale and skeleton
     */
    UErrorCode status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat create instance with default locale and skeleton");
 
    DateIntervalFormat* dtitvfmt = DateIntervalFormat::createInstance(YEAR_MONTH_DAY_LONG_FORMAT, status);
    if(U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateIntervalFormat (skeleton + default locale) - exitting");
        return;
    } else {
        delete dtitvfmt;
    }


    /* ====== Test create interval instance with given locale and skeleton
     */
    status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat create instance with given locale and skeleton");
 
    dtitvfmt = DateIntervalFormat::createInstance(YEAR_MONTH_DAY_LONG_FORMAT, Locale::getJapanese(), status);
    if(U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateIntervalFormat (skeleton + locale) - exitting");
        return;
    } else {
        delete dtitvfmt;
    }


    /* ====== Test create interval instance with dateIntervalInfo and skeleton
     */
    status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat create instance with dateIntervalInfo  and skeleton");
 
    DateIntervalInfo* dtitvinf = new DateIntervalInfo(Locale::getSimplifiedChinese(), status);

    dtitvfmt = DateIntervalFormat::createInstance("EEEdMMMyhms", dtitvinf, status);
    if(U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateIntervalFormat (skeleton + DateIntervalInfo + default locale) - exitting");
        return;
    } else {
        delete dtitvfmt;
    } 


    /* ====== Test create interval instance with dateIntervalInfo and skeleton
     */
    status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat create instance with dateIntervalInfo  and skeleton");
 
    dtitvinf = new DateIntervalInfo(Locale::getSimplifiedChinese(), status);

    dtitvfmt = DateIntervalFormat::createInstance("EEEdMMMyhms", Locale::getSimplifiedChinese(), dtitvinf, status);
    if(U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateIntervalFormat (skeleton + DateIntervalInfo + locale) - exitting");
        return;
    } 
    // not deleted, test clone 


    // ====== Test clone()
    status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat clone");

    DateIntervalFormat* another = (DateIntervalFormat*)dtitvfmt->clone();
    if ( (*another) != (*dtitvfmt) ) {
        dataerrln("ERROR: clone failed");
    }

 
    // ====== Test getDateIntervalInfo, setDateIntervalInfo, adoptDateIntervalInfo
    status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat getDateIntervalInfo");
    const DateIntervalInfo* inf = another->getDateIntervalInfo();
    dtitvfmt->setDateIntervalInfo(*inf, status);
    const DateIntervalInfo* anotherInf = dtitvfmt->getDateIntervalInfo();
    if ( (*inf) != (*anotherInf) || U_FAILURE(status) ) {
        dataerrln("ERROR: getDateIntervalInfo/setDateIntervalInfo failed");
    }

    status = U_ZERO_ERROR;
    DateIntervalInfo* nonConstInf = inf->clone();
    dtitvfmt->adoptDateIntervalInfo(nonConstInf, status);
    anotherInf = dtitvfmt->getDateIntervalInfo();
    if ( (*inf) != (*anotherInf) || U_FAILURE(status) ) {
        dataerrln("ERROR: adoptDateIntervalInfo failed");
    }

    // ====== Test getDateFormat, setDateFormat, adoptDateFormat
    
    status = U_ZERO_ERROR;
    logln("Testing DateIntervalFormat getDateFormat");
    const DateFormat* fmt = another->getDateFormat();
    dtitvfmt->setDateFormat(*fmt, status);
    const DateFormat* anotherFmt = dtitvfmt->getDateFormat();
    if ( (*fmt) != (*anotherFmt) || U_FAILURE(status) ) {
        dataerrln("ERROR: getDateFormat/setDateFormat failed");
    }

    status = U_ZERO_ERROR;
    DateFormat* nonConstFmt = (DateFormat*)fmt->clone();
    dtitvfmt->adoptDateFormat(nonConstFmt, status);
    anotherFmt = dtitvfmt->getDateFormat();
    if ( (*fmt) != (*anotherFmt) || U_FAILURE(status) ) {
        dataerrln("ERROR: adoptDateFormat failed");
    }


    // ======= Test getStaticClassID()

    logln("Testing getStaticClassID()");


    if(dtitvfmt->getDynamicClassID() != DateIntervalFormat::getStaticClassID()) {
        errln("ERROR: getDynamicClassID() didn't return the expected value");
    }
    
    delete another;

    // ====== test constructor/copy constructor and assignment
    /* they are protected, no test
    logln("Testing DateIntervalFormat constructor and assigment operator");
    status = U_ZERO_ERROR;

    DateFormat* constFmt = (constFmt*)dtitvfmt->getDateFormat()->clone();
    inf = dtitvfmt->getDateIntervalInfo()->clone();


    DateIntervalFormat* dtifmt = new DateIntervalFormat(fmt, inf, status);
    if(U_FAILURE(status)) {
        dataerrln("ERROR: Could not create DateIntervalFormat (default) - exitting");
        return;
    } 

    DateIntervalFormat* dtifmt2 = new(dtifmt);
    if ( (*dtifmt) != (*dtifmt2) ) {
        dataerrln("ERROR: Could not create DateIntervalFormat (default) - exitting");
        return;
    }

    DateIntervalFormat dtifmt3 = (*dtifmt);
    if ( (*dtifmt) != dtifmt3 ) {
        dataerrln("ERROR: Could not create DateIntervalFormat (default) - exitting");
        return;
    }

    delete dtifmt2;
    delete dtifmt3;
    delete dtifmt;
    */

    delete dtitvfmt;


    //====== test format  in testFormat()
    
}


/**
 * Test various generic API methods of DateIntervalFormat for API coverage.
 */
void DateIntervalFormatTest::testFormat() {

    const char* DATA[] = {
        "yyyy MM dd HH:mm:ss",
        "2007 10 10 10:10:10", "2008 10 10 10:10:10", 
        "2007 10 10 10:10:10", "2007 11 10 10:10:10", 
        "2007 11 10 10:10:10", "2007 11 20 10:10:10", 
        "2007 01 10 10:00:10", "2007 01 10 14:10:10", 
        "2007 01 10 10:00:10", "2007 01 10 10:20:10", 
        "2007 01 10 10:10:10", "2007 01 10 10:10:20", 
    };

    const char* testLocale[][3] = {
        {"en", "", ""},
        {"zh", "", ""},
        {"de", "", ""},
        {"ar", "", ""},
        {"en", "GB",  ""},
        {"fr", "", ""},
        {"it", "", ""},
        {"nl", "", ""},
        {"zh", "TW",  ""},
        {"ja", "", ""},
        {"pt", "BR", ""},
        {"ru", "", ""},
        {"pl", "", ""},
        {"tr", "", ""},
        {"es", "", ""},
        {"ko", "", ""},
        {"th", "", ""},
        {"sv", "", ""},
        {"fi", "", ""},
        {"da", "", ""},
        {"pt", "PT", ""},
        {"ro", "", ""},
        {"hu", "", ""},
        {"he", "", ""},
        {"in", "", ""},
        {"cs", "", ""},
        {"el", "", ""},
        {"no", "", ""},
        {"vi", "", ""},
        {"bg", "", ""},
        {"hr", "", ""},
        {"lt", "", ""},
        {"sk", "", ""},
        {"sl", "", ""},
        {"sr", "", ""},
        {"ca", "", ""},
        {"lv", "", ""},
        {"uk", "", ""},
        {"hi", "", ""},
    };

    
    uint32_t localeIndex;
    for ( localeIndex = 0; localeIndex < ARRAY_SIZE(testLocale); ++localeIndex ) {
        char locName[32];
        uprv_strcpy(locName, testLocale[localeIndex][0]);
        uprv_strcat(locName, testLocale[localeIndex][1]);
        expect(DATA, ARRAY_SIZE(DATA), Locale(testLocale[localeIndex][0], testLocale[localeIndex][1], testLocale[localeIndex][2]), locName);
    }
}


void DateIntervalFormatTest::expect(const char** data, int32_t data_length,
                                    const Locale& loc, const char* locName) {

    /*
    UnicodeString formatResults[] = {
    };
    */

    UnicodeString skeleton[] = {
        "EEEEdMMMMy",
        "dMMMMy",
        "dMMMM",
        "MMMMy",
        "EEEEdMMMM",
        "EEEdMMMy",
        "dMMMy",
        "dMMM",
        "MMMy",
        "EEEdMMM",
        "EEEdMy",
        "dMy",
        "dM",
        "My",
        "EEEdM",
        "d",
        "EEEd",
        "y",
        "M",
        "MMM",
        "MMMM",
        "hm",
        "hmv",
        "hmz",
        "h",
        "hv",
        "hz",
        "EEddMMyyyy", // following could be normalized
        "EddMMy", 
        "hhmm",
        "hhmmzz",
        "hms",  // following could not be normalized
        "dMMMMMy",
        "EEEEEdM",
    };

    int32_t i = 0;
    UErrorCode ec = U_ZERO_ERROR;
    UnicodeString str, str2;
    SimpleDateFormat ref(data[i++], loc, ec);
    if (!assertSuccess("construct SimpleDateFormat", ec)) return;

#ifdef DTIFMTTS_DEBUG
    char result[1000]; 
    char mesg[1000];
    sprintf(mesg, "locale: %s\n", locName);  
    PRINTMESG(mesg);
#endif

    while (i<data_length) {

            // 'f'
            const char* datestr = data[i++];
            const char* datestr_2 = data[i++];
#ifdef DTIFMTTS_DEBUG
            sprintf(mesg, "original date: %s - %s\n", datestr, datestr_2);
            PRINTMESG(mesg)
#endif
            UDate date = ref.parse(ctou(datestr), ec);
            if (!assertSuccess("parse", ec)) return;
            UDate date_2 = ref.parse(ctou(datestr_2), ec);
            if (!assertSuccess("parse", ec)) return;
            DateInterval dtitv(date, date_2);

            for ( uint32_t skeletonIndex = 0; 
                  skeletonIndex < ARRAY_SIZE(skeleton); 
                  ++skeletonIndex ) {
                const UnicodeString& oneSkeleton = skeleton[skeletonIndex];
                DateIntervalFormat* dtitvfmt = DateIntervalFormat::createInstance(oneSkeleton, loc, ec);
                if (!assertSuccess("createInstance(skeleton)", ec)) return;
                FieldPosition pos=0;
                dtitvfmt->format(&dtitv, str.remove(), pos, ec);
                if (!assertSuccess("format", ec)) return;
#ifdef DTIFMTTS_DEBUG
                oneSkeleton.extract(0,  oneSkeleton.length(), result, "UTF-8");
                sprintf(mesg, "interval by skeleton: %s\n", result);
                PRINTMESG(mesg)
                str.extract(0,  str.length(), result, "UTF-8");
                sprintf(mesg, "interval date: %s\n", result);
                PRINTMESG(mesg)
#endif
                delete dtitvfmt;
            }

            // test user created DateIntervalInfo
            ec = U_ZERO_ERROR;
            DateIntervalInfo* dtitvinf = new DateIntervalInfo(ec);
            dtitvinf->setFallbackIntervalPattern("{0} --- {1}");
            dtitvinf->setIntervalPattern(YEAR_MONTH_DAY_MEDIUM_FORMAT, UCAL_MONTH, "yyyy MMM d - MMM y",ec);
            if (!assertSuccess("DateIntervalInfo::setIntervalPattern", ec)) return;
            dtitvinf->setIntervalPattern(YEAR_MONTH_DAY_MEDIUM_FORMAT, UCAL_HOUR_OF_DAY, "yyyy MMM d HH:mm - HH:mm", ec);
            if (!assertSuccess("DateIntervalInfo::setIntervalPattern", ec)) return;
            DateIntervalFormat* dtitvfmt = DateIntervalFormat::createInstance(YEAR_MONTH_DAY_MEDIUM_FORMAT, loc, dtitvinf, ec);
            if (!assertSuccess("createInstance(skeleton,dtitvinf)", ec)) return;
            FieldPosition pos=0;
            dtitvfmt->format(&dtitv, str.remove(), pos, ec);
            if (!assertSuccess("format", ec)) return;
#ifdef DTIFMTTS_DEBUG
            PRINTMESG("interval format using user defined DateIntervalInfo\n");
            str.extract(0,  str.length(), result, "UTF-8");
            sprintf(mesg, "interval date: %s\n", result);
            PRINTMESG(mesg)
#endif
            delete dtitvfmt;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
