/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2007, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/***********************************************************************
 * Modification history
 * Date        Name        Description
 * 07/09/2007  srl         Copied from dadrcoll.cpp
 ***********************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/tstdtmod.h"
#include "tsdate.h"
#include "dadrcal.h"
#include "unicode/calendar.h"
#include "intltest.h"
#include <string.h>
#include "unicode/schriter.h"
#include "unicode/regex.h"
#include "unicode/smpdtfmt.h"
#include "unicode/dbgutil.h"

#include <stdio.h>

/** ------- Calendar Fields Set -------- **/
class CalendarFieldsSet {
    public:
        CalendarFieldsSet();
        ~CalendarFieldsSet();

        void clear();
        void clear(UCalendarDateFields field);
        void set(UCalendarDateFields field, int32_t amount);

        UBool isSet(UCalendarDateFields field) const;
        int32_t get(UCalendarDateFields field) const;

        /**
         * @param diffSet fillin to hold any fields different. Will have the calendar's value set on them.
         * @return true if the calendar matches in these fields.
         */
        UBool matches(Calendar *cal, CalendarFieldsSet &diffSet,
                UErrorCode& status) const;

        /**
         * set the specified fields on this calendar. Doesn't clear first. Returns any errors the cale 
         */
        void setOnCalendar(Calendar *cal, UErrorCode& status) const;

        /**
         * @param "expected" set to match against
         * @return a formatted string listing which fields are set in 
         *   this, with the comparison made agaainst those fields in other.
         */
        UnicodeString diffFrom(const CalendarFieldsSet& other) const;
        /**
         * parse from a string.
         * @param str the string to parse
         * @param inherit the CalendarFieldsSet to inherit any inherited items from, or NULL
         * @param status error code
         * @return the number of items successfully parsed
         */
        int32_t parseFrom(const UnicodeString& str,
                const CalendarFieldsSet* inheritFrom, UErrorCode& status);
        int32_t parseFrom(const UnicodeString& str, UErrorCode& status);

    private:
        int32_t fValue[UDAT_FIELD_COUNT]; /** field values **/
        UBool fIsSet[UDAT_FIELD_COUNT]; /** Is this field set? **/
};

CalendarFieldsSet::CalendarFieldsSet() {
    clear();
}

CalendarFieldsSet::~CalendarFieldsSet() {
    // 
}

void CalendarFieldsSet::clear() {
    for (int i=0; i<UDAT_FIELD_COUNT; i++) {
        fValue[i]=-1;
        fIsSet[i]=FALSE;
    }
}

void CalendarFieldsSet::clear(UCalendarDateFields field) {
    if (field<0|| field>=UCAL_FIELD_COUNT) {
        return;
    }
    fValue[field] = -1;
    fIsSet[field] = FALSE;
}
void CalendarFieldsSet::set(UCalendarDateFields field, int32_t amount) {
    if (field<0|| field>=UCAL_FIELD_COUNT) {
        return;
    }
    fValue[field] = amount;
    fIsSet[field] = TRUE;
}

UBool CalendarFieldsSet::isSet(UCalendarDateFields field) const {
    if (field<0|| field>=UCAL_FIELD_COUNT) {
        return FALSE;
    }
    return fIsSet[field];
}
int32_t CalendarFieldsSet::get(UCalendarDateFields field) const {
    if (field<0|| field>=UCAL_FIELD_COUNT) {
        return -1;
    }
    return fValue[field];
}

/**
 * set the specified fields on this calendar. Doesn't clear first. Returns any errors the caller 
 */
void CalendarFieldsSet::setOnCalendar(Calendar *cal, UErrorCode& /*status*/) const {
    for (int i=0; i<UDAT_FIELD_COUNT; i++) {
        if (isSet((UCalendarDateFields)i)) {
            int32_t value = get((UCalendarDateFields)i);
            //fprintf(stderr, "Setting: %s#%d=%d\n",udbg_enumName(UDBG_UCalendarDateFields,i),i,value);            
            cal->set((UCalendarDateFields)i, value);
        }
    }
}

/**
 * return true if the calendar matches in these fields
 */
UBool CalendarFieldsSet::matches(Calendar *cal, CalendarFieldsSet &diffSet,
        UErrorCode& status) const {
    UBool match = TRUE;
    if (U_FAILURE(status))
        return FALSE;
    for (int i=0; i<UDAT_FIELD_COUNT; i++) {
        if (isSet((UCalendarDateFields)i)) {
            int32_t calVal = cal->get((UCalendarDateFields)i, status);
            if (U_FAILURE(status))
                return FALSE;
            if (calVal != get((UCalendarDateFields)i)) {
                match = FALSE;
                diffSet.set((UCalendarDateFields)i, calVal);
                //fprintf(stderr, "match failed: %s#%d=%d != %d\n",udbg_enumName(UDBG_UCalendarDateFields,i),i,cal->get((UCalendarDateFields)i,status), get((UCalendarDateFields)i));;
            }
        }
    }
    return match;
}

UnicodeString CalendarFieldsSet::diffFrom(const CalendarFieldsSet& other) const {
    UnicodeString str;
    for (int i=0; i<UDAT_FIELD_COUNT; i++) {
        if (isSet((UCalendarDateFields)i)) {
            int32_t myVal = get((UCalendarDateFields)i);
            int32_t theirVal = other.get((UCalendarDateFields)i);
            const UnicodeString& fieldName = udbg_enumString(
                    UDBG_UCalendarDateFields, i);

            str = str + fieldName +"="+myVal+" not "+theirVal+", ";
        }
    }
    return str;
}

int32_t CalendarFieldsSet::parseFrom(const UnicodeString& str,
        UErrorCode& status) {
    return parseFrom(str, NULL, status);
}

/**
 * parse from a string.
 * @param str the string to parse
 * @param inherit the CalendarFieldsSet to inherit any inherited items from, or NULL
 * @param status error code
 * @return the number of items successfully parsed or -1 on error
 */
int32_t CalendarFieldsSet::parseFrom(const UnicodeString& str,
        const CalendarFieldsSet* inheritFrom, UErrorCode& status) {
    UnicodeString pattern(",", "");
    RegexMatcher matcher(pattern, 0, status);
    UnicodeString pattern2("=", "");
    RegexMatcher matcher2(pattern2, 0, status);
    if (U_FAILURE(status))
        return -1;

    UnicodeString dest[UDAT_FIELD_COUNT+10];
    int32_t destCount = matcher.split(str, dest, sizeof(dest)/sizeof(dest[0]), status);
    if(U_FAILURE(status)) return -1;
    for(int i=0;i<destCount;i++) {
        UnicodeString kv[2];
        matcher2.split(dest[i],kv,2,status);
        if(U_FAILURE(status)) {
            fprintf(stderr, "Parse failed: splitting\n");
            return -1;
        }

        int32_t field = udbg_enumByString(UDBG_UCalendarDateFields, kv[0]);

        if(field == -1) {
            char ch[256];
            const UChar *u = kv[0].getBuffer();
            int32_t len = kv[0].length();
            u_UCharsToChars(u, ch, len);
            ch[len] = 0; /* include terminating \0 */
            fprintf(stderr,"Parse Failed: Unknown Field %s\n", ch);
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return -1;
        }

        int value = -1;

        if(kv[1].length()==0) {
            // inherit
            if((inheritFrom == NULL) || !inheritFrom->isSet((UCalendarDateFields)field)) {
                // couldn't inherit from field 
                fprintf(stderr,"Parse Failed: Couldn't inherit field %d [%s]\n", field, udbg_enumName(UDBG_UCalendarDateFields, field));
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return -1;
            }
            value = inheritFrom->get((UCalendarDateFields)field);
        } else {
            if(field==UCAL_MONTH) {
                value = udbg_enumByString(UDBG_UCalendarMonths, kv[1]);
            } // else, other types..
            if(value==-1) {
                // parse as decimal
                value = udbg_stoi(kv[1]);
            }
        }
        //fprintf(stderr, "Parsed: %s#%d=%d\n",udbg_enumName(UDBG_UCalendarDateFields,field),field,value);
        set((UCalendarDateFields)field,value);
    }

    return -1;
}

DataDrivenCalendarTest::DataDrivenCalendarTest() {
    UErrorCode status = U_ZERO_ERROR;
    driver = TestDataModule::getTestDataModule("calendar", *this, status);
}

DataDrivenCalendarTest::~DataDrivenCalendarTest() {
    delete driver;
}

void DataDrivenCalendarTest::runIndexedTest(int32_t index, UBool exec,
        const char* &name, char* /*par */) {
    if (driver != NULL) {
        if (exec) {
            //  logln("Begin ");
        }
        const DataMap *info= NULL;
        UErrorCode status= U_ZERO_ERROR;
        TestData *testData = driver->createTestData(index, status);
        if (U_SUCCESS(status)) {
            name = testData->getName();
            if (testData->getInfo(info, status)) {
                log(info->getString("Description", status));
            }
            if (exec) {
                log(name);
                logln("---");
                logln("");

                processTest(testData);
            }
            delete testData;
        } else {
            name = "";
        }
    } else {
        errln("format/DataDrivenTest data (calendar.res) not initialized!");
        name = "";
    }

}

void DataDrivenCalendarTest::testOps(TestData *testData,
        const DataMap * /*settings*/) {
    UErrorCode status = U_ZERO_ERROR;
    const DataMap *currentCase= NULL;
    char toCalLoc[256] = "";

    // TODO: static strings?
    const UnicodeString kADD("add", "");
    const UnicodeString kROLL("roll", "");

    // Get 'from' time 
    CalendarFieldsSet fromSet, toSet, paramsSet, diffSet;
    SimpleDateFormat fmt(UnicodeString("EEE MMM dd yyyy / YYYY'-W'ww-ee"),
            status);
    if (U_FAILURE(status)) {
        errln("FAIL: Couldn't create SimpleDateFormat: %s\n",
                u_errorName(status));
        return;
    }
    // Start the processing
    int n = 0;
    while (testData->nextCase(currentCase, status)) {
        ++n;
        Calendar *toCalendar= NULL;
        Calendar *fromCalendar= NULL;

        // load parameters
        char theCase[200];
        sprintf(theCase, "[case %d]", n);
        UnicodeString caseString(theCase, "");
        // build to calendar
        //             Headers { "locale","from","operation","params","to" }
        // #1 locale
        const char *param = "locale";
        UnicodeString locale;
        UnicodeString testSetting = currentCase->getString(param, status);
        if (U_FAILURE(status)) {
            errln(caseString+": Unable to get param '"+param+"' "
                    + UnicodeString(" - "));
            continue;
        }
        testSetting.extract(0, testSetting.length(), toCalLoc, (const char*)0);
        fromCalendar = Calendar::createInstance(toCalLoc, status);
        if (U_FAILURE(status)) {
            errln(caseString+": Unable to instantiate calendar for "
                    +testSetting);
            continue;
        }

        fromSet.clear();
        // #2 'from' info
        param = "from";
        UnicodeString from = testSetting=currentCase->getString(param, status);
        if (U_FAILURE(status)) {
            errln(caseString+": Unable to get parameter '"+param+"' "
                    + UnicodeString(" - "));
            continue;
        }
        fromSet.parseFrom(testSetting, status);
        if (U_FAILURE(status)) {
            errln(caseString+": Failed to parse '"+param+"' parameter: "
                    +testSetting);
            continue;
        }

        // #4 'operation' info
        param = "operation";
        UnicodeString operation = testSetting=currentCase->getString(param,
                status);
        if (U_FAILURE(status)) {
            errln(caseString+": Unable to get parameter '"+param+"' "
                    + UnicodeString(" - "));
            continue;
        }
        if (U_FAILURE(status)) {
            errln(caseString+": Failed to parse '"+param+"' parameter: "
                    +testSetting);
            continue;
        }

        paramsSet.clear();
        // #3 'params' info
        param = "params";
        UnicodeString params = testSetting
                =currentCase->getString(param, status);
        if (U_FAILURE(status)) {
            errln(caseString+": Unable to get parameter '"+param+"' "
                    + UnicodeString(" - "));
            continue;
        }
        paramsSet.parseFrom(testSetting, status); // parse with inheritance.
        if (U_FAILURE(status)) {
            errln(caseString+": Failed to parse '"+param+"' parameter: "
                    +testSetting);
            continue;
        }

        toSet.clear();
        // #4 'to' info
        param = "to";
        UnicodeString to = testSetting=currentCase->getString(param, status);
        if (U_FAILURE(status)) {
            errln(caseString+": Unable to get parameter '"+param+"' "
                    + UnicodeString(" - "));
            continue;
        }
        toSet.parseFrom(testSetting, &fromSet, status); // parse with inheritance.
        if (U_FAILURE(status)) {
            errln(caseString+": Failed to parse '"+param+"' parameter: "
                    +testSetting);
            continue;
        }

        UnicodeString caseContentsString = locale+":  from "+from+": "
                +operation +" [[[ "+params+" ]]]   >>> "+to;
        logln(caseString+": "+caseContentsString);

        // ------
        // now, do it.

        /// prepare calendar
        fromSet.setOnCalendar(fromCalendar, status);
        if (U_FAILURE(status)) {
            errln(caseString+" FAIL: Failed to set on Source calendar: "
                    + u_errorName(status));
            return;
        }

        
        diffSet.clear();
        // Is the calendar sane after being set?
        if (!fromSet.matches(fromCalendar, diffSet, status)) {
            UnicodeString diffs = diffSet.diffFrom(fromSet);
            errln((UnicodeString)"FAIL: "+caseString
                    +", SET SOURCE calendar was not set: Differences: "+ diffs
                    +"', status: "+ u_errorName(status));
        } else if (U_FAILURE(status)) {
            errln("FAIL: "+caseString+" SET SOURCE calendar Failed to match: "
                    +u_errorName(status));
        } else {
            logln("PASS: "+caseString+" SET SOURCE calendar match.");
        }
        
        // to calendar - copy of from calendar
        toCalendar = fromCalendar->clone();

        /// perform op
        for (int q=0; q<UCAL_FIELD_COUNT; q++) {
            if (paramsSet.isSet((UCalendarDateFields)q)) {
                if (operation == kROLL) {
                    toCalendar->roll((UCalendarDateFields)q,
                            paramsSet.get((UCalendarDateFields)q), status);
                } else if (operation == kADD) {
                    toCalendar->add((UCalendarDateFields)q,
                            paramsSet.get((UCalendarDateFields)q), status);
                } else {
                    errln(caseString+ " FAIL: unknown operation "+ operation);
                }
                logln(operation + " of "+ paramsSet.get((UCalendarDateFields)q)
                        +" -> "+u_errorName(status));
            }
        }
        if (U_FAILURE(status)) {
            errln(caseString+" FAIL: after "+operation+" of "+params+" -> "
                    +u_errorName(status));
            continue;
        }

        // now - what's the result?
        diffSet.clear();

        if (!toSet.matches(toCalendar, diffSet, status)) {
            UnicodeString diffs = diffSet.diffFrom(toSet);
            errln((UnicodeString)"FAIL: "+caseString+" - , "+caseContentsString
                    +" Differences: "+ diffs +"', status: "
                    + u_errorName(status));
        } else if (U_FAILURE(status)) {
            errln("FAIL: "+caseString+" Match operation had an error: "
                    +u_errorName(status));
        } else {
            logln("PASS: "+caseString+" matched!");
        }

        delete fromCalendar;
        delete toCalendar;
    }
}

void DataDrivenCalendarTest::testConvert(int32_t n,
        const CalendarFieldsSet &fromSet, Calendar *fromCalendar,
        const CalendarFieldsSet &toSet, Calendar *toCalendar, UBool forward) {
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString thisString = (UnicodeString)"#"+n+" "+(forward ? "forward"
            : "reverse")+" "+fromCalendar->getType()+"->"+toCalendar->getType();

    fromCalendar->clear();

    fromSet.setOnCalendar(fromCalendar, status);
    if (U_FAILURE(status)) {
        errln("FAIL: Failed to set on Source calendar: %s", u_errorName(status));
        return;
    }

    CalendarFieldsSet diffSet;

    diffSet.clear();
    // Is the calendar sane at the first?
    if (!fromSet.matches(fromCalendar, diffSet, status)) {
        UnicodeString diffs = diffSet.diffFrom(fromSet);
        errln((UnicodeString)"FAIL: "+thisString
                +", SOURCE calendar was not set: Differences: "+ diffs
                +"', status: "+ u_errorName(status));
    } else if (U_FAILURE(status)) {
        errln("FAIL: "+thisString+" SOURCE calendar Failed to match: "
                +u_errorName(status));
    } else {
        logln("PASS: "+thisString+" SOURCE calendar match.");
    }

    //logln("Set Source calendar: " + from);

    UDate fromTime = fromCalendar->getTime(status);
    if (U_FAILURE(status)) {
        errln("FAIL: Failed to get Source time: %s", u_errorName(status));
        return;
    }

    diffSet.clear();
    // Is the calendar sane after being set?
    if (!fromSet.matches(fromCalendar, diffSet, status)) {
        UnicodeString diffs = diffSet.diffFrom(fromSet);
        errln((UnicodeString)"FAIL: "+thisString
                +", SET SOURCE calendar was not set: Differences: "+ diffs
                +"', status: "+ u_errorName(status));
    } else if (U_FAILURE(status)) {
        errln("FAIL: "+thisString+" SET SOURCE calendar Failed to match: "
                +u_errorName(status));
    } else {
        logln("PASS: "+thisString+" SET SOURCE calendar match.");
    }

    toCalendar->clear();
    toCalendar->setTime(fromTime, status);
    if (U_FAILURE(status)) {
        errln("FAIL: Failed to set Target time: %s", u_errorName(status));
        return;
    }

    diffSet.clear();
    if (!toSet.matches(toCalendar, diffSet, status)) {
        UnicodeString diffs = diffSet.diffFrom(toSet);
        errln((UnicodeString)"FAIL: "+thisString+", Differences: "+ diffs
                +"', status: "+ u_errorName(status));
        SimpleDateFormat fmt(UnicodeString("EEE MMM dd yyyy G"), status);
        UnicodeString fromString;
        fmt.format(fromTime, fromString);
        logln("Source Time: "+fromString+", Source Calendar: "
                +fromCalendar->getType());
    } else if (U_FAILURE(status)) {
        errln("FAIL: "+thisString+" Failed to match: "+u_errorName(status));
    } else {
        logln("PASS: "+thisString+" match.");
    }
}

void DataDrivenCalendarTest::testConvert(TestData *testData,
        const DataMap *settings, UBool forward) {
    UErrorCode status = U_ZERO_ERROR;
    Calendar *toCalendar= NULL;
    const DataMap *currentCase= NULL;
    char toCalLoc[256] = "";
    char fromCalLoc[256] = "";
    // build to calendar
    UnicodeString testSetting = settings->getString("ToCalendar", status);
    if (U_SUCCESS(status)) {
        testSetting.extract(0, testSetting.length(), toCalLoc, (const char*)0);
        toCalendar = Calendar::createInstance(toCalLoc, status);
        if (U_FAILURE(status)) {
            errln("Unable to instantiate ToCalendar for "+testSetting);
            return;
        }
    }

    CalendarFieldsSet fromSet, toSet, diffSet;
    SimpleDateFormat fmt(UnicodeString("EEE MMM dd yyyy / YYYY'-W'ww-ee"),
            status);
    if (U_FAILURE(status)) {
        errln("FAIL: Couldn't create SimpleDateFormat: %s\n",
                u_errorName(status));
        return;
    }
    // Start the processing
    int n = 0;
    while (testData->nextCase(currentCase, status)) {
        ++n;
        Calendar *fromCalendar= NULL;
        UnicodeString locale = currentCase->getString("locale", status);
        if (U_SUCCESS(status)) {
            locale.extract(0, locale.length(), fromCalLoc, (const char*)0); // default codepage.  Invariant codepage doesn't have '@'!
            fromCalendar = Calendar::createInstance(fromCalLoc, status);
            if (U_FAILURE(status)) {
                errln("Unable to instantiate fromCalendar for "+locale);
                return;
            }
        } else {
            errln("No 'locale' line.");
            continue;
        }

        fromSet.clear();
        toSet.clear();

        UnicodeString from = currentCase->getString("from", status);
        if (U_FAILURE(status)) {
            errln("No 'from' line.");
            continue;
        }
        fromSet.parseFrom(from, status);
        if (U_FAILURE(status)) {
            errln("Failed to parse 'from' parameter: "+from);
            continue;
        }
        UnicodeString to = currentCase->getString("to", status);
        if (U_FAILURE(status)) {
            errln("No 'to' line.");
            continue;
        }
        toSet.parseFrom(to, &fromSet, status);
        if (U_FAILURE(status)) {
            errln("Failed to parse 'to' parameter: "+to);
            continue;
        }

        // now, do it.
        if (forward) {
            logln((UnicodeString)"#"+n+" "+locale+"/"+from+" >>> "+toCalLoc+"/"
                    +to);
            testConvert(n, fromSet, fromCalendar, toSet, toCalendar, forward);
        } else {
            logln((UnicodeString)"#"+n+" "+locale+"/"+from+" <<< "+toCalLoc+"/"
                    +to);
            testConvert(n, toSet, toCalendar, fromSet, fromCalendar, forward);
        }

        delete fromCalendar;
    }
    delete toCalendar;
}

void DataDrivenCalendarTest::processTest(TestData *testData) {
    //Calendar *cal= NULL;
    //const UChar *arguments= NULL;
    //int32_t argLen = 0;
    char testType[256];
    const DataMap *settings= NULL;
    //const UChar *type= NULL;
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString testSetting;
    int n = 0;
    while (testData->nextSettings(settings, status)) {
        status = U_ZERO_ERROR;
        // try to get a locale
        testSetting = settings->getString("Type", status);
        if (U_SUCCESS(status)) {
            if ((++n)>0) {
                logln("---");
            }
            logln(testSetting + "---");
            testSetting.extract(0, testSetting.length(), testType, "");
        } else {
            errln("Unable to extract 'Type'. Skipping..");
            continue;
        }

        if (!strcmp(testType, "convert_fwd")) {
            testConvert(testData, settings, true);
        } else if (!strcmp(testType, "convert_rev")) {
            testConvert(testData, settings, false);
        } else if (!strcmp(testType, "ops")) {
            testOps(testData, settings);
        } else {
            errln("Unknown type: %s", testType);
        }
    }
}

#endif
