/*
********************************************************************************
*   Copyright (C) 2005-2006, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINCOLL.H
*
********************************************************************************
*/

#ifndef __WINCOLL
#define __WINCOLL

#include "unicode/utypes.h"

#ifdef U_WINDOWS

#if !UCONFIG_NO_COLLATION

#include "unicode/coll.h"
#include "unicode/ustring.h"
#include "unicode/locid.h"

/**
 * \file 
 * \brief C++ API: Collation using Windows API.
 */

U_NAMESPACE_BEGIN

class Win32Collator : public Collator
{
public:
    Win32Collator(const Locale &locale, UErrorCode &status);

    Win32Collator(const Locale &locale, ECollationStrength strength, UErrorCode &status);

    Win32Collator(const Win32Collator &other);

    virtual ~Win32Collator();

    virtual Collator *clone(void) const;

    Win32Collator &operator=(const Win32Collator &other);

    /**
     * The comparison function compares the character data stored in two
     * different strings. Returns information about whether a string is less
     * than, greater than or equal to another string.
     * @param source the source string to be compared with.
     * @param target the string that is to be compared with the source string.
     * @param status possible error code
     * @return Returns an enum value. UCOL_GREATER if source is greater
     * than target; UCOL_EQUAL if source is equal to target; UCOL_LESS if source is less
     * than target
     * @draft ICU 3.6
     */
    virtual UCollationResult compare(const UnicodeString& source,
                                      const UnicodeString& target,
                                      UErrorCode &status) const;

    /**
     * Does the same thing as compare but limits the comparison to a specified
     * length
     * @param source the source string to be compared with.
     * @param target the string that is to be compared with the source string.
     * @param length the length the comparison is limited to
     * @param status possible error code
     * @return Returns an enum value. UCOL_GREATER if source (up to the specified
     *         length) is greater than target; UCOL_EQUAL if source (up to specified
     *         length) is equal to target; UCOL_LESS if source (up to the specified
     *         length) is less  than target.
     * @draft ICU 3.6
     */
    virtual UCollationResult compare(const UnicodeString& source,
                                      const UnicodeString& target,
                                      int32_t length,
                                      UErrorCode &status) const;

    /**
     * The comparison function compares the character data stored in two
     * different string arrays. Returns information about whether a string array
     * is less than, greater than or equal to another string array.
     * @param source the source string array to be compared with.
     * @param sourceLength the length of the source string array.  If this value
     *        is equal to -1, the string array is null-terminated.
     * @param target the string that is to be compared with the source string.
     * @param targetLength the length of the target string array.  If this value
     *        is equal to -1, the string array is null-terminated.
     * @param status possible error code
     * @return Returns an enum value. UCOL_GREATER if source is greater
     * than target; UCOL_EQUAL if source is equal to target; UCOL_LESS if source is less
     * than target
     * @draft ICU 3.6
     */
    virtual UCollationResult compare(const UChar* source, int32_t sourceLength,
                                      const UChar* target, int32_t targetLength,
                                      UErrorCode &status) const;

    /**
     * Transforms the string into a series of characters that can be compared
     * with CollationKey::compareTo. It is not possible to restore the original
     * string from the chars in the sort key.  The generated sort key handles
     * only a limited number of ignorable characters.
     * <p>Use CollationKey::equals or CollationKey::compare to compare the
     * generated sort keys.
     * If the source string is null, a null collation key will be returned.
     * @param source the source string to be transformed into a sort key.
     * @param key the collation key to be filled in
     * @param status the error code status.
     * @return the collation key of the string based on the collation rules.
     * @see CollationKey#compare
     * @deprecated ICU 2.8 Use getSortKey(...) instead
     */
    virtual CollationKey& getCollationKey(const UnicodeString&  source,
                                          CollationKey& key,
                                          UErrorCode& status) const;

    /**
     * Transforms the string into a series of characters that can be compared
     * with CollationKey::compareTo. It is not possible to restore the original
     * string from the chars in the sort key.  The generated sort key handles
     * only a limited number of ignorable characters.
     * <p>Use CollationKey::equals or CollationKey::compare to compare the
     * generated sort keys.
     * <p>If the source string is null, a null collation key will be returned.
     * @param source the source string to be transformed into a sort key.
     * @param sourceLength length of the collation key
     * @param key the collation key to be filled in
     * @param status the error code status.
     * @return the collation key of the string based on the collation rules.
     * @see CollationKey#compare
     * @deprecated ICU 2.8 Use getSortKey(...) instead
     */
    virtual CollationKey& getCollationKey(const UChar*source,
                                          int32_t sourceLength,
                                          CollationKey& key,
                                          UErrorCode& status) const;
    /**
     * Generates the hash code for the collation object
     * @draft ICU 3.6
     */
    virtual int32_t hashCode(void) const;

    /**
     * Gets the locale of the Collator
     *
     * @param type can be either requested, valid or actual locale. For more
     *             information see the definition of ULocDataLocaleType in
     *             uloc.h
     * @param status the error code status.
     * @return locale where the collation data lives. If the collator
     *         was instantiated from rules, locale is empty.
     * @deprecated ICU 2.8 This API is under consideration for revision
     * in ICU 3.0.
     */
    virtual const Locale getLocale(ULocDataLocaleType type, UErrorCode& status) const;

    /**
     * Determines the minimum strength that will be use in comparison or
     * transformation.
     * <p>E.g. with strength == SECONDARY, the tertiary difference is ignored
     * <p>E.g. with strength == PRIMARY, the secondary and tertiary difference
     * are ignored.
     * @return the current comparison level.
     * @see Collator#setStrength
     * @deprecated ICU 2.6 Use getAttribute(UCOL_STRENGTH...) instead
     */
    virtual ECollationStrength getStrength(void) const;

    /**
     * Sets the minimum strength to be used in comparison or transformation.
     * <p>Example of use:
     * <pre>
     *  \code
     *  UErrorCode status = U_ZERO_ERROR;
     *  Collator*myCollation = Collator::createInstance(Locale::US, status);
     *  if (U_FAILURE(status)) return;
     *  myCollation->setStrength(Collator::PRIMARY);
     *  // result will be "abc" == "ABC"
     *  // tertiary differences will be ignored
     *  Collator::ComparisonResult result = myCollation->compare("abc", "ABC");
     * \endcode
     * </pre>
     * @see Collator#getStrength
     * @param newStrength the new comparison level.
     * @deprecated ICU 2.6 Use setAttribute(UCOL_STRENGTH...) instead
     */
    virtual void setStrength(ECollationStrength newStrength);

    /**
     * Gets the version information for a Collator.
     * @param info the version # information, the result will be filled in
     * @draft ICU 3.6
     */
    virtual void getVersion(UVersionInfo info) const;

    /**
     * Universal attribute setter
     * @param attr attribute type
     * @param value attribute value
     * @param status to indicate whether the operation went on smoothly or
     *        there were errors
     * @draft ICU 3.6
     */
    virtual void setAttribute(UColAttribute attr, UColAttributeValue value,
                              UErrorCode &status);

    /**
     * Universal attribute getter
     * @param attr attribute type
     * @param status to indicate whether the operation went on smoothly or
     *        there were errors
     * @return attribute value
     * @draft ICU 3.6
     */
    virtual UColAttributeValue getAttribute(UColAttribute attr,
                                            UErrorCode &status);

    /**
     * Sets the variable top to a collation element value of a string supplied.
     * @param varTop one or more (if contraction) UChars to which the variable top should be set
     * @param len length of variable top string. If -1 it is considered to be zero terminated.
     * @param status error code. If error code is set, the return value is undefined. Errors set by this function are: <br>
     *    U_CE_NOT_FOUND_ERROR if more than one character was passed and there is no such a contraction<br>
     *    U_PRIMARY_TOO_LONG_ERROR if the primary for the variable top has more than two bytes
     * @return a 32 bit value containing the value of the variable top in upper 16 bits. Lower 16 bits are undefined
     * @draft ICU 3.6
     */
    virtual uint32_t setVariableTop(const UChar *varTop, int32_t len, UErrorCode &status);

    /**
     * Sets the variable top to a collation element value of a string supplied.
     * @param varTop an UnicodeString size 1 or more (if contraction) of UChars to which the variable top should be set
     * @param status error code. If error code is set, the return value is undefined. Errors set by this function are: <br>
     *    U_CE_NOT_FOUND_ERROR if more than one character was passed and there is no such a contraction<br>
     *    U_PRIMARY_TOO_LONG_ERROR if the primary for the variable top has more than two bytes
     * @return a 32 bit value containing the value of the variable top in upper 16 bits. Lower 16 bits are undefined
     * @draft ICU 3.6
     */
    virtual uint32_t setVariableTop(const UnicodeString varTop, UErrorCode &status);

    /**
     * Sets the variable top to a collation element value supplied. Variable top is set to the upper 16 bits.
     * Lower 16 bits are ignored.
     * @param varTop CE value, as returned by setVariableTop or ucol)getVariableTop
     * @param status error code (not changed by function)
     * @draft ICU 3.6
     */
    virtual void setVariableTop(const uint32_t varTop, UErrorCode &status);

    /**
     * Gets the variable top value of a Collator.
     * Lower 16 bits are undefined and should be ignored.
     * @param status error code (not changed by function). If error code is set, the return value is undefined.
     * @draft ICU 3.6
     */
    virtual uint32_t getVariableTop(UErrorCode &status) const;

    /**
     * Thread safe cloning operation
     * @return pointer to the new clone, user should remove it.
     * @draft ICU 3.6
     */
    virtual Collator* safeClone(void);

    /**
     * Get the sort key as an array of bytes from an UnicodeString.
     * Sort key byte arrays are zero-terminated and can be compared using
     * strcmp().
     * @param source string to be processed.
     * @param result buffer to store result in. If NULL, number of bytes needed
     *        will be returned.
     * @param resultLength length of the result buffer. If if not enough the
     *        buffer will be filled to capacity.
     * @return Number of bytes needed for storing the sort key
     * @draft ICU 3.6
     */
    virtual int32_t getSortKey(const UnicodeString& source,
                              uint8_t* result,
                              int32_t resultLength) const;

    /**
     * Get the sort key as an array of bytes from an UChar buffer.
     * Sort key byte arrays are zero-terminated and can be compared using
     * strcmp().
     * @param source string to be processed.
     * @param sourceLength length of string to be processed.
     *        If -1, the string is 0 terminated and length will be decided by the
     *        function.
     * @param result buffer to store result in. If NULL, number of bytes needed
     *        will be returned.
     * @param resultLength length of the result buffer. If if not enough the
     *        buffer will be filled to capacity.
     * @return Number of bytes needed for storing the sort key
     * @draft ICU 3.6
     */
    virtual int32_t getSortKey(const UChar*source, int32_t sourceLength,
                               uint8_t*result, int32_t resultLength) const;

    /**
     * Return the class ID for this class. This is useful only for comparing to
     * a return value from getDynamicClassID(). For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .       erived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @draft ICU 3.6
     */
    static UClassID U_EXPORT2 getStaticClassID(void);

    /**
     * Returns a unique class ID POLYMORPHICALLY. Pure virtual override. This
     * method is to implement a simple version of RTTI, since not all C++
     * compilers support genuine RTTI. Polymorphic operator==() and clone()
     * methods call this method.
     *
     * @return          The class ID for this object. All objects of a
     *                  given class have the same class ID.  Objects of
     *                  other classes have different class IDs.
     * @draft ICU 3.6
     */
    virtual UClassID getDynamicClassID(void) const;

private:
   /**
    * Converts C's UCollationStrength to ECollationStrength
    * @param strength member of the enum UCollationStrength
    * @return ECollationStrength equivalent of UCollationStrength
    */
    Collator::ECollationStrength getECollationStrength(UCollationStrength strength) const;

   /**
    * Converts C++'s ECollationStrength to UCollationStrength
    * @param strength member of the enum ECollationStrength
    * @return UCollationStrength equivalent of ECollationStrength
    */
    UCollationStrength getUCollationStrength(Collator::ECollationStrength strength) const;


    int32_t fLCID;
    ECollationStrength fStrength;

};

inline Collator::ECollationStrength Win32Collator::getECollationStrength(UCollationStrength strength) const
{
    switch (strength)
    {
    case UCOL_PRIMARY :
        return Collator::PRIMARY;

    case UCOL_SECONDARY :
        return Collator::SECONDARY;

    case UCOL_TERTIARY :
        return Collator::TERTIARY;

    case UCOL_QUATERNARY :
        return Collator::QUATERNARY;

    default :
        return Collator::IDENTICAL;
    }
}

inline UCollationStrength Win32Collator::getUCollationStrength(Collator::ECollationStrength strength) const
{
    switch (strength)
    {
    case Collator::PRIMARY :
        return UCOL_PRIMARY;

    case Collator::SECONDARY :
        return UCOL_SECONDARY;

    case Collator::TERTIARY :
        return UCOL_TERTIARY;

    case Collator::QUATERNARY :
        return UCOL_QUATERNARY;

    default :
        return UCOL_IDENTICAL;
    }
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_COLLATION */

#endif // #ifdef U_WINDOWS

#endif // __WINCOLL
