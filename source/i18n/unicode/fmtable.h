/*
********************************************************************************
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File FMTABLE.H
*
* Modification History:
*
*   Date        Name        Description
*   02/29/97    aliu        Creation.
********************************************************************************
*/
#ifndef FMTABLE_H
#define FMTABLE_H


#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/uobject.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

/**
 * Formattable objects can be passed to the Format class or
 * its subclasses for formatting.  Formattable is a thin wrapper
 * class which interconverts between the primitive numeric types
 * (double, long, etc.) as well as UDate and UnicodeString.
 * <P>
 * Note that this is fundamentally different from the Java behavior, since
 * in this case the various formattable objects do not occupy a hierarchy,
 * but are all wrapped within this one class.  Formattable encapsulates all
 * the polymorphism in itself.
 * <P>
 * It would be easy to change this so that Formattable was an abstract base
 * class of a genuine hierarchy, and that would clean up the code that
 * currently must explicitly check for type, but that seems like overkill at
 * this point.
 */
class U_I18N_API Formattable : public UObject {
public:
    /**
     * This enum is only used to let callers distinguish between
     * the Formattable(UDate) constructor and the Formattable(double)
     * constructor; the compiler cannot distinguish the signatures,
     * since UDate is currently typedefed to be either double or long.
     * If UDate is changed later to be a bonafide class
     * or struct, then we no longer need this enum.
     * @draft ICU 2.4
     */
    enum ISDATE { kIsDate };

    /**
     * Default constructor
     * @draft ICU 2.4
     */
    Formattable(); // Type kLong, value 0
    /**
     * Creates a Formattable object with a UDate instance.
     * @param d the UDate instance.
     * @param flag the flag to indicate this is a date. Always set it to kIsDate
     * @stable ICU 2.0  
     */
    Formattable(UDate d, ISDATE flag);
    /**
     * Creates a Formattable object with a double number.
     * @param d the double number.
     * @stable ICU 2.0
     */
    Formattable(double d);
    /**
     * Creates a Formattable object with a long number.
     * @param l the long number.
     * @stable ICU 2.0
     */
    Formattable(int32_t l);
    /**
     * Creates a Formattable object with a char string pointer.
     * Assumes that the char string is null terminated.
     * @param strToCopy the char string.
     * @stable ICU 2.0
     */
    Formattable(const char* strToCopy);
    /**
     * Creates a Formattable object with a UnicodeString object to copy from.
     * @param strToCopy the UnicodeString string.
     * @stable ICU 2.0
     */
    Formattable(const UnicodeString& strToCopy);
    /**
     * Creates a Formattable object with a UnicodeString object to adopt from.
     * @param strToAdopt the UnicodeString string.
     * @stable ICU 2.0
     */
    Formattable(UnicodeString* strToAdopt);
    /**
     * Creates a Formattable object with an array of Formattable objects.
     * @param arrayToCopy the Formattable object array.
     * @param count the array count.
     * @stable ICU 2.0
     */
    Formattable(const Formattable* arrayToCopy, int32_t count);

    /**
     * Copy constructor.
     * @stable ICU 2.0
     */
    Formattable(const Formattable&);
    /**
     * Assignment operator.
     * @param rhs   The Formattable object to copy into this object.
     * @stable ICU 2.0
     */
    Formattable&    operator=(const Formattable &rhs);
    /**
     * Equality comparison.
     * @param other    the object to be compared with.
     * @return        TRUE if other are equal to this, FALSE otherwise.
     * @stable ICU 2.0
     */
    UBool          operator==(const Formattable &other) const;
    
    /** 
     * Equality operator.
     * @param other    the object to be compared with.
     * @return        TRUE if other are unequal to this, FALSE otherwise.
     * @stable ICU 2.0
     */
    UBool          operator!=(const Formattable& other) const
      { return !operator==(other); }

    /** 
     * Destructor.
     * @stable ICU 2.0
     */
    virtual         ~Formattable();

    /** 
     * The list of possible data types of this Formattable object.
     * @draft ICU 2.4
     */
    enum Type {
        kDate,      // Date
        kDouble,    // double
        kLong,      // long
        kString,    // UnicodeString
        kArray      // Formattable[]
    };

    /**
     * Gets the data type of this Formattable object.
     * @return    the data type of this Formattable object.
     * @stable ICU 2.0
     */
    Type            getType(void) const;
    
    /**
     * Gets the double value of this object.
     * @return    the double value of this object.
     * @stable ICU 2.0
     */ 
    double          getDouble(void) const { return fValue.fDouble; }
    /**
     * Gets the long value of this object.
     * @return    the long value of this object.
     * @stable ICU 2.0
     */ 
    int32_t            getLong(void) const { return fValue.fLong; }
    /**
     * Gets the Date value of this object.
     * @return    the Date value of this object.
     * @stable ICU 2.0
     */ 
    UDate            getDate(void) const { return fValue.fDate; }

    /**
     * Gets the string value of this object.
     * @param result    Output param to receive the Date value of this object.
     * @return          A reference to 'result'.
     * @stable ICU 2.0
     */ 
    UnicodeString&  getString(UnicodeString& result) const
      { result=*fValue.fString; return result; }

    /**
     * Gets a const reference to the string value of this object.
     * @return   a const reference to the string value of this object.
     * @stable ICU 2.0
     */
    inline const UnicodeString& getString(void) const;

    /**
     * Gets a reference to the string value of this object.
     * @return   a reference to the string value of this object.
     * @stable ICU 2.0
     */
    inline UnicodeString& getString(void);

    /**
     * Gets the array value and count of this object.
     * @param count    fill-in with the count of this object.
     * @return         the array value of this object.
     * @stable ICU 2.0
     */ 
    const Formattable* getArray(int32_t& count) const
      { count=fValue.fArrayAndCount.fCount; return fValue.fArrayAndCount.fArray; }

    /**
     * Accesses the specified element in the array value of this Formattable object.
     * @param index the specified index.
     * @return the accessed element in the array.
     * @stable ICU 2.0
     */
    Formattable&    operator[](int32_t index) { return fValue.fArrayAndCount.fArray[index]; }

    /**
     * Sets the double value of this object.
     * @param d    the new double value to be set.
     * @stable ICU 2.0
     */ 
    void            setDouble(double d);
    /**
     * Sets the long value of this object.
     * @param l    the new long value to be set.
     * @stable ICU 2.0
     */ 
    void            setLong(int32_t l);
    /**
     * Sets the Date value of this object.
     * @param d    the new Date value to be set.
     * @stable ICU 2.0
     */ 
    void            setDate(UDate d);
    /**
     * Sets the string value of this object.
     * @param stringToCopy    the new string value to be set.
     * @stable ICU 2.0
     */ 
    void            setString(const UnicodeString& stringToCopy);
    /**
     * Sets the array value and count of this object.
     * @param array    the array value.
     * @param count    the number of array elements to be copied.
     * @stable ICU 2.0
     */ 
    void            setArray(const Formattable* array, int32_t count);
    /**
     * Sets and adopts the string value and count of this object.
     * @param stringToAdopt    the new string value to be adopted.
     * @stable ICU 2.0
     */ 
    void            adoptString(UnicodeString* stringToAdopt);
    /**
     * Sets and adopts the array value and count of this object.
     * @stable ICU 2.0
     */ 
    void            adoptArray(Formattable* array, int32_t count);
        
    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID();

private:
    /**
     * Cleans up the memory for unwanted values.  For example, the adopted
     * string or array objects.
     */
    void            dispose(void);

    /**
     * Creates a new Formattable array and copies the values from the specified
     * original.
     * @param array the original array
     * @param count the original array count
     * @return the new Formattable array.
     */
    static Formattable* createArrayCopy(const Formattable* array, int32_t count);

    // Note: For now, we do not handle unsigned long and unsigned
    // double types.  Smaller unsigned types, such as unsigned
    // short, can fit within a long.
    union {
        UnicodeString*  fString;
        double          fDouble;
        int32_t            fLong;
        UDate            fDate;
        struct
        {
          Formattable*  fArray;
          int32_t          fCount;
        }               fArrayAndCount;
    }                   fValue;

    Type                fType;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

inline UClassID Formattable::getStaticClassID()
{ return (UClassID)&fgClassID; }
        
inline UClassID Formattable::getDynamicClassID() const
{ return Formattable::getStaticClassID(); }

inline Formattable*
Formattable::createArrayCopy(const Formattable* array, int32_t count)
{
    Formattable *result = new Formattable[count];
    for (int32_t i=0; i<count; ++i) result[i] = array[i]; // Don't memcpy!
    return result;
}

inline const UnicodeString& Formattable::getString(void) const {
    return *fValue.fString;
}

inline UnicodeString& Formattable::getString(void) {
    return *fValue.fString;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif //_FMTABLE
//eof
     
