/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File FMTABLE.CPP
*
* Modification History:
*
*   Date        Name        Description
*   03/25/97    clhuang     Initial Implementation.
********************************************************************************
*/
#ifndef _FMTABLE
#include "unicode/fmtable.h"
#endif

#ifndef _CMEMORY
#include "cmemory.h"
#endif

// *****************************************************************************
// class Formattable
// *****************************************************************************
 
// -------------------------------------
// default constructor.
// Creates a formattable object with a long value 0.
 
Formattable::Formattable()
    :   fType(kLong)
{
    fValue.fLong = 0;
}

// -------------------------------------
// Creates a formattable object with a Date instance.

Formattable::Formattable(UDate date, ISDATE isDate)
    :   fType(kDate)
{
    fValue.fDate = date;
}

// -------------------------------------
// Creates a formattable object with a double value.

Formattable::Formattable(double value)
    :   fType(kDouble)
{
    fValue.fDouble = value;
}
 
// -------------------------------------
// Creates a formattable object with a long value.

Formattable::Formattable(int32_t value)
    :   fType(kLong)
{
    fValue.fLong = value;
}
 
// -------------------------------------
// Creates a formattable object with a char* string.
 
Formattable::Formattable(const char* stringToCopy)
    :   fType(kString)
{
    fValue.fString = new UnicodeString(stringToCopy);
}
 
// -------------------------------------
// Creates a formattable object with a UnicodeString instance.
 
Formattable::Formattable(const UnicodeString& stringToCopy)
    :   fType(kString)
{
    fValue.fString = new UnicodeString(stringToCopy);
}
 
// -------------------------------------
// Creates a formattable object with a UnicodeString* value.
// (adopting symantics)

Formattable::Formattable(UnicodeString* stringToAdopt)
    :   fType(kString)
{
    fValue.fString = stringToAdopt;
}
 
// -------------------------------------

Formattable::Formattable(const Formattable* arrayToCopy, int32_t count)
    :   fType(kArray)
{
    fValue.fArrayAndCount.fArray = createArrayCopy(arrayToCopy, count);
    fValue.fArrayAndCount.fCount = count;
}

// -------------------------------------
// copy constructor

Formattable::Formattable(const Formattable &source)
:   fType(kLong)
{
    *this = source;
}

// -------------------------------------
// assignment operator

Formattable&
Formattable::operator=(const Formattable& source)
{
    if (this != &source)
    {
        // Disposes the current formattable value/setting.
        dispose();

        // Sets the correct data type for this value.
        fType = source.fType;
        switch (fType)
        {
        case kArray:
            // Sets each element in the array one by one and records the array count.
            fValue.fArrayAndCount.fCount = source.fValue.fArrayAndCount.fCount;
            fValue.fArrayAndCount.fArray = createArrayCopy(source.fValue.fArrayAndCount.fArray,
                                                           source.fValue.fArrayAndCount.fCount);
            break;
        case kString:
            // Sets the string value.
            fValue.fString = new UnicodeString(*source.fValue.fString);
            break;
        case kDouble:
            // Sets the double value.
            fValue.fDouble = source.fValue.fDouble;
            break;
        case kLong:
            // Sets the long value.
            fValue.fLong = source.fValue.fLong;
            break;
        case kDate:
            // Sets the Date value.
            fValue.fDate = source.fValue.fDate;
            break;
        }
    }
    return *this;
}

// -------------------------------------

bool_t
Formattable::operator==(const Formattable& that) const
{
    // Checks class ID.
    if (this == &that) return TRUE;

    // Returns FALSE if the data types are different.
    if (fType != that.fType) return FALSE;

    // Compares the actual data values.
    switch (fType) {
    case kDate:
        return fValue.fDate == that.fValue.fDate;
    case kDouble:
        return fValue.fDouble == that.fValue.fDouble;
    case kLong:
        return fValue.fLong == that.fValue.fLong;
    case kString:
        return *(fValue.fString) == *(that.fValue.fString);
    case kArray:
        if (fValue.fArrayAndCount.fCount != that.fValue.fArrayAndCount.fCount)
            return FALSE;
        // Checks each element for equality.
        for (int32_t i=0; i<fValue.fArrayAndCount.fCount; ++i)
            if (fValue.fArrayAndCount.fArray[i] != that.fValue.fArrayAndCount.fArray[i])
                return FALSE;
        break;
    }
    return TRUE;
}
 
// -------------------------------------

Formattable::~Formattable()
{
    dispose();
}

// -------------------------------------

void Formattable::dispose()
{
    // Deletes the data value if necessary.
    switch (fType) {
    case kString:
        delete fValue.fString;
        break;
    case kArray:
        delete[] fValue.fArrayAndCount.fArray;
        break;
    }
}

// -------------------------------------
// Gets the data type of this Formattable object. 
Formattable::Type
Formattable::getType() const
{
    return fType;
}
 
// -------------------------------------
// Sets the value to a double value d.
 
void
Formattable::setDouble(double d)
{
    dispose();
    fType = kDouble;
    fValue.fDouble = d;
}
 
// -------------------------------------
// Sets the value to a long value l.
 
void
Formattable::setLong(int32_t l)
{
    dispose();
    fType = kLong;
    fValue.fLong = l;
}
 
// -------------------------------------
// Sets the value to a Date instance d.
 
void
Formattable::setDate(UDate d)
{
    dispose();
    fType = kDate;
    fValue.fDate = d;
}
 
// -------------------------------------
// Sets the value to a string value stringToCopy.
 
void
Formattable::setString(const UnicodeString& stringToCopy)
{
    dispose();
    fType = kString;
    fValue.fString = new UnicodeString(stringToCopy);
}
 
// -------------------------------------
// Sets the value to an array of Formattable objects.

void
Formattable::setArray(const Formattable* array, int32_t count)
{
    dispose();
    fType = kArray;
    fValue.fArrayAndCount.fArray = createArrayCopy(array, count);
    fValue.fArrayAndCount.fCount = count;
} 

// -------------------------------------
// Adopts the stringToAdopt value.
 
void
Formattable::adoptString(UnicodeString* stringToAdopt)
{
    dispose();
    fType = kString;
    fValue.fString = stringToAdopt;
}
 
// -------------------------------------
// Adopts the array value and its count.
 
void
Formattable::adoptArray(Formattable* array, int32_t count)
{
    dispose();
    fType = kArray;
    fValue.fArrayAndCount.fArray = array;
    fValue.fArrayAndCount.fCount = count;
} 

//----------------------------------------------------
// console I/O
//----------------------------------------------------
#ifdef _DEBUG

#include <iostream.h>

#include "unicode/datefmt.h"
#include "unistrm.h"

class FormattableStreamer
{
public:
    static void streamOut(ostream& stream, const Formattable& obj);
};

// This is for debugging purposes only.  This will send a displayable
// form of the Formattable object to the output stream.

void
FormattableStreamer::streamOut(ostream& stream, const Formattable& obj)
{
    static DateFormat *defDateFormat = 0;

    UnicodeString buffer;
    switch(obj.getType()) {
        case Formattable::kDate : 
            // Creates a DateFormat instance for formatting the
            // Date instance.
            if (defDateFormat == 0) {
                defDateFormat = DateFormat::createInstance();
            }
            defDateFormat->format(obj.getDate(), buffer);
            stream << buffer;
            break;
        case Formattable::kDouble :
            // Output the double as is.
            stream << obj.getDouble() << 'D';
            break;
        case Formattable::kLong :
            // Output the double as is.
            stream << obj.getLong() << 'L';
            break;
        case Formattable::kString:
            // Output the double as is.  Please see UnicodeString console
            // I/O routine for more details.
            stream << '"' << obj.getString(buffer) << '"';
            break;
        case Formattable::kArray:
            int32_t i, count;
            const Formattable* array;
            array = obj.getArray(count);
            stream << '[';
            // Recursively calling the console I/O routine for each element in the array.
            for (i=0; i<count; ++i) {
                FormattableStreamer::streamOut(stream, array[i]);
                stream << ( (i==(count-1)) ? "" : ", " );
            }
            stream << ']';
            break;
        default:
            // Not a recognizable Formattable object.
            stream << "INVALID_Formattable";
    }
    stream.flush();
}
#endif

//eof
