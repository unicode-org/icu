/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2007, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * DataDrivenCalendarTest is a test class that uses data stored in resource
 * bundles to perform testing. For more details on data structure, see
 * source/test/testdata/calendar.txt
 */

#ifndef _INTLTESTDATADRIVENCALENDAR
#define _INTLTESTDATADRIVENCALENDAR

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "tsdate.h"
#include "uvector.h"
#include "unicode/calendar.h"

class TestDataModule;
class TestData;
class DataMap;
class CalendarFieldsSet;


class DataDrivenCalendarTest: public IntlTest {
    void runIndexedTest(int32_t index, UBool exec, const char* &name, char* par = NULL );
public:
    DataDrivenCalendarTest();
    virtual ~DataDrivenCalendarTest();
protected:

    void DataDrivenTest(char *par);
    void processSequence(Calendar* cal, const UnicodeString &sequence);
    void processTest(TestData *testData);
    void processArguments(Calendar *cal, const UChar *start, int32_t optLen);
private:
    void testConvert(TestData *testData, const DataMap *settings, UBool fwd);
    void testConvert(int32_t n, const CalendarFieldsSet &fromSet, Calendar *fromCal, 
    							const CalendarFieldsSet &toSet, Calendar *toCal, UBool fwd);
private:
  TestDataModule *driver;
  UErrorCode status;
};



#endif /* #if !UCONFIG_NO_COLLATION */

#endif
