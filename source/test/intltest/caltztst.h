/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1998      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
* File CALTZTST.H
*
* Modification History:
*
*   Date        Name        Description
*   08/06/97    aliu        Creation.
********************************************************************************
*/

#ifndef _CALTZTST
#define _CALTZTST

#include "utypes.h"
#include "unistr.h"
#include "intltest.h"
class SimpleDateFormat;
class DateFormat;
class Calendar;

/** 
 * This class doesn't perform any tests, but provides utility methods to its subclasses.
 **/
class CalendarTimeZoneTest : public IntlTest
{
protected:
    // Return true if the given status indicates failure.  Also has the side effect
    // of calling errln().  Msg should be of the form "Class::Method" in general.
    bool_t failure(UErrorCode status, const char* msg);

    // Utility method for formatting dates for printing; useful for Java->C++ conversion.
    // Tries to mimic the Java Date.toString() format.
    UnicodeString  dateToString(UDate d);
    UnicodeString& dateToString(UDate d, UnicodeString& str);

    // Utility methods to create a date.  This is useful for converting Java constructs
    // which create a Date object.  Returns a Date in the current local time.
    UDate date(int32_t y, int32_t m, int32_t d, int32_t hr=0, int32_t min=0, int32_t sec=0);

    // Utility methods to create a date.  Returns a Date in UTC.  This will differ
    // from local dates returned by date() by the current default zone offset.
//  Date utcDate(int y, int m, int d, int hr=0, int min=0, int sec=0);  

    // Utility method to get the fields of a date; similar to Date.getYear() etc.
    void dateToFields(UDate date, int32_t& y, int32_t& m, int32_t& d, int32_t& hr, int32_t& min, int32_t& sec);

protected:
    static DateFormat*         fgDateFormat;
    static Calendar*           fgCalendar;

    // the 'get()' functions are not static because they can call errln().
    // they are effectively static otherwise.
     DateFormat*               getDateFormat(void);
    static void                releaseDateFormat(DateFormat *f);

     Calendar*                 getCalendar(void);
    static void                releaseCalendar(Calendar *c);
};

#endif //_CALTZTST
//eof
