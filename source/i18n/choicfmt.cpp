/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File CHOICFMT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/20/97    helena      Finished first cut of implementation and got rid 
*                           of nextDouble/previousDouble and replaced with
*                           boolean array.
*   4/10/97     aliu        Clean up.  Modified to work on AIX.
*   06/04/97    helena      Fixed applyPattern(), toPattern() and not to include 
*                           wchar.h.
*   07/09/97    helena      Made ParsePosition into a class.
*   08/06/97    nos         removed overloaded constructor, fixed 'format(array)'
*    07/22/98    stephen        JDK 1.2 Sync - removed UBool array (doubleFlags)
*   02/22/99    stephen     Removed character literals for EBCDIC safety
********************************************************************************
*/

#include "cpputils.h"
#include "unicode/choicfmt.h"
#include "unicode/numfmt.h"
#include "unicode/locid.h"
#include "mutex.h" 

// *****************************************************************************
// class ChoiceFormat
// *****************************************************************************
 
char        ChoiceFormat::fgClassID = 0; // Value is irrelevant

NumberFormat* ChoiceFormat::fgNumberFormat = 0;

// -------------------------------------
// Creates a ChoiceFormat instance based on the pattern.
 
ChoiceFormat::ChoiceFormat(const UnicodeString& newPattern,
                           UErrorCode& status)
: fChoiceLimits(0),
  fChoiceFormats(0),
  fCount(0)
{
    applyPattern(newPattern, status);
}
 
// -------------------------------------
// Creates a ChoiceFormat instance with the limit array and 
// format strings for each limit.

ChoiceFormat::ChoiceFormat(const double* limits, 
                           const UnicodeString* formats, 
                           int32_t cnt )
: fChoiceLimits(0),
  fChoiceFormats(0),
  fCount(0)
{
    setChoices(limits, formats, cnt );
}

// -------------------------------------
// copy constructor

ChoiceFormat::ChoiceFormat(const    ChoiceFormat&   that) 
    : fChoiceLimits(0),
      fChoiceFormats(0)
{
    *this = that;
}

// -------------------------------------

UBool
ChoiceFormat::operator==(const Format& that) const
{
    if (this == &that) return TRUE;
    if (this->getDynamicClassID() != that.getDynamicClassID()) return FALSE;  // not the same class
    if (!NumberFormat::operator==(that)) return FALSE;
    ChoiceFormat& thatAlias = (ChoiceFormat&)that;
    if (fCount != thatAlias.fCount) return FALSE;
    // Checks the limits, the corresponding format string and LE or LT flags.
    // LE means less than and equal to, LT means less than.
    for (int32_t i = 0; i < fCount; i++) {
        if ((fChoiceLimits[i] != thatAlias.fChoiceLimits[i]) ||
            (fChoiceFormats[i] != thatAlias.fChoiceFormats[i]))
            return FALSE;
    }
    return TRUE;
}

// -------------------------------------
// copy constructor

const ChoiceFormat&
ChoiceFormat::operator=(const   ChoiceFormat& that)
{
    if (this != &that) {
        NumberFormat::operator=(that);
        fCount = that.fCount;
        delete [] fChoiceLimits; fChoiceLimits = 0;
        delete [] fChoiceFormats; fChoiceFormats = 0;
        fChoiceLimits = new double[fCount];
        fChoiceFormats = new UnicodeString[fCount];

        uprv_arrayCopy(that.fChoiceLimits, fChoiceLimits, fCount);
        uprv_arrayCopy(that.fChoiceFormats, fChoiceFormats, fCount);
    }
    return *this;
}

// -------------------------------------

ChoiceFormat::~ChoiceFormat()
{
    delete [] fChoiceLimits;
    fChoiceLimits = 0;
    delete [] fChoiceFormats;
    fChoiceFormats = 0;
    fCount = 0;
}

// -------------------------------------
// NumberFormat cache management

NumberFormat* 
ChoiceFormat::getNumberFormat(UErrorCode &status)
{
    NumberFormat *theFormat = 0;

    if (fgNumberFormat != 0) // if there's something in the cache
    {
        Mutex lock;

        if (fgNumberFormat != 0) // Someone might have grabbed it.
        {
            theFormat = fgNumberFormat;
            fgNumberFormat = 0; // We have exclusive right to this formatter.
        }
    }

    if(theFormat == 0) // If we weren't able to pull it out of the cache, then we have to create it.
    {
        theFormat = NumberFormat::createInstance(Locale::US, status);
        if(U_FAILURE(status))
            return 0;
        theFormat->setMinimumFractionDigits(1);
    }

    return theFormat;
}

void          
ChoiceFormat::releaseNumberFormat(NumberFormat *adopt)
{
    if(fgNumberFormat == 0) // If the cache is empty we must add it back.
    {
        Mutex lock;

        if(fgNumberFormat == 0)
        {
            fgNumberFormat = adopt;
            adopt = 0;
        }
    }

    delete adopt;
}

/**
 * Convert a string to a double value using a default NumberFormat object
 * which is static (shared by all ChoiceFormat instances).
 */
double
ChoiceFormat::stod(const UnicodeString& string,
                   UErrorCode& status)
{
    // Use a shared global number format to convert a double value to 
    // or string or vice versa.
    NumberFormat *myFormat = getNumberFormat(status);

    if(U_FAILURE(status))
        return -1; // OK?

    Formattable result;
    myFormat->parse(string, result, status);
    releaseNumberFormat(myFormat);
    double value = 0.0;
    if (U_SUCCESS(status))
    {
        switch(result.getType())
        {
            case Formattable::kLong: value = result.getLong(); break;
            case Formattable::kDouble: value = result.getDouble(); break;
        }
    }
    return value;
}

// -------------------------------------

/**
 * Convert a double value to a string using a default NumberFormat object
 * which is static (shared by all ChoiceFormat instances).
 */
UnicodeString&
ChoiceFormat::dtos(double value,
                   UnicodeString& string,
                   UErrorCode& status)
{
    NumberFormat *myFormat = getNumberFormat(status);

    if (U_SUCCESS(status)) {
        FieldPosition fieldPos(0);
        myFormat->format(value, string, fieldPos);
    }
    releaseNumberFormat(myFormat);
    return string;
}

// -------------------------------------
// Applies the pattern to this ChoiceFormat instance.
 
void
ChoiceFormat::applyPattern(const UnicodeString& newPattern,
                           UErrorCode& status)
{
    if (U_FAILURE(status))
        return;

    UnicodeString segments[2];
    double newChoiceLimits[30];  // current limit
    UnicodeString newChoiceFormats[30];   // later, use Vectors
    int32_t count = 0;
    int32_t part = 0;
    double startValue = 0;
    double oldStartValue = uprv_getNaN();
    UBool inQuote = FALSE;
    for(int i = 0; i < newPattern.length(); ++i) {
        UChar ch = newPattern[i];
        if(ch == 0x0027 /*'\''*/) {
            // Check for "''" indicating a literal quote
            if((i+1) < newPattern.length() && newPattern[i+1] == ch) {
                segments[part] += ch;
                ++i;
            }
            else 
                inQuote = !inQuote;
        }
        else if (inQuote) {
            segments[part] += ch;
        }
        else if (ch == 0x003C /*'<'*/ || ch == 0x0023 /*'#'*/ || ch == 0x2264) {
            if (segments[0] == "") {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }

            UnicodeString tempBuffer = segments[0];
            tempBuffer.trim();
            UChar posInf = 0x221E;
            UChar negInf [] = {0x002D /*'-'*/, posInf };
            if (tempBuffer == UnicodeString(&posInf, 1, 1)) {
                startValue = uprv_getInfinity();
            } 
            else if (tempBuffer == UnicodeString(negInf, 2, 2)) {
                startValue = - uprv_getInfinity();
            } 
            else {
                //segments[0].trim();
                startValue = stod(tempBuffer, status);
                if(U_FAILURE(status))
                    return;
            }

            if (ch == 0x003C /*'<'*/ && ! uprv_isInfinite(startValue)) {
                startValue = nextDouble(startValue);
            }
            // {sfb} There is a bug in MSVC 5.0 sp3 -- 0.0 <= NaN ==> TRUE
            //if (startValue <= oldStartValue) {
            if (startValue <= oldStartValue && ! uprv_isNaN(oldStartValue)) {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            segments[0].remove();
            part = 1;
        } else if (ch == 0x007C /*'|'*/) {
            newChoiceLimits[count] = startValue;
            newChoiceFormats[count] = segments[1];
            ++count;
            oldStartValue = startValue;
            segments[1].remove();
            part = 0;
        } else {
            segments[part] += ch;
        }
    }
    // clean up last one
    if (part == 1) {
        newChoiceLimits[count] = startValue;
        newChoiceFormats[count] = segments[1];
        ++count;
    }


    delete [] fChoiceLimits; fChoiceLimits = 0;
    delete [] fChoiceFormats; fChoiceFormats = 0;

    fCount = count;
    fChoiceLimits    = new double[fCount];
    fChoiceFormats    = new UnicodeString[fCount];
    
    uprv_arrayCopy(newChoiceLimits, fChoiceLimits, fCount);
    uprv_arrayCopy(newChoiceFormats, fChoiceFormats, fCount);
}
 
// -------------------------------------
// Reconstruct the original input pattern.
 
UnicodeString&
ChoiceFormat::toPattern(UnicodeString& result) const
{
    result.remove();
    for (int32_t i = 0; i < fCount; ++i) {
        if (i != 0) {
            result += (UChar)0x007C /*'|'*/;
        }
        // choose based upon which has less precision
        // approximate that by choosing the closest one to an integer.
        // could do better, but it's not worth it.
        double less = previousDouble(fChoiceLimits[i]);
        double tryLessOrEqual = uprv_fabs(uprv_IEEEremainder(fChoiceLimits[i], 1.0));
        double tryLess = uprv_fabs(uprv_IEEEremainder(less, 1.0));

        UErrorCode status = U_ZERO_ERROR;
        UnicodeString buf;
        // {sfb} hack to get this to work on MSVC - NaN doesn't behave as it should
        if (tryLessOrEqual < tryLess && 
            ! (uprv_isNaN(tryLessOrEqual) || uprv_isNaN(tryLess))) {
            result += dtos(fChoiceLimits[i], buf, status);
            result += (UChar)0x0023 /*'#'*/;
        } 
        else {
            if (uprv_isPositiveInfinity(fChoiceLimits[i])) {
                result += (UChar32)0x221E;
            } else if (uprv_isNegativeInfinity(fChoiceLimits[i])) {
                result += (UChar)0x002D /*'-'*/;
                result += (UChar32)0x221E;
            } else {
                result += dtos(less, buf, status);
            }
            result += (UChar)0x003C /*'<'*/;
        }
        // Append fChoiceFormats[i], using quotes if there are special characters.
        // Single quotes themselves must be escaped in either case.
        UnicodeString text = fChoiceFormats[i];
        UBool needQuote = text.indexOf((UChar)0x003C /*'<'*/) >= 0
            || text.indexOf((UChar)0x0023 /*'#'*/) >= 0
            || text.indexOf((UChar32)0x2264) >= 0
            || text.indexOf((UChar)0x007C /*'|'*/) >= 0;
        if (needQuote) 
            result += (UChar)0x0027 /*'\''*/;
        if (text.indexOf((UChar)0x0027 /*'\''*/) < 0) 
            result += text;
        else {
            for (int j = 0; j < text.length(); ++j) {
                UChar c = text[j];
                result += c;
                if (c == 0x0027 /*'\''*/) 
                    result += c;
            }
        }
        if (needQuote) 
            result += (UChar)0x0027 /*'\''*/;
    }
    
    return result;
}
 
// -------------------------------------
// Adopts the limit and format arrays.
 
void
ChoiceFormat::adoptChoices(double *limits, 
                           UnicodeString *formats, 
                           int32_t cnt )
{
    if(limits == 0 || formats == 0)
        return;
        
    delete [] fChoiceLimits;
    fChoiceLimits = 0;
    delete [] fChoiceFormats;
    fChoiceFormats = 0;
    fChoiceLimits = limits;
    fChoiceFormats = formats;
    fCount = cnt;
}
 
// -------------------------------------
// Sets the limit and format arrays. 
void
ChoiceFormat::setChoices(  const double* limits, 
                           const UnicodeString* formats, 
                           int32_t cnt )
{
    if(limits == 0 || formats == 0)
        return;

    delete [] fChoiceLimits; fChoiceLimits = 0;
    delete [] fChoiceFormats; fChoiceFormats = 0;

    // Note that the old arrays are deleted and this owns
    // the created array.
    fCount = cnt;
    fChoiceLimits = new double[fCount];
    fChoiceFormats = new UnicodeString[fCount];

    uprv_arrayCopy(limits, fChoiceLimits, fCount);
    uprv_arrayCopy(formats, fChoiceFormats, fCount);
}
 
// -------------------------------------
// Gets the limit array.
 
const double*
ChoiceFormat::getLimits(int32_t& cnt) const 
{
    cnt = fCount;
    return fChoiceLimits;
}
 
// -------------------------------------
// Gets the format array.
 
const UnicodeString*
ChoiceFormat::getFormats(int32_t& cnt) const
{
    cnt = fCount;
    return fChoiceFormats;
}
 
// -------------------------------------
// Formats a long number, it's actually formatted as
// a double.  The returned format string may differ
// from the input number because of this.
 
UnicodeString&
ChoiceFormat::format(int32_t number, 
                     UnicodeString& toAppendTo, 
                     FieldPosition& status) const
{
    return format((double) number, toAppendTo, status);
}
 
// -------------------------------------
// Formats a double number.
 
UnicodeString&
ChoiceFormat::format(double number, 
                     UnicodeString& toAppendTo, 
                     FieldPosition& /*pos*/) const
{
    // find the number
    int32_t i;
    for (i = 0; i < fCount; ++i) {
        if (!(number >= fChoiceLimits[i])) {
            // same as number < fChoiceLimits, except catches NaN
            break;
        }
    }
    --i;
    if (i < 0) 
        i = 0;
    // return either a formatted number, or a string
    return (toAppendTo += fChoiceFormats[i]);
}
 
// -------------------------------------
// Formats an array of objects. Checks if the data type of the objects
// to get the right value for formatting.  

UnicodeString&
ChoiceFormat::format(const Formattable* objs,
                     int32_t cnt,
                     UnicodeString& toAppendTo,
                     FieldPosition& pos,
                     UErrorCode& status) const
{
    if(cnt < 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return toAppendTo;
    }
    
    UnicodeString buffer;
    for (int32_t i = 0; i < cnt; i++) {
        buffer.remove();
        toAppendTo += format((objs[i].getType() == Formattable::kLong) ? objs[i].getLong() : objs[i].getDouble(), 
                             buffer, pos);
    }

    return toAppendTo;
}

// -------------------------------------
// Formats an array of objects. Checks if the data type of the objects
// to get the right value for formatting.  

UnicodeString&
ChoiceFormat::format(const Formattable& obj, 
                     UnicodeString& toAppendTo, 
                     FieldPosition& pos,
                     UErrorCode& status) const
{
    return NumberFormat::format(obj, toAppendTo, pos, status); 
}
// -------------------------------------
 
void
ChoiceFormat::parse(const UnicodeString& text, 
                    Formattable& result,
                    ParsePosition& status) const
{
    // find the best number (defined as the one with the longest parse)
    int32_t start = status.getIndex();
    int32_t furthest = start;
    double bestNumber = uprv_getNaN();
    double tempNumber = 0.0;
    for (int i = 0; i < fCount; ++i) {
        UnicodeString tempString = fChoiceFormats[i];
        if(text.compareBetween(start, tempString.length(), tempString, 0, tempString.length()) == 0) {
            status.setIndex(start + tempString.length());
            tempNumber = fChoiceLimits[i];
            if (status.getIndex() > furthest) {
                furthest = status.getIndex();
                bestNumber = tempNumber;
                if (furthest == text.length()) 
                    break;
            }
        }
    }
    status.setIndex(furthest);
    if (status.getIndex() == start) {
        status.setErrorIndex(furthest);
    }
    result.setDouble(bestNumber);
}

// -------------------------------------
// Parses the text and return the Formattable object.  
 
void
ChoiceFormat::parse(const UnicodeString& text, 
                    Formattable& result,
                    UErrorCode& status) const
{
    NumberFormat::parse(text, result, status);
}

// -------------------------------------
 
Format*
ChoiceFormat::clone() const
{
    ChoiceFormat *aCopy = new ChoiceFormat(*this);
    return aCopy;
}

// -------------------------------------

double 
ChoiceFormat::nextDouble( double d, UBool positive )
{
    return uprv_nextDouble( d, positive );
}

//eof
