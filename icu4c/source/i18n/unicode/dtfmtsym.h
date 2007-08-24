/*  
********************************************************************************
*   Copyright (C) 1997-2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File DTFMTSYM.H
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*    07/21/98    stephen        Added getZoneIndex()
*                            Changed to match C++ conventions
********************************************************************************
*/
     
#ifndef DTFMTSYM_H
#define DTFMTSYM_H
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"
#include "unicode/uobject.h"
#include "unicode/locid.h"
#include "unicode/ures.h"

/**
 * \file 
 * \brief C++ API: Symbols for formatting dates.
 */

U_NAMESPACE_BEGIN

/* forward declaration */
class SimpleDateFormat;
class Hashtable;

/**
 * DateFormatSymbols is a public class for encapsulating localizable date-time
 * formatting data -- including timezone data. DateFormatSymbols is used by
 * DateFormat and SimpleDateFormat.
 * <P>
 * Rather than first creating a DateFormatSymbols to get a date-time formatter
 * by using a SimpleDateFormat constructor, clients are encouraged to create a
 * date-time formatter using the getTimeInstance(), getDateInstance(), or
 * getDateTimeInstance() method in DateFormat. Each of these methods can return a
 * date/time formatter initialized with a default format pattern along with the
 * date-time formatting data for a given or default locale. After a formatter is
 * created, clients may modify the format pattern using the setPattern function
 * as so desired. For more information on using these formatter factory
 * functions, see DateFormat.
 * <P>
 * If clients decide to create a date-time formatter with a particular format
 * pattern and locale, they can do so with new SimpleDateFormat(aPattern,
 * new DateFormatSymbols(aLocale)).  This will load the appropriate date-time
 * formatting data from the locale.
 * <P>
 * DateFormatSymbols objects are clonable. When clients obtain a
 * DateFormatSymbols object, they can feel free to modify the date-time
 * formatting data as necessary. For instance, clients can
 * replace the localized date-time format pattern characters with the ones that
 * they feel easy to remember. Or they can change the representative cities
 * originally picked by default to using their favorite ones.
 * <P>
 * DateFormatSymbols are not expected to be subclassed. Data for a calendar is
 * loaded out of resource bundles.  The 'type' parameter indicates the type of
 * calendar, for example, "gregorian" or "japanese".  If the type is not gregorian
 * (or NULL, or an empty string) then the type is appended to the resource name,
 * for example,  'Eras_japanese' instead of 'Eras'.   If the resource 'Eras_japanese' did
 * not exist (even in root), then this class will fall back to just 'Eras', that is,
 * Gregorian data.  Therefore, the calendar implementor MUST ensure that the root
 * locale at least contains any resources that are to be particularized for the
 * calendar type.
 */
class U_I18N_API DateFormatSymbols : public UObject {
public:
    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default locale, in the default calendar (Gregorian).
     * <P>
     * NOTE: This constructor will never fail; if it cannot get resource
     * data for the default locale, it will return a last-resort object
     * based on hard-coded strings.
     *
     * @param status    Status code.  Failure
     *                  results if the resources for the default cannot be
     *                  found or cannot be loaded
     * @stable ICU 2.0
     */
    DateFormatSymbols(UErrorCode& status);

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given locale, in the default calendar (Gregorian).
     *
     * @param locale    Locale to load format data from.
     * @param status    Status code.  Failure
     *                  results if the resources for the locale cannot be
     *                  found or cannot be loaded
     * @stable ICU 2.0
     */
    DateFormatSymbols(const Locale& locale,
                      UErrorCode& status);

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default locale, in the default calendar (Gregorian).
     * <P>
     * NOTE: This constructor will never fail; if it cannot get resource
     * data for the default locale, it will return a last-resort object
     * based on hard-coded strings.
     *
     * @param type      Type of calendar (as returned by Calendar::getType). 
     *                  Will be used to access the correct set of strings.
     *                  (NULL or empty string defaults to "gregorian".)
     * @param status    Status code.  Failure
     *                  results if the resources for the default cannot be
     *                  found or cannot be loaded
     * @internal
     */
    DateFormatSymbols(const char *type, UErrorCode& status);

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given locale, in the default calendar (Gregorian).
     *
     * @param locale    Locale to load format data from.
     * @param type      Type of calendar (as returned by Calendar::getType). 
     *                  Will be used to access the correct set of strings.
     *                  (NULL or empty string defaults to "gregorian".)
     * @param status    Status code.  Failure
     *                  results if the resources for the locale cannot be
     *                  found or cannot be loaded
     * @internal
     */
    DateFormatSymbols(const Locale& locale,
                      const char *type,
                      UErrorCode& status);

    /**
     * Copy constructor.
     * @stable ICU 2.0
     */
    DateFormatSymbols(const DateFormatSymbols&);

    /**
     * Assignment operator.
     * @stable ICU 2.0
     */
    DateFormatSymbols& operator=(const DateFormatSymbols&);

    /**
     * Destructor. This is nonvirtual because this class is not designed to be
     * subclassed.
     * @stable ICU 2.0
     */
    virtual ~DateFormatSymbols();

    /**
     * Return true if another object is semantically equal to this one.
     *
     * @param other    the DateFormatSymbols object to be compared with.
     * @return         true if other is semantically equal to this.
     * @stable ICU 2.0
     */
    UBool operator==(const DateFormatSymbols& other) const;

    /**
     * Return true if another object is semantically unequal to this one.
     *
     * @param other    the DateFormatSymbols object to be compared with.
     * @return         true if other is semantically unequal to this.
     * @stable ICU 2.0
     */
    UBool operator!=(const DateFormatSymbols& other) const { return !operator==(other); }

    /**
     * Gets era strings. For example: "AD" and "BC".
     *
     * @param count    Filled in with length of the array.
     * @return         the era strings.
     * @stable ICU 2.0
     */
    const UnicodeString* getEras(int32_t& count) const;

    /**
     * Sets era strings. For example: "AD" and "BC".
     * @param eras  Array of era strings (DateFormatSymbols retains ownership.)
     * @param count Filled in with length of the array.
     * @stable ICU 2.0
     */
    void setEras(const UnicodeString* eras, int32_t count);

    /**
     * Gets era name strings. For example: "Anno Domini" and "Before Christ".
     *
     * @param count    Filled in with length of the array.
     * @return         the era name strings.
     * @stable ICU 3.4
     */
    const UnicodeString* getEraNames(int32_t& count) const;

    /**
     * Sets era name strings. For example: "Anno Domini" and "Before Christ".
     * @param eraNames  Array of era name strings (DateFormatSymbols retains ownership.)
     * @param count Filled in with length of the array.
     * @stable ICU 3.6
     */
    void setEraNames(const UnicodeString* eraNames, int32_t count);

    /**
     * Gets month strings. For example: "January", "February", etc.
     * @param count Filled in with length of the array.
     * @return the month strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 2.0
     */
    const UnicodeString* getMonths(int32_t& count) const;

    /**
     * Sets month strings. For example: "January", "February", etc.
     *
     * @param months    the new month strings. (not adopted; caller retains ownership)
     * @param count     Filled in with length of the array.
     * @stable ICU 2.0
     */
    void setMonths(const UnicodeString* months, int32_t count);

    /**
     * Gets short month strings. For example: "Jan", "Feb", etc.
     *
     * @param count Filled in with length of the array.
     * @return the short month strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 2.0
     */
    const UnicodeString* getShortMonths(int32_t& count) const;

    /**
     * Sets short month strings. For example: "Jan", "Feb", etc.
     * @param count        Filled in with length of the array.
     * @param shortMonths  the new short month strings. (not adopted; caller retains ownership)
     * @stable ICU 2.0
     */
    void setShortMonths(const UnicodeString* shortMonths, int32_t count);

    /**
     * Selector for date formatting context
     * @stable ICU 3.6
     */
    enum DtContextType {
         FORMAT,
         STANDALONE,
         DT_CONTEXT_COUNT
    };

    /**
     * Selector for date formatting width
     * @stable ICU 3.6
     */
    enum DtWidthType {
         ABBREVIATED,
         WIDE,
         NARROW,
         DT_WIDTH_COUNT
    };

    /**
     * Gets month strings by width and context. For example: "January", "February", etc.
     * @param count Filled in with length of the array.
     * @param context The formatting context, either FORMAT or STANDALONE
     * @param width   The width of returned strings, either WIDE, ABBREVIATED, or NARROW.
     * @return the month strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 3.4
     */
    const UnicodeString* getMonths(int32_t& count, DtContextType context, DtWidthType width) const;

    /**
     * Sets month strings by width and context. For example: "January", "February", etc.
     *
     * @param months  The new month strings. (not adopted; caller retains ownership)
     * @param count   Filled in with length of the array.
     * @param context The formatting context, either FORMAT or STANDALONE
     * @param width   The width of returned strings, either WIDE, ABBREVIATED, or NARROW.
     * @stable ICU 3.6
     */
    void setMonths(const UnicodeString* months, int32_t count, DtContextType context, DtWidthType width);

    /**
     * Gets weekday strings. For example: "Sunday", "Monday", etc.
     * @param count        Filled in with length of the array.
     * @return the weekday strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 2.0
     */
    const UnicodeString* getWeekdays(int32_t& count) const;


    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param weekdays     the new weekday strings. (not adopted; caller retains ownership)
     * @param count        Filled in with length of the array.
     * @stable ICU 2.0
     */
    void setWeekdays(const UnicodeString* weekdays, int32_t count);

    /**
     * Gets short weekday strings. For example: "Sun", "Mon", etc.
     * @param count        Filled in with length of the array.
     * @return             the short weekday strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 2.0
     */
    const UnicodeString* getShortWeekdays(int32_t& count) const;

    /**
     * Sets short weekday strings. For example: "Sun", "Mon", etc.
     * @param shortWeekdays  the new short weekday strings. (not adopted; caller retains ownership)
     * @param count          Filled in with length of the array.
     * @stable ICU 2.0
     */
    void setShortWeekdays(const UnicodeString* shortWeekdays, int32_t count);

    /**
     * Gets weekday strings by width and context. For example: "Sunday", "Monday", etc.
     * @param count   Filled in with length of the array.
     * @param context The formatting context, either FORMAT or STANDALONE
     * @param width   The width of returned strings, either WIDE, ABBREVIATED, or NARROW
     * @return the month strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 3.4
     */
    const UnicodeString* getWeekdays(int32_t& count, DtContextType context, DtWidthType width) const;

    /**
     * Sets weekday strings by width and context. For example: "Sunday", "Monday", etc.
     * @param weekdays  The new weekday strings. (not adopted; caller retains ownership)
     * @param count     Filled in with length of the array.
     * @param context   The formatting context, either FORMAT or STANDALONE
     * @param width     The width of returned strings, either WIDE, ABBREVIATED, or NARROW
     * @stable ICU 3.6
     */
    void setWeekdays(const UnicodeString* weekdays, int32_t count, DtContextType context, DtWidthType width);

    /**
     * Gets quarter strings by width and context. For example: "1st Quarter", "2nd Quarter", etc.
     * @param count Filled in with length of the array.
     * @param context The formatting context, either FORMAT or STANDALONE
     * @param width   The width of returned strings, either WIDE or ABBREVIATED. There
     *                are no NARROW quarters.
     * @return the quarter strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 3.6
     */
    const UnicodeString* getQuarters(int32_t& count, DtContextType context, DtWidthType width) const;

    /**
     * Sets quarter strings by width and context. For example: "1st Quarter", "2nd Quarter", etc.
     *
     * @param quarters  The new quarter strings. (not adopted; caller retains ownership)
     * @param count   Filled in with length of the array.
     * @param context The formatting context, either FORMAT or STANDALONE
     * @param width   The width of returned strings, either WIDE or ABBREVIATED. There
     *                are no NARROW quarters.
     * @stable ICU 3.6
     */
    void setQuarters(const UnicodeString* quarters, int32_t count, DtContextType context, DtWidthType width);

    /**
     * Gets AM/PM strings. For example: "AM" and "PM".
     * @param count        Filled in with length of the array.
     * @return             the weekday strings. (DateFormatSymbols retains ownership.)
     * @stable ICU 2.0
     */
    const UnicodeString* getAmPmStrings(int32_t& count) const;

    /**
     * Sets ampm strings. For example: "AM" and "PM".
     * @param ampms        the new ampm strings. (not adopted; caller retains ownership)
     * @param count        Filled in with length of the array.
     * @stable ICU 2.0
     */
    void setAmPmStrings(const UnicodeString* ampms, int32_t count);

    /**
     * Gets timezone strings. These strings are stored in a 2-dimensional array.
     * @param rowCount      Output param to receive number of rows.
     * @param columnCount   Output param to receive number of columns.
     * @return              The timezone strings as a 2-d array. (DateFormatSymbols retains ownership.)
     * @deprecated ICU 3.6
     */
    const UnicodeString** getZoneStrings(int32_t& rowCount, int32_t& columnCount) const;

    /**
     * Sets timezone strings. These strings are stored in a 2-dimensional array.
     * @param strings       The timezone strings as a 2-d array to be copied. (not adopted; caller retains ownership)
     * @param rowCount      The number of rows (count of first index).
     * @param columnCount   The number of columns (count of second index).
     * @stable ICU 2.0
     */
    void setZoneStrings(const UnicodeString* const* strings, int32_t rowCount, int32_t columnCount);

    /**
     * Get the non-localized date-time pattern characters.
     * @return    the non-localized date-time pattern characters
     * @stable ICU 2.0
     */
    static const UChar * U_EXPORT2 getPatternUChars(void);

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
     * <p>
     * Note: ICU no longer provides localized date-time pattern characters for a locale
     * starting ICU 3.8.  This method returns the non-localized date-time pattern
     * characters unless user defined localized data is set by setLocalPatternChars.
     * @param result    Output param which will receive the localized date-time pattern characters.
     * @return          A reference to 'result'.
     * @stable ICU 2.0
     */
    UnicodeString& getLocalPatternChars(UnicodeString& result) const;

    /**
     * Sets localized date-time pattern characters. For example: 'u', 't', etc.
     * @param newLocalPatternChars the new localized date-time
     * pattern characters.
     * @stable ICU 2.0
     */
    void setLocalPatternChars(const UnicodeString& newLocalPatternChars);

    /**
     * Returns the locale for this object. Two flavors are available:
     * valid and actual locale.
     * @stable ICU 2.8
     */
    Locale getLocale(ULocDataLocaleType type, UErrorCode& status) const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 2.2
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 2.2
     */
    static UClassID U_EXPORT2 getStaticClassID();

    /**
     * The translation type of the translated zone strings
     * @internal ICU 3.6
     */
    enum TimeZoneTranslationType {
        TIMEZONE_SHORT_GENERIC,
        TIMEZONE_SHORT_STANDARD,
        TIMEZONE_SHORT_DAYLIGHT,
        TIMEZONE_LONG_GENERIC,
        TIMEZONE_LONG_STANDARD,
        TIMEZONE_LONG_DAYLIGHT,
        TIMEZONE_EXEMPLAR_CITY,
        TIMEZONE_COUNT
    };
    
    /**
     * Creates an enumeration of time zone IDs. The object is owned by the caller and should delete it after use.
     * The time zone IDs are just for programmatic lookup. NOT LOCALIZED!!!
     * @param status   Input/output parameter, set to success or
     *                 failure code upon return.
     * @return A new StringEnumeration object
     * @internal ICU 3.6
     */
    virtual StringEnumeration* createZoneStringIDs(UErrorCode &status);

    /**
     * Gets timezone string give the key and translation type
     * @param ID       The ID of zone strings,  e.g: "America/Los_Angeles".
     *                 The time zone ID is  for programmatic lookup.
     * @param type     The translation type requested
     * @param result   Output parameter to recieve the translation string
     * @param status   Input/output parameter, set to success or
     *                 failure code upon return.
     * @return the input UnicodeString parameter for chaining
     * @internal ICU 3.8
     */
    UnicodeString& getZoneString(const UnicodeString &ID, const TimeZoneTranslationType type, UnicodeString &result, UErrorCode &status);

    /**
     * Gets metazone string given the key and translation type and calendar
     * @param ID       The ID of zone strings,  e.g: "America/Los_Angeles".
     *                 The time zone ID is  for programmatic lookup.
     * @param type     The translation type requested
     * @param cal      The calendar
     * @param result   Output parameter to recieve the translation string
     * @param status   Input/output parameter, set to success or
     *                 failure code upon return.
     * @return the input UnicodeString parameter for chaining
     * @internal ICU 3.8
     */
    UnicodeString getMetazoneString(const UnicodeString &ID, const TimeZoneTranslationType type, Calendar &cal, UnicodeString &result, UErrorCode &status);

    /**
     * Gets fallback string given the key
     * @param ID       The ID of zone strings,  e.g: "America/Los_Angeles".
     *                 The time zone ID is  for programmatic lookup.
     * @param result   Output parameter to recieve the translation string
     * @param status   Input/output parameter, set to success or
     *                 failure code upon return.
     * @return the input UnicodeString parameter for chaining
     * @internal ICU 3.8
     */
    UnicodeString& getFallbackString(const UnicodeString &ID, UnicodeString &result, UErrorCode &status);

    /**
     * Sets timezone string for the given the ID and translation type
     * @param ID       The ID of zone strings,  e.g: "America/Los_Angeles".
     *                 The time zone ID is for programmatic lookup.
     * @param type     The translation type to set the value for
     * @param value    The string with which current translation needs to be replaced
     * @param status   Input/output parameter, set to success or     
     * @internal ICU 3.6
     */

    /**
     * Determines if the Commonly Used flag is set for this zone 
     * @param zid      The ID of zone strings,  e.g: "America/Los_Angeles".
     *                 The time zone ID is  for programmatic lookup.
     * @return         A boolean value indicating if the zone is commonlyUsed or not.
     * @internal ICU 3.8
     */
    UBool isCommonlyUsed(const UnicodeString &zid);
    
    /**
     * Sets timezone string for the given the ID and translation type
     * @param ID       The ID of zone strings,  e.g: "America/Los_Angeles".
     *                 The time zone ID is for programmatic lookup.
     * @param type     The translation type to set the value for
     * @param value    The string with which current translation needs to be replaced
     * @param status   Input/output parameter, set to success or     
     * @internal ICU 3.6
     */
    void setZoneString(const UnicodeString &ID, const TimeZoneTranslationType type, const UnicodeString &value, UErrorCode &status);

private:

    friend class SimpleDateFormat;
    friend class DateFormatSymbolsSingleSetter; // see udat.cpp

    /**
     * Era strings. For example: "AD" and "BC".
     */
    UnicodeString*  fEras;
    int32_t         fErasCount;

    /**
     * Era name strings. For example: "Anno Domini" and "Before Christ".
     */
    UnicodeString*  fEraNames;
    int32_t         fEraNamesCount;

    /**
     * Month strings. For example: "January", "February", etc.
     */
    UnicodeString*  fMonths;
    int32_t         fMonthsCount;

    /**
     * Short month strings. For example: "Jan", "Feb", etc.
     */
    UnicodeString*  fShortMonths;
    int32_t         fShortMonthsCount;

    /**
     * Narrow month strings. For example: "J", "F", etc.
     */
    UnicodeString*  fNarrowMonths;
    int32_t         fNarrowMonthsCount;

    /**
     * Standalone Month strings. For example: "January", "February", etc.
     */
    UnicodeString*  fStandaloneMonths;
    int32_t         fStandaloneMonthsCount;

    /**
     * Standalone Short month strings. For example: "Jan", "Feb", etc.
     */
    UnicodeString*  fStandaloneShortMonths;
    int32_t         fStandaloneShortMonthsCount;

    /**
     * Standalone Narrow month strings. For example: "J", "F", etc.
     */
    UnicodeString*  fStandaloneNarrowMonths;
    int32_t         fStandaloneNarrowMonthsCount;

    /**
     * Weekday strings. For example: "Sunday", "Monday", etc.
     */
    UnicodeString*  fWeekdays;
    int32_t         fWeekdaysCount;

    /**
     * Short weekday strings. For example: "Sun", "Mon", etc.
     */
    UnicodeString*  fShortWeekdays;
    int32_t         fShortWeekdaysCount;

    /**
     * Narrow weekday strings. For example: "Sun", "Mon", etc.
     */
    UnicodeString*  fNarrowWeekdays;
    int32_t         fNarrowWeekdaysCount;

    /**
     * Standalone Weekday strings. For example: "Sunday", "Monday", etc.
     */
    UnicodeString*  fStandaloneWeekdays;
    int32_t         fStandaloneWeekdaysCount;

    /**
     * Standalone Short weekday strings. For example: "Sun", "Mon", etc.
     */
    UnicodeString*  fStandaloneShortWeekdays;
    int32_t         fStandaloneShortWeekdaysCount;

    /**
     * Standalone Narrow weekday strings. For example: "Sun", "Mon", etc.
     */
    UnicodeString*  fStandaloneNarrowWeekdays;
    int32_t         fStandaloneNarrowWeekdaysCount;

    /**
     * Ampm strings. For example: "AM" and "PM".
     */
    UnicodeString*  fAmPms;
    int32_t         fAmPmsCount;

    /**
     * Quarter strings. For example: "1st quarter", "2nd quarter", etc.
     */
    UnicodeString  *fQuarters;
    int32_t         fQuartersCount;

    /**
     * Short quarters. For example: "Q1", "Q2", etc.
     */
    UnicodeString  *fShortQuarters;
    int32_t         fShortQuartersCount;

    /**
     * Standalone quarter strings. For example: "1st quarter", "2nd quarter", etc.
     */
    UnicodeString  *fStandaloneQuarters;
    int32_t         fStandaloneQuartersCount;

    /**
     * Standalone short quarter strings. For example: "Q1", "Q2", etc.
     */
    UnicodeString  *fStandaloneShortQuarters;
    int32_t         fStandaloneShortQuartersCount;

    /**
     * The format data of all the timezones in this locale.
     */
    UnicodeString** fZoneStrings;
    int32_t         fZoneStringsRowCount;
    int32_t         fZoneStringsColCount;
    StringEnumeration* fZoneIDEnumeration;
    Hashtable*         fZoneStringsHash;
    UResourceBundle* fResourceBundle;
    const char*      fCountry;

    /**
     * Localized date-time pattern characters. For example: use 'u' as 'y'.
     */
    UnicodeString   fLocalPatternChars;

private:
    /** valid/actual locale information 
     *  these are always ICU locales, so the length should not be a problem
     */
    char validLocale[ULOC_FULLNAME_CAPACITY];
    char actualLocale[ULOC_FULLNAME_CAPACITY];

    DateFormatSymbols(); // default constructor not implemented

    /**
     * Called by the constructors to actually load data from the resources
     *
     * @param locale               The locale to get symbols for.
     * @param type                 Calendar Type (as from Calendar::getType())
     * @param status               Input/output parameter, set to success or
     *                             failure code upon return.
     * @param useLastResortData    determine if use last resort data
     */
    void initializeData(const Locale&, const char *type, UErrorCode& status, UBool useLastResortData = FALSE);

    /**
     * Copy or alias an array in another object, as appropriate.
     *
     * @param dstArray    the copy destination array.
     * @param dstCount    fill in with the lenth of 'dstArray'.
     * @param srcArray    the source array to be copied.
     * @param srcCount    the length of items to be copied from the 'srcArray'.
     */
    static void assignArray(UnicodeString*& dstArray,
                            int32_t& dstCount,
                            const UnicodeString* srcArray,
                            int32_t srcCount);

    /**
     * Return true if the given arrays' contents are equal, or if the arrays are
     * identical (pointers are equal).
     *
     * @param array1   one array to be compared with.
     * @param array2   another array to be compared with.
     * @param count    the length of items to be copied.
     * @return         true if the given arrays' contents are equal, or if the arrays are
     *                 identical (pointers are equal).
     */
    static UBool arrayCompare(const UnicodeString* array1,
                             const UnicodeString* array2,
                             int32_t count);

    /**
     * Create a copy, in fZoneStrings, of the given zone strings array. The
     * member variables fZoneStringsRowCount and fZoneStringsColCount should be
     * set already by the caller.
     */
    void createZoneStrings(const UnicodeString *const * otherStrings);

    /**
     * Package private: used by SimpleDateFormat
     * Gets the index for the given time zone ID to obtain the timezone
     * strings for formatting. The time zone ID is just for programmatic
     * lookup. NOT LOCALIZED!!!
     * @param ID the given time zone ID.
     * @return the index of the given time zone ID.  Returns -1 if
     * the given time zone ID can't be located in the DateFormatSymbols object.
     * @see java.util.SimpleTimeZone
     */
    int32_t getZoneIndex(const UnicodeString& ID) const;

    // Internal method; see source for documentation
    int32_t _getZoneIndex(const UnicodeString& id) const;

    /**
     * Delete all the storage owned by this object.
     */
    void dispose(void);

    /**
     * Copy all of the other's data to this.
     * @param other the object to be copied.
     */
    void copyData(const DateFormatSymbols& other);

    /**
     * Delete just the zone strings.
     */
    void disposeZoneStrings(void);

    /**
     * Initializes the zoneStrings hash and keys StringEnumeration after reading the zoneStrings resource
     */
    void initZoneStrings(UErrorCode &status);
    /** 
     * initialzes the zoneStrings has and keys enumeration after reading the strings[][]. Required for backwards
     * compatibility of setZoneStrings method
     */
    void initZoneStrings(const UnicodeString** strings, int32_t rowCount, int32_t collumnCount, UErrorCode& status);
    /**
     * initialization of the fZoneStrings data member
     */
    void initZoneStringsArray(UErrorCode& status);
    /**
     * Creates a deep clone of the Hashtable
     */
    Hashtable* createZoneStringsHash(const Hashtable* otherHash);
    
    /**
     * Fetches the key from the hashtable for a given ID.
     * e.g: for a given ID such as PST returns "Americal/Los_Angeles"
     * Used by SimpleDateFormat class.
     * @param ID The id of the time zone for which the key needs to be fetched
     * @param result Output parameter to recieve the key.
     * @return the input UnicodeString object for chaining
     */
    UnicodeString& getZoneID(const UnicodeString& zid, UnicodeString& result, UErrorCode& status);
    
    /**
     * Fetches the zone type and zone string from the hashtable for a given key.
     * e.g: for key: "Americal/Los_Angeles", text: "2004/1/1 PT 1:00" and start:9
     * returns TIMEZONE_SHORT_GENERIC and "PT". 
     * Used by SimpleDateFormat class.
     * @param ID the name of the timezone
     * @param text the string containing the time zone translation
     * @param start The position in string where time zone string starts
     * @param type  output parameter to recieve the type of time zone string
     * @param value output parameter to recieve the the acutal time zone string
     */
    void getZoneType(const UnicodeString& zid, const UnicodeString& text, int32_t start, TimeZoneTranslationType& type, UnicodeString& value, UErrorCode& status);
    
    /**
     * Fetches the zone type and zone string from the hashtable by cycling through all elements in the hashtable.
     * e.g: text: "2004/1/1 PT 1:00" and start:9
     * returns "Americal/Los_Angeles", TIMEZONE_SHORT_GENERIC and "PT". Used by SimpleDateFormat class.
     * Used by SimpleDateFormat class.
     * @param ID output parameter to recieve the key name of the time zone
     * @param text the string containing the time zone translation
     * @param start The position in string where time zone string starts
     * @param type  output parameter to recieve the type of time zone string
     * @param value output parameter to recieve the the acutal time zone string
     * @param status output parameter to recive the error information
     */
    void findZoneIDTypeValue(UnicodeString& zid, const UnicodeString& text, int32_t start, TimeZoneTranslationType& type, UnicodeString& value, UErrorCode& status);

    UnicodeString resolveParsedMetazone(const UnicodeString& zid);
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _DTFMTSYM
//eof
