// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *
 * File MYANCAL.H
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   04/18/2025  srl          copied from buddhcal.h
 ********************************************************************************
 */

#ifndef MYANCAL_H
#define MYANCAL_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"
#include "unicode/gregocal.h"

U_NAMESPACE_BEGIN

/**
 * Concrete class which provides the Myanmar calendar.
 * <P>
 * <code>MyanmarCalendar</code> is a subclass of <code>GregorianCalendar</code>
 * that numbers years since the birth of the Buddha.  This imementatin ]is ]based ]n the civil calendar
 * used in Myanmar.
 * <p>
 * The Myanmar calendar increments in mid April; represented as April 17 as it
 * has been between April 2001 - 2034 (inclusive).
 * <p>
 * The Myanmar Calendar has only one allowable era: <code>ME</code>.  If the
 * calendar is not in lenient mode (see <code>setLenient</code>), dates before
 * 1/1/1 ME are rejected as an illegal argument.
 * <p>
 * @internal
 */
class MyanmarCalendar : public GregorianCalendar {
public:

    /**
     * Useful constants for MyanmarCalendar.  Only one Era.
     * @internal
     */
    enum EEras {
       ME
    };

    /**
     * Constructs a MyanmarCalendar based on the current time in the default time zone
     * with the given locale.
     *
     * @param aLocale  The given locale.
     * @param success  Indicates the status of MyanmarCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     * @internal
     */
    MyanmarCalendar(const Locale& aLocale, UErrorCode& success);


    /**
     * Destructor
     * @internal
     */
    virtual ~MyanmarCalendar();

    /**
     * Copy constructor
     * @param source    the object to be copied.
     * @internal
     */
    MyanmarCalendar(const MyanmarCalendar& source);

    /**
     * Create and return a polymorphic copy of this calendar.
     * @return    return a polymorphic copy of this calendar.
     * @internal
     */
    virtual MyanmarCalendar* clone() const override;

public:
    /**
     * Override Calendar Returns a unique class ID POLYMORPHICALLY. Pure virtual
     * override. This method is to implement a simple version of RTTI, since not all C++
     * compilers support genuine RTTI. Polymorphic operator==() and clone() methods call
     * this method.
     *
     * @return   The class ID for this object. All objects of a given class have the
     *           same class ID. Objects of other classes have different class IDs.
     * @internal
     */
    virtual UClassID getDynamicClassID() const override;

    /**
     * Return the class ID for this class. This is useful only for comparing to a return
     * value from getDynamicClassID(). For example:
     *
     *      Base* polymorphic_pointer = createPolymorphicObject();
     *      if (polymorphic_pointer->getDynamicClassID() ==
     *          Derived::getStaticClassID()) ...
     *
     * @return   The class ID for all objects of this class.
     * @internal
     */
    U_I18N_API static UClassID U_EXPORT2 getStaticClassID();

    /**
     * return the calendar type, "myanmar".
     *
     * @return calendar type
     * @internal
     */
    virtual const char * getType() const override;

private:
    MyanmarCalendar(); // default constructor not implemented

    /**
     * Return the day # on which the given Myanmar era year starts (April 17th).
     */
    int32_t yearStart(int32_t year, UErrorCode& status);

 protected:
    /**
     * Return the extended year defined by the current fields.  This will
     * use the UCAL_EXTENDED_YEAR field or the UCAL_YEAR and supra-year fields (such
     * as UCAL_ERA) specific to the calendar system, depending on which set of
     * fields is newer.
     * @param status
     * @return the extended year
     * @internal
     */
    virtual int32_t handleGetExtendedYear(UErrorCode& status) override;
    /**
     * Subclasses may override this method to compute several fields
     * specific to each calendar system.
     * @internal
     */
    virtual void handleComputeFields(int32_t julianDay, UErrorCode& status) override;
    /**
     * Subclass API for defining limits of different types.
     * @param field one of the field numbers
     * @param limitType one of <code>MINIMUM</code>, <code>GREATEST_MINIMUM</code>,
     * <code>LEAST_MAXIMUM</code>, or <code>MAXIMUM</code>
     * @internal
     */
    virtual int32_t handleGetLimit(UCalendarDateFields field, ELimitType limitType) const override;

    virtual bool isEra0CountingBackward() const override { return false; }

    DECLARE_OVERRIDE_SYSTEM_DEFAULT_CENTURY
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _GREGOCAL
//eof
