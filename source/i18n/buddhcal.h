/*
* Copyright (C) 2003, International Business Machines Corporation and others. All Rights Reserved.
********************************************************************************
*
* File BUDDHCAL.H
*
* Modification History:
*
*   Date        Name        Description
*   05/13/2003  srl          copied from gregocal.h
********************************************************************************
*/

#ifndef BUDDHCAL_H
#define BUDDHCAL_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"
#include "unicode/gregocal.h"

U_NAMESPACE_BEGIN

/**
 * Concrete class which provides the Buddhist calendar.
 * <P>
 * <code>BuddhistCalendar</code> is a subclass of <code>GregorianCalendar</code>
 * that numbers years since the birth of the Buddha.  This is the civil calendar
 * in some predominantly Buddhist countries such as Thailand, and it is used for
 * religious purposes elsewhere.
 * <p>
 * The Buddhist calendar is identical to the Gregorian calendar in all respects
 * except for the year and era.  Years are numbered since the birth of the
 * Buddha in 543 BC (Gregorian), so that 1 AD (Gregorian) is equivalent to 544
 * BE (Buddhist Era) and 1998 AD is 2541 BE.
 * <p>
 * The Buddhist Calendar has only one allowable era: <code>BE</code>.  If the
 * calendar is not in lenient mode (see <code>setLenient</code>), dates before
 * 1/1/1 BE are rejected as an illegal argument.
 * <p>
 * @internal
 */
class U_I18N_API BuddhistCalendar : public GregorianCalendar {
public:

    /**
     * Useful constants for BuddhistCalendar.  Only one Era.
     * @internal
     */
    enum EEras {
       BE
    };

    /**
     * Constructs a BuddhistCalendar based on the current time in the default time zone
     * with the given locale.
     *
     * @param aLocale  The given locale.
     * @param success  Indicates the status of BuddhistCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     * @stable ICU 2.0
     */
    BuddhistCalendar(const Locale& aLocale, UErrorCode& success);


    /**
     * Destructor
     * @internal
     */
    virtual ~BuddhistCalendar();

    /**
     * Copy constructor
     * @param source    the object to be copied.
     * @internal
     */
    BuddhistCalendar(const BuddhistCalendar& source);

    /**
     * Default assignment operator
     * @param right    the object to be copied.
     * @internal
     */
    BuddhistCalendar& operator=(const BuddhistCalendar& right);

    /**
     * Create and return a polymorphic copy of this calendar.
     * @return    return a polymorphic copy of this calendar.
     * @internal
     */
    virtual Calendar* clone(void) const;

    

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
    virtual UClassID getDynamicClassID(void) const;

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
    static inline UClassID getStaticClassID(void);

    /**
     * return the calendar type, "buddhist".
     *
     * @return calendar type
     * @draft ICU 2.6
     */
    virtual const char * getType() const;

private:
    BuddhistCalendar(); // default constructor not implemented

    static const char fgClassID;

 protected:
    virtual int32_t monthLength(int32_t month) const; 
    virtual int32_t monthLength(int32_t month, int32_t year) const; 
    int32_t getGregorianYear(UErrorCode& status);
    int32_t getMaximum(UCalendarDateFields field) const;
    int32_t getLeastMaximum(UCalendarDateFields field) const;
    virtual int32_t internalGetEra() const;
    virtual void timeToFields(UDate theTime, UBool quick, UErrorCode& status);
};

inline UClassID
BuddhistCalendar::getStaticClassID(void)
{ return (UClassID)&fgClassID; }

inline UClassID
BuddhistCalendar::getDynamicClassID(void) const
{ return BuddhistCalendar::getStaticClassID(); }


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _GREGOCAL
//eof

