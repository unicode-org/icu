/*
* Copyright © {1997-1999}, International Business Machines Corporation and others. All Rights Reserved.
********************************************************************************
*
* File TIMEZONE.H
*
* Modification History:
*
*   Date        Name        Description
*   04/21/97    aliu        Overhauled header.
*   07/09/97    helena      Changed createInstance to createDefault.
*   08/06/97    aliu        Removed dependency on internal header for Hashtable.
*    08/10/98    stephen        Changed getDisplayName() API conventions to match
*    08/19/98    stephen        Changed createTimeZone() to never return 0
*    09/02/98    stephen        Sync to JDK 1.2 8/31
*                             - Added getOffset(... monthlen ...)
*                             - Added hasSameRules()
*    09/15/98    stephen        Added getStaticClassID
*  12/03/99     aliu        Moved data out of static table into icudata.dll.
*                           Hashtable replaced by new static data structures.
*  12/14/99     aliu        Made GMT public.
********************************************************************************
*/

#ifndef TIMEZONE_H
#define TIMEZONE_H


#include "unicode/unistr.h"
#include "unicode/locid.h"

#include "unicode/udata.h"

class SimpleTimeZone;
struct TZHeader;
struct OffsetIndex;
  
/**
 * <code>TimeZone</code> represents a time zone offset, and also figures out daylight
 * savings.
 *
 * <p>
 * Typically, you get a <code>TimeZone</code> using <code>createDefault</code>
 * which creates a <code>TimeZone</code> based on the time zone where the program
 * is running. For example, for a program running in Japan, <code>createDefault</code>
 * creates a <code>TimeZone</code> object based on Japanese Standard Time.
 *
 * <p>
 * You can also get a <code>TimeZone</code> using <code>createTimeZone</code> along
 * with a time zone ID. For instance, the time zone ID for the Pacific
 * Standard Time zone is "PST". So, you can get a PST <code>TimeZone</code> object
 * with:
 * <blockquote>
 * <pre>
 * TimeZone *tz = TimeZone::createTimeZone("PST");
 * </pre>
 * </blockquote>
 * You can use <code>getAvailableIDs</code> method to iterate through
 * all the supported time zone IDs. You can then choose a
 * supported ID to get a <code>TimeZone</code>.
 * If the time zone you want is not represented by one of the
 * supported IDs, then you can create a custom time zone ID with
 * the following syntax:
 *
 * <blockquote>
 * <pre>
 * GMT[+|-]hh[[:]mm]
 * </pre>
 * </blockquote>
 *
 * For example, you might specify GMT+14:00 as a custom
 * time zone ID.  The <code>TimeZone</code> that is returned
 * when you specify a custom time zone ID does not include
 * daylight savings time.
 *

  
 * TimeZone is an abstract class representing a time zone.  A TimeZone is needed for
 * Calendar to produce local time for a particular time zone.  A TimeZone comprises
 * three basic pieces of information:<ul>
 *    <li>A time zone offset; that, is the number of milliseconds to add or subtract
 *      from a time expressed in terms of GMT to convert it to the same time in that
 *      time zone (without taking daylight savings time into account).
 *    <li>Logic necessary to take daylight savings time into account if daylight savings
 *      time is observed in that time zone (e.g., the days and hours on which daylight
 *      savings time begins and ends).
 *    <li>An ID.  This is a text string that uniquely identifies the time zone.</ul>
 *
 * (Only the ID is actually implemented in TimeZone; subclasses of TimeZone may handle
 * daylight savings time and GMT offset in different ways.  Currently we only have one
 * TimeZone subclass: SimpleTimeZone.)
 * <P>
 * The TimeZone class contains a static list containing a TimeZone object for every
 * combination of GMT offset and daylight-savings time rules currently in use in the
 * world, each with a unique ID.  Each ID consists of a region (usually a continent or
 * ocean) and a city in that region, separated by a slash, (for example, Pacific
 * Standard Time is "America/Los_Angeles.")  Because older versions of this class used
 * three- or four-letter abbreviations instead, there is also a table that maps the older
 * abbreviations to the newer ones (for example, "PST" maps to "America/LosAngeles").
 * Anywhere the API requires an ID, you can use either form.
 * <P>
 * To create a new TimeZone, you call the factory function TimeZone::createTimeZone()
 * and pass it a time zone ID.  You can use the createAvailableIDs() function to
 * obtain a list of all the time zone IDs recognized by createTimeZone().
 * <P>
 * You can also use TimeZone::createDefault() to create a TimeZone.  This function uses
 * platform-specific APIs to produce a TimeZone for the time zone corresponding to 
 * the client's computer's physical location.  For example, if you're in Japan (assuming
 * your machine is set up correctly), TimeZone::createDefault() will return a TimeZone
 * for Japanese Standard Time ("Asia/Tokyo").
 */
class U_I18N_API TimeZone {
public:
    /**
     * @stable
     */
    virtual ~TimeZone();

    /**
     * The GMT zone has a raw offset of zero and does not use daylight
     * savings time.
     */
    static const TimeZone* GMT;

    /**
     * Creates a <code>TimeZone</code> for the given ID.
     * @param ID the ID for a <code>TimeZone</code>, either an abbreviation such as
     * "PST", a full name such as "America/Los_Angeles", or a custom ID
     * such as "GMT-8:00".
     * @return the specified <code>TimeZone</code>, or the GMT zone if the given ID
     * cannot be understood.  Return result guaranteed to be non-null.  If you
     * require that the specific zone asked for be returned, check the ID of the
     * return result.
     * @stable
     */
    static TimeZone* createTimeZone(const UnicodeString& ID);

    /**
     * Returns a list of time zone IDs, one for each time zone with a given GMT offset.
     * The return value is a list because there may be several times zones with the same
     * GMT offset that differ in the way they handle daylight savings time.  For example,
     * the state of Arizona doesn't observe Daylight Savings time.  So if you ask for
     * the time zone IDs corresponding to GMT-7:00, you'll get back two time zone IDs:
     * "America/Denver," which corresponds to Mountain Standard Time in the winter and
     * Mountain Daylight Time in the summer, and "America/Phoenix", which corresponds to
     * Mountain Standard Time year-round, even in the summer.
     * <P>
     * The caller owns the list that is returned, but does not own the strings contained
     * in that list.  Delete the array, but DON'T delete the elements in the array.
     *
     * @param rawOffset  An offset from GMT in milliseconds.
     * @param numIDs     Receives the number of items in the array that is returned.
     * @return           An array of UnicodeString pointers, where each UnicodeString is
     *                   a time zone ID for a time zone with the given GMT offset.  If
     *                   there is no timezone that matches the GMT offset
     *                   specified, NULL is returned.
     * @stable
     */
    static const UnicodeString** const createAvailableIDs(int32_t rawOffset, int32_t& numIDs);

    /**
     * Returns a list of all time zone IDs supported by the TimeZone class (i.e., all
     * IDs that it's legal to pass to createTimeZone()).  The caller owns the list that
     * is returned, but does not own the strings contained in that list.  Delete the array,
     * but DON'T delete the elements in the array.
     *
     * @param numIDs  Receives the number of zone IDs returned.
     * @return        An array of UnicodeString pointers, where each is a time zone ID
     *                supported by the TimeZone class.
     * @stable
     */
    static const UnicodeString** const createAvailableIDs(int32_t& numIDs);

    /**
     * Creates a new copy of the default TimeZone for this host. Unless the default time
     * zone has already been set using adoptDefault() or setDefault(), the default is
     * determined by querying the system using methods in TPlatformUtilities. If the
     * system routines fail, or if they specify a TimeZone or TimeZone offset which is not
     * recognized, the TimeZone indicated by the ID kLastResortID is instantiated
     * and made the default.
     *
     * @return   A default TimeZone. Clients are responsible for deleting the time zone
     *           object returned.
     * @stable
     */
    static TimeZone* createDefault(void);

    /**
     * Sets the default time zone (i.e., what's returned by getDefault()) to be the
     * specified time zone.  If NULL is specified for the time zone, the default time
     * zone is set to the default host time zone.  This call adopts the TimeZone object
     * passed in; the clent is no longer responsible for deleting it.
     *
     * @param zone  A pointer to the new TimeZone object to use as the default.
     * @stable
     */
    static void adoptDefault(TimeZone* zone);

    /**
     * Same as adoptDefault(), except that the TimeZone object passed in is NOT adopted;
     * the caller remains responsible for deleting it.
     *
     * @param zone  The given timezone.
     * @system
     */
    static void setDefault(const TimeZone& zone);

    /**
     * Returns true if the two TimeZones are equal.  (The TimeZone version only compares
     * IDs, but subclasses are expected to also compare the fields they add.)
     *
     * @param that  The TimeZone object to be compared with.
     * @return      True if the given TimeZone is equal to this TimeZone; false
     *              otherwise.
     * @stable
     */
    virtual bool_t operator==(const TimeZone& that) const;

    /**
     * Returns true if the two TimeZones are NOT equal; that is, if operator==() returns
     * false.
     *
     * @param that  The TimeZone object to be compared with.
     * @return      True if the given TimeZone is not equal to this TimeZone; false
     *              otherwise.
     * @stable
     */
    bool_t operator!=(const TimeZone& that) const {return !operator==(that);}

    /**
     * Returns the TimeZone's adjusted GMT offset (i.e., the number of milliseconds to add
     * to GMT to get local time in this time zone, taking daylight savings time into
     * account) as of a particular reference date.  The reference date is used to determine
     * whether daylight savings time is in effect and needs to be figured into the offset
     * that is returned (in other words, what is the adjusted GMT offset in this time zone
     * at this particular date and time?).  For the time zones produced by createTimeZone(),
     * the reference data is specified according to the Gregorian calendar, and the date
     * and time fields are in GMT, NOT local time.
     *
     * @param era        The reference date's era
     * @param year       The reference date's year
     * @param month      The reference date's month (0-based; 0 is January)
     * @param day        The reference date's day-in-month (1-based)
     * @param dayOfWeek  The reference date's day-of-week (1-based; 1 is Sunday)
     * @param millis     The reference date's milliseconds in day, UTT (NOT local time).
     * @return           The offset in milliseconds to add to GMT to get local time.
     * @stable
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month, int32_t day,
                              uint8_t dayOfWeek, int32_t millis, UErrorCode& status) const = 0;
    /**
     * @deprecated 
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month, int32_t day,
                              uint8_t dayOfWeek, int32_t millis) const = 0;

    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add *to* UTC to get local time.
     * @param era the era of the given date.
     * @param year the year in the given date.
     * @param month the month in the given date.
     * Month is 0-based. e.g., 0 for January.
     * @param day the day-in-month of the given date.
     * @param dayOfWeek the day-of-week of the given date.
     * @param milliseconds the millis in day in <em>standard</em> local time.
     * @param monthLength the length of the given month in days.
     * @return the offset to add *to* GMT to get local time.
     * @stable
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month, int32_t day,
                           uint8_t dayOfWeek, int32_t milliseconds, 
                           int32_t monthLength, UErrorCode& status) const = 0;

    /**
     * Sets the TimeZone's raw GMT offset (i.e., the number of milliseconds to add
     * to GMT to get local time, before taking daylight savings time into account).
     *
     * @param offsetMillis  The new raw GMT offset for this time zone.
     * @stable
     */
    virtual void setRawOffset(int32_t offsetMillis) = 0;

    /**
     * Returns the TimeZone's raw GMT offset (i.e., the number of milliseconds to add
     * to GMT to get local time, before taking daylight savings time into account).
     *
     * @return   The TimeZone's raw GMT offset.
     * @stable
     */
    virtual int32_t getRawOffset(void) const = 0;

    /**
     * Fills in "ID" with the TimeZone's ID.
     *
     * @param ID  Receives this TimeZone's ID.
     * @return    "ID"
     * @stable
     */
    UnicodeString& getID(UnicodeString& ID) const;

    /**
     * Sets the TimeZone's ID to the specified value.  This doesn't affect any other
     * fields (for example, if you say<
     * blockquote><pre>
     * .     TimeZone* foo = TimeZone::createTimeZone("America/New_York");
     * .     foo.setID("America/Los_Angeles");
     * </pre></blockquote>
     * the time zone's GMT offset and daylight-savings rules don't change to those for
     * Los Angeles.  They're still those for New York.  Only the ID has changed.)
     *
     * @param ID  The new timezone ID.
     * @stable
     */
    void setID(const UnicodeString& ID);

    /**
     * Enum for use with getDisplayName
     */

    enum EDisplayType { 
        SHORT = 1,
        LONG 
    };

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the default locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @return the human-readable name of this time zone in the default locale.
     * @stable
     */
    UnicodeString& getDisplayName(UnicodeString& result) const;

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @stable
     */
    UnicodeString& getDisplayName(const Locale& locale, UnicodeString& result) const;

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the default locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style either <code>LONG</code> or <code>SHORT</code>
     * @return the human-readable name of this time zone in the default locale.
     * @stable
     */
    UnicodeString& getDisplayName(bool_t daylight, EDisplayType style, UnicodeString& result) const;

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style either <code>LONG</code> or <code>SHORT</code>
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @stable
     */
    UnicodeString& getDisplayName(bool_t daylight, EDisplayType style, const Locale& locale, UnicodeString& result) const;

    /**
     * Queries if this time zone uses daylight savings time.
     * @return true if this time zone uses daylight savings time,
     * false, otherwise.
     * @stable
     */
    virtual bool_t useDaylightTime(void) const = 0;

    /**
     * Queries if the given date is in daylight savings time in
     * this time zone.
     * @param date the given UDate.
     * @return true if the given date is in daylight savings time,
     * false, otherwise.
     * @deprecated
     */
    virtual bool_t inDaylightTime(UDate date, UErrorCode& status) const = 0;

    /**
     * Returns true if this zone has the same rule and offset as another zone.
     * That is, if this zone differs only in ID, if at all.
     * @param other the <code>TimeZone</code> object to be compared with
     * @return true if the given zone is the same as this one,
     * with the possible exception of the ID
     * @stable
     */
    virtual bool_t hasSameRules(const TimeZone& other) const;

    /**
     * Clones TimeZone objects polymorphically. Clients are responsible for deleting
     * the TimeZone object cloned.
     *
     * @return   A new copy of this TimeZone object.
     * @stable
     */
    virtual TimeZone* clone(void) const = 0;

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .       Derived::getStaticClassID()) ...
     * </pre>
     * @return The class ID for all objects of this class.
     * @stable
     */
    static UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }

    /**
     * Returns a unique class ID POLYMORPHICALLY. Pure virtual method. This method is to
     * implement a simple version of RTTI, since not all C++ compilers support genuine
     * RTTI. Polymorphic operator==() and clone() methods call this method.
     * <P>
     * Concrete subclasses of TimeZone must implement getDynamicClassID() and also a
     * static method and data member:
     * <pre>
     * .     static UClassID getStaticClassID() { return (UClassID)&fgClassID; }
     * .     static char fgClassID;
     * </pre>
     * @return   The class ID for this object. All objects of a given class have the
     *           same class ID. Objects of other classes have different class IDs.
     * @stable
     */
    virtual UClassID getDynamicClassID(void) const = 0;

protected:

    /**
     * Default constructor.  ID is initialized to the empty string.
     * @stable
     */
    TimeZone();

    /**
     * Copy constructor.
     * @stable
     */
    TimeZone(const TimeZone& source);

    /**
     * Default assignment operator.
     * @stable
     */
    TimeZone& operator=(const TimeZone& right);

private:
    static char fgClassID;

    static TimeZone*        createCustomTimeZone(const UnicodeString&); // Creates a time zone based on the string.

    static TimeZone*        fgDefaultZone; // default time zone (lazy evaluated)

    static const UnicodeString      GMT_ID;
    static const int32_t            GMT_ID_LENGTH;
    static const UnicodeString      CUSTOM_ID;

    ////////////////////////////////////////////////////////////////
    // Pointers into memory-mapped icudata.  Writing to this memory
    // will segfault!  See tzdat.h for more details.
    ////////////////////////////////////////////////////////////////

    /**
     * DATA is the start of the memory-mapped zone data, and
     * specifically points to the header object located there.
     * May be zero if loading failed for some reason.
     */
    static const TZHeader *    DATA;

    /**
     * INDEX_BY_ID is an index table in lexicographic order of ID.
     * Each entry is an offset from DATA to the zone object, which
     * will either be a StandardZone or a DSTZone object.
     */
    static const uint32_t*     INDEX_BY_ID;

    /**
     * INDEX_BY_OFFSET is an OffsetIndex table.  This table can only
     * be walked through sequentially because the entries are of
     * variable size.
     */
    static const OffsetIndex*  INDEX_BY_OFFSET;

    ////////////////////////////////////////////////////////////////
    // Other system zone data structures
    ////////////////////////////////////////////////////////////////
    /**
     * ZONE_IDS is an array of all the system zone ID strings, in
     * lexicographic order.  The createAvailableIDs() methods return
     * arrays of pointers into this array.
     */
    static UnicodeString*      ZONE_IDS;

    /**
     * If DATA_LOADED is true, then an attempt has already been made
     * to load the system zone data, and further attempts will not be
     * made.  If DATA_LOADED is true, DATA itself will be zero if
     * loading failed, or non-zero if it succeeded.
     */
    static bool_t              DATA_LOADED;

    /**
     * The mutex object used to control write access to DATA,
     * INDEX_BY_ID, INDEX_BY_OFFSET, and ZONE_IDS.  Also used to
     * control read/write access to fgDefaultZone.
     */
    static UMTX                LOCK;    

    /**
     * Responsible for setting up fgDefaultZone.  Uses routines in TPlatformUtilities
     * (i.e., platform-specific calls) to get the current system time zone.  Failing
     * that, uses the platform-specific default time zone.  Failing that, uses GMT.
     */
    static void             initDefault(void);

    // See source file for documentation
    static void   loadZoneData(void);

    // See source file for documentation
    static bool_t U_CALLCONV isDataAcceptable(void *context,
                                   const char *type, const char *name,
                                   const UDataInfo *pInfo);

    // See source file for documentation
    static TimeZone* createSystemTimeZone(const UnicodeString& name);

    UnicodeString           fID;    // this time zone's ID
};


// -------------------------------------

inline UnicodeString&
TimeZone::getID(UnicodeString& ID) const
{
    ID = fID;
    return ID;
}

// -------------------------------------

inline void
TimeZone::setID(const UnicodeString& ID)
{
    fID = ID;
}

#endif //_TIMEZONE
//eof
