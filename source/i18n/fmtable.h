/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1999      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
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


#include "utypes.h"
#include "unistr.h"

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
class U_I18N_API Formattable {
public:
    /**
     * This enum is only used to let callers distinguish between
     * the Formattable(UDate) constructor and the Formattable(double)
     * constructor; the compiler cannot distinguish the signatures,
     * since UDate is currently typedefed to be either double or long.
     * If UDate is changed later to be a bonafide class
     * or struct, then we no longer need this enum.
     */
                    enum ISDATE { kIsDate };

                    Formattable(); // Type kLong, value 0
    /**
     * Creates a Formattable object with a UDate instance.
     * @param d the UDate instance.
     * @param ISDATE the flag to indicate this is a date.
     */
                    Formattable(UDate d, ISDATE);
    /**
     * Creates a Formattable object with a double number.
     * @param d the double number.
     */
                    Formattable(double d);
    /**
     * Creates a Formattable object with a long number.
     * @param d the long number.
     */
                    Formattable(int32_t l);
    /**
     * Creates a Formattable object with a char string pointer.
     * Assumes that the char string is null terminated.
     * @param strToCopy the char string.
     */
                    Formattable(const char* strToCopy);
    /**
     * Creates a Formattable object with a UnicodeString object to copy from.
     * @param strToCopy the UnicodeString string.
     */
                    Formattable(const UnicodeString& stringToCopy);
    /**
     * Creates a Formattable object with a UnicodeString object to adopt from.
     * @param strToAdopt the UnicodeString string.
     */
                    Formattable(UnicodeString* stringToAdopt);
    /**
     * Creates a Formattable object with an array of Formattable objects.
     * @param arrayToCopy the Formattable object array.
     * @param count the array count.
     */
                    Formattable(const Formattable* arrayToCopy, int32_t count);

    /**
     * Copy constructor.
     */
                    Formattable(const Formattable&);
    /**
     * Assignment operator.
     */
    Formattable&    operator=(const Formattable&);
    /**
     * Equality comparison.
     */
    bool_t          operator==(const Formattable&) const;
    bool_t          operator!=(const Formattable& other) const
      { return !operator==(other); }

    /** Destructor.
    */
    virtual         ~Formattable();

    /** 
     * The list of possible data types of this Formattable object.
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
     */
    Type            getType(void) const;
    
    /**
     * Gets the double value of this object.
     */ 
    double          getDouble(void) const { return fValue.fDouble; }
    /**
     * Gets the long value of this object.
     */ 
    int32_t            getLong(void) const { return fValue.fLong; }
    /**
     * Gets the Date value of this object.
     */ 
    UDate            getDate(void) const { return fValue.fDate; }

    /**
     * Gets the string value of this object.
     */ 
    UnicodeString&  getString(UnicodeString& result) const
      { result=*fValue.fString; return result; }

    /**
     * Gets the array value and count of this object.
     */ 
    const Formattable* getArray(int32_t& count) const
      { count=fValue.fArrayAndCount.fCount; return fValue.fArrayAndCount.fArray; }

    /**
     * Accesses the specified element in the array value of this Formattable object.
     * @param index the specified index.
     * @return the accessed element in the array.
     */
    Formattable&    operator[](int32_t index) { return fValue.fArrayAndCount.fArray[index]; }

    /**
     * Sets the double value of this object.
     */ 
    void            setDouble(double d);
    /**
     * Sets the long value of this object.
     */ 
    void            setLong(int32_t l);
    /**
     * Sets the Date value of this object.
     */ 
    void            setDate(UDate d);
    /**
     * Sets the string value of this object.
     */ 
    void            setString(const UnicodeString& stringToCopy);
    /**
     * Sets the array value and count of this object.
     */ 
    void            setArray(const Formattable* array, int32_t count);
    /**
     * Sets and adopts the string value and count of this object.
     */ 
    void            adoptString(UnicodeString* stringToAdopt);
    /**
     * Sets and adopts the array value and count of this object.
     */ 
    void            adoptArray(Formattable* array, int32_t count);
        
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
};

inline Formattable*
Formattable::createArrayCopy(const Formattable* array, int32_t count)
{
    Formattable *result = new Formattable[count];
    for (int32_t i=0; i<count; ++i) result[i] = array[i]; // Don't memcpy!
    return result;
}

#endif //_FMTABLE
//eof
     
