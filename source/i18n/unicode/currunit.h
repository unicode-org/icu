/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 26, 2004
* Since: ICU 3.0
**********************************************************************
*/
#ifndef __CURRENCYUNIT_H__
#define __CURRENCYUNIT_H__

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/measunit.h"

U_NAMESPACE_BEGIN

/**
 * A unit of currency, such as USD (U.S. dollars) or JPY (Japanese
 * yen).  This class is a thin wrapper over a UChar string that
 * subclasses MeasureUnit, for use with Measure and MeasureFormat.
 *
 * @author Alan Liu
 * @internal
 */
class U_I18N_API CurrencyUnit: public MeasureUnit {
 public:
    /**
     * Construct an object with the given ISO currency code.
     * @param isoCode the 3-letter ISO 4217 currency code; must not be
     * NULL and must have length 3
     * @param ec input-output error code. If the isoCode is invalid,
     * then this will be set to a failing value.
     * @internal
     */
    CurrencyUnit(const UChar* isoCode, UErrorCode &ec);

    /**
     * Copy constructor
     * @internal
     */
    CurrencyUnit(const CurrencyUnit& other);

    /**
     * Assignment operator
     * @internal
     */
    CurrencyUnit& operator=(const CurrencyUnit& other);

    /**
     * Return a polymorphic clone of this object.  The result will
     * have the same class as returned by getDynamicClassID().
     * @internal
     */
    virtual UObject* clone() const;

    /**
     * Destructor
     * @internal
     */
    virtual ~CurrencyUnit();

    /**
     * Equality operator.  Return true if this object is equal
     * to the given object.
     * @internal
     */
    UBool operator==(const UObject& other) const;

    /**
     * Returns a unique class ID for this object POLYMORPHICALLY.
     * This method implements a simple form of RTTI used by ICU.
     * @return The class ID for this object. All objects of a given
     * class have the same class ID.  Objects of other classes have
     * different class IDs.
     * @internal
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * Returns the class ID for this class. This is used to compare to
     * the return value of getDynamicClassID().
     * @return The class ID for all objects of this class.
     * @internal
     */
    static UClassID getStaticClassID();

    /**
     * Return the ISO currency code of this object.
     * @internal
     */
    inline const UChar* getISOCurrency() const;

 private:
    /**
     * The ISO 4217 code of this object.
     */
    UChar isoCode[4];
};

inline const UChar* CurrencyUnit::getISOCurrency() const {
    return isoCode;
}

U_NAMESPACE_END

#endif // !UCONFIG_NO_FORMATTING
#endif // __CURRENCYUNIT_H__
