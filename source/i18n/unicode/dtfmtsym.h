/*  
********************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
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
#include "unicode/locid.h"

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
 * New DateFormatSymbols sub-classes may be added to support SimpleDateFormat
 * for date-time formatting for additional locales.
 */
class U_I18N_API DateFormatSymbols {
public:
    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default locale.
     * <P>
     * NOTE: This constructor will never fail; if it cannot get resource
     * data for the default locale, it will return a last-resort object
     * based on hard-coded strings.
     *
     * @param status    Output param set to success of failure.  Failure
     *                  results if the resources for the default cannot be
     *                  found or cannot be loaded
     */
    DateFormatSymbols(UErrorCode& status);

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given locale.
     *
     * @param locale    Locale to load format data from.
     * @param status    Output param set to success of failure.  Failure
     *                  results if the resources for the locale cannot be
     *                  found or cannot be loaded
     */
    DateFormatSymbols(const Locale& locale,
                      UErrorCode& status);

    /**
     * Copy constructor.
     */
    DateFormatSymbols(const DateFormatSymbols&);

    /**
     * Assignment operator.
     */
    DateFormatSymbols& operator=(const DateFormatSymbols&);

    /**
     * Destructor. This is nonvirtual because this class is not designed to be
     * subclassed.
     */
    ~DateFormatSymbols();

    /**
     * Return true if another object is semantically equal to this one.
     */
    bool_t operator==(const DateFormatSymbols& other) const;

    /**
     * Return true if another object is semantically unequal to this one.
     */
    bool_t operator!=(const DateFormatSymbols& other) const { return !operator==(other); }

    /**
     * Gets era strings. For example: "AD" and "BC".
     * @return the era strings.
     */
    const UnicodeString* getEras(int32_t& count) const;

    /**
     * Sets era strings. For example: "AD" and "BC".
     * @param eras  Array of era strings (DateFormatSymbols retains ownership.)
     * @param count Filled in with length of the array.
     */
    void setEras(const UnicodeString* eras, int32_t count);

    /**
     * Gets month strings. For example: "January", "February", etc.
     * @param count Filled in with length of the array.
     * @return the month strings. (DateFormatSymbols retains ownership.)
     */
    const UnicodeString* getMonths(int32_t& count) const;

    /**
     * Sets month strings. For example: "January", "February", etc.
     * @param newMonths the new month strings. (not adopted; caller retains ownership)
     */
    void setMonths(const UnicodeString* months, int32_t count);

    /**
     * Gets short month strings. For example: "Jan", "Feb", etc.
     * @return the short month strings. (DateFormatSymbols retains ownership.)
     */
    const UnicodeString* getShortMonths(int32_t& count) const;

    /**
     * Sets short month strings. For example: "Jan", "Feb", etc.
     * @param newShortMonths the new short month strings. (not adopted; caller retains ownership)
     */
    void setShortMonths(const UnicodeString* shortMonths, int32_t count);

    /**
     * Gets weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. (DateFormatSymbols retains ownership.)
     */
    const UnicodeString* getWeekdays(int32_t& count) const;

    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param newWeekdays the new weekday strings. (not adopted; caller retains ownership)
     */
    void setWeekdays(const UnicodeString* weekdays, int32_t count);

    /**
     * Gets short weekday strings. For example: "Sun", "Mon", etc.
     * @return the short weekday strings. (DateFormatSymbols retains ownership.)
     */
    const UnicodeString* getShortWeekdays(int32_t& count) const;

    /**
     * Sets short weekday strings. For example: "Sun", "Mon", etc.
     * @param newShortWeekdays the new short weekday strings. (not adopted; caller retains ownership)
     */
    void setShortWeekdays(const UnicodeString* shortWeekdays, int32_t count);

    /**
     * Gets AM/PM strings. For example: "AM" and "PM".
     * @return the weekday strings. (DateFormatSymbols retains ownership.)
     */
    const UnicodeString* getAmPmStrings(int32_t& count) const;

    /**
     * Sets ampm strings. For example: "AM" and "PM".
     * @param newAmpms the new ampm strings. (not adopted; caller retains ownership)
     */
    void setAmPmStrings(const UnicodeString* ampms, int32_t count);

    /**
     * Gets timezone strings. These strings are stored in a 2-dimensional array.
     * @param rowCount      Output param to receive number of rows.
     * @param columnCount   Output param to receive number of columns.
     * @return              The timezone strings as a 2-d array. (DateFormatSymbols retains ownership.)
     */
    const UnicodeString** getZoneStrings(int32_t& rowCount, int32_t& columnCount) const;

    /**
     * Sets timezone strings. These strings are stored in a 2-dimensional array.
     * @param strings       The timezone strings as a 2-d array to be copied. (not adopted; caller retains ownership)
     * @param rowCount      The number of rows (count of first index).
     * @param columnCount   The number of columns (count of second index).
     */
    void setZoneStrings(const UnicodeString* const* strings, int32_t rowCount, int32_t columnCount);

    /**
     * Get the non-localized date-time pattern characters.
     */
    static const UnicodeString& getPatternChars(void) { return fgPatternChars; }

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
     * @return the localized date-time pattern characters.
     */
    UnicodeString& getLocalPatternChars(UnicodeString& result) const;

    /**
     * Sets localized date-time pattern characters. For example: 'u', 't', etc.
     * @param newLocalPatternChars the new localized date-time
     * pattern characters.
     */
    void setLocalPatternChars(const UnicodeString& newLocalPatternChars);

private:
    friend class SimpleDateFormat;

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

    /**
     * Unlocalized date-time pattern characters. For example: 'y', 'd', etc. All
     * locales use the same these unlocalized pattern characters.
     */
    static UnicodeString fgPatternChars;

private:
    /**
     * Called by the constructors to actually load data from the resources
     */
    void initializeData(const Locale&, UErrorCode& status, bool_t useLastResortData = FALSE);

    /**
     * Copy or alias an array in another object, as appropriate.
     */
    void assignArray(UnicodeString*& dstArray,
                     int32_t& dstCount,
                     const UnicodeString* srcArray,
                     int32_t srcCount,
                     const DateFormatSymbols& other,
                     int32_t which);

    /**
     * Return true if the given arrays' contents are equal, or if the arrays are
     * identical (pointers are equal).
     */
    static bool_t arrayCompare(const UnicodeString* array1,
                             const UnicodeString* array2,
                             int32_t count);

    /**
     * Create a copy, in fZoneStrings, of the given zone strings array. The
     * member variables fZoneStringsRowCount and fZoneStringsColCount should be
     * set already by the caller. The fIsOwned flags are not checked or set by
     * this method; that is the caller's responsibility.
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

    /**
     * Delete all the storage owned by this object and reset the fIsOwned flag
     * to indicate that arrays have been deleted.
     */
    void dispose(void);

    /**
     * Delete just the zone strings, if they are owned by this object. This
     * method does NOT modify fIsOwned; the caller must handle that.
     */
    void disposeZoneStrings(void);

    /**
     * These are static arrays we use only in the case where we have no
     * resource data.
     */
    static const UnicodeString     fgLastResortMonthNames[];
    static const UnicodeString     fgLastResortDayNames[];
    static const UnicodeString     fgLastResortAmPmMarkers[];
    static const UnicodeString     fgLastResortEras[];
    static const UnicodeString     fgLastResortZoneStrings[];
    static UnicodeString**         fgLastResortZoneStringsH;

    /**
     * The member fIsOwned is a bit field with flags indicating which of the arrays
     * we own. This is necessary since the user may alter our symbols, but in
     * most cases, will not, so we do not want to copy these arrays unless
     * necessary.
     */
    enum
    {
        kEras,
        kMonths,
        kShortMonths,
        kWeekdays,
        kShortWeekdays,
        kAmPms,
        kZoneStrings
    };
    uint8_t                 fIsOwned;

    /**
     * Sets the fIsOwned flag for the specfied string array
     */
    void                    setIsOwned(int32_t which, bool_t isOwned);

    /**
     * Tests the fIsOwned flag for the specified string array
     */
    bool_t                  isOwned(int32_t which) const;
};

inline void
DateFormatSymbols::setIsOwned(int32_t which, bool_t isOwned)
{
    fIsOwned = ( fIsOwned & ~(1 << which) ) | ( (isOwned ? 1 : 0) << which );
}

inline bool_t
DateFormatSymbols::isOwned(int32_t which) const
{
    return ( (fIsOwned >> which) & 1 ) != 0;
}

#endif // _DTFMTSYM
//eof
