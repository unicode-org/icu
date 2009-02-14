/*
*******************************************************************************
* Copyright (C) 2009, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*
*
* File NUMSYS.H
*
* Modification History:*
*   Date        Name        Description
*
********************************************************************************
*/

#ifndef NUMSYS
#define NUMSYS

#include "unicode/utypes.h"

/**
 * \file
 * \brief C++ API: NumberingSystems object
 */

#if !UCONFIG_NO_FORMATTING


#include "unicode/format.h"
#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

/**
 * Defines numbering systems. Put more description here.
 */

class U_I18N_API NumberingSystem : public UObject {
public:

    /**
     * Default Constructor.
     *
     * @draft ICU 4.2
     */
    NumberingSystem();

    /**
     * Copy constructor.
     * @draft ICU 4.2
     */
    NumberingSystem(const NumberingSystem& other);

    /**
     * Destructor.
     * @draft ICU 4.2
     */
    virtual ~NumberingSystem();

    /**
     * Create the default numbering system associated with the specified locale.
     * @param inLocale The given locale.
     * @draft ICU 4.2
     */
    static NumberingSystem* U_EXPORT2 createInstance(const Locale & inLocale, UErrorCode& status);

    /**
     * Create the default numbering system associated with the default locale.
     * @draft ICU 4.2
     */
    static NumberingSystem* U_EXPORT2 createInstance(UErrorCode& status);

    /**
     * Create a numbering system using the specified radix, type, and description. 
     * @param radix         The radix (base) for this numbering system.
     * @param isAlgorithmic TRUE if the numbering system is algorithmic rather than numeric.
     * @param description   The string representing the set of digits used in a numeric system, or the name of the RBNF
     *                      ruleset to be used in an algorithmic system.
     * @draft ICU 4.2
     */
    static NumberingSystem* U_EXPORT2 createInstance(int32_t radix, UBool isAlgorithmic, const UnicodeString& description, UErrorCode& status );

    /**
     * Return a StringEnumeration over all the names of numbering systems known to ICU.
     * @draft ICU 4.2
     */

     static StringEnumeration * U_EXPORT2 getAvailableNames(UErrorCode& status);

    /**
     * Create a numbering system from one of the predefined numbering systems known to ICU.
     * @param name   The name of the numbering system.
     * @draft ICU 4.2
     */
    static NumberingSystem* U_EXPORT2 createInstanceByName(const char* name, UErrorCode& status);


    /**
     * Returns the radix of this numbering system.
     * @draft ICU 4.2
     */
    int32_t getRadix();

    /**
     * Returns the description string of this numbering system, which is either
     * the string of digits in the case of simple systems, or the ruleset name
     * in the case of algorithmic systems.
     * @draft ICU 4.2
     */
    virtual UnicodeString getDescription();



    /**
     * Returns TRUE if the given numbering system is algorithmic
     *
     * @return         TRUE if the numbering system is algorithmic.
     *                 Otherwise, return FALSE.
     * @draft ICU 4.2
     */
    UBool isAlgorithmic() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 4.0
     *
    */
    static UClassID U_EXPORT2 getStaticClassID(void);

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 4.0
     */
    virtual UClassID getDynamicClassID() const;


private:
    UnicodeString   desc;
    int32_t         radix;
    UBool           algorithmic;

    void setRadix(int32_t radix);

    void setAlgorithmic(UBool algorithmic);

    void setDesc(UnicodeString desc);

    static UBool isValidDigitString(const UnicodeString &str);

    UBool hasContiguousDecimalDigits() const;
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _NUMSYS
//eof
