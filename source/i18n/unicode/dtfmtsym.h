/*  
********************************************************************************
*   Copyright (C) 1997-2004, International Business Machines
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

#include "unicode/uobject.h"
#include "unicode/locid.h"
#include "unicode/resbund.h"

U_NAMESPACE_BEGIN

/* forward declaration */
class SimpleDateFormat;

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
    ~DateFormatSymbols();

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
     * @stable ICU 2.0
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
    static const UChar *getPatternUChars(void);

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
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
     * @draft ICU 2.8 likely to change in ICU 3.0, based on feedback
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
    static UClassID getStaticClassID();

private:

    friend class SimpleDateFormat;
    friend class DateFormatSymbolsSingleSetter; // see udat.cpp

    /**
     * Era strings. For example: "AD" and "BC".
     */
    UnicodeString*  fEras;
    int32_t         fErasCount;

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
     * Ampm strings. For example: "AM" and "PM".
     */
    UnicodeString*  fAmPms;
    int32_t         fAmPmsCount;

    /**
     * The format data of all the timezones in this locale.
     */
    UnicodeString** fZoneStrings;
    int32_t         fZoneStringsRowCount;
    int32_t         fZoneStringsColCount;

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


    /* Sizes for the last resort string arrays */
    typedef enum LastResortSize {
        kMonthNum = 13,
        kMonthLen = 3,

        kDayNum = 8,
        kDayLen = 2,

        kAmPmNum = 2,
        kAmPmLen = 3,

        kEraNum = 2,
        kEraLen = 3,

        kZoneNum = 5,
        kZoneLen = 4
    } LastResortSize;

    DateFormatSymbols(); // default constructor not implemented

    void initField(UnicodeString **field, int32_t& length, const ResourceBundle &data, UErrorCode &status);
    void initField(UnicodeString **field, int32_t& length, const UChar *data, LastResortSize numStr, LastResortSize strLen, UErrorCode &status);

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
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _DTFMTSYM
//eof
