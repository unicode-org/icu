/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File MSGFMT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/20/97    helena      Finished first cut of implementation.
*   04/10/97    aliu        Made to work on AIX.  Added stoi to replace wtoi.
*   06/11/97    helena      Fixed addPattern to take the pattern correctly.
*   06/17/97    helena      Fixed the getPattern to return the correct pattern.
*   07/09/97    helena      Made ParsePosition into a class.
*   02/22/99    stephen     Removed character literals for EBCDIC safety
********************************************************************************
*/

#include "unicode/msgfmt.h"
#include "unicode/decimfmt.h"
#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"
#include "unicode/choicfmt.h"
#include "unicode/ustring.h"
#include "unicode/ucnv_err.h"
#include "ustrfmt.h"

// *****************************************************************************
// class MessageFormat
// *****************************************************************************

#define COMMA             ((UChar)0x002C)
#define SINGLE_QUOTE      ((UChar)0x0027)
#define LEFT_CURLY_BRACE  ((UChar)0x007B)
#define RIGHT_CURLY_BRACE ((UChar)0x007D)

//---------------------------------------
// static data

static const UChar g_umsg_number[]    = {
    0x6E, 0x75, 0x6D, 0x62, 0x65, 0x72, 0  /* "number" */
};
static const UChar g_umsg_date[]      = {
    0x64, 0x61, 0x74, 0x65, 0  /* "date" */
};
static const UChar g_umsg_time[]      = {
    0x74, 0x69, 0x6D, 0x65, 0  /* "time" */
};
static const UChar g_umsg_choice[]    = {
    0x63, 0x68, 0x6F, 0x69, 0x63, 0x65, 0  /* "choice" */
};

// MessageFormat Type List  Number, Date, Time or Choice
static const UChar *g_umsgTypeList[] = {
    NULL,           NULL,           g_umsg_number,
    NULL,           g_umsg_date,    NULL,
    g_umsg_time,    NULL,           g_umsg_choice
};
 
static const UChar g_umsg_currency[]  = {
    0x63, 0x75, 0x72, 0x72, 0x65, 0x6E, 0x63, 0x79, 0  /* "currency" */
};
static const UChar g_umsg_percent[]   = {
    0x70, 0x65, 0x72, 0x63, 0x65, 0x6E, 0x74, 0    /* "percent" */
};
static const UChar g_umsg_integer[]   = {
    0x69, 0x6E, 0x74, 0x65, 0x67, 0x65, 0x72, 0    /* "integer" */
};

// NumberFormat modifier list, default, currency, percent or integer
static const UChar *g_umsgModifierList[] = {
    NULL,           NULL,           g_umsg_currency,
    NULL,           g_umsg_percent, NULL,
    g_umsg_integer, NULL,           NULL
};
 
static const UChar g_umsg_short[]     = {
    0x73, 0x68, 0x6F, 0x72, 0x74, 0    /* "short" */
};
static const UChar g_umsg_medium[]    = {
    0x6D, 0x65, 0x64, 0x69, 0x75, 0x6D, 0  /* "medium" */
};
static const UChar g_umsg_long[]      = {
    0x6C, 0x6F, 0x6E, 0x67, 0  /* "long" */
};
static const UChar g_umsg_full[]      = {
    0x66, 0x75, 0x6C, 0x6C, 0  /* "full" */
};

// DateFormat modifier list, default, short, medium, long or full
static const UChar *g_umsgDateModifierList[] = {
    NULL,           NULL,           g_umsg_short,
    NULL,           g_umsg_medium,  NULL,
    g_umsg_long,    NULL,           g_umsg_full
};
 
static const int32_t g_umsgListLength = 9;


U_NAMESPACE_BEGIN

// -------------------------------------
const char MessageFormat::fgClassID = 0; // Value is irrelevant

// -------------------------------------
// Creates a MessageFormat instance based on the pattern.

MessageFormat::MessageFormat(const UnicodeString& pattern,
                             UErrorCode& success)
: fLocale(Locale::getDefault()),  // Uses the default locale
  fOffsets(NULL),
  fCount(kMaxFormat),
  fArgumentNumbers(NULL)
{
    fOffsets = new int32_t[fCount];
    fArgumentNumbers = new int32_t[fCount];
    for (int32_t i = 0; i < fCount; i++) {
        fFormats[i] = NULL;       // Format instances
        fOffsets[i] = 0;          // Starting offset
        fArgumentNumbers[i] = 0;  // Argument numbers.
    }
    applyPattern(pattern, success);
}
 
MessageFormat::MessageFormat(const UnicodeString& pattern,
                             const Locale& newLocale,
                             UErrorCode& success)
: fLocale(newLocale),  // Uses the default locale
  fOffsets(NULL),
  fCount(0),
  fArgumentNumbers(NULL)
{
    fCount = kMaxFormat;
    fOffsets = new int32_t[fCount];
    fArgumentNumbers = new int32_t[fCount];
    for (int32_t i = 0; i < fCount; i++) {
        fFormats[i] = NULL;       // Format instances
        fOffsets[i] = 0;          // Starting offset
        fArgumentNumbers[i] = 0;  // Argument numbers.
    }
    applyPattern(pattern, success);
}

MessageFormat::MessageFormat(const UnicodeString& pattern,
                             const Locale& newLocale,
                             UParseError& parseError,
                             UErrorCode& success)
: fLocale(newLocale),  // Uses the default locale
  fOffsets(NULL),
  fCount(0),
  fArgumentNumbers(NULL)
{
    fCount = kMaxFormat;
    fOffsets = new int32_t[fCount];
    fArgumentNumbers = new int32_t[fCount];
    for (int32_t i = 0; i < fCount; i++) {
        fFormats[i] = NULL;       // Format instances
        fOffsets[i] = 0;          // Starting offset
        fArgumentNumbers[i] = 0;  // Argument numbers.
    }
    applyPattern(pattern,parseError, success);
}

MessageFormat::~MessageFormat()
{
    for (int32_t i = 0; i < fCount; i++) {
        if (fFormats[i]) {
            delete fFormats[i];
        }
    }
    delete [] fOffsets;
    delete [] fArgumentNumbers;
    fCount = 0;
}

// -------------------------------------
// copy constructor

MessageFormat::MessageFormat(const MessageFormat& that)
: Format(that),
  fLocale(that.fLocale),
  fPattern(that.fPattern),
  fOffsets(new int32_t[that.fCount]),
  fCount(that.fCount),
  fArgumentNumbers(new int32_t[that.fCount]),
  fMaxOffset(that.fMaxOffset)
{
    // Sets up the format instance array, offsets and argument numbers.
    for (int32_t i = 0; i < fCount; i++) {
        fFormats[i] = NULL; // init since delete may be called
        if (that.fFormats[i] != NULL) {
            setFormat(i, *(that.fFormats[i]) );  // setFormat clones the format
        }
        fOffsets[i] = that.fOffsets[i];
        fArgumentNumbers[i] = that.fArgumentNumbers[i];
    }
}

// -------------------------------------
// assignment operator

const MessageFormat&
MessageFormat::operator=(const MessageFormat& that)
{
    if (this != &that) {
        // Calls the super class for assignment first.
        Format::operator=(that);
        // Cleans up the format array and the offsets, argument numbers.
        for (int32_t j = 0; j < fCount; j++) {
            delete fFormats[j];
            fFormats[j] = NULL;
        }
        delete [] fOffsets; fOffsets = NULL;
        delete [] fArgumentNumbers; fArgumentNumbers = NULL;
        fPattern = that.fPattern;
        fLocale = that.fLocale;
        fCount = that.fCount;
        fMaxOffset = that.fMaxOffset;
        fOffsets = new int32_t[fCount];
        fArgumentNumbers = new int32_t[fCount];
        // Sets up the format instance array, offsets and argument numbers.
        for (int32_t i = 0; i < fCount; i++) {
            if (that.fFormats[i] == NULL) {
                fFormats[i] = NULL;
            }else{
                adoptFormat(i, that.fFormats[i]->clone());
            }
            fOffsets[i] = that.fOffsets[i];
            fArgumentNumbers[i] = that.fArgumentNumbers[i];
        }
    }
    return *this;
}

UBool
MessageFormat::operator==(const Format& that) const 
{
    if (this == &that) return TRUE;
    // Are the instances derived from the same Format class?
    if (getStaticClassID() != that.getDynamicClassID()) return FALSE;  // not the same class
    // Calls the super class for equality check first.
    if (!Format::operator==(that)) return FALSE;
    MessageFormat& thatAlias = (MessageFormat&)that;
    // Checks the pattern, locale and array count of this MessageFormat object.
    if (fMaxOffset != thatAlias.fMaxOffset) return FALSE;
    if (fPattern != thatAlias.fPattern) return FALSE;
    if (fLocale != thatAlias.fLocale) return FALSE;
    if (fCount != thatAlias.fCount) return FALSE;
    // Checks each element in the arrays for equality last.
    for (int32_t i = 0; i < fCount; i++) {
        if ((fFormats[i] != thatAlias.fFormats[i]) ||
            (fOffsets[i] != thatAlias.fOffsets[i]) ||
            (fArgumentNumbers[i] != thatAlias.fArgumentNumbers[i]))
            return FALSE;
    }
    return TRUE;
}

// -------------------------------------
// Creates a copy of this MessageFormat, the caller owns the copy.
 
Format*
MessageFormat::clone() const
{
    MessageFormat *aCopy = new MessageFormat(*this);
    return aCopy;
}
 
// -------------------------------------
// Sets the locale of this MessageFormat object to theLocale.
 
void
MessageFormat::setLocale(const Locale& theLocale)
{
    fLocale = theLocale;
}
 
// -------------------------------------
// Gets the locale of this MessageFormat object.
 
const Locale&
MessageFormat::getLocale() const
{
    return fLocale;
}


#if 0 
// -------------------------------------
// Applies the new pattern and returns an error if the pattern
// is not correct.
// For example, consider the pattern, 
// "There {0,choice,0#are no files|1#is one file|1<are {0,number,integer} files}"
// The segments would look like the following,
// segments[0] == "There "
// segments[1] == "0"
// segments[2] == "{0,choice,0#are no files|1#is one file|1<are {0,number,integer}"
// segments[3] == " files"
 
void
MessageFormat::applyPattern(const UnicodeString& newPattern, 
                            UErrorCode& success)
{
    UnicodeString segments[4];
    int32_t part = 0;
    int32_t formatNumber = 0;
    UBool inQuote = FALSE;
    int32_t braceStack = 0;
    fMaxOffset = -1;
    for (int i = 0; i < newPattern.length(); ++i) {
        UChar ch = newPattern[i];
        if (part == 0) {
            if (ch == SINGLE_QUOTE) {
                if (i + 1 < newPattern.length()
                    && newPattern[i+1] == SINGLE_QUOTE) {
                    segments[part] += ch;  // handle doubles
                    ++i;
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == LEFT_CURLY_BRACE && !inQuote) {
                part = 1;
            } else {
                segments[part] += ch;
            }
        } else  if (inQuote) {              // just copy quotes in parts
            segments[part] += ch;
            if (ch == SINGLE_QUOTE) {
                inQuote = FALSE;
            }
        } else {
            switch (ch) {
            case COMMA:
                if (part < 3)
                    part += 1;
                else
                    segments[part] += ch;
                break;
            case LEFT_CURLY_BRACE:
                ++braceStack;
                segments[part] += ch;
                break;
            case RIGHT_CURLY_BRACE:
                if (braceStack == 0) {
                    part = 0;
                    makeFormat(/*i,*/ formatNumber, segments, success);
                    if(U_FAILURE(success))
                        return;
                    formatNumber++;
                } else {
                    --braceStack;
                    segments[part] += ch;
                }
                break;
            case SINGLE_QUOTE:
                inQuote = TRUE;
                // fall through, so we keep quotes in other parts
            default:
                segments[part] += ch;
                break;
            }
        }
    }
    if (braceStack == 0 && part != 0) {
        fMaxOffset = -1;
        success = U_INVALID_FORMAT_ERROR;
        return;
        //throw new IllegalArgumentException("Unmatched braces in the pattern.");
    }
    fPattern = segments[0];
}
#endif 


void
MessageFormat::applyPattern(const UnicodeString& newPattern, 
                            UErrorCode& status)
{
    UParseError parseError;
    applyPattern(newPattern,parseError,status);
}


void
MessageFormat::applyPattern(const UnicodeString& newPattern, 
                            UParseError& parseError,
                            UErrorCode& success)
{
    
    if(U_FAILURE(success))
    {
        return;
    }
    UnicodeString segments[4];
    int32_t part = 0;
    int32_t maxFormatNumber = 0;
    int32_t formatNumber = 0;
    UBool inQuote = FALSE;
    int32_t braceStack = 0;
    int32_t i = 0;
    fMaxOffset = -1;
    // Clear error struct
    parseError.offset = 0;
    parseError.preContext[0] = parseError.postContext[0] = (UChar)0;
    int32_t patLen = newPattern.length();
    for (; i < patLen; ++i) {
        int32_t currFormatNumber;
        UChar ch = newPattern[i];
        if (part == 0) {
            if (ch == SINGLE_QUOTE) {
                if (i + 1 < patLen && newPattern[i+1] == SINGLE_QUOTE) {
                    segments[part] += ch;  // handle doubles
                    ++i;
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == LEFT_CURLY_BRACE && !inQuote) {
                part = 1;
            } else {
                segments[part] += ch;
            }
        } else  if (inQuote) {              // just copy quotes in parts
            segments[part] += ch;
            if (ch == SINGLE_QUOTE) {
                inQuote = FALSE;
            }
        } else {
            switch (ch) {
            case COMMA:
                if (part < 3)
                    part += 1;
                else
                    segments[part] += ch;
                break;
            case LEFT_CURLY_BRACE:
                ++braceStack;
                segments[part] += ch;
                break;
            case RIGHT_CURLY_BRACE:
                if (braceStack == 0) {
                    part = 0;
                    currFormatNumber = makeFormat(/*i,*/ formatNumber, segments, parseError,success);
                    if(U_FAILURE(success)){
                        syntaxError(newPattern,i,parseError);
                        return;
                    }
                    formatNumber++;
                    if (currFormatNumber > maxFormatNumber) {
                        maxFormatNumber = currFormatNumber;
                    }
                } else {
                    --braceStack;
                    segments[part] += ch;
                }
                break;
            case SINGLE_QUOTE:
                inQuote = TRUE;
                // fall through, so we keep quotes in other parts
            default:
                segments[part] += ch;
                break;
            }
        }
    }
    if (braceStack == 0 && part != 0) {
        fMaxOffset = -1;
        success = U_UNMATCHED_BRACES;
        syntaxError(newPattern,i,parseError);
        return;
        //throw new IllegalArgumentException("Unmatched braces in the pattern.");
    }
    fPattern = segments[0];
    fListCount = maxFormatNumber + 1;
}
// -------------------------------------
// Converts this MessageFormat instance to a pattern. 
UnicodeString&
MessageFormat::toPattern(UnicodeString& result) const
{
    // later, make this more extensible
    int32_t lastOffset = 0;
    for (int i = 0; i <= fMaxOffset; ++i) {
        copyAndFixQuotes(fPattern, lastOffset, fOffsets[i], result);
        lastOffset = fOffsets[i];
        result += LEFT_CURLY_BRACE;
        // {sfb} check this later
        //result += (UChar) (fArgumentNumbers[i] + '0');
        UnicodeString temp;
        result += itos(fArgumentNumbers[i], temp);
        if (fFormats[i] == NULL) {
            // do nothing, string format
        } 
        else if (fFormats[i]->getDynamicClassID() == DecimalFormat::getStaticClassID()) {
            
            UErrorCode status = U_ZERO_ERROR;
            NumberFormat& formatAlias = *(NumberFormat*)fFormats[i];
            NumberFormat *numberTemplate = NumberFormat::createInstance(fLocale, status);
            NumberFormat *currencyTemplate = NumberFormat::createCurrencyInstance(fLocale, status);
            NumberFormat *percentTemplate = NumberFormat::createPercentInstance(fLocale, status);
            NumberFormat *integerTemplate = createIntegerFormat(fLocale, status);
 
            result += COMMA;
            result += g_umsg_number;
            if (formatAlias != *numberTemplate) {
                result += COMMA;
                if (formatAlias == *currencyTemplate) {
                    result += g_umsg_currency;
                } 
                else if (formatAlias == *percentTemplate) {
                    result += g_umsg_percent;
                } 
                else if (formatAlias == *integerTemplate) {
                    result += g_umsg_integer;
                } 
                else {
                    UnicodeString buffer;
                    result += ((DecimalFormat*)fFormats[i])->toPattern(buffer);
                }
            }
            
            delete numberTemplate;
            delete currencyTemplate;
            delete percentTemplate;
            delete integerTemplate;
        } 
        else if (fFormats[i]->getDynamicClassID() == SimpleDateFormat::getStaticClassID()) {
            DateFormat& formatAlias = *(DateFormat*)fFormats[i];
            DateFormat *defaultDateTemplate = DateFormat::createDateInstance(DateFormat::kDefault, fLocale);
            DateFormat *shortDateTemplate = DateFormat::createDateInstance(DateFormat::kShort, fLocale);
            DateFormat *longDateTemplate = DateFormat::createDateInstance(DateFormat::kLong, fLocale);
            DateFormat *fullDateTemplate = DateFormat::createDateInstance(DateFormat::kFull, fLocale);
            DateFormat *defaultTimeTemplate = DateFormat::createTimeInstance(DateFormat::kDefault, fLocale);
            DateFormat *shortTimeTemplate = DateFormat::createTimeInstance(DateFormat::kShort, fLocale);
            DateFormat *longTimeTemplate = DateFormat::createTimeInstance(DateFormat::kLong, fLocale);
            DateFormat *fullTimeTemplate = DateFormat::createTimeInstance(DateFormat::kFull, fLocale);
            
            
            result += COMMA;
            if (formatAlias == *defaultDateTemplate) {
                result += g_umsg_date;
            } 
            else if (formatAlias == *shortDateTemplate) {
                result += g_umsg_date;
                result += COMMA;
                result += g_umsg_short;
            } 
            else if (formatAlias == *defaultDateTemplate) {
                result += g_umsg_date;
                result += COMMA;
                result += g_umsg_medium;
            } 
            else if (formatAlias == *longDateTemplate) {
                result += g_umsg_date;
                result += COMMA;
                result += g_umsg_long;
            } 
            else if (formatAlias == *fullDateTemplate) {
                result += g_umsg_date;
                result += COMMA;
                result += g_umsg_full;
            } 
            else if (formatAlias == *defaultTimeTemplate) {
                result += g_umsg_time;
            } 
            else if (formatAlias == *shortTimeTemplate) {
                result += g_umsg_time;
                result += COMMA;
                result += g_umsg_short;
            } 
            else if (formatAlias == *defaultTimeTemplate) {
                result += g_umsg_time;
                result += COMMA;
                result += g_umsg_medium;
            } 
            else if (formatAlias == *longTimeTemplate) {
                result += g_umsg_time;
                result += COMMA;
                result += g_umsg_long;
            } 
            else if (formatAlias == *fullTimeTemplate) {
                result += g_umsg_time;
                result += COMMA;
                result += g_umsg_full;
            } 
            else {
                UnicodeString buffer;
                result += g_umsg_date;
                result += COMMA;
                result += ((SimpleDateFormat*)fFormats[i])->toPattern(buffer);
            }
            
            delete defaultDateTemplate;
            delete shortDateTemplate;
            delete longDateTemplate;
            delete fullDateTemplate;
            delete defaultTimeTemplate;
            delete shortTimeTemplate;
            delete longTimeTemplate;
            delete fullTimeTemplate;
            // {sfb} there should be a more efficient way to do this!
        } 
        else if (fFormats[i]->getDynamicClassID() == ChoiceFormat::getStaticClassID()) {
            UnicodeString buffer;
            result += COMMA;
            result += g_umsg_choice;
            result += COMMA;
            result += ((ChoiceFormat*)fFormats[i])->toPattern(buffer);
        } 
        else {
            //result += ", unknown";
        }
        result += RIGHT_CURLY_BRACE;
  }
  copyAndFixQuotes(fPattern, lastOffset, fPattern.length(), result);
  return result;
}
 
// -------------------------------------
// Adopts the new formats array and updates the array count.
// This MessageFormat instance owns the new formats.
 
void
MessageFormat::adoptFormats(Format** newFormats,
                            int32_t cnt)
{
    if(newFormats == NULL || cnt < 0)
        return;
    
    int32_t i;
    // Cleans up first.
    for (i = 0; i < fCount; i++)
        delete fFormats[i];
    fCount = (cnt > kMaxFormat) ? kMaxFormat : cnt;
    for (i = 0; i < fCount; i++)
        fFormats[i] = newFormats[i];
    for (i = kMaxFormat; i < cnt; i++) 
        delete newFormats[i];
}   
// -------------------------------------
// Sets the new formats array and updates the array count.
// This MessageFormat instance maks a copy of the new formats.
 
void
MessageFormat::setFormats(const Format** newFormats,
                            int32_t cnt)
{
    if(newFormats == NULL || cnt < 0)
        return;

    int32_t i;
    // Cleans up first.
    for (i = 0; i < fCount; i++) {
        delete fFormats[i];
    }
    fCount = (cnt > kMaxFormat) ? kMaxFormat : cnt;
    for (i = 0; i < fCount; i++) {
        if (newFormats[i] == NULL) {
            fFormats[i] = NULL;
        }
        else{
            fFormats[i] = newFormats[i]->clone();
        }
    }
}   
 
// -------------------------------------
// Adopts the first *variable* formats in the format array.
// This MessageFormat instance owns the new formats.
// Do nothing is the variable is not less than the array count.
 
void
MessageFormat::adoptFormat(int32_t variable, Format *newFormat)
{
    if(variable < 0)
        return;
        
    if (variable < fCount) {
        // Deletes the old formats.
        delete fFormats[variable];
        fFormats[variable] = newFormat;
    }
}

// -------------------------------------
// Sets the first *variable* formats in the format array, this
// MessageFormat instance makes copies of the new formats.
// Do nothing is the variable is not less than the array count.
 
void
MessageFormat::setFormat(int32_t variable, const Format& newFormat)
{
    if (variable < fCount) {
        delete fFormats[variable];
        if (&(newFormat) == NULL) {
            fFormats[variable] = NULL;
        }
        else{
            fFormats[variable] = newFormat.clone();
        }
    }
}
 
// -------------------------------------
// Gets the format array.
 
const Format**
MessageFormat::getFormats(int32_t& cnt) const
{
    cnt = fCount;
    return (const Format**)fFormats;
}
 
// -------------------------------------
// Formats the source Formattable array and copy into the result buffer.
// Ignore the FieldPosition result for error checking.
 
UnicodeString&
MessageFormat::format(const Formattable* source,
                      int32_t cnt, 
                      UnicodeString& result, 
                      FieldPosition& ignore, 
                      UErrorCode& success) const
{
    if (U_FAILURE(success)) 
        return result;
    
    return format(source, cnt, result, ignore, 0, success);
}
 
// -------------------------------------
// Internally creates a MessageFormat instance based on the
// pattern and formats the arguments Formattable array and 
// copy into the result buffer.
 
UnicodeString&
MessageFormat::format(  const UnicodeString& pattern,
                        const Formattable* arguments,
                        int32_t cnt,
                        UnicodeString& result, 
                        UErrorCode& success)
{
    // {sfb} why does this use a local when so many other places use a static?
    MessageFormat *temp = new MessageFormat(pattern, success);
    if (U_FAILURE(success)) 
        return result;
    FieldPosition ignore(0);
    temp->format(arguments, cnt, result, ignore, success);
    delete temp;
    return result;
}
 
// -------------------------------------
// Formats the source Formattable object and copy into the 
// result buffer.  The Formattable object must be an array
// of Formattable instances, returns error otherwise.
 
UnicodeString&
MessageFormat::format(const Formattable& source, 
                      UnicodeString& result, 
                      FieldPosition& ignore, 
                      UErrorCode& success) const
{
    int32_t cnt;

    if (U_FAILURE(success)) 
        return result;
    if (source.getType() != Formattable::kArray) {
        success = U_ILLEGAL_ARGUMENT_ERROR;
        return result;
    }
    const Formattable* tmpPtr = source.getArray(cnt);
    
    return format(tmpPtr, cnt, result, ignore, 0, success);
}
 
// -------------------------------------
// Formats the arguments Formattable array and copy into the result buffer.
// Ignore the FieldPosition result for error checking.

UnicodeString&
MessageFormat::format(const Formattable* arguments, 
                      int32_t cnt, 
                      UnicodeString& result, 
                      FieldPosition& status, 
                      int32_t recursionProtection,
                      UErrorCode& success) const 
{
    if(/*arguments == NULL ||*/ cnt < 0) {
        success = U_ILLEGAL_ARGUMENT_ERROR;
        return result;
    }
    
    UnicodeString buffer;
    
    int32_t lastOffset = 0;
    for (int32_t i = 0; i <= fMaxOffset;++i) {
        // Cleans up the temp buffer for each formattable arguments.
        buffer.remove();
        // Append the prefix of current format element.
        fPattern.extract(lastOffset, fOffsets[i] - lastOffset, buffer);
        result += buffer;
        lastOffset = fOffsets[i];
        int32_t argumentNumber = fArgumentNumbers[i];
        // Checks the scope of the argument number.
        if (argumentNumber >= cnt) {
            /*success = U_ILLEGAL_ARGUMENT_ERROR;
            return result;*/
            result += LEFT_CURLY_BRACE;
            UnicodeString temp;
            result += itos(argumentNumber, temp);
            result += RIGHT_CURLY_BRACE;
            continue;
        }

        Formattable obj = arguments[argumentNumber];
        UnicodeString arg;
        UBool tryRecursion = FALSE;
        // Recursively calling the format process only if the current format argument
        // refers to a ChoiceFormat object.
        if (fFormats[i] != NULL) {
            fFormats[i]->format(obj, arg, success);
            tryRecursion = (fFormats[i]->getDynamicClassID() == ChoiceFormat::getStaticClassID());
        }
        // If the obj data type if a number, use a NumberFormat instance.
        else if ((obj.getType() == Formattable::kDouble) ||
                 (obj.getType() == Formattable::kLong)) {
            NumberFormat *numTemplate = NULL;
            numTemplate = NumberFormat::createInstance(fLocale, success);
            if (U_FAILURE(success)) { 
                delete numTemplate; 
                return result; 
            }
            numTemplate->format((obj.getType() == Formattable::kDouble) ? obj.getDouble() : obj.getLong(), arg);
            delete numTemplate;
            if (U_FAILURE(success)) 
                return result;
        }
        // If the obj data type is a Date instance, use a DateFormat instance.
        else if (obj.getType() == Formattable::kDate) {
            DateFormat *dateTemplate = NULL;
            dateTemplate = DateFormat::createDateTimeInstance(DateFormat::kShort, DateFormat::kShort, fLocale);
            dateTemplate->format(obj.getDate(), arg);
            delete dateTemplate;
        }
        else if (obj.getType() == Formattable::kString) {
            obj.getString(arg);
        }
        else {
#ifdef LIUDEBUG  
            cerr << "Unknown object of type:" << obj.getType() << endl;
#endif
            success = U_ILLEGAL_ARGUMENT_ERROR;
            return result;
        }
        // Needs to reprocess the ChoiceFormat option by using the MessageFormat
        // pattern application.
        if (tryRecursion && arg.indexOf(LEFT_CURLY_BRACE) >= 0) {
            MessageFormat *temp = NULL;
            temp = new MessageFormat(arg, fLocale, success);
            if (U_FAILURE(success)) 
                return result;
            temp->format(arguments, cnt, result, status, recursionProtection, success);
            if (U_FAILURE(success)) { 
                delete temp; 
                return result; 
            }
            delete temp;
        }
        else {
            result += arg;
        }
    }
    buffer.remove();
    // Appends the rest of the pattern characters after the real last offset.
    fPattern.extract(lastOffset, fPattern.length(), buffer);
    result += buffer;
    return result;
}


// -------------------------------------
// Parses the source pattern and returns the Formattable objects array,
// the array count and the ending parse position.  The caller of this method 
// owns the array.
 
Formattable*
MessageFormat::parse(const UnicodeString& source, 
                     ParsePosition& status,
                     int32_t& count) const
{
    Formattable *resultArray = new Formattable[kMaxFormat];
    int32_t patternOffset = 0;
    int32_t sourceOffset = status.getIndex();
    ParsePosition tempStatus(0);
    count = 0; // {sfb} reset to zero
    for (int32_t i = 0; i <= fMaxOffset; ++i) {
        // match up to format
        int32_t len = fOffsets[i] - patternOffset;
        if (len == 0 || 
            fPattern.compare(patternOffset, len, source, sourceOffset, len) == 0) {
            sourceOffset += len;
            patternOffset += len;
        } 
        else {
            status.setErrorIndex(sourceOffset);
            delete [] resultArray;
            count = 0;
            return NULL; // leave index as is to signal error
        }
        
        // now use format
        if (fFormats[i] == NULL) {   // string format
            // if at end, use longest possible match
            // otherwise uses first match to intervening string
            // does NOT recursively try all possibilities
            int32_t tempLength = (i != fMaxOffset) ? fOffsets[i+1] : fPattern.length();
            
            int32_t next;
            if (patternOffset >= tempLength) {
                next = source.length();
            }
            else {
                UnicodeString buffer;
                fPattern.extract(patternOffset,tempLength - patternOffset, buffer);
                next = source.indexOf(buffer, sourceOffset);
            }
            
            if (next < 0) {
                status.setErrorIndex(sourceOffset);
                delete [] resultArray;
                count = 0;
                return NULL; // leave index as is to signal error
            } 
            else {
                UnicodeString buffer;
                source.extract(sourceOffset,next - sourceOffset, buffer);
                UnicodeString strValue = buffer;
                UnicodeString temp(LEFT_CURLY_BRACE);
                // {sfb} check this later
                UnicodeString temp1;
                temp += itos(fArgumentNumbers[i], temp1);
                temp += RIGHT_CURLY_BRACE;
                if (strValue != temp) {
                    source.extract(sourceOffset,next - sourceOffset, buffer);
                    resultArray[fArgumentNumbers[i]].setString(buffer);
                    // {sfb} not sure about this
                    if ((fArgumentNumbers[i] + 1) > count) {
                        count = (fArgumentNumbers[i] + 1);
                    }
                }
                sourceOffset = next;
            }
        } 
        else {
            tempStatus.setIndex(sourceOffset);
            fFormats[i]->parseObject(source, resultArray[fArgumentNumbers[i]], tempStatus);
            if (tempStatus.getIndex() == sourceOffset) {
                status.setErrorIndex(sourceOffset);
                delete [] resultArray;
                count = 0;
                return NULL; // leave index as is to signal error
            }
            
            if ((fArgumentNumbers[i] + 1) > count)
                count = (fArgumentNumbers[i] + 1);
            
            sourceOffset = tempStatus.getIndex(); // update
        }
    }
    int32_t len = fPattern.length() - patternOffset;
    if (len == 0 || 
        fPattern.compare(patternOffset, len, source, sourceOffset, len) == 0) {
        status.setIndex(sourceOffset + len);
    } 
    else {
        status.setErrorIndex(sourceOffset);
        delete [] resultArray;
        count = 0;
        return NULL; // leave index as is to signal error
    }
    
    return resultArray;
}
 
// -------------------------------------
// Parses the source string and returns the array of 
// Formattable objects and the array count.  The caller 
// owns the returned array.
 
Formattable*
MessageFormat::parse(const UnicodeString& source, 
                     int32_t& cnt,
                     UErrorCode& success) const
{
    ParsePosition status(0);
    // Calls the actual implementation method and starts
    // from zero offset of the source text.
    Formattable* result = parse(source, status, cnt);
    if (status.getIndex() == 0) {
        success = U_MESSAGE_PARSE_ERROR;
        return NULL;
    }
    return result;
}
 
// -------------------------------------
// Parses the source text and copy into the result buffer.
 
void
MessageFormat::parseObject( const UnicodeString& source,
                            Formattable& result,
                            ParsePosition& status) const
{
    int32_t cnt = 0;
    Formattable* tmpResult = parse(source, status, cnt);
    if (tmpResult != NULL) 
        result.adoptArray(tmpResult, cnt);
}
  
// -------------------------------------
// NumberFormat cache management

/*
NumberFormat* 
MessageFormat::getNumberFormat(UErrorCode &status)
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
        theFormat->setParseIntegerOnly(TRUE);
    }

    return theFormat;
}

void          
MessageFormat::releaseNumberFormat(NumberFormat *adopt)
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
*/

/**
 * Converts a string to an integer value using a default NumberFormat object
 * which is static (shared by all MessageFormat instances).  This replaces
 * a call to wtoi().
 */
int32_t
MessageFormat::stoi(const UnicodeString& string)
{
    /*
    NumberFormat *myFormat = getNumberFormat(status);

    if(U_FAILURE(status))
        return -1; // OK?

    Formattable result;
    // Uses the global number formatter to parse the string.
    // Note: We assume here that parse() is thread-safe.
    myFormat->parse(string, result, status);
    releaseNumberFormat(myFormat);

    int32_t value = 0;
    if (U_SUCCESS(status) && result.getType() == Formattable::kLong)
        value = result.getLong();


    return value;
    */
    
    /* this ignores any white spaces between '{' and digit char
     * so now we can have {  0, date} {0 , date }
     */
    for(int i=0;i<string.length();i++){
        UChar32 ch = string.char32At(i);
        if(u_isdigit(ch)){
            return u_charDigitValue(ch);
        }else if(!u_isspace(ch)){
            break;
        }

    }
    return -1;
}

// -------------------------------------

/**
 * Converts an integer value to a string using a default NumberFormat object
 * which is static (shared by all MessageFormat instances).  This replaces
 * a call to wtoi().
 */
UnicodeString&
MessageFormat::itos(int32_t i,
                    UnicodeString& string)
{
    /*
    UErrorCode status = U_ZERO_ERROR;
    NumberFormat *myFormat = getNumberFormat(status);

    if(U_FAILURE(status)) {
        // "<ERROR>" 
        static const UChar ERROR[] = {0x3C, 0x45, 0x52, 0x52, 0x4F, 0x52, 0x3E, 0};

        return string = ERROR; // TODO: maybe toPattern should take an errorcode.
    }

    UnicodeString &retval = myFormat->format(i, string);

    releaseNumberFormat(myFormat);
    return retval;
    */
    UChar temp[10] = { '\0' };
    uprv_itou(temp,i,16,0);
    string.append(temp);
    return string;
}

// -------------------------------------
// Checks which format instance we are really using based on the segments.
 
int32_t
MessageFormat::makeFormat(/*int32_t position, */
                          int32_t offsetNumber, 
                          UnicodeString* segments,
                          UParseError& parseError,
                          UErrorCode& success)
{
    if(U_FAILURE(success))
        return -1;

    // get the number
    int32_t argumentNumber = stoi(segments[1]); // always unlocalized!
    int32_t oldMaxOffset = fMaxOffset;
    if (argumentNumber < 0 || argumentNumber > 9) {
        success = U_INVALID_FORMAT_ERROR;
        return argumentNumber;
    }
    fMaxOffset = offsetNumber;
    fOffsets[offsetNumber] = segments[0].length();
    fArgumentNumbers[offsetNumber] = argumentNumber;
    int test = 0;
    // now get the format
    Format *newFormat = NULL;
    switch (findKeyword(segments[2], g_umsgTypeList)) {
    case 0:
        fFormatTypeList[argumentNumber] = Formattable::kString;
        break;
    case 1: case 2:// number
        test=findKeyword(segments[3], g_umsgModifierList);
        fFormatTypeList[argumentNumber] = Formattable::kDouble;

        switch (test) {
        case 0: // default;
            newFormat = NumberFormat::createInstance(fLocale, success);
            break;
        case 1: case 2:// currency
            newFormat = NumberFormat::createCurrencyInstance(fLocale, success);
            break;
        case 3: case 4:// percent
            newFormat = NumberFormat::createPercentInstance(fLocale, success);
            break;
        case 5: case 6:// integer
            fFormatTypeList[argumentNumber] = Formattable::kLong;
            newFormat = createIntegerFormat(fLocale, success);
            break;
        default: // pattern
            newFormat = NumberFormat::createInstance(fLocale, success);
            if(U_FAILURE(success)) {
                newFormat = NULL;
                return argumentNumber;
            }
            if(newFormat->getDynamicClassID() == DecimalFormat::getStaticClassID()){
                ((DecimalFormat*)newFormat)->applyPattern(segments[3],parseError,success);
            }
            if(U_FAILURE(success)) {
                fMaxOffset = oldMaxOffset;
                return argumentNumber;
            }
            break;
        }
        break;

    case 3: case 4: // date
        fFormatTypeList[argumentNumber] = Formattable::kDate;

        switch (findKeyword(segments[3], g_umsgDateModifierList)) {
        case 0: // default
            newFormat = DateFormat::createDateInstance(DateFormat::kDefault, fLocale);
            break;
        case 1: case 2: // short
            newFormat = DateFormat::createDateInstance(DateFormat::kShort, fLocale);
            break;
        case 3: case 4: // medium
            newFormat = DateFormat::createDateInstance(DateFormat::kDefault, fLocale);
            break;
        case 5: case 6: // long
            newFormat = DateFormat::createDateInstance(DateFormat::kLong, fLocale);
            break;
        case 7: case 8: // full
            newFormat = DateFormat::createDateInstance(DateFormat::kFull, fLocale);
            break;
        default:
            newFormat = DateFormat::createDateInstance(DateFormat::kDefault, fLocale);
            if(newFormat->getDynamicClassID() == SimpleDateFormat::getStaticClassID()){
                    ((SimpleDateFormat*)newFormat)->applyPattern(segments[3]);
            }
                /* Ram: 'success' is not passed to above methods 
                   and is not set so we donot have to check for failure.
                if(U_FAILURE(success)) {
                    fMaxOffset = oldMaxOffset;
                    success = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
                */
            break;
        }
        break;
    case 5: case 6:// time

        fFormatTypeList[argumentNumber]= Formattable::kDate;
        
        switch (findKeyword(segments[3], g_umsgDateModifierList)) {
        case 0: // default
            newFormat = DateFormat::createTimeInstance(DateFormat::kDefault, fLocale);
            break;
        case 1: case 2: // short
            newFormat = DateFormat::createTimeInstance(DateFormat::kShort, fLocale);
            break;
        case 3: case 4: // medium
            newFormat = DateFormat::createTimeInstance(DateFormat::kDefault, fLocale);
            break;
        case 5: case 6: // long
            newFormat = DateFormat::createTimeInstance(DateFormat::kLong, fLocale);
            break;
        case 7: case 8: // full
            newFormat = DateFormat::createTimeInstance(DateFormat::kFull, fLocale);
            break;
        default:
            newFormat = DateFormat::createTimeInstance(DateFormat::kDefault, fLocale);
            if(newFormat->getDynamicClassID() == SimpleDateFormat::getStaticClassID()){
                    ((SimpleDateFormat*)newFormat)->applyPattern(segments[3]);
            }
                /* Ram: 'success' is not passed to above methods 
                   and is not set so we donot have to check for failure.
                if(U_FAILURE(success)) {
                    fMaxOffset = oldMaxOffset;
                    success = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
                */
            break;
        }
        break;
    case 7: case 8:// choice
        fFormatTypeList[argumentNumber] = Formattable::kDouble;

        newFormat = new ChoiceFormat(segments[3],parseError,success);
        if(U_FAILURE(success)) {
            fMaxOffset = oldMaxOffset;
            return argumentNumber;
        }
        break;
    default:
        fMaxOffset = oldMaxOffset;
        success = U_ILLEGAL_ARGUMENT_ERROR;
        return argumentNumber;
    }

    if(newFormat != NULL) {
        delete fFormats[offsetNumber];
        fFormats[offsetNumber] = newFormat;
    }
    segments[1].remove();   // throw away other segments
    segments[2].remove();
    segments[3].remove();

    return argumentNumber;
}
 
// -------------------------------------
// Finds the string, s, in the string array, list. 
int32_t MessageFormat::findKeyword(const UnicodeString& s, 
                           const UChar **list)
{
    if (s.length() == 0)
        return 0;

    UnicodeString buffer = s;
    // Trims the space characters and turns all characters
    // in s to lower case.
    buffer.trim().toLower();
    for (int32_t i = 0; i < g_umsgListLength; ++i) {
        if (list[i] && !buffer.compare(list[i], u_strlen(list[i]))) 
            return i;
    }
    return -1;
}
  
// -------------------------------------
// Checks the range of the source text to quote the special
// characters, { and ' and copy to target buffer.
 
void
MessageFormat::copyAndFixQuotes(const UnicodeString& source, 
                                int32_t start, 
                                int32_t end, 
                                UnicodeString& target)
{
    UBool gotLB = FALSE;
    
    for (UTextOffset i = start; i < end; ++i) {
        UChar ch = source[i];
        if (ch == LEFT_CURLY_BRACE) {
            target += SINGLE_QUOTE;
            target += LEFT_CURLY_BRACE;
            target += SINGLE_QUOTE;
            gotLB = TRUE;
        } 
        else if (ch == RIGHT_CURLY_BRACE) {
            if(gotLB) {
                target += RIGHT_CURLY_BRACE;
                gotLB = FALSE;
            }
            else {
                // orig code.
                target += SINGLE_QUOTE;
                target += RIGHT_CURLY_BRACE;
                target += SINGLE_QUOTE;
            }
        } 
        else if (ch == SINGLE_QUOTE) {
            target += SINGLE_QUOTE;
            target += SINGLE_QUOTE;
        } 
        else {
            target += ch;
        }
    }
}

/**
 * Convenience method that ought to be in NumberFormat
 */
NumberFormat* 
MessageFormat::createIntegerFormat(const Locale& locale, UErrorCode& status) const {
    NumberFormat *temp = NumberFormat::createInstance(locale, status);
    if (temp->getDynamicClassID() == DecimalFormat::getStaticClassID()) {
        DecimalFormat *temp2 = (DecimalFormat*) temp;
        temp2->setMaximumFractionDigits(0);
        temp2->setDecimalSeparatorAlwaysShown(FALSE);
        temp2->setParseIntegerOnly(TRUE);
    }

    return temp;
}

U_NAMESPACE_END

//eof
