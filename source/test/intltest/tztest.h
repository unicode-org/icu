
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/
 
#ifndef __TimeZoneTest__
#define __TimeZoneTest__
 
#include "utypes.h"
#include "caltztst.h"
class SimpleTimeZone;

/** 
 * Various tests for TimeZone
 **/
class TimeZoneTest: public CalendarTimeZoneTest {
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public: // package
    static const int32_t millisPerHour;
 
public:
    /**
     * Test the offset of the PRT timezone.
     */
    virtual void TestPRTOffset(void);
    /**
     * Regress a specific bug with a sequence of API calls.
     */
    virtual void TestVariousAPI518(void);
    /**
     * Test the call which retrieves the available IDs.
     */
    virtual void TestGetAvailableIDs913(void);

    /**
     * Generic API testing for API coverage.
     */
    virtual void TestGenericAPI(void);
    /**
     * Test the setStartRule/setEndRule API calls.
     */
    virtual void TestRuleAPI(void);

    /**
     * subtest used by TestRuleAPI
     **/
    void testUsingBinarySearch(SimpleTimeZone* tz, UDate min, UDate max, UDate expectedBoundary);


    /**
     *  Test short zone IDs for compliance
     */ 
    virtual void TestShortZoneIDs(void);


    /**
     *  Test parsing custom zones
     */ 
    virtual void TestCustomParse(void);
    
    /**
     *  Test new getDisplayName() API
     */ 
    virtual void TestDisplayName(void);

    void TestDSTSavings(void);
    void TestAlternateRules(void);


    static const UDate INTERVAL;

private:
    // internal functions
    static UnicodeString& formatMinutes(int32_t min, UnicodeString& rv);
};
 
#endif // __TimeZoneTest__
