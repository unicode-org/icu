
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

#ifndef __TimeZoneBoundaryTest__
#define __TimeZoneBoundaryTest__
 
#include "utypes.h"
#include "caltztst.h"
class TimeZone;
class SimpleTimeZone;

/**
 * A test which discovers the boundaries of DST programmatically and verifies
 * that they are correct.
 */
class TimeZoneBoundaryTest: public CalendarTimeZoneTest {
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public: // package
    static UDate ONE_SECOND;
    static UDate ONE_MINUTE;
    static UDate ONE_HOUR;
    static UDate ONE_DAY;
    static UDate ONE_YEAR;
    static UDate SIX_MONTHS;
    static int32_t MONTH_LENGTH[];
    static UDate PST_1997_BEG;
    static UDate PST_1997_END;
    static UDate INTERVAL;
    /**
     * Date.toString().substring() Boundary Test
     * Look for a DST changeover to occur within 6 months of the given Date.
     * The initial Date.toString() should yield a string containing the
     * startMode as a SUBSTRING.  The boundary will be tested to be
     * at the expectedBoundary value.
     */

    /**
     * internal routines used by major test routines to perform subtests
     **/
    virtual void findDaylightBoundaryUsingDate(UDate d, const char* startMode, UDate expectedBoundary);
    virtual void findDaylightBoundaryUsingTimeZone(UDate d, bool_t startsInDST, UDate expectedBoundary);
    virtual void findDaylightBoundaryUsingTimeZone(UDate d, bool_t startsInDST, UDate expectedBoundary, TimeZone* tz);
 
private:
    //static UnicodeString* showDate(long l);
    UnicodeString showDate(UDate d);
    static UnicodeString showNN(int32_t n);
 
public: // package
    /**
     * Given a date, a TimeZone, and expected values for inDaylightTime,
     * useDaylightTime, zone and DST offset, verify that this is the case.
     */
    virtual void verifyDST(UDate d, TimeZone* time_zone, bool_t expUseDaylightTime, bool_t expInDaylightTime, UDate expZoneOffset, UDate expDSTOffset);
 
public:
    /**
     * Test the behavior of SimpleTimeZone at the transition into and out of DST.
     * Use a binary search to find boundaries.
     */
    virtual void TestBoundaries(void);
 
public: // package
    /**
     * internal subroutine used by TestNewRules
     **/
    virtual void testUsingBinarySearch(SimpleTimeZone* tz, UDate d, UDate expectedBoundary);
 
public:
    /**
     * Test the handling of the "new" rules; that is, rules other than nth Day of week.
     */
    virtual void TestNewRules(void);
 
public: // package
    /**
     * Find boundaries by stepping.
     */
    virtual void findBoundariesStepwise(int32_t year, UDate interval, TimeZone* z, int32_t expectedChanges);
 
public:
    /**
     * Test the behavior of SimpleTimeZone at the transition into and out of DST.
     * Use a stepwise march to find boundaries.
     */ 
    virtual void TestStepwise(void);
};
 
#endif // __TimeZoneBoundaryTest__
