/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1998      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
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
 
#include "msgfmt.h"
#include "decimfmt.h"
#include "datefmt.h"
#include "smpdtfmt.h"
#include "choicfmt.h"
#include "mutex.h"
 
// *****************************************************************************
// class MessageFormat
// *****************************************************************************
 
// -------------------------------------
char MessageFormat::fgClassID = 0; // Value is irrelevant
 
// This global NumberFormat instance is shared by all MessageFormat to 
// convert a number to(format)/from(parse) a string.
NumberFormat* MessageFormat::fgNumberFormat = 0;

// -------------------------------------
// Creates a MessageFormat instance based on the pattern.

MessageFormat::MessageFormat(const UnicodeString& pattern,
                             UErrorCode& success)
: fOffsets(NULL),
  fArgumentNumbers(NULL),
  fLocale(Locale::getDefault()),  // Uses the default locale
  fCount(0)
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
                             UErrorCode& success)
: fOffsets(NULL),
  fArgumentNumbers(NULL),
  fLocale(newLocale),  // Uses the default locale
  fCount(0)
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

MessageFormat::~MessageFormat()
{
    for (int32_t i = 0; i < fCount; i++)
        delete fFormats[i];
    delete [] fOffsets;
    delete [] fArgumentNumbers;
    fCount = 0;
}

// -------------------------------------
// copy constructor

MessageFormat::MessageFormat(const MessageFormat& that)
    : Format(that),
      fOffsets(NULL),
      fCount(that.fCount),
      fLocale(that.fLocale),
      fMaxOffset(that.fMaxOffset),
      fArgumentNumbers(NULL),
      fPattern(that.fPattern)
{
    fOffsets = new int32_t[fCount];
    fArgumentNumbers = new int32_t[fCount];
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

bool_t
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
    bool_t inQuote = FALSE;
    int32_t braceStack = 0;
    fMaxOffset = -1;
    for (int i = 0; i < newPattern.size(); ++i) {
        UChar ch = newPattern[i];
        if (part == 0) {
            if (ch == 0x0027 /*'\''*/) {
                if (i + 1 < newPattern.size()
                    && newPattern[i+1] == 0x0027 /*'\''*/) {
                    segments[part] += ch;  // handle doubles
                    ++i;
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == 0x007B /*'{'*/ && !inQuote) {
                part = 1;
            } else {
                segments[part] += ch;
            }
        } else  if (inQuote) {              // just copy quotes in parts
            segments[part] += ch;
            if (ch == 0x0027 /*'\''*/) {
                inQuote = FALSE;
            }
        } else {
            switch (ch) {
            case 0x002C /*','*/:
                if (part < 3)
                    part += 1;
                else
                    segments[part] += ch;
                break;
            case 0x007B /*'{'*/:
                ++braceStack;
                segments[part] += ch;
                break;
            case 0x007D /*'}'*/:
                if (braceStack == 0) {
                    part = 0;
                    makeFormat(i, formatNumber, segments, success);
                    if(FAILURE(success))
                        return;
                    formatNumber++;
                } else {
                    --braceStack;
                    segments[part] += ch;
                }
                break;
            case 0x0027 /*'\''*/:
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
        success = INVALID_FORMAT_ERROR;
        return;
        //throw new IllegalArgumentException("Unmatched braces in the pattern.");
    }
    fPattern = segments[0];
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
    result += 0x007B /*'{'*/;
    // {sfb} check this later
    //result += (UChar) (fArgumentNumbers[i] + '0');
    UnicodeString temp;
    result += itos(fArgumentNumbers[i], temp);
    if (fFormats[i] == NULL) {
      // do nothing, string format
    } 
    else if (fFormats[i]->getDynamicClassID() == DecimalFormat::getStaticClassID()) {
      
      UErrorCode status = ZERO_ERROR;
      NumberFormat& formatAlias = *(NumberFormat*)fFormats[i];
      NumberFormat *numberTemplate = NumberFormat::createInstance(fLocale, status);
      NumberFormat *currencyTemplate = NumberFormat::createCurrencyInstance(fLocale, status);
      NumberFormat *percentTemplate = NumberFormat::createPercentInstance(fLocale, status);
      NumberFormat *integerTemplate = createIntegerFormat(fLocale, status);
      
      if (formatAlias == *numberTemplate) {
    result += ",number";
      } 
      else if (formatAlias == *currencyTemplate) {
    result += ",number,currency";
      } 
      else if (formatAlias == *percentTemplate) {
    result += ",number,percent";
      } 
      else if (formatAlias == *integerTemplate) {
    result += ",number,integer";
      } 
      else {
    UnicodeString buffer;
    result += ",number,";
    result += ((DecimalFormat*)fFormats[i])->toPattern(buffer);
      }
      
      delete numberTemplate;
      delete currencyTemplate;
      delete percentTemplate;
      delete integerTemplate;
    } 
    else if (fFormats[i]->getDynamicClassID() == SimpleDateFormat::getStaticClassID()) {
      UErrorCode success = ZERO_ERROR;
      DateFormat& formatAlias = *(DateFormat*)fFormats[i];
      DateFormat *defaultDateTemplate = DateFormat::createDateInstance(DateFormat::kDefault, fLocale);
      DateFormat *shortDateTemplate = DateFormat::createDateInstance(DateFormat::kShort, fLocale);
      DateFormat *longDateTemplate = DateFormat::createDateInstance(DateFormat::kLong, fLocale);
      DateFormat *fullDateTemplate = DateFormat::createDateInstance(DateFormat::kFull, fLocale);
      DateFormat *defaultTimeTemplate = DateFormat::createTimeInstance(DateFormat::kDefault, fLocale);
      DateFormat *shortTimeTemplate = DateFormat::createTimeInstance(DateFormat::kShort, fLocale);
      DateFormat *longTimeTemplate = DateFormat::createTimeInstance(DateFormat::kLong, fLocale);
      DateFormat *fullTimeTemplate = DateFormat::createTimeInstance(DateFormat::kFull, fLocale);
      
      
      if (formatAlias == *defaultDateTemplate) {
    result += ",date";
      } 
      else if (formatAlias == *shortDateTemplate) {
    result += ",date,short";
      } 
      else if (formatAlias == *defaultDateTemplate) {
    result += ",date,medium";
      } 
      else if (formatAlias == *longDateTemplate) {
    result += ",date,long";
      } 
      else if (formatAlias == *fullDateTemplate) {
    result += ",date,full";
      } 
      else if (formatAlias == *defaultTimeTemplate) {
    result += ",time";
      } 
      else if (formatAlias == *shortTimeTemplate) {
    result += ",time,short";
      } 
      else if (formatAlias == *defaultTimeTemplate) {
    result += ",time,medium";
      } 
      else if (formatAlias == *longTimeTemplate) {
    result += ",time,long";
      } 
      else if (formatAlias == *fullTimeTemplate) {
    result += ",time,full";
      } 
      else {
    UnicodeString buffer;
    result += ",date,";
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
      result += ",choice,";
      result += ((ChoiceFormat*)fFormats[i])->toPattern(buffer);
    } 
    else {
      //result += ", unknown";
    }
    result += 0x007D /*'}'*/;
  }
  copyAndFixQuotes(fPattern, lastOffset, fPattern.size(), result);
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
    for (i = 0; i < fCount; i++)
        delete fFormats[i];
    fCount = (cnt > kMaxFormat) ? kMaxFormat : cnt;
    for (i = 0; i < fCount; i++)
        if (newFormats[i] == NULL) {
            fFormats[i] = NULL;
        }
        else{
            fFormats[i] = newFormats[i]->clone();
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
    if (FAILURE(success)) 
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
    if (FAILURE(success)) 
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

    if (FAILURE(success)) 
        return result;
    if (source.getType() != Formattable::kArray) {
        success = ILLEGAL_ARGUMENT_ERROR;
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
        success = ILLEGAL_ARGUMENT_ERROR;
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
            /*success = ILLEGAL_ARGUMENT_ERROR;
            return result;*/
            result += "{";
            UnicodeString temp;
            result += itos(argumentNumber, temp);
            result += "}";
            continue;
        }

        Formattable obj = arguments[argumentNumber];
        UnicodeString arg;
        bool_t tryRecursion = FALSE;
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
            if (FAILURE(success)) { 
                delete numTemplate; 
                return result; 
            }
            numTemplate->format((obj.getType() == Formattable::kDouble) ? obj.getDouble() : obj.getLong(), arg);
            delete numTemplate;
            if (FAILURE(success)) 
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
            success = ILLEGAL_ARGUMENT_ERROR;
            return result;
        }
        // Needs to reprocess the ChoiceFormat option by using the MessageFormat
        // pattern application.
        if (tryRecursion && arg.indexOf("{") >= 0) {
            MessageFormat *temp = NULL;
            temp = new MessageFormat(arg, fLocale, success);
            if (FAILURE(success)) 
                return result;
            temp->format(arguments, cnt, result, status, recursionProtection, success);
            if (FAILURE(success)) { 
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
    fPattern.extract(lastOffset, fPattern.size(), buffer);
    result += buffer;
    return result;
}
 
// MessageFormat Type List  Number, Date, Time or Choice
const UnicodeString MessageFormat::fgTypeList[] = {
    "", "", "number", "", "date", "", "time", "", "choice"
};
 
// NumberFormat modifier list, default, currency, percent or integer
const UnicodeString MessageFormat::fgModifierList[] = {
    "", "", "currency", "", "percent", "", "integer", "", ""
};
 
// DateFormat modifier list, default, short, medium, long or full
const UnicodeString MessageFormat::fgDateModifierList[] = {
    "", "", "short", "", "medium", "", "long", "", "full"
};
 
const int32_t MessageFormat::fgListLength= 9;

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
      int32_t tempLength = (i != fMaxOffset) ? fOffsets[i+1] : fPattern.size();

      int32_t next;
      if (patternOffset >= tempLength) {
    next = source.size();
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
    UnicodeString temp("{");
                // {sfb} check this later
    UnicodeString temp1;
    temp += itos(fArgumentNumbers[i], temp1);
    temp += "}";
    if (strValue != temp) {
      source.extract(sourceOffset,next - sourceOffset, buffer);
      resultArray[fArgumentNumbers[i]].setString(buffer);
      // {sfb} not sure about this
      if ((fArgumentNumbers[i] + 1) > count) 
        count = (fArgumentNumbers[i] + 1);
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
  int32_t len = fPattern.size() - patternOffset;
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
        success = MESSAGE_PARSE_ERROR;
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
        if(FAILURE(status))
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


/**
 * Converts a string to an integer value using a default NumberFormat object
 * which is static (shared by all MessageFormat instances).  This replaces
 * a call to wtoi().
 */
int32_t
MessageFormat::stoi(const UnicodeString& string,
                    UErrorCode& status)
{
    NumberFormat *myFormat = getNumberFormat(status);

    if(FAILURE(status))
        return -1; // OK?

    Formattable result;
    // Uses the global number formatter to parse the string.
    // Note: We assume here that parse() is thread-safe.
    myFormat->parse(string, result, status);
    releaseNumberFormat(myFormat);

    int32_t value = 0;
    if (SUCCESS(status) && result.getType() == Formattable::kLong)
        value = result.getLong();


    return value;
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
    UErrorCode status = ZERO_ERROR;
    NumberFormat *myFormat = getNumberFormat(status);

    if(FAILURE(status))
        return (string = "<ERROR>"); // _REVISIT_ maybe toPattern should take an errorcode.

    UnicodeString &retval = myFormat->format(i, string);

    releaseNumberFormat(myFormat);

    return retval;
}

// -------------------------------------
// Checks which format instance we are really using based on the segments.
 
void
MessageFormat::makeFormat(int32_t position, 
                          int32_t offsetNumber, 
                          UnicodeString* segments,
                          UErrorCode& success)
{
    if(FAILURE(success))
        return;

    // get the number
    int32_t argumentNumber;
    int32_t oldMaxOffset = fMaxOffset;
    argumentNumber = stoi(segments[1], success); // always unlocalized!
    if (argumentNumber < 0 || argumentNumber > 9) {
        success = INVALID_FORMAT_ERROR;
        return;
    }
    fMaxOffset = offsetNumber;
    fOffsets[offsetNumber] = segments[0].size();
    fArgumentNumbers[offsetNumber] = argumentNumber;

    // now get the format
    Format *newFormat = NULL;
    switch (findKeyword(segments[2], fgTypeList)) {
    case 0:
        break;
    case 1: case 2:// number
        switch (findKeyword(segments[3], fgModifierList)) {
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
            newFormat = createIntegerFormat(fLocale, success);
            break;
        default: // pattern
            newFormat = NumberFormat::createInstance(fLocale, success);
            if(FAILURE(success)) {
                newFormat = NULL;
                return;
            }
            if(newFormat->getDynamicClassID() == DecimalFormat::getStaticClassID())
                ((DecimalFormat*)newFormat)->applyPattern(segments[3], success);
            if(FAILURE(success)) {
                fMaxOffset = oldMaxOffset;
                success = ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            break;
        }
        break;

    case 3: case 4: // date
        switch (findKeyword(segments[3], fgDateModifierList)) {
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
                if(newFormat->getDynamicClassID() == SimpleDateFormat::getStaticClassID())
                    ((SimpleDateFormat*)newFormat)->applyPattern(segments[3]);
                if(FAILURE(success)) {
                    fMaxOffset = oldMaxOffset;
                    success = ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
            break;
        }
        break;
    case 5: case 6:// time
        switch (findKeyword(segments[3], fgDateModifierList)) {
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
                if(newFormat->getDynamicClassID() == SimpleDateFormat::getStaticClassID())
                    ((SimpleDateFormat*)newFormat)->applyPattern(segments[3]);
                if(FAILURE(success)) {
                    fMaxOffset = oldMaxOffset;
                    success = ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
            break;
        }
        break;
    case 7: case 8:// choice
            newFormat = new ChoiceFormat(segments[3], success);
        if(FAILURE(success)) {
            fMaxOffset = oldMaxOffset;
            success = ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        break;
    default:
        fMaxOffset = oldMaxOffset;
        success = ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(newFormat != NULL) {
         delete fFormats[offsetNumber];
        fFormats[offsetNumber] = newFormat;
    }
    segments[1].remove();   // throw away other segments
    segments[2].remove();
    segments[3].remove();

}
 
// -------------------------------------
// Finds the string, s, in the string array, list. 
int32_t MessageFormat::findKeyword(const UnicodeString& s, 
                           const UnicodeString* list)
{
  UnicodeString buffer = s;
  // Trims the space characters and turns all characters
  // in s to lower case.
  buffer.trim().toLower();
  for (int32_t i = 0; i < fgListLength; ++i) {
    if (buffer == list[i]) 
      return i;
  }
  return - 1;
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
  bool_t gotLB = FALSE;
  
  for (UTextOffset i = start; i < end; ++i) {
    UChar ch = source[i];
    if (ch == 0x007B /*'{'*/) {
      target += "'{'";
      gotLB = TRUE;
    } 
    else if (ch == 0x007D /*'}'*/) {
      if(gotLB) {
    target += "}";
    gotLB = FALSE;
      }
      else
    // orig code.
    target += "'}'";
    } 
    else if (ch == 0x0027 /*'\''*/) {
      target += "''";
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

//eof
